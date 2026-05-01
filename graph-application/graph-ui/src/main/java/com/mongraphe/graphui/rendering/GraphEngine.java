package com.mongraphe.graphui.rendering;

import java.nio.ByteBuffer;
import java.nio.ByteOrder;
import java.nio.FloatBuffer;
import java.util.ArrayList;
import java.util.HashMap;
import java.util.Iterator;
import java.util.List;
import java.util.Map;
import java.util.concurrent.ConcurrentLinkedQueue;
import java.util.concurrent.Executors;
import java.util.concurrent.ScheduledExecutorService;
import java.util.concurrent.ScheduledFuture;
import java.util.concurrent.TimeUnit;
import java.util.concurrent.atomic.AtomicLong;

import com.mongraphe.graphui.model.*;

/**
 * Cœur logique de l'application orchestrant la simulation, les données et le
 * rendu du graphe.
 *
 * <p>
 * Cette classe fait le pont entre la couche utilisateur JavaFX et le moteur de
 * calcul natif.
 * Elle gère le cycle de vie de la simulation de forces, le filtrage de
 * visibilité,
 * ainsi que la synchronisation des positions des sommets via un tampon mémoire
 * partagé (Direct ByteBuffer).
 * </p>
 *
 * <h2>Architecture</h2>
 * <ul>
 * <li><b>Thread de Simulation :</b> Un exécuteur dédié rafraîchit les positions
 * à intervalle régulier (16ms).</li>
 * <li><b>Modèle de Données :</b> Maintient une structure {@link GraphModel}
 * synchronisée avec les données natives.</li>
 * <li><b>Système d'Événements :</b> Notifie les auditeurs lors des changements
 * de données ou d'état de simulation.</li>
 * </ul>
 */
public final class GraphEngine {

    private final GraphNativeEngine nativeEngine;
    private final Camera2D camera;
    private final GraphModel model;
    private final GraphVisibilityFilter visibility;

    private volatile float clearR = 1f, clearG = 1f, clearB = 1f, clearA = 1f;
    private int clusterUpdateFrequency = 1;
    private boolean initialized = false;

    /**
     * Version logique des données utilisées par le renderer.
     *
     * <p>
     * Le renderer compare cette version avec la dernière version envoyée au GPU.
     * Si la version n'a pas changé, il ne reconstruit pas les tableaux CPU et ne
     * fait pas de glBufferSubData inutile.
     * </p>
     */
    private final AtomicLong renderDataVersion = new AtomicLong(0L);

    /**
     * Accès direct nativeId -> Vertex.
     *
     * <p>
     * Cela évite une recherche dans le modèle à chaque synchronisation de position.
     * </p>
     */
    private volatile Vertex[] verticesByNativeId = new Vertex[0];

    /** Exécuteur gérant le thread de calcul de la simulation de forces. */
    private final ScheduledExecutorService simulationExecutor = Executors.newSingleThreadScheduledExecutor(r -> {
        Thread t = new Thread(r, "graph-simulation-thread");
        t.setDaemon(true);
        return t;
    });
    private final Object simulationLock = new Object();
    private volatile ScheduledFuture<?> simulationTask;
    private volatile boolean simulationRunning = false;
    private volatile boolean graphLoaded = false;
    private volatile int simulationTicksPerSecond = 60;

    /** Interface pour suivre l'état d'exécution de la simulation. */
    public interface GraphEngineListener {
        void onSimulationStarted();

        void onSimulationStopped();
    }

    private final List<GraphEngineListener> listeners = new ArrayList<>();

    /** Interface pour être notifié des modifications structurelles du graphe. */
    public interface GraphDataListener {
        void onGraphDataChanged();
    }

    private final List<GraphDataListener> dataListeners = new ArrayList<>();

    public void addDataListener(GraphDataListener listener) {
        synchronized (dataListeners) {
            dataListeners.add(listener);
        }
    }

    public void removeDataListener(GraphDataListener listener) {
        synchronized (dataListeners) {
            dataListeners.remove(listener);
        }
    }

    public long renderDataVersion() {
        return renderDataVersion.get();
    }

    private void markRenderDataDirty() {
        renderDataVersion.incrementAndGet();
    }

    private void notifyDataChanged() {
        markRenderDataDirty();

        List<GraphDataListener> copy;
        synchronized (dataListeners) {
            copy = new ArrayList<>(dataListeners);
        }
        for (GraphDataListener l : copy) {
            l.onGraphDataChanged();
        }
    }

    /**
     * Snapshot immuable des statistiques actuelles du graphe (pour l'UI).
     */
    public record StatsSnapshot(
            int visibleVertices, int hiddenVertices, int deletedVertices,
            int visibleEdges, int hiddenEdges, int totalVertices, int totalEdges,
            Metadata initMetadata, int selectedVertexId, double selectedX, double selectedY) {
    }

    /** Calcule et retourne un état instantané des statistiques. */
    public StatsSnapshot getStatsSnapshot() {
        int totalVertices = model.vertices().size();
        int totalEdges = model.edges().size();
        long deletedVertices = model.getDeletedVerticesCount();
        int visibleVertices = model.getVisibleVertexCount();
        int visibleEdges = model.getVisibleEdgeCount();
        int hiddenVertices = Math.max(0, totalVertices - visibleVertices - (int) deletedVertices);
        int hiddenEdges = Math.max(0, totalEdges - visibleEdges);
        Metadata initMetadata = getInitMetadata();
        int selectedId = model.getSelectedVertexId();
        double selectedX = 0, selectedY = 0;
        Vertex sel = model.vertexById(selectedId);
        if (sel != null) {
            selectedX = sel.getX();
            selectedY = sel.getY();
        }
        return new StatsSnapshot(visibleVertices, hiddenVertices, (int) deletedVertices,
                visibleEdges, hiddenEdges, totalVertices, totalEdges,
                initMetadata, selectedId, selectedX, selectedY);
    }

    /**
     * Snapshot complet des données pour les opérations d'export ou de rendu
     * statique.
     */
    public static class GraphDataSnapshot {
        private final List<Vertex> vertices;
        private final ConcurrentLinkedQueue<Edge> edges;
        private final Map<Integer, Vertex> verticesById;
        private final int visibleVertexCount;
        private final int visibleEdgeCount;

        public GraphDataSnapshot(List<Vertex> vertices, ConcurrentLinkedQueue<Edge> edges, int visibleVertexCount,
                int visibleEdgeCount) {
            this.vertices = new ArrayList<>(vertices);
            this.edges = edges;
            this.visibleVertexCount = visibleVertexCount;
            this.visibleEdgeCount = visibleEdgeCount;
            this.verticesById = new HashMap<>();
            for (Vertex v : vertices)
                verticesById.put(v.getId(), v);
        }

        public List<Vertex> getVertices() {
            return vertices;
        }

        public ConcurrentLinkedQueue<Edge> getEdges() {
            return edges;
        }

        public int getVisibleVertexCount() {
            return visibleVertexCount;
        }

        public int getVisibleEdgeCount() {
            return visibleEdgeCount;
        }

        public Vertex getVertexById(int id) {
            return verticesById.get(id);
        }
    }

    /**
     * Représentation d'une page de données pour la pagination dans les tables UI.
     */
    public static class GraphPage<T> {
        private final List<T> data;
        private final int totalSize;

        public GraphPage(List<T> data, int totalSize) {
            this.data = data;
            this.totalSize = totalSize;
        }

        public List<T> getData() {
            return data;
        }

        public int getTotalSize() {
            return totalSize;
        }
    }

    /** Récupère une portion paginée des sommets. */
    public GraphPage<Vertex> getVerticesPage(int page, int pageSize) {
        ConcurrentLinkedQueue<Vertex> all = model.vertices();
        int total = all.size();
        int from = page * pageSize;
        if (from >= total)
            return new GraphPage<>(List.of(), total);
        List<Vertex> pageData = new ArrayList<>(pageSize);
        Iterator<Vertex> it = all.iterator();
        for (int i = 0; i < from && it.hasNext(); i++)
            it.next();
        for (int i = 0; i < pageSize && it.hasNext(); i++)
            pageData.add(it.next());
        return new GraphPage<>(pageData, total);
    }

    /** Récupère une portion paginée des arêtes. */
    public GraphPage<Edge> getEdgesPage(int page, int pageSize) {
        ConcurrentLinkedQueue<Edge> all = model.edges();
        int total = all.size();
        int from = page * pageSize;
        if (from >= total)
            return new GraphPage<>(List.of(), total);
        List<Edge> pageData = new ArrayList<>(pageSize);
        Iterator<Edge> it = all.iterator();
        for (int i = 0; i < from && it.hasNext(); i++)
            it.next();
        for (int i = 0; i < pageSize && it.hasNext(); i++)
            pageData.add(it.next());
        return new GraphPage<>(pageData, total);
    }

    public GraphEngine(GraphNativeEngine nativeEngine) {
        this.nativeEngine = nativeEngine;
        this.camera = new Camera2D();
        this.model = new GraphModel();
        this.visibility = new GraphVisibilityFilter();
    }

    public boolean initialized() {
        return initialized;
    }

    public boolean isGraphLoaded() {
        return graphLoaded;
    }

    public void addListener(GraphEngineListener listener) {
        synchronized (listeners) {
            listeners.add(listener);
        }
    }

    public void removeListener(GraphEngineListener listener) {
        synchronized (listeners) {
            listeners.remove(listener);
        }
    }

    public GraphDataSnapshot getDataSnapshot() {
        return new GraphDataSnapshot(
                new ArrayList<>(model.vertices()),
                model.edges(),
                model.getVisibleVertexCount(),
                model.getVisibleEdgeCount());
    }

    /** Mappe l'énumération de communauté vers l'index entier du moteur natif. */
    private int getModeCommunity(GraphData.NodeCommunity community) {
        if (community == null)
            throw new IllegalArgumentException("Le mode de communauté ne peut pas être nul.");
        return switch (community) {
            case LOUVAIN -> 0;
            case LOUVAIN_PAR_COMPOSANTE -> 1;
            case LEIDEN -> 2;
            case LEIDEN_CPM -> 3;
            case COULEURS_SPECIALES -> 4;
        };
    }

    /** Mappe l'énumération de similitude vers l'index entier du moteur natif. */
    private int getModeSimilitude(GraphData.SimilitudeMode mode) {
        if (mode == null)
            throw new IllegalArgumentException("Le mode de similarité ne peut pas être nul.");
        return switch (mode) {
            case CORRELATION -> 0;
            case DISTANCE_COSINE -> 1;
            case DISTANCE_EUCLIDIENNE -> 2;
            case NORME_L1 -> 3;
            case NORME_LINF -> 4;
            case KL_DIVERGENCE -> 5;
        };
    }

    public void startProgram(String path) {
        if (path == null || path.isBlank())
            throw new IllegalArgumentException("CSV path missing");
        nativeEngine.startsProgram(path);
    }

    /**
     * Réinitialise complètement le graphe chargé côté Java et côté natif.
     *
     * <p>
     * Cette méthode est appelée avant tout rechargement afin d'éviter qu'un ancien
     * thread de simulation, un ancien buffer direct ou un ancien état natif
     * continuent d'accéder à des structures C devenues invalides.
     * </p>
     */
    private void resetLoadedGraph() {
        stopSimulation();
        graphLoaded = false;
        verticesByNativeId = new Vertex[0];
        nativeEngine.freeAllocatedMemory();
        model.clear();
        notifyDataChanged();
    }

    /**
     * Charge un graphe depuis un CSV en configurant les seuils et modes
     * algorithmiques.
     */
    public void loadCsv(GraphData.SimilitudeMode sim,
            GraphData.NodeCommunity communityMode,
            double edgeThreshold, double antiThreshold) {
        resetLoadedGraph();
        int modeSim = getModeSimilitude(sim);
        int modeComm = getModeCommunity(communityMode);
        nativeEngine.initializeGraph(modeSim, modeComm, edgeThreshold, antiThreshold);
        rebuildModelFromNative();
        graphLoaded = true;
        notifyDataChanged();
    }

    /** Charge un graphe depuis un fichier au format DOT. */
    public void loadDot(String path, GraphData.NodeCommunity communityMode) {
        if (path == null || path.isBlank())
            throw new IllegalArgumentException("DOT path missing");
        resetLoadedGraph();
        int modeComm = getModeCommunity(communityMode);
        nativeEngine.initializeDot(path, modeComm);
        rebuildModelFromNative();
        graphLoaded = true;
        notifyDataChanged();
    }

    /** Calcule les seuils de distribution pour les données chargées. */
    public Metadata computeThreshold(GraphData.SimilitudeMode md, int edgeFactor) {
        int i = getModeSimilitude(md);
        return nativeEngine.computeThreshold(i, edgeFactor);
    }

    /**
     * Synchronise intégralement le modèle Java avec les structures de données
     * natives.
     *
     * <p>
     * Version optimisée : elle ne demande plus au JNI de construire un
     * {@code Vertex[]} et un {@code EdgeC[]}. Les données arrivent dans des buffers
     * primitifs directs, puis la reconstruction objet est faite côté Java dans
     * {@link GraphModel}.
     * </p>
     */
    private void rebuildModelFromNative() {
        GraphNativeEngine.NativeGraphBuffers nativeBuffers = nativeEngine.readNativeGraphBuffers();

        GraphModel.NativeBuildResult result = model.buildFromNativeBuffers(
                nativeBuffers.vertexCount(),
                nativeBuffers.edgeCount(),
                nativeBuffers.positionsAsFloatBuffer(),
                nativeBuffers.edgeEndpointsAsIntBuffer(),
                nativeBuffers.edgeWeightsAsFloatBuffer(),
                nativeBuffers.communityIdsAsIntBuffer(),
                nativeBuffers.communityColorsAsFloatBuffer());

        verticesByNativeId = result.verticesByNativeId();

        visibility.apply(model);
        markRenderDataDirty();
    }

    /** Démarre le thread de simulation de forces. */
    public void startSimulation() {
        camera.reset();
        boolean started = false;
        synchronized (simulationLock) {
            if (!graphLoaded || simulationRunning)
                return;
            simulationRunning = true;
            rescheduleSimulationTaskLocked();
            started = true;
        }
        if (started) {
            synchronized (listeners) {
                for (GraphEngineListener listener : listeners)
                    listener.onSimulationStarted();
            }
        }
    }

    /** Arrête la simulation. */
    public void stopSimulation() {
        boolean stopped = false;
        synchronized (simulationLock) {
            if (!simulationRunning && simulationTask == null)
                return;
            simulationRunning = false;
            if (simulationTask != null) {
                simulationTask.cancel(false);
                simulationTask = null;
            }
            stopped = true;
        }
        if (stopped) {
            synchronized (listeners) {
                for (GraphEngineListener listener : listeners)
                    listener.onSimulationStopped();
            }
        }
    }

    /** Exécute une itération de simulation et synchronise les positions. */
    private void simulateStep() {
        if (!simulationRunning || !graphLoaded)
            return;
        ByteBuffer buf = nativeEngine.sharedPositionsBuffer;
        if (buf == null)
            return;
        if (nativeEngine.updatePositions(buf)) {
            updateVerticesFromBuffer();
        }
    }

    /**
     * Lit le tampon natif partagé pour mettre à jour les coordonnées des Vertex
     * Java.
     */
    private void updateVerticesFromBuffer() {
        ByteBuffer buf = nativeEngine.sharedPositionsBuffer;
        if (buf == null)
            return;

        Vertex[] vertices = verticesByNativeId;
        if (vertices == null || vertices.length == 0)
            return;

        ByteBuffer duplicate = buf.duplicate();
        duplicate.order(ByteOrder.nativeOrder());
        duplicate.position(0);

        FloatBuffer fb = duplicate.asFloatBuffer();
        int count = Math.min(vertices.length, fb.capacity() / 2);

        for (int i = 0; i < count; i++) {
            Vertex v = vertices[i];
            if (v == null)
                continue;

            float x = fb.get(i * 2);
            float y = fb.get(i * 2 + 1);
            v.updatePosition(x, y);
        }

        markRenderDataDirty();
    }

    public boolean isSimulationRunning() {
        return simulationRunning;
    }

    /** Retourne la fréquence d'exécution de la routine de simulation. */
    public int getSimulationTicksPerSecond() {
        return simulationTicksPerSecond;
    }

    public GraphModel model() {
        return model;
    }

    public Camera2D camera() {
        return camera;
    }

    public GraphVisibilityFilter visibility() {
        return visibility;
    }

    public Metadata getMetadata() {
        return nativeEngine.getMetadata();
    }

    public Metadata getInitMetadata() {
        return nativeEngine.getInitMetadata();
    }

    public void setBackgroundColor(float r, float g, float b, float a) {
        clearR = clamp01(r);
        clearG = clamp01(g);
        clearB = clamp01(b);
        clearA = clamp01(a);
    }

    public void setSelectedVertexId(int id) {
        model.setSelectedVertexId(id);
        notifyDataChanged();
    }

    public float getBackgroundColorR() {
        return clearR;
    }

    public float getBackgroundColorG() {
        return clearG;
    }

    public float getBackgroundColorB() {
        return clearB;
    }

    public float getBackgroundColorA() {
        return clearA;
    }

    public void setColoringMode(GraphModel.ColoringMode mode) {
        model.setColoringMode(mode);
        notifyDataChanged();
    }

    public void setUniformNodeColor(float r, float g, float b) {
        model.setUniformNodeColor(r, g, b);
        notifyDataChanged();
    }

    public void setMinimumDegree(int degree) {
        visibility.setMinimumDegree(degree);
        model.setFilterMinDegree(degree);
        visibility.apply(model);
        notifyDataChanged();
    }

    public void setMinimumEdgeWeight(double weight) {
        model.setFilterMinEdgeWeight(weight);
        visibility.setEdgeWeightThreshold((float) weight);
        visibility.apply(model);
        notifyDataChanged();
    }

    public void setInitialNodeSize(double size) {
        Vertex.initial_node_size = size;
        nativeEngine.setInitialNodeSize(size);

        Vertex[] vertices = verticesByNativeId;
        if (vertices != null && vertices.length > 0) {
            for (Vertex v : vertices) {
                if (v != null)
                    v.updateDiameter();
            }
        } else {
            for (Vertex v : model.vertices())
                v.updateDiameter();
        }

        notifyDataChanged();
    }

    public void setDegreeScaleFactor(double factor) {
        Vertex.degree_scale_factor = factor;
        nativeEngine.setDegreeScaleFactor(factor);

        Vertex[] vertices = verticesByNativeId;
        if (vertices != null && vertices.length > 0) {
            for (Vertex v : vertices) {
                if (v != null)
                    v.updateDiameter();
            }
        } else {
            for (Vertex v : model.vertices())
                v.updateDiameter();
        }

        notifyDataChanged();
    }

    /**
     * Modifie la fréquence d'exécution de la routine de simulation.
     *
     * @param hz nombre d'itérations par seconde
     */
    public void setSimulationTicksPerSecond(int hz) {
        if (hz < 1 || hz > 240)
            throw new IllegalArgumentException("La fréquence de simulation doit être comprise entre 1 et 240 Hz.");
        synchronized (simulationLock) {
            simulationTicksPerSecond = hz;
            if (simulationRunning && graphLoaded) {
                rescheduleSimulationTaskLocked();
            }
        }
    }

    /** Reprogramme la tâche périodique de simulation avec la fréquence courante. */
    private void rescheduleSimulationTaskLocked() {
        if (simulationTask != null) {
            simulationTask.cancel(false);
            simulationTask = null;
        }
        long periodNanos = Math.max(1L, 1_000_000_000L / Math.max(1, simulationTicksPerSecond));
        simulationTask = simulationExecutor.scheduleAtFixedRate(this::simulateStep, 0, periodNanos,
                TimeUnit.NANOSECONDS);
    }

    public void setNewFriction(double f) {
        nativeEngine.setFriction(f);
    }

    public void setRepulsionCoeff(double coeff) {
        nativeEngine.setRepulsionCoeff(coeff);
    }

    public void setLambda(double eps) {
        nativeEngine.setLambda(eps);
    }

    public void setAttractionCoefficient(double c) {
        nativeEngine.setAttractionCoeff(c);
    }

    public void setRepulsionThreshold(double t) {
        nativeEngine.setSeuilRep(t);
    }

    public void setAntiRepulsion(double t) {
        nativeEngine.setAntiRepulsion(t);
    }

    public void setNewAmortissement(double a) {
        nativeEngine.setAmortissement(a);
    }

    public void setSpatialCells(int cells) {
        nativeEngine.setSpatialCells(cells);
        notifyDataChanged();
    }

    public void setRepulsionMode(GraphData.RepulsionMode mode) {
        if (mode != null)
            nativeEngine.setModeRepulsion(mode.ordinal());
    }

    public void setKmeansMode(boolean enabled) {
        nativeEngine.setKmeansMode(enabled);
    }

    public void setDimensions(double width, double height) {
        nativeEngine.setDimension(width, height);
    }

    public double[] getDimensions() {
        return nativeEngine.getDimensions();
    }

    public void setNodePosition(int index, double x, double y) {
        nativeEngine.setNodePosition(index, x, y);

        Vertex v = vertexByNativeId(index);
        if (v != null) {
            v.updatePosition(x, y);
            notifyDataChanged();
        }
    }

    public void deleteNode(int index) {
        nativeEngine.deleteNode(index);
        Vertex v = vertexByNativeId(index);
        if (v != null) {
            model.deleteVertex(v);
            visibility.apply(model);
        }
        notifyDataChanged();
    }

    public void restoreNode(int index) {
        nativeEngine.restoreNode(index);

        Vertex v = vertexByNativeId(index);
        if (v != null) {
            v.restore();
            model.applyCurrentFilters();
            visibility.apply(model);
            notifyDataChanged();
            return;
        }

        rebuildModelFromNative();
        notifyDataChanged();
    }

    private Vertex vertexByNativeId(int index) {
        Vertex[] vertices = verticesByNativeId;
        if (vertices != null && index >= 0 && index < vertices.length) {
            return vertices[index];
        }
        return model.vertexById(index);
    }

    public int getClusterUpdateFrequency() {
        return clusterUpdateFrequency;
    }

    /** Libère proprement les ressources natives et arrête les threads. */
    public void dispose() {
        resetLoadedGraph();
        simulationExecutor.shutdown();
        try {
            if (!simulationExecutor.awaitTermination(1, TimeUnit.SECONDS)) {
                simulationExecutor.shutdownNow();
            }
        } catch (InterruptedException e) {
            simulationExecutor.shutdownNow();
            Thread.currentThread().interrupt();
        }
        model.clear();
        verticesByNativeId = new Vertex[0];
        markRenderDataDirty();
    }

    private float clamp01(float v) {
        return v < 0f ? 0f : (v > 1f ? 1f : v);
    }
}
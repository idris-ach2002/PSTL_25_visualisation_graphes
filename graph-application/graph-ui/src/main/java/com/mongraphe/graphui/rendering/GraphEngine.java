package com.mongraphe.graphui.rendering;

import java.nio.ByteBuffer;
import java.nio.FloatBuffer;
import java.util.ArrayList;
import java.util.HashMap;
import java.util.Iterator;
import java.util.List;
import java.util.Map;
import java.util.concurrent.ConcurrentLinkedQueue;
import java.util.concurrent.Executors;
import java.util.concurrent.ScheduledExecutorService;
import java.util.concurrent.TimeUnit;

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

    /** Exécuteur gérant le thread de calcul de la simulation de forces. */
    private final ScheduledExecutorService simulationExecutor = Executors.newSingleThreadScheduledExecutor(r -> {
        Thread t = new Thread(r, "graph-simulation-thread");
        t.setDaemon(true);
        return t;
    });
    private volatile boolean simulationRunning = false;
    private volatile boolean graphLoaded = false;

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

    private void notifyDataChanged() {
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
     * Charge un graphe depuis un CSV en configurant les seuils et modes
     * algorithmiques.
     */
    public void loadCsv(GraphData.SimilitudeMode sim,
            GraphData.NodeCommunity communityMode,
            double edgeThreshold, double antiThreshold) {
        graphLoaded = false;
        int modeSim = getModeSimilitude(sim);
        int modeComm = getModeCommunity(communityMode);
        nativeEngine.initializeGraph(modeSim, modeComm, edgeThreshold, antiThreshold);
        rebuildModelFromNative();
        notifyDataChanged();
        graphLoaded = true;
    }

    /** Charge un graphe depuis un fichier au format DOT. */
    public void loadDot(String path, GraphData.NodeCommunity communityMode) {
        if (path == null || path.isBlank())
            throw new IllegalArgumentException("DOT path missing");
        graphLoaded = false;
        int modeComm = getModeCommunity(communityMode);
        nativeEngine.initializeDot(path, modeComm);
        rebuildModelFromNative();
        graphLoaded = true;
    }

    /** Calcule les seuils de distribution pour les données chargées. */
    public Metadata computeThreshold(GraphData.SimilitudeMode md, int edgeFactor) {
        int i = getModeSimilitude(md);
        return nativeEngine.computeThreshold(i, edgeFactor);
    }

    /**
     * Synchronise intégralement le modèle Java avec les structures de données
     * natives.
     */
    private void rebuildModelFromNative() {
        Vertex[] verticesArray = nativeEngine.getPositions();
        EdgeC[] edgesArray = nativeEngine.getEdges();
        int[] communityIds = nativeEngine.getCommunities();
        float[][] colors = nativeEngine.getCommunityColors();

        if (verticesArray == null || edgesArray == null) {
            throw new IllegalStateException("Moteur natif sans données");
        }

        for (int i = 0; i < verticesArray.length; i++) {
            if (verticesArray[i] != null)
                verticesArray[i].setId(i);
        }

        Map<Integer, Community> communities = new HashMap<>();
        for (int i = 0; i < verticesArray.length; i++) {
            int cid = (communityIds != null && i < communityIds.length) ? communityIds[i] : 0;
            Community c = communities.get(cid);
            if (c == null) {
                float r = (colors != null && i < colors.length) ? colors[i][0] : 0.7f;
                float g = (colors != null && i < colors.length) ? colors[i][1] : 0.7f;
                float b = (colors != null && i < colors.length) ? colors[i][2] : 0.7f;
                c = new Community(cid, r, g, b);
                communities.put(cid, c);
            }
            if (verticesArray[i] != null)
                verticesArray[i].setCommunity(c);
        }

        model.clear();
        for (Vertex v : verticesArray) {
            if (v == null)
                continue;
            v.updateDiameter();
            model.addVertex(v);
        }
        for (EdgeC ec : edgesArray) {
            if (ec == null)
                continue;
            Vertex start = model.vertexById(ec.getStart());
            Vertex end = model.vertexById(ec.getEnd());
            if (start != null && end != null) {
                model.addEdge(new Edge(start, end, ec.getWeight()));
            }
        }

        nativeEngine.initSharedPositionsBuffer(model.vertexCount());
        visibility.apply(model);
    }

    /** Démarre le thread de simulation de forces. */
    public void startSimulation() {
        camera.reset();
        if (!graphLoaded)
            return;
        if (simulationRunning)
            return;
        simulationRunning = true;
        simulationExecutor.scheduleAtFixedRate(this::simulateStep, 0, 16, TimeUnit.MILLISECONDS);
        synchronized (listeners) {
            for (GraphEngineListener listener : listeners)
                listener.onSimulationStarted();
        }
    }

    /** Arrête la simulation. */
    public void stopSimulation() {
        simulationRunning = false;
        synchronized (listeners) {
            for (GraphEngineListener listener : listeners)
                listener.onSimulationStopped();
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
        FloatBuffer fb = buf.asFloatBuffer();
        fb.rewind();
        for (Vertex v : model().vertices()) {
            float x = fb.get();
            float y = fb.get();
            if (v != null) {
                v.updatePosition(x, y);
            }
        }
    }

    public boolean isSimulationRunning() {
        return simulationRunning;
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
        visibility.apply(model);
        model.setFilterMinDegree(degree);
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
        for (Vertex v : model.vertices())
            v.updateDiameter();
    }

    public void setDegreeScaleFactor(double factor) {
        Vertex.degree_scale_factor = factor;
        nativeEngine.setDegreeScaleFactor(factor);
        for (Vertex v : model.vertices())
            v.updateDiameter();
        notifyDataChanged();
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
        Vertex v = model.vertexById(index);
        if (v != null) {
            v.updatePosition(x, y);
            notifyDataChanged();
        }
    }

    public void deleteNode(int index) {
        nativeEngine.deleteNode(index);
        Vertex v = model.vertexById(index);
        if (v != null)
            model.deleteVertex(v);
        notifyDataChanged();
    }

    public void restoreNode(int index) {
        nativeEngine.restoreNode(index);
        rebuildModelFromNative();
        notifyDataChanged();
    }

    public int getClusterUpdateFrequency() {
        return clusterUpdateFrequency;
    }

    /** Libère proprement les ressources natives et arrête les threads. */
    public void dispose() {
        simulationRunning = false;
        graphLoaded = false;
        simulationExecutor.shutdown();
        try {
            if (!simulationExecutor.awaitTermination(1, TimeUnit.SECONDS)) {
                simulationExecutor.shutdownNow();
            }
        } catch (InterruptedException e) {
            simulationExecutor.shutdownNow();
            Thread.currentThread().interrupt();
        }
        nativeEngine.freeAllocatedMemory();
        model.clear();
    }

    private float clamp01(float v) {
        return v < 0f ? 0f : (v > 1f ? 1f : v);
    }
}
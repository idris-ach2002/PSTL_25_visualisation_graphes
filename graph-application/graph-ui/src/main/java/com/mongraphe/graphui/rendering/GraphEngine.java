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
import java.util.concurrent.atomic.AtomicInteger;

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

    /** Version incrementee uniquement lorsqu'un nouveau slot de positions est publié. */
    private final AtomicLong renderPositionVersion = new AtomicLong(0L);

    /** Nombre de slots de positions utilisés pour découpler simulation, upload GPU et rendu. */
    private static final int POSITION_SLOT_COUNT = 3;

    /** Buffers directs écrits par le JNI. Un slot publié n'est jamais réécrit immédiatement. */
    private volatile ByteBuffer[] positionSlotBytes = new ByteBuffer[0];

    /** Vues float des slots directs, conservées pour éviter les allocations par frame. */
    private volatile FloatBuffer[] positionSlotFloats = new FloatBuffer[0];

    /** Dernier slot entièrement écrit par le moteur natif et prêt pour le renderer. */
    private final AtomicInteger latestReadyPositionSlot = new AtomicInteger(-1);

    /** Slot que le thread simulation tentera d'utiliser au prochain tick. */
    private int nextSimulationWriteSlot = 0;

    /** Nombre de sommets disponibles dans le buffer de positions de rendu. */
    private final AtomicInteger renderPositionVertexCount = new AtomicInteger(0);

    /** Dernier temps de synchronisation des objets Vertex Java pour stats/interaction. */
    private volatile long lastJavaVertexSyncNanos = 0L;

    /** Fréquence maximale de synchronisation des objets Vertex Java. */
    private static final long JAVA_VERTEX_SYNC_INTERVAL_NANOS = 250_000_000L;

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

    /** Retourne la version des positions publiees pour le rendu. */
    public long renderPositionVersion() {
        return renderPositionVersion.get();
    }

    /**
     * Retourne un duplicata direct du buffer de positions le plus récent.
     *
     * @return positions x,y en mémoire directe, ou null si aucun graphe n est chargé
     */
    public FloatBuffer renderPositionsBuffer() {
        int slot = latestReadyPositionSlot.get();
        FloatBuffer[] slots = positionSlotFloats;
        if (slot < 0 || slot >= slots.length) {
            return null;
        }
        FloatBuffer source = slots[slot];
        if (source == null) {
            return null;
        }
        FloatBuffer duplicate = source.duplicate();
        duplicate.position(0);
        duplicate.limit(Math.min(duplicate.capacity(), renderPositionVertexCount.get() * 2));
        return duplicate;
    }

    /** Retourne le nombre de sommets presents dans le buffer de positions de rendu. */
    public int renderPositionVertexCount() {
        return renderPositionVertexCount.get();
    }

    /**
     * Retourne la table nativeId -> Vertex utilisée par le culling de rendu.
     *
     * @return tableau de sommets indexé par identifiant natif
     */
    public Vertex[] renderVerticesByNativeId() {
        Vertex[] vertices = verticesByNativeId;
        return vertices == null ? new Vertex[0] : vertices;
    }

    private void markRenderDataDirty() {
        renderDataVersion.incrementAndGet();
    }

    private void markRenderPositionsDirty() {
        renderPositionVersion.incrementAndGet();
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
        positionSlotBytes = new ByteBuffer[0];
        positionSlotFloats = new FloatBuffer[0];
        latestReadyPositionSlot.set(-1);
        nextSimulationWriteSlot = 0;
        renderPositionVertexCount.set(0);
        markRenderPositionsDirty();
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
        publishInitialRenderPositions(nativeBuffers.positionsAsFloatBuffer(), result.vertexCount());

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

    /**
     * Exécute une itération de simulation sans bloquer le renderer.
     *
     * <p>Le moteur natif écrit directement dans un slot de positions direct choisi
     * par le thread simulation. Une fois l'appel JNI terminé, le slot est publié
     * atomiquement. Le renderer consomme uniquement le dernier slot prêt et peut
     * ignorer les versions intermédiaires si l'écran n'a pas le temps de les
     * afficher.</p>
     */
    private void simulateStep() {
        if (!simulationRunning || !graphLoaded) {
            return;
        }
        ByteBuffer target = acquirePositionWriteSlot();
        if (target == null) {
            return;
        }
        int writtenSlot = nextSimulationWriteSlot;
        if (nativeEngine.updatePositions(target)) {
            publishPositionSlot(writtenSlot);
            maybeSyncJavaVerticesFromSlot(writtenSlot);
        }
    }

    /**
     * Retourne un slot direct non publié pour l'appel JNI {@code updatePositions}.
     *
     * @return buffer direct réutilisable ou {@code null} si aucun graphe n'est prêt
     */
    private ByteBuffer acquirePositionWriteSlot() {
        ByteBuffer[] byteSlots = positionSlotBytes;
        if (byteSlots.length == 0) {
            return null;
        }

        int latest = latestReadyPositionSlot.get();
        int candidate = nextSimulationWriteSlot;
        for (int i = 0; i < byteSlots.length; i++) {
            if (candidate != latest && byteSlots[candidate] != null) {
                nextSimulationWriteSlot = candidate;
                ByteBuffer target = byteSlots[candidate].duplicate();
                target.order(ByteOrder.nativeOrder());
                target.clear();
                target.limit(Math.min(target.capacity(), Math.max(0, renderPositionVertexCount.get()) * 2 * Float.BYTES));
                return target;
            }
            candidate = (candidate + 1) % byteSlots.length;
        }

        // Avec trois slots cette branche ne devrait pratiquement pas arriver.
        // On préfère sauter une frame de simulation plutôt que réécrire le buffer
        // que le thread OpenGL est peut-être en train d'uploader.
        return null;
    }

    /**
     * Publie le slot écrit par le moteur natif.
     *
     * @param slot index du slot entièrement rempli
     */
    private void publishPositionSlot(int slot) {
        FloatBuffer[] floatSlots = positionSlotFloats;
        if (slot < 0 || slot >= floatSlots.length || floatSlots[slot] == null) {
            return;
        }
        latestReadyPositionSlot.set(slot);
        nextSimulationWriteSlot = (slot + 1) % Math.max(1, floatSlots.length);
        markRenderPositionsDirty();
    }

    /** Initialise le triple buffer de rendu à partir des positions chargées. */
    private void publishInitialRenderPositions(FloatBuffer positions, int vertexCount) {
        ensurePositionSlots(vertexCount);
        int count = Math.max(0, vertexCount);
        renderPositionVertexCount.set(count);

        if (positions != null && count > 0 && positionSlotFloats.length > 0) {
            FloatBuffer src = positions.duplicate();
            src.position(0);
            src.limit(Math.min(src.capacity(), count * 2));

            FloatBuffer dst = positionSlotFloats[0].duplicate();
            dst.clear();
            dst.put(src);
            dst.flip();

            latestReadyPositionSlot.set(0);
            nextSimulationWriteSlot = 1 % POSITION_SLOT_COUNT;
        } else {
            latestReadyPositionSlot.set(-1);
            nextSimulationWriteSlot = 0;
        }
        markRenderPositionsDirty();
    }

    /**
     * Garantit l'existence de trois buffers directs assez grands pour les positions.
     *
     * @param vertexCount nombre de sommets du graphe
     */
    private void ensurePositionSlots(int vertexCount) {
        int floats = Math.max(2, vertexCount * 2);
        int bytes = floats * Float.BYTES;
        ByteBuffer[] oldBytes = positionSlotBytes;
        if (oldBytes.length == POSITION_SLOT_COUNT) {
            boolean enough = true;
            for (ByteBuffer buffer : oldBytes) {
                enough &= buffer != null && buffer.capacity() >= bytes;
            }
            if (enough) {
                return;
            }
        }

        ByteBuffer[] byteSlots = new ByteBuffer[POSITION_SLOT_COUNT];
        FloatBuffer[] floatSlots = new FloatBuffer[POSITION_SLOT_COUNT];
        for (int i = 0; i < POSITION_SLOT_COUNT; i++) {
            ByteBuffer buffer = ByteBuffer.allocateDirect(bytes).order(ByteOrder.nativeOrder());
            byteSlots[i] = buffer;
            floatSlots[i] = buffer.asFloatBuffer();
        }
        positionSlotBytes = byteSlots;
        positionSlotFloats = floatSlots;
        latestReadyPositionSlot.set(-1);
        nextSimulationWriteSlot = 0;
    }

    /** Synchronise ponctuellement les objets Java pour les interactions et les stats. */
    private void maybeSyncJavaVerticesFromSlot(int slot) {
        long now = System.nanoTime();
        if (now - lastJavaVertexSyncNanos < JAVA_VERTEX_SYNC_INTERVAL_NANOS) {
            return;
        }
        lastJavaVertexSyncNanos = now;

        Vertex[] vertices = verticesByNativeId;
        FloatBuffer[] slots = positionSlotFloats;
        if (vertices == null || slot < 0 || slot >= slots.length || slots[slot] == null) {
            return;
        }
        syncJavaVerticesFromPositions(slots[slot], vertices, Math.min(vertices.length, renderPositionVertexCount.get()));
    }

    /** Synchronise ponctuellement les objets Java pour les interactions et les stats. */
    private void syncJavaVerticesFromPositions(FloatBuffer positions, Vertex[] vertices, int count) {
        FloatBuffer src = positions.duplicate();
        src.position(0);
        for (int i = 0; i < count; i++) {
            Vertex v = vertices[i];
            if (v == null) {
                continue;
            }
            v.updatePosition(src.get(i * 2), src.get(i * 2 + 1));
        }
    }

    public boolean isSimulationRunning() {
        return simulationRunning;
    }

    /** Retourne la fréquence d'exécution de la routine de simulation. */
    public int getSimulationTicksPerSecond() {
        return simulationTicksPerSecond;
    }

    public Vertex findVertexAt(int screenX, int screenY) {
        float worldX = camera.screenToWorldX(screenX);
        float worldY = camera.screenToWorldY(screenY);
        FloatBuffer positions = renderPositionsBuffer();
        Vertex[] vertices = renderVerticesByNativeId();
        int count = Math.min(renderPositionVertexCount.get(), vertices.length);

        if (positions != null) {
            Vertex best = null;
            double bestDist2 = Double.MAX_VALUE;
            for (int i = 0; i < count; i++) {
                Vertex v = vertices[i];
                if (v == null || v.isDeleted() || !v.isVisible()) {
                    continue;
                }
                double dx = worldX - positions.get(i * 2);
                double dy = worldY - positions.get(i * 2 + 1);
                double r = Math.max(5.0, v.getDiameter() * 0.65) / Math.max(camera.getZoom(), 1.0e-6f);
                double dist2 = dx * dx + dy * dy;
                if (dist2 <= r * r && dist2 < bestDist2) {
                    best = v;
                    bestDist2 = dist2;
                }
            }
            return best;
        }

        return model.findVertexAt(screenX, screenY, camera);
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
        // scheduleWithFixedDelay évite l'effet de rattrapage : si une itération C
        // prend plus que la période cible, on ne lance pas immédiatement une rafale
        // de ticks en retard. Cela réduit la pression sur le CPU et sur les uploads
        // de positions quand le graphe devient très grand.
        simulationTask = simulationExecutor.scheduleWithFixedDelay(this::simulateStep, 0, periodNanos,
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
            updatePublishedSinglePosition(index, (float) x, (float) y);
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

    /**
     * Met à jour la position publiée lors d'un déplacement manuel de sommet.
     *
     * <p>Le déplacement manuel est rare par rapport aux ticks de simulation. On
     * écrit donc la nouvelle position dans tous les slots afin que le renderer ne
     * puisse pas réafficher une ancienne coordonnée si le slot courant change juste
     * après l'interaction.</p>
     */
    private void updatePublishedSinglePosition(int index, float x, float y) {
        if (index < 0) {
            return;
        }
        FloatBuffer[] slots = positionSlotFloats;
        int offset = index * 2;
        boolean updated = false;
        for (FloatBuffer slot : slots) {
            if (slot == null || offset + 1 >= slot.capacity()) {
                continue;
            }
            slot.put(offset, x);
            slot.put(offset + 1, y);
            updated = true;
        }
        if (updated) {
            markRenderPositionsDirty();
        }
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
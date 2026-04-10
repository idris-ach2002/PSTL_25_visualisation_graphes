package com.mongraphe.graphui.rendering;

import java.nio.ByteBuffer;
import java.nio.FloatBuffer;
import java.util.ArrayList;
import java.util.HashMap;
import java.util.List;
import java.util.Map;
import java.util.concurrent.Executors;
import java.util.concurrent.ScheduledExecutorService;
import java.util.concurrent.TimeUnit;

import com.mongraphe.graphui.model.*;

public final class GraphEngine {

    private final GraphNativeEngine nativeEngine;
    private final Camera2D camera;
    private final GraphModel model;
    private final GraphVisibilityFilter visibility;

    private volatile float clearR = 1f, clearG = 1f, clearB = 1f, clearA = 1f;
    private int clusterUpdateFrequency = 1;
    private boolean initialized = false;

    private final ScheduledExecutorService simulationExecutor = Executors.newSingleThreadScheduledExecutor(r -> {
        Thread t = new Thread(r, "graph-simulation-thread");
        t.setDaemon(true);
        return t;
    });
    private volatile boolean simulationRunning = false;
    private volatile boolean graphLoaded = false;

    public interface GraphEngineListener {
        void onSimulationStarted();

        void onSimulationStopped();
    }

    private final List<GraphEngineListener> listeners = new ArrayList<>();

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

    public record StatsSnapshot(
            int visibleVertices, int hiddenVertices, int deletedVertices,
            int visibleEdges, int hiddenEdges, int totalVertices, int totalEdges,
            Metadata initMetadata, int selectedVertexId, double selectedX, double selectedY) {
    }

    public StatsSnapshot getStatsSnapshot() {
        model.lock().readLock().lock();
        try {
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
        } finally {
            model.lock().readLock().unlock();
        }
    }

    public static class GraphDataSnapshot {
        private final List<Vertex> vertices;
        private final List<Edge> edges;
        private final Map<Integer, Vertex> verticesById;
        private final int visibleVertexCount;
        private final int visibleEdgeCount;

        public GraphDataSnapshot(List<Vertex> vertices, List<Edge> edges, int visibleVertexCount,
                int visibleEdgeCount) {
            this.vertices = new ArrayList<>(vertices);
            this.edges = new ArrayList<>(edges);
            this.visibleVertexCount = visibleVertexCount;
            this.visibleEdgeCount = visibleEdgeCount;
            this.verticesById = new HashMap<>();
            for (Vertex v : vertices)
                verticesById.put(v.getId(), v);
        }

        public List<Vertex> getVertices() {
            return vertices;
        }

        public List<Edge> getEdges() {
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

    public GraphPage<Vertex> getVerticesPage(int page, int pageSize) {

        model.lock().readLock().lock();
        try {
            int total = model().vertices().size();
            int from = page * pageSize;
            int to = Math.min(from + pageSize, total);

            return new GraphPage<>(model().vertices().subList(from, to), total);
        } finally {
            model.lock().readLock().unlock();
        }
    }

    public GraphPage<Edge> getEdgesPage(int page, int pageSize) {
        model.lock().readLock().lock();
        try {
            List<Edge> all = model.edges();
            int from = page * pageSize;
            int to = Math.min(from + pageSize, all.size());
            if (from >= all.size())
                return new GraphPage<>(List.of(), all.size());
            return new GraphPage<>(all.subList(from, to), all.size());
        } finally {
            model.lock().readLock().unlock();
        }
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
        model.lock().readLock().lock();
        try {
            return new GraphDataSnapshot(
                    new ArrayList<>(model.vertices()),
                    model.edges(),
                    model.getVisibleVertexCount(),
                    model.getVisibleEdgeCount());
        } finally {
            model.lock().readLock().unlock();
        }
    }

    public boolean load(String path, GraphProject.SourceType type, GraphData.SimilitudeMode sim,
            GraphData.NodeCommunity communityMode) {
        switch (type) {
            case CSV -> {
                loadCsv(path, sim, communityMode);
                initialized = true;
                return true;
            }
            case DOT -> {
                loadDot(path, communityMode);
                initialized = true;
                return true;
            }
            default -> {
                return false;
            }
        }
    }

    public void loadCsv(String path, GraphData.SimilitudeMode sim, GraphData.NodeCommunity communityMode) {
        if (path == null || path.isBlank())
            throw new IllegalArgumentException("CSV path missing");
        graphLoaded = false;
        nativeEngine.initGraphCsv(path, sim, communityMode);
        rebuildModelFromNative();
        notifyDataChanged();
        graphLoaded = true;
    }

    public void loadDot(String path, GraphData.NodeCommunity communityMode) {
        if (path == null || path.isBlank())
            throw new IllegalArgumentException("DOT path missing");
        graphLoaded = false;
        nativeEngine.initGraphDot(path, communityMode);
        rebuildModelFromNative();
        graphLoaded = true;
    }

    private void rebuildModelFromNative() {
        Vertex[] verticesArray = nativeEngine.getPositions();
        EdgeC[] edgesArray = nativeEngine.getEdges();
        int[] communityIds = nativeEngine.getCommunities();
        float[][] colors = nativeEngine.getClusterColors();

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

        model.lock().writeLock().lock();
        try {
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
        } finally {
            model.lock().writeLock().unlock();
        }

        nativeEngine.initSharedPositionsBuffer(model.vertexCount());
        visibility.apply(model);
    }

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

    public void stopSimulation() {
        simulationRunning = false;
        synchronized (listeners) {
            for (GraphEngineListener listener : listeners)
                listener.onSimulationStopped();
        }
    }

    private void simulateStep() {
        if (!simulationRunning || !graphLoaded)
            return;
        ByteBuffer buf = nativeEngine.sharedPositionsBuffer;
        if (buf == null)
            return; // déjà libéré
        if (nativeEngine.updatePositions(buf)) {
            updateVerticesFromBuffer();
        }
    }

    private void updateVerticesFromBuffer() {
        ByteBuffer buf = nativeEngine.sharedPositionsBuffer;
        if (buf == null)
            return;
        FloatBuffer fb = buf.asFloatBuffer();
        fb.rewind();
        model.lock().readLock().lock();
        try {
            int numVertices = model.vertices().size();
            for (int i = 0; i < numVertices; i++) {
                float x = fb.get();
                float y = fb.get();
                Vertex v = model.vertexById(i);
                if (v != null) {
                    v.updatePosition(x, y);
                }
            }
        } finally {
            model.lock().readLock().unlock();
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
        model.lock().writeLock().lock();
        try {
            model.setColoringMode(mode);
        } finally {
            model.lock().writeLock().unlock();
        }
        notifyDataChanged();
    }

    public void setUniformNodeColor(float r, float g, float b) {
        model.lock().writeLock().lock();
        try {
            model.setUniformNodeColor(r, g, b);
        } finally {
            model.lock().writeLock().unlock();
        }
        notifyDataChanged();
    }

    public void setMinimumDegree(int degree) {
        model.lock().writeLock().lock();
        try {
            visibility.setMinimumDegree(degree);
            visibility.apply(model);
            model.setFilterMinDegree(degree);
        } finally {
            model.lock().writeLock().unlock();
        }
        notifyDataChanged();
    }

    public void setMinimumEdgeWeight(double weight) {
        model.lock().writeLock().lock();
        try {
            model.setFilterMinEdgeWeight(weight);
            visibility.setEdgeWeightThreshold((float) weight);
            visibility.apply(model);
        } finally {
            model.lock().writeLock().unlock();
        }
        notifyDataChanged();
    }

    public void setInitialNodeSize(double size) {
        Vertex.initial_node_size = size;
        nativeEngine.setInitialNodeSize(size);
        model.lock().writeLock().lock();
        try {
            for (Vertex v : model.vertices())
                v.updateDiameter();
        } finally {
            model.lock().writeLock().unlock();
        }
    }

    public void setDegreeScaleFactor(double factor) {
        Vertex.degree_scale_factor = factor;
        nativeEngine.setDegreeScaleFactor(factor);

        model.lock().writeLock().lock();
        try {
            for (Vertex v : model.vertices())
                v.updateDiameter();
        } finally {
            model.lock().writeLock().unlock();
        }
        notifyDataChanged();
    }

    public void setStabilizedThreshold(double t) {
        nativeEngine.setThresholdS(t);
    }

    public void setAttractionThreshold(double t) {
        nativeEngine.setThresholdA(t);
    }

    public void setNewFriction(double f) {
        nativeEngine.setFriction(f);
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

    public void setNbClusters(int n) {
        nativeEngine.SetNumberClusters(n);
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

    public double[] getDimensions(){
        return nativeEngine.getDimensions();
    }

    public void setNodePosition(int index, double x, double y) {
        nativeEngine.setNodePosition(index, x, y);
        model.lock().writeLock().lock();
        try {
            Vertex v = model.vertexById(index);
            if (v != null) {
                v.updatePosition(x, y);
                notifyDataChanged();
            }
        } finally {
            model.lock().writeLock().unlock();
        }
    }

    public void deleteNode(int index) {
        nativeEngine.deleteNode(index);
        model.lock().writeLock().lock();
        try {
            Vertex v = model.vertexById(index);
            if (v != null)
                model.deleteVertex(v);
        } finally {
            model.lock().writeLock().unlock();
        }
        notifyDataChanged();
    }

    public void restoreNode(int index) {
        nativeEngine.restoreNode(index);
        rebuildModelFromNative();
        notifyDataChanged();
    }

    public void setClusterUpdateFrequency(int saut) {
        clusterUpdateFrequency = Math.max(1, saut);
        nativeEngine.setSaut(clusterUpdateFrequency);
    }

    public int getClusterUpdateFrequency() {
        return clusterUpdateFrequency;
    }

    public void dispose() {
        // Arrêter la simulation
        simulationRunning = false;
        graphLoaded = false;

        // Arrêter l'exécuteur et attendre la fin des tâches en cours
        simulationExecutor.shutdown();
        try {
            if (!simulationExecutor.awaitTermination(1, TimeUnit.SECONDS)) {
                simulationExecutor.shutdownNow();
            }
        } catch (InterruptedException e) {
            simulationExecutor.shutdownNow();
            Thread.currentThread().interrupt();
        }

        // Libérer la mémoire native
        nativeEngine.freeAllocatedMemory();

        // Vider le modèle Java
        model.lock().writeLock().lock();
        try {
            model.clear();
        } finally {
            model.lock().writeLock().unlock();
        }
    }

    private float clamp01(float v) {
        return v < 0f ? 0f : (v > 1f ? 1f : v);
    }
}
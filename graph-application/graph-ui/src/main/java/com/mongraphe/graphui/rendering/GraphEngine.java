package com.mongraphe.graphui.rendering;

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

    // Double‑buffering des positions
    private float[] posBuffer0;
    private float[] posBuffer1;
    private volatile float[] currentPositions; // lu par le rendu

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
        synchronized (model.mutex()) {
            List<Vertex> all = model.vertices();
            int from = page * pageSize;
            int to = Math.min(from + pageSize, all.size());
            if (from >= all.size())
                return new GraphPage<>(List.of(), all.size());
            return new GraphPage<>(all.subList(from, to), all.size());
        }
    }

    public GraphPage<Edge> getEdgesPage(int page, int pageSize) {
        synchronized (model.mutex()) {
            List<Edge> all = model.edges();
            int from = page * pageSize;
            int to = Math.min(from + pageSize, all.size());
            if (from >= all.size())
                return new GraphPage<>(List.of(), all.size());
            return new GraphPage<>(all.subList(from, to), all.size());
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
        synchronized (model.mutex()) {
            return new GraphDataSnapshot(model.vertices(), model.edges(), model.getVisibleVertexCount(),
                    model.getVisibleEdgeCount());
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
            throw new IllegalStateException("Le moteur natif n'a pas renvoyé de structure de graphe exploitable.");
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

        synchronized (model.mutex()) {
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
                if (ec.getStart() < 0 || ec.getStart() >= model.vertices().size())
                    continue;
                if (ec.getEnd() < 0 || ec.getEnd() >= model.vertices().size())
                    continue;
                Vertex start = model.vertices().get(ec.getStart());
                Vertex end = model.vertices().get(ec.getEnd());
                model.addEdge(new Edge(start, end, ec.getWeight()));
            }
        }
        visibility.apply(model);

        // Initialiser les buffers de positions
        int vertexCount = verticesArray.length;
        posBuffer0 = new float[vertexCount * 2];
        posBuffer1 = new float[vertexCount * 2];
        for (int i = 0; i < vertexCount; i++) {
            if (verticesArray[i] == null)
                continue;
            float x = (float) verticesArray[i].getX();
            float y = (float) verticesArray[i].getY();
            posBuffer0[i * 2] = x;
            posBuffer0[i * 2 + 1] = y;
            posBuffer1[i * 2] = x;
            posBuffer1[i * 2 + 1] = y;
        }
        currentPositions = posBuffer0;
    }

    public void startSimulation() {
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
        try {
            nativeEngine.updatePositions();
            Vertex[] positions = nativeEngine.getPositions();
            if (positions == null || positions.length == 0)
                return;

            float[] backBuffer = (currentPositions == posBuffer0) ? posBuffer1 : posBuffer0;
            int vertexCount = positions.length;
            for (int i = 0; i < vertexCount; i++) {
                if (positions[i] == null)
                    continue;
                backBuffer[i * 2] = (float) positions[i].getX();
                backBuffer[i * 2 + 1] = (float) positions[i].getY();
            }
            currentPositions = backBuffer;

            // Mise à jour du modèle (visibilité)
            synchronized (model.mutex()) {
                visibility.apply(model);
            }
        } catch (Exception e) {
            // log éventuel
        }
    }

    public boolean isSimulationRunning() {
        return simulationRunning;
    }

    public float[] getPositionsBuffer() {
        return currentPositions;
    }

    public float[] getVertexPosition(int id) {
        float[] buf = currentPositions;
        if (buf == null || id < 0 || id * 2 + 1 >= buf.length)
            return new float[] { 0f, 0f };
        return new float[] { buf[id * 2], buf[id * 2 + 1] };
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
        synchronized (model.mutex()) {
            model.setColoringMode(mode);
        }
    }

    public void setUniformNodeColor(float r, float g, float b) {
        synchronized (model.mutex()) {
            model.setUniformNodeColor(r, g, b);
        }
    }

    public void setMinimumDegree(int degree) {
        synchronized (model.mutex()) {
            model.setFilterMinDegree(degree);
        }
        visibility.setMinimumDegree(degree);
        visibility.apply(model);
    }

    public void setMinimumEdgeWeight(double weight) {
        synchronized (model.mutex()) {
            model.setFilterMinEdgeWeight(weight);
        }
        visibility.setEdgeWeightThreshold((float) weight);
        visibility.apply(model);
    }

    public void setInitialNodeSize(double size) {
        Vertex.initial_node_size = size;
        nativeEngine.setInitialNodeSize(size);
        synchronized (model.mutex()) {
            for (Vertex v : model.vertices())
                v.updateDiameter();
        }
    }

    public void setDegreeScaleFactor(double factor) {
        Vertex.degree_scale_factor = factor;
        nativeEngine.setDegreeScaleFactor(factor);
        synchronized (model.mutex()) {
            for (Vertex v : model.vertices())
                v.updateDiameter();
        }
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

    public void enableKmeans(boolean enabled) {
        nativeEngine.setKmeansMode(enabled);
    }

    public void setDimensions(double width, double height) {
        nativeEngine.setDimension(width, height);
    }

    public void setNodePosition(int index, double x, double y) {
        nativeEngine.setNodePosition(index, x, y);
        float[] cur = currentPositions;
        if (cur != null && index * 2 + 1 < cur.length) {
            cur[index * 2] = (float) x;
            cur[index * 2 + 1] = (float) y;
        }
        // Optionnel mettre à jour aussi le Vertex
    }

    public void deleteNode(int index) {
        nativeEngine.deleteNode(index);
        synchronized (model.mutex()) {
            Vertex v = model.vertexById(index);
            if (v != null)
                model.deleteVertex(v);
        }
    }

    public void restoreNode(int index) {
        nativeEngine.restoreNode(index);
        rebuildModelFromNative();
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
        synchronized (model.mutex()) {
            model.clear();
        }

        currentPositions = null;
    }

    private float clamp01(float v) {
        return v < 0f ? 0f : (v > 1f ? 1f : v);
    }
}
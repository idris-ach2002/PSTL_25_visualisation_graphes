package com.mongraphe.graphui.rendering;

import java.util.ArrayList;
import java.util.HashMap;
import java.util.List;
import java.util.Map;

import com.mongraphe.graphui.model.Community;
import com.mongraphe.graphui.model.Edge;
import com.mongraphe.graphui.model.EdgeC;
import com.mongraphe.graphui.model.GraphData;
import com.mongraphe.graphui.model.GraphModel;
import com.mongraphe.graphui.model.GraphProject;
import com.mongraphe.graphui.model.Metadata;
import com.mongraphe.graphui.model.Vertex;

public final class GraphEngine {

    private final GraphNativeEngine nativeEngine;
    private final Camera2D camera;
    private final GraphModel model;
    private final GraphSimulation simulation;
    private final GraphVisibilityFilter visibility;

    private volatile float clearR = 1f, clearG = 1f, clearB = 1f, clearA = 1f;
    private int clusterUpdateFrequency = 1;

    public interface GraphEngineListener {
        void onSimulationStarted();
        void onSimulationStopped();
    }

    private final List<GraphEngineListener> listeners = new ArrayList<>();

    public static class GraphDataSnapshot {
        private final List<Vertex> vertices = new ArrayList<>();
        private final List<Edge> edges = new ArrayList<>();
        private final Map<Integer, Vertex> verticesById = new HashMap<>();
        private final int visibleVertexCount;
        private final int visibleEdgeCount;

        public GraphDataSnapshot(List<Vertex> vertices, List<Edge> edges, int visibleVertexCount, int visibleEdgeCount) {
            this.vertices.addAll(vertices);
            this.edges.addAll(edges);
            this.visibleVertexCount = visibleVertexCount;
            this.visibleEdgeCount = visibleEdgeCount;
            for (Vertex v : vertices) {
                verticesById.put(v.getId(), v);
            }
        }

        public List<Vertex> getVertices() { return vertices; }
        public List<Edge> getEdges() { return edges; }
        public int getVisibleVertexCount() { return visibleVertexCount; }
        public int getVisibleEdgeCount() { return visibleEdgeCount; }
        public Vertex getVertexById(int id) { return verticesById.get(id); }
    }

    public GraphEngine(GraphNativeEngine nativeEngine) {
        this.nativeEngine = nativeEngine;
        this.camera = new Camera2D();
        this.model = new GraphModel();
        this.simulation = new GraphSimulation(nativeEngine);
        this.visibility = new GraphVisibilityFilter();
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
            return new GraphDataSnapshot(model.vertices(), model.edges(), model.getVisibleVertexCount(), model.getVisibleEdgeCount());
        }
    }

    public boolean load(String path, GraphProject.SourceType type, GraphData.SimilitudeMode sim, GraphData.NodeCommunity communityMode) {
        switch (type) {
            case CSV -> { loadCsv(path, sim, communityMode); return true; }
            case DOT -> { loadDot(path, communityMode); return true; }
            default -> { return false; }
        }
    }

    public void loadCsv(String path, GraphData.SimilitudeMode sim, GraphData.NodeCommunity communityMode) {
        if (path == null || path.isBlank()) {
            throw new IllegalArgumentException("CSV path missing");
        }
        nativeEngine.initGraphCsv(path, sim, communityMode);
        rebuildModelFromNative();
    }

    public void loadDot(String path, GraphData.NodeCommunity communityMode) {
        if (path == null || path.isBlank()) {
            throw new IllegalArgumentException("DOT path missing");
        }
        nativeEngine.initGraphDot(path, communityMode);
        rebuildModelFromNative();
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
            if (verticesArray[i] != null) {
                verticesArray[i].setId(i);
            }
        }

        HashMap<Integer, Community> communities = new HashMap<>();
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
            if (verticesArray[i] != null) {
                verticesArray[i].setCommunity(c);
            }
        }

        synchronized (model.mutex()) {
            model.clear();
            for (Vertex v : verticesArray) {
                if (v == null) {
                    continue;
                }
                v.updateDiameter();
                model.addVertex(v);
            }
            for (EdgeC ec : edgesArray) {
                if (ec == null) continue;
                if (ec.getStart() < 0 || ec.getStart() >= model.vertices().size()) continue;
                if (ec.getEnd() < 0 || ec.getEnd() >= model.vertices().size()) continue;
                Vertex start = model.vertices().get(ec.getStart());
                Vertex end = model.vertices().get(ec.getEnd());
                model.addEdge(new Edge(start, end, ec.getWeight()));
            }
        }
        visibility.apply(model);
    }

    public void update() {
        simulation.update(model);
        visibility.apply(model);
    }

    public GraphModel model() { return model; }
    public Camera2D camera() { return camera; }
    public GraphVisibilityFilter visibility() { return visibility; }
    public Metadata getMetadata() { return nativeEngine.getMetadata(); }
    public Metadata getInitMetadata() { return nativeEngine.getInitMetadata(); }

    public void setBackgroundColor(float r, float g, float b, float a) {
        clearR = clamp01(r);
        clearG = clamp01(g);
        clearB = clamp01(b);
        clearA = clamp01(a);
    }

    public float getBackgroundColorR() { return clearR; }
    public float getBackgroundColorG() { return clearG; }
    public float getBackgroundColorB() { return clearB; }
    public float getBackgroundColorA() { return clearA; }

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
            for (Vertex v : model.vertices()) {
                v.updateDiameter();
            }
        }
    }

    public void setDegreeScaleFactor(double factor) {
        Vertex.degree_scale_factor = factor;
        nativeEngine.setDegreeScaleFactor(factor);
        synchronized (model.mutex()) {
            for (Vertex v : model.vertices()) {
                v.updateDiameter();
            }
        }
    }

    public void setUpscale(int up) { Vertex.upscale = up; }
    public void setStabilizedThreshold(double t) { nativeEngine.setThresholdS(t); }
    public void setAttractionThreshold(double t) { nativeEngine.setThresholdA(t); }
    public void setNewFriction(double f) { nativeEngine.setFriction(f); }
    public void setAttractionCoefficient(double c) { nativeEngine.setAttractionCoeff(c); }
    public void setRepulsionThreshold(double t) { nativeEngine.setSeuilRep(t); }
    public void setAntiRepulsion(double t) { nativeEngine.setAntiRepulsion(t); }
    public void setNewAmortissement(double a) { nativeEngine.setAmortissement(a); }
    public void setNbClusters(int n) { nativeEngine.SetNumberClusters(n); }
    public void setRepulsionMode(GraphData.RepulsionMode mode) { if (mode != null) nativeEngine.setModeRepulsion(mode.ordinal()); }
    public void enableKmeans(boolean enabled) { nativeEngine.setKmeansMode(enabled); }
    public void setDimensions(double width, double height) { nativeEngine.setDimension(width, height); }

    public void setNodePosition(int index, double x, double y) {
        nativeEngine.setNodePosition(index, x, y);
        synchronized (model.mutex()) {
            Vertex v = model.vertexById(index);
            if (v != null) {
                v.updatePosition(x, y);
            }
        }
    }

    public void deleteNode(int index) {
        nativeEngine.deleteNode(index);
        synchronized (model.mutex()) {
            Vertex v = model.vertexById(index);
            if (v != null) {
                model.deleteVertex(v);
            }
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

    public int getClusterUpdateFrequency() { return clusterUpdateFrequency; }

    public void startSimulation() {
        simulation.start();
        synchronized (listeners) {
            for (GraphEngineListener listener : listeners) {
                listener.onSimulationStarted();
            }
        }
    }

    public void stopSimulation() {
        simulation.stop();
        synchronized (listeners) {
            for (GraphEngineListener listener : listeners) {
                listener.onSimulationStopped();
            }
        }
    }

    public boolean isSimulationRunning() { return simulation.isRunning(); }

    public void freeNativeMemory() {
        nativeEngine.freeAllocatedMemory();
        synchronized (model.mutex()) {
            model.clear();
        }
    }

    private float clamp01(float v) {
        if (v < 0f) return 0f;
        if (v > 1f) return 1f;
        return v;
    }
}

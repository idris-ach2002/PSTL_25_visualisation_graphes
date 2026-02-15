package com.mongraphe.graphui.app;

import com.mongraphe.graphui.Vertex;
import com.mongraphe.graphui.Edge;
import com.mongraphe.graphui.GraphData;
import com.mongraphe.graphui.interfaces.GraphImageExporter;
import com.mongraphe.graphui.interfaces.InteractionModel;
import com.mongraphe.graphui.model.GraphModel;
import com.mongraphe.graphui.rendering.Camera2D;
import com.mongraphe.graphui.rendering.GraphEngine;

import java.io.File;
import java.util.ArrayList;
import java.util.List;

import com.jogamp.newt.opengl.GLWindow;
import com.jogamp.opengl.GL2;
import com.jogamp.opengl.GLAutoDrawable;

public final class GraphEngineAdapter implements InteractionModel {

    private final GraphEngine engine;
    private final Camera2D camera;
    private final GraphImageExporter exporter;

    private final GLWindow window;

    public GraphEngineAdapter(GraphEngine engine,
            Camera2D camera,
            GraphImageExporter exporter, GLWindow window) {
        this.engine = engine;
        this.camera = camera;
        this.exporter = exporter;
        this.window = window;
    }

    public static final class GraphDataSnapshot {
        private final List<Vertex> vertices;
        private final List<Edge> edges;
        private int visibleVertexCount;
        private int visibleEdgeCount;

        public GraphDataSnapshot(List<Vertex> vertices, List<Edge> edges, int visibleVertexCount,
                int visibleEdgeCount) {
            this.vertices = vertices;
            this.edges = edges;
            this.visibleVertexCount = visibleVertexCount;
            this.visibleEdgeCount = visibleEdgeCount;
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
    }

    private void invokeOnGlThread(Runnable task) {
        if (window == null)
            return;
        window.invoke(false, drawable -> {
            task.run();
            return true;
        });
    }

    public GraphDataSnapshot getDataSnapshot() {
        GraphModel model = engine.model();
        synchronized (model.mutex()) {
            List<Vertex> verticesCopy = new ArrayList<>(model.vertices());
            List<Edge> edgesCopy = new ArrayList<>(model.edges());
            int visibleVertexCount = model.getVisibleVertexCount();
            int visibleEdgeCount = model.getVisibleEdgeCount();
            return new GraphDataSnapshot(verticesCopy, edgesCopy, visibleVertexCount, visibleEdgeCount);
        }
    }

    public void exportPng(File file, int width, int height) {
        if (exporter != null)
            exporter.exportPng(file, width, height);
    }

    @Override
    public Vertex findVertexAt(float wx, float wy) {
        return engine.model().findVertexAt(wx, wy);
    }

    @Override
    public void setSelectedVertex(Vertex v) {
        engine.model().setSelectedVertexId(v != null ? v.getId() : -1);
    }

    public void setNodeDiameter(int vertexId, double diameter) {
        invokeOnGlThread(() -> engine.setNodeDiameter(vertexId, diameter));
    }

    @Override
    public void moveVertex(int vertexId, float x, float y) {
        invokeOnGlThread(() -> {
            engine.setNodePosition(vertexId, x, y);
            Vertex v = engine.model().vertexById(vertexId);
            if (v != null)
                v.updatePosition(x, y);
        });
    }

    public void setNodePosition(int vertexId, float x, float y) {
        invokeOnGlThread(() -> engine.setNodePosition(vertexId, x, y));
    }

    public void resizeCamera(int width, int height) {
        invokeOnGlThread(() -> camera.resize(width, height));
    }

    @Override
    public void panCamera(int dx, int dy) {
        camera.pan(dx, dy);
    }

    @Override
    public void zoomCamera(int sx, int sy, float rotation) {
        camera.zoomAt(sx, sy, rotation);
    }

    @Override
    public boolean isSimulationRunning() {
        return engine.isSimulationRunning();
    }

    @Override
    public void startSimulation() {
        invokeOnGlThread(engine::startSimulation);
    }

    @Override
    public void stopSimulation() {
        invokeOnGlThread(engine::stopSimulation);
    }

    public void deleteNode(int index) {
        invokeOnGlThread(() -> engine.deleteNode(index));
    }

    public void restoreNode(int index) {
        invokeOnGlThread(() -> engine.restoreNode(index));
    }

    public void setRepulsionMode(GraphData.RepulsionMode mode) {
        invokeOnGlThread(() -> engine.setRepulsionMode(mode));
    }

    // --- Options de simulation / rendu ---
    public void setInitialNodeSize(double size) {
        invokeOnGlThread(() -> engine.setInitialNodeSize(size));
    }

    public void setDegreeScaleFactor(double factor) {
        invokeOnGlThread(() -> engine.setDegreeScaleFactor(factor));
    }

    public void setUpscale(int up) {
        invokeOnGlThread(() -> engine.setUpscale(up));
    }

    public void setStabilizedThreshold(double t) {
        invokeOnGlThread(() -> engine.setStabilizedThreshold(t));
    }

    public void setAttractionThreshold(double t) {
        invokeOnGlThread(() -> engine.setAttractionThreshold(t));
    }

    public void setClusterUpdateFrequency(int freq) {
        invokeOnGlThread(() -> engine.setClusterUpdateFrequency(freq));
    }

    public void setNewFriction(double f) {
        invokeOnGlThread(() -> engine.setNewFriction(f));
    }

    public void setAttractionCoefficient(double c) {
        invokeOnGlThread(() -> engine.setAttractionCoefficient(c));
    }

    public void setRepulsionThreshold(double t) {
        invokeOnGlThread(() -> engine.setRepulsionThreshold(t));
    }

    public void setNewAmortissement(double a) {
        invokeOnGlThread(() -> engine.setNewAmortissement(a));
    }

    public void setNbClusters(int n) {
        invokeOnGlThread(() -> engine.setNbClusters(n));
    }

    public void setMinimumDegree(int min) {
        invokeOnGlThread(() -> engine.setMinimumDegree(min));
    }

    public void load(java.io.File file, GraphProject.SourceType type, Object sim, Object community) {
        invokeOnGlThread(() -> {
            switch (type) {
                case CSV -> engine.loadCsv(file.getAbsolutePath(), (GraphData.SimilitudeMode) sim,
                        (GraphData.NodeCommunity) community);
                case DOT -> engine.loadDot(file.getAbsolutePath(), (GraphData.NodeCommunity) community);
            }
        });
    }

    public float screenToWorldX(int sx) {
        return camera.screenToWorldX(sx);
    }

    public float screenToWorldY(int sy) {
        return camera.screenToWorldY(sy);
    }

    public void setBackgroundColor(float red, float green, float blue, float alpha) {
        if (window == null)
            return;
        window.invoke(false, drawable -> {
            GLAutoDrawable glDrawable = (GLAutoDrawable) drawable;
            GL2 gl = glDrawable.getGL().getGL2();
            gl.glClearColor(red, green, blue, alpha);
            return true;
        });
    }
}

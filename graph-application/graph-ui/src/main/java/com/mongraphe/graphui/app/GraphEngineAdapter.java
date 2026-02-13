package com.mongraphe.graphui.app;

import com.mongraphe.graphui.Vertex;
import com.mongraphe.graphui.interfaces.InteractionModel;
import com.mongraphe.graphui.rendering.Camera2D;
import com.mongraphe.graphui.rendering.GraphEngine;

public final class GraphEngineAdapter implements InteractionModel {
    private final GraphEngine engine;
    private final Camera2D camera;

    private final Object mutex = new Object();

    public GraphEngineAdapter(GraphEngine engine, Camera2D camera) {
        this.engine = engine;
        this.camera = camera;
    }

    @Override
    public Vertex findVertexAt(float wx, float wy) {
        synchronized (mutex) {
            return engine.model().findVertexAt(wx, wy);
        }
    }

    @Override
    public void setSelectedVertex(Vertex v) {
        synchronized (mutex) {
            engine.model().setSelectedVertexId(v != null ? v.getId() : -1);
        }
    }

    @Override
    public void moveVertex(int vertexId, float x, float y) {
        synchronized (mutex) {
            engine.setNodePosition(vertexId, x, y);
            Vertex v = engine.model().vertexById(vertexId);
            if (v != null)
                v.updatePosition(x, y);
        }
    }

    @Override
    public void panCamera(int dx, int dy) {
        synchronized (mutex) {
            camera.pan(dx, dy);
        }
    }

    @Override
    public void zoomCamera(int sx, int sy, float rotation) {
        synchronized (mutex) {
            camera.zoomAt(sx, sy, rotation);
        }
    }

    @Override
    public boolean isSimulationRunning() {
        synchronized (mutex) {
            return engine.isSimulationRunning();
        }
    }

    @Override
    public void startSimulation() {
        synchronized (mutex) {
            engine.startSimulation();
        }
    }

    @Override
    public void stopSimulation() {
        synchronized (mutex) {
            engine.stopSimulation();
        }
    }

    public float screenToWorldX(int sx) {
        return camera.screenToWorldX(sx);
    }

    public float screenToWorldY(int sy) {
        return camera.screenToWorldY(sy);
    }
}
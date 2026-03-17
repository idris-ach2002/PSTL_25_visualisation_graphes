package com.mongraphe.graphui.rendering;

import com.mongraphe.graphui.model.GraphModel;

public final class GraphSimulation {

    private final GraphNativeEngine nativeEngine;
    private volatile boolean running;

    public GraphSimulation(GraphNativeEngine nativeEngine) {
        this.nativeEngine = nativeEngine;
    }

    public void update(GraphModel model) {
        if (!running) {
            return;
        }

        if (!nativeEngine.updatePositions()) {
            running = false;
        }

        model.updateVertexPositions(nativeEngine.getPositions());
    }

    public void start() { running = true; }
    public void stop() { running = false; }
    public boolean isRunning() { return running; }
}

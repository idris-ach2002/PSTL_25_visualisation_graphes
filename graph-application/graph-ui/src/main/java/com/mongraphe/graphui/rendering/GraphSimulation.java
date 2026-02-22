package com.mongraphe.graphui.rendering;

import com.mongraphe.graphui.model.GraphModel;
public final class GraphSimulation {

    private final GraphNativeEngine nativeEngine;
    private boolean running = true;

    public GraphSimulation(GraphNativeEngine nativeEngine) {
        this.nativeEngine = nativeEngine;
    }

    public void update(GraphModel model) {

        if (!running)
            return;

        nativeEngine.updatePositions();

        model.updateVertexPositions(nativeEngine.getPositions());
    }

    public void start() {
        running = true;
    }

    public boolean isRunning() {
        return running;
    }

    public void stop() {
        running = false;
    }
}
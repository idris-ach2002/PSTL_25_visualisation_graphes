package com.mongraphe.graphui.rendering;

import com.mongraphe.graphui.model.GraphModel;

public final class GraphSimulation {

    private final GraphNativeEngine nativeEngine;
    private volatile boolean running;
    private volatile boolean finishedNaturally;

    private Runnable onFinished;   // callback pour fin naturelle

    public GraphSimulation(GraphNativeEngine nativeEngine) {
        this.nativeEngine = nativeEngine;
    }

    public void setOnFinished(Runnable onFinished) {
        this.onFinished = onFinished;
    }

    public void update(GraphModel model) {
        if (!running) return;

        if (!nativeEngine.updatePositions()) {
            running = false;
            finishedNaturally = true;
            if (onFinished != null) onFinished.run();
        }

        model.updateVertexPositions(nativeEngine.getPositions());
    }

    public void start() {
        running = true;
        finishedNaturally = false;
    }

    public void stop() {
        running = false;
        finishedNaturally = false;
    }

    public boolean isRunning() { return running; }
    public boolean isFinishedNaturally() { return finishedNaturally; }
}
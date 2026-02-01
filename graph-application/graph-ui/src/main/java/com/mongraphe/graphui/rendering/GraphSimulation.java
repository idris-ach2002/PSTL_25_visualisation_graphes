package com.mongraphe.graphui.rendering;

import com.mongraphe.graphui.Vertex;
import java.util.List;

public class GraphSimulation {

    private final GraphNativeEngine nativeGraph;
    private boolean running = true;

    public GraphSimulation(GraphNativeEngine nativeGraph) {
        this.nativeGraph = nativeGraph;
    }

    public void update(GraphScene scene) {

        if (!running)
            return;

        boolean stillMoving = nativeGraph.updatePositions();

        Vertex[] updated = nativeGraph.getPositions();
        List<Vertex> sceneVertices = scene.vertices();

        for (int i = 0; i < sceneVertices.size(); i++) {
            Vertex v = sceneVertices.get(i);
            Vertex nv = updated[i];

            v.updatePosition(nv.getX(), nv.getY());
        }

        if (!stillMoving) {
            running = false;
        }
    }

    public void start() {
        running = true;
    }

    public void stop() {
        running = false;
    }

    public boolean isRunning() {
        return running;
    }
}
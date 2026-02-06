package com.mongraphe.graphui.rendering;

import com.mongraphe.graphui.GraphData;
import com.mongraphe.graphui.model.GraphModel;

public final class GraphEngine {

    private final GraphNativeEngine nativeEngine;
    private final GraphModel model;
    private final GraphSimulation simulation;
    private final GraphVisibilityFilter visibility;

    public GraphEngine(GraphNativeEngine nativeEngine) {
        this.nativeEngine = nativeEngine;
        this.model = new GraphModel();
        this.simulation = new GraphSimulation(nativeEngine);
        this.visibility = new GraphVisibilityFilter();
    }

    public void loadCsv(String path,
            GraphData.SimilitudeMode sim,
            GraphData.NodeCommunity community) {

        if (path == null || path.isBlank())
            throw new IllegalArgumentException("CSV path missing");

        nativeEngine.initGraphCsv(path, sim, community);

        model.buildFromData(
                nativeEngine.getPositions(),
                nativeEngine.getEdges());

        visibility.apply(model);
    }

    public void update() {
        simulation.update(model);
        visibility.apply(model);
    }

    public GraphModel model() {
        return model;
    }

    public GraphVisibilityFilter visibility() {
        return visibility;
    }
}

package com.mongraphe.graphui.interaction.commands;

import com.mongraphe.graphui.controller.EngineOptions;
import com.mongraphe.graphui.interfaces.GraphCommand;
import com.mongraphe.graphui.model.GraphData;
import com.mongraphe.graphui.rendering.GraphEngine;

public class SetEngineOptionsCommand implements GraphCommand<GraphEngine> {
    private final EngineOptions opts;

    public SetEngineOptionsCommand(EngineOptions opts) {
        this.opts = opts;
    }

    @Override
    public void execute(GraphEngine context) {
        boolean wasRunning = context.isSimulationRunning();
        context.stopSimulation();

        if (opts.friction != null)
            context.setNewFriction(opts.friction);
        if (opts.attractionCoefficient != null)
            context.setAttractionCoefficient(opts.attractionCoefficient);
        if (opts.repulsionCoeff != null)
            context.setRepulsionCoeff(opts.repulsionCoeff);
        if (opts.antiRepulsion != null)
            context.setAntiRepulsion(opts.antiRepulsion);
        if (opts.amortissement != null)
            context.setNewAmortissement(opts.amortissement);
        if (opts.repulsionMode != null)
            context.setRepulsionMode(GraphData.RepulsionMode.values()[opts.repulsionMode]);
        if (opts.spatialCells != null)
            context.setSpatialCells(opts.spatialCells);
        if (opts.kmeansEnabled != null)
            context.setKmeansMode(opts.kmeansEnabled);
        if (opts.lambda != null)
            context.setLambda(opts.lambda);
        if (opts.minimumDegree != null)
            context.setMinimumDegree(opts.minimumDegree);
        if (opts.minEdgeWeight != null)
            context.setMinimumEdgeWeight(opts.minEdgeWeight);
        if (opts.initialNodeSize != null)
            context.setInitialNodeSize(opts.initialNodeSize);
        if (opts.degreeFactor != null)
            context.setDegreeScaleFactor(opts.degreeFactor);

        if (wasRunning) {
            context.startSimulation();
        }
    }
}
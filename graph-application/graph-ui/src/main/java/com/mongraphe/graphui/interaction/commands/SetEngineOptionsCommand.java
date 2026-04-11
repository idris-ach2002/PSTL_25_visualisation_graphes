package com.mongraphe.graphui.interaction.commands;

import com.mongraphe.graphui.controller.EngineOptions;
import com.mongraphe.graphui.interfaces.GraphCommand;
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

        if (opts.degreeFactor != null)
            context.setDegreeScaleFactor(opts.degreeFactor);
        if (opts.initialNodeSize != null)
            context.setInitialNodeSize(opts.initialNodeSize);
        if (opts.nbClusters != null)
            context.setNbClusters(opts.nbClusters);
        if (opts.newFriction != null)
            context.setNewFriction(opts.newFriction);
        if (opts.attractionCoefficient != null)
            context.setAttractionCoefficient(opts.attractionCoefficient);
        if (opts.repulsionThreshold != null)
            context.setRepulsionThreshold(opts.repulsionThreshold);
        if (opts.newAmortissement != null)
            context.setNewAmortissement(opts.newAmortissement);
        if (opts.minimumDegree != null)
            context.setMinimumDegree(opts.minimumDegree);
        if (opts.minEdgeWeight != null)
            context.setMinimumEdgeWeight(opts.minEdgeWeight);
        if (opts.kmeansEnabled != null) {
            context.setKmeansMode(opts.kmeansEnabled);
        }

        if (wasRunning) {
            context.startSimulation();
        }
    }
}

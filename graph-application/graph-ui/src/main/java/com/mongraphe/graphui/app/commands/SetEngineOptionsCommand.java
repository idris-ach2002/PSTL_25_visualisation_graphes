package com.mongraphe.graphui.app.commands;

import com.mongraphe.graphui.controller.EngineOptions;
import com.mongraphe.graphui.interfaces.UndoableGraphCommand;
import com.mongraphe.graphui.rendering.GraphEngine;

public class SetEngineOptionsCommand implements UndoableGraphCommand<GraphEngine> {

    private EngineOptions opts;

    public SetEngineOptionsCommand(EngineOptions opts){
        this.opts = opts;
    }

    @Override
    public void execute(GraphEngine context) {
        context.stopSimulation();
        context.setDegreeScaleFactor(opts.degreeFactor);
        context.setInitialNodeSize(opts.initialNodeSize);
        context.setUpscale(opts.upScale);
        context.setAttractionThreshold(opts.newAmortissement);
        context.setClusterUpdateFrequency(opts.clusterUpdateFrequency);
        context.setNewFriction(opts.newFriction);
        context.setAttractionCoefficient(opts.attractionCoefficient);
        context.setRepulsionThreshold(opts.repulsionThreshold);
        //context.setNbClusters(opts.nbClusters); // TODO / C'EST BROKEN SEGFAULT DU C !
        context.setMinimumDegree(opts.minimumDegree);
        context.setRepulsionMode(opts.repulsionMode);
    }

    @Override
    public void undo(GraphEngine context) {
    }
    
}

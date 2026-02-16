package com.mongraphe.graphui.app.commands;

import com.mongraphe.graphui.interfaces.GraphCommand;
import com.mongraphe.graphui.rendering.GraphEngine;

public class SetNbClustersCommand implements GraphCommand<GraphEngine> {

    private final int nbClusters;

    public SetNbClustersCommand(int nbClusters) {
        this.nbClusters = nbClusters;
    }

    @Override
    public void execute(GraphEngine engine) {
        engine.setNbClusters(nbClusters);
    }
    
}
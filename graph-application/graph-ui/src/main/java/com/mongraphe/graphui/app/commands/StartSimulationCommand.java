package com.mongraphe.graphui.app.commands;

import com.mongraphe.graphui.interfaces.GraphCommand;
import com.mongraphe.graphui.rendering.GraphEngine;

public final class StartSimulationCommand
        implements GraphCommand<GraphEngine> {

    @Override
    public void execute(GraphEngine engine) {
        engine.startSimulation();
    }
}
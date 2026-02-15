package com.mongraphe.graphui.app.commands;

import com.mongraphe.graphui.GraphData;
import com.mongraphe.graphui.interfaces.GraphCommand;
import com.mongraphe.graphui.rendering.GraphEngine;

public final class SetRepulsionModeCommand
        implements GraphCommand<GraphEngine> {

    private final GraphData.RepulsionMode mode;

    public SetRepulsionModeCommand(GraphData.RepulsionMode mode) {
        this.mode = mode;
    }

    @Override
    public void execute(GraphEngine engine) {
        engine.setRepulsionMode(mode);
    }
}
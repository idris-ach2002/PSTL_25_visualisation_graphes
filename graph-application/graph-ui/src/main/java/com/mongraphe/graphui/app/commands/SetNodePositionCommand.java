package com.mongraphe.graphui.app.commands;

import com.mongraphe.graphui.interfaces.GraphCommand;
import com.mongraphe.graphui.rendering.GraphEngine;

public final class SetNodePositionCommand
        implements GraphCommand<GraphEngine> {

    private final int id;
    private final double x, y;

    public SetNodePositionCommand(int id, double x, double y) {
        this.id = id;
        this.x = x;
        this.y = y;
    }

    @Override
    public void execute(GraphEngine engine) {
        engine.setNodePosition(id, x, y);
    }
}
package com.mongraphe.graphui.app.commands;

import com.mongraphe.graphui.interfaces.UndoableGraphCommand;
import com.mongraphe.graphui.rendering.GraphEngine;

public final class MoveVertexCommand
        implements UndoableGraphCommand<GraphEngine> {

    private final int id;
    private final double oldX, oldY;
    private final double newX, newY;

    public MoveVertexCommand(int id,
            double oldX, double oldY,
            double newX, double newY) {
        this.id = id;
        this.oldX = oldX;
        this.oldY = oldY;
        this.newX = newX;
        this.newY = newY;
    }

    @Override
    public void execute(GraphEngine engine) {
        engine.setNodePosition(id, newX, newY);
    }

    @Override
    public void undo(GraphEngine engine) {
        engine.setNodePosition(id, oldX, oldY);
    }
}
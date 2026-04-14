package com.mongraphe.graphui.interaction.commands;

import com.mongraphe.graphui.interfaces.app.UndoableGraphCommand;
import com.mongraphe.graphui.rendering.GraphEngine;

public final class DeleteNodeCommand
        implements UndoableGraphCommand<GraphEngine> {

    private final int id;

    public DeleteNodeCommand(int id) {
        this.id = id;
    }

    @Override
    public void execute(GraphEngine engine) {
        engine.deleteNode(id);
    }

    @Override
    public void undo(GraphEngine engine) {
        engine.restoreNode(id);
    }
}
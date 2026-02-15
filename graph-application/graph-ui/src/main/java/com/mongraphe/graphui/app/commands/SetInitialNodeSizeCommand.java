package com.mongraphe.graphui.app.commands;

import com.mongraphe.graphui.interfaces.GraphCommand;
import com.mongraphe.graphui.rendering.GraphEngine;

public final class SetInitialNodeSizeCommand
        implements GraphCommand<GraphEngine> {

    private final double size;

    public SetInitialNodeSizeCommand(double size) {
        this.size = size;
    }

    @Override
    public void execute(GraphEngine engine) {
        engine.setInitialNodeSize(size);
    }
}
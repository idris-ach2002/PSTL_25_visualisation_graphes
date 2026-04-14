package com.mongraphe.graphui.interaction.commands;

import com.mongraphe.graphui.interfaces.app.UndoableGraphCommand;
import com.mongraphe.graphui.rendering.GraphEngine;

public final class ResizeCameraCommand implements UndoableGraphCommand<GraphEngine> {

    private final float oldZoom;
    private final float newZoom;

    public ResizeCameraCommand(float oldZoom, float newZoom) {
        this.oldZoom = oldZoom;
        this.newZoom = newZoom;
    }

    @Override
    public void execute(GraphEngine engine) {
        engine.camera().setZoom(newZoom);
    }

    @Override
    public void undo(GraphEngine engine) {
        engine.camera().setZoom(oldZoom);
    }
}
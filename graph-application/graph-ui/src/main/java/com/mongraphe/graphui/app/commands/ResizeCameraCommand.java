package com.mongraphe.graphui.app.commands;

import com.mongraphe.graphui.interfaces.UndoableGraphCommand;
import com.mongraphe.graphui.rendering.GraphEngine;

public class ResizeCameraCommand implements UndoableGraphCommand<GraphEngine> {

    private final double oldZoom;
    private final double newZoom;

    public ResizeCameraCommand(double oldZoom, double newZoom) {
        this.oldZoom = oldZoom;
        this.newZoom = newZoom;
    }

    @Override
    public void execute(GraphEngine engine) {
        engine.camera().zoomAt(newZoom > oldZoom ? 1 : -1);
    }

    @Override
    public void undo(GraphEngine engine) {
        engine.camera().zoomAt(newZoom > oldZoom ? -1 : 1);
    }
}
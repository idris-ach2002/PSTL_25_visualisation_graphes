package com.mongraphe.graphui.interaction.modes;

import com.jogamp.newt.event.MouseEvent;
import com.mongraphe.graphui.Vertex;
import com.mongraphe.graphui.app.CommandBus;
import com.mongraphe.graphui.app.commands.MoveVertexCommand;
import com.mongraphe.graphui.interfaces.InteractionModeHandler;
import com.mongraphe.graphui.rendering.GraphEngine;

public final class MoveModeHandler implements InteractionModeHandler {

    private Vertex selected;
    private boolean dragging;
    private double startX, startY;
    private int id = -1;

    @Override
    public void onMousePressed(CommandBus<GraphEngine> bus, int sx, int sy, int button) {

        if (button != MouseEvent.BUTTON1)
            return;

        if (bus == null){
            return;
        }

        float wx = bus.dispatchSync(b -> b.camera().screenToWorldX(sx));
        float wy = bus.dispatchSync(b -> b.camera().screenToWorldY(sy));

        selected = bus.dispatchSync(b -> b.model().findVertexAt(wx, wy));

        if (selected == null)
            return;

        dragging = true;
        id = selected.getId();
        startX = selected.getX();
        startY = selected.getY();
    }

    @Override
    public void onMouseDragged(CommandBus<GraphEngine> bus, int sx, int sy, int button) {

        if (!dragging || selected == null || bus == null)
            return;

        float wx = bus.dispatchSync(b -> b.camera().screenToWorldX(sx));
        float wy = bus.dispatchSync(b -> b.camera().screenToWorldY(sy));

        bus.dispatch(b -> b.setNodePosition(id, wx, wy));
    }

    @Override
    public void onMouseReleased(CommandBus<GraphEngine> bus, int sx, int sy, int button) {

        if (!dragging)
            return;

        dragging = false;

        double endX = selected.getX();
        double endY = selected.getY();

        if (Math.abs(endX - startX) > 1e-6 ||
            Math.abs(endY - startY) > 1e-6) {

            bus.dispatch(new MoveVertexCommand(id, startX, startY, endX, endY));
        }

        selected = null;
        id = -1;
    }

    @Override public void onMouseWheel(CommandBus<GraphEngine> bus, int sx, int sy, float r) {}
    @Override public void onKeyPressed(CommandBus<GraphEngine> bus, int key, boolean ctrl) {}
}
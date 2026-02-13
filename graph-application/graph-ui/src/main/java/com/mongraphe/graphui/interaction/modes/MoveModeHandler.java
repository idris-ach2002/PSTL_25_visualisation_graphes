package com.mongraphe.graphui.interaction.modes;

import com.jogamp.newt.event.MouseEvent;
import com.mongraphe.graphui.Vertex;
import com.mongraphe.graphui.app.InteractionContext;
import com.mongraphe.graphui.interfaces.InteractionModeHandler;
import com.mongraphe.graphui.interaction.actions.MoveAction;
import com.mongraphe.graphui.rendering.Camera2D;
import com.mongraphe.graphui.rendering.GraphEngine;
import com.mongraphe.graphui.model.GraphModel;

public final class MoveModeHandler implements InteractionModeHandler {

    private Vertex selected;
    private boolean dragging;
    private double startX, startY;
    private int id = -1;

    @Override
    public void onMousePressed(InteractionContext ctx, int sx, int sy, int button) {

        if (button != MouseEvent.BUTTON1)
            return;

        GraphEngine engine = ctx.graphService().engine();
        Camera2D cam = ctx.graphService().camera();

        if (engine == null || cam == null)
            return;

        float wx = cam.screenToWorldX(sx);
        float wy = cam.screenToWorldY(sy);

        GraphModel model = engine.model();

        synchronized (model.mutex()) {
            selected = model.findVertexAt(wx, wy);
        }

        if (selected == null)
            return;

        dragging = true;
        id = selected.getId();
        startX = selected.getX();
        startY = selected.getY();
    }

    @Override
    public void onMouseDragged(InteractionContext ctx, int sx, int sy, int button) {

        if (!dragging || selected == null)
            return;

        GraphEngine engine = ctx.graphService().engine();
        Camera2D cam = ctx.graphService().camera();

        if (engine == null || cam == null)
            return;

        float wx = cam.screenToWorldX(sx);
        float wy = cam.screenToWorldY(sy);

        engine.setNodePosition(id, wx, wy);

        synchronized (engine.model().mutex()) {
            selected.updatePosition(wx, wy);
        }
    }

    @Override
    public void onMouseReleased(InteractionContext ctx, int sx, int sy, int button) {

        if (!dragging)
            return;

        dragging = false;

        double endX = selected.getX();
        double endY = selected.getY();

        if (Math.abs(endX - startX) > 1e-6 ||
            Math.abs(endY - startY) > 1e-6) {

            ctx.undo().push(
                new MoveAction(ctx.graphService().engine(), id,
                        startX, startY, endX, endY)
            );
        }

        selected = null;
        id = -1;
    }

    @Override public void onMouseWheel(InteractionContext ctx, int sx, int sy, float r) {}
    @Override public void onKeyPressed(InteractionContext ctx, int key, boolean ctrl) {}
}
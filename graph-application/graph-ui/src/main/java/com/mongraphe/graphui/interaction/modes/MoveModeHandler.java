package com.mongraphe.graphui.interaction.modes;

import com.jogamp.newt.event.MouseEvent;
import com.mongraphe.graphui.Vertex;
import com.mongraphe.graphui.app.InteractionContext;
import com.mongraphe.graphui.interfaces.InteractionModeHandler;
import com.mongraphe.graphui.interaction.actions.MoveAction;

public final class MoveModeHandler implements InteractionModeHandler {

    private Vertex selected;
    private boolean dragging;
    private double startX, startY;
    private int id = -1;

    @Override
    public void onMousePressed(InteractionContext ctx, int sx, int sy, int button) {

        if (button != MouseEvent.BUTTON1)
            return;

        if (ctx.getGraphAdapter() == null)
            return;

        float wx = ctx.getGraphAdapter().screenToWorldX(sx);
        float wy = ctx.getGraphAdapter().screenToWorldY(sy);

        selected = ctx.getGraphAdapter().findVertexAt(wx, wy);

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

        if (ctx.getGraphAdapter() == null)
            return;

        float wx = ctx.getGraphAdapter().screenToWorldX(sx);
        float wy = ctx.getGraphAdapter().screenToWorldY(sy);

        ctx.getGraphAdapter().setNodePosition(id, wx, wy);
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

            ctx.getUndoManager().push(
                new MoveAction(ctx.getGraphAdapter(), id,
                        startX, startY, endX, endY)
            );
        }

        selected = null;
        id = -1;
    }

    @Override public void onMouseWheel(InteractionContext ctx, int sx, int sy, float r) {}
    @Override public void onKeyPressed(InteractionContext ctx, int key, boolean ctrl) {}
}
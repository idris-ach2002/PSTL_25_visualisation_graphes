package com.mongraphe.graphui.interaction.modes;

import com.jogamp.newt.event.MouseEvent;
import com.mongraphe.graphui.Vertex;
import com.mongraphe.graphui.app.InteractionContext;
import com.mongraphe.graphui.interfaces.InteractionModeHandler;
import com.mongraphe.graphui.interaction.actions.DeleteAction;

public final class DeleteModeHandler implements InteractionModeHandler {

    @Override
    public void onMousePressed(InteractionContext ctx,
            int sx,
            int sy,
            int button) {

        if (button != MouseEvent.BUTTON1)
            return;

        if (ctx.getGraphAdapter() == null)
            return;

        float wx = ctx.getGraphAdapter().screenToWorldX(sx);
        float wy = ctx.getGraphAdapter().screenToWorldY(sy);

        Vertex selected = ctx.getGraphAdapter().findVertexAt(wx, wy);

        if (selected == null)
            return;

        int id = selected.getId();
        double previousDiameter = selected.getDiameter();

        ctx.getGraphAdapter().deleteNode(id);

        ctx.getUndoManager().push(new DeleteAction(ctx.getGraphAdapter(), id, previousDiameter));

        ctx.getUI().setStatus("Sommet supprimé: " + id);
    }

    @Override
    public void onMouseDragged(InteractionContext ctx, int sx, int sy, int b) {
    }

    @Override
    public void onMouseReleased(InteractionContext ctx, int sx, int sy, int b) {
    }

    @Override
    public void onMouseWheel(InteractionContext ctx,
            int sx,
            int sy,
            float rotation) {

        ctx.getGraphAdapter().zoomCamera(sx, sy, rotation);
    }

    @Override
    public void onKeyPressed(InteractionContext ctx,
            int keyCode,
            boolean ctrlDown) {
    }
}

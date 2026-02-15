package com.mongraphe.graphui.interaction.modes;

import com.jogamp.newt.event.MouseEvent;
import com.mongraphe.graphui.Vertex;
import com.mongraphe.graphui.app.ApplicationContext;
import com.mongraphe.graphui.interfaces.InteractionModeHandler;

public final class SelectModeHandler implements InteractionModeHandler {

    @Override
    public void onMousePressed(ApplicationContext ctx,
            int sx,
            int sy,
            int button) {

        if (button != MouseEvent.BUTTON1)
            return;

        if (ctx.getGraphAdapter() == null)
            return;

        float wx = ctx.getGraphAdapter().screenToWorldX(sx);
        float wy = ctx.getGraphAdapter().screenToWorldY(sy);

        Vertex selected;

        selected = ctx.getGraphAdapter().findVertexAt(wx, wy);

        if (selected != null) {
            ctx.getUI().setStatus(
                    "Sélection: sommet " + selected.getId());
        } else {
            ctx.getUI().setStatus("Aucune sélection");
        }
    }

    @Override
    public void onMouseWheel(ApplicationContext ctx,
            int sx,
            int sy,
            float rotation) {
        if (ctx.getGraphAdapter() != null)
            ctx.getGraphAdapter().zoomCamera(sx, sy, rotation);
    }

    @Override
    public void onMouseDragged(ApplicationContext ctx,
            int sx,
            int sy,
            int button) {
    }

    @Override
    public void onMouseReleased(ApplicationContext ctx,
            int sx,
            int sy,
            int button) {
    }

    @Override
    public void onKeyPressed(ApplicationContext ctx,
            int keyCode,
            boolean ctrlDown) {
    }
}

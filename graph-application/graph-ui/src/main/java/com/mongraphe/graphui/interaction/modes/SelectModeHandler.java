package com.mongraphe.graphui.interaction.modes;

import com.jogamp.newt.event.MouseEvent;
import com.mongraphe.graphui.Vertex;
import com.mongraphe.graphui.app.InteractionContext;
import com.mongraphe.graphui.interfaces.InteractionModeHandler;
import com.mongraphe.graphui.rendering.Camera2D;
import com.mongraphe.graphui.rendering.GraphEngine;
import com.mongraphe.graphui.model.GraphModel;

public final class SelectModeHandler implements InteractionModeHandler {

    @Override
    public void onMousePressed(InteractionContext ctx,
            int sx,
            int sy,
            int button) {

        if (button != MouseEvent.BUTTON1)
            return;

        GraphEngine engine = ctx.graphService().engine();
        Camera2D camera = ctx.graphService().camera();

        if (engine == null || camera == null)
            return;

        float wx = camera.screenToWorldX(sx);
        float wy = camera.screenToWorldY(sy);

        GraphModel model = engine.model();

        Vertex selected;

        synchronized (model.mutex()) {
            selected = model.findVertexAt(wx, wy);
            model.setSelectedVertexId(
                    selected != null ? selected.getId() : -1);
        }

        if (selected != null) {
            ctx.ui().setStatus(
                    "Sélection: sommet " + selected.getId());
        } else {
            ctx.ui().setStatus("Aucune sélection");
        }
    }


    @Override
    public void onMouseWheel(InteractionContext ctx,
            int sx,
            int sy,
            float rotation) {

        Camera2D camera = ctx.graphService().camera();

        if (camera == null)
            return;

        camera.zoomAt(sx, sy, rotation);
    }

    @Override
    public void onMouseDragged(InteractionContext ctx,
            int sx,
            int sy,
            int button) {
    }

    @Override
    public void onMouseReleased(InteractionContext ctx,
            int sx,
            int sy,
            int button) {
    }

    @Override
    public void onKeyPressed(InteractionContext ctx,
            int keyCode,
            boolean ctrlDown) {
    }
}

package com.mongraphe.graphui.interaction.modes;

import java.awt.event.MouseEvent;

import com.mongraphe.graphui.app.CommandBus;
import com.mongraphe.graphui.app.UiState;
import com.mongraphe.graphui.interaction.commands.DeleteNodeCommand;
import com.mongraphe.graphui.interfaces.InteractionModeHandler;
import com.mongraphe.graphui.model.Vertex;
import com.mongraphe.graphui.rendering.GraphEngine;

public final class DeleteModeHandler implements InteractionModeHandler {

    private final UiState state;
    private boolean panning;
    private int lastX, lastY;

    public DeleteModeHandler(UiState state) {
        this.state = state;
    }

    @Override
    public void onMousePressed(CommandBus<GraphEngine> bus, int sx, int sy, int button) {
        if (bus == null)
            return;

        if (button == MouseEvent.BUTTON3) {
            panning = true;
            lastX = sx;
            lastY = sy;
            return;
        }

        if (button != MouseEvent.BUTTON1)
            return;

        Vertex selected = bus.dispatchSync(engine -> {
            Vertex v = engine.model().findVertexAt(sx, sy, engine.camera());
            return v;
        });

        if (selected == null) {
            state.setStatus("Aucun sommet sous le curseur");
            return;
        }

        bus.dispatchUndoable(new DeleteNodeCommand(selected.getId()));
        bus.dispatch(engine -> engine.setSelectedVertexId(-1));
        state.setStatus("Sommet supprimé: " + selected.getId());
    }

    @Override
    public void onMouseDragged(CommandBus<GraphEngine> bus, int sx, int sy, int button) {
        if (!panning || bus == null)
            return;
        int dx = sx - lastX;
        int dy = sy - lastY;
        lastX = sx;
        lastY = sy;
        bus.dispatch(engine -> engine.camera().pan(dx, dy));
    }

    @Override
    public void onMouseReleased(CommandBus<GraphEngine> bus, int sx, int sy, int button) {
        panning = false;
    }

    @Override
    public void onMouseWheel(CommandBus<GraphEngine> bus, int sx, int sy, float rotation) {
        if (bus != null) {
            float factor = (rotation > 0) ? 1.1f : 0.9f;
            bus.dispatch(engine -> engine.camera().zoomAt(sx, sy, factor));
        }
    }

    @Override
    public void onKeyPressed(CommandBus<GraphEngine> bus, int keyCode, boolean ctrlDown) {
        // Rien à faire
    }
}
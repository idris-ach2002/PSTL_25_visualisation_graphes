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

    public DeleteModeHandler(UiState state) {
        this.state = state;
    }

    @Override
    public void onMousePressed(CommandBus<GraphEngine> bus, int sx, int sy, int button) {
        if (button != MouseEvent.BUTTON1 || bus == null) return;

        Vertex selected = bus.dispatchSync(engine -> {
            float wx = engine.camera().screenToWorldX(sx);
            float wy = engine.camera().screenToWorldY(sy);
            return engine.model().findVertexAt(wx, wy);
        });

        if (selected == null) {
            state.setStatus("Aucun sommet sous le curseur");
            return;
        }

        bus.dispatchUndoable(new DeleteNodeCommand(selected.getId()));
        bus.dispatch(engine -> engine.model().setSelectedVertexId(-1));
        state.setStatus("Sommet supprimé: " + selected.getId());
    }

    @Override public void onMouseDragged(CommandBus<GraphEngine> bus, int sx, int sy, int b) {}
    @Override public void onMouseReleased(CommandBus<GraphEngine> bus, int sx, int sy, int b) {}

    @Override
    public void onMouseWheel(CommandBus<GraphEngine> bus, int sx, int sy, float rotation) {
        if (bus != null) bus.dispatch(engine -> engine.camera().zoomAt(sx, sy, rotation));
    }

    @Override public void onKeyPressed(CommandBus<GraphEngine> bus, int keyCode, boolean ctrlDown) {}
}

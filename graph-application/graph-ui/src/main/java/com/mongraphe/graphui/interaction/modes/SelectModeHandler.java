package com.mongraphe.graphui.interaction.modes;

import java.awt.event.KeyEvent;
import java.awt.event.MouseEvent;

import com.mongraphe.graphui.app.CommandBus;
import com.mongraphe.graphui.app.UiState;
import com.mongraphe.graphui.interaction.commands.DeleteNodeCommand;
import com.mongraphe.graphui.interfaces.InteractionModeHandler;
import com.mongraphe.graphui.model.Vertex;
import com.mongraphe.graphui.rendering.GraphEngine;

public final class SelectModeHandler implements InteractionModeHandler {

    private final UiState state;

    public SelectModeHandler(UiState state) {
        this.state = state;
    }

    @Override
    public void onMousePressed(CommandBus<GraphEngine> bus, int sx, int sy, int button) {
        if (button != MouseEvent.BUTTON1 || bus == null) return;

        Vertex selected = bus.dispatchSync(engine -> {
            float wx = engine.camera().screenToWorldX(sx);
            float wy = engine.camera().screenToWorldY(sy);
            Vertex v = engine.model().findVertexAt(wx, wy);
            engine.model().setSelectedVertexId(v == null ? -1 : v.getId());
            return v;
        });

        if (selected != null) {
            state.setStatus("Sélection: sommet " + selected.getId());
        } else {
            state.setStatus("Aucune sélection");
        }
    }

    @Override
    public void onMouseWheel(CommandBus<GraphEngine> bus, int sx, int sy, float rotation) {
        if (bus != null) bus.dispatch(engine -> engine.camera().zoomAt(sx, sy, rotation));
    }

    @Override public void onMouseDragged(CommandBus<GraphEngine> bus, int sx, int sy, int button) {}
    @Override public void onMouseReleased(CommandBus<GraphEngine> bus, int sx, int sy, int button) {}

    @Override
    public void onKeyPressed(CommandBus<GraphEngine> bus, int keyCode, boolean ctrlDown) {
        if (bus == null) return;
        if (keyCode == KeyEvent.VK_DELETE || keyCode == KeyEvent.VK_BACK_SPACE) {
            Integer id = bus.dispatchSync(engine -> engine.model().getSelectedVertexId());
            if (id != null && id >= 0) {
                bus.dispatchUndoable(new DeleteNodeCommand(id));
                bus.dispatch(engine -> engine.model().setSelectedVertexId(-1));
            }
        }
    }
}

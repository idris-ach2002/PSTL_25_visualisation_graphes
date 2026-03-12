package com.mongraphe.graphui.interaction.modes;

import com.jogamp.newt.event.MouseEvent;
import com.mongraphe.graphui.app.CommandBus;
import com.mongraphe.graphui.app.UiState;
import com.mongraphe.graphui.interfaces.InteractionModeHandler;
import com.mongraphe.graphui.model.Vertex;
import com.mongraphe.graphui.rendering.GraphEngine;

import javafx.application.Platform;

public final class SelectModeHandler implements InteractionModeHandler {
    private UiState state;

    public SelectModeHandler(UiState state) {
        this.state = state;
    }

    @Override
    public void onMousePressed(CommandBus<GraphEngine> bus, int sx, int sy, int button) {
        if (button != MouseEvent.BUTTON1 || bus == null)
            return;

        bus.dispatch(engine -> {
            float wx = engine.camera().screenToWorldX(sx);
            float wy = engine.camera().screenToWorldY(sy);
            Vertex selected = engine.model().findVertexAt(wx, wy);

            Platform.runLater(() -> {
                if (selected != null)
                    state.setStatus("Sélection: sommet " + selected.getId());
                else
                    state.setStatus("Aucune sélection");
            });
        });
    }

    @Override
    public void onMouseWheel(CommandBus<GraphEngine> bus,
            int sx,
            int sy,
            float rotation) {
        if (bus != null)
            bus.dispatch(b -> b.camera().zoomAt(sx, sy, rotation));
    }

    @Override
    public void onMouseDragged(CommandBus<GraphEngine> bus,
            int sx,
            int sy,
            int button) {
    }

    @Override
    public void onMouseReleased(CommandBus<GraphEngine> bus,
            int sx,
            int sy,
            int button) {
    }

    @Override
    public void onKeyPressed(CommandBus<GraphEngine> bus,
            int keyCode,
            boolean ctrlDown) {
    }
}

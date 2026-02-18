package com.mongraphe.graphui.interaction.modes;

import com.mongraphe.graphui.app.CommandBus;
import com.mongraphe.graphui.app.UiState;
import com.mongraphe.graphui.interfaces.InteractionModeHandler;
import com.mongraphe.graphui.rendering.GraphEngine;

import javafx.scene.input.KeyCode;

public final class RunModeHandler implements InteractionModeHandler {

    @Override
    public void onMousePressed(CommandBus<GraphEngine> bus, int sx, int sy, int button) {
        // Ne rien faire, le mode "Run" ne gère pas les interactions de la souris
    }

    @Override
    public void onMouseDragged(CommandBus<GraphEngine> bus, int sx, int sy, int button) {
        // Déplacement de la caméra pour faire du "panning"
        if (bus != null) {
            bus.dispatch(b -> b.camera().pan(sx, sy));
        }
    }

    @Override
    public void onMouseReleased(CommandBus<GraphEngine> bus, int sx, int sy, int button) {
        // Ne rien faire, le mode "Run" ne gère pas les interactions de la souris
    }

    @Override
    public void onMouseWheel(CommandBus<GraphEngine> bus, int sx, int sy, float rotation) {
        // Zoom de la caméra
        if (bus != null) {
            bus.dispatch(b -> b.camera().zoomAt(sx, sy, rotation));
        }
    }

    @Override
    public void onKeyPressed(CommandBus<GraphEngine> bus, int keyCode, boolean ctrlDown) {
        if (bus != null && keyCode == KeyCode.SPACE.getCode()) {
            boolean run = bus.dispatchSync(b -> b.isSimulationRunning());
            if (run) {
                bus.dispatch(b -> b.stopSimulation());
            } else {
                bus.dispatch(b -> b.startSimulation());
            }
        }
    }

    @Override
    public void setUiState(UiState state) {}

}

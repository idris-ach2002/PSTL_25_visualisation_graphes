package com.mongraphe.graphui.interaction.modes;

import com.mongraphe.graphui.app.CommandBus;
import com.mongraphe.graphui.app.UiState;
import com.mongraphe.graphui.interfaces.InteractionModeHandler;
import com.mongraphe.graphui.rendering.GraphEngine;

import javafx.scene.input.KeyCode;

public final class RunModeHandler implements InteractionModeHandler {

    private int lastX;
    private int lastY;

    public RunModeHandler(UiState state) {}

    @Override
    public void onMousePressed(CommandBus<GraphEngine> bus, int sx, int sy, int button) {
        lastX = sx;
        lastY = sy;
    }

    @Override
    public void onMouseDragged(CommandBus<GraphEngine> bus, int sx, int sy, int button) {

        int dx = sx - lastX;
        int dy = sy - lastY;

        lastX = sx;
        lastY = sy;

        if (bus != null) {
            bus.dispatch(b -> b.camera().pan(dx, dy));
        }
    }

    @Override
    public void onMouseReleased(CommandBus<GraphEngine> bus, int sx, int sy, int button) {}

    @Override
    public void onMouseWheel(CommandBus<GraphEngine> bus, int sx, int sy, float rotation) {
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
}
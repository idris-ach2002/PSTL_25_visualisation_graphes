package com.mongraphe.graphui.interaction.modes;

import com.jogamp.newt.event.KeyEvent;
import com.mongraphe.graphui.interfaces.InteractionModeHandler;
import com.mongraphe.graphui.interfaces.InteractionModel;

public final class RunModeHandler implements InteractionModeHandler {

    @Override
    public void onMousePressed(InteractionModel model, int mx, int my, int button) {}

    @Override
    public void onMouseDragged(InteractionModel model, int sx, int sy, int button) {
        model.panCamera(sx, sy);
    }

    @Override
    public void onMouseReleased(InteractionModel model, int sx, int sy, int button) {}

    @Override
    public void onMouseWheel(InteractionModel model, int sx, int sy, float rotation) {
        model.zoomCamera(sx, sy, rotation);
    }

    @Override
    public void onKeyPressed(InteractionModel model, int keyCode, boolean ctrlDown) {
        if (keyCode != KeyEvent.VK_SPACE)
            return;

        if (model.isSimulationRunning()) {
            model.stopSimulation();
        } else {
            model.startSimulation();
        }
    }
}

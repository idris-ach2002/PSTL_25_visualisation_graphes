package com.mongraphe.graphui.interaction.modes;

import com.jogamp.newt.event.KeyEvent;
import com.jogamp.newt.event.MouseEvent;
import com.mongraphe.graphui.Vertex;
import com.mongraphe.graphui.app.InteractionContext;
import com.mongraphe.graphui.interfaces.InteractionModeHandler;
import com.mongraphe.graphui.rendering.Camera2D;
import com.mongraphe.graphui.rendering.GraphEngine;
import com.mongraphe.graphui.model.GraphModel;

public final class RunModeHandler implements InteractionModeHandler {

    private Vertex selected;
    private boolean dragging;

    @Override
    public void onMousePressed(InteractionContext ctx,
            int sx,
            int sy,
            int button) {

        GraphEngine engine = ctx.graphService().engine();
        Camera2D camera = ctx.graphService().camera();

        if (engine == null || camera == null)
            return;

        if (button == MouseEvent.BUTTON1) {

            float wx = camera.screenToWorldX(sx);
            float wy = camera.screenToWorldY(sy);

            GraphModel model = engine.model();

            synchronized (model.mutex()) {
                selected = model.findVertexAt(wx, wy);
                model.setSelectedVertexId(
                        selected != null ? selected.getId() : -1);
            }

            if (selected != null) {
                dragging = true;
                ctx.graphService().engine().stopSimulation(); // supprime flicker
            }
        }
    }

    @Override
    public void onMouseDragged(InteractionContext ctx,
            int sx,
            int sy,
            int button) {

        GraphEngine engine = ctx.graphService().engine();
        Camera2D camera = ctx.graphService().camera();

        if (engine == null || camera == null)
            return;

        if (button == MouseEvent.BUTTON1 &&
                dragging && selected != null) {

            float wx = camera.screenToWorldX(sx);
            float wy = camera.screenToWorldY(sy);

            engine.setNodePosition(selected.getId(), wx, wy);

            synchronized (engine.model().mutex()) {
                selected.updatePosition(wx, wy);
            }
        }

        if (button == MouseEvent.BUTTON3) {
            camera.pan(sx, sy);
        }
    }

    @Override
    public void onMouseReleased(InteractionContext ctx,
            int sx,
            int sy,
            int button) {

        dragging = false;
        selected = null;
    }

    @Override
    public void onMouseWheel(InteractionContext ctx,
            int sx,
            int sy,
            float rotation) {

        Camera2D camera = ctx.graphService().camera();
        if (camera != null)
            camera.zoomAt(sx, sy, rotation);
    }

    @Override
    public void onKeyPressed(InteractionContext ctx,
            int keyCode,
            boolean ctrlDown) {

        if (keyCode != KeyEvent.VK_SPACE)
            return;

        GraphEngine engine = ctx.graphService().engine();
        if (engine == null)
            return;

        if (engine.isSimulationRunning()) {
            engine.stopSimulation();
            ctx.ui().setRunning(false);
            ctx.ui().setStatus("Pause");
        } else {
            engine.startSimulation();
            ctx.ui().setRunning(true);
            ctx.ui().setStatus("Simulation en cours");
        }
    }
}

package com.mongraphe.graphui.interaction.modes;

import java.awt.event.KeyEvent;
import java.awt.event.MouseEvent;

import com.mongraphe.graphui.app.CommandBus;
import com.mongraphe.graphui.app.UiState;
import com.mongraphe.graphui.interaction.commands.DeleteNodeCommand;
import com.mongraphe.graphui.interfaces.InteractionModeHandler;
import com.mongraphe.graphui.model.Vertex;
import com.mongraphe.graphui.rendering.GraphEngine;

public final class RunModeHandler implements InteractionModeHandler {

    private final UiState state;
    private int lastX;
    private int lastY;
    private boolean panning;

    public RunModeHandler(UiState state) {
        this.state = state;
    }

    @Override
    public void onMousePressed(CommandBus<GraphEngine> bus, int sx, int sy, int button) {
        lastX = sx;
        lastY = sy;
        panning = button == MouseEvent.BUTTON3;

        if (button == MouseEvent.BUTTON1 && bus != null) {
            Vertex selected = bus.dispatchSync(engine -> {
                float wx = engine.camera().screenToWorldX(sx);
                float wy = engine.camera().screenToWorldY(sy);
                Vertex v = engine.model().findVertexAt(wx, wy);
                engine.model().setSelectedVertexId(v == null ? -1 : v.getId());
                return v;
            });

            if (selected != null) {
                state.setStatus("Sélection: sommet " + selected.getId());
            }
        }
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
        if (bus == null)
            return;

        if (keyCode == KeyEvent.VK_SPACE) {
            boolean running = bus.dispatchSync(GraphEngine::isSimulationRunning);
            if (running) {
                bus.dispatch(GraphEngine::stopSimulation);
                state.setStatus("Pause");
            } else {
                bus.dispatch(GraphEngine::startSimulation);
                state.setStatus("Simulation en cours");
            }
            return;
        }

        if (keyCode == KeyEvent.VK_DELETE || keyCode == KeyEvent.VK_BACK_SPACE) {
            Integer selectedId = bus.dispatchSync(engine -> engine.model().getSelectedVertexId());
            if (selectedId != null && selectedId >= 0) {
                bus.dispatchUndoable(new DeleteNodeCommand(selectedId));
                bus.dispatch(engine -> engine.model().setSelectedVertexId(-1));
            }
        }
    }
}

package com.mongraphe.graphui.interaction.modes;

import java.awt.event.KeyEvent;
import java.awt.event.MouseEvent;

import com.mongraphe.graphui.app.CommandBus;
import com.mongraphe.graphui.app.UiState;
import com.mongraphe.graphui.interaction.commands.DeleteNodeCommand;
import com.mongraphe.graphui.interaction.commands.MoveVertexCommand;
import com.mongraphe.graphui.interfaces.InteractionModeHandler;
import com.mongraphe.graphui.model.Vertex;
import com.mongraphe.graphui.rendering.GraphEngine;

public final class MoveModeHandler implements InteractionModeHandler {

    private final UiState state;
    private Vertex selected;
    private boolean dragging;
    private double startX, startY;
    private int id = -1;
    private boolean panning;
    private int lastX, lastY;

    public MoveModeHandler(UiState state) {
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

        selected = bus.dispatchSync(engine -> {
            float wx = engine.camera().screenToWorldX(sx);
            float wy = engine.camera().screenToWorldY(sy);
            Vertex v = engine.model().findVertexAt(wx, wy);
            engine.model().setSelectedVertexId(v == null ? -1 : v.getId());
            return v;
        });

        if (selected == null)
            return;

        dragging = true;
        id = selected.getId();
        startX = selected.getX();
        startY = selected.getY();
    }

    @Override
    public void onMouseDragged(CommandBus<GraphEngine> bus, int sx, int sy, int button) {
        if (bus == null)
            return;

        if (panning) {
            int dx = sx - lastX;
            int dy = sy - lastY;
            lastX = sx;
            lastY = sy;
            bus.dispatch(engine -> engine.camera().pan(dx, dy));
            return;
        }

        if (!dragging || selected == null)
            return;

        float wx = bus.dispatchSync(engine -> engine.camera().screenToWorldX(sx));
        float wy = bus.dispatchSync(engine -> engine.camera().screenToWorldY(sy));
        bus.dispatch(engine -> engine.setNodePosition(id, wx, wy));
        state.setStatus("Sommet " + id + " déplacé");
    }

    @Override
    public void onMouseReleased(CommandBus<GraphEngine> bus, int sx, int sy, int button) {
        if (panning) {
            panning = false;
            return;
        }

        if (!dragging || bus == null)
            return;
        dragging = false;

        double[] end = bus.dispatchSync(engine -> {
            Vertex v = engine.model().vertexById(id);
            return v == null ? new double[] { startX, startY } : new double[] { v.getX(), v.getY() };
        });

        if (Math.abs(end[0] - startX) > 1e-6 || Math.abs(end[1] - startY) > 1e-6) {
            bus.dispatchUndoable(new MoveVertexCommand(id, startX, startY, end[0], end[1]));
        }

        selected = null;
        id = -1;
    }

    @Override
    public void onMouseWheel(CommandBus<GraphEngine> bus, int sx, int sy, float rotation) {
        if (bus != null)
            bus.dispatch(engine -> engine.camera().zoomAt(sx, sy, rotation));
    }

    @Override
    public void onKeyPressed(CommandBus<GraphEngine> bus, int keyCode, boolean ctrlDown) {
        if (bus == null)
            return;
        if (keyCode == KeyEvent.VK_DELETE || keyCode == KeyEvent.VK_BACK_SPACE) {
            Integer selectedId = bus.dispatchSync(engine -> engine.model().getSelectedVertexId());
            if (selectedId != null && selectedId >= 0) {
                bus.dispatchUndoable(new DeleteNodeCommand(selectedId));
                bus.dispatch(engine -> engine.model().setSelectedVertexId(-1));
            }
        }
    }
}
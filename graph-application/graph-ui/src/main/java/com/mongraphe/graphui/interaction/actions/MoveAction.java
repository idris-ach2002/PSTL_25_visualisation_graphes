package com.mongraphe.graphui.interaction.actions;

import com.mongraphe.graphui.interaction.UndoManager;
import com.mongraphe.graphui.app.GraphEngineAdapter;

public final class MoveAction implements UndoManager.EditAction {

    private final GraphEngineAdapter adapter;
    private final int id;
    private final double fromX, fromY;
    private final double toX, toY;

    public MoveAction(GraphEngineAdapter adapter,
            int id,
            double fromX, double fromY,
            double toX, double toY) {

        this.adapter = adapter;
        this.id = id;
        this.fromX = fromX;
        this.fromY = fromY;
        this.toX = toX;
        this.toY = toY;
    }

    @Override
    public void undo() {
        setPosition(fromX, fromY);
    }

    @Override
    public void redo() {
        setPosition(toX, toY);
    }

    private void setPosition(double x, double y) {

        if (adapter == null)
            return;

        adapter.setNodePosition(id, (float) x, (float) y);
    }
}

package com.mongraphe.graphui.interaction.actions;

import com.mongraphe.graphui.interaction.UndoManager;
import com.mongraphe.graphui.rendering.GraphEngine;
import com.mongraphe.graphui.model.GraphModel;
import com.mongraphe.graphui.Vertex;

public final class MoveAction implements UndoManager.EditAction {

    private final GraphEngine engine;
    private final int id;
    private final double fromX, fromY;
    private final double toX, toY;

    public MoveAction(GraphEngine engine,
                      int id,
                      double fromX, double fromY,
                      double toX, double toY) {

        this.engine = engine;
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

        if (engine == null)
            return;

        engine.setNodePosition(id, x, y);

        GraphModel model = engine.model();

        synchronized (model.mutex()) {
            if (id >= 0 && id < model.vertices().size()) {
                Vertex v = model.vertices().get(id);
                v.updatePosition(x, y);
            }
        }
    }
}

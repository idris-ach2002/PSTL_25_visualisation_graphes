package com.mongraphe.graphui.interaction.actions;

import com.mongraphe.graphui.Vertex;
import com.mongraphe.graphui.interaction.UndoManager;
import com.mongraphe.graphui.model.GraphModel;
import com.mongraphe.graphui.rendering.GraphEngine;

public class DeleteAction implements UndoManager.EditAction {

    private final GraphEngine engine;
    private final int id;
    private final double previousDiameter;

    public DeleteAction(GraphEngine engine,
            int id,
            double previousDiameter) {

        this.engine = engine;
        this.id = id;
        this.previousDiameter = previousDiameter;
    }

    @Override
    public void undo() {

        if (engine == null)
            return;

        engine.restoreNode(id);

        GraphModel model = engine.model();

        synchronized (model.mutex()) {
            if (id >= 0 && id < model.vertices().size()) {
                Vertex v = model.vertices().get(id);
                v.restore();
                model.applyFilters();
            }
        }
    }

    @Override
    public void redo() {

        if (engine == null)
            return;

        engine.deleteNode(id);

        GraphModel model = engine.model();

        synchronized (model.mutex()) {
            if (id >= 0 && id < model.vertices().size()) {
                model.deleteVertex(model.vertices().get(id));
                model.setSelectedVertexId(-1);
            }
        }
    }

}
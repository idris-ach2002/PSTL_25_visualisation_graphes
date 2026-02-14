package com.mongraphe.graphui.interaction.actions;

import com.mongraphe.graphui.Vertex;
import com.mongraphe.graphui.app.GraphEngineAdapter;
import com.mongraphe.graphui.interaction.UndoManager;
import com.mongraphe.graphui.model.GraphModel;
import com.mongraphe.graphui.rendering.GraphEngine;

public class DeleteAction implements UndoManager.EditAction {

    private final GraphEngineAdapter adapter;
    private final int id;
    private final double previousDiameter;

    public DeleteAction(GraphEngineAdapter adapter,
            int id,
            double previousDiameter) {

        this.adapter = adapter;
        this.id = id;
        this.previousDiameter = previousDiameter;
    }

    @Override
    public void undo() {

        if (adapter == null)
            return;

        adapter.restoreNode(id);
        adapter.setNodeDiameter(id, previousDiameter);
    }

    @Override
    public void redo() {

        if (adapter == null)
            return;

        adapter.deleteNode(id);
        adapter.setNodeDiameter(id, previousDiameter);
    }

}
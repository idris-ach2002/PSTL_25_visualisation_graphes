package com.mongraphe.graphui.interaction.modes;

import com.jogamp.newt.event.MouseEvent;
import com.mongraphe.graphui.Vertex;
import com.mongraphe.graphui.app.CommandBus;
import com.mongraphe.graphui.app.commands.DeleteNodeCommand;
import com.mongraphe.graphui.interfaces.InteractionModeHandler;
import com.mongraphe.graphui.rendering.GraphEngine;

public final class DeleteModeHandler implements InteractionModeHandler {

    @Override
    public void onMousePressed(CommandBus<GraphEngine> bus,
            int sx,
            int sy,
            int button) {

        if (button != MouseEvent.BUTTON1)
            return;

        if (bus == null)
            return;

        float wx = bus.dispatchSync(b -> b.camera().screenToWorldX(sx));
        float wy = bus.dispatchSync(b -> b.camera().screenToWorldY(sy));

        Vertex selected = bus.dispatchSync(b -> b.model().findVertexAt(wx, wy));

        int id = selected.getId();
        double previousDiameter = selected.getDiameter();

        bus.dispatch(new DeleteNodeCommand(id));

        getUI().setStatus("Sommet supprimé: " + id);
    }

    @Override
    public void onMouseDragged(CommandBus<GraphEngine> bus, int sx, int sy, int b) {
    }

    @Override
    public void onMouseReleased(CommandBus<GraphEngine> bus, int sx, int sy, int b) {
    }

    @Override
    public void onMouseWheel(CommandBus<GraphEngine> bus,
            int sx,
            int sy,
            float rotation) {

        bus.dispatch(b -> b.camera().zoomAt(sx, sy, rotation));
    }

    @Override
    public void onKeyPressed(CommandBus<GraphEngine> bus,
            int keyCode,
            boolean ctrlDown) {
    }
}

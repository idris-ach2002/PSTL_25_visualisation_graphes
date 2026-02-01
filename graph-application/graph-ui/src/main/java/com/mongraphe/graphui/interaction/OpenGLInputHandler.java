package com.mongraphe.graphui.interaction;

import com.jogamp.newt.event.*;
import com.mongraphe.graphui.export.ExportService;
import com.mongraphe.graphui.model.GraphModel;
import com.mongraphe.graphui.rendering.Camera2D;

public class OpenGLInputHandler implements MouseListener, KeyListener {

    private final GraphModel model;
    private final Camera2D camera;
    private final GraphInteractionState state;
    private final RenderRequester renderRequester;
    private final ExportService exportService;

    public OpenGLInputHandler(GraphModel model,
            Camera2D camera,
            GraphInteractionState state,
            RenderRequester renderRequester,
            ExportService exportService) {
        this.model = model;
        this.camera = camera;
        this.state = state;
        this.renderRequester = renderRequester;
        this.exportService = exportService;
    }

    @Override
    public void mousePressed(MouseEvent e) {
        double x = camera.screenToWorldX(e.getX());
        double y = camera.screenToWorldY(e.getY());

        var v = model.findVertexAt(x, y);
        state.setSelectedVertex(v);

        if (state.isSelectionMode() && v != null)
            state.startVertexDrag(v, x, y);
        else if (state.isMoveMode())
            state.startGraphDrag(e.getX(), e.getY());
    }

    @Override
    public void mouseDragged(MouseEvent e) {
        if (state.isDraggingVertex()) {
            state.dragVertex(camera.screenToWorldX(e.getX()),
                    camera.screenToWorldY(e.getY()));
        } else if (state.isDraggingGraph()) {
            camera.panFromDrag(e.getX() - state.getDragStartX(),
                    e.getY() - state.getDragStartY());
            state.updateDragStart(e.getX(), e.getY());
        }
        renderRequester.requestRender();
    }

    @Override
    public void mouseWheelMoved(MouseEvent e) {
        camera.zoomAt(e.getX(), e.getY(), e.getRotation()[1]);
        renderRequester.requestRender();
    }

    @Override
    public void mouseClicked(MouseEvent e) {
        double x = camera.screenToWorldX(e.getX());
        double y = camera.screenToWorldY(e.getY());
        var v = model.findVertexAt(x, y);

        if (state.isDeleteMode() && v != null) {
            model.deleteVertex(v);
            renderRequester.requestRender();
        }
    }

    @Override
    public void mouseReleased(MouseEvent e) {
        state.stopAllDrags();
    }

    @Override
    public void keyReleased(KeyEvent e) {
    }

    @Override
    public void mouseMoved(MouseEvent e) {
    }

    @Override
    public void mouseEntered(MouseEvent e) {
    }

    @Override
    public void mouseExited(MouseEvent e) {
    }

    @Override
    public void keyPressed(KeyEvent e) {
        switch (e.getKeyChar()) {
            case '1' -> state.setSelectionMode();
            case '2' -> state.setDeleteMode();
            case '3' -> state.setRunMode();
            case '4' -> state.setMoveMode();
            case '6' -> exportService.export("capture/graph.png");
        }
    }
}
package com.mongraphe.graphui.interaction;

import com.jogamp.newt.event.*;
import com.mongraphe.graphui.Vertex;
import com.mongraphe.graphui.model.GraphModel;

public class OpenGLInputHandler implements MouseListener, KeyListener {

    private final GraphModel model;
    private final CameraController camera;
    private final GraphInteractionState state;

    private final ViewportProvider viewport;
    private final RenderRequester renderRequester;
    private final ExportService exportService;

    public OpenGLInputHandler(
            GraphModel model,
            CameraController camera,
            GraphInteractionState state,
            ViewportProvider viewport,
            RenderRequester renderRequester,
            ExportService exportService) {

        this.model = model;
        this.camera = camera;
        this.state = state;
        this.viewport = viewport;
        this.renderRequester = renderRequester;
        this.exportService = exportService;
    }

    // ================== MOUSE ==================

    @Override
    public void mousePressed(MouseEvent e) {
        double x = camera.screenToWorldX(e.getX());
        double y = camera.screenToWorldY(e.getY());

        Vertex v = model.findVertexAt(x, y);
        state.setSelectedVertex(v);

        if (state.isSelectionMode() && v != null)
            state.startVertexDrag(v, x, y);
        else if (state.isMoveMode())
            state.startGraphDrag(e.getX(), e.getY());
    }

    @Override
    public void mouseDragged(MouseEvent e) {

        if (state.isDraggingVertex()) {
            state.dragVertex(
                    camera.screenToWorldX(e.getX()),
                    camera.screenToWorldY(e.getY()));
            renderRequester.requestRender();
        } else if (state.isDraggingGraph()) {
            int dx = e.getX() - state.getDragStartX();
            int dy = e.getY() - state.getDragStartY();
            camera.panFromDrag(dx, dy);
            state.updateDragStart(e.getX(), e.getY());
            renderRequester.requestRender();
        }
    }

    @Override
    public void mouseWheelMoved(MouseEvent e) {
        if (!state.isMoveMode())
            return;
        camera.zoomAt(e.getX(), e.getY(), e.getRotation()[1]);
        renderRequester.requestRender();
    }

    @Override
    public void mouseClicked(MouseEvent e) {
        double x = camera.screenToWorldX(e.getX());
        double y = camera.screenToWorldY(e.getY());

        Vertex v = model.findVertexAt(x, y);
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
            case '1':
                state.setSelectionMode();
                break;
            case '2':
                state.setDeleteMode();
                break;
            case '3':
                state.setRunMode();
                break;
            case '4':
                state.setMoveMode();
                break;
            case '5':
                state.toggleMinimumDegree();
                break;
            case '6':
                exportService.export("capture/graph.png");
                break;
        }
    }

    @Override
    public void keyReleased(KeyEvent e) {
    }
}
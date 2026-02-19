package com.mongraphe.graphui.interaction;

import com.mongraphe.graphui.Vertex;

public class GraphInteractionState {

    private Vertex selectedVertex;

    private boolean selectionMode = true;
    private boolean deleteMode = false;
    private boolean moveMode = false;
    private boolean runMode = false;

    private boolean draggingVertex = false;
    private boolean draggingGraph = false;

    private double dragOffsetX;
    private double dragOffsetY;

    private int dragStartX;
    private int dragStartY;

    public void setSelectionMode() {
        selectionMode = true;
        deleteMode = moveMode = runMode = false;
    }

    public void setDeleteMode() {
        deleteMode = true;
        selectionMode = moveMode = runMode = false;
    }

    public void setMoveMode() {
        moveMode = true;
        selectionMode = deleteMode = runMode = false;
    }

    public void setRunMode() {
        runMode = true;
        selectionMode = deleteMode = moveMode = false;
    }

    public boolean isSelectionMode() {
        return selectionMode;
    }

    public boolean isDeleteMode() {
        return deleteMode;
    }

    public boolean isMoveMode() {
        return moveMode;
    }

    public boolean isRunMode() {
        return runMode;
    }

    public void setSelectedVertex(Vertex v) {
        selectedVertex = v;
    }

    public void startVertexDrag(Vertex v, double worldX, double worldY) {
        draggingVertex = true;
        dragOffsetX = v.getX() - worldX;
        dragOffsetY = v.getY() - worldY;
    }

    public void dragVertex(double worldX, double worldY) {
        if (selectedVertex != null) {
            selectedVertex.updatePosition(worldX + dragOffsetX, worldY + dragOffsetY);
        }
    }

    public boolean isDraggingVertex() {
        return draggingVertex;
    }

    public void startGraphDrag(int x, int y) {
        draggingGraph = true;
        dragStartX = x;
        dragStartY = y;
    }

    public boolean isDraggingGraph() {
        return draggingGraph;
    }

    public int getDragStartX() {
        return dragStartX;
    }

    public int getDragStartY() {
        return dragStartY;
    }

    public void updateDragStart(int x, int y) {
        dragStartX = x;
        dragStartY = y;
    }

    public void stopAllDrags() {
        draggingVertex = false;
        draggingGraph = false;
    }
}
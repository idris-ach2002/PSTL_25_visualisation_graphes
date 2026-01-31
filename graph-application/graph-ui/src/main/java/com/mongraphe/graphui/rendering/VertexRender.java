package com.mongraphe.graphui.rendering;

import java.util.List;

import com.mongraphe.graphui.Community;
import com.mongraphe.graphui.Vertex;

public class VertexRender implements com.mongraphe.interfaces.VertexRenderI {
    public List<Vertex> vertices;
    private float[] vertexPoints;
    private float[] vertexSizes;
    private float[] vertexColors;
    private float[] vertexVisibility;

    /**
     * sets new position for the node
     * 
     * @param index index of node to move
     * @param x     new x position
     * @param y     new y position
     */
    public native void setNodePosition(int index, double x, double y);

    /**
     * ignores node for the algorithm
     * 
     * @param index index of node to delete
     */
    public native void deleteNode(int index);

    /**
     * restores deleted node for the algorithm
     * 
     * @param index index of node to restore
     */
    public native void restoreNode(int index);

    public Vertex findVertexAt(double x, double y, double zoomFactor) {
        for (Vertex v : vertices) {
            if (v.isDeleted())
                continue;

            double dx = x - v.getX();
            double dy = y - v.getY();
            double distance = Math.sqrt(dx * dx + dy * dy);
            double vertexDiameter = v.getDiameter();
            double margin = (vertexDiameter < 3) ? 3 : 0;
            double selectionRadius = ((v.getDiameter() / 2) + margin) / zoomFactor;

            if (distance <= selectionRadius) {
                return v;
            }
        }
        return null;
    }

    private void prepareVertexRenderData() {
        Vertex currentVertex;
        Community currentCommunity;

        for (int i = 0; i < vertices.size(); i++) {
            currentVertex = vertices.get(i);
            currentCommunity = currentVertex.getCommunity(); // léger nettoyage

            // Mise à jour des buffers
            vertexPoints[i * 2] = (float) currentVertex.getX();
            vertexPoints[i * 2 + 1] = (float) currentVertex.getY();
            vertexSizes[i] = (float) currentVertex.getDiameter();
            vertexColors[i * 3] = currentCommunity.getR();
            vertexColors[i * 3 + 1] = currentCommunity.getG();
            vertexColors[i * 3 + 2] = currentCommunity.getB();

            // Mise à jour de la visibilité
            vertexVisibility[i] = (currentVertex.isDeleted() || !currentVertex.isVisible()) ? 0.0f : 1.0f;
        }
    }
}

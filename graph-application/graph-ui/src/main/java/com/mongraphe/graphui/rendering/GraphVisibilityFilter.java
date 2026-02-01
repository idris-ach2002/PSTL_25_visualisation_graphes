package com.mongraphe.graphui.rendering;

import com.mongraphe.graphui.Edge;
import com.mongraphe.graphui.Vertex;

public class GraphVisibilityFilter {

    private int minimumDegree = 0;
    private float edgeWeightThreshold = 0.0f;

    // Stats calculées
    private int visibleVertices;
    private int hiddenVertices;
    private int deletedVertices;
    private int visibleEdges;
    private int hiddenEdges;

    public void setMinimumDegree(int degree) {
        this.minimumDegree = Math.max(0, degree);
    }

    public void setEdgeWeightThreshold(float threshold) {
        this.edgeWeightThreshold = threshold;
    }

    public void apply(GraphScene scene) {

        visibleVertices = 0;
        hiddenVertices = 0;
        deletedVertices = 0;
        visibleEdges = 0;
        hiddenEdges = 0;

        for (Vertex v : scene.vertices()) {
            if (v.isDeleted()) {
                deletedVertices++;
                v.setVisible(false);
            } else if (v.getDegree() >= minimumDegree) {
                v.setVisible(true);
                visibleVertices++;
            } else {
                v.setVisible(false);
                hiddenVertices++;
            }
        }

        for (Edge e : scene.edges()) {
            boolean visible = e.getStart().isVisible() && e.getEnd().isVisible()
                    && e.getWeight() >= edgeWeightThreshold;
            e.setVisible(visible);
            if (visible)
                visibleEdges++;
            else
                hiddenEdges++;
        }

        scene.computeVisibilityStats();
    }

    public int getVisibleVertices() {
        return visibleVertices;
    }

    public int getHiddenVertices() {
        return hiddenVertices;
    }

    public int getDeletedVertices() {
        return deletedVertices;
    }

    public int getVisibleEdges() {
        return visibleEdges;
    }

    public int getHiddenEdges() {
        return hiddenEdges;
    }
}
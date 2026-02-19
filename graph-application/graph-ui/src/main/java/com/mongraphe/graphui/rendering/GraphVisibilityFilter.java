package com.mongraphe.graphui.rendering;

import com.mongraphe.graphui.Edge;
import com.mongraphe.graphui.Vertex;
import com.mongraphe.graphui.model.GraphModel;

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

    public void apply(GraphModel model) {

        visibleVertices = 0;
        hiddenVertices = 0;
        deletedVertices = 0;
        visibleEdges = 0;
        hiddenEdges = 0;

        for (Vertex v : model.vertices()) {
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

        for (Edge e : model.edges()) {
            boolean visible = e.getStart().isVisible() && e.getEnd().isVisible()
                    && e.getWeight() >= edgeWeightThreshold;
            e.setVisible(visible);
            if (visible)
                visibleEdges++;
            else
                hiddenEdges++;
        }

        model.computeVisibilityStats();
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
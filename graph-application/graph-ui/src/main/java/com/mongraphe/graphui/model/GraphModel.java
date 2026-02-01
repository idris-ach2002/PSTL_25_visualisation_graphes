package com.mongraphe.graphui.model;

import com.mongraphe.graphui.Vertex;
import com.mongraphe.graphui.rendering.GraphScene;

public class GraphModel {

    private final GraphScene scene;
    private double zoom = 1.0;

    public GraphModel(GraphScene scene) {
        this.scene = scene;
    }

    public void setZoom(double z) {
        this.zoom = z;
    }

    public Vertex findVertexAt(double x, double y) {
        for (Vertex v : scene.vertices()) {
            if (v.isDeleted()) continue;

            double dx = x - v.getX();
            double dy = y - v.getY();
            double dist = Math.sqrt(dx * dx + dy * dy);
            double r = (v.getDiameter() / 2.0) / zoom;

            if (dist <= r) return v;
        }
        return null;
    }

    public void deleteVertex(Vertex v) {
        v.delete();
    }
}
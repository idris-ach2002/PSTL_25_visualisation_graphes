package com.mongraphe.graphui.model;

import java.util.ArrayList;
import java.util.List;

import com.mongraphe.graphui.Edge;
import com.mongraphe.graphui.EdgeC;
import com.mongraphe.graphui.Vertex;

public class GraphModel {

    private double zoom = 1.0;
    private final List<Vertex> vertices = new ArrayList<>();
    private final List<Edge> edges = new ArrayList<>();

    private int visibleVertexCount;
    private int visibleEdgeCount;

    public void setZoom(double z) {
        this.zoom = z;
    }

    public Vertex findVertexAt(double x, double y) {
        for (Vertex v : vertices) {
            if (v.isDeleted()) continue;

            double dx = x - v.getX();
            double dy = y - v.getY();
            double dist = Math.sqrt(dx * dx + dy * dy);
            double r = (v.getDiameter() / 2.0) / zoom;

            if (dist <= r) return v;
        }
        return null;
    }

    public void buildFromData(Vertex[] verticesArray, EdgeC[] edgesArray) {
        clear();

        for (Vertex v : verticesArray) {
            addVertex(v);
        }

        for (EdgeC ec : edgesArray) {
            Vertex start = vertices.get(ec.getStart());
            Vertex end = vertices.get(ec.getEnd());
            Edge e = new Edge(start, end, ec.getWeight());
            addEdge(e);
        }

        computeVisibilityStats();
    }

    public void updateVertexPositions(Vertex[] verticesArray) {
        for (int i = 0; i < vertices.size(); i++) {
            Vertex v = vertices.get(i);
            Vertex newV = verticesArray[i];
            v.updatePosition(newV.getX(), newV.getY());
        }
        computeVisibilityStats();
    }

    public List<Edge> edges() {
        return edges;
    }

    public List<Vertex> vertices() {
        return vertices;
    }

    public int vertexCount() {
        return vertices.size();
    }

    public int edgeCount() {
        return edges.size();
    }

    public void addVertex(Vertex v) {
        vertices.add(v);
    }

    public void addEdge(Edge e) {
        edges.add(e);
    }

    public void removeVertex(Vertex v) {
        vertices.remove(v);
        edges.removeIf(e -> e.getStart() == v || e.getEnd() == v);
    }

    public void clear() {
        vertices.clear();
        edges.clear();
    }

    public void computeVisibilityStats() {
        visibleVertexCount = 0;
        visibleEdgeCount = 0;

        for (Vertex v : vertices) {
            if (!v.isDeleted() && v.isVisible())
                visibleVertexCount++;
        }

        for (Edge e : edges) {
            if (e.isVisible())
                visibleEdgeCount++;
        }
    }

    public int getVisibleVertexCount() {
        return visibleVertexCount;
    }

    public int getVisibleEdgeCount() {
        return visibleEdgeCount;
    }

    public void deleteVertex(Vertex v) {
        v.delete();
    }
}
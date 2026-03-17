package com.mongraphe.graphui.model;

import java.util.ArrayList;
import java.util.HashMap;
import java.util.List;
import java.util.Map;

public class GraphModel {

    public enum ColoringMode {
        COMMUNITY,
        DEGREE,
        UNIFORM
    }

    private final Object mutex = new Object();

    private double zoom = 1.0;
    private int selectedVertexId = -1;
    private int filterMinDegree = 0;
    private double filterMinEdgeWeight = 0.0;

    private float uniformNodeR = 0.82f;
    private float uniformNodeG = 0.82f;
    private float uniformNodeB = 0.86f;

    private ColoringMode coloringMode = ColoringMode.COMMUNITY;
    private final List<Vertex> vertices = new ArrayList<>();
    private final List<Edge> edges = new ArrayList<>();
    private final Map<Integer, Vertex> verticesById = new HashMap<>();

    private int visibleVertexCount;
    private int visibleEdgeCount;
    private int maxDegree = 1;

    public Object mutex() { return mutex; }
    public void setZoom(double z) { this.zoom = z; }
    public double getZoom() { return zoom; }
    public int getSelectedVertexId() { return selectedVertexId; }
    public void setSelectedVertexId(int id) { this.selectedVertexId = id; }
    public ColoringMode getColoringMode() { return coloringMode; }
    public void setColoringMode(ColoringMode mode) { this.coloringMode = mode == null ? ColoringMode.COMMUNITY : mode; }
    public int getMaxDegree() { return maxDegree; }
    public int getFilterMinDegree() { return filterMinDegree; }
    public double getFilterMinEdgeWeight() { return filterMinEdgeWeight; }

    public void setFilterMinDegree(int minDegree) {
        this.filterMinDegree = Math.max(0, minDegree);
        applyFilters();
    }

    public void setFilterMinEdgeWeight(double minEdgeWeight) {
        this.filterMinEdgeWeight = Math.max(0.0, minEdgeWeight);
        applyFilters();
    }

    public void setUniformNodeColor(float r, float g, float b) {
        this.uniformNodeR = clamp01(r);
        this.uniformNodeG = clamp01(g);
        this.uniformNodeB = clamp01(b);
    }

    public float getUniformNodeR() { return uniformNodeR; }
    public float getUniformNodeG() { return uniformNodeG; }
    public float getUniformNodeB() { return uniformNodeB; }

    private static float clamp01(float v) { return Math.max(0f, Math.min(1f, v)); }

    public Vertex findVertexAt(double x, double y) {
        for (Vertex v : vertices) {
            if (v.isDeleted() || !v.isVisible()) continue;
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
        for (Vertex v : verticesArray) addVertex(v);
        for (EdgeC ec : edgesArray) {
            Vertex start = vertices.get(ec.getStart());
            Vertex end = vertices.get(ec.getEnd());
            addEdge(new Edge(start, end, ec.getWeight()));
        }
        applyFilters();
    }

    public void updateVertexPositions(Vertex[] verticesArray) {
        int limit = Math.min(vertices.size(), verticesArray.length);
        for (int i = 0; i < limit; i++) {
            Vertex v = vertices.get(i);
            Vertex newV = verticesArray[i];
            v.updatePosition(newV.getX(), newV.getY());
        }
        applyFilters();
    }

    public List<Edge> edges() { return edges; }
    public List<Vertex> vertices() { return vertices; }
    public int vertexCount() { return vertices.size(); }
    public int edgeCount() { return edges.size(); }

    public void addVertex(Vertex v) {
        vertices.add(v);
        verticesById.put(v.getId(), v);
    }

    public Vertex vertexById(int id) { return verticesById.get(id); }
    public void addEdge(Edge e) { edges.add(e); }

    public void removeVertex(Vertex v) {
        vertices.remove(v);
        verticesById.remove(v.getId());
        edges.removeIf(e -> e.getStart() == v || e.getEnd() == v);
        applyFilters();
    }

    public void clear() {
        vertices.clear();
        edges.clear();
        verticesById.clear();
        selectedVertexId = -1;
        visibleVertexCount = 0;
        visibleEdgeCount = 0;
        maxDegree = 1;
    }

    public void applyFilters() {
        for (Vertex v : vertices) {
            if (v.isDeleted()) {
                v.setVisible(false);
                continue;
            }
            v.setVisible(v.getDegree() >= filterMinDegree);
        }

        for (Edge e : edges) {
            boolean endpointsOk = e.getStart().isVisible() && e.getEnd().isVisible();
            boolean wOk = e.getWeight() >= filterMinEdgeWeight;
            e.setVisible(endpointsOk && wOk);
        }

        computeVisibilityStats();
    }

    public void computeVisibilityStats() {
        visibleVertexCount = 0;
        visibleEdgeCount = 0;
        maxDegree = 1;

        for (Vertex v : vertices) {
            if (!v.isDeleted() && v.isVisible()) visibleVertexCount++;
            maxDegree = Math.max(maxDegree, v.getDegree());
        }

        for (Edge e : edges) {
            if (e.isVisible()) visibleEdgeCount++;
        }
    }

    public int getVisibleVertexCount() { return visibleVertexCount; }
    public int getVisibleEdgeCount() { return visibleEdgeCount; }

    public void deleteVertex(Vertex v) {
        v.delete();
        if (selectedVertexId == v.getId()) {
            selectedVertexId = -1;
        }
        applyFilters();
    }
}

package com.mongraphe.graphui.model;

import java.util.ArrayList;
import java.util.List;
import java.util.concurrent.ConcurrentLinkedQueue;
import java.util.concurrent.locks.ReentrantReadWriteLock;

import com.mongraphe.graphui.rendering.Camera2D;

public class GraphModel {

    public enum ColoringMode {
        COMMUNITY, DEGREE, UNIFORM
    }

    private final ReentrantReadWriteLock lock = new ReentrantReadWriteLock();

    private volatile int selectedVertexId = -1;
    private int filterMinDegree = 0;
    private double filterMinEdgeWeight = 0.0;

    private float uniformNodeR = 0.82f;
    private float uniformNodeG = 0.82f;
    private float uniformNodeB = 0.86f;

    private ColoringMode coloringMode = ColoringMode.COMMUNITY;
    private final ConcurrentLinkedQueue<Edge> edges = new ConcurrentLinkedQueue<>();
    private final List<Vertex> vertices = new ArrayList<>(); // index = id

    private int visibleVertexCount;
    private int visibleEdgeCount;
    private int maxDegree = 1;
    private int deletedVerticesCount = 0;

    public ReentrantReadWriteLock lock() {
        return lock;
    }

    public int getSelectedVertexId() {
        return selectedVertexId;
    }

    public void setSelectedVertexId(int id) {
        this.selectedVertexId = id;
    }

    public ColoringMode getColoringMode() {
        return coloringMode;
    }

    public void setColoringMode(ColoringMode mode) {
        this.coloringMode = mode == null ? ColoringMode.COMMUNITY : mode;
    }

    public int getMaxDegree() {
        return maxDegree;
    }

    public int getFilterMinDegree() {
        return filterMinDegree;
    }

    public double getFilterMinEdgeWeight() {
        return filterMinEdgeWeight;
    }

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

    public float getUniformNodeR() {
        return uniformNodeR;
    }

    public float getUniformNodeG() {
        return uniformNodeG;
    }

    public float getUniformNodeB() {
        return uniformNodeB;
    }

    private static float clamp01(float v) {
        return Math.max(0f, Math.min(1f, v));
    }

    public Vertex findVertexAt(int screenX, int screenY, Camera2D camera) {
        double worldX = camera.screenToWorldX(screenX);
        double worldY = camera.screenToWorldY(screenY);
        for (Vertex v : vertices) {
            if (v == null || v.isDeleted() || !v.isVisible())
                continue;
            double dx = worldX - v.getX();
            double dy = worldY - v.getY();
            double dist = Math.sqrt(dx * dx + dy * dy);
            double r = (v.getDiameter() / 2.0) / camera.getZoom();
            if (dist <= r)
                return v;
        }
        return null;
    }

    public void buildFromData(Vertex[] verticesArray, EdgeC[] edgesArray) {
        lock.writeLock().lock();
        try {
            clear();
            for (Vertex v : verticesArray) {
                if (v != null) {
                    vertices.add(v);
                }
            }
            for (EdgeC ec : edgesArray) {
                Vertex start = vertices.get(ec.getStart());
                Vertex end = vertices.get(ec.getEnd());
                addEdge(new Edge(start, end, ec.getWeight()));
            }
            applyFilters();
        } finally {
            lock.writeLock().unlock();
        }
    }

    public ConcurrentLinkedQueue<Edge> edges() {
        return edges;
    }

    public int getDeletedVerticesCount() {
        return deletedVerticesCount;
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

    public Vertex vertexById(int id) {
        if (id < 0 || id >= vertices.size())
            return null;
        return vertices.get(id);
    }

    public void addVertex(Vertex v) {
        vertices.add(v);
    }

    public void addEdge(Edge e) {
        edges.add(e);
    }

    public void clear() {
        vertices.clear();
        edges.clear();
        selectedVertexId = -1;
        visibleVertexCount = 0;
        visibleEdgeCount = 0;
        maxDegree = 1;
        deletedVerticesCount = 0;
    }

    private void applyFilters() {
        deletedVerticesCount = 0;
        for (Vertex v : vertices) {
            if (v.isDeleted()) {
                v.setVisible(false);
                deletedVerticesCount++;
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
            if (!v.isDeleted() && v.isVisible())
                visibleVertexCount++;
            maxDegree = Math.max(maxDegree, v.getDegree());
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
        if (selectedVertexId == v.getId())
            selectedVertexId = -1;
        applyFilters();
    }
}
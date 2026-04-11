package com.mongraphe.graphui.model;

import java.util.concurrent.ConcurrentHashMap;
import java.util.concurrent.ConcurrentLinkedQueue;
import java.util.concurrent.atomic.AtomicInteger;

import com.mongraphe.graphui.rendering.Camera2D;

public class GraphModel {

    public enum ColoringMode {
        COMMUNITY, DEGREE, UNIFORM
    }

    // Collections concurrentes (lecture lock-free)
    private final ConcurrentLinkedQueue<Vertex> vertices = new ConcurrentLinkedQueue<>();
    private final ConcurrentHashMap<Integer, Vertex> vertexById = new ConcurrentHashMap<>();
    private final ConcurrentLinkedQueue<Edge> edges = new ConcurrentLinkedQueue<>();

    // Statistiques atomiques
    private final AtomicInteger visibleVertexCount = new AtomicInteger(0);
    private final AtomicInteger visibleEdgeCount = new AtomicInteger(0);
    private final AtomicInteger maxDegree = new AtomicInteger(1);
    private final AtomicInteger deletedVerticesCount = new AtomicInteger(0);
    private final AtomicInteger nextVertexId = new AtomicInteger(0);

    // Paramètres volatils (écrits rarement, lus souvent)
    private volatile int selectedVertexId = -1;
    private volatile int filterMinDegree = 0;
    private volatile double filterMinEdgeWeight = 0.0;
    private volatile ColoringMode coloringMode = ColoringMode.COMMUNITY;

    private volatile float uniformNodeR = 0.82f;
    private volatile float uniformNodeG = 0.82f;
    private volatile float uniformNodeB = 0.86f;

    // Verrou pour les opérations d'écriture multi-étapes
    private final Object writeLock = new Object();

    // ----------------------------------------------------------------------
    // Accesseurs (lecture lock‑free)
    // ----------------------------------------------------------------------

    public ConcurrentLinkedQueue<Vertex> vertices() {
        return vertices;
    }

    public ConcurrentLinkedQueue<Edge> edges() {
        return edges;
    }

    public Vertex vertexById(int id) {
        return vertexById.get(id);
    }

    public int vertexCount() {
        return vertices.size();
    }

    public int edgeCount() {
        return edges.size();
    }

    public int getVisibleVertexCount() {
        return visibleVertexCount.get();
    }

    public int getVisibleEdgeCount() {
        return visibleEdgeCount.get();
    }

    public int getMaxDegree() {
        return maxDegree.get();
    }

    public int getDeletedVerticesCount() {
        return deletedVerticesCount.get();
    }

    public int getSelectedVertexId() {
        return selectedVertexId;
    }

    public ColoringMode getColoringMode() {
        return coloringMode;
    }

    public int getFilterMinDegree() {
        return filterMinDegree;
    }

    public double getFilterMinEdgeWeight() {
        return filterMinEdgeWeight;
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

    // ----------------------------------------------------------------------
    // Mutateurs (la plupart sans verrou car simples)
    // ----------------------------------------------------------------------

    public void setSelectedVertexId(int id) {
        this.selectedVertexId = id;
    }

    public void setColoringMode(ColoringMode mode) {
        this.coloringMode = mode == null ? ColoringMode.COMMUNITY : mode;
    }

    public void setUniformNodeColor(float r, float g, float b) {
        this.uniformNodeR = clamp01(r);
        this.uniformNodeG = clamp01(g);
        this.uniformNodeB = clamp01(b);
    }

    public void setFilterMinDegree(int minDegree) {
        this.filterMinDegree = Math.max(0, minDegree);
        applyFilters();
    }

    public void setFilterMinEdgeWeight(double minEdgeWeight) {
        this.filterMinEdgeWeight = Math.max(0.0, minEdgeWeight);
        applyFilters();
    }

    // ----------------------------------------------------------------------
    // Construction / Mise à jour depuis le natif
    // ----------------------------------------------------------------------

    /**
     * Reconstruit entièrement le modèle à partir des tableaux natifs.
     * Les indices du tableau temporaire sont utilisés pour lier les arêtes.
     */
    public void buildFromData(Vertex[] verticesArray, EdgeC[] edgesArray) {
        synchronized (writeLock) {
            clear();

            // Table temporaire pour les correspondances index -> Vertex
            Vertex[] tempIndex = new Vertex[verticesArray.length];

            for (int i = 0; i < verticesArray.length; i++) {
                Vertex v = verticesArray[i];
                if (v != null) {
                    int id = nextVertexId.getAndIncrement();
                    v.setId(id);
                    vertices.add(v);
                    vertexById.put(id, v);
                    tempIndex[i] = v;
                }
            }

            for (EdgeC ec : edgesArray) {
                Vertex start = tempIndex[ec.getStart()];
                Vertex end = tempIndex[ec.getEnd()];
                if (start != null && end != null) {
                    edges.add(new Edge(start, end, ec.getWeight()));
                }
            }

            applyFilters();
        }
    }

    // ----------------------------------------------------------------------
    // Ajout dynamique
    // ----------------------------------------------------------------------

    public void addVertex(Vertex v) {
        synchronized (writeLock) {
            int id = nextVertexId.getAndIncrement();
            v.setId(id);
            vertices.add(v);
            vertexById.put(id, v);
            applyFilters();
        }
    }

    public void addEdge(Edge e) {
        edges.add(e);
        if (e.isVisible()) {
            visibleEdgeCount.incrementAndGet();
        }
    }

    // ----------------------------------------------------------------------
    // Nettoyage
    // ----------------------------------------------------------------------

    public void clear() {
        synchronized (writeLock) {
            vertices.clear();
            vertexById.clear();
            edges.clear();
            nextVertexId.set(0);
            selectedVertexId = -1;
            visibleVertexCount.set(0);
            visibleEdgeCount.set(0);
            maxDegree.set(1);
            deletedVerticesCount.set(0);
        }
    }

    // ----------------------------------------------------------------------
    // Suppression
    // ----------------------------------------------------------------------

    public void deleteVertex(Vertex v) {
        v.delete();
        if (selectedVertexId == v.getId()) {
            selectedVertexId = -1;
        }
        applyFilters();
    }

    // ----------------------------------------------------------------------
    // Application des filtres (recalcule visibilité et stats)
    // ----------------------------------------------------------------------

    private void applyFilters() {
        synchronized (writeLock) {
            int visV = 0, visE = 0, maxDeg = 1, delCount = 0;

            for (Vertex v : vertices) {
                if (v.isDeleted()) {
                    v.setVisible(false);
                    delCount++;
                    continue;
                }
                boolean visible = v.getDegree() >= filterMinDegree;
                v.setVisible(visible);
                if (visible)
                    visV++;
                maxDeg = Math.max(maxDeg, v.getDegree());
            }

            for (Edge e : edges) {
                boolean endpointsOk = e.getStart().isVisible() && e.getEnd().isVisible();
                boolean wOk = e.getWeight() >= filterMinEdgeWeight;
                boolean visible = endpointsOk && wOk;
                e.setVisible(visible);
                if (visible)
                    visE++;
            }

            visibleVertexCount.set(visV);
            visibleEdgeCount.set(visE);
            maxDegree.set(maxDeg);
            deletedVerticesCount.set(delCount);
        }
    }

    // ----------------------------------------------------------------------
    // Recherche spatiale (lecture lock‑free directe)
    // ----------------------------------------------------------------------

    public Vertex findVertexAt(int screenX, int screenY, Camera2D camera) {
        double worldX = camera.screenToWorldX(screenX);
        double worldY = camera.screenToWorldY(screenY);
        for (Vertex v : vertices) {
            if (v.isDeleted() || !v.isVisible())
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

    private static float clamp01(float v) {
        return Math.max(0f, Math.min(1f, v));
    }
}
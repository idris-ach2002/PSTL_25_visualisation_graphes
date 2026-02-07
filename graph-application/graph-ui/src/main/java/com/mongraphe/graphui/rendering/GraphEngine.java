package com.mongraphe.graphui.rendering;

import java.util.HashMap;

import com.mongraphe.graphui.Community;
import com.mongraphe.graphui.Edge;
import com.mongraphe.graphui.EdgeC;
import com.mongraphe.graphui.GraphData;
import com.mongraphe.graphui.Vertex;
import com.mongraphe.graphui.model.GraphModel;

public final class GraphEngine {

    private final GraphNativeEngine nativeEngine;
    private final GraphModel model;
    private final GraphSimulation simulation;
    private final GraphVisibilityFilter visibility;
    private double updateInterval = 16.0; // ~60 FPS

    public GraphEngine(GraphNativeEngine nativeEngine) {
        this.nativeEngine = nativeEngine;
        this.model = new GraphModel();
        this.simulation = new GraphSimulation(nativeEngine);
        this.visibility = new GraphVisibilityFilter();
    }

    public void loadCsv(String path,
            GraphData.SimilitudeMode sim,
            GraphData.NodeCommunity communityMode) {

        if (path == null || path.isBlank())
            throw new IllegalArgumentException("CSV path missing");

        nativeEngine.initGraphCsv(path, sim, communityMode);

        // Récupère les positions, arêtes et communautés depuis le moteur natif
        Vertex[] verticesArray = nativeEngine.getPositions();
        EdgeC[] edgesArray = nativeEngine.getEdges();
        int[] communityIds = nativeEngine.getCommunities();
        float[][] colors = nativeEngine.getClusterColors();

        HashMap<Integer, Community> communities = new HashMap<>();
        for (int i = 0; i < verticesArray.length; i++) {
            int cid = communityIds[i];
            Community c = communities.get(cid);
            if (c == null) {
                c = new Community(cid, colors[i][0], colors[i][1], colors[i][2]);
                communities.put(cid, c);
            }
            verticesArray[i].setCommunity(c);
        }

        // Construire le modèle Java
        model.clear();
        for (Vertex v : verticesArray) {
            v.updateDiameter();
            model.addVertex(v);
        }
        for (EdgeC ec : edgesArray) {
            Vertex start = model.vertices().get(ec.getStart());
            Vertex end = model.vertices().get(ec.getEnd());
            model.addEdge(new Edge(start, end, ec.getWeight()));
        }

        visibility.apply(model);
    }

    public void update() {
        simulation.update(model);
        visibility.apply(model);
    }

    public GraphModel model() {
        return model;
    }

    public void setMinimumDegree(int degree) {
        visibility.setMinimumDegree(degree);
    }

    public GraphVisibilityFilter visibility() {
        return visibility;
    }

    public void setNodeSize(int size) {
        for (Vertex v : model.vertices()) {
            v.setDiameter(size);
        }
    }

    public void setDegreeScaleFactor(double factor) {
        nativeEngine.setThresholdS(factor);
    }

    public void setUpscale(int up) {
        nativeEngine.setSeuilRep(up);
    }

    public void setStabilizedThreshold(double t) {
        nativeEngine.setThresholdA(t);
    }

    public void setAttractionThreshold(double t) {
        nativeEngine.setThresholdA(t);
    }

    public void setNewFriction(double f) {
        nativeEngine.setFriction(f);
    }

    public void setAttractionCoefficient(double c) {
        nativeEngine.setAttractionCoeff(c);
    }

    public void setRepulsionThreshold(double t) {
        nativeEngine.setAntiRepulsion(t);
    }

    public void setNewAmortissement(double a) {
        nativeEngine.setAmortissement(a);
    }

    public void setNbClusters(int n) {
        nativeEngine.SetNumberClusters(n);
    }

    public void setRepulsionMode(GraphData.RepulsionMode mode) {
        nativeEngine.setModeRepulsion(mode.ordinal());
    }

    public void enableKmeans(boolean enabled) {
        nativeEngine.setKmeansMode(enabled);
    }

    public void setNodePosition(int index, double x, double y) {
        nativeEngine.setNodePosition(index, x, y);
    }

    public void deleteNode(int index) {
        nativeEngine.deleteNode(index);
    }

    public void restoreNode(int index) {
        nativeEngine.restoreNode(index);
    }

    public void setUpdateInterval(double ms) {
        this.updateInterval = ms;
    }

    public void setDimensions(double width, double height) {
        nativeEngine.setDimension(width, height);
    }
}
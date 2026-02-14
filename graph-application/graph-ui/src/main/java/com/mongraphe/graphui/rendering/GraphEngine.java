package com.mongraphe.graphui.rendering;

import java.util.HashMap;

import com.mongraphe.graphui.Community;
import com.mongraphe.graphui.Edge;
import com.mongraphe.graphui.EdgeC;
import com.mongraphe.graphui.GraphData;
import com.mongraphe.graphui.Vertex;
import com.mongraphe.graphui.model.GraphModel;

/**
 * Façade Java côté UI pour piloter le moteur natif via JNI et maintenir
 * un modèle Java exploitable par le renderer et l'interface.
 */
public final class GraphEngine {

    private final GraphNativeEngine nativeEngine;
    private final GraphModel model;
    private final GraphSimulation simulation;
    private final GraphVisibilityFilter visibility;

    /**
     * Fréquence (itérations) d'update des clusters côté natif (k-means).
     * L'UI la manipule via le champ "updatedFrequence".
     */
    private int clusterUpdateFrequency = 1;

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

        rebuildModelFromNative();
    }

    public void loadDot(String path,
            GraphData.NodeCommunity communityMode) {

        if (path == null || path.isBlank())
            throw new IllegalArgumentException("DOT path missing");

        nativeEngine.initGraphDot(path, communityMode);

        rebuildModelFromNative();
    }

    private void rebuildModelFromNative() {
        // Récupère les positions, arêtes et communautés depuis le moteur natif
        Vertex[] verticesArray = nativeEngine.getPositions();
        EdgeC[] edgesArray = nativeEngine.getEdges();
        int[] communityIds = nativeEngine.getCommunities();
        float[][] colors = nativeEngine.getClusterColors();

        // Stabilité: le moteur natif ne pose pas forcément l'id -> on force l'index.
        for (int i = 0; i < verticesArray.length; i++) {
            verticesArray[i].setId(i);
        }

        HashMap<Integer, Community> communities = new HashMap<>();
        for (int i = 0; i < verticesArray.length; i++) {
            int cid = (communityIds != null && i < communityIds.length) ? communityIds[i] : 0;
            Community c = communities.get(cid);
            if (c == null) {
                float r = (colors != null && i < colors.length) ? colors[i][0] : 0.7f;
                float g = (colors != null && i < colors.length) ? colors[i][1] : 0.7f;
                float b = (colors != null && i < colors.length) ? colors[i][2] : 0.7f;
                c = new Community(cid, r, g, b);
                communities.put(cid, c);
            }
            verticesArray[i].setCommunity(c);
        }

        synchronized (model.mutex()) {
            model.clear();
            for (Vertex v : verticesArray) {
                v.updateDiameter();
                model.addVertex(v);
            }
            for (EdgeC ec : edgesArray) {
                if (ec.getStart() < 0 || ec.getStart() >= model.vertices().size())
                    continue;
                if (ec.getEnd() < 0 || ec.getEnd() >= model.vertices().size())
                    continue;

                Vertex start = model.vertices().get(ec.getStart());
                Vertex end = model.vertices().get(ec.getEnd());
                model.addEdge(new Edge(start, end, ec.getWeight()));
            }
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

    public void setNodeDiameter(int index, double diameter) {
        synchronized (model.mutex()) {
            if (index >= 0 && index < model.vertices().size()) {
                model.vertices().get(index).setDiameter(diameter);
            }
        }
    }

    public void setMinimumDegree(int degree) {
        visibility.setMinimumDegree(degree);
    }

    public GraphVisibilityFilter visibility() {
        return visibility;
    }

    /**
     * Taille initiale du nœud (base), avant ajout du facteur de degré.
     * Important: met à jour JNI + les statics du modèle Java.
     */
    public void setInitialNodeSize(double size) {
        Vertex.initial_node_size = size;
        nativeEngine.setInitialNodeSize(size);
        for (Vertex v : model.vertices()) {
            v.updateDiameter();
        }
    }

    /**
     * Facteur de taille proportionnel au degré.
     * Important: met à jour JNI + les statics du modèle Java.
     */
    public void setDegreeScaleFactor(double factor) {
        Vertex.degree_scale_factor = factor;
        nativeEngine.setDegreeScaleFactor(factor);
        for (Vertex v : model.vertices()) {
            v.updateDiameter();
        }
    }

    /**
     * Upscale Java (rend les coordonnées plus lisibles côté rendu).
     * Note: ne re-scale pas les coordonnées déjà chargées.
     */
    public void setUpscale(int up) {
        Vertex.upscale = up;
    }

    public void setStabilizedThreshold(double t) {
        nativeEngine.setThresholdS(t);
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
        nativeEngine.setSeuilRep(t);
    }

    public void setAntiRepulsion(double t) {
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
        synchronized (model.mutex()) {
            if (index >= 0 && index < model.vertices().size()) {
                model.vertices().get(index).updatePosition(x, y);
            }
        }
    }

    public void deleteNode(int index) {
        nativeEngine.deleteNode(index);
        synchronized (model.mutex()) {
            if (index >= 0 && index < model.vertices().size()) {
                model.deleteVertex(model.vertices().get(index));
            }
        }
    }

    public void restoreNode(int index) {
        nativeEngine.restoreNode(index);
        rebuildModelFromNative(); // TODO : normalement on devrait pas avoir à tt rebuild !
    }

    /**
     * Champ UI "updatedFrequence": fréquence des recalculs de clusters.
     */
    public void setClusterUpdateFrequency(int saut) {
        this.clusterUpdateFrequency = Math.max(1, saut);
        nativeEngine.setSaut(this.clusterUpdateFrequency);
    }

    public int getClusterUpdateFrequency() {
        return clusterUpdateFrequency;
    }

    public void setDimensions(double width, double height) {
        nativeEngine.setDimension(width, height);
    }

    /**
     * Libère la mémoire allouée côté C (structures du graphe, buffers, etc.).
     * À appeler lors d'un reload ou à la fermeture.
     */
    public void startSimulation() {
        simulation.start();
    }

    public void stopSimulation() {
        simulation.stop();
    }

    public boolean isSimulationRunning() {
        return simulation.isRunning();
    }

    public void freeNativeMemory() {
        nativeEngine.freeAllocatedMemory();
    }
}

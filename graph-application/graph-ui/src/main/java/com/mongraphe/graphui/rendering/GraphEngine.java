package com.mongraphe.graphui.rendering;

import java.util.ArrayList;
import java.util.HashMap;
import java.util.List;
import java.util.Map;

import com.mongraphe.graphui.model.Community;
import com.mongraphe.graphui.model.Edge;
import com.mongraphe.graphui.model.EdgeC;
import com.mongraphe.graphui.model.GraphData;
import com.mongraphe.graphui.model.GraphModel;
import com.mongraphe.graphui.model.GraphProject;
import com.mongraphe.graphui.model.Vertex;

/**
 * Façade Java côté UI pour piloter le moteur natif via JNI et maintenir
 * un modèle Java exploitable par le renderer et l'interface.
 */
public final class GraphEngine {

    private final GraphNativeEngine nativeEngine;
    private final Camera2D camera;
    private final GraphModel model;
    private final GraphSimulation simulation;
    private final GraphVisibilityFilter visibility;

    // Couleur de fond paramétrable depuis l'UI
    private volatile float clearR = 1f, clearG = 1f, clearB = 1f, clearA = 1f;

    /**
     * Fréquence (itérations) d'update des clusters côté natif (k-means).
     * L'UI la manipule via le champ "updatedFrequence".
     */
    private int clusterUpdateFrequency = 1;

    public interface GraphEngineListener {
        /** Appelé lorsque la simulation démarre réellement */
        void onSimulationStarted();

        /** Appelé lorsque la simulation se termine */
        void onSimulationStopped();
    }

    private final List<GraphEngineListener> listeners = new ArrayList<>();

    public void addListener(GraphEngineListener listener) {
        synchronized (listeners) {
            listeners.add(listener);
        }
    }

    public void removeListener(GraphEngineListener listener) {
        synchronized (listeners) {
            listeners.remove(listener);
        }
    }

    public static class GraphDataSnapshot {
        private final List<Vertex> vertices = new ArrayList<>();
        private final List<Edge> edges = new ArrayList<>();
        private final Map<Integer, Vertex> verticesById = new HashMap<>();

        private int visibleVertexCount;
        private int visibleEdgeCount;

        public GraphDataSnapshot(List<Vertex> vertices, List<Edge> edges, int visibleVertexCount,
                int visibleEdgeCount) {
            this.vertices.addAll(vertices);
            this.edges.addAll(edges);
            this.visibleVertexCount = visibleVertexCount;
            this.visibleEdgeCount = visibleEdgeCount;

            for (Vertex v : vertices) {
                verticesById.put(v.getId(), v);
            }
        }

        public List<Vertex> getVertices() {
            return vertices;
        }

        public List<Edge> getEdges() {
            return edges;
        }

        public int getVisibleVertexCount() {
            return visibleVertexCount;
        }

        public int getVisibleEdgeCount() {
            return visibleEdgeCount;
        }
    }

    public GraphDataSnapshot getDataSnapshot() {
        synchronized (model.mutex()) {
            return new GraphDataSnapshot(model.vertices(), model.edges(), model.getVisibleVertexCount(),
                    model.getVisibleEdgeCount());
        }
    }

    public GraphEngine(GraphNativeEngine nativeEngine) {
        this.nativeEngine = nativeEngine;
        this.camera = new Camera2D();
        this.model = new GraphModel();
        this.simulation = new GraphSimulation(nativeEngine);
        this.visibility = new GraphVisibilityFilter();
    }

    public boolean load(String path, GraphProject.SourceType type, GraphData.SimilitudeMode sim,
            GraphData.NodeCommunity communityMode) {
        switch (type) {
            case CSV:
                loadCsv(path, sim, communityMode);
                return true;
            case DOT:
                loadDot(path, communityMode);
                return true;
        }
        return false;
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

    public void setBackgroundColor(float r, float g, float b, float a) {
        this.clearR = clamp01(r);
        this.clearG = clamp01(g);
        this.clearB = clamp01(b);
        this.clearA = clamp01(a);
    }

    public float getBackgroundColorR() {
        return clearR;
    }

    public float getBackgroundColorG() {
        return clearG;
    }

    public float getBackgroundColorB() {
        return clearB;
    }

    public float getBackgroundColorA() {
        return clearA;
    }

    private float clamp01(float v) {
        if (v < 0f)
            return 0f;
        if (v > 1f)
            return 1f;
        return v;
    }

    public GraphModel model() {
        return model;
    }

    public Camera2D camera() {
        return camera;
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

    /**
     * Libère la mémoire allouée côté C (structures du graphe, buffers, etc.).
     * À appeler lors d'un reload ou à la fermeture.
     */
    public void startSimulation() {
        simulation.start();

        // Notifier les listeners que la simulation a démarré
        synchronized (listeners) {
            for (GraphEngineListener listener : listeners) {
                listener.onSimulationStarted();
            }
        }
    }

    public void stopSimulation() {
        simulation.stop();

        // Notifier que la simulation s'est arrêtée
        synchronized (listeners) {
            for (GraphEngineListener listener : listeners) {
                listener.onSimulationStopped();
            }
        }
    }

    public boolean isSimulationRunning() {
        return simulation.isRunning();
    }
}

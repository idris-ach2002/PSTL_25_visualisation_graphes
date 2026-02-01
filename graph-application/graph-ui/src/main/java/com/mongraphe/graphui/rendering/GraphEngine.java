package com.mongraphe.graphui.rendering;

import com.mongraphe.graphui.GraphData;
import com.mongraphe.graphui.Metadata;

public class GraphEngine {

    private final GraphNativeEngine nativeEngine;
    private final GraphScene scene;
    private final GraphVisibilityFilter visibilityFilter;

    public GraphEngine(GraphNativeEngine nativeEngine) {
        this.nativeEngine = nativeEngine;
        this.scene = new GraphScene();
        this.visibilityFilter = new GraphVisibilityFilter();
    }

    public void initCsv(String path, GraphData.SimilitudeMode sim, GraphData.NodeCommunity community) {
        if (path == null || path.isEmpty())
            throw new RuntimeException("Chemin du fichier non spécifié.");

        nativeEngine.startsProgram(path);

        int modeSimilitude = nativeEngine.getModeSimilitude(sim);
        Metadata thresholds = nativeEngine.computeThreshold(modeSimilitude, 50);
        if (thresholds == null)
            throw new RuntimeException("Erreur lors du calcul des seuils.");

        int modeCommunity = nativeEngine.getModeCommunity(community);
        nativeEngine.initializeGraph(modeCommunity, thresholds.getEdgeThreshold(), thresholds.getAntiThreshold());

        scene.buildFromData(nativeEngine.getPositions(), nativeEngine.getEdges());

        System.out.println("Graphe chargé : " + nativeEngine.getPositions().length + " sommets, "
                + nativeEngine.getEdges().length + " arêtes.");
    }

    public void update() {
        if (nativeEngine.updatePositions()) {
            scene.updateVertexPositions(nativeEngine.getPositions());
        }
        visibilityFilter.apply(scene);
    }

    public GraphScene getScene() {
        return scene;
    }
}

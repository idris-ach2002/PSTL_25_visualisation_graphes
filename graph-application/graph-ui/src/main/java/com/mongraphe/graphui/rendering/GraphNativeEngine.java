package com.mongraphe.graphui.rendering;

import com.mongraphe.graphui.EdgeC;
import com.mongraphe.graphui.GraphData;
import com.mongraphe.graphui.Metadata;
import com.mongraphe.graphui.Vertex;

/**
 * Classe centrale pour interfacer avec le moteur natif C.
 * Toutes les fonctions JNI sont regroupées ici.
 */
public class GraphNativeEngine {

    private Metadata metadata;        // Metadata du graphe courant
    private Metadata init_metadata;   // Metadata après le calcul des seuils

    static {
        System.loadLibrary("native"); // Charger la bibliothèque native "libnative"
    }

    public double[][] initGraph(String path, GraphData.SimilitudeMode sim, GraphData.NodeCommunity community) {
        if (path == null || path.isEmpty())
            throw new RuntimeException("initGraph : Chemin du fichier non spécifié.");

        double[][] csvData = startsProgram(path);

        int modeSimilitude = getModeSimilitude(sim);
        init_metadata = computeThreshold(modeSimilitude, 50); // edge_factor = 50
        if (init_metadata == null)
            throw new RuntimeException("initGraph : Erreur lors du calcul des seuils.");

        double threshold = init_metadata.getEdgeThreshold();
        double antiThreshold = init_metadata.getAntiThreshold();
        System.out.println("Seuil recommandé : " + threshold + ", Anti-seuil : " + antiThreshold);

        int modeCommunity = getModeCommunity(community);
        metadata = initializeGraph(modeCommunity, threshold, antiThreshold);

        return csvData;
    }

    public double[][] initGraphCsv(String path, GraphData.SimilitudeMode sim, GraphData.NodeCommunity community) {
        if (path == null || path.isEmpty())
            throw new RuntimeException("initGraphCsv : Chemin du fichier non spécifié.");

        double[][] csvData = startsProgram(path);

        int modeSimilitude = getModeSimilitude(sim);
        init_metadata = computeThreshold(modeSimilitude, 5); // edge_factor = 5 pour CSV
        if (init_metadata == null)
            throw new RuntimeException("initGraphCsv : Erreur lors du calcul des seuils.");

        double threshold = init_metadata.getEdgeThreshold();
        double antiThreshold = init_metadata.getAntiThreshold();
        System.out.println("Seuil recommandé : " + threshold + ", Anti-seuil : " + antiThreshold);

        int modeCommunity = getModeCommunity(community);
        metadata = initializeGraph(modeCommunity, threshold, antiThreshold);

        return csvData;
    }

    public void initGraphDot(String path, GraphData.NodeCommunity community) {
        if (path == null || path.isEmpty())
            throw new RuntimeException("initGraphDot : Chemin du fichier non spécifié.");

        int modeCommunity = getModeCommunity(community);
        metadata = initializeDot(path, modeCommunity);
    }

    public int getModeCommunity(GraphData.NodeCommunity community) {
        return switch (community) {
            case LOUVAIN -> 0;
            case LOUVAIN_PAR_COMPOSANTE -> 1;
            case LEIDEN -> 2;
            case LEIDEN_CPM -> 3;
            case COULEURS_SPECIALES -> 4;
        };
    }

    public int getModeSimilitude(GraphData.SimilitudeMode mode) {
        return switch (mode) {
            case CORRELATION -> 0;
            case DISTANCE_COSINE -> 1;
            case DISTANCE_EUCLIDIENNE -> 2;
            case NORME_L1 -> 3;
            case NORME_LINF -> 4;
            case KL_DIVERGENCE -> 5;
        };
    }

    public Metadata getMetadata() {
        return metadata;
    }

    public Metadata getInitMetadata() {
        return init_metadata;
    }

    public native Metadata initializeDot(String filepath, int md);
    public native Metadata computeThreshold(int modeSimilitude, int edge_factor);
    public native Metadata initializeGraph(int modeCommunity, double threshold, double anti_threshold);
    public native double[][] startsProgram(String filename);
    public native boolean updatePositions();
    public native Vertex[] getPositions();
    public native EdgeC[] getEdges();
    public native void setNodePosition(int id, double x, double y);
    public native void deleteNode(int id);
    public native void restoreNode(int id);
}

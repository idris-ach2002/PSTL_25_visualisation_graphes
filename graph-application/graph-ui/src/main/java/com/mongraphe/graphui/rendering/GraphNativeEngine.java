package com.mongraphe.graphui.rendering;

import com.mongraphe.graphui.model.EdgeC;
import com.mongraphe.graphui.model.GraphData;
import com.mongraphe.graphui.model.Metadata;
import com.mongraphe.graphui.model.Vertex;

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

    private int getModeCommunity(GraphData.NodeCommunity community) {
        return switch (community) {
            case LOUVAIN -> 0;
            case LOUVAIN_PAR_COMPOSANTE -> 1;
            case LEIDEN -> 2;
            case LEIDEN_CPM -> 3;
            case COULEURS_SPECIALES -> 4;
        };
    }

    private int getModeSimilitude(GraphData.SimilitudeMode mode) {
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

    // Méthodes JNI
    public native Metadata initializeDot(String filepath, int md);
    public native Metadata initializeGraph(int modeCommunity, double threshold, double antiThreshold);
    public native double[][] startsProgram(String filename);
    public native Metadata computeThreshold(int modeSimilitude, int edge_factor);
    public native void setDimension(double width, double height);
    public native boolean updatePositions();
    public native Vertex[] getPositions();
    public native void setNodePosition(int index, double x, double y);
    public native EdgeC[] getEdges();
    public native int[] getCommunities();
    public native float[][] getClusterColors();
    public native void setSaut(int saut);
    public native void setThresholdS(double thresholdS);
    public native void setFriction(double friction);
    public native void setModeRepulsion(int mode);
    public native void setAntiRepulsion(double antiedge_repulsion);
    public native void setAttractionCoeff(double attraction_coeff);
    public native void setThresholdA(double thresholdA);
    public native void setSeuilRep(double seuilrep);
    public native void setAmortissement(double amortissement);
    public native void SetNumberClusters(int new_number_of_clusters);
    public native void setKmeansMode(boolean md);
    public native void setInitialNodeSize(double size);
    public native void setDegreeScaleFactor(double factor);
    public native void deleteNode(int index);
    public native void restoreNode(int index);
    public native void freeAllocatedMemory();
}
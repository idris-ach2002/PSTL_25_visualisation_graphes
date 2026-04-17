package com.mongraphe.graphui.controller;

public final class EngineOptions {
    // Dynamique
    public Double friction;
    public Double attractionCoefficient;
    public Double repulsionCoeff; // intra‑cluster
    public Double antiRepulsion; // anti‑arêtes
    public Double amortissement;
    public Integer repulsionMode; // 0,1,2

    // Partition spatiale
    public Double lambda; // pr le clustering
    public Integer spatialCells;
    public Boolean kmeansEnabled;

    // Filtres (visuels)
    public Integer minimumDegree;
    public Double minEdgeWeight;

    // Apparence
    public Double initialNodeSize;
    public Double degreeFactor;
}

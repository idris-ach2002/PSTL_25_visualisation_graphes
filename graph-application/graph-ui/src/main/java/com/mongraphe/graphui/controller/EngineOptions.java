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
    public Integer spatialCells;
    public Boolean kmeansEnabled;
    public Double epsilon; // convergence k‑means

    // Filtres (visuels)
    public Integer minimumDegree;
    public Double minEdgeWeight;

    // Apparence
    public Double initialNodeSize;
    public Double degreeFactor;
}

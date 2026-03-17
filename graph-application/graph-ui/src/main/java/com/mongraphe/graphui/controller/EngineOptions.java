package com.mongraphe.graphui.controller;

import com.mongraphe.graphui.model.GraphData;

public final class EngineOptions {
    public Double degreeFactor;
    public Double initialNodeSize;
    public Integer upScale;
    public Double stabilizedThreshold;
    public Double attractionThreshold;
    public Integer clusterUpdateFrequency;
    public Double newFriction;
    public Double attractionCoefficient;
    public Double repulsionThreshold;
    public Double newAmortissement;
    public Integer nbClusters;
    public Integer minimumDegree;
    public Double minEdgeWeight;
    public GraphData.RepulsionMode repulsionMode;
}

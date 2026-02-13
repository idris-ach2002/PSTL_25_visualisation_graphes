package com.mongraphe.graphui.interfaces;

import com.mongraphe.graphui.Vertex;

public interface InteractionModel {
    Vertex findVertexAt(float wx, float wy);
    void setSelectedVertex(Vertex v);
    void moveVertex(int vertexId, float x, float y);
    void panCamera(int dx, int dy);
    void zoomCamera(int sx, int sy, float rotation);
    boolean isSimulationRunning();
    void startSimulation();
    void stopSimulation();
    float screenToWorldX(int sx);
    float screenToWorldY(int sy);
}

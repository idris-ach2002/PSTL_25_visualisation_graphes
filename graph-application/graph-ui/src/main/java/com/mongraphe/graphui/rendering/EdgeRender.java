package com.mongraphe.graphui.rendering;

import java.util.List;

import com.jogamp.opengl.GL4;
import com.mongraphe.graphui.Community;
import com.mongraphe.graphui.Edge;
import com.mongraphe.graphui.Vertex;

public class EdgeRender {

    public List<Edge> edges;

    private float[] edgePoints;
    private float[] edgeSizes;
    private float[] edgeColors;
    private float[] edgeVisibility;

    private int edgeBuffer;
    private int edgeColorBuffer;
    private int edgeSizeBuffer;
    private int edgeVisibilityBuffer;

    private static final double CORRELATION_THRESHOLD = 0.1;

    public void prepareEdgeRenderData() {
        for (int i = 0; i < edges.size(); i++) {
            Edge currentEdge = edges.get(i);

            if (currentEdge.getWeight() > CORRELATION_THRESHOLD) {
                Vertex startVertex = currentEdge.getStart();
                Vertex endVertex = currentEdge.getEnd();
                Community startCommunity = startVertex.getCommunity();
                Community endCommunity = endVertex.getCommunity();

                // Points de début de l'arête
                edgePoints[i * 4] = (float) startVertex.getX();
                edgePoints[i * 4 + 1] = (float) startVertex.getY();

                // Points de fin de l'arête
                edgePoints[i * 4 + 2] = (float) endVertex.getX();
                edgePoints[i * 4 + 3] = (float) endVertex.getY();

                // Couleur moyenne entre les deux communautés
                float r = (startCommunity.getR() + endCommunity.getR()) / 2.0f;
                float g = (startCommunity.getG() + endCommunity.getG()) / 2.0f;
                float b = (startCommunity.getB() + endCommunity.getB()) / 2.0f;

                // Couleur pour les deux points de l'arête
                edgeColors[i * 6] = r; // Début R
                edgeColors[i * 6 + 1] = g; // Début G
                edgeColors[i * 6 + 2] = b; // Début B
                edgeColors[i * 6 + 3] = r; // Fin R
                edgeColors[i * 6 + 4] = g; // Fin G
                edgeColors[i * 6 + 5] = b; // Fin B

                // Taille pour les deux points
                float size = (float) currentEdge.getWeight();
                edgeSizes[i * 2] = size;
                edgeSizes[i * 2 + 1] = size;

                // Visibilité - 0.0 si supprimé, 1.0 si visible
                boolean isHidden = startVertex.isDeleted() ||
                        endVertex.isDeleted() ||
                        !startVertex.isVisible() ||
                        !endVertex.isVisible();
                float visibility = isHidden ? 0.0f : 1.0f;
                edgeVisibility[i * 2] = visibility;
                edgeVisibility[i * 2 + 1] = visibility;
            }
        }
    }

    public void createEdgeBuffers(GL4 gl) {
        int[] buffers = new int[4];
        gl.glGenBuffers(4, buffers, 0);

        edgeBuffer = buffers[0];
        edgeColorBuffer = buffers[1];
        edgeSizeBuffer = buffers[2];
        edgeVisibilityBuffer = buffers[3];

        // La taille des buffers doit correspondre à la quantité de données
        gl.glBindBuffer(GL4.GL_ARRAY_BUFFER, edgeBuffer);
        gl.glBufferData(GL4.GL_ARRAY_BUFFER, (long) edgePoints.length * Float.BYTES, null, GL4.GL_DYNAMIC_DRAW);

        gl.glBindBuffer(GL4.GL_ARRAY_BUFFER, edgeColorBuffer);
        gl.glBufferData(GL4.GL_ARRAY_BUFFER, (long) edgeColors.length * Float.BYTES, null, GL4.GL_DYNAMIC_DRAW);

        gl.glBindBuffer(GL4.GL_ARRAY_BUFFER, edgeSizeBuffer);
        gl.glBufferData(GL4.GL_ARRAY_BUFFER, (long) edgeSizes.length * Float.BYTES, null, GL4.GL_DYNAMIC_DRAW);

        gl.glBindBuffer(GL4.GL_ARRAY_BUFFER, edgeVisibilityBuffer);
        gl.glBufferData(GL4.GL_ARRAY_BUFFER, (long) edgeVisibility.length * Float.BYTES, null, GL4.GL_DYNAMIC_DRAW);
    }

}

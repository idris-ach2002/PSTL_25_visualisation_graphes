package com.mongraphe.graphui.rendering;

import java.nio.FloatBuffer;
import com.jogamp.opengl.GL4;
import com.mongraphe.graphui.model.Edge;
import com.mongraphe.graphui.model.GraphModel;

public class EdgeGpuBuffer {

    private float[] positions;
    private float[] colors;
    private float[] sizes;
    private float[] visibility;

    private int posVbo;
    private int colorVbo;
    private int sizeVbo;
    private int visVbo;

    private int vertexCount;

    public void init(GL4 gl) {
        int[] b = new int[4];
        gl.glGenBuffers(4, b, 0);
        posVbo = b[0];
        colorVbo = b[1];
        sizeVbo = b[2];
        visVbo = b[3];
    }

    public void update(GraphModel model, float[] positionBuffer) {
        int edgeCount = model.edges().size();
        vertexCount = edgeCount * 2;

        positions = new float[edgeCount * 4];
        colors = new float[edgeCount * 6];
        sizes = new float[edgeCount * 2];
        visibility = new float[edgeCount * 2];

        int p = 0, c = 0, s = 0, v = 0;

        for (Edge e : model.edges()) {
            int startId = e.getStart().getId();
            int endId = e.getEnd().getId();

            float x1 = (positionBuffer != null && startId * 2 + 1 < positionBuffer.length) ? positionBuffer[startId * 2]
                    : 0f;
            float y1 = (positionBuffer != null && startId * 2 + 1 < positionBuffer.length)
                    ? positionBuffer[startId * 2 + 1]
                    : 0f;
            float x2 = (positionBuffer != null && endId * 2 + 1 < positionBuffer.length) ? positionBuffer[endId * 2]
                    : 0f;
            float y2 = (positionBuffer != null && endId * 2 + 1 < positionBuffer.length) ? positionBuffer[endId * 2 + 1]
                    : 0f;

            positions[p++] = x1;
            positions[p++] = y1;
            positions[p++] = x2;
            positions[p++] = y2;

            float r = e.getR();
            float g = e.getG();
            float b = e.getB();

            colors[c++] = r;
            colors[c++] = g;
            colors[c++] = b;
            colors[c++] = r;
            colors[c++] = g;
            colors[c++] = b;

            float w = (float) e.getWeight();
            sizes[s++] = w;
            sizes[s++] = w;

            float vis = e.isVisible() ? 1f : 0f;
            visibility[v++] = vis;
            visibility[v++] = vis;
        }
    }

    public void upload(GL4 gl) {
        upload(gl, posVbo, positions);
        upload(gl, colorVbo, colors);
        upload(gl, sizeVbo, sizes);
        upload(gl, visVbo, visibility);
    }

    private void upload(GL4 gl, int vbo, float[] data) {
        gl.glBindBuffer(GL4.GL_ARRAY_BUFFER, vbo);
        gl.glBufferData(GL4.GL_ARRAY_BUFFER, (long) data.length * Float.BYTES, FloatBuffer.wrap(data),
                GL4.GL_DYNAMIC_DRAW);
    }

    public void draw(GL4 gl) {
        gl.glEnableVertexAttribArray(0);
        gl.glBindBuffer(GL4.GL_ARRAY_BUFFER, posVbo);
        gl.glVertexAttribPointer(0, 2, GL4.GL_FLOAT, false, 0, 0);

        gl.glEnableVertexAttribArray(1);
        gl.glBindBuffer(GL4.GL_ARRAY_BUFFER, colorVbo);
        gl.glVertexAttribPointer(1, 3, GL4.GL_FLOAT, false, 0, 0);

        gl.glEnableVertexAttribArray(2);
        gl.glBindBuffer(GL4.GL_ARRAY_BUFFER, sizeVbo);
        gl.glVertexAttribPointer(2, 1, GL4.GL_FLOAT, false, 0, 0);

        gl.glEnableVertexAttribArray(3);
        gl.glBindBuffer(GL4.GL_ARRAY_BUFFER, visVbo);
        gl.glVertexAttribPointer(3, 1, GL4.GL_FLOAT, false, 0, 0);

        gl.glDrawArrays(GL4.GL_LINES, 0, vertexCount);
    }
}
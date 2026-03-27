package com.mongraphe.graphui.rendering;

import java.nio.FloatBuffer;

import com.jogamp.opengl.GL4;
import com.mongraphe.graphui.model.Community;
import com.mongraphe.graphui.model.GraphModel;
import com.mongraphe.graphui.model.Vertex;
import com.mongraphe.graphui.model.GraphModel.ColoringMode;

public final class VertexGpuBuffer {

    private float[] pos, col, size, vis;
    private int posVbo, colVbo, sizeVbo, visVbo;
    private int count;

    public void init(GL4 gl) {
        int[] b = new int[4];
        gl.glGenBuffers(4, b, 0);
        posVbo = b[0];
        sizeVbo = b[1];
        colVbo = b[2];
        visVbo = b[3];
    }

    public void update(GraphModel model, float[] positionBuffer) {
        count = model.vertices().size();
        pos = new float[count * 2];
        col = new float[count * 3];
        size = new float[count];
        vis = new float[count];

        final int selectedId = model.getSelectedVertexId();
        final ColoringMode mode = model.getColoringMode();
        final int maxDeg = Math.max(1, model.getMaxDegree());

        for (int i = 0; i < count; i++) {
            Vertex v = model.vertices().get(i);
            if (positionBuffer != null && i * 2 + 1 < positionBuffer.length) {
                pos[i * 2] = positionBuffer[i * 2];
                pos[i * 2 + 1] = positionBuffer[i * 2 + 1];
            } else {
                pos[i * 2] = 0f;
                pos[i * 2 + 1] = 0f;
            }

            size[i] = (float) v.getDiameter();

            boolean visibleVertex = !(v.isDeleted() || !v.isVisible());
            vis[i] = visibleVertex ? 1f : 0f;

            float r, g, b;
            if (v.getId() == selectedId && selectedId >= 0) {
                r = 1f;
                g = 1f;
                b = 1f;
            } else if (mode == ColoringMode.UNIFORM) {
                r = model.getUniformNodeR();
                g = model.getUniformNodeG();
                b = model.getUniformNodeB();
            } else if (mode == ColoringMode.DEGREE) {
                float t = (float) v.getDegree() / maxDeg;
                float base = 0.15f;
                float intensity = base + (1f - base) * t;
                r = intensity;
                g = intensity;
                b = intensity;
            } else {
                Community c = v.getCommunity();
                if (c != null) {
                    r = c.getR();
                    g = c.getG();
                    b = c.getB();
                } else {
                    r = 0.6f;
                    g = 0.6f;
                    b = 0.6f;
                }
            }

            col[i * 3] = r;
            col[i * 3 + 1] = g;
            col[i * 3 + 2] = b;
        }
    }

    public void upload(GL4 gl) {
        upload(gl, posVbo, pos);
        upload(gl, colVbo, col);
        upload(gl, sizeVbo, size);
        upload(gl, visVbo, vis);
    }

    private void upload(GL4 gl, int vbo, float[] data) {
        gl.glBindBuffer(GL4.GL_ARRAY_BUFFER, vbo);
        gl.glBufferData(GL4.GL_ARRAY_BUFFER, (long) data.length * 4, FloatBuffer.wrap(data), GL4.GL_DYNAMIC_DRAW);
    }

    public void draw(GL4 gl) {
        gl.glEnableVertexAttribArray(0);
        gl.glBindBuffer(GL4.GL_ARRAY_BUFFER, posVbo);
        gl.glVertexAttribPointer(0, 2, GL4.GL_FLOAT, false, 0, 0);

        gl.glEnableVertexAttribArray(1);
        gl.glBindBuffer(GL4.GL_ARRAY_BUFFER, sizeVbo);
        gl.glVertexAttribPointer(1, 1, GL4.GL_FLOAT, false, 0, 0);

        gl.glEnableVertexAttribArray(2);
        gl.glBindBuffer(GL4.GL_ARRAY_BUFFER, colVbo);
        gl.glVertexAttribPointer(2, 3, GL4.GL_FLOAT, false, 0, 0);

        gl.glEnableVertexAttribArray(3);
        gl.glBindBuffer(GL4.GL_ARRAY_BUFFER, visVbo);
        gl.glVertexAttribPointer(3, 1, GL4.GL_FLOAT, false, 0, 0);

        gl.glDrawArrays(GL4.GL_POINTS, 0, count);
    }
}
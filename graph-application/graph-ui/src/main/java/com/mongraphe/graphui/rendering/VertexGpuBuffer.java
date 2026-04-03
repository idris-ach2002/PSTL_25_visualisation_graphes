package com.mongraphe.graphui.rendering;

import java.nio.FloatBuffer;
import java.util.List;

import com.jogamp.opengl.GL4;
import com.mongraphe.graphui.model.Community;
import com.mongraphe.graphui.model.Vertex;
import com.mongraphe.graphui.model.GraphModel.ColoringMode;

public final class VertexGpuBuffer {

    private float[] pos, col, size, vis;

    private int posVbo, colVbo, sizeVbo, visVbo;

    private int count;
    private int capacity;

    private static final int INITIAL_CAPACITY = 1024;

    public void init(GL4 gl) {
        int[] b = new int[4];
        gl.glGenBuffers(4, b, 0);

        posVbo = b[0];
        sizeVbo = b[1];
        colVbo = b[2];
        visVbo = b[3];

        capacity = INITIAL_CAPACITY;

        pos = new float[capacity * 2];
        col = new float[capacity * 3];
        size = new float[capacity];
        vis = new float[capacity];

        allocateGpu(gl, capacity);
    }

    private void allocateGpu(GL4 gl, int cap) {
        gl.glBindBuffer(GL4.GL_ARRAY_BUFFER, posVbo);
        gl.glBufferData(GL4.GL_ARRAY_BUFFER, cap * 2L * Float.BYTES, null, GL4.GL_DYNAMIC_DRAW);

        gl.glBindBuffer(GL4.GL_ARRAY_BUFFER, colVbo);
        gl.glBufferData(GL4.GL_ARRAY_BUFFER, cap * 3L * Float.BYTES, null, GL4.GL_DYNAMIC_DRAW);

        gl.glBindBuffer(GL4.GL_ARRAY_BUFFER, sizeVbo);
        gl.glBufferData(GL4.GL_ARRAY_BUFFER, cap * Float.BYTES, null, GL4.GL_DYNAMIC_DRAW);

        gl.glBindBuffer(GL4.GL_ARRAY_BUFFER, visVbo);
        gl.glBufferData(GL4.GL_ARRAY_BUFFER, cap * Float.BYTES, null, GL4.GL_DYNAMIC_DRAW);
    }

    private void ensureCapacity(GL4 gl, int vertexCount) {
        if (vertexCount <= capacity)
            return;

        int newCap = capacity;
        while (newCap < vertexCount)
            newCap *= 2;

        capacity = newCap;

        pos = new float[capacity * 2];
        col = new float[capacity * 3];
        size = new float[capacity];
        vis = new float[capacity];

        allocateGpu(gl, capacity);
    }

    public void update(
            GL4 gl,
            List<Vertex> vertices,
            int selectedVertexId,
            int maxDegree,
            ColoringMode mode,
            float uniformR, float uniformG, float uniformB) {

        if (vertices == null || vertices.isEmpty()) {
            count = 0;
            return;
        }

        count = vertices.size();
        ensureCapacity(gl, count);
        int maxDeg = Math.max(1, maxDegree);
        int i = 0;
        for (Vertex v : vertices) {
            if (v == null) {
                i++;
                continue;
            }

            int p = i * 2;
            int c = i * 3;

            pos[p] = (float) v.getX();
            pos[p + 1] = (float) v.getY();

            size[i] = (float) v.getDiameter();
            vis[i] = (v.isDeleted() || !v.isVisible()) ? 0f : 1f;

            float r, g, b;

            if (v.getId() == selectedVertexId) {
                r = g = b = 1f;
            } else if (mode == ColoringMode.UNIFORM) {
                r = uniformR;
                g = uniformG;
                b = uniformB;
            } else if (mode == ColoringMode.DEGREE) {
                float t = (float) v.getDegree() / maxDeg;
                float base = 0.15f;
                float intensity = base + (1f - base) * t;
                r = g = b = intensity;
            } else {
                Community com = v.getCommunity();
                if (com != null) {
                    r = com.getR();
                    g = com.getG();
                    b = com.getB();
                } else {
                    r = g = b = 0.6f;
                }
            }

            col[c] = r;
            col[c + 1] = g;
            col[c + 2] = b;
            i++;
        }
    }

    public void upload(GL4 gl) {
        upload(gl, posVbo, pos, count * 2);
        upload(gl, colVbo, col, count * 3);
        upload(gl, sizeVbo, size, count);
        upload(gl, visVbo, vis, count);
    }

    private void upload(GL4 gl, int vbo, float[] data, int elements) {
        gl.glBindBuffer(GL4.GL_ARRAY_BUFFER, vbo);
        gl.glBufferSubData(
                GL4.GL_ARRAY_BUFFER,
                0,
                elements * (long) Float.BYTES,
                FloatBuffer.wrap(data, 0, elements));
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
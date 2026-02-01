package com.mongraphe.graphui.rendering;

import java.nio.FloatBuffer;
import com.jogamp.opengl.GL4;
import com.mongraphe.graphui.Vertex;

public class VertexGpuBuffer {

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

    public void update(GraphScene scene) {
        count = scene.vertices().size();
        pos = new float[count * 2];
        col = new float[count * 3];
        size = new float[count];
        vis = new float[count];

        for (int i = 0; i < count; i++) {
            Vertex v = scene.vertices().get(i);
            pos[i * 2] = (float) v.getX();
            pos[i * 2 + 1] = (float) v.getY();
            col[i * 3] = v.getCommunity().getR();
            col[i * 3 + 1] = v.getCommunity().getG();
            col[i * 3 + 2] = v.getCommunity().getB();
            size[i] = (float) v.getDiameter();
            vis[i] = (v.isDeleted() || !v.isVisible()) ? 0f : 1f;
        }
    }

    public void upload(GL4 gl) {
        upload(gl, posVbo, pos);
        upload(gl, colVbo, col);
        upload(gl, sizeVbo, size);
        upload(gl, visVbo, vis);
    }

    private void upload(GL4 gl, int vbo, float[] d) {
        gl.glBindBuffer(GL4.GL_ARRAY_BUFFER, vbo);
        gl.glBufferData(GL4.GL_ARRAY_BUFFER, (long) d.length * 4, FloatBuffer.wrap(d), GL4.GL_DYNAMIC_DRAW);
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
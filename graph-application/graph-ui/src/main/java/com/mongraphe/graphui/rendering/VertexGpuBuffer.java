package com.mongraphe.graphui.rendering;

import java.nio.FloatBuffer;

import com.jogamp.opengl.GL4;

public class VertexGpuBuffer {

    private float[] positions, colors, sizes, visibility;
    private int posVbo, colorVbo, sizeVbo, visVbo;
    private int vertexCount;

    public void init(GL4 gl) {
        int[] b = new int[4];
        gl.glGenBuffers(4, b, 0);
        posVbo = b[0]; sizeVbo = b[1]; colorVbo = b[2]; visVbo = b[3];
    }

    public void updateCpu(GraphScene scene) {
        vertexCount = scene.vertices().size();
    }

    public void upload(GL4 gl) {
        upload(gl, posVbo, positions);
        upload(gl, sizeVbo, sizes);
        upload(gl, colorVbo, colors);
        upload(gl, visVbo, visibility);
    }

    private void upload(GL4 gl, int vbo, float[] data) {
        gl.glBindBuffer(GL4.GL_ARRAY_BUFFER, vbo);
        gl.glBufferData(GL4.GL_ARRAY_BUFFER, (long)data.length * 4,
                FloatBuffer.wrap(data), GL4.GL_DYNAMIC_DRAW);
    }

    public void draw(GL4 gl) {
        gl.glEnableVertexAttribArray(0);
        gl.glBindBuffer(GL4.GL_ARRAY_BUFFER, posVbo);
        gl.glVertexAttribPointer(0, 2, GL4.GL_FLOAT, false, 0, 0);

        gl.glEnableVertexAttribArray(1);
        gl.glBindBuffer(GL4.GL_ARRAY_BUFFER, sizeVbo);
        gl.glVertexAttribPointer(1, 1, GL4.GL_FLOAT, false, 0, 0);

        gl.glEnableVertexAttribArray(2);
        gl.glBindBuffer(GL4.GL_ARRAY_BUFFER, colorVbo);
        gl.glVertexAttribPointer(2, 3, GL4.GL_FLOAT, false, 0, 0);

        gl.glEnableVertexAttribArray(3);
        gl.glBindBuffer(GL4.GL_ARRAY_BUFFER, visVbo);
        gl.glVertexAttribPointer(3, 1, GL4.GL_FLOAT, false, 0, 0);

        gl.glDrawArrays(GL4.GL_POINTS, 0, vertexCount);
    }
}
package com.mongraphe.graphui.rendering;

import java.nio.FloatBuffer;
import com.jogamp.opengl.GL4;

public abstract class AbstractGpuBuffer {

    protected void upload(GL4 gl, int vbo, float[] data) {
        gl.glBindBuffer(GL4.GL_ARRAY_BUFFER, vbo);
        gl.glBufferData(GL4.GL_ARRAY_BUFFER,
                (long) data.length * Float.BYTES,
                FloatBuffer.wrap(data),
                GL4.GL_DYNAMIC_DRAW);
    }
}

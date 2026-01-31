package com.mongraphe.graphui.rendering;

import java.nio.FloatBuffer;

public class Camera2D {
    private FloatBuffer projection = FloatBuffer.allocate(16);

    public void resize(int width, int height) {
        float left = -width / 2f;
        float right = width / 2f;
        float bottom = -height / 2f;
        float top = height / 2f;

        float[] ortho = {
            2f/(right-left),0,0,0,
            0,2f/(top-bottom),0,0,
            0,0,-1,0,
            -(right+left)/(right-left),
            -(top+bottom)/(top-bottom),
            0,1
        };
        projection = FloatBuffer.wrap(ortho);
    }

    public FloatBuffer getProjection() { return projection; }
}
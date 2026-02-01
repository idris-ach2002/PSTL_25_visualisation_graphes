package com.mongraphe.graphui.rendering;

import java.nio.FloatBuffer;

public class Camera2D {

    private float zoom = 1f;
    private float offsetX = 0;
    private float offsetY = 0;
    private FloatBuffer projection;

    public void resize(int w, int h) {
        float hw = w / 2f;
        float hh = h / 2f;

        float left = -hw / zoom + offsetX;
        float right = hw / zoom + offsetX;
        float bottom = -hh / zoom + offsetY;
        float top = hh / zoom + offsetY;

        float[] ortho = {
                2f / (right - left), 0, 0, 0,
                0, 2f / (top - bottom), 0, 0,
                0, 0, -1, 0,
                -(right + left) / (right - left),
                -(top + bottom) / (top - bottom),
                0, 1
        };
        projection = FloatBuffer.wrap(ortho);
    }

    public void zoomAt(int mx, int my, float amount) {
        zoom *= (amount > 0) ? 1.1f : 0.9f;
    }

    public void panFromDrag(int dx, int dy) {
        offsetX -= dx / zoom;
        offsetY += dy / zoom;
    }

    public double screenToWorldX(int x) {
        return x / zoom + offsetX;
    }

    public double screenToWorldY(int y) {
        return y / zoom + offsetY;
    }

    public FloatBuffer getProjection() {
        return projection;
    }
}
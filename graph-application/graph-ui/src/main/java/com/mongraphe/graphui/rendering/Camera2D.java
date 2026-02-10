package com.mongraphe.graphui.rendering;

import java.nio.FloatBuffer;

public final class Camera2D {

    private float zoom = 1f;
    private float offsetX;
    private float offsetY;

    private int width = 1;
    private int height = 1;

    private final FloatBuffer projection = FloatBuffer.allocate(16);

    public void resize(int w, int h) {
        width = Math.max(1, w);
        height = Math.max(1, h);
        updateProjection();
    }

    public void zoomAt(float amount) {
        zoom *= (amount > 0) ? 1.1f : 0.9f;
        updateProjection();
    }

    public void zoomAt(float screenX, float screenY, float amount) {

        float worldX = screenToWorldX(screenX);
        float worldY = screenToWorldY(screenY);

        zoom *= (amount > 0) ? 1.1f : 0.9f;

        offsetX = worldX - (screenX - width / 2f) / zoom;
        offsetY = worldY - (height / 2f - screenY) / zoom;

        updateProjection();
    }

    public void pan(float dx, float dy) {
        offsetX -= dx / zoom;
        offsetY += dy / zoom;
        updateProjection();
    }

    /**
     * Réinitialise le cadrage (zoom/pan).
     */
    public void reset() {
        zoom = 1f;
        offsetX = 0f;
        offsetY = 0f;
        updateProjection();
    }

    private void updateProjection() {

        float hw = width / 2f / zoom;
        float hh = height / 2f / zoom;

        float left = -hw + offsetX;
        float right = hw + offsetX;
        float bottom = -hh + offsetY;
        float top = hh + offsetY;

        float[] ortho = {
                2f / (right - left), 0, 0, 0,
                0, 2f / (top - bottom), 0, 0,
                0, 0, -1, 0,
                -(right + left) / (right - left),
                -(top + bottom) / (top - bottom),
                0, 1
        };

        projection.clear();
        projection.put(ortho).flip();
    }

    public FloatBuffer getProjection() {
        return projection;
    }

    /**
     * Zoom courant (1.0 = neutre). Utile pour la sélection sous zoom.
     */
    public float getZoom() {
        return zoom;
    }

    public float screenToWorldX(float screenX) {
        return (screenX - width / 2f) / zoom + offsetX;
    }

    public float screenToWorldY(float screenY) {
        return (height / 2f - screenY) / zoom + offsetY;
    }
}

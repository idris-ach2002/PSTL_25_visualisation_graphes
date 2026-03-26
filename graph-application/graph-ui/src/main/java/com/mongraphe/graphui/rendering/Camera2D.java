package com.mongraphe.graphui.rendering;

import java.nio.FloatBuffer;

import com.jogamp.common.nio.Buffers;

public final class Camera2D {

    private float zoom = 1f;
    private float offsetX;
    private float offsetY;

    private int width = 1;
    private int height = 1;

    private final FloatBuffer bufferA = Buffers.newDirectFloatBuffer(16);
    private final FloatBuffer bufferB = Buffers.newDirectFloatBuffer(16);

    private volatile FloatBuffer projection = bufferA;
    private FloatBuffer writeBuffer = bufferB;

    public void resize(int w, int h) {
        width = Math.max(1, w);
        height = Math.max(1, h);
        updateProjection();
    }

    public void pan(float dx, float dy) {
        offsetX -= dx / zoom;
        offsetY += dy / zoom;
        updateProjection();
    }

    public void zoomAt(float screenX, float screenY, float factor) {
        float worldX = screenToWorldX(screenX);
        float worldY = screenToWorldY(screenY);

        zoom *= factor;

        offsetX = worldX - (screenX - width / 2f) / zoom;
        offsetY = worldY - (height / 2f - screenY) / zoom;

        updateProjection();
    }

    public void zoomIn() {
        zoomAt(width / 2f, height / 2f, 1.1f);
    }

    public void zoomOut() {
        zoomAt(width / 2f, height / 2f, 0.9f);
    }

    public void setZoom(float zoom) {
        this.zoom = zoom;
        updateProjection();
    }

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

        writeBuffer.clear();
        writeBuffer.put(ortho).flip();

        FloatBuffer oldRead = projection;
        projection = writeBuffer;
        writeBuffer = oldRead;
    }

    public FloatBuffer getProjection() {
        return projection;
    }

    public float screenToWorldX(float screenX) {
        return (screenX - width / 2f) / zoom + offsetX;
    }

    public float screenToWorldY(float screenY) {
        return (height / 2f - screenY) / zoom + offsetY;
    }

    public float getZoom() {
        return zoom;
    }
}
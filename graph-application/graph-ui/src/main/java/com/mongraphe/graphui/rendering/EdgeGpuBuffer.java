package com.mongraphe.graphui.rendering;

import java.nio.FloatBuffer;

import com.jogamp.opengl.GL4;
import com.mongraphe.graphui.model.Edge;
import com.mongraphe.graphui.model.GraphModel;

public class EdgeGpuBuffer {

    private static final float LOOP_OFFSET = 22f;
    private static final int COMPONENTS_POS = 2;
    private static final int COMPONENTS_COLOR = 3;

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

    public void update(GraphModel model, float[] positionBuffer, GraphRenderOptions options) {
        GraphRenderOptions effectiveOptions = options == null ? GraphRenderOptions.straight() : options;
        if (effectiveOptions.edgeStyle() == GraphRenderOptions.EdgeStyle.CURVED_PARABOLIC) {
            updateCurved(model, positionBuffer, effectiveOptions);
        } else {
            updateStraight(model, positionBuffer);
        }
    }

    private void updateStraight(GraphModel model, float[] positionBuffer) {
        int edgeCount = model.edges().size();
        vertexCount = edgeCount * 2;

        positions = new float[edgeCount * 4];
        colors = new float[edgeCount * 6];
        sizes = new float[edgeCount * 2];
        visibility = new float[edgeCount * 2];

        int p = 0, c = 0, s = 0, v = 0;

        for (Edge e : model.edges()) {
            float x1 = getX(positionBuffer, e.getStartId());
            float y1 = getY(positionBuffer, e.getStartId());
            float x2 = getX(positionBuffer, e.getEndId());
            float y2 = getY(positionBuffer, e.getEndId());

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

    private void updateCurved(GraphModel model, float[] positionBuffer, GraphRenderOptions options) {
        int segments = Math.max(2, options.curveSegments());
        int edgeCount = model.edges().size();
        int lineSegmentCount = edgeCount * segments;
        vertexCount = lineSegmentCount * 2;

        positions = new float[lineSegmentCount * 2 * COMPONENTS_POS];
        colors = new float[lineSegmentCount * 2 * COMPONENTS_COLOR];
        sizes = new float[lineSegmentCount * 2];
        visibility = new float[lineSegmentCount * 2];

        int p = 0, c = 0, s = 0, v = 0;

        for (Edge e : model.edges()) {
            float x1 = getX(positionBuffer, e.getStartId());
            float y1 = getY(positionBuffer, e.getStartId());
            float x2 = getX(positionBuffer, e.getEndId());
            float y2 = getY(positionBuffer, e.getEndId());

            float[] control = computeControlPoint(e, x1, y1, x2, y2, options);
            float[] previous = new float[] { x1, y1 };
            float vis = e.isVisible() ? 1f : 0f;
            float r = e.getR();
            float g = e.getG();
            float b = e.getB();
            float w = Math.max(1f, (float) e.getWeight());

            for (int i = 1; i <= segments; i++) {
                float t = (float) i / (float) segments;
                float[] current = evalQuadraticBezier(x1, y1, control[0], control[1], x2, y2, t);

                positions[p++] = previous[0];
                positions[p++] = previous[1];
                positions[p++] = current[0];
                positions[p++] = current[1];

                colors[c++] = r;
                colors[c++] = g;
                colors[c++] = b;
                colors[c++] = r;
                colors[c++] = g;
                colors[c++] = b;

                sizes[s++] = w;
                sizes[s++] = w;

                visibility[v++] = vis;
                visibility[v++] = vis;

                previous = current;
            }
        }
    }

    private float getX(float[] positionBuffer, int vertexId) {
        return (positionBuffer != null && vertexId * 2 + 1 < positionBuffer.length) ? positionBuffer[vertexId * 2] : 0f;
    }

    private float getY(float[] positionBuffer, int vertexId) {
        return (positionBuffer != null && vertexId * 2 + 1 < positionBuffer.length) ? positionBuffer[vertexId * 2 + 1]
                : 0f;
    }

    private float[] computeControlPoint(Edge edge, float x1, float y1, float x2, float y2, GraphRenderOptions options) {
        float dx = x2 - x1;
        float dy = y2 - y1;
        float mx = (x1 + x2) * 0.5f;
        float my = (y1 + y2) * 0.5f;
        float len = (float) Math.hypot(dx, dy);

        if (len < 0.0001f) {
            return new float[] { mx + LOOP_OFFSET, my + LOOP_OFFSET };
        }

        float nx = -dy / len;
        float ny = dx / len;
        float angleRad = (float) Math.toRadians(options.curveAngleDegrees());
        float offset = (float) Math.tan(angleRad) * len * 0.5f;
        if (options.maxCurveOffset() > 0f) {
            offset = Math.min(offset, options.maxCurveOffset());
        }

        float sign = 1f;
        if (options.alternateCurveDirection()) {
            int a = Math.min(edge.getStartId(), edge.getEndId());
            int b = Math.max(edge.getStartId(), edge.getEndId());
            int hash = (31 * a) ^ (17 * b);
            sign = (hash & 1) == 0 ? 1f : -1f;
        }

        return new float[] { mx + nx * offset * sign, my + ny * offset * sign };
    }

    private float[] evalQuadraticBezier(float x0, float y0, float cx, float cy, float x1, float y1, float t) {
        float u = 1f - t;
        float x = u * u * x0 + 2f * u * t * cx + t * t * x1;
        float y = u * u * y0 + 2f * u * t * cy + t * t * y1;
        return new float[] { x, y };
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

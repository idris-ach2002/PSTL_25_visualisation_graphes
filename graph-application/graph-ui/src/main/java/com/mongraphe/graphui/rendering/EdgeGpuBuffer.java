package com.mongraphe.graphui.rendering;

import java.nio.FloatBuffer;
import java.util.List;

import com.jogamp.opengl.GL4;
import com.mongraphe.graphui.model.Edge;

public class EdgeGpuBuffer {

    private static final float LOOP_OFFSET = 22f;

    private float[] positions;
    private float[] colors;
    private float[] sizes;
    private float[] visibility;
    private int capacity;

    private final float[] control = new float[2];
    private final float[] current = new float[2];
    private final float[] previous = new float[2];

    private int posVbo;
    private int colorVbo;
    private int sizeVbo;
    private int visVbo;

    private GL4 gl;

    private int vertexCount;

    public void init(GL4 gl) {
        this.gl = gl;

        int[] b = new int[4];
        gl.glGenBuffers(4, b, 0);

        posVbo = b[0];
        colorVbo = b[1];
        sizeVbo = b[2];
        visVbo = b[3];

        capacity = 0; // force première allocation
    }

    public void update(List<Edge> edges, float[] positionBuffer, GraphRenderOptions options) {
        GraphRenderOptions effectiveOptions = options == null ? GraphRenderOptions.straight() : options;
        if (effectiveOptions.edgeStyle() == GraphRenderOptions.EdgeStyle.CURVED_PARABOLIC) {
            updateCurved(edges, positionBuffer, effectiveOptions);
        } else {
            updateStraight(edges, positionBuffer);
        }
    }

    private void ensureCapacity(int vertexCount) {

        if (vertexCount <= capacity)
            return;

        capacity = Math.max(vertexCount, capacity * 2);

        positions = new float[capacity * 2];
        colors = new float[capacity * 3];
        sizes = new float[capacity];
        visibility = new float[capacity];

        // Réallocation GPU
        gl.glBindBuffer(GL4.GL_ARRAY_BUFFER, posVbo);
        gl.glBufferData(GL4.GL_ARRAY_BUFFER, (long) capacity * 2 * Float.BYTES, null, GL4.GL_DYNAMIC_DRAW);

        gl.glBindBuffer(GL4.GL_ARRAY_BUFFER, colorVbo);
        gl.glBufferData(GL4.GL_ARRAY_BUFFER, (long) capacity * 3 * Float.BYTES, null, GL4.GL_DYNAMIC_DRAW);

        gl.glBindBuffer(GL4.GL_ARRAY_BUFFER, sizeVbo);
        gl.glBufferData(GL4.GL_ARRAY_BUFFER, (long) capacity * Float.BYTES, null, GL4.GL_DYNAMIC_DRAW);

        gl.glBindBuffer(GL4.GL_ARRAY_BUFFER, visVbo);
        gl.glBufferData(GL4.GL_ARRAY_BUFFER, (long) capacity * Float.BYTES, null, GL4.GL_DYNAMIC_DRAW);
    }

    private void updateStraight(List<Edge> edges, float[] positionBuffer) {
        int edgeCount = edges.size();
        vertexCount = edgeCount * 2;

        ensureCapacity(vertexCount);

        int p = 0, c = 0, s = 0, v = 0;

        for (Edge e : edges) {
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

    private void updateCurved(List<Edge> edges, float[] positionBuffer, GraphRenderOptions options) {

        int segments = Math.max(2, options.curveSegments());
        int edgeCount = edges.size();
        int lineSegmentCount = edgeCount * segments;

        vertexCount = lineSegmentCount * 2;

        ensureCapacity(vertexCount);

        int p = 0, c = 0, s = 0, v = 0;

        for (Edge e : edges) {

            float x1 = getX(positionBuffer, e.getStartId());
            float y1 = getY(positionBuffer, e.getStartId());
            float x2 = getX(positionBuffer, e.getEndId());
            float y2 = getY(positionBuffer, e.getEndId());

            computeControlPoint(e, x1, y1, x2, y2, options, control);

            previous[0] = x1;
            previous[1] = y1;

            float vis = e.isVisible() ? 1f : 0f;
            float r = e.getR();
            float g = e.getG();
            float b = e.getB();
            float w = Math.max(1f, (float) e.getWeight());

            for (int i = 1; i <= segments; i++) {

                float t = (float) i / (float) segments;

                evalQuadraticBezier(x1, y1, control[0], control[1], x2, y2, t, current);

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

                previous[0] = current[0];
                previous[1] = current[1];
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

    private void computeControlPoint(
            Edge edge,
            float x1, float y1,
            float x2, float y2,
            GraphRenderOptions options,
            float[] out) {

        float dx = x2 - x1;
        float dy = y2 - y1;

        float mx = (x1 + x2) * 0.5f;
        float my = (y1 + y2) * 0.5f;

        float len = (float) Math.hypot(dx, dy);

        if (len < 0.0001f) {
            out[0] = mx + LOOP_OFFSET;
            out[1] = my + LOOP_OFFSET;
            return;
        }

        float nx = -dy / len;
        float ny = dx / len;

        float angleRad = (float) Math.toRadians(options.curveAngleDegrees());
        float offset = (float) Math.tan(angleRad) * len * 0.5f;

        if (options.maxCurveOffset() > 0f)
            offset = Math.min(offset, options.maxCurveOffset());

        float sign = 1f;

        if (options.alternateCurveDirection()) {

            int a = Math.min(edge.getStartId(), edge.getEndId());
            int b = Math.max(edge.getStartId(), edge.getEndId());

            int hash = (31 * a) ^ (17 * b);

            sign = (hash & 1) == 0 ? 1f : -1f;
        }

        out[0] = mx + nx * offset * sign;
        out[1] = my + ny * offset * sign;
    }

    private void evalQuadraticBezier(
            float x0, float y0,
            float cx, float cy,
            float x1, float y1,
            float t,
            float[] out) {

        float u = 1f - t;

        out[0] = u * u * x0 + 2f * u * t * cx + t * t * x1;
        out[1] = u * u * y0 + 2f * u * t * cy + t * t * y1;
    }

    public void upload(GL4 gl) {
        upload(gl, posVbo, positions, vertexCount * 2);
        upload(gl, colorVbo, colors, vertexCount * 3);
        upload(gl, sizeVbo, sizes, vertexCount);
        upload(gl, visVbo, visibility, vertexCount);
    }

    private void upload(GL4 gl, int vbo, float[] data, int elements) {

        gl.glBindBuffer(GL4.GL_ARRAY_BUFFER, vbo);

        gl.glBufferSubData(
                GL4.GL_ARRAY_BUFFER,
                0,
                (long) elements * Float.BYTES,
                FloatBuffer.wrap(data, 0, elements));
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

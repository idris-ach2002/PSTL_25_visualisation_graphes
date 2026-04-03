package com.mongraphe.graphui.rendering;

import java.nio.FloatBuffer;
import java.util.List;

import com.jogamp.opengl.GL4;
import com.mongraphe.graphui.model.Edge;
import com.mongraphe.graphui.model.Vertex;

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

    private int currentGpuCapacityPos = 0;
    private int currentGpuCapacityCol = 0;
    private int currentGpuCapacitySize = 0;
    private int currentGpuCapacityVis = 0;

    private int posVbo, colorVbo, sizeVbo, visVbo;
    private int vertexCount;

    public void init(GL4 gl) {
        int[] b = new int[4];
        gl.glGenBuffers(4, b, 0);
        posVbo = b[0];
        colorVbo = b[1];
        sizeVbo = b[2];
        visVbo = b[3];
        capacity = 0;
    }

    public void update(List<Edge> edges, List<Vertex> vertices, GraphRenderOptions options) {
        if (edges == null || vertices == null || edges.isEmpty()) {
            vertexCount = 0;
            ensureCapacity(1);
            return;
        }

        GraphRenderOptions effectiveOptions = (options == null) ? GraphRenderOptions.straight() : options;

        if (effectiveOptions.edgeStyle() == GraphRenderOptions.EdgeStyle.CURVED_PARABOLIC) {
            updateCurved(edges, vertices, effectiveOptions);
        } else {
            updateStraight(edges, vertices);
        }
    }

    private void ensureCapacity(int neededVertices) {
        if (neededVertices <= capacity)
            return;
        int newCap = Math.max(neededVertices, capacity * 2);
        if (newCap == 0)
            newCap = 1;
        capacity = newCap;
        positions = new float[capacity * 2];
        colors = new float[capacity * 3];
        sizes = new float[capacity];
        visibility = new float[capacity];
    }

    private void updateStraight(List<Edge> edges, List<Vertex> vertices) {
        int edgeCount = edges.size();
        vertexCount = edgeCount * 2;
        ensureCapacity(vertexCount);

        int p = 0, c = 0, s = 0, v = 0;
        for (Edge e : edges) {
            int id1 = e.getStartId();
            int id2 = e.getEndId();
            if (id1 < 0 || id1 >= vertices.size() || id2 < 0 || id2 >= vertices.size()) {
                positions[p++] = 0f;
                positions[p++] = 0f;
                positions[p++] = 0f;
                positions[p++] = 0f;
                colors[c++] = 0f;
                colors[c++] = 0f;
                colors[c++] = 0f;
                colors[c++] = 0f;
                colors[c++] = 0f;
                colors[c++] = 0f;
                sizes[s++] = 0f;
                sizes[s++] = 0f;
                visibility[v++] = 0f;
                visibility[v++] = 0f;
                continue;
            }

            Vertex v1 = vertices.get(id1);
            Vertex v2 = vertices.get(id2);
            float x1 = (float) v1.getX();
            float y1 = (float) v1.getY();
            float x2 = (float) v2.getX();
            float y2 = (float) v2.getY();

            if (Float.isNaN(x1) || Float.isNaN(y1) || Float.isNaN(x2) || Float.isNaN(y2)) {
                positions[p++] = 0f;
                positions[p++] = 0f;
                positions[p++] = 0f;
                positions[p++] = 0f;
            } else {
                positions[p++] = x1;
                positions[p++] = y1;
                positions[p++] = x2;
                positions[p++] = y2;
            }

            float r = e.getR(), g = e.getG(), b = e.getB();
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

    private void updateCurved(List<Edge> edges, List<Vertex> vertices, GraphRenderOptions options) {
        int segments = Math.max(2, options.curveSegments());
        int edgeCount = edges.size();
        int lineSegmentCount = edgeCount * segments;
        vertexCount = lineSegmentCount * 2;
        ensureCapacity(vertexCount);

        int p = 0, c = 0, s = 0, v = 0;
        for (Edge e : edges) {
            int id1 = e.getStartId();
            int id2 = e.getEndId();
            if (id1 < 0 || id1 >= vertices.size() || id2 < 0 || id2 >= vertices.size()) {
                for (int seg = 0; seg < segments; seg++) {
                    positions[p++] = 0f;
                    positions[p++] = 0f;
                    positions[p++] = 0f;
                    positions[p++] = 0f;
                    colors[c++] = 0f;
                    colors[c++] = 0f;
                    colors[c++] = 0f;
                    colors[c++] = 0f;
                    colors[c++] = 0f;
                    colors[c++] = 0f;
                    sizes[s++] = 0f;
                    sizes[s++] = 0f;
                    visibility[v++] = 0f;
                    visibility[v++] = 0f;
                }
                continue;
            }

            Vertex v1 = vertices.get(id1);
            Vertex v2 = vertices.get(id2);
            float x1 = (float) v1.getX();
            float y1 = (float) v1.getY();
            float x2 = (float) v2.getX();
            float y2 = (float) v2.getY();

            if (Float.isNaN(x1) || Float.isNaN(y1) || Float.isNaN(x2) || Float.isNaN(y2)) {
                for (int seg = 0; seg < segments; seg++) {
                    positions[p++] = 0f;
                    positions[p++] = 0f;
                    positions[p++] = 0f;
                    positions[p++] = 0f;
                    colors[c++] = 0f;
                    colors[c++] = 0f;
                    colors[c++] = 0f;
                    colors[c++] = 0f;
                    colors[c++] = 0f;
                    colors[c++] = 0f;
                    sizes[s++] = 0f;
                    sizes[s++] = 0f;
                    visibility[v++] = 0f;
                    visibility[v++] = 0f;
                }
                continue;
            }

            computeControlPoint(e, x1, y1, x2, y2, options, control);
            previous[0] = x1;
            previous[1] = y1;

            float vis = e.isVisible() ? 1f : 0f;
            float r = e.getR(), g = e.getG(), b = e.getB();
            float w = Math.max(1f, (float) e.getWeight());

            for (int seg = 1; seg <= segments; seg++) {
                float t = (float) seg / segments;
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

    private void computeControlPoint(Edge edge, float x1, float y1, float x2, float y2,
            GraphRenderOptions options, float[] out) {
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

    private void evalQuadraticBezier(float x0, float y0, float cx, float cy,
            float x1, float y1, float t, float[] out) {
        float u = 1f - t;
        out[0] = u * u * x0 + 2f * u * t * cx + t * t * x1;
        out[1] = u * u * y0 + 2f * u * t * cy + t * t * y1;
    }

    public void upload(GL4 gl) {
        if (vertexCount == 0)
            return;
        int needed = capacity;

        if (needed > currentGpuCapacityPos) {
            gl.glBindBuffer(GL4.GL_ARRAY_BUFFER, posVbo);
            gl.glBufferData(GL4.GL_ARRAY_BUFFER, needed * 2L * Float.BYTES, null, GL4.GL_DYNAMIC_DRAW);
            currentGpuCapacityPos = needed;
        }
        gl.glBindBuffer(GL4.GL_ARRAY_BUFFER, posVbo);
        gl.glBufferSubData(GL4.GL_ARRAY_BUFFER, 0, (long) vertexCount * 2 * Float.BYTES,
                FloatBuffer.wrap(positions, 0, vertexCount * 2));

        if (needed > currentGpuCapacityCol) {
            gl.glBindBuffer(GL4.GL_ARRAY_BUFFER, colorVbo);
            gl.glBufferData(GL4.GL_ARRAY_BUFFER, needed * 3L * Float.BYTES, null, GL4.GL_DYNAMIC_DRAW);
            currentGpuCapacityCol = needed;
        }
        gl.glBindBuffer(GL4.GL_ARRAY_BUFFER, colorVbo);
        gl.glBufferSubData(GL4.GL_ARRAY_BUFFER, 0, (long) vertexCount * 3 * Float.BYTES,
                FloatBuffer.wrap(colors, 0, vertexCount * 3));

        if (needed > currentGpuCapacitySize) {
            gl.glBindBuffer(GL4.GL_ARRAY_BUFFER, sizeVbo);
            gl.glBufferData(GL4.GL_ARRAY_BUFFER, needed * Float.BYTES, null, GL4.GL_DYNAMIC_DRAW);
            currentGpuCapacitySize = needed;
        }
        gl.glBindBuffer(GL4.GL_ARRAY_BUFFER, sizeVbo);
        gl.glBufferSubData(GL4.GL_ARRAY_BUFFER, 0, (long) vertexCount * Float.BYTES,
                FloatBuffer.wrap(sizes, 0, vertexCount));

        if (needed > currentGpuCapacityVis) {
            gl.glBindBuffer(GL4.GL_ARRAY_BUFFER, visVbo);
            gl.glBufferData(GL4.GL_ARRAY_BUFFER, needed * Float.BYTES, null, GL4.GL_DYNAMIC_DRAW);
            currentGpuCapacityVis = needed;
        }
        gl.glBindBuffer(GL4.GL_ARRAY_BUFFER, visVbo);
        gl.glBufferSubData(GL4.GL_ARRAY_BUFFER, 0, (long) vertexCount * Float.BYTES,
                FloatBuffer.wrap(visibility, 0, vertexCount));
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
package com.mongraphe.graphui.rendering;

import java.nio.FloatBuffer;
import java.nio.IntBuffer;
import java.util.concurrent.ConcurrentLinkedQueue;

import com.mongraphe.graphui.model.Edge;

import org.lwjgl.BufferUtils;

import static org.lwjgl.opengl.GL11.*;
import static org.lwjgl.opengl.GL15.*;
import static org.lwjgl.opengl.GL20.*;
import static org.lwjgl.opengl.GL30.*;

/**
 * VBO compact des arêtes visibles.
 *
 * <p>Les identifiants stockés dans ce buffer sont les indices du buffer compact
 * de positions visibles. Lorsque le renderer active le LOD, plusieurs sommets
 * globaux peuvent pointer vers le même indice compact. Les arêtes internes à un
 * même agrégat sont donc ignorées, car elles seraient invisibles ou très
 * bruitées à l'écran.</p>
 */
public final class EdgeGpuBuffer {

    private static final int INITIAL_CAPACITY = 4096;
    private static final int EDGE_ATTR_FLOATS = 4; // r, g, b, visible
    private static final int EDGE_ATTR_STRIDE_BYTES = EDGE_ATTR_FLOATS * Float.BYTES;

    private int[] endpointIds;
    private float[] attributes;
    private IntBuffer directEndpointIds;
    private FloatBuffer directAttributes;

    private int capacity;
    private int vertexCount;
    private int endpointGpuCapacity;
    private int attribGpuCapacity;

    private int vao;
    private int endpointVbo;
    private int attribVbo;

    private final EdgePairSet aggregatedPairs = new EdgePairSet();

    /** Initialise le VAO et les VBO d'arêtes. */
    public void init() {
        vao = glGenVertexArrays();
        endpointVbo = glGenBuffers();
        attribVbo = glGenBuffers();
        resizeCpuStorage(INITIAL_CAPACITY);
        configureVertexArray();
    }


    /**
     * Reconstruit les arêtes statiques en conservant directement les identifiants
     * natifs des sommets.
     *
     * <p>Cette méthode remplace le culling CPU par frame. Elle est appelée
     * seulement lorsque la structure du graphe, la visibilité ou les couleurs
     * changent. Le GPU clippe ensuite les segments hors écran à chaque frame.</p>
     *
     * @param edges arêtes du modèle
     * @param totalVertices nombre de sommets présents dans la texture de positions
     */
    public void updateAllStatic(ConcurrentLinkedQueue<Edge> edges, int totalVertices) {
        vertexCount = 0;
        if (edges == null || edges.isEmpty() || totalVertices <= 0) return;

        ensureCapacity(INITIAL_CAPACITY);
        for (Edge edge : edges) {
            if (edge == null || !edge.isVisible()) continue;
            int startId = edge.getStartId();
            int endId = edge.getEndId();
            if (startId < 0 || endId < 0 || startId >= totalVertices || endId >= totalVertices || startId == endId) {
                continue;
            }
            ensureCapacity(vertexCount + 2);
            writeEndpoint(startId, edge);
            writeEndpoint(endId, edge);
        }
    }

    /**
     * Reconstruit uniquement les arêtes dont les deux extrémités sont visibles.
     *
     * @param edges arêtes du modèle
     * @param globalToVisibleIndex table id global -> index compact visible, -1 si invisible
     * @param maxEdgesToDraw nombre maximal d'arêtes à envoyer au GPU pour cette frame
     * @param inspectionStride stride d'inspection stable. En mode LOD, le renderer
     *                         peut sauter une partie des arêtes avant le test de
     *                         visibilité afin d'éviter de parcourir un million
     *                         d'arêtes à chaque changement de positions.
     */
    public void updateVisibleStatic(ConcurrentLinkedQueue<Edge> edges, int[] globalToVisibleIndex, int maxEdgesToDraw, int inspectionStride) {
        vertexCount = 0;
        if (edges == null || edges.isEmpty() || globalToVisibleIndex == null || maxEdgesToDraw <= 0) return;

        int edgeIndex = 0;
        int keptEdges = 0;
        int stride = Math.max(1, inspectionStride);

        ensureCapacity(Math.min(maxEdgesToDraw * 2, Math.max(2, globalToVisibleIndex.length * 8)));
        for (Edge edge : edges) {
            if ((edgeIndex++ % stride) != 0) continue;
            if (edge == null || !edge.isVisible()) continue;
            int startId = edge.getStartId();
            int endId = edge.getEndId();
            if (startId < 0 || endId < 0 || startId >= globalToVisibleIndex.length || endId >= globalToVisibleIndex.length) {
                continue;
            }
            int startVisible = globalToVisibleIndex[startId];
            int endVisible = globalToVisibleIndex[endId];
            if (startVisible < 0 || endVisible < 0 || startVisible == endVisible) continue;
            ensureCapacity(vertexCount + 2);
            writeEndpoint(startVisible, edge);
            writeEndpoint(endVisible, edge);
            keptEdges++;
            if (keptEdges >= maxEdgesToDraw) break;
        }
    }


    /**
     * Reconstruit des arêtes agrégées entre cellules LOD.
     *
     * <p>Contrairement à {@link #updateVisibleStatic(ConcurrentLinkedQueue, int[], int, int)},
     * cette méthode ne dessine pas toutes les arêtes qui tombent entre deux
     * groupes de sommets. Elle cumule les doublons et conserve une seule ligne
     * par couple de cellules. Cela stabilise l image pendant le pan/zoom et
     * évite de saturer le GPU avec des milliers d arêtes visuellement
     * superposées.</p>
     *
     * @param edges arêtes du modèle
     * @param globalToVisibleIndex table id global -> cluster visible, -1 si hors vue
     * @param maxEdgesToDraw plafond d arêtes agrégées à envoyer au GPU
     */
    public void updateAggregatedStatic(ConcurrentLinkedQueue<Edge> edges, int[] globalToVisibleIndex, int maxEdgesToDraw) {
        vertexCount = 0;
        if (edges == null || edges.isEmpty() || globalToVisibleIndex == null || maxEdgesToDraw <= 0) return;

        aggregatedPairs.clearForExpectedSize(maxEdgesToDraw * 2);
        ensureCapacity(Math.max(2, maxEdgesToDraw * 2));

        int keptEdges = 0;
        for (Edge edge : edges) {
            if (edge == null || !edge.isVisible()) continue;
            int startId = edge.getStartId();
            int endId = edge.getEndId();
            if (startId < 0 || endId < 0 || startId >= globalToVisibleIndex.length || endId >= globalToVisibleIndex.length) {
                continue;
            }
            int startVisible = globalToVisibleIndex[startId];
            int endVisible = globalToVisibleIndex[endId];
            if (startVisible < 0 || endVisible < 0 || startVisible == endVisible) continue;
            if (!aggregatedPairs.add(startVisible, endVisible)) continue;

            ensureCapacity(vertexCount + 2);
            writeEndpoint(startVisible, edge);
            writeEndpoint(endVisible, edge);
            keptEdges++;
            if (keptEdges >= maxEdgesToDraw) break;
        }
    }

    /** Transfère les arêtes visibles vers le GPU sans réallocation systématique. */
    public void uploadStatic() {
        if (vertexCount <= 0) return;

        directEndpointIds.clear();
        directEndpointIds.put(endpointIds, 0, vertexCount).flip();

        directAttributes.clear();
        directAttributes.put(attributes, 0, vertexCount * EDGE_ATTR_FLOATS).flip();

        glBindBuffer(GL_ARRAY_BUFFER, endpointVbo);
        ensureEndpointGpuCapacity(vertexCount);
        glBufferSubData(GL_ARRAY_BUFFER, 0L, directEndpointIds);

        glBindBuffer(GL_ARRAY_BUFFER, attribVbo);
        ensureAttribGpuCapacity(vertexCount * EDGE_ATTR_FLOATS);
        glBufferSubData(GL_ARRAY_BUFFER, 0L, directAttributes);

        glBindBuffer(GL_ARRAY_BUFFER, 0);
    }

    public int count() {
        return vertexCount / 2;
    }

    /** Dessine les arêtes visibles avec un seul draw call. */
    public void draw() {
        if (vertexCount <= 0) return;
        glBindVertexArray(vao);
        glDrawArrays(GL_LINES, 0, vertexCount);
        glBindVertexArray(0);
    }

    /** Libère les ressources GPU. */
    public void dispose() {
        if (endpointVbo != 0) glDeleteBuffers(endpointVbo);
        if (attribVbo != 0) glDeleteBuffers(attribVbo);
        if (vao != 0) glDeleteVertexArrays(vao);
        vao = 0;
        endpointVbo = 0;
        attribVbo = 0;
        vertexCount = 0;
    }

    private void configureVertexArray() {
        glBindVertexArray(vao);

        glBindBuffer(GL_ARRAY_BUFFER, endpointVbo);
        glBufferData(GL_ARRAY_BUFFER, INITIAL_CAPACITY * (long) Integer.BYTES, GL_STREAM_DRAW);
        endpointGpuCapacity = INITIAL_CAPACITY;
        glEnableVertexAttribArray(0);
        glVertexAttribIPointer(0, 1, GL_INT, Integer.BYTES, 0L);

        glBindBuffer(GL_ARRAY_BUFFER, attribVbo);
        glBufferData(GL_ARRAY_BUFFER, INITIAL_CAPACITY * (long) EDGE_ATTR_STRIDE_BYTES, GL_STREAM_DRAW);
        attribGpuCapacity = INITIAL_CAPACITY * EDGE_ATTR_FLOATS;
        glEnableVertexAttribArray(1);
        glVertexAttribPointer(1, 3, GL_FLOAT, false, EDGE_ATTR_STRIDE_BYTES, 0L);
        glEnableVertexAttribArray(2);
        glVertexAttribPointer(2, 1, GL_FLOAT, false, EDGE_ATTR_STRIDE_BYTES, 3L * Float.BYTES);

        glBindBuffer(GL_ARRAY_BUFFER, 0);
        glBindVertexArray(0);
    }

    private void ensureCapacity(int requiredVertices) {
        if (requiredVertices <= capacity) return;
        int newCapacity = capacity;
        while (newCapacity < requiredVertices) newCapacity *= 2;
        resizeCpuStorage(newCapacity);
    }

    private void resizeCpuStorage(int newCapacity) {
        capacity = Math.max(1, newCapacity);
        endpointIds = new int[capacity];
        attributes = new float[capacity * EDGE_ATTR_FLOATS];
        directEndpointIds = BufferUtils.createIntBuffer(capacity);
        directAttributes = BufferUtils.createFloatBuffer(capacity * EDGE_ATTR_FLOATS);
    }

    private void ensureEndpointGpuCapacity(int requiredInts) {
        if (requiredInts <= endpointGpuCapacity) return;
        int next = endpointGpuCapacity <= 0 ? INITIAL_CAPACITY : endpointGpuCapacity;
        while (next < requiredInts) next *= 2;
        endpointGpuCapacity = next;
        glBufferData(GL_ARRAY_BUFFER, endpointGpuCapacity * (long) Integer.BYTES, GL_STREAM_DRAW);
    }

    private void ensureAttribGpuCapacity(int requiredFloats) {
        if (requiredFloats <= attribGpuCapacity) return;
        int next = attribGpuCapacity <= 0 ? INITIAL_CAPACITY * EDGE_ATTR_FLOATS : attribGpuCapacity;
        while (next < requiredFloats) next *= 2;
        attribGpuCapacity = next;
        glBufferData(GL_ARRAY_BUFFER, attribGpuCapacity * (long) Float.BYTES, GL_STREAM_DRAW);
    }

    private void writeEndpoint(int visibleId, Edge edge) {
        endpointIds[vertexCount] = visibleId;
        int base = vertexCount * EDGE_ATTR_FLOATS;
        attributes[base] = clamp01(edge.getR());
        attributes[base + 1] = clamp01(edge.getG());
        attributes[base + 2] = clamp01(edge.getB());
        attributes[base + 3] = 1f;
        vertexCount++;
    }

    /** Ensemble primitif de couples de clusters pour cumuler les arêtes LOD. */
    private static final class EdgePairSet {
        private static final long EMPTY = Long.MIN_VALUE;
        private long[] keys = new long[4096];

        EdgePairSet() {
            java.util.Arrays.fill(keys, EMPTY);
        }

        void clearForExpectedSize(int expected) {
            int required = 1;
            int target = Math.max(4096, expected * 2);
            while (required < target) required <<= 1;
            if (keys.length < required) {
                keys = new long[required];
            }
            java.util.Arrays.fill(keys, EMPTY);
        }

        boolean add(int a, int b) {
            int lo = Math.min(a, b);
            int hi = Math.max(a, b);
            long key = (((long) lo) << 32) ^ (hi & 0xffffffffL);
            int mask = keys.length - 1;
            int slot = mix(key) & mask;
            while (true) {
                long existing = keys[slot];
                if (existing == EMPTY) {
                    keys[slot] = key;
                    return true;
                }
                if (existing == key) {
                    return false;
                }
                slot = (slot + 1) & mask;
            }
        }

        private static int mix(long value) {
            value ^= value >>> 33;
            value *= 0xff51afd7ed558ccdL;
            value ^= value >>> 33;
            value *= 0xc4ceb9fe1a85ec53L;
            value ^= value >>> 33;
            return (int) value;
        }
    }

    private static float clamp01(float value) {
        if (Float.isNaN(value) || Float.isInfinite(value)) return 0f;
        return Math.max(0f, Math.min(1f, value));
    }
}

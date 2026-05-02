package com.mongraphe.graphui.rendering;

import java.nio.FloatBuffer;

import com.mongraphe.graphui.model.Community;
import com.mongraphe.graphui.model.GraphModel.ColoringMode;
import com.mongraphe.graphui.model.Vertex;

import org.lwjgl.BufferUtils;

import static org.lwjgl.opengl.GL11.*;
import static org.lwjgl.opengl.GL15.*;
import static org.lwjgl.opengl.GL20.*;
import static org.lwjgl.opengl.GL30.*;
import static org.lwjgl.opengl.GL31.*;
import static org.lwjgl.opengl.GL33.*;

/**
 * VBO des attributs de sommets visibles ou agrégés.
 *
 * <p>Les positions sont stockées séparément dans {@link PositionTextureBuffer}.
 * Ce buffer contient uniquement les attributs par instance : diamètre, couleur
 * et visibilité. Quand le renderer active le LOD, une instance peut représenter
 * plusieurs sommets proches à l'écran.</p>
 */
public final class VertexGpuBuffer {

    private static final int INITIAL_CAPACITY = 1024;
    private static final int ATTRIB_FLOATS = 5; // diameter, r, g, b, visible
    private static final int ATTRIB_STRIDE_BYTES = ATTRIB_FLOATS * Float.BYTES;
    private static final float MIN_POINT_SIZE = 2.5f;

    private static final float[] UNIT_QUAD = {
            -0.5f, -0.5f, 0.5f, -0.5f, 0.5f, 0.5f,
            -0.5f, -0.5f, 0.5f, 0.5f, -0.5f, 0.5f
    };

    private float[] attributes;
    private FloatBuffer directAttributes;
    private int capacity;
    private int count;
    private int gpuCapacity;

    private int vao;
    private int quadVbo;
    private int attribVbo;

    /** Initialise les VAO/VBO nécessaires au rendu instancié des sommets. */
    public void init() {
        vao = glGenVertexArrays();
        quadVbo = glGenBuffers();
        attribVbo = glGenBuffers();
        resizeCpuStorage(INITIAL_CAPACITY);
        configureVertexArray();
    }


    /**
     * Reconstruit les attributs pour tous les sommets indexés par identifiant natif.
     *
     * <p>Cette méthode est utilisée par le pipeline GPU exact : elle n'est pas
     * appelée à chaque frame, mais seulement lorsque le style ou la structure du
     * graphe change. Les positions restent dans un buffer séparé mis à jour par
     * le moteur C.</p>
     *
     * @param verticesById table nativeId -> Vertex
     * @param totalVertices nombre de sommets exploitables
     * @param selectedVertexId sommet sélectionné, ou -1
     * @param maxDegree degré maximal du graphe
     * @param mode mode de coloration
     * @param uniformR rouge uniforme
     * @param uniformG vert uniforme
     * @param uniformB bleu uniforme
     */
    public void updateAllAttributes(
            Vertex[] verticesById,
            int totalVertices,
            int selectedVertexId,
            int maxDegree,
            ColoringMode mode,
            float uniformR,
            float uniformG,
            float uniformB) {

        count = Math.max(0, Math.min(totalVertices, verticesById == null ? 0 : verticesById.length));
        ensureCapacity(count);
        int maxDeg = Math.max(1, maxDegree);

        for (int id = 0; id < count; id++) {
            Vertex vertex = verticesById[id];
            int base = id * ATTRIB_FLOATS;
            if (vertex == null || vertex.isDeleted() || !vertex.isVisible()) {
                attributes[base] = MIN_POINT_SIZE;
                attributes[base + 1] = 0f;
                attributes[base + 2] = 0f;
                attributes[base + 3] = 0f;
                attributes[base + 4] = 0f;
                continue;
            }
            attributes[base] = Math.max(MIN_POINT_SIZE, safeFloat(vertex.getDiameter()));
            writeColor(vertex, selectedVertexId, maxDeg, mode, uniformR, uniformG, uniformB, base + 1);
            attributes[base + 4] = 1f;
        }
    }

    /**
     * Reconstruit les attributs pour les sommets visibles ou agrégés.
     *
     * @param verticesById table nativeId -> Vertex
     * @param visibleIds identifiants natifs représentatifs, dans l'ordre du buffer compact
     * @param aggregateCounts nombre de sommets représentés par chaque instance
     * @param visibleCount nombre d'entrées valides dans {@code visibleIds}
     * @param selectedVertexId sommet sélectionné, ou -1
     * @param maxDegree degré maximal du graphe
     * @param mode mode de coloration
     * @param uniformR rouge uniforme
     * @param uniformG vert uniforme
     * @param uniformB bleu uniforme
     */
    public void updateVisibleAttributes(
            Vertex[] verticesById,
            int[] visibleIds,
            int[] aggregateCounts,
            int visibleCount,
            int selectedVertexId,
            int maxDegree,
            ColoringMode mode,
            float uniformR,
            float uniformG,
            float uniformB) {

        count = Math.max(0, visibleCount);
        ensureCapacity(count);
        int maxDeg = Math.max(1, maxDegree);

        for (int i = 0; i < count; i++) {
            int id = visibleIds[i];
            Vertex vertex = id >= 0 && id < verticesById.length ? verticesById[id] : null;
            int base = i * ATTRIB_FLOATS;
            if (vertex == null || vertex.isDeleted() || !vertex.isVisible()) {
                attributes[base] = MIN_POINT_SIZE;
                attributes[base + 1] = 0f;
                attributes[base + 2] = 0f;
                attributes[base + 3] = 0f;
                attributes[base + 4] = 0f;
                continue;
            }
            int represented = aggregateCounts != null && i < aggregateCounts.length ? Math.max(1, aggregateCounts[i]) : 1;
            float baseDiameter = Math.max(MIN_POINT_SIZE, safeFloat(vertex.getDiameter()));
            attributes[base] = Math.min(9f, baseDiameter + densityBoost(represented));
            writeColor(vertex, selectedVertexId, maxDeg, mode, uniformR, uniformG, uniformB, base + 1);
            attributes[base + 4] = 1f;
        }
    }

    /** Transfère les attributs visibles vers le GPU sans réallocation inutile. */
    public void uploadAttributes() {
        if (count <= 0) return;
        int elements = count * ATTRIB_FLOATS;
        directAttributes.clear();
        directAttributes.put(attributes, 0, elements).flip();

        glBindBuffer(GL_ARRAY_BUFFER, attribVbo);
        ensureGpuCapacity(elements);
        glBufferSubData(GL_ARRAY_BUFFER, 0L, directAttributes);
        glBindBuffer(GL_ARRAY_BUFFER, 0);
    }

    /**
     * Dessine les sommets visibles avec un seul draw call instancié.
     *
     * @param positionCount nombre de positions compactes disponibles
     */
    public void draw(int positionCount) {
        int drawCount = Math.min(count, positionCount);
        if (drawCount <= 0) return;
        glBindVertexArray(vao);
        glDrawArraysInstanced(GL_TRIANGLES, 0, 6, drawCount);
        glBindVertexArray(0);
    }

    /**
     * @return nombre d'instances de sommets actuellement préparées côté GPU.
     */
    public int count() {
        return count;
    }

    /** Libère les ressources OpenGL. */
    public void dispose() {
        if (quadVbo != 0) glDeleteBuffers(quadVbo);
        if (attribVbo != 0) glDeleteBuffers(attribVbo);
        if (vao != 0) glDeleteVertexArrays(vao);
        vao = 0;
        quadVbo = 0;
        attribVbo = 0;
        count = 0;
        gpuCapacity = 0;
    }

    private void configureVertexArray() {
        FloatBuffer quad = BufferUtils.createFloatBuffer(UNIT_QUAD.length);
        quad.put(UNIT_QUAD).flip();

        glBindVertexArray(vao);

        glBindBuffer(GL_ARRAY_BUFFER, quadVbo);
        glBufferData(GL_ARRAY_BUFFER, quad, GL_STATIC_DRAW);
        glEnableVertexAttribArray(0);
        glVertexAttribPointer(0, 2, GL_FLOAT, false, 2 * Float.BYTES, 0L);

        glBindBuffer(GL_ARRAY_BUFFER, attribVbo);
        glBufferData(GL_ARRAY_BUFFER, INITIAL_CAPACITY * (long) ATTRIB_STRIDE_BYTES, GL_STREAM_DRAW);
        gpuCapacity = INITIAL_CAPACITY * ATTRIB_FLOATS;

        glEnableVertexAttribArray(1);
        glVertexAttribPointer(1, 1, GL_FLOAT, false, ATTRIB_STRIDE_BYTES, 0L);
        glVertexAttribDivisor(1, 1);

        glEnableVertexAttribArray(2);
        glVertexAttribPointer(2, 3, GL_FLOAT, false, ATTRIB_STRIDE_BYTES, Float.BYTES);
        glVertexAttribDivisor(2, 1);

        glEnableVertexAttribArray(3);
        glVertexAttribPointer(3, 1, GL_FLOAT, false, ATTRIB_STRIDE_BYTES, 4L * Float.BYTES);
        glVertexAttribDivisor(3, 1);

        glBindBuffer(GL_ARRAY_BUFFER, 0);
        glBindVertexArray(0);
    }

    private void ensureCapacity(int required) {
        if (required <= capacity) return;
        int newCapacity = capacity;
        while (newCapacity < required) newCapacity *= 2;
        resizeCpuStorage(newCapacity);
    }

    private void resizeCpuStorage(int newCapacity) {
        capacity = Math.max(1, newCapacity);
        attributes = new float[capacity * ATTRIB_FLOATS];
        directAttributes = BufferUtils.createFloatBuffer(capacity * ATTRIB_FLOATS);
    }

    private void ensureGpuCapacity(int requiredFloats) {
        if (requiredFloats <= gpuCapacity) return;
        int newCapacity = gpuCapacity <= 0 ? INITIAL_CAPACITY * ATTRIB_FLOATS : gpuCapacity;
        while (newCapacity < requiredFloats) newCapacity *= 2;
        gpuCapacity = newCapacity;
        glBufferData(GL_ARRAY_BUFFER, gpuCapacity * (long) Float.BYTES, GL_STREAM_DRAW);
    }

    private void writeColor(Vertex vertex, int selectedVertexId, int maxDegree, ColoringMode mode,
            float uniformR, float uniformG, float uniformB, int colorIndex) {
        float r, g, b;
        if (vertex.getId() == selectedVertexId) {
            r = 1f; g = 1f; b = 1f;
        } else if (mode == ColoringMode.UNIFORM) {
            r = uniformR; g = uniformG; b = uniformB;
        } else if (mode == ColoringMode.DEGREE) {
            float t = (float) vertex.getDegree() / maxDegree;
            float intensity = 0.15f + 0.85f * t;
            r = intensity; g = intensity; b = intensity;
        } else {
            Community community = vertex.getCommunity();
            if (community != null) {
                r = community.getR(); g = community.getG(); b = community.getB();
            } else {
                r = 0.8f; g = 0.8f; b = 0.85f;
            }
        }
        attributes[colorIndex] = clamp01(r);
        attributes[colorIndex + 1] = clamp01(g);
        attributes[colorIndex + 2] = clamp01(b);
    }

    private static float densityBoost(int represented) {
        if (represented <= 1) return 0f;
        return (float) Math.min(4.0, Math.log(represented) * 0.9);
    }

    private static float safeFloat(double value) {
        return Double.isFinite(value) ? (float) value : 0f;
    }

    private static float clamp01(float value) {
        if (Float.isNaN(value) || Float.isInfinite(value)) return 0f;
        return Math.max(0f, Math.min(1f, value));
    }
}

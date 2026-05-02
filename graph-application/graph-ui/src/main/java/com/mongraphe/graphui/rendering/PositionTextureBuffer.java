package com.mongraphe.graphui.rendering;

import java.nio.FloatBuffer;

import static org.lwjgl.opengl.GL11.*;
import static org.lwjgl.opengl.GL13.*;
import static org.lwjgl.opengl.GL15.*;
import static org.lwjgl.opengl.GL30.*;
import static org.lwjgl.opengl.GL31.*;

/**
 * Texture-buffer de positions avec triple buffering GPU.
 *
 * <p>Le moteur C publie des positions dans un triple buffer direct côté Java.
 * Cette classe applique la même stratégie côté GPU : trois VBO/TBO sont utilisés
 * en rotation. Le renderer n'écrase donc pas le buffer que le GPU peut encore
 * lire pour la frame précédente, ce qui réduit les stalls dans
 * {@code glBufferSubData} et dans le driver Mesa/Gallium.</p>
 *
 * <p>Le buffer contient {@code x,y} en {@code GL_RG32F}. Les shaders récupèrent
 * la position du sommet {@code i} via {@code texelFetch(u_positions, i)}.</p>
 */
public final class PositionTextureBuffer {

    private static final int GPU_SLOT_COUNT = 3;

    private final int[] bufferIds = new int[GPU_SLOT_COUNT];
    private final int[] textureIds = new int[GPU_SLOT_COUNT];
    private final int[] capacityFloats = new int[GPU_SLOT_COUNT];

    private int activeSlot;
    private int vertexCount;

    /** Initialise les objets OpenGL. */
    public void init() {
        for (int i = 0; i < GPU_SLOT_COUNT; i++) {
            bufferIds[i] = glGenBuffers();
            textureIds[i] = glGenTextures();
        }
        activeSlot = 0;
        vertexCount = 0;
    }

    /**
     * Transfère les positions vers le prochain slot GPU.
     *
     * @param positions buffer direct contenant {@code x,y} pour chaque sommet
     * @param vertexCount nombre de sommets à rendre
     */
    public void upload(FloatBuffer positions, int vertexCount) {
        this.vertexCount = Math.max(0, vertexCount);
        if (positions == null || this.vertexCount <= 0) {
            return;
        }

        FloatBuffer src = positions.duplicate();
        src.position(0);
        src.limit(Math.min(src.capacity(), this.vertexCount * 2));
        int requiredFloats = src.remaining();

        int slot = (activeSlot + 1) % GPU_SLOT_COUNT;
        activeSlot = slot;

        glBindBuffer(GL_TEXTURE_BUFFER, bufferIds[slot]);
        if (requiredFloats > capacityFloats[slot]) {
            capacityFloats[slot] = growCapacity(requiredFloats);
            glBufferData(GL_TEXTURE_BUFFER, capacityFloats[slot] * (long) Float.BYTES, GL_STREAM_DRAW);
            glBindTexture(GL_TEXTURE_BUFFER, textureIds[slot]);
            glTexBuffer(GL_TEXTURE_BUFFER, GL_RG32F, bufferIds[slot]);
            glBindTexture(GL_TEXTURE_BUFFER, 0);
        }
        glBufferSubData(GL_TEXTURE_BUFFER, 0L, src);
        glBindBuffer(GL_TEXTURE_BUFFER, 0);
    }

    /**
     * Lie la texture du slot GPU courant.
     *
     * @param textureUnit unité OpenGL à activer
     */
    public void bind(int textureUnit) {
        glActiveTexture(GL_TEXTURE0 + textureUnit);
        glBindTexture(GL_TEXTURE_BUFFER, textureIds[activeSlot]);
    }

    /**
     * Débind la texture de positions.
     *
     * @param textureUnit unité OpenGL à nettoyer
     */
    public void unbind(int textureUnit) {
        glActiveTexture(GL_TEXTURE0 + textureUnit);
        glBindTexture(GL_TEXTURE_BUFFER, 0);
    }

    /** @return nombre de sommets disponibles dans le slot actif. */
    public int vertexCount() {
        return vertexCount;
    }

    /** Libère les ressources OpenGL. */
    public void dispose() {
        for (int i = 0; i < GPU_SLOT_COUNT; i++) {
            if (textureIds[i] != 0) glDeleteTextures(textureIds[i]);
            if (bufferIds[i] != 0) glDeleteBuffers(bufferIds[i]);
            textureIds[i] = 0;
            bufferIds[i] = 0;
            capacityFloats[i] = 0;
        }
        activeSlot = 0;
        vertexCount = 0;
    }

    private static int growCapacity(int required) {
        int capacity = 1024;
        while (capacity < required) {
            capacity <<= 1;
        }
        return capacity;
    }
}

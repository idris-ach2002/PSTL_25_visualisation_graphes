package com.mongraphe.graphui.rendering;

import java.nio.FloatBuffer;

import org.lwjgl.BufferUtils;

import static org.lwjgl.opengl.GL15.*;

/**
 * Base commune minimale pour les anciens gestionnaires de VBO.
 *
 * <p>Cette classe ne doit pas utiliser {@code FloatBuffer.wrap(...)} avec
 * LWJGL. Les appels OpenGL reçoivent un {@link FloatBuffer} direct afin que
 * l'adresse native transmise au pilote soit valide.</p>
 */
public abstract class AbstractGpuBuffer {

    /**
     * Remplace le contenu complet d'un VBO par les valeurs fournies.
     *
     * @param vbo identifiant OpenGL du tampon cible
     * @param data valeurs flottantes à envoyer au GPU
     */
    protected void upload(int vbo, float[] data) {
        FloatBuffer direct = BufferUtils.createFloatBuffer(data.length);
        direct.put(data);
        direct.flip();

        glBindBuffer(GL_ARRAY_BUFFER, vbo);
        glBufferData(GL_ARRAY_BUFFER, direct, GL_DYNAMIC_DRAW);
    }
}

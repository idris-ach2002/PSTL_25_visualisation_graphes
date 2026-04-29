package com.mongraphe.graphui.rendering;

import java.nio.FloatBuffer;
import java.util.concurrent.ConcurrentLinkedQueue;

import com.jogamp.opengl.GL4;
import com.mongraphe.graphui.model.Community;
import com.mongraphe.graphui.model.Vertex;
import com.mongraphe.graphui.model.GraphModel.ColoringMode;

/**
 * Gestionnaire de tampons GPU pour le rendu haute performance des sommets
 * (noeuds).
 * *
 * <p>
 * Cette classe gère plusieurs Vertex Buffer Objects (VBO) pour stocker les
 * attributs
 * des sommets. Elle supporte le redimensionnement dynamique de la capacité et
 * différents modes de coloration (par degré, par communauté ou uniforme).
 * </p>
 * *
 * <h2>Attributs gérés</h2>
 * <ul>
 * <li><b>Position (Location 0) :</b> Coordonnées (x, y) en 2D.</li>
 * <li><b>Taille (Location 1) :</b> Diamètre du point (gl_PointSize).</li>
 * <li><b>Couleur (Location 2) :</b> Données RGB (float).</li>
 * <li><b>Visibilité (Location 3) :</b> Facteur 0.0 (caché) ou 1.0
 * (visible).</li>
 * </ul>
 */
public final class VertexGpuBuffer {

    private float[] pos, col, size, vis;

    private int posVbo, colVbo, sizeVbo, visVbo;

    private int count;
    private int capacity;

    private static final int INITIAL_CAPACITY = 1024;

    /**
     * Initialise les VBOs et alloue les tableaux CPU initiaux.
     * 
     * @param gl Le contexte OpenGL 4.
     */
    public void init(GL4 gl) {
        int[] b = new int[4];
        gl.glGenBuffers(4, b, 0);

        posVbo = b[0];
        sizeVbo = b[1];
        colVbo = b[2];
        visVbo = b[3];

        capacity = INITIAL_CAPACITY;

        pos = new float[capacity * 2];
        col = new float[capacity * 3];
        size = new float[capacity];
        vis = new float[capacity];

        allocateGpu(gl, capacity);
    }

    /**
     * Alloue la mémoire sur la carte graphique pour la capacité spécifiée.
     */
    private void allocateGpu(GL4 gl, int cap) {
        gl.glBindBuffer(GL4.GL_ARRAY_BUFFER, posVbo);
        gl.glBufferData(GL4.GL_ARRAY_BUFFER, cap * 2L * Float.BYTES, null, GL4.GL_DYNAMIC_DRAW);

        gl.glBindBuffer(GL4.GL_ARRAY_BUFFER, colVbo);
        gl.glBufferData(GL4.GL_ARRAY_BUFFER, cap * 3L * Float.BYTES, null, GL4.GL_DYNAMIC_DRAW);

        gl.glBindBuffer(GL4.GL_ARRAY_BUFFER, sizeVbo);
        gl.glBufferData(GL4.GL_ARRAY_BUFFER, cap * Float.BYTES, null, GL4.GL_DYNAMIC_DRAW);

        gl.glBindBuffer(GL4.GL_ARRAY_BUFFER, visVbo);
        gl.glBufferData(GL4.GL_ARRAY_BUFFER, cap * Float.BYTES, null, GL4.GL_DYNAMIC_DRAW);
    }

    /**
     * Redimensionne les buffers si le nombre de sommets dépasse la capacité
     * actuelle.
     * Utilise une stratégie de doublement de capacité.
     */
    private void ensureCapacity(GL4 gl, int vertexCount) {
        if (vertexCount <= capacity)
            return;

        int newCap = capacity;
        while (newCap < vertexCount)
            newCap *= 2;

        capacity = newCap;

        pos = new float[capacity * 2];
        col = new float[capacity * 3];
        size = new float[capacity];
        vis = new float[capacity];

        allocateGpu(gl, capacity);
    }

    /**
     * Met à jour les tableaux CPU à partir du modèle logique du graphe.
     * * @param gl Le contexte OpenGL.
     * 
     * @param vertices         La file des sommets à traiter.
     * @param selectedVertexId ID du sommet actuellement sélectionné (sera affiché
     *                         en blanc).
     * @param maxDegree        Degré maximum trouvé dans le graphe (pour la
     *                         normalisation du mode DEGREE).
     * @param mode             Mode de coloration souhaité.
     * @param uniformR         Composante Rouge pour le mode UNIFORM.
     * @param uniformG         Composante Verte pour le mode UNIFORM.
     * @param uniformB         Composante Bleue pour le mode UNIFORM.
     */
    public void update(GL4 gl,
            ConcurrentLinkedQueue<Vertex> vertices,
            int selectedVertexId,
            int maxDegree,
            ColoringMode mode,
            float uniformR, float uniformG, float uniformB) {

        if (vertices == null) {
            count = 0;
            return;
        }

        count = 0;
        int maxDeg = Math.max(1, maxDegree);

        for (Vertex v : vertices) {
            if (v == null)
                continue;

            ensureCapacity(gl, count + 1);

            int p = count * 2;
            int c = count * 3;

            pos[p] = (float) v.getX();
            pos[p + 1] = (float) v.getY();

            size[count] = (float) v.getDiameter();
            vis[count] = (v.isDeleted() || !v.isVisible()) ? 0f : 1f;

            float r, g, b;
            // Logique de calcul de la couleur par sommet
            if (v.getId() == selectedVertexId) {
                r = g = b = 1f; // Sélection = Blanc
            } else if (mode == ColoringMode.UNIFORM) {
                r = uniformR;
                g = uniformG;
                b = uniformB;
            } else if (mode == ColoringMode.DEGREE) {
                float t = (float) v.getDegree() / maxDeg;
                float base = 0.15f;
                float intensity = base + (1f - base) * t;
                r = g = b = intensity; // Dégradé de gris selon le degré
            } else {
                Community com = v.getCommunity();
                if (com != null) {
                    r = com.getR();
                    g = com.getG();
                    b = com.getB();
                } else {
                    r = g = b = 0.6f;
                }
            }

            col[c] = r;
            col[c + 1] = g;
            col[c + 2] = b;

            count++;
        }
    }

    /**
     * Envoie les données préparées vers la mémoire vidéo (VRAM).
     */
    public void upload(GL4 gl) {
        upload(gl, posVbo, pos, count * 2);
        upload(gl, colVbo, col, count * 3);
        upload(gl, sizeVbo, size, count);
        upload(gl, visVbo, vis, count);
    }

    private void upload(GL4 gl, int vbo, float[] data, int elements) {
        gl.glBindBuffer(GL4.GL_ARRAY_BUFFER, vbo);
        gl.glBufferSubData(
                GL4.GL_ARRAY_BUFFER,
                0,
                elements * (long) Float.BYTES,
                FloatBuffer.wrap(data, 0, elements));
    }

    /**
     * Active les attributs de sommets et lance l'appel de dessin GL_POINTS.
     */
    public void draw(GL4 gl) {
        // Position
        gl.glEnableVertexAttribArray(0);
        gl.glBindBuffer(GL4.GL_ARRAY_BUFFER, posVbo);
        gl.glVertexAttribPointer(0, 2, GL4.GL_FLOAT, false, 0, 0);

        // Taille
        gl.glEnableVertexAttribArray(1);
        gl.glBindBuffer(GL4.GL_ARRAY_BUFFER, sizeVbo);
        gl.glVertexAttribPointer(1, 1, GL4.GL_FLOAT, false, 0, 0);

        // Couleur
        gl.glEnableVertexAttribArray(2);
        gl.glBindBuffer(GL4.GL_ARRAY_BUFFER, colVbo);
        gl.glVertexAttribPointer(2, 3, GL4.GL_FLOAT, false, 0, 0);

        // Visibilité
        gl.glEnableVertexAttribArray(3);
        gl.glBindBuffer(GL4.GL_ARRAY_BUFFER, visVbo);
        gl.glVertexAttribPointer(3, 1, GL4.GL_FLOAT, false, 0, 0);

        gl.glDrawArrays(GL4.GL_POINTS, 0, count);
    }
}
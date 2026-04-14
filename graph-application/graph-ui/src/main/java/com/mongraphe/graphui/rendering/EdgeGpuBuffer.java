package com.mongraphe.graphui.rendering;

import java.nio.FloatBuffer;
import java.util.concurrent.ConcurrentLinkedQueue;

import com.jogamp.opengl.GL4;
import com.mongraphe.graphui.model.Edge;
import com.mongraphe.graphui.model.Vertex;

/**
 * Gestionnaire de tampons GPU pour le rendu haute performance des arêtes.
 *
 * <p>
 * Cette classe gère le cycle de vie des Vertex Buffer Objects (VBO) pour le
 * dessin
 * des arêtes en OpenGL 4. Elle supporte le rendu de lignes droites et de
 * courbes
 * de Bézier quadratiques.
 * </p>
 *
 * <h2>Fonctionnement</h2>
 * <ul>
 * <li><b>Streaming :</b> Les données sont préparées côté CPU puis envoyées au
 * GPU via {@code glBufferSubData}.</li>
 * <li><b>Batching :</b> Toutes les arêtes sont dessinées en un seul appel (Draw
 * Call) pour optimiser les performances.</li>
 * <li><b>Géométrie dynamique :</b> Les arêtes courbes sont discrétisées en
 * segments de lignes.</li>
 * </ul>
 */
public class EdgeGpuBuffer {

    private static final float LOOP_OFFSET = 22f;
    private static final int INITIAL_CAPACITY = 4096;

    private float[] positions;
    private float[] colors;
    private float[] sizes;
    private float[] visibility;
    private int capacity;
    private int vertexCount; // nombre réel de sommets écrits

    private final float[] control = new float[2];
    private final float[] current = new float[2];
    private final float[] previous = new float[2];

    private int currentGpuCapacityPos = 0;
    private int currentGpuCapacityCol = 0;
    private int currentGpuCapacitySize = 0;
    private int currentGpuCapacityVis = 0;

    private int posVbo, colorVbo, sizeVbo, visVbo;

    /**
     * Initialise les identifiants de buffers OpenGL et alloue la mémoire initiale.
     * 
     * @param gl Le contexte OpenGL.
     */
    public void init(GL4 gl) {
        int[] b = new int[4];
        gl.glGenBuffers(4, b, 0);
        posVbo = b[0];
        colorVbo = b[1];
        sizeVbo = b[2];
        visVbo = b[3];
        capacity = 0;
        ensureCapacity(INITIAL_CAPACITY);
        vertexCount = 0;
    }

    /**
     * Ajuste la taille des tableaux CPU si le nombre de sommets à stocker augmente.
     * 
     * @param needed Capacité totale nécessaire.
     */
    private void ensureCapacity(int needed) {
        if (needed <= capacity)
            return;
        int newCap = Math.max(needed, capacity * 2);
        if (newCap == 0)
            newCap = INITIAL_CAPACITY;

        float[] newPos = new float[newCap * 2];
        float[] newCol = new float[newCap * 3];
        float[] newSizes = new float[newCap];
        float[] newVis = new float[newCap];

        if (positions != null) {
            System.arraycopy(positions, 0, newPos, 0, vertexCount * 2);
            System.arraycopy(colors, 0, newCol, 0, vertexCount * 3);
            System.arraycopy(sizes, 0, newSizes, 0, vertexCount);
            System.arraycopy(visibility, 0, newVis, 0, vertexCount);
        }

        positions = newPos;
        colors = newCol;
        sizes = newSizes;
        visibility = newVis;
        capacity = newCap;
    }

    /**
     * Analyse la file d'arêtes et génère les sommets correspondants dans les
     * tableaux CPU.
     * 
     * @param edges   File des arêtes du graphe.
     * @param options Options de rendu (Style, Segments, etc.).
     */
    public void update(ConcurrentLinkedQueue<Edge> edges, GraphRenderOptions options) {
        vertexCount = 0;
        if (edges == null || edges.isEmpty()) {
            ensureCapacity(1);
            return;
        }

        GraphRenderOptions effectiveOptions = (options == null)
                ? GraphRenderOptions.straight()
                : options;

        if (effectiveOptions.edgeStyle() == GraphRenderOptions.EdgeStyle.CURVED_PARABOLIC) {
            updateCurved(edges, effectiveOptions);
        } else {
            updateStraight(edges);
        }
    }

    /**
     * Génère les sommets pour un rendu de lignes droites.
     */
    private void updateStraight(ConcurrentLinkedQueue<Edge> edges) {
        for (Edge e : edges) {
            // On va ajouter 2 sommets (début et fin)
            ensureCapacity(vertexCount + 2);

            Vertex v1 = e.getStart();
            Vertex v2 = e.getEnd();

            float x1 = (float) v1.getX();
            float y1 = (float) v1.getY();
            float x2 = (float) v2.getX();
            float y2 = (float) v2.getY();

            int p = vertexCount * 2;
            int c = vertexCount * 3;

            if (Float.isNaN(x1) || Float.isNaN(y1) || Float.isNaN(x2) || Float.isNaN(y2)) {
                positions[p] = 0f;
                positions[p + 1] = 0f;
                positions[p + 2] = 0f;
                positions[p + 3] = 0f;
            } else {
                positions[p] = x1;
                positions[p + 1] = y1;
                positions[p + 2] = x2;
                positions[p + 3] = y2;
            }

            float r = e.getR(), g = e.getG(), b = e.getB();
            colors[c] = r;
            colors[c + 1] = g;
            colors[c + 2] = b;
            colors[c + 3] = r;
            colors[c + 4] = g;
            colors[c + 5] = b;

            float w = (float) e.getWeight();
            sizes[vertexCount] = w;
            sizes[vertexCount + 1] = w;

            float vis = e.isVisible() ? 1f : 0f;
            visibility[vertexCount] = vis;
            visibility[vertexCount + 1] = vis;

            vertexCount += 2;
        }
    }

    /**
     * Génère les sommets pour un rendu de lignes courbes (Bézier).
     */
    private void updateCurved(ConcurrentLinkedQueue<Edge> edges, GraphRenderOptions options) {
        int segments = Math.max(2, options.curveSegments());
        for (Edge e : edges) {
            // Chaque arête courbe produit segments * 2 sommets
            int vertsPerEdge = segments * 2;
            ensureCapacity(vertexCount + vertsPerEdge);

            Vertex v1 = e.getStart();
            Vertex v2 = e.getEnd();
            float x1 = (float) v1.getX();
            float y1 = (float) v1.getY();
            float x2 = (float) v2.getX();
            float y2 = (float) v2.getY();

            if (Float.isNaN(x1) || Float.isNaN(y1) || Float.isNaN(x2) || Float.isNaN(y2)) {
                // Remplir avec des zéros
                for (int seg = 0; seg < segments; seg++) {
                    int p = vertexCount * 2;
                    int c = vertexCount * 3;
                    positions[p] = 0f;
                    positions[p + 1] = 0f;
                    positions[p + 2] = 0f;
                    positions[p + 3] = 0f;
                    colors[c] = 0f;
                    colors[c + 1] = 0f;
                    colors[c + 2] = 0f;
                    colors[c + 3] = 0f;
                    colors[c + 4] = 0f;
                    colors[c + 5] = 0f;
                    sizes[vertexCount] = 0f;
                    sizes[vertexCount + 1] = 0f;
                    visibility[vertexCount] = 0f;
                    visibility[vertexCount + 1] = 0f;
                    vertexCount += 2;
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

                int p = vertexCount * 2;
                int c = vertexCount * 3;

                positions[p] = previous[0];
                positions[p + 1] = previous[1];
                positions[p + 2] = current[0];
                positions[p + 3] = current[1];

                colors[c] = r;
                colors[c + 1] = g;
                colors[c + 2] = b;
                colors[c + 3] = r;
                colors[c + 4] = g;
                colors[c + 5] = b;

                sizes[vertexCount] = w;
                sizes[vertexCount + 1] = w;
                visibility[vertexCount] = vis;
                visibility[vertexCount + 1] = vis;

                previous[0] = current[0];
                previous[1] = current[1];
                vertexCount += 2;
            }
        }
    }

    /**
     * Calcule le point de contrôle pour la courbure parabolique de l'arête.
     */
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

    /**
     * Calcule un point sur une courbe de Bézier quadratique.
     */
    private void evalQuadraticBezier(float x0, float y0, float cx, float cy,
            float x1, float y1, float t, float[] out) {
        float u = 1f - t;
        out[0] = u * u * x0 + 2f * u * t * cx + t * t * x1;
        out[1] = u * u * y0 + 2f * u * t * cy + t * t * y1;
    }

    /**
     * Transfère les données des tableaux CPU vers la mémoire vidéo du GPU.
     * 
     * @param gl Le contexte OpenGL.
     */
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

    /**
     * Active les attributs et dessine les arêtes en utilisant GL_LINES.
     * 
     * @param gl Le contexte OpenGL.
     */
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
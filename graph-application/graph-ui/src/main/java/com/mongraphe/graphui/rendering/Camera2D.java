package com.mongraphe.graphui.rendering;

import java.nio.FloatBuffer;
import com.jogamp.common.nio.Buffers;

/**
 * Caméra orthographique 2D pour la gestion de la vue du graphe.
 * *
 * <p>
 * Cette classe gère les transformations de vue (zoom et translation) et génère
 * la matrice de projection correspondante. Elle utilise un système de
 * double-buffering
 * pour assurer que le thread de rendu OpenGL accède toujours à une matrice
 * stable,
 * même si celle-ci est mise à jour simultanément par le thread UI.
 * </p>
 * *
 * <h2>Coordonnées</h2>
 * <ul>
 * <li><b>World Space :</b> L'espace où vivent les sommets du graphe.</li>
 * <li><b>Screen Space :</b> L'espace en pixels de la fenêtre (0,0 en haut à
 * gauche).</li>
 * </ul>
 */
public final class Camera2D {

    private volatile float zoom = 1f;
    private volatile float offsetX, offsetY;
    private volatile int width = 1, height = 1;

    /** Buffers directs pour éviter les allocations à chaque frame. */
    private final FloatBuffer bufferA = Buffers.newDirectFloatBuffer(16);
    private final FloatBuffer bufferB = Buffers.newDirectFloatBuffer(16);

    /** Buffer actuellement utilisé pour le rendu (lecture seule). */
    private volatile FloatBuffer projection = bufferA;

    /** Buffer utilisé pour calculer la prochaine matrice (écriture). */
    private FloatBuffer writeBuffer = bufferB;

    public int getWidth() {
        return width;
    }

    public int getHeight() {
        return height;
    }

    public float getOffsetX() {
        return offsetX;
    }

    public float getOffsetY() {
        return offsetY;
    }

    /**
     * Met à jour les dimensions du viewport et recalcule la projection.
     * 
     * @param w Largeur en pixels.
     * @param h Hauteur en pixels.
     */
    public void resize(int w, int h) {
        width = Math.max(1, w);
        height = Math.max(1, h);
        updateProjection();
    }

    /**
     * Déplace la caméra (translation).
     * 
     * @param dx Déplacement horizontal en pixels.
     * @param dy Déplacement vertical en pixels.
     */
    public void pan(float dx, float dy) {
        offsetX -= dx / zoom;
        offsetY += dy / zoom;
        updateProjection();
    }

    /**
     * Applique un zoom centré sur une coordonnée spécifique de l'écran.
     * <p>
     * Essentiel pour permettre de zoomer vers le curseur de la souris.
     * </p>
     * 
     * @param screenX Position X de la souris.
     * @param screenY Position Y de la souris.
     * @param factor  Facteur multiplicateur (ex: 1.1 pour +10%).
     */
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

    /** Réinitialise la vue à l'origine avec un zoom par défaut. */
    public void reset() {
        zoom = 1f;
        offsetX = 0f;
        offsetY = 0f;
        updateProjection();
    }

    /**
     * Calcule la matrice de projection orthographique.
     * <p>
     * La matrice est générée au format column-major pour OpenGL. Elle définit
     * un volume de vue où le centre de l'écran correspond à (offsetX, offsetY).
     * </p>
     * * Formule simplifiée de la matrice résultante :
     * $$M_{ortho} = \begin{bmatrix} \frac{2}{r-l} & 0 & 0 & -\frac{r+l}{r-l} \\ 0 &
     * \frac{2}{t-b} & 0 & -\frac{t+b}{t-b} \\ 0 & 0 & -1 & 0 \\ 0 & 0 & 0 & 1
     * \end{bmatrix}$$
     */
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

        // Swap atomique des buffers (Double Buffering)
        FloatBuffer oldRead = projection;
        projection = writeBuffer;
        writeBuffer = oldRead;
    }

    /**
     * @return La matrice de projection actuelle prête pour
     *         {@code glUniformMatrix4fv}.
     */
    public FloatBuffer getProjection() {
        return projection;
    }

    /**
     * Convertit une coordonnée écran X en coordonnée monde.
     * 
     * @param screenX Coordonnée en pixels (0 = gauche).
     */
    public float screenToWorldX(float screenX) {
        return (screenX - width / 2f) / zoom + offsetX;
    }

    /**
     * Convertit une coordonnée écran Y en coordonnée monde.
     * 
     * @param screenY Coordonnée en pixels (0 = haut).
     */
    public float screenToWorldY(float screenY) {
        return (height / 2f - screenY) / zoom + offsetY;
    }

    public float getZoom() {
        return zoom;
    }
}
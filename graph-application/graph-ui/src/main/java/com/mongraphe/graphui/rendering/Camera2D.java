package com.mongraphe.graphui.rendering;

import java.nio.FloatBuffer;
import java.util.concurrent.atomic.AtomicLong;
import org.lwjgl.BufferUtils;

/**
 * Caméra orthographique 2D utilisée par le renderer et les interactions.
 *
 * <p>La caméra expose à la fois la matrice OpenGL et les bornes visibles en
 * coordonnées monde. Le renderer s'appuie sur ces bornes pour ne préparer que
 * les sommets et les arêtes susceptibles d'apparaître à l'écran.</p>
 */
public final class Camera2D {

    /**
     * Plus petit zoom accepté pour garder une projection finie.
     *
     * <p>Ce seuil n'est pas une limite utilisateur : il évite uniquement les
     * divisions par zéro et les matrices OpenGL contenant NaN/Inf après un très
     * grand nombre de dézooms successifs.</p>
     */
    private static final float MIN_FINITE_ZOOM = 1.0e-9f;

    private volatile float zoom = 1f;
    private volatile float offsetX, offsetY;
    private volatile int width = 1, height = 1;
    private volatile float worldLeft = -0.5f;
    private volatile float worldRight = 0.5f;
    private volatile float worldBottom = -0.5f;
    private volatile float worldTop = 0.5f;

    /** Version incrémentée à chaque changement de projection ou de taille. */
    private final AtomicLong viewVersion = new AtomicLong(0L);

    /** Buffers directs pour éviter les allocations à chaque frame. */
    private final FloatBuffer bufferA = BufferUtils.createFloatBuffer(16);
    private final FloatBuffer bufferB = BufferUtils.createFloatBuffer(16);

    /** Buffer actuellement utilisé par OpenGL. */
    private volatile FloatBuffer projection = bufferA;

    /** Buffer d'écriture pour le double-buffering de matrice. */
    private FloatBuffer writeBuffer = bufferB;

    public int getWidth() { return width; }
    public int getHeight() { return height; }
    public float getOffsetX() { return offsetX; }
    public float getOffsetY() { return offsetY; }
    public float getZoom() { return zoom; }

    /** @return version monotone de la vue, utile pour invalider le culling. */
    public long getViewVersion() { return viewVersion.get(); }

    /** @return borne gauche visible en coordonnées monde. */
    public float worldLeft() { return worldLeft; }

    /** @return borne droite visible en coordonnées monde. */
    public float worldRight() { return worldRight; }

    /** @return borne basse visible en coordonnées monde. */
    public float worldBottom() { return worldBottom; }

    /** @return borne haute visible en coordonnées monde. */
    public float worldTop() { return worldTop; }

    /**
     * Met à jour la taille du viewport.
     *
     * @param w largeur framebuffer en pixels
     * @param h hauteur framebuffer en pixels
     */
    public void resize(int w, int h) {
        width = Math.max(1, w);
        height = Math.max(1, h);
        updateProjection();
    }

    /**
     * Déplace la caméra en pixels écran.
     *
     * @param dx déplacement horizontal écran
     * @param dy déplacement vertical écran
     */
    public void pan(float dx, float dy) {
        offsetX -= dx / zoom;
        offsetY += dy / zoom;
        updateProjection();
    }

    /**
     * Zoome autour d'un point écran.
     *
     * <p>Le zoom n'est plus borné par une limite applicative. On garde seulement
     * un garde-fou numérique minimal afin de ne jamais produire de matrice de
     * projection invalide.</p>
     *
     * @param screenX coordonnée écran X
     * @param screenY coordonnée écran Y
     * @param factor facteur multiplicatif du zoom
     */
    public void zoomAt(float screenX, float screenY, float factor) {
        if (!Float.isFinite(factor) || factor <= 0f) {
            return;
        }

        float worldX = screenToWorldX(screenX);
        float worldY = screenToWorldY(screenY);

        zoom = sanitizeZoom(zoom * factor);

        offsetX = worldX - (screenX - width / 2f) / zoom;
        offsetY = worldY - (height / 2f - screenY) / zoom;

        updateProjection();
    }

    public void zoomIn() { zoomAt(width / 2f, height / 2f, 1.1f); }
    public void zoomOut() { zoomAt(width / 2f, height / 2f, 0.9f); }

    /**
     * Fixe le zoom de la caméra.
     *
     * @param zoom nouveau zoom demandé
     */
    public void setZoom(float zoom) {
        this.zoom = sanitizeZoom(zoom);
        updateProjection();
    }

    /**
     * Nettoie une valeur de zoom sans imposer de borne visuelle.
     *
     * @param value zoom candidat
     * @return zoom strictement positif et fini
     */
    private static float sanitizeZoom(float value) {
        if (Float.isNaN(value) || value <= 0f) {
            return MIN_FINITE_ZOOM;
        }
        if (value < MIN_FINITE_ZOOM) {
            return MIN_FINITE_ZOOM;
        }
        if (Float.isInfinite(value)) {
            return Float.MAX_VALUE;
        }
        return value;
    }

    /** Réinitialise la caméra. */
    public void reset() {
        zoom = 1f;
        offsetX = 0f;
        offsetY = 0f;
        updateProjection();
    }

    /** Recalcule les bornes visibles et la matrice orthographique OpenGL. */
    private void updateProjection() {
        zoom = sanitizeZoom(zoom);

        float hw = width / 2f / zoom;
        float hh = height / 2f / zoom;

        float left = -hw + offsetX;
        float right = hw + offsetX;
        float bottom = -hh + offsetY;
        float top = hh + offsetY;

        worldLeft = left;
        worldRight = right;
        worldBottom = bottom;
        worldTop = top;

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

        FloatBuffer oldRead = projection;
        projection = writeBuffer;
        writeBuffer = oldRead;
        viewVersion.incrementAndGet();
    }

    /**
     * @return matrice de projection column-major prête pour glUniformMatrix4fv
     */
    public FloatBuffer getProjection() { return projection; }

    /**
     * Convertit un X écran en X monde.
     *
     * @param screenX coordonnée écran X en pixels
     * @return coordonnée monde X
     */
    public float screenToWorldX(float screenX) {
        return (screenX - width / 2f) / zoom + offsetX;
    }

    /**
     * Convertit un Y écran en Y monde.
     *
     * @param screenY coordonnée écran Y en pixels
     * @return coordonnée monde Y
     */
    public float screenToWorldY(float screenY) {
        return (height / 2f - screenY) / zoom + offsetY;
    }
}

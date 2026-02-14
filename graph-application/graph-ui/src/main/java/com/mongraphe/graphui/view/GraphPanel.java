package com.mongraphe.graphui.view;

import com.jogamp.newt.javafx.NewtCanvasJFX;
import com.jogamp.newt.opengl.GLWindow;
import com.jogamp.opengl.GLCapabilities;
import com.jogamp.opengl.GLProfile;
import com.jogamp.opengl.util.FPSAnimator;
import com.mongraphe.graphui.rendering.GraphRenderer;

/**
 * Wrapper JOGL (GLWindow) + intégration JavaFX (NewtCanvasJFX).
 * Gère aussi l'Animator pour pouvoir arrêter proprement à la fermeture/reload.
 */
public final class GraphPanel {

    private final GLWindow window;
    private final NewtCanvasJFX canvas;
    private final FPSAnimator animator;

    public GraphPanel(GraphRenderer renderer) {

        GLProfile profile = GLProfile.get(GLProfile.GL4);
        GLCapabilities caps = new GLCapabilities(profile);
        caps.setDoubleBuffered(true);
        caps.setHardwareAccelerated(true);
        // Antialiasing pour un rendu plus lisse
        caps.setSampleBuffers(true);
        caps.setNumSamples(4); // 4x MSAA
        // Désactiver le stencil buffer (non nécessaire pour ce projet)
        caps.setStencilBits(0);
        // Désactiver le depth buffer (non nécessaire pour un rendu 2D)
        caps.setDepthBits(0);

        // Important: créer le canvas AVANT de démarrer l'animator.
        // Sinon la GLWindow peut être "réalisée" et apparaître comme une fenêtre
        // séparée
        // avant d'être reparentée dans le canvas JavaFX.
        window = GLWindow.create(caps);
        window.setUndecorated(true);
        window.addGLEventListener(renderer);

        // Intégration JavaFX (reparenting)
        canvas = new NewtCanvasJFX(window);
        canvas.setFocusTraversable(false);

        // Taille initiale (sera redimensionnée dynamiquement par GraphView)
        window.setSize(1024, 768);

        animator = new FPSAnimator(window, 60, true);
        animator.start();
    }

    public NewtCanvasJFX canvas() {
        return canvas;
    }

    /**
     * Libération propre (évite les threads JOGL orphelins et les fuites).
     */
    public void dispose() {
        try {
            if (animator != null && animator.isStarted()) {
                animator.stop();
            }
        } catch (Exception ignored) {
        }

        try {
            if (window != null) {
                window.destroy();
            }
        } catch (Exception ignored) {
        }
    }

    /**
     * Redimensionne la surface GL (appelé depuis JavaFX quand le viewport change).
     */
    public void resize(int w, int h) {
        if (w <= 0 || h <= 0)
            return;
        try {
            window.setSize(w, h);
        } catch (Exception ignored) {
        }
    }
}

package com.mongraphe.graphui.rendering;

import com.jogamp.newt.javafx.NewtCanvasJFX;
import com.jogamp.newt.opengl.GLWindow;
import com.jogamp.opengl.GLCapabilities;
import com.jogamp.opengl.GLProfile;
import com.jogamp.opengl.util.FPSAnimator;
import com.mongraphe.graphui.Graph;

/**
 * GraphRenderingContext
 * ---------------------
 *
 * Cette classe encapsule TOUT ce qui concerne :
 * - l'initialisation d'OpenGL via JOGL,
 * - la création du contexte graphique,
 * - la gestion du canvas OpenGL intégré à JavaFX,
 * - le cycle de vie de l'animation (FPSAnimator).
 *
 * Objectif principal :
 * Séparer clairement le rendu OpenGL (bas niveau) de l'interface JavaFX.
 *
 * Cette classe NE FAIT PAS :
 * - de logique métier,
 * - de gestion de données du graphe,
 * - de gestion d'événements JavaFX (souris, clavier),
 * - de calculs (layout, clustering, etc.).
 *
 *  Elle se contente de :
 * - créer un contexte OpenGL valide,
 * - brancher le moteur de rendu (Graph),
 * - fournir un canvas JavaFX prêt à être affiché.
 *
 * Cette séparation permet :
 * - une meilleure lisibilité du code,
 * - une maintenance plus simple,
 * - une compréhension pédagogique claire du rôle du rendering.
 */
public class GraphRenderingContext {

    /**
     * Fenêtre OpenGL native (JOGL).
     * C'est elle qui détient le contexte OpenGL.
     */
    private GLWindow glWindow;

    /**
     * Canvas JavaFX servant de pont entre JavaFX et OpenGL.
     * Ce composant est inséré dans la scène JavaFX.
     */
    private NewtCanvasJFX canvas;

    /**
     * Animateur OpenGL.
     * Il déclenche périodiquement les appels à display().
     */
    private FPSAnimator animator;

    /**
     * Initialise le contexte de rendu OpenGL.
     *
     * @param graph  Le moteur de rendu implémentant GLEventListener
     * @param width  Largeur initiale du canvas
     * @param height Hauteur initiale du canvas
     */
    public void init(Graph graph, double width, double height) {

        // Sélection du profil OpenGL (GL4 recommandé pour performances modernes)
        GLProfile glProfile = GLProfile.get(GLProfile.GL4);

        // Capacités OpenGL
        GLCapabilities capabilities = new GLCapabilities(glProfile);
        capabilities.setDoubleBuffered(true);
        capabilities.setHardwareAccelerated(true);

        // Création de la fenêtre OpenGL native
        glWindow = GLWindow.create(capabilities);

        // Le Graph joue le rôle de GLEventListener (init, display, reshape, dispose)
        glWindow.addGLEventListener(graph);

        graph.glWindow = glWindow;

        // Création du canvas JavaFX qui héberge la fenêtre OpenGL
        canvas = new NewtCanvasJFX(glWindow);
        canvas.setWidth(width);
        canvas.setHeight(height);
        canvas.setVisible(true);

        graph.addMouseListeners();

        // Création et démarrage de l'animateur OpenGL
        animator = new FPSAnimator(glWindow, 60);
        animator.setExclusiveContext(false);
        animator.start();
    }

    /**
     * Retourne le canvas JavaFX à insérer dans la scène.
     *
     * @return le canvas OpenGL JavaFX
     */
    public NewtCanvasJFX getCanvas() {
        return canvas;
    }

    /**
     * Met en pause l'animation OpenGL.
     * Utile lorsque la vue n'est plus visible.
     */
    public void pause() {
        if (animator != null && animator.isAnimating()) {
            animator.pause();
        }
    }

    /**
     * Reprend l'animation OpenGL si elle est en pause.
     */
    public void resume() {
        if (animator != null && !animator.isAnimating()) {
            animator.resume();
        }
    }

    /**
     * Libère proprement les ressources OpenGL.
     * À appeler lors de la fermeture ou du rechargement du graphe.
     */
    public void destroy() {
        if (animator != null) {
            animator.stop();
        }
        if (glWindow != null) {
            glWindow.destroy();
        }
    }
}

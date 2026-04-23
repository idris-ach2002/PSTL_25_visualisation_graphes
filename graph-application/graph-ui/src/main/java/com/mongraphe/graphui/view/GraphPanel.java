package com.mongraphe.graphui.view;

import javax.swing.SwingUtilities;

import javafx.application.Platform;

import com.jogamp.opengl.GLCapabilities;
import com.jogamp.opengl.GLProfile;
import com.jogamp.opengl.awt.GLJPanel;
import com.jogamp.opengl.util.FPSAnimator;
import com.mongraphe.graphui.export.OpenGLGraphImageExporter;
import com.mongraphe.graphui.interaction.InteractionService;
import com.mongraphe.graphui.interaction.SwingInputHandler;
import com.mongraphe.graphui.interfaces.GraphImageExporter;
import com.mongraphe.graphui.rendering.GraphRenderer;

import javafx.embed.swing.SwingNode;
import javafx.scene.layout.Region;

/**
 * Composant de rendu principal du graphe.
 *
 * <p>
 * {@code GraphPanel} encapsule une surface OpenGL (JOGL {@link GLJPanel})
 * intégrée dans JavaFX via {@link SwingNode}.
 *
 * <p>
 * Responsabilités :
 * <ul>
 * <li>rendu du graphe via {@link GraphRenderer}</li>
 * <li>gestion du pipeline OpenGL (GL4)</li>
 * <li>animation via {@link FPSAnimator}</li>
 * <li>gestion des interactions utilisateur (souris, clavier)</li>
 * <li>export d’image du rendu OpenGL</li>
 * </ul>
 *
 * <p>
 * Architecture hybride :
 * <ul>
 * <li>JavaFX pour l’UI globale</li>
 * <li>SwingNode comme pont d’intégration</li>
 * <li>JOGL pour le rendu GPU</li>
 * </ul>
 */
public final class GraphPanel {

    /** Surface de rendu OpenGL JOGL. */
    private final GLJPanel glPanel;

    /** Pont d’intégration Swing → JavaFX. */
    private final SwingNode swingNode;

    /** Contrôleur d’animation FPS (boucle de rendu). */
    private final FPSAnimator animator;

    /** Renderer OpenGL responsable du dessin du graphe. */
    private final GraphRenderer renderer;

    /** Gestionnaire d'entrée Swing conservé pour le retrait propre. */
    private final SwingInputHandler inputHandler;

    private volatile boolean disposed = false;

    /**
     * Construit un panneau de rendu graphique du graphe.
     *
     * @param renderer    moteur de rendu OpenGL du graphe
     * @param interaction service de gestion des interactions utilisateur
     */
    public GraphPanel(GraphRenderer renderer, InteractionService interaction) {
        this.renderer = renderer;

        GLProfile profile = GLProfile.get(GLProfile.GL4);
        GLCapabilities caps = new GLCapabilities(profile);

        glPanel = new GLJPanel(caps);
        glPanel.addGLEventListener(renderer);

        inputHandler = new SwingInputHandler(interaction);
        glPanel.addMouseListener(inputHandler);
        glPanel.addMouseMotionListener(inputHandler);
        glPanel.addMouseWheelListener(inputHandler);
        glPanel.addKeyListener(inputHandler);

        glPanel.setFocusable(true);

        animator = new FPSAnimator(glPanel, 60, true);

        swingNode = new SwingNode();
        createAndSetSwingContent();
    }

    /**
     * Initialise le contenu Swing dans le thread EDT.
     *
     * <p>
     * Nécessaire pour respecter les contraintes Swing (thread UI unique).
     */
    private void createAndSetSwingContent() {
        SwingUtilities.invokeLater(() -> {
            if (!disposed) {
                swingNode.setContent(glPanel);
            }
        });
    }

    /**
     * Crée un exporteur d’image du graphe basé sur OpenGL.
     *
     * @return exporteur capable de capturer le rendu courant
     */
    public void detachContent() {
        Runnable detach = () -> {
            try {
                swingNode.setContent(null);
            } catch (Exception ignored) {
            }
        };

        if (Platform.isFxApplicationThread()) {
            detach.run();
        } else {
            Platform.runLater(detach);
        }
    }

    private void requestAnimatorStopAsync() {
        if (!animator.isStarted())
            return;

        Thread stopThread = new Thread(() -> {
            try {
                if (animator.isAnimating()) {
                    animator.pause();
                }
            } catch (Exception ignored) {
            }

            try {
                animator.stop();
            } catch (Exception ignored) {
            }
        }, "graphpanel-animator-stop");

        stopThread.setDaemon(true);
        stopThread.start();
    }

    public GraphImageExporter createExporter() {
        return new OpenGLGraphImageExporter(glPanel, renderer);
    }

    /**
     * Retourne le nœud JavaFX contenant le canvas OpenGL.
     *
     * @return SwingNode encapsulant le GLJPanel
     */
    public SwingNode canvas() {
        return swingNode;
    }

    /**
     * Retourne le renderer OpenGL utilisé pour le graphe.
     *
     * @return instance du renderer
     */
    public GraphRenderer renderer() {
        return renderer;
    }

    /**
     * Démarre la boucle de rendu (animation OpenGL).
     *
     * <p>
     * Si l’animation est déjà en pause, elle est reprise.
     */
    public void start() {
        if (!animator.isStarted()) {
            animator.start();
        } else if (animator.isPaused()) {
            animator.resume();
        }
    }

    /**
     * Arrête la boucle de rendu.
     */
    public void stop() {
        requestAnimatorStopAsync();
    }

    /**
     * Met en pause la boucle de rendu.
     */
    public void pause() {
        try {
            if (animator.isStarted()) {
                animator.pause();
            }
        } catch (Exception ignored) {
        }
    }

    /**
     * Libère les ressources OpenGL et Swing associées au panneau.
     *
     * <p>
     * Actions :
     * <ul>
     * <li>arrêt de l’animator</li>
     * <li>désenregistrement du renderer OpenGL</li>
     * </ul>
     *
     * <p>
     * Les exceptions sont ignorées volontairement pour éviter les crashes
     * lors de la fermeture UI.
     */
    public void dispose() {
        if (disposed)
            return;
        disposed = true;

        requestAnimatorStopAsync();

        SwingUtilities.invokeLater(() -> {
            try {
                glPanel.removeMouseListener(inputHandler);
                glPanel.removeMouseMotionListener(inputHandler);
                glPanel.removeMouseWheelListener(inputHandler);
                glPanel.removeKeyListener(inputHandler);
            } catch (Exception ignored) {
            }

            try {
                glPanel.removeGLEventListener(renderer);
            } catch (Exception ignored) {
            }

            try {
                glPanel.setFocusable(false);
                glPanel.setEnabled(false);
                glPanel.setVisible(false);
            } catch (Exception ignored) {
            }
        });
    }

    /**
     * Redimensionne la surface de rendu.
     *
     * <p>
     * Met à jour :
     * <ul>
     * <li>la taille du GLJPanel</li>
     * <li>la taille du SwingNode parent (JavaFX Region)</li>
     * </ul>
     *
     * @param w largeur en pixels
     * @param h hauteur en pixels
     */
    public void resize(int w, int h) {
        if (w <= 0 || h <= 0)
            return;

        glPanel.setSize(w, h);
        glPanel.repaint();

        if (swingNode.getParent() instanceof Region region) {
            region.setPrefSize(w, h);
        }
    }
}
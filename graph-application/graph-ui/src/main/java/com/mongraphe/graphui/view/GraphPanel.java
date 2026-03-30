package com.mongraphe.graphui.view;

import javax.swing.SwingUtilities;

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

public final class GraphPanel {

    private final GLJPanel glPanel;
    private final SwingNode swingNode;
    private final FPSAnimator animator;
    private final GraphRenderer renderer;

    public GraphPanel(GraphRenderer renderer, InteractionService interaction) {
        this.renderer = renderer;

        GLProfile profile = GLProfile.get(GLProfile.GL4);
        GLCapabilities caps = new GLCapabilities(profile);
        glPanel = new GLJPanel(caps);
        glPanel.addGLEventListener(renderer);

        SwingInputHandler input = new SwingInputHandler(interaction);
        glPanel.addMouseListener(input);
        glPanel.addMouseMotionListener(input);
        glPanel.addMouseWheelListener(input);
        glPanel.addKeyListener(input);
        glPanel.setFocusable(true);

        animator = new FPSAnimator(glPanel, 120, true);
        swingNode = new SwingNode();
        createAndSetSwingContent();
    }

    private void createAndSetSwingContent() {
        SwingUtilities.invokeLater(() -> swingNode.setContent(glPanel));
    }

    public GraphImageExporter createExporter() {
        return new OpenGLGraphImageExporter(glPanel, renderer);
    }

    public SwingNode canvas() { return swingNode; }
    public GraphRenderer renderer() { return renderer; }

    public void start() {
        if (!animator.isStarted()) {
            animator.start();
        } else if (animator.isPaused()) {
            animator.resume();
        }
    }

    public void stop() {
        if (animator.isStarted()) {
            animator.stop();
        }
    }

    public void pause() {
        if (animator.isStarted()) {
            animator.pause();
        }
    }

    public void dispose() {
        try {
            if (animator.isStarted()) {
                animator.stop();
            }
        } catch (Exception ignored) {
        }

        try {
            glPanel.removeGLEventListener(renderer);
        } catch (Exception ignored) {
        }
    }

    public void resize(int w, int h) {
        if (w <= 0 || h <= 0) return;
        glPanel.setSize(w, h);
        glPanel.repaint();
        if (swingNode.getParent() instanceof Region region) {
            region.setPrefSize(w, h);
        }
    }
}

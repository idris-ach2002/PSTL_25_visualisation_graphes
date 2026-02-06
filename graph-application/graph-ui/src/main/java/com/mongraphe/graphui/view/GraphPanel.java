package com.mongraphe.graphui.view;

import com.jogamp.newt.javafx.NewtCanvasJFX;
import com.jogamp.newt.opengl.GLWindow;
import com.jogamp.opengl.GLCapabilities;
import com.jogamp.opengl.GLProfile;
import com.jogamp.opengl.util.FPSAnimator;
import com.mongraphe.graphui.rendering.GraphRenderer;
public final class GraphPanel {

    private final GLWindow window;
    private final NewtCanvasJFX canvas;

    public GraphPanel(GraphRenderer renderer) {

        GLProfile profile = GLProfile.get(GLProfile.GL4);
        GLCapabilities caps = new GLCapabilities(profile);
        caps.setDoubleBuffered(true);


        window = GLWindow.create(caps);
        window.addGLEventListener(renderer);

        new FPSAnimator(window, 60).start();

        canvas = new NewtCanvasJFX(window);
    }

    public NewtCanvasJFX canvas() {
        return canvas;
    }

    public GLWindow window() {
        return window;
    }
}
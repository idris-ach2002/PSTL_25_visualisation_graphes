package com.mongraphe.graphui.interaction;

public class JoglRenderRequester implements RenderRequester {

    private final com.jogamp.newt.opengl.GLWindow window;

    public JoglRenderRequester(com.jogamp.newt.opengl.GLWindow window) {
        this.window = window;
    }

    @Override
    public void requestRender() {
        window.display();
    }
}
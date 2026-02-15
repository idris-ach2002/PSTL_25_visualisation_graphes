package com.mongraphe.graphui.export;

import java.io.File;

import com.jogamp.newt.opengl.GLWindow;
import com.mongraphe.graphui.interfaces.GraphImageExporter;
import com.mongraphe.graphui.rendering.GraphRenderer;

public final class OpenGLGraphImageExporter implements GraphImageExporter {

    private final GLWindow window;
    private final GraphRenderer renderer;

    public OpenGLGraphImageExporter(GLWindow window, GraphRenderer renderer) {
        this.window = window;
        this.renderer = renderer;
    }

    @Override
    public void exportPng(File file, int width, int height) {
        GraphExport exporter = new GraphExport(window);
        exporter.export(file.getAbsolutePath(), width, height, renderer);
    }
}
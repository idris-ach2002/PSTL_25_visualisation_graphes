package com.mongraphe.graphui.export;

import java.io.File;

import com.jogamp.opengl.GLAutoDrawable;
import com.mongraphe.graphui.interfaces.GraphImageExporter;
import com.mongraphe.graphui.rendering.GraphRenderer;

public final class OpenGLGraphImageExporter implements GraphImageExporter {

    private final GLAutoDrawable drawable;
    private final GraphRenderer renderer;

    public OpenGLGraphImageExporter(GLAutoDrawable drawable, GraphRenderer renderer) {
        this.drawable = drawable;
        this.renderer = renderer;
    }

    @Override
    public void exportPng(File file, int width, int height) {
        GraphExport exporter = new GraphExport(drawable);
        exporter.export(file.getAbsolutePath(), width, height, renderer);
    }
}

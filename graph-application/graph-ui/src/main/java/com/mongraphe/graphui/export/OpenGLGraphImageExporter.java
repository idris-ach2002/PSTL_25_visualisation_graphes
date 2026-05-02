package com.mongraphe.graphui.export;

import java.io.File;

import com.mongraphe.graphui.interfaces.GraphImageExporter;
import com.mongraphe.graphui.view.GraphPanel;

/** Exporteur PNG branché sur le composant OpenGLFX courant. */
public final class OpenGLGraphImageExporter implements GraphImageExporter {

    private final GraphPanel panel;

    public OpenGLGraphImageExporter(GraphPanel panel) {
        this.panel = panel;
    }

    @Override
    public void exportPng(File file, int width, int height) {
        panel.exportPngSync(file, width, height);
    }
}

package com.mongraphe.graphui.view;

import java.io.File;

import com.mongraphe.graphui.export.OpenGLGraphImageExporter;
import com.mongraphe.graphui.interfaces.GraphImageExporter;
import javafx.fxml.FXML;
import javafx.stage.FileChooser;

public final class ExportPanel {

    private GraphPanel graph;

    public void init(GraphPanel graph) {
        this.graph = graph;
    }

    @FXML
    private void handleExportPng(int width, int height) {
        if (graph == null)
            return;

        FileChooser fc = new FileChooser();
        fc.getExtensionFilters().add(
                new FileChooser.ExtensionFilter("PNG", "*.png"));

        File out = fc.showSaveDialog(null);
        if (out == null)
            return;

        GraphImageExporter exporter = new OpenGLGraphImageExporter(graph.window(), graph.renderer());
        exporter.exportPng(out, width, height);
    }
}

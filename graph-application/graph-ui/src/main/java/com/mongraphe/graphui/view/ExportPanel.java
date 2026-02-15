package com.mongraphe.graphui.view;

import java.io.File;

import com.mongraphe.graphui.app.GraphEngineAdapter;

import javafx.fxml.FXML;
import javafx.stage.FileChooser;

public final class ExportPanel {

    private GraphEngineAdapter engine;

    public void init(GraphEngineAdapter engine) {
        this.engine = engine;
    }

    @FXML
    private void handleExportPng() {
        if (engine == null)
            return;

        FileChooser fc = new FileChooser();
        fc.getExtensionFilters().add(
                new FileChooser.ExtensionFilter("PNG", "*.png"));

        File out = fc.showSaveDialog(null);
        if (out == null)
            return;

        engine.exportPng(out, 1920, 1080);
    }
}

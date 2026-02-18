package com.mongraphe.graphui.controller;

import com.mongraphe.graphui.GraphData;
import com.mongraphe.graphui.app.CommandBus;
import com.mongraphe.graphui.app.GraphProject;
import com.mongraphe.graphui.interfaces.CommandBusLinkedI;
import com.mongraphe.graphui.rendering.GraphEngine;

import javafx.fxml.FXML;
import javafx.stage.FileChooser;
import java.io.File;

public final class ProjectController implements CommandBusLinkedI<GraphEngine> {

    private File currentFile;

    private StatsController stats;
    private CommandBus<GraphEngine> bus;
    private GraphData.SimilitudeMode similitudeMode;
    private GraphData.NodeCommunity communityMode;

    public void setStats(StatsController stats) {
        this.stats = stats;
    }

    @Override
    public void setBus(CommandBus<GraphEngine> bus) {
        this.bus = bus;
    }

    @FXML
    private void handleStart() {

        if (currentFile == null || similitudeMode == null || communityMode == null || stats == null)
            return;

        GraphProject.SourceType type = currentFile.getName().toLowerCase().endsWith(".dot")
                ? GraphProject.SourceType.DOT
                : GraphProject.SourceType.CSV;

        bus.dispatch(engine -> {
            engine.load(currentFile.getPath(), type, similitudeMode, communityMode);
            stats.refresh();
        });
    }

    @FXML
    private void handleOpen() {

        FileChooser fc = new FileChooser();
        fc.getExtensionFilters().addAll(
                new FileChooser.ExtensionFilter("Graph CSV", "*.csv"),
                new FileChooser.ExtensionFilter("Graph DOT", "*.dot"));

        currentFile = fc.showOpenDialog(null);

        if (currentFile == null)
            return;
    }
}

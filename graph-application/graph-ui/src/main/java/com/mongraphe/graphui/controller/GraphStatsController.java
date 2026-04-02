package com.mongraphe.graphui.controller;

import com.mongraphe.graphui.app.CommandBus;
import com.mongraphe.graphui.model.Metadata;
import com.mongraphe.graphui.rendering.GraphEngine;

import javafx.application.Platform;
import javafx.fxml.FXML;
import javafx.scene.control.Label;
import javafx.scene.layout.VBox;

public class GraphStatsController implements GraphEngine.GraphDataListener {

    private CommandBus<GraphEngine> bus;

    @FXML
    private VBox statsPane;
    @FXML
    private Label nodesDisplayedLabel;
    @FXML
    private Label nodesHiddenLabel;
    @FXML
    private Label nodesDeletedLabel;
    @FXML
    private Label edgesDisplayedLabel;
    @FXML
    private Label edgesHiddenLabel;
    @FXML
    private Label totalElementsLabel;
    @FXML
    private Label recommendedThresholdLabel;
    @FXML
    private Label recommendedAntiThresholdLabel;
    @FXML
    private Label selectedVertexIdLabel;
    @FXML
    private Label selectedVertexXLabel;
    @FXML
    private Label selectedVertexYLabel;

    public void setBus(CommandBus<GraphEngine> bus) {
        this.bus = bus;
        if (bus != null) {
            bus.dispatch(engine -> engine.addDataListener(this));
            refreshStats(); // initial refresh
        }
    }

    @Override
    public void onGraphDataChanged() {
        Platform.runLater(this::refreshStats);
    }

    public void refreshStats() {
        if (bus == null)
            return;
        try {
            GraphEngine.StatsSnapshot stats = bus.dispatchSync(GraphEngine::getStatsSnapshot);
            nodesDisplayedLabel.setText(String.valueOf(stats.visibleVertices()));
            nodesHiddenLabel.setText(String.valueOf(stats.hiddenVertices()));
            nodesDeletedLabel.setText(String.valueOf(stats.deletedVertices()));
            edgesDisplayedLabel.setText(String.valueOf(stats.visibleEdges()));
            edgesHiddenLabel.setText(String.valueOf(stats.hiddenEdges()));
            totalElementsLabel.setText(String.valueOf(stats.totalVertices() + stats.totalEdges()));

            Metadata initMetadata = stats.initMetadata();
            if (initMetadata != null) {
                recommendedThresholdLabel.setText(String.format("%.4f", initMetadata.getEdgeThreshold()));
                recommendedAntiThresholdLabel.setText(String.format("%.4f", initMetadata.getAntiThreshold()));
            } else {
                recommendedThresholdLabel.setText("-");
                recommendedAntiThresholdLabel.setText("-");
            }

            if (stats.selectedVertexId() >= 0) {
                selectedVertexIdLabel.setText(String.valueOf(stats.selectedVertexId()));
                selectedVertexXLabel.setText(String.format("%.2f", stats.selectedX()));
                selectedVertexYLabel.setText(String.format("%.2f", stats.selectedY()));
            } else {
                selectedVertexIdLabel.setText("-");
                selectedVertexXLabel.setText("-");
                selectedVertexYLabel.setText("-");
            }
        } catch (Exception ignored) {
        }
    }

}

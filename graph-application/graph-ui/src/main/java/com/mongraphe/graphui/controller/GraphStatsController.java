package com.mongraphe.graphui.controller;

import com.mongraphe.graphui.app.CommandBus;
import com.mongraphe.graphui.model.Metadata;
import com.mongraphe.graphui.model.Vertex;
import com.mongraphe.graphui.rendering.GraphEngine;

import javafx.animation.AnimationTimer;
import javafx.fxml.FXML;
import javafx.scene.control.Label;
import javafx.scene.layout.VBox;

public class GraphStatsController {

    private MainGraphController mainController;
    private CommandBus<GraphEngine> bus;

    private AnimationTimer statsTimer;

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

    public void setMainController(MainGraphController controller) {
        this.mainController = controller;
        startStatsTimer();
    }

    public void setBus(CommandBus<GraphEngine> bus) {
        this.bus = bus;
    }

    private void startStatsTimer() {
        if (statsTimer != null) {
            return;
        }
        statsTimer = new AnimationTimer() {
            private long last;

            @Override
            public void handle(long now) {
                if (now - last < 200_000_000L) {
                    return;
                }
                last = now;
                refreshStats();
            }
        };
        statsTimer.start();
    }

    public void refreshStats() {
        if (bus == null) {
            return;
        }

        try {
            GraphEngine.GraphDataSnapshot snapshot = bus.dispatchSync(GraphEngine::getDataSnapshot);
            int totalVertices = snapshot.getVertices().size();
            int totalEdges = snapshot.getEdges().size();
            long deletedVertices = snapshot.getVertices().stream().filter(Vertex::isDeleted).count();
            int visibleVertices = snapshot.getVisibleVertexCount();
            int visibleEdges = snapshot.getVisibleEdgeCount();
            int hiddenVertices = Math.max(0, totalVertices - visibleVertices - (int) deletedVertices);
            int hiddenEdges = Math.max(0, totalEdges - visibleEdges);

            nodesDisplayedLabel.setText(String.valueOf(visibleVertices));
            nodesHiddenLabel.setText(String.valueOf(hiddenVertices));
            nodesDeletedLabel.setText(String.valueOf(deletedVertices));
            edgesDisplayedLabel.setText(String.valueOf(visibleEdges));
            edgesHiddenLabel.setText(String.valueOf(hiddenEdges));
            totalElementsLabel.setText(String.valueOf(totalVertices + totalEdges));

            Metadata initMetadata = bus.dispatchSync(GraphEngine::getInitMetadata);
            if (initMetadata != null) {
                recommendedThresholdLabel.setText(String.format("%.4f", initMetadata.getEdgeThreshold()));
                recommendedAntiThresholdLabel.setText(String.format("%.4f", initMetadata.getAntiThreshold()));
            } else {
                recommendedThresholdLabel.setText("-");
                recommendedAntiThresholdLabel.setText("-");
            }

            int selectedId = bus.dispatchSync(engine -> engine.model().getSelectedVertexId());
            if (selectedId >= 0) {
                Vertex selected = bus.dispatchSync(engine -> engine.model().vertexById(selectedId));
                if (selected != null) {
                    selectedVertexIdLabel.setText(String.valueOf(selectedId));
                    selectedVertexXLabel.setText(String.format("%.2f", selected.getX()));
                    selectedVertexYLabel.setText(String.format("%.2f", selected.getY()));
                }
            } else {
                selectedVertexIdLabel.setText("-");
                selectedVertexXLabel.setText("-");
                selectedVertexYLabel.setText("-");
            }
        } catch (Exception ignored) {
        }
    }

}

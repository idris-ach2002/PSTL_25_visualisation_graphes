package com.mongraphe.graphui.controller;

import com.mongraphe.graphui.app.GraphProject;
import com.mongraphe.graphui.interfaces.CommandBusLinkedI;
import com.mongraphe.graphui.model.GraphData;
import com.mongraphe.graphui.rendering.GraphEngine;

import javafx.fxml.FXML;
import javafx.scene.control.ComboBox;

public final class GraphWorkspaceController
        implements CommandBusLinkedI<GraphEngine> {

    private MainGraphController mainController;

    @FXML
    private ComboBox<GraphData.SimilitudeMode> similarityCombo;

    @FXML
    private ComboBox<GraphData.NodeCommunity> communityCombo;

    @FXML
    private ComboBox<GraphData.RepulsionMode> repulsionCombo;

    public void setMainController(MainGraphController controller) {
        this.mainController = controller;
    }

    @FXML
    private void initialize() {

        similarityCombo.getItems().setAll(GraphData.SimilitudeMode.values());
        communityCombo.getItems().setAll(GraphData.NodeCommunity.values());
        repulsionCombo.getItems().setAll(GraphData.RepulsionMode.values());
    }

    @FXML
    private void handleStartButton() {

        if (mainController == null) {
            return;
        }

        if (similarityCombo.getValue() == null ||
                communityCombo.getValue() == null) {
            return;
        }

        GraphProject.SourceType type = GraphProject.SourceType.CSV;

        mainController.startGraph(
                similarityCombo.getValue(),
                communityCombo.getValue(),
                type);
    }

    @Override
    public void setBus(com.mongraphe.graphui.app.CommandBus<GraphEngine> bus) {
    }
}
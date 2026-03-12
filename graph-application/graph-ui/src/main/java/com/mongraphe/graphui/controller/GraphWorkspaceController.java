package com.mongraphe.graphui.controller;

import com.mongraphe.graphui.interfaces.CommandBusLinkedI;
import com.mongraphe.graphui.model.GraphData;
import com.mongraphe.graphui.rendering.GraphEngine;

import javafx.fxml.FXML;
import javafx.scene.control.ComboBox;
import javafx.scene.control.Control;
import javafx.scene.control.Tooltip;

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

    private void showTooltip(Control control, String message) {
        Tooltip tooltip = new Tooltip(message);
        tooltip.setAutoHide(true);

        tooltip.show(
                control,
                control.localToScreen(0, 0).getX(),
                control.localToScreen(0, 0).getY() - 30);

        // Ajouter une bordure rouge
        control.setStyle("-fx-border-color: red; -fx-border-width: 2px;");

        // Fermer le tooltip et enlever la bordure après 2 secondes
        javafx.animation.PauseTransition delay = new javafx.animation.PauseTransition(javafx.util.Duration.seconds(2));
        delay.setOnFinished(e -> control.setStyle(""));
        delay.play();
    }

    @FXML
    private void handleStartButton() {

        if (mainController == null) {
            return;
        }

        if (similarityCombo.getValue() == null || communityCombo.getValue() == null) {

            if (similarityCombo.getValue() == null) {
                showTooltip(similarityCombo, "Sélectionnez une mesure de similarité");
            }

            if (communityCombo.getValue() == null) {
                showTooltip(communityCombo, "Sélectionnez une méthode de communauté");
            }

            return;
        }
        mainController.startGraph(
                similarityCombo.getValue(),
                communityCombo.getValue());
    }

    @Override
    public void setBus(com.mongraphe.graphui.app.CommandBus<GraphEngine> bus) {
    }
}
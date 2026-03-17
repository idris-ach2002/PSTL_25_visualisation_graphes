package com.mongraphe.graphui.controller;

import com.mongraphe.graphui.interfaces.CommandBusLinkedI;
import com.mongraphe.graphui.model.GraphData;
import com.mongraphe.graphui.rendering.GraphEngine;

import javafx.animation.PauseTransition;
import javafx.fxml.FXML;
import javafx.scene.control.ComboBox;
import javafx.scene.control.Control;
import javafx.scene.control.Tooltip;
import javafx.util.Duration;

public final class GraphWorkspaceController implements CommandBusLinkedI<GraphEngine> {

    private MainGraphController mainController;

    @FXML private ComboBox<GraphData.SimilitudeMode> similarityCombo;
    @FXML private ComboBox<GraphData.NodeCommunity> communityCombo;
    @FXML private ComboBox<GraphData.RepulsionMode> repulsionCombo;

    public void setMainController(MainGraphController controller) {
        this.mainController = controller;
    }

    @FXML
    private void initialize() {
        similarityCombo.getItems().setAll(GraphData.SimilitudeMode.values());
        communityCombo.getItems().setAll(GraphData.NodeCommunity.values());
        repulsionCombo.getItems().setAll(GraphData.RepulsionMode.values());
        if (!repulsionCombo.getItems().isEmpty()) {
            repulsionCombo.getSelectionModel().selectFirst();
        }
    }

    public GraphData.SimilitudeMode getSelectedSimilarity() {
        return similarityCombo.getValue();
    }

    public GraphData.NodeCommunity getSelectedCommunity() {
        return communityCombo.getValue();
    }

    public GraphData.RepulsionMode getSelectedRepulsionMode() {
        return repulsionCombo.getValue();
    }

    public void setSelections(GraphData.SimilitudeMode similarity,
            GraphData.NodeCommunity community,
            GraphData.RepulsionMode repulsion) {
        similarityCombo.setValue(similarity);
        communityCombo.setValue(community);
        if (repulsion != null) {
            repulsionCombo.setValue(repulsion);
        }
    }

    private void showTooltip(Control control, String message) {
        Tooltip tooltip = new Tooltip(message);
        tooltip.setAutoHide(true);
        tooltip.show(control, control.localToScreen(0, 0).getX(), control.localToScreen(0, 0).getY() - 30);
        control.setStyle("-fx-border-color: red; -fx-border-width: 2px;");

        PauseTransition delay = new PauseTransition(Duration.seconds(2));
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

        mainController.startGraph(similarityCombo.getValue(), communityCombo.getValue(), repulsionCombo.getValue());
    }

    @Override
    public void setBus(com.mongraphe.graphui.app.CommandBus<GraphEngine> bus) {
        // volontairement vide : ce panneau pilote le contrôleur principal.
    }
}

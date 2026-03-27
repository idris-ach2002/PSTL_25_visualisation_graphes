package com.mongraphe.graphui.controller;

import com.mongraphe.graphui.app.CommandBus;
import com.mongraphe.graphui.interfaces.CommandBusLinkedI;
import com.mongraphe.graphui.model.GraphData;
import com.mongraphe.graphui.rendering.GraphEngine;

import javafx.animation.PauseTransition;
import javafx.fxml.FXML;
import javafx.scene.control.Button;
import javafx.scene.control.ComboBox;
import javafx.scene.control.Control;
import javafx.scene.control.Tooltip;
import javafx.util.Duration;

public final class GraphWorkspaceController implements CommandBusLinkedI<GraphEngine> {

    private MainGraphController mainController;

    @FXML private ComboBox<GraphData.SimilitudeMode> similarityCombo;
    @FXML private ComboBox<GraphData.NodeCommunity> communityCombo;
    @FXML private ComboBox<GraphData.RepulsionMode> repulsionCombo;

    @FXML private Button playPauseButton;
    @FXML private Button restartButton;

    private CommandBus<GraphEngine> bus;

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

        playPauseButton.setText("▶");
        restartButton.setText("⟳");
        restartButton.setTooltip(new Tooltip("Relancer l'algorithme depuis zéro"));
        playPauseButton.setTooltip(new Tooltip("Lancer"));
    }

    // Appelée par MainGraphController via le listener du moteur
    public void updatePlayPauseIcon(boolean running) {
        if (bus == null) return;
        
        if (running) {
            playPauseButton.setText("⏸");
            playPauseButton.setTooltip(new Tooltip("Mettre en pause"));
        } else {
            playPauseButton.setText("▶");
            playPauseButton.setTooltip(new Tooltip("Lancer"));
        }
    }

    @FXML
    private void handlePlayPause() {
        if (mainController == null || bus == null) return;
        boolean running = bus.dispatchSync(GraphEngine::isSimulationRunning);
        if (running) {
            bus.dispatch(GraphEngine::stopSimulation);
        } else {
            int vertexCount = bus.dispatchSync(engine -> engine.model().vertexCount());
            if (vertexCount == 0) {
                startGraph();
            } else {
                bus.dispatch(GraphEngine::startSimulation);
            }
        }
    }

    @FXML
    private void handleRestart() {
        if (mainController == null) return;
        startGraph();
    }

    private void startGraph() {
        if (similarityCombo.getValue() == null || communityCombo.getValue() == null) {
            if (similarityCombo.getValue() == null) {
                showTooltip(similarityCombo, "Sélectionnez une mesure de similarité");
            }
            if (communityCombo.getValue() == null) {
                showTooltip(communityCombo, "Sélectionnez une méthode de communauté");
            }
            return;
        }
        mainController.startGraph(similarityCombo.getValue(),
                communityCombo.getValue(),
                repulsionCombo.getValue());
    }

    private void showTooltip(Control control, String message) {
        Tooltip tooltip = new Tooltip(message);
        tooltip.setAutoHide(true);
        tooltip.show(control, control.localToScreen(0, 0).getX(),
                control.localToScreen(0, 0).getY() - 30);
        control.setStyle("-fx-border-color: red; -fx-border-width: 2px;");

        PauseTransition delay = new PauseTransition(Duration.seconds(2));
        delay.setOnFinished(e -> control.setStyle(""));
        delay.play();
    }

    public GraphData.SimilitudeMode getSelectedSimilarity() { return similarityCombo.getValue(); }
    public GraphData.NodeCommunity getSelectedCommunity() { return communityCombo.getValue(); }
    public GraphData.RepulsionMode getSelectedRepulsionMode() { return repulsionCombo.getValue(); }

    public void setSelections(GraphData.SimilitudeMode similarity,
                              GraphData.NodeCommunity community,
                              GraphData.RepulsionMode repulsion) {
        similarityCombo.setValue(similarity);
        communityCombo.setValue(community);
        if (repulsion != null) repulsionCombo.setValue(repulsion);
    }

    @Override
    public void setBus(CommandBus<GraphEngine> bus) { this.bus = bus; }
}
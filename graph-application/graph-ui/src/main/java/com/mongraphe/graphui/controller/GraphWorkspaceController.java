package com.mongraphe.graphui.controller;

import com.mongraphe.graphui.interfaces.CommandBusLinkedI;
import com.mongraphe.graphui.model.GraphData;
import com.mongraphe.graphui.rendering.GraphEngine;

import com.mongraphe.graphui.app.CommandBus;
import javafx.animation.AnimationTimer;
import javafx.animation.PauseTransition;
import javafx.fxml.FXML;
import javafx.scene.control.Button;
import javafx.scene.control.ComboBox;
import javafx.scene.control.Control;
import javafx.scene.control.Tooltip;
import javafx.util.Duration;

public final class GraphWorkspaceController implements CommandBusLinkedI<GraphEngine> {

    private MainGraphController mainController;

    @FXML
    private ComboBox<GraphData.SimilitudeMode> similarityCombo;
    @FXML
    private ComboBox<GraphData.NodeCommunity> communityCombo;
    @FXML
    private ComboBox<GraphData.RepulsionMode> repulsionCombo;

    @FXML
    private Button playPauseButton;
    @FXML
    private Button restartButton;

    private CommandBus<GraphEngine> bus;
    private AnimationTimer stateUpdater;
    private boolean simulationRunning = false;

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

        // Timer pour mettre à jour l'icône en fonction de l'état de la simulation
        stateUpdater = new AnimationTimer() {
            private long lastUpdate = 0;

            @Override
            public void handle(long now) {
                if (now - lastUpdate < 200_000_000L)
                    return; // mise à jour toutes les 200 ms
                lastUpdate = now;
                updatePlayPauseIcon();
            }
        };
        stateUpdater.start();
    }

    private void updatePlayPauseIcon() {
        if (bus == null)
            return;

        try {
            boolean running = bus.dispatchSync(GraphEngine::isSimulationRunning);
            if (running != simulationRunning) {
                simulationRunning = running;
                playPauseButton.setText(running ? "⏸" : "▶");
                playPauseButton.setTooltip(new Tooltip(running ? "Mettre en pause" : "Lancer"));
            }
        } catch (Exception ignored) {
            // Si le moteur n'est pas encore initialisé
            if (simulationRunning) {
                simulationRunning = false;
                playPauseButton.setText("▶");
                playPauseButton.setTooltip(new Tooltip("Lancer"));
            }
        }
    }

    @FXML
    private void handlePlayPause() {
        if (mainController == null)
            return;

        try {
            boolean running = bus.dispatchSync(GraphEngine::isSimulationRunning);
            if (running) {
                bus.dispatch(engine -> engine.stopSimulation());
                playPauseButton.setText("▶");
            } else {
                // Si le graphe n'a aucun sommet, on le charge d'abord
                boolean init = bus.dispatchSync(engine -> engine.initialized());
                if (!init) {
                    startGraph();
                } else {
                    bus.dispatch(engine -> engine.startSimulation());
                }
            }
        } catch (Exception e) {
            // En cas d'erreur, on tente de charger le graphe
            startGraph();
        }
    }

    @FXML
    private void handleRestart() {
        if (mainController == null)
            return;

        // On relance complètement le graphe avec les mêmes paramètres
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

    @Override
    public void setBus(CommandBus<GraphEngine> bus) {
        this.bus = bus;
    }
}
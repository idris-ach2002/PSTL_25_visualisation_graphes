package com.mongraphe.graphui.controller;

import java.io.File;

import com.mongraphe.graphui.app.CommandBus;
import com.mongraphe.graphui.interfaces.CommandBusLinkedI;
import com.mongraphe.graphui.model.GraphData;
import com.mongraphe.graphui.model.Metadata;
import com.mongraphe.graphui.rendering.GraphEngine;

import javafx.animation.PauseTransition;
import javafx.application.Platform;
import javafx.fxml.FXML;
import javafx.scene.control.*;
import javafx.scene.layout.VBox;
import javafx.util.Duration;

public final class GraphWorkspaceController implements CommandBusLinkedI<GraphEngine> {

    private MainGraphController mainController;

    @FXML
    private VBox csvSpecificBox;
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

    @FXML
    private TextField widthField;
    @FXML
    private TextField heightField;

    @FXML
    private CheckBox useRecommendedThresholds;
    @FXML
    private TextField edgeThresholdField;
    @FXML
    private TextField antiThresholdField;
    @FXML
    private Label recommendedThresholdsLabel;

    // Nouveaux champs pour la partition spatiale
    @FXML
    private TextField spatialCellsField;
    @FXML
    private CheckBox enableKmeans;
    @FXML
    private TextField epsilonField;

    private CommandBus<GraphEngine> bus;
    private boolean isCsvProject = false;
    private double recommendedEdge = 0.0;
    private double recommendedAnti = 0.0;

    public void setMainController(MainGraphController controller) {
        this.mainController = controller;
    }

    @FXML
    private void initialize() {
        communityCombo.getItems().setAll(GraphData.NodeCommunity.values());
        repulsionCombo.getItems().setAll(GraphData.RepulsionMode.values());
        if (!repulsionCombo.getItems().isEmpty()) {
            repulsionCombo.getSelectionModel().selectFirst();
        }

        playPauseButton.setText("▶");
        restartButton.setText("⟳");
        restartButton.setTooltip(new Tooltip("Relancer l'algorithme depuis zéro"));
        playPauseButton.setTooltip(new Tooltip("Lancer"));

        communityCombo.getSelectionModel().selectFirst();
        repulsionCombo.getSelectionModel().selectFirst();

        // CSV
        similarityCombo.getItems().setAll(GraphData.SimilitudeMode.values());
        similarityCombo.getSelectionModel().selectFirst();
        useRecommendedThresholds.selectedProperty().addListener((obs, old, sel) -> {
            edgeThresholdField.setDisable(sel);
            antiThresholdField.setDisable(sel);
            if (sel) {
                edgeThresholdField.setText(String.format("%.4f", recommendedEdge));
                antiThresholdField.setText(String.format("%.4f", recommendedAnti));
            }
        });

        // Valeurs par défaut pour la partition spatiale
        enableKmeans.setSelected(false);
        epsilonField.setText("0.1");
    }

    public void setProjectType(boolean isCsv) {
        this.isCsvProject = isCsv;
        csvSpecificBox.setVisible(isCsv);
        csvSpecificBox.setManaged(isCsv);
    }

    public void setRecommendedThresholds(double edge, double anti) {
        this.recommendedEdge = edge;
        this.recommendedAnti = anti;
        recommendedThresholdsLabel.setText(String.format("Recommandés : %.4f / %.4f", edge, anti));
        if (useRecommendedThresholds.isSelected()) {
            edgeThresholdField.setText(String.format("%.4f", edge));
            antiThresholdField.setText(String.format("%.4f", anti));
        }
    }

    public void updatePlayPauseIcon(boolean running) {
        if (bus == null)
            return;
        if (running) {
            playPauseButton.setText("⏸");
            playPauseButton.setTooltip(new Tooltip("Mettre en pause"));
        } else {
            playPauseButton.setText("▶");
            playPauseButton.setTooltip(new Tooltip("Lancer"));
        }
    }

    private boolean validateDimensions() {
        String widthText = widthField.getText().trim();
        String heightText = heightField.getText().trim();

        if (widthText.isEmpty()) {
            showTooltip(widthField, "La largeur est obligatoire");
            return false;
        }
        if (heightText.isEmpty()) {
            showTooltip(heightField, "La hauteur est obligatoire");
            return false;
        }

        try {
            double w = Double.parseDouble(widthText);
            double h = Double.parseDouble(heightText);
            if (w <= 0 || h <= 0) {
                showTooltip(widthField, "Les dimensions doivent être positives");
                return false;
            }
        } catch (NumberFormatException e) {
            showTooltip(widthField, "Valeur numérique invalide");
            return false;
        }

        widthField.setStyle("");
        heightField.setStyle("");
        return true;
    }

    @FXML
    private void handlePlayPause() {
        if (mainController == null || bus == null)
            return;
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
        if (mainController == null)
            return;
        startGraph();
    }

    private void startGraph() {
        if (!validateDimensions())
            return;

        double width = Double.parseDouble(widthField.getText().trim());
        double height = Double.parseDouble(heightField.getText().trim());

        File f = mainController.getFile();
        String path = f.getAbsolutePath();
        bus.dispatchSyncVoid(e -> e.startProgram(path));

        // Initialisation automatique du champ spatialCells
        Platform.runLater(() -> {
            if (spatialCellsField.getText().isEmpty()) {
                Integer nodeCount = bus.dispatchSync(engine -> engine.model().vertexCount());
                if (nodeCount != null && nodeCount > 0) {
                    int cells = (int) Math.sqrt(nodeCount);
                    spatialCellsField.setText(String.valueOf(cells));
                }
            }
        });

        if (isCsvProject) {
            if (similarityCombo.getValue() == null) {
                showTooltip(similarityCombo, "Sélectionnez une mesure de similarité");
                return;
            }

            double edgeThreshold, antiThreshold;
            if (useRecommendedThresholds.isSelected()) {
                Metadata recommended = bus
                        .dispatchSync(engine -> engine.computeThreshold(similarityCombo.getValue(), 10));
                recommendedEdge = recommended.getEdgeThreshold();
                recommendedAnti = recommended.getAntiThreshold();
                edgeThreshold = recommendedEdge;
                antiThreshold = recommendedAnti;
                setRecommendedThresholds(recommendedEdge, recommendedAnti);
            } else {
                try {
                    edgeThreshold = Double.parseDouble(edgeThresholdField.getText().trim());
                    antiThreshold = Double.parseDouble(antiThresholdField.getText().trim());
                } catch (NumberFormatException e) {
                    showTooltip(edgeThresholdField, "Seuil invalide");
                    return;
                }
            }

            // Récupérer les paramètres de partition spatiale (peuvent être vides)
            Integer spatialCells = parseSpatialCells();
            Boolean kmeans = enableKmeans.isSelected();
            Double epsilon = parseEpsilon();

            if (spatialCells != null) {
                bus.dispatchSyncVoid(e -> e.setSpatialCells(spatialCells));
            }

            bus.dispatchSyncVoid(e -> e.setKmeansMode(kmeans));

            if (kmeans && epsilon != null) {
                bus.dispatchSyncVoid(e -> e.setEpsilon(epsilon));
            }

            mainController.startGraphCsv(
                    similarityCombo.getValue(),
                    communityCombo.getValue(),
                    repulsionCombo.getValue(),
                    width, height,
                    edgeThreshold, antiThreshold);
        } else {
            mainController.startGraphDot(
                    communityCombo.getValue(),
                    width, height);
        }
    }

    private Integer parseSpatialCells() {
        String text = spatialCellsField.getText().trim();
        if (text.isEmpty() || text.equalsIgnoreCase("auto"))
            return null;
        try {
            return Integer.parseInt(text);
        } catch (NumberFormatException e) {
            return null;
        }
    }

    private Double parseEpsilon() {
        String text = epsilonField.getText().trim();
        if (text.isEmpty())
            return null;
        try {
            return Double.parseDouble(text);
        } catch (NumberFormatException e) {
            return null;
        }
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

    public GraphData.NodeCommunity getSelectedCommunity() {
        return communityCombo.getValue();
    }

    public GraphData.RepulsionMode getSelectedRepulsionMode() {
        return repulsionCombo.getValue();
    }

    public void setSelections(GraphData.NodeCommunity community, GraphData.RepulsionMode repulsion) {
        communityCombo.setValue(community);
        if (repulsion != null)
            repulsionCombo.setValue(repulsion);
    }

    @Override
    public void setBus(CommandBus<GraphEngine> bus) {
        this.bus = bus;
    }
}
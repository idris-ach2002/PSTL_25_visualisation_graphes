package com.mongraphe.graphui.view;

import java.io.File;

import com.mongraphe.graphui.Edge;
import com.mongraphe.graphui.GraphData;
import com.mongraphe.graphui.GraphData.NodeCommunity;
import com.mongraphe.graphui.GraphData.SimilitudeMode;
import com.mongraphe.graphui.Vertex;
import com.mongraphe.graphui.rendering.Camera2D;
import com.mongraphe.graphui.rendering.GraphEngine;
import com.mongraphe.graphui.rendering.GraphNativeEngine;
import com.mongraphe.graphui.rendering.GraphRenderer;

import javafx.application.Platform;
import javafx.beans.binding.DoubleExpression;
import javafx.beans.property.DoubleProperty;
import javafx.beans.property.SimpleBooleanProperty;
import javafx.beans.property.SimpleDoubleProperty;
import javafx.beans.property.SimpleIntegerProperty;
import javafx.beans.property.SimpleStringProperty;
import javafx.beans.property.StringProperty;
import javafx.event.ActionEvent;
import javafx.fxml.FXML;
import javafx.scene.control.Alert;
import javafx.scene.control.CheckBox;
import javafx.scene.control.ColorPicker;
import javafx.scene.control.ComboBox;
import javafx.scene.control.Label;
import javafx.scene.control.ProgressIndicator;
import javafx.scene.control.TableColumn;
import javafx.scene.control.TableView;
import javafx.scene.control.TextField;
import javafx.scene.control.Toggle;
import javafx.scene.control.ToggleGroup;
import javafx.scene.layout.AnchorPane;
import javafx.scene.layout.GridPane;
import javafx.scene.layout.StackPane;

/**
 * GraphVue
 * --------
 *
 * Contrôleur JavaFX principal de l'application de visualisation de graphes.
 *
 * Rôle :
 * - gérer l'interface utilisateur (FXML),
 * - orchestrer l'initialisation du graphe,
 * - déléguer le rendu, les données et les interactions
 * à des composants spécialisés.
 *
 * Cette classe NE FAIT PAS :
 * - le rendu OpenGL,
 * - la gestion des interactions bas niveau,
 * - les calculs lourds (JNI).
 */
public final class GraphView {

    @FXML
    private StackPane mainContentPane;
    @FXML
    private GridPane overviewPane;
    @FXML
    private AnchorPane dataPane;
    @FXML
    private AnchorPane previewPane;
    @FXML
    private StackPane graphContainer;

    @FXML
    private ToggleGroup viewToggleGroup;
    @FXML
    private ToggleGroup graphModeToggleGroup;

    /*
     * ============================
     * === FXML : statistiques ===
     * ============================
     */

    @FXML
    private Label nodesDeletedLabel;
    @FXML
    private Label nodesDisplayedLabel;
    @FXML
    private Label nodesHiddenLabel;
    @FXML
    private Label edgesDisplayedLabel;
    @FXML
    private Label edgesDeletedLabel;
    @FXML
    private Label totalElementsLabel;

    @FXML
    private Label recommendedTreshold;
    @FXML
    private Label recommendedAntiTreshold;
    @FXML
    private Label treshold;
    @FXML
    private Label antiTreshold;

    @FXML
    private Label idHovredVertexLabel;
    @FXML
    private Label XHovredVertexLabel;
    @FXML
    private Label YHovredVertexLabel;

    /*
     * ============================
     * === FXML : tables ===
     * ============================
     */

    @FXML
    private TableView<Vertex> vertexTable;
    @FXML
    private TableColumn<Vertex, Integer> vertexIdCol;
    @FXML
    private TableColumn<Vertex, Integer> vertexCommunityCol;
    @FXML
    private TableColumn<Vertex, Integer> vertexDegreeCol;
    @FXML
    private TableColumn<Vertex, Double> vertexXCol;
    @FXML
    private TableColumn<Vertex, Double> vertexYCol;
    @FXML
    private TableColumn<Vertex, Double> vertexDiameterCol;
    @FXML
    private TableColumn<Vertex, Boolean> vertexDeletedCol;

    @FXML
    private TableView<Edge> edgeTable;
    @FXML
    private TableColumn<Edge, Integer> edgeStartCol;
    @FXML
    private TableColumn<Edge, Integer> edgeEndCol;
    @FXML
    private TableColumn<Edge, Double> edgeWeightCol;

    /*
     * ============================
     * === FXML : options ===
     * ============================
     */

    @FXML
    private ProgressIndicator loadingIndicator;
    @FXML
    private ColorPicker canvasColorPicker;
    @FXML
    private CheckBox enableKmeans;

    @FXML
    private ComboBox<GraphData.RepulsionMode> repulsionModeComboBox;
    @FXML
    private ComboBox<GraphData.SimilitudeMode> mesureChamp;
    @FXML
    private ComboBox<GraphData.NodeCommunity> clusteringChamp;

    @FXML
    private TextField initNodeSize;
    @FXML
    private TextField degreeFactor;
    @FXML
    private TextField upScale;
    @FXML
    private TextField stabilizedTreshold;
    @FXML
    private TextField attractionTreshold;
    @FXML
    private TextField updatedFrequence;
    @FXML
    private TextField newFriction;
    @FXML
    private TextField attractionCoefficient;
    @FXML
    private TextField repulsionTreshold;
    @FXML
    private TextField newAmortissement;
    @FXML
    private TextField nbClusters;
    @FXML
    private TextField minimumDegree;

    /*
     * ============================
     * === Données d'entrée ===
     * ============================
     */
    private File fichier;
    private SimilitudeMode similitudeMode;
    private NodeCommunity communityMode;

    /*
     * ============================
     * === Pipeline graphique ===
     * ============================
     */
    private GraphEngine engine;
    private GraphPanel panel;

    /*
     * ============================
     * === Initialisation ===
     * ============================
     */
    @FXML
    private void initialize() {
        mesureChamp.getItems().setAll(SimilitudeMode.values());
        clusteringChamp.getItems().setAll(NodeCommunity.values());

        vertexIdCol.setCellValueFactory(d -> new SimpleIntegerProperty(d.getValue().getId()).asObject());
        vertexCommunityCol.setCellValueFactory(d -> new SimpleIntegerProperty(d.getValue().getCommunity().getId()).asObject());
        vertexDegreeCol.setCellValueFactory(d -> new SimpleIntegerProperty(d.getValue().getDegree()).asObject());
        vertexXCol.setCellValueFactory(d -> new SimpleDoubleProperty(d.getValue().getX()).asObject());
        vertexYCol.setCellValueFactory(d -> new SimpleDoubleProperty(d.getValue().getY()).asObject());
        vertexDiameterCol.setCellValueFactory(d -> new SimpleDoubleProperty(d.getValue().getDiameter()).asObject());
        vertexDeletedCol.setCellValueFactory(d -> new SimpleBooleanProperty(d.getValue().isDeleted()).asObject());

        edgeStartCol.setCellValueFactory(d -> new SimpleIntegerProperty(d.getValue().getStart().getId()).asObject());
        edgeEndCol.setCellValueFactory(d -> new SimpleIntegerProperty(d.getValue().getEnd().getId()).asObject());
        edgeWeightCol.setCellValueFactory(d -> new SimpleDoubleProperty(d.getValue().getWeight()).asObject());
    }

    /*
     * ============================
     * === Chargement graphe ===
     * ============================
     */
    @FXML
    private void handleStartButton() {

        if (fichier == null) {
            System.err.println("Aucun fichier chargé");
            return;
        }

        loadingIndicator.setVisible(true);

        new Thread(() -> {
            try {
                GraphNativeEngine nativeEngine = new GraphNativeEngine();
                engine = new GraphEngine(nativeEngine);

                engine.loadCsv(fichier.getAbsolutePath(), similitudeMode, communityMode);

                // Debug positions et edges
                System.out.println("Positions loaded: " + nativeEngine.getPositions().length);
                System.out.println("Edges loaded: " + nativeEngine.getEdges().length);

                Camera2D camera = new Camera2D();
                GraphRenderer renderer = new GraphRenderer(engine, camera);
                panel = new GraphPanel(renderer);

                Platform.runLater(() -> {
                    engine.setNodeSize(5);
                    panel.canvas().setWidth(graphContainer.getWidth());
                    panel.canvas().setHeight(graphContainer.getHeight());
                    camera.resize((int) graphContainer.getWidth(), (int) graphContainer.getHeight());

                    graphContainer.getChildren().setAll(panel.canvas());
                    loadingIndicator.setVisible(false);
                });

            } catch (Exception e) {
                e.printStackTrace();
                Platform.runLater(() -> loadingIndicator.setVisible(false));
            }
        }).start();
    }

    /*
     * ============================
     * === Appliquer options ===
     * ============================
     */
    @FXML
    private void applyOptions(ActionEvent event) {
        applyOptions();
    }

    @FXML
    private void applyOptions() {
        if (engine == null)
            return;

        try {
            if (!degreeFactor.getText().isEmpty()) {
                engine.setDegreeScaleFactor(Double.parseDouble(degreeFactor.getText()));
            }
            if (!initNodeSize.getText().isEmpty()) {
                engine.setNodeSize(Integer.parseInt(initNodeSize.getText()));
            }
            if (!upScale.getText().isEmpty()) {
                engine.setUpscale(Integer.parseInt(upScale.getText()));
            }
            if (!stabilizedTreshold.getText().isEmpty()) {
                engine.setStabilizedThreshold(Double.parseDouble(stabilizedTreshold.getText()));
            }
            if (!attractionTreshold.getText().isEmpty()) {
                engine.setAttractionThreshold(Double.parseDouble(attractionTreshold.getText()));
            }
            if (!updatedFrequence.getText().isEmpty()) {
                engine.setUpdateInterval(Double.parseDouble(updatedFrequence.getText()));
            }
            if (!newFriction.getText().isEmpty()) {
                engine.setNewFriction(Double.parseDouble(newFriction.getText()));
            }
            if (!attractionCoefficient.getText().isEmpty()) {
                engine.setAttractionCoefficient(Double.parseDouble(attractionCoefficient.getText()));
            }
            if (!repulsionTreshold.getText().isEmpty()) {
                engine.setRepulsionThreshold(Double.parseDouble(repulsionTreshold.getText()));
            }
            if (!newAmortissement.getText().isEmpty()) {
                engine.setNewAmortissement(Double.parseDouble(newAmortissement.getText()));
            }
            if (!nbClusters.getText().isEmpty()) {
                engine.setNbClusters(Integer.parseInt(nbClusters.getText()));
            }
            if (!minimumDegree.getText().isEmpty()) {
                engine.setMinimumDegree(Integer.parseInt(minimumDegree.getText()));
            }

            GraphData.RepulsionMode mode = repulsionModeComboBox.getValue();
            if (mode != null) {
                engine.setRepulsionMode(mode);
            }

        } catch (NumberFormatException e) {
            System.err.println("Erreur de format : " + e.getMessage());
        }
    }

    @FXML
    private void resetGraphSettings(ActionEvent event) {
        resetGraphSettings();
    }

    @FXML
    private void resetGraphSettings() {
        // Réinitialisation future (laissé volontairement vide)
    }

    @FXML
    private void handleEnableKmeans(ActionEvent event) {
        if (engine != null) {
            engine.enableKmeans(enableKmeans.isSelected());
        }
    }

    @FXML
    private void handleApplyGraphMode(ActionEvent event) {

        Toggle selected = graphModeToggleGroup.getSelectedToggle();
        if (selected == null)
            return;

        try {
            GraphData.GraphMode mode = GraphData.GraphMode.valueOf(selected.getUserData().toString());

            // TODO : appliquer le mode au graphe via le contrôleur d'interaction

        } catch (IllegalArgumentException e) {
            System.err.println("Mode invalide : " + selected.getUserData());
        }
    }

    /*
     * ============================
     * === Changement paramètres ===
     * ============================
     */
    @FXML
    private void applyChangement(ActionEvent event) {
        if (mesureChamp.getValue() != null)
            similitudeMode = mesureChamp.getValue();
        if (clusteringChamp.getValue() != null)
            communityMode = clusteringChamp.getValue();
        graphContainer.getChildren().clear();
        handleStartButton();
    }

    /*
     * ============================
     * === Navigation vues ===
     * ============================
     */
    @FXML
    private void handleViewChange(ActionEvent event) {
        Toggle selected = viewToggleGroup.getSelectedToggle();
        if (selected == null)
            return;

        String view = selected.getUserData().toString();

        overviewPane.setVisible("overview".equals(view));
        dataPane.setVisible("data".equals(view));
        previewPane.setVisible("preview".equals(view));
    }

    /*
     * ============================
     * === Initialisation données ===
     * ============================
     */
    public void initData(File fichier, SimilitudeMode similitudeMode, double upThreshold, double downThreshold,
            NodeCommunity communityMode) {
        this.fichier = fichier;
        this.similitudeMode = similitudeMode;
        this.communityMode = communityMode;
    }

    /*
     * ============================
     * === Fermeture ===
     * ============================
     */
    @FXML
    private void handleQuit(ActionEvent event) {
        Platform.exit();
    }

    @FXML
    private void handleAbout(ActionEvent event) {
        Alert alert = new Alert(Alert.AlertType.INFORMATION);
        alert.setTitle("À propos");
        alert.setHeaderText("Graph Application");
        alert.setContentText("Version 1.0\nAuteur: ZIZI");
        alert.showAndWait();
    }

    /*
     * ============================
     * === Propriétés globales ===
     * ============================
     */
    public static final DoubleProperty updateFrequency = new SimpleDoubleProperty(1.0);
    public static final StringProperty backgroundColor = new SimpleStringProperty("#000000");
}
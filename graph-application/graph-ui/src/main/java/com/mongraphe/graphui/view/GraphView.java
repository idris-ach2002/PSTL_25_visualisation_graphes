package com.mongraphe.graphui.view;

import java.awt.Desktop;
import java.io.File;
import java.io.FileOutputStream;
import java.io.IOException;
import java.net.URI;
import java.nio.file.Files;
import java.util.Properties;

import com.mongraphe.graphui.Edge;
import com.mongraphe.graphui.GraphData;
import com.mongraphe.graphui.GraphData.NodeCommunity;
import com.mongraphe.graphui.GraphData.SimilitudeMode;
import com.mongraphe.graphui.Vertex;
import com.mongraphe.graphui.app.GraphProject;
import com.mongraphe.graphui.app.GraphService;
import com.mongraphe.graphui.app.InteractionService;
import com.mongraphe.graphui.app.InteractionService.Mode;
import com.mongraphe.graphui.app.UiState;
import com.mongraphe.graphui.model.GraphModel;

import javafx.application.Platform;
import javafx.beans.property.SimpleBooleanProperty;
import javafx.beans.property.SimpleDoubleProperty;
import javafx.beans.property.SimpleIntegerProperty;
import javafx.event.ActionEvent;
import javafx.fxml.FXML;
import javafx.fxml.FXMLLoader;
import javafx.geometry.Rectangle2D;
import javafx.scene.Parent;
import javafx.scene.Scene;
import javafx.scene.control.Alert;
import javafx.scene.control.Alert.AlertType;
import javafx.scene.control.Button;
import javafx.scene.control.CheckBox;
import javafx.scene.control.CheckMenuItem;
import javafx.scene.control.ColorPicker;
import javafx.scene.control.ComboBox;
import javafx.scene.control.Label;
import javafx.scene.control.ProgressIndicator;
import javafx.scene.control.TableColumn;
import javafx.scene.control.TableView;
import javafx.scene.control.TextField;
import javafx.scene.control.Toggle;
import javafx.scene.control.ToggleButton;
import javafx.scene.control.ToggleGroup;
import javafx.scene.layout.AnchorPane;
import javafx.scene.layout.GridPane;
import javafx.scene.layout.StackPane;
import javafx.stage.FileChooser;
import javafx.stage.Modality;
import javafx.stage.Screen;
import javafx.stage.Stage;

/**
 * Contrôleur principal (Visualisation.fxml).
 *
 * Objectif: une seule vue "globale".
 * - L'utilisateur choisit un fichier
 * - puis choisit mesure + algo
 * - puis "Démarrer" initialise et affiche le graphe.
 *
 * Le rendu et l'interaction sont délégués à GraphService/InteractionService.
 */
public final class GraphView {

    // =====================
    // === FXML: layout ===
    // =====================
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
    private AnchorPane statsPane;

    @FXML
    private ToggleGroup viewToggleGroup;
    @FXML
    private ToggleGroup graphModeToggleGroup;

    // Toolbar tools
    @FXML
    private ToggleGroup toolToggleGroup;
    @FXML
    private ToggleButton toolRun;
    @FXML
    private ToggleButton toolSelect;
    @FXML
    private ToggleButton toolMove;
    @FXML
    private ToggleButton toolDelete;

    // Menu
    @FXML
    private CheckMenuItem showStatsMenuItem;

    // Status
    @FXML
    private Label statusLabel;

    // Start button
    @FXML
    private Button startButton;

    // =========================
    // === FXML : statistiques ===
    // =========================
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

    // ====================
    // === FXML : tables ===
    // ====================
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

    // =====================
    // === FXML : options ===
    // =====================
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

    // Nouveaux contrôles (présents dans le FXML)
    @FXML
    private ComboBox<GraphModel.ColoringMode> coloringModeCombo;
    @FXML
    private ColorPicker uniformNodeColorPicker;

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

    private final UiState ui = new UiState();
    private final GraphService graphs = new GraphService(ui);
    private final InteractionService interaction = new InteractionService(ui, graphs);

    private File fichier;
    private SimilitudeMode similitudeMode;
    private NodeCommunity communityMode;

    @FXML
    private void initialize() {
        // Combos
        mesureChamp.getItems().setAll(SimilitudeMode.values());
        clusteringChamp.getItems().setAll(NodeCommunity.values());
        repulsionModeComboBox.getItems().setAll(GraphData.RepulsionMode.values());
        if (coloringModeCombo != null) {
            coloringModeCombo.getItems().setAll(GraphModel.ColoringMode.values());
            coloringModeCombo.getSelectionModel().select(GraphModel.ColoringMode.COMMUNITY);
        }

        // Tables
        vertexIdCol.setCellValueFactory(d -> new SimpleIntegerProperty(d.getValue().getId()).asObject());
        vertexCommunityCol.setCellValueFactory(d -> {
            var c = d.getValue().getCommunity();
            return new SimpleIntegerProperty(c == null ? -1 : c.getId()).asObject();
        });
        vertexDegreeCol.setCellValueFactory(d -> new SimpleIntegerProperty(d.getValue().getDegree()).asObject());
        vertexXCol.setCellValueFactory(d -> new SimpleDoubleProperty(d.getValue().getX()).asObject());
        vertexYCol.setCellValueFactory(d -> new SimpleDoubleProperty(d.getValue().getY()).asObject());
        vertexDiameterCol.setCellValueFactory(d -> new SimpleDoubleProperty(d.getValue().getDiameter()).asObject());
        vertexDeletedCol.setCellValueFactory(d -> new SimpleBooleanProperty(d.getValue().isDeleted()).asObject());

        edgeStartCol.setCellValueFactory(d -> new SimpleIntegerProperty(d.getValue().getStart().getId()).asObject());
        edgeEndCol.setCellValueFactory(d -> new SimpleIntegerProperty(d.getValue().getEnd().getId()).asObject());
        edgeWeightCol.setCellValueFactory(d -> new SimpleDoubleProperty(d.getValue().getWeight()).asObject());

        // Status binding
        statusLabel.textProperty().bind(ui.statusTextProperty());

        // Canvas background
        if (canvasColorPicker != null) {
            canvasColorPicker.setValue(javafx.scene.paint.Color.WHITE);
            canvasColorPicker.setOnAction(e -> applyCanvasColor());
        }

        // Similarity / algo selection
        mesureChamp.valueProperty().addListener((obs, o, n) -> {
            similitudeMode = n;
            updateStartButtonState();
        });
        clusteringChamp.valueProperty().addListener((obs, o, n) -> {
            communityMode = n;
            updateStartButtonState();
        });

        // Tool selection
        if (toolToggleGroup != null) {
            toolToggleGroup.selectedToggleProperty().addListener((obs, old, now) -> handleToolMode(null));
            // Default
            if (toolRun != null)
                toolToggleGroup.selectToggle(toolRun);
        }

        // Toggle stats default: visible
        if (showStatsMenuItem != null) {
            showStatsMenuItem.setSelected(true);
        }
        setStatsVisible(true);

        loadingIndicator.setVisible(false);

        updateStartButtonState();
    }

    private void updateStartButtonState() {
        if (startButton == null)
            return;
        boolean ready = fichier != null && similitudeMode != null && communityMode != null;
        startButton.setDisable(!ready);
    }

    private void applyCanvasColor() {
        if (canvasColorPicker == null)
            return;
        var c = canvasColorPicker.getValue();
        ui.setBackground(c);
        if (graphs.renderer() != null) {
            graphs.renderer().setBackgroundColor((float) c.getRed(), (float) c.getGreen(), (float) c.getBlue(),
                    (float) c.getOpacity());
        }
    }

    @FXML
    private void handleStartButton() {
        // Boucle d'alertes (oui, c'est un peu autoritaire, mais c'est l'idée)
        while (fichier == null || similitudeMode == null || communityMode == null) {
            StringBuilder sb = new StringBuilder("Avant d'afficher le graphe, il faut:");
            if (fichier == null)
                sb.append("\n- Choisir un fichier");
            if (similitudeMode == null)
                sb.append("\n- Choisir une mesure de similarité");
            if (communityMode == null)
                sb.append("\n- Choisir un algorithme");
            alert(AlertType.WARNING, "Paramètres manquants", sb.toString());
            return;
        }

        loadingIndicator.setVisible(true);

        new Thread(() -> {
            try {
                Platform.runLater(() -> {
                    GraphProject.SourceType type = detectType(fichier);
                    graphs.load(fichier, type, similitudeMode, communityMode);

                    // Appliquer options moteurs (s'il y a déjà des valeurs)
                    applyOptionsInternal();

                    mountPanelIfNeeded();
                    loadingIndicator.setVisible(false);
                });
            } catch (Exception e) {
                e.printStackTrace();
                Platform.runLater(() -> {
                    loadingIndicator.setVisible(false);
                    alert(AlertType.ERROR, "Erreur", "Impossible de charger le graphe: " + e.getMessage());
                });
            }
        }).start();
    }

    private GraphProject.SourceType detectType(File f) {
        String n = f.getName().toLowerCase();
        if (n.endsWith(".dot"))
            return GraphProject.SourceType.DOT;
        return GraphProject.SourceType.CSV;
    }

    private void mountPanelIfNeeded() {
        if (graphs.panel() == null)
            return;

        // Panel JOGL -> JavaFX
        if (!graphContainer.getChildren().contains(graphs.panel().canvas())) {
            graphContainer.getChildren().setAll(graphs.panel().canvas());
        }

        // Resize camera & canvas
        graphs.panel().canvas().setWidth(graphContainer.getWidth());
        graphs.panel().canvas().setHeight(graphContainer.getHeight());
        graphs.camera().resize((int) graphContainer.getWidth(), (int) graphContainer.getHeight());

        graphContainer.widthProperty().addListener((obs, o, n) -> {
            graphs.panel().canvas().setWidth(n.doubleValue());
            graphs.camera().resize((int) n.doubleValue(), (int) graphContainer.getHeight());
        });
        graphContainer.heightProperty().addListener((obs, o, n) -> {
            graphs.panel().canvas().setHeight(n.doubleValue());
            graphs.camera().resize((int) graphContainer.getWidth(), (int) n.doubleValue());
        });

        interaction.attach(graphs.panel().window());
        handleToolMode(null);

        applyCanvasColor();

        // Coloring mode
        if (graphs.engine() != null && coloringModeCombo != null) {
            coloringModeCombo.valueProperty().addListener((obs, o, n) -> {
                GraphModel m = graphs.engine().model();
                synchronized (m.mutex()) {
                    m.setColoringMode(n == null ? GraphModel.ColoringMode.COMMUNITY : n);
                }
            });
        }
        if (graphs.engine() != null && uniformNodeColorPicker != null) {
            uniformNodeColorPicker.setOnAction(e -> {
                var c = uniformNodeColorPicker.getValue();
                GraphModel m = graphs.engine().model();
                synchronized (m.mutex()) {
                    m.setUniformNodeColor((float) c.getRed(), (float) c.getGreen(), (float) c.getBlue());
                }
            });
        }

        // Stats refresh
        populateTablesOnce();
    }

    private void populateTablesOnce() {
        if (graphs.engine() == null)
            return;
        GraphModel model = graphs.engine().model();
        synchronized (model.mutex()) {
            vertexTable.getItems().setAll(model.vertices());
            edgeTable.getItems().setAll(model.edges());
        }
    }

    @FXML
    private void applyOptions(ActionEvent event) {
        applyOptionsInternal();
    }

    private void applyOptionsInternal() {
        if (graphs.engine() == null)
            return;

        try {
            if (!degreeFactor.getText().isEmpty())
                graphs.engine().setDegreeScaleFactor(Double.parseDouble(degreeFactor.getText()));
            if (!initNodeSize.getText().isEmpty())
                graphs.engine().setInitialNodeSize(Double.parseDouble(initNodeSize.getText()));
            if (!upScale.getText().isEmpty())
                graphs.engine().setUpscale(Integer.parseInt(upScale.getText()));
            if (!stabilizedTreshold.getText().isEmpty())
                graphs.engine().setStabilizedThreshold(Double.parseDouble(stabilizedTreshold.getText()));
            if (!attractionTreshold.getText().isEmpty())
                graphs.engine().setAttractionThreshold(Double.parseDouble(attractionTreshold.getText()));
            if (!updatedFrequence.getText().isEmpty())
                graphs.engine().setClusterUpdateFrequency(Integer.parseInt(updatedFrequence.getText()));
            if (!newFriction.getText().isEmpty())
                graphs.engine().setNewFriction(Double.parseDouble(newFriction.getText()));
            if (!attractionCoefficient.getText().isEmpty())
                graphs.engine().setAttractionCoefficient(Double.parseDouble(attractionCoefficient.getText()));
            if (!repulsionTreshold.getText().isEmpty())
                graphs.engine().setRepulsionThreshold(Double.parseDouble(repulsionTreshold.getText()));
            if (!newAmortissement.getText().isEmpty())
                graphs.engine().setNewAmortissement(Double.parseDouble(newAmortissement.getText()));
            if (!nbClusters.getText().isEmpty())
                graphs.engine().setNbClusters(Integer.parseInt(nbClusters.getText()));
            if (!minimumDegree.getText().isEmpty())
                graphs.engine().setMinimumDegree(Integer.parseInt(minimumDegree.getText()));

            GraphData.RepulsionMode rm = repulsionModeComboBox.getValue();
            if (rm != null)
                graphs.engine().setRepulsionMode(rm);
        } catch (NumberFormatException e) {
            ui.setStatus("Valeur invalide: " + e.getMessage());
        } catch (Exception e) {
            ui.setStatus("Impossible d'appliquer les options: " + e.getMessage());
        }
    }

    @FXML
    private void resetGraphSettings(ActionEvent event) {
        // Minimal reset: caméra + filtres, sans écraser tes réglages par défaut
        if (graphs.camera() != null)
            graphs.camera().reset();
        if (graphs.engine() != null) {
            GraphModel m = graphs.engine().model();
            synchronized (m.mutex()) {
                m.setFilterMinDegree(0);
                m.applyFilters();
            }
        }
        ui.setStatus("Réinitialisation appliquée");
    }

    @FXML
    private void handleEnableKmeans(ActionEvent event) {
        if (graphs.engine() != null)
            graphs.engine().enableKmeans(enableKmeans.isSelected());
    }

    @FXML
    private void handleApplyGraphMode(ActionEvent event) {
        Toggle selected = graphModeToggleGroup == null ? null : graphModeToggleGroup.getSelectedToggle();
        if (selected == null || graphs.engine() == null)
            return;
        try {
            GraphData.GraphMode mode = GraphData.GraphMode.valueOf(String.valueOf(selected.getUserData()));
            // Ce "GraphMode" correspond aux outils d'interaction
            // (RUN/SELECTION/MOVE/DELETE).
            // On le mappe sur InteractionService.Mode sans toucher à la logique UI.
            switch (mode) {
                case RUN -> interaction.setMode(Mode.RUN);
                case SELECTION -> interaction.setMode(Mode.SELECT);
                case MOVE -> interaction.setMode(Mode.MOVE);
                case DELETE -> interaction.setMode(Mode.DELETE);
            }
            ui.setStatus("Mode graphe: " + mode);
        } catch (Exception e) {
            ui.setStatus("Mode invalide");
        }
    }

    @FXML
    private void applyChangement(ActionEvent event) {
        if (mesureChamp.getValue() != null)
            similitudeMode = mesureChamp.getValue();
        if (clusteringChamp.getValue() != null)
            communityMode = clusteringChamp.getValue();
        updateStartButtonState();
        handleStartButton();
    }

    @FXML
    private void handleViewChange(ActionEvent event) {
        Toggle selected = viewToggleGroup == null ? null : viewToggleGroup.getSelectedToggle();
        if (selected == null)
            return;
        String view = String.valueOf(selected.getUserData());

        overviewPane.setVisible("overview".equals(view));
        dataPane.setVisible("data".equals(view));
        previewPane.setVisible("preview".equals(view));
    }

    public void initData(File fichier, SimilitudeMode similitudeMode, double upThreshold, double downThreshold,
            NodeCommunity communityMode) {
        this.fichier = fichier;
        this.similitudeMode = similitudeMode;
        this.communityMode = communityMode;
        Platform.runLater(this::updateStartButtonState);
    }

    @FXML
    private void handleToolMode(ActionEvent event) {
        Toggle t = toolToggleGroup == null ? null : toolToggleGroup.getSelectedToggle();
        if (t == null)
            return;
        Object ud = t.getUserData();
        if (ud == null)
            return;
        try {
            interaction.setMode(Mode.valueOf(String.valueOf(ud)));
        } catch (Exception ignored) {
            interaction.setMode(Mode.RUN);
        }
    }

    @FXML
    private void handleNew(ActionEvent event) {
        // Retour à l'accueil
        try {
            Stage stage = (Stage) mainContentPane.getScene().getWindow();
            Parent root = FXMLLoader.load(getClass().getResource("/fxml/HomeScreen.fxml"));
            Scene scene = new Scene(root);
            stage.setTitle("MonGraphe");
            stage.setScene(scene);

            Rectangle2D sb = Screen.getPrimary().getVisualBounds();
            stage.setX(sb.getMinX());
            stage.setY(sb.getMinY());
            stage.setWidth(sb.getWidth());
            stage.setHeight(sb.getHeight());
            stage.setResizable(false);
            stage.show();

            disposeGraph();
        } catch (IOException e) {
            alert(AlertType.ERROR, "Erreur", "Impossible de revenir à l'accueil: " + e.getMessage());
        }
    }

    @FXML
    private void handleOpen(ActionEvent event) {
        FileChooser fc = new FileChooser();
        fc.setTitle("Ouvrir un fichier de graphe");
        fc.getExtensionFilters().addAll(
                new FileChooser.ExtensionFilter("Graph CSV", "*.csv"),
                new FileChooser.ExtensionFilter("Graph DOT", "*.dot"),
                new FileChooser.ExtensionFilter("Tous les fichiers", "*.*"));
        File f = fc.showOpenDialog(mainContentPane.getScene().getWindow());
        if (f == null)
            return;
        fichier = f;
        ui.setStatus("Fichier: " + f.getName());
        updateStartButtonState();
        if (startButton != null && !startButton.isDisable()) {
            handleStartButton();
        }
    }

    @FXML
    private void handleSaveProject(ActionEvent event) {
        if (fichier == null) {
            alert(AlertType.WARNING, "Enregistrer", "Aucun fichier chargé");
            return;
        }
        FileChooser fc = new FileChooser();
        fc.setTitle("Enregistrer le projet");
        fc.getExtensionFilters().add(new FileChooser.ExtensionFilter("Projet MonGraphe", "*.mongraphe"));
        File out = fc.showSaveDialog(mainContentPane.getScene().getWindow());
        if (out == null)
            return;
        if (!out.getName().toLowerCase().endsWith(".mongraphe"))
            out = new File(out.getAbsolutePath() + ".mongraphe");

        Properties p = new Properties();
        p.setProperty("source", fichier.getAbsolutePath());
        p.setProperty("sourceType", detectType(fichier).name());
        p.setProperty("similitudeMode", similitudeMode == null ? "" : similitudeMode.name());
        p.setProperty("communityMode", communityMode == null ? "" : communityMode.name());
        if (canvasColorPicker != null && canvasColorPicker.getValue() != null) {
            p.setProperty("background", canvasColorPicker.getValue().toString());
        }
        if (coloringModeCombo != null && coloringModeCombo.getValue() != null) {
            p.setProperty("coloring", coloringModeCombo.getValue().name());
        }

        try (FileOutputStream fos = new FileOutputStream(out)) {
            p.store(fos, "MonGraphe project");
            ui.setStatus("Projet enregistré: " + out.getName());
        } catch (IOException e) {
            alert(AlertType.ERROR, "Erreur", "Impossible d'enregistrer: " + e.getMessage());
        }
    }

    @FXML
    private void handleUndo(ActionEvent event) {
        interaction.undo();
    }

    @FXML
    private void handleRedo(ActionEvent event) {
        interaction.redo();
    }

    @FXML
    private void handleFullScreen(ActionEvent event) {
        Stage stage = (Stage) mainContentPane.getScene().getWindow();
        stage.setFullScreen(!stage.isFullScreen());
    }

    @FXML
    private void handleToggleStats(ActionEvent event) {
        boolean show = showStatsMenuItem == null ? statsPane.isVisible() : showStatsMenuItem.isSelected();
        setStatsVisible(show);
    }

    private void setStatsVisible(boolean show) {
        if (statsPane == null)
            return;
        statsPane.setVisible(show);
        statsPane.setManaged(show);
    }

    @FXML
    private void handleLayoutReset(ActionEvent event) {
        if (graphs.camera() != null)
            graphs.camera().reset();
        ui.setStatus("Vue réinitialisée");
    }

    @FXML
    private void handleOptions(ActionEvent event) {
        // Les options sont déjà dans le panneau gauche, mais on ouvre un mini rappel.
        Stage owner = (Stage) mainContentPane.getScene().getWindow();
        Alert a = new Alert(AlertType.INFORMATION);
        a.initOwner(owner);
        a.initModality(Modality.WINDOW_MODAL);
        a.setTitle("Options");
        a.setHeaderText("Raccourcis et outils");
        a.setContentText(
                "Outils: Exécuter / Sélectionner / Déplacer / Supprimer\n" +
                        "SPACE: pause/run\n" +
                        "DEL: supprimer le sommet sélectionné\n" +
                        "Molette: zoom (sur curseur)\n" +
                        "Clic droit + drag: déplacer la caméra\n" +
                        "Undo/Redo: via le menu Édition");
        a.showAndWait();
    }

    @FXML
    private void handleDocumentation(ActionEvent event) {
        // Ouvre le README si présent, sinon une page neutre.
        try {
            File readme = new File("README.md");
            if (readme.exists() && Desktop.isDesktopSupported()) {
                Desktop.getDesktop().open(readme);
                return;
            }
        } catch (Exception ignored) {
        }
        try {
            if (Desktop.isDesktopSupported()) {
                Desktop.getDesktop().browse(URI.create("https://openjfx.io/"));
            }
        } catch (Exception e) {
            ui.setStatus("Impossible d'ouvrir la documentation");
        }
    }

    @FXML
    private void handleExportPng(ActionEvent event) {
        if (graphs.panel() == null) {
            alert(AlertType.WARNING, "Export PNG", "Aucun graphe affiché");
            return;
        }
        FileChooser fc = new FileChooser();
        fc.setTitle("Exporter en PNG");
        fc.getExtensionFilters().add(new FileChooser.ExtensionFilter("Image PNG", "*.png"));
        File out = fc.showSaveDialog(mainContentPane.getScene().getWindow());
        if (out == null)
            return;
        if (!out.getName().toLowerCase().endsWith(".png"))
            out = new File(out.getAbsolutePath() + ".png");

        final File outFinal = out;
        int w = Math.max(1, (int) graphContainer.getWidth());
        int h = Math.max(1, (int) graphContainer.getHeight());

        new Thread(() -> {
            try {
                if (outFinal.toPath().getParent() != null) {
                    Files.createDirectories(outFinal.toPath().getParent());
                }
                graphs.exportPng(outFinal, w, h);
            } catch (Exception e) {
                Platform.runLater(() -> alert(AlertType.ERROR, "Erreur", "Export PNG impossible: " + e.getMessage()));
            }
        }).start();
    }

    @FXML
    private void handleQuit(ActionEvent event) {
        disposeGraph();
        Platform.exit();
    }

    @FXML
    private void handleAbout(ActionEvent event) {
        Alert alert = new Alert(AlertType.INFORMATION);
        alert.setTitle("À propos");
        alert.setHeaderText("MonGraphe");
        alert.setContentText("Visualisation de graphes (JavaFX + JOGL + JNI)\n" +
                "Build: 0.0.1-SNAPSHOT");
        alert.showAndWait();
    }

    private void disposeGraph() {
        try {
            graphs.dispose();
        } catch (Exception ignored) {
        }
    }

    private void alert(AlertType type, String title, String content) {
        Alert a = new Alert(type);
        a.setTitle(title);
        a.setHeaderText(null);
        a.setContentText(content);
        a.showAndWait();
    }
}

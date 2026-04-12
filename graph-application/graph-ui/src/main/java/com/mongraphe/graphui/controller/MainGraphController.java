package com.mongraphe.graphui.controller;

import java.io.File;
import java.nio.file.Files;
import java.util.ArrayList;
import java.util.Optional;

import com.mongraphe.graphui.app.CommandBus;
import com.mongraphe.graphui.app.UiState;
import com.mongraphe.graphui.export.SvgExporter;
import com.mongraphe.graphui.interaction.InteractionService;
import com.mongraphe.graphui.model.GraphData;
import com.mongraphe.graphui.model.GraphProject;
import com.mongraphe.graphui.rendering.EngineExecutor;
import com.mongraphe.graphui.rendering.GraphEngine;
import com.mongraphe.graphui.rendering.GraphNativeEngine;
import com.mongraphe.graphui.rendering.GraphRenderer;
import com.mongraphe.graphui.rendering.GraphRenderOptions;
import com.mongraphe.graphui.view.GraphPanel;
import com.mongraphe.graphui.view.PngResolutionDialog;
import com.mongraphe.graphui.view.SvgResolutionDialog;

import javafx.application.Platform;
import javafx.fxml.FXML;
import javafx.scene.control.Alert;
import javafx.scene.control.Button;
import javafx.scene.control.TabPane;
import javafx.scene.control.Toggle;
import javafx.scene.control.ToggleGroup;
import javafx.scene.layout.BorderPane;
import javafx.scene.layout.HBox;
import javafx.scene.layout.StackPane;
import javafx.scene.layout.VBox;
import javafx.stage.FileChooser;
import javafx.stage.Stage;

public final class MainGraphController implements GraphEngine.GraphEngineListener {

    private final UiState uiState = new UiState();

    private GraphProject project;
    private GraphPanel panel;
    private CommandBus<GraphEngine> bus;
    private InteractionService interaction;
    private GraphEngine engine;
    private GraphPanel previewGraphPanel;

    @FXML
    private MainMenuController menuViewController;
    @FXML
    private GraphWorkspaceController workspaceViewController;
    @FXML
    private EngineOptionsController engineOptionsViewController;
    @FXML
    private DataController dataViewController;
    @FXML
    private GraphStatsController graphStatsController;
    @FXML
    private PreviewController previewController;

    @FXML
    private HBox toolBar;
    @FXML
    private HBox toolsBox;
    @FXML
    private ToggleGroup toolToggleGroup;
    private boolean interactionEnabled = false;

    @FXML
    private TabPane rootTabPane; // remplace rootStack
    @FXML
    private BorderPane overview; // contenu de l'onglet Overview
    @FXML
    private BorderPane dataView; // contenu de l'onglet Data (inclus via fx:include)
    @FXML
    private BorderPane preview; // contenu de l'onglet Preview (inclus via fx:include)
    @FXML
    private StackPane graphHostPane; // zone où se trouve le panel OpenGL (dans overview)
    @FXML
    private VBox graphStats;

    @FXML
    private Button zoomInButton;
    @FXML
    private Button zoomOutButton;

    @FXML
    private void initialize() {
        GraphNativeEngine nativeEngine = new GraphNativeEngine();
        this.engine = new GraphEngine(nativeEngine);
        GraphRenderer mainRenderer = new GraphRenderer(engine, engine.camera(), GraphRenderOptions.straight());
        bus = new CommandBus<>(engine, new EngineExecutor());
        interaction = new InteractionService(bus, uiState);
        panel = new GraphPanel(mainRenderer, interaction);

        menuViewController.setBus(bus);
        menuViewController.setMainController(this);
        workspaceViewController.setMainController(this);
        workspaceViewController.setBus(bus);
        engineOptionsViewController.setBus(bus);
        dataViewController.setBus(bus);
        graphStatsController.setBus(bus);
        previewController.setBus(bus);

        // Création du renderer et du panel pour l'aperçu (Preview)
        GraphRenderer previewRenderer = new GraphRenderer(engine, engine.camera(), GraphRenderOptions.previewView());
        previewGraphPanel = new GraphPanel(previewRenderer, interaction);
        previewGraphPanel.start();

        previewController.setPreviewRenderer(previewRenderer);
        previewController.setGraphPanel(previewGraphPanel.canvas());

        engine.addListener(this);

        setupGraphSurfaceResize();
        setupToolToggle();
        setupCloseWindowListener(nativeEngine);
        setupTabPaneListener();

        graphHostPane.getChildren().add(panel.canvas());
        panel.start();

        setInteractionEnabled(false);
        uiState.setStatus("Prêt");
    }

    // Gère les actions lors du changement d'onglet
    private void setupTabPaneListener() {
        rootTabPane.getSelectionModel().selectedItemProperty().addListener((obs, oldTab, newTab) -> {
            if (newTab == null)
                return;
            String tabText = newTab.getText();
            boolean isOverview = "Overview".equals(tabText);

            // Afficher/masquer la barre d'outils uniquement pour l'onglet Overview
            if (toolsBox != null) {
                toolsBox.setVisible(isOverview);
                toolsBox.setManaged(isOverview);
            }

            if ("Data".equals(tabText)) {
                dataViewController.refresh();
            }
            if ("Preview".equals(tabText)) {
                bus.dispatch(e -> e.stopSimulation());
            }
        });
    }

    // Implémentation de GraphEngineListener
    @Override
    public void onSimulationStarted() {
        Platform.runLater(() -> {
            workspaceViewController.updatePlayPauseIcon(true);
            setInteractionEnabled(false);
            uiState.setStatus("Simulation en cours");
        });
    }

    @Override
    public void onSimulationStopped() {
        Platform.runLater(() -> {
            workspaceViewController.updatePlayPauseIcon(false);
            setInteractionEnabled(true);
            uiState.setStatus("Pause");
        });
    }

    @FXML
    private void zoomIn() {
        bus.dispatch(engine -> engine.camera().zoomIn());
    }

    @FXML
    private void zoomOut() {
        bus.dispatch(engine -> engine.camera().zoomOut());
    }

    private void setInteractionEnabled(boolean enabled) {
        interactionEnabled = enabled;
        toolBar.setDisable(!enabled);
        if (!enabled) {
            interaction.setModeRun();
        } else {
            Toggle selected = toolToggleGroup.getSelectedToggle();
            if (selected != null) {
                String mode = (String) selected.getUserData();
                try {
                    interaction.setMode(InteractionService.Mode.valueOf(mode));
                } catch (IllegalArgumentException e) {
                    interaction.setMode(InteractionService.Mode.SELECT);
                }
            } else {
                interaction.setMode(InteractionService.Mode.SELECT);
            }
        }
    }

    public void openFile(File file) {
        if (file == null)
            return;
        GraphProject.SourceType type = detectType(file);
        this.project = new GraphProject(file, type);
        workspaceViewController.setProjectType(type == GraphProject.SourceType.CSV);
        uiState.setStatus("Fichier sélectionné : " + file.getName());
    }

    public File getFile() {
        if (project == null) {
            return null;
        }
        return project.sourceFile();
    }

    public void setProject(GraphProject project) {
        this.project = project;
    }

    public void startGraphCsv(GraphData.SimilitudeMode similitude,
            GraphData.NodeCommunity community,
            GraphData.RepulsionMode repulsion,
            double width, double height,
            double edgeThreshold, double antiThreshold) {
        if (project == null) {
            alert(Alert.AlertType.WARNING, "Projet manquant", "Choisissez d'abord un fichier.");
            return;
        }
        try {
            bus.dispatchSyncVoid(engine -> {
                engine.stopSimulation();
                engine.loadCsv(similitude, community,
                        edgeThreshold, antiThreshold);
                if (repulsion != null)
                    engine.setRepulsionMode(repulsion);
            });
            engine.setDimensions(width, height);
            engineOptionsViewController.applyCurrentOptions(false);
            bus.dispatch(engine -> engine.startSimulation());
            uiState.setRunning(true);
            uiState.setStatus("Graph chargé : " + project.sourceFile().getName());
            graphStatsController.refreshStats();
        } catch (Exception e) {
            alert(Alert.AlertType.ERROR, "Erreur de chargement", rootCauseMessage(e));
        }
    }

    public void startGraphDot(GraphData.NodeCommunity community, double width, double height) {
        if (project == null) {
            alert(Alert.AlertType.WARNING, "Projet manquant", "Choisissez d'abord un fichier.");
            return;
        }
        try {
            bus.dispatchSyncVoid(engine -> {
                engine.stopSimulation();
                engine.loadDot(project.sourceFile().getAbsolutePath(), community);
            });
            engine.setDimensions(width, height);
            engineOptionsViewController.applyCurrentOptions(false);
            bus.dispatch(engine -> engine.startSimulation());
            uiState.setRunning(true);
            uiState.setStatus("Graph chargé : " + project.sourceFile().getName());
            graphStatsController.refreshStats();
        } catch (Exception e) {
            alert(Alert.AlertType.ERROR, "Erreur de chargement", rootCauseMessage(e));
        }
    }

    private void setupCloseWindowListener(GraphNativeEngine nat) {
        rootTabPane.sceneProperty().addListener((obs, oldScene, scene) -> {
            if (scene != null) {
                scene.windowProperty().addListener((obsW, oldWindow, window) -> {
                    if (window != null) {
                        Stage stage = (Stage) window;
                        stage.setOnCloseRequest(e -> {
                            panel.stop();
                            if (previewGraphPanel != null)
                                previewGraphPanel.stop();
                            engine.dispose();
                        });
                    }
                });
            }
        });
    }

    public void exportPng() {
        if (panel == null) {
            alert(Alert.AlertType.WARNING, "Export PNG", "Aucun graphe affiché.");
            return;
        }

        // Dimensions par défaut = taille actuelle du panneau d'affichage
        int currentWidth = Math.max(1, (int) graphHostPane.getWidth());
        int currentHeight = Math.max(1, (int) graphHostPane.getHeight());

        PngResolutionDialog dialog = new PngResolutionDialog(currentWidth, currentHeight);
        java.util.Optional<int[]> result = dialog.showAndWait();
        if (result.isEmpty())
            return;

        int[] dims = result.get();
        int width = dims[0];
        int height = dims[1];

        FileChooser chooser = new FileChooser();
        chooser.setTitle("Exporter en PNG");
        chooser.getExtensionFilters().add(new FileChooser.ExtensionFilter("Image PNG", "*.png"));
        File out = chooser.showSaveDialog(getStage());
        if (out == null)
            return;
        if (!out.getName().toLowerCase().endsWith(".png")) {
            out = new File(out.getAbsolutePath() + ".png");
        }
        final File outFile = out;

        new Thread(() -> {
            try {
                if (outFile.toPath().getParent() != null) {
                    Files.createDirectories(outFile.toPath().getParent());
                }
                panel.createExporter().exportPng(outFile, width, height);
                Platform.runLater(() -> uiState.setStatus("Export PNG : " + outFile.getName()));
            } catch (Exception e) {
                Platform.runLater(() -> alert(Alert.AlertType.ERROR, "Erreur",
                        "Export PNG impossible : " + e.getMessage()));
            }
        }, "graph-export-thread").start();
    }

    public void exportSvg() {
        if (engine == null || !engine.isGraphLoaded()) {
            alert(Alert.AlertType.WARNING, "Export SVG", "Aucun graphe chargé.");
            return;
        }

        // Demander la résolution souhaitée (par défaut 2000)
        SvgResolutionDialog resDialog = new SvgResolutionDialog(2000);
        Optional<Double> resolution = resDialog.showAndWait();
        if (resolution.isEmpty())
            return;
        double svgSize = resolution.get();

        FileChooser chooser = new FileChooser();
        chooser.setTitle("Exporter en SVG");
        chooser.getExtensionFilters().add(new FileChooser.ExtensionFilter("Fichier SVG", "*.svg"));
        File out = chooser.showSaveDialog(getStage());
        if (out == null)
            return;
        if (!out.getName().toLowerCase().endsWith(".svg")) {
            out = new File(out.getAbsolutePath() + ".svg");
        }
        final File outFile = out;

        GraphEngine.GraphDataSnapshot snapshot = engine.getDataSnapshot();
        double[] dims = engine.getDimensions();

        GraphRenderOptions renderOptions = panel.renderer().getRenderOptions();

        new Thread(() -> {
            try {
                SvgExporter.export(outFile,
                        snapshot.getVertices(),
                        new ArrayList<>(snapshot.getEdges()),
                        dims[0], dims[1],
                        renderOptions,
                        svgSize); //
                Platform.runLater(() -> uiState.setStatus("Export SVG : " + outFile.getName()));
            } catch (Exception e) {
                Platform.runLater(() -> alert(Alert.AlertType.ERROR, "Erreur",
                        "Export SVG impossible : " + e.getMessage()));
            }
        }, "svg-export-thread").start();
    }

    public void setStatsVisible(boolean show) {
        if (graphStats == null)
            return;
        graphStats.setVisible(show);
        graphStats.setManaged(show);
    }

    private GraphProject.SourceType detectType(File file) {
        String name = file.getName().toLowerCase();
        if (name.endsWith(".csv"))
            return GraphProject.SourceType.CSV;
        if (name.endsWith(".dot"))
            return GraphProject.SourceType.DOT;
        throw new IllegalArgumentException("Type de fichier inconnu : " + name);
    }

    private void setupGraphSurfaceResize() {
        graphHostPane.widthProperty().addListener((obs, oldV, newV) -> resizeGraphSurface());
        graphHostPane.heightProperty().addListener((obs, oldV, newV) -> resizeGraphSurface());
    }

    private void resizeGraphSurface() {
        int width = Math.max(1, (int) graphHostPane.getWidth());
        int height = Math.max(1, (int) graphHostPane.getHeight());
        panel.resize(width, height);
        bus.dispatch(engine -> engine.camera().resize(width, height));
    }

    private void setupToolToggle() {
        if (toolToggleGroup == null)
            return;
        toolToggleGroup.selectedToggleProperty().addListener((obs, oldToggle, newToggle) -> {
            if (interactionEnabled && newToggle != null) {
                String mode = (String) newToggle.getUserData();
                try {
                    interaction.setMode(InteractionService.Mode.valueOf(mode));
                } catch (IllegalArgumentException e) {
                    interaction.setMode(InteractionService.Mode.SELECT);
                }
            }
        });
    }

    public Stage getStage() {
        return (Stage) rootTabPane.getScene().getWindow();
    }

    private String rootCauseMessage(Throwable throwable) {
        Throwable current = throwable;
        while (current.getCause() != null && current.getCause() != current) {
            current = current.getCause();
        }
        String message = current.getMessage();
        return message == null || message.isBlank() ? current.toString() : message;
    }

    private void alert(Alert.AlertType type, String title, String content) {
        Alert alert = new Alert(type);
        alert.setTitle(title);
        alert.setHeaderText(null);
        alert.setContentText(content);
        alert.showAndWait();
    }
}
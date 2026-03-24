package com.mongraphe.graphui.controller;

import java.io.File;
import java.nio.file.Files;
import javafx.application.Platform;
import javafx.fxml.FXML;
import javafx.scene.control.Alert;
import javafx.scene.control.Toggle;
import javafx.scene.control.ToggleGroup;
import javafx.scene.layout.BorderPane;
import javafx.scene.layout.Pane;
import javafx.scene.layout.StackPane;
import javafx.scene.layout.VBox;
import javafx.stage.FileChooser;
import javafx.stage.Stage;

import com.mongraphe.graphui.app.CommandBus;
import com.mongraphe.graphui.app.UiState;
import com.mongraphe.graphui.interaction.InteractionService;
import com.mongraphe.graphui.model.GraphData;
import com.mongraphe.graphui.model.GraphProject;
import com.mongraphe.graphui.rendering.EngineExecutor;
import com.mongraphe.graphui.rendering.GraphEngine;
import com.mongraphe.graphui.rendering.GraphNativeEngine;
import com.mongraphe.graphui.rendering.GraphRenderer;
import com.mongraphe.graphui.view.GraphPanel;

public final class MainGraphController {

    private final UiState uiState = new UiState();

    private GraphProject project;
    private GraphPanel panel;
    private CommandBus<GraphEngine> bus;
    private InteractionService interaction;
    private GraphNativeEngine nativeEngine;

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
    private ToggleGroup viewToggleGroup;
    @FXML
    private ToggleGroup toolToggleGroup;

    @FXML
    private StackPane rootStack;
    @FXML
    private BorderPane overview;
    @FXML
    private BorderPane dataView;
    @FXML
    private StackPane graphHostPane;
    @FXML
    private Pane preview;
    @FXML
    private VBox graphStats;

    @FXML
    private void initialize() {
        nativeEngine = new GraphNativeEngine();
        GraphEngine engine = new GraphEngine(nativeEngine);
        GraphRenderer renderer = new GraphRenderer(engine, engine.camera());
        bus = new CommandBus<>(engine, new EngineExecutor());
        interaction = new InteractionService(bus, uiState);
        panel = new GraphPanel(renderer, interaction);

        graphHostPane.getChildren().add(panel.canvas());
        panel.start();

        menuViewController.setBus(bus);
        menuViewController.setMainController(this);
        workspaceViewController.setMainController(this);
        workspaceViewController.setBus(bus);
        engineOptionsViewController.setBus(bus);
        dataViewController.setBus(bus);
        graphStatsController.setMainController(this);
        graphStatsController.setBus(bus);

        setupGraphSurfaceResize();
        setupToolToggle();
        setupCloseWindowListener(nativeEngine);

        uiState.setStatus("Prêt");
    }

    public void openFile(File file) {
        if (file == null) {
            return;
        }
        this.project = new GraphProject(file, detectType(file));
        uiState.setStatus("Fichier sélectionné : " + file.getName());
    }

    public void setProject(GraphProject project) {
        this.project = project;
    }

    public void startGraph(GraphData.SimilitudeMode similitude,
            GraphData.NodeCommunity community,
            GraphData.RepulsionMode repulsion) {

        if (project == null) {
            alert(Alert.AlertType.WARNING, "Projet manquant",
                    "Choisissez d'abord un fichier CSV, DOT ou un projet .mongraphe.");
            return;
        }

        try {
            bus.dispatchSyncVoid(engine -> {
                engine.stopSimulation();
                engine.freeNativeMemory();
                engine.load(project.sourceFile().getAbsolutePath(), project.sourceType(), similitude, community);
                if (repulsion != null) {
                    engine.setRepulsionMode(repulsion);
                }
            });

            engineOptionsViewController.applyCurrentOptions(false);
            bus.dispatch(engine -> engine.startSimulation());
            uiState.setRunning(true);
            uiState.setStatus("Graph chargé : " + project.sourceFile().getName());
            graphStatsController.refreshStats();
            if (overview != null) {
                overview.setVisible(true);
            }
        } catch (Exception e) {
            alert(Alert.AlertType.ERROR, "Erreur de chargement",
                    "Impossible de charger le graphe : " + rootCauseMessage(e));
        }
    }

    private void setupCloseWindowListener(GraphNativeEngine nat) {
        rootStack.sceneProperty().addListener((obs, oldScene, scene) -> {
            if (scene != null) {

                scene.windowProperty().addListener((obsW, oldWindow, window) -> {
                    if (window != null) {
                        Stage stage = (Stage) window;

                        stage.setOnCloseRequest(e -> {
                            panel.stop();
                            nat.freeAllocatedMemory();
                        });
                    }
                });

            }
        });
    }

    @FXML
    private void handleViewChange() {
        Toggle selected = viewToggleGroup == null ? null : viewToggleGroup.getSelectedToggle();
        String view = selected == null ? "overview" : String.valueOf(selected.getUserData());

        overview.setVisible("overview".equals(view));
        dataView.setVisible("data".equals(view));
        preview.setVisible("preview".equals(view));

        if ("data".equals(view)) {
            dataViewController.refresh();
        }
    }

    public void openGraphFile() {
        FileChooser chooser = new FileChooser();
        chooser.setTitle("Ouvrir un fichier de graphe");
        chooser.getExtensionFilters().addAll(
                new FileChooser.ExtensionFilter("Projets MonGraphe", "*.mongraphe"),
                new FileChooser.ExtensionFilter("Graph CSV / DOT", "*.csv", "*.dot"),
                new FileChooser.ExtensionFilter("Tous les fichiers", "*.*"));
        File file = chooser.showOpenDialog(getStage());
        if (file != null) {
            openFile(file);
            if (!file.getName().toLowerCase().endsWith(".mongraphe")) {
                GraphData.SimilitudeMode sim = workspaceViewController.getSelectedSimilarity();
                GraphData.NodeCommunity community = workspaceViewController.getSelectedCommunity();
                if (sim != null && community != null) {
                    startGraph(sim, community, workspaceViewController.getSelectedRepulsionMode());
                }
            }
        }
    }

    /*
     * public void saveProject() {
     * if (project == null) {
     * alert(Alert.AlertType.WARNING, "Enregistrer",
     * "Aucun fichier source n'est chargé.");
     * return;
     * }
     * 
     * FileChooser chooser = new FileChooser();
     * chooser.setTitle("Enregistrer le projet");
     * chooser.getExtensionFilters().add(new
     * FileChooser.ExtensionFilter("Projet MonGraphe", "*.mongraphe"));
     * File file = chooser.showSaveDialog(getStage());
     * if (file == null) {
     * return;
     * }
     * if (!file.getName().toLowerCase().endsWith(".mongraphe")) {
     * file = new File(file.getAbsolutePath() + ".mongraphe");
     * }
     * 
     * Properties p = new Properties();
     * p.setProperty("source", project.sourceFile().getAbsolutePath());
     * p.setProperty("sourceType", project.sourceType().name());
     * if (currentSimilarity != null)
     * p.setProperty("similarity", currentSimilarity.name());
     * if (currentCommunity != null)
     * p.setProperty("community", currentCommunity.name());
     * if (currentRepulsion != null)
     * p.setProperty("workspaceRepulsion", currentRepulsion.name());
     * engineOptionsViewController.fillProperties(p);
     * 
     * try (FileOutputStream fos = new FileOutputStream(file)) {
     * p.store(fos, "MonGraphe project");
     * uiState.setStatus("Projet enregistré : " + file.getName());
     * } catch (IOException e) {
     * alert(Alert.AlertType.ERROR, "Erreur",
     * "Impossible d'enregistrer le projet : " + e.getMessage());
     * }
     * }
     */

    public void exportPng() {
        if (panel == null) {
            alert(Alert.AlertType.WARNING, "Export PNG", "Aucun graphe affiché.");
            return;
        }

        FileChooser chooser = new FileChooser();
        chooser.setTitle("Exporter en PNG");
        chooser.getExtensionFilters().add(new FileChooser.ExtensionFilter("Image PNG", "*.png"));
        File out = chooser.showSaveDialog(getStage());
        if (out == null) {
            return;
        }
        if (!out.getName().toLowerCase().endsWith(".png")) {
            out = new File(out.getAbsolutePath() + ".png");
        }

        final File outFile = out;
        int width = Math.max(1, (int) graphHostPane.getWidth());
        int height = Math.max(1, (int) graphHostPane.getHeight());

        new Thread(() -> {
            try {
                if (outFile.toPath().getParent() != null) {
                    Files.createDirectories(outFile.toPath().getParent());
                }
                panel.createExporter().exportPng(outFile, width, height);
                Platform.runLater(() -> uiState.setStatus("Export PNG : " + outFile.getName()));
            } catch (Exception e) {
                Platform.runLater(
                        () -> alert(Alert.AlertType.ERROR, "Erreur", "Export PNG impossible : " + e.getMessage()));
            }
        }, "graph-export-thread").start();
    }

    public void setStatsVisible(boolean show) {
        if (graphStats == null) {
            return;
        }
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
        if (toolToggleGroup == null) {
            return;
        }

        toolToggleGroup.selectedToggleProperty().addListener((obs, oldToggle, newToggle) -> {
            String mode = newToggle == null ? "RUN" : String.valueOf(newToggle.getUserData());
            try {
                interaction.setMode(InteractionService.Mode.valueOf(mode));
            } catch (IllegalArgumentException e) {
                interaction.setMode(InteractionService.Mode.RUN);
            }
        });
    }

    public Stage getStage() {
        return (Stage) rootStack.getScene().getWindow();
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

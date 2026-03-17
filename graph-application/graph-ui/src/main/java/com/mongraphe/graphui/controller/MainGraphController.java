package com.mongraphe.graphui.controller;

import java.awt.Desktop;
import java.io.File;
import java.io.FileInputStream;
import java.io.FileOutputStream;
import java.io.IOException;
import java.net.URI;
import java.nio.file.Files;
import java.util.Properties;

import com.mongraphe.graphui.app.CommandBus;
import com.mongraphe.graphui.app.UiState;
import com.mongraphe.graphui.interaction.InteractionService;
import com.mongraphe.graphui.model.GraphData;
import com.mongraphe.graphui.model.GraphProject;
import com.mongraphe.graphui.model.Metadata;
import com.mongraphe.graphui.model.Vertex;
import com.mongraphe.graphui.rendering.EngineExecutor;
import com.mongraphe.graphui.rendering.GraphEngine;
import com.mongraphe.graphui.rendering.GraphNativeEngine;
import com.mongraphe.graphui.rendering.GraphRenderer;
import com.mongraphe.graphui.view.GraphPanel;

import javafx.animation.AnimationTimer;
import javafx.application.Platform;
import javafx.fxml.FXML;
import javafx.fxml.FXMLLoader;
import javafx.geometry.Rectangle2D;
import javafx.scene.Parent;
import javafx.scene.Scene;
import javafx.scene.control.Alert;
import javafx.scene.control.Label;
import javafx.scene.control.MenuBar;
import javafx.scene.control.Toggle;
import javafx.scene.control.ToggleButton;
import javafx.scene.control.ToggleGroup;
import javafx.scene.layout.BorderPane;
import javafx.scene.layout.Pane;
import javafx.scene.layout.StackPane;
import javafx.scene.layout.VBox;
import javafx.stage.FileChooser;
import javafx.stage.Screen;
import javafx.stage.Stage;

public final class MainGraphController {

    private final UiState uiState = new UiState();

    private GraphProject project;
    private GraphPanel panel;
    private CommandBus<GraphEngine> bus;
    private InteractionService interaction;
    private GraphNativeEngine nativeEngine;

    private GraphData.SimilitudeMode currentSimilarity;
    private GraphData.NodeCommunity currentCommunity;
    private GraphData.RepulsionMode currentRepulsion;

    private AnimationTimer statsTimer;

    @FXML private MenuBar menuView;
    @FXML private MainMenuController menuViewController;
    @FXML private VBox workspaceView;
    @FXML private GraphWorkspaceController workspaceViewController;
    @FXML private VBox engineOptionsView;
    @FXML private EngineOptionsController engineOptionsViewController;
    @FXML private BorderPane dataView;
    @FXML private DataController dataViewController;

    @FXML private ToggleGroup viewToggleGroup;
    @FXML private ToggleGroup toolToggleGroup;
    @FXML private ToggleButton toolRun;
    @FXML private ToggleButton toolSelect;
    @FXML private ToggleButton toolMove;
    @FXML private ToggleButton toolDelete;

    @FXML private StackPane rootStack;
    @FXML private BorderPane overview;
    @FXML private StackPane graphHostPane;
    @FXML private Pane preview;
    @FXML private Label statusLabel;
    @FXML private VBox statsPane;

    @FXML private Label nodesDisplayedLabel;
    @FXML private Label nodesHiddenLabel;
    @FXML private Label nodesDeletedLabel;
    @FXML private Label edgesDisplayedLabel;
    @FXML private Label edgesHiddenLabel;
    @FXML private Label totalElementsLabel;
    @FXML private Label recommendedThresholdLabel;
    @FXML private Label recommendedAntiThresholdLabel;
    @FXML private Label selectedVertexIdLabel;
    @FXML private Label selectedVertexXLabel;
    @FXML private Label selectedVertexYLabel;

    @FXML
    private void initialize() {
        nativeEngine = new GraphNativeEngine();
        GraphEngine engine = new GraphEngine(nativeEngine);
        GraphRenderer renderer = new GraphRenderer(engine, engine.camera());
        bus = new CommandBus<>(engine, new EngineExecutor());
        interaction = new InteractionService(bus, uiState);
        panel = new GraphPanel(renderer, interaction);

        statusLabel.textProperty().bind(uiState.statusTextProperty());
        graphHostPane.getChildren().add(panel.canvas());
        panel.start();

        menuViewController.setBus(bus);
        menuViewController.setMainController(this);
        workspaceViewController.setMainController(this);
        engineOptionsViewController.setBus(bus);
        dataViewController.setBus(bus);

        setupGraphSurfaceResize();
        setupToolToggle();
        setupCloseWindowListener();
        startStatsTimer();

        uiState.setStatus("Prêt");
    }

    public void openFile(File file) {
        if (file == null) {
            return;
        }

        if (file.getName().toLowerCase().endsWith(".mongraphe")) {
            openSavedProject(file);
            return;
        }

        try {
            this.project = new GraphProject(file, detectType(file));
            uiState.setStatus("Fichier sélectionné : " + file.getName());
        } catch (IllegalArgumentException e) {
            alert(Alert.AlertType.WARNING, "Format non pris en charge", e.getMessage());
        }
    }

    public void setProject(GraphProject project) {
        this.project = project;
    }

    public void startGraph(GraphData.SimilitudeMode similitude,
            GraphData.NodeCommunity community,
            GraphData.RepulsionMode repulsion) {

        if (project == null) {
            alert(Alert.AlertType.WARNING, "Projet manquant", "Choisissez d'abord un fichier CSV, DOT ou un projet .mongraphe.");
            return;
        }

        currentSimilarity = similitude;
        currentCommunity = community;
        currentRepulsion = repulsion;

        try {
            bus.dispatchSyncVoid(engine -> {
                engine.stopSimulation();
                engine.freeNativeMemory();
                engine.load(project.sourceFile().getAbsolutePath(), project.sourceType(), similitude, community);
                if (repulsion != null) {
                    engine.setRepulsionMode(repulsion);
                }
            });

            setupCloseWindowListener(nativeEngine);


            engineOptionsViewController.applyCurrentOptions(false);
            bus.dispatch(engine -> engine.startSimulation());
            uiState.setRunning(true);
            uiState.setStatus("Graph chargé : " + project.sourceFile().getName());
            refreshStats();
            if (overview != null) {
                overview.setVisible(true);
            }
        } catch (Exception e) {
            alert(Alert.AlertType.ERROR, "Erreur de chargement", "Impossible de charger le graphe : " + rootCauseMessage(e));
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
            Platform.runLater(() -> dataViewController.refresh());
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

    public void saveProject() {
        if (project == null) {
            alert(Alert.AlertType.WARNING, "Enregistrer", "Aucun fichier source n'est chargé.");
            return;
        }

        FileChooser chooser = new FileChooser();
        chooser.setTitle("Enregistrer le projet");
        chooser.getExtensionFilters().add(new FileChooser.ExtensionFilter("Projet MonGraphe", "*.mongraphe"));
        File file = chooser.showSaveDialog(getStage());
        if (file == null) {
            return;
        }
        if (!file.getName().toLowerCase().endsWith(".mongraphe")) {
            file = new File(file.getAbsolutePath() + ".mongraphe");
        }

        Properties p = new Properties();
        p.setProperty("source", project.sourceFile().getAbsolutePath());
        p.setProperty("sourceType", project.sourceType().name());
        if (currentSimilarity != null) p.setProperty("similarity", currentSimilarity.name());
        if (currentCommunity != null) p.setProperty("community", currentCommunity.name());
        if (currentRepulsion != null) p.setProperty("workspaceRepulsion", currentRepulsion.name());
        engineOptionsViewController.fillProperties(p);

        try (FileOutputStream fos = new FileOutputStream(file)) {
            p.store(fos, "MonGraphe project");
            uiState.setStatus("Projet enregistré : " + file.getName());
        } catch (IOException e) {
            alert(Alert.AlertType.ERROR, "Erreur", "Impossible d'enregistrer le projet : " + e.getMessage());
        }
    }

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
                Platform.runLater(() -> alert(Alert.AlertType.ERROR, "Erreur", "Export PNG impossible : " + e.getMessage()));
            }
        }, "graph-export-thread").start();
    }

    public void undo() {
        bus.undo();
        uiState.setStatus("Annulation demandée");
    }

    public void redo() {
        bus.redo();
        uiState.setStatus("Rétablissement demandé");
    }

    public void toggleFullScreen() {
        Stage stage = getStage();
        stage.setFullScreen(!stage.isFullScreen());
    }

    public void setStatsVisible(boolean show) {
        if (statsPane == null) {
            return;
        }
        statsPane.setVisible(show);
        statsPane.setManaged(show);
    }

    public void resetLayout() {
        bus.dispatchSyncVoid(engine -> engine.camera().reset());
        uiState.setStatus("Vue réinitialisée");
    }

    public void showOptionsInfo() {
        alert(Alert.AlertType.INFORMATION,
                "Aide rapide",
                "Outils disponibles : Run / Select / Move / Delete\n"
                        + "SPACE : pause / reprise\n"
                        + "SUPPR : supprime le sommet sélectionné\n"
                        + "Molette : zoom\n"
                        + "Clic droit + drag : déplacement de la caméra\n"
                        + "Undo / Redo : via le menu Édition");
    }

    public void openDocumentation() {
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
            uiState.setStatus("Impossible d'ouvrir la documentation");
        }
    }

    public void showAbout() {
        alert(Alert.AlertType.INFORMATION,
                "À propos",
                "MonGraphe\nVisualisation de graphes (JavaFX + JOGL + JNI)\nBuild refactor + fonctionnalités restaurées.");
    }

    public void closeWorkspace() {
        returnToHome();
    }

    public void returnToHome() {
        try {
            disposeGraph();
            FXMLLoader loader = new FXMLLoader(getClass().getResource("/fxml/HomeScreen.fxml"));
            Parent root = loader.load();
            Scene scene = new Scene(root);
            Stage stage = getStage();
            Rectangle2D sb = Screen.getPrimary().getVisualBounds();
            stage.setTitle("MonGraphe");
            stage.setScene(scene);
            stage.setX(sb.getMinX());
            stage.setY(sb.getMinY());
            stage.setWidth(sb.getWidth());
            stage.setHeight(sb.getHeight());
            stage.show();
        } catch (IOException e) {
            alert(Alert.AlertType.ERROR, "Erreur", "Impossible de revenir à l'accueil : " + e.getMessage());
        }
    }

    public void handleQuit() {
        disposeGraph();
        Platform.exit();
    }

    private void openSavedProject(File projectFile) {
        Properties p = new Properties();
        try (FileInputStream fis = new FileInputStream(projectFile)) {
            p.load(fis);
        } catch (IOException e) {
            alert(Alert.AlertType.ERROR, "Erreur", "Impossible de lire le projet : " + e.getMessage());
            return;
        }

        String source = p.getProperty("source");
        String sourceType = p.getProperty("sourceType");
        if (source == null || sourceType == null) {
            alert(Alert.AlertType.ERROR, "Projet invalide", "Le fichier .mongraphe ne contient pas de source exploitable.");
            return;
        }

        File sourceFile = new File(source);
        if (!sourceFile.exists()) {
            alert(Alert.AlertType.ERROR, "Source introuvable", "Le fichier source référencé par le projet n'existe plus : " + sourceFile.getAbsolutePath());
            return;
        }

        try {
            project = new GraphProject(sourceFile, GraphProject.SourceType.valueOf(sourceType));
        } catch (IllegalArgumentException e) {
            alert(Alert.AlertType.ERROR, "Projet invalide", "Type de source inconnu dans le projet : " + sourceType);
            return;
        }

        GraphData.SimilitudeMode similarity = parseEnum(p.getProperty("similarity"), GraphData.SimilitudeMode.class);
        GraphData.NodeCommunity community = parseEnum(p.getProperty("community"), GraphData.NodeCommunity.class);
        GraphData.RepulsionMode workspaceRepulsion = parseEnum(p.getProperty("workspaceRepulsion"), GraphData.RepulsionMode.class);

        workspaceViewController.setSelections(similarity, community, workspaceRepulsion);
        engineOptionsViewController.loadFromProperties(p);
        uiState.setStatus("Projet chargé : " + projectFile.getName());

        if (similarity != null && community != null) {
            startGraph(similarity, community, workspaceRepulsion);
        }
    }

    private <E extends Enum<E>> E parseEnum(String name, Class<E> enumClass) {
        if (name == null || name.isBlank()) {
            return null;
        }
        try {
            return Enum.valueOf(enumClass, name);
        } catch (IllegalArgumentException e) {
            return null;
        }
    }

    private GraphProject.SourceType detectType(File file) {
        String name = file.getName().toLowerCase();
        if (name.endsWith(".csv")) return GraphProject.SourceType.CSV;
        if (name.endsWith(".dot")) return GraphProject.SourceType.DOT;
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

        if (toolRun != null) {
            toolToggleGroup.selectToggle(toolRun);
        }
    }

    private void setupCloseWindowListener() {
        rootStack.sceneProperty().addListener((obs, oldScene, scene) -> {
            if (scene == null) {
                return;
            }
            scene.windowProperty().addListener((obsW, oldWindow, window) -> {
                if (window instanceof Stage stage) {
                    stage.setOnCloseRequest(e -> disposeGraph());
                }
            });
        });
    }

    private void startStatsTimer() {
        if (statsTimer != null) {
            return;
        }
        statsTimer = new AnimationTimer() {
            private long last;

            @Override
            public void handle(long now) {
                if (now - last < 200_000_000L) {
                    return;
                }
                last = now;
                refreshStats();
            }
        };
        statsTimer.start();
    }

    private void refreshStats() {
        if (bus == null) {
            return;
        }

        try {
            GraphEngine.GraphDataSnapshot snapshot = bus.dispatchSync(GraphEngine::getDataSnapshot);
            int totalVertices = snapshot.getVertices().size();
            int totalEdges = snapshot.getEdges().size();
            long deletedVertices = snapshot.getVertices().stream().filter(Vertex::isDeleted).count();
            int visibleVertices = snapshot.getVisibleVertexCount();
            int visibleEdges = snapshot.getVisibleEdgeCount();
            int hiddenVertices = Math.max(0, totalVertices - visibleVertices - (int) deletedVertices);
            int hiddenEdges = Math.max(0, totalEdges - visibleEdges);

            nodesDisplayedLabel.setText(String.valueOf(visibleVertices));
            nodesHiddenLabel.setText(String.valueOf(hiddenVertices));
            nodesDeletedLabel.setText(String.valueOf(deletedVertices));
            edgesDisplayedLabel.setText(String.valueOf(visibleEdges));
            edgesHiddenLabel.setText(String.valueOf(hiddenEdges));
            totalElementsLabel.setText(String.valueOf(totalVertices + totalEdges));

            Metadata initMetadata = bus.dispatchSync(GraphEngine::getInitMetadata);
            if (initMetadata != null) {
                recommendedThresholdLabel.setText(String.format("%.4f", initMetadata.getEdgeThreshold()));
                recommendedAntiThresholdLabel.setText(String.format("%.4f", initMetadata.getAntiThreshold()));
            } else {
                recommendedThresholdLabel.setText("-");
                recommendedAntiThresholdLabel.setText("-");
            }

            int selectedId = bus.dispatchSync(engine -> engine.model().getSelectedVertexId());
            if (selectedId >= 0) {
                Vertex selected = bus.dispatchSync(engine -> engine.model().vertexById(selectedId));
                if (selected != null) {
                    selectedVertexIdLabel.setText(String.valueOf(selectedId));
                    selectedVertexXLabel.setText(String.format("%.2f", selected.getX()));
                    selectedVertexYLabel.setText(String.format("%.2f", selected.getY()));
                }
            } else {
                selectedVertexIdLabel.setText("-");
                selectedVertexXLabel.setText("-");
                selectedVertexYLabel.setText("-");
            }
        } catch (Exception ignored) {
        }
    }

    private void disposeGraph() {
        try {
            if (statsTimer != null) {
                statsTimer.stop();
                statsTimer = null;
            }
        } catch (Exception ignored) {
        }

        try {
            if (bus != null) {
                bus.dispatchSyncVoid(engine -> {
                    engine.stopSimulation();
                    engine.freeNativeMemory();
                });
            }
        } catch (Exception ignored) {
        }

        try {
            if (panel != null) {
                panel.dispose();
            }
        } catch (Exception ignored) {
        }
    }

    private Stage getStage() {
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

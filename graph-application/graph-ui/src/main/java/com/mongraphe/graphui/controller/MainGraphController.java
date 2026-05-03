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
import javafx.scene.control.TabPane;
import javafx.scene.control.Toggle;
import javafx.scene.control.ToggleGroup;
import javafx.scene.layout.HBox;
import javafx.scene.layout.StackPane;
import javafx.scene.layout.VBox;
import javafx.stage.FileChooser;
import javafx.stage.Stage;

/**
 * Contrôleur JavaFX principal coordonnant la visualisation et la gestion du
 * graphe.
 *
 * <p>
 * Cette classe agit comme le point central de l'application (Main Controller).
 * Elle orchestre
 * les interactions entre le moteur de rendu OpenGL (OpenGLFX/LWJGL), le système de
 * commandes,
 * l'état de l'UI et les différents sous-contrôleurs injectés via FXML.
 * </p>
 *
 * <h2>Responsabilités</h2>
 * <ul>
 * <li>Initialiser le moteur de rendu {@code GraphEngine} et le bus de commandes
 * {@code CommandBus}.</li>
 * <li>Gérer le cycle de vie de la fenêtre et des ressources natives
 * (dispose).</li>
 * <li>Coordonner les sous-vues (Menu, Workspace, Options, Data, Stats,
 * Preview).</li>
 * <li>Piloter le chargement des données (CSV/DOT) et le lancement des
 * simulations de forces.</li>
 * <li>Gérer les exports d'images (PNG) et de fichiers vectoriels (SVG) avec
 * choix de résolution.</li>
 * <li>Assurer la navigation entre les onglets (Vue générale, Données,
 * Aperçu).</li>
 * </ul>
 *
 * <h2>Architecture et Flux</h2>
 * <ol>
 * <li>L'initialisation configure les deux panels OpenGL (principal et
 * aperçu).</li>
 * <li>Les actions utilisateur sont capturées par {@code InteractionService} et
 * dispatchées via le bus.</li>
 * <li>Le contrôleur écoute les événements du moteur (simulation
 * démarrée/arrêtée) pour mettre à jour l'interface.</li>
 * </ol>
 */
public final class MainGraphController implements GraphEngine.GraphEngineListener {

    /** État interne de l'interface utilisateur (Statut, mode de fonctionnement). */
    private final UiState uiState = new UiState();

    /** Données du projet en cours (Fichier source et type). */
    private GraphProject project;

    /** Panneau d'affichage principal utilisant OpenGLFX/LWJGL pour le rendu OpenGL. */
    private GraphPanel panel;

    /** Système de transport des commandes pour manipuler le moteur de rendu. */
    private CommandBus<GraphEngine> bus;

    /** Exécuteur dédié au moteur, à arrêter explicitement lors de la fermeture. */
    private EngineExecutor engineExecutor;

    /**
     * Service gérant les modes d'interaction souris/clavier (Move, Select, etc.).
     */
    private InteractionService interaction;

    /** Cœur logique gérant les calculs et la structure du graphe. */
    private GraphEngine engine;

    /** Panneau d'affichage secondaire utilisé pour l'onglet de prévisualisation. */
    private GraphPanel previewGraphPanel;

    // --- Sous-contrôleurs injectés ---
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

    // --- Éléments UI ---
    @FXML
    private HBox toolBar;
    @FXML
    private HBox toolsBox;
    @FXML
    private ToggleGroup toolToggleGroup;
    @FXML
    private TabPane rootTabPane;
    @FXML
    private StackPane graphHostPane;
    @FXML
    private VBox graphStats;

    /** Flag indiquant si l'interaction directe avec le graphe est active. */
    private boolean interactionEnabled = false;

    private boolean shuttingDown = false;
    private boolean closeSequenceArmed = false;

    /**
     * Méthode d'initialisation appelée automatiquement par JavaFX.
     * <p>
     * Configure le moteur natif, les systèmes de rendu, injecte les dépendances
     * dans tous les sous-contrôleurs et lance les threads de rendu OpenGL.
     * </p>
     */
    @FXML
    private void initialize() {
        GraphNativeEngine nativeEngine = new GraphNativeEngine();
        this.engine = new GraphEngine(nativeEngine);
        GraphRenderer mainRenderer = new GraphRenderer(engine, engine.camera(), GraphRenderOptions.straight());
        engineExecutor = new EngineExecutor();
        bus = new CommandBus<>(engine, engineExecutor);
        interaction = new InteractionService(bus, uiState);
        panel = new GraphPanel(mainRenderer, interaction);

        // Injection des dépendances croisées
        menuViewController.setBus(bus);
        menuViewController.setMainController(this);
        workspaceViewController.setMainController(this);
        workspaceViewController.setBus(bus);
        engineOptionsViewController.setBus(bus);
        dataViewController.setBus(bus);
        graphStatsController.setBus(bus);
        previewController.setBus(bus);

        // Configuration de l'aperçu (Preview)
        GraphRenderer previewRenderer = new GraphRenderer(engine, engine.camera(), GraphRenderOptions.previewView());
        previewGraphPanel = new GraphPanel(previewRenderer, interaction);
        previewGraphPanel.stop();

        previewController.setPreviewRenderer(previewRenderer);
        previewController.setGraphPanel(previewGraphPanel);

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

    /**
     * Gère les comportements spécifiques lors du changement d'onglet
     * (Overview/Data/Preview).
     * <p>
     * Masque les outils inutiles, rafraîchit les tables de données ou arrête la
     * simulation lors du passage à l'aperçu pour économiser les ressources.
     * </p>
     */
    private void setupTabPaneListener() {
        rootTabPane.getSelectionModel().selectedItemProperty().addListener((obs, oldTab, newTab) -> {
            if (newTab == null)
                return;
            String tabText = newTab.getText();
            boolean isOverview = "Overview".equals(tabText);
            boolean isPreview = "Preview".equals(tabText);

            if (toolsBox != null) {
                toolsBox.setVisible(isOverview);
                toolsBox.setManaged(isOverview);
            }

            // Un seul GLCanvas doit tourner à la fois. L'ancienne version lançait
            // l'aperçu dès l'initialisation, ce qui doublait le coût GLCanvas,
            // les uploads et les callbacks OpenGLFX même lorsque l'onglet Preview
            // n'était pas visible.
            if (panel != null) {
                if (isOverview) panel.start(); else panel.stop();
            }
            if (previewGraphPanel != null) {
                if (isPreview) {
                    previewGraphPanel.start();
                    previewGraphPanel.forceRenderBurst();
                } else {
                    previewGraphPanel.stop();
                }
            }

            if ("Data".equals(tabText)) {
                dataViewController.refresh();
            }
            if (isPreview) {
                bus.dispatch(e -> e.stopSimulation());
            }
        });
    }

    /**
     * Met à jour l'interface lorsque la simulation de force démarre.
     * Désactive l'interaction manuelle pour éviter les conflits de calcul.
     */
    @Override
    public void onSimulationStarted() {
        Platform.runLater(() -> {
            workspaceViewController.updatePlayPauseIcon(true);
            setInteractionEnabled(false);
            uiState.setStatus("Simulation en cours");
        });
    }

    /**
     * Met à jour l'interface lorsque la simulation s'arrête.
     * Réactive l'interaction manuelle (sélection, déplacement de sommets).
     */
    @Override
    public void onSimulationStopped() {
        Platform.runLater(() -> {
            workspaceViewController.updatePlayPauseIcon(false);
            setInteractionEnabled(true);
            uiState.setStatus("Pause");
        });
    }

    /** Zoom avant sur la caméra du graphe. */
    @FXML
    private void zoomIn() {
        bus.dispatch(engine -> engine.camera().zoomIn());
    }

    /** Zoom arrière sur la caméra du graphe. */
    @FXML
    private void zoomOut() {
        bus.dispatch(engine -> engine.camera().zoomOut());
    }

    /**
     * Active ou désactive les contrôles d'interaction.
     * 
     * @param enabled vrai si les outils sont accessibles.
     */
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

    /**
     * Associe un fichier physique au projet actuel.
     * 
     * @param file Le fichier .csv ou .dot à charger.
     */
    public void openFile(File file) {
        if (file == null)
            return;
        GraphProject.SourceType type = detectType(file);
        this.project = new GraphProject(file, type);
        workspaceViewController.setProjectType(type == GraphProject.SourceType.CSV);
        uiState.setStatus("Fichier sélectionné : " + file.getName());
    }

    /**
     * Déclenche le chargement et la simulation d'un graphe à partir d'un fichier
     * CSV.
     * * @param similitude Méthode de calcul des similitudes.
     * 
     * @param community     Algorithme de détection de communautés.
     * @param repulsion     Mode de gestion de la répulsion entre nœuds.
     * @param width         Largeur de l'espace de simulation.
     * @param height        Hauteur de l'espace de simulation.
     * @param edgeThreshold Seuil de filtrage des arêtes.
     * @param antiThreshold Seuil de répulsion négative.
     */
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
                engine.setDimensions(width, height);
                engine.loadCsv(similitude, community, edgeThreshold, antiThreshold);
                if (repulsion != null)
                    engine.setRepulsionMode(repulsion);
            });
            engineOptionsViewController.applyCurrentOptions(false);
            bus.dispatch(engine -> engine.startSimulation());
            uiState.setRunning(true);
            uiState.setStatus("Graph chargé : " + project.sourceFile().getName());
            graphStatsController.refreshStats();
        } catch (Exception e) {
            alert(Alert.AlertType.ERROR, "Erreur de chargement", rootCauseMessage(e));
        }
    }

    /**
     * Déclenche le chargement et la simulation d'un graphe à partir d'un fichier
     * DOT.
     */
    public void startGraphDot(GraphData.NodeCommunity community, double width, double height) {
        if (project == null) {
            alert(Alert.AlertType.WARNING, "Projet manquant", "Choisissez d'abord un fichier.");
            return;
        }
        try {
            bus.dispatchSyncVoid(engine -> {
                engine.stopSimulation();
                engine.setDimensions(width, height);
                engine.loadDot(project.sourceFile().getAbsolutePath(), community);
            });
            engineOptionsViewController.applyCurrentOptions(false);
            bus.dispatch(engine -> engine.startSimulation());
            uiState.setRunning(true);
            uiState.setStatus("Graph chargé : " + project.sourceFile().getName());
            graphStatsController.refreshStats();
        } catch (Exception e) {
            alert(Alert.AlertType.ERROR, "Erreur de chargement", rootCauseMessage(e));
        }
    }

    /**
     * Capture le contenu du panel OpenGL et l'enregistre au format PNG.
     * <p>
     * Une boîte de dialogue permet de définir une résolution personnalisée
     * (ex: 4K) indépendante de la taille de la fenêtre.
     * </p>
     */
    public void exportPng() {
        if (panel == null) {
            alert(Alert.AlertType.WARNING, "Export PNG", "Aucun graphe affiché.");
            return;
        }

        int currentWidth = Math.max(1, (int) graphHostPane.getWidth());
        int currentHeight = Math.max(1, (int) graphHostPane.getHeight());

        PngResolutionDialog dialog = new PngResolutionDialog(currentWidth, currentHeight);
        java.util.Optional<int[]> result = dialog.showAndWait();
        if (result.isEmpty())
            return;

        int[] dims = result.get();
        FileChooser chooser = new FileChooser();
        chooser.setTitle("Exporter en PNG");
        chooser.getExtensionFilters().add(new FileChooser.ExtensionFilter("Image PNG", "*.png"));
        File out = chooser.showSaveDialog(getStage());
        if (out == null)
            return;

        final File outFile = out.getName().toLowerCase().endsWith(".png") ? out
                : new File(out.getAbsolutePath() + ".png");

        new Thread(() -> {
            try {
                if (outFile.toPath().getParent() != null)
                    Files.createDirectories(outFile.toPath().getParent());
                panel.createExporter().exportPng(outFile, dims[0], dims[1]);
                Platform.runLater(() -> uiState.setStatus("Export PNG : " + outFile.getName()));
            } catch (Exception e) {
                Platform.runLater(
                        () -> alert(Alert.AlertType.ERROR, "Erreur", "Export PNG impossible : " + e.getMessage()));
            }
        }, "graph-export-thread").start();
    }

    /**
     * Génère un fichier vectoriel SVG représentant le graphe actuel.
     * <p>
     * Utilise les positions actuelles des sommets via un snapshot pour garantir
     * la cohérence de l'export même si le moteur continue de tourner.
     * </p>
     */
    public void exportSvg() {
        if (engine == null || !engine.isGraphLoaded()) {
            alert(Alert.AlertType.WARNING, "Export SVG", "Aucun graphe chargé.");
            return;
        }

        SvgResolutionDialog resDialog = new SvgResolutionDialog(2000);
        Optional<Double> resolution = resDialog.showAndWait();
        if (resolution.isEmpty())
            return;

        FileChooser chooser = new FileChooser();
        chooser.setTitle("Exporter en SVG");
        chooser.getExtensionFilters().add(new FileChooser.ExtensionFilter("Fichier SVG", "*.svg"));
        File out = chooser.showSaveDialog(getStage());
        if (out == null)
            return;

        final File outFile = out.getName().toLowerCase().endsWith(".svg") ? out
                : new File(out.getAbsolutePath() + ".svg");

        GraphEngine.GraphDataSnapshot snapshot = engine.getDataSnapshot();
        double[] dims = engine.getDimensions();
        GraphRenderOptions renderOptions = panel.renderer().getRenderOptions();

        new Thread(() -> {
            try {
                SvgExporter.export(outFile, snapshot.getVertices(), new ArrayList<>(snapshot.getEdges()),
                        dims[0], dims[1], renderOptions, resolution.get());
                Platform.runLater(() -> uiState.setStatus("Export SVG : " + outFile.getName()));
            } catch (Exception e) {
                Platform.runLater(
                        () -> alert(Alert.AlertType.ERROR, "Erreur", "Export SVG impossible : " + e.getMessage()));
            }
        }, "svg-export-thread").start();
    }

    /** Affiche ou masque le panneau des statistiques de performance. */
    public void setStatsVisible(boolean show) {
        if (graphStats == null)
            return;
        graphStats.setVisible(show);
        graphStats.setManaged(show);
    }

    /** Détecte le type de fichier source à partir de son extension. */
    private GraphProject.SourceType detectType(File file) {
        String name = file.getName().toLowerCase();
        if (name.endsWith(".csv"))
            return GraphProject.SourceType.CSV;
        if (name.endsWith(".dot"))
            return GraphProject.SourceType.DOT;
        throw new IllegalArgumentException("Type de fichier inconnu : " + name);
    }

    /**
     * Configure les listeners pour redimensionner le canvas OpenGL quand la fenêtre
     * change de taille.
     */
    private void setupGraphSurfaceResize() {
        graphHostPane.widthProperty().addListener((obs, oldV, newV) -> resizeGraphSurface());
        graphHostPane.heightProperty().addListener((obs, oldV, newV) -> resizeGraphSurface());
    }

    /** Transmet les nouvelles dimensions au panel et à la caméra du moteur. */
    private void resizeGraphSurface() {
        int width = Math.max(1, (int) graphHostPane.getWidth());
        int height = Math.max(1, (int) graphHostPane.getHeight());
        panel.resize(width, height);
        bus.dispatch(engine -> engine.camera().resize(width, height));
    }

    /** Lie le groupe de boutons d'outils au service d'interaction. */
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

    /**
     * Arrête proprement tous les sous-systèmes liés à l'espace de travail courant.
     *
     * <p>
     * Cette méthode peut être appelée plusieurs fois sans effet secondaire.
     * Elle évite de laisser en mémoire un ancien moteur, un ancien canvas OpenGLFX
     * ou un ancien thread d'exécution lors d'un retour à l'accueil ou d'un
     * changement de scène.
     * </p>
     */
    public void shutdown() {
        if (!Platform.isFxApplicationThread()) {
            Platform.runLater(this::shutdown);
            return;
        }

        if (shuttingDown)
            return;
        shuttingDown = true;

        try {
            if (bus != null) {
                try {
                    bus.dispatchSyncVoid(GraphEngine::stopSimulation);
                } catch (Exception ignored) {
                }
            } else if (engine != null) {
                engine.stopSimulation();
            }

            if (engine != null) {
                engine.removeListener(this);
            }

            if (graphStatsController != null) {
                graphStatsController.dispose();
            }

            if (panel != null) {
                panel.detachContent();
            }
            if (previewGraphPanel != null) {
                previewGraphPanel.detachContent();
            }

            if (previewController != null) {
                previewController.clearGraphPanel();
            }
            if (graphHostPane != null) {
                graphHostPane.getChildren().clear();
            }

            if (panel != null) {
                panel.dispose();
                panel = null;
            }
            if (previewGraphPanel != null) {
                previewGraphPanel.dispose();
                previewGraphPanel = null;
            }

            if (engine != null) {
                engine.dispose();
                engine = null;
            }
            if (engineExecutor != null) {
                engineExecutor.shutdown();
                engineExecutor = null;
            }

        } finally {
            uiState.setRunning(false);
            uiState.setStatus("Fermeture");
        }
    }

    /**
     * Libère les ressources OpenGLFX/LWJGL et natives lors de la fermeture de l'application.
     */
    private void setupCloseWindowListener(GraphNativeEngine nat) {
        rootTabPane.sceneProperty().addListener((obs, oldScene, scene) -> {
            if (scene != null) {
                scene.windowProperty().addListener((obsW, oldWindow, window) -> {
                    if (window != null) {
                        Stage stage = (Stage) window;
                        stage.setOnCloseRequest(e -> {
                            if (closeSequenceArmed) {
                                return;
                            }
                            closeSequenceArmed = true;
                            e.consume();
                            stage.hide();
                            Platform.runLater(() -> {
                                shutdown();
                                Platform.runLater(stage::close);
                            });
                        });
                    }
                });
            }
        });
    }

    /** @return La fenêtre principale du contrôleur. */
    public Stage getStage() {
        return (Stage) rootTabPane.getScene().getWindow();
    }

    /** @return Le fichier source du projet actuel. */
    public File getFile() {
        return (project == null) ? null : project.sourceFile();
    }

    /** Définit le projet en cours. */
    public void setProject(GraphProject project) {
        this.project = project;
    }

    /**
     * Parcourt les exceptions parentes pour trouver le message d'erreur d'origine.
     */
    private String rootCauseMessage(Throwable throwable) {
        Throwable current = throwable;
        while (current.getCause() != null && current.getCause() != current)
            current = current.getCause();
        String message = current.getMessage();
        return message == null || message.isBlank() ? current.toString() : message;
    }

    /** Affiche une boîte de dialogue standard. */
    private void alert(Alert.AlertType type, String title, String content) {
        Alert alert = new Alert(type);
        alert.setTitle(title);
        alert.setHeaderText(null);
        alert.setContentText(content);
        alert.showAndWait();
    }
}
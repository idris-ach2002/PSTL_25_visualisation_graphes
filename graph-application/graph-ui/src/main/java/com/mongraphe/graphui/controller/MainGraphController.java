package com.mongraphe.graphui.controller;

import com.mongraphe.graphui.app.CommandBus;
import com.mongraphe.graphui.app.UiState;
import com.mongraphe.graphui.interaction.InteractionService;
import com.mongraphe.graphui.model.GraphData;
import com.mongraphe.graphui.model.GraphProject;
import com.mongraphe.graphui.rendering.Camera2D;
import com.mongraphe.graphui.rendering.EngineExecutor;
import com.mongraphe.graphui.rendering.GraphEngine;
import com.mongraphe.graphui.rendering.GraphNativeEngine;
import com.mongraphe.graphui.rendering.GraphRenderer;
import com.mongraphe.graphui.view.GraphPanel;

import javafx.application.Platform;
import javafx.fxml.FXML;
import javafx.scene.control.ToggleGroup;
import javafx.scene.layout.BorderPane;
import javafx.scene.layout.Pane;
import javafx.scene.layout.StackPane;
import javafx.stage.Stage;

public final class MainGraphController {

    private GraphProject project;
    private GraphPanel panel;
    private CommandBus<GraphEngine> bus;

    @FXML
    private MainMenuController menuViewController;
    @FXML
    private GraphWorkspaceController workspaceViewController;
    @FXML
    private EngineOptionsController engineOptionsViewController;
    @FXML
    private DataController dataViewController;

    @FXML
    private ToggleGroup viewToggleGroup;
    @FXML
    private StackPane rootStack;
    @FXML
    private BorderPane overview;
    @FXML
    private StackPane graphHostPane;
    @FXML
    private BorderPane dataView;
    @FXML
    private Pane preview;

    @FXML
    private void initialize() throws Exception {
        GraphNativeEngine nativeEngine = new GraphNativeEngine();
        GraphEngine engine = new GraphEngine(nativeEngine);
        Camera2D camera = new Camera2D();
        GraphRenderer renderer = new GraphRenderer(engine, camera);
        bus = new CommandBus<>(engine, new EngineExecutor());
        panel = new GraphPanel(renderer, new InteractionService(bus, new UiState()));

        graphHostPane.getChildren().add(panel.canvas());
        panel.start();

        menuViewController.setBus(bus);
        workspaceViewController.setMainController(this);
        engineOptionsViewController.setBus(bus);
        dataViewController.setBus(bus);

        setupCloseWindowListener(nativeEngine);
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

        String view = String.valueOf(
                viewToggleGroup.getSelectedToggle().getUserData());

        overview.setVisible("overview".equals(view));
        dataView.setVisible("data".equals(view));
        preview.setVisible("preview".equals(view));

        if ("data".equals(view)) {
            Platform.runLater(() -> dataViewController.refresh());
        }
    }

    public void setProject(GraphProject project) {
        this.project = project;
    }

    public void startGraph(
            GraphData.SimilitudeMode similitude,
            GraphData.NodeCommunity community) {

        if (project == null)
            return;

        bus.dispatchSyncVoid(e -> {
            e.load(project.sourceFile().getPath(), project.sourceType(), similitude, community);
        });

        bus.dispatch(e -> {
            e.startSimulation();
        });
    }
}
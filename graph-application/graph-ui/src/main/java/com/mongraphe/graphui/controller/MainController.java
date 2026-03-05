package com.mongraphe.graphui.controller;

import java.io.File;
import com.mongraphe.graphui.GraphData;
import com.mongraphe.graphui.app.CommandBus;
import com.mongraphe.graphui.app.GLExecutor;
import com.mongraphe.graphui.app.GraphProject;
import com.mongraphe.graphui.app.InteractionService;
import com.mongraphe.graphui.app.UiState;
import com.mongraphe.graphui.rendering.Camera2D;
import com.mongraphe.graphui.rendering.GraphEngine;
import com.mongraphe.graphui.rendering.GraphEngine.GraphEngineListener;
import com.mongraphe.graphui.rendering.GraphNativeEngine;
import com.mongraphe.graphui.rendering.GraphRenderer;
import com.mongraphe.graphui.view.GraphPanel;

import javafx.application.Platform;
import javafx.fxml.FXML;
import javafx.scene.control.ToggleGroup;
import javafx.scene.layout.BorderPane;
import javafx.scene.layout.Pane;
import javafx.scene.layout.StackPane;

public final class MainController {

    private File pendingFile;
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
        bus = new CommandBus<>(engine, new GLExecutor());
        panel = new GraphPanel(renderer, new InteractionService(bus, new UiState()));

        graphHostPane.getChildren().add(panel.canvas());
        panel.start();

        menuViewController.setBus(bus);
        workspaceViewController.setMainController(this);
        engineOptionsViewController.setBus(bus);
        dataViewController.setBus(bus);
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

    public void setFile(File file) {
        this.pendingFile = file;
    }

    public void startGraph(
            GraphData.SimilitudeMode similitude,
            GraphData.NodeCommunity community,
            GraphProject.SourceType type) {

        if (pendingFile == null)
            return;

        System.out.println("Dispatch !");

        bus.dispatchSyncVoid(e -> {
            e.load(pendingFile.getPath(), type, similitude, community);
        });

        bus.dispatch(e -> {
            e.startSimulation();
        });
    }
}
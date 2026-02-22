package com.mongraphe.graphui.controller;

import java.io.File;

import com.mongraphe.graphui.GraphData;
import com.mongraphe.graphui.app.CommandBus;
import com.mongraphe.graphui.app.GLExecutor;
import com.mongraphe.graphui.app.GraphProject;
import com.mongraphe.graphui.rendering.Camera2D;
import com.mongraphe.graphui.rendering.GraphEngine;
import com.mongraphe.graphui.rendering.GraphNativeEngine;
import com.mongraphe.graphui.rendering.GraphRenderer;
import com.mongraphe.graphui.view.GraphPanel;

import javafx.fxml.FXML;
import javafx.fxml.FXMLLoader;
import javafx.scene.layout.BorderPane;
import javafx.scene.layout.StackPane;
import javafx.scene.layout.VBox;

public final class MainController {

    private File pendingFile;
    private CommandBus<GraphEngine> bus;

    @FXML
    private BorderPane root;

    @FXML
    private StackPane graphHostPane;

    private MainMenuController menuController;
    private GraphWorkspaceController workspaceController;
    private EngineOptionsController engineOptionsController;
    private DataController dataController;
    private ViewSwitcherController viewSwitcherController;

    @FXML
    private void initialize() throws Exception {

        GraphNativeEngine nativeEngine = new GraphNativeEngine();
        GraphEngine engine = new GraphEngine(nativeEngine);
        Camera2D camera = new Camera2D();
        GraphRenderer renderer = new GraphRenderer(engine, camera);
        GraphPanel panel = new GraphPanel(renderer);
        bus = new CommandBus<>(engine, new GLExecutor());

        FXMLLoader centerLoader = new FXMLLoader(getClass().getResource("/fxml/CenterView.fxml"));
        centerLoader.load();
        viewSwitcherController = centerLoader.getController();
        viewSwitcherController.setBus(bus);

        viewSwitcherController.setGraphPanel(panel);

        panel.start();

        FXMLLoader menuLoader = new FXMLLoader(getClass().getResource("/fxml/MainMenuView.fxml"));
        menuLoader.load();
        menuController = menuLoader.getController();
        menuController.setBus(bus);

        FXMLLoader workspaceLoader = new FXMLLoader(getClass().getResource("/fxml/GraphWorkspaceView.fxml"));
        workspaceLoader.load();
        workspaceController = workspaceLoader.getController();

        FXMLLoader engineOptionsLoader = new FXMLLoader(getClass().getResource("/fxml/EngineOptionsView.fxml"));
        engineOptionsLoader.load();
        engineOptionsController = engineOptionsLoader.getController();
        engineOptionsController.setBus(bus);

        FXMLLoader statsLoader = new FXMLLoader(getClass().getResource("/fxml/DataView.fxml"));
        statsLoader.load();
        dataController = statsLoader.getController();
        dataController.setBus(bus);

        workspaceController.setMainController(this);
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

        bus.dispatchSync(e -> e.load(
                pendingFile.getPath(),
                type,
                similitude,
                community));
        dataController.refresh();
    }
}
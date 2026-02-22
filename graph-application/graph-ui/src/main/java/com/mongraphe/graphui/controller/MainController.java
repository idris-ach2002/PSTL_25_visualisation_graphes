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

public final class MainController {

    private File pendingFile;
    private GraphPanel panel;
    private CommandBus<GraphEngine> bus;

    @FXML
    private StackPane graphHostPane;

    @FXML
    private MainMenuController menuViewController;
    @FXML
    private GraphWorkspaceController workspaceViewController;
    @FXML
    private EngineOptionsController engineOptionsViewController;
    @FXML
    private DataController dataViewController;

    private ViewSwitcherController centerViewController;

    @FXML
    private void initialize() throws Exception {
        GraphNativeEngine nativeEngine = new GraphNativeEngine();
        GraphEngine engine = new GraphEngine(nativeEngine);
        Camera2D camera = new Camera2D();
        GraphRenderer renderer = new GraphRenderer(engine, camera);
        panel = new GraphPanel(renderer);
        CommandBus<GraphEngine> bus = new CommandBus<>(engine, new GLExecutor());

        graphHostPane.getChildren().add(panel.canvas());

        // Charger le FXML de la centerView
        FXMLLoader centerLoader = new FXMLLoader(getClass().getResource("/fxml/CenterView.fxml"));
        BorderPane centerView = centerLoader.load();
        centerViewController = centerLoader.getController();
        centerViewController.setBus(bus);

        menuViewController.setBus(bus);
        workspaceViewController.setMainController(this);
        engineOptionsViewController.setBus(bus);
        dataViewController.setBus(bus);
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
        dataViewController.refresh();
    }
}
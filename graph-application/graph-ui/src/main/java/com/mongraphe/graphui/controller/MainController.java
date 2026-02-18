package com.mongraphe.graphui.controller;

import com.mongraphe.graphui.app.CommandBus;
import com.mongraphe.graphui.app.GLExecutor;
import com.mongraphe.graphui.interfaces.CommandBusLinkedI;
import com.mongraphe.graphui.rendering.Camera2D;
import com.mongraphe.graphui.rendering.GraphEngine;
import com.mongraphe.graphui.rendering.GraphNativeEngine;
import com.mongraphe.graphui.rendering.GraphRenderer;
import com.mongraphe.graphui.view.GraphPanel;

import javafx.fxml.FXML;
import javafx.scene.layout.BorderPane;

public final class MainController {

    @FXML
    private BorderPane root;

    @FXML
    private GraphWorkspaceController workspaceController;
    @FXML
    private StatsController statsController;
    @FXML
    private EngineOptionsController optionsController;
    @FXML
    private ProjectController projectController;
    @FXML
    private ViewSwitcherController viewSwitcherController;
    @FXML
    private MainMenuController menuController;

    @FXML
    private void initialize() {

        GraphNativeEngine nativeEngine = new GraphNativeEngine();
        GraphEngine engine = new GraphEngine(nativeEngine);
        Camera2D camera = new Camera2D();

        GraphRenderer renderer = new GraphRenderer(engine, camera);
        GraphPanel panel = new GraphPanel(renderer);

        CommandBus<GraphEngine> bus = new CommandBus<GraphEngine>(engine, new GLExecutor(panel.window()));

        inject(workspaceController, bus);
        inject(statsController, bus);
        inject(optionsController, bus);
        inject(projectController, bus);
        inject(viewSwitcherController, bus);
        inject(menuController, bus);

        projectController.setStats(statsController);
    }

    private void inject(CommandBusLinkedI<GraphEngine> cbl, CommandBus<GraphEngine> bus) {
        cbl.setBus(bus);
    }
}
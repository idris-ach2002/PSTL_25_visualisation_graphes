package com.mongraphe.graphui.controller;

import com.mongraphe.graphui.app.ApplicationContext;
import com.mongraphe.graphui.app.GraphEngineAdapter;
import com.mongraphe.graphui.app.UiState;
import com.mongraphe.graphui.interfaces.CommandBusLinked;
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

        UiState uiState = new UiState();

        GraphNativeEngine nativeEngine = new GraphNativeEngine();
        GraphEngine engine = new GraphEngine(nativeEngine);
        Camera2D camera = new Camera2D();

        GraphRenderer renderer = new GraphRenderer(engine, camera);
        GraphPanel panel = new GraphPanel(renderer);

        GraphEngineAdapter adapter = new GraphEngineAdapter(engine, camera, panel.createExporter(renderer), panel.window());

        ApplicationContext context = new ApplicationContext(uiState, adapter);

        inject(workspaceController, context);
        inject(statsController, context);
        inject(optionsController, context);
        inject(projectController, context);
        inject(viewSwitcherController, context);
        inject(menuController, context);
    }

    private void inject(CommandBusLinked controller, ApplicationContext context) {
        controller.setContext(context);
    }
}
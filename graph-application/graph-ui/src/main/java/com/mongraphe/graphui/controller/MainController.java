package com.mongraphe.graphui.controller;

import javafx.fxml.FXML;
import javafx.scene.layout.BorderPane;


public final class MainController {

    @FXML private BorderPane root;

    @FXML private GraphWorkspaceController workspaceController;
    @FXML private StatsController statsController;
    @FXML private EngineOptionsController optionsController;
    @FXML private ProjectController projectController;
    @FXML private ViewSwitcherController viewSwitcherController;
    @FXML private MainMenuController menuController;

    @FXML
    private void initialize() {

        projectController.setWorkspace(workspaceController);
        projectController.setOptions(optionsController);
        projectController.setStats(statsController);

        menuController.setWorkspace(workspaceController);
        menuController.setProject(projectController);
    }
}
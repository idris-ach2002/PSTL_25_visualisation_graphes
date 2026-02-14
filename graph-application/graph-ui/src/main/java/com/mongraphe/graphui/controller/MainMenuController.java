package com.mongraphe.graphui.controller;

import javafx.fxml.FXML;
import javafx.application.Platform;
import javafx.scene.control.Alert;

public final class MainMenuController {

    private GraphWorkspaceController workspace;
    private ProjectController project;

    @FXML
    private void handleQuit() {
        Platform.exit();
    }

    @FXML
    private void handleNewProject() {
        // TODO : gérer la création de projet + sauvegarde
        return;
    }

    @FXML
    private void handleCloseWorkspace() {
        // TODO : gérer la fermeture de projet + sauvegarde
        return;
    }

    @FXML
    private void handleAbout() {
        Alert a = new Alert(Alert.AlertType.INFORMATION);
        a.setHeaderText("MonGraphe");
        a.setContentText("Visualisation de graphes - 0.0.1");
        a.showAndWait();
    }

    public void setWorkspace(GraphWorkspaceController workspace) {
        this.workspace = workspace;
    }

    public void setProject(ProjectController project) {
        this.project = project;
    }
}

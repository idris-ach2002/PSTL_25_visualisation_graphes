package com.mongraphe.graphui.controller;

import javafx.fxml.FXML;

import com.mongraphe.graphui.app.ApplicationContext;
import com.mongraphe.graphui.interfaces.ContextAware;

import javafx.application.Platform;
import javafx.scene.control.Alert;

public final class MainMenuController implements ContextAware {

    private ApplicationContext context;

    @Override
    public void setContext(ApplicationContext context) {
        this.context = context;
    }

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
}
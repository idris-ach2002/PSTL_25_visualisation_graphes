package com.mongraphe.graphui.controller;

import javafx.fxml.FXML;

import com.mongraphe.graphui.app.CommandBus;
import com.mongraphe.graphui.interfaces.CommandBusLinked;
import com.mongraphe.graphui.rendering.GraphEngine;

import javafx.application.Platform;
import javafx.scene.control.Alert;

public final class MainMenuController implements CommandBusLinked<GraphEngine> {

    private CommandBus<GraphEngine> bus;
    
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

    @Override
    public void setBus(CommandBus<GraphEngine> bus) {
        this.bus = bus;
    }
}
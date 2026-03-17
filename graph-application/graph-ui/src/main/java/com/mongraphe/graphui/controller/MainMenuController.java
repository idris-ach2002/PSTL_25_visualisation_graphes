package com.mongraphe.graphui.controller;

import com.mongraphe.graphui.app.CommandBus;
import com.mongraphe.graphui.interfaces.CommandBusLinkedI;
import com.mongraphe.graphui.rendering.GraphEngine;

import javafx.fxml.FXML;
import javafx.scene.control.CheckMenuItem;
import javafx.scene.control.MenuBar;

public final class MainMenuController implements CommandBusLinkedI<GraphEngine> {

    private CommandBus<GraphEngine> bus;
    private MainGraphController mainController;

    @FXML private MenuBar menuController;
    @FXML private CheckMenuItem showStatsMenuItem;

    public void setMainController(MainGraphController mainController) {
        this.mainController = mainController;
    }

    @FXML private void handleQuit() { if (mainController != null) mainController.handleQuit(); }
    @FXML private void handleNewProject() { if (mainController != null) mainController.returnToHome(); }
    @FXML private void handleCloseWorkspace() { if (mainController != null) mainController.closeWorkspace(); }
    @FXML private void handleAbout() { if (mainController != null) mainController.showAbout(); }
    @FXML private void handleOpen() { if (mainController != null) mainController.openGraphFile(); }
    @FXML private void handleSaveProject() { if (mainController != null) mainController.saveProject(); }
    @FXML private void handleUndo() { if (mainController != null) mainController.undo(); }
    @FXML private void handleRedo() { if (mainController != null) mainController.redo(); }
    @FXML private void handleFullScreen() { if (mainController != null) mainController.toggleFullScreen(); }
    @FXML private void handleToggleStats() { if (mainController != null) mainController.setStatsVisible(showStatsMenuItem == null || showStatsMenuItem.isSelected()); }
    @FXML private void handleLayoutReset() { if (mainController != null) mainController.resetLayout(); }
    @FXML private void handleOptions() { if (mainController != null) mainController.showOptionsInfo(); }
    @FXML private void handleDocumentation() { if (mainController != null) mainController.openDocumentation(); }
    @FXML private void handleExportPng() { if (mainController != null) mainController.exportPng(); }

    @Override
    public void setBus(CommandBus<GraphEngine> bus) {
        this.bus = bus;
    }
}

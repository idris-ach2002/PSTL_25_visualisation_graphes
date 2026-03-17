package com.mongraphe.graphui.controller;

import java.io.File;
import java.io.FileNotFoundException;
import java.io.IOException;
import java.net.URI;
import java.net.URL;

import com.mongraphe.graphui.app.CommandBus;
import com.mongraphe.graphui.interfaces.CommandBusLinkedI;
import com.mongraphe.graphui.rendering.GraphEngine;

import javafx.application.Platform;
import javafx.fxml.FXML;
import javafx.fxml.FXMLLoader;
import javafx.geometry.Rectangle2D;
import javafx.scene.Parent;
import javafx.scene.Scene;
import javafx.scene.control.Alert;
import javafx.scene.control.CheckMenuItem;
import javafx.scene.control.MenuBar;
import javafx.stage.FileChooser;
import javafx.stage.Screen;
import javafx.stage.Stage;

public final class MainMenuController implements CommandBusLinkedI<GraphEngine> {

    private CommandBus<GraphEngine> bus;
    private MainGraphController mainController;

    @FXML
    private MenuBar menuController;
    @FXML
    private CheckMenuItem showStatsMenuItem;

    private void returnToHome() {
        try {
            FXMLLoader loader = new FXMLLoader(getClass().getResource("/fxml/HomeScreen.fxml"));
            Parent root = loader.load();
            Scene scene = new Scene(root);
            Stage stage = mainController.getStage();
            Rectangle2D sb = Screen.getPrimary().getVisualBounds();
            stage.setTitle("MonGraphe");
            stage.setScene(scene);
            stage.setX(sb.getMinX());
            stage.setY(sb.getMinY());
            stage.setWidth(sb.getWidth());
            stage.setHeight(sb.getHeight());
            stage.show();
        } catch (IOException e) {
            e.printStackTrace();
        }
    }

    private void alert(Alert.AlertType type, String title, String content) {
        Alert alert = new Alert(type);
        alert.setTitle(title);
        alert.setHeaderText(null);
        alert.setContentText(content);
        alert.showAndWait();
    }

    public void setMainController(MainGraphController mainController) {
        this.mainController = mainController;
    }

    @FXML
    private void handleQuit() {
        System.exit(0);
    }

    @FXML
    private void handleNewProject() {
        if (mainController != null)
            returnToHome(); // TODO C'est pas bien fait normalement faut creer un env vide
    }

    @FXML
    private void handleCloseWorkspace() {
        if (mainController != null)
            returnToHome();
    }

    @FXML
    private void handleAbout() {
        alert(Alert.AlertType.INFORMATION,
                "À propos",
                "MonGraphe\nVisualisation de graphes (JavaFX + JOGL + JNI)\nBuild refactor + fonctionnalités restaurées.");
    }

    @FXML
    private void handleOpen() {
        if (mainController != null) {
            FileChooser chooser = new FileChooser();
            chooser.setTitle("Ouvrir un fichier de graphe");
            chooser.getExtensionFilters().addAll(
                    new FileChooser.ExtensionFilter("Projets MonGraphe", "*.mongraphe"),
                    new FileChooser.ExtensionFilter("Graph CSV / DOT", "*.csv", "*.dot"),
                    new FileChooser.ExtensionFilter("Tous les fichiers", "*.*"));
            File file = chooser.showOpenDialog(mainController.getStage());
            if (file != null) {
                mainController.openFile(file);
            }
        }
    }

    @FXML
    private void handleSaveProject() {
        if (mainController != null){

        }
            //mainController.saveProject();
    }

    @FXML
    private void handleUndo() {
        bus.undo();
    }

    @FXML
    private void handleRedo() {
        bus.redo();
    }

    @FXML
    private void handleFullScreen() {
        if (mainController != null)
            mainController.getStage().setFullScreen(true);
    }

    @FXML
    private void handleToggleStats() {
        if (mainController != null)
            mainController.setStatsVisible(showStatsMenuItem == null || showStatsMenuItem.isSelected());
    }

    @FXML
    private void handleLayoutReset() {
        if (mainController != null)
            bus.dispatchSyncVoid(engine -> engine.camera().reset());
    }

    @FXML
    private void handleOptions() {
        alert(Alert.AlertType.INFORMATION,
                "Aide rapide",
                "Outils disponibles : Run / Select / Move / Delete\n"
                        + "SPACE : pause / reprise\n"
                        + "SUPPR : supprime le sommet sélectionné\n"
                        + "Molette : zoom\n"
                        + "Clic droit + drag : déplacement de la caméra\n"
                        + "Undo / Redo : via le menu Édition");
    }

    private void openInBrowser(URI uri) throws IOException {
        String url = uri.toString();

        // Essayer Firefox
        try {
            new ProcessBuilder("firefox", url).start();
            return;
        } catch (IOException ignored) {
        }

        // Essayer Chrome
        try {
            new ProcessBuilder("google-chrome", url).start();
            return;
        } catch (IOException ignored) {
        }

        // Dernier recours : xdg-open (mais peut rouvrir la messagerie)
        new ProcessBuilder("xdg-open", url).start();
    }

    @FXML
    private void handleDocumentation() {
        new Thread(() -> {
            try {
                URL mdUrl = getClass().getResource("/markdown/README.md");
                File mdFile = new File(mdUrl.toURI());

                if (!mdFile.exists()) {
                    throw new FileNotFoundException("README.md introuvable");
                }

                File htmlFile = new File(getClass().getResource("/html").getFile(), "README.html");

                // Conversion Markdown → HTML
                ProcessBuilder pb = new ProcessBuilder(
                        "pandoc",
                        mdFile.getAbsolutePath(),
                        "-o",
                        htmlFile.getAbsolutePath(),
                        "--standalone",
                        "--css=github-markdown.css",
                        "--metadata=pagetitle=Documentation");

                pb.redirectErrorStream(true);
                Process process = pb.start();
                process.waitFor();

                // Ouvrir dans un vrai navigateur
                openInBrowser(htmlFile.toURI());
                return;

            } catch (Exception e) {
                e.printStackTrace();
            }

            Platform.runLater(() -> alert(
                    Alert.AlertType.ERROR,
                    "Documentation",
                    "Impossible de générer ou d'ouvrir la documentation."));
        }).start();
    }

    @FXML
    private void handleExportPng() {
        if (mainController != null)
            mainController.exportPng();
    }

    @Override
    public void setBus(CommandBus<GraphEngine> bus) {
        this.bus = bus;
    }
}

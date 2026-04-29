package com.mongraphe.graphui.controller;

import java.io.File;
import java.io.IOException;

import com.mongraphe.graphui.app.CommandBus;
import com.mongraphe.graphui.interfaces.CommandBusLinkedI;
import com.mongraphe.graphui.rendering.GraphEngine;
import com.mongraphe.graphui.view.UserDocumentationService;

import javafx.fxml.FXML;
import javafx.fxml.FXMLLoader;
import javafx.geometry.Rectangle2D;
import javafx.scene.Parent;
import javafx.scene.Scene;
import javafx.scene.control.Alert;
import javafx.scene.control.CheckMenuItem;
import javafx.scene.control.MenuBar;
import javafx.scene.control.MenuItem;
import javafx.scene.input.KeyCode;
import javafx.scene.input.KeyCodeCombination;
import javafx.scene.input.KeyCombination;
import javafx.stage.FileChooser;
import javafx.stage.Screen;
import javafx.stage.Stage;

/**
 * Contrôleur JavaFX gérant la barre de menus principale de l'application.
 *
 * <p>
 * Cette classe fait le lien entre l'interface utilisateur (la barre de menus)
 * et
 * les fonctionnalités logiques de l'application. Elle communique avec le
 * {@code MainGraphController} pour les actions sur la vue et utilise un
 * {@code CommandBus} pour les interactions avec le moteur de rendu.
 * </p>
 *
 * <h2>Responsabilités</h2>
 * <ul>
 * <li>Gérer les actions de gestion de fichiers (Nouveau, Ouvrir, Fermer).</li>
 * <li>Piloter les fonctions d'annulation/rétablissement (Undo/Redo) via le bus
 * de commandes.</li>
 * <li>Contrôler l'affichage et le rendu (Plein écran, Reset caméra).</li>
 * <li>Exporter le graphe sous différents formats (PNG, SVG).</li>
 * <li>Ouvrir la documentation utilisateur embarquée.</li>
 * </ul>
 */
public final class MainMenuController implements CommandBusLinkedI<GraphEngine> {

    /** Bus de communication pour envoyer des commandes au moteur de graphe. */
    private CommandBus<GraphEngine> bus;

    /** Référence vers le contrôleur principal. */
    private MainGraphController mainController;

    /** Conteneur principal de la barre de menus. */
    @FXML
    private MenuBar menuController;

    @FXML
    private MenuItem newProjectMenuItem;
    @FXML
    private MenuItem openMenuItem;
    @FXML
    private MenuItem exportPngMenuItem;
    @FXML
    private MenuItem exportSvgMenuItem;
    @FXML
    private MenuItem closeWorkspaceMenuItem;
    @FXML
    private MenuItem quitMenuItem;
    @FXML
    private MenuItem undoMenuItem;
    @FXML
    private MenuItem redoMenuItem;
    @FXML
    private MenuItem fullScreenMenuItem;
    @FXML
    private MenuItem resetViewMenuItem;
    @FXML
    private MenuItem quickHelpMenuItem;
    @FXML
    private MenuItem documentationMenuItem;

    /**
     * Option de menu permettant d'activer ou désactiver l'affichage des
     * statistiques.
     */
    @FXML
    private CheckMenuItem showStatsMenuItem;

    @FXML
    private void initialize() {
        installAccelerators();
    }

    private void installAccelerators() {
        if (openMenuItem != null) {
            openMenuItem.setAccelerator(new KeyCodeCombination(KeyCode.O, KeyCombination.SHORTCUT_DOWN));
        }
        if (closeWorkspaceMenuItem != null) {
            closeWorkspaceMenuItem.setAccelerator(new KeyCodeCombination(KeyCode.W, KeyCombination.SHORTCUT_DOWN));
        }
        if (quitMenuItem != null) {
            quitMenuItem.setAccelerator(new KeyCodeCombination(KeyCode.Q, KeyCombination.SHORTCUT_DOWN));
        }
        if (undoMenuItem != null) {
            undoMenuItem.setAccelerator(new KeyCodeCombination(KeyCode.Z, KeyCombination.SHORTCUT_DOWN));
        }
        if (redoMenuItem != null) {
            redoMenuItem.setAccelerator(new KeyCodeCombination(KeyCode.Y, KeyCombination.SHORTCUT_DOWN));
        }
        if (fullScreenMenuItem != null) {
            fullScreenMenuItem.setAccelerator(new KeyCodeCombination(KeyCode.F11));
        }
        if (resetViewMenuItem != null) {
            resetViewMenuItem.setAccelerator(new KeyCodeCombination(KeyCode.DIGIT0, KeyCombination.SHORTCUT_DOWN));
        }
        if (documentationMenuItem != null) {
            documentationMenuItem.setAccelerator(new KeyCodeCombination(KeyCode.F1));
        }
    }

    /**
     * Redirige l'utilisateur vers l'écran d'accueil de l'application.
     */
    private void returnToHome() {
        try {
            if (mainController != null) {
                mainController.shutdown();
            }
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
            returnToHome();
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
        if (mainController != null) {
            // TODO: Implémenter la sauvegarde
        }
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
                        + "SUPPR / RETOUR ARRIÈRE : supprime le sommet sélectionné\n"
                        + "Molette : zoom\n"
                        + "Clic droit + drag : déplacement de la caméra\n"
                        + "Ctrl+Z : annuler\n"
                        + "Ctrl+Y ou Ctrl+Shift+Z : rétablir\n"
                        + "Ctrl++ : zoom avant\n"
                        + "Ctrl+- : zoom arrière\n"
                        + "Ctrl+0 : réinitialiser la vue\n"
                        + "Ctrl+O : ouvrir un fichier\n"
                        + "Ctrl+W : fermer l'espace de travail\n"
                        + "F1 : documentation\n"
                        + "F11 : plein écran");
    }

    @FXML
    private void handleDocumentation() {
        try {
            UserDocumentationService.openDocumentationWindow();
        } catch (Exception e) {
            e.printStackTrace();
            alert(
                    Alert.AlertType.ERROR,
                    "Documentation",
                    "Impossible d'ouvrir la documentation utilisateur.");
        }
    }

    @FXML
    private void handleExportPng() {
        if (mainController != null)
            mainController.exportPng();
    }

    @FXML
    private void handleExportSvg() {
        if (mainController != null)
            mainController.exportSvg();
    }

    @Override
    public void setBus(CommandBus<GraphEngine> bus) {
        this.bus = bus;
    }
}
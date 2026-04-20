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
 * <li>Contrôler l'affichage et le rendu (Plein écran, Statistiques, Reset
 * caméra).</li>
 * <li>Exporter le graphe sous différents formats (PNG, SVG).</li>
 * <li>Générer et afficher dynamiquement la documentation à partir de fichiers
 * Markdown.</li>
 * </ul>
 *
 * <h2>Interaction avec le CommandBus</h2>
 * <p>
 * En implémentant {@code CommandBusLinkedI<GraphEngine>}, ce contrôleur peut
 * envoyer
 * des commandes synchrones ou asynchrones au moteur de rendu JOGL, comme le
 * repositionnement de la caméra.
 * </p>
 */
public final class MainMenuController implements CommandBusLinkedI<GraphEngine> {

    /** Bus de communication pour envoyer des commandes au moteur de graphe. */
    private CommandBus<GraphEngine> bus;

    /**
     * Référence vers le contrôleur principal pour manipuler la scène et les
     * données.
     */
    private MainGraphController mainController;

    /** Conteneur principal de la barre de menus. */
    @FXML
    private MenuBar menuController;

    /**
     * Option de menu permettant d'activer ou désactiver l'affichage des
     * statistiques de rendu.
     */
    @FXML
    private CheckMenuItem showStatsMenuItem;

    /**
     * Redirige l'utilisateur vers l'écran d'accueil de l'application.
     * *
     * <p>
     * Cette méthode charge le fichier {@code HomeScreen.fxml} et réinitialise
     * la scène sur la fenêtre principale.
     * </p>
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

    /**
     * Affiche une boîte de dialogue d'alerte.
     *
     * @param type    Type d'alerte (Information, Erreur, etc.).
     * @param title   Titre de la fenêtre.
     * @param content Corps du message.
     */
    private void alert(Alert.AlertType type, String title, String content) {
        Alert alert = new Alert(type);
        alert.setTitle(title);
        alert.setHeaderText(null);
        alert.setContentText(content);
        alert.showAndWait();
    }

    /**
     * Définit le contrôleur principal associé à cette barre de menus.
     *
     * @param mainController Le contrôleur de la vue principale.
     */
    public void setMainController(MainGraphController mainController) {
        this.mainController = mainController;
    }

    /**
     * Quitte l'application proprement.
     */
    @FXML
    private void handleQuit() {
        System.exit(0);
    }

    /**
     * Action déclenchée pour créer un nouveau projet.
     * Actuellement, redirige vers l'écran d'accueil.
     */
    @FXML
    private void handleNewProject() {
        if (mainController != null)
            returnToHome();
    }

    /**
     * Ferme l'espace de travail actuel et revient à l'accueil.
     */
    @FXML
    private void handleCloseWorkspace() {
        if (mainController != null)
            returnToHome();
    }

    /**
     * Affiche les informations de version et les technologies utilisées.
     */
    @FXML
    private void handleAbout() {
        alert(Alert.AlertType.INFORMATION,
                "À propos",
                "MonGraphe\nVisualisation de graphes (JavaFX + JOGL + JNI)\nBuild refactor + fonctionnalités restaurées.");
    }

    /**
     * Ouvre un explorateur de fichiers pour charger un graphe.
     * Supporte les formats .mongraphe, .csv et .dot.
     */
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

    /**
     * Enregistre les modifications du projet actuel (non implémenté).
     */
    @FXML
    private void handleSaveProject() {
        if (mainController != null) {
            // TODO: Implémenter la sauvegarde
        }
    }

    /**
     * Annule la dernière action effectuée via le CommandBus.
     */
    @FXML
    private void handleUndo() {
        bus.undo();
    }

    /**
     * Rétablit la dernière action annulée via le CommandBus.
     */
    @FXML
    private void handleRedo() {
        bus.redo();
    }

    /**
     * Bascule l'application en mode plein écran.
     */
    @FXML
    private void handleFullScreen() {
        if (mainController != null)
            mainController.getStage().setFullScreen(true);
    }

    /**
     * Affiche ou masque les statistiques de performance (FPS, etc.) sur le canvas.
     */
    @FXML
    private void handleToggleStats() {
        if (mainController != null)
            mainController.setStatsVisible(showStatsMenuItem == null || showStatsMenuItem.isSelected());
    }

    /**
     * Réinitialise la position et le zoom de la caméra pour centrer le graphe.
     */
    @FXML
    private void handleLayoutReset() {
        if (mainController != null)
            bus.dispatchSyncVoid(engine -> engine.camera().reset());
    }

    /**
     * Affiche une boîte de dialogue résumant les raccourcis clavier et souris.
     */
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

    /**
     * Tente d'ouvrir une URI dans le navigateur par défaut du système (Linux).
     * * @param uri L'adresse à ouvrir.
     * 
     * @throws IOException Si aucun navigateur n'est trouvé.
     */
    private void openInBrowser(URI uri) throws IOException {
        String url = uri.toString();
        try {
            new ProcessBuilder("firefox", url).start();
            return;
        } catch (IOException ignored) {
        }

        try {
            new ProcessBuilder("google-chrome", url).start();
            return;
        } catch (IOException ignored) {
        }

        new ProcessBuilder("xdg-open", url).start();
    }

    /**
     * Génère la documentation HTML à partir du README.md via Pandoc et l'ouvre.
     * *
     * <p>
     * Cette opération est effectuée dans un thread séparé pour ne pas bloquer
     * l'interface utilisateur pendant la conversion.
     * </p>
     */
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

                // Conversion Markdown → HTML via Pandoc
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

    /**
     * Déclenche l'exportation du rendu actuel au format image PNG.
     */
    @FXML
    private void handleExportPng() {
        if (mainController != null)
            mainController.exportPng();
    }

    /**
     * Déclenche l'exportation du graphe au format vectoriel SVG.
     */
    @FXML
    private void handleExportSvg() {
        if (mainController != null)
            mainController.exportSvg();
    }

    /**
     * Injecte le bus de commandes pour interagir avec le moteur.
     * * @param bus Le CommandBus gérant le moteur de type GraphEngine.
     */
    @Override
    public void setBus(CommandBus<GraphEngine> bus) {
        this.bus = bus;
    }
}
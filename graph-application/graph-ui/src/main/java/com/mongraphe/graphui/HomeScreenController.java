package com.mongraphe.graphui;

import java.io.File;
import java.io.IOException;

import com.mongraphe.graphui.controller.MainController;

import javafx.fxml.FXML;
import javafx.fxml.FXMLLoader;
import javafx.geometry.Rectangle2D;
import javafx.scene.Parent;
import javafx.scene.Scene;
import javafx.scene.control.Hyperlink;
import javafx.stage.FileChooser;
import javafx.stage.Screen;
import javafx.stage.Stage;

public class HomeScreenController {

    // Liens de la page d'accueil (d'après HomeScreen.fxml)
    @FXML
    private Hyperlink newProjectLink;
    @FXML
    private Hyperlink openFileLink;

    @FXML
    private Hyperlink example1;
    @FXML
    private Hyperlink example2;
    @FXML
    private Hyperlink example3;
    @FXML
    private Hyperlink example4;

    @FXML
    public void initialize() {
        // Les onAction du menu principal sont déjà dans le FXML,
        // mais on sécurise + on branche les exemples.
        if (example1 != null)
            example1.setOnAction(e -> ouvrirFichierSample(example1.getText()));
        if (example2 != null)
            example2.setOnAction(e -> ouvrirFichierSample(example2.getText()));
        if (example3 != null)
            example3.setOnAction(e -> ouvrirFichierSample(example3.getText()));
        if (example4 != null)
            example4.setOnAction(e -> ouvrirFichierSample(example4.getText()));
    }

    @FXML
    void handleNewProject() {
        System.out.println("Nouveau projet");
        // TODO: logique nouveau projet
    }

    @FXML
    void handleOpenProject() {
        FileChooser fileChooser = new FileChooser();
        fileChooser.setTitle("Ouvrir un fichier de graphe");

        fileChooser.getExtensionFilters().addAll(
                new FileChooser.ExtensionFilter("DOT", "*.dot"),
                new FileChooser.ExtensionFilter("CSV", "*.csv"));

        Stage stage = getStage();
        File fichier = fileChooser.showOpenDialog(stage);
        if (fichier != null) {
            ouvrirFenetreGraphe(stage, fichier);
        }
    }

    private void ouvrirFichierSample(String nomFichier) {
        // Tes exemples affichent des noms de fichiers, donc on tente direct dans
        // samples/
        File fichier = new File("samples/" + nomFichier);
        if (fichier.exists()) {
            ouvrirFenetreGraphe(getStage(), fichier);
        } else {
            System.err.println("Fichier sample introuvable : " + fichier.getAbsolutePath());
        }
    }

    private Stage getStage() {
        // On récupère un node sûr (openFileLink ou un exemple) pour remonter au Stage.
        if (openFileLink != null && openFileLink.getScene() != null) {
            return (Stage) openFileLink.getScene().getWindow();
        }
        if (newProjectLink != null && newProjectLink.getScene() != null) {
            return (Stage) newProjectLink.getScene().getWindow();
        }
        if (example1 != null && example1.getScene() != null) {
            return (Stage) example1.getScene().getWindow();
        }
        // Dernier recours: ça ne devrait pas arriver si le FXML est chargé correctement
        throw new IllegalStateException("Impossible de récupérer le Stage (Scene non initialisée).");
    }

    void ouvrirFenetreGraphe(Stage stage, File fichier) {
        try {
            FXMLLoader loader = new FXMLLoader(getClass().getResource("/fxml/MainView.fxml"));

            Parent root = loader.load();
            MainController controller = loader.getController();
            controller.setFile(fichier);

            // Créer une scène avec la taille de l'écran
            Rectangle2D screenBounds = Screen.getPrimary().getVisualBounds();
            Scene scene = new Scene(root, screenBounds.getWidth(), screenBounds.getHeight());

            stage.setTitle("Graph");
            stage.setScene(scene);

            // Désactiver le redimensionnement et maximiser
            stage.setResizable(true);
            stage.setX(screenBounds.getMinX());
            stage.setY(screenBounds.getMinY());
            stage.setWidth(screenBounds.getWidth());
            stage.setHeight(screenBounds.getHeight());

            stage.show();
        } catch (IOException e) {
            e.printStackTrace();
        }
    }
}

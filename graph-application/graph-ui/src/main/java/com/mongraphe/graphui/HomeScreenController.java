package com.mongraphe.graphui;

import java.io.File;
import java.io.IOException;

import com.mongraphe.graphui.view.GraphView;

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

    @FXML
    private Hyperlink newProjectLink;
    @FXML
    private Hyperlink openFileLink;

    @FXML
    private void handleNewProject() {
        try {
            // Créer une instance de la classe Graph
            GraphView graph = new GraphView();

            // Définir le fichier à utiliser
            File file = new File(System.getProperty("user.dir") + "/samples/iris.csv");
            if (!file.exists()) {
                System.err.println("Le fichier spécifié n'existe pas : " + file.getAbsolutePath());
                return;
            }

        } catch (Exception e) {
            e.printStackTrace();
        }
    }

    @FXML
    private void handleOpenFile() {
        FileChooser fileChooser = new FileChooser();
        fileChooser.getExtensionFilters().add(
                new FileChooser.ExtensionFilter("Fichiers Graphiques", "*.csv", "*.dot"));
        Stage stage = (Stage) openFileLink.getScene().getWindow();
        File selectedFile = fileChooser.showOpenDialog(stage);

        if (selectedFile != null) {
            ouvrirFenetreGraphe(stage, selectedFile);
        }
    }

    private void ouvrirFenetreGraphe(Stage stage, File fichier) {
        try {
            FXMLLoader loader = new FXMLLoader(getClass().getResource("/fxml/Configurations.fxml"));

            // Créer une instance du contrôleur avec le fichier
            ConfigurationController configController = new ConfigurationController(fichier);
            loader.setController(configController);

            Parent root = loader.load();

            // Créer une scène avec la taille de l'écran
            Rectangle2D screenBounds = Screen.getPrimary().getVisualBounds();
            Scene scene = new Scene(root, screenBounds.getWidth(), screenBounds.getHeight());

            stage.setTitle("Graph");
            stage.setScene(scene);

            // Désactiver le redimensionnement et maximiser
            stage.setResizable(false);
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

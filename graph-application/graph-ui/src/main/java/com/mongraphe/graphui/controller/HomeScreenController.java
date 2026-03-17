package com.mongraphe.graphui.controller;

import java.io.File;

import javafx.fxml.FXML;
import javafx.fxml.FXMLLoader;
import javafx.geometry.Rectangle2D;
import javafx.scene.Parent;
import javafx.scene.Scene;
import javafx.scene.control.Alert;
import javafx.scene.control.Hyperlink;
import javafx.stage.FileChooser;
import javafx.stage.Screen;
import javafx.stage.Stage;

public final class HomeScreenController {

    @FXML private Hyperlink newProjectLink;
    @FXML private Hyperlink openFileLink;
    @FXML private Hyperlink example1;
    @FXML private Hyperlink example2;
    @FXML private Hyperlink example3;
    @FXML private Hyperlink example4;

    @FXML
    public void initialize() {
        if (example1 != null) example1.setOnAction(e -> openSample(example1.getText()));
        if (example2 != null) example2.setOnAction(e -> openSample(example2.getText()));
        if (example3 != null) example3.setOnAction(e -> openSample(example3.getText()));
        if (example4 != null) example4.setOnAction(e -> openSample(example4.getText()));
    }

    @FXML
    void handleNewProject() {
        handleOpenProject();
    }

    @FXML
    void handleOpenProject() {
        FileChooser chooser = new FileChooser();
        chooser.setTitle("Ouvrir un fichier de graphe");
        chooser.getExtensionFilters().addAll(
                new FileChooser.ExtensionFilter("Projets MonGraphe", "*.mongraphe"),
                new FileChooser.ExtensionFilter("Fichiers DOT / CSV", "*.dot", "*.csv"),
                new FileChooser.ExtensionFilter("Tous les fichiers", "*.*"));

        File file = chooser.showOpenDialog(getStage());
        if (file != null) {
            openGraphWindow(getStage(), file);
        }
    }

    private void openSample(String fileName) {
        File f = new File("samples/" + fileName);
        if (!f.exists()) {
            f = new File("graph-ui/samples/" + fileName);
        }
        if (!f.exists()) {
            alert(Alert.AlertType.ERROR, "Fichier introuvable", "Le fichier d'exemple est introuvable :\n" + f.getAbsolutePath());
            return;
        }
        openGraphWindow(getStage(), f);
    }

    private Stage getStage() {
        if (openFileLink != null && openFileLink.getScene() != null) {
            return (Stage) openFileLink.getScene().getWindow();
        }
        if (newProjectLink != null && newProjectLink.getScene() != null) {
            return (Stage) newProjectLink.getScene().getWindow();
        }
        throw new IllegalStateException("Impossible de récupérer la fenêtre principale.");
    }

    private void openGraphWindow(Stage stage, File file) {
        try {
            if (!isSupportedFile(file)) {
                alert(Alert.AlertType.WARNING,
                        "Format non pris en charge",
                        "Formats acceptés : .csv, .dot et .mongraphe\nFichier reçu : " + file.getName());
                return;
            }

            FXMLLoader loader = new FXMLLoader(getClass().getResource("/fxml/MainView.fxml"));
            Parent root = loader.load();
            MainGraphController controller = loader.getController();
            controller.openFile(file);

            Rectangle2D screenBounds = Screen.getPrimary().getVisualBounds();
            Scene scene = new Scene(root, screenBounds.getWidth(), screenBounds.getHeight());
            stage.setTitle("MonGraphe");
            stage.setScene(scene);
            stage.setResizable(true);
            stage.setX(screenBounds.getMinX());
            stage.setY(screenBounds.getMinY());
            stage.setWidth(screenBounds.getWidth());
            stage.setHeight(screenBounds.getHeight());
            stage.show();
        } catch (Exception e) {
            alert(Alert.AlertType.ERROR,
                    "Ouverture impossible",
                    "Impossible d'ouvrir la vue principale.\n\n" + rootCauseMessage(e));
        }
    }

    private boolean isSupportedFile(File file) {
        String name = file.getName().toLowerCase();
        return name.endsWith(".csv") || name.endsWith(".dot") || name.endsWith(".mongraphe");
    }

    private String rootCauseMessage(Throwable throwable) {
        Throwable current = throwable;
        while (current.getCause() != null && current.getCause() != current) {
            current = current.getCause();
        }
        String message = current.getMessage();
        return message == null || message.isBlank() ? current.toString() : message;
    }

    private void alert(Alert.AlertType type, String title, String content) {
        Alert alert = new Alert(type);
        alert.setTitle(title);
        alert.setHeaderText(null);
        alert.setContentText(content);
        alert.showAndWait();
    }
}

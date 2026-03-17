package com.mongraphe.graphui.app;

import javafx.application.Application;
import javafx.fxml.FXMLLoader;
import javafx.scene.Parent;
import javafx.scene.Scene;
import javafx.scene.control.Alert;
import javafx.stage.Stage;

public class MainApp extends Application {
    @Override
    public void start(Stage primaryStage) {
        try {
            FXMLLoader loader = new FXMLLoader(getClass().getResource("/fxml/HomeScreen.fxml"));
            Parent root = loader.load();
            primaryStage.setTitle("Accueil");
            primaryStage.setScene(new Scene(root, 1000, 700));
            primaryStage.show();
        } catch (Exception e) {
            Alert alert = new Alert(Alert.AlertType.ERROR);
            alert.setTitle("Erreur de démarrage");
            alert.setHeaderText(null);
            alert.setContentText(e.getMessage() == null ? e.toString() : e.getMessage());
            alert.showAndWait();
        }
    }
}

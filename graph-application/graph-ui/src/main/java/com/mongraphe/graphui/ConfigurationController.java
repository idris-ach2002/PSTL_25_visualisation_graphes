package com.mongraphe.graphui;

import java.io.File;
import java.io.IOException;

import com.mongraphe.graphui.GraphData.NodeCommunity;
import com.mongraphe.graphui.GraphData.SimilitudeMode;
import com.mongraphe.graphui.view.GraphView;

import javafx.collections.ObservableList;
import javafx.fxml.FXML;
import javafx.fxml.FXMLLoader;
import javafx.scene.Parent;
import javafx.scene.Scene;
import javafx.scene.control.Alert;
import javafx.scene.control.Button;
import javafx.scene.control.ComboBox;
import javafx.scene.control.TableView;
import javafx.scene.control.TextArea;
import javafx.scene.layout.BorderPane;
import javafx.stage.Stage;

public class ConfigurationController {

    @FXML
    private BorderPane mainBorderPane;
    @FXML
    private Button precedentButton;
    @FXML
    private Button suivantButton;
    @FXML
    private Button terminerButton;
    @FXML
    private Button annulerButton;
    @FXML
    private TableView<ObservableList<String>> tableView;
    @FXML
    private Button continuerButton;
    @FXML
    private TextArea fichierContent;
    @FXML
    private ComboBox<String> similarityMeasureComboBox;

    private File fichier;
    private ChoixSimilitudeController choixSimilitudeController;
    private ClusteringController clusteringController;
    private SimilitudeMode currentMeasure;
    private NodeCommunity selectedMethod;
    private String currentView = "/fxml/ChoixSimilitude.fxml";

    // Paramètres supplémentaires pour conserver l'état
    private double upThreshold = 0.0;
    private double downThreshold = 0.0;

    @FXML
    public void initialize() {
        if (fichier != null) {
            System.out.println("Fichier reçu : " + fichier.getAbsolutePath());
        } else {
            System.out.println("Aucun fichier sélectionné");
        }

        updateButtonStates();
        loadInitialView();
    }

    private void loadInitialView() {
        try {
            FXMLLoader loader = new FXMLLoader(getClass().getResource(currentView));
            Parent view = loader.load();

            choixSimilitudeController = loader.getController();
            choixSimilitudeController.setFichier(fichier);
            choixSimilitudeController.setOnMeasureSelected(measure -> {
                this.currentMeasure = measure;
                updateButtonStates();
            });

            // Restaurer la sélection précédente si elle existe
            if (currentMeasure != null) {
                choixSimilitudeController.setSelectedMeasure(currentMeasure);
            }

            mainBorderPane.setCenter(view);
        } catch (IOException e) {
            handleLoadError(e);
        }
    }

    @FXML
    private void handlePrecedent() {
        saveCurrentState();

        if (currentView.equals("/fxml/Clustering.fxml")) {
            currentView = "/fxml/ChoixSimilitude.fxml";
            loadChoixSimilitudeView();
        }

        updateButtonStates();
    }

    @FXML
    private void handleSuivant() {
        saveCurrentState();

        if (currentView.equals("/fxml/ChoixSimilitude.fxml")) {
            if (currentMeasure == null) {
                showAlert("Sélection requise", "Veuillez sélectionner une mesure de similarité avant de continuer.");
                return;
            }
            currentView = "/fxml/Clustering.fxml";
            loadClusteringView();
        }

        updateButtonStates();
    }

    @FXML
    private void handleTerminer() {
        saveCurrentState();

        if (selectedMethod == null) {
            showAlert("Sélection requise", "Veuillez sélectionner une méthode de clustering avant de terminer.");
            return;
        }

        loadVisualisationView(
                fichier,
                currentMeasure,
                upThreshold,
                downThreshold,
                selectedMethod);
    }

    private void saveCurrentState() {
        if (clusteringController != null) {
            this.selectedMethod = clusteringController.getSelectedMethod();
            // Sauvegarder d'autres paramètres si nécessaire
            this.upThreshold = clusteringController.getUpThreshold();
            this.downThreshold = clusteringController.getDownThreshold();
        }
    }

    private void loadChoixSimilitudeView() {
        try {
            FXMLLoader loader = new FXMLLoader(getClass().getResource(currentView));
            Parent view = loader.load();

            choixSimilitudeController = loader.getController();
            choixSimilitudeController.setFichier(fichier);
            choixSimilitudeController.setOnMeasureSelected(measure -> {
                this.currentMeasure = measure;
                updateButtonStates();
            });

            // Restaurer la sélection précédente
            if (currentMeasure != null) {
                choixSimilitudeController.setSelectedMeasure(currentMeasure);
            }

            mainBorderPane.setCenter(view);
        } catch (IOException e) {
            handleLoadError(e);
        }
    }

    private void loadClusteringView() {
        try {
            FXMLLoader loader = new FXMLLoader(getClass().getResource(currentView));
            Parent view = loader.load();

            clusteringController = loader.getController();
            clusteringController.initData(fichier, currentMeasure, upThreshold, downThreshold);

            // Restaurer la sélection précédente si elle existe
            if (selectedMethod != null) {
                clusteringController.setSelectedMethod(selectedMethod);
            }

            mainBorderPane.setCenter(view);
        } catch (IOException e) {
            handleLoadError(e);
        }
    }

    private void updateButtonStates() {
        precedentButton.setDisable(currentView.equals("/fxml/ChoixSimilitude.fxml"));
        suivantButton.setDisable(currentView.equals("/fxml/Clustering.fxml"));
        terminerButton.setDisable(!currentView.equals("/fxml/Clustering.fxml"));
    }

    private void showAlert(String title, String message) {
        Alert alert = new Alert(Alert.AlertType.WARNING);
        alert.setTitle(title);
        alert.setHeaderText(null);
        alert.setContentText(message);
        alert.showAndWait();
    }

    private void loadVisualisationView(File file, SimilitudeMode measure,
            double upThresh, double downThresh,
            NodeCommunity method) {
        try {
            FXMLLoader loader = new FXMLLoader();
            loader.setLocation(getClass().getResource("/fxml/Visualisation.fxml"));
            Parent root = loader.load();

            GraphView controller = loader.getController();
            controller.initData(file, measure, upThresh, downThresh, method);

            Stage stage = (Stage) mainBorderPane.getScene().getWindow();
            stage.setScene(new Scene(root, 1500, 800));
            stage.show();

        } catch (IOException e) {
            handleLoadError(e);
        }
    }

    private void handleLoadError(IOException e) {
        System.err.println("Erreur de chargement du FXML:");
        e.printStackTrace();
        Alert alert = new Alert(Alert.AlertType.ERROR);
        alert.setTitle("Erreur");
        alert.setHeaderText("Impossible de charger l'interface");
        alert.setContentText("Le fichier FXML est introuvable ou corrompu: " + e.getMessage());
        alert.showAndWait();
    }

    @FXML
    private void handleAnnuler() {
        try {
            FXMLLoader loader = new FXMLLoader(getClass().getResource("/fxml/HomeScreen.fxml"));
            Parent root = loader.load();

            Stage stage = (Stage) annulerButton.getScene().getWindow();
            Scene scene = new Scene(root, 1000, 700);
            stage.setScene(scene);
            stage.show();
        } catch (IOException e) {
            handleLoadError(e);
        }
    }

    public ConfigurationController(File fichier) {
        this.fichier = fichier;
    }

    public void setFichier(File fichier) {
        this.fichier = fichier;
        System.out.println("Fichier reçu dans ConfigurationController: " + fichier);

        // Mettre à jour les contrôleurs si ils existent déjà
        if (choixSimilitudeController != null) {
            choixSimilitudeController.setFichier(fichier);
        }
        if (clusteringController != null) {
            clusteringController.setFile(fichier);
        }
    }
}
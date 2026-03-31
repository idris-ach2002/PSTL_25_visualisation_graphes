package com.mongraphe.graphui.controller;

import java.util.ArrayList;
import java.util.List;
import java.util.Properties;

import com.mongraphe.graphui.app.CommandBus;
import com.mongraphe.graphui.interaction.commands.SetEngineOptionsCommand;
import com.mongraphe.graphui.interfaces.CommandBusLinkedI;
import com.mongraphe.graphui.rendering.GraphEngine;

import javafx.fxml.FXML;
import javafx.scene.control.Alert;
import javafx.scene.control.CheckBox;
import javafx.scene.control.TextField;

public final class EngineOptionsController implements CommandBusLinkedI<GraphEngine> {

    private CommandBus<GraphEngine> bus;

    @FXML private TextField upScale;
    @FXML private TextField stabilizedThreshold;
    @FXML private TextField attractionThreshold;
    @FXML private TextField updatedFrequence;
    @FXML private TextField newFriction;
    @FXML private TextField attractionCoefficient;
    @FXML private TextField repulsionThreshold;
    @FXML private TextField newAmortissement;
    @FXML private TextField nbClusters;

    @FXML private CheckBox enableKmeans;

    @Override
    public void setBus(CommandBus<GraphEngine> bus) {
        this.bus = bus;
    }

    @FXML
    private void applyOptions() {
        applyCurrentOptions(true);
    }

    public void applyCurrentOptions(boolean undoable) {
        if (bus == null) {
            return;
        }

        List<String> errors = new ArrayList<>();
        EngineOptions options = readOptions(errors);
        if (!errors.isEmpty()) {
            alert("Valeurs invalides", String.join("\n", errors));
            return;
        }

        if (undoable) {
            bus.dispatch(new SetEngineOptionsCommand(options));
        } else {
            bus.dispatchSyncVoid(engine -> new SetEngineOptionsCommand(options).execute(engine));
        }
    }

    public void fillProperties(Properties p) {
        put(p, "upScale", upScale.getText());
        put(p, "stabilizedThreshold", stabilizedThreshold.getText());
        put(p, "attractionThreshold", attractionThreshold.getText());
        put(p, "updatedFrequence", updatedFrequence.getText());
        put(p, "newFriction", newFriction.getText());
        put(p, "attractionCoefficient", attractionCoefficient.getText());
        put(p, "repulsionThreshold", repulsionThreshold.getText());
        put(p, "newAmortissement", newAmortissement.getText());
        put(p, "nbClusters", nbClusters.getText());

        p.setProperty("kmeansEnabled", Boolean.toString(enableKmeans.isSelected()));
    }

    public void loadFromProperties(Properties p) {
        upScale.setText(p.getProperty("upScale", upScale.getText()));
        stabilizedThreshold.setText(p.getProperty("stabilizedThreshold", stabilizedThreshold.getText()));
        attractionThreshold.setText(p.getProperty("attractionThreshold", attractionThreshold.getText()));
        updatedFrequence.setText(p.getProperty("updatedFrequence", updatedFrequence.getText()));
        newFriction.setText(p.getProperty("newFriction", newFriction.getText()));
        attractionCoefficient.setText(p.getProperty("attractionCoefficient", attractionCoefficient.getText()));
        repulsionThreshold.setText(p.getProperty("repulsionThreshold", repulsionThreshold.getText()));
        newAmortissement.setText(p.getProperty("newAmortissement", newAmortissement.getText()));
        nbClusters.setText(p.getProperty("nbClusters", nbClusters.getText()));

        enableKmeans.setSelected(Boolean.parseBoolean(p.getProperty("kmeansEnabled", "false")));
    }

    private void put(Properties p, String key, String value) {
        if (value != null) {
            p.setProperty(key, value);
        }
    }

    private EngineOptions readOptions(List<String> errors) {
        EngineOptions options = new EngineOptions();
        options.stabilizedThreshold = parseDoubleOrNull(stabilizedThreshold, errors, "Seuil de stabilité");
        options.attractionThreshold = parseDoubleOrNull(attractionThreshold, errors, "Seuil d'attraction");
        options.clusterUpdateFrequency = parseIntOrNull(updatedFrequence, errors, "Fréquence des clusters");
        options.newFriction = parseDoubleOrNull(newFriction, errors, "Friction");
        options.attractionCoefficient = parseDoubleOrNull(attractionCoefficient, errors, "Coefficient d'attraction");
        options.repulsionThreshold = parseDoubleOrNull(repulsionThreshold, errors, "Seuil de répulsion");
        options.newAmortissement = parseDoubleOrNull(newAmortissement, errors, "Amortissement");
        options.nbClusters = parseIntOrNull(nbClusters, errors, "Nombre de clusters");
        validateStrictlyPositive(updatedFrequence, options.clusterUpdateFrequency, errors, "Fréquence des clusters");
        validateStrictlyPositive(newFriction, options.newFriction, errors, "Friction");
        validateNonNegative(stabilizedThreshold, options.stabilizedThreshold, errors, "Seuil de stabilité");
        validateNonNegative(attractionThreshold, options.attractionThreshold, errors, "Seuil d'attraction");
        validateNonNegative(attractionCoefficient, options.attractionCoefficient, errors, "Coefficient d'attraction");
        validateNonNegative(repulsionThreshold, options.repulsionThreshold, errors, "Seuil de répulsion");
        validateStrictlyPositive(newAmortissement, options.newAmortissement, errors, "Amortissement");
        validateStrictlyPositive(nbClusters, options.nbClusters, errors, "Nombre de clusters");
        return options;
    }

    private void validateStrictlyPositive(TextField field, Number value, List<String> errors, String label) {
        if (value == null) {
            return;
        }
        if (value.doubleValue() > 0d) {
            clearInvalid(field);
            return;
        }
        markInvalid(field, errors, label, String.valueOf(value));
    }

    private void validateNonNegative(TextField field, Number value, List<String> errors, String label) {
        if (value == null) {
            return;
        }
        if (value.doubleValue() >= 0d) {
            clearInvalid(field);
            return;
        }
        markInvalid(field, errors, label, String.valueOf(value));
    }

    private Double parseDoubleOrNull(TextField field, List<String> errors, String label) {
        String value = normalize(field);
        if (value == null) {
            return null;
        }
        try {
            clearInvalid(field);
            return Double.parseDouble(value.replace(',', '.'));
        } catch (NumberFormatException e) {
            markInvalid(field, errors, label, value);
            return null;
        }
    }

    private Integer parseIntOrNull(TextField field, List<String> errors, String label) {
        String value = normalize(field);
        if (value == null) {
            return null;
        }
        try {
            clearInvalid(field);
            return Integer.parseInt(value);
        } catch (NumberFormatException e) {
            markInvalid(field, errors, label, value);
            return null;
        }
    }

    private String normalize(TextField field) {
        String value = field.getText();
        if (value == null) {
            clearInvalid(field);
            return null;
        }
        value = value.trim();
        if (value.isEmpty()) {
            clearInvalid(field);
            return null;
        }
        return value;
    }

    private void markInvalid(TextField field, List<String> errors, String label, String value) {
        field.setStyle("-fx-border-color: #d33; -fx-border-width: 2;");
        errors.add(label + " : valeur invalide ('" + value + "')");
    }

    private void clearInvalid(TextField field) {
        field.setStyle("");
    }

    private void alert(String title, String content) {
        Alert alert = new Alert(Alert.AlertType.WARNING);
        alert.setTitle(title);
        alert.setHeaderText(null);
        alert.setContentText(content);
        alert.showAndWait();
    }
}

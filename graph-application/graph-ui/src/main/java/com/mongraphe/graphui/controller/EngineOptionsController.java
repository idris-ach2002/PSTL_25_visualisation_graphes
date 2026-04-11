package com.mongraphe.graphui.controller;

import java.util.ArrayList;
import java.util.List;

import com.mongraphe.graphui.app.CommandBus;
import com.mongraphe.graphui.interaction.commands.SetEngineOptionsCommand;
import com.mongraphe.graphui.interfaces.CommandBusLinkedI;
import com.mongraphe.graphui.rendering.GraphEngine;

import javafx.fxml.FXML;
import javafx.scene.control.*;
import javafx.util.StringConverter;

public final class EngineOptionsController implements CommandBusLinkedI<GraphEngine> {

    private CommandBus<GraphEngine> bus;

    @FXML
    private TextField frictionField;
    @FXML
    private TextField attractionCoeffField;
    @FXML
    private TextField repulsionCoeffField;
    @FXML
    private TextField antiRepulsionField;
    @FXML
    private TextField amortissementField;
    @FXML
    private ComboBox<Integer> repulsionModeCombo;

    @Override
    public void setBus(CommandBus<GraphEngine> bus) {
        this.bus = bus;
    }

    @FXML
    private void initialize() {
        repulsionModeCombo.getItems().addAll(0, 1, 2);
        repulsionModeCombo.setConverter(new StringConverter<>() {
            @Override
            public String toString(Integer mode) {
                return switch (mode) {
                    case 0 -> "Pondéré par degrés";
                    case 1 -> "Uniforme";
                    case 2 -> "Renforcé inter-communautés";
                    default -> "";
                };
            }

            @Override
            public Integer fromString(String s) {
                return null;
            }
        });
        repulsionModeCombo.getSelectionModel().selectFirst();
    }

    @FXML
    private void applyOptions() {
        applyCurrentOptions(true);
    }

    public void applyCurrentOptions(boolean undoable) {
        if (bus == null)
            return;

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

    private EngineOptions readOptions(List<String> errors) {
        EngineOptions options = new EngineOptions();
        options.friction = parsePositiveDouble(frictionField, errors, "Friction");
        options.attractionCoefficient = parseNonNegativeDouble(attractionCoeffField, errors, "Attraction");
        options.repulsionCoeff = parseNonNegativeDouble(repulsionCoeffField, errors, "Répulsion intra-zone");
        options.antiRepulsion = parseNonNegativeDouble(antiRepulsionField, errors, "Répulsion anti-arêtes");
        options.amortissement = parsePositiveDouble(amortissementField, errors, "Amortissement");
        options.repulsionMode = repulsionModeCombo.getValue();
        return options;
    }

    private Double parsePositiveDouble(TextField field, List<String> errors, String label) {
        Double value = parseDoubleOrNull(field, errors, label);
        if (value != null && value <= 0) {
            markInvalid(field, errors, label, String.valueOf(value));
            return null;
        }
        return value;
    }

    private Double parseNonNegativeDouble(TextField field, List<String> errors, String label) {
        Double value = parseDoubleOrNull(field, errors, label);
        if (value != null && value < 0) {
            markInvalid(field, errors, label, String.valueOf(value));
            return null;
        }
        return value;
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

package com.mongraphe.graphui.controller;

import com.mongraphe.graphui.GraphData;
import com.mongraphe.graphui.app.CommandBus;
import com.mongraphe.graphui.app.commands.*;
import com.mongraphe.graphui.interfaces.CommandBusLinked;
import com.mongraphe.graphui.rendering.GraphEngine;

import javafx.fxml.FXML;
import javafx.scene.control.ComboBox;
import javafx.scene.control.TextField;

public final class EngineOptionsController
        implements CommandBusLinked<GraphEngine> {

    private CommandBus<GraphEngine> bus;

    public void setCommandBus(CommandBus<GraphEngine> bus) {
        this.bus = bus;
    }

    @FXML
    private TextField degreeFactor;
    @FXML
    private TextField initNodeSize;
    @FXML
    private TextField upScale;
    @FXML
    private TextField stabilizedThreshold;
    @FXML
    private TextField attractionThreshold;
    @FXML
    private TextField updatedFrequence;
    @FXML
    private TextField newFriction;
    @FXML
    private TextField attractionCoefficient;
    @FXML
    private TextField repulsionThreshold;
    @FXML
    private TextField newAmortissement;
    @FXML
    private TextField nbClusters;
    @FXML
    private TextField minimumDegree;
    @FXML
    private ComboBox<GraphData.RepulsionMode> repulsionModeComboBox;

    @FXML
    private void applyOptions() {

        if (bus == null)
            return;

        try {

            parseDouble(degreeFactor,
                    v -> bus.dispatch(new SetDegreeScaleFactorCommand(v)));

            parseDouble(initNodeSize,
                    v -> bus.dispatch(new SetInitialNodeSizeCommand(v)));

            parseInt(upScale,
                    v -> bus.dispatch(new SetUpscaleCommand(v)));

            parseDouble(stabilizedThreshold,
                    v -> bus.dispatch(new SetStabilizedThresholdCommand(v)));

            parseDouble(attractionThreshold,
                    v -> bus.dispatch(new SetAttractionThresholdCommand(v)));

            parseInt(updatedFrequence,
                    v -> bus.dispatch(new SetClusterUpdateFrequencyCommand(v)));

            parseDouble(newFriction,
                    v -> bus.dispatch(new SetNewFrictionCommand(v)));

            parseDouble(attractionCoefficient,
                    v -> bus.dispatch(new SetAttractionCoefficientCommand(v)));

            parseDouble(repulsionThreshold,
                    v -> bus.dispatch(new SetRepulsionThresholdCommand(v)));

            parseDouble(newAmortissement,
                    v -> bus.dispatch(new SetNewAmortissementCommand(v)));

            parseInt(nbClusters,
                    v -> bus.dispatch(new SetNbClustersCommand(v)));

            parseInt(minimumDegree,
                    v -> bus.dispatch(new SetMinimumDegreeCommand(v)));

            GraphData.RepulsionMode rm = repulsionModeComboBox.getValue();
            if (rm != null) {
                bus.dispatch(new SetRepulsionModeCommand(rm));
            }

        } catch (NumberFormatException e) {
            System.out.println("Valeur invalide: " + e.getMessage());
        }
    }

    private void parseDouble(TextField field,
            java.util.function.DoubleConsumer consumer) {
        if (field.getText() != null && !field.getText().isEmpty()) {
            consumer.accept(Double.parseDouble(field.getText()));
        }
    }

    private void parseInt(TextField field,
            java.util.function.IntConsumer consumer) {
        if (field.getText() != null && !field.getText().isEmpty()) {
            consumer.accept(Integer.parseInt(field.getText()));
        }
    }

    @Override
    public void setBus(CommandBus<GraphEngine> bus) {
        this.bus = bus;
    }
}
package com.mongraphe.graphui.controller;

import com.mongraphe.graphui.GraphData;
import com.mongraphe.graphui.app.CommandBus;
import com.mongraphe.graphui.app.commands.*;
import com.mongraphe.graphui.interfaces.CommandBusLinkedI;
import com.mongraphe.graphui.rendering.GraphEngine;

import javafx.fxml.FXML;
import javafx.scene.control.ComboBox;
import javafx.scene.control.TextField;

public final class EngineOptionsController implements CommandBusLinkedI<GraphEngine> {

        private CommandBus<GraphEngine> bus;

        @Override
        public void setBus(CommandBus<GraphEngine> bus) {
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
                        EngineOptions options = new EngineOptions();

                        options.degreeFactor = parseDouble(degreeFactor);
                        options.initialNodeSize = parseDouble(initNodeSize);
                        options.upScale = parseInt(upScale);
                        options.stabilizedThreshold = parseDouble(stabilizedThreshold);
                        options.attractionThreshold = parseDouble(attractionThreshold);
                        options.clusterUpdateFrequency = parseInt(updatedFrequence);
                        options.newFriction = parseDouble(newFriction);
                        options.attractionCoefficient = parseDouble(attractionCoefficient);
                        options.repulsionThreshold = parseDouble(repulsionThreshold);
                        options.newAmortissement = parseDouble(newAmortissement);
                        options.nbClusters = parseInt(nbClusters);
                        options.minimumDegree = parseInt(minimumDegree);
                        options.repulsionMode = repulsionModeComboBox.getValue();

                        bus.dispatchUndoable(new SetEngineOptionsCommand(options));

                } catch (NumberFormatException e) {
                        System.out.println("Valeur invalide: " + e.getMessage());
                }
        }

        private double parseDouble(TextField field) {
                return Double.parseDouble(field.getText());
        }

        private int parseInt(TextField field) {
                return Integer.parseInt(field.getText());
        }

}
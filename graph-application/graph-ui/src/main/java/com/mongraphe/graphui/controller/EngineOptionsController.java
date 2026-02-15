package com.mongraphe.graphui.controller;

import com.mongraphe.graphui.app.ApplicationContext;
import com.mongraphe.graphui.app.GraphEngineAdapter;
import com.mongraphe.graphui.interfaces.ContextAware;
import com.mongraphe.graphui.GraphData;

import javafx.fxml.FXML;
import javafx.scene.control.ComboBox;
import javafx.scene.control.TextField;

public final class EngineOptionsController implements ContextAware {

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

    private ApplicationContext context;

    @FXML
    private void applyOptions() {
        if (context == null)
            return;

        try {
            GraphEngineAdapter adapter = context.getGraphAdapter();
            parseDouble(degreeFactor, adapter::setDegreeScaleFactor);
            parseDouble(initNodeSize, adapter::setInitialNodeSize);
            parseInt(upScale, adapter::setUpscale);
            parseDouble(stabilizedThreshold, adapter::setStabilizedThreshold);
            parseDouble(attractionThreshold, adapter::setAttractionThreshold);
            parseInt(updatedFrequence, adapter::setClusterUpdateFrequency);
            parseDouble(newFriction, adapter::setNewFriction);
            parseDouble(attractionCoefficient, adapter::setAttractionCoefficient);
            parseDouble(repulsionThreshold, adapter::setRepulsionThreshold);
            parseDouble(newAmortissement, adapter::setNewAmortissement);
            parseInt(nbClusters, adapter::setNbClusters);
            parseInt(minimumDegree, adapter::setMinimumDegree);

            GraphData.RepulsionMode rm = repulsionModeComboBox.getValue();
            if (rm != null)
                adapter.setRepulsionMode(rm);

        } catch (NumberFormatException e) {
            setStatus("Valeur invalide: " + e.getMessage());
        } catch (Exception e) {
            setStatus("Impossible d'appliquer les options: " + e.getMessage());
        }
    }

    private void parseDouble(TextField field, java.util.function.DoubleConsumer consumer) {
        if (field.getText() != null && !field.getText().isEmpty()) {
            consumer.accept(Double.parseDouble(field.getText()));
        }
    }

    private void parseInt(TextField field, java.util.function.IntConsumer consumer) {
        if (field.getText() != null && !field.getText().isEmpty()) {
            consumer.accept(Integer.parseInt(field.getText()));
        }
    }

    private void setStatus(String message) {
        System.out.println(message);
    }

    @Override
    public void setContext(ApplicationContext context) {
        this.context = context;
    }
}

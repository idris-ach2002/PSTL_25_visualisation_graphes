package com.mongraphe.graphui.controller;

import java.util.ArrayList;
import java.util.List;
import java.util.Properties;

import com.mongraphe.graphui.app.CommandBus;
import com.mongraphe.graphui.interaction.commands.SetBackgroundColorCommand;
import com.mongraphe.graphui.interaction.commands.SetEngineOptionsCommand;
import com.mongraphe.graphui.interfaces.CommandBusLinkedI;
import com.mongraphe.graphui.model.GraphModel;
import com.mongraphe.graphui.rendering.GraphEngine;
import com.mongraphe.graphui.rendering.GraphRenderOptions;
import com.mongraphe.graphui.rendering.GraphRenderer;

import javafx.embed.swing.SwingNode;
import javafx.fxml.FXML;
import javafx.scene.control.Alert;
import javafx.scene.control.CheckBox;
import javafx.scene.control.ColorPicker;
import javafx.scene.control.ComboBox;
import javafx.scene.control.TextField;
import javafx.scene.layout.BorderPane;
import javafx.scene.paint.Color;

public class PreviewController implements CommandBusLinkedI<GraphEngine> {

    private static final double DEFAULT_CURVE_ANGLE = 22d;

    private CommandBus<GraphEngine> bus;
    private GraphRenderer previewRenderer;

    @FXML private TextField degreeFactor;
    @FXML private TextField initNodeSize;

    @FXML private TextField minimumDegree;
    @FXML private TextField minEdgeWeight;

    @FXML private CheckBox curvedEdgesToggle;
    @FXML private TextField curveAngleField;

    @FXML private BorderPane graphContainer;

    @FXML private ComboBox<GraphModel.ColoringMode> coloringModeCombo;
    @FXML private ColorPicker canvasColorPicker;
    @FXML private ColorPicker uniformNodeColorPicker;

    @FXML
    private void initialize() {
        if (coloringModeCombo != null) {
            coloringModeCombo.getItems().setAll(GraphModel.ColoringMode.values());
            coloringModeCombo.getSelectionModel().select(GraphModel.ColoringMode.COMMUNITY);
        }

        if (canvasColorPicker != null) {
            canvasColorPicker.setValue(Color.WHITE);
        }
        if (uniformNodeColorPicker != null) {
            uniformNodeColorPicker.setValue(Color.web("#d1d1db"));
        }

        if (curvedEdgesToggle != null) {
            curvedEdgesToggle.setSelected(true);
            curvedEdgesToggle.selectedProperty().addListener((obs, oldValue, selected) -> updateCurveAngleFieldState(selected));
        }
        if (curveAngleField != null) {
            curveAngleField.setText(formatAngle(DEFAULT_CURVE_ANGLE));
        }
        updateCurveAngleFieldState(curvedEdgesToggle == null || curvedEdgesToggle.isSelected());
    }

    @Override
    public void setBus(CommandBus<GraphEngine> bus) {
        this.bus = bus;
    }

    public void setPreviewRenderer(GraphRenderer previewRenderer) {
        this.previewRenderer = previewRenderer;
        applyPreviewRenderOptions();
    }

    public void setGraphPanel(SwingNode panel) {
        if (graphContainer != null) {
            graphContainer.setCenter(panel);
        }
    }

    @FXML
    private void applyOptions() {
        applyCurrentOptions(true);
    }

    public void applyCurrentOptions(boolean undoable) {
        if (bus == null) {
            applyPreviewRenderOptions();
            return;
        }

        List<String> errors = new ArrayList<>();
        EngineOptions options = readOptions(errors);
        Double previewCurveAngle = readCurveAngle(errors);
        if (!errors.isEmpty()) {
            alert("Valeurs invalides", String.join("\n", errors));
            return;
        }

        if (undoable) {
            bus.dispatch(new SetEngineOptionsCommand(options));
        } else {
            bus.dispatchSyncVoid(engine -> new SetEngineOptionsCommand(options).execute(engine));
        }

        applyAppearance(previewCurveAngle);
    }

    public void applyAppearance() {
        applyAppearance(null);
    }

    private void applyAppearance(Double previewCurveAngle) {
        if (bus != null) {
            Color bg = canvasColorPicker == null ? null : canvasColorPicker.getValue();
            if (bg != null) {
                bus.dispatchSyncVoid(engine -> new SetBackgroundColorCommand(
                        (float) bg.getRed(),
                        (float) bg.getGreen(),
                        (float) bg.getBlue(),
                        (float) bg.getOpacity()).execute(engine));
            }

            GraphModel.ColoringMode coloringMode = coloringModeCombo == null ? null : coloringModeCombo.getValue();
            if (coloringMode != null) {
                bus.dispatchSyncVoid(engine -> engine.setColoringMode(coloringMode));
            }

            Color uniform = uniformNodeColorPicker == null ? null : uniformNodeColorPicker.getValue();
            if (uniform != null) {
                bus.dispatchSyncVoid(engine -> engine.setUniformNodeColor(
                        (float) uniform.getRed(),
                        (float) uniform.getGreen(),
                        (float) uniform.getBlue()));
            }
        }

        applyPreviewRenderOptions(previewCurveAngle);
    }

    private void applyPreviewRenderOptions() {
        List<String> errors = new ArrayList<>();
        Double previewCurveAngle = readCurveAngle(errors);
        if (!errors.isEmpty()) {
            return;
        }
        applyPreviewRenderOptions(previewCurveAngle);
    }

    private void applyPreviewRenderOptions(Double previewCurveAngle) {
        if (previewRenderer == null) {
            return;
        }

        boolean curved = curvedEdgesToggle == null || curvedEdgesToggle.isSelected();
        double effectiveAngle = previewCurveAngle == null ? DEFAULT_CURVE_ANGLE : previewCurveAngle.doubleValue();
        previewRenderer.setRenderOptions(GraphRenderOptions.previewView(curved, (float) effectiveAngle));
    }

    public void fillProperties(Properties p) {
        put(p, "degreeFactor", textOf(degreeFactor));
        put(p, "initNodeSize", textOf(initNodeSize));
        put(p, "minimumDegree", textOf(minimumDegree));
        put(p, "minEdgeWeight", textOf(minEdgeWeight));
        put(p, "previewCurveAngle", textOf(curveAngleField));

        if (curvedEdgesToggle != null) {
            p.setProperty("previewCurvedEdges", Boolean.toString(curvedEdgesToggle.isSelected()));
        }
        if (coloringModeCombo != null && coloringModeCombo.getValue() != null) {
            p.setProperty("coloringMode", coloringModeCombo.getValue().name());
        }
        if (canvasColorPicker != null && canvasColorPicker.getValue() != null) {
            p.setProperty("background", canvasColorPicker.getValue().toString());
        }
        if (uniformNodeColorPicker != null && uniformNodeColorPicker.getValue() != null) {
            p.setProperty("uniformNodeColor", uniformNodeColorPicker.getValue().toString());
        }
    }

    public void loadFromProperties(Properties p) {
        setTextIfPresent(degreeFactor, p.getProperty("degreeFactor"));
        setTextIfPresent(initNodeSize, p.getProperty("initNodeSize"));
        setTextIfPresent(minimumDegree, p.getProperty("minimumDegree"));
        setTextIfPresent(minEdgeWeight, p.getProperty("minEdgeWeight"));
        setTextIfPresent(curveAngleField, p.getProperty("previewCurveAngle", formatAngle(DEFAULT_CURVE_ANGLE)));

        if (curvedEdgesToggle != null) {
            curvedEdgesToggle.setSelected(Boolean.parseBoolean(p.getProperty("previewCurvedEdges", "true")));
        }
        updateCurveAngleFieldState(curvedEdgesToggle == null || curvedEdgesToggle.isSelected());

        setEnumValue(coloringModeCombo, p.getProperty("coloringMode"), GraphModel.ColoringMode.class);

        String background = p.getProperty("background");
        if (canvasColorPicker != null && background != null && !background.isBlank()) {
            canvasColorPicker.setValue(Color.valueOf(background));
        }
        String uniform = p.getProperty("uniformNodeColor");
        if (uniformNodeColorPicker != null && uniform != null && !uniform.isBlank()) {
            uniformNodeColorPicker.setValue(Color.valueOf(uniform));
        }

        applyPreviewRenderOptions();
    }

    private <E extends Enum<E>> void setEnumValue(ComboBox<E> comboBox, String name, Class<E> enumClass) {
        if (comboBox == null || name == null || name.isBlank()) {
            return;
        }
        try {
            comboBox.setValue(Enum.valueOf(enumClass, name));
        } catch (IllegalArgumentException ignored) {
        }
    }

    private void put(Properties p, String key, String value) {
        if (value != null) {
            p.setProperty(key, value);
        }
    }

    private EngineOptions readOptions(List<String> errors) {
        EngineOptions options = new EngineOptions();
        options.degreeFactor = parseDoubleOrNull(degreeFactor, errors, "Agrandissement par degré");
        options.initialNodeSize = parseDoubleOrNull(initNodeSize, errors, "Taille d'un nœud");
        options.minimumDegree = parseIntOrNull(minimumDegree, errors, "Degré minimum");
        options.minEdgeWeight = parseDoubleOrNull(minEdgeWeight, errors, "Poids min arêtes");

        validateStrictlyPositive(initNodeSize, options.initialNodeSize, errors, "Taille d'un nœud");
        validateNonNegative(degreeFactor, options.degreeFactor, errors, "Agrandissement par degré");
        validateNonNegative(minimumDegree, options.minimumDegree, errors, "Degré minimum");
        validateNonNegative(minEdgeWeight, options.minEdgeWeight, errors, "Poids min arêtes");
        return options;
    }

    private Double readCurveAngle(List<String> errors) {
        if (curveAngleField == null) {
            return DEFAULT_CURVE_ANGLE;
        }

        Double angle = parseDoubleOrNull(curveAngleField, errors, "Angle de courbure");
        if (angle == null) {
            clearInvalid(curveAngleField);
            return DEFAULT_CURVE_ANGLE;
        }
        if (angle >= 0d && angle <= 85d) {
            clearInvalid(curveAngleField);
            return angle;
        }
        markInvalid(curveAngleField, errors, "Angle de courbure", String.valueOf(angle));
        return null;
    }

    private void validateStrictlyPositive(TextField field, Number value, List<String> errors, String label) {
        if (field == null || value == null) {
            return;
        }
        if (value.doubleValue() > 0d) {
            clearInvalid(field);
            return;
        }
        markInvalid(field, errors, label, String.valueOf(value));
    }

    private void validateNonNegative(TextField field, Number value, List<String> errors, String label) {
        if (field == null || value == null) {
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
        if (field == null) {
            return null;
        }
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
        if (field != null) {
            field.setStyle("-fx-border-color: #d33; -fx-border-width: 2;");
        }
        errors.add(label + " : valeur invalide ('" + value + "')");
    }

    private void clearInvalid(TextField field) {
        if (field != null) {
            field.setStyle("");
        }
    }

    private void updateCurveAngleFieldState(boolean curvedEnabled) {
        if (curveAngleField == null) {
            return;
        }
        curveAngleField.setDisable(!curvedEnabled);
        if (curvedEnabled) {
            curveAngleField.setStyle("");
        }
    }

    private String textOf(TextField field) {
        return field == null ? null : field.getText();
    }

    private void setTextIfPresent(TextField field, String value) {
        if (field != null && value != null) {
            field.setText(value);
        }
    }

    private String formatAngle(double angle) {
        if (Math.rint(angle) == angle) {
            return Integer.toString((int) angle);
        }
        return Double.toString(angle);
    }

    private void alert(String title, String content) {
        Alert alert = new Alert(Alert.AlertType.WARNING);
        alert.setTitle(title);
        alert.setHeaderText(null);
        alert.setContentText(content);
        alert.showAndWait();
    }
}

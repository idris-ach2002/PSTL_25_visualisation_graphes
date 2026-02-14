package com.mongraphe.graphui.controller;

import com.mongraphe.graphui.app.GraphEngineAdapter;
import com.mongraphe.graphui.app.InteractionService;
import com.mongraphe.graphui.app.InteractionService.Mode;
import com.mongraphe.graphui.view.GraphPanel;

import javafx.fxml.FXML;
import javafx.scene.control.ColorPicker;
import javafx.scene.control.Toggle;
import javafx.scene.control.ToggleGroup;
import javafx.scene.layout.StackPane;

public final class GraphWorkspaceController {

    @FXML
    private StackPane graphContainer;
    @FXML
    private ToggleGroup toolToggleGroup;
    @FXML
    private ColorPicker canvasColorPicker;

    private GraphEngineAdapter graphEngineAdapter;
    private InteractionService interaction;

    public void init(GraphEngineAdapter adapter, InteractionService interaction, GraphPanel panel) {
        this.graphEngineAdapter = adapter;
        this.interaction = interaction;
        mountCanvas(panel);
    }

    private void mountCanvas(GraphPanel panel) {
        if (graphEngineAdapter == null)
            return;

        var canvasNode = panel.canvas();

        if (canvasNode == null)
            return;

        graphContainer.getChildren().setAll(canvasNode);
        
        // Adapter resize
        graphContainer.widthProperty().addListener((obs, oldVal, newVal) -> {
            graphEngineAdapter.resizeCamera(newVal.intValue(), (int) graphContainer.getHeight());
        });
        graphContainer.heightProperty().addListener((obs, oldVal, newVal) -> {
            graphEngineAdapter.resizeCamera((int) graphContainer.getWidth(), newVal.intValue());
        });
    }

    @FXML
    private void handleToolChange() {
        Toggle t = toolToggleGroup.getSelectedToggle();
        if (t == null)
            return;

        try {
            interaction.setMode(Mode.valueOf(String.valueOf(t.getUserData())));
        } catch (Exception e) {
            interaction.setMode(Mode.RUN);
        }
    }

    @FXML
    private void applyCanvasColor() {

        if (graphEngineAdapter == null)
            return;

        var c = canvasColorPicker.getValue();

        graphEngineAdapter.setBackgroundColor(
                (float) c.getRed(),
                (float) c.getGreen(),
                (float) c.getBlue(),
                (float) c.getOpacity());
    }
}

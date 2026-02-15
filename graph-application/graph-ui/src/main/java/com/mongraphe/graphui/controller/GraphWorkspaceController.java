package com.mongraphe.graphui.controller;

import com.mongraphe.graphui.app.ApplicationContext;
import com.mongraphe.graphui.app.GraphEngineAdapter;
import com.mongraphe.graphui.app.InteractionService;
import com.mongraphe.graphui.app.InteractionService.Mode;
import com.mongraphe.graphui.interfaces.ContextAware;
import com.mongraphe.graphui.view.GraphPanel;

import javafx.fxml.FXML;
import javafx.scene.control.ColorPicker;
import javafx.scene.control.Toggle;
import javafx.scene.control.ToggleGroup;
import javafx.scene.layout.StackPane;

public final class GraphWorkspaceController implements ContextAware {

    @FXML
    private StackPane graphContainer;
    @FXML
    private ToggleGroup toolToggleGroup;
    @FXML
    private ColorPicker canvasColorPicker;

    private InteractionService interaction;
    private ApplicationContext context;

    public void init(GraphEngineAdapter adapter, InteractionService interaction, GraphPanel panel) {
        this.interaction = interaction;
        mountCanvas(panel);
    }

    private void mountCanvas(GraphPanel panel) {
        if (context == null)
            return;

        var canvasNode = panel.canvas();

        if (canvasNode == null)
            return;

        graphContainer.getChildren().setAll(canvasNode);
        
        // Adapter resize
        graphContainer.widthProperty().addListener((obs, oldVal, newVal) -> {
            context.getGraphAdapter().resizeCamera(newVal.intValue(), (int) graphContainer.getHeight());
        });
        graphContainer.heightProperty().addListener((obs, oldVal, newVal) -> {
            context.getGraphAdapter().resizeCamera((int) graphContainer.getWidth(), newVal.intValue());
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

        if (context.getGraphAdapter() == null)
            return;

        var c = canvasColorPicker.getValue();

        context.getGraphAdapter().setBackgroundColor(
                (float) c.getRed(),
                (float) c.getGreen(),
                (float) c.getBlue(),
                (float) c.getOpacity());
    }

    @Override
    public void setContext(ApplicationContext context) {
        this.context = context;
    }
}

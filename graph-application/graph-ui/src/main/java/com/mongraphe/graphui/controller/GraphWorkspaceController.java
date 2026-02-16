package com.mongraphe.graphui.controller;

import com.mongraphe.graphui.app.CommandBus;
import com.mongraphe.graphui.app.InteractionService;
import com.mongraphe.graphui.app.InteractionService.Mode;
import com.mongraphe.graphui.app.commands.SetBackgroundColorCommand;
import com.mongraphe.graphui.interfaces.CommandBusLinked;
import com.mongraphe.graphui.rendering.GraphEngine;
import com.mongraphe.graphui.view.GraphPanel;

import javafx.fxml.FXML;
import javafx.scene.control.ColorPicker;
import javafx.scene.control.Toggle;
import javafx.scene.control.ToggleGroup;
import javafx.scene.layout.StackPane;

public final class GraphWorkspaceController implements CommandBusLinked<GraphEngine> {

    @FXML
    private StackPane graphContainer;
    @FXML
    private ToggleGroup toolToggleGroup;
    @FXML
    private ColorPicker canvasColorPicker;

    private CommandBus<GraphEngine> bus;
    private InteractionService interaction;

    public GraphWorkspaceController(GraphPanel panel, InteractionService interaction) {
        this.interaction = interaction;
        mountCanvas(panel);
    }

    private void mountCanvas(GraphPanel panel) {
        if (bus == null)
            return;

        var canvasNode = panel.canvas();

        if (canvasNode == null)
            return;

        graphContainer.getChildren().setAll(canvasNode);

        graphContainer.widthProperty().addListener((obs, oldVal, newVal) -> {
            bus.dispatch(g -> g.camera().resize(newVal.intValue(), (int) graphContainer.getHeight()));
        });
        graphContainer.heightProperty().addListener((obs, oldVal, newVal) -> {
            bus.dispatch(g -> g.camera().resize((int) graphContainer.getWidth(), newVal.intValue()));
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

        if (bus == null)
            return;

        var c = canvasColorPicker.getValue();

        bus.dispatch(new SetBackgroundColorCommand(
                (float) c.getRed(),
                (float) c.getGreen(),
                (float) c.getBlue(),
                (float) c.getOpacity()));
    }

    @Override
    public void setBus(CommandBus<GraphEngine> bus) {
        this.bus = bus;
    }
}

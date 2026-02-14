package com.mongraphe.graphui.view;

import com.mongraphe.graphui.app.GraphEngineAdapter;
import com.mongraphe.graphui.app.InteractionService;

import javafx.fxml.FXML;
import javafx.scene.control.ColorPicker;
import javafx.scene.control.ToggleButton;
import javafx.scene.control.ToggleGroup;
import javafx.scene.layout.StackPane;

public final class GraphWorkspacePanel {

    @FXML
    private StackPane graphContainer;
    @FXML
    private ToggleGroup toolToggleGroup;
    @FXML
    private ToggleButton toolRun;
    @FXML
    private ToggleButton toolSelect;
    @FXML
    private ToggleButton toolMove;
    @FXML
    private ToggleButton toolDelete;
    @FXML
    private ColorPicker canvasColorPicker;

    private InteractionService interaction;
    private GraphEngineAdapter engine;

    public void init(GraphEngineAdapter engine, InteractionService interaction) {
        this.engine = engine;
        this.interaction = interaction;

        //mountCanvas();
        setupTools();
        setupCanvasColor();
    }

    /*
    private void mountCanvas() {
        if (engine.panel() == null)
            return;

        graphContainer.getChildren().setAll(engine.panel().canvas());
        interaction.attach(engine.panel().window());

        graphContainer.widthProperty()
                .addListener((obs, o, n) -> engine.camera().resize(n.intValue(), (int) graphContainer.getHeight()));

        graphContainer.heightProperty()
                .addListener((obs, o, n) -> engine.camera().resize((int) graphContainer.getWidth(), n.intValue()));
    }*/

    private void setupTools() {
        toolToggleGroup.selectedToggleProperty().addListener((obs, old, now) -> {
            if (now == null)
                return;
            interaction.setMode(
                    InteractionService.Mode.valueOf(String.valueOf(now.getUserData())));
        });

        toolToggleGroup.selectToggle(toolRun);
    }

    private void setupCanvasColor() {
        canvasColorPicker.setOnAction(e -> {
            var c = canvasColorPicker.getValue();
            engine.setBackgroundColor(
                    (float) c.getRed(),
                    (float) c.getGreen(),
                    (float) c.getBlue(),
                    1f);
        });
    }
}

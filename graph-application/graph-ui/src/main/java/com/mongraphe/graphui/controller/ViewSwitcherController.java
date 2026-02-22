package com.mongraphe.graphui.controller;

import com.mongraphe.graphui.app.CommandBus;
import com.mongraphe.graphui.interfaces.CommandBusLinkedI;
import com.mongraphe.graphui.rendering.GraphEngine;
import com.mongraphe.graphui.view.GraphPanel;

import javafx.fxml.FXML;
import javafx.scene.control.Toggle;
import javafx.scene.control.ToggleGroup;
import javafx.scene.layout.BorderPane;
import javafx.scene.layout.Pane;

public final class ViewSwitcherController implements CommandBusLinkedI<GraphEngine> {

    private CommandBus<GraphEngine> bus;

    @Override
    public void setBus(CommandBus<GraphEngine> bus) {
        this.bus = bus;
    }

    @FXML
    private ToggleGroup viewToggleGroup;
    @FXML
    private BorderPane mainLayout;
    @FXML
    private Pane overview;
    @FXML
    private Pane dataView;
    @FXML
    private Pane preview;

    public void setGraphPanel(GraphPanel panel) {
        overview.getChildren().add(panel.canvas());
    }

    @FXML
    private void handleViewChange() {

        String view = String.valueOf(
                viewToggleGroup.getSelectedToggle().getUserData());

        mainLayout.setVisible("overview".equals(view));
        dataView.setVisible("data".equals(view));
        preview.setVisible("preview".equals(view));
    }
}
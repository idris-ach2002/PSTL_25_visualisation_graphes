package com.mongraphe.graphui.controller;

import com.mongraphe.graphui.model.GraphModel;
import com.mongraphe.graphui.Vertex;
import com.mongraphe.graphui.Edge;
import javafx.fxml.FXML;
import javafx.scene.control.Label;
import javafx.scene.control.TableView;

public final class StatsController {

    @FXML
    private TableView<Vertex> vertexTable;
    @FXML
    private TableView<Edge> edgeTable;
    @FXML
    private Label totalElementsLabel;

    public void refresh(GraphModel model) {
        synchronized (model.mutex()) {
            vertexTable.getItems().setAll(model.vertices());
            edgeTable.getItems().setAll(model.edges());
            totalElementsLabel.setText(
                    String.valueOf(
                            model.vertices().size() + model.edges().size()));
        }
    }
}

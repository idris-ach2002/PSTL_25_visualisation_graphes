package com.mongraphe.graphui.controller;

import com.mongraphe.graphui.Vertex;
import com.mongraphe.graphui.app.ApplicationContext;
import com.mongraphe.graphui.app.GraphEngineAdapter.GraphDataSnapshot;
import com.mongraphe.graphui.interfaces.CommandBusLinked;

import com.mongraphe.graphui.Edge;
import javafx.fxml.FXML;
import javafx.scene.control.Label;
import javafx.scene.control.TableView;

public final class StatsController implements CommandBusLinked<CommandGraph> {

    @FXML
    private TableView<Vertex> vertexTable;
    @FXML
    private TableView<Edge> edgeTable;
    @FXML
    private Label totalElementsLabel;

    private ApplicationContext context;

    public void refresh() {
        if (context == null) {
            return;
        }
        GraphDataSnapshot snapshot = context.getGraphAdapter().getDataSnapshot();
        vertexTable.getItems().setAll(snapshot.getVertices());
        edgeTable.getItems().setAll(snapshot.getEdges());
        totalElementsLabel.setText(
                String.valueOf(snapshot.getVertices().size() + snapshot.getEdges().size()));
    }

    @Override
    public void setContext(ApplicationContext context) {
        this.context = context;
    }
}

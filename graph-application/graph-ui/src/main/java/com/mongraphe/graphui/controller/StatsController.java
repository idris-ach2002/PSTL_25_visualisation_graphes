package com.mongraphe.graphui.controller;

import com.mongraphe.graphui.Vertex;
import com.mongraphe.graphui.app.CommandBus;
import com.mongraphe.graphui.interfaces.CommandBusLinkedI;
import com.mongraphe.graphui.rendering.GraphEngine;
import com.mongraphe.graphui.rendering.GraphEngine.GraphDataSnapshot;
import com.mongraphe.graphui.Edge;
import javafx.fxml.FXML;
import javafx.scene.control.Label;
import javafx.scene.control.TableView;

public final class StatsController implements CommandBusLinkedI<GraphEngine> {

    @FXML
    private Label nodesDisplayedLabel;
    @FXML
    private Label edgesDisplayedLabel;
    @FXML
    private TableView<Vertex> vertexTable;
    @FXML
    private TableView<Edge> edgeTable;
    @FXML
    private Label totalElementsLabel;

    private CommandBus<GraphEngine> bus;

    @Override
    public void setBus(CommandBus<GraphEngine> bus) {
        this.bus = bus;
    }

    public void refresh() {
        if (bus == null) {
            return;
        }
        GraphDataSnapshot snapshot = bus.dispatchSync(e -> e.getDataSnapshot());

        nodesDisplayedLabel.setText(String.valueOf(snapshot.getVisibleVertexCount()));
        edgesDisplayedLabel.setText(String.valueOf(snapshot.getVisibleEdgeCount()));
        vertexTable.getItems().setAll(snapshot.getVertices());
        edgeTable.getItems().setAll(snapshot.getEdges());
    }

}
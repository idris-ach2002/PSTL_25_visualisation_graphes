package com.mongraphe.graphui.controller;

import com.mongraphe.graphui.Vertex;
import com.mongraphe.graphui.app.CommandBus;
import com.mongraphe.graphui.interfaces.CommandBusLinkedI;
import com.mongraphe.graphui.rendering.GraphEngine;
import com.mongraphe.graphui.rendering.GraphEngine.GraphDataSnapshot;
import com.mongraphe.graphui.Edge;
import javafx.fxml.FXML;
import javafx.scene.control.Label;
import javafx.scene.control.TableColumn;
import javafx.scene.control.TableView;

public final class DataController implements CommandBusLinkedI<GraphEngine> {

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

    @FXML
    private TableColumn<Vertex, Integer> vertexIdCol;
    @FXML
    private TableColumn<Vertex, ?> vertexCommunityCol;
    @FXML
    private TableColumn<Vertex, Integer> vertexDegreeCol;
    @FXML
    private TableColumn<Vertex, Double> vertexXCol;
    @FXML
    private TableColumn<Vertex, Double> vertexYCol;
    @FXML
    private TableColumn<Vertex, Double> vertexDiameterCol;
    @FXML
    private TableColumn<Vertex, Boolean> vertexDeletedCol;

    @FXML
    private TableColumn<Edge, Integer> edgeStartCol;
    @FXML
    private TableColumn<Edge, Integer> edgeEndCol;
    @FXML
    private TableColumn<Edge, Double> edgeWeightCol;

    private CommandBus<GraphEngine> bus;

    @Override
    public void setBus(CommandBus<GraphEngine> bus) {
        this.bus = bus;
    }

    public void refresh() {
        if (bus == null) {
            System.out.println("Bus is null");
            return;
        }
        GraphDataSnapshot snapshot = bus.dispatchSync(e -> e.getDataSnapshot());
        System.out.println("Refresh ! " + snapshot.getVisibleEdgeCount());

        nodesDisplayedLabel.setText(
                String.valueOf(snapshot.getVisibleVertexCount()));
        
        edgesDisplayedLabel.setText(
                String.valueOf(snapshot.getVisibleEdgeCount()));

        totalElementsLabel.setText(
                String.valueOf(
                        snapshot.getVertices().size() +
                                snapshot.getEdges().size()));

        vertexTable.getItems().clear();
        vertexTable.getItems().addAll(snapshot.getVertices());

        edgeTable.getItems().clear();
        edgeTable.getItems().addAll(snapshot.getEdges());
    }

}
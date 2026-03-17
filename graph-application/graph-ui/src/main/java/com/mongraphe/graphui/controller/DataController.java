package com.mongraphe.graphui.controller;

import com.mongraphe.graphui.app.CommandBus;
import com.mongraphe.graphui.interfaces.CommandBusLinkedI;
import com.mongraphe.graphui.model.Edge;
import com.mongraphe.graphui.model.Vertex;
import com.mongraphe.graphui.rendering.GraphEngine;
import com.mongraphe.graphui.rendering.GraphEngine.GraphDataSnapshot;

import javafx.fxml.FXML;
import javafx.scene.control.*;
import javafx.scene.control.cell.PropertyValueFactory;
import javafx.scene.input.KeyCode;
import java.util.Collections;
import java.util.List;

public final class DataController implements CommandBusLinkedI<GraphEngine> {

    private static final int ROWS_PER_PAGE = 100;

    @FXML private Label nodesDisplayedLabel;
    @FXML private Label edgesDisplayedLabel;
    @FXML private Label totalElementsLabel;

    @FXML private Pagination vertexPagination;
    @FXML private Pagination edgePagination;

    @FXML private TextField vertexPageField;
    @FXML private TextField edgePageField;

    private List<Vertex> allVertices = Collections.emptyList();
    private List<Edge> allEdges = Collections.emptyList();

    private TableColumn<Vertex, Integer> vertexIdCol;
    private TableColumn<Vertex, ?> vertexCommunityCol;
    private TableColumn<Vertex, Integer> vertexDegreeCol;
    private TableColumn<Vertex, Double> vertexXCol;
    private TableColumn<Vertex, Double> vertexYCol;
    private TableColumn<Vertex, Double> vertexDiameterCol;
    private TableColumn<Vertex, Boolean> vertexDeletedCol;

    private TableColumn<Edge, Integer> edgeStartCol;
    private TableColumn<Edge, Integer> edgeEndCol;
    private TableColumn<Edge, Double> edgeWeightCol;

    private CommandBus<GraphEngine> bus;

    @Override
    public void setBus(CommandBus<GraphEngine> bus) {
        this.bus = bus;
    }

    @FXML
    private void initialize() {
        // Vertex columns
        vertexIdCol = new TableColumn<>("ID");
        vertexIdCol.setCellValueFactory(new PropertyValueFactory<>("id"));
        vertexCommunityCol = new TableColumn<>("Communauté");
        vertexCommunityCol.setCellValueFactory(new PropertyValueFactory<>("communityName"));
        vertexDegreeCol = new TableColumn<>("Degré");
        vertexDegreeCol.setCellValueFactory(new PropertyValueFactory<>("degree"));
        vertexXCol = new TableColumn<>("X");
        vertexXCol.setCellValueFactory(new PropertyValueFactory<>("x"));
        vertexYCol = new TableColumn<>("Y");
        vertexYCol.setCellValueFactory(new PropertyValueFactory<>("y"));
        vertexDiameterCol = new TableColumn<>("Diamètre");
        vertexDiameterCol.setCellValueFactory(new PropertyValueFactory<>("diameter"));
        vertexDeletedCol = new TableColumn<>("Supprimé ?");
        vertexDeletedCol.setCellValueFactory(new PropertyValueFactory<>("deleted"));

        // Edge columns
        edgeStartCol = new TableColumn<>("Début");
        edgeStartCol.setCellValueFactory(new PropertyValueFactory<>("startId"));
        edgeEndCol = new TableColumn<>("Fin");
        edgeEndCol.setCellValueFactory(new PropertyValueFactory<>("endId"));
        edgeWeightCol = new TableColumn<>("Poids");
        edgeWeightCol.setCellValueFactory(new PropertyValueFactory<>("weight"));

        // Pagination
        vertexPagination.setPageFactory(this::createVertexPage);
        edgePagination.setPageFactory(this::createEdgePage);

        vertexPagination.setPageCount(1);
        edgePagination.setPageCount(1);

        // Navigation par champ de texte
        setupPageField(vertexPageField, vertexPagination);
        setupPageField(edgePageField, edgePagination);
    }

    private void setupPageField(TextField field, Pagination pagination) {
        field.setOnAction(e -> goToPage(field, pagination));
        field.setOnKeyPressed(e -> {
            if (e.getCode() == KeyCode.ENTER) {
                goToPage(field, pagination);
            }
        });
    }

    private void goToPage(TextField field, Pagination pagination) {
        try {
            int page = Integer.parseInt(field.getText());
            if (page >= 1 && page <= pagination.getPageCount()) {
                pagination.setCurrentPageIndex(page - 1);
            } else {
                field.setText(String.valueOf(pagination.getCurrentPageIndex() + 1));
            }
        } catch (NumberFormatException ex) {
            field.setText(String.valueOf(pagination.getCurrentPageIndex() + 1));
        }
    }

    public void refresh() {
        if (bus == null) return;

        GraphDataSnapshot snapshot = bus.dispatchSync(GraphEngine::getDataSnapshot);

        allVertices = snapshot.getVertices();
        allEdges = snapshot.getEdges();

        int vertexPages = (int) Math.ceil((double) allVertices.size() / ROWS_PER_PAGE);
        int edgePages = (int) Math.ceil((double) allEdges.size() / ROWS_PER_PAGE);

        vertexPagination.setPageCount(Math.max(vertexPages, 1));
        edgePagination.setPageCount(Math.max(edgePages, 1));

        vertexPagination.setCurrentPageIndex(0);
        edgePagination.setCurrentPageIndex(0);

        vertexPageField.setText("1");
        edgePageField.setText("1");

        nodesDisplayedLabel.setText(String.valueOf(snapshot.getVisibleVertexCount()));
        edgesDisplayedLabel.setText(String.valueOf(snapshot.getVisibleEdgeCount()));
        totalElementsLabel.setText(String.valueOf(allVertices.size() + allEdges.size()));
    }

    @SuppressWarnings("unchecked")
    private TableView<Vertex> createVertexPage(int pageIndex) {
        TableView<Vertex> table = new TableView<>();
        table.getColumns().addAll(vertexIdCol, vertexCommunityCol, vertexDegreeCol,
                vertexXCol, vertexYCol, vertexDiameterCol, vertexDeletedCol);

        int from = pageIndex * ROWS_PER_PAGE;
        int to = Math.min(from + ROWS_PER_PAGE, allVertices.size());
        if (!allVertices.isEmpty()) {
            table.getItems().setAll(allVertices.subList(from, to));
        }
        return table;
    }

    @SuppressWarnings("unchecked")
    private TableView<Edge> createEdgePage(int pageIndex) {
        TableView<Edge> table = new TableView<>();
        table.getColumns().addAll(edgeStartCol, edgeEndCol, edgeWeightCol);

        int from = pageIndex * ROWS_PER_PAGE;
        int to = Math.min(from + ROWS_PER_PAGE, allEdges.size());
        if (!allEdges.isEmpty()) {
            table.getItems().setAll(allEdges.subList(from, to));
        }
        return table;
    }
}
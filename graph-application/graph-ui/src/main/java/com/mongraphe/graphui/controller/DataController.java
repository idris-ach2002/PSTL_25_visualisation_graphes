package com.mongraphe.graphui.controller;

import com.mongraphe.graphui.app.CommandBus;
import com.mongraphe.graphui.interfaces.CommandBusLinkedI;
import com.mongraphe.graphui.interfaces.controller.DataControllerDocumentation;
import com.mongraphe.graphui.model.Edge;
import com.mongraphe.graphui.model.Vertex;
import com.mongraphe.graphui.rendering.GraphEngine;

import javafx.fxml.FXML;
import javafx.scene.control.Label;
import javafx.scene.control.Pagination;
import javafx.scene.control.TableColumn;
import javafx.scene.control.TableView;
import javafx.scene.control.TextField;
import javafx.scene.control.cell.PropertyValueFactory;
import javafx.scene.input.KeyCode;

public final class DataController implements CommandBusLinkedI<GraphEngine>, DataControllerDocumentation {

    private static final int ROWS_PER_PAGE = 100;

    @FXML
    private Label nodesDisplayedLabel;
    @FXML
    private Label edgesDisplayedLabel;
    @FXML
    private Label totalElementsLabel;

    @FXML
    private Pagination vertexPagination;
    @FXML
    private Pagination edgePagination;

    @FXML
    private TextField vertexPageField;
    @FXML
    private TextField edgePageField;

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

    /**
     * Méthode d'initialisation automatique appelée par le chargeur FXML
     * après l'injection des composants annotés {@code @FXML}.
     *
     * <p>
     * Elle prépare les colonnes des tables, configure les fabriques de page
     * des paginations et attache les comportements de navigation aux champs
     * de texte.
     * </p>
     *
     * <p>
     * Cette méthode n'est pas destinée à être appelée manuellement.
     * </p>
     */
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
        if (bus == null)
            return;

        GraphEngine.GraphPage<Vertex> vPage = bus.dispatchSync(engine -> engine.getVerticesPage(0, 1));

        GraphEngine.GraphPage<Edge> ePage = bus.dispatchSync(engine -> engine.getEdgesPage(0, 1));

        int vertexTotal = vPage.getTotalSize();
        int edgeTotal = ePage.getTotalSize();

        int vertexPages = (int) Math.ceil((double) vertexTotal / ROWS_PER_PAGE);
        int edgePages = (int) Math.ceil((double) edgeTotal / ROWS_PER_PAGE);

        vertexPagination.setPageCount(Math.max(vertexPages, 1));
        edgePagination.setPageCount(Math.max(edgePages, 1));

        vertexPagination.setCurrentPageIndex(0);
        edgePagination.setCurrentPageIndex(0);

        vertexPageField.setText("1");
        edgePageField.setText("1");

        totalElementsLabel.setText(String.valueOf(vertexTotal + edgeTotal));

        nodesDisplayedLabel.setText(String.valueOf(vertexTotal));
        edgesDisplayedLabel.setText(String.valueOf(edgeTotal));
    }

    @SuppressWarnings("unchecked")
    private TableView<Vertex> createVertexPage(int pageIndex) {
        TableView<Vertex> table = new TableView<>();

        // Permet aux colonnes de s'étendre pour remplir la largeur
        table.setColumnResizePolicy(TableView.CONSTRAINED_RESIZE_POLICY_FLEX_LAST_COLUMN);

        table.setPrefWidth(Double.MAX_VALUE);
        table.setPrefHeight(Double.MAX_VALUE);

        table.getColumns().addAll(
                vertexIdCol,
                vertexCommunityCol,
                vertexDegreeCol,
                vertexXCol,
                vertexYCol,
                vertexDiameterCol,
                vertexDeletedCol);

        GraphEngine.GraphPage<Vertex> page = bus
                .dispatchSync(engine -> engine.getVerticesPage(pageIndex, ROWS_PER_PAGE));

        table.getItems().setAll(page.getData());

        return table;
    }

    @SuppressWarnings("unchecked")
    private TableView<Edge> createEdgePage(int pageIndex) {
        TableView<Edge> table = new TableView<>();

        // Permet aux colonnes de s'étendre pour remplir la largeur
        table.setColumnResizePolicy(TableView.CONSTRAINED_RESIZE_POLICY_FLEX_LAST_COLUMN);

        table.setPrefWidth(Double.MAX_VALUE);
        table.setPrefHeight(Double.MAX_VALUE);

        table.getColumns().addAll(
                edgeStartCol,
                edgeEndCol,
                edgeWeightCol);

        GraphEngine.GraphPage<Edge> page = bus.dispatchSync(engine -> engine.getEdgesPage(pageIndex, ROWS_PER_PAGE));

        table.getItems().setAll(page.getData());

        return table;
    }
}
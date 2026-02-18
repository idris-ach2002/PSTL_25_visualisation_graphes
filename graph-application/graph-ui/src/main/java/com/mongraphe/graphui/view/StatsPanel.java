package com.mongraphe.graphui.view;

import com.mongraphe.graphui.Edge;
import com.mongraphe.graphui.Vertex;
import com.mongraphe.graphui.app.CommandBus;
import com.mongraphe.graphui.rendering.GraphEngine;
import com.mongraphe.graphui.rendering.GraphEngine.GraphDataSnapshot;

import javafx.fxml.FXML;
import javafx.scene.control.Label;
import javafx.scene.control.TableView;

public final class StatsPanel {

    @FXML
    private Label nodesDisplayedLabel;
    @FXML
    private Label edgesDisplayedLabel;

    @FXML
    private TableView<Vertex> vertexTable;
    @FXML
    private TableView<Edge> edgeTable;

    private CommandBus<GraphEngine> engine;

    public void init(CommandBus<GraphEngine> engine) {
        this.engine = engine;
        refresh();
    }

    public void refresh() {
        if (engine == null) {
            return;
        }
        // Copier les données du modèle dans les éléments graphiques
        GraphDataSnapshot snapshot = engine.dispatchSync(e -> e.getDataSnapshot());

        nodesDisplayedLabel.setText(String.valueOf(snapshot.getVisibleVertexCount()));
        edgesDisplayedLabel.setText(String.valueOf(snapshot.getVisibleEdgeCount()));
        vertexTable.getItems().setAll(snapshot.getVertices());
        edgeTable.getItems().setAll(snapshot.getEdges());
    }
}

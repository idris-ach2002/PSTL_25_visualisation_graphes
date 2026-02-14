package com.mongraphe.graphui.view;

import com.mongraphe.graphui.Edge;
import com.mongraphe.graphui.Vertex;
import com.mongraphe.graphui.app.GraphEngineAdapter;
import com.mongraphe.graphui.app.GraphEngineAdapter.GraphDataSnapshot;
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

    private GraphEngineAdapter engine;

    public void init(GraphEngineAdapter engine) {
        this.engine = engine;
        refresh();
    }

    public void refresh() {
        if (engine == null) {
            return;
        }
        // Copier les données du modèle dans les éléments graphiques
        GraphDataSnapshot snapshot = engine.getDataSnapshot();

        nodesDisplayedLabel.setText(String.valueOf(snapshot.getVisibleVertexCount()));
        edgesDisplayedLabel.setText(String.valueOf(snapshot.getVisibleEdgeCount()));
        vertexTable.getItems().setAll(snapshot.getVertices());
        edgeTable.getItems().setAll(snapshot.getEdges());
    }
}

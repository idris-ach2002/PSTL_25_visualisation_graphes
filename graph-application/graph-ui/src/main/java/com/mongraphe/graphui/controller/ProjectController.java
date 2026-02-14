package com.mongraphe.graphui.controller;

import com.mongraphe.graphui.app.GraphEngineAdapter;
import com.mongraphe.graphui.app.GraphProject;
import com.mongraphe.graphui.app.InteractionService;

import javafx.fxml.FXML;
import javafx.stage.FileChooser;
import java.io.File;

public final class ProjectController {

    private GraphWorkspaceController workspace;
    private EngineOptionsController options;
    private StatsController stats;

    private File currentFile;
    
    public void setWorkspace(GraphWorkspaceController w) {
        this.workspace = w;
    }

    public void setOptions(EngineOptionsController o) {
        this.options = o;
    }

    public void setStats(StatsController s) {
        this.stats = s;
    }

    @FXML
    private void handleOpen() {
        FileChooser fc = new FileChooser();
        currentFile = fc.showOpenDialog(null);
        if (currentFile == null)
            return;

        /** TODO: gérer l'ouverture d'un projet
        GraphEngineAdapter adapter = new GraphEngineAdapter();

        adapter.load(currentFile, GraphProject.SourceType.CSV, null, null);

        InteractionService interaction = new InteractionService(adapter);

        workspace.init(adapter, interaction);

        options.setAdapter(adapter);

        GraphEngineAdapter.GraphDataSnapshot snapshot = adapter.getDataSnapshot();
        stats.refresh(snapshot.getVertices(), snapshot.getEdges());
        */
    }
}

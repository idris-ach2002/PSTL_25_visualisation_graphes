package com.mongraphe.graphui.controller;

import com.mongraphe.graphui.app.ApplicationContext;
import com.mongraphe.graphui.interfaces.CommandBusLinked;

import javafx.fxml.FXML;
import javafx.stage.FileChooser;
import java.io.File;

public final class ProjectController implements CommandBusLinked {

    private File currentFile;

    private ApplicationContext context;

    @Override
    public void setContext(ApplicationContext context) {
        this.context = context;
    }

    @FXML
    private void handleOpen() {
        FileChooser fc = new FileChooser();
        currentFile = fc.showOpenDialog(null);
        if (currentFile == null)
            return;

        /**
         * TODO: gérer l'ouverture d'un projet
         * GraphEngineAdapter adapter = new GraphEngineAdapter();
         * 
         * adapter.load(currentFile, GraphProject.SourceType.CSV, null, null);
         * 
         * InteractionService interaction = new InteractionService(adapter);
         * 
         * workspace.init(adapter, interaction);
         * 
         * options.setAdapter(adapter);
         * 
         * GraphEngineAdapter.GraphDataSnapshot snapshot = adapter.getDataSnapshot();
         * stats.refresh(snapshot.getVertices(), snapshot.getEdges());
         */
    }
}

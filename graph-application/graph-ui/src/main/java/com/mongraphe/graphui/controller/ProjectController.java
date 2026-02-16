package com.mongraphe.graphui.controller;

import com.mongraphe.graphui.app.CommandBus;
import com.mongraphe.graphui.interfaces.CommandBusLinked;
import com.mongraphe.graphui.rendering.GraphEngine;

import javafx.fxml.FXML;
import javafx.stage.FileChooser;
import java.io.File;

public final class ProjectController implements CommandBusLinked<GraphEngine> {

    private File currentFile;

    private CommandBus<GraphEngine> bus;

    @Override
    public void setBus(CommandBus<GraphEngine> bus) {
        this.bus = bus;
    }

    @FXML
    private void handleOpen() {
        FileChooser fc = new FileChooser();
        currentFile = fc.showOpenDialog(null);
        if (currentFile == null)
            return;

        // Regarder l'extension du fichier pour savoir comment le charger si c'est csv ou dot

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

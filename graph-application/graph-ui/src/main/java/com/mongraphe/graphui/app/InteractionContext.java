package com.mongraphe.graphui.app;

import com.mongraphe.graphui.interaction.UndoManager;

/** This class encapsulates the shared state of the application, such as the UI state and the interaction service. */
public class InteractionContext {
    
    private final UiState ui;
    private final GraphEngineAdapter graphAdapter;
    private final UndoManager undoManager = new UndoManager();

    public InteractionContext(UiState ui, GraphEngineAdapter graphAdapter) {
        this.ui = ui;
        this.graphAdapter = graphAdapter;
    }

    public UiState getUI() {
        return ui;
    }

    public GraphEngineAdapter getGraphAdapter() {
        return graphAdapter;
    }

    public UndoManager getUndoManager() {
        return undoManager;
    }
}

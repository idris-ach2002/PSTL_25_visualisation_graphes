package com.mongraphe.graphui.app;

import java.util.Objects;

import com.mongraphe.graphui.interaction.UndoManager;

public class InteractionContext {
    private final UiState ui;
    private final GraphService graphService;
    private final UndoManager undo;

    public InteractionContext(
            UiState ui,
            GraphService graphs,
            UndoManager undo) {

        this.ui = Objects.requireNonNull(ui);
        this.graphService = Objects.requireNonNull(graphs);
        this.undo = Objects.requireNonNull(undo);
    }

    public UiState ui() {
        return ui;
    }

    public GraphService graphService() {
        return graphService();
    }

    public UndoManager undo() {
        return undo;
    }
}

package com.mongraphe.graphui.interaction;

import java.util.Objects;

import com.mongraphe.graphui.app.CommandBus;
import com.mongraphe.graphui.app.UiState;
import com.mongraphe.graphui.interfaces.InteractionModeHandler;
import com.mongraphe.graphui.interaction.modes.DeleteModeHandler;
import com.mongraphe.graphui.interaction.modes.MoveModeHandler;
import com.mongraphe.graphui.interaction.modes.RunModeHandler;
import com.mongraphe.graphui.interaction.modes.SelectModeHandler;
import com.mongraphe.graphui.rendering.GraphEngine;

public final class InteractionService {

    public enum Mode {
        RUN,
        SELECT,
        MOVE,
        DELETE
    }

    private InteractionModeHandler current;
    private final CommandBus<GraphEngine> bus;
    private final UiState state;

    private final InteractionModeHandler selectHandler;
    private final InteractionModeHandler moveHandler;
    private final InteractionModeHandler deleteHandler;
    private final InteractionModeHandler runHandler;

    public InteractionService(CommandBus<GraphEngine> bus, UiState state) {
        Objects.requireNonNull(bus);
        Objects.requireNonNull(state);

        this.bus = bus;
        this.state = state;
        this.selectHandler = new SelectModeHandler(state);
        this.moveHandler = new MoveModeHandler(state);
        this.deleteHandler = new DeleteModeHandler(state);
        this.runHandler = new RunModeHandler(state);
        this.current = runHandler;
    }

    public Mode getMode() {
        if (current == selectHandler) return Mode.SELECT;
        if (current == moveHandler) return Mode.MOVE;
        if (current == deleteHandler) return Mode.DELETE;
        return Mode.RUN;
    }

    public void setMode(Mode mode) {
        Objects.requireNonNull(mode);
        switch (mode) {
            case SELECT -> { current = selectHandler; state.setStatus("Mode: sélection"); }
            case MOVE -> { current = moveHandler; state.setStatus("Mode: déplacement"); }
            case DELETE -> { current = deleteHandler; state.setStatus("Mode: suppression"); }
            case RUN -> { current = runHandler; state.setStatus("Mode: exécution"); }
        }
    }

    public void undo() { bus.undo(); }
    public void redo() { bus.redo(); }
    public boolean canUndo() { return bus.canUndo(); }
    public boolean canRedo() { return bus.canRedo(); }

    public void onMousePressed(int x, int y, int b) { current.onMousePressed(bus, x, y, b); }
    public void onMouseDragged(int x, int y, int b) { current.onMouseDragged(bus, x, y, b); }
    public void onMouseReleased(int x, int y, int b) { current.onMouseReleased(bus, x, y, b); }
    public void onMouseWheel(int x, int y, float a) { current.onMouseWheel(bus, x, y, a); }
    public void onKeyPressed(int key, boolean ctrl) { current.onKeyPressed(bus, key, ctrl); }
}

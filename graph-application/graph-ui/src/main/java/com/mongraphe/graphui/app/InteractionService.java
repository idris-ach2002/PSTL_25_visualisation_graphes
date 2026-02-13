package com.mongraphe.graphui.app;

import java.util.Objects;
import com.jogamp.newt.opengl.GLWindow;
import com.mongraphe.graphui.interaction.OpenGLInputHandler;
import com.mongraphe.graphui.interaction.UndoManager;
import com.mongraphe.graphui.interfaces.InteractionModeHandler;
import com.mongraphe.graphui.interaction.modes.*;

public final class InteractionService {

    public enum Mode {
        RUN,
        SELECT,
        MOVE,
        DELETE
    }

    private final InteractionContext context;
    private InteractionModeHandler current;

    private final InteractionModeHandler selectHandler;
    private final InteractionModeHandler moveHandler;
    private final InteractionModeHandler deleteHandler;
    private final InteractionModeHandler runHandler;

    public InteractionService(UiState ui, GraphService graphs) {

        UndoManager undo = new UndoManager();
        this.context = new InteractionContext(ui, graphs, undo);
        this.selectHandler = new SelectModeHandler();
        this.moveHandler = new MoveModeHandler();
        this.deleteHandler = new DeleteModeHandler();
        this.runHandler = new RunModeHandler();
        this.current = runHandler;
    }

    public void setMode(Mode mode) {
        Objects.requireNonNull(mode);

        switch (mode) {
            case SELECT -> current = selectHandler;
            case MOVE -> current = moveHandler;
            case DELETE -> current = deleteHandler;
            case RUN -> current = runHandler;
        }
    }

    public void attach(GLWindow window) {

        OpenGLInputHandler handler = new OpenGLInputHandler(this);

        window.addMouseListener(handler);
        window.addKeyListener(handler);
    }

    public void onMousePressed(int x, int y, int b) {
        current.onMousePressed(context, x, y, b);
    }

    public void onMouseDragged(int x, int y, int b) {
        current.onMouseDragged(context, x, y, b);
    }

    public void onMouseReleased(int x, int y, int b) {
        current.onMouseReleased(context, x, y, b);
    }

    public void onMouseWheel(int x, int y, float a) {
        current.onMouseWheel(context, x, y, a);
    }

    public void onKeyPressed(int key, boolean ctrl) {
        current.onKeyPressed(context, key, ctrl);
    }

    public void undo() {
        context.undo().undo();
    }

    public void redo() {
        context.undo().redo();
    }
}

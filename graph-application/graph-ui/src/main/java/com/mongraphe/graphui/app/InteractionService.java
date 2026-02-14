package com.mongraphe.graphui.app;

import java.util.Objects;
import com.jogamp.newt.opengl.GLWindow;
import com.mongraphe.graphui.interaction.OpenGLInputHandler;
import com.mongraphe.graphui.interfaces.InteractionModeHandler;
import com.mongraphe.graphui.rendering.Camera2D;
import com.mongraphe.graphui.rendering.GraphEngine;
import com.mongraphe.graphui.interaction.modes.*;

public final class InteractionService {

    public enum Mode {
        RUN,
        SELECT,
        MOVE,
        DELETE
    }

    private InteractionModeHandler current;
    private final InteractionContext ctx;

    private final InteractionModeHandler selectHandler;
    private final InteractionModeHandler moveHandler;
    private final InteractionModeHandler deleteHandler;
    private final InteractionModeHandler runHandler;

    public InteractionService(GraphEngine engine,
            Camera2D camera,
            GLWindow window) {

        GraphEngineAdapter adapter = new GraphEngineAdapter(engine, camera, window);

        this.ctx = new InteractionContext(new UiState(), adapter);

        this.selectHandler = new SelectModeHandler();
        this.moveHandler = new MoveModeHandler();
        this.deleteHandler = new DeleteModeHandler();
        this.runHandler = new RunModeHandler();

        this.current = runHandler;

        attach(window);
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

    private void attach(GLWindow window) {
        OpenGLInputHandler handler = new OpenGLInputHandler(this);
        window.addMouseListener(handler);
        window.addKeyListener(handler);
    }

    public void onMousePressed(int x, int y, int b) {
        current.onMousePressed(ctx, x, y, b);
    }

    public void onMouseDragged(int x, int y, int b) {
        current.onMouseDragged(ctx, x, y, b);
    }

    public void onMouseReleased(int x, int y, int b) {
        current.onMouseReleased(ctx, x, y, b);
    }

    public void onMouseWheel(int x, int y, float a) {
        current.onMouseWheel(ctx, x, y, a);
    }

    public void onKeyPressed(int key, boolean ctrl) {
        current.onKeyPressed(ctx, key, ctrl);
    }
}

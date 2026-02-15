package com.mongraphe.graphui.app;

import java.util.concurrent.Executor;

import com.mongraphe.graphui.interaction.UndoManager;
import com.mongraphe.graphui.interfaces.GraphCommand;
import com.mongraphe.graphui.interfaces.UndoableGraphCommand;

public final class CommandBus<C> {

    private final C context;
    private final Executor executor;

    public CommandBus(C context, Executor executor) {
        this.context = context;
        this.executor = executor;
    }

    public void dispatch(GraphCommand<C> command) {
        executor.execute(() -> command.execute(context));
    }

    public void dispatchUndoable(
            UndoableGraphCommand<C> command,
            UndoManager undoManager) {

        executor.execute(() -> {

            command.execute(context);

            undoManager.push(new UndoManager.EditAction() {

                @Override
                public void undo() {
                    executor.execute(() -> command.undo(context));
                }

                @Override
                public void redo() {
                    executor.execute(() -> command.execute(context));
                }
            });
        });
    }
}
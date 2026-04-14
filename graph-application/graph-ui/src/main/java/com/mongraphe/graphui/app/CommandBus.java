package com.mongraphe.graphui.app;

import java.util.concurrent.Executor;
import java.util.concurrent.FutureTask;
import java.util.function.Consumer;
import java.util.function.Function;

import com.mongraphe.graphui.interaction.UndoManager;
import com.mongraphe.graphui.interfaces.CommandBusI;
import com.mongraphe.graphui.interfaces.GraphCommand;
import com.mongraphe.graphui.interfaces.app.UndoableGraphCommand;

public final class CommandBus<C> implements CommandBusI<C> {

    private final C context;
    private final Executor executor;
    private final UndoManager undoManager = new UndoManager();

    public CommandBus(C context, Executor executor) {
        this.context = context;
        this.executor = executor;
    }

    public C getContext() {
        return context;
    }

    public void dispatch(GraphCommand<C> command) {
        executor.execute(() -> command.execute(context));
    }

    public void dispatchSyncVoid(Consumer<C> command) {
        FutureTask<Void> task = new FutureTask<>(() -> {
            command.accept(context);
            return null;
        });
        executor.execute(task);
        try {
            task.get();
        } catch (Exception e) {
            throw new RuntimeException(e);
        }
    }

    public <R> R dispatchSync(Function<C, R> query) {
        FutureTask<R> task = new FutureTask<>(() -> query.apply(context));
        executor.execute(task);
        try {
            return task.get();
        } catch (Exception e) {
            throw new RuntimeException(e);
        }
    }

    public void dispatchUndoable(UndoableGraphCommand<C> command) {
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

    public void undo() {
        undoManager.undo();
    }

    public void redo() {
        undoManager.redo();
    }

    public boolean canUndo() {
        return undoManager.canUndo();
    }

    public boolean canRedo() {
        return undoManager.canRedo();
    }
}

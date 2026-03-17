package com.mongraphe.graphui.interaction;

import java.util.ArrayDeque;

public final class UndoManager {

    private final ArrayDeque<EditAction> undoStack = new ArrayDeque<>();
    private final ArrayDeque<EditAction> redoStack = new ArrayDeque<>();

    public synchronized void push(EditAction a) {
        undoStack.push(a);
        redoStack.clear();
    }

    public synchronized void undo() {
        if (undoStack.isEmpty()) return;
        EditAction a = undoStack.pop();
        a.undo();
        redoStack.push(a);
    }

    public synchronized void redo() {
        if (redoStack.isEmpty()) return;
        EditAction a = redoStack.pop();
        a.redo();
        undoStack.push(a);
    }

    public synchronized boolean canUndo() {
        return !undoStack.isEmpty();
    }

    public synchronized boolean canRedo() {
        return !redoStack.isEmpty();
    }

    public interface EditAction {
        void undo();
        void redo();
    }
}

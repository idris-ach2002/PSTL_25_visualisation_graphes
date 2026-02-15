package com.mongraphe.graphui.interfaces;

public interface UndoableGraphCommand<C>
        extends GraphCommand<C> {

    /**
     * Undoes the command with the given context.
     *
     * @param context the context to undo the command with
     */
    void undo(C context);
}

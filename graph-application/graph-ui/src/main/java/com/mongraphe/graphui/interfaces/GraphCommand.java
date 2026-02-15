package com.mongraphe.graphui.interfaces;

public interface GraphCommand<C> {

    /**
     * Executes the command with the given context.
     *
     * @param context the context to execute the command with
     */
    void execute(C context);
}
package com.mongraphe.graphui.app;

import java.util.ArrayList;
import java.util.Collections;
import java.util.List;

import com.mongraphe.graphui.interfaces.UndoableGraphCommand;

public final class CompositeUndoableCommand<C>
        implements UndoableGraphCommand<C> {

    private final List<UndoableGraphCommand<C>> commands;

    public CompositeUndoableCommand(List<UndoableGraphCommand<C>> commands) {
        this.commands = new ArrayList<>(commands);
    }

    @Override
    public void execute(C context) {
        for (UndoableGraphCommand<C> c : commands) {
            c.execute(context);
        }
    }

    @Override
    public void undo(C context) {
        List<UndoableGraphCommand<C>> reversed = new ArrayList<>(commands);
        Collections.reverse(reversed);

        for (UndoableGraphCommand<C> c : reversed) {
            c.undo(context);
        }
    }

    public boolean isEmpty() {
        return commands.isEmpty();
    }
}

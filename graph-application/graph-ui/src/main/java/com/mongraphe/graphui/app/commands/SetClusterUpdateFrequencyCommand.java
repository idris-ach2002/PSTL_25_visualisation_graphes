package com.mongraphe.graphui.app.commands;

import com.mongraphe.graphui.interfaces.GraphCommand;
import com.mongraphe.graphui.rendering.GraphEngine;

public final class SetClusterUpdateFrequencyCommand
        implements GraphCommand<GraphEngine> {

    private final int frequency;

    public SetClusterUpdateFrequencyCommand(int frequency) {
        this.frequency = frequency;
    }

    @Override
    public void execute(GraphEngine engine) {
        engine.setClusterUpdateFrequency(frequency);
    }
}
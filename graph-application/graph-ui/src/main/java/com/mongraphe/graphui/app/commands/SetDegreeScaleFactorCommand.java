package com.mongraphe.graphui.app.commands;

import com.mongraphe.graphui.interfaces.GraphCommand;
import com.mongraphe.graphui.rendering.GraphEngine;

public final class SetDegreeScaleFactorCommand
        implements GraphCommand<GraphEngine> {

    private final double factor;

    public SetDegreeScaleFactorCommand(double factor) {
        this.factor = factor;
    }

    @Override
    public void execute(GraphEngine engine) {
        engine.setDegreeScaleFactor(factor);
    }
}
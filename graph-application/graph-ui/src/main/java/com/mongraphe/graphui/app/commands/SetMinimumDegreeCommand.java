package com.mongraphe.graphui.app.commands;

import com.mongraphe.graphui.interfaces.GraphCommand;
import com.mongraphe.graphui.rendering.GraphEngine;

public class SetMinimumDegreeCommand implements GraphCommand<GraphEngine> {

    private final int degree;

    public SetMinimumDegreeCommand(int degree) {
        this.degree = degree;
    }

    @Override
    public void execute(GraphEngine engine) {
        engine.setMinimumDegree(degree);
    }
    
}
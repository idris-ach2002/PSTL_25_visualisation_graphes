package com.mongraphe.graphui.app.commands;

import com.mongraphe.graphui.interfaces.GraphCommand;
import com.mongraphe.graphui.rendering.GraphEngine;

public class SetAttractionCoefficientCommand implements GraphCommand<GraphEngine> {

    private final double coefficient;

    public SetAttractionCoefficientCommand(double coefficient) {
        this.coefficient = coefficient;
    }

    @Override
    public void execute(GraphEngine engine) {
        engine.setAttractionCoefficient(coefficient);
    }
    
}
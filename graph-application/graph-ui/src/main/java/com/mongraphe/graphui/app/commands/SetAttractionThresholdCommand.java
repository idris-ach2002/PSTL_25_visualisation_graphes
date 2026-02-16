package com.mongraphe.graphui.app.commands;

import com.mongraphe.graphui.interfaces.GraphCommand;
import com.mongraphe.graphui.rendering.GraphEngine;

public class SetAttractionThresholdCommand implements GraphCommand<GraphEngine> {

    private final double threshold;

    public SetAttractionThresholdCommand(double threshold) {
        this.threshold = threshold;
    }

    @Override
    public void execute(GraphEngine engine) {
        engine.setAttractionThreshold(threshold);
    }
    
}

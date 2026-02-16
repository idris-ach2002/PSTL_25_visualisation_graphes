package com.mongraphe.graphui.app.commands;

import com.mongraphe.graphui.interfaces.GraphCommand;
import com.mongraphe.graphui.rendering.GraphEngine;

public class SetNewAmortissementCommand implements GraphCommand<GraphEngine> {

    private final double amortissement;

    public SetNewAmortissementCommand(double amortissement) {
        this.amortissement = amortissement;
    }

    @Override
    public void execute(GraphEngine engine) {
        engine.setNewAmortissement(amortissement);
    }
    
}
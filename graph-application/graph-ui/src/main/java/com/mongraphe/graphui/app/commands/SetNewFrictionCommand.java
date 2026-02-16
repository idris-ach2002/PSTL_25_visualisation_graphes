package com.mongraphe.graphui.app.commands;

import com.mongraphe.graphui.interfaces.GraphCommand;
import com.mongraphe.graphui.rendering.GraphEngine;

public class SetNewFrictionCommand implements GraphCommand<GraphEngine> {

    private final double friction;

    public SetNewFrictionCommand(double friction) {
        this.friction = friction;
    }

    @Override
    public void execute(GraphEngine engine) {
        engine.setNewFriction(friction);
    }
    
}

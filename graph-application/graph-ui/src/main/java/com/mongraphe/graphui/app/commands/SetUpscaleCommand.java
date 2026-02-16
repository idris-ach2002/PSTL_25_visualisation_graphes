package com.mongraphe.graphui.app.commands;

import com.mongraphe.graphui.interfaces.GraphCommand;
import com.mongraphe.graphui.rendering.GraphEngine;

public class SetUpscaleCommand implements GraphCommand<GraphEngine> {

    private final int upscale;

    public SetUpscaleCommand(int upscale) {
        this.upscale = upscale;
    }

    @Override
    public void execute(GraphEngine engine) {
        engine.setUpscale(upscale);
    }
}
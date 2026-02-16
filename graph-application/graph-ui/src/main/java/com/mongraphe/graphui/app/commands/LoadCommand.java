package com.mongraphe.graphui.app.commands;

import com.mongraphe.graphui.interfaces.GraphCommand;
import com.mongraphe.graphui.rendering.GraphEngine;

public class LoadCommand implements GraphCommand<GraphEngine> {

    private final String path;

    public LoadCommand(String path) {
        this.path = path;
    }

    @Override
    public void execute(GraphEngine engine) {
        engine.load(path);
    }
}
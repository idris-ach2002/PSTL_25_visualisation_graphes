package com.mongraphe.graphui.app.commands;

import java.io.File;

import com.mongraphe.graphui.GraphData;
import com.mongraphe.graphui.app.GraphProject;
import com.mongraphe.graphui.interfaces.GraphCommand;
import com.mongraphe.graphui.rendering.GraphEngine;

public final class LoadGraphCommand
        implements GraphCommand<GraphEngine> {

    private final File file;
    private final GraphProject.SourceType type;
    private final Object sim;
    private final Object community;

    public LoadGraphCommand(File file,
            GraphProject.SourceType type,
            Object sim,
            Object community) {
        this.file = file;
        this.type = type;
        this.sim = sim;
        this.community = community;
    }

    @Override
    public void execute(GraphEngine engine) {
        switch (type) {
            case CSV ->
                engine.loadCsv(file.getAbsolutePath(),
                        (GraphData.SimilitudeMode) sim,
                        (GraphData.NodeCommunity) community);

            case DOT ->
                engine.loadDot(file.getAbsolutePath(),
                        (GraphData.NodeCommunity) community);
        }
    }
}
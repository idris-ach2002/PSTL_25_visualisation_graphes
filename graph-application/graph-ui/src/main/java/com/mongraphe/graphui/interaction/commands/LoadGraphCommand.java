package com.mongraphe.graphui.interaction.commands;

import java.io.File;
import com.mongraphe.graphui.interfaces.GraphCommand;
import com.mongraphe.graphui.model.GraphData;
import com.mongraphe.graphui.model.GraphProject;
import com.mongraphe.graphui.rendering.GraphEngine;

public final class LoadGraphCommand implements GraphCommand<GraphEngine> {

    private final File file;
    private final GraphProject.SourceType type;
    private final GraphData.SimilitudeMode sim;
    private final GraphData.NodeCommunity community;
    private final double edgeThreshold;
    private final double antiThreshold;

    public LoadGraphCommand(File file,
            GraphProject.SourceType type,
            GraphData.SimilitudeMode sim,
            GraphData.NodeCommunity community,
            double edgeThreshold,
            double antiThreshold) {
        this.file = file;
        this.type = type;
        this.sim = sim;
        this.community = community;
        this.edgeThreshold = edgeThreshold;
        this.antiThreshold = antiThreshold;
    }

    @Override
    public void execute(GraphEngine engine) {
        switch (type) {
            case CSV -> engine.loadCsv(sim, community,
                    edgeThreshold, antiThreshold);
            case DOT -> engine.loadDot(file.getAbsolutePath(), community);
        }
    }
}
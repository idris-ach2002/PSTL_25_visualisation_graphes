package com.mongraphe.graphui.interfaces;

import com.mongraphe.graphui.rendering.GraphRenderer;

public interface GraphExportI {
    void exportToPng(GraphRenderer renderer, int width, int height, String path);
}

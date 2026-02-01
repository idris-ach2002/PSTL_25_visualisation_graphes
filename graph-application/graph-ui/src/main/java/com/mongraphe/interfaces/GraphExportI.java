package com.mongraphe.interfaces;

import com.jogamp.opengl.GL4;
import com.mongraphe.graphui.rendering.GraphRenderer;

public interface GraphExportI {
    /**
     * Exports the current OpenGL rendering to a PNG file without disturbing the
     * current display state. Creates an offscreen framebuffer for rendering.
     * 
     * @param gl   the OpenGL context
     * @param path the file path where the PNG will be saved
     */
    public void exportToPng(GL4 gl,
            GraphRenderer renderer,
            int width,
            int height,
            String path);
}

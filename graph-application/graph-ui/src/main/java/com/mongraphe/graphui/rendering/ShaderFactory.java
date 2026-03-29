package com.mongraphe.graphui.rendering;

import com.jogamp.opengl.GL4;

public final class ShaderFactory {

    private ShaderFactory() {
    }

    public static GLShaderProgram createPointShader(GL4 gl) {
        return GLShaderProgram.createShaderProgram(
                gl,
                vertexShaderPoints(),
                fragmentShaderPoints());
    }

    public static GLShaderProgram createEdgeShader(GL4 gl) {
        return GLShaderProgram.createShaderProgram(
                gl,
                vertexShaderEdges(),
                fragmentShaderEdges());
    }

    // ====== SHADERS POINTS ======

    private static String vertexShaderPoints() {
        return """
                    #version 400 core
                    layout(location = 0) in vec2 position;
                    layout(location = 1) in float size;
                    layout(location = 2) in vec3 color;
                    layout(location = 3) in float visibility;

                    uniform mat4 u_transform;
                    uniform float u_zoom;   // zoom actuel de la caméra

                    out vec3 fragColor;
                    out float fragVisibility;

                    void main() {
                        gl_Position = u_transform * vec4(position, 0.0, 1.0);
                        // La taille en pixels = taille monde * zoom
                        gl_PointSize = size * u_zoom;
                        fragColor = color;
                        fragVisibility = visibility;
                    }
                """;
    }

    private static String fragmentShaderPoints() {
        return """
                    #version 400 core
                    in vec3 fragColor;
                    in float fragVisibility;

                    out vec4 color;

                    void main() {
                        if (fragVisibility == 0.0)
                            discard;

                        float d = length(gl_PointCoord - vec2(0.5));
                        if (d > 0.5)
                            discard;

                        color = vec4(fragColor, 1.0);
                    }
                """;
    }

    // ====== SHADERS EDGES ======

    private static String vertexShaderEdges() {
        return """
                    #version 400 core
                    layout(location = 0) in vec2 position;
                    layout(location = 1) in vec3 color;
                    layout(location = 2) in float size;
                    layout(location = 3) in float visibility;

                    uniform mat4 u_transform;

                    out vec3 fragColor;
                    out float fragVisibility;

                    void main() {
                        gl_Position = u_transform * vec4(position, 0.0, 1.0);
                        fragColor = color;
                        fragVisibility = visibility;
                    }
                """;
    }

    private static String fragmentShaderEdges() {
        return """
                    #version 400 core
                    in vec3 fragColor;
                    in float fragVisibility;

                    out vec4 color;

                    void main() {
                        if (fragVisibility == 0.0)
                            discard;

                        color = vec4(fragColor, 1.0);
                    }
                """;
    }
}

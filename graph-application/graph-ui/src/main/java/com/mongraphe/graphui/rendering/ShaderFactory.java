package com.mongraphe.graphui.rendering;

/**
 * Fabrique les programmes GLSL utilisés par le renderer LWJGL.
 *
 * Les positions dynamiques restent dans une texture-buffer. Les sommets lisent
 * cette texture avec gl_InstanceID, les arêtes lisent les deux extrémités depuis
 * leurs attributs d'identifiants. Les arêtes courbes sont produites par un
 * geometry shader afin d'éviter de reconstruire des segments côté Java.
 */
public final class ShaderFactory {

    private ShaderFactory() {
    }

    /**
     * Crée le shader des sommets rendus en quads instanciés.
     *
     * @return programme GLSL compilé et linké
     */
    public static GLShaderProgram createPointShader() {
        return GLShaderProgram.createShaderProgram(vertexShaderPoints(), fragmentShaderPoints());
    }

    /**
     * Crée le shader des arêtes droites.
     *
     * @return programme GLSL compilé et linké
     */
    public static GLShaderProgram createStraightEdgeShader() {
        return GLShaderProgram.createShaderProgram(vertexShaderStraightEdges(), fragmentShaderEdges());
    }

    /**
     * Crée le shader des arêtes courbes.
     *
     * @return programme GLSL compilé et linké
     */
    public static GLShaderProgram createCurvedEdgeShader() {
        return GLShaderProgram.createShaderProgram(
                vertexShaderCurvedEdges(), geometryShaderCurvedEdges(), fragmentShaderEdges());
    }

    /** Compatibilité avec l'ancien nom utilisé par quelques appels. */
    public static GLShaderProgram createEdgeShader() {
        return createStraightEdgeShader();
    }

    private static String vertexShaderPoints() {
        return """
                    #version 410 core
                    layout(location = 0) in vec2 quadCorner;
                    layout(location = 1) in float diameter;
                    layout(location = 2) in vec3 inColor;
                    layout(location = 3) in float visibility;

                    uniform samplerBuffer u_positions;
                    uniform mat4 u_transform;
                    uniform vec2 u_viewport;
                    uniform float u_zoom;

                    out vec2 localCoord;
                    out vec3 fragColor;
                    out float fragVisibility;

                    void main() {
                        vec2 center = texelFetch(u_positions, gl_InstanceID).xy;
                        vec4 clipCenter = u_transform * vec4(center, 0.0, 1.0);

                        float zoomScale = clamp(sqrt(max(u_zoom, 0.02)) * 0.85, 0.16, 1.0);
                        float safeDiameter = clamp(diameter * zoomScale, 1.15, 12.0);
                        vec2 pixelOffset = quadCorner * safeDiameter;
                        vec2 clipOffset = vec2(
                            2.0 * pixelOffset.x / max(u_viewport.x, 1.0),
                            2.0 * pixelOffset.y / max(u_viewport.y, 1.0)
                        );

                        gl_Position = clipCenter + vec4(clipOffset * clipCenter.w, 0.0, 0.0);
                        localCoord = quadCorner * 2.0;
                        fragColor = inColor;
                        fragVisibility = visibility;
                    }
                """;
    }

    private static String fragmentShaderPoints() {
        return """
                    #version 410 core
                    in vec2 localCoord;
                    in vec3 fragColor;
                    in float fragVisibility;

                    out vec4 outColor;

                    void main() {
                        if (fragVisibility <= 0.0) discard;
                        if (dot(localCoord, localCoord) > 1.0) discard;
                        outColor = vec4(fragColor, 1.0);
                    }
                """;
    }

    private static String vertexShaderStraightEdges() {
        return """
                    #version 410 core
                    layout(location = 0) in int vertexId;
                    layout(location = 1) in vec3 inColor;
                    layout(location = 2) in float visibility;

                    uniform samplerBuffer u_positions;
                    uniform mat4 u_transform;

                    out vec3 edgeColor;
                    out float edgeVisibility;

                    void main() {
                        vec2 position = texelFetch(u_positions, vertexId).xy;
                        gl_Position = u_transform * vec4(position, 0.0, 1.0);
                        edgeColor = inColor;
                        edgeVisibility = visibility;
                    }
                """;
    }

    private static String vertexShaderCurvedEdges() {
        return """
                    #version 410 core
                    layout(location = 0) in int vertexId;
                    layout(location = 1) in vec3 inColor;
                    layout(location = 2) in float visibility;

                    uniform samplerBuffer u_positions;

                    out vec3 vEdgeColor;
                    out float vEdgeVisibility;
                    out vec2 edgeWorldPosition;

                    void main() {
                        edgeWorldPosition = texelFetch(u_positions, vertexId).xy;
                        gl_Position = vec4(edgeWorldPosition, 0.0, 1.0);
                        vEdgeColor = inColor;
                        vEdgeVisibility = visibility;
                    }
                """;
    }

    private static String geometryShaderCurvedEdges() {
        return """
                    #version 410 core
                    layout(lines) in;
                    layout(line_strip, max_vertices = 34) out;

                    uniform mat4 u_transform;
                    uniform int u_curveSegments;
                    uniform float u_curveAngleRadians;
                    uniform float u_maxCurveOffset;
                    uniform int u_alternateCurveDirection;

                    in vec3 vEdgeColor[];
                    in float vEdgeVisibility[];
                    in vec2 edgeWorldPosition[];

                    out vec3 edgeColor;
                    out float edgeVisibility;

                    float stableSign(vec2 a, vec2 b) {
                        float h = sin(dot(a + b * 1.37, vec2(12.9898, 78.233))) * 43758.5453;
                        return fract(h) < 0.5 ? -1.0 : 1.0;
                    }

                    void main() {
                        vec2 p0 = edgeWorldPosition[0];
                        vec2 p1 = edgeWorldPosition[1];
                        vec2 dir = p1 - p0;
                        float len = length(dir);
                        if (len < 1e-6) {
                            return;
                        }

                        int segments = clamp(u_curveSegments, 2, 32);
                        vec2 normal = vec2(-dir.y, dir.x) / len;
                        float side = u_alternateCurveDirection != 0 ? stableSign(p0, p1) : 1.0;
                        float offset = min(abs(tan(u_curveAngleRadians)) * len * 0.25, u_maxCurveOffset) * side;
                        vec2 control = (p0 + p1) * 0.5 + normal * offset;

                        vec3 color = (vEdgeColor[0] + vEdgeColor[1]) * 0.5;
                        float visibility = min(vEdgeVisibility[0], vEdgeVisibility[1]);

                        for (int i = 0; i <= segments; ++i) {
                            float t = float(i) / float(segments);
                            float u = 1.0 - t;
                            vec2 p = u * u * p0 + 2.0 * u * t * control + t * t * p1;
                            gl_Position = u_transform * vec4(p, 0.0, 1.0);
                            edgeColor = color;
                            edgeVisibility = visibility;
                            EmitVertex();
                        }
                        EndPrimitive();
                    }
                """;
    }

    private static String fragmentShaderEdges() {
        return """
                    #version 410 core
                    in vec3 edgeColor;
                    in float edgeVisibility;

                    out vec4 outColor;

                    void main() {
                        if (edgeVisibility <= 0.0) discard;
                        outColor = vec4(edgeColor, 1.0);
                    }
                """;
    }
}

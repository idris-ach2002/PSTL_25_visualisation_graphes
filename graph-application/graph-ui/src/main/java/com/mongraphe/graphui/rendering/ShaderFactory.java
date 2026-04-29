package com.mongraphe.graphui.rendering;

import com.jogamp.opengl.GL4;

/**
 * Fabrique de programmes de shaders GLSL (OpenGL Shading Language).
 * *
 * <p>
 * Cette classe contient les sources des shaders (Vertex et Fragment) sous forme
 * de chaînes
 * de caractères et gère leur compilation via {@link GLShaderProgram}.
 * </p>
 * *
 * <h2>Pipeline de Rendu</h2>
 * <ul>
 * <li><b>Vertex Shader :</b> Projette les coordonnées 2D du graphe dans
 * l'espace de rendu
 * en appliquant la matrice de transformation de la caméra.</li>
 * <li><b>Fragment Shader :</b> Détermine la couleur finale de chaque pixel.
 * Pour les sommets,
 * il transforme les carrés par défaut en cercles parfaits.</li>
 * </ul>
 */
public final class ShaderFactory {

    private ShaderFactory() {
    }

    /**
     * Crée le programme de rendu pour les sommets.
     */
    public static GLShaderProgram createPointShader(GL4 gl) {
        return GLShaderProgram.createShaderProgram(
                gl,
                vertexShaderPoints(),
                fragmentShaderPoints());
    }

    /**
     * Crée le programme de rendu pour les arêtes.
     */
    public static GLShaderProgram createEdgeShader(GL4 gl) {
        return GLShaderProgram.createShaderProgram(
                gl,
                vertexShaderEdges(),
                fragmentShaderEdges());
    }

    // ====== SHADERS POINTS ======

    /**
     * Vertex Shader pour les points.
     * Reçoit la position, la taille, la couleur et la visibilité.
     */
    private static String vertexShaderPoints() {
        return """
                    #version 400 core
                    layout(location = 0) in vec2 position;
                    layout(location = 1) in float size;
                    layout(location = 2) in vec3 color;
                    layout(location = 3) in float visibility;

                    uniform mat4 u_transform;

                    out vec3 fragColor;
                    out float fragVisibility;

                    void main() {
                        gl_Position = u_transform * vec4(position, 0.0, 1.0);
                        gl_PointSize = size;
                        fragColor = color;
                        fragVisibility = visibility;
                    }
                """;
    }

    /**
     * Fragment Shader pour les points.
     * <p>
     * Utilise {@code gl_PointCoord} pour calculer la distance par rapport au centre
     * du point.
     * Si la distance est supérieure à 0.5, le fragment est rejeté, créant ainsi un
     * disque.
     * </p>
     */
    private static String fragmentShaderPoints() {
        return """
                    #version 400 core
                    in vec3 fragColor;
                    in float fragVisibility;

                    out vec4 color;

                    void main() {
                        // Gestion du filtrage de visibilité
                        if (fragVisibility == 0.0)
                            discard;

                        // Calcul pour transformer le carré OpenGL en cercle
                        float d = length(gl_PointCoord - vec2(0.5));
                        if (d > 0.5)
                            discard;

                        color = vec4(fragColor, 1.0);
                    }
                """;
    }

    // ====== SHADERS EDGES ======

    /**
     * Vertex Shader pour les arêtes.
     */
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

    /**
     * Fragment Shader pour les arêtes.
     * Simple rendu de couleur avec test de visibilité.
     */
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
package com.mongraphe.graphui.rendering;

import java.nio.FloatBuffer;
import java.util.HashMap;
import java.util.Map;

import static org.lwjgl.opengl.GL20.*;

/**
 * Programme shader OpenGL manipulé directement via LWJGL.
 *
 * <p>La classe encapsule la compilation, l'édition des uniformes et la
 * destruction du programme. Les emplacements d'uniformes sont mis en cache afin
 * d'éviter des appels répétés à {@code glGetUniformLocation} pendant la boucle
 * de rendu.</p>
 */
public final class GLShaderProgram {

    private final int programId;
    private final Map<String, Integer> uniformLocationCache = new HashMap<>();

    /**
     * Construit un wrapper autour d'un programme OpenGL déjà lié.
     *
     * @param programId identifiant OpenGL du programme
     */
    public GLShaderProgram(int programId) {
        this.programId = programId;
    }

    /**
     * Compile deux shaders GLSL et les lie dans un programme OpenGL.
     *
     * @param vertexSrc source GLSL du vertex shader
     * @param fragmentSrc source GLSL du fragment shader
     * @return programme shader prêt à être utilisé
     */
    public static GLShaderProgram createShaderProgram(String vertexSrc, String fragmentSrc) {
        int vertexShader = compileShader(GL_VERTEX_SHADER, vertexSrc, "Vertex");
        int fragmentShader = compileShader(GL_FRAGMENT_SHADER, fragmentSrc, "Fragment");

        int program = glCreateProgram();
        glAttachShader(program, vertexShader);
        glAttachShader(program, fragmentShader);
        glLinkProgram(program);

        if (glGetProgrami(program, GL_LINK_STATUS) == 0) {
            String info = glGetProgramInfoLog(program);
            glDeleteShader(vertexShader);
            glDeleteShader(fragmentShader);
            glDeleteProgram(program);
            throw new RuntimeException("Shader program linking failed: " + info);
        }

        glDetachShader(program, vertexShader);
        glDetachShader(program, fragmentShader);
        glDeleteShader(vertexShader);
        glDeleteShader(fragmentShader);

        return new GLShaderProgram(program);
    }

    /**
     * Compile un vertex shader, un geometry shader et un fragment shader.
     *
     * @param vertexSrc source du vertex shader
     * @param geometrySrc source du geometry shader
     * @param fragmentSrc source du fragment shader
     * @return programme shader prêt à être utilisé
     */
    public static GLShaderProgram createShaderProgram(String vertexSrc, String geometrySrc, String fragmentSrc) {
        int vertexShader = compileShader(GL_VERTEX_SHADER, vertexSrc, "Vertex");
        int geometryShader = compileShader(org.lwjgl.opengl.GL32.GL_GEOMETRY_SHADER, geometrySrc, "Geometry");
        int fragmentShader = compileShader(GL_FRAGMENT_SHADER, fragmentSrc, "Fragment");

        int program = glCreateProgram();
        glAttachShader(program, vertexShader);
        glAttachShader(program, geometryShader);
        glAttachShader(program, fragmentShader);
        glLinkProgram(program);

        if (glGetProgrami(program, GL_LINK_STATUS) == 0) {
            String info = glGetProgramInfoLog(program);
            glDeleteShader(vertexShader);
            glDeleteShader(geometryShader);
            glDeleteShader(fragmentShader);
            glDeleteProgram(program);
            throw new RuntimeException("Shader program linking failed: " + info);
        }

        glDetachShader(program, vertexShader);
        glDetachShader(program, geometryShader);
        glDetachShader(program, fragmentShader);
        glDeleteShader(vertexShader);
        glDeleteShader(geometryShader);
        glDeleteShader(fragmentShader);
        return new GLShaderProgram(program);
    }

    private static int compileShader(int type, String source, String label) {
        int shader = glCreateShader(type);
        glShaderSource(shader, source);
        glCompileShader(shader);
        if (glGetShaderi(shader, GL_COMPILE_STATUS) == 0) {
            String info = glGetShaderInfoLog(shader);
            glDeleteShader(shader);
            throw new RuntimeException(label + " shader compilation failed: " + info);
        }
        return shader;
    }
    /** Active ce programme pour les appels de rendu suivants. */
    public void use() {
        glUseProgram(programId);
    }

    /**
     * Retourne l'identifiant OpenGL brut du programme.
     *
     * @return identifiant du programme
     */
    public int id() {
        return programId;
    }

    /**
     * Écrit une matrice 4x4 dans un uniforme GLSL.
     *
     * @param name nom de l'uniforme
     * @param matrix matrice column-major stockée dans un {@link FloatBuffer}
     */
    public void setMat4(String name, FloatBuffer matrix) {
        int loc = uniformLocation(name);
        if (loc >= 0) {
            FloatBuffer copy = matrix.duplicate();
            copy.rewind();
            glUniformMatrix4fv(loc, false, copy);
        }
    }

    /**
     * Écrit un vecteur 2D dans un uniforme GLSL.
     *
     * @param name nom de l'uniforme
     * @param x première composante
     * @param y seconde composante
     */
    public void setVec2(String name, float x, float y) {
        int loc = uniformLocation(name);
        if (loc >= 0) {
            glUniform2f(loc, x, y);
        }
    }

    /**
     * Écrit un entier dans un uniforme GLSL.
     *
     *  name nom de l.uniforme
     *  value valeur entière à écrire
     */
    public void setInt(String name, int value) {
        int loc = uniformLocation(name);
        if (loc >= 0) {
            glUniform1i(loc, value);
        }
    }


    /**
     * Écrit un flottant dans un uniforme GLSL.
     *
     * @param name nom de l'uniforme
     * @param value valeur à écrire
     */
    public void setFloat(String name, float value) {
        int loc = uniformLocation(name);
        if (loc >= 0) {
            glUniform1f(loc, value);
        }
    }

    /**
     * Retourne l'emplacement d'un uniforme en utilisant un cache local.
     *
     * @param name nom de l'uniforme GLSL
     * @return emplacement OpenGL, ou {@code -1} si l'uniforme est absent
     */
    private int uniformLocation(String name) {
        Integer cached = uniformLocationCache.get(name);
        if (cached != null) {
            return cached;
        }
        int loc = glGetUniformLocation(programId, name);
        uniformLocationCache.put(name, loc);
        return loc;
    }

    /** Libère le programme shader côté GPU. */
    public void delete() {
        if (programId != 0) {
            glDeleteProgram(programId);
        }
        uniformLocationCache.clear();
    }
}

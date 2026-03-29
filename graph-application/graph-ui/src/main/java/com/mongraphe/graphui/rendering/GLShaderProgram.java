package com.mongraphe.graphui.rendering;

import com.jogamp.opengl.GL4;

import java.nio.FloatBuffer;

public class GLShaderProgram {

    private final int programId;

    public GLShaderProgram(int programId) {
        this.programId = programId;
    }

    public void setFloat(GL4 gl, String name, float value) {
        int loc = gl.glGetUniformLocation(programId, name);
        gl.glUniform1f(loc, value);
    }

    public static GLShaderProgram createShaderProgram(GL4 gl, String vertexSrc, String fragmentSrc) {
        int vertexShader = gl.glCreateShader(GL4.GL_VERTEX_SHADER);
        int fragmentShader = gl.glCreateShader(GL4.GL_FRAGMENT_SHADER);

        // Charger le code source
        gl.glShaderSource(vertexShader, 1, new String[] { vertexSrc }, null);
        gl.glShaderSource(fragmentShader, 1, new String[] { fragmentSrc }, null);

        // Compiler les shaders
        gl.glCompileShader(vertexShader);
        gl.glCompileShader(fragmentShader);

        // Vérifier compilation vertex
        int[] compiled = new int[1];
        gl.glGetShaderiv(vertexShader, GL4.GL_COMPILE_STATUS, compiled, 0);
        if (compiled[0] == 0) {
            byte[] info = new byte[1024];
            gl.glGetShaderInfoLog(vertexShader, info.length, null, 0, info, 0);
            throw new RuntimeException("Vertex shader compilation failed: " + new String(info));
        }

        // Vérifier compilation fragment
        gl.glGetShaderiv(fragmentShader, GL4.GL_COMPILE_STATUS, compiled, 0);
        if (compiled[0] == 0) {
            byte[] info = new byte[1024];
            gl.glGetShaderInfoLog(fragmentShader, info.length, null, 0, info, 0);
            throw new RuntimeException("Fragment shader compilation failed: " + new String(info));
        }

        // Créer le programme
        int program = gl.glCreateProgram();
        gl.glAttachShader(program, vertexShader);
        gl.glAttachShader(program, fragmentShader);
        gl.glLinkProgram(program);

        // Vérifier le lien
        int[] linked = new int[1];
        gl.glGetProgramiv(program, GL4.GL_LINK_STATUS, linked, 0);
        if (linked[0] == 0) {
            byte[] info = new byte[1024];
            gl.glGetProgramInfoLog(program, info.length, null, 0, info, 0);
            throw new RuntimeException("Shader program linking failed: " + new String(info));
        }

        // Supprimer les shaders attachés après le lien
        gl.glDeleteShader(vertexShader);
        gl.glDeleteShader(fragmentShader);

        return new GLShaderProgram(program);
    }

    public void use(GL4 gl) {
        gl.glUseProgram(programId);
    }

    public int id() {
        return programId;
    }

    public void setMat4(GL4 gl, String name, FloatBuffer matrix) {
        int loc = gl.glGetUniformLocation(programId, name);
        gl.glUniformMatrix4fv(loc, 1, false, matrix);
    }

    public void delete(GL4 gl) {
        gl.glDeleteProgram(programId);
    }
}

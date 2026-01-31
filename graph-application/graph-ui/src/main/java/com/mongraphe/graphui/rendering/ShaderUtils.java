package com.mongraphe.graphui.rendering;

import java.nio.IntBuffer;

import com.jogamp.opengl.GL4;

public class ShaderUtils {

    public int createShaderProgram(GL4 gl, String vertexSource, String fragmentSource) {
        // Compiler les shaders
        int vertexShader = compileShader(gl, GL4.GL_VERTEX_SHADER, vertexSource);
        int fragmentShader = compileShader(gl, GL4.GL_FRAGMENT_SHADER, fragmentSource);

        // Créer un programme shader
        int program = gl.glCreateProgram();
        gl.glAttachShader(program, vertexShader);
        gl.glAttachShader(program, fragmentShader);
        gl.glLinkProgram(program);

        // Vérifier si le programme a bien été lié
        IntBuffer linkStatus = IntBuffer.allocate(1);
        gl.glGetProgramiv(program, GL4.GL_LINK_STATUS, linkStatus);
        if (linkStatus.get(0) != GL4.GL_TRUE) {
            System.err.println("Erreur de liaison du programme de shaders.");
        }

        return program;
    }

    public int compileShader(GL4 gl, int type, String source) {
        int shader = gl.glCreateShader(type);
        gl.glShaderSource(shader, 1, new String[] { source }, null);
        gl.glCompileShader(shader);

        // Vérification de la compilation
        IntBuffer compiled = IntBuffer.allocate(1);
        gl.glGetShaderiv(shader, GL4.GL_COMPILE_STATUS, compiled);
        if (compiled.get(0) != GL4.GL_TRUE) {
            System.err.println("Erreur de compilation du shader");
            System.err.println(getShaderInfoLog(gl, shader));
        }

        return shader;
    }

    public String getShaderInfoLog(GL4 gl, int shader) {
        IntBuffer logLength = IntBuffer.allocate(1);
        gl.glGetShaderiv(shader, GL4.GL_INFO_LOG_LENGTH, logLength);
        byte[] log = new byte[logLength.get(0)];
        gl.glGetShaderInfoLog(shader, logLength.get(0), null, 0, log, 0);
        return new String(log);
    }

}

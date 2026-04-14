package com.mongraphe.graphui.rendering;

import com.jogamp.opengl.GL4;

import java.nio.FloatBuffer;

/**
 * Représente un programme de shader OpenGL.
 *
 * <p>
 * Cette classe encapsule la création, la compilation et l'utilisation
 * d'un programme de shaders OpenGL dans l'environnement JOGL.
 * Un programme de shader est constitué généralement d'un shader
 * de sommet (vertex shader) et d'un shader de fragment (fragment shader).
 * </p>
 *
 * <p>
 * Elle fournit des méthodes utilitaires permettant :
 * </p>
 *
 * <ul>
 * <li>de compiler les shaders GLSL</li>
 * <li>de lier les shaders dans un programme GPU</li>
 * <li>d'activer le programme dans le pipeline OpenGL</li>
 * <li>d'envoyer des uniforms (ex: matrices)</li>
 * <li>de libérer les ressources GPU associées</li>
 * </ul>
 *
 * <h2>Cycle de vie typique</h2>
 *
 * <pre>
 * GLShaderProgram program =
 *      GLShaderProgram.createShaderProgram(gl, vertexSrc, fragmentSrc);
 *
 * program.use(gl);
 * program.setMat4(gl, "uProjection", matrixBuffer);
 *
 * ...
 *
 * program.delete(gl);
 * </pre>
 *
 * <p>
 * Les erreurs de compilation ou de liaison sont détectées et provoquent
 * une {@link RuntimeException} contenant le log GLSL généré par OpenGL.
 * </p>
 */
public class GLShaderProgram {

    /** Identifiant OpenGL du programme shader. */
    private final int programId;

    /**
     * Construit un objet représentant un programme shader déjà créé.
     *
     * @param programId identifiant OpenGL du programme
     */
    public GLShaderProgram(int programId) {
        this.programId = programId;
    }

    /**
     * Compile les shaders GLSL et crée un programme shader OpenGL.
     *
     * <p>
     * La méthode effectue les étapes suivantes :
     * </p>
     *
     * <ol>
     * <li>création des shaders vertex et fragment</li>
     * <li>chargement du code source GLSL</li>
     * <li>compilation des shaders</li>
     * <li>vérification des erreurs de compilation</li>
     * <li>création du programme OpenGL</li>
     * <li>attachement des shaders</li>
     * <li>édition des liens (link)</li>
     * <li>vérification des erreurs de liaison</li>
     * <li>suppression des shaders intermédiaires</li>
     * </ol>
     *
     * @param gl          contexte OpenGL actif
     * @param vertexSrc   code source GLSL du vertex shader
     * @param fragmentSrc code source GLSL du fragment shader
     * @return programme shader prêt à être utilisé
     *
     * @throws RuntimeException si la compilation ou la liaison échoue
     */
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

    /**
     * Active ce programme shader dans le pipeline OpenGL.
     *
     * <p>
     * Une fois activé, toutes les opérations de rendu suivantes
     * utiliseront ce programme shader jusqu'à ce qu'un autre programme
     * soit activé.
     * </p>
     *
     * @param gl contexte OpenGL actif
     */
    public void use(GL4 gl) {
        gl.glUseProgram(programId);
    }

    /**
     * Retourne l'identifiant OpenGL du programme shader.
     *
     * @return identifiant GPU du programme
     */
    public int id() {
        return programId;
    }

    /**
     * Envoie une matrice 4x4 vers une variable uniforme du shader.
     *
     * <p>
     * Cette méthode est généralement utilisée pour transmettre
     * des matrices de transformation telles que :
     * </p>
     *
     * <ul>
     * <li>matrice modèle</li>
     * <li>matrice vue</li>
     * <li>matrice projection</li>
     * </ul>
     *
     * @param gl     contexte OpenGL actif
     * @param name   nom de la variable uniforme dans le shader GLSL
     * @param matrix buffer contenant les 16 valeurs de la matrice
     */
    public void setMat4(GL4 gl, String name, FloatBuffer matrix) {
        int loc = gl.glGetUniformLocation(programId, name);
        gl.glUniformMatrix4fv(loc, 1, false, matrix);
    }

    /**
     * Libère les ressources GPU associées au programme shader.
     *
     * <p>
     * Cette méthode doit être appelée lorsque le programme
     * n'est plus utilisé afin d'éviter les fuites mémoire GPU.
     * </p>
     *
     * @param gl contexte OpenGL actif
     */
    public void delete(GL4 gl) {
        gl.glDeleteProgram(programId);
    }
}
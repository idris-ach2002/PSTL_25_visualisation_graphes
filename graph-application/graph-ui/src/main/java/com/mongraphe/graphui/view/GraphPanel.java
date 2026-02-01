package com.mongraphe.graphui.view;

import com.jogamp.opengl.*;
import com.jogamp.opengl.awt.GLJPanel;
import com.jogamp.opengl.util.FPSAnimator;
import com.mongraphe.graphui.rendering.*;

public class GraphPanel extends GLJPanel {

    private final GraphEngine engine;
    private final VertexGpuBuffer vertexBuffer;
    private final EdgeGpuBuffer edgeBuffer;
    private final Camera2D camera;
    private final GraphRenderer renderer;
    private GLShaderProgram pointsShader;
    private GLShaderProgram edgesShader;

    public GraphPanel(GraphEngine engine) {
        super(new GLCapabilities(GLProfile.get(GLProfile.GL4)));

        this.engine = engine;
        this.vertexBuffer = new VertexGpuBuffer();
        this.edgeBuffer = new EdgeGpuBuffer();
        this.camera = new Camera2D();

        // Créer un GLEventListener temporaire pour compiler les shaders avant de créer
        // le renderer
        this.addGLEventListener(new GLEventListener() {
            @Override
            public void init(GLAutoDrawable drawable) {
                GL4 gl = drawable.getGL().getGL4();

                pointsShader = GLShaderProgram.createShaderProgram(gl, vertexShaderPoints(), fragmentShaderPoints());
                edgesShader = GLShaderProgram.createShaderProgram(gl, vertexShaderEdges(), fragmentShaderEdges());

                rendererInit(gl);
            }

            @Override
            public void dispose(GLAutoDrawable drawable) {
            }

            @Override
            public void display(GLAutoDrawable drawable) {
            }

            @Override
            public void reshape(GLAutoDrawable drawable, int x, int y, int w, int h) {
            }
        });

        this.renderer = new GraphRenderer(engine, vertexBuffer, edgeBuffer, pointsShader, edgesShader, camera);
        this.addGLEventListener(renderer);

        FPSAnimator animator = new FPSAnimator(this, 60, true);
        animator.start();
    }

    private void rendererInit(GL4 gl) {
        gl.glEnable(GL4.GL_BLEND);
        gl.glBlendFunc(GL4.GL_SRC_ALPHA, GL4.GL_ONE_MINUS_SRC_ALPHA);
        gl.glEnable(GL4.GL_PROGRAM_POINT_SIZE);
        gl.glEnable(GL4.GL_DEPTH_TEST);

        vertexBuffer.init(gl);
        edgeBuffer.init(gl);
    }

    private String vertexShaderPoints() {
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

    private String fragmentShaderPoints() {
        return """
                    #version 400 core
                    in vec3 fragColor;
                    in float fragVisibility;
                    out vec4 color;
                    void main() {
                        if(fragVisibility==0.0) discard;
                        float dist = length(gl_PointCoord - vec2(0.5,0.5));
                        if(dist<0.5) color = vec4(fragColor,1.0);
                        else discard;
                    }
                """;
    }

    private String vertexShaderEdges() {
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
                        gl_Position = u_transform * vec4(position,0.0,1.0);
                        fragColor = color;
                        fragVisibility = visibility;
                    }
                """;
    }

    private String fragmentShaderEdges() {
        return """
                    #version 400 core
                    in vec3 fragColor;
                    in float fragVisibility;
                    out vec4 color;
                    void main() {
                        if(fragVisibility==0.0) discard;
                        color = vec4(fragColor,1.0);
                    }
                """;
    }

    public GraphEngine getEngine() {
        return engine;
    }

    public Camera2D getCamera() {
        return camera;
    }
}
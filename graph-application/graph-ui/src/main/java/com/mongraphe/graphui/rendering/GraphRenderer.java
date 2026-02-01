package com.mongraphe.graphui.rendering;

import com.jogamp.opengl.GL4;
import com.jogamp.opengl.GLAutoDrawable;
import com.jogamp.opengl.GLEventListener;

public class GraphRenderer implements GLEventListener {

    private final GraphEngine engine;
    private final VertexGpuBuffer vertexBuffer;
    private final EdgeGpuBuffer edgeBuffer;
    private final GLShaderProgram pointsShader;
    private final GLShaderProgram edgesShader;
    private final Camera2D camera;

    public GraphRenderer(GraphEngine engine,
            VertexGpuBuffer vertexBuffer,
            EdgeGpuBuffer edgeBuffer,
            GLShaderProgram pointsShader,
            GLShaderProgram edgesShader,
            Camera2D camera) {

        this.engine = engine;
        this.vertexBuffer = vertexBuffer;
        this.edgeBuffer = edgeBuffer;
        this.pointsShader = pointsShader;
        this.edgesShader = edgesShader;
        this.camera = camera;
    }
    
    @Override
    public void init(GLAutoDrawable drawable) {
        GL4 gl = drawable.getGL().getGL4();

        gl.glEnable(GL4.GL_BLEND);
        gl.glBlendFunc(GL4.GL_SRC_ALPHA, GL4.GL_ONE_MINUS_SRC_ALPHA);
        gl.glEnable(GL4.GL_PROGRAM_POINT_SIZE);
        gl.glEnable(GL4.GL_DEPTH_TEST);

        vertexBuffer.init(gl);
        edgeBuffer.init(gl);
    }

    @Override
    public void display(GLAutoDrawable drawable) {
        render(drawable.getGL().getGL4());
    }

    public void render(GL4 gl) {
        gl.glClear(GL4.GL_COLOR_BUFFER_BIT | GL4.GL_DEPTH_BUFFER_BIT);

        engine.update();
        GraphScene scene = engine.getScene();

        vertexBuffer.update(scene);
        edgeBuffer.update(scene);

        vertexBuffer.upload(gl);
        edgeBuffer.upload(gl);

        pointsShader.use(gl);
        pointsShader.setMat4(gl, "u_transform", camera.getProjection());
        vertexBuffer.draw(gl);

        edgesShader.use(gl);
        edgesShader.setMat4(gl, "u_transform", camera.getProjection());
        edgeBuffer.draw(gl);

        gl.glUseProgram(0);
    }

    @Override
    public void reshape(GLAutoDrawable d, int x, int y, int w, int h) {
        camera.resize(w, h);
    }

    @Override
    public void dispose(GLAutoDrawable drawable) {
    }
}

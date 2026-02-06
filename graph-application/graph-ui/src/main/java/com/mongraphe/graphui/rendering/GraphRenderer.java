package com.mongraphe.graphui.rendering;

import com.jogamp.opengl.GL4;
import com.jogamp.opengl.GLAutoDrawable;
import com.jogamp.opengl.GLEventListener;
import com.mongraphe.graphui.model.GraphModel;

public final class GraphRenderer implements GLEventListener {

    private final GraphEngine engine;
    private final Camera2D camera;

    private VertexGpuBuffer vertexBuffer;
    private EdgeGpuBuffer edgeBuffer;

    private GLShaderProgram pointShader;
    private GLShaderProgram edgeShader;

    public GraphRenderer(GraphEngine engine, Camera2D camera) {
        this.engine = engine;
        this.camera = camera;
    }

    @Override
    public void init(GLAutoDrawable drawable) {

        GL4 gl = drawable.getGL().getGL4();

        configureGL(gl);

        vertexBuffer = new VertexGpuBuffer();
        edgeBuffer = new EdgeGpuBuffer();

        vertexBuffer.init(gl);
        edgeBuffer.init(gl);

        pointShader = ShaderFactory.createPointShader(gl);
        edgeShader  = ShaderFactory.createEdgeShader(gl);
    }

    private void configureGL(GL4 gl) {
        gl.glEnable(GL4.GL_BLEND);
        gl.glBlendFunc(GL4.GL_SRC_ALPHA, GL4.GL_ONE_MINUS_SRC_ALPHA);
        gl.glEnable(GL4.GL_PROGRAM_POINT_SIZE);
    }

    @Override
    public void display(GLAutoDrawable drawable) {
        render(drawable.getGL().getGL4());
    }

    public void render(GL4 gl) {

        gl.glClear(GL4.GL_COLOR_BUFFER_BIT);

        engine.update();

        GraphModel model = engine.model();

        vertexBuffer.update(model);
        edgeBuffer.update(model);

        vertexBuffer.upload(gl);
        edgeBuffer.upload(gl);

        drawVertices(gl);
        drawEdges(gl);
    }

    private void drawVertices(GL4 gl) {
        pointShader.use(gl);
        pointShader.setMat4(gl, "u_transform", camera.getProjection());
        vertexBuffer.draw(gl);
    }

    private void drawEdges(GL4 gl) {
        edgeShader.use(gl);
        edgeShader.setMat4(gl, "u_transform", camera.getProjection());
        edgeBuffer.draw(gl);
    }

    @Override
    public void reshape(GLAutoDrawable d, int x, int y, int w, int h) {
        camera.resize(w, h);
    }

    @Override
    public void dispose(GLAutoDrawable drawable) {
        // TODO liber les ressources GPU
    }
}


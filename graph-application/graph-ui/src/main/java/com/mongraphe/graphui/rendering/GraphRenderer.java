package com.mongraphe.graphui.rendering;

import com.jogamp.opengl.GL4;
import com.jogamp.opengl.GLAutoDrawable;
import com.jogamp.opengl.GLEventListener;
import com.mongraphe.graphui.model.GraphModel;

/**
 * Renderer JOGL: dessine sommets/arêtes depuis le modèle Java.
 * La simulation est déclenchée via GraphEngine.update().
 */
public final class GraphRenderer implements GLEventListener {

    private final GraphEngine engine;

    private VertexGpuBuffer vertexBuffer;
    private EdgeGpuBuffer edgeBuffer;

    private GLShaderProgram pointShader;
    private GLShaderProgram edgeShader;

    public GraphRenderer(GraphEngine engine, Camera2D camera) {
        this.engine = engine;
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
        edgeShader = ShaderFactory.createEdgeShader(gl);
    }

    private void configureGL(GL4 gl) {
        gl.glEnable(GL4.GL_BLEND);
        gl.glBlendFunc(GL4.GL_SRC_ALPHA, GL4.GL_ONE_MINUS_SRC_ALPHA);
        gl.glEnable(GL4.GL_PROGRAM_POINT_SIZE);
        gl.glClearColor(engine.getBackgroundColorR(), engine.getBackgroundColorG(), engine.getBackgroundColorB(), engine.getBackgroundColorA());
    }

    @Override
    public void display(GLAutoDrawable drawable) {
        System.out.println("je suis la");
        render(drawable.getGL().getGL4());
    }
    
    public void render(GL4 gl) {

        gl.glClearColor(engine.getBackgroundColorR(), engine.getBackgroundColorG(), engine.getBackgroundColorB(), engine.getBackgroundColorA());
        gl.glClear(GL4.GL_COLOR_BUFFER_BIT);

        engine.update();

        GraphModel model = engine.model();

        synchronized (model.mutex()) {
            model.setZoom(engine.camera().getZoom());
            vertexBuffer.update(model);
            edgeBuffer.update(model);
        }

        vertexBuffer.upload(gl);
        edgeBuffer.upload(gl);

        drawEdges(gl);
        drawVertices(gl);
    }

    private void drawVertices(GL4 gl) {
        pointShader.use(gl);
        pointShader.setMat4(gl, "u_transform", engine.camera().getProjection());
        vertexBuffer.draw(gl);
    }

    private void drawEdges(GL4 gl) {
        edgeShader.use(gl);
        edgeShader.setMat4(gl, "u_transform", engine.camera().getProjection());
        edgeBuffer.draw(gl);
    }

    @Override
    public void reshape(GLAutoDrawable d, int x, int y, int w, int h) {
        engine.camera().resize(w, h);
    }

    @Override
    public void dispose(GLAutoDrawable drawable) {
        // Best effort: libère au moins les programmes shader
        try {
            GL4 gl = drawable.getGL().getGL4();
            if (pointShader != null)
                pointShader.delete(gl);
            if (edgeShader != null)
                edgeShader.delete(gl);
        } catch (Exception ignored) {
        }
    }
}

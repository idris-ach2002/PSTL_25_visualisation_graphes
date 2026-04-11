package com.mongraphe.graphui.rendering;

import com.jogamp.opengl.GL4;
import com.jogamp.opengl.GLAutoDrawable;
import com.jogamp.opengl.GLEventListener;
import com.mongraphe.graphui.model.GraphModel;

public final class GraphRenderer implements GLEventListener {

    private final GraphEngine engine;
    private VertexGpuBuffer vertexBuffer;
    private EdgeGpuBuffer edgeBuffer;
    private GLShaderProgram pointShader;
    private GLShaderProgram edgeShader;
    private volatile GraphRenderOptions renderOptions;

    public GraphRenderer(GraphEngine engine, Camera2D camera) {
        this(engine, camera, GraphRenderOptions.straight());
    }

    public GraphRenderer(GraphEngine engine, Camera2D camera, GraphRenderOptions renderOptions) {
        this.engine = engine;
        this.renderOptions = renderOptions == null ? GraphRenderOptions.straight() : renderOptions;
    }

    public void setRenderOptions(GraphRenderOptions renderOptions) {
        this.renderOptions = renderOptions == null ? GraphRenderOptions.straight() : renderOptions;
    }

    public GraphRenderOptions getRenderOptions() {
        return renderOptions;
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
        gl.glClearColor(engine.getBackgroundColorR(), engine.getBackgroundColorG(), engine.getBackgroundColorB(),
                engine.getBackgroundColorA());
    }

    @Override
    public void display(GLAutoDrawable drawable) {
        GL4 gl = drawable.getGL().getGL4();
        gl.glClearColor(engine.getBackgroundColorR(), engine.getBackgroundColorG(), engine.getBackgroundColorB(),
                engine.getBackgroundColorA());
        gl.glClear(GL4.GL_COLOR_BUFFER_BIT);

        GraphModel model = engine.model();
        GraphRenderOptions options = renderOptions;

        vertexBuffer.update(
                gl,
                model.vertices(),
                model.getSelectedVertexId(),
                model.getMaxDegree(),
                model.getColoringMode(),
                model.getUniformNodeR(),
                model.getUniformNodeG(),
                model.getUniformNodeB());

        edgeBuffer.update(model.edges(), options);

        vertexBuffer.upload(gl);
        edgeBuffer.upload(gl);

        drawEdges(gl, options);
        drawVertices(gl);
    }

    private void drawVertices(GL4 gl) {
        pointShader.use(gl);
        pointShader.setMat4(gl, "u_transform", engine.camera().getProjection());
        vertexBuffer.draw(gl);
    }

    private void drawEdges(GL4 gl, GraphRenderOptions options) {
        edgeShader.use(gl);
        edgeShader.setMat4(gl, "u_transform", engine.camera().getProjection());
        gl.glLineWidth(options == null ? 1f : options.edgeLineWidth());
        edgeBuffer.draw(gl);
        gl.glLineWidth(1f);
    }

    @Override
    public void reshape(GLAutoDrawable d, int x, int y, int w, int h) {
        engine.camera().resize(w, h);
    }

    @Override
    public void dispose(GLAutoDrawable drawable) {
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

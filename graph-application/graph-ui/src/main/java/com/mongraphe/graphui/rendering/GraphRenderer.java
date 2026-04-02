package com.mongraphe.graphui.rendering;

import java.util.List;
import java.util.concurrent.locks.StampedLock;

import com.jogamp.opengl.GL4;
import com.jogamp.opengl.GLAutoDrawable;
import com.jogamp.opengl.GLEventListener;
import com.mongraphe.graphui.model.Edge;
import com.mongraphe.graphui.model.GraphModel;
import com.mongraphe.graphui.model.Vertex;

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
        float[] posBuffer = engine.getPositionsBuffer();
        if (posBuffer == null)
            return;

        StampedLock lock = model.lock();
        long stamp = lock.tryOptimisticRead();

        List<Vertex> vertices = model.vertices();
        List<Edge> edges = model.edges();

        int selected = model.getSelectedVertexId();
        int maxDegree = model.getMaxDegree();
        GraphModel.ColoringMode mode = model.getColoringMode();

        float uniformR = model.getUniformNodeR();
        float uniformG = model.getUniformNodeG();
        float uniformB = model.getUniformNodeB();

        if (!lock.validate(stamp)) {

            stamp = lock.readLock();
            try {
                vertices = model.vertices();
                edges = model.edges();

                selected = model.getSelectedVertexId();
                maxDegree = model.getMaxDegree();
                mode = model.getColoringMode();

                uniformR = model.getUniformNodeR();
                uniformG = model.getUniformNodeG();
                uniformB = model.getUniformNodeB();
            } finally {
                lock.unlockRead(stamp);
            }
        }

        GraphRenderOptions options = renderOptions;

        vertexBuffer.update(
                gl,
                vertices,
                selected,
                maxDegree,
                mode,
                uniformR,
                uniformG,
                uniformB,
                posBuffer);

        edgeBuffer.update(edges, posBuffer, options);

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
        // On récupère les dimensions réelles de la surface de rendu
        int surfaceW = d.getSurfaceWidth();
        int surfaceH = d.getSurfaceHeight();

        GL4 gl = d.getGL().getGL4();
        gl.glViewport(0, 0, surfaceW, surfaceH);

        // On met à jour la caméra avec ces dimensions précises
        engine.camera().resize(surfaceW, surfaceH);
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

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
    private final Camera2D camera;

    private VertexGpuBuffer vertexBuffer;
    private EdgeGpuBuffer edgeBuffer;

    private GLShaderProgram pointShader;
    private GLShaderProgram edgeShader;

    // Couleur de fond paramétrable depuis l'UI
    private volatile float clearR = 1f, clearG = 1f, clearB = 1f, clearA = 1f;

    public GraphRenderer(GraphEngine engine, Camera2D camera) {
        this.engine = engine;
        this.camera = camera;
    }

    public void setBackgroundColor(float r, float g, float b, float a) {
        this.clearR = clamp01(r);
        this.clearG = clamp01(g);
        this.clearB = clamp01(b);
        this.clearA = clamp01(a);
    }

    private float clamp01(float v) {
        if (v < 0f)
            return 0f;
        if (v > 1f)
            return 1f;
        return v;
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
        gl.glClearColor(clearR, clearG, clearB, clearA);
    }

    @Override
    public void display(GLAutoDrawable drawable) {
        render(drawable.getGL().getGL4());
    }
    
    public void render(GL4 gl) {

        gl.glClearColor(clearR, clearG, clearB, clearA);
        gl.glClear(GL4.GL_COLOR_BUFFER_BIT);

        engine.update();

        GraphModel model = engine.model();

        synchronized (model.mutex()) {
            model.setZoom(camera.getZoom());
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

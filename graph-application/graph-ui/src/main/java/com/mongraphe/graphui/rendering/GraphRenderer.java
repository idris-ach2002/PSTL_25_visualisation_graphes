package com.mongraphe.graphui.rendering;

import com.jogamp.opengl.GL4;
import com.jogamp.opengl.GLAutoDrawable;
import com.jogamp.opengl.GLEventListener;
import com.mongraphe.graphui.model.GraphModel;

/**
 * Orchestrateur du pipeline de rendu OpenGL pour la visualisation du graphe.
 *
 * <p>
 * Cette classe implémente {@link GLEventListener} pour s'intégrer au cycle de
 * vie JOGL.
 * Elle fait le lien entre les données logiques du {@link GraphEngine} et les
 * buffers
 * GPU (sommets et arêtes).
 * </p>
 *
 * <h2>Fonctionnement</h2>
 * <ul>
 * <li><b>Initialisation :</b> Configure les états OpenGL (Blending, PointSize)
 * et compile les Shaders via la {@link ShaderFactory}.</li>
 * <li><b>Boucle de rendu :</b> À chaque frame, elle demande la mise à jour des
 * données CPU-GPU via {@code VertexGpuBuffer}
 * et {@code EdgeGpuBuffer}, puis exécute les appels de dessin (Draw
 * Calls).</li>
 * <li><b>Options de rendu :</b> Gère dynamiquement le style d'affichage (lignes
 * droites ou courbes) via {@link GraphRenderOptions}.</li>
 * </ul>
 */
public final class GraphRenderer implements GLEventListener {

    private final GraphEngine engine;
    private VertexGpuBuffer vertexBuffer;
    private EdgeGpuBuffer edgeBuffer;
    private GLShaderProgram pointShader;
    private GLShaderProgram edgeShader;
    private volatile GraphRenderOptions renderOptions;

    /**
     * Version des données déjà envoyées au GPU.
     *
     * <p>
     * Si la version du moteur ne change pas, le renderer ne reconstruit pas les
     * tableaux CPU et ne refait pas les uploads GPU.
     * </p>
     */
    private long lastUploadedRenderVersion = Long.MIN_VALUE;

    /** Dernières options de rendu utilisées pour construire les buffers. */
    private GraphRenderOptions lastUploadedOptions;

    /** Indique si un premier upload GPU a déjà eu lieu. */
    private boolean buffersUploaded = false;

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

    /**
     * Appelé par JOGL lors de la création du contexte OpenGL.
     * Initialise les états globaux et prépare les ressources GPU.
     */
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

        lastUploadedRenderVersion = Long.MIN_VALUE;
        lastUploadedOptions = null;
        buffersUploaded = false;
    }

    /** Configuration des paramètres de rendu (transparence, couleur de fond). */
    private void configureGL(GL4 gl) {
        gl.glEnable(GL4.GL_BLEND);
        gl.glBlendFunc(GL4.GL_SRC_ALPHA, GL4.GL_ONE_MINUS_SRC_ALPHA);
        gl.glEnable(GL4.GL_PROGRAM_POINT_SIZE);
        gl.glClearColor(engine.getBackgroundColorR(), engine.getBackgroundColorG(), engine.getBackgroundColorB(),
                engine.getBackgroundColorA());
    }

    /**
     * Boucle principale de rendu.
     * Synchronise les données du modèle vers le GPU et dessine les primitives.
     */
    @Override
    public void display(GLAutoDrawable drawable) {
        GL4 gl = drawable.getGL().getGL4();
        gl.glClearColor(engine.getBackgroundColorR(), engine.getBackgroundColorG(), engine.getBackgroundColorB(),
                engine.getBackgroundColorA());
        gl.glClear(GL4.GL_COLOR_BUFFER_BIT);

        GraphModel model = engine.model();
        GraphRenderOptions options = renderOptions;

        long currentVersion = engine.renderDataVersion();
        boolean dataChanged = currentVersion != lastUploadedRenderVersion;
        boolean optionsChanged = lastUploadedOptions != options;

        /*
         * Préparation CPU et transfert GPU uniquement si nécessaire.
         */
        if (!buffersUploaded || dataChanged || optionsChanged) {
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

            lastUploadedRenderVersion = currentVersion;
            lastUploadedOptions = options;
            buffersUploaded = true;
        }

        // Dessin effectif
        drawEdges(gl, options);
        drawVertices(gl);
    }

    /** Dessine les sommets (points) en utilisant le shader dédié. */
    private void drawVertices(GL4 gl) {
        pointShader.use(gl);
        pointShader.setMat4(gl, "u_transform", engine.camera().getProjection());
        vertexBuffer.draw(gl);
    }

    /**
     * Dessine les arêtes (lignes) en appliquant l'épaisseur définie dans les
     * options.
     */
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

    /** Libère les ressources GPU (Shaders) à la fermeture du moteur de rendu. */
    @Override
    public void dispose(GLAutoDrawable drawable) {
        try {
            GL4 gl = drawable.getGL().getGL4();
            if (pointShader != null)
                pointShader.delete(gl);
            if (edgeShader != null)
                edgeShader.delete(gl);
        } catch (Exception ignored) {
        } finally {
            lastUploadedRenderVersion = Long.MIN_VALUE;
            lastUploadedOptions = null;
            buffersUploaded = false;
        }
    }
}
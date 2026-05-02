package com.mongraphe.graphui.rendering;

import java.nio.FloatBuffer;

import com.mongraphe.graphui.model.GraphModel;
import com.mongraphe.graphui.model.Vertex;

import static org.lwjgl.opengl.GL11.*;

/**
 * Renderer OpenGLFX/LWJGL orienté GPU pour les graphes dynamiques de grande
 * taille.
 *
 * <p>
 * Cette version retire le LOD CPU interactif qui coûtait trop cher dans le
 * profilage ({@code updateVisibleStatic}, {@code rebuildVertexSet},
 * {@code updateVisibleAttributes}). Le renderer ne reconstruit plus une liste
 * de sommets visibles à chaque frame. Il conserve au contraire une topologie et
 * des attributs statiques sur le GPU, puis met uniquement à jour le buffer de
 * positions lorsque le moteur C publie une nouvelle itération.
 * </p>
 *
 * <p>
 * Le clipping de viewport est laissé au pipeline OpenGL. Parcourir 500 000 à 1
 * 000 000 d'objets en Java pour décider quoi afficher est
 * plus coûteux que de laisser le GPU rejeter les primitives hors écran. Le LOD
 * restant est visuel et non destructif : il adapte seulement le diamètre des
 * sommets selon le zoom, sans regrouper les couleurs ni supprimer les arêtes.
 * </p>
 */
public final class GraphRenderer {

    private static final int POSITION_TEXTURE_UNIT = 0;

    private final GraphEngine engine;

    private VertexGpuBuffer vertexBuffer;
    private EdgeGpuBuffer edgeBuffer;
    private PositionTextureBuffer positionBuffer;
    private GLShaderProgram pointShader;
    private GLShaderProgram straightEdgeShader;
    private GLShaderProgram curvedEdgeShader;
    private volatile GraphRenderOptions renderOptions;

    private long lastUploadedPositionVersion = Long.MIN_VALUE;
    private long lastUploadedDataVersion = Long.MIN_VALUE;
    private int lastUploadedVertexCount = -1;
    private boolean initialized = false;

    /**
     * Construit un renderer avec arêtes droites.
     *
     * @param engine moteur contenant le modèle, la caméra et les buffers natifs
     * @param camera paramètre conservé pour compatibilité de signature
     */
    public GraphRenderer(GraphEngine engine, Camera2D camera) {
        this(engine, camera, GraphRenderOptions.straight());
    }

    /**
     * Construit un renderer OpenGLFX/LWJGL.
     *
     * @param engine        moteur contenant le modèle, la caméra et les buffers
     *                      natifs
     * @param camera        paramètre conservé pour compatibilité de signature
     * @param renderOptions options de rendu initiales
     */
    public GraphRenderer(GraphEngine engine, Camera2D camera, GraphRenderOptions renderOptions) {
        this.engine = engine;
        this.renderOptions = renderOptions == null ? GraphRenderOptions.straight() : renderOptions;
    }

    /**
     * Change les options de rendu.
     *
     * @param renderOptions nouvelles options, ou arêtes droites si {@code null}
     */
    public void setRenderOptions(GraphRenderOptions renderOptions) {
        this.renderOptions = renderOptions == null ? GraphRenderOptions.straight() : renderOptions;
    }

    /** @return options de rendu actuellement demandées par l'interface. */
    public GraphRenderOptions getRenderOptions() {
        return renderOptions;
    }

    /** @return moteur utilisé par le scheduler de frames. */
    public GraphEngine engineForScheduling() {
        return engine;
    }

    /** Initialise les shaders, les buffers GPU et l'état OpenGL fixe. */
    public void init() {
        configureGL();
        positionBuffer = new PositionTextureBuffer();
        vertexBuffer = new VertexGpuBuffer();
        edgeBuffer = new EdgeGpuBuffer();

        positionBuffer.init();
        vertexBuffer.init();
        edgeBuffer.init();

        pointShader = ShaderFactory.createPointShader();
        straightEdgeShader = ShaderFactory.createStraightEdgeShader();
        curvedEdgeShader = ShaderFactory.createCurvedEdgeShader();

        invalidateBuffers();
        initialized = true;
    }

    /**
     * Configure un état OpenGL stable pour une surface OpenGLFX compositée par
     * Prism.
     */
    private void configureGL() {
        glDisable(GL_DEPTH_TEST);
        glDisable(GL_CULL_FACE);
        glDisable(GL_SCISSOR_TEST);
        glDisable(GL_DITHER);
        glDisable(GL_BLEND);
        glColorMask(true, true, true, true);
        glClearColor(engine.getBackgroundColorR(), engine.getBackgroundColorG(), engine.getBackgroundColorB(), 1f);
    }

    /**
     * Dessine une frame complète.
     *
     * <p>
     * Le chemin rapide fait seulement trois choses : clear, upload éventuel
     * des positions, deux draw calls. Les attributs et les arêtes ne sont
     * reconstruits que si la version de données du modèle change.
     * </p>
     */
    public void display() {
        if (!initialized) {
            return;
        }

        Camera2D camera = engine.camera();
        glDisable(GL_SCISSOR_TEST);
        glDisable(GL_BLEND);
        glViewport(0, 0, Math.max(1, camera.getWidth()), Math.max(1, camera.getHeight()));
        glClearColor(engine.getBackgroundColorR(), engine.getBackgroundColorG(), engine.getBackgroundColorB(), 1f);
        glClear(GL_COLOR_BUFFER_BIT);

        GraphModel model = engine.model();
        FloatBuffer sourcePositions = engine.renderPositionsBuffer();
        Vertex[] verticesById = engine.renderVerticesByNativeId();
        int totalVertices = Math.min(engine.renderPositionVertexCount(), verticesById.length);
        if (sourcePositions == null || totalVertices <= 0) {
            return;
        }

        long positionVersion = engine.renderPositionVersion();
        if (positionVersion != lastUploadedPositionVersion || totalVertices != lastUploadedVertexCount) {
            positionBuffer.upload(sourcePositions, totalVertices);
            lastUploadedPositionVersion = positionVersion;
            lastUploadedVertexCount = totalVertices;
        }

        GraphRenderOptions options = effectiveOptions(model, renderOptions);
        long dataVersion = engine.renderDataVersion();
        boolean dataChanged = dataVersion != lastUploadedDataVersion;
        if (dataChanged || totalVertices != vertexBuffer.count()) {
            vertexBuffer.updateAllAttributes(
                    verticesById,
                    totalVertices,
                    model.getSelectedVertexId(),
                    model.getMaxDegree(),
                    model.getColoringMode(),
                    model.getUniformNodeR(),
                    model.getUniformNodeG(),
                    model.getUniformNodeB());
            vertexBuffer.uploadAttributes();

            edgeBuffer.updateAllStatic(model.edges(), totalVertices);
            edgeBuffer.uploadStatic();

            lastUploadedDataVersion = dataVersion;
        }

        positionBuffer.bind(POSITION_TEXTURE_UNIT);
        drawEdges(options);
        drawVertices();
        positionBuffer.unbind(POSITION_TEXTURE_UNIT);
    }

    /** Retourne les options de rendu demandées par l'interface. */
    private GraphRenderOptions effectiveOptions(GraphModel model, GraphRenderOptions requested) {
        return requested == null ? GraphRenderOptions.straight() : requested;
    }

    /** Dessine les sommets avec un seul draw call instancié. */
    private void drawVertices() {
        pointShader.use();
        pointShader.setInt("u_positions", POSITION_TEXTURE_UNIT);
        pointShader.setMat4("u_transform", engine.camera().getProjection());
        pointShader.setVec2("u_viewport", engine.camera().getWidth(), engine.camera().getHeight());
        pointShader.setFloat("u_zoom", engine.camera().getZoom());
        vertexBuffer.draw(positionBuffer.vertexCount());
    }

    /**
     * Dessine les arêtes droites ou courbes sans reconstruire de segments côté
     * Java.
     */
    private void drawEdges(GraphRenderOptions options) {
        GraphRenderOptions safe = options == null ? GraphRenderOptions.straight() : options;
        GLShaderProgram shader = safe.edgeStyle() == GraphRenderOptions.EdgeStyle.CURVED_PARABOLIC
                ? curvedEdgeShader
                : straightEdgeShader;

        shader.use();
        shader.setInt("u_positions", POSITION_TEXTURE_UNIT);
        shader.setMat4("u_transform", engine.camera().getProjection());
        if (safe.edgeStyle() == GraphRenderOptions.EdgeStyle.CURVED_PARABOLIC) {
            shader.setInt("u_curveSegments", Math.min(32, Math.max(2, safe.curveSegments())));
            shader.setFloat("u_curveAngleRadians", (float) Math.toRadians(safe.curveAngleDegrees()));
            shader.setFloat("u_maxCurveOffset", safe.maxCurveOffset());
            shader.setInt("u_alternateCurveDirection", safe.alternateCurveDirection() ? 1 : 0);
        }
        glLineWidth(safe.edgeLineWidth());
        edgeBuffer.draw();
        glLineWidth(1f);
    }

    /**
     * Met à jour le viewport et la projection caméra.
     *
     * @param w largeur framebuffer en pixels
     * @param h hauteur framebuffer en pixels
     */
    public void reshape(int w, int h) {
        int width = Math.max(1, w);
        int height = Math.max(1, h);
        engine.camera().resize(width, height);
        glViewport(0, 0, width, height);
    }

    /** Force le renvoi des données statiques et dynamiques au prochain rendu. */
    public void invalidateBuffers() {
        lastUploadedPositionVersion = Long.MIN_VALUE;
        lastUploadedDataVersion = Long.MIN_VALUE;
        lastUploadedVertexCount = -1;
    }

    /** Libère les ressources OpenGL. */
    public void dispose() {
        try {
            if (pointShader != null)
                pointShader.delete();
            if (straightEdgeShader != null)
                straightEdgeShader.delete();
            if (curvedEdgeShader != null)
                curvedEdgeShader.delete();
            if (vertexBuffer != null)
                vertexBuffer.dispose();
            if (edgeBuffer != null)
                edgeBuffer.dispose();
            if (positionBuffer != null)
                positionBuffer.dispose();
        } catch (Exception ignored) {
        } finally {
            initialized = false;
            invalidateBuffers();
        }
    }
}

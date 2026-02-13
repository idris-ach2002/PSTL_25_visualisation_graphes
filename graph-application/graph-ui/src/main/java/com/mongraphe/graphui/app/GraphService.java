package com.mongraphe.graphui.app;

import java.io.File;
import java.util.Objects;

import com.mongraphe.graphui.GraphData;
import com.mongraphe.graphui.export.GraphExport;
import com.mongraphe.graphui.rendering.Camera2D;
import com.mongraphe.graphui.rendering.GraphEngine;
import com.mongraphe.graphui.rendering.GraphNativeEngine;
import com.mongraphe.graphui.rendering.GraphRenderer;
import com.mongraphe.graphui.view.GraphPanel;

/**
 * Service "métier" qui encapsule l'Engine, le Renderer et le Panel JOGL.
 * Le contrôleur JavaFX n'a plus à gérer l'allocation/désallocation, ni JNI directement.
 */
public final class GraphService {

    private final UiState ui;

    private GraphProject project;
    private GraphNativeEngine nativeEngine;
    private GraphEngine engine;
    private Camera2D camera;
    private GraphRenderer renderer;
    private GraphPanel panel;

    public GraphService(UiState ui) {
        this.ui = Objects.requireNonNull(ui, "ui");
    }

    public GraphPanel panel() { return panel; }
    public GraphEngine engine() { return engine; }
    public GraphRenderer renderer() { return renderer; }
    public Camera2D camera() { return camera; }
    public GraphProject project() { return project; }

    public void initIfNeeded() {
        if (engine != null) return;

        nativeEngine = new GraphNativeEngine();
        engine = new GraphEngine(nativeEngine);

        camera = new Camera2D();
        renderer = new GraphRenderer(engine, camera);

        panel = new GraphPanel(renderer);
    }

    /**
     * Charge un graphe depuis un fichier.
     *
     * Important: les seuils (threshold / anti-threshold) sont calculés côté natif
     * dans {@link GraphNativeEngine#initGraphCsv(String, GraphData.SimilitudeMode, GraphData.NodeCommunity)}.
     */
    public void load(File file,
                     GraphProject.SourceType type,
                     GraphData.SimilitudeMode similitudeMode,
                     GraphData.NodeCommunity communityMode) {
        Objects.requireNonNull(file, "file");
        initIfNeeded();

        // Nettoyage et rechargement
        unload();

        project = new GraphProject(file, type);

        if (type == GraphProject.SourceType.DOT) {
            engine.loadDot(file.getAbsolutePath(), communityMode);
        } else {
            engine.loadCsv(file.getAbsolutePath(), similitudeMode, communityMode);
        }

        ui.setStatus("Chargé: " + file.getName());
        ui.setRunning(true);
        engine.startSimulation();
    }

    public void unload() {
        if (engine == null) return;

        try { engine.stopSimulation(); } catch (Exception ignored) {}
        try { engine.freeNativeMemory(); } catch (Exception ignored) {}
    }

    public void dispose() {
        unload();

        if (panel != null) {
            try { panel.dispose(); } catch (Exception ignored) {}
        }
    }

    public void exportPng(File outFile, int width, int height) throws Exception {
        Objects.requireNonNull(outFile, "outFile");
        if (panel == null || renderer == null) throw new IllegalStateException("Panel non initialisé");
        new GraphExport(panel.window()).export(outFile.getAbsolutePath(), width, height, renderer);
        ui.setStatus("Export PNG: " + outFile.getName());
    }
}

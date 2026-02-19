package com.mongraphe.graphui.app;

import java.util.Objects;
import java.util.ArrayDeque;

import com.jogamp.newt.event.KeyAdapter;
import com.jogamp.newt.event.KeyEvent;
import com.jogamp.newt.event.MouseAdapter;
import com.jogamp.newt.event.MouseEvent;
import com.jogamp.newt.opengl.GLWindow;
import com.mongraphe.graphui.Vertex;
import com.mongraphe.graphui.rendering.Camera2D;
import com.mongraphe.graphui.rendering.GraphEngine;
import com.mongraphe.graphui.model.GraphModel;

/**
 * Interaction "Gephi-like" simplifiée:
 * - Clic: sélectionner un sommet
 * - Drag (clic gauche): déplacer le sommet sélectionné
 * - Drag (clic droit): déplacer la caméra (pan)
 * - Molette: zoom (centré sur curseur)
 * - ESPACE: pause/run (toggle)
 * - SUPPR/DEL: supprimer le sommet sélectionné
 */
public final class InteractionService {

    /**
     * Modes d'interaction (toolbar Gephi-like).
     */
    public enum Mode {
        RUN,
        SELECT,
        MOVE,
        DELETE
    }

    private final UiState ui;
    private final GraphService graphs;

    private Mode mode = Mode.RUN;

    private Vertex selected;
    private boolean draggingVertex = false;
    private int lastX, lastY;

    // ---- Undo/Redo (scope: déplacer/supprimer un sommet)
    private final ArrayDeque<EditAction> undoStack = new ArrayDeque<>();
    private final ArrayDeque<EditAction> redoStack = new ArrayDeque<>();

    private boolean moveArmed = false;
    private int movingVertexId = -1;
    private double moveStartX, moveStartY;

    public InteractionService(UiState ui, GraphService graphs) {
        this.ui = Objects.requireNonNull(ui, "ui");
        this.graphs = Objects.requireNonNull(graphs, "graphs");
    }

    public Mode getMode() {
        return mode;
    }

    public void setMode(Mode mode) {
        this.mode = (mode == null) ? Mode.RUN : mode;

        // Petit feedback (pas de spam de logs)
        switch (this.mode) {
            case RUN -> ui.setStatus("Mode: Exécuter (SPACE = pause/run)");
            case SELECT -> ui.setStatus("Mode: Sélection");
            case MOVE -> ui.setStatus("Mode: Déplacer");
            case DELETE -> ui.setStatus("Mode: Supprimer");
        }
    }

    public Vertex selectedVertex() { return selected; }

    /**
     * Undo dernière action (déplacement/suppression).
     * Note: si l'action ne peut pas être appliquée (engine non chargé), on no-op.
     */
    public void undo() {
        if (undoStack.isEmpty()) {
            ui.setStatus("Rien à annuler");
            return;
        }
        EditAction a = undoStack.pop();
        try {
            a.undo();
            redoStack.push(a);
        } catch (Exception ex) {
            ui.setStatus("Annulation impossible: " + ex.getMessage());
        }
    }

    /** Redo dernière action annulée. */
    public void redo() {
        if (redoStack.isEmpty()) {
            ui.setStatus("Rien à rétablir");
            return;
        }
        EditAction a = redoStack.pop();
        try {
            a.redo();
            undoStack.push(a);
        } catch (Exception ex) {
            ui.setStatus("Rétablissement impossible: " + ex.getMessage());
        }
    }

    private void pushAction(EditAction a) {
        if (a == null) return;
        undoStack.push(a);
        redoStack.clear();
    }

    public void attach(GLWindow window) {
        Objects.requireNonNull(window, "window");

        window.addMouseListener(new MouseAdapter() {
            @Override
            public void mousePressed(MouseEvent e) {
                if (graphs.engine() == null) return;

                lastX = e.getX();
                lastY = e.getY();

                if (e.getButton() == MouseEvent.BUTTON1) {
                    // Clic gauche : dépend du mode.
                    selectAt(e.getX(), e.getY());

                    if (mode == Mode.DELETE) {
                        deleteSelected();
                        draggingVertex = false;
                        return;
                    }

                    // En mode SELECT on ne déplace jamais.
                    if (mode == Mode.SELECT) {
                        draggingVertex = false;
                        return;
                    }

                    // RUN ou MOVE => drag possible si un sommet est sélectionné.
                    draggingVertex = (selected != null);

                    // Arm move tracking for undo (only if actually draggable)
                    if (draggingVertex && (mode == Mode.MOVE || mode == Mode.RUN) && selected != null) {
                        moveArmed = true;
                        movingVertexId = selected.getId();
                        moveStartX = selected.getX();
                        moveStartY = selected.getY();
                    } else {
                        moveArmed = false;
                        movingVertexId = -1;
                    }
                } else if (e.getButton() == MouseEvent.BUTTON3) {
                    draggingVertex = false;
                    moveArmed = false;
                    movingVertexId = -1;
                }
            }

            @Override
            public void mouseReleased(MouseEvent e) {
                draggingVertex = false;

                // Push a MoveAction on release if there was an actual move
                if (moveArmed && movingVertexId >= 0 && graphs.engine() != null) {
                    GraphModel model = graphs.engine().model();
                    Vertex v;
                    synchronized (model.mutex()) {
                        v = (movingVertexId < model.vertices().size()) ? model.vertices().get(movingVertexId) : null;
                    }
                    if (v != null) {
                        double endX = v.getX();
                        double endY = v.getY();
                        if (Math.abs(endX - moveStartX) > 1e-6 || Math.abs(endY - moveStartY) > 1e-6) {
                            pushAction(new MoveAction(movingVertexId, moveStartX, moveStartY, endX, endY));
                        }
                    }
                }

                moveArmed = false;
                movingVertexId = -1;
            }

            @Override
            public void mouseDragged(MouseEvent e) {
                if (graphs.engine() == null) return;

                int dx = e.getX() - lastX;
                int dy = e.getY() - lastY;
                lastX = e.getX();
                lastY = e.getY();

                Camera2D cam = graphs.camera();
                if (cam == null) return;

                // Pan camera (clic droit)
                if (e.getButton() == MouseEvent.BUTTON3) {
                    cam.pan(dx, dy);
                    return;
                }

                // Drag vertex (clic gauche)
                if (e.getButton() == MouseEvent.BUTTON1 && draggingVertex && selected != null && (mode == Mode.MOVE || mode == Mode.RUN)) {
                    float wx = cam.screenToWorldX(e.getX());
                    float wy = cam.screenToWorldY(e.getY());

                    selected.updatePosition(wx, wy);
                    graphs.engine().setNodePosition(selected.getId(), wx, wy);
                    ui.setStatus("Sommet " + selected.getId() + " déplacé");
                }
            }

            @Override
            public void mouseWheelMoved(MouseEvent e) {
                Camera2D cam = graphs.camera();
                if (cam == null) return;

                float amount = e.getRotation()[1];
                cam.zoomAt(e.getX(), e.getY(), amount);
            }
        });

        window.addKeyListener(new KeyAdapter() {
            @Override
            public void keyPressed(KeyEvent e) {
                if (graphs.engine() == null) return;

                if (e.getKeyCode() == KeyEvent.VK_SPACE) {
                    toggleSimulation();
                } else if (e.getKeyCode() == KeyEvent.VK_DELETE || e.getKeyCode() == KeyEvent.VK_BACK_SPACE) {
                    deleteSelected();
                }
            }
        });
    }

    public boolean canUndo() { return !undoStack.isEmpty(); }
    public boolean canRedo() { return !redoStack.isEmpty(); }

    public void toggleSimulation() {
        GraphEngine engine = graphs.engine();
        if (engine == null) return;

        boolean running = engine.isSimulationRunning();
        if (running) {
            engine.stopSimulation();
            ui.setRunning(false);
            ui.setStatus("Pause (ESPACE pour relancer)");
        } else {
            engine.startSimulation();
            ui.setRunning(true);
            ui.setStatus("Simulation en cours");
        }
    }

    private void selectAt(int sx, int sy) {
        GraphEngine engine = graphs.engine();
        Camera2D cam = graphs.camera();
        if (engine == null || cam == null) return;

        float wx = cam.screenToWorldX(sx);
        float wy = cam.screenToWorldY(sy);

        GraphModel model = engine.model();
        synchronized (model.mutex()) {
            selected = model.findVertexAt(wx, wy);
            model.setSelectedVertexId(selected != null ? selected.getId() : -1);
        }

        if (selected != null) {
            ui.setStatus("Sélection: sommet " + selected.getId() + " (SUPPR pour supprimer)");
        } else {
            ui.setStatus("Aucune sélection");
        }
    }

    public void deleteSelected() {
        if (selected == null) return;
        GraphEngine engine = graphs.engine();
        if (engine == null) return;

        int id = selected.getId();
        double prevDiameter = selected.getDiameter();
        engine.deleteNode(id);

        GraphModel model = engine.model();
        synchronized (model.mutex()) {
            model.deleteVertex(selected);
            model.setSelectedVertexId(-1);
        }
        ui.setStatus("Sommet supprimé: " + id);

        // undo/redo
        pushAction(new DeleteAction(id, prevDiameter));

        selected = null;
    }

    // ============================================================
    // Undo/Redo actions
    // ============================================================
    private interface EditAction {
        void undo();
        void redo();
    }

    private final class DeleteAction implements EditAction {
        private final int id;
        @SuppressWarnings("unused")
        private final double previousDiameter;

        private DeleteAction(int id, double previousDiameter) {
            this.id = id;
            this.previousDiameter = previousDiameter;
        }

        @Override
        public void undo() {
            GraphEngine engine = graphs.engine();
            if (engine == null) return;
            engine.restoreNode(id);

            GraphModel model = engine.model();
            synchronized (model.mutex()) {
                if (id >= 0 && id < model.vertices().size()) {
                    Vertex v = model.vertices().get(id);
                    v.restore();
                    model.applyFilters();
                }
            }
            ui.setStatus("Annulé: suppression du sommet " + id);
        }

        @Override
        public void redo() {
            GraphEngine engine = graphs.engine();
            if (engine == null) return;
            engine.deleteNode(id);
            GraphModel model = engine.model();
            synchronized (model.mutex()) {
                if (id >= 0 && id < model.vertices().size()) {
                    model.deleteVertex(model.vertices().get(id));
                    model.setSelectedVertexId(-1);
                }
            }
            ui.setStatus("Rétabli: suppression du sommet " + id);
        }
    }

    private final class MoveAction implements EditAction {
        private final int id;
        private final double fromX, fromY, toX, toY;

        private MoveAction(int id, double fromX, double fromY, double toX, double toY) {
            this.id = id;
            this.fromX = fromX;
            this.fromY = fromY;
            this.toX = toX;
            this.toY = toY;
        }

        @Override
        public void undo() {
            setPos(id, fromX, fromY);
            ui.setStatus("Annulé: déplacement du sommet " + id);
        }

        @Override
        public void redo() {
            setPos(id, toX, toY);
            ui.setStatus("Rétabli: déplacement du sommet " + id);
        }

        private void setPos(int id, double x, double y) {
            GraphEngine engine = graphs.engine();
            if (engine == null) return;
            engine.setNodePosition(id, x, y);
            GraphModel model = engine.model();
            synchronized (model.mutex()) {
                if (id >= 0 && id < model.vertices().size()) {
                    model.vertices().get(id).updatePosition(x, y);
                }
            }
        }
    }
}

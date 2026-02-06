package com.mongraphe.graphui.interaction;

import com.mongraphe.graphui.Graph;
import com.mongraphe.graphui.GraphData;
import com.mongraphe.graphui.Vertex;
import com.mongraphe.graphui.view.GraphView;

/**
 * GraphInteractionController
 * --------------------------
 *
 * Cette classe est responsable de la gestion des interactions utilisateur :
 * - modes du graphe (RUN, MOVE, DELETE, SELECTION),
 * - survol de sommets,
 * - communication entre le moteur du graphe et l'interface utilisateur.
 *
 * Objectif :
 * Découpler la logique d'interaction du contrôleur JavaFX (GraphView)
 * afin d'améliorer la lisibilité, la maintenabilité et la compréhension pédagogique.
 *
 * Cette classe ne gère PAS :
 * - le rendu OpenGL,
 * - l'interface JavaFX (FXML),
 * - les calculs lourds (JNI).
 */
public class GraphInteractionController {

    private final Graph graph;
    private final GraphView graphView;

    /**
     * Crée un contrôleur d'interaction pour un graphe donné.
     *
     * @param graph    le graphe métier
     * @param graphView la vue à notifier (survol, infos UI)
     */
    public GraphInteractionController(Graph graph, GraphView graphView) {
        this.graph = graph;
        this.graphView = graphView;
    }

    /**
     * Applique un mode d'interaction au graphe.
     * API typée : aucun String, aucun switch fragile.
     *
     * @param mode mode du graphe à appliquer
     */
    public void applyGraphMode(GraphData.GraphMode mode) {
        if (mode == null) {
            return;
        }
        graph.setMode(mode);
    }

    /**
     * Notifie la vue lorsqu'un sommet est survolé.
     *
     * @param vertex sommet survolé, ou null si aucun
     */
    public void onVertexHovered(Vertex vertex) {
        graphView.setHoveredVertex(vertex);
    }

    /**
     * Met en pause l'animation du graphe.
     */
    public void pause() {
        if (graph.animator != null && graph.animator.isAnimating()) {
            graph.animator.pause();
        }
    }

    /**
     * Reprend l'animation du graphe.
     */
    public void resume() {
        if (graph.animator != null && !graph.animator.isAnimating()) {
            graph.animator.resume();
        }
    }
}
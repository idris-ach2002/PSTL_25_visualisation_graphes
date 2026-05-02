package com.mongraphe.graphui.controller;

import com.mongraphe.graphui.app.CommandBus;
import com.mongraphe.graphui.interfaces.CommandBusLinkedI;
import com.mongraphe.graphui.rendering.GraphEngine;

import javafx.application.Platform;
import javafx.fxml.FXML;
import javafx.scene.control.Label;
import javafx.scene.layout.VBox;

/**
 * Contrôleur JavaFX chargé d'afficher les statistiques du graphe
 * actuellement géré par le {@link GraphEngine}.
 *
 * <p>
 * Ce contrôleur observe les modifications des données du graphe
 * en implémentant l'interface {@link GraphEngine.GraphDataListener}.
 * Lorsqu'un changement survient, les statistiques affichées dans
 * l'interface utilisateur sont automatiquement mises à jour.
 * </p>
 *
 * <h2>Responsabilités</h2>
 * <ul>
 * <li>Afficher différentes statistiques du graphe.</li>
 * <li>Réagir aux changements de données du moteur de graphe.</li>
 * <li>Mettre à jour l'interface utilisateur de manière thread-safe.</li>
 * <li>Afficher les informations du sommet actuellement sélectionné.</li>
 * </ul>
 *
 * <h2>Statistiques affichées</h2>
 * <ul>
 * <li>Nombre de sommets visibles</li>
 * <li>Nombre de sommets cachés</li>
 * <li>Nombre de sommets supprimés</li>
 * <li>Nombre d'arêtes visibles</li>
 * <li>Nombre d'arêtes cachées</li>
 * <li>Nombre total d'éléments</li>
 * <li>Informations sur le sommet sélectionné (identifiant et position)</li>
 * </ul>
 *
 * <h2>Gestion du multithreading</h2>
 * <p>
 * Les notifications provenant du moteur peuvent être émises depuis
 * un thread non graphique. Pour garantir la sécurité de l'interface
 * JavaFX, les mises à jour sont exécutées dans le thread JavaFX via
 * {@link Platform#runLater(Runnable)}.
 * </p>
 */
public class GraphStatsController implements GraphEngine.GraphDataListener, CommandBusLinkedI<GraphEngine> {

    private volatile boolean disposed = false;

    /** Evite d'empiler des rafraîchissements de stats dans la file JavaFX. */
    private final java.util.concurrent.atomic.AtomicBoolean refreshQueued = new java.util.concurrent.atomic.AtomicBoolean(false);

    /** Dernier rafraîchissement effectif des labels de statistiques. */
    private volatile long lastRefreshNanos = 0L;

    /** Fréquence maximale de mise à jour UI des statistiques pendant les gros calculs. */
    private static final long MIN_REFRESH_INTERVAL_NANOS = 250_000_000L;

    /**
     * Bus de commandes utilisé pour communiquer avec le moteur de graphe.
     */
    private CommandBus<GraphEngine> bus;

    /** Conteneur principal affichant les statistiques. */
    @FXML
    private VBox statsPane;

    /** Label affichant le nombre de sommets visibles. */
    @FXML
    private Label nodesDisplayedLabel;

    /** Label affichant le nombre de sommets cachés. */
    @FXML
    private Label nodesHiddenLabel;

    /** Label affichant le nombre de sommets supprimés. */
    @FXML
    private Label nodesDeletedLabel;

    /** Label affichant le nombre d'arêtes visibles. */
    @FXML
    private Label edgesDisplayedLabel;

    /** Label affichant le nombre d'arêtes cachées. */
    @FXML
    private Label edgesHiddenLabel;

    /** Label affichant le nombre total d'éléments du graphe. */
    @FXML
    private Label totalElementsLabel;

    /** Label affichant l'identifiant du sommet sélectionné. */
    @FXML
    private Label selectedVertexIdLabel;

    /** Label affichant la coordonnée X du sommet sélectionné. */
    @FXML
    private Label selectedVertexXLabel;

    /** Label affichant la coordonnée Y du sommet sélectionné. */
    @FXML
    private Label selectedVertexYLabel;

    /**
     * Associe le bus de commandes au contrôleur.
     *
     * <p>
     * Lorsqu'un bus valide est fourni :
     * </p>
     * <ul>
     * <li>le contrôleur s'enregistre comme écouteur des changements
     * de données du moteur de graphe</li>
     * <li>un rafraîchissement initial des statistiques est effectué</li>
     * </ul>
     *
     * @param bus bus de commandes permettant d'accéder au moteur
     */
    @Override
    public void setBus(CommandBus<GraphEngine> bus) {
        if (this.bus != null) {
            try {
                this.bus.dispatch(engine -> engine.removeDataListener(this));
            } catch (Exception ignored) {
            }
        }

        this.bus = bus;
        this.disposed = false;

        if (bus != null) {
            bus.dispatch(engine -> engine.addDataListener(this));
            refreshStats();
        }
    }

    /**
     * Méthode appelée lorsque les données du graphe ont changé.
     *
     * <p>
     * Cette méthode est invoquée par le moteur de graphe via le mécanisme
     * d'écouteurs. Afin de garantir la sécurité du thread JavaFX,
     * la mise à jour de l'interface est planifiée dans le thread graphique.
     * </p>
     */
    @Override
    public void onGraphDataChanged() {
        if (disposed || !refreshQueued.compareAndSet(false, true)) {
            return;
        }
        Platform.runLater(() -> {
            try {
                if (!disposed) {
                    refreshStatsThrottled();
                }
            } finally {
                refreshQueued.set(false);
            }
        });
    }

    /** Rafraîchit les stats au maximum quatre fois par seconde. */
    private void refreshStatsThrottled() {
        long now = System.nanoTime();
        if (now - lastRefreshNanos < MIN_REFRESH_INTERVAL_NANOS) {
            return;
        }
        lastRefreshNanos = now;
        refreshStats();
    }

    /**
     * Met à jour les statistiques affichées dans l'interface utilisateur.
     *
     * <p>
     * Les données sont récupérées sous forme d'un instantané
     * ({@link GraphEngine.StatsSnapshot}) obtenu depuis le moteur
     * via le {@link CommandBus}.
     * </p>
     *
     * <p>
     * Les labels sont ensuite mis à jour pour refléter :
     * </p>
     * <ul>
     * <li>le nombre de sommets visibles, cachés et supprimés</li>
     * <li>le nombre d'arêtes visibles et cachées</li>
     * <li>le nombre total d'éléments</li>
     * <li>les informations du sommet sélectionné</li>
     * </ul>
     *
     * <p>
     * Si aucun sommet n'est sélectionné, les informations correspondantes
     * sont remplacées par le symbole "-".
     * </p>
     */
    public void refreshStats() {
        if (disposed || bus == null)
            return;

        try {
            GraphEngine.StatsSnapshot stats = bus.dispatchSync(GraphEngine::getStatsSnapshot);

            nodesDisplayedLabel.setText(String.valueOf(stats.visibleVertices()));
            nodesHiddenLabel.setText(String.valueOf(stats.hiddenVertices()));
            nodesDeletedLabel.setText(String.valueOf(stats.deletedVertices()));

            edgesDisplayedLabel.setText(String.valueOf(stats.visibleEdges()));
            edgesHiddenLabel.setText(String.valueOf(stats.hiddenEdges()));

            totalElementsLabel.setText(String.valueOf(stats.totalVertices() + stats.totalEdges()));

            if (stats.selectedVertexId() >= 0) {
                selectedVertexIdLabel.setText(String.valueOf(stats.selectedVertexId()));
                selectedVertexXLabel.setText(String.format("%.2f", stats.selectedX()));
                selectedVertexYLabel.setText(String.format("%.2f", stats.selectedY()));
            } else {
                selectedVertexIdLabel.setText("-");
                selectedVertexXLabel.setText("-");
                selectedVertexYLabel.setText("-");
            }

        } catch (Exception ignored) {
            // Les erreurs sont volontairement ignorées pour éviter
            // de perturber l'interface utilisateur en cas de problème ponctuel.
        }
    }

    public void dispose() {
        disposed = true;
        if (bus != null) {
            try {
                bus.dispatch(engine -> engine.removeDataListener(this));
            } catch (Exception ignored) {
            }
            bus = null;
        }
    }
}
package com.mongraphe.graphui.controller;

import java.io.File;

import com.mongraphe.graphui.app.CommandBus;
import com.mongraphe.graphui.interfaces.CommandBusLinkedI;
import com.mongraphe.graphui.model.GraphData;
import com.mongraphe.graphui.model.Metadata;
import com.mongraphe.graphui.rendering.GraphEngine;

import javafx.animation.PauseTransition;
import javafx.application.Platform;
import javafx.fxml.FXML;
import javafx.scene.control.*;
import javafx.scene.layout.VBox;
import javafx.util.Duration;

/**
 * Contrôleur JavaFX gérant l'espace de travail principal de configuration
 * et d'exécution du graphe.
 *
 * <p>
 * Cette classe permet à l'utilisateur de configurer les paramètres
 * de génération et de simulation du graphe avant son lancement.
 * Elle agit comme intermédiaire entre l'interface graphique et
 * le moteur de simulation {@link GraphEngine}.
 * </p>
 *
 * <h2>Responsabilités</h2>
 * <ul>
 * <li>Configurer les paramètres du graphe avant son lancement.</li>
 * <li>Gérer les actions de contrôle de la simulation (lecture, pause,
 * redémarrage).</li>
 * <li>Appliquer les paramètres spécifiques aux projets CSV.</li>
 * <li>Configurer la partition spatiale et les paramètres de clustering.</li>
 * <li>Valider les valeurs saisies par l'utilisateur.</li>
 * </ul>
 *
 * <h2>Types de projets supportés</h2>
 * <ul>
 * <li><b>CSV</b> : nécessite la configuration de seuils de similarité
 * et de paramètres supplémentaires.</li>
 * <li><b>DOT</b> : utilise uniquement les paramètres de base du graphe.</li>
 * </ul>
 *
 * <h2>Architecture</h2>
 * <p>
 * Ce contrôleur communique avec le moteur via un {@link CommandBus},
 * permettant d'exécuter des commandes sur le moteur de graphe tout
 * en maintenant une séparation claire entre l'interface et la logique
 * de simulation.
 * </p>
 */
public final class GraphWorkspaceController implements CommandBusLinkedI<GraphEngine> {

    /**
     * Contrôleur principal responsable de la gestion globale du graphe.
     */
    private MainGraphController mainController;

    /** Conteneur affichant les options spécifiques aux projets CSV. */
    @FXML
    private VBox csvSpecificBox;

    /** Sélecteur du mode de similarité pour les données CSV. */
    @FXML
    private ComboBox<GraphData.SimilitudeMode> similarityCombo;

    /** Sélecteur du mode de détection de communautés. */
    @FXML
    private ComboBox<GraphData.NodeCommunity> communityCombo;

    /** Sélecteur du mode de répulsion entre les nœuds. */
    @FXML
    private ComboBox<GraphData.RepulsionMode> repulsionCombo;

    /** Bouton permettant de lancer ou mettre en pause la simulation. */
    @FXML
    private Button playPauseButton;

    /** Bouton permettant de relancer la simulation depuis zéro. */
    @FXML
    private Button restartButton;

    /** Champ de saisie de la largeur du graphe. */
    @FXML
    private TextField widthField;

    /** Champ de saisie de la hauteur du graphe. */
    @FXML
    private TextField heightField;

    /** Active l'utilisation des seuils recommandés pour les projets CSV. */
    @FXML
    private CheckBox useRecommendedThresholds;

    /** Champ de saisie du seuil de création d'arêtes. */
    @FXML
    private TextField edgeThresholdField;

    /** Champ de saisie du seuil anti-arêtes. */
    @FXML
    private TextField antiThresholdField;

    /** Label affichant les seuils recommandés. */
    @FXML
    private Label recommendedThresholdsLabel;

    /** Nombre de cellules utilisées pour la partition spatiale. */
    @FXML
    private TextField spatialCellsField;

    /** Active l'utilisation du clustering K-means. */
    @FXML
    private CheckBox enableKmeans;

    /** Paramètre epsilon utilisé par l'algorithme de clustering. */
    @FXML
    private TextField epsilonField;

    /** Bus de communication avec le moteur de graphe. */
    private CommandBus<GraphEngine> bus;

    /** Indique si le projet chargé est un projet CSV. */
    private boolean isCsvProject = false;

    /** Valeur recommandée pour le seuil d'arêtes. */
    private double recommendedEdge = 0.0;

    /** Valeur recommandée pour le seuil anti-arêtes. */
    private double recommendedAnti = 0.0;

    /**
     * Associe le contrôleur principal à ce contrôleur.
     *
     * @param controller contrôleur principal de l'application
     */
    public void setMainController(MainGraphController controller) {
        this.mainController = controller;
    }

    /**
     * Méthode appelée automatiquement après le chargement du FXML.
     *
     * <p>
     * Elle initialise les composants de l'interface et configure
     * les valeurs par défaut utilisées lors de la création du graphe.
     * </p>
     */
    @FXML
    private void initialize() {

        communityCombo.getItems().setAll(GraphData.NodeCommunity.values());
        repulsionCombo.getItems().setAll(GraphData.RepulsionMode.values());

        if (!repulsionCombo.getItems().isEmpty()) {
            repulsionCombo.getSelectionModel().selectFirst();
        }

        playPauseButton.setText("▶");
        restartButton.setText("⟳");

        restartButton.setTooltip(new Tooltip("Relancer l'algorithme depuis zéro"));
        playPauseButton.setTooltip(new Tooltip("Lancer"));

        communityCombo.getSelectionModel().selectFirst();
        repulsionCombo.getSelectionModel().selectFirst();

        similarityCombo.getItems().setAll(GraphData.SimilitudeMode.values());
        similarityCombo.getSelectionModel().selectFirst();

        useRecommendedThresholds.selectedProperty().addListener((obs, old, sel) -> {
            edgeThresholdField.setDisable(sel);
            antiThresholdField.setDisable(sel);
            if (sel) {
                edgeThresholdField.setText(String.format("%.4f", recommendedEdge));
                antiThresholdField.setText(String.format("%.4f", recommendedAnti));
            }
        });

        enableKmeans.setSelected(false);
        epsilonField.setText("0.1");
    }

    /**
     * Définit le type de projet actuellement chargé.
     *
     * @param isCsv indique si le projet est un projet CSV
     */
    public void setProjectType(boolean isCsv) {
        this.isCsvProject = isCsv;
        csvSpecificBox.setVisible(isCsv);
        csvSpecificBox.setManaged(isCsv);
    }

    /**
     * Met à jour les valeurs de seuils recommandées.
     *
     * @param edge seuil recommandé pour les arêtes
     * @param anti seuil recommandé pour les anti-arêtes
     */
    public void setRecommendedThresholds(double edge, double anti) {
        this.recommendedEdge = edge;
        this.recommendedAnti = anti;

        recommendedThresholdsLabel.setText(
                String.format("Recommandés : %.4f / %.4f", edge, anti));

        if (useRecommendedThresholds.isSelected()) {
            edgeThresholdField.setText(String.format("%.4f", edge));
            antiThresholdField.setText(String.format("%.4f", anti));
        }
    }

    /**
     * Met à jour l'icône du bouton de contrôle de simulation.
     *
     * @param running indique si la simulation est en cours
     */
    public void updatePlayPauseIcon(boolean running) {

        if (bus == null)
            return;

        if (running) {
            playPauseButton.setText("⏸");
            playPauseButton.setTooltip(new Tooltip("Mettre en pause"));
        } else {
            playPauseButton.setText("▶");
            playPauseButton.setTooltip(new Tooltip("Lancer"));
        }
    }

    /**
     * Vérifie la validité des dimensions du graphe.
     *
     * @return true si les dimensions sont valides
     */
    private boolean validateDimensions() {

        String widthText = widthField.getText().trim();
        String heightText = heightField.getText().trim();

        if (widthText.isEmpty()) {
            showTooltip(widthField, "La largeur est obligatoire");
            return false;
        }

        if (heightText.isEmpty()) {
            showTooltip(heightField, "La hauteur est obligatoire");
            return false;
        }

        try {

            double w = Double.parseDouble(widthText);
            double h = Double.parseDouble(heightText);

            if (w <= 0 || h <= 0) {
                showTooltip(widthField, "Les dimensions doivent être positives");
                return false;
            }

        } catch (NumberFormatException e) {
            showTooltip(widthField, "Valeur numérique invalide");
            return false;
        }

        widthField.setStyle("");
        heightField.setStyle("");

        return true;
    }

    /**
     * Action associée au bouton lecture / pause.
     */
    @FXML
    private void handlePlayPause() {

        if (mainController == null || bus == null)
            return;

        boolean running = bus.dispatchSync(GraphEngine::isSimulationRunning);

        if (running) {
            bus.dispatch(GraphEngine::stopSimulation);
        } else {

            int vertexCount = bus.dispatchSync(engine -> engine.model().vertexCount());

            if (vertexCount == 0) {
                startGraph();
            } else {
                bus.dispatch(GraphEngine::startSimulation);
            }
        }
    }

    /**
     * Action permettant de relancer complètement l'algorithme.
     */
    @FXML
    private void handleRestart() {

        if (mainController == null)
            return;

        startGraph();
    }

    /**
     * Lance la création et la simulation du graphe avec les
     * paramètres actuellement configurés.
     */
    private void startGraph() {

        if (!validateDimensions())
            return;

        double width = Double.parseDouble(widthField.getText().trim());
        double height = Double.parseDouble(heightField.getText().trim());

        File f = mainController.getFile();
        String path = f.getAbsolutePath();

        bus.dispatchSyncVoid(e -> e.startProgram(path));

        Platform.runLater(() -> {

            if (spatialCellsField.getText().isEmpty()) {

                Integer nodeCount = bus.dispatchSync(engine -> engine.model().vertexCount());

                if (nodeCount != null && nodeCount > 0) {

                    int cells = (int) Math.sqrt(nodeCount);
                    spatialCellsField.setText(String.valueOf(cells));
                }
            }
        });

        if (isCsvProject) {

            if (similarityCombo.getValue() == null) {
                showTooltip(similarityCombo,
                        "Sélectionnez une mesure de similarité");
                return;
            }

            double edgeThreshold;
            double antiThreshold;

            if (useRecommendedThresholds.isSelected()) {

                Metadata recommended = bus.dispatchSync(engine -> engine.computeThreshold(
                        similarityCombo.getValue(), 10));

                recommendedEdge = recommended.getEdgeThreshold();
                recommendedAnti = recommended.getAntiThreshold();

                edgeThreshold = recommendedEdge;
                antiThreshold = recommendedAnti;

                setRecommendedThresholds(recommendedEdge, recommendedAnti);

            } else {

                try {

                    edgeThreshold = Double.parseDouble(edgeThresholdField.getText().trim());
                    antiThreshold = Double.parseDouble(antiThresholdField.getText().trim());

                } catch (NumberFormatException e) {
                    showTooltip(edgeThresholdField, "Seuil invalide");
                    return;
                }
            }

            Integer spatialCells = parseSpatialCells();
            Boolean kmeans = enableKmeans.isSelected();
            Double epsilon = parseEpsilon();

            if (spatialCells != null) {
                bus.dispatchSyncVoid(e -> e.setSpatialCells(spatialCells));
            }

            bus.dispatchSyncVoid(e -> e.setKmeansMode(kmeans));

            if (kmeans && epsilon != null) {
                bus.dispatchSyncVoid(e -> e.setEpsilon(epsilon));
            }

            mainController.startGraphCsv(
                    similarityCombo.getValue(),
                    communityCombo.getValue(),
                    repulsionCombo.getValue(),
                    width,
                    height,
                    edgeThreshold,
                    antiThreshold);

        } else {

            mainController.startGraphDot(
                    communityCombo.getValue(),
                    width,
                    height);
        }
    }

    /**
     * Analyse le nombre de cellules de partition spatiale.
     *
     * @return valeur numérique ou null si non définie
     */
    private Integer parseSpatialCells() {

        String text = spatialCellsField.getText().trim();

        if (text.isEmpty() || text.equalsIgnoreCase("auto"))
            return null;

        try {
            return Integer.parseInt(text);
        } catch (NumberFormatException e) {
            return null;
        }
    }

    /**
     * Analyse la valeur du paramètre epsilon.
     *
     * @return valeur numérique ou null
     */
    private Double parseEpsilon() {

        String text = epsilonField.getText().trim();

        if (text.isEmpty())
            return null;

        try {
            return Double.parseDouble(text);
        } catch (NumberFormatException e) {
            return null;
        }
    }

    /**
     * Affiche un message d'erreur sous forme de tooltip
     * directement sur un composant de l'interface.
     *
     * @param control composant cible
     * @param message message d'erreur
     */
    private void showTooltip(Control control, String message) {

        Tooltip tooltip = new Tooltip(message);
        tooltip.setAutoHide(true);

        tooltip.show(
                control,
                control.localToScreen(0, 0).getX(),
                control.localToScreen(0, 0).getY() - 30);

        control.setStyle("-fx-border-color: red; -fx-border-width: 2px;");

        PauseTransition delay = new PauseTransition(Duration.seconds(2));
        delay.setOnFinished(e -> control.setStyle(""));
        delay.play();
    }

    /**
     * Retourne la méthode de détection de communautés sélectionnée.
     *
     * @return méthode de communautés
     */
    public GraphData.NodeCommunity getSelectedCommunity() {
        return communityCombo.getValue();
    }

    /**
     * Retourne le mode de répulsion sélectionné.
     *
     * @return mode de répulsion
     */
    public GraphData.RepulsionMode getSelectedRepulsionMode() {
        return repulsionCombo.getValue();
    }

    /**
     * Définit les valeurs sélectionnées dans les menus déroulants.
     *
     * @param community communauté sélectionnée
     * @param repulsion mode de répulsion
     */
    public void setSelections(
            GraphData.NodeCommunity community,
            GraphData.RepulsionMode repulsion) {

        communityCombo.setValue(community);

        if (repulsion != null)
            repulsionCombo.setValue(repulsion);
    }

    /**
     * Associe le bus de communication avec le moteur de graphe.
     *
     * @param bus bus de commandes
     */
    @Override
    public void setBus(CommandBus<GraphEngine> bus) {
        this.bus = bus;
    }
}
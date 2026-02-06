package com.mongraphe.graphui;

import java.io.File;

import com.jogamp.newt.javafx.NewtCanvasJFX;
import com.mongraphe.graphui.GraphData.NodeCommunity;
import com.mongraphe.graphui.GraphData.SimilitudeMode;
import com.mongraphe.graphui.data.GraphDataInitializer;
import com.mongraphe.graphui.interaction.GraphInteractionController;
import com.mongraphe.graphui.randerer.GraphRenderingContext;

import javafx.application.Platform;
import javafx.beans.property.DoubleProperty;
import javafx.beans.property.SimpleBooleanProperty;
import javafx.beans.property.SimpleDoubleProperty;
import javafx.beans.property.SimpleIntegerProperty;
import javafx.beans.property.SimpleStringProperty;
import javafx.beans.property.StringProperty;
import javafx.event.ActionEvent;
import javafx.fxml.FXML;
import javafx.scene.control.CheckBox;
import javafx.scene.control.ColorPicker;
import javafx.scene.control.ComboBox;
import javafx.scene.control.Label;
import javafx.scene.control.ProgressIndicator;
import javafx.scene.control.TableColumn;
import javafx.scene.control.TableView;
import javafx.scene.control.TextField;
import javafx.scene.control.Toggle;
import javafx.scene.control.ToggleGroup;
import javafx.scene.layout.AnchorPane;
import javafx.scene.layout.GridPane;
import javafx.scene.layout.Pane;
import javafx.scene.layout.StackPane;
import javafx.scene.paint.Color;

/**
 * GraphVue
 * --------
 *
 * Contrôleur JavaFX principal de l'application de visualisation de graphes.
 *
 * Rôle :
 * - gérer l'interface utilisateur (FXML),
 * - orchestrer l'initialisation du graphe,
 * - déléguer le rendu, les données et les interactions
 *   à des composants spécialisés.
 *
 * Cette classe NE FAIT PAS :
 * - le rendu OpenGL,
 * - la gestion des interactions bas niveau,
 * - les calculs lourds (JNI).
 */
public class GraphVue {

    /* ============================
       === FXML : vues & layout ===
       ============================ */

    @FXML private StackPane mainContentPane;
    @FXML private GridPane overviewPane;
    @FXML private AnchorPane dataPane;
    @FXML private AnchorPane previewPane;
    @FXML private StackPane graphContainer;

    @FXML private ToggleGroup viewToggleGroup;
    @FXML private ToggleGroup graphModeToggleGroup;

    /* ============================
       === FXML : statistiques ===
       ============================ */

    @FXML private Label nodesDeletedLabel;
    @FXML private Label nodesDisplayedLabel;
    @FXML private Label nodesHiddenLabel;
    @FXML private Label edgesDisplayedLabel;
    @FXML private Label edgesDeletedLabel;
    @FXML private Label totalElementsLabel;

    @FXML private Label recommendedTreshold;
    @FXML private Label recommendedAntiTreshold;
    @FXML private Label treshold;
    @FXML private Label antiTreshold;

    @FXML private Label idHovredVertexLabel;
    @FXML private Label XHovredVertexLabel;
    @FXML private Label YHovredVertexLabel;

    /* ============================
       === FXML : tables ===
       ============================ */

    @FXML private TableView<Vertex> vertexTable;
    @FXML private TableColumn<Vertex, Integer> vertexIdCol;
    @FXML private TableColumn<Vertex, Integer> vertexCommunityCol;
    @FXML private TableColumn<Vertex, Integer> vertexDegreeCol;
    @FXML private TableColumn<Vertex, Double> vertexXCol;
    @FXML private TableColumn<Vertex, Double> vertexYCol;
    @FXML private TableColumn<Vertex, Double> vertexDiameterCol;
    @FXML private TableColumn<Vertex, Boolean> vertexDeletedCol;

    @FXML private TableView<Edge> edgeTable;
    @FXML private TableColumn<Edge, Integer> edgeStartCol;
    @FXML private TableColumn<Edge, Integer> edgeEndCol;
    @FXML private TableColumn<Edge, Double> edgeWeightCol;

    /* ============================
       === FXML : options ===
       ============================ */

    @FXML private ProgressIndicator loadingIndicator;
    @FXML private ColorPicker canvasColorPicker;
    @FXML private CheckBox enableKmeans;

    @FXML private ComboBox<GraphData.RepulsionMode> repulsionModeComboBox;
    @FXML private ComboBox<GraphData.SimilitudeMode> mesureChamp;
    @FXML private ComboBox<GraphData.NodeCommunity> clusteringChamp;

    @FXML private TextField initNodeSize;
    @FXML private TextField degreeFactor;
    @FXML private TextField upScale;
    @FXML private TextField stabilizedTreshold;
    @FXML private TextField attractionTreshold;
    @FXML private TextField updatedFrequence;
    @FXML private TextField newFriction;
    @FXML private TextField attractionCoefficient;
    @FXML private TextField repulsionTreshold;
    @FXML private TextField newAmortissement;
    @FXML private TextField nbClusters;
    @FXML private TextField minimumDegree;

    /* ============================
       === Graphe & composants ===
       ============================ */

    private Graph graph;
    private Pane root;
    private NewtCanvasJFX newtCanvas;

    private GraphDataInitializer dataInitializer;
    private GraphRenderingContext renderingContext;
    private GraphInteractionController interactionController;

    /* ============================
       === Données d'entrée ===
       ============================ */

    private File fichier;
    private SimilitudeMode measureCode;
    private NodeCommunity methodCode;

    /* ============================
       === Initialisation JavaFX ===
       ============================ */

    @FXML
    private void initialize() {

        repulsionModeComboBox.getItems().setAll(GraphData.RepulsionMode.values());
        mesureChamp.getItems().setAll(GraphData.SimilitudeMode.values());
        clusteringChamp.getItems().setAll(GraphData.NodeCommunity.values());

        vertexIdCol.setCellValueFactory(d -> new SimpleIntegerProperty(d.getValue().getId()).asObject());
        vertexCommunityCol.setCellValueFactory(d -> new SimpleIntegerProperty(d.getValue().getCommunity().getId()).asObject());
        vertexDegreeCol.setCellValueFactory(d -> new SimpleIntegerProperty(d.getValue().getDegree()).asObject());
        vertexXCol.setCellValueFactory(d -> new SimpleDoubleProperty(d.getValue().getX()).asObject());
        vertexYCol.setCellValueFactory(d -> new SimpleDoubleProperty(d.getValue().getY()).asObject());
        vertexDiameterCol.setCellValueFactory(d -> new SimpleDoubleProperty(d.getValue().getDiameter()).asObject());
        vertexDeletedCol.setCellValueFactory(d -> new SimpleBooleanProperty(d.getValue().isDeleted()).asObject());

        edgeStartCol.setCellValueFactory(d -> new SimpleIntegerProperty(d.getValue().getStart().getId()).asObject());
        edgeEndCol.setCellValueFactory(d -> new SimpleIntegerProperty(d.getValue().getEnd().getId()).asObject());
        edgeWeightCol.setCellValueFactory(d -> new SimpleDoubleProperty(d.getValue().getWeight()).asObject());
    }

    /* ============================
       === Cycle de vie du graphe ===
       ============================ */

    @FXML
    private void handleStartButton() {

        loadingIndicator.setVisible(true);

        new Thread(() -> {

            graph = new Graph(this, graphContainer.getWidth(), graphContainer.getHeight());

            Platform.runLater(() -> {
                graphInit();
                graphContainer.getChildren().add(root);
                loadingIndicator.setVisible(false);
            });

        }).start();
    }

    private void graphInit() {

        root = new Pane();

        initializeGraph();

        dataInitializer = new GraphDataInitializer();
        dataInitializer.initialize(graph);

        renderingContext = new GraphRenderingContext();
        renderingContext.init(graph, graphContainer.getWidth(), graphContainer.getHeight());

        newtCanvas = renderingContext.getCanvas();
        root.getChildren().add(newtCanvas);

        interactionController = new GraphInteractionController(graph, this);

        updateTables();
    }

    private void initializeGraph() {

        graph.initGraphCsv(fichier.getAbsolutePath(), measureCode, methodCode);
        graph.setScreenSize(graphContainer.getWidth(), graphContainer.getHeight());

        applyBackgroundColor();

        graph.setUpscale(5);
        graph.setInitialNodeSize(3);
        graph.setDegreeScaleFactor(0.9);
    }

    private void applyBackgroundColor() {

        Color c = canvasColorPicker.getValue();
        if (c == null) return;

        graph.setBackgroundColor(
                (float) c.getRed(),
                (float) c.getGreen(),
                (float) c.getBlue()
        );
    }

    private void updateTables() {
        Platform.runLater(() -> {
            vertexTable.getItems().setAll(graph.vertices);
            edgeTable.getItems().setAll(graph.edges);
        });
    }

    /* ============================
       === Interactions UI ===
       ============================ */

	@FXML
	private void applyOptions(ActionEvent event) {
		applyOptions();
	}

	@FXML
	private void applyOptions() {
		try {

			if (!degreeFactor.getText().isEmpty()) {
				graph.setDegreeScaleFactor(Double.parseDouble(degreeFactor.getText()));
			}
			if (!initNodeSize.getText().isEmpty()) {
				graph.setInitialNodeSize(Double.parseDouble(initNodeSize.getText()));
			}
			if (!upScale.getText().isEmpty()) {
				graph.setUpscale(Integer.parseInt(upScale.getText()));
			}
			if (!stabilizedTreshold.getText().isEmpty()) {
				graph.setStabilizedThreshold(Double.parseDouble(stabilizedTreshold.getText()));
			}
			if (!attractionTreshold.getText().isEmpty()) {
				graph.setAttractionThreshold(Double.parseDouble(attractionTreshold.getText()));
			}
			if (!updatedFrequence.getText().isEmpty()) {
				graph.setUpdatedFrequence(Integer.parseInt(updatedFrequence.getText()));
			}
			if (!newFriction.getText().isEmpty()) {
				graph.setNewFriction(Double.parseDouble(newFriction.getText()));
			}
			if (!attractionCoefficient.getText().isEmpty()) {
				graph.setAttractionCoefficient(Double.parseDouble(attractionCoefficient.getText()));
			}
			if (!repulsionTreshold.getText().isEmpty()) {
				graph.setRepulsionThreshold(Double.parseDouble(repulsionTreshold.getText()));
			}
			if (!newAmortissement.getText().isEmpty()) {
				graph.setNewAmortissement(Double.parseDouble(newAmortissement.getText()));
			}
			if (!nbClusters.getText().isEmpty()) {
				graph.setNbClusters(Integer.parseInt(nbClusters.getText()));
			}
			if (!minimumDegree.getText().isEmpty()) {
				graph.setMinimumDegree(Integer.parseInt(minimumDegree.getText()));
			}

			GraphData.RepulsionMode mode = repulsionModeComboBox.getValue();
			if (mode != null) {
				graph.setRepulsionMode(mode);
			}

			treshold.setText(String.valueOf(graph.getThreshold()));
			antiTreshold.setText(String.valueOf(graph.getAntiThreshold()));

		} catch (NumberFormatException e) {
			System.err.println("Erreur de format : " + e.getMessage());
		}
	}


	@FXML
	private void applyChangement(ActionEvent event) {
		applyChangement();
	}

	@FXML
	private void applyChangement() {

		if (graph != null) {
			graph.stop();
		}

		if (mesureChamp.getValue() != null) {
			measureCode = mesureChamp.getValue();
		}

		if (clusteringChamp.getValue() != null) {
			methodCode = clusteringChamp.getValue();
		}

		graphContainer.getChildren().clear();
		if (root != null) {
			root.getChildren().clear();
		}

		graph = new Graph(this, graphContainer.getWidth(), graphContainer.getHeight());
		graphInit();
		graphContainer.getChildren().add(root);
	}

	@FXML
	private void resetGraphSettings(ActionEvent event) {
		resetGraphSettings();
	}

	@FXML
	private void resetGraphSettings() {
		// Réinitialisation future (laissé volontairement vide)
	}


	@FXML
	private void handleEnableKmeans(ActionEvent event) {
		if (graph != null) {
			graph.enableKmeans(enableKmeans.isSelected());
		}
	}





    @FXML
    private void handleApplyGraphMode(ActionEvent event) {

        Toggle selected = graphModeToggleGroup.getSelectedToggle();
        if (selected == null) return;

        try {
            GraphData.GraphMode mode =
                    GraphData.GraphMode.valueOf(selected.getUserData().toString());

            interactionController.applyGraphMode(mode);

        } catch (IllegalArgumentException e) {
            System.err.println("Mode invalide : " + selected.getUserData());
        }
    }

		/**
	 * Met à jour les statistiques affichées dans l'interface.
	 * Cette méthode est appelée par le moteur du graphe.
	 */
	public void updateNodeStats(int displayed,
								int hidden,
								int edgesDisplayed,
								int edgesDeleted,
								int deleted) {

		nodesDisplayedLabel.setText(String.valueOf(displayed));
		nodesHiddenLabel.setText(String.valueOf(hidden));
		this.edgesDisplayedLabel.setText(String.valueOf(edgesDisplayed));
		this.edgesDeletedLabel.setText(String.valueOf(edgesDeleted));
		nodesDeletedLabel.setText(String.valueOf(deleted));

		int total = displayed + hidden + edgesDisplayed + edgesDeleted + deleted;
		totalElementsLabel.setText(String.valueOf(total));
	}


    public void setHoveredVertex(Vertex vertex) {

        if (vertex == null) {
            idHovredVertexLabel.setText("/");
            XHovredVertexLabel.setText("/");
            YHovredVertexLabel.setText("/");
            return;
        }

        idHovredVertexLabel.setText(String.valueOf(vertex.getId()));
        XHovredVertexLabel.setText(String.valueOf(vertex.getX()));
        YHovredVertexLabel.setText(String.valueOf(vertex.getY()));
    }

    /* ============================
       === Navigation vues ===
       ============================ */

    @FXML
    private void handleViewChange(ActionEvent event) {

        Toggle selected = viewToggleGroup.getSelectedToggle();
        if (selected == null) return;

        String viewType = selected.getUserData().toString();

        overviewPane.setVisible("overview".equals(viewType));
        dataPane.setVisible("data".equals(viewType));
        previewPane.setVisible("preview".equals(viewType));

        if (renderingContext == null || root == null) return;

        if ("overview".equals(viewType)) {
            if (!root.getChildren().contains(newtCanvas)) {
                root.getChildren().setAll(newtCanvas);
            }
            renderingContext.resume();
        } else {
            root.getChildren().remove(newtCanvas);
            renderingContext.pause();
        }
    }

    /* ============================
       === Utilitaires ===
       ============================ */

    public void initData(File fichier,
                         SimilitudeMode measureCode,
                         double upThreshold,
                         double downThreshold,
                         NodeCommunity methodCode) {

        this.fichier = fichier;
        this.measureCode = measureCode;
        this.methodCode = methodCode;
    }

	@FXML
	private void handleAbout(ActionEvent event) {
		// À implémenter plus tard (boîte "À propos", etc.)
	}


    public void handleQuit(ActionEvent event) {
        if (graph != null) graph.stop();
        Platform.exit();
    }

    /* ============================
       === Propriétés globales ===
       ============================ */

    public static final DoubleProperty updateFrequency =
            new SimpleDoubleProperty(1.0);

    public static final StringProperty background_color =
            new SimpleStringProperty("#000000");
}

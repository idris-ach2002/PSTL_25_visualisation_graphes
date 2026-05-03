package com.mongraphe.graphui.controller;

import java.util.ArrayList;
import java.util.List;
import java.util.Properties;

import com.mongraphe.graphui.app.CommandBus;
import com.mongraphe.graphui.interaction.commands.SetBackgroundColorCommand;
import com.mongraphe.graphui.interaction.commands.SetEngineOptionsCommand;
import com.mongraphe.graphui.interfaces.CommandBusLinkedI;
import com.mongraphe.graphui.model.GraphModel;
import com.mongraphe.graphui.rendering.GraphEngine;
import com.mongraphe.graphui.rendering.GraphRenderOptions;
import com.mongraphe.graphui.rendering.GraphRenderer;
import com.mongraphe.graphui.view.GraphPanel;

import javafx.scene.Node;
import javafx.fxml.FXML;
import javafx.scene.control.Alert;
import javafx.scene.control.CheckBox;
import javafx.scene.control.ColorPicker;
import javafx.scene.control.ComboBox;
import javafx.scene.control.TextField;
import javafx.scene.layout.BorderPane;
import javafx.scene.paint.Color;

/**
 * Contrôleur JavaFX responsable du panneau de configuration et de
 * prévisualisation
 * de l'apparence du graphe.
 *
 * <p>
 * Cette classe permet à l'utilisateur de modifier diverses options visuelles et
 * structurelles :
 * taille des nœuds, épaisseur et courbure des arêtes, couleurs de fond et des
 * nœuds, ainsi que
 * les critères de filtrage (degré minimum, poids minimum).
 * </p>
 *
 * <h2>Fonctionnement</h2>
 * <ul>
 * <li>Récupère les saisies utilisateur via les composants JavaFX (TextField,
 * ColorPicker, etc.).</li>
 * <li>Valide les données saisies (vérification des nombres positifs, formats
 * corrects).</li>
 * <li>Met à jour un moteur de rendu d'aperçu (Preview) en temps réel.</li>
 * <li>Envoie des commandes via le {@link CommandBus} pour appliquer les
 * modifications au graphe principal.</li>
 * <li>Permet la sauvegarde et le chargement de ces paramètres via l'objet
 * {@link Properties}.</li>
 * </ul>
 */
public class PreviewController implements CommandBusLinkedI<GraphEngine> {

    /** Angle de courbure par défaut pour les arêtes incurvées (en degrés). */
    private static final double DEFAULT_CURVE_ANGLE = 22d;

    /** Bus de commandes pour interagir avec le moteur de graphe principal. */
    private CommandBus<GraphEngine> bus;

    /** Moteur de rendu dédié exclusivement à la zone de prévisualisation. */
    private GraphRenderer previewRenderer;

    private GraphPanel previewPanel;

    @FXML
    private TextField degreeFactor;
    @FXML
    private TextField initNodeSize;

    @FXML
    private TextField minimumDegree;
    @FXML
    private TextField minEdgeWeight;

    @FXML
    private TextField edgeLineWidthField;

    @FXML
    private CheckBox curvedEdgesToggle;
    @FXML
    private TextField curveAngleField;

    @FXML
    private BorderPane graphContainer;

    @FXML
    private ComboBox<GraphModel.ColoringMode> coloringModeCombo;
    @FXML
    private ColorPicker canvasColorPicker;
    @FXML
    private ColorPicker uniformNodeColorPicker;

    /**
     * Méthode d'initialisation appelée automatiquement par JavaFX après le
     * chargement du fichier FXML.
     * <p>
     * Elle configure les valeurs par défaut des composants (couleurs, mode de
     * coloration,
     * état des arêtes incurvées) et met en place les écouteurs d'événements de
     * base.
     * </p>
     */
    @FXML
    private void initialize() {
        if (coloringModeCombo != null) {
            coloringModeCombo.getItems().setAll(GraphModel.ColoringMode.values());
            coloringModeCombo.getSelectionModel().select(GraphModel.ColoringMode.COMMUNITY);
        }

        if (canvasColorPicker != null) {
            canvasColorPicker.setValue(Color.WHITE);
        }
        if (uniformNodeColorPicker != null) {
            uniformNodeColorPicker.setValue(Color.web("#d1d1db"));
        }

        if (curvedEdgesToggle != null) {
            curvedEdgesToggle.setSelected(true);
            curvedEdgesToggle.selectedProperty()
                    .addListener((obs, oldValue, selected) -> updateCurveAngleFieldState(selected));
        }
        if (curveAngleField != null) {
            curveAngleField.setText(formatAngle(DEFAULT_CURVE_ANGLE));
        }
        updateCurveAngleFieldState(curvedEdgesToggle == null || curvedEdgesToggle.isSelected());
    }

    /**
     * Associe le bus de commandes au contrôleur.
     *
     * @param bus Le bus de commandes permettant d'accéder et de modifier le
     *            {@link GraphEngine}.
     */
    @Override
    public void setBus(CommandBus<GraphEngine> bus) {
        this.bus = bus;
    }

    /**
     * Définit le moteur de rendu utilisé pour la prévisualisation locale des
     * paramètres.
     * Applique immédiatement les options de rendu actuelles à ce moteur.
     *
     * @param previewRenderer L'instance du renderer d'aperçu.
     */
    public void setPreviewRenderer(GraphRenderer previewRenderer) {
        this.previewRenderer = previewRenderer;
        applyPreviewRenderOptions();
    }

    /**
     * Intègre le composant d'affichage JavaFX/OpenGLFX dans l'interface
     * JavaFX.
     *
     * @param panel le nœud JavaFX encapsulant le canvas de rendu du graphe.
     */
    public void setGraphPanel(GraphPanel panel) {
        this.previewPanel = panel;
        if (graphContainer != null) {
            graphContainer.setCenter(panel == null ? null : panel.canvas());
            graphContainer.widthProperty().addListener((obs, oldValue, newValue) -> requestPreviewFrame());
            graphContainer.heightProperty().addListener((obs, oldValue, newValue) -> requestPreviewFrame());
            graphContainer.visibleProperty().addListener((obs, oldValue, visible) -> {
                if (visible) requestPreviewFrame();
            });
        }
        requestPreviewFrame();
    }

    public void setGraphPanel(Node panel) {
        this.previewPanel = null;
        if (graphContainer != null) {
            graphContainer.setCenter(panel);
        }
    }

    public void clearGraphPanel() {
        if (graphContainer != null) {
            graphContainer.setCenter(null);
        }
    }

    /**
     * Action déclenchée depuis l'interface (par exemple, clic sur le bouton
     * "Appliquer").
     * Applique les options avec possibilité d'annulation (undoable).
     */
    @FXML
    private void applyOptions() {
        applyCurrentOptions(true);
    }

    /**
     * Lit, valide et applique les options de configuration saisies par
     * l'utilisateur.
     * <p>
     * Si les champs contiennent des valeurs invalides, une alerte est affichée.
     * Sinon, les commandes appropriées sont envoyées au moteur de graphe.
     * </p>
     *
     * @param undoable {@code true} si l'action doit être enregistrée dans
     *                 l'historique (UndoManager), {@code false} pour une exécution
     *                 directe synchrone.
     */
    public void applyCurrentOptions(boolean undoable) {
        if (bus == null) {
            applyPreviewRenderOptions();
            return;
        }

        List<String> errors = new ArrayList<>();
        EngineOptions options = readOptions(errors);
        Double previewCurveAngle = readCurveAngle(errors);

        if (!errors.isEmpty()) {
            alert("Valeurs invalides", String.join("\n", errors));
            return;
        }

        if (undoable) {
            bus.dispatch(new SetEngineOptionsCommand(options));
        } else {
            bus.dispatchSyncVoid(engine -> new SetEngineOptionsCommand(options).execute(engine));
        }

        applyAppearance(previewCurveAngle);
    }

    /**
     * Applique les modifications liées à l'apparence générale (couleur de fond,
     * mode de coloration, couleur uniforme des nœuds) sans préciser d'angle de
     * courbure.
     */
    public void applyAppearance() {
        applyAppearance(null);
    }

    /**
     * Applique les modifications d'apparence au moteur principal et met à jour
     * l'aperçu.
     *
     * @param previewCurveAngle L'angle de courbure à appliquer pour l'aperçu (si
     *                          non nul).
     */
    private void applyAppearance(Double previewCurveAngle) {
        if (bus != null) {
            Color bg = canvasColorPicker == null ? null : canvasColorPicker.getValue();
            if (bg != null) {
                bus.dispatch(engine -> new SetBackgroundColorCommand(
                        (float) bg.getRed(),
                        (float) bg.getGreen(),
                        (float) bg.getBlue(),
                        (float) bg.getOpacity()).execute(engine));
            }

            GraphModel.ColoringMode coloringMode = coloringModeCombo == null ? null : coloringModeCombo.getValue();
            if (coloringMode != null) {
                bus.dispatch(engine -> engine.setColoringMode(coloringMode));
            }

            Color uniform = uniformNodeColorPicker == null ? null : uniformNodeColorPicker.getValue();
            if (uniform != null) {
                bus.dispatch(engine -> engine.setUniformNodeColor(
                        (float) uniform.getRed(),
                        (float) uniform.getGreen(),
                        (float) uniform.getBlue()));
            }
        }

        applyPreviewRenderOptions(previewCurveAngle);
    }

    /**
     * Met à jour les options de rendu de l'aperçu en lisant l'angle de courbure
     * depuis le champ texte.
     */
    private void applyPreviewRenderOptions() {
        List<String> errors = new ArrayList<>();
        Double previewCurveAngle = readCurveAngle(errors);
        if (!errors.isEmpty()) {
            return;
        }
        applyPreviewRenderOptions(previewCurveAngle);
    }

    /**
     * Construit et applique l'objet {@link GraphRenderOptions} au moteur de
     * prévisualisation,
     * gérant ainsi l'épaisseur des traits et la courbure des arêtes.
     *
     * @param previewCurveAngle L'angle de courbure spécifié.
     */
    private void applyPreviewRenderOptions(Double previewCurveAngle) {
        if (previewRenderer == null) {
            return;
        }

        boolean curved = curvedEdgesToggle == null || curvedEdgesToggle.isSelected();
        double effectiveAngle = previewCurveAngle == null ? DEFAULT_CURVE_ANGLE : previewCurveAngle.doubleValue();

        // Récupérer l'épaisseur des lignes
        float lineWidth = 1.35f;
        if (edgeLineWidthField != null) {
            try {
                lineWidth = Float.parseFloat(edgeLineWidthField.getText().trim());
                if (lineWidth < 0.5f)
                    lineWidth = 0.5f;
            } catch (NumberFormatException ignored) {
            }
        }

        // Construire les options de rendu avec l'épaisseur personnalisée
        GraphRenderOptions options;
        if (curved) {
            options = new GraphRenderOptions(
                    GraphRenderOptions.EdgeStyle.CURVED_PARABOLIC,
                    20,
                    (float) effectiveAngle,
                    64f,
                    lineWidth,
                    true);
        } else {
            options = new GraphRenderOptions(
                    GraphRenderOptions.EdgeStyle.STRAIGHT,
                    1, 0f, 0f, lineWidth, false);
        }

        previewRenderer.setRenderOptions(options);
        requestPreviewFrame();
    }

    private void requestPreviewFrame() {
        if (previewPanel != null) {
            previewPanel.forceRenderBurst();
        }
    }

    /**
     * Extrait l'état actuel des composants de l'interface et les sauvegarde dans un
     * objet {@link Properties}.
     *
     * @param p L'objet Properties dans lequel stocker les paramètres.
     */
    public void fillProperties(Properties p) {
        put(p, "degreeFactor", textOf(degreeFactor));
        put(p, "initNodeSize", textOf(initNodeSize));
        put(p, "minimumDegree", textOf(minimumDegree));
        put(p, "minEdgeWeight", textOf(minEdgeWeight));
        put(p, "previewCurveAngle", textOf(curveAngleField));
        put(p, "edgeLineWidth", textOf(edgeLineWidthField));

        if (curvedEdgesToggle != null) {
            p.setProperty("previewCurvedEdges", Boolean.toString(curvedEdgesToggle.isSelected()));
        }
        if (coloringModeCombo != null && coloringModeCombo.getValue() != null) {
            p.setProperty("coloringMode", coloringModeCombo.getValue().name());
        }
        if (canvasColorPicker != null && canvasColorPicker.getValue() != null) {
            p.setProperty("background", canvasColorPicker.getValue().toString());
        }
        if (uniformNodeColorPicker != null && uniformNodeColorPicker.getValue() != null) {
            p.setProperty("uniformNodeColor", uniformNodeColorPicker.getValue().toString());
        }
    }

    /**
     * Restaure l'état des composants de l'interface à partir des valeurs contenues
     * dans un objet {@link Properties}.
     *
     * @param p L'objet Properties contenant les paramètres sauvegardés.
     */
    public void loadFromProperties(Properties p) {
        setTextIfPresent(degreeFactor, p.getProperty("degreeFactor"));
        setTextIfPresent(initNodeSize, p.getProperty("initNodeSize"));
        setTextIfPresent(minimumDegree, p.getProperty("minimumDegree"));
        setTextIfPresent(minEdgeWeight, p.getProperty("minEdgeWeight"));
        setTextIfPresent(curveAngleField, p.getProperty("previewCurveAngle", formatAngle(DEFAULT_CURVE_ANGLE)));
        setTextIfPresent(edgeLineWidthField, p.getProperty("edgeLineWidth", "1"));

        if (curvedEdgesToggle != null) {
            curvedEdgesToggle.setSelected(Boolean.parseBoolean(p.getProperty("previewCurvedEdges", "true")));
        }
        updateCurveAngleFieldState(curvedEdgesToggle == null || curvedEdgesToggle.isSelected());

        setEnumValue(coloringModeCombo, p.getProperty("coloringMode"), GraphModel.ColoringMode.class);

        String background = p.getProperty("background");
        if (canvasColorPicker != null && background != null && !background.isBlank()) {
            canvasColorPicker.setValue(Color.valueOf(background));
        }
        String uniform = p.getProperty("uniformNodeColor");
        if (uniformNodeColorPicker != null && uniform != null && !uniform.isBlank()) {
            uniformNodeColorPicker.setValue(Color.valueOf(uniform));
        }

        applyPreviewRenderOptions();
    }

    /**
     * Assigne une valeur d'énumération à une ComboBox à partir d'une chaîne de
     * caractères.
     */
    private <E extends Enum<E>> void setEnumValue(ComboBox<E> comboBox, String name, Class<E> enumClass) {
        if (comboBox == null || name == null || name.isBlank()) {
            return;
        }
        try {
            comboBox.setValue(Enum.valueOf(enumClass, name));
        } catch (IllegalArgumentException ignored) {
        }
    }

    /**
     * Ajoute une paire clé-valeur dans les Properties, si la valeur n'est pas
     * nulle.
     */
    private void put(Properties p, String key, String value) {
        if (value != null) {
            p.setProperty(key, value);
        }
    }

    /**
     * Lit, analyse et valide les champs de saisie pour construire l'objet des
     * options du moteur.
     *
     * @param errors Liste dans laquelle ajouter les messages d'erreur de
     *               validation.
     * @return Une instance contenant les options configurées par l'utilisateur.
     */
    private EngineOptions readOptions(List<String> errors) {
        EngineOptions options = new EngineOptions();
        options.degreeFactor = parseDoubleOrNull(degreeFactor, errors, "Agrandissement par degré");
        options.initialNodeSize = parseDoubleOrNull(initNodeSize, errors, "Taille d'un nœud");
        options.minimumDegree = parseIntOrNull(minimumDegree, errors, "Degré minimum");
        options.minEdgeWeight = parseDoubleOrNull(minEdgeWeight, errors, "Poids min arêtes");

        validateStrictlyPositive(initNodeSize, options.initialNodeSize, errors, "Taille d'un nœud");
        validateNonNegative(degreeFactor, options.degreeFactor, errors, "Agrandissement par degré");
        validateNonNegative(minimumDegree, options.minimumDegree, errors, "Degré minimum");
        validateNonNegative(minEdgeWeight, options.minEdgeWeight, errors, "Poids min arêtes");
        return options;
    }

    /**
     * Lit et valide l'angle de courbure renseigné.
     *
     * @param errors Liste dans laquelle ajouter les messages d'erreur.
     * @return L'angle de courbure validé (entre 0 et 85), ou la valeur par défaut.
     */
    private Double readCurveAngle(List<String> errors) {
        if (curveAngleField == null) {
            return DEFAULT_CURVE_ANGLE;
        }

        Double angle = parseDoubleOrNull(curveAngleField, errors, "Angle de courbure");
        if (angle == null) {
            clearInvalid(curveAngleField);
            return DEFAULT_CURVE_ANGLE;
        }
        if (angle >= 0d && angle <= 85d) {
            clearInvalid(curveAngleField);
            return angle;
        }
        markInvalid(curveAngleField, errors, "Angle de courbure", String.valueOf(angle));
        return null;
    }

    /**
     * Vérifie qu'une valeur numérique est strictement supérieure à zéro.
     */
    private void validateStrictlyPositive(TextField field, Number value, List<String> errors, String label) {
        if (field == null || value == null) {
            return;
        }
        if (value.doubleValue() > 0d) {
            clearInvalid(field);
            return;
        }
        markInvalid(field, errors, label, String.valueOf(value));
    }

    /**
     * Vérifie qu'une valeur numérique est positive ou nulle.
     */
    private void validateNonNegative(TextField field, Number value, List<String> errors, String label) {
        if (field == null || value == null) {
            return;
        }
        if (value.doubleValue() >= 0d) {
            clearInvalid(field);
            return;
        }
        markInvalid(field, errors, label, String.valueOf(value));
    }

    /**
     * Parse le texte d'un champ en Double, en gérant la conversion de virgule en
     * point.
     */
    private Double parseDoubleOrNull(TextField field, List<String> errors, String label) {
        String value = normalize(field);
        if (value == null) {
            return null;
        }
        try {
            clearInvalid(field);
            return Double.parseDouble(value.replace(',', '.'));
        } catch (NumberFormatException e) {
            markInvalid(field, errors, label, value);
            return null;
        }
    }

    /**
     * Parse le texte d'un champ en Integer.
     */
    private Integer parseIntOrNull(TextField field, List<String> errors, String label) {
        String value = normalize(field);
        if (value == null) {
            return null;
        }
        try {
            clearInvalid(field);
            return Integer.parseInt(value);
        } catch (NumberFormatException e) {
            markInvalid(field, errors, label, value);
            return null;
        }
    }

    /**
     * Nettoie la valeur texte d'un champ en supprimant les espaces inutiles.
     *
     * @return La chaîne nettoyée, ou null si le champ est vide.
     */
    private String normalize(TextField field) {
        if (field == null) {
            return null;
        }
        String value = field.getText();
        if (value == null) {
            clearInvalid(field);
            return null;
        }
        value = value.trim();
        if (value.isEmpty()) {
            clearInvalid(field);
            return null;
        }
        return value;
    }

    /**
     * Signale visuellement une erreur sur un champ (bordure rouge) et ajoute le
     * message aux erreurs.
     */
    private void markInvalid(TextField field, List<String> errors, String label, String value) {
        if (field != null) {
            field.setStyle("-fx-border-color: #d33; -fx-border-width: 2;");
        }
        errors.add(label + " : valeur invalide ('" + value + "')");
    }

    /**
     * Supprime le signalement visuel d'erreur d'un champ.
     */
    private void clearInvalid(TextField field) {
        if (field != null) {
            field.setStyle("");
        }
    }

    /**
     * Active ou désactive le champ de saisie de l'angle de courbure en fonction de
     * l'état de la case à cocher.
     */
    private void updateCurveAngleFieldState(boolean curvedEnabled) {
        if (curveAngleField == null) {
            return;
        }
        curveAngleField.setDisable(!curvedEnabled);
        if (curvedEnabled) {
            curveAngleField.setStyle("");
        }
    }

    /**
     * Récupère le texte d'un champ de manière sécurisée (gère le cas null).
     */
    private String textOf(TextField field) {
        return field == null ? null : field.getText();
    }

    /**
     * Affecte un texte à un champ s'il est présent (non null).
     */
    private void setTextIfPresent(TextField field, String value) {
        if (field != null && value != null) {
            field.setText(value);
        }
    }

    /**
     * Formate un angle pour affichage : retire la décimale si c'est un entier
     * parfait.
     */
    private String formatAngle(double angle) {
        if (Math.rint(angle) == angle) {
            return Integer.toString((int) angle);
        }
        return Double.toString(angle);
    }

    /**
     * Affiche une boîte de dialogue d'avertissement JavaFX contenant des messages
     * d'erreur.
     *
     * @param title   Le titre de la fenêtre d'alerte.
     * @param content Le détail des erreurs à afficher à l'utilisateur.
     */
    private void alert(String title, String content) {
        Alert alert = new Alert(Alert.AlertType.WARNING);
        alert.setTitle(title);
        alert.setHeaderText(null);
        alert.setContentText(content);
        alert.showAndWait();
    }
}
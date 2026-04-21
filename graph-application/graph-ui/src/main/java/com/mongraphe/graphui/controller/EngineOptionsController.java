package com.mongraphe.graphui.controller;

import java.util.ArrayList;
import java.util.List;

import com.mongraphe.graphui.app.CommandBus;
import com.mongraphe.graphui.interaction.commands.SetEngineOptionsCommand;
import com.mongraphe.graphui.interfaces.CommandBusLinkedI;
import com.mongraphe.graphui.rendering.GraphEngine;

import javafx.fxml.FXML;
import javafx.scene.control.*;
import javafx.util.StringConverter;

/**
 * Contrôleur JavaFX responsable de la gestion des options du moteur de graphe.
 *
 * <p>
 * Cette classe permet à l'utilisateur de configurer différents paramètres
 * physiques utilisés par le {@link GraphEngine}. Les valeurs sont saisies
 * dans l'interface graphique puis validées avant d'être appliquées au moteur.
 * </p>
 *
 * <h2>Responsabilités</h2>
 * <ul>
 * <li>Lire les paramètres saisis dans l'interface utilisateur.</li>
 * <li>Valider les valeurs numériques entrées.</li>
 * <li>Signaler visuellement les champs invalides.</li>
 * <li>Construire un objet {@code EngineOptions} contenant les paramètres.</li>
 * <li>Envoyer une commande au moteur via le {@link CommandBus}.</li>
 * </ul>
 *
 * <h2>Architecture</h2>
 * <p>
 * Ce contrôleur s'intègre dans une architecture basée sur le pattern
 * <b>Command</b>. Les modifications du moteur sont encapsulées dans
 * des commandes telles que {@link SetEngineOptionsCommand}, permettant
 * notamment l'intégration d'un système d'annulation (undo/redo).
 * </p>
 *
 * <h2>Paramètres configurables</h2>
 * <ul>
 * <li>Friction</li>
 * <li>Coefficient d'attraction</li>
 * <li>Coefficient de répulsion intra-zone</li>
 * <li>Répulsion anti-arêtes</li>
 * <li>Amortissement</li>
 * <li>Mode de calcul de la répulsion</li>
 * </ul>
 */
public final class EngineOptionsController
        implements CommandBusLinkedI<GraphEngine> {

    /**
     * Bus de commandes permettant d'envoyer des actions au moteur de graphe.
     */
    private CommandBus<GraphEngine> bus;

    /** Champ de saisie pour le coefficient de friction. */
    @FXML
    private TextField frictionField;

    /** Champ de saisie pour le coefficient d'attraction entre les sommets. */
    @FXML
    private TextField attractionCoeffField;

    /** Champ de saisie pour le coefficient de répulsion intra-zone. */
    @FXML
    private TextField repulsionCoeffField;

    /** Champ de saisie pour la répulsion anti-arêtes. */
    @FXML
    private TextField antiRepulsionField;

    /** Champ de saisie pour le coefficient d'amortissement. */
    @FXML
    private TextField amortissementField;

    /** Sélecteur du mode de calcul de la répulsion. */
    @FXML
    private ComboBox<Integer> repulsionModeCombo;

    /** Champ de saisie pour la fréquence d'exécution de la simulation (Hz). */
    @FXML
    private TextField simulationRateField;

    /**
     * Associe le bus de commandes au contrôleur.
     *
     * @param bus bus utilisé pour transmettre les commandes au moteur
     */
    @Override
    public void setBus(CommandBus<GraphEngine> bus) {
        this.bus = bus;
        if (bus != null && simulationRateField != null) {
            Integer hz = bus.dispatchSync(GraphEngine::getSimulationTicksPerSecond);
            simulationRateField.setPromptText(String.valueOf(hz));
            simulationRateField.setText(String.valueOf(hz));
        }
    }

    /**
     * Méthode appelée automatiquement par le chargeur FXML après
     * l'initialisation du contrôleur.
     *
     * <p>
     * Elle initialise la liste des modes de répulsion et configure
     * leur représentation textuelle dans la {@link ComboBox}.
     * </p>
     */
    @FXML
    private void initialize() {
        repulsionModeCombo.getItems().addAll(0, 1, 2);
        repulsionModeCombo.setConverter(new StringConverter<>() {
            @Override
            public String toString(Integer mode) {
                return switch (mode) {
                    case 0 -> "Pondéré par degrés";
                    case 1 -> "Uniforme";
                    case 2 -> "Renforcé inter-communautés";
                    default -> "";
                };
            }

            @Override
            public Integer fromString(String s) {
                return null;
            }
        });
        repulsionModeCombo.getSelectionModel().selectFirst();
    }

    /**
     * Action déclenchée par l'utilisateur pour appliquer
     * les paramètres configurés dans l'interface.
     */
    @FXML
    private void applyOptions() {
        applyCurrentOptions(true);
    }

    /**
     * Applique les options actuellement saisies dans l'interface.
     *
     * <p>
     * Les valeurs sont d'abord validées. Si des erreurs sont détectées,
     * un message d'avertissement est affiché à l'utilisateur.
     * </p>
     *
     * <p>
     * Si {@code undoable} est vrai, la modification est envoyée sous
     * forme de commande afin d'être enregistrée dans l'historique.
     * </p>
     *
     * @param undoable indique si l'action doit être enregistrée
     *                 dans l'historique des commandes
     */
    public void applyCurrentOptions(boolean undoable) {
        if (bus == null)
            return;

        List<String> errors = new ArrayList<>();
        EngineOptions options = readOptions(errors);

        if (!errors.isEmpty()) {
            alert("Valeurs invalides", String.join("\n", errors));
            return;
        }

        if (undoable) {
            bus.dispatch(new SetEngineOptionsCommand(options));
        } else {
            bus.dispatchSyncVoid(engine -> new SetEngineOptionsCommand(options).execute(engine));
        }
    }

    /**
     * Lit les valeurs présentes dans les champs de saisie et
     * construit un objet {@code EngineOptions}.
     *
     * @param errors liste destinée à accumuler les messages d'erreur
     * @return objet contenant les options du moteur
     */
    private EngineOptions readOptions(List<String> errors) {
        EngineOptions options = new EngineOptions();
        options.friction = parsePositiveDouble(frictionField, errors, "Friction");
        options.attractionCoefficient = parseNonNegativeDouble(attractionCoeffField, errors, "Attraction");
        options.repulsionCoeff = parseNonNegativeDouble(repulsionCoeffField, errors, "Répulsion intra-zone");
        options.antiRepulsion = parseNonNegativeDouble(antiRepulsionField, errors, "Répulsion anti-arêtes");
        options.amortissement = parsePositiveDouble(amortissementField, errors, "Amortissement");
        options.repulsionMode = repulsionModeCombo.getValue();
        options.simulationTicksPerSecond = parseBoundedInteger(simulationRateField, errors,
                "Fréquence de simulation", 1, 240);
        return options;
    }

    /**
     * Analyse une valeur numérique devant être strictement positive.
     *
     * @param field  champ contenant la valeur
     * @param errors liste d'erreurs
     * @param label  nom du paramètre
     * @return valeur positive ou {@code null} si invalide
     */
    private Double parsePositiveDouble(TextField field, List<String> errors, String label) {
        Double value = parseDoubleOrNull(field, errors, label);
        if (value != null && value <= 0) {
            markInvalid(field, errors, label, String.valueOf(value));
            return null;
        }
        return value;
    }

    /**
     * Analyse une valeur numérique qui ne peut pas être négative.
     *
     * @param field  champ contenant la valeur
     * @param errors liste d'erreurs
     * @param label  nom du paramètre
     * @return valeur valide ou {@code null}
     */
    private Double parseNonNegativeDouble(TextField field, List<String> errors, String label) {
        Double value = parseDoubleOrNull(field, errors, label);
        if (value != null && value < 0) {
            markInvalid(field, errors, label, String.valueOf(value));
            return null;
        }
        return value;
    }

    /**
     * Tente de convertir le contenu d'un champ texte en nombre réel.
     *
     * <p>
     * Les séparateurs décimaux '.' et ',' sont acceptés.
     * </p>
     *
     * @param field  champ de saisie
     * @param errors liste d'erreurs
     * @param label  nom du paramètre
     * @return valeur numérique ou {@code null}
     */

    /**
     * Analyse une valeur entière bornée entre deux limites incluses.
     *
     * @param field  champ contenant la valeur
     * @param errors liste d'erreurs
     * @param label  nom du paramètre
     * @param min    borne minimale incluse
     * @param max    borne maximale incluse
     * @return valeur entière valide ou {@code null}
     */
    private Integer parseBoundedInteger(TextField field, List<String> errors, String label, int min, int max) {
        String value = normalize(field);
        if (value == null) {
            return null;
        }
        try {
            clearInvalid(field);
            int parsed = Integer.parseInt(value);
            if (parsed < min || parsed > max) {
                markInvalid(field, errors, label, value + " (attendu entre " + min + " et " + max + ")");
                return null;
            }
            return parsed;
        } catch (NumberFormatException e) {
            markInvalid(field, errors, label, value);
            return null;
        }
    }

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
     * Nettoie et normalise la valeur saisie dans un champ texte.
     *
     * @param field champ à analyser
     * @return chaîne nettoyée ou {@code null} si vide
     */
    private String normalize(TextField field) {
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
     * Marque un champ comme invalide et ajoute un message d'erreur.
     *
     * @param field  champ concerné
     * @param errors liste d'erreurs
     * @param label  nom du paramètre
     * @param value  valeur incorrecte
     */
    private void markInvalid(TextField field, List<String> errors, String label, String value) {
        field.setStyle("-fx-border-color: #d33; -fx-border-width: 2;");
        errors.add(label + " : valeur invalide ('" + value + "')");
    }

    /**
     * Supprime le marquage visuel d'erreur d'un champ.
     *
     * @param field champ à réinitialiser
     */
    private void clearInvalid(TextField field) {
        field.setStyle("");
    }

    /**
     * Affiche une boîte de dialogue d'avertissement.
     *
     * @param title   titre de la fenêtre
     * @param content message affiché
     */
    private void alert(String title, String content) {
        Alert alert = new Alert(Alert.AlertType.WARNING);
        alert.setTitle(title);
        alert.setHeaderText(null);
        alert.setContentText(content);
        alert.showAndWait();
    }
}
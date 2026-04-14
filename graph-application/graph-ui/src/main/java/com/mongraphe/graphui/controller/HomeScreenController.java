package com.mongraphe.graphui.controller;

import java.io.File;

import javafx.fxml.FXML;
import javafx.fxml.FXMLLoader;
import javafx.geometry.Rectangle2D;
import javafx.scene.Parent;
import javafx.scene.Scene;
import javafx.scene.control.Alert;
import javafx.scene.control.Hyperlink;
import javafx.stage.FileChooser;
import javafx.stage.Screen;
import javafx.stage.Stage;

/**
 * Contrôleur JavaFX de l'écran d'accueil de l'application.
 *
 * <p>
 * Ce contrôleur gère les interactions utilisateur sur la vue d'accueil,
 * notamment l'ouverture de projets existants ou d'exemples fournis avec
 * l'application. Il permet également de charger la vue principale
 * permettant l'affichage et la manipulation d'un graphe.
 * </p>
 *
 * <h2>Responsabilités</h2>
 * <ul>
 * <li>Gérer les actions utilisateur de l'écran d'accueil.</li>
 * <li>Permettre l'ouverture de fichiers de graphe depuis le système.</li>
 * <li>Permettre l'ouverture d'exemples fournis avec l'application.</li>
 * <li>Charger la vue principale du visualiseur de graphe.</li>
 * <li>Gérer l'affichage des messages d'erreur liés au chargement.</li>
 * </ul>
 *
 * <h2>Formats de fichiers supportés</h2>
 * <ul>
 * <li><b>.csv</b> : fichiers de graphe sous forme tabulaire</li>
 * <li><b>.dot</b> : format GraphViz</li>
 * <li><b>.mongraphe</b> : format natif de l'application</li>
 * </ul>
 *
 * <h2>Fonctionnement général</h2>
 * <ol>
 * <li>L'utilisateur choisit d'ouvrir un fichier ou un exemple.</li>
 * <li>Le fichier est vérifié afin de s'assurer que son format est
 * supporté.</li>
 * <li>La vue principale {@code MainView.fxml} est chargée.</li>
 * <li>Le contrôleur {@code MainGraphController} reçoit le fichier à
 * ouvrir.</li>
 * <li>La fenêtre principale est redimensionnée à la taille de l'écran.</li>
 * </ol>
 */
public final class HomeScreenController {

    /** Lien permettant de créer ou ouvrir un nouveau projet. */
    @FXML
    private Hyperlink newProjectLink;

    /** Lien permettant d'ouvrir un fichier existant. */
    @FXML
    private Hyperlink openFileLink;

    /** Lien vers le premier fichier exemple. */
    @FXML
    private Hyperlink example1;

    /** Lien vers le second fichier exemple. */
    @FXML
    private Hyperlink example2;

    /** Lien vers le troisième fichier exemple. */
    @FXML
    private Hyperlink example3;

    /** Lien vers le quatrième fichier exemple. */
    @FXML
    private Hyperlink example4;

    /**
     * Méthode appelée automatiquement après le chargement du fichier FXML.
     *
     * <p>
     * Elle associe les actions aux liens permettant d'ouvrir les
     * différents exemples fournis avec l'application.
     * </p>
     */
    @FXML
    public void initialize() {
        if (example1 != null)
            example1.setOnAction(e -> openSample(example1.getText()));
        if (example2 != null)
            example2.setOnAction(e -> openSample(example2.getText()));
        if (example3 != null)
            example3.setOnAction(e -> openSample(example3.getText()));
        if (example4 != null)
            example4.setOnAction(e -> openSample(example4.getText()));
    }

    /**
     * Action déclenchée lorsque l'utilisateur choisit de créer
     * un nouveau projet.
     *
     * <p>
     * Dans l'état actuel de l'application, cette action redirige
     * vers le même comportement que l'ouverture d'un projet.
     * </p>
     */
    @FXML
    void handleNewProject() {
        handleOpenProject();
    }

    /**
     * Ouvre un sélecteur de fichier permettant à l'utilisateur
     * de choisir un fichier de graphe à charger.
     *
     * <p>
     * Seuls certains formats sont proposés par défaut dans la
     * boîte de dialogue.
     * </p>
     */
    @FXML
    void handleOpenProject() {
        FileChooser chooser = new FileChooser();
        chooser.setTitle("Ouvrir un fichier de graphe");
        chooser.getExtensionFilters().addAll(
                new FileChooser.ExtensionFilter("Projets MonGraphe", "*.mongraphe"),
                new FileChooser.ExtensionFilter("Fichiers DOT / CSV", "*.dot", "*.csv"),
                new FileChooser.ExtensionFilter("Tous les fichiers", "*.*"));

        File file = chooser.showOpenDialog(getStage());
        if (file != null) {
            openGraphWindow(getStage(), file);
        }
    }

    /**
     * Ouvre un fichier exemple situé dans le dossier des exemples.
     *
     * <p>
     * La méthode tente d'abord de trouver le fichier dans
     * {@code samples/}, puis dans {@code graph-ui/samples/}.
     * </p>
     *
     * @param fileName nom du fichier exemple à ouvrir
     */
    private void openSample(String fileName) {
        File f = new File("samples/" + fileName);
        if (!f.exists()) {
            f = new File("graph-ui/samples/" + fileName);
        }
        if (!f.exists()) {
            alert(Alert.AlertType.ERROR, "Fichier introuvable",
                    "Le fichier d'exemple est introuvable :\n" + f.getAbsolutePath());
            return;
        }
        openGraphWindow(getStage(), f);
    }

    /**
     * Récupère la fenêtre principale de l'application.
     *
     * <p>
     * La méthode tente de récupérer la fenêtre à partir
     * des éléments de l'interface présents dans la scène.
     * </p>
     *
     * @return fenêtre principale de l'application
     * @throws IllegalStateException si la fenêtre ne peut pas être déterminée
     */
    private Stage getStage() {
        if (openFileLink != null && openFileLink.getScene() != null) {
            return (Stage) openFileLink.getScene().getWindow();
        }
        if (newProjectLink != null && newProjectLink.getScene() != null) {
            return (Stage) newProjectLink.getScene().getWindow();
        }
        throw new IllegalStateException("Impossible de récupérer la fenêtre principale.");
    }

    /**
     * Charge la vue principale permettant d'afficher et manipuler le graphe.
     *
     * <p>
     * Le fichier sélectionné est transmis au contrôleur principal
     * afin d'être chargé dans le moteur de graphe.
     * </p>
     *
     * <p>
     * La fenêtre est ensuite redimensionnée pour occuper toute la
     * surface visible de l'écran.
     * </p>
     *
     * @param stage fenêtre principale de l'application
     * @param file  fichier de graphe à ouvrir
     */
    private void openGraphWindow(Stage stage, File file) {
        try {
            if (!isSupportedFile(file)) {
                alert(Alert.AlertType.WARNING,
                        "Format non pris en charge",
                        "Formats acceptés : .csv, .dot et .mongraphe\nFichier reçu : " + file.getName());
                return;
            }

            FXMLLoader loader = new FXMLLoader(getClass().getResource("/fxml/MainView.fxml"));
            Parent root = loader.load();
            MainGraphController controller = loader.getController();
            controller.openFile(file);

            Rectangle2D screenBounds = Screen.getPrimary().getVisualBounds();
            Scene scene = new Scene(root, screenBounds.getWidth(), screenBounds.getHeight());
            stage.setTitle("MonGraphe");
            stage.setScene(scene);
            stage.setResizable(true);
            stage.setX(screenBounds.getMinX());
            stage.setY(screenBounds.getMinY());
            stage.setWidth(screenBounds.getWidth());
            stage.setHeight(screenBounds.getHeight());
            stage.show();
        } catch (Exception e) {
            alert(Alert.AlertType.ERROR,
                    "Ouverture impossible",
                    "Impossible d'ouvrir la vue principale.\n\n" + rootCauseMessage(e));
        }
    }

    /**
     * Vérifie si le fichier possède une extension supportée.
     *
     * @param file fichier à vérifier
     * @return {@code true} si le format est supporté
     */
    private boolean isSupportedFile(File file) {
        String name = file.getName().toLowerCase();
        return name.endsWith(".csv") || name.endsWith(".dot") || name.endsWith(".mongraphe");
    }

    /**
     * Récupère le message de la cause racine d'une exception.
     *
     * <p>
     * Cette méthode parcourt la chaîne des causes afin d'obtenir
     * l'erreur la plus pertinente pour l'utilisateur.
     * </p>
     *
     * @param throwable exception initiale
     * @return message de la cause principale
     */
    private String rootCauseMessage(Throwable throwable) {
        Throwable current = throwable;
        while (current.getCause() != null && current.getCause() != current) {
            current = current.getCause();
        }
        String message = current.getMessage();
        return message == null || message.isBlank() ? current.toString() : message;
    }

    /**
     * Affiche une boîte de dialogue d'information ou d'erreur.
     *
     * @param type    type d'alerte à afficher
     * @param title   titre de la fenêtre
     * @param content message affiché
     */
    private void alert(Alert.AlertType type, String title, String content) {
        Alert alert = new Alert(type);
        alert.setTitle(title);
        alert.setHeaderText(null);
        alert.setContentText(content);
        alert.showAndWait();
    }
}
package com.mongraphe.graphui.interfaces.app;

import javafx.stage.Stage;

/**
 * Interface de documentation décrivant le comportement attendu de
 * la classe principale de l'application JavaFX.
 *
 * <p>
 * Cette interface sert uniquement de support de documentation et définit
 * le contrat fonctionnel de démarrage de l'application graphique.
 * L’implémentation concrète est assurée par la classe {@code MainApp}.
 * </p>
 *
 * <p>
 * L'application repose sur le framework JavaFX et charge son interface
 * graphique principale à partir d'un fichier FXML.
 * </p>
 *
 * <h2>Responsabilités principales</h2>
 * <ul>
 * <li>Initialiser l'application JavaFX.</li>
 * <li>Charger la vue principale depuis un fichier FXML.</li>
 * <li>Créer et configurer la scène principale.</li>
 * <li>Afficher la fenêtre principale de l'application.</li>
 * <li>Gérer les erreurs de démarrage et informer l'utilisateur.</li>
 * </ul>
 *
 * <h2>Flux de démarrage</h2>
 * <ol>
 * <li>Chargement du fichier FXML contenant l’interface graphique.</li>
 * <li>Création de l’arbre de composants JavaFX (Parent).</li>
 * <li>Création d’une scène JavaFX.</li>
 * <li>Configuration de la fenêtre principale (Stage).</li>
 * <li>Affichage de la fenêtre à l’utilisateur.</li>
 * </ol>
 *
 * <h2>Gestion des erreurs</h2>
 * <p>
 * En cas d'échec lors du chargement de l'interface ou de l'initialisation
 * de la scène, une boîte de dialogue d'erreur est affichée à l'utilisateur.
 * Cette boîte de dialogue contient le message d'erreur généré par
 * l'exception interceptée.
 * </p>
 *
 * <h2>Fichier FXML utilisé</h2>
 * <p>
 * L'interface principale est définie dans :
 * </p>
 * 
 * <pre>
 * /fxml/HomeScreen.fxml
 * </pre>
 *
 * <h2>Fenêtre principale</h2>
 * <ul>
 * <li>Titre : "Accueil"</li>
 * <li>Largeur initiale : 1000 pixels</li>
 * <li>Hauteur initiale : 700 pixels</li>
 * </ul>
 */
public interface MainAppDocumentation {

    /**
     * Méthode appelée automatiquement par le runtime JavaFX lors du
     * lancement de l'application.
     *
     * <p>
     * Cette méthode constitue le point d'entrée principal pour
     * l'initialisation de l'interface graphique. Elle reçoit
     * la fenêtre principale ({@link Stage}) dans laquelle
     * l'application doit construire et afficher sa scène.
     * </p>
     *
     * <h3>Étapes réalisées par l’implémentation</h3>
     * <ul>
     * <li>Chargement du fichier FXML via {@code FXMLLoader}.</li>
     * <li>Création du conteneur racine de la scène.</li>
     * <li>Création de la {@code Scene} JavaFX.</li>
     * <li>Configuration du {@code Stage} principal.</li>
     * <li>Affichage de la fenêtre.</li>
     * </ul>
     *
     * <h3>Gestion des exceptions</h3>
     * <p>
     * Si une erreur survient durant le chargement du FXML ou la création
     * de la scène, l’implémentation doit afficher une boîte de dialogue
     * d'erreur afin d’informer l’utilisateur du problème rencontré.
     * </p>
     *
     * @param primaryStage
     *                     Fenêtre principale fournie par le runtime JavaFX. Elle
     *                     représente
     *                     la fenêtre principale de l'application graphique dans
     *                     laquelle
     *                     la scène sera affichée.
     */
    void start(Stage primaryStage);
}
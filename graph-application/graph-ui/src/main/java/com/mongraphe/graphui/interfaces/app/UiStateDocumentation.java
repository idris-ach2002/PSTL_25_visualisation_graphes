package com.mongraphe.graphui.interfaces.app;

import javafx.beans.property.BooleanProperty;
import javafx.beans.property.StringProperty;

/**
 * Interface de documentation décrivant le rôle et le contrat
 * de la classe {@code UiState}.
 *
 * <p>
 * Cette structure représente l'état global de l'interface utilisateur
 * d'une application JavaFX. Elle encapsule plusieurs propriétés observables
 * permettant de synchroniser automatiquement l'état de l'interface graphique
 * avec la logique applicative grâce au mécanisme de binding fourni par JavaFX.
 * </p>
 *
 * <h2>Objectif</h2>
 * <p>
 * La classe implémentant ce contrat agit comme un conteneur centralisé
 * d'état pour l'interface utilisateur. Les composants graphiques peuvent
 * observer ces propriétés afin de réagir automatiquement aux changements.
 * </p>
 *
 * <h2>Propriétés gérées</h2>
 * <ul>
 * <li>
 * <b>statusText</b> :
 * texte affiché dans la barre d’état ou une zone d’information
 * indiquant l’état courant de l’application.
 * </li>
 * <li>
 * <b>simulationRunning</b> :
 * indicateur booléen précisant si une simulation ou un processus
 * interne est actuellement en cours d'exécution.
 * </li>
 * </ul>
 *
 * <h2>Utilisation dans JavaFX</h2>
 * <p>
 * Les propriétés retournées par cette interface sont des
 * {@link javafx.beans.property.Property} JavaFX. Elles permettent :
 * </p>
 *
 * <ul>
 * <li>le binding bidirectionnel entre la vue et le modèle</li>
 * <li>l'observation automatique des changements</li>
 * <li>la mise à jour dynamique de l'interface graphique</li>
 * </ul>
 *
 * <h2>Principe de fonctionnement</h2>
 * <ol>
 * <li>L'état est stocké sous forme de propriétés observables.</li>
 * <li>Les contrôleurs JavaFX peuvent se lier à ces propriétés.</li>
 * <li>Tout changement déclenche automatiquement la mise à jour de
 * l'interface.</li>
 * </ol>
 *
 * <h2>Gestion des valeurs</h2>
 * <p>
 * L’implémentation garantit certaines règles de cohérence :
 * </p>
 *
 * <ul>
 * <li>Les valeurs nulles pour le statut sont converties en chaîne vide.</li>
 * <li>Les propriétés ne sont mises à jour que si la valeur change
 * réellement.</li>
 * <li>Cela évite des notifications inutiles aux observateurs.</li>
 * </ul>
 */
public interface UiStateDocumentation {

    /**
     * Retourne la propriété observable représentant le texte
     * de statut de l'application.
     *
     * <p>
     * Cette propriété peut être liée à des composants graphiques
     * tels qu'un {@code Label} ou une barre d'état afin d'afficher
     * dynamiquement l'état courant de l'application.
     * </p>
     *
     * <h3>Exemple d'utilisation</h3>
     * 
     * <pre>
     * labelStatus.textProperty().bind(uiState.statusTextProperty());
     * </pre>
     *
     * @return propriété JavaFX contenant le texte de statut
     */
    StringProperty statusTextProperty();

    /**
     * Retourne la propriété observable indiquant si une simulation
     * ou un traitement est actuellement en cours.
     *
     * <p>
     * Cette propriété permet notamment d'activer ou désactiver
     * certains éléments de l'interface utilisateur (boutons,
     * menus, contrôles interactifs).
     * </p>
     *
     * <h3>Exemple d'utilisation</h3>
     * 
     * <pre>
     * buttonStart.disableProperty().bind(uiState.simulationRunningProperty());
     * </pre>
     *
     * @return propriété booléenne observable représentant l'état
     *         d'exécution d'une simulation
     */
    BooleanProperty simulationRunningProperty();

    /**
     * Retourne la valeur actuelle du texte de statut.
     *
     * <p>
     * Cette méthode fournit un accès direct à la valeur contenue
     * dans la propriété {@link #statusTextProperty()} sans passer
     * par le mécanisme de binding.
     * </p>
     *
     * @return texte représentant l'état actuel de l'application
     */
    String getStatus();

    /**
     * Indique si une simulation ou un processus interne est
     * actuellement en cours d'exécution.
     *
     * <p>
     * Cette méthode retourne la valeur contenue dans la propriété
     * {@link #simulationRunningProperty()}.
     * </p>
     *
     * @return {@code true} si la simulation est active,
     *         {@code false} sinon
     */
    boolean isRunning();

    /**
     * Met à jour le texte de statut de l'application.
     *
     * <p>
     * L'implémentation doit garantir les règles suivantes :
     * </p>
     *
     * <ul>
     * <li>Une valeur {@code null} est convertie en chaîne vide.</li>
     * <li>La propriété n'est modifiée que si la valeur change réellement.</li>
     * </ul>
     *
     * <p>
     * Cette stratégie permet de limiter les notifications inutiles
     * envoyées aux observateurs de la propriété.
     * </p>
     *
     * @param status nouveau texte de statut à afficher
     */
    void setStatus(String status);

    /**
     * Met à jour l'état d'exécution d'une simulation ou d'un
     * traitement interne.
     *
     * <p>
     * Si la valeur fournie est différente de l'état actuel,
     * la propriété observable correspondante est mise à jour,
     * ce qui déclenche les mécanismes de notification JavaFX.
     * </p>
     *
     * @param running indique si la simulation est active
     */
    void setRunning(boolean running);
}
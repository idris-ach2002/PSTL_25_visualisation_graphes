package com.mongraphe.graphui.interfaces.controller;

import com.mongraphe.graphui.app.CommandBus;
import com.mongraphe.graphui.interfaces.CommandBusLinkedI;
import com.mongraphe.graphui.rendering.GraphEngine;

/**
 * Interface de documentation décrivant le comportement attendu du contrôleur
 * responsable de l'affichage tabulaire paginé des données du graphe.
 *
 * <p>
 * Cette interface sert uniquement de support de documentation et définit
 * le contrat fonctionnel du contrôleur de données. L'implémentation concrète
 * est assurée par la classe
 * {@code com.mongraphe.graphui.controller.DataController}.
 * </p>
 *
 * <p>
 * Le contrôleur est lié à une vue FXML contenant deux zones de pagination
 * distinctes (une pour les sommets, une pour les arêtes) ainsi que des
 * indicateurs de totalisation et des champs de saisie pour la navigation
 * directe vers une page spécifique.
 * </p>
 *
 * <h2>Responsabilités principales</h2>
 * <ul>
 * <li>Récupérer les données paginées du graphe via le bus de commandes.</li>
 * <li>Construire et configurer les tables de colonnes pour les sommets et les
 * arêtes.</li>
 * <li>Gérer la pagination des deux tableaux indépendamment.</li>
 * <li>Permettre la saisie manuelle du numéro de page.</li>
 * <li>Actualiser l'ensemble des vues lors d'un rafraîchissement global.</li>
 * <li>Afficher les statistiques globales (nombre de sommets, d'arêtes,
 * total).</li>
 * </ul>
 *
 * <h2>Flux d'initialisation</h2>
 * <ol>
 * <li>La méthode {@code initialize()} est appelée automatiquement par le
 * chargeur FXML.</li>
 * <li>Les colonnes des tables sont créées et liées aux propriétés des modèles
 * {@code Vertex} et {@code Edge} via {@code PropertyValueFactory}.</li>
 * <li>Les fabriques de page des paginations sont configurées pour générer
 * des {@code TableView} à la demande.</li>
 * <li>Les champs de texte de navigation sont reliés aux paginations
 * correspondantes.</li>
 * <li>Le contrôleur attend l'injection du bus de commandes via
 * {@link #setBus(CommandBus)}.</li>
 * </ol>
 *
 * <h2>Rafraîchissement des données</h2>
 * <p>
 * La méthode {@code refresh()} doit être appelée chaque fois que les données
 * sous‑jacentes du graphe sont modifiées (ajout, suppression, mise à jour).
 * Elle interroge le moteur de graphe pour obtenir le nombre total d'éléments,
 * recalcule le nombre de pages nécessaires et réinitialise les paginations
 * sur la première page.
 * </p>
 *
 * <h2>Fichier FXML associé</h2>
 * <p>
 * La vue liée à ce contrôleur doit déclarer les composants JavaFX suivants
 * avec les identifiants {@code fx:id} correspondants :
 * </p>
 *
 * <pre>
 * &lt;Label fx:id="nodesDisplayedLabel" /&gt;
 * &lt;Label fx:id="edgesDisplayedLabel" /&gt;
 * &lt;Label fx:id="totalElementsLabel" /&gt;
 *
 * &lt;Pagination fx:id="vertexPagination" /&gt;
 * &lt;Pagination fx:id="edgePagination" /&gt;
 *
 * &lt;TextField fx:id="vertexPageField" /&gt;
 * &lt;TextField fx:id="edgePageField" /&gt;
 * </pre>
 *
 * <h2>Configuration des tables</h2>
 * <p>
 * Les colonnes des tables sont configurées avec les en‑têtes suivants et
 * liées aux propriétés correspondantes des objets métier.
 * </p>
 *
 * <h3>Table des sommets ({@code Vertex})</h3>
 * <ul>
 * <li><b>ID</b> : identifiant unique du sommet</li>
 * <li><b>Communauté</b> : nom de la communauté d'appartenance</li>
 * <li><b>Degré</b> : nombre d'arêtes incidentes</li>
 * <li><b>X</b> : coordonnée horizontale</li>
 * <li><b>Y</b> : coordonnée verticale</li>
 * <li><b>Diamètre</b> : diamètre d'affichage</li>
 * <li><b>Supprimé ?</b> : indicateur booléen de suppression logique</li>
 * </ul>
 *
 * <h3>Table des arêtes ({@code Edge})</h3>
 * <ul>
 * <li><b>Début</b> : identifiant du sommet source</li>
 * <li><b>Fin</b> : identifiant du sommet destination</li>
 * <li><b>Poids</b> : valeur numérique du poids de l'arête</li>
 * </ul>
 *
 * <h2>Gestion de la pagination</h2>
 * <p>
 * Chaque pagination affiche au maximum
 * {@value com.mongraphe.graphui.controller.DataController#ROWS_PER_PAGE}
 * éléments par page. Les numéros de page saisis par l'utilisateur sont validés
 * et, en cas d'erreur, le champ est réinitialisé avec la page courante.
 * </p>
 *
 * <h2>Dépendance au bus de commandes</h2>
 * <p>
 * Ce contrôleur implémente {@link CommandBusLinkedI}{@code <GraphEngine>} et
 * nécessite un {@link CommandBus}{@code <GraphEngine>} valide pour interroger
 * le moteur de graphe. Tant que le bus n'est pas injecté, les opérations de
 * rafraîchissement et de création de pages sont sans effet.
 * </p>
 */
public interface DataControllerDocumentation {

    /**
     * Injecte le bus de commandes utilisé pour communiquer avec le moteur
     * de graphe.
     *
     * <p>
     * Cette méthode est appelée par le gestionnaire d'application après
     * l'instanciation du contrôleur. Elle permet au contrôleur d'effectuer
     * des appels synchrones au moteur via {@code bus.dispatchSync()}.
     * </p>
     *
     * @param bus
     *            le bus de commandes associé au {@code GraphEngine} ; ne doit
     *            pas être {@code null}.
     */
    void setBus(CommandBus<GraphEngine> bus);

    /**
     * Rafraîchit l'intégralité de l'interface utilisateur en interrogeant
     * le moteur de graphe pour obtenir les dernières données.
     *
     * <p>
     * Cette méthode doit être invoquée après toute modification du graphe
     * (ajout, suppression, déplacement, changement de communauté, etc.)
     * afin que les tables et les statistiques affichées restent cohérentes.
     * </p>
     *
     * <h3>Actions réalisées</h3>
     * <ol>
     * <li>Récupération du nombre total de sommets et d'arêtes.</li>
     * <li>Calcul du nombre de pages nécessaires pour chaque pagination.</li>
     * <li>Réinitialisation des paginations à la première page.</li>
     * <li>Mise à jour des labels de totalisation.</li>
     * </ol>
     *
     * <p>
     * Si le bus de commandes n'a pas encore été injecté, cette méthode
     * ne produit aucun effet.
     * </p>
     */
    void refresh();
}
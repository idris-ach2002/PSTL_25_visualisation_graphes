package com.mongraphe.graphui.interfaces;

import java.util.function.Consumer;
import java.util.function.Function;

import com.mongraphe.graphui.interfaces.app.UndoableGraphCommand;

/**
 * Bus de commandes permettant d'exécuter des opérations sur un contexte donné
 * via un mécanisme d'exécution contrôlé (souvent un
 * {@link java.util.concurrent.Executor}).
 *
 * <p>
 * Cette abstraction centralise l'envoi de commandes vers un moteur ou un modèle
 * applicatif. Elle permet :
 * </p>
 *
 * <ul>
 * <li>l'exécution asynchrone de commandes</li>
 * <li>l'exécution synchrone de requêtes</li>
 * <li>la gestion d'opérations undo/redo</li>
 * </ul>
 *
 * <p>
 * Le bus garantit que toutes les commandes sont exécutées dans le même
 * contexte d'exécution (thread du moteur, worker dédié, etc.), ce qui permet
 * d'éviter les problèmes de concurrence sur l'état interne du moteur.
 * </p>
 *
 * @param <C> type du contexte manipulé par les commandes (ex: moteur de graphe)
 */
public interface CommandBusI<C> {

    /**
     * Envoie une commande pour exécution asynchrone.
     *
     * <p>
     * La commande est exécutée par l'exécuteur associé au bus. L'appelant
     * ne bloque pas et ne reçoit pas de résultat.
     * </p>
     *
     * @param command commande à exécuter
     */
    void dispatch(GraphCommand<C> command);

    /**
     * Exécute une opération de manière synchrone sans valeur de retour.
     *
     * <p>
     * L'appelant est bloqué jusqu'à la fin de l'exécution de la commande.
     * Cette méthode est utile pour les opérations d'initialisation ou
     * lorsque l'état du moteur doit être modifié avant de poursuivre.
     * </p>
     *
     * @param command opération à exécuter sur le contexte
     */
    void dispatchSyncVoid(Consumer<C> command);

    /**
     * Exécute une requête synchrone retournant un résultat.
     *
     * <p>
     * Cette méthode est généralement utilisée pour lire l'état du moteur
     * ou du modèle tout en garantissant que l'accès se fait dans le bon
     * contexte d'exécution.
     * </p>
     *
     * @param <R>   type du résultat
     * @param query fonction appliquée au contexte
     * @return résultat de la requête
     */
    <R> R dispatchSync(Function<C, R> query);

    /**
     * Exécute une commande supportant undo/redo.
     *
     * <p>
     * La commande est exécutée puis enregistrée dans le gestionnaire
     * d'historique afin de permettre son annulation ou sa réexécution.
     * </p>
     *
     * @param command commande undoable
     */
    void dispatchUndoable(UndoableGraphCommand<C> command);
}
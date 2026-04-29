package com.mongraphe.graphui.interfaces.app;

import java.util.function.Consumer;
import java.util.function.Function;

import com.mongraphe.graphui.interfaces.GraphCommand;

/**
 * Interface définissant le contrat pour un bus de commandes (Command Bus).
 * *
 * <p>
 * Ce bus centralise et gère l'exécution de différentes opérations (commandes,
 * requêtes)
 * sur un contexte spécifique. Il supporte des exécutions synchrones,
 * asynchrones,
 * ainsi que la gestion d'un historique d'actions pour permettre d'annuler
 * (undo)
 * ou de refaire (redo) certaines commandes.
 * </p>
 *
 * @param <C> Le type du contexte sur lequel les commandes et requêtes seront
 *            appliquées.
 */
public interface CommandBusI<C> {

    /**
     * Récupère le contexte actuellement géré par ce bus de commandes.
     *
     * @return L'instance du contexte de type {@code <C>}.
     */
    C getContext();

    /**
     * Soumet une commande pour une exécution asynchrone.
     * *
     * <p>
     * La commande est confiée à l'exécuteur (Executor) interne et sera traitée
     * dans un thread séparé, sans bloquer le thread appelant.
     * </p>
     *
     * @param command La commande à exécuter, contenant la logique métier.
     */
    void dispatch(GraphCommand<C> command);

    /**
     * Exécute une action de manière synchrone.
     * *
     * <p>
     * Bien que la tâche soit confiée à l'exécuteur, cette méthode bloque
     * le thread appelant jusqu'à ce que l'action soit complètement terminée.
     * </p>
     *
     * @param command L'action à exécuter, représentée par un {@link Consumer}
     *                prenant le contexte en paramètre.
     * @throws RuntimeException Si l'exécution de la tâche est interrompue ou si une
     *                          exception survient pendant son traitement.
     */
    void dispatchSyncVoid(Consumer<C> command);

    /**
     * Exécute une requête de manière synchrone et retourne son résultat.
     * *
     * <p>
     * Tout comme {@link #dispatchSyncVoid(Consumer)}, cette méthode bloque le
     * thread
     * appelant jusqu'à ce que le calcul soit terminé et que le résultat soit
     * disponible.
     * </p>
     *
     * @param <R>   Le type du résultat attendu.
     * @param query La fonction à exécuter, prenant le contexte en paramètre et
     *              retournant un résultat.
     * @return Le résultat produit par l'exécution de la requête.
     * @throws RuntimeException Si l'exécution de la tâche est interrompue ou
     *                          échoue.
     */
    <R> R dispatchSync(Function<C, R> query);

    /**
     * Exécute une commande de manière asynchrone et l'enregistre dans le
     * gestionnaire d'annulation (UndoManager).
     * *
     * <p>
     * Une fois exécutée, cette commande rejoint l'historique, ce qui permet
     * d'utiliser
     * par la suite les méthodes {@link #undo()} et {@link #redo()} pour modifier
     * l'état de l'application.
     * </p>
     *
     * @param command La commande annulable à exécuter et à historiser.
     */
    void dispatchUndoable(UndoableGraphCommand<C> command);

    /**
     * Annule la dernière commande annulable exécutée.
     * *
     * <p>
     * Si l'historique est vide ou si aucune action ne peut être annulée,
     * l'appel à cette méthode est ignoré en silence.
     * </p>
     */
    void undo();

    /**
     * Refait la dernière commande qui a été précédemment annulée via
     * {@link #undo()}.
     * *
     * <p>
     * Si aucune action n'a été annulée au préalable, l'appel à cette méthode est
     * ignoré.
     * </p>
     */
    void redo();

    /**
     * Indique s'il existe au moins une action dans l'historique pouvant être
     * annulée.
     *
     * @return {@code true} si une action peut être annulée, {@code false} dans le
     *         cas contraire.
     */
    boolean canUndo();

    /**
     * Indique s'il existe au moins une action dans l'historique pouvant être
     * refaite.
     *
     * @return {@code true} si une action précédemment annulée peut être refaite,
     *         {@code false} dans le cas contraire.
     */
    boolean canRedo();
}
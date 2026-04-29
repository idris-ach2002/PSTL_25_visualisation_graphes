package com.mongraphe.graphui.interfaces.app;

/**
 * <p>
 * Représente une commande graphique qui peut être exécutée puis annulée
 * de manière réversible. Cette interface est au cœur du mécanisme d'undo/redo
 * pour les opérations effectuées sur un contexte graphique.
 * </p>
 * 
 * <p>
 * Les implémentations typiques encapsulent une modification atomique ou
 * composite du modèle de graphe. L'interface permet de construire des
 * commandes composites (voir
 * {@link com.mongraphe.graphui.app.CompositeUndoableCommand})
 * qui regroupent plusieurs commandes élémentaires en une seule opération
 * annulable.
 * </p>
 *
 * @param <C> le type du contexte sur lequel la commande opère (par exemple,
 *            le gestionnaire de graphe, le canvas, ou le modèle de données).
 *            Ce contexte est passé en paramètre aux méthodes
 *            {@link #execute(C)}
 *            et {@link #undo(C)}.
 * 
 * @see com.mongraphe.graphui.app.CompositeUndoableCommand
 * @since 1.0
 */
public interface UndoableGraphCommand<C> {

    /**
     * Exécute la commande en appliquant ses modifications au contexte fourni.
     * <p>
     * Cette méthode est appelée lorsqu'une action est effectuée pour la première
     * fois ou lorsqu'elle est réexécutée après avoir été annulée (redo).
     * L'implémentation doit garantir que l'exécution produit un état cohérent
     * du contexte et que cet état peut être restauré ultérieurement par un appel
     * à {@link #undo(C)}.
     * </p>
     *
     * @param contexte le contexte graphique cible de l'opération ; ne doit pas
     *                 être {@code null} (la gestion des valeurs nulles est laissée
     *                 à l'appréciation de l'implémentation).
     * @throws NullPointerException  si le contexte est {@code null} et que
     *                               l'implémentation ne le supporte pas.
     * @throws IllegalStateException si la commande ne peut pas être exécutée
     *                               dans l'état courant du contexte.
     */
    void execute(C contexte);

    /**
     * Annule les effets de la commande en restaurant le contexte dans son état
     * antérieur à l'exécution.
     * <p>
     * Cette méthode est appelée lorsqu'une action précédemment exécutée est
     * annulée via le mécanisme d'undo. L'implémentation doit inverser précisément
     * les modifications apportées par {@link #execute(C)}.
     * </p>
     * <p>
     * Il est attendu que l'appel à {@code undo} après un {@code execute} ramène
     * le contexte dans un état équivalent à celui précédant l'exécution.
     * </p>
     *
     * @param contexte le contexte graphique sur lequel annuler les modifications.
     * @throws NullPointerException  si le contexte est {@code null} et que
     *                               l'implémentation ne le supporte pas.
     * @throws IllegalStateException si l'annulation n'est pas possible parce que
     *                               la commande n'a pas été exécutée ou que le
     *                               contexte a été modifié entre-temps.
     */
    void undo(C contexte);
}
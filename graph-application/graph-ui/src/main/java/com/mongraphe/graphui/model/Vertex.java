package com.mongraphe.graphui.model;

import java.util.ArrayList;

/**
 * Représentation d’un sommet (nœud) dans un graphe.
 *
 * <p>
 * Un {@code Vertex} encapsule :
 * <ul>
 * <li>une position 2D (x, y) pour le rendu ou layout</li>
 * <li>un diamètre dépendant du degré du nœud</li>
 * <li>une appartenance à une communauté</li>
 * <li>un état (visible / supprimé)</li>
 * <li>une liste d’arêtes incidentes</li>
 * </ul>
 *
 * <p>
 * La classe est utilisée à la fois pour :
 * <ul>
 * <li>le rendu graphique (UI / OpenGL / JavaFX)</li>
 * <li>les algorithmes de graphe (communautés, degré)</li>
 * <li>la simulation dynamique du graphe</li>
 * </ul>
 */
public class Vertex {

    /**
     * Taille initiale des nœuds (diamètre de base).
     */
    public static double initial_node_size = 10;

    /**
     * Facteur d’échelle du diamètre en fonction du degré.
     *
     * <p>
     * Formule utilisée :
     * {@code diameter = initial_node_size + sqrt(degree_scale_factor * degree)}
     */
    public static double degree_scale_factor = 0.5;

    private int id;

    /** Position X du nœud (volatile pour mise à jour concurrente possible). */
    private volatile double x;

    /** Position Y du nœud (volatile pour mise à jour concurrente possible). */
    private volatile double y;

    /** Diamètre utilisé pour le rendu graphique. */
    private volatile double diameter;

    /** Indique si le nœud est affiché dans l’interface. */
    private boolean isVisible = true;

    /** Indique si le nœud a été supprimé logiquement. */
    private boolean isDeleted = false;

    /** Liste des arêtes incidentes au nœud. */
    private final ArrayList<Edge> edges = new ArrayList<>();

    /** Communauté à laquelle appartient le nœud. */
    private Community community;

    /**
     * Construit un nœud à une position donnée.
     *
     * @param x position initiale en abscisse
     * @param y position initiale en ordonnée
     */
    public Vertex(double x, double y) {
        this.x = x;
        this.y = y;
        this.diameter = initial_node_size;
    }

    /**
     * Retourne l’identifiant unique du nœud.
     *
     * @return id du vertex
     */
    public int getId() {
        return id;
    }

    /**
     * Définit l’identifiant du nœud.
     *
     * @param id identifiant unique
     */
    public void setId(int id) {
        this.id = id;
    }

    /**
     * Retourne la position X du nœud.
     */
    public double getX() {
        return x;
    }

    /**
     * Retourne la position Y du nœud.
     */
    public double getY() {
        return y;
    }

    /**
     * Met à jour la position du nœud.
     *
     * @param x nouvelle position X
     * @param y nouvelle position Y
     */
    public void updatePosition(double x, double y) {
        this.x = x;
        this.y = y;
    }

    /**
     * Retourne le diamètre actuel du nœud.
     */
    public double getDiameter() {
        return diameter;
    }

    /**
     * Met à jour le diamètre du nœud en fonction de son degré.
     *
     * <p>
     * Formule :
     * {@code diameter = initial_node_size + sqrt(degree_scale_factor * degree)}
     *
     * <p>
     * Le facteur {@code degree_scale_factor} est contraint à être ≥ 0.
     */
    public void updateDiameter() {
        if (degree_scale_factor < 0)
            degree_scale_factor = 0;

        diameter = initial_node_size + Math.sqrt(degree_scale_factor * getDegree());
    }

    /**
     * Indique si le nœud est visible dans l’interface.
     */
    public boolean isVisible() {
        return isVisible;
    }

    /**
     * Définit la visibilité du nœud.
     *
     * @param visible état de visibilité
     */
    public void setVisible(boolean visible) {
        this.isVisible = visible;
    }

    /**
     * Supprime logiquement le nœud.
     *
     * <p>
     * Effets :
     * <ul>
     * <li>marque le nœud comme supprimé</li>
     * <li>réduit son diamètre à 0</li>
     * </ul>
     */
    public void delete() {
        isDeleted = true;
        diameter = 0;
    }

    /**
     * Restaure un nœud supprimé.
     *
     * <p>
     * Effets :
     * <ul>
     * <li>réactive le nœud</li>
     * <li>le rend visible</li>
     * <li>recalcule son diamètre</li>
     * </ul>
     */
    public void restore() {
        isDeleted = false;
        isVisible = true;
        updateDiameter();
    }

    /**
     * Indique si le nœud est supprimé.
     */
    public boolean isDeleted() {
        return isDeleted;
    }

    /**
     * Retourne la communauté associée au nœud.
     */
    public Community getCommunity() {
        return community;
    }

    /**
     * Définit la communauté du nœud.
     *
     * @param c communauté cible
     */
    public void setCommunity(Community c) {
        community = c;
    }

    /**
     * Retourne le nom de la communauté du nœud.
     *
     * @return nom de la communauté ou chaîne vide si null
     */
    public String getCommunityName() {
        return community != null ? community.getName() : "";
    }

    /**
     * Ajoute une arête incidente au nœud.
     *
     * @param edge arête à ajouter
     */
    public void addEdge(Edge edge) {
        edges.add(edge);
    }

    /**
     * Retourne le degré du nœud.
     *
     * <p>
     * Le degré correspond au nombre d’arêtes incidentes.
     */
    public int getDegree() {
        return edges.size();
    }

    /**
     * Représentation textuelle du nœud.
     *
     * @return chaîne formatée {@code id (x, y)}
     */
    @Override
    public String toString() {
        return id + " (" + x + ", " + y + ")";
    }
}
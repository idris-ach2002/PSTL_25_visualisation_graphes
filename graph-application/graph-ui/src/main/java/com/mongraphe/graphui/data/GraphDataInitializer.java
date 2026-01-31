package com.mongraphe.graphui.data;

import java.util.ArrayList;
import java.util.HashMap;
import java.util.List;

import com.mongraphe.graphui.Community;
import com.mongraphe.graphui.Edge;
import com.mongraphe.graphui.EdgeC;
import com.mongraphe.graphui.Graph;
import com.mongraphe.graphui.Vertex;

/**
 * GraphDataInitializer
 * --------------------
 *
 * Cette classe est responsable de :
 * - l'initialisation des données du graphe,
 * - la transformation des résultats natifs (C / JNI) en objets Java,
 * - l'association des communautés, couleurs et degrés,
 * - la préparation des structures utilisées par le rendu.
 *
 * Elle ne gère PAS :
 * - l'interface JavaFX,
 * - le rendu OpenGL,
 * - les interactions utilisateur.
 *
 * Objectif :
 * Centraliser toute la logique de préparation des données
 * afin de rendre GraphVue lisible et maintenable.
 */
public class GraphDataInitializer {

    /**
     * Initialise complètement les données du graphe.
     *
     * @param graph Le graphe métier
     */
    public void initialize(Graph graph) {

        // Récupération des données calculées par le moteur natif
        graph.vertices = List.of(graph.getPositions());
        EdgeC[] edgesC = graph.getEdges();
        int[] communitiesRaw = graph.getCommunities();
        float[][] colors = graph.getClusterColors();

        // Construction des communautés
        HashMap<Integer, Community> communities = new HashMap<>();

        for (int i = 0; i < graph.vertices.size(); i++) {

            int communityId = communitiesRaw[i];

            Community community = communities.get(communityId);
            if (community == null) {
                community = new Community(
                        communityId,
                        colors[i][0],
                        colors[i][1],
                        colors[i][2]
                );
                communities.put(communityId, community);
            }

            Vertex v = graph.vertices.get(i);
            v.setId(i);
            v.setCommunity(community);
        }


        // Construction des arêtes
        graph.edges = new ArrayList<>();
        for (EdgeC edgeC : edgesC) {
            Edge edge = new Edge(
                graph.vertices.get(edgeC.getStart()),
                graph.vertices.get(edgeC.getEnd()),
                edgeC.getWeight()
            );
            graph.edges.add(edge);
        }

        // Mise à jour des tailles de sommets (degré)
        for (Vertex v : graph.vertices) {
            v.updateDiameter();
        }

        // Initialisation des buffers utilisés pour le rendu
        graph.initializeArrays();
    }
}

#ifndef FORCEATLAS_COMMUNITIES_H
#define FORCEATLAS_COMMUNITIES_H

#include "cluster.h"

typedef struct {
    int community;
    double total_weight;
    int component;  // Ajout du champ pour la composante connexe
} Community;

extern int *csr_offsets;      // décalages par nœud (taille num_nodes+1)
extern int *csr_neighbors;    // voisins (taille 2*num_edges)
extern double *csr_weights;   // poids associés (taille 2*num_edges)
extern int csr_total_edges;   // nombre total d'entrées (2*num_edges)

// Fonctions de gestion du CSR
void build_csr_adjacency(void);
void free_csr_adjacency(void);

int louvain_method();
int louvain_methodC();
int leiden_method_CPM();
int leiden_method();
int count_unique_communities(int *communities, int num_nodes);
void find_connected_components();
void mark_component(int, int);
void apply_louvain_to_component(int);
void initialize_adjacency_list();
void initialize_community_colors();
void compute_average_vectors();
void add_edge_to_adjacency_list(int node, int neighbor, double weight);
double calculate_gain_modularity(int node, int new_community, double total_graph_weight); 
double calculate_gain_modularity_cpm(int node, int new_community, double resolution_parameter);

#endif
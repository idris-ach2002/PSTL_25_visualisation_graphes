#include "communities.h"
#include "../global.h"
#include <assert.h>
#include <stdbool.h>

double lambda = 0.1;
int *csr_offsets = NULL;
int *csr_neighbors = NULL;
double *csr_weights = NULL;
int csr_total_edges = 0;
Community node_community_map[MAX_NODES];
int num_communities = 0;
int component_sizes[MAX_NODES];
int num_components = 0;

void free_csr_adjacency(void) {
    free(csr_offsets);
    free(csr_neighbors);
    free(csr_weights);
    csr_offsets = csr_neighbors = NULL;
    csr_weights = NULL;
    csr_total_edges = 0;
}

void build_csr_adjacency(void) {
    free_csr_adjacency();

    int *degree = calloc(num_nodes, sizeof(int));
    for (int i = 0; i < num_edges; i++) {
        int u = edges[i].node1, v = edges[i].node2;
        degree[u]++;
        degree[v]++;
    }

    csr_offsets = malloc((num_nodes + 1) * sizeof(int));
    csr_offsets[0] = 0;
    for (int i = 0; i < num_nodes; i++) {
        csr_offsets[i + 1] = csr_offsets[i] + degree[i];
    }
    csr_total_edges = csr_offsets[num_nodes];

    csr_neighbors = malloc(csr_total_edges * sizeof(int));
    csr_weights = malloc(csr_total_edges * sizeof(double));

    int *pos = malloc(num_nodes * sizeof(int));
    memcpy(pos, csr_offsets, num_nodes * sizeof(int));

    for (int i = 0; i < num_edges; i++) {
        int u = edges[i].node1, v = edges[i].node2;
        double w = edges[i].weight;

        int pu = pos[u]++;
        csr_neighbors[pu] = v;
        csr_weights[pu] = w;

        int pv = pos[v]++;
        csr_neighbors[pv] = u;
        csr_weights[pv] = w;
    }

    free(degree);
    free(pos);
}

// ------------------------------------------------------------
// Détection des composantes connexes
// ------------------------------------------------------------
void find_connected_components() {
    for (int i = 0; i < num_nodes; i++) {
        node_community_map[i].component = -1;
    }
    for (int i = 0; i < MAX_NODES; i++) {
        component_sizes[i] = 0;
    }
    num_components = 0;

    int *stack = malloc(num_nodes * sizeof(int));
    for (int i = 0; i < num_nodes; i++) {
        if (node_community_map[i].component == -1) {
            mark_component(i, num_components, stack);
            num_components++;
        }
    }
    free(stack);
    printf("Number of connected components: %d\n", num_components);
}

void mark_component(int node, int component, int *stack) {
    int top = -1;
    stack[++top] = node;
    node_community_map[node].component = component;
    while (top >= 0) {
        int cur = stack[top--];
        component_sizes[component]++;
        for (int idx = csr_offsets[cur]; idx < csr_offsets[cur + 1]; idx++) {
            int neigh = csr_neighbors[idx];
            if (node_community_map[neigh].component == -1) {
                node_community_map[neigh].component = component;
                stack[++top] = neigh;
            }
        }
    }
}

// ------------------------------------------------------------
// Fonctions de gain de modularité
// ------------------------------------------------------------
double calculate_gain_modularity(int node, int new_community,
                                 double total_graph_weight) {
    double current_modularity = 0.0, new_modularity = 0.0;
    int current_community = node_community_map[node].community;

    for (int idx = csr_offsets[node]; idx < csr_offsets[node + 1]; idx++) {
        int neigh = csr_neighbors[idx];
        double w = csr_weights[idx];
        int neigh_comm = node_community_map[neigh].community;
        if (neigh_comm == current_community)
            current_modularity += w;
        else if (neigh_comm == new_community)
            new_modularity += w;
    }

    double current_weight = node_community_map[node].total_weight;
    double neighbor_weight = node_community_map[new_community].total_weight;
    double delta = (new_modularity - current_modularity) -
                   (current_weight * neighbor_weight) / total_graph_weight;
    return delta;
}

double calculate_gain_modularity_cpm(int node, int new_community,
                                     double resolution_parameter) {
    double current_internal = 0.0, new_internal = 0.0;
    int current_community = node_community_map[node].community;
    int degree_node = 0;

    for (int idx = csr_offsets[node]; idx < csr_offsets[node + 1]; idx++) {
        int neigh = csr_neighbors[idx];
        double w = csr_weights[idx];
        int neigh_comm = node_community_map[neigh].community;
        if (neigh_comm == current_community)
            current_internal += w;
        else if (neigh_comm == new_community)
            new_internal += w;
        degree_node += w;
    }

    double current_size = node_community_map[current_community].total_weight;
    double new_size = node_community_map[new_community].total_weight;

    double delta = new_internal - current_internal -
                   resolution_parameter * (degree_node * (new_size - current_size));
    return delta;
}

// ------------------------------------------------------------
// Fonction auxiliaire pour l'agrégation (construction du nouveau CSR)
// ------------------------------------------------------------
static int aggregate_and_build_csr(int n_cur, int *partition, int N_orig,
                                   int E_orig, int *node_comm, Edge *edges,
                                   int **new_offsets, int **new_neighbors,
                                   double **new_weights) {
    // Compression des communautés
    int *compress = malloc(n_cur * sizeof(int));
    for (int i = 0; i < n_cur; i++) compress[i] = -1;
    int n_new = 0;
    for (int i = 0; i < n_cur; i++) {
        int c = partition[i];
        if (compress[c] == -1) compress[c] = n_new++;
    }

    // Mettre à jour node_comm pour les nœuds originaux
    for (int i = 0; i < N_orig; i++) {
        node_comm[i] = compress[partition[node_comm[i]]];
    }

    // Nouvelle partition pour le graphe agrégé (identité)
    int *new_partition = malloc(n_new * sizeof(int));
    for (int i = 0; i < n_new; i++) new_partition[i] = i;

    // Matrice d'adjacence
    double **adj_mat = calloc(n_new, sizeof(double*));
    for (int i = 0; i < n_new; i++) adj_mat[i] = calloc(n_new, sizeof(double));

    for (int i = 0; i < E_orig; i++) {
        int u = edges[i].node1, v = edges[i].node2;
        double w = edges[i].weight;
        int cu = node_comm[u], cv = node_comm[v];
        if (cu == cv) adj_mat[cu][cu] += w;
        else {
            adj_mat[cu][cv] += w;
            adj_mat[cv][cu] += w;
        }
    }

    // Conversion en CSR
    *new_offsets = malloc((n_new + 1) * sizeof(int));
    int *deg_new = calloc(n_new, sizeof(int));
    for (int i = 0; i < n_new; i++) {
        for (int j = 0; j < n_new; j++) {
            if (adj_mat[i][j] > 0) deg_new[i]++;
        }
    }
    (*new_offsets)[0] = 0;
    for (int i = 0; i < n_new; i++) {
        (*new_offsets)[i+1] = (*new_offsets)[i] + deg_new[i];
    }
    int new_total_edges = (*new_offsets)[n_new];
    *new_neighbors = malloc(new_total_edges * sizeof(int));
    *new_weights = malloc(new_total_edges * sizeof(double));

    int *pos = malloc(n_new * sizeof(int));
    memcpy(pos, *new_offsets, n_new * sizeof(int));
    for (int i = 0; i < n_new; i++) {
        for (int j = 0; j < n_new; j++) {
            if (adj_mat[i][j] > 0) {
                int p = pos[i]++;
                (*new_neighbors)[p] = j;
                (*new_weights)[p] = adj_mat[i][j];
            }
        }
    }
    free(pos);
    free(deg_new);

    // Libération de la matrice temporaire
    for (int i = 0; i < n_new; i++) free(adj_mat[i]);
    free(adj_mat);
    free(compress);
    free(new_partition); // new_partition n'est plus utile ici (on retourne seulement le CSR)

    return n_new;
}

// ------------------------------------------------------------
// Algorithme de Louvain
// ------------------------------------------------------------
int louvain_method() {
    int N_orig = num_nodes;
    int E_orig = num_edges;

    double *deg_orig = calloc(N_orig, sizeof(double));
    double total_weight_orig = 0.0;
    for (int i = 0; i < E_orig; i++) {
        int u = edges[i].node1, v = edges[i].node2;
        double w = edges[i].weight;
        deg_orig[u] += w;
        deg_orig[v] += w;
        total_weight_orig += w;
    }

    int *node_comm = malloc(N_orig * sizeof(int));
    for (int i = 0; i < N_orig; i++) node_comm[i] = i;

    int n_cur = N_orig;
    int *offsets = malloc((n_cur + 1) * sizeof(int));
    int *neighbors = malloc(csr_total_edges * sizeof(int));
    double *weights = malloc(csr_total_edges * sizeof(double));
    memcpy(offsets, csr_offsets, (n_cur + 1) * sizeof(int));
    memcpy(neighbors, csr_neighbors, csr_total_edges * sizeof(int));
    memcpy(weights, csr_weights, csr_total_edges * sizeof(double));
    int total_edges = csr_total_edges;
    double total_weight = total_weight_orig;

    int *partition = malloc(n_cur * sizeof(int));
    for (int i = 0; i < n_cur; i++) partition[i] = i;

    double *comm_deg = malloc(n_cur * sizeof(double));
    memcpy(comm_deg, deg_orig, n_cur * sizeof(double));

    int *mark = calloc(n_cur, sizeof(int));
    int current_mark = 1;
    bool improvement = true;

    while (improvement) {
        improvement = false;

        // Mouvements locaux
        for (int node = 0; node < n_cur; node++) {
            int old_comm = partition[node];
            double k_in_old = 0.0;
            for (int idx = offsets[node]; idx < offsets[node+1]; idx++) {
                int neigh = neighbors[idx];
                if (partition[neigh] == old_comm) k_in_old += weights[idx];
            }

            double best_delta = 0.0;
            int best_comm = old_comm;

            for (int idx = offsets[node]; idx < offsets[node+1]; idx++) {
                int neigh = neighbors[idx];
                int neigh_comm = partition[neigh];
                if (neigh_comm == old_comm) continue;
                if (mark[neigh_comm] != current_mark) {
                    mark[neigh_comm] = current_mark;

                    double k_in_new = 0.0;
                    for (int jdx = offsets[node]; jdx < offsets[node+1]; jdx++) {
                        int n2 = neighbors[jdx];
                        if (partition[n2] == neigh_comm) k_in_new += weights[jdx];
                    }

                    double delta = (k_in_new - k_in_old) -
                                   (deg_orig[node] * (comm_deg[neigh_comm] - comm_deg[old_comm])) /
                                   (2.0 * total_weight);
                    if (delta > best_delta) {
                        best_delta = delta;
                        best_comm = neigh_comm;
                    }
                }
            }
            current_mark++;

            if (best_delta > 1e-12 && best_comm != old_comm) {
                partition[node] = best_comm;
                comm_deg[old_comm] -= deg_orig[node];
                comm_deg[best_comm] += deg_orig[node];
                improvement = true;
            }
        }

        if (!improvement) break;

        // Agrégation
        int *new_offsets, *new_neighbors;
        double *new_weights;
        int n_new = aggregate_and_build_csr(n_cur, partition, N_orig, E_orig,
                                            node_comm, edges, &new_offsets,
                                            &new_neighbors, &new_weights);

        // Libérer l'ancien CSR local
        free(offsets);
        free(neighbors);
        free(weights);

        offsets = new_offsets;
        neighbors = new_neighbors;
        weights = new_weights;
        total_edges = (offsets[n_new] - offsets[0]);

        // Recalculer total_weight et deg_orig
        total_weight = 0.0;
        for (int i = 0; i < total_edges; i++) total_weight += weights[i];

        double *new_deg_orig = malloc(n_new * sizeof(double));
        for (int i = 0; i < n_new; i++) {
            new_deg_orig[i] = 0.0;
            for (int idx = offsets[i]; idx < offsets[i+1]; idx++) {
                int j = neighbors[idx];
                double w = weights[idx];
                if (j == i) new_deg_orig[i] += 2.0 * w;
                else new_deg_orig[i] += w;
            }
        }
        free(deg_orig);
        deg_orig = new_deg_orig;

        double *new_comm_deg = malloc(n_new * sizeof(double));
        memcpy(new_comm_deg, deg_orig, n_new * sizeof(double));
        free(comm_deg);
        comm_deg = new_comm_deg;

        free(partition);
        partition = malloc(n_new * sizeof(int));
        for (int i = 0; i < n_new; i++) partition[i] = i;

        free(mark);
        mark = calloc(n_new, sizeof(int));
        current_mark = 1;
        n_cur = n_new;
    }

    // Affectation finale
    for (int i = 0; i < N_orig; i++) communities[i] = node_comm[i];

    free(partition);
    free(deg_orig);
    free(comm_deg);
    free(mark);
    free(node_comm);
    free(offsets);
    free(neighbors);
    free(weights);

    return n_cur;
}

// ------------------------------------------------------------
// Louvain sur chaque composante
// ------------------------------------------------------------
int louvain_methodC() {
    find_connected_components();

    int *comp_sizes = calloc(num_components, sizeof(int));
    for (int i = 0; i < num_nodes; i++) {
        int c = node_community_map[i].component;
        comp_sizes[c]++;
    }
    int **comp_nodes = malloc(num_components * sizeof(int*));
    for (int i = 0; i < num_components; i++) {
        comp_nodes[i] = malloc(comp_sizes[i] * sizeof(int));
    }
    int *comp_pos = calloc(num_components, sizeof(int));
    for (int i = 0; i < num_nodes; i++) {
        int c = node_community_map[i].component;
        comp_nodes[c][comp_pos[c]++] = i;
    }
    free(comp_pos);

    Edge *original_edges = malloc(MAX_EDGES * sizeof(Edge));
    memcpy(original_edges, edges, MAX_EDGES * sizeof(Edge));
    int original_num_edges = num_edges;
    int original_num_nodes = num_nodes;

    int *global_labels = malloc(num_nodes * sizeof(int));
    int next_comm_id = 0;

    for (int comp = 0; comp < num_components; comp++) {
        int n_comp = comp_sizes[comp];
        int *nodes = comp_nodes[comp];

        if (n_comp == 1) {
            global_labels[nodes[0]] = next_comm_id++;
            continue;
        }

        int *local_index = malloc(num_nodes * sizeof(int));
        for (int i = 0; i < num_nodes; i++) local_index[i] = -1;
        for (int i = 0; i < n_comp; i++) local_index[nodes[i]] = i;

        int edge_count = 0;
        for (int i = 0; i < n_comp; i++) {
            int u = nodes[i];
            for (int idx = csr_offsets[u]; idx < csr_offsets[u+1]; idx++) {
                int v = csr_neighbors[idx];
                if (local_index[v] != -1 && v > u) edge_count++;
            }
        }

        if (edge_count == 0) {
            for (int i = 0; i < n_comp; i++) global_labels[nodes[i]] = next_comm_id++;
            free(local_index);
            continue;
        }

        Edge *local_edges = malloc(edge_count * sizeof(Edge));
        int e = 0;
        for (int i = 0; i < n_comp; i++) {
            int u = nodes[i];
            for (int idx = csr_offsets[u]; idx < csr_offsets[u+1]; idx++) {
                int v = csr_neighbors[idx];
                if (local_index[v] != -1 && v > u) {
                    local_edges[e].node1 = i;
                    local_edges[e].node2 = local_index[v];
                    local_edges[e].weight = csr_weights[idx];
                    e++;
                }
            }
        }
        assert(e == edge_count);

        memcpy(edges, local_edges, edge_count * sizeof(Edge));
        num_edges = edge_count;
        num_nodes = n_comp;
        build_csr_adjacency();

        int n_local_comm = louvain_method();

        int *local_labels = malloc(n_comp * sizeof(int));
        for (int i = 0; i < n_comp; i++) local_labels[i] = communities[i];

        num_edges = original_num_edges;
        num_nodes = original_num_nodes;
        memcpy(edges, original_edges, MAX_EDGES * sizeof(Edge));
        build_csr_adjacency();

        for (int i = 0; i < n_comp; i++) {
            global_labels[nodes[i]] = next_comm_id + local_labels[i];
        }
        next_comm_id += n_local_comm;

        free(local_edges);
        free(local_labels);
        free(local_index);
    }

    for (int i = 0; i < num_nodes; i++) communities[i] = global_labels[i];

    free(global_labels);
    free(original_edges);
    for (int i = 0; i < num_components; i++) free(comp_nodes[i]);
    free(comp_nodes);
    free(comp_sizes);

    int total = count_unique_communities(communities, num_nodes);
    printf("Total number of communities detected (per component): %d\n", total);
    return total;
}

// ------------------------------------------------------------
// Leiden (modularité)
// ------------------------------------------------------------
int leiden_method() {
    int N_orig = num_nodes;
    int E_orig = num_edges;

    double *deg_orig = calloc(N_orig, sizeof(double));
    double total_weight_orig = 0.0;
    for (int i = 0; i < E_orig; i++) {
        int u = edges[i].node1, v = edges[i].node2;
        double w = edges[i].weight;
        deg_orig[u] += w;
        deg_orig[v] += w;
        total_weight_orig += w;
    }

    int *node_comm = malloc(N_orig * sizeof(int));
    for (int i = 0; i < N_orig; i++) node_comm[i] = i;

    int n_cur = N_orig;
    int *offsets = malloc((n_cur + 1) * sizeof(int));
    int *neighbors = malloc(csr_total_edges * sizeof(int));
    double *weights = malloc(csr_total_edges * sizeof(double));
    memcpy(offsets, csr_offsets, (n_cur + 1) * sizeof(int));
    memcpy(neighbors, csr_neighbors, csr_total_edges * sizeof(int));
    memcpy(weights, csr_weights, csr_total_edges * sizeof(double));
    double total_weight = total_weight_orig;

    int *partition = malloc(n_cur * sizeof(int));
    for (int i = 0; i < n_cur; i++) partition[i] = i;

    double *comm_deg = malloc(n_cur * sizeof(double));
    memcpy(comm_deg, deg_orig, n_cur * sizeof(double));

    bool improvement = true;
    while (improvement) {
        improvement = false;

        // Mouvements locaux
        int *mark = calloc(n_cur, sizeof(int));
        int cur_mark = 1;
        for (int node = 0; node < n_cur; node++) {
            int old_comm = partition[node];
            double k_in_old = 0.0;
            for (int idx = offsets[node]; idx < offsets[node+1]; idx++) {
                int neigh = neighbors[idx];
                if (partition[neigh] == old_comm) k_in_old += weights[idx];
            }

            double best_delta = 0.0;
            int best_comm = old_comm;

            for (int idx = offsets[node]; idx < offsets[node+1]; idx++) {
                int neigh = neighbors[idx];
                int neigh_comm = partition[neigh];
                if (neigh_comm == old_comm) continue;
                if (mark[neigh_comm] != cur_mark) {
                    mark[neigh_comm] = cur_mark;

                    double k_in_new = 0.0;
                    for (int jdx = offsets[node]; jdx < offsets[node+1]; jdx++) {
                        int n2 = neighbors[jdx];
                        if (partition[n2] == neigh_comm) k_in_new += weights[jdx];
                    }

                    double delta = (k_in_new - k_in_old) -
                                   (deg_orig[node] * (comm_deg[neigh_comm] - comm_deg[old_comm])) /
                                   (2.0 * total_weight);
                    if (delta > best_delta) {
                        best_delta = delta;
                        best_comm = neigh_comm;
                    }
                }
            }
            cur_mark++;

            if (best_delta > 1e-12 && best_comm != old_comm) {
                partition[node] = best_comm;
                comm_deg[old_comm] -= deg_orig[node];
                comm_deg[best_comm] += deg_orig[node];
                improvement = true;
            }
        }
        free(mark);
        if (!improvement) break;

        // Raffinement
        int *refined = malloc(n_cur * sizeof(int));
        for (int i = 0; i < n_cur; i++) refined[i] = partition[i];
        bool refined_changed = true;
        while (refined_changed) {
            refined_changed = false;
            for (int node = 0; node < n_cur; node++) {
                int old = refined[node];
                double best_delta = 0.0;
                int best = old;

                double k_in_old = 0.0;
                for (int idx = offsets[node]; idx < offsets[node+1]; idx++) {
                    int neigh = neighbors[idx];
                    if (refined[neigh] == old) k_in_old += weights[idx];
                }

                for (int idx = offsets[node]; idx < offsets[node+1]; idx++) {
                    int neigh = neighbors[idx];
                    int cand = refined[neigh];
                    if (cand == old) continue;

                    double k_in_new = 0.0;
                    for (int jdx = offsets[node]; jdx < offsets[node+1]; jdx++) {
                        int n2 = neighbors[jdx];
                        if (refined[n2] == cand) k_in_new += weights[jdx];
                    }

                    double delta = (k_in_new - k_in_old) -
                                   (deg_orig[node] * (comm_deg[cand] - comm_deg[old])) /
                                   (2.0 * total_weight);
                    if (delta > best_delta) {
                        best_delta = delta;
                        best = cand;
                    }
                }

                if (best != old && best_delta > 1e-12) {
                    refined[node] = best;
                    refined_changed = true;
                }
            }
        }

        // Agrégation
        int *new_offsets, *new_neighbors;
        double *new_weights;
        int n_new = aggregate_and_build_csr(n_cur, refined, N_orig, E_orig,
                                            node_comm, edges, &new_offsets,
                                            &new_neighbors, &new_weights);

        free(offsets);
        free(neighbors);
        free(weights);
        offsets = new_offsets;
        neighbors = new_neighbors;
        weights = new_weights;

        total_weight = 0.0;
        int new_total_edges = (offsets[n_new] - offsets[0]);
        for (int i = 0; i < new_total_edges; i++) total_weight += weights[i];

        double *new_deg_orig = malloc(n_new * sizeof(double));
        for (int i = 0; i < n_new; i++) {
            new_deg_orig[i] = 0.0;
            for (int idx = offsets[i]; idx < offsets[i+1]; idx++) {
                int j = neighbors[idx];
                double w = weights[idx];
                if (j == i) new_deg_orig[i] += 2.0 * w;
                else new_deg_orig[i] += w;
            }
        }
        free(deg_orig);
        deg_orig = new_deg_orig;

        double *new_comm_deg = malloc(n_new * sizeof(double));
        memcpy(new_comm_deg, deg_orig, n_new * sizeof(double));
        free(comm_deg);
        comm_deg = new_comm_deg;

        free(partition);
        partition = malloc(n_new * sizeof(int));
        for (int i = 0; i < n_new; i++) partition[i] = i;

        n_cur = n_new;
        free(refined);
    }

    for (int i = 0; i < N_orig; i++) communities[i] = node_comm[i];

    free(partition);
    free(deg_orig);
    free(comm_deg);
    free(node_comm);
    free(offsets);
    free(neighbors);
    free(weights);

    int uniq = count_unique_communities(communities, N_orig);
    printf("Number of communities detected (Leiden): %d\n", uniq);
    return uniq;
}

// ------------------------------------------------------------
// Leiden CPM
// ------------------------------------------------------------
int leiden_method_CPM() {
    int N_orig = num_nodes;
    int E_orig = num_edges;

    double *deg_orig = calloc(N_orig, sizeof(double));
    for (int i = 0; i < E_orig; i++) {
        int u = edges[i].node1, v = edges[i].node2;
        double w = edges[i].weight;
        deg_orig[u] += w;
        deg_orig[v] += w;
    }

    int *node_comm = malloc(N_orig * sizeof(int));
    for (int i = 0; i < N_orig; i++) node_comm[i] = i;

    int n_cur = N_orig;
    int *offsets = malloc((n_cur + 1) * sizeof(int));
    int *neighbors = malloc(csr_total_edges * sizeof(int));
    double *weights = malloc(csr_total_edges * sizeof(double));
    memcpy(offsets, csr_offsets, (n_cur + 1) * sizeof(int));
    memcpy(neighbors, csr_neighbors, csr_total_edges * sizeof(int));
    memcpy(weights, csr_weights, csr_total_edges * sizeof(double));

    int *partition = malloc(n_cur * sizeof(int));
    for (int i = 0; i < n_cur; i++) partition[i] = i;

    double *comm_deg = malloc(n_cur * sizeof(double));
    memcpy(comm_deg, deg_orig, n_cur * sizeof(double));

    bool improvement = true;
    while (improvement) {
        improvement = false;

        // Mouvements locaux (gain CPM)
        int *mark = calloc(n_cur, sizeof(int));
        int cur_mark = 1;
        for (int node = 0; node < n_cur; node++) {
            int old_comm = partition[node];
            double k_in_old = 0.0;
            for (int idx = offsets[node]; idx < offsets[node+1]; idx++) {
                int neigh = neighbors[idx];
                if (partition[neigh] == old_comm) k_in_old += weights[idx];
            }

            double best_delta = 0.0;
            int best_comm = old_comm;

            for (int idx = offsets[node]; idx < offsets[node+1]; idx++) {
                int neigh = neighbors[idx];
                int neigh_comm = partition[neigh];
                if (neigh_comm == old_comm) continue;
                if (mark[neigh_comm] != cur_mark) {
                    mark[neigh_comm] = cur_mark;

                    double k_in_new = 0.0;
                    for (int jdx = offsets[node]; jdx < offsets[node+1]; jdx++) {
                        int n2 = neighbors[jdx];
                        if (partition[n2] == neigh_comm) k_in_new += weights[jdx];
                    }

                    double deg_i = 0.0;
                    for (int jdx = offsets[node]; jdx < offsets[node+1]; jdx++) {
                        deg_i += weights[jdx];
                    }
                    double size_old = comm_deg[old_comm];
                    double size_new = comm_deg[neigh_comm];
                    double delta = (k_in_new - k_in_old) - lambda * deg_i * (size_new - size_old);

                    if (delta > best_delta) {
                        best_delta = delta;
                        best_comm = neigh_comm;
                    }
                }
            }
            cur_mark++;

            if (best_delta > 1e-12 && best_comm != old_comm) {
                partition[node] = best_comm;
                comm_deg[old_comm] -= deg_orig[node];
                comm_deg[best_comm] += deg_orig[node];
                improvement = true;
            }
        }
        free(mark);
        if (!improvement) break;

        // Raffinement (gain CPM)
        int *refined = malloc(n_cur * sizeof(int));
        for (int i = 0; i < n_cur; i++) refined[i] = partition[i];
        bool refined_changed = true;
        while (refined_changed) {
            refined_changed = false;
            for (int node = 0; node < n_cur; node++) {
                int old = refined[node];
                double best_delta = 0.0;
                int best = old;

                double k_in_old = 0.0;
                for (int idx = offsets[node]; idx < offsets[node+1]; idx++) {
                    int neigh = neighbors[idx];
                    if (refined[neigh] == old) k_in_old += weights[idx];
                }

                for (int idx = offsets[node]; idx < offsets[node+1]; idx++) {
                    int neigh = neighbors[idx];
                    int cand = refined[neigh];
                    if (cand == old) continue;

                    double k_in_new = 0.0;
                    for (int jdx = offsets[node]; jdx < offsets[node+1]; jdx++) {
                        int n2 = neighbors[jdx];
                        if (refined[n2] == cand) k_in_new += weights[jdx];
                    }

                    double deg_i = 0.0;
                    for (int jdx = offsets[node]; jdx < offsets[node+1]; jdx++) {
                        deg_i += weights[jdx];
                    }
                    double size_old = comm_deg[old];
                    double size_new = comm_deg[cand];
                    double delta = (k_in_new - k_in_old) - lambda * deg_i * (size_new - size_old);

                    if (delta > best_delta) {
                        best_delta = delta;
                        best = cand;
                    }
                }

                if (best != old && best_delta > 1e-12) {
                    refined[node] = best;
                    refined_changed = true;
                }
            }
        }

        // Agrégation
        int *new_offsets, *new_neighbors;
        double *new_weights;
        int n_new = aggregate_and_build_csr(n_cur, refined, N_orig, E_orig,
                                            node_comm, edges, &new_offsets,
                                            &new_neighbors, &new_weights);

        free(offsets);
        free(neighbors);
        free(weights);
        offsets = new_offsets;
        neighbors = new_neighbors;
        weights = new_weights;

        double *new_deg_orig = malloc(n_new * sizeof(double));
        for (int i = 0; i < n_new; i++) {
            new_deg_orig[i] = 0.0;
            for (int idx = offsets[i]; idx < offsets[i+1]; idx++) {
                int j = neighbors[idx];
                double w = weights[idx];
                if (j == i) new_deg_orig[i] += 2.0 * w;
                else new_deg_orig[i] += w;
            }
        }
        free(deg_orig);
        deg_orig = new_deg_orig;

        double *new_comm_deg = malloc(n_new * sizeof(double));
        memcpy(new_comm_deg, deg_orig, n_new * sizeof(double));
        free(comm_deg);
        comm_deg = new_comm_deg;

        free(partition);
        partition = malloc(n_new * sizeof(int));
        for (int i = 0; i < n_new; i++) partition[i] = i;

        n_cur = n_new;
        free(refined);
    }

    for (int i = 0; i < N_orig; i++) communities[i] = node_comm[i];

    free(partition);
    free(deg_orig);
    free(comm_deg);
    free(node_comm);
    free(offsets);
    free(neighbors);
    free(weights);

    int uniq = count_unique_communities(communities, N_orig);
    printf("Number of communities detected (Leiden CPM, lambda=%.2f): %d\n",
           lambda, uniq);
    return uniq;
}

// ------------------------------------------------------------
// Fonctions utilitaires
// ------------------------------------------------------------
static int *community_mark = NULL;
static int current_mark = 1;
static int mark_size = 0;

int count_unique_communities(int *communities, int num_nodes) {
    if (num_nodes > mark_size) {
        int new_size = num_nodes;
        community_mark = realloc(community_mark, new_size * sizeof(int));
        for (int i = mark_size; i < new_size; i++) community_mark[i] = 0;
        mark_size = new_size;
    }
    int unique = 0;
    for (int i = 0; i < num_nodes; i++) {
        int c = communities[i];
        if (community_mark[c] != current_mark) {
            community_mark[c] = current_mark;
            unique++;
        }
    }
    current_mark++;
    return unique;
}

void initialize_community_colors() {
    for (int i = 0; i < num_nodes; i++) {
        float min = 0.2f;
        cluster_colors[i][0] = min + (float)rand() / RAND_MAX * (1.0f - min);
        cluster_colors[i][1] = min + (float)rand() / RAND_MAX * (1.0f - min);
        cluster_colors[i][2] = min + (float)rand() / RAND_MAX * (1.0f - min);
    }
}

void compute_ratio_S(int *S) {
    if (num_communities <= 0) {
        printf("Error: num_communities not initialized.\n");
        return;
    }
    int *community_sizes = calloc(num_nodes, sizeof(int));
    int *community_s1_counts = calloc(num_nodes, sizeof(int));

    for (int i = 0; i < num_rows; i++) {
        int comm = communities[i];
        if (comm < 0 || comm >= num_nodes) {
            printf("Error: Invalid community index %d for node %d.\n", comm, i);
            continue;
        }
        community_sizes[comm]++;
        if (S[i] == 1) community_s1_counts[comm]++;
    }

    printf("Community Ratios (>0.5%% of nodes):\n");
    for (int comm = 0; comm < num_nodes; comm++) {
        if (community_sizes[comm] > num_nodes / 200) {
            double ratio = (double)community_s1_counts[comm] / community_sizes[comm];
            printf("Community %d: Size = %d, Ratio of 1 = %.10f\n", comm,
                   community_sizes[comm], ratio);
            fflush(stdout);
        }
    }
    free(community_sizes);
    free(community_s1_counts);
}
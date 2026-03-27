#include "communities.h"
#include "../global.h"

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
    int u = edges[i].node1;
    int v = edges[i].node2;
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
    int u = edges[i].node1;
    int v = edges[i].node2;
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

  double delta =
      new_internal - current_internal -
      resolution_parameter * (degree_node * (new_size - current_size));
  return delta;
}

// ------------------------------------------------------------
// Algorithme de Louvain (version simple)
// ------------------------------------------------------------
int louvain_method() {
  double total_graph_weight = 0.0;

  for (int i = 0; i < num_nodes; i++) {
    node_community_map[i].community = i;
    node_community_map[i].total_weight = 0.0;
  }
  for (int i = 0; i < num_edges; i++) {
    int u = edges[i].node1, v = edges[i].node2;
    double w = edges[i].weight;
    node_community_map[u].total_weight += w;
    node_community_map[v].total_weight += w;
    total_graph_weight += 2 * w;
  }

  find_connected_components();

  int improvement = 1;
  while (improvement) {
    improvement = 0;
    for (int node = 0; node < num_nodes; node++) {
      int cur_comm = node_community_map[node].community;
      double best_delta = 0.0;
      int best_comm = cur_comm;

      for (int idx = csr_offsets[node]; idx < csr_offsets[node + 1]; idx++) {
        int neigh = csr_neighbors[idx];
        int neigh_comm = node_community_map[neigh].community;
        if (neigh_comm == cur_comm)
          continue;
        double delta =
            calculate_gain_modularity(node, neigh_comm, total_graph_weight);
        if (delta > best_delta) {
          best_delta = delta;
          best_comm = neigh_comm;
        }
      }

      if (best_comm != cur_comm) {
        node_community_map[node].community = best_comm;
        improvement = 1;
      }
    }
  }

  for (int i = 0; i < num_nodes; i++)
    communities[i] = node_community_map[i].community;

  int uniq = count_unique_communities(communities, num_nodes);
  printf("Number of communities detected: %d\n", uniq);
  return uniq;
}

// ------------------------------------------------------------
// Louvain sur chaque composante (louvain_methodC)
// ------------------------------------------------------------
void apply_louvain_to_component(int component) {
  double total_graph_weight = 0.0;

  // Réinitialisation pour la composante
  for (int i = 0; i < num_nodes; i++) {
    if (node_community_map[i].component == component) {
      node_community_map[i].community = i;
      node_community_map[i].total_weight = 0.0;
    }
  }

  for (int i = 0; i < num_edges; i++) {
    int u = edges[i].node1, v = edges[i].node2;
    double w = edges[i].weight;
    if (node_community_map[u].component == component &&
        node_community_map[v].component == component) {
      node_community_map[u].total_weight += w;
      node_community_map[v].total_weight += w;
      total_graph_weight += 2 * w;
    }
  }

  int improvement = 1;
  while (improvement) {
    improvement = 0;
    for (int node = 0; node < num_nodes; node++) {
      if (node_community_map[node].component != component)
        continue;

      int cur_comm = node_community_map[node].community;
      double best_delta = 0.0;
      int best_comm = cur_comm;

      for (int idx = csr_offsets[node]; idx < csr_offsets[node + 1]; idx++) {
        int neigh = csr_neighbors[idx];
        if (node_community_map[neigh].component != component)
          continue;
        int neigh_comm = node_community_map[neigh].community;
        if (neigh_comm == cur_comm)
          continue;

        double delta =
            calculate_gain_modularity(node, neigh_comm, total_graph_weight);
        if (delta > best_delta) {
          best_delta = delta;
          best_comm = neigh_comm;
        }
      }

      if (best_comm != cur_comm) {
        node_community_map[node].community = best_comm;
        improvement = 1;
      }
    }
  }

  for (int i = 0; i < num_nodes; i++) {
    if (node_community_map[i].component == component)
      communities[i] = node_community_map[i].community;
  }
}

int louvain_methodC() {
  for (int i = 0; i < num_nodes; i++)
    node_community_map[i].component = -1;
  find_connected_components();

  for (int comp = 0; comp < num_components; comp++)
    apply_louvain_to_component(comp);

  int total = count_unique_communities(communities, num_nodes);
  printf("Total number of communities detected: %d\n", total);
  return total;
}

// ------------------------------------------------------------
// Leiden (simple)
// ------------------------------------------------------------
int leiden_method() {
  double total_graph_weight = 0.0;

  for (int i = 0; i < num_nodes; i++) {
    node_community_map[i].community = i;
    node_community_map[i].total_weight = 0.0;
  }
  for (int i = 0; i < num_edges; i++) {
    int u = edges[i].node1, v = edges[i].node2;
    double w = edges[i].weight;
    node_community_map[u].total_weight += w;
    node_community_map[v].total_weight += w;
    total_graph_weight += 2 * w;
  }

  int improvement = 1;
  while (improvement) {
    improvement = 0;
    for (int node = 0; node < num_nodes; node++) {
      int cur_comm = node_community_map[node].community;
      double best_delta = 0.0;
      int best_comm = cur_comm;

      for (int idx = csr_offsets[node]; idx < csr_offsets[node + 1]; idx++) {
        int neigh = csr_neighbors[idx];
        int neigh_comm = node_community_map[neigh].community;
        if (neigh_comm == cur_comm)
          continue;
        double delta =
            calculate_gain_modularity(node, neigh_comm, total_graph_weight);
        if (delta > best_delta) {
          best_delta = delta;
          best_comm = neigh_comm;
        }
      }

      if (best_comm != cur_comm) {
        node_community_map[node].community = best_comm;
        improvement = 1;
      }
    }
  }

  for (int i = 0; i < num_nodes; i++)
    communities[i] = node_community_map[i].community;

  int uniq = count_unique_communities(communities, num_nodes);
  printf("Number of communities detected: %d\n", uniq);
  return uniq;
}

// ------------------------------------------------------------
// Leiden CPM
// ------------------------------------------------------------
int leiden_method_CPM() {
  double total_graph_weight = 0.0;

  for (int i = 0; i < num_nodes; i++) {
    node_community_map[i].community = i;
    node_community_map[i].total_weight = 0.0;
  }
  for (int i = 0; i < num_edges; i++) {
    int u = edges[i].node1, v = edges[i].node2;
    double w = edges[i].weight;
    node_community_map[u].total_weight += w;
    node_community_map[v].total_weight += w;
    total_graph_weight += 2 * w;
  }

  int improvement = 1;
  while (improvement) {
    improvement = 0;
    for (int node = 0; node < num_nodes; node++) {
      int cur_comm = node_community_map[node].community;
      double best_delta = 0.0;
      int best_comm = cur_comm;

      for (int idx = csr_offsets[node]; idx < csr_offsets[node + 1]; idx++) {
        int neigh = csr_neighbors[idx];
        int neigh_comm = node_community_map[neigh].community;
        if (neigh_comm == cur_comm)
          continue;

        double delta = calculate_gain_modularity_cpm(node, neigh_comm, lambda);
        if (delta > best_delta) {
          best_delta = delta;
          best_comm = neigh_comm;
        }
      }

      if (best_comm != cur_comm) {
        node_community_map[node].community = best_comm;
        improvement = 1;
      }
    }
  }

  for (int i = 0; i < num_nodes; i++)
    communities[i] = node_community_map[i].community;

  int uniq = count_unique_communities(communities, num_nodes);
  printf("Number of communities detected: %d\n", uniq);
  return uniq;
}

// ------------------------------------------------------------
// Fonctions utilitaires
// ------------------------------------------------------------
int count_unique_communities(int *communities, int num_nodes) {
  bool *seen = calloc(num_nodes, sizeof(bool));
  int unique = 0;
  for (int i = 0; i < num_nodes; i++) {
    int c = communities[i];
    if (!seen[c]) {
      seen[c] = true;
      unique++;
    }
  }
  free(seen);
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
    if (S[i] == 1)
      community_s1_counts[comm]++;
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
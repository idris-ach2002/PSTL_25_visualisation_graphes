#include "communities.h"
#include "../global.h"

#include <assert.h>
#include <limits.h>
#include <math.h>
#include <stdbool.h>
#include <stdio.h>
#include <stdlib.h>
#include <string.h>

#ifndef COMMUNITY_EPS
#define COMMUNITY_EPS 1e-12
#endif

double lambda = 0.1;
int *csr_offsets = NULL;
int *csr_neighbors = NULL;
double *csr_weights = NULL;
int csr_total_edges = 0;
Community node_community_map[MAX_NODES];
int num_communities = 0;
int component_sizes[MAX_NODES];
int num_components = 0;

typedef enum { OBJECTIVE_MODULARITY = 0, OBJECTIVE_CPM = 1 } ObjectiveMode;

typedef struct {
  int a;
  int b;
  double w;
} PairEdge;

static int compare_pair_edge(const void *a, const void *b) {
  const PairEdge *ea = (const PairEdge *)a;
  const PairEdge *eb = (const PairEdge *)b;
  if (ea->a != eb->a)
    return (ea->a < eb->a) ? -1 : 1;
  if (ea->b != eb->b)
    return (ea->b < eb->b) ? -1 : 1;
  return 0;
}

static void free_ptr(void **ptr) {
  if (ptr != NULL && *ptr != NULL) {
    free(*ptr);
    *ptr = NULL;
  }
}

void free_csr_adjacency(void) {
  free_ptr((void **)&csr_offsets);
  free_ptr((void **)&csr_neighbors);
  free_ptr((void **)&csr_weights);
  csr_total_edges = 0;
}

static int build_csr_from_edge_array(int n, int m, const Edge *edge_array,
                                     int **offsets_out, int **neighbors_out,
                                     double **weights_out, double **degree_out,
                                     double *m2_out) {
  if (offsets_out == NULL || neighbors_out == NULL || weights_out == NULL ||
      degree_out == NULL || m2_out == NULL) {
    return -1;
  }

  *offsets_out = NULL;
  *neighbors_out = NULL;
  *weights_out = NULL;
  *degree_out = NULL;
  *m2_out = 0.0;

  if (n < 0 || m < 0)
    return -1;

  int *adj_count = calloc((size_t)(n > 0 ? n : 1), sizeof(int));
  double *degree = calloc((size_t)(n > 0 ? n : 1), sizeof(double));
  if (adj_count == NULL || degree == NULL) {
    free(adj_count);
    free(degree);
    return -1;
  }

  double m2 = 0.0;
  for (int i = 0; i < m; i++) {
    int u = edge_array[i].node1;
    int v = edge_array[i].node2;
    double w = edge_array[i].weight;

    if (u < 0 || u >= n || v < 0 || v >= n)
      continue;
    if (w <= 0.0)
      continue;

    if (u == v) {
      adj_count[u] += 1;
      degree[u] += 2.0 * w;
      m2 += 2.0 * w;
    } else {
      adj_count[u] += 1;
      adj_count[v] += 1;
      degree[u] += w;
      degree[v] += w;
      m2 += 2.0 * w;
    }
  }

  int *offsets = malloc((size_t)(n + 1) * sizeof(int));
  if (offsets == NULL) {
    free(adj_count);
    free(degree);
    return -1;
  }

  offsets[0] = 0;
  for (int i = 0; i < n; i++) {
    offsets[i + 1] = offsets[i] + adj_count[i];
  }

  int total_adj = offsets[n];
  int *neighbors = NULL;
  double *weights = NULL;

  if (total_adj > 0) {
    neighbors = malloc((size_t)total_adj * sizeof(int));
    weights = malloc((size_t)total_adj * sizeof(double));
    if (neighbors == NULL || weights == NULL) {
      free(offsets);
      free(neighbors);
      free(weights);
      free(adj_count);
      free(degree);
      return -1;
    }
  }

  int *pos = malloc((size_t)(n > 0 ? n : 1) * sizeof(int));
  if (pos == NULL) {
    free(offsets);
    free(neighbors);
    free(weights);
    free(adj_count);
    free(degree);
    return -1;
  }
  if (n > 0) {
    memcpy(pos, offsets, (size_t)n * sizeof(int));
  }

  for (int i = 0; i < m; i++) {
    int u = edge_array[i].node1;
    int v = edge_array[i].node2;
    double w = edge_array[i].weight;

    if (u < 0 || u >= n || v < 0 || v >= n)
      continue;
    if (w <= 0.0)
      continue;

    if (u == v) {
      int p = pos[u]++;
      neighbors[p] = u;
      weights[p] = w;
    } else {
      int pu = pos[u]++;
      neighbors[pu] = v;
      weights[pu] = w;
      int pv = pos[v]++;
      neighbors[pv] = u;
      weights[pv] = w;
    }
  }

  free(adj_count);
  free(pos);

  *offsets_out = offsets;
  *neighbors_out = neighbors;
  *weights_out = weights;
  *degree_out = degree;
  *m2_out = m2;
  return total_adj;
}

void build_csr_adjacency(void) {
  free_csr_adjacency();

  double *degree = NULL;
  int total = build_csr_from_edge_array(
      num_nodes, num_edges, edges, &csr_offsets, &csr_neighbors, &csr_weights,
      &degree, &((double){0.0}));
  if (total < 0) {
    fprintf(stderr, "Failed to build CSR adjacency.\n");
    free_csr_adjacency();
    return;
  }
  csr_total_edges = total;
  free(degree);
}

void initialize_adjacency_list(void) {}
void add_edge_to_adjacency_list(int node, int neighbor, double weight) {
  (void)node;
  (void)neighbor;
  (void)weight;
}
void apply_louvain_to_component(int component) { (void)component; }

// ------------------------------------------------------------
// Connected components on the global CSR
// ------------------------------------------------------------
void mark_component(int node, int component, int *stack) {
  if (stack == NULL || csr_offsets == NULL || csr_neighbors == NULL)
    return;

  int top = 0;
  stack[top++] = node;
  node_community_map[node].component = component;

  while (top > 0) {
    int cur = stack[--top];
    component_sizes[component]++;

    for (int idx = csr_offsets[cur]; idx < csr_offsets[cur + 1]; idx++) {
      int neigh = csr_neighbors[idx];
      if (node_community_map[neigh].component == -1) {
        node_community_map[neigh].component = component;
        stack[top++] = neigh;
      }
    }
  }
}

void find_connected_components() {
  if (csr_offsets == NULL)
    build_csr_adjacency();

  for (int i = 0; i < num_nodes; i++) {
    node_community_map[i].component = -1;
  }
  for (int i = 0; i < MAX_NODES; i++) {
    component_sizes[i] = 0;
  }
  num_components = 0;

  if (num_nodes <= 0 || csr_offsets == NULL) {
    return;
  }

  int *stack = malloc((size_t)num_nodes * sizeof(int));
  if (stack == NULL) {
    fprintf(stderr, "Failed to allocate component stack.\n");
    return;
  }

  for (int i = 0; i < num_nodes; i++) {
    if (node_community_map[i].component == -1) {
      mark_component(i, num_components, stack);
      num_components++;
    }
  }

  free(stack);
  printf("Number of connected components: %d\n", num_components);
}

// ------------------------------------------------------------
// Utility helpers
// ------------------------------------------------------------
static void shuffle_indices(int *array, int n) {
  for (int i = n - 1; i > 0; i--) {
    int j = rand() % (i + 1);
    int tmp = array[i];
    array[i] = array[j];
    array[j] = tmp;
  }
}

static int renumber_labels_inplace(int *labels, int n) {
  int *map = malloc((size_t)(n > 0 ? n : 1) * sizeof(int));
  if (map == NULL) {
    return -1;
  }
  for (int i = 0; i < n; i++)
    map[i] = -1;

  int next = 0;
  for (int i = 0; i < n; i++) {
    int c = labels[i];
    if (c < 0 || c >= n)
      continue;
    if (map[c] == -1)
      map[c] = next++;
  }
  for (int i = 0; i < n; i++) {
    labels[i] = map[labels[i]];
  }

  free(map);
  return next;
}

static int local_move_pass(int n, const int *offsets, const int *neighbors,
                           const double *weights, const double *degree,
                           double m2, const int *node_size,
                           ObjectiveMode objective, int *partition,
                           bool random_order) {
  if (n <= 0)
    return 0;

  double *comm_tot = calloc((size_t)n, sizeof(double));
  int *comm_size = calloc((size_t)n, sizeof(int));
  int *mark = calloc((size_t)n, sizeof(int));
  double *cand_weight = calloc((size_t)n, sizeof(double));
  int *touched = malloc((size_t)n * sizeof(int));
  int *order = malloc((size_t)n * sizeof(int));

  if (comm_tot == NULL || comm_size == NULL || mark == NULL ||
      cand_weight == NULL || touched == NULL || order == NULL) {
    free(comm_tot);
    free(comm_size);
    free(mark);
    free(cand_weight);
    free(touched);
    free(order);
    return 0;
  }

  for (int i = 0; i < n; i++) {
    int c = partition[i];
    if (c >= 0 && c < n) {
      comm_tot[c] += degree[i];
      comm_size[c] += node_size != NULL ? node_size[i] : 1;
    }
    order[i] = i;
  }

  int total_moves = 0;
  int current_mark_local = 1;

  for (int pass = 0; pass < 100; pass++) {
    if (random_order) {
      shuffle_indices(order, n);
    }

    int moved_this_pass = 0;
    for (int pos = 0; pos < n; pos++) {
      int node = order[pos];
      int old_comm = partition[node];
      double ki = degree[node];
      int size_i = node_size != NULL ? node_size[node] : 1;

      if (old_comm < 0 || old_comm >= n)
        continue;

      int nt = 0;
      if (current_mark_local == INT_MAX) {
        memset(mark, 0, (size_t)n * sizeof(int));
        current_mark_local = 1;
      }
      current_mark_local++;

      for (int idx = offsets[node]; idx < offsets[node + 1]; idx++) {
        int neigh = neighbors[idx];
        int c = partition[neigh];
        if (c < 0 || c >= n)
          continue;
        if (mark[c] != current_mark_local) {
          mark[c] = current_mark_local;
          cand_weight[c] = 0.0;
          touched[nt++] = c;
        }
        cand_weight[c] += weights[idx];
      }

      double ki_in_old =
          (mark[old_comm] == current_mark_local) ? cand_weight[old_comm] : 0.0;

      comm_tot[old_comm] -= ki;
      comm_size[old_comm] -= size_i;
      partition[node] = -1;

      double best_gain;
      if (objective == OBJECTIVE_MODULARITY) {
        best_gain =
            ki_in_old - (m2 > 0.0 ? (ki * comm_tot[old_comm]) / m2 : 0.0);
      } else {
        best_gain =
            ki_in_old - lambda * (double)size_i * (double)comm_size[old_comm];
      }
      int best_comm = old_comm;

      for (int t = 0; t < nt; t++) {
        int c = touched[t];
        if (c == old_comm)
          continue;

        double gain;
        if (objective == OBJECTIVE_MODULARITY) {
          gain = cand_weight[c] - (m2 > 0.0 ? (ki * comm_tot[c]) / m2 : 0.0);
        } else {
          gain =
              cand_weight[c] - lambda * (double)size_i * (double)comm_size[c];
        }

        if (gain > best_gain + COMMUNITY_EPS ||
            (fabs(gain - best_gain) <= COMMUNITY_EPS && c < best_comm)) {
          best_gain = gain;
          best_comm = c;
        }
      }

      partition[node] = best_comm;
      comm_tot[best_comm] += ki;
      comm_size[best_comm] += size_i;

      if (best_comm != old_comm) {
        moved_this_pass++;
      }
    }

    total_moves += moved_this_pass;
    if (moved_this_pass == 0)
      break;
  }

  free(comm_tot);
  free(comm_size);
  free(mark);
  free(cand_weight);
  free(touched);
  free(order);
  return total_moves;
}

static int refine_partition_within_coarse(
    int n, const int *offsets, const int *neighbors, const double *weights,
    const double *degree, double m2, const int *node_size,
    const int *coarse_partition, ObjectiveMode objective,
    int *refined_partition_out) {
  if (n <= 0)
    return 0;

  for (int i = 0; i < n; i++) {
    refined_partition_out[i] = i;
  }

  double *comm_tot = calloc((size_t)n, sizeof(double));
  int *comm_size = calloc((size_t)n, sizeof(int));
  int *mark = calloc((size_t)n, sizeof(int));
  double *cand_weight = calloc((size_t)n, sizeof(double));
  int *touched = malloc((size_t)n * sizeof(int));
  int *order = malloc((size_t)n * sizeof(int));

  if (comm_tot == NULL || comm_size == NULL || mark == NULL ||
      cand_weight == NULL || touched == NULL || order == NULL) {
    free(comm_tot);
    free(comm_size);
    free(mark);
    free(cand_weight);
    free(touched);
    free(order);
    memcpy(refined_partition_out, coarse_partition, (size_t)n * sizeof(int));
    return renumber_labels_inplace(refined_partition_out, n);
  }

  for (int i = 0; i < n; i++) {
    comm_tot[i] = degree[i];
    comm_size[i] = node_size != NULL ? node_size[i] : 1;
    order[i] = i;
  }

  int current_mark_local = 1;

  for (int pass = 0; pass < 100; pass++) {
    shuffle_indices(order, n);
    int moved_this_pass = 0;

    for (int pos = 0; pos < n; pos++) {
      int node = order[pos];
      int old_comm = refined_partition_out[node];
      double ki = degree[node];
      int size_i = node_size != NULL ? node_size[node] : 1;
      int coarse_comm = coarse_partition[node];

      int nt = 0;
      if (current_mark_local == INT_MAX) {
        memset(mark, 0, (size_t)n * sizeof(int));
        current_mark_local = 1;
      }
      current_mark_local++;

      for (int idx = offsets[node]; idx < offsets[node + 1]; idx++) {
        int neigh = neighbors[idx];
        if (coarse_partition[neigh] != coarse_comm)
          continue;
        int c = refined_partition_out[neigh];
        if (c < 0 || c >= n)
          continue;
        if (mark[c] != current_mark_local) {
          mark[c] = current_mark_local;
          cand_weight[c] = 0.0;
          touched[nt++] = c;
        }
        cand_weight[c] += weights[idx];
      }

      double ki_in_old =
          (mark[old_comm] == current_mark_local) ? cand_weight[old_comm] : 0.0;

      comm_tot[old_comm] -= ki;
      comm_size[old_comm] -= size_i;
      refined_partition_out[node] = -1;

      double best_gain;
      if (objective == OBJECTIVE_MODULARITY) {
        best_gain =
            ki_in_old - (m2 > 0.0 ? (ki * comm_tot[old_comm]) / m2 : 0.0);
      } else {
        best_gain =
            ki_in_old - lambda * (double)size_i * (double)comm_size[old_comm];
      }
      int best_comm = old_comm;

      for (int t = 0; t < nt; t++) {
        int c = touched[t];
        if (c == old_comm)
          continue;
        double gain;
        if (objective == OBJECTIVE_MODULARITY) {
          gain = cand_weight[c] - (m2 > 0.0 ? (ki * comm_tot[c]) / m2 : 0.0);
        } else {
          gain =
              cand_weight[c] - lambda * (double)size_i * (double)comm_size[c];
        }
        if (gain > best_gain + COMMUNITY_EPS ||
            (fabs(gain - best_gain) <= COMMUNITY_EPS && c < best_comm)) {
          best_gain = gain;
          best_comm = c;
        }
      }

      refined_partition_out[node] = best_comm;
      comm_tot[best_comm] += ki;
      comm_size[best_comm] += size_i;

      if (best_comm != old_comm) {
        moved_this_pass++;
      }
    }

    if (moved_this_pass == 0)
      break;
  }

  free(comm_tot);
  free(comm_size);
  free(mark);
  free(cand_weight);
  free(touched);
  free(order);
  return renumber_labels_inplace(refined_partition_out, n);
}

static int build_induced_graph_from_partition(int n, int m,
                                              const Edge *edge_array,
                                              const int *partition,
                                              const int *node_size, int new_n,
                                              Edge **new_edges_out,
                                              int **new_node_size_out) {
  if (new_edges_out == NULL || new_node_size_out == NULL)
    return -1;

  *new_edges_out = NULL;
  *new_node_size_out = NULL;

  if (new_n <= 0)
    return 0;

  int *new_node_size = calloc((size_t)new_n, sizeof(int));
  if (new_node_size == NULL)
    return -1;

  for (int i = 0; i < n; i++) {
    int c = partition[i];
    if (c >= 0 && c < new_n) {
      new_node_size[c] += node_size != NULL ? node_size[i] : 1;
    }
  }

  if (m <= 0) {
    *new_node_size_out = new_node_size;
    *new_edges_out = NULL;
    return 0;
  }

  PairEdge *tmp = malloc((size_t)m * sizeof(PairEdge));
  if (tmp == NULL) {
    free(new_node_size);
    return -1;
  }

  int cnt = 0;
  for (int i = 0; i < m; i++) {
    int u = edge_array[i].node1;
    int v = edge_array[i].node2;
    double w = edge_array[i].weight;
    if (u < 0 || u >= n || v < 0 || v >= n || w <= 0.0)
      continue;

    int a = partition[u];
    int b = partition[v];
    if (a < 0 || a >= new_n || b < 0 || b >= new_n)
      continue;
    if (a > b) {
      int t = a;
      a = b;
      b = t;
    }
    tmp[cnt].a = a;
    tmp[cnt].b = b;
    tmp[cnt].w = w;
    cnt++;
  }

  if (cnt == 0) {
    free(tmp);
    *new_node_size_out = new_node_size;
    *new_edges_out = NULL;
    return 0;
  }

  qsort(tmp, (size_t)cnt, sizeof(PairEdge), compare_pair_edge);

  Edge *new_edges = malloc((size_t)cnt * sizeof(Edge));
  if (new_edges == NULL) {
    free(tmp);
    free(new_node_size);
    return -1;
  }

  int out_m = 0;
  for (int i = 0; i < cnt;) {
    int a = tmp[i].a;
    int b = tmp[i].b;
    double sum_w = 0.0;
    int j = i;
    while (j < cnt && tmp[j].a == a && tmp[j].b == b) {
      sum_w += tmp[j].w;
      j++;
    }
    new_edges[out_m].node1 = a;
    new_edges[out_m].node2 = b;
    new_edges[out_m].weight = sum_w;
    out_m++;
    i = j;
  }

  free(tmp);
  *new_edges_out = new_edges;
  *new_node_size_out = new_node_size;
  return out_m;
}

static int run_multilevel_detection(int n0, int m0, const Edge *edges0,
                                    ObjectiveMode objective,
                                    bool use_refinement, int *labels_out) {
  if (labels_out == NULL)
    return -1;

  if (n0 <= 0) {
    return 0;
  }

  int *orig_to_cur = malloc((size_t)n0 * sizeof(int));
  int *node_size_cur = malloc((size_t)n0 * sizeof(int));
  Edge *edges_cur = NULL;

  if (orig_to_cur == NULL || node_size_cur == NULL) {
    free(orig_to_cur);
    free(node_size_cur);
    return -1;
  }

  for (int i = 0; i < n0; i++) {
    orig_to_cur[i] = i;
    node_size_cur[i] = 1;
  }

  if (m0 > 0) {
    edges_cur = malloc((size_t)m0 * sizeof(Edge));
    if (edges_cur == NULL) {
      free(orig_to_cur);
      free(node_size_cur);
      return -1;
    }
    memcpy(edges_cur, edges0, (size_t)m0 * sizeof(Edge));
  }

  int n_cur = n0;
  int m_cur = m0;

  while (true) {
    int *offsets = NULL;
    int *neighbors = NULL;
    double *weights = NULL;
    double *degree = NULL;
    double m2 = 0.0;

    int total_adj = build_csr_from_edge_array(
        n_cur, m_cur, edges_cur, &offsets, &neighbors, &weights, &degree, &m2);
    if (total_adj < 0) {
      free(orig_to_cur);
      free(node_size_cur);
      free(edges_cur);
      return -1;
    }

    int *coarse = malloc((size_t)n_cur * sizeof(int));
    if (coarse == NULL) {
      free(offsets);
      free(neighbors);
      free(weights);
      free(degree);
      free(orig_to_cur);
      free(node_size_cur);
      free(edges_cur);
      return -1;
    }
    for (int i = 0; i < n_cur; i++) {
      coarse[i] = i;
    }

    local_move_pass(n_cur, offsets, neighbors, weights, degree, m2,
                    node_size_cur, objective, coarse, true);
    int coarse_count = renumber_labels_inplace(coarse, n_cur);

    if (coarse_count < 0) {
      free(coarse);
      free(offsets);
      free(neighbors);
      free(weights);
      free(degree);
      free(orig_to_cur);
      free(node_size_cur);
      free(edges_cur);
      return -1;
    }

    if (coarse_count == n_cur) {
      free(coarse);
      free(offsets);
      free(neighbors);
      free(weights);
      free(degree);
      break;
    }

    int *partition_to_use = coarse;
    int new_n = coarse_count;
    int *refined = NULL;

    if (use_refinement) {
      refined = malloc((size_t)n_cur * sizeof(int));
      if (refined != NULL) {
        int refined_count = refine_partition_within_coarse(
            n_cur, offsets, neighbors, weights, degree, m2, node_size_cur,
            coarse, objective, refined);

        if (refined_count > 0 && refined_count < n_cur) {
          partition_to_use = refined;
          new_n = refined_count;
          free(coarse);
          coarse = NULL;
        } else {
          free(refined);
          refined = NULL;
        }
      }
    }

    for (int i = 0; i < n0; i++) {
      int cur_node = orig_to_cur[i];
      orig_to_cur[i] = partition_to_use[cur_node];
    }

    Edge *new_edges = NULL;
    int *new_node_size = NULL;
    int new_m = build_induced_graph_from_partition(
        n_cur, m_cur, edges_cur, partition_to_use, node_size_cur, new_n,
        &new_edges, &new_node_size);

    free(coarse);
    free(refined);
    free(offsets);
    free(neighbors);
    free(weights);
    free(degree);
    free(edges_cur);
    free(node_size_cur);

    if (new_m < 0 || new_node_size == NULL) {
      free(new_edges);
      free(new_node_size);
      free(orig_to_cur);
      return -1;
    }

    edges_cur = new_edges;
    node_size_cur = new_node_size;
    n_cur = new_n;
    m_cur = new_m;

    if (n_cur <= 1)
      break;
  }

  memcpy(labels_out, orig_to_cur, (size_t)n0 * sizeof(int));
  int final_count = renumber_labels_inplace(labels_out, n0);

  free(orig_to_cur);
  free(node_size_cur);
  free(edges_cur);
  return final_count;
}

static void fill_node_community_map_from_result(void) {
  double *community_weight =
      calloc((size_t)(num_nodes > 0 ? num_nodes : 1), sizeof(double));
  if (community_weight != NULL) {
    for (int i = 0; i < num_edges; i++) {
      int u = edges[i].node1;
      int v = edges[i].node2;
      double w = edges[i].weight;
      if (u < 0 || u >= num_nodes || v < 0 || v >= num_nodes || w <= 0.0)
        continue;
      int cu = communities[u];
      int cv = communities[v];
      if (cu == cv && cu >= 0 && cu < num_nodes) {
        if (u == v)
          community_weight[cu] += 2.0 * w;
        else
          community_weight[cu] += 2.0 * w;
      }
    }
  }

  for (int i = 0; i < num_nodes; i++) {
    node_community_map[i].community = communities[i];
    node_community_map[i].total_weight =
        community_weight != NULL ? community_weight[communities[i]] : 0.0;
  }

  free(community_weight);
}

// ------------------------------------------------------------
// Public algorithms
// ------------------------------------------------------------
double calculate_gain_modularity(int node, int new_community,
                                 double total_graph_weight) {
  if (csr_offsets == NULL || csr_neighbors == NULL || csr_weights == NULL ||
      total_graph_weight <= 0.0 || node < 0 || node >= num_nodes ||
      new_community < 0 || new_community >= num_nodes) {
    return 0.0;
  }

  int old_community = node_community_map[node].community;
  double ki = 0.0;
  double ki_old = 0.0;
  double ki_new = 0.0;
  double tot_old = 0.0;
  double tot_new = 0.0;

  for (int i = 0; i < num_nodes; i++) {
    double deg_i = 0.0;
    for (int idx = csr_offsets[i]; idx < csr_offsets[i + 1]; idx++) {
      deg_i += csr_weights[idx];
    }
    if (node_community_map[i].community == old_community)
      tot_old += deg_i;
    if (node_community_map[i].community == new_community)
      tot_new += deg_i;
  }

  for (int idx = csr_offsets[node]; idx < csr_offsets[node + 1]; idx++) {
    int neigh = csr_neighbors[idx];
    double w = csr_weights[idx];
    ki += w;
    if (node_community_map[neigh].community == old_community)
      ki_old += w;
    if (node_community_map[neigh].community == new_community)
      ki_new += w;
  }

  tot_old -= ki;
  return (ki_new - (ki * tot_new) / total_graph_weight) -
         (ki_old - (ki * tot_old) / total_graph_weight);
}

double calculate_gain_modularity_cpm(int node, int new_community,
                                     double resolution_parameter) {
  if (csr_offsets == NULL || csr_neighbors == NULL || csr_weights == NULL ||
      node < 0 || node >= num_nodes || new_community < 0 ||
      new_community >= num_nodes) {
    return 0.0;
  }

  int old_community = node_community_map[node].community;
  double ki_old = 0.0;
  double ki_new = 0.0;
  int size_old = 0;
  int size_new = 0;

  for (int i = 0; i < num_nodes; i++) {
    if (node_community_map[i].community == old_community)
      size_old++;
    if (node_community_map[i].community == new_community)
      size_new++;
  }

  for (int idx = csr_offsets[node]; idx < csr_offsets[node + 1]; idx++) {
    int neigh = csr_neighbors[idx];
    double w = csr_weights[idx];
    if (node_community_map[neigh].community == old_community)
      ki_old += w;
    if (node_community_map[neigh].community == new_community)
      ki_new += w;
  }

  return (ki_new - ki_old) +
         resolution_parameter * (double)(size_old - 1 - size_new);
}

int louvain_method() {
  int detected = run_multilevel_detection(
      num_nodes, num_edges, edges, OBJECTIVE_MODULARITY, false, communities);
  if (detected < 0)
    detected = 0;
  num_communities = detected;
  fill_node_community_map_from_result();
  printf("Number of communities detected (Louvain): %d\n", detected);
  return detected;
}

int leiden_method() {
  int detected = run_multilevel_detection(
      num_nodes, num_edges, edges, OBJECTIVE_MODULARITY, true, communities);
  if (detected < 0)
    detected = 0;
  num_communities = detected;
  fill_node_community_map_from_result();
  printf("Number of communities detected (Leiden): %d\n", detected);
  return detected;
}

int leiden_method_CPM() {
  int detected = run_multilevel_detection(num_nodes, num_edges, edges,
                                          OBJECTIVE_CPM, true, communities);
  if (detected < 0)
    detected = 0;
  num_communities = detected;
  fill_node_community_map_from_result();
  printf("Number of communities detected (Leiden CPM, lambda=%.4f): %d\n",
         lambda, detected);
  return detected;
}

int louvain_methodC() {
  if (csr_offsets == NULL)
    build_csr_adjacency();

  find_connected_components();

  if (num_components <= 0) {
    num_communities = 0;
    return 0;
  }

  int *comp_sizes = calloc((size_t)num_components, sizeof(int));
  if (comp_sizes == NULL)
    return 0;

  for (int i = 0; i < num_nodes; i++) {
    int c = node_community_map[i].component;
    if (c >= 0 && c < num_components)
      comp_sizes[c]++;
  }

  int **comp_nodes = malloc((size_t)num_components * sizeof(int *));
  int *comp_pos = calloc((size_t)num_components, sizeof(int));
  if (comp_nodes == NULL || comp_pos == NULL) {
    free(comp_sizes);
    free(comp_nodes);
    free(comp_pos);
    return 0;
  }

  for (int c = 0; c < num_components; c++) {
    comp_nodes[c] = malloc((size_t)comp_sizes[c] * sizeof(int));
    if (comp_nodes[c] == NULL) {
      for (int k = 0; k < c; k++)
        free(comp_nodes[k]);
      free(comp_nodes);
      free(comp_pos);
      free(comp_sizes);
      return 0;
    }
  }

  for (int i = 0; i < num_nodes; i++) {
    int c = node_community_map[i].component;
    comp_nodes[c][comp_pos[c]++] = i;
  }
  free(comp_pos);

  int *global_labels = malloc((size_t)num_nodes * sizeof(int));
  if (global_labels == NULL) {
    for (int c = 0; c < num_components; c++)
      free(comp_nodes[c]);
    free(comp_nodes);
    free(comp_sizes);
    return 0;
  }

  int next_comm_id = 0;
  for (int comp = 0; comp < num_components; comp++) {
    int n_comp = comp_sizes[comp];
    int *nodes = comp_nodes[comp];

    if (n_comp <= 1) {
      global_labels[nodes[0]] = next_comm_id++;
      continue;
    }

    int *local_index = malloc((size_t)num_nodes * sizeof(int));
    if (local_index == NULL) {
      continue;
    }
    for (int i = 0; i < num_nodes; i++)
      local_index[i] = -1;
    for (int i = 0; i < n_comp; i++)
      local_index[nodes[i]] = i;

    int edge_count = 0;
    for (int e = 0; e < num_edges; e++) {
      int u = edges[e].node1;
      int v = edges[e].node2;
      if (u < 0 || u >= num_nodes || v < 0 || v >= num_nodes)
        continue;
      if (node_community_map[u].component == comp &&
          node_community_map[v].component == comp) {
        edge_count++;
      }
    }

    Edge *local_edges = NULL;
    if (edge_count > 0) {
      local_edges = malloc((size_t)edge_count * sizeof(Edge));
    }
    if (edge_count > 0 && local_edges == NULL) {
      free(local_index);
      continue;
    }

    int out_e = 0;
    for (int e = 0; e < num_edges; e++) {
      int u = edges[e].node1;
      int v = edges[e].node2;
      if (u < 0 || u >= num_nodes || v < 0 || v >= num_nodes)
        continue;
      if (node_community_map[u].component == comp &&
          node_community_map[v].component == comp) {
        local_edges[out_e].node1 = local_index[u];
        local_edges[out_e].node2 = local_index[v];
        local_edges[out_e].weight = edges[e].weight;
        out_e++;
      }
    }
    edge_count = out_e;

    int *local_labels = malloc((size_t)n_comp * sizeof(int));
    if (local_labels == NULL) {
      free(local_edges);
      free(local_index);
      continue;
    }

    int detected =
        run_multilevel_detection(n_comp, edge_count, local_edges,
                                 OBJECTIVE_MODULARITY, false, local_labels);
    if (detected < 0)
      detected = n_comp;

    for (int i = 0; i < n_comp; i++) {
      global_labels[nodes[i]] = next_comm_id + local_labels[i];
    }
    next_comm_id += detected;

    free(local_labels);
    free(local_edges);
    free(local_index);
  }

  memcpy(communities, global_labels, (size_t)num_nodes * sizeof(int));
  num_communities = renumber_labels_inplace(communities, num_nodes);
  fill_node_community_map_from_result();

  for (int c = 0; c < num_components; c++)
    free(comp_nodes[c]);
  free(comp_nodes);
  free(comp_sizes);
  free(global_labels);

  printf("Total number of communities detected (per component): %d\n",
         num_communities);
  return num_communities;
}

void initialize_community_colors() {
  for (int i = 0; i < num_nodes; i++) {
    float min = 0.2f;
    cluster_colors[i][0] = min + (float)rand() / RAND_MAX * (1.0f - min);
    cluster_colors[i][1] = min + (float)rand() / RAND_MAX * (1.0f - min);
    cluster_colors[i][2] = min + (float)rand() / RAND_MAX * (1.0f - min);
  }
}

void compute_ratio_S(int *S_local) {
  if (num_communities <= 0 || S_local == NULL) {
    printf("Error: num_communities not initialized.\n");
    return;
  }

  int *community_sizes = calloc((size_t)num_nodes, sizeof(int));
  int *community_s1_counts = calloc((size_t)num_nodes, sizeof(int));
  if (community_sizes == NULL || community_s1_counts == NULL) {
    free(community_sizes);
    free(community_s1_counts);
    return;
  }

  for (int i = 0; i < num_rows; i++) {
    int comm = communities[i];
    if (comm < 0 || comm >= num_nodes) {
      printf("Error: Invalid community index %d for node %d.\n", comm, i);
      continue;
    }
    community_sizes[comm]++;
    if (S_local[i] == 1)
      community_s1_counts[comm]++;
  }

  printf("Community Ratios (>0.5%% of nodes):\n");
  for (int comm = 0; comm < num_nodes; comm++) {
    if (community_sizes[comm] > num_nodes / 200) {
      double ratio =
          (double)community_s1_counts[comm] / (double)community_sizes[comm];
      printf("Community %d: Size = %d, Ratio of 1 = %.10f\n", comm,
             community_sizes[comm], ratio);
      fflush(stdout);
    }
  }

  free(community_sizes);
  free(community_s1_counts);
}
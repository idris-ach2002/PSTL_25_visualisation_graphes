#include "repulsion.h"
#include "cluster.h"
#include "../global.h"

struct landmark {
  int node;
  double weight;
};

struct repulsion_args {
  double (*forces)[2];
  double fmax_x;
  double fmax_y;
  int first;
  int last;
  const struct landmark *landmarks;
  int landmark_count;
  Barrier barrier;
};

static struct landmark *landmarks = NULL;
static int landmark_capacity = 0;
static int landmark_count = 0;

static inline double node_mass(int node) {
  return mode == 1 ? 1.0 : (double)(node_degrees[node] + 1);
}

static inline unsigned mix_u32(unsigned x) {
  x ^= x >> 16;
  x *= 0x7feb352du;
  x ^= x >> 15;
  x *= 0x846ca68bu;
  x ^= x >> 16;
  return x;
}

static int target_landmark_count(void) {
  if (live_nodes < 60000) {
    return 0;
  }

  int count = (int)(sqrt((double)live_nodes) / 18.0);
  if (count < 32) {
    count = 32;
  }
  if (count > 80) {
    count = 80;
  }
  return count;
}

static int ensure_landmarks(int count) {
  if (count <= landmark_capacity) {
    return 1;
  }

  struct landmark *next =
      (struct landmark *)realloc(landmarks, sizeof(struct landmark) * count);
  if (next == NULL) {
    return 0;
  }

  landmarks = next;
  landmark_capacity = count;
  return 1;
}

/**
 * Reconstruit un petit ensemble de sommets représentants.
 *
 * @return 1 si la reconstruction a réussi, 0 sinon.
 *
 * Les représentants sont des sommets réels choisis par hachage. Ils ne sont pas
 * placés sur une grille, donc ils ne peuvent pas imposer de damier ou de forme
 * rectangulaire au champ de forces. Leur poids approxime les sommets non
 * sélectionnés.
 */
static int rebuild_landmarks(void) {
  int count = target_landmark_count();
  landmark_count = count;
  if (count == 0) {
    return 1;
  }
  if (!ensure_landmarks(count)) {
    landmark_count = 0;
    return 0;
  }

  int found = 0;
  unsigned salt = (unsigned)(iteration / 12) * 0x9e3779b9u;
  for (int k = 0; k < count; ++k) {
    int node = (int)(mix_u32((unsigned)k + salt) % (unsigned)num_nodes);
    int attempts = 0;
    while (attempts < 16 && vertices[node].deleted) {
      node = (node + 1) % num_nodes;
      attempts++;
    }
    if (!vertices[node].deleted) {
      landmarks[found].node = node;
      found++;
    }
  }

  landmark_count = found;
  if (found == 0) {
    return 0;
  }

  double weight = (double)live_nodes / (double)found;
  for (int i = 0; i < found; ++i) {
    landmarks[i].weight = weight;
  }
  return 1;
}

static inline void add_force_from_point(double (*forces)[2], int node,
                                        double source_x, double source_y,
                                        double source_mass, double scale) {
  double dx = source_x - vertices[node].x;
  double dy = source_y - vertices[node].y;
  double dist2 = dx * dx + dy * dy;
  if (dist2 < seuilrep) {
    dist2 = seuilrep > 1e-9 ? seuilrep : 1e-9;
  }

  double force = scale * repulsion_coeff * node_mass(node) * source_mass / dist2;
  forces[node][0] -= dx * force;
  forces[node][1] -= dy * force;
}

static void landmark_repulsion_worker(void *arg) {
  struct repulsion_args *a = (struct repulsion_args *)arg;

  for (int i = a->first; i < a->last; ++i) {
    if (vertices[i].deleted) {
      continue;
    }

    for (int k = 0; k < a->landmark_count; ++k) {
      int source = a->landmarks[k].node;
      if (source == i || vertices[source].deleted) {
        continue;
      }
      add_force_from_point(a->forces, i, vertices[source].x, vertices[source].y,
                           a->landmarks[k].weight * node_mass(source), 0.035);
    }

    if (a->forces[i][0] > a->fmax_x) {
      a->forces[i][0] = a->fmax_x;
    } else if (a->forces[i][0] < -a->fmax_x) {
      a->forces[i][0] = -a->fmax_x;
    }

    if (a->forces[i][1] > a->fmax_y) {
      a->forces[i][1] = a->fmax_y;
    } else if (a->forces[i][1] < -a->fmax_y) {
      a->forces[i][1] = -a->fmax_y;
    }
  }

  decrement_barrier(a->barrier, 1);
}

static inline void add_pair_repulsion(double (*forces)[2], int a, int b) {
  double dx = vertices[b].x - vertices[a].x;
  double dy = vertices[b].y - vertices[a].y;
  double dist2 = dx * dx + dy * dy;
  if (dist2 < seuilrep) {
    dist2 = seuilrep > 1e-9 ? seuilrep : 1e-9;
  }

  double factor = repulsion_coeff * node_mass(a) * node_mass(b) / dist2;
  if (mode == 2 && communities[a] != communities[b]) {
    factor *= 4.0;
  }

  forces[a][0] -= dx * factor;
  forces[a][1] -= dy * factor;
  forces[b][0] += dx * factor;
  forces[b][1] += dy * factor;
}

static void exact_cluster_range(double (*forces)[2], int first_cluster,
                                int last_cluster, double fmax_x,
                                double fmax_y) {
  for (int c = first_cluster; c < last_cluster; ++c) {
    Cluster *cluster = &cluster_nodes[c];
    for (int i = 0; i < cluster->size; ++i) {
      int node_i = cluster->nodes[i];
      if (vertices[node_i].deleted) {
        continue;
      }

      for (int j = i + 1; j < cluster->size; ++j) {
        int node_j = cluster->nodes[j];
        if (!vertices[node_j].deleted) {
          add_pair_repulsion(forces, node_i, node_j);
        }
      }

      if (forces[node_i][0] > fmax_x) {
        forces[node_i][0] = fmax_x;
      } else if (forces[node_i][0] < -fmax_x) {
        forces[node_i][0] = -fmax_x;
      }
      if (forces[node_i][1] > fmax_y) {
        forces[node_i][1] = fmax_y;
      } else if (forces[node_i][1] < -fmax_y) {
        forces[node_i][1] = -fmax_y;
      }
    }
  }
}

struct exact_args {
  double (*forces)[2];
  double fmax_x;
  double fmax_y;
  int first_cluster;
  int last_cluster;
  Barrier barrier;
};

static void exact_worker(void *arg) {
  struct exact_args *a = (struct exact_args *)arg;
  exact_cluster_range(a->forces, a->first_cluster, a->last_cluster, a->fmax_x,
                      a->fmax_y);
  decrement_barrier(a->barrier, 1);
}

static void parallel_exact_clusters(double (*forces)[2], double fmax_x,
                                    double fmax_y) {
  int threads = pool.nb_threads > 0 ? pool.nb_threads : 1;
  if (threads > n_clusters) {
    threads = n_clusters;
  }

  struct barrier bar;
  new_barrier(&bar, threads);
  int chunk = (n_clusters + threads - 1) / threads;

  for (int t = 0; t < threads; ++t) {
    struct exact_args *arg =
        (struct exact_args *)malloc(sizeof(struct exact_args));
    if (arg == NULL) {
      decrement_barrier(&bar, 1);
      continue;
    }
    arg->forces = forces;
    arg->fmax_x = fmax_x;
    arg->fmax_y = fmax_y;
    arg->first_cluster = t * chunk;
    arg->last_cluster = arg->first_cluster + chunk;
    if (arg->last_cluster > n_clusters) {
      arg->last_cluster = n_clusters;
    }
    arg->barrier = &bar;

    struct Job job;
    job.j = exact_worker;
    job.args = arg;
    submit(&pool, job);
  }

  wait_barrier(&bar);
}

static void parallel_landmark_repulsion(double (*forces)[2], double fmax_x,
                                        double fmax_y) {
  if (!rebuild_landmarks() || landmark_count == 0) {
    return;
  }

  int threads = pool.nb_threads > 0 ? pool.nb_threads : 1;
  if (threads > num_nodes) {
    threads = num_nodes;
  }

  struct barrier bar;
  new_barrier(&bar, threads);
  int chunk = (num_nodes + threads - 1) / threads;

  for (int t = 0; t < threads; ++t) {
    struct repulsion_args *arg =
        (struct repulsion_args *)malloc(sizeof(struct repulsion_args));
    if (arg == NULL) {
      decrement_barrier(&bar, 1);
      continue;
    }
    arg->forces = forces;
    arg->fmax_x = fmax_x;
    arg->fmax_y = fmax_y;
    arg->first = t * chunk;
    arg->last = arg->first + chunk;
    if (arg->last > num_nodes) {
      arg->last = num_nodes;
    }
    arg->landmarks = landmarks;
    arg->landmark_count = landmark_count;
    arg->barrier = &bar;

    struct Job job;
    job.j = landmark_repulsion_worker;
    job.args = arg;
    submit(&pool, job);
  }

  wait_barrier(&bar);
}

/**
 * Calcule la répulsion entre sommets.
 *
 * @param forces tableau forces[num_nodes][2] accumulé par updatePositions().
 * @param FMaxX force horizontale maximale conservée après répulsion.
 * @param FMaxY force verticale maximale conservée après répulsion.
 *
 * Pour les graphes modestes, la répulsion exacte par cluster est gardée. Pour
 * les grands graphes, on utilise des représentants réels choisis par hachage.
 * Le coût devient O(N * R), avec R borné et indépendant de la taille du graphe.
 * Cette approximation ne repose sur aucune grille spatiale et ne crée donc pas
 * de damier visible.
 */
void parallel_intra_clusters(double (*forces)[2], double FMaxX, double FMaxY) {
  if (modified_graph && mode != 1) {
    calculate_node_degrees();
  }

  if (cluster_nodes == NULL || n_clusters <= 0) {
    return;
  }

  if (target_landmark_count() == 0) {
    parallel_exact_clusters(forces, FMaxX, FMaxY);
  } else {
    parallel_landmark_repulsion(forces, FMaxX, FMaxY);
  }
}

void repulsion_intra_clusters(double (*forces)[2], double FMaxX, double FMaxY) {
  parallel_intra_clusters(forces, FMaxX, FMaxY);
}

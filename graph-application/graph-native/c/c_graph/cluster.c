#include "cluster.h"
#include "kmeans.h"
#include "../global.h"

int communities[MAX_NODES];
int clusters[MAX_NODES];
float cluster_colors[MAX_NODES][3];
double centers[MAX_NODES][2];
Cluster *cluster_nodes = NULL;
int n_clusters = 0;
int kmeans_mode = 0;

double epsilon = 0.1;
int espacement = 1;

double initial_node_size = 10.0;
double degree_scale_factor = 0.5;

double repulsion_coeff = 10.0;
int saut = 1;
int mode = 0;

static int cluster_update_period(void) {
  int period = saut <= 0 ? 1 : saut;
  return period < 4 ? 4 : period;
}

static int valid_cluster_id(int id) {
  if (n_clusters <= 0) {
    return 0;
  }
  if (id < 0) {
    id = -id;
  }
  return id % n_clusters;
}

/**
 * Calcule le diamètre logique d'un sommet.
 *
 * @param node_index indice du sommet.
 * @return taille utilisée par les anciens chemins anti-chevauchement.
 */
double compute_node_size(int node_index) {
  if (degree_scale_factor < 0.0) {
    degree_scale_factor = 0.0;
  }
  return initial_node_size + sqrt(degree_scale_factor * node_degrees[node_index]);
}

/**
 * Calcule la distance carrée entre un centre k-means et un sommet.
 *
 * @param center tableau {x, y}.
 * @param p sommet testé.
 * @return distance euclidienne carrée.
 */
double squared_distance(double *center, Point p) {
  const double dx = p.x - center[0];
  const double dy = p.y - center[1];
  return dx * dx + dy * dy;
}

/**
 * Place un point aléatoire dans le plan de simulation.
 *
 * @param p point à modifier.
 */
void random_point_in_plane(Point *p) {
  p->x = (rand() / (double)RAND_MAX) * Lx - Lx / 2.0;
  p->y = (rand() / (double)RAND_MAX) * Ly - Ly / 2.0;
}

/**
 * Initialise les centres k-means par tirage uniforme.
 */
void initialize_centers(void) {
  for (int i = 0; i < n_clusters; ++i) {
    random_point_in_plane((Point *)centers[i]);
  }
}

/**
 * Initialise les centres k-means avec une variante simple de k-means++.
 *
 * Cette fonction est utilisée uniquement lors d'une réinitialisation. Elle ne
 * fait pas partie du chemin chaud de updatePositions().
 */
void initialize_centers_plus(void) {
  if (num_nodes <= 0 || n_clusters <= 0) {
    return;
  }

  double *nearest = (double *)malloc(sizeof(double) * (size_t)num_nodes);
  if (nearest == NULL) {
    initialize_centers();
    return;
  }

  for (int i = 0; i < num_nodes; ++i) {
    nearest[i] = DBL_MAX;
  }

  int first = rand() % num_nodes;
  centers[0][0] = vertices[first].x;
  centers[0][1] = vertices[first].y;
  nearest[first] = 0.0;

  for (int c = 1; c < n_clusters; ++c) {
    double total = 0.0;
    for (int i = 0; i < num_nodes; ++i) {
      if (vertices[i].deleted) {
        continue;
      }
      double d = squared_distance(centers[c - 1], vertices[i]);
      if (d < nearest[i]) {
        nearest[i] = d;
      }
      total += nearest[i];
    }

    double r = (rand() / (double)RAND_MAX) * total;
    int chosen = first;
    for (int i = 0; i < num_nodes; ++i) {
      r -= nearest[i];
      if (r <= 0.0) {
        chosen = i;
        break;
      }
    }

    centers[c][0] = vertices[chosen].x;
    centers[c][1] = vertices[chosen].y;
    nearest[chosen] = 0.0;
  }

  free(nearest);
}

/**
 * Vide les listes de sommets associées aux clusters.
 */
void clear_clusters(void) {
  for (int i = 0; i < n_clusters; ++i) {
    cluster_nodes[i].size = 0;
  }
}

/**
 * Libère la mémoire des clusters.
 */
void free_clusters(void) {
  if (cluster_nodes == NULL) {
    return;
  }
  for (int i = 0; i < n_clusters; ++i) {
    free(cluster_nodes[i].nodes);
  }
  free(cluster_nodes);
  cluster_nodes = NULL;
  n_clusters = 0;
}

/**
 * Alloue les listes de clusters.
 *
 * @param num_clusters nombre de clusters à créer.
 */
void init_clusters(int num_clusters) {
  if (num_clusters <= 0) {
    num_clusters = 1;
  }

  n_clusters = num_clusters;
  int estimated = live_nodes > 0 ? live_nodes / num_clusters + 1 : 8;
  if (estimated < 8) {
    estimated = 8;
  }

  cluster_nodes = (Cluster *)calloc((size_t)num_clusters, sizeof(Cluster));
  for (int i = 0; i < num_clusters; ++i) {
    cluster_nodes[i].capacity = estimated;
    cluster_nodes[i].nodes = (int *)malloc(sizeof(int) * (size_t)estimated);
  }
}

/**
 * Ajoute un sommet dans un cluster.
 *
 * @param cluster_id identifiant logique du cluster.
 * @param node indice du sommet.
 */
void add_node_to_cluster(int cluster_id, int node) {
  cluster_id = valid_cluster_id(cluster_id);
  Cluster *cluster = &cluster_nodes[cluster_id];

  if (cluster->size >= cluster->capacity) {
    cluster->capacity = cluster->capacity * 2 + 8;
    cluster->nodes = (int *)realloc(cluster->nodes,
                                    sizeof(int) * (size_t)cluster->capacity);
  }
  cluster->nodes[cluster->size++] = node;
}

/**
 * Réinitialise la structure de clusters.
 *
 * @param new_num_clusters nouveau nombre de clusters.
 */
void reinitialize_clusters(int new_num_clusters) {
  if (new_num_clusters <= 0) {
    new_num_clusters = 1;
  }

  if (new_num_clusters != n_clusters || cluster_nodes == NULL) {
    free_clusters();
    init_clusters(new_num_clusters);
  } else {
    clear_clusters();
  }

  initialize_centers();
  assign_cluster_colors();
}

/**
 * Attribue une couleur stable aux clusters.
 */
void assign_cluster_colors(void) {
  for (int i = 0; i < n_clusters; ++i) {
    cluster_colors[i][0] = (float)rand() / (float)RAND_MAX;
    cluster_colors[i][1] = (float)rand() / (float)RAND_MAX;
    cluster_colors[i][2] = (float)rand() / (float)RAND_MAX;
  }
}

static int fallback_cluster_for_node(int node) {
  if (num_communities > 0) {
    return valid_cluster_id(communities[node]);
  }
  return valid_cluster_id(node * 2654435761u);
}

static void rebuild_cluster_lists(void) {
  if (cluster_nodes == NULL || n_clusters <= 0) {
    return;
  }

  clear_clusters();
  for (int i = 0; i < num_nodes; ++i) {
    if (!vertices[i].deleted) {
      add_node_to_cluster(clusters[i], i);
    }
  }
}

/**
 * Met à jour l'appartenance des sommets aux clusters.
 *
 * En mode k-means, une seule itération est exécutée périodiquement. On évite la
 * boucle jusqu'à convergence, qui rendait updatePositions() imprévisible. Sans
 * k-means, l'appartenance ne repose plus sur une grille spatiale afin de ne pas
 * imposer de structure rectangulaire au graphe.
 */
void update_clusters(void) {
  if (cluster_nodes == NULL || n_clusters <= 0) {
    return;
  }

  int changed = 0;
  if (kmeans_mode != 0) {
    if (iteration % cluster_update_period() == 0) {
      double max_diff = 0.0;
      kmeans_iteration_fast(num_nodes, n_clusters, clusters, centers, Lx, Ly,
                            &max_diff);
      changed = 1;
    }
  } else if (iteration % cluster_update_period() == 0 || iteration == 0) {
    for (int i = 0; i < num_nodes; ++i) {
      if (!vertices[i].deleted) {
        clusters[i] = fallback_cluster_for_node(i);
      }
    }
    changed = 1;
  }

  if (changed) {
    rebuild_cluster_lists();
  }
}

/**
 * Ancienne fonction conservée pour compatibilité.
 *
 * Elle ne modifie plus le chemin de forces. L'ancien maillage spatial créait
 * des formes rectangulaires visibles sur les grands graphes.
 */
void maintainSpatialGrid(void) {}

/**
 * Ancien clustering par grille conservé pour compatibilité.
 *
 * @param num_points nombre de sommets.
 * @param num_clusters nombre de clusters.
 * @param labels tableau de sortie.
 * @param lx largeur ignorée.
 * @param ly hauteur ignorée.
 */
void grid_clustering(int num_points, int num_clusters, int *labels,
                     double lx, double ly) {
  (void)lx;
  (void)ly;
  for (int i = 0; i < num_points; ++i) {
    labels[i] = num_clusters > 0 ? i % num_clusters : 0;
  }
}

/**
 * Ancien anti-chevauchement conservé pour compatibilité.
 */
void noverlap_force(double (*forces)[2], double FMaxX, double FMaxY) {
  (void)forces;
  (void)FMaxX;
  (void)FMaxY;
}

void kmeans_iteration(int num_points, int num_clusters, int *labels,
                      double centers[][2], double Lx, double Ly,
                      double *max_diff) {
  kmeans_iteration_fast(num_points, num_clusters, labels, centers, Lx, Ly,
                        max_diff);
}

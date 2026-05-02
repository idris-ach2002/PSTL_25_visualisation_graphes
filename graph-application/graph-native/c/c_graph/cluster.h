#ifndef FORCEATLAS_CLUSTER_H
#define FORCEATLAS_CLUSTER_H

#include <float.h>
#include <stdlib.h>
#include <string.h>
#include "graph.h"

typedef struct {
  int *nodes;
  int size;
  int capacity;
} Cluster;

void kmeans_iteration(int num_points, int num_clusters, int *labels,
                      double centers[][2], double Lx, double Ly,
                      double *max_diff);

void update_clusters(void);
void repulsion_intra_clusters(double (*forces)[2], double FMaxX, double FMaxY);
void parallel_intra_clusters(double (*forces)[2], double FMaxX, double FMaxY);

void initialize_centers(void);
void initialize_centers_plus(void);
void clear_clusters(void);
void reinitialize_clusters(int new_num_clusters);
void free_clusters(void);
void assign_cluster_colors(void);
void init_clusters(int num_clusters);
void add_node_to_cluster(int cluster_id, int node);
void random_point_in_plane(Point *p);

double compute_node_size(int node_index);
double squared_distance(double *center, Point p);

/* Compatibilité avec l'ancienne API. Ces fonctions ne sont plus utilisées dans
 * le chemin rapide mais restent déclarées pour éviter de casser les appels
 * existants dans le projet. */
void noverlap_force(double (*forces)[2], double FMaxX, double FMaxY);
void grid_clustering(int num_points, int num_clusters, int *labels,
                     double Lx, double Ly);
void maintainSpatialGrid(void);

#endif

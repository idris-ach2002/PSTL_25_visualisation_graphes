#ifndef GRAPH_ENGINE_H
#define GRAPH_ENGINE_H

#ifdef __cplusplus
extern "C" {
#endif

int ge_init_from_edges(int node_count, int edge_count, const int *edge_pairs, const float *edge_weights);
int ge_init_random(int node_count, int edge_count);
int ge_step(int iterations);
void ge_free(void);

int ge_get_node_count(void);
int ge_get_edge_count(void);
int ge_get_live_node_count(void);
int ge_get_iteration(void);

float *ge_get_positions_ptr(void);
float *ge_get_colors_ptr(void);
int *ge_get_edges_ptr(void);
int *ge_get_degrees_ptr(void);
float *ge_get_weights_ptr(void);
int *ge_get_communities_ptr(void);
unsigned char *ge_get_deleted_ptr(void);

void ge_set_dimensions(float width, float height);
void ge_set_force_params(float repulsion, float attraction, float damping, float time_step, float theta, float anti_repulsion, int repulsion_mode, int kmeans_enabled, int kmeans_clusters);
void ge_set_node_position(int node, float x, float y);
void ge_delete_node(int node);
void ge_restore_node(int node);
void ge_run_label_propagation(int iterations);
void ge_run_spatial_kmeans(int clusters, int iterations);
void ge_reset_layout(unsigned int seed);

#ifdef __cplusplus
}
#endif

#endif

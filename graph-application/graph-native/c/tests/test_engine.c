#include <math.h>
#include <stdio.h>
#include <stdlib.h>

#include "../global.h"
#include "test_utils.h"

static void init_engine() {
  if (num_nodes > MAX_NODES) {
    fprintf(stderr, "Error: num_nodes=%d exceeds MAX_NODES=%d\n", num_nodes,
            MAX_NODES);
    exit(1);
  }
  for (int i = 0; i < num_nodes; i++)
    random_point_in_center(i);

  n_clusters = (int)sqrt(num_nodes);
  printf("Nombre de clusters : %i", n_clusters);

  init_clusters(n_clusters);
  initialize_centers();
  calculate_node_degrees();

  thresholdS = (Lx / 4000) * (Ly / 4000);
  friction = 0.1;
}

static int run_layout() {
  int iter = 0;

  double forces[MAX_NODES][2] = {0};

  while (iter < 2000) {
    repulsion_edges(forces);
    parallel_intra_clusters(forces, 10, 10);
    repulsion_anti_edges(forces);

    double max_move = update_position_forces(forces, Lx / 10., Ly / 10., 0);

    reset_forces(forces, num_nodes);

    check_nan_positions();

    if (max_move < thresholdS)
      break;

    iter++;
  }

  return iter;
}

int main(int argc, char **argv) {
  if (argc < 2) {
    printf("Usage: %s graph.dot\n", argv[0]);
    return 1;
  }

  const char *path = argv[1];

  printf("TEST graph: %s\n", path);

  parse_dot_file(path);

  printf("Nodes: %d Edges: %d\n", num_nodes, num_edges);

  srand(42);

  init_engine();

  int iterations = run_layout();

  printf("Converged in %d iterations\n", iterations);

  print_positions(10);

  printf("PASS\n");

  return 0;
}
#include "graph.h"
#include "../global.h"

Point vertices[MAX_NODES];

Edge edges[MAX_EDGES]; // Pour les arêtes normales
double velocities[MAX_NODES][2];
int node_degrees[MAX_NODES];

_Atomic int num_edges = 0;
_Atomic int num_antiedges = 0;
double coeff_antiarete = 50; // Facteur de répulsion des antiarêtes
Edge antiedges[MAX_EDGES];   // Pour les anti-arêtes

int num_nodes = 0;
int live_nodes = 0;
double Lx = 300, Ly = 300;

double friction = 0.1;
double attraction_coeff = 10;
double thresholdA = 1;
double seuilrep = 0;
double thresholdS = 1;

volatile double amortissement = 0.999;

short modified_graph = 0;

void toroidal_vector(Point *dir, Point p1, Point p2) {
  dir->x = p2.x - p1.x;
  dir->y = p2.y - p1.y;
  if (fabs(dir->x) > Lx / 2)
    dir->x -= copysign(Lx, dir->x);
  if (fabs(dir->y) > Ly / 2)
    dir->y -= copysign(Ly, dir->y);
}

static inline void layout_vector(Point *dir, Point p1, Point p2) {
  dir->x = p2.x - p1.x;
  dir->y = p2.y - p1.y;
}

// Calculer la distance toroïdale entre deux points
double toroidal_distance(Point p1, Point p2) {
  Point dir;
  toroidal_vector(&dir, p1, p2);
  return sqrt(dir.x * dir.x + dir.y * dir.y);
}

// Calculer les degrés de chaque noeud
// Works with dot files
void calculate_node_degrees(void) {
  for (int i = 0; i < num_nodes; i++) {
    node_degrees[i] = 0;
  }

  for (int i = 0; i < num_edges; i++) {
    int node1 = edges[i].node1;
    int node2 = edges[i].node2;

    if (vertices[node1].deleted == 0 && vertices[node2].deleted == 0) {
      node_degrees[node1]++;
      node_degrees[node2]++;
    }
  }

  modified_graph = 0;
}

// Générer un point aléatoire près du centre
void random_point_in_center(int index) {
  double center_width = Lx * 0.3;
  double center_height = Ly * 0.3;
  vertices[index].x =
      (rand() / (double)RAND_MAX) * center_width - center_width / 2;
  vertices[index].y =
      (rand() / (double)RAND_MAX) * center_height - center_height / 2;
  vertices[index].deleted = 0;
}

void translate_positions(double dx, double dy) {
  for (int i = 0; i < num_nodes; ++i) {
    vertices[i].x += dx;
    vertices[i].y += dy;
  }
}

void repulsion_edges(double (*forces)[2]) {

  for (int edge_index = 0; edge_index < num_edges; edge_index++) {
    int node1 = edges[edge_index].node1;
    int node2 = edges[edge_index].node2;

    if (vertices[node1].deleted == 0 && vertices[node2].deleted == 0) {
      Point dir;
      layout_vector(&dir, vertices[node1], vertices[node2]);

      double dist_squared = dir.x * dir.x + dir.y * dir.y;
      double att_force = attraction_coeff; //*dist_squared;

      if (dist_squared > thresholdA) {
        forces[node1][0] += dir.x * att_force;
        forces[node1][1] += dir.y * att_force;
        forces[node2][0] -= dir.x * att_force;
        forces[node2][1] -= dir.y * att_force;
      }
    }
  }
}

void repulsion_anti_edges(double (*forces)[2]) {

  for (int edge_index = 0; edge_index < num_antiedges; edge_index++) {
    int node1 = antiedges[edge_index].node1;
    int node2 = antiedges[edge_index].node2;

    if (vertices[node1].deleted == 0 && vertices[node2].deleted == 0) {
      Point dir;
      layout_vector(&dir, vertices[node1], vertices[node2]);

      double dist = sqrt(dir.x * dir.x + dir.y * dir.y);
      if (dist > seuilrep) {
        double rep_force = coeff_antiarete / (dist * dist);
        forces[node1][0] -= (dir.x / dist) * rep_force;
        forces[node1][1] -= (dir.y / dist) * rep_force;
        forces[node2][0] += (dir.x / dist) * rep_force;
        forces[node2][1] += (dir.y / dist) * rep_force;
      } else {
        double rep_force = coeff_antiarete / seuilrep;

        forces[node1][0] -= dir.x * rep_force;
        forces[node1][1] -= dir.y * rep_force;
        forces[node2][0] += dir.x * rep_force;
        forces[node2][1] += dir.y * rep_force;
      }
    }
  }
}


/**
 * Applique les forces accumulées aux sommets et nettoie le buffer de forces.
 *
 * @param forces tableau mutable forces[num_nodes][2]. Les valeurs sont lues,
 *        intégrées dans velocities, puis remises à zéro dans la même boucle.
 * @param PasMaxX déplacement horizontal maximal autorisé pour un tick.
 * @param PasMaxY déplacement vertical maximal autorisé pour un tick.
 * @param Max_movement valeur de départ utilisée pour calculer le maximum.
 * @return le plus grand déplacement carré observé pendant ce tick.
 *
 * @details Le nettoyage du buffer de forces est fusionné avec l'intégration des
 *          positions. Les coordonnées ne sont plus repliées dans un rectangle.
 *          Le graphe est seulement recentré par son barycentre pour éviter une
 *          dérive globale de la scène.
 */
double update_position_forces(double (*forces)[2], double PasMaxX,
                              double PasMaxY, double Max_movement) {
  double new_max_movement = 0.0;
  double sum_x = 0.0;
  double sum_y = 0.0;
  int active = 0;

  for (int i = 0; i < num_nodes; i++) {
    if (vertices[i].deleted == 0) {
      velocities[i][0] = (velocities[i][0] + forces[i][0]) * friction;
      velocities[i][1] = (velocities[i][1] + forces[i][1]) * friction;

      if (velocities[i][0] < -PasMaxX) {
        velocities[i][0] = -PasMaxX;
      } else if (velocities[i][0] > PasMaxX) {
        velocities[i][0] = PasMaxX;
      }

      if (velocities[i][1] < -PasMaxY) {
        velocities[i][1] = -PasMaxY;
      } else if (velocities[i][1] > PasMaxY) {
        velocities[i][1] = PasMaxY;
      }

      vertices[i].x += velocities[i][0];
      vertices[i].y += velocities[i][1];

      sum_x += vertices[i].x;
      sum_y += vertices[i].y;
      active++;

      const double movement = velocities[i][0] * velocities[i][0] +
                              velocities[i][1] * velocities[i][1];
      if (movement > new_max_movement) {
        new_max_movement = movement;
      }
    }

    forces[i][0] = 0.0;
    forces[i][1] = 0.0;
  }

  if (active > 0) {
    const double mean_x = sum_x / (double)active;
    const double mean_y = sum_y / (double)active;
    if (fabs(mean_x) > 1e-9 || fabs(mean_y) > 1e-9) {
      translate_positions(-mean_x, -mean_y);
    }
  }

  return new_max_movement > Max_movement ? new_max_movement : Max_movement;
}

// Normaliser un point
void normalize(Point *p) {
  double norm = sqrt(p->x * p->x + p->y * p->y);
  if (norm > 0) {
    p->x /= norm;
    p->y /= norm;
  }
}
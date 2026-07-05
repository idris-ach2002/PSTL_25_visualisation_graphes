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
  double half_Lx = Lx / 2.0;
  double half_Ly = Ly / 2.0;
  for (int i = 0; i < num_nodes; ++i) {
    double x = vertices[i].x + dx;
    double y = vertices[i].y + dy;
    // Appliquer les conditions aux limites toroïdales

    while (x < -half_Lx) {
      x += Lx;
    }
    while (x > half_Lx) {
      x -= Lx;
    }
    while (y < -half_Ly) {
      y += Ly;
    }
    while (y > half_Ly) {
      y -= Ly;
    }

    vertices[i].x = x;
    vertices[i].y = y;
  }
}

void repulsion_edges(double (*forces)[2]) {

  for (int edge_index = 0; edge_index < num_edges; edge_index++) {
    int node1 = edges[edge_index].node1;
    int node2 = edges[edge_index].node2;

    if (vertices[node1].deleted == 0 && vertices[node2].deleted == 0) {
      Point dir;
      toroidal_vector(&dir, vertices[node1], vertices[node2]);

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
      toroidal_vector(&dir, vertices[node1], vertices[node2]);

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

// probablement privé utilisé dans update_positions
// Étape 3 : Mettre à jour les positions des sommets du graphe en fonction des
// forces
double update_position_forces(double (*forces)[2], double PasMaxX,
                              double PasMaxY, double Max_movement) {
  double half_Lx = Lx / 2.0;
  double half_Ly = Ly / 2.0;

  double new_max_movement = 0.0;
  for (int i = 0; i < num_nodes; i++) {
    if (vertices[i].deleted == 0) {
      velocities[i][0] = (velocities[i][0] + forces[i][0]) * friction;
      velocities[i][1] = (velocities[i][1] + forces[i][1]) * friction;
      velocities[i][0] = fmin(fmax(velocities[i][0], -PasMaxX),
                              PasMaxX); // Capper la force en x à 1
      velocities[i][1] = fmin(fmax(velocities[i][1], -PasMaxY),
                              PasMaxY); // Capper la force en y à 1

      double x = vertices[i].x + velocities[i][0];
      double y = vertices[i].y + velocities[i][1];
      // Appliquer les conditions aux limites toroïdales

      if (x < -half_Lx) {
        x = -half_Lx;
      }
      if (x > half_Lx) {
        x = half_Lx;
      }
      if (y < -half_Ly) {
        y = -half_Ly;
      }
      if (y > half_Ly) {
        y = half_Ly;
      }

      vertices[i].x = x;
      vertices[i].y = y;

      new_max_movement =
          fmax(Max_movement, velocities[i][0] * velocities[i][0] +
                                 velocities[i][1] * velocities[i][1]);
    }
  }

  return new_max_movement;
}

// Normaliser un point
void normalize(Point *p) {
  double norm = sqrt(p->x * p->x + p->y * p->y);
  if (norm > 0) {
    p->x /= norm;
    p->y /= norm;
  }
}
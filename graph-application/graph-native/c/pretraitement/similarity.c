#include "similarity.h"
#include "../global.h"

int mode_similitude = 0;
int global_histogram[NUM_BINS];

// Calcul de la corrélation entre deux vecteurs
double correlation_similarity(int i, int j) {
  double mean_i = 0.0, mean_j = 0.0;
  for (int k = 0; k < num_columns; k++) {
    mean_i += data[i][k];
    mean_j += data[j][k];
  }
  mean_i /= num_columns;
  mean_j /= num_columns;

  double num = 0.0, den_i = 0.0, den_j = 0.0;
  for (int k = 0; k < num_columns; k++) {
    double di = data[i][k] - mean_i;
    double dj = data[j][k] - mean_j;
    num += di * dj;
    den_i += di * di;
    den_j += dj * dj;
  }

  if (den_i < EPSILON || den_j < EPSILON) {
    // variance nulle dc vecteurs constants
    return (den_i == den_j) ? 1.0 : 0.0;
  }
  double corr = num / sqrt(den_i * den_j);
  // Arrondir à [-1,1] pour éviter les erreurs d'arrondi
  if (corr > 1.0)
    corr = 1.0;
  if (corr < -1.0)
    corr = -1.0;
  return corr;
}

// Calcul de la norme L1 (somme des distances absolues)
double L1_norm(int i, int j) {
  double sum = 0.0;
  for (int k = 0; k < num_columns; k++) {
    sum += fabs(data[i][k] - data[j][k]);
  }
  return 1 / (1 + sum);
}

// Calcul de la norme Linf (distance maximale entre les composantes des
// vecteurs)
double Linf_norm(int i, int j) {
  double max_diff = 0.0;
  for (int k = 0; k < num_columns; k++) {
    double diff = fabs(data[i][k] - data[j][k]);
    if (diff > max_diff) {
      max_diff = diff;
    }
  }
  return 1 / (1 + max_diff);
}

// Calcul de la distance cosinus
double cosine_similarity(int i, int j) {
  double num = 0.0, den_i = 0.0, den_j = 0.0;
  for (int k = 0; k < num_columns; k++) {
    num += data[i][k] * data[j][k];
    den_i += data[i][k] * data[i][k];
    den_j += data[j][k] * data[j][k];
  }
  if (den_i < EPSILON || den_j < EPSILON)
    return 0.0;
  return num / (sqrt(den_i) * sqrt(den_j));
}

// Fonction pour calculer la divergence KL entre deux vecteurs normalisés
double KL_divergence(int i, int j) {
  double kl_div = 0.0;
  double p[num_columns];
  double q[num_columns];
  for (int k = 0; k < num_columns; k++) {
    p[k] = data[i][k];
    q[k] = data[j][k];
  }
  normalize_vector(p, num_columns);
  normalize_vector(q, num_columns);
  for (int k = 0; k < num_columns; k++) {
    // Ajouter un petit epsilon pour éviter log(0)
    double pk = (p[k] < EPSILON) ? EPSILON : p[k];
    double qk = (q[k] < EPSILON) ? EPSILON : q[k];
    kl_div += pk * log(pk / qk);
  }
  return 1.0 / (1.0 + kl_div);
}

// Calcul de la distance euclidienne
double euclidean_distance(int i, int j) {
  double sum = 0.0;
  for (int k = 0; k < num_columns; k++) {
    double diff = data[i][k] - data[j][k];
    sum += diff * diff;
  }
  return 1 / (1 + sum);
}

double compute_norm(double *vector, int length) {
  double norm = 0.0;
  for (int i = 0; i < length; i++)
    norm += vector[i] * vector[i];
  return sqrt(norm);
}

void normalize_vector(double *vector, int length) {
  double norm = compute_norm(vector, length);
  if (norm > 0) {
    for (int i = 0; i < length; i++)
      vector[i] /= norm;
  }
}

// Fonction utilitaire pour comparer des doubles (pour qsort)
int compare_double(const void *a, const void *b) {
  double arg1 = *(const double *)a;
  double arg2 = *(const double *)b;
  if (arg1 < arg2)
    return -1;
  if (arg1 > arg2)
    return 1;
  return 0;
}

// Structure pour les arguments des threads lors de la création des arêtes
struct edge_creation_args {
  int choice;
  double threshold;
  double anti_threshold;
  int start_row;
  int end_row;
  Barrier barrier;
};

// Fonction exécutée par chaque thread pour créer les arêtes à partir des
// similarités
static void edge_creation_job(void *args) {
  struct edge_creation_args *data = (struct edge_creation_args *)args;
  for (int i = data->start_row; i < data->end_row; i++) {
    for (int j = i + 1; j < num_rows; j++) {
      double similarity;
      switch (data->choice) {
      case 0:
        similarity = correlation_similarity(i, j);
        break;
      case 1:
        similarity = cosine_similarity(i, j);
        break;
      case 2:
        similarity = euclidean_distance(i, j);
        break;
      case 3:
        similarity = L1_norm(i, j);
        break;
      case 4:
        similarity = Linf_norm(i, j);
        break;
      case 5:
        similarity = KL_divergence(i, j);
        break;
      default:
        similarity = 0.0;
      }

      if (similarity > data->threshold && num_edges < MAX_EDGES) {
        int edge_index = incr_or_max(&num_edges, MAX_EDGES);
        if (edge_index < MAX_EDGES) {
          edges[edge_index].node1 = i;
          edges[edge_index].node2 = j;
          edges[edge_index].weight = similarity;
        }
      } else if (similarity < data->anti_threshold &&
                 num_antiedges < MAX_EDGES) {
        int antiedge_index = incr_or_max(&num_antiedges, MAX_EDGES);
        if (antiedge_index < MAX_EDGES) {
          antiedges[antiedge_index].node1 = i;
          antiedges[antiedge_index].node2 = j;
          antiedges[antiedge_index].weight = similarity;
        }
      }
    }
  }
  decrement_barrier(data->barrier, 1);
}

void create_edges_from_thresholds(int choice, double threshold,
                                  double anti_threshold) {
  num_edges = 0;
  num_antiedges = 0;

  struct barrier bar;
  new_barrier(&bar, pool.nb_threads);

  int rows_per_thread = num_rows / pool.nb_threads;
  int remaining_rows = num_rows % pool.nb_threads;

  for (int t = 0; t < pool.nb_threads; t++) {
    struct edge_creation_args *args = malloc(sizeof(struct edge_creation_args));
    args->choice = choice;
    args->threshold = threshold;
    args->anti_threshold = anti_threshold;
    args->start_row = t * rows_per_thread;
    args->end_row = (t + 1) * rows_per_thread;
    if (t == pool.nb_threads - 1)
      args->end_row += remaining_rows;
    args->barrier = &bar;

    struct Job task;
    task.j = edge_creation_job;
    task.args = args;
    submit(&pool, task);
  }

  wait_barrier(&bar);

  num_antiedges = fmin(num_antiedges, MAX_EDGES);
  num_edges = fmin(num_edges, MAX_EDGES);
}
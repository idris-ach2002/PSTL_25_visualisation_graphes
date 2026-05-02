#include "kmeans.h"
#include "cluster.h"
#include "../global.h"

struct kmeans_args {
  int first;
  int last;
  int num_clusters;
  int *labels;
  double (*centers)[2];
  double *sum_x;
  double *sum_y;
  int *count;
  Barrier barrier;
};

static int choose_nearest_center(Point p, double centers[][2], int count) {
  double best_dist = DBL_MAX;
  int best = 0;

  for (int c = 0; c < count; ++c) {
    double dx = p.x - centers[c][0];
    double dy = p.y - centers[c][1];
    double dist = dx * dx + dy * dy;
    if (dist < best_dist) {
      best_dist = dist;
      best = c;
    }
  }
  return best;
}

static void kmeans_worker(void *arg) {
  struct kmeans_args *a = (struct kmeans_args *)arg;

  for (int i = a->first; i < a->last; ++i) {
    if (vertices[i].deleted) {
      continue;
    }

    int c = choose_nearest_center(vertices[i], a->centers, a->num_clusters);
    a->labels[i] = c;
    a->sum_x[c] += vertices[i].x;
    a->sum_y[c] += vertices[i].y;
    a->count[c]++;
  }

  decrement_barrier(a->barrier, 1);
}

/**
 * Effectue une itération k-means sans verrou dans la boucle chaude.
 *
 * @param num_points nombre de sommets à traiter.
 * @param num_clusters nombre de centres.
 * @param labels tableau labels[num_points] mis à jour.
 * @param centers tableau centers[num_clusters][2] mis à jour.
 * @param Lx largeur du plan, conservée pour compatibilité.
 * @param Ly hauteur du plan, conservée pour compatibilité.
 * @param max_diff reçoit le plus grand déplacement carré d'un centre.
 *
 * Les threads accumulent dans des tableaux locaux. La réduction est faite après
 * la barrière. Cela supprime le mutex global de l'ancienne version.
 */
void kmeans_iteration_fast(int num_points, int num_clusters, int *labels,
                           double centers[][2], double Lx, double Ly,
                           double *max_diff) {
  (void)Lx;
  (void)Ly;

  if (num_points <= 0 || num_clusters <= 0) {
    return;
  }

  int threads = pool.nb_threads > 0 ? pool.nb_threads : 1;
  if (threads > num_points) {
    threads = num_points;
  }

  double *local_x = (double *)calloc((size_t)threads * (size_t)num_clusters,
                                    sizeof(double));
  double *local_y = (double *)calloc((size_t)threads * (size_t)num_clusters,
                                    sizeof(double));
  int *local_count = (int *)calloc((size_t)threads * (size_t)num_clusters,
                                  sizeof(int));
  if (local_x == NULL || local_y == NULL || local_count == NULL) {
    free(local_x);
    free(local_y);
    free(local_count);
    return;
  }

  struct barrier bar;
  new_barrier(&bar, threads);

  int chunk = (num_points + threads - 1) / threads;
  for (int t = 0; t < threads; ++t) {
    struct kmeans_args *arg =
        (struct kmeans_args *)malloc(sizeof(struct kmeans_args));
    if (arg == NULL) {
      decrement_barrier(&bar, 1);
      continue;
    }
    arg->first = t * chunk;
    arg->last = arg->first + chunk;
    if (arg->last > num_points) {
      arg->last = num_points;
    }
    arg->num_clusters = num_clusters;
    arg->labels = labels;
    arg->centers = centers;
    arg->sum_x = local_x + (size_t)t * (size_t)num_clusters;
    arg->sum_y = local_y + (size_t)t * (size_t)num_clusters;
    arg->count = local_count + (size_t)t * (size_t)num_clusters;
    arg->barrier = &bar;

    struct Job job;
    job.j = kmeans_worker;
    job.args = arg;
    submit(&pool, job);
  }

  wait_barrier(&bar);

  if (max_diff != NULL) {
    *max_diff = 0.0;
  }

  for (int c = 0; c < num_clusters; ++c) {
    double sx = 0.0;
    double sy = 0.0;
    int count = 0;

    for (int t = 0; t < threads; ++t) {
      size_t idx = (size_t)t * (size_t)num_clusters + (size_t)c;
      sx += local_x[idx];
      sy += local_y[idx];
      count += local_count[idx];
    }

    if (count > 0) {
      double old_x = centers[c][0];
      double old_y = centers[c][1];
      centers[c][0] = sx / (double)count;
      centers[c][1] = sy / (double)count;

      if (max_diff != NULL) {
        double dx = centers[c][0] - old_x;
        double dy = centers[c][1] - old_y;
        double diff = dx * dx + dy * dy;
        if (diff > *max_diff) {
          *max_diff = diff;
        }
      }
    }
  }

  free(local_x);
  free(local_y);
  free(local_count);
}

#ifndef FORCEATLAS_SIMILITUDE_H
#define FORCEATLAS_SIMILITUDE_H

#include <stdatomic.h>
#include "../concurrent/Pool.h"

// Prototypes des fonctions de calcul des mesures de similarité
double correlation_similarity(int i, int j);
double cosine_similarity(int i, int j);
double euclidean_distance(int i, int j);
double L1_norm(int i, int j);
double Linf_norm(int i, int j);
double KL_divergence(int i, int j);

double compute_norm(double *vector, int length);
void normalize_vector(double *vector, int length);

// Créer les arêtes avec des seuils donnés
void create_edges_from_thresholds(int choice, double threshold, double anti_threshold);

// Estimation des seuils par échantillonnage + création des arêtes
void compute_threshold_and_edges(int choice, double *threshold, double *anti_threshold, int target_edges);

// Fonction de comparaison pour qsort
int compare_double(const void *a, const void *b);

#endif
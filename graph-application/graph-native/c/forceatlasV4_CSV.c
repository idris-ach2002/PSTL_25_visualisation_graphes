#include <float.h>
#include <math.h>
#include <stdbool.h>
#include <string.h>
#include <time.h>

#ifdef _WIN32
#include <windows.h>
#else
#include <termios.h>
#endif

#include <stdatomic.h>

#include "global.h"

#ifdef _DEBUG_
#include "debug/debug_time.h"
#endif

/* #include "../out/linux/com_mongraphe_graphui_Graph.h" */

int modeA = 0; // mode pour afficher des noeuds en fonction de classe

int iteration = 0;
double Max_movementOld = 0;
int max_iterations = 5000;

short pause_updates = 0;

/**
 *
 *
 *
 Fonction Pour L'interface
 *
 *
 *
 *
 */

JNIEXPORT jboolean JNICALL
Java_com_mongraphe_graphui_rendering_GraphNativeEngine_updatePositions(
    JNIEnv *env, jobject obj, jobject buffer) {
  double FMaxX = Lx / (friction * 1000);
  double FMaxY = Ly / (friction * 1000);

  double PasMaxX = Lx / 10.;
  double PasMaxY = Ly / 10.;

  // Recalculer les seuils en fonction de la taille actuelle de la fenêtre
  thresholdS = (Lx / 4000.0) * (Ly / 4000.0);
  thresholdA = (Lx / 4000.0) * (Ly / 4000.0);
  epsilon = (Lx / 800.0) * (Ly / 800.0);
  seuilrep = (Lx / 1000.0) * (Lx / 1000.0);

  static double forces[MAX_NODES][2] = {{0, 0}};

  if (pause_updates == 0) {

    parallel_repulsion_edges(forces);

    parallel_intra_clusters(forces, FMaxX, FMaxY);

    parallel_repulsion_anti_edges(forces);

    double Max_movement = update_position_forces(forces, PasMaxX, PasMaxY, 0.);
    update_clusters();

    ++iteration;

    if (Max_movement == Max_movementOld) {
      friction *= 0.7;
    }

    Max_movementOld = Max_movement;
    friction *= amortissement;
  }

  for (int i = 0; i < num_nodes; ++i) {
    forces[i][0] = 0.;
    forces[i][1] = 0.;
  }

  if (buffer != NULL) {
    void *addr = (*env)->GetDirectBufferAddress(env, buffer);
    if (addr != NULL) {
      float *pos = (float *)addr;
      for (int i = 0; i < num_nodes; ++i) {
        pos[2 * i] = (float)vertices[i].x;
        pos[2 * i + 1] = (float)vertices[i].y;
      }
    }
  }

  return 1;
}

JNIEXPORT jintArray JNICALL
Java_com_mongraphe_graphui_rendering_GraphNativeEngine_getCommunities(
    JNIEnv *env, jobject obj) {
  jintArray result = (*env)->NewIntArray(env, num_nodes);
  if (result == NULL) {
    return NULL;
  }

  if (num_nodes > 0) {
    (*env)->SetIntArrayRegion(env, result, 0, num_nodes,
                              (const jint *)communities);
  }
  return result;
}

JNIEXPORT jobjectArray JNICALL
Java_com_mongraphe_graphui_rendering_GraphNativeEngine_getCommunityColors(
    JNIEnv *env, jobject obj) {
  jclass float_array_class = (*env)->FindClass(env, "[F");
  if (float_array_class == NULL) {
    return NULL;
  }

  jobjectArray result =
      (*env)->NewObjectArray(env, num_nodes, float_array_class, NULL);
  if (result == NULL) {
    (*env)->DeleteLocalRef(env, float_array_class);
    return NULL;
  }

  for (int i = 0; i < num_nodes; ++i) {
    jfloatArray float_array = (*env)->NewFloatArray(env, 3);
    if (float_array == NULL) {
      break;
    }

    (*env)->SetFloatArrayRegion(env, float_array, 0, 3, cluster_colors[i]);
    (*env)->SetObjectArrayElement(env, result, i, float_array);
    (*env)->DeleteLocalRef(env, float_array);
  }

  (*env)->DeleteLocalRef(env, float_array_class);
  return result;
}

JNIEXPORT jobjectArray JNICALL
Java_com_mongraphe_graphui_rendering_GraphNativeEngine_getEdges(JNIEnv *env,
                                                                jobject obj) {
  jclass obj_class =
      (*env)->FindClass(env, "com/mongraphe/graphui/model/EdgeC");
  if (obj_class == NULL) {
    return NULL;
  }

  jmethodID edge_constructor =
      (*env)->GetMethodID(env, obj_class, "<init>", "(IID)V");
  if (edge_constructor == NULL) {
    (*env)->DeleteLocalRef(env, obj_class);
    return NULL;
  }

  jobjectArray result = (*env)->NewObjectArray(env, num_edges, obj_class, NULL);
  if (result == NULL) {
    (*env)->DeleteLocalRef(env, obj_class);
    return NULL;
  }

  for (int i = 0; i < num_edges; ++i) {
    int node1 = edges[i].node1;
    int node2 = edges[i].node2;
    double weight = edges[i].weight;
    jobject edge = (*env)->NewObject(env, obj_class, edge_constructor, node1,
                                     node2, weight);
    if (edge == NULL) {
      break;
    }

    (*env)->SetObjectArrayElement(env, result, i, edge);
    (*env)->DeleteLocalRef(env, edge);
  }

  (*env)->DeleteLocalRef(env, obj_class);
  return result;
}

JNIEXPORT jobjectArray JNICALL
Java_com_mongraphe_graphui_rendering_GraphNativeEngine_getPositions(
    JNIEnv *env, jobject obj) {
  jclass obj_class =
      (*env)->FindClass(env, "com/mongraphe/graphui/model/Vertex");
  if (obj_class == NULL) {
    return NULL;
  }

  jmethodID point_constructor =
      (*env)->GetMethodID(env, obj_class, "<init>", "(DD)V");
  if (point_constructor == NULL) {
    (*env)->DeleteLocalRef(env, obj_class);
    return NULL;
  }

  jobjectArray result = (*env)->NewObjectArray(env, num_nodes, obj_class, NULL);
  if (result == NULL) {
    (*env)->DeleteLocalRef(env, obj_class);
    return NULL;
  }

  for (int i = 0; i < num_nodes; ++i) {
    double x = vertices[i].x;
    double y = vertices[i].y;

    jobject point = (*env)->NewObject(env, obj_class, point_constructor, x, y);
    if (point == NULL) {
      break;
    }

    (*env)->SetObjectArrayElement(env, result, i, point);
    (*env)->DeleteLocalRef(env, point);
  }

  (*env)->DeleteLocalRef(env, obj_class);
  return result;
}

JNIEXPORT jobject JNICALL
Java_com_mongraphe_graphui_rendering_GraphNativeEngine_startsProgram(
    JNIEnv *env, jobject obj, jstring filepath) {
  srand(time(NULL));

  jboolean b = JNI_FALSE;
  const char *str = (*env)->GetStringUTFChars(env, filepath, &b);

  num_rows = 0;

  load_csv_data(str);

  jclass obj_class = (*env)->FindClass(env, "[D");
  jobjectArray result = (*env)->NewObjectArray(env, num_rows, obj_class, 0);

  for (int i = 0; i < num_rows; ++i) {
    jdoubleArray double_array = (*env)->NewDoubleArray(env, num_columns);
    (*env)->SetDoubleArrayRegion(env, double_array, 0, num_columns, data[i]);

    (*env)->SetObjectArrayElement(env, result, i, double_array);
    (*env)->DeleteLocalRef(env, double_array);
  }

  (*env)->ReleaseStringUTFChars(env, filepath, str);

  return result;
}

JNIEXPORT jobject JNICALL
Java_com_mongraphe_graphui_rendering_GraphNativeEngine_computeThreshold(
    JNIEnv *env, jobject obj, jint modeSimilitude, jint edge_factor) {

  num_nodes = num_rows;
  live_nodes = num_nodes;
  mode_similitude = modeSimilitude;

  double threshold, anti_threshold, mean_similarity = 0.0;
  int total_pairs = num_rows * (num_rows - 1) / 2;
  int target_edges = edge_factor * num_nodes; // nombre d'arêtes souhaité

  // Taille de l'échantillon : 100 * target_edges mais pas plus de 500 000
  int sample_size =
      (target_edges * 100 < total_pairs) ? target_edges * 100 : total_pairs;
  if (sample_size < 1)
    sample_size = 1;
  if (sample_size > 500000)
    sample_size = 500000;

  double *sample_similarities = malloc(sample_size * sizeof(double));
  if (!sample_similarities) {
    // Fallback : valeurs par défaut
    threshold = anti_threshold = mean_similarity = 0.5;
    jclass res_class =
        (*env)->FindClass(env, "com/mongraphe/graphui/model/Metadata");
    jmethodID constructor =
        (*env)->GetMethodID(env, res_class, "<init>", "(IDDD)V");
    return (*env)->NewObject(env, res_class, constructor, num_nodes, threshold,
                             anti_threshold, mean_similarity);
  }

  double sum = 0.0;
  for (int k = 0; k < sample_size; k++) {
    int i = rand() % num_rows;
    int j = rand() % num_rows;
    while (i == j)
      j = rand() % num_rows;
    double sim;
    switch (modeSimilitude) {
    case 0:
      sim = correlation_similarity(i, j);
      break;
    case 1:
      sim = cosine_similarity(i, j);
      break;
    case 2:
      sim = euclidean_distance(i, j);
      break;
    case 3:
      sim = L1_norm(i, j);
      break;
    case 4:
      sim = Linf_norm(i, j);
      break;
    case 5:
      sim = KL_divergence(i, j);
      break;
    default:
      sim = 0.0;
    }
    sample_similarities[k] = sim;
    sum += sim;
  }

  qsort(sample_similarities, sample_size, sizeof(double), compare_double);

  // Calcul de l'indice en fonction de la proportion d'arêtes souhaitée
  double fraction = (double)target_edges / total_pairs;
  int idx = (int)(fraction * sample_size);
  if (idx < 0)
    idx = 0;
  if (idx >= sample_size)
    idx = sample_size - 1;

  threshold = sample_similarities[sample_size - 1 - idx];
  anti_threshold = sample_similarities[idx];
  mean_similarity = sum / sample_size;

  free(sample_similarities);

  jclass res_class =
      (*env)->FindClass(env, "com/mongraphe/graphui/model/Metadata");
  jmethodID constructor =
      (*env)->GetMethodID(env, res_class, "<init>", "(IDDD)V");
  return (*env)->NewObject(env, res_class, constructor, num_nodes, threshold,
                           anti_threshold, mean_similarity);
}

JNIEXPORT jobject JNICALL
Java_com_mongraphe_graphui_rendering_GraphNativeEngine_initializeGraph(
    JNIEnv *env, jobject obj, jint modeSimilitude, jint modeCommunity,
    jdouble thresh, jdouble anti_thresh) {

  num_nodes = num_rows;
  live_nodes = num_nodes;

  num_edges = 0;
  num_antiedges = 0;

  InitPool(&pool, 1000, 8);

  // Création des arêtes avec le mode de similarité
  create_edges_from_thresholds(modeSimilitude, thresh, anti_thresh);
  build_csr_adjacency();

  // Détection des communautés avec le mode communauté
  modeA = 0;
  if (modeCommunity == 0) {
    num_communities = louvain_method();
  } else if (modeCommunity == 1) {
    num_communities = louvain_methodC();
  } else if (modeCommunity == 2) {
    num_communities = leiden_method();
  } else if (modeCommunity == 3) {
    num_communities = leiden_method_CPM();
  } else if (modeCommunity == 4) {
    init_S(num_nodes);
    num_communities = leiden_method_CPM();
    lireColonneCSV(S, &nbValeurs);
    modeA = 1;
    compute_ratio_S(S);
    free_S();
  }

  initialize_community_colors();

  for (int i = 0; i < num_nodes; i++) {
    random_point_in_center(i);
  }
  n_clusters = (int)sqrt(num_nodes);
  init_clusters(n_clusters);
  initialize_centers();
  assign_cluster_colors();
  calculate_node_degrees();

  jclass res_class =
      (*env)->FindClass(env, "com/mongraphe/graphui/model/Metadata");
  jmethodID constructor =
      (*env)->GetMethodID(env, res_class, "<init>", "(IDDIII)V");
  jobject res =
      (*env)->NewObject(env, res_class, constructor, num_nodes, thresh,
                        anti_thresh, num_edges, num_antiedges, n_clusters);

  // Initialisation des paramètres par défaut
  thresholdS = (Lx / 4000) * (Ly / 4000);
  thresholdA = (Lx / 4000) * (Ly / 4000);
  epsilon = (Lx / 800) * (Ly / 800);
  seuilrep = (Lx / 1000) * (Lx / 1000);

  friction = 0.1;
  Max_movementOld = 0.;
  iteration = 0;
  espacement = 1;
  pause_updates = 0;

  return res;
}

JNIEXPORT jobject JNICALL
Java_com_mongraphe_graphui_rendering_GraphNativeEngine_initializeDot(
    JNIEnv *env, jobject obj, jstring filepath, jint modeCommunity) {

  const char *str = (*env)->GetStringUTFChars(env, filepath, NULL);
  parse_dot_file(str);
  (*env)->ReleaseStringUTFChars(env, filepath, str);

  build_csr_adjacency();
  InitPool(&pool, 1000, 8);

  modeA = 0;
  if (modeCommunity == 0) {
    num_communities = louvain_method();
  } else if (modeCommunity == 1) {
    num_communities = louvain_methodC();
  } else if (modeCommunity == 2) {
    num_communities = leiden_method();
  } else if (modeCommunity == 3) {
    num_communities = leiden_method_CPM();
  } else if (modeCommunity == 4) {
    init_S(num_nodes);
    num_communities = leiden_method_CPM();
    lireColonneCSV(S, &nbValeurs);
    modeA = 1;
    compute_ratio_S(S);
    free_S();
  }

  initialize_community_colors();
  for (int i = 0; i < num_nodes; i++) {
    random_point_in_center(i);
  }

  n_clusters = (int)sqrt(num_nodes);
  init_clusters(n_clusters);
  initialize_centers();
  assign_cluster_colors();
  calculate_node_degrees();

  jclass res_class =
      (*env)->FindClass(env, "com/mongraphe/graphui/model/Metadata");
  jmethodID constructor =
      (*env)->GetMethodID(env, res_class, "<init>", "(IDDD)V");
  jobject res =
      (*env)->NewObject(env, res_class, constructor, num_nodes, 0., 0., 0.);

  live_nodes = num_nodes;
  thresholdS = (Lx / 4000) * (Ly / 4000);
  thresholdA = (Lx / 4000) * (Ly / 4000);
  epsilon = (Lx / 800) * (Ly / 800);
  seuilrep = (Lx / 1000) * (Lx / 1000);

  friction = 0.1;
  Max_movementOld = 0.;
  iteration = 0;
  espacement = 1;
  pause_updates = 0;

  return res;
}

JNIEXPORT void JNICALL
Java_com_mongraphe_graphui_rendering_GraphNativeEngine_nativeFreeAllocatedMemory(
    JNIEnv *env, jobject obj) {

  free_clusters();

  freeNodeNames();
  free_csr_adjacency();
  FreePool(&pool);

  num_nodes = 0;
  live_nodes = 0;
  num_edges = 0;
  num_antiedges = 0;
}

JNIEXPORT void JNICALL
Java_com_mongraphe_graphui_rendering_GraphNativeEngine_setFriction(JNIEnv *env,
                                                                   jobject obj,
                                                                   jdouble f) {
  friction = f;
}

JNIEXPORT void JNICALL
Java_com_mongraphe_graphui_rendering_GraphNativeEngine_setModeRepulsion(
    JNIEnv *env, jobject obj, jint modeRepulsion) {
  mode = modeRepulsion;
}

JNIEXPORT void JNICALL
Java_com_mongraphe_graphui_rendering_GraphNativeEngine_setAntiRepulsion(
    JNIEnv *env, jobject obj, jdouble repulsion) {
  coeff_antiarete = repulsion;
}

JNIEXPORT void JNICALL
Java_com_mongraphe_graphui_rendering_GraphNativeEngine_setAttractionCoeff(
    JNIEnv *env, jobject obj, jdouble coeff) {
  attraction_coeff = coeff;
}

JNIEXPORT void JNICALL
Java_com_mongraphe_graphui_rendering_GraphNativeEngine_setDimension(
    JNIEnv *env, jobject obj, jdouble width, jdouble height) {
  Lx = width;
  Ly = height;
}

JNIEXPORT jdoubleArray JNICALL
Java_com_mongraphe_graphui_rendering_GraphNativeEngine_getDimensions(
    JNIEnv *env, jobject obj) {
  jdoubleArray result = (*env)->NewDoubleArray(env, 2);
  if (result == NULL)
    return NULL;
  jdouble dims[2] = {Lx, Ly};
  (*env)->SetDoubleArrayRegion(env, result, 0, 2, dims);
  return result;
}

JNIEXPORT void JNICALL
Java_com_mongraphe_graphui_rendering_GraphNativeEngine_setAmortissement(
    JNIEnv *env, jobject obj, jdouble amort) {
  amortissement = amort;
}

JNIEXPORT void JNICALL
Java_com_mongraphe_graphui_rendering_GraphNativeEngine_setNodePosition(
    JNIEnv *env, jobject obj, jint index, jdouble x, jdouble y) {
  vertices[index].x = x;
  vertices[index].y = y;
}

JNIEXPORT void JNICALL
Java_com_mongraphe_graphui_rendering_GraphNativeEngine_setSpatialCells(
    JNIEnv *env, jobject obj, jint new_n_clusters) {
  free_clusters();
  n_clusters = new_n_clusters;
  init_clusters(n_clusters);
  initialize_centers();
  assign_cluster_colors();
}

JNIEXPORT void JNICALL
Java_com_mongraphe_graphui_rendering_GraphNativeEngine_setEpsilon(JNIEnv *env,
                                                                  jobject obj,
                                                                  jdouble eps) {
  epsilon = eps;
}

JNIEXPORT void JNICALL
Java_com_mongraphe_graphui_rendering_GraphNativeEngine_deleteNode(JNIEnv *env,
                                                                  jobject obj,
                                                                  jint index) {

  if (vertices[index].deleted == 0) {
    vertices[index].deleted = 1;
    modified_graph = 1;
    --live_nodes;
  }
}

JNIEXPORT void JNICALL
Java_com_mongraphe_graphui_rendering_GraphNativeEngine_restoreNode(JNIEnv *env,
                                                                   jobject obj,
                                                                   jint index) {

  if (vertices[index].deleted == 1) {
    vertices[index].deleted = 0;
    modified_graph = 1;
    ++live_nodes;
  }
}

JNIEXPORT void JNICALL
Java_com_mongraphe_graphui_rendering_GraphNativeEngine_setKmeansMode(
    JNIEnv *env, jobject obj, jboolean b) {
  kmeans_mode = b;
}

JNIEXPORT jobject JNICALL
Java_com_mongraphe_graphui_rendering_GraphEngine_getHistogram(JNIEnv *env,
                                                              jobject obj) {
  jintArray result = (*env)->NewIntArray(env, NUM_BINS);

  (*env)->SetIntArrayRegion(env, result, 0, NUM_BINS,
                            (const jint *)global_histogram);

  return result;
}

JNIEXPORT void JNICALL
Java_com_mongraphe_graphui_rendering_GraphNativeEngine_setInitialNodeSize(
    JNIEnv *env, jobject obj, jdouble size) {
  initial_node_size = size;
}

JNIEXPORT void JNICALL
Java_com_mongraphe_graphui_rendering_GraphNativeEngine_setDegreeScaleFactor(
    JNIEnv *env, jobject obj, jdouble factor) {
  degree_scale_factor = factor;
}

JNIEXPORT void JNICALL
Java_com_mongraphe_graphui_rendering_GraphNativeEngine_setRepulsionCoeff(
    JNIEnv *env, jobject obj, jdouble coeff) {
  repulsion_coeff = coeff;
}
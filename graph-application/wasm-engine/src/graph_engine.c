#include "graph_engine.h"
#include <math.h>
#include <stdint.h>
#include <stdlib.h>
#include <string.h>

#ifdef __EMSCRIPTEN__
#include <emscripten/emscripten.h>
#else
#define EMSCRIPTEN_KEEPALIVE
#endif

#define GE_MAX(a,b) ((a) > (b) ? (a) : (b))
#define GE_MIN(a,b) ((a) < (b) ? (a) : (b))
#define GE_EPS 0.0001f
#define GE_MAX_TREE_MULT 8

typedef struct {
    int node_count;
    int edge_count;
    int live_node_count;
    int iteration;
    float width;
    float height;
    float repulsion;
    float attraction;
    float damping;
    float time_step;
    float theta;
    float anti_repulsion;
    int repulsion_mode;
    int kmeans_enabled;
    int kmeans_clusters;
    float *positions;       /* x,y per node */
    float *velocities;      /* vx,vy per node */
    float *forces;          /* fx,fy per node */
    float *colors;          /* r,g,b per node */
    int *edges;             /* source,target per edge */
    float *weights;         /* weight per edge */
    int *degrees;
    int *communities;
    unsigned char *deleted;
} GraphState;

typedef struct {
    float cx;
    float cy;
    float half;
    float mass;
    float com_x;
    float com_y;
    int body;
    int child[4];
} QuadNode;

static GraphState G = {0};
static QuadNode *tree = NULL;
static int tree_count = 0;
static int tree_capacity = 0;
static uint32_t rng_state = 0xC001CAFEu;

static uint32_t xorshift32(void) {
    uint32_t x = rng_state;
    x ^= x << 13;
    x ^= x >> 17;
    x ^= x << 5;
    rng_state = x ? x : 0x9E3779B9u;
    return rng_state;
}

static float rand01(void) {
    return (float)(xorshift32() & 0x00FFFFFFu) / (float)0x01000000u;
}

static int alloc_graph(int n, int m) {
    if (n <= 0 || m < 0) return 0;
    ge_free();
    G.node_count = n;
    G.edge_count = m;
    G.live_node_count = n;
    G.width = 1600.0f;
    G.height = 1000.0f;
    G.repulsion = 9000.0f;
    G.attraction = 0.015f;
    G.damping = 0.86f;
    G.time_step = 0.016f;
    G.theta = 0.72f;
    G.anti_repulsion = 2800.0f;
    G.repulsion_mode = 0;
    G.kmeans_enabled = 0;
    G.kmeans_clusters = 12;

    G.positions = (float *)calloc((size_t)n * 2u, sizeof(float));
    G.velocities = (float *)calloc((size_t)n * 2u, sizeof(float));
    G.forces = (float *)calloc((size_t)n * 2u, sizeof(float));
    G.colors = (float *)calloc((size_t)n * 3u, sizeof(float));
    G.edges = (int *)calloc((size_t)m * 2u, sizeof(int));
    G.weights = (float *)calloc((size_t)m, sizeof(float));
    G.degrees = (int *)calloc((size_t)n, sizeof(int));
    G.communities = (int *)calloc((size_t)n, sizeof(int));
    G.deleted = (unsigned char *)calloc((size_t)n, sizeof(unsigned char));

    if (!G.positions || !G.velocities || !G.forces || !G.colors || !G.edges ||
        !G.weights || !G.degrees || !G.communities || !G.deleted) {
        ge_free();
        return 0;
    }
    return 1;
}

static void compute_degrees(void) {
    if (!G.degrees) return;
    memset(G.degrees, 0, (size_t)G.node_count * sizeof(int));
    for (int e = 0; e < G.edge_count; ++e) {
        int a = G.edges[2 * e];
        int b = G.edges[2 * e + 1];
        if (a >= 0 && a < G.node_count && b >= 0 && b < G.node_count) {
            G.degrees[a]++;
            G.degrees[b]++;
        }
    }
}

static float hue_to_rgb(float p, float q, float t) {
    if (t < 0.0f) t += 1.0f;
    if (t > 1.0f) t -= 1.0f;
    if (t < 1.0f / 6.0f) return p + (q - p) * 6.0f * t;
    if (t < 1.0f / 2.0f) return q;
    if (t < 2.0f / 3.0f) return p + (q - p) * (2.0f / 3.0f - t) * 6.0f;
    return p;
}

static void color_from_community(int community, float *r, float *g, float *b) {
    float h = fmodf((float)community * 0.61803398875f, 1.0f);
    float s = 0.68f;
    float l = 0.56f;
    float q = l < 0.5f ? l * (1.0f + s) : l + s - l * s;
    float p = 2.0f * l - q;
    *r = hue_to_rgb(p, q, h + 1.0f / 3.0f);
    *g = hue_to_rgb(p, q, h);
    *b = hue_to_rgb(p, q, h - 1.0f / 3.0f);
}

static void apply_community_colors(void) {
    for (int i = 0; i < G.node_count; ++i) {
        int c = G.communities ? G.communities[i] : i;
        color_from_community(c, &G.colors[3 * i], &G.colors[3 * i + 1], &G.colors[3 * i + 2]);
    }
}

EMSCRIPTEN_KEEPALIVE
void ge_reset_layout(unsigned int seed) {
    if (!G.positions) return;
    rng_state = seed ? seed : 0xC001CAFEu;
    float radius = sqrtf((float)G.node_count) * 18.0f + 80.0f;
    for (int i = 0; i < G.node_count; ++i) {
        float angle = rand01() * 6.28318530718f;
        float r = sqrtf(rand01()) * radius;
        G.positions[2 * i] = cosf(angle) * r + (rand01() - 0.5f) * 20.0f;
        G.positions[2 * i + 1] = sinf(angle) * r + (rand01() - 0.5f) * 20.0f;
        G.velocities[2 * i] = 0.0f;
        G.velocities[2 * i + 1] = 0.0f;
    }
    G.iteration = 0;
}

EMSCRIPTEN_KEEPALIVE
int ge_init_from_edges(int node_count, int edge_count, const int *edge_pairs, const float *edge_weights) {
    if (!alloc_graph(node_count, edge_count)) return 0;
    for (int e = 0; e < edge_count; ++e) {
        int a = edge_pairs ? edge_pairs[2 * e] : 0;
        int b = edge_pairs ? edge_pairs[2 * e + 1] : 0;
        if (a < 0 || a >= node_count || b < 0 || b >= node_count || a == b) {
            a = 0; b = 0;
        }
        G.edges[2 * e] = a;
        G.edges[2 * e + 1] = b;
        float w = edge_weights ? edge_weights[e] : 1.0f;
        if (!isfinite(w) || fabsf(w) < 0.00001f) w = 1.0f;
        if (w > 100.0f) w = 100.0f;
        if (w < -100.0f) w = -100.0f;
        G.weights[e] = w;
    }
    for (int i = 0; i < node_count; ++i) G.communities[i] = i;
    compute_degrees();
    ge_run_label_propagation(8);
    ge_reset_layout(0xA11CEu);
    return 1;
}

EMSCRIPTEN_KEEPALIVE
int ge_init_random(int node_count, int edge_count) {
    if (node_count <= 0) return 0;
    if (edge_count < node_count - 1) edge_count = node_count - 1;
    if (!alloc_graph(node_count, edge_count)) return 0;
    for (int i = 0; i < node_count; ++i) G.communities[i] = i;
    int e = 0;
    for (int i = 1; i < node_count && e < edge_count; ++i, ++e) {
        G.edges[2 * e] = i;
        G.edges[2 * e + 1] = (int)(xorshift32() % (uint32_t)i);
        G.weights[e] = 1.0f;
    }
    while (e < edge_count) {
        int a = (int)(xorshift32() % (uint32_t)node_count);
        int b = (int)(xorshift32() % (uint32_t)node_count);
        if (a == b) b = (b + 1) % node_count;
        G.edges[2 * e] = a;
        G.edges[2 * e + 1] = b;
        G.weights[e] = 1.0f;
        ++e;
    }
    compute_degrees();
    ge_run_label_propagation(10);
    ge_reset_layout(0xBEEFu);
    return 1;
}

static int tree_new_node(float cx, float cy, float half) {
    if (tree_count >= tree_capacity) return -1;
    int idx = tree_count++;
    tree[idx].cx = cx;
    tree[idx].cy = cy;
    tree[idx].half = half;
    tree[idx].mass = 0.0f;
    tree[idx].com_x = 0.0f;
    tree[idx].com_y = 0.0f;
    tree[idx].body = -1;
    for (int k = 0; k < 4; ++k) tree[idx].child[k] = -1;
    return idx;
}

static int quadrant_for(const QuadNode *q, float x, float y) {
    int east = x >= q->cx;
    int south = y >= q->cy;
    return south * 2 + east;
}

static void child_center(const QuadNode *q, int quadrant, float *cx, float *cy) {
    float d = q->half * 0.5f;
    int east = quadrant & 1;
    int south = quadrant & 2;
    *cx = q->cx + (east ? d : -d);
    *cy = q->cy + (south ? d : -d);
}

static float node_mass(int body) {
    if (G.repulsion_mode == 1) return 1.0f;
    return 1.0f + sqrtf((float)GE_MAX(0, G.degrees[body]));
}

static void tree_update_mass(int node_idx, int body) {
    float x = G.positions[2 * body];
    float y = G.positions[2 * body + 1];
    float m = node_mass(body);
    float old_mass = tree[node_idx].mass;
    float new_mass = old_mass + m;
    if (new_mass > 0.0f) {
        tree[node_idx].com_x = (tree[node_idx].com_x * old_mass + x * m) / new_mass;
        tree[node_idx].com_y = (tree[node_idx].com_y * old_mass + y * m) / new_mass;
    }
    tree[node_idx].mass = new_mass;
}

static void tree_insert_body(int node_idx, int body, int depth) {
    if (node_idx < 0 || body < 0 || depth > 32) return;
    tree_update_mass(node_idx, body);

    if (tree[node_idx].body == -1 && tree[node_idx].child[0] == -1) {
        tree[node_idx].body = body;
        return;
    }

    if (tree[node_idx].child[0] == -1) {
        int old = tree[node_idx].body;
        tree[node_idx].body = -1;
        for (int q = 0; q < 4; ++q) {
            float cx, cy;
            child_center(&tree[node_idx], q, &cx, &cy);
            tree[node_idx].child[q] = tree_new_node(cx, cy, tree[node_idx].half * 0.5f);
        }
        int q_old = quadrant_for(&tree[node_idx], G.positions[2 * old], G.positions[2 * old + 1]);
        tree_insert_body(tree[node_idx].child[q_old], old, depth + 1);
    }

    int q_new = quadrant_for(&tree[node_idx], G.positions[2 * body], G.positions[2 * body + 1]);
    tree_insert_body(tree[node_idx].child[q_new], body, depth + 1);
}

static int build_tree(void) {
    if (G.node_count <= 0) return 0;
    tree_capacity = GE_MAX(16, G.node_count * GE_MAX_TREE_MULT);
    QuadNode *new_tree = (QuadNode *)realloc(tree, (size_t)tree_capacity * sizeof(QuadNode));
    if (!new_tree) return 0;
    tree = new_tree;
    tree_count = 0;

    float min_x = 1e30f, max_x = -1e30f, min_y = 1e30f, max_y = -1e30f;
    for (int i = 0; i < G.node_count; ++i) {
        if (G.deleted[i]) continue;
        float x = G.positions[2 * i];
        float y = G.positions[2 * i + 1];
        min_x = GE_MIN(min_x, x); max_x = GE_MAX(max_x, x);
        min_y = GE_MIN(min_y, y); max_y = GE_MAX(max_y, y);
    }
    if (min_x > max_x) return 0;
    float cx = 0.5f * (min_x + max_x);
    float cy = 0.5f * (min_y + max_y);
    float half = 0.5f * GE_MAX(max_x - min_x, max_y - min_y) + 10.0f;
    int root = tree_new_node(cx, cy, half);
    if (root < 0) return 0;
    for (int i = 0; i < G.node_count; ++i) {
        if (!G.deleted[i]) tree_insert_body(root, i, 0);
    }
    return 1;
}

static void apply_repulsion_from_tree(int body, int node_idx) {
    if (node_idx < 0 || body < 0 || tree[node_idx].mass <= 0.0f) return;
    if (tree[node_idx].body == body && tree[node_idx].child[0] == -1) return;

    float x = G.positions[2 * body];
    float y = G.positions[2 * body + 1];
    float dx = x - tree[node_idx].com_x;
    float dy = y - tree[node_idx].com_y;
    float dist2 = dx * dx + dy * dy + 25.0f;
    float dist = sqrtf(dist2);
    float size = tree[node_idx].half * 2.0f;

    if (tree[node_idx].child[0] == -1 || (size / dist) < G.theta) {
        float body_mass = node_mass(body);
        float f = G.repulsion * body_mass * tree[node_idx].mass / dist2;
        G.forces[2 * body] += (dx / dist) * f;
        G.forces[2 * body + 1] += (dy / dist) * f;
        return;
    }

    for (int q = 0; q < 4; ++q) {
        if (tree[node_idx].child[q] >= 0) apply_repulsion_from_tree(body, tree[node_idx].child[q]);
    }
}

static void apply_forces_once(void) {
    memset(G.forces, 0, (size_t)G.node_count * 2u * sizeof(float));
    if (build_tree()) {
        for (int i = 0; i < G.node_count; ++i) {
            if (!G.deleted[i]) apply_repulsion_from_tree(i, 0);
        }
    }

    for (int e = 0; e < G.edge_count; ++e) {
        int a = G.edges[2 * e];
        int b = G.edges[2 * e + 1];
        if (a < 0 || a >= G.node_count || b < 0 || b >= G.node_count || G.deleted[a] || G.deleted[b]) continue;
        float ax = G.positions[2 * a];
        float ay = G.positions[2 * a + 1];
        float bx = G.positions[2 * b];
        float by = G.positions[2 * b + 1];
        float dx = bx - ax;
        float dy = by - ay;
        float dist = sqrtf(dx * dx + dy * dy) + GE_EPS;
        float w = G.weights[e];
        float desired = 38.0f + 3.0f * sqrtf((float)(G.degrees[a] + G.degrees[b] + 1));
        float f;
        if (w >= 0.0f) {
            f = G.attraction * w * (dist - desired);
        } else {
            /* Anti-arêtes : les lignes très dissemblables se repoussent directement. */
            f = -G.anti_repulsion * (-w) / (dist * dist + 25.0f);
        }
        if (G.repulsion_mode == 2 && G.communities && G.communities[a] != G.communities[b]) {
            f *= 1.35f;
        }
        float fx = (dx / dist) * f;
        float fy = (dy / dist) * f;
        G.forces[2 * a] += fx;
        G.forces[2 * a + 1] += fy;
        G.forces[2 * b] -= fx;
        G.forces[2 * b + 1] -= fy;
    }

    float max_speed = 550.0f;
    for (int i = 0; i < G.node_count; ++i) {
        if (G.deleted[i]) continue;
        int ix = 2 * i;
        float vx = (G.velocities[ix] + G.forces[ix] * G.time_step) * G.damping;
        float vy = (G.velocities[ix + 1] + G.forces[ix + 1] * G.time_step) * G.damping;
        float speed = sqrtf(vx * vx + vy * vy);
        if (speed > max_speed) {
            float scale = max_speed / speed;
            vx *= scale;
            vy *= scale;
        }
        G.velocities[ix] = vx;
        G.velocities[ix + 1] = vy;
        G.positions[ix] += vx * G.time_step;
        G.positions[ix + 1] += vy * G.time_step;
    }
    G.iteration++;
}


static void ge_run_spatial_kmeans_internal(int clusters, int iterations) {
    if (!G.positions || !G.communities || G.node_count <= 0) return;
    if (clusters < 2) clusters = 2;
    if (clusters > 128) clusters = 128;
    if (clusters > G.node_count) clusters = G.node_count;
    if (iterations < 1) iterations = 1;
    if (iterations > 12) iterations = 12;

    float *cx = (float *)calloc((size_t)clusters, sizeof(float));
    float *cy = (float *)calloc((size_t)clusters, sizeof(float));
    int *counts = (int *)calloc((size_t)clusters, sizeof(int));
    if (!cx || !cy || !counts) {
        free(cx); free(cy); free(counts);
        return;
    }

    for (int c = 0; c < clusters; ++c) {
        int idx = (int)((long long)c * (long long)G.node_count / (long long)clusters);
        cx[c] = G.positions[2 * idx];
        cy[c] = G.positions[2 * idx + 1];
    }

    for (int it = 0; it < iterations; ++it) {
        memset(counts, 0, (size_t)clusters * sizeof(int));
        float *sumx = (float *)calloc((size_t)clusters, sizeof(float));
        float *sumy = (float *)calloc((size_t)clusters, sizeof(float));
        if (!sumx || !sumy) {
            free(sumx); free(sumy);
            break;
        }
        for (int i = 0; i < G.node_count; ++i) {
            if (G.deleted && G.deleted[i]) continue;
            float x = G.positions[2 * i];
            float y = G.positions[2 * i + 1];
            int best = 0;
            float best_d = 1e38f;
            for (int c = 0; c < clusters; ++c) {
                float dx = x - cx[c];
                float dy = y - cy[c];
                float d = dx * dx + dy * dy;
                if (d < best_d) { best_d = d; best = c; }
            }
            G.communities[i] = best;
            sumx[best] += x;
            sumy[best] += y;
            counts[best]++;
        }
        for (int c = 0; c < clusters; ++c) {
            if (counts[c] > 0) {
                cx[c] = sumx[c] / (float)counts[c];
                cy[c] = sumy[c] / (float)counts[c];
            }
        }
        free(sumx); free(sumy);
    }
    free(cx); free(cy); free(counts);
    apply_community_colors();
}

EMSCRIPTEN_KEEPALIVE
void ge_run_spatial_kmeans(int clusters, int iterations) {
    ge_run_spatial_kmeans_internal(clusters, iterations);
}

EMSCRIPTEN_KEEPALIVE
int ge_step(int iterations) {
    if (!G.positions || !G.edges || iterations < 1) return 0;
    if (iterations > 200) iterations = 200;
    for (int i = 0; i < iterations; ++i) {
        apply_forces_once();
        if (G.kmeans_enabled && (G.iteration % 30) == 0) {
            ge_run_spatial_kmeans_internal(G.kmeans_clusters, 1);
        }
    }
    return G.iteration;
}

EMSCRIPTEN_KEEPALIVE
void ge_free(void) {
    free(G.positions); free(G.velocities); free(G.forces); free(G.colors);
    free(G.edges); free(G.weights); free(G.degrees); free(G.communities); free(G.deleted);
    memset(&G, 0, sizeof(G));
}

EMSCRIPTEN_KEEPALIVE int ge_get_node_count(void) { return G.node_count; }
EMSCRIPTEN_KEEPALIVE int ge_get_edge_count(void) { return G.edge_count; }
EMSCRIPTEN_KEEPALIVE int ge_get_live_node_count(void) { return G.live_node_count; }
EMSCRIPTEN_KEEPALIVE int ge_get_iteration(void) { return G.iteration; }
EMSCRIPTEN_KEEPALIVE float *ge_get_positions_ptr(void) { return G.positions; }
EMSCRIPTEN_KEEPALIVE float *ge_get_colors_ptr(void) { return G.colors; }
EMSCRIPTEN_KEEPALIVE int *ge_get_edges_ptr(void) { return G.edges; }
EMSCRIPTEN_KEEPALIVE int *ge_get_degrees_ptr(void) { return G.degrees; }
EMSCRIPTEN_KEEPALIVE float *ge_get_weights_ptr(void) { return G.weights; }
EMSCRIPTEN_KEEPALIVE int *ge_get_communities_ptr(void) { return G.communities; }
EMSCRIPTEN_KEEPALIVE unsigned char *ge_get_deleted_ptr(void) { return G.deleted; }

EMSCRIPTEN_KEEPALIVE
void ge_set_dimensions(float width, float height) {
    G.width = width > 1.0f ? width : G.width;
    G.height = height > 1.0f ? height : G.height;
}

EMSCRIPTEN_KEEPALIVE
void ge_set_force_params(float repulsion, float attraction, float damping, float time_step, float theta, float anti_repulsion, int repulsion_mode, int kmeans_enabled, int kmeans_clusters) {
    if (repulsion > 0.0f) G.repulsion = repulsion;
    if (attraction > 0.0f) G.attraction = attraction;
    if (damping > 0.0f && damping < 1.0f) G.damping = damping;
    if (time_step > 0.0f && time_step < 0.2f) G.time_step = time_step;
    if (theta > 0.1f && theta < 2.0f) G.theta = theta;
    if (anti_repulsion > 0.0f) G.anti_repulsion = anti_repulsion;
    if (repulsion_mode >= 0 && repulsion_mode <= 2) G.repulsion_mode = repulsion_mode;
    G.kmeans_enabled = kmeans_enabled ? 1 : 0;
    if (kmeans_clusters >= 2 && kmeans_clusters <= 128) G.kmeans_clusters = kmeans_clusters;
}

EMSCRIPTEN_KEEPALIVE
void ge_set_node_position(int node, float x, float y) {
    if (node < 0 || node >= G.node_count || !G.positions) return;
    G.positions[2 * node] = x;
    G.positions[2 * node + 1] = y;
    G.velocities[2 * node] = 0.0f;
    G.velocities[2 * node + 1] = 0.0f;
}

EMSCRIPTEN_KEEPALIVE
void ge_delete_node(int node) {
    if (node < 0 || node >= G.node_count || !G.deleted) return;
    if (!G.deleted[node]) {
        G.deleted[node] = 1;
        if (G.live_node_count > 0) G.live_node_count--;
    }
}

EMSCRIPTEN_KEEPALIVE
void ge_restore_node(int node) {
    if (node < 0 || node >= G.node_count || !G.deleted) return;
    if (G.deleted[node]) {
        G.deleted[node] = 0;
        G.live_node_count++;
    }
}

EMSCRIPTEN_KEEPALIVE
void ge_run_label_propagation(int iterations) {
    if (!G.communities || !G.edges) return;
    if (iterations < 1) iterations = 1;
    if (iterations > 50) iterations = 50;
    for (int i = 0; i < G.node_count; ++i) G.communities[i] = i;
    if (G.node_count > 5000 || G.edge_count > 30000) {
        for (int i = 0; i < G.node_count; ++i) G.communities[i] = G.degrees ? (G.degrees[i] % 32) : (i % 32);
        apply_community_colors();
        return;
    }
    int *new_labels = (int *)malloc((size_t)G.node_count * sizeof(int));
    if (!new_labels) return;
    for (int it = 0; it < iterations; ++it) {
        memcpy(new_labels, G.communities, (size_t)G.node_count * sizeof(int));
        for (int i = 0; i < G.node_count; ++i) {
            if (G.deleted && G.deleted[i]) continue;
            int best_label = G.communities[i];
            int best_score = -1;
            for (int e = 0; e < G.edge_count; ++e) {
                int a = G.edges[2 * e];
                int b = G.edges[2 * e + 1];
                int other = -1;
                if (a == i) other = b;
                else if (b == i) other = a;
                if (other < 0 || other >= G.node_count) continue;
                int label = G.communities[other];
                int score = 0;
                for (int e2 = 0; e2 < G.edge_count; ++e2) {
                    int u = G.edges[2 * e2];
                    int v = G.edges[2 * e2 + 1];
                    int neighbor = -1;
                    if (u == i) neighbor = v;
                    else if (v == i) neighbor = u;
                    if (neighbor >= 0 && G.communities[neighbor] == label) score++;
                }
                if (score > best_score || (score == best_score && label < best_label)) {
                    best_score = score;
                    best_label = label;
                }
            }
            new_labels[i] = best_label;
        }
        memcpy(G.communities, new_labels, (size_t)G.node_count * sizeof(int));
    }
    free(new_labels);
    apply_community_colors();
}

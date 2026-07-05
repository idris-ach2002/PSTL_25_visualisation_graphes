#define _GNU_SOURCE
#include "data.h"
#include "../global.h"

#include <ctype.h>
#include <errno.h>
#include <stdio.h>
#include <stdlib.h>
#include <string.h>

#if defined(__GLIBC__) || defined(__linux__)
#define FAST_GETC getc_unlocked
#else
#define FAST_GETC fgetc
#endif

#define MAXBUF 1024

int nbValeurs;
double **data = NULL;
int num_rows = 0, num_columns = 0;
char delimiter[1] = "\0";
int *S = NULL;

char **node_names = NULL;

static double *data_block = NULL;

/* ------------------------------------------------------------ */
/* CSV loading                                                  */
/* ------------------------------------------------------------ */

static void free_csv_data(void) {
  free(data);
  data = NULL;

  free(data_block);
  data_block = NULL;

  num_rows = 0;
  num_columns = 0;
  delimiter[0] = '\0';
}

char *my_getline(FILE *file) {
  static char buffer[MAX_LINE_LENGTH];
  if (fgets(buffer, MAX_LINE_LENGTH, file) != NULL) {
    return buffer;
  }
  return NULL;
}

short str_is_number(char *line) {
  if (*line == '-') {
    ++line;
  } else if (*line == '\0') {
    return 0;
  }

  short pt = 0;
  while ((*line >= '0' && *line <= '9') || (*line == '.' && !pt) ||
         *line == 'e' || *line == 'E' || *line == '+' || *line == '-') {
    if (*line == '.') {
      pt = 1;
    } else if (*line == 'e' || *line == 'E') {
      ++line;
      if (*line == '+' || *line == '-') {
        ++line;
      }
      if (*line == '\0') {
        return 0;
      }
      continue;
    }
    ++line;
  }

  return *line == '\0';
}

static char detect_csv_delimiter(const char *line) {
  const char candidates[] = {',', ';', '|', '\t', ' '};
  int best_count = -1;
  char best = ',';

  for (size_t k = 0; k < sizeof(candidates); ++k) {
    int count = 0;
    for (const char *p = line; *p != '\0'; ++p) {
      if (*p == candidates[k]) {
        ++count;
      }
    }
    if (count > best_count) {
      best_count = count;
      best = candidates[k];
    }
  }

  return best;
}

static int count_columns_from_header(const char *line, char delim) {
  int count = 1;
  for (const char *p = line; *p != '\0'; ++p) {
    if (*p == delim) {
      ++count;
    }
  }
  return count;
}

static int ensure_csv_capacity(int wanted_rows, int cols, int *row_capacity) {
  if (wanted_rows <= *row_capacity) {
    return 1;
  }

  int new_cap = (*row_capacity > 0) ? *row_capacity : 1024;
  while (new_cap < wanted_rows) {
    new_cap *= 2;
  }

  double *new_block =
      (double *)realloc(data_block, (size_t)new_cap * (size_t)cols * sizeof(double));
  if (new_block == NULL) {
    return 0;
  }
  data_block = new_block;

  double **new_rows = (double **)realloc(data, (size_t)new_cap * sizeof(double *));
  if (new_rows == NULL) {
    return 0;
  }
  data = new_rows;

  for (int i = 0; i < new_cap; ++i) {
    data[i] = data_block + (size_t)i * (size_t)cols;
  }

  *row_capacity = new_cap;
  return 1;
}

static void parse_csv_row_inplace(char *line, char delim, double *out, int cols,
                                  int row_index) {
  int col = 0;
  char *p = line;

  while (col < cols) {
    char *start = p;
    char *end = start;

    while (*end != '\0' && *end != delim && *end != '\n' && *end != '\r') {
      ++end;
    }

    char saved = *end;
    *end = '\0';

    while (*start == ' ' || *start == '\t') {
      ++start;
    }

    if (*start == '\0') {
      out[col] = 0.0;
    } else {
      char *parse_end = NULL;
      double value = strtod(start, &parse_end);
      if (parse_end == start) {
        out[col] = 0.0;
        fprintf(stderr, "Warning: invalid value \"%s\" on row %d, col %d\n",
                start, row_index + 1, col);
      } else {
        out[col] = value;
      }
    }

    ++col;

    if (saved == '\0') {
      break;
    }
    p = end + 1;
  }

  while (col < cols) {
    out[col++] = 0.0;
  }
}

void load_csv_data(const char *filename) {
  free_csv_data();

  FILE *file = fopen(filename, "r");
  if (!file) {
    fprintf(stderr, "Could not open file %s: %s\n", filename, strerror(errno));
    exit(1);
  }

  char *line = NULL;
  size_t line_cap = 0;
  ssize_t line_len = 0;

  line_len = getline(&line, &line_cap, file);
  if (line_len < 0) {
    free(line);
    fclose(file);
    num_rows = 0;
    num_columns = 0;
    return;
  }

  line[strcspn(line, "\r\n")] = '\0';
  delimiter[0] = detect_csv_delimiter(line);
  num_columns = count_columns_from_header(line, delimiter[0]);

  int row_capacity = 0;
  num_rows = 0;

  while ((line_len = getline(&line, &line_cap, file)) >= 0) {
    line[strcspn(line, "\r\n")] = '\0';

    if (!ensure_csv_capacity(num_rows + 1, num_columns, &row_capacity)) {
      perror("realloc csv data");
      free(line);
      fclose(file);
      free_csv_data();
      exit(1);
    }

    parse_csv_row_inplace(line, delimiter[0], data[num_rows], num_columns,
                          num_rows);
    ++num_rows;
  }

  free(line);
  fclose(file);

  if (num_rows == 0) {
    free_csv_data();
    return;
  }

  double *shrunk_block = (double *)realloc(
      data_block, (size_t)num_rows * (size_t)num_columns * sizeof(double));
  if (shrunk_block != NULL) {
    data_block = shrunk_block;
    for (int i = 0; i < num_rows; ++i) {
      data[i] = data_block + (size_t)i * (size_t)num_columns;
    }
  }

  double **shrunk_rows =
      (double **)realloc(data, (size_t)num_rows * sizeof(double *));
  if (shrunk_rows != NULL) {
    data = shrunk_rows;
  }
}

void init_S(int num_nodes_local) {
  int size = (num_nodes_local * 2 < MAX_NODES) ? num_nodes_local * 2 : MAX_NODES;
  S = (int *)malloc(sizeof(int) * (size_t)size);
  if (!S) {
    fprintf(stderr, "Error d'allocation mémoire: init_S\n");
    exit(1);
  }
}

void free_S() {
  if (S) {
    free(S);
    S = NULL;
  }
}

void lireColonneCSV(int *S_out, int *nbValeurs_out) {
  char chemin[256];
  printf("Veuillez entrer le chemin du fichier CSV: ");
  scanf("%255s", chemin);

  FILE *fichier = fopen(chemin, "r");
  if (fichier == NULL) {
    perror("Erreur lors de l'ouverture du fichier");
    return;
  }

  char *line = NULL;
  size_t line_cap = 0;
  ssize_t line_len = 0;

  *nbValeurs_out = 0;

  /* Ignore header */
  (void)getline(&line, &line_cap, fichier);

  while ((line_len = getline(&line, &line_cap, fichier)) >= 0) {
    char *p = line;
    char *end = p;
    while (*end != '\0' && *end != delimiter[0] && *end != '\n' && *end != '\r') {
      ++end;
    }
    *end = '\0';

    if (*p != '\0') {
      char *parse_end = NULL;
      long value = strtol(p, &parse_end, 10);
      if (parse_end != p) {
        if (*nbValeurs_out < MAX_NODES) {
          S_out[*nbValeurs_out] = (int)value;
          (*nbValeurs_out)++;
        } else {
          fprintf(stderr, "Nombre maximum de valeurs atteint.\n");
          break;
        }
      }
    }
  }

  printf("Les 5 premières lignes des données :\n");
  for (int i = 0; i < 5 && i < *nbValeurs_out; i++) {
    printf("%d\n", S_out[i]);
  }

  free(line);
  fclose(fichier);
}

/* ------------------------------------------------------------ */
/* DOT parsing                                                  */
/* ------------------------------------------------------------ */

struct HashPair {
  int index;
  char *label;
};

static size_t hash_string_len(const char *label, size_t len) {
  unsigned long long h = 1469598103934665603ULL;
  for (size_t i = 0; i < len; ++i) {
    h ^= (unsigned char)label[i];
    h *= 1099511628211ULL;
  }
  return (size_t)h;
}

int hash_string(char *label) {
  return (int)(hash_string_len(label, strlen(label)) & 0x7fffffffU);
}

static int hashpair_insert_existing(struct HashPair *map, size_t capacity,
                                    char *label, int index) {
  size_t pos = hash_string_len(label, strlen(label)) % capacity;
  while (map[pos].index != -1) {
    pos = (pos + 1) % capacity;
  }
  map[pos].label = label;
  map[pos].index = index;
  return 1;
}

int map_put(struct HashPair **map, size_t *capacity, char *key, int size_key) {
  if ((*capacity == 0) || ((size_t)(num_nodes + 1) * 10 >= (*capacity) * 7)) {
    size_t old_capacity = *capacity;
    size_t new_capacity = old_capacity ? old_capacity * 2 : 1024;

    struct HashPair *new_map =
        (struct HashPair *)malloc(sizeof(struct HashPair) * new_capacity);
    if (new_map == NULL) {
      return -1;
    }

    for (size_t i = 0; i < new_capacity; ++i) {
      new_map[i].label = NULL;
      new_map[i].index = -1;
    }

    for (size_t i = 0; i < old_capacity; ++i) {
      if ((*map)[i].index != -1 && (*map)[i].label != NULL) {
        hashpair_insert_existing(new_map, new_capacity, (*map)[i].label,
                                 (*map)[i].index);
      }
    }

    char **new_node_names =
        (char **)realloc(node_names, sizeof(char *) * new_capacity);
    if (new_node_names == NULL) {
      free(new_map);
      return -1;
    }
    node_names = new_node_names;
    for (size_t i = old_capacity; i < new_capacity; ++i) {
      node_names[i] = NULL;
    }

    free(*map);
    *map = new_map;
    *capacity = new_capacity;
  }

  size_t ind = hash_string_len(key, (size_t)size_key) % *capacity;
  while ((*map)[ind].index != -1) {
    if (strncmp(key, (*map)[ind].label, (size_t)size_key) == 0 &&
        (*map)[ind].label[size_key] == '\0') {
      return (*map)[ind].index;
    }
    ind = (ind + 1) % *capacity;
  }

  (*map)[ind].index = num_nodes++;
  (*map)[ind].label = (char *)malloc((size_t)size_key + 1);
  if ((*map)[ind].label == NULL) {
    return -1;
  }
  memcpy((*map)[ind].label, key, (size_t)size_key);
  (*map)[ind].label[size_key] = '\0';

  return (*map)[ind].index;
}

int belongs(char *str, const char **keywords, size_t size) {
  for (size_t i = 0; i < size; ++i) {
    if (strcmp(str, keywords[i]) == 0) {
      return 1;
    }
  }
  return 0;
}

int isKeyword(char *str) {
  const char *keywords[] = {"digraph", "strict",  "graph",   "subgraph", "node",
                            "edge",    "nodesep", "ranksep", "margin"};
  return belongs(str, keywords, 9);
}

int isCompass(char *str) {
  const char *compass[] = {"n",  "ne", "nw", "s", "se",
                           "sw", "e",  "w",  "c", "_"};
  return belongs(str, compass, 10);
}

int isDelimiter(char c) {
  const char delimiters[] = {' ', '[', ']', '{', '}', '=',  ';',
                             ',', ':', '"', '-', '>', '\n', '\r', '\t'};
  size_t nb_delimiter = sizeof(delimiters) / sizeof(delimiters[0]);
  for (size_t i = 0; i < nb_delimiter; ++i) {
    if (delimiters[i] == c) {
      return 1;
    }
  }
  return 0;
}

int isHtmlString(const char *s, size_t size) {
  return size >= 2 && s[0] == '<' && s[size - 1] == '>';
}

int isQuoteString(const char *s, size_t size) {
  if (size < 2 || s[0] != '"' || s[size - 1] != '"') {
    return 0;
  }

  for (size_t i = 0; i + 1 < size; ++i) {
    if (s[i] == '\\' && s[i + 1] == '"') {
      ++i;
    } else if (s[i] == '"') {
      return 0;
    }
  }
  return 1;
}

int isAlpha(const char *s, size_t size) {
  if (size == 0) {
    return 0;
  }

  for (size_t i = 0; i < size; ++i) {
    unsigned char ch = (unsigned char)s[i];
    if (!isalnum(ch) && (200 > ch || ch > 255) && ch != '_') {
      return 0;
    }
  }
  return 1;
}

int isId(char *s, size_t size) {
  return isHtmlString(s, size) || str_is_number(s) || isQuoteString(s, size) ||
         isAlpha(s, size);
}

int isParameter(char *s) {
  if (strcmp(s, "weight") == 0)
    return 1;
  if (strcmp(s, "label") == 0)
    return 2;
  return 0;
}

int next_token(FILE *file, char *buffer, size_t *size, int inQuotes) {
  int c;
  *size = 0;

  while ((c = FAST_GETC(file)) != EOF) {
    if ((inQuotes && (c != '"' || (*size > 0 && buffer[*size - 1] == '\\'))) ||
        !isDelimiter((char)c)) {
      if (*size + 1 < MAXBUF) {
        buffer[*size] = (char)c;
        ++*size;
      }
    } else {
      break;
    }
  }

  buffer[*size] = '\0';
  return c;
}

struct vect {
  int *content;
  int capacity, size;
};
typedef struct vect *Vect;

struct vect newVect(int capacity) {
  struct vect res;
  res.capacity = capacity;
  res.content = (int *)malloc(sizeof(int) * (size_t)capacity);
  res.size = 0;
  return res;
}

void add_elem_list(Vect nodes, int elem) {
  if (nodes->size >= nodes->capacity) {
    int new_capacity = nodes->capacity * 2;
    int *new_content =
        (int *)realloc(nodes->content, (size_t)new_capacity * sizeof(int));
    if (new_content == NULL) {
      return;
    }
    nodes->content = new_content;
    nodes->capacity = new_capacity;
  }
  nodes->content[nodes->size++] = elem;
}

void freeVect(Vect v) {
  if (v != NULL) {
    free(v->content);
    free(v);
  }
}

int add_new_edge(Vect nodes, Vect tmp) {
  if (tmp) {
    for (int i = 0; i < tmp->size; ++i) {
      if (num_edges >= MAX_EDGES)
        return -1;
      edges[num_edges].node1 = tmp->content[i];
      edges[num_edges].node2 = nodes->content[nodes->size - 1];
      edges[num_edges].weight = 1.0;
      ++num_edges;
    }
  } else if (num_edges < MAX_EDGES) {
    edges[num_edges].node1 = nodes->content[nodes->size - 2];
    edges[num_edges].node2 = nodes->content[nodes->size - 1];
    edges[num_edges].weight = 1.0;
    ++num_edges;
  } else {
    return -1;
  }

  return 0;
}

void parse_attr(FILE *file, char *buffer, double *weight, char **label) {
  size_t size = 0;
  int c = next_token(file, buffer, &size, 0);

  while (c != EOF && c != ']') {
    int id = isParameter(buffer);
    if (id != 0) {
      c = next_token(file, buffer, &size, 0);
      while (size == 0 && c != '"' && c != EOF) {
        c = next_token(file, buffer, &size, 0);
      }

      if (id == 1) {
        char *endptr = NULL;
        double w = strtod(buffer, &endptr);
        if (endptr != buffer) {
          *weight = w;
        }
      } else {
        if (c == '"') {
          c = next_token(file, buffer, &size, 1);
        }

        *label = (char *)malloc(size + 1);
        if (*label == NULL) {
          return;
        }
        memcpy(*label, buffer, size);
        (*label)[size] = '\0';
      }
    }
    c = next_token(file, buffer, &size, 0);
  }
}

Vect parse_stmt_list(FILE *file, char *buffer, struct HashPair **map,
                     size_t *capacity) {
  size_t size = 0;
  int flag = 1;
  int side_edge = 0;
  int ignore = 0;

  struct vect node_list1 = newVect(4);
  Vect node_list_tmp1 = NULL;
  Vect node_list_tmp2 = NULL;

  int last_edge = num_edges;
  int edge_overflow = 0;

  while (flag) {
    int c = next_token(file, buffer, &size, 0);

    if (size > 0 && !isKeyword(buffer) && isId(buffer, size) &&
        !isCompass(buffer)) {
      if (!ignore) {
        int index = map_put(map, capacity, buffer, (int)size);
        if (index < 0) {
          freeVect(node_list_tmp1);
          freeVect(node_list_tmp2);
          break;
        }

        if (!side_edge) {
          for (int i = 0; node_list_tmp1 && i < node_list_tmp1->size; ++i) {
            add_elem_list(&node_list1, node_list_tmp1->content[i]);
          }
          freeVect(node_list_tmp1);
          node_list_tmp1 = NULL;
        }

        add_elem_list(&node_list1, index);
        last_edge = num_edges;

        if (side_edge) {
          edge_overflow |= add_new_edge(&node_list1, node_list_tmp1);
          freeVect(node_list_tmp1);
          node_list_tmp1 = NULL;
        }
        side_edge = 0;
      } else {
        ignore = 0;
      }
    }

    switch (c) {
    case '{':
      node_list_tmp2 = parse_stmt_list(file, buffer, map, capacity);
      if (side_edge) {
        last_edge = num_edges;
        if (node_list_tmp1) {
          for (int i = 0; !edge_overflow && i < node_list_tmp1->size; ++i) {
            for (int j = 0; j < node_list_tmp2->size; ++j) {
              if (num_edges >= MAX_EDGES) {
                edge_overflow = 1;
                break;
              }
              edges[num_edges].node1 = node_list_tmp1->content[i];
              edges[num_edges].node2 = node_list_tmp2->content[j];
              edges[num_edges].weight = 1.0;
              ++num_edges;
            }
          }
        } else {
          for (int i = 0; !edge_overflow && i < node_list_tmp2->size; ++i) {
            if (num_edges >= MAX_EDGES) {
              edge_overflow = 1;
              break;
            }
            int last_list1 = node_list1.size - 1;
            edges[num_edges].node1 = node_list1.content[last_list1];
            edges[num_edges].node2 = node_list_tmp2->content[i];
            edges[num_edges].weight = 1.0;
            ++num_edges;
          }
        }
        side_edge = 0;
      }

      for (int i = 0; node_list_tmp1 && i < node_list_tmp1->size; ++i) {
        add_elem_list(&node_list1, node_list_tmp1->content[i]);
      }
      freeVect(node_list_tmp1);
      node_list_tmp1 = node_list_tmp2;
      node_list_tmp2 = NULL;
      break;

    case '-':
      c = next_token(file, buffer, &size, 0);
      if (c == '>' || c == '-') {
        side_edge = 1;
      }
      break;

    case '"':
      (void)next_token(file, buffer, &size, 1);
      break;

    case '[': {
      double w = 1.0;
      char *label = NULL;
      parse_attr(file, buffer, &w, &label);

      for (int i = last_edge; i < num_edges; ++i) {
        edges[i].weight = w;
      }

      if (label != NULL && last_edge == num_edges && node_list1.size > 0) {
        int node_index = node_list1.content[node_list1.size - 1];
        free(node_names[node_index]);
        node_names[node_index] = label;
      }
      break;
    }

    case '=':
      ignore = 1;
      break;

    case '}':
    case EOF:
      flag = 0;
      break;

    default:
      break;
    }
  }

  for (int i = 0; node_list_tmp1 && i < node_list_tmp1->size; ++i) {
    add_elem_list(&node_list1, node_list_tmp1->content[i]);
  }
  freeVect(node_list_tmp1);

  Vect res = (Vect)malloc(sizeof(struct vect));
  if (res == NULL) {
    free(node_list1.content);
    return NULL;
  }

  res->content = node_list1.content;
  res->capacity = node_list1.capacity;
  res->size = node_list1.size;
  return res;
}

static void freeHashMap(struct HashPair *map, size_t capacity) {
  if (map == NULL) {
    return;
  }

  for (size_t i = 0; i < capacity; ++i) {
    free(map[i].label);
    map[i].label = NULL;
  }

  free(map);
}

void parse_dot_file(const char *filename) {
  FILE *file = fopen(filename, "r");
  if (file == NULL) {
    perror("Error opening file");
    return;
  }

  freeNodeNames();

  num_nodes = 0;
  num_edges = 0;
  num_antiedges = 0;

  char buf[MAXBUF];
  size_t size = 0;

  int c = next_token(file, buf, &size, 0);
  while (c != EOF && c != '{') {
    c = next_token(file, buf, &size, 0);
  }

  size_t capacity = 1024;
  if ((size_t)MAX_NODES < capacity) {
    capacity = (size_t)MAX_NODES;
  }

  struct HashPair *map =
      (struct HashPair *)malloc(sizeof(struct HashPair) * capacity);
  node_names = (char **)malloc(sizeof(char *) * capacity);

  if (map == NULL || node_names == NULL) {
    free(map);
    free(node_names);
    node_names = NULL;
    fclose(file);
    return;
  }

  for (size_t i = 0; i < capacity; ++i) {
    map[i].index = -1;
    map[i].label = NULL;
    node_names[i] = NULL;
  }

  if (c == '{') {
    Vect res = parse_stmt_list(file, buf, &map, &capacity);
    freeVect(res);
  }

  freeHashMap(map, capacity);
  fclose(file);
}

void freeNodeNames() {
  if (node_names != NULL) {
    for (int i = 0; i < num_nodes; ++i) {
      free(node_names[i]);
      node_names[i] = NULL;
    }
    free(node_names);
    node_names = NULL;
  }
}
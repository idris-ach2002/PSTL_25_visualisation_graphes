import type { BuildOptions, EdgeTuple, ImportAnalysis, ImportSampleMode, ParsedGraph, SimilarityMode } from '../types/graph';

export const WEB_GRAPH_NODE_LIMIT = 1000;
export const DEMO_GRAPH_NODE_LIMIT = 400;

export class GraphSizeLimitError extends Error {
  constructor(public readonly nodeCount: number, public readonly limit = WEB_GRAPH_NODE_LIMIT, public readonly source = 'import') {
    super(`Le graphe analysé contient ${nodeCount.toLocaleString('fr-FR')} nœuds. La version web est limitée à ${limit.toLocaleString('fr-FR')} nœuds pour conserver une visualisation fluide.`);
    this.name = 'GraphSizeLimitError';
  }
}

function ensureNodeLimit(nodeCount: number, limit = WEB_GRAPH_NODE_LIMIT, source = 'import'): void {
  if (nodeCount > limit) throw new GraphSizeLimitError(nodeCount, limit, source);
}


const DEFAULT_PARSE_OPTIONS: Pick<BuildOptions, 'maxWebNodes' | 'similarityMode' | 'automaticThresholds' | 'edgeThreshold' | 'antiEdgeThreshold' | 'kNearest' | 'maxExactNodes'> = {
  maxWebNodes: WEB_GRAPH_NODE_LIMIT,
  similarityMode: 'cosine',
  automaticThresholds: true,
  edgeThreshold: 0.92,
  antiEdgeThreshold: -0.45,
  kNearest: 8,
  maxExactNodes: WEB_GRAPH_NODE_LIMIT
};

type NumericRows = { rows: number[][]; labels?: string[]; warnings: string[]; width: number };

type ParseOptions = Partial<BuildOptions>;

function splitLines(text: string): string[] {
  return text.replace(/^\uFEFF/, '').split(/\r?\n/).map((line) => line.trim()).filter(Boolean);
}

function stripComments(line: string): string {
  return line.replace(/\/\/.*$/, '').replace(/#.*$/, '').trim();
}

function safeNumber(value: string): number | null {
  const normalized = value.trim().replace(',', '.');
  if (normalized === '') return null;
  const n = Number(normalized);
  return Number.isFinite(n) ? n : null;
}

function detectDelimiter(line: string): string {
  const candidates = [',', ';', '\t'];
  return candidates.sort((a, b) => line.split(b).length - line.split(a).length)[0];
}

function clamp(value: number, min: number, max: number): number {
  return Math.max(min, Math.min(max, value));
}

function normalizeSignedWeight(value: number | null): number {
  if (value == null || !Number.isFinite(value) || value === 0) return 1;
  const sign = value < 0 ? -1 : 1;
  return sign * clamp(Math.abs(value), 0.05, 100);
}

function addUndirectedEdge(edges: EdgeTuple[], seen: Set<string>, a: number, b: number, weight = 1): boolean {
  if (a === b || a < 0 || b < 0) return false;
  const x = Math.min(a, b);
  const y = Math.max(a, b);
  const key = `${x}:${y}`;
  if (seen.has(key)) return false;
  seen.add(key);
  edges.push([x, y, normalizeSignedWeight(weight)]);
  return true;
}

function quantile(sortedValues: number[], q: number): number {
  if (sortedValues.length === 0) return 0;
  const idx = clamp(Math.floor((sortedValues.length - 1) * q), 0, sortedValues.length - 1);
  return sortedValues[idx];
}

function uniqueNumericLabel(value: string): string {
  const numeric = safeNumber(value);
  if (numeric != null && Number.isInteger(numeric)) return String(numeric);
  return value.trim();
}

export function parseDot(text: string, name = 'DOT importé', parseOptions: ParseOptions = {}): ParsedGraph {
  const nodeIds = new Map<string, number>();
  const labels: string[] = [];
  const edges: EdgeTuple[] = [];
  const seen = new Set<string>();
  const getId = (raw: string) => {
    const clean = raw.trim().replace(/^"|"$/g, '');
    let id = nodeIds.get(clean);
    if (id == null) {
      id = nodeIds.size;
      nodeIds.set(clean, id);
      labels[id] = clean;
    }
    return id;
  };

  for (const rawLine of splitLines(text)) {
    const line = stripComments(rawLine).replace(/;$/, '').trim();
    const match = line.match(/^"?([^"\-\>\[]+)"?\s*(--|->)\s*"?([^"\[;]+)"?/);
    if (!match) continue;
    const a = getId(match[1]);
    const b = getId(match[3]);
    const weightMatch = line.match(/weight\s*=\s*(-?[0-9.,]+)/i);
    addUndirectedEdge(edges, seen, a, b, weightMatch ? safeNumber(weightMatch[1]) ?? 1 : 1);
  }

  ensureNodeLimit(nodeIds.size, parseOptions.maxWebNodes ?? WEB_GRAPH_NODE_LIMIT, 'dot');

  return {
    name,
    nodeCount: nodeIds.size,
    edges,
    labels,
    sourceKind: 'dot',
    warnings: edges.length === 0 ? ['Aucune arête DOT détectée. Format attendu : a -- b ou a -> b.'] : [],
    metadata: { positiveEdges: edges.filter((e) => e[2] >= 0).length, antiEdges: edges.filter((e) => e[2] < 0).length }
  };
}

export function parseEdgeListCsv(text: string, name = 'CSV edge-list', parseOptions: ParseOptions = {}): ParsedGraph | null {
  const lines = splitLines(text);
  if (lines.length === 0) return null;
  const delimiter = detectDelimiter(lines[0]);
  const header = lines[0].split(delimiter).map((v) => v.trim().toLowerCase());
  const sourceIndex = header.findIndex((h) => ['source', 'src', 'from', 'node1', 'u', 'start', 'début', 'debut'].includes(h));
  const targetIndex = header.findIndex((h) => ['target', 'dst', 'to', 'node2', 'v', 'end', 'fin'].includes(h));
  const weightIndex = header.findIndex((h) => ['weight', 'poids', 'w'].includes(h));
  const hasHeader = sourceIndex >= 0 && targetIndex >= 0;
  const start = hasHeader ? 1 : 0;

  const map = new Map<string, number>();
  const labels: string[] = [];
  const getId = (value: string) => {
    const label = uniqueNumericLabel(value);
    if (label === '') return -1;
    let id = map.get(label);
    if (id == null) {
      id = map.size;
      map.set(label, id);
      labels[id] = label;
    }
    return id;
  };

  const edges: EdgeTuple[] = [];
  const seen = new Set<string>();
  for (let i = start; i < lines.length; i++) {
    const cols = lines[i].split(delimiter).map((v) => v.trim());
    if (cols.length < 2) continue;
    const aCol = hasHeader ? sourceIndex : 0;
    const bCol = hasHeader ? targetIndex : 1;
    const wCol = hasHeader ? weightIndex : 2;
    const a = getId(cols[aCol] ?? '');
    const b = getId(cols[bCol] ?? '');
    const weight = wCol >= 0 ? safeNumber(cols[wCol] ?? '') : 1;
    addUndirectedEdge(edges, seen, a, b, normalizeSignedWeight(weight));
  }

  const firstData = lines[start]?.split(delimiter) ?? [];
  const firstTwoNumeric = firstData.length >= 2 && safeNumber(firstData[0]) != null && safeNumber(firstData[1]) != null;
  if (!hasHeader && !firstTwoNumeric) return null;
  if (edges.length === 0) return null;

  ensureNodeLimit(labels.length, parseOptions.maxWebNodes ?? WEB_GRAPH_NODE_LIMIT, 'edge-list');

  return {
    name,
    nodeCount: labels.length,
    edges,
    labels,
    sourceKind: 'edge-list',
    warnings: [],
    metadata: {
      positiveEdges: edges.filter((e) => e[2] >= 0).length,
      antiEdges: edges.filter((e) => e[2] < 0).length,
      density: edges.length / Math.max(1, labels.length * Math.max(1, labels.length - 1) / 2)
    }
  };
}

function shouldSkipLegacyMetadataRow(rows: string[][]): boolean {
  if (rows.length < 2) return false;
  const first = rows[0];
  const second = rows[1];
  const first0 = safeNumber(first[0] ?? '');
  const first1 = safeNumber(first[1] ?? '');
  const secondNumericCount = second.filter((v) => safeNumber(v) != null).length;
  return first0 != null && first1 != null && first.length > 2 && secondNumericCount >= Math.max(2, second.length - 1);
}

function parseNumericRows(text: string): NumericRows {
  const lines = splitLines(text).map(stripComments).filter(Boolean);
  if (lines.length === 0) return { rows: [], warnings: ['Fichier vide.'], width: 0 };
  const delimiter = detectDelimiter(lines[0]);
  let rawRows = lines.map((line) => line.split(delimiter).map((v) => v.trim()));
  const warnings: string[] = [];
  if (shouldSkipLegacyMetadataRow(rawRows)) {
    warnings.push('Première ligne type ancien projet détectée et ignorée : nombre de lignes, nombre de colonnes, classes.');
    rawRows = rawRows.slice(1);
  } else {
    const firstNumeric = rawRows[0].filter((v) => safeNumber(v) != null).length;
    const secondNumeric = rawRows[1]?.filter((v) => safeNumber(v) != null).length ?? 0;
    if (firstNumeric < secondNumeric && secondNumeric >= 2) {
      warnings.push('Ligne d’en-tête CSV ignorée.');
      rawRows = rawRows.slice(1);
    }
  }

  let labels: string[] | undefined;
  const hasLeadingLabel = rawRows.length > 0 && rawRows.every((cols) => cols.length >= 2 && safeNumber(cols[0]) == null && cols.slice(1).filter((v) => safeNumber(v) != null).length >= 2);
  if (hasLeadingLabel) {
    labels = rawRows.map((cols, i) => cols[0] || String(i));
    rawRows = rawRows.map((cols) => cols.slice(1));
    warnings.push('Première colonne détectée comme identifiant/label et exclue du calcul numérique.');
  }

  let rows = rawRows.map((cols) => cols.map(safeNumber).filter((n): n is number => n != null));
  rows = rows.filter((row) => row.length >= 2);
  if (rows.length === 0) return { rows: [], labels, warnings: [...warnings, 'Aucune ligne numérique exploitable détectée.'], width: 0 };

  const width = Math.min(...rows.map((row) => row.length));
  rows = rows.map((row) => row.slice(0, width));

  if (width > 2) {
    const lastValues = rows.map((row) => row[width - 1]);
    const unique = new Set(lastValues.map((v) => String(v)));
    const allInteger = lastValues.every((v) => Number.isInteger(v));
    if (allInteger && unique.size <= Math.max(20, Math.ceil(rows.length * 0.1))) {
      rows = rows.map((row) => row.slice(0, width - 1));
      warnings.push('Dernière colonne détectée comme classe/label et exclue du calcul de similarité.');
      return { rows, labels, warnings, width: width - 1 };
    }
  }
  return { rows, labels, warnings, width };
}

function standardizeRows(rows: number[][]): Float32Array[] {
  const n = rows.length;
  const d = rows[0]?.length ?? 0;
  const mean = new Array(d).fill(0);
  const stdev = new Array(d).fill(0);
  for (const row of rows) for (let j = 0; j < d; j++) mean[j] += row[j];
  for (let j = 0; j < d; j++) mean[j] /= n;
  for (const row of rows) for (let j = 0; j < d; j++) stdev[j] += (row[j] - mean[j]) ** 2;
  for (let j = 0; j < d; j++) stdev[j] = Math.sqrt(stdev[j] / Math.max(1, n - 1)) || 1;
  return rows.map((row) => Float32Array.from(row.map((v, j) => (v - mean[j]) / stdev[j])));
}

function cosine(a: Float32Array, b: Float32Array): number {
  let dot = 0;
  let na = 0;
  let nb = 0;
  for (let i = 0; i < a.length; i++) {
    dot += a[i] * b[i];
    na += a[i] * a[i];
    nb += b[i] * b[i];
  }
  return dot / (Math.sqrt(na) * Math.sqrt(nb) + 1e-9);
}

function euclideanSimilarity(a: Float32Array, b: Float32Array): number {
  let sum = 0;
  for (let i = 0; i < a.length; i++) {
    const d = a[i] - b[i];
    sum += d * d;
  }
  const dist = Math.sqrt(sum / Math.max(1, a.length));
  return 1 / (1 + dist);
}

function similarity(a: Float32Array, b: Float32Array, mode: SimilarityMode): number {
  if (mode === 'euclidean') return euclideanSimilarity(a, b);
  // Les lignes sont standardisées ; la corrélation et le cosinus deviennent proches.
  return cosine(a, b);
}

function sampleThresholds(vectors: Float32Array[], mode: SimilarityMode): { edgeThreshold: number; antiEdgeThreshold: number; sampled: number } {
  const n = vectors.length;
  const values: number[] = [];
  const maxSamples = 600000;
  if (n <= 1) return { edgeThreshold: 1, antiEdgeThreshold: -1, sampled: 0 };
  const step = Math.max(1, Math.floor((n * (n - 1) / 2) / maxSamples));
  let seen = 0;
  for (let i = 0; i < n; i++) {
    for (let j = i + 1; j < n; j++) {
      if (seen % step === 0) values.push(similarity(vectors[i], vectors[j], mode));
      seen++;
    }
  }
  values.sort((a, b) => a - b);
  const highQ = mode === 'euclidean' ? 0.965 : 0.97;
  const lowQ = mode === 'euclidean' ? 0.02 : 0.03;
  return {
    edgeThreshold: quantile(values, highQ),
    antiEdgeThreshold: quantile(values, lowQ),
    sampled: values.length
  };
}

function buildExactSimilarityGraph(vectors: Float32Array[], options: Required<Pick<BuildOptions, 'similarityMode' | 'automaticThresholds' | 'edgeThreshold' | 'antiEdgeThreshold' | 'kNearest'>>): { edges: EdgeTuple[]; warnings: string[]; edgeThreshold: number; antiEdgeThreshold: number; positiveEdges: number; antiEdges: number } {
  const n = vectors.length;
  const seen = new Set<string>();
  const edges: EdgeTuple[] = [];
  const warnings: string[] = [];
  let edgeThreshold = options.edgeThreshold;
  let antiEdgeThreshold = options.antiEdgeThreshold;

  if (options.automaticThresholds) {
    const thresholds = sampleThresholds(vectors, options.similarityMode);
    edgeThreshold = thresholds.edgeThreshold;
    antiEdgeThreshold = thresholds.antiEdgeThreshold;
    warnings.push(`Seuils recommandés calculés sur ${thresholds.sampled.toLocaleString('fr-FR')} comparaisons : arêtes ≥ ${edgeThreshold.toFixed(3)}, anti-arêtes ≤ ${antiEdgeThreshold.toFixed(3)}.`);
  }

  for (let i = 0; i < n; i++) {
    const best: Array<{ j: number; s: number }> = [];
    for (let j = i + 1; j < n; j++) {
      const s = similarity(vectors[i], vectors[j], options.similarityMode);
      if (s >= edgeThreshold) {
        addUndirectedEdge(edges, seen, i, j, options.similarityMode === 'euclidean' ? 0.2 + s * 2 : 1 + Math.max(0, s));
      } else if (s <= antiEdgeThreshold) {
        addUndirectedEdge(edges, seen, i, j, -Math.max(0.2, Math.abs(s - antiEdgeThreshold) + 0.25));
      }
      if (best.length < options.kNearest) {
        best.push({ j, s });
        best.sort((a, b) => b.s - a.s);
      } else if (s > best[best.length - 1].s) {
        best[best.length - 1] = { j, s };
        best.sort((a, b) => b.s - a.s);
      }
    }
    if (options.automaticThresholds) {
      for (const candidate of best) {
        addUndirectedEdge(edges, seen, i, candidate.j, options.similarityMode === 'euclidean' ? 0.2 + candidate.s * 2 : 1 + Math.max(0, candidate.s));
      }
    }
  }

  const positiveEdges = edges.filter((e) => e[2] >= 0).length;
  const antiEdges = edges.length - positiveEdges;
  return { edges, warnings, edgeThreshold, antiEdgeThreshold, positiveEdges, antiEdges };
}

function buildApproximateSimilarityGraph(vectors: Float32Array[], options: Required<Pick<BuildOptions, 'similarityMode' | 'kNearest'>>): { edges: EdgeTuple[]; warnings: string[]; positiveEdges: number; antiEdges: number } {
  const nodeCount = vectors.length;
  const seen = new Set<string>();
  const edges: EdgeTuple[] = [];
  const projections = 5;
  const neighborhood = Math.min(10, Math.max(4, options.kNearest));
  const warnings = [`Graphe volumineux (${nodeCount.toLocaleString('fr-FR')} lignes) : génération d’arêtes approximative par projections, plus rapide qu’un kNN complet.`];
  for (let p = 0; p < projections; p++) {
    const scored = vectors.map((row, index) => {
      let score = 0;
      for (let j = 0; j < row.length; j++) {
        const coeff = Math.sin((j + 1) * (p + 2) * 12.9898) + Math.cos((j + 3) * (p + 1) * 4.1414);
        score += row[j] * coeff;
      }
      return { index, score };
    });
    scored.sort((a, b) => a.score - b.score);
    for (let i = 0; i < scored.length; i++) {
      for (let offset = 1; offset <= neighborhood; offset++) {
        const j = i + offset;
        if (j >= scored.length) break;
        const a = scored[i].index;
        const b = scored[j].index;
        const s = similarity(vectors[a], vectors[b], options.similarityMode);
        addUndirectedEdge(edges, seen, a, b, options.similarityMode === 'euclidean' ? 0.2 + s * 2 : 1 + Math.max(0, s));
      }
    }
  }
  return { edges, warnings, positiveEdges: edges.length, antiEdges: 0 };
}

export function parseNumericTableCsv(text: string, name = 'CSV numérique', parseOptions: ParseOptions = {}): ParsedGraph {
  const options = { ...DEFAULT_PARSE_OPTIONS, ...parseOptions };
  const { rows, labels, warnings, width } = parseNumericRows(text);
  const nodeCount = rows.length;
  if (nodeCount === 0) {
    return { name, nodeCount: 0, edges: [], sourceKind: 'numeric-table', warnings, metadata: { rows: 0, columns: 0 } };
  }
  ensureNodeLimit(nodeCount, options.maxWebNodes ?? WEB_GRAPH_NODE_LIMIT, 'numeric-table');

  const vectors = standardizeRows(rows);
  const exactLimit = Math.max(200, options.maxExactNodes ?? DEFAULT_PARSE_OPTIONS.maxExactNodes);
  const built = nodeCount <= exactLimit
    ? buildExactSimilarityGraph(vectors, {
        similarityMode: options.similarityMode ?? 'cosine',
        automaticThresholds: options.automaticThresholds ?? true,
        edgeThreshold: options.edgeThreshold ?? 0.92,
        antiEdgeThreshold: options.antiEdgeThreshold ?? -0.45,
        kNearest: Math.max(1, options.kNearest ?? 8)
      })
    : buildApproximateSimilarityGraph(vectors, {
        similarityMode: options.similarityMode ?? 'cosine',
        kNearest: Math.max(1, options.kNearest ?? 8)
      });

  const thresholdInfo = built as { edgeThreshold?: number; antiEdgeThreshold?: number };

  return {
    name,
    nodeCount,
    edges: built.edges,
    labels: labels ?? Array.from({ length: nodeCount }, (_, i) => String(i)),
    sourceKind: 'numeric-table',
    warnings: [...warnings, ...built.warnings],
    metadata: {
      rows: nodeCount,
      columns: width,
      positiveEdges: built.positiveEdges,
      antiEdges: built.antiEdges,
      edgeThreshold: thresholdInfo.edgeThreshold,
      antiEdgeThreshold: thresholdInfo.antiEdgeThreshold,
      similarityMode: options.similarityMode ?? 'cosine',
      density: built.edges.length / Math.max(1, nodeCount * Math.max(1, nodeCount - 1) / 2)
    }
  };
}

export function parseGraphText(text: string, fileName = 'graphe', parseOptions: ParseOptions = {}): ParsedGraph {
  const trimmed = text.trim();
  if (/\b(graph|digraph)\b/i.test(trimmed) || /(--|->)/.test(trimmed)) {
    const dot = parseDot(trimmed, fileName, parseOptions);
    if (dot.edges.length !== 0) return dot;
  }
  const edgeList = parseEdgeListCsv(trimmed, fileName, parseOptions);
  if (edgeList && edgeList.edges.length > 0 && edgeList.nodeCount > 0) return edgeList;
  return parseNumericTableCsv(trimmed, fileName, parseOptions);
}


export function analyzeGraphText(text: string, fileName = 'graphe', parseOptions: ParseOptions = {}): ImportAnalysis {
  const limit = parseOptions.maxWebNodes ?? WEB_GRAPH_NODE_LIMIT;
  const trimmed = text.trim();
  let sourceKind: ImportAnalysis['sourceKind'] = 'unknown';
  let nodeCount = 0;
  let edgeCount = 0;
  const warnings: string[] = [];

  try {
    if (/\b(graph|digraph)\b/i.test(trimmed) || /(--|->)/.test(trimmed)) {
      sourceKind = 'dot';
      const nodeSet = new Set<string>();
      for (const rawLine of splitLines(trimmed)) {
        const line = stripComments(rawLine).replace(/;$/, '').trim();
        const match = line.match(/^"?([^"\-\>\[]+)"?\s*(--|->)\s*"?([^"\[;]+)"?/);
        if (!match) continue;
        nodeSet.add(match[1].trim().replace(/^"|"$/g, ''));
        nodeSet.add(match[3].trim().replace(/^"|"$/g, ''));
        edgeCount++;
      }
      nodeCount = nodeSet.size;
    }

    if (nodeCount === 0) {
      const edgeList = parseEdgeListCsv(trimmed, fileName, { ...parseOptions, maxWebNodes: Number.MAX_SAFE_INTEGER });
      if (edgeList && edgeList.edges.length > 0) {
        sourceKind = 'edge-list';
        nodeCount = edgeList.nodeCount;
        edgeCount = edgeList.edges.length;
      }
    }

    if (nodeCount === 0) {
      const numeric = parseNumericRows(trimmed);
      sourceKind = 'numeric-table';
      nodeCount = numeric.rows.length;
      edgeCount = 0;
      warnings.push(...numeric.warnings);
    }
  } catch (error) {
    warnings.push(error instanceof Error ? error.message : String(error));
  }

  const accepted = nodeCount > 0 && nodeCount <= limit;
  const densityEstimate = nodeCount > 1 && edgeCount > 0 ? edgeCount / (nodeCount * (nodeCount - 1) / 2) : 0;
  const estimatedMemory = `${Math.max(1, Math.ceil((nodeCount * 64 + Math.max(edgeCount, nodeCount * 4) * 32) / 1024))} Ko estimés`;
  return {
    fileName,
    sourceKind,
    nodeCount,
    edgeCount,
    accepted,
    severity: accepted ? (nodeCount > limit * 0.8 ? 'warning' : 'ok') : 'blocked',
    message: accepted
      ? `Fichier compatible : ${nodeCount.toLocaleString('fr-FR')} nœuds détectés dans la limite configurée de ${limit.toLocaleString('fr-FR')}.`
      : `Fichier trop grand : ${nodeCount.toLocaleString('fr-FR')} nœuds détectés pour une limite de ${limit.toLocaleString('fr-FR')}.`,
    advice: accepted
      ? `Tu peux importer ce fichier. Densité estimée : ${densityEstimate.toFixed(4)}.`
      : 'Utilise un échantillon, augmente la limite si ta machine le permet, ou filtre les données avant import.',
    warnings,
    estimatedMemory
  };
}

function sampleNumericTableText(text: string, mode: ImportSampleMode, limit: number): string {
  const rawLines = text.replace(/^\uFEFF/, '').split(/\r?\n/).filter((line) => line.trim() !== '');
  if (rawLines.length <= limit) return text;
  const first = rawLines[0] ?? '';
  const delimiter = detectDelimiter(first);
  const firstNumeric = first.split(delimiter).filter((v) => safeNumber(v) != null).length;
  const secondNumeric = rawLines[1]?.split(delimiter).filter((v) => safeNumber(v) != null).length ?? 0;
  const hasHeader = firstNumeric < secondNumeric && secondNumeric >= 2;
  const header = hasHeader ? [rawLines[0]] : [];
  const rows = hasHeader ? rawLines.slice(1) : rawLines;
  const capped = Math.max(1, limit - header.length);
  let selected: string[];
  if (mode === 'random') {
    selected = rows.map((line, index) => ({ line, key: Math.sin((index + 1) * 12.9898) * 43758.5453 % 1 })).sort((a, b) => a.key - b.key).slice(0, capped).map((entry) => entry.line);
  } else {
    selected = rows.slice(0, capped);
  }
  return [...header, ...selected].join('\n');
}

export function sampleParsedGraph(graph: ParsedGraph, limit: number, mode: ImportSampleMode): ParsedGraph {
  if (graph.nodeCount <= limit) return graph;
  const selected = new Set<number>();
  if (mode === 'top-degree') {
    const degree = new Array(graph.nodeCount).fill(0);
    for (const [a, b] of graph.edges) { degree[a]++; degree[b]++; }
    degree.map((d, index) => ({ d, index })).sort((a, b) => b.d - a.d).slice(0, limit).forEach((entry) => selected.add(entry.index));
  } else if (mode === 'random') {
    Array.from({ length: graph.nodeCount }, (_, index) => ({ index, key: Math.sin((index + 1) * 78.233) * 43758.5453 % 1 }))
      .sort((a, b) => a.key - b.key).slice(0, limit).forEach((entry) => selected.add(entry.index));
  } else {
    for (let i = 0; i < Math.min(limit, graph.nodeCount); i++) selected.add(i);
  }
  const remap = new Map<number, number>();
  Array.from(selected).sort((a, b) => a - b).forEach((oldId, nextId) => remap.set(oldId, nextId));
  const seen = new Set<string>();
  const edges: EdgeTuple[] = [];
  for (const [a, b, w] of graph.edges) {
    const na = remap.get(a);
    const nb = remap.get(b);
    if (na == null || nb == null) continue;
    addUndirectedEdge(edges, seen, na, nb, w);
  }
  return {
    ...graph,
    name: `${graph.name.replace(/\.[^.]+$/, '')}_sample_${mode}`,
    nodeCount: remap.size,
    edges,
    labels: graph.labels ? Array.from(remap.keys()).sort((a, b) => a - b).map((oldId) => graph.labels?.[oldId] ?? String(oldId)) : undefined,
    warnings: [...graph.warnings, `Échantillon ${mode} créé : ${remap.size.toLocaleString('fr-FR')} nœuds conservés sur ${graph.nodeCount.toLocaleString('fr-FR')}.`],
    metadata: { ...graph.metadata, positiveEdges: edges.filter((e) => e[2] >= 0).length, antiEdges: edges.filter((e) => e[2] < 0).length, density: edges.length / Math.max(1, remap.size * Math.max(1, remap.size - 1) / 2) }
  };
}

export function parseGraphTextSampled(text: string, fileName = 'graphe', parseOptions: ParseOptions = {}, mode: ImportSampleMode = 'first'): ParsedGraph {
  const limit = parseOptions.maxWebNodes ?? WEB_GRAPH_NODE_LIMIT;
  const analysis = analyzeGraphText(text, fileName, parseOptions);
  if (analysis.nodeCount <= limit) return parseGraphText(text, fileName, parseOptions);
  if (analysis.sourceKind === 'numeric-table') {
    return parseGraphText(sampleNumericTableText(text, mode, limit), fileName, parseOptions);
  }
  const parsed = parseGraphText(text, fileName, { ...parseOptions, maxWebNodes: Number.MAX_SAFE_INTEGER, maxExactNodes: Math.min(parseOptions.maxExactNodes ?? limit, limit) });
  return sampleParsedGraph(parsed, limit, mode);
}

export function createDemoGraph(nodeCount = 360, edgeCount = 820, limit = DEMO_GRAPH_NODE_LIMIT): ParsedGraph {
  nodeCount = Math.max(8, Math.min(limit, Math.floor(nodeCount)));
  edgeCount = Math.max(nodeCount - 1, Math.min(Math.floor(edgeCount), Math.floor((nodeCount * (nodeCount - 1)) / 2)));
  const edges: EdgeTuple[] = [];
  const seen = new Set<string>();
  const communityCount = Math.max(4, Math.floor(Math.sqrt(nodeCount) / 8));
  for (let i = 1; i < nodeCount; i++) {
    const target = Math.max(0, Math.floor(Math.random() * i));
    addUndirectedEdge(edges, seen, i, target, 1);
  }
  while (edges.length < edgeCount) {
    const c = Math.floor(Math.random() * communityCount);
    const start = Math.floor(c * nodeCount / communityCount);
    const end = Math.floor((c + 1) * nodeCount / communityCount);
    const a = start + Math.floor(Math.random() * Math.max(1, end - start));
    const b = Math.random() < 0.82
      ? start + Math.floor(Math.random() * Math.max(1, end - start))
      : Math.floor(Math.random() * nodeCount);
    addUndirectedEdge(edges, seen, a, b, 1);
  }
  return {
    name: 'Démo générée',
    nodeCount,
    edges,
    labels: Array.from({ length: nodeCount }, (_, i) => String(i)),
    sourceKind: 'demo',
    warnings: [`Démo générée dans la limite web de ${limit.toLocaleString('fr-FR')} nœuds.`],
    metadata: { positiveEdges: edges.length, antiEdges: 0, density: edges.length / Math.max(1, nodeCount * (nodeCount - 1) / 2) }
  };
}

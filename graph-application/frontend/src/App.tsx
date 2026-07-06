import { useCallback, useEffect, useMemo, useRef, useState } from 'react';
import type { ChangeEvent } from 'react';
import { DataPanel } from './components/DataPanel';
import { GraphCanvas, type GraphCanvasHandle } from './components/GraphCanvas';
import { StatsPanel } from './components/StatsPanel';
import { Toolbar } from './components/Toolbar';
import { HelpPanel } from './components/HelpPanel';
import { LimitDialog } from './components/LimitDialog';
import { ImportAssistantDialog } from './components/ImportAssistantDialog';
import { DEMO_GRAPH_NODE_LIMIT, GraphSizeLimitError, WEB_GRAPH_NODE_LIMIT, analyzeGraphText, createDemoGraph, parseGraphText, parseGraphTextSampled } from './engine/GraphParser';
import { DEFAULT_RENDER_OPTIONS, type GraphRenderData } from './rendering/GraphRenderer';
import type { ActionLogEntry, BuildOptions, EngineOptions, GraphFrame, GraphInsights, ImportAnalysis, ImportSampleMode, LayoutPreset, ParsedGraph, RenderOptions, WorkerInMessage, WorkerOutMessage } from './types/graph';

const defaultOptions: EngineOptions = {
  repulsion: 9000,
  attraction: 0.015,
  antiRepulsion: 2800,
  damping: 0.86,
  friction: 0.1,
  timeStep: 0.016,
  theta: 0.72,
  stepsPerFrame: 2,
  simulationRate: 60,
  repulsionMode: 'degree-weighted',
  kmeansEnabled: false,
  kmeansClusters: 12
};

const WEB_NODE_LIMIT = WEB_GRAPH_NODE_LIMIT;
const MAX_CONFIGURABLE_NODE_LIMIT = WEB_GRAPH_NODE_LIMIT;

const defaultBuildOptions: BuildOptions = {
  maxWebNodes: WEB_NODE_LIMIT,
  similarityMode: 'correlation',
  automaticThresholds: true,
  edgeThreshold: 0.92,
  antiEdgeThreshold: -0.45,
  kNearest: 8,
  maxExactNodes: WEB_NODE_LIMIT,
  communityMode: 'label-propagation',
  width: 1000,
  height: 500,
  spatialCells: 0,
  kmeansEnabled: false,
  kmeansClusters: 12
};

type UndoAction =
  | { type: 'delete'; node: number }
  | { type: 'move'; node: number; from: { x: number; y: number }; to: { x: number; y: number } };

type SavedProject = {
  version: 2;
  graph: ParsedGraph;
  options: EngineOptions;
  buildOptions: BuildOptions;
  renderOptions: RenderOptions;
  actionLog?: ActionLogEntry[];
};

type ToolMode = 'select' | 'move' | 'delete';
type MainTab = 'overview' | 'data' | 'preview' | 'help';

function frameToRenderData(frame: GraphFrame, graph: ParsedGraph | null): GraphRenderData {
  return {
    positions: frame.positions,
    colors: frame.colors,
    edges: frame.edges,
    weights: frame.weights,
    degrees: frame.degrees,
    communities: frame.communities,
    deleted: frame.deleted,
    nodeCount: frame.nodeCount,
    edgeCount: frame.edgeCount,
    labels: graph?.labels
  };
}

function downloadText(content: string, fileName: string, type: string): void {
  const blob = new Blob([content], { type });
  const url = URL.createObjectURL(blob);
  const link = document.createElement('a');
  link.href = url;
  link.download = fileName;
  document.body.appendChild(link);
  link.click();
  link.remove();
  URL.revokeObjectURL(url);
}

function projectFileName(graph: ParsedGraph | null, ext: string): string {
  const base = (graph?.name ?? 'graph').replace(/\.[^.]+$/, '').replace(/[^a-z0-9_-]+/gi, '_').replace(/^_+|_+$/g, '') || 'graph';
  return `${base}.${ext}`;
}

function formatNumber(value: number): string {
  return value.toLocaleString('fr-FR');
}

function fileInputChange(onFile: (file: File) => void) {
  return (event: ChangeEvent<HTMLInputElement>) => {
    const file = event.target.files?.[0];
    if (file) onFile(file);
    event.currentTarget.value = '';
  };
}


function computeInsights(data: GraphRenderData | null): GraphInsights | null {
  if (!data || data.nodeCount === 0) return null;
  const degree = Array.from(data.degrees);
  let maxDegree = 0;
  let maxDegreeNode = 0;
  let isolatedNodes = 0;
  for (let i = 0; i < data.nodeCount; i++) {
    if (data.deleted[i]) continue;
    const d = degree[i] ?? 0;
    if (d === 0) isolatedNodes++;
    if (d > maxDegree) { maxDegree = d; maxDegreeNode = i; }
  }
  const liveNodes = Math.max(1, data.nodeCount - Array.from(data.deleted).filter(Boolean).length);
  const density = data.edgeCount / Math.max(1, data.nodeCount * Math.max(1, data.nodeCount - 1) / 2);
  const averageDegree = data.edgeCount * 2 / liveNodes;
  const communities = new Set<number>();
  for (let i = 0; i < data.nodeCount; i++) if (!data.deleted[i]) communities.add(data.communities[i] ?? 0);

  const adjacency = Array.from({ length: data.nodeCount }, () => [] as number[]);
  for (let e = 0; e < data.edgeCount; e++) {
    const a = data.edges[2 * e];
    const b = data.edges[2 * e + 1];
    if (data.deleted[a] || data.deleted[b]) continue;
    adjacency[a].push(b);
    adjacency[b].push(a);
  }
  const visited = new Uint8Array(data.nodeCount);
  let connectedComponents = 0;
  let largestComponentSize = 0;
  for (let i = 0; i < data.nodeCount; i++) {
    if (visited[i] || data.deleted[i]) continue;
    connectedComponents++;
    let size = 0;
    const stack = [i];
    visited[i] = 1;
    while (stack.length) {
      const node = stack.pop()!;
      size++;
      for (const neighbor of adjacency[node]) {
        if (!visited[neighbor]) { visited[neighbor] = 1; stack.push(neighbor); }
      }
    }
    largestComponentSize = Math.max(largestComponentSize, size);
  }
  const interpretation = [
    density < 0.015 ? 'Graphe peu dense : les arêtes importantes ressortent bien.' : density < 0.08 ? 'Densité intermédiaire : les filtres de poids/degré seront utiles.' : 'Graphe dense : utilise le mode voisins ou communauté pour lire la structure.',
    communities.size > 1 ? `${communities.size} communautés visibles : la coloration par communauté est pertinente.` : 'Peu de communautés détectées pour l’instant.',
    maxDegree > averageDegree * 2 ? `Le nœud ${maxDegreeNode} joue un rôle de hub central.` : 'Aucun hub dominant ne ressort fortement.'
  ];
  return { density, averageDegree, maxDegree, maxDegreeNode, isolatedNodes, communityCount: communities.size, connectedComponents, largestComponentSize, interpretation };
}

function neighborsOf(data: GraphRenderData | null, node: number | null): number[] {
  if (!data || node == null || node < 0) return [];
  const result: number[] = [];
  for (let e = 0; e < data.edgeCount; e++) {
    const a = data.edges[2 * e];
    const b = data.edges[2 * e + 1];
    if (a === node) result.push(b);
    else if (b === node) result.push(a);
  }
  return Array.from(new Set(result)).sort((a, b) => (data.degrees[b] ?? 0) - (data.degrees[a] ?? 0));
}

export default function App() {
  const workerRef = useRef<Worker | null>(null);
  const canvasRef = useRef<GraphCanvasHandle | null>(null);
  const graphRef = useRef<ParsedGraph | null>(null);
  const dataRef = useRef<GraphRenderData | null>(null);
  const sourceRef = useRef<{ name: string; text: string } | null>(null);
  const graphFileInputRef = useRef<HTMLInputElement | null>(null);
  const projectFileInputRef = useRef<HTMLInputElement | null>(null);

  const [options, setOptions] = useState<EngineOptions>(defaultOptions);
  const [buildOptions, setBuildOptions] = useState<BuildOptions>(defaultBuildOptions);
  const [renderOptions, setRenderOptions] = useState<RenderOptions>({ ...DEFAULT_RENDER_OPTIONS });
  const [running, setRunning] = useState(false);
  const [graph, setGraph] = useState<ParsedGraph | null>(null);
  const [renderData, setRenderData] = useState<GraphRenderData | null>(null);
  const [stats, setStats] = useState({ nodeCount: 0, edgeCount: 0, liveNodeCount: 0, iteration: 0 });
  const [selectedNode, setSelectedNode] = useState<number | null>(null);
  const [error, setError] = useState<string | null>(null);
  const [ready, setReady] = useState(false);
  const [activeTab, setActiveTab] = useState<MainTab>('overview');
  const [toolMode, setToolMode] = useState<ToolMode>('select');
  const [showStats, setShowStats] = useState(true);
  const [leftCollapsed, setLeftCollapsed] = useState(false);
  const [rightCollapsed, setRightCollapsed] = useState(false);
  const [undoStack, setUndoStack] = useState<UndoAction[]>([]);
  const [redoStack, setRedoStack] = useState<UndoAction[]>([]);
  const [limitDialog, setLimitDialog] = useState<{ title: string; message: string; advice: string; details?: string } | null>(null);
  const [pendingImport, setPendingImport] = useState<{ fileName: string; text: string; analysis: ImportAnalysis } | null>(null);
  const [searchQuery, setSearchQuery] = useState('');
  const [zoomPercent, setZoomPercent] = useState(100);
  const [actionLog, setActionLog] = useState<ActionLogEntry[]>([]);
  const [presentationMode, setPresentationMode] = useState(false);

  const showLimitError = useCallback((err: unknown, fileName?: string) => {
    if (err instanceof GraphSizeLimitError) {
      setLimitDialog({
        title: 'Limite web atteinte',
        message: err.message,
        advice: `Cette limite est paramétrable jusqu’à ${MAX_CONFIGURABLE_NODE_LIMIT.toLocaleString('fr-FR')} nœuds. Tu peux importer un fichier plus petit, augmenter la limite si ta machine suit, ou utiliser l’échantillonnage intelligent.`,
        details: fileName ? `Fichier analysé : ${fileName}` : undefined
      });
      setError(err.message);
      return true;
    }
    return false;
  }, []);

  const post = useCallback((message: WorkerInMessage) => {
    workerRef.current?.postMessage(message);
  }, []);

  const logAction = useCallback((label: string, detail?: string) => {
    const time = new Date().toLocaleTimeString('fr-FR', { hour: '2-digit', minute: '2-digit', second: '2-digit' });
    setActionLog((entries) => [{ time, label, detail }, ...entries].slice(0, 24));
  }, []);

  useEffect(() => {
    const worker = new Worker(new URL('./engine/graph.worker.ts', import.meta.url), { type: 'module' });
    workerRef.current = worker;
    worker.onerror = (event) => {
      setError(`Worker indisponible : ${event.message || 'erreur inconnue'}`);
      setReady(true);
      setRunning(false);
    };
    worker.onmessageerror = () => {
      setError('Message Worker illisible. Recharge la page ou relance ./run_app.sh fresh.');
      setReady(true);
      setRunning(false);
    };
    worker.onmessage = (event: MessageEvent<WorkerOutMessage>) => {
      const message = event.data;
      if (message.type === 'ready') {
        setReady(true);
      } else if (message.type === 'error') {
        setError(message.message);
        setReady(true);
        setRunning(false);
      } else if (message.type === 'loaded') {
        setStats({ nodeCount: message.nodeCount, edgeCount: message.edgeCount, liveNodeCount: message.liveNodeCount, iteration: message.iteration });
      } else if (message.type === 'frame') {
        const frame = frameToRenderData(message, graphRef.current);
        dataRef.current = frame;
        setRenderData(frame);
        setStats({ nodeCount: message.nodeCount, edgeCount: message.edgeCount, liveNodeCount: message.liveNodeCount, iteration: message.iteration });
      }
    };
    return () => worker.terminate();
  }, []);

  const loadParsedGraph = useCallback((nextGraph: ParsedGraph, nextOptions = options, nextBuildOptions = buildOptions) => {
    if (nextGraph.nodeCount <= 0 || nextGraph.edges.length <= 0) {
      setError('Le fichier ne contient pas assez de données pour construire un graphe.');
      return;
    }
    setError(null);
    setGraph(nextGraph);
    graphRef.current = nextGraph;
    setSelectedNode(null);
    setUndoStack([]);
    setRedoStack([]);
    setActiveTab('overview');
    logAction('Graphe chargé', `${nextGraph.name} · ${nextGraph.nodeCount.toLocaleString('fr-FR')} nœuds · ${nextGraph.edges.length.toLocaleString('fr-FR')} arêtes`);
    post({ type: 'loadGraph', graph: nextGraph, options: nextOptions, buildOptions: nextBuildOptions });
  }, [buildOptions, logAction, options, post]);

  const handleFile = useCallback(async (file: File) => {
    try {
      const text = await file.text();
      const analysis = analyzeGraphText(text, file.name, buildOptions);
      sourceRef.current = { name: file.name, text };
      setPendingImport({ fileName: file.name, text, analysis });
      logAction('Fichier analysé', `${file.name} · ${analysis.nodeCount.toLocaleString('fr-FR')} nœuds détectés`);
    } catch (err) {
      if (!showLimitError(err, file.name)) setError(err instanceof Error ? err.message : String(err));
    }
  }, [buildOptions, logAction, showLimitError]);

  const confirmPendingImport = useCallback(() => {
    if (!pendingImport) return;
    try {
      const parsed = parseGraphText(pendingImport.text, pendingImport.fileName, buildOptions);
      setPendingImport(null);
      loadParsedGraph(parsed);
    } catch (err) {
      if (!showLimitError(err, pendingImport.fileName)) setError(err instanceof Error ? err.message : String(err));
    }
  }, [buildOptions, loadParsedGraph, pendingImport, showLimitError]);

  const samplePendingImport = useCallback((mode: ImportSampleMode) => {
    if (!pendingImport) return;
    try {
      const parsed = parseGraphTextSampled(pendingImport.text, pendingImport.fileName, buildOptions, mode);
      setPendingImport(null);
      loadParsedGraph(parsed);
      logAction('Échantillon importé', `${mode} · ${parsed.nodeCount.toLocaleString('fr-FR')} nœuds`);
    } catch (err) {
      if (!showLimitError(err, pendingImport.fileName)) setError(err instanceof Error ? err.message : String(err));
    }
  }, [buildOptions, loadParsedGraph, logAction, pendingImport, showLimitError]);

  const handleRebuild = useCallback(() => {
    const source = sourceRef.current;
    if (!source) {
      setError('Aucun fichier source CSV/DOT à reconstruire. Importe un fichier ou ouvre un projet.');
      return;
    }
    try {
      const parsed = parseGraphText(source.text, source.name, buildOptions);
      loadParsedGraph(parsed);
    } catch (err) {
      if (!showLimitError(err, source.name)) setError(err instanceof Error ? err.message : String(err));
    }
  }, [buildOptions, loadParsedGraph, showLimitError]);

  const handleDemo = useCallback((requestedNodeCount = 360) => {
    const capped = Math.max(40, Math.min(DEMO_GRAPH_NODE_LIMIT, Math.floor(requestedNodeCount)));
    const demo = createDemoGraph(capped, Math.floor(capped * 2.4), DEMO_GRAPH_NODE_LIMIT);
    sourceRef.current = null;
    setError(null);
    loadParsedGraph(demo);
    logAction('Démo générée', `${capped.toLocaleString('fr-FR')} nœuds · plafond démo ${DEMO_GRAPH_NODE_LIMIT.toLocaleString('fr-FR')}`);
  }, [loadParsedGraph, logAction]);

  const handleNewProject = useCallback(() => {
    setRunning(false);
    post({ type: 'setRunning', running: false });
    sourceRef.current = null;
    graphRef.current = null;
    dataRef.current = null;
    setGraph(null);
    setRenderData(null);
    setSelectedNode(null);
    setStats({ nodeCount: 0, edgeCount: 0, liveNodeCount: 0, iteration: 0 });
    setError(null);
    setUndoStack([]);
    setRedoStack([]);
    setActionLog([]);
    setActiveTab('overview');
    logAction('Nouveau projet');
  }, [logAction, post]);

  const handleOptionsChange = useCallback((next: EngineOptions) => {
    setOptions(next);
    post({ type: 'setOptions', options: next });
  }, [post]);

  const handleBuildOptionsChange = useCallback((next: BuildOptions) => {
    const normalized = {
      ...next,
      maxWebNodes: Math.max(100, Math.min(MAX_CONFIGURABLE_NODE_LIMIT, Math.round(next.maxWebNodes))),
      maxExactNodes: Math.max(100, Math.min(Math.round(next.maxWebNodes), Math.round(next.maxExactNodes)))
    };
    setBuildOptions(normalized);
    const syncedOptions = { ...options, kmeansEnabled: normalized.kmeansEnabled, kmeansClusters: normalized.kmeansClusters };
    setOptions(syncedOptions);
    post({ type: 'setOptions', options: syncedOptions });
  }, [options, post]);

  const toggleRunning = useCallback(() => {
    const next = !running;
    setRunning(next);
    post({ type: 'setRunning', running: next });
  }, [post, running]);

  const requestStep = useCallback(() => post({ type: 'requestFrame' }), [post]);
  const resetLayout = useCallback(() => post({ type: 'resetLayout' }), [post]);
  const moveNode = useCallback((node: number, x: number, y: number) => post({ type: 'setNodePosition', node, x, y }), [post]);

  const pushUndo = useCallback((action: UndoAction) => {
    setUndoStack((stack) => [...stack.slice(-99), action]);
    setRedoStack([]);
  }, []);

  const moveComplete = useCallback((node: number, from: { x: number; y: number }, to: { x: number; y: number }) => {
    pushUndo({ type: 'move', node, from, to });
  }, [pushUndo]);

  const deleteNode = useCallback((node: number) => {
    const data = dataRef.current;
    if (data?.deleted[node]) return;
    pushUndo({ type: 'delete', node });
    post({ type: 'deleteNode', node });
  }, [post, pushUndo]);

  const applyAction = useCallback((action: UndoAction, direction: 'undo' | 'redo') => {
    if (action.type === 'delete') {
      post({ type: direction === 'undo' ? 'restoreNode' : 'deleteNode', node: action.node });
    } else {
      const target = direction === 'undo' ? action.from : action.to;
      post({ type: 'setNodePosition', node: action.node, x: target.x, y: target.y });
    }
  }, [post]);

  const undo = useCallback(() => {
    setUndoStack((stack) => {
      const action = stack[stack.length - 1];
      if (!action) return stack;
      applyAction(action, 'undo');
      setRedoStack((redo) => [...redo, action]);
      return stack.slice(0, -1);
    });
  }, [applyAction]);

  const redo = useCallback(() => {
    setRedoStack((stack) => {
      const action = stack[stack.length - 1];
      if (!action) return stack;
      applyAction(action, 'redo');
      setUndoStack((undoStackCurrent) => [...undoStackCurrent, action]);
      return stack.slice(0, -1);
    });
  }, [applyAction]);

  const runCommunities = useCallback(() => post({ type: 'runCommunities', iterations: 10 }), [post]);
  const runKMeans = useCallback(() => post({ type: 'runKMeans', clusters: buildOptions.kmeansClusters, iterations: 5 }), [buildOptions.kmeansClusters, post]);

  const exportProject = useCallback(() => {
    if (!graph) return;
    const saved: SavedProject = { version: 2, graph, options, buildOptions, renderOptions, actionLog };
    downloadText(JSON.stringify(saved, null, 2), projectFileName(graph, 'graph-project.json'), 'application/json;charset=utf-8');
  }, [actionLog, buildOptions, graph, options, renderOptions]);

  const loadProject = useCallback(async (file: File) => {
    try {
      const parsed = JSON.parse(await file.text()) as Partial<SavedProject>;
      if (!parsed.graph || !Array.isArray(parsed.graph.edges)) throw new Error('Projet JSON invalide.');
      const nextOptions = { ...defaultOptions, ...parsed.options };
      const nextBuildOptions = { ...defaultBuildOptions, ...parsed.buildOptions, maxWebNodes: Math.min(MAX_CONFIGURABLE_NODE_LIMIT, parsed.buildOptions?.maxWebNodes ?? defaultBuildOptions.maxWebNodes) };
      if ((parsed.graph.nodeCount ?? 0) > nextBuildOptions.maxWebNodes) throw new GraphSizeLimitError(parsed.graph.nodeCount ?? 0, nextBuildOptions.maxWebNodes, 'project');
      const nextRenderOptions = { ...DEFAULT_RENDER_OPTIONS, ...parsed.renderOptions };
      setOptions(nextOptions);
      setBuildOptions(nextBuildOptions);
      setRenderOptions(nextRenderOptions);
      setActionLog(parsed.actionLog ?? []);
      sourceRef.current = null;
      loadParsedGraph(parsed.graph as ParsedGraph, nextOptions, nextBuildOptions);
    } catch (err) {
      if (!showLimitError(err, file.name)) setError(err instanceof Error ? err.message : String(err));
    }
  }, [loadParsedGraph, showLimitError]);

  const status = useMemo(() => {
    if (!ready) return 'Chargement du moteur WASM…';
    if (!graph) return 'Prêt';
    return running ? 'Simulation en cours' : 'Prêt';
  }, [graph, ready, running]);

  useEffect(() => {
    const loadInitial = async () => {
      try {
        const response = await fetch('/samples/iris.csv');
        if (!response.ok) return;
        const text = await response.text();
        sourceRef.current = { name: 'iris.csv', text };
        const parsed = parseGraphText(text, 'iris.csv', defaultBuildOptions);
        loadParsedGraph(parsed, defaultOptions, defaultBuildOptions);
      } catch {
        // Le projet reste utilisable sans échantillon.
      }
    };
    if (ready && !graph) void loadInitial();
  }, [graph, loadParsedGraph, ready]);

  const derivedStats = useMemo(() => {
    const data = renderData;
    if (!data) return { hiddenNodes: 0, displayedEdges: 0, hiddenEdges: 0, deletedNodes: 0 };
    let hiddenNodes = 0;
    let deletedNodes = 0;
    for (let i = 0; i < data.nodeCount; i++) {
      if (data.deleted[i]) deletedNodes++;
      else if (data.degrees[i] < renderOptions.minDegree) hiddenNodes++;
    }
    let displayedEdges = 0;
    for (let e = 0; e < data.edgeCount; e++) {
      const a = data.edges[2 * e];
      const b = data.edges[2 * e + 1];
      const hidden = data.deleted[a] || data.deleted[b] || data.degrees[a] < renderOptions.minDegree || data.degrees[b] < renderOptions.minDegree || Math.abs(data.weights[e] ?? 1) < renderOptions.minEdgeWeight;
      if (hidden) continue;
      displayedEdges++;
    }
    return { hiddenNodes, displayedEdges, hiddenEdges: Math.max(0, data.edgeCount - displayedEdges), deletedNodes };
  }, [renderData, renderOptions.minDegree, renderOptions.minEdgeWeight]);

  const graphInsights = useMemo(() => computeInsights(renderData), [renderData]);

  const selectedDetails = useMemo(() => {
    const data = renderData;
    const id = selectedNode;
    if (!data || id == null || id < 0 || id >= data.nodeCount) return null;
    const neighbors = neighborsOf(data, id);
    return {
      id,
      label: data.labels?.[id] ?? String(id),
      x: data.positions[2 * id] ?? 0,
      y: data.positions[2 * id + 1] ?? 0,
      degree: data.degrees[id] ?? 0,
      community: data.communities[id] ?? 0,
      deleted: Boolean(data.deleted[id]),
      neighbors,
      topNeighbors: neighbors.slice(0, 12).map((neighbor) => ({ id: neighbor, label: data.labels?.[neighbor] ?? String(neighbor), degree: data.degrees[neighbor] ?? 0 }))
    };
  }, [renderData, selectedNode]);

  const handleSelectNode = useCallback((node: number | null) => {
    setSelectedNode(node);
    setRenderOptions((current) => ({ ...current, focusNode: node ?? -1 }));
  }, []);

  const focusSelected = useCallback((mode: RenderOptions['focusMode']) => {
    if (selectedNode == null) return;
    setRenderOptions((current) => ({ ...current, focusNode: selectedNode, focusMode: mode }));
    canvasRef.current?.focusNode(selectedNode);
    logAction('Focus sélection', `Nœud ${selectedNode} · mode ${mode}`);
  }, [logAction, selectedNode]);

  const clearFocus = useCallback(() => {
    setRenderOptions((current) => ({ ...current, focusNode: -1, focusMode: 'none', communityFilter: -1 }));
    setSelectedNode(null);
    logAction('Focus réinitialisé');
  }, [logAction]);

  const copySelectedDetails = useCallback(() => {
    if (!selectedDetails) return;
    const text = `Nœud ${selectedDetails.id}\nLabel: ${selectedDetails.label}\nDegré: ${selectedDetails.degree}\nCommunauté: ${selectedDetails.community}\nVoisins: ${selectedDetails.neighbors.join(', ')}`;
    void navigator.clipboard?.writeText(text);
    logAction('Informations copiées', `Nœud ${selectedDetails.id}`);
  }, [logAction, selectedDetails]);

  const applyLayoutPreset = useCallback((layout: LayoutPreset) => {
    const data = dataRef.current;
    if (!data) return;
    if (layout === 'force') {
      post({ type: 'resetLayout' });
      logAction('Layout appliqué', 'ForceAtlas / reset moteur');
      return;
    }
    const n = data.nodeCount;
    const radius = Math.max(220, Math.sqrt(n) * 35);
    const communityCenters = new Map<number, { x: number; y: number; count: number; index: number }>();
    const communities = Array.from(new Set(Array.from(data.communities))).sort((a, b) => a - b);
    communities.forEach((community, index) => {
      const angle = index / Math.max(1, communities.length) * Math.PI * 2;
      communityCenters.set(community, { x: Math.cos(angle) * radius * 0.75, y: Math.sin(angle) * radius * 0.75, count: 0, index });
    });
    const selected = selectedNode ?? 0;
    for (let i = 0; i < n; i++) {
      let x = data.positions[2 * i];
      let y = data.positions[2 * i + 1];
      if (layout === 'circle') {
        const angle = i / Math.max(1, n) * Math.PI * 2;
        x = Math.cos(angle) * radius;
        y = Math.sin(angle) * radius;
      } else if (layout === 'grid') {
        const cols = Math.ceil(Math.sqrt(n));
        const gap = 38;
        x = (i % cols - cols / 2) * gap;
        y = (Math.floor(i / cols) - Math.ceil(n / cols) / 2) * gap;
      } else if (layout === 'communities') {
        const center = communityCenters.get(data.communities[i]) ?? { x: 0, y: 0, count: 0, index: 0 };
        const local = center.count++;
        const angle = local * 2.399963;
        const r = 18 + Math.sqrt(local) * 17;
        x = center.x + Math.cos(angle) * r;
        y = center.y + Math.sin(angle) * r;
      } else if (layout === 'radial') {
        const neighbors = neighborsOf(data, selected);
        if (i === selected) { x = 0; y = 0; }
        else {
          const level = neighbors.includes(i) ? 1 : 2;
          const angle = i / Math.max(1, n) * Math.PI * 2;
          x = Math.cos(angle) * radius * (level === 1 ? 0.35 : 0.9);
          y = Math.sin(angle) * radius * (level === 1 ? 0.35 : 0.9);
        }
      }
      post({ type: 'setNodePosition', node: i, x, y });
    }
    window.setTimeout(() => canvasRef.current?.fitView(), 120);
    logAction('Layout appliqué', layout);
  }, [logAction, post, selectedNode]);

  const selectNodeFromSearch = useCallback(() => {
    const query = searchQuery.trim().toLowerCase();
    const data = dataRef.current;
    if (!query || !data) return;
    const numeric = Number(query);
    let found = Number.isInteger(numeric) && numeric >= 0 && numeric < data.nodeCount ? numeric : -1;
    if (found < 0 && data.labels) {
      found = data.labels.findIndex((label) => label.toLowerCase().includes(query));
    }
    if (found >= 0) {
      handleSelectNode(found);
      setActiveTab('overview');
      canvasRef.current?.focusNode(found);
      setError(null);
    } else {
      setError(`Aucun nœud trouvé pour « ${searchQuery} ». Essaie un identifiant numérique ou une partie du label.`);
    }
  }, [handleSelectNode, searchQuery]);

  const handleZoomPercentChange = useCallback((nextZoomPercent: number) => {
    const normalized = Math.max(30, Math.min(450, Math.round(nextZoomPercent)));
    setZoomPercent(normalized);
    canvasRef.current?.setZoomPercent(normalized);
  }, []);

  const applyClarityPreset = useCallback(() => {
    setRenderOptions((current) => ({
      ...current,
      backgroundColor: '#030814',
      edgeStyle: 'simple',
      edgeColor: '#eaf7ff',
      edgeLineWidth: 1.45,
      edgeOpacity: 0.72,
      edgeGlow: 0,
      edgeFlow: 0,
      edgeSoftness: 0.45,
      edgeTaper: false,
      hdrEnabled: false,
      hdrExposure: 1,
      bloomStrength: 0,
      nodeShape: 'sphere',
      nodeRimStrength: 0.42,
      nodeSpecularStrength: 0.54,
      nodeInnerGlow: 0.12,
      nodeBaseSize: 10.5,
      degreeFactor: 0.95,
      showWorldGrid: true,
      renderMode: 'flat'
    }));
    window.setTimeout(() => canvasRef.current?.fitView(), 80);
  }, []);

  const applyReadingPreset = useCallback(() => {
    setRenderOptions((current) => ({
      ...current,
      minDegree: Math.max(current.minDegree, 1),
      minEdgeWeight: 0,
      showLabels: true,
      edgeLineWidth: 1.2,
      edgeOpacity: 0.55,
      renderMode: 'flat',
      rotationX: 0,
      rotationY: 0,
      rotationZ: 0
    }));
    window.setTimeout(() => canvasRef.current?.fitView(), 80);
  }, []);

  const optimizeGraphNow = useCallback(() => {
    runCommunities();
    runKMeans();
    applyClarityPreset();
  }, [applyClarityPreset, runCommunities, runKMeans]);

  const toggleFullscreen = useCallback(() => {
    if (document.fullscreenElement) void document.exitFullscreen();
    else void document.documentElement.requestFullscreen?.();
  }, []);

  useEffect(() => {
    const handler = (event: KeyboardEvent) => {
      const key = event.key.toLowerCase();
      if ((event.ctrlKey || event.metaKey) && key === 'o') {
        event.preventDefault();
        graphFileInputRef.current?.click();
      } else if ((event.ctrlKey || event.metaKey) && key === 's') {
        event.preventDefault();
        exportProject();
      } else if ((event.ctrlKey || event.metaKey) && key === 'z') {
        event.preventDefault();
        undo();
      } else if ((event.ctrlKey || event.metaKey) && key === 'y') {
        event.preventDefault();
        redo();
      } else if (event.key === 'F11') {
        event.preventDefault();
        toggleFullscreen();
      }
    };
    window.addEventListener('keydown', handler);
    return () => window.removeEventListener('keydown', handler);
  }, [exportProject, redo, toggleFullscreen, undo]);

  return (
    <main className={`desktopApp ${leftCollapsed || presentationMode ? 'leftCollapsed' : ''} ${rightCollapsed || !showStats || presentationMode ? 'rightCollapsed' : ''} ${presentationMode ? 'presentationMode' : ''}`}>
      <input ref={graphFileInputRef} className="hiddenFileInput" type="file" accept=".csv,.dot,.txt,text/csv,text/plain" onChange={fileInputChange(handleFile)} />
      <input ref={projectFileInputRef} className="hiddenFileInput" type="file" accept=".json,application/json" onChange={fileInputChange(loadProject)} />

      <div className="desktopTitlebar">
        <div className="appMark">MG</div>
        <div className="titlebarText">
          <strong>MonGraphe Web</strong>
          <span>{graph?.name ?? 'Espace de travail'}</span>
        </div>
        <div className="titlebarPills" aria-label="État de l'application">
          <span className="statusPill ok">Cloudflare</span>
          <span className="statusPill">{renderOptions.renderMode === 'space3d' ? '3D orbitale' : '2D simple'}</span>
        </div>
        <div className="windowDots" aria-hidden="true"><span /> <span /> <span /></div>
      </div>

      <nav className="desktopMenubar" aria-label="Menus de l'application">
        <div className="menuItem" tabIndex={0}>Fichier
          <div className="submenu">
            <button type="button" onClick={handleNewProject}>Nouveau projet</button>
            <button type="button" onClick={() => graphFileInputRef.current?.click()}>Ouvrir… <kbd>Ctrl+O</kbd></button>
            <button type="button" onClick={() => projectFileInputRef.current?.click()}>Ouvrir projet JSON…</button>
            <button type="button" onClick={exportProject} disabled={!graph}>Enregistrer le projet… <kbd>Ctrl+S</kbd></button>
            <span className="menuSeparator" />
            <button type="button" onClick={() => canvasRef.current?.exportPng(projectFileName(graph, 'png'))} disabled={!renderData}>Exporter en PNG…</button>
            <button type="button" onClick={() => canvasRef.current?.exportSvg(projectFileName(graph, 'svg'))} disabled={!renderData}>Exporter en SVG…</button>
            <span className="menuSeparator" />
            <button type="button" onClick={handleNewProject}>Fermer l'espace de travail</button>
          </div>
        </div>
        <div className="menuItem" tabIndex={0}>Édition
          <div className="submenu">
            <button type="button" onClick={undo} disabled={undoStack.length === 0}>Annuler <kbd>Ctrl+Z</kbd></button>
            <button type="button" onClick={redo} disabled={redoStack.length === 0}>Rétablir <kbd>Ctrl+Y</kbd></button>
            <span className="menuSeparator" />
            <button type="button" onClick={resetLayout} disabled={!renderData}>Réinitialiser le layout</button>
            <button type="button" onClick={runCommunities} disabled={!renderData}>Détecter les communautés</button>
            <button type="button" onClick={runKMeans} disabled={!renderData}>Appliquer K-Means</button>
          </div>
        </div>
        <div className="menuItem" tabIndex={0}>Affichage
          <div className="submenu">
            <button type="button" onClick={toggleFullscreen}>Plein écran <kbd>F11</kbd></button>
            <button type="button" onClick={() => setShowStats((value) => !value)}>{showStats ? 'Masquer' : 'Afficher'} les statistiques</button>
            <button type="button" onClick={() => setLeftCollapsed((value) => !value)}>{leftCollapsed ? 'Afficher' : 'Masquer'} les paramètres</button>
            <button type="button" onClick={() => setRightCollapsed((value) => !value)}>{rightCollapsed ? 'Afficher' : 'Masquer'} le panneau droit</button>
            <span className="menuSeparator" />
            <button type="button" onClick={() => canvasRef.current?.fitView()} disabled={!renderData}>Centrer le graphe</button>
            <button type="button" onClick={() => canvasRef.current?.resetCamera()} disabled={!renderData}>Réinitialiser la caméra</button>
          </div>
        </div>
        <div className="menuItem" tabIndex={0}>Outils
          <div className="submenu">
            <button type="button" onClick={() => setToolMode('select')}>Select</button>
            <button type="button" onClick={() => setToolMode('move')}>Move</button>
            <button type="button" onClick={() => setToolMode('delete')}>Delete</button>
            <span className="menuSeparator" />
            <button type="button" onClick={handleRebuild}>Reconstruire le graphe</button>
            <button type="button" onClick={() => handleDemo()}>Générer une démo</button>
          </div>
        </div>
        <div className="menuItem" tabIndex={0}>Aide
          <div className="submenu wide">
            <p><strong>Raccourcis</strong></p>
            <p>Ctrl+O : ouvrir un CSV/DOT · Ctrl+S : sauvegarder · F11 : plein écran.</p>
            <p>Canvas : molette pour zoomer, glisser le fond pour déplacer, glisser un nœud pour le fixer, double-clic pour supprimer.</p>
          </div>
        </div>
      </nav>

      <div className="quickToolbar">
        <span className="toolLabel">Outils:</span>
        <button type="button" className={toolMode === 'select' ? 'active' : ''} onClick={() => setToolMode('select')}>Select</button>
        <button type="button" className={toolMode === 'move' ? 'active' : ''} onClick={() => setToolMode('move')}>Move</button>
        <button type="button" className={toolMode === 'delete' ? 'active' : ''} onClick={() => setToolMode('delete')}>Delete</button>
        <span className="toolbarDivider" />
        <button type="button" onClick={() => canvasRef.current?.zoomIn()} disabled={!renderData}>+</button>
        <button type="button" onClick={() => canvasRef.current?.zoomOut()} disabled={!renderData}>−</button>
        <button type="button" onClick={() => canvasRef.current?.fitView()} disabled={!renderData}>Fit</button>
        <span className="toolbarDivider" />
        <button type="button" className="playButton" onClick={toggleRunning} disabled={!renderData}>{running ? 'Ⅱ' : '▶'}</button>
        <button type="button" onClick={requestStep} disabled={!renderData}>⟳</button>
        <span className="toolbarDivider" />
        <button type="button" onClick={optimizeGraphNow} disabled={!renderData}>Optimiser</button>
        <button type="button" onClick={applyClarityPreset}>Clarté dark</button>
        <button type="button" onClick={applyReadingPreset} disabled={!renderData}>Lecture</button>
        <label className="quickSearch">
          <span>Rechercher</span>
          <input
            value={searchQuery}
            placeholder="id ou label"
            onChange={(event) => setSearchQuery(event.target.value)}
            onKeyDown={(event) => { if (event.key === 'Enter') selectNodeFromSearch(); }}
          />
          <button type="button" onClick={selectNodeFromSearch} disabled={!renderData}>Focus</button>
        </label>
        <span className="toolbarSpacer" />
        <span className="workspaceMeta">{graph ? `${graph.name} · ${formatNumber(stats.nodeCount)} nœuds · ${formatNumber(stats.edgeCount)} arêtes` : `Aucun graphe chargé · limite web ${buildOptions.maxWebNodes} nœuds`}</span>
      </div>

      <div className="desktopBody">
        {!leftCollapsed && (
          <Toolbar
            running={running}
            options={options}
            buildOptions={buildOptions}
            renderOptions={renderOptions}
            canUndo={undoStack.length > 0}
            canRedo={redoStack.length > 0}
            onToggleRunning={toggleRunning}
            onOptionsChange={handleOptionsChange}
            onBuildOptionsChange={handleBuildOptionsChange}
            onRenderOptionsChange={setRenderOptions}
            onLoadFile={handleFile}
            onLoadProject={loadProject}
            onDemo={handleDemo}
            onStep={requestStep}
            onReset={resetLayout}
            onRebuild={handleRebuild}
            onRunCommunities={runCommunities}
            onRunKMeans={runKMeans}
            onUndo={undo}
            onRedo={redo}
            onExportProject={exportProject}
            onExportPng={() => canvasRef.current?.exportPng(projectFileName(graph, 'png'))}
            onExportSvg={() => canvasRef.current?.exportSvg(projectFileName(graph, 'svg'))}
            onFitView={() => canvasRef.current?.fitView()}
            onResetCamera={() => canvasRef.current?.resetCamera()}
            onZoomIn={() => canvasRef.current?.zoomIn()}
            onZoomOut={() => canvasRef.current?.zoomOut()}
            zoomPercent={zoomPercent}
            onZoomPercentChange={handleZoomPercentChange}
            onApplyLayout={applyLayoutPreset}
            onPresentationMode={() => setPresentationMode((value) => !value)}
            maxWebNodeHardLimit={MAX_CONFIGURABLE_NODE_LIMIT}
          />
        )}

        <section className="centralWorkspace">
          <nav className="mainTabs" aria-label="Vues">
            <button type="button" className={activeTab === 'overview' ? 'active' : ''} onClick={() => setActiveTab('overview')}>Overview</button>
            <button type="button" className={activeTab === 'data' ? 'active' : ''} onClick={() => setActiveTab('data')}>Data</button>
            <button type="button" className={activeTab === 'preview' ? 'active' : ''} onClick={() => setActiveTab('preview')}>Preview</button>
            <button type="button" className={activeTab === 'help' ? 'active' : ''} onClick={() => setActiveTab('help')}>Help / Doc</button>
          </nav>

          <div className="viewSurface">
            {activeTab === 'data' ? (
              <DataPanel data={renderData} />
            ) : activeTab === 'help' ? (
              <HelpPanel nodeLimit={buildOptions.maxWebNodes} onDemo={handleDemo} onOpenFile={() => graphFileInputRef.current?.click()} />
            ) : (
              <>
                {activeTab === 'preview' && <p className="previewNotice">Mode prévisualisation : tailles, filtres, couleur, épaisseur et arêtes courbes sont appliqués en direct. L’export SVG reprend ces paramètres.</p>}
                <GraphCanvas
                  ref={canvasRef}
                  data={renderData}
                  running={running}
                  selectedNode={selectedNode}
                  renderOptions={renderOptions}
                  onSelectNode={handleSelectNode}
                  onMoveNode={moveNode}
                  onMoveComplete={moveComplete}
                  onDeleteNode={deleteNode}
                  onZoomChange={setZoomPercent}
                  onRenderOptionsChange={setRenderOptions}
                />
              </>
            )}
          </div>
        </section>

        {showStats && !rightCollapsed && (
          <StatsPanel
            graph={graph}
            nodeCount={stats.nodeCount}
            edgeCount={stats.edgeCount}
            liveNodeCount={stats.liveNodeCount}
            iteration={stats.iteration}
            selectedNode={selectedNode}
            selectedDetails={selectedDetails}
            hiddenNodes={derivedStats.hiddenNodes}
            displayedEdges={derivedStats.displayedEdges}
            hiddenEdges={derivedStats.hiddenEdges}
            deletedNodes={derivedStats.deletedNodes}
            insights={graphInsights}
            actionLog={actionLog}
            error={error}
            onFocusSelected={focusSelected}
            onClearFocus={clearFocus}
            onCopySelected={copySelectedDetails}
            onCenterSelected={() => { if (selectedNode != null) canvasRef.current?.focusNode(selectedNode); }}
          />
        )}
      </div>


      {pendingImport && (
        <ImportAssistantDialog
          analysis={pendingImport.analysis}
          limit={buildOptions.maxWebNodes}
          maxLimit={MAX_CONFIGURABLE_NODE_LIMIT}
          onChangeLimit={(maxWebNodes) => {
            const nextBuildOptions = { ...buildOptions, maxWebNodes, maxExactNodes: Math.min(maxWebNodes, buildOptions.maxExactNodes) };
            setBuildOptions(nextBuildOptions);
            setPendingImport({ ...pendingImport, analysis: analyzeGraphText(pendingImport.text, pendingImport.fileName, nextBuildOptions) });
          }}
          onImport={confirmPendingImport}
          onSample={samplePendingImport}
          onCancel={() => setPendingImport(null)}
          onImportAnother={() => { setPendingImport(null); graphFileInputRef.current?.click(); }}
        />
      )}

      {limitDialog && (
        <LimitDialog
          title={limitDialog.title}
          message={limitDialog.message}
          advice={limitDialog.advice}
          details={limitDialog.details}
          onClose={() => setLimitDialog(null)}
          onImportAnother={() => { setLimitDialog(null); graphFileInputRef.current?.click(); }}
        />
      )}

      <nav className="mobileCommandBar" aria-label="Navigation rapide mobile">
        <button type="button" onClick={() => setLeftCollapsed((value) => !value)}>{leftCollapsed ? 'Paramètres' : 'Masquer'}</button>
        <button type="button" className={activeTab === 'overview' ? 'active' : ''} onClick={() => setActiveTab('overview')}>Graphe</button>
        <button type="button" onClick={() => { setShowStats(true); setRightCollapsed((value) => !value); }}>{rightCollapsed || !showStats ? 'Analyse' : 'Masquer'}</button>
        <button type="button" className={activeTab === 'help' ? 'active' : ''} onClick={() => setActiveTab('help')}>Guide</button>
      </nav>

      <footer className="statusbar">
        <strong>Statut:</strong>
        <span>{status}</span>
        {error && <span className="statusError">{error}</span>}
        <span className="statusRight">WASM {ready ? 'chargé' : 'chargement'} · Itération {formatNumber(stats.iteration)} · Mode {toolMode}</span>
      </footer>
    </main>
  );
}

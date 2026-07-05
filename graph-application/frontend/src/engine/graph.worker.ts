/// <reference lib="webworker" />
import type { BuildOptions, EngineOptions, GraphFrame, ParsedGraph, RepulsionMode, WorkerInMessage, WorkerOutMessage } from '../types/graph';

const ctx = self as unknown as DedicatedWorkerGlobalScope;

type WasmModule = {
  HEAP32: Int32Array;
  HEAPU32: Uint32Array;
  HEAPF32: Float32Array;
  HEAPU8: Uint8Array;
  _malloc(size: number): number;
  _free(ptr: number): void;
  _ge_init_from_edges(nodeCount: number, edgeCount: number, edgePtr: number, weightPtr: number): number;
  _ge_init_random(nodeCount: number, edgeCount: number): number;
  _ge_step(iterations: number): number;
  _ge_free(): void;
  _ge_get_node_count(): number;
  _ge_get_edge_count(): number;
  _ge_get_live_node_count(): number;
  _ge_get_iteration(): number;
  _ge_get_positions_ptr(): number;
  _ge_get_colors_ptr(): number;
  _ge_get_edges_ptr(): number;
  _ge_get_weights_ptr(): number;
  _ge_get_degrees_ptr(): number;
  _ge_get_communities_ptr(): number;
  _ge_get_deleted_ptr(): number;
  _ge_set_dimensions(width: number, height: number): void;
  _ge_set_force_params(repulsion: number, attraction: number, damping: number, timeStep: number, theta: number, antiRepulsion: number, repulsionMode: number, kmeansEnabled: number, kmeansClusters: number): void;
  _ge_set_node_position(node: number, x: number, y: number): void;
  _ge_delete_node(node: number): void;
  _ge_restore_node(node: number): void;
  _ge_run_label_propagation(iterations: number): void;
  _ge_run_spatial_kmeans(clusters: number, iterations: number): void;
  _ge_reset_layout(seed: number): void;
};

let wasm: WasmModule | null = null;
let running = false;
let options: EngineOptions = {
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
let buildOptions: BuildOptions | null = null;
let loopTimer: number | null = null;

function post(message: WorkerOutMessage, transfer?: Transferable[]): void {
  ctx.postMessage(message, transfer ?? []);
}

async function ensureWasm(): Promise<WasmModule> {
  if (wasm) return wasm;

  // Le module Emscripten est chargé comme asset public et NON bundlé par Vite.
  // C'est plus stable en production Docker/Nginx : le navigateur demande explicitement
  // /wasm/graph-engine.js puis /wasm/graph-engine.wasm.
  const engineJsUrl = new URL('/wasm/graph-engine.js', ctx.location.origin).href;
  const engineWasmUrl = new URL('/wasm/graph-engine.wasm', ctx.location.origin).href;
  const imported = await import(/* @vite-ignore */ engineJsUrl) as {
    default: (options?: { locateFile?: (path: string) => string }) => Promise<WasmModule>;
  };

  wasm = await imported.default({
    locateFile: (path: string) => path.endsWith('.wasm') ? engineWasmUrl : new URL(`/wasm/${path}`, ctx.location.origin).href
  });
  post({ type: 'ready' });
  return wasm;
}

function repulsionModeToNative(mode: RepulsionMode): number {
  if (mode === 'uniform') return 1;
  if (mode === 'inter-community') return 2;
  return 0;
}

function applyOptions(): void {
  if (!wasm) return;
  wasm._ge_set_force_params(
    options.repulsion,
    options.attraction,
    options.damping,
    options.timeStep,
    options.theta,
    options.antiRepulsion,
    repulsionModeToNative(options.repulsionMode),
    options.kmeansEnabled ? 1 : 0,
    options.kmeansClusters
  );
  if (buildOptions) wasm._ge_set_dimensions(buildOptions.width, buildOptions.height);
}

function loadGraph(module: WasmModule, graph: ParsedGraph): void {
  const edgeCount = graph.edges.length;
  const edgeData = new Int32Array(edgeCount * 2);
  const weightData = new Float32Array(edgeCount);
  for (let i = 0; i < edgeCount; i++) {
    const [source, target, weight] = graph.edges[i];
    edgeData[2 * i] = source;
    edgeData[2 * i + 1] = target;
    weightData[i] = weight || 1;
  }

  const edgePtr = module._malloc(edgeData.byteLength);
  const weightPtr = module._malloc(weightData.byteLength);
  module.HEAP32.set(edgeData, edgePtr >> 2);
  module.HEAPF32.set(weightData, weightPtr >> 2);
  const ok = module._ge_init_from_edges(graph.nodeCount, edgeCount, edgePtr, weightPtr);
  module._free(edgePtr);
  module._free(weightPtr);
  if (!ok) throw new Error('Initialisation WASM impossible. Vérifie le graphe importé.');
  applyOptions();
  if (buildOptions?.communityMode === 'none') {
    // Le moteur conserve quand même une couleur par nœud. Pas de traitement supplémentaire.
  } else if (buildOptions?.communityMode === 'degree-buckets') {
    module._ge_run_spatial_kmeans(Math.max(2, buildOptions.kmeansClusters), 3);
  } else {
    module._ge_run_label_propagation(8);
  }
}

function snapshot(): GraphFrame | null {
  if (!wasm) return null;
  const nodeCount = wasm._ge_get_node_count();
  const edgeCount = wasm._ge_get_edge_count();
  if (nodeCount <= 0) return null;

  const positionsPtr = wasm._ge_get_positions_ptr() >> 2;
  const colorsPtr = wasm._ge_get_colors_ptr() >> 2;
  const edgesPtr = wasm._ge_get_edges_ptr() >> 2;
  const weightsPtr = wasm._ge_get_weights_ptr() >> 2;
  const degreesPtr = wasm._ge_get_degrees_ptr() >> 2;
  const communitiesPtr = wasm._ge_get_communities_ptr() >> 2;
  const deletedPtr = wasm._ge_get_deleted_ptr();

  const positions = new Float32Array(wasm.HEAPF32.subarray(positionsPtr, positionsPtr + nodeCount * 2));
  const colors = new Float32Array(wasm.HEAPF32.subarray(colorsPtr, colorsPtr + nodeCount * 3));
  const edges = new Uint32Array(wasm.HEAPU32.subarray(edgesPtr, edgesPtr + edgeCount * 2));
  const weights = new Float32Array(wasm.HEAPF32.subarray(weightsPtr, weightsPtr + edgeCount));
  const degrees = new Int32Array(wasm.HEAP32.subarray(degreesPtr, degreesPtr + nodeCount));
  const communities = new Int32Array(wasm.HEAP32.subarray(communitiesPtr, communitiesPtr + nodeCount));
  const deleted = new Uint8Array(wasm.HEAPU8.subarray(deletedPtr, deletedPtr + nodeCount));

  return {
    type: 'frame',
    nodeCount,
    edgeCount,
    liveNodeCount: wasm._ge_get_live_node_count(),
    iteration: wasm._ge_get_iteration(),
    positions,
    colors,
    edges,
    weights,
    degrees,
    communities,
    deleted
  };
}

function sendFrame(): void {
  const frame = snapshot();
  if (!frame) return;
  post(frame, [frame.positions.buffer, frame.colors.buffer, frame.edges.buffer, frame.weights.buffer, frame.degrees.buffer, frame.communities.buffer, frame.deleted.buffer]);
}

function scheduleNextTick(): void {
  const delayMs = Math.max(4, Math.round(1000 / Math.max(1, Math.min(240, options.simulationRate))));
  loopTimer = ctx.setTimeout(tick, delayMs);
}

function tick(): void {
  if (!wasm || !running) return;
  wasm._ge_step(options.stepsPerFrame);
  sendFrame();
  scheduleNextTick();
}

function setRunning(value: boolean): void {
  running = value;
  if (loopTimer != null) {
    ctx.clearTimeout(loopTimer);
    loopTimer = null;
  }
  if (running) tick();
}

ctx.onmessage = async (event: MessageEvent<WorkerInMessage>) => {
  try {
    const module = await ensureWasm();
    const message = event.data;
    switch (message.type) {
      case 'loadGraph':
        options = message.options;
        buildOptions = message.buildOptions;
        loadGraph(module, message.graph);
        post({
          type: 'loaded',
          nodeCount: module._ge_get_node_count(),
          edgeCount: module._ge_get_edge_count(),
          liveNodeCount: module._ge_get_live_node_count(),
          iteration: module._ge_get_iteration()
        });
        sendFrame();
        break;
      case 'loadDemo':
        options = message.options;
        buildOptions = null;
        module._ge_init_random(message.nodeCount, message.edgeCount);
        applyOptions();
        post({
          type: 'loaded',
          nodeCount: module._ge_get_node_count(),
          edgeCount: module._ge_get_edge_count(),
          liveNodeCount: module._ge_get_live_node_count(),
          iteration: module._ge_get_iteration()
        });
        sendFrame();
        break;
      case 'setOptions':
        options = message.options;
        applyOptions();
        break;
      case 'setRunning':
        setRunning(message.running);
        break;
      case 'requestFrame':
        if (module._ge_get_node_count() > 0) {
          module._ge_step(options.stepsPerFrame);
          sendFrame();
        }
        break;
      case 'setNodePosition':
        module._ge_set_node_position(message.node, message.x, message.y);
        sendFrame();
        break;
      case 'deleteNode':
        module._ge_delete_node(message.node);
        sendFrame();
        break;
      case 'restoreNode':
        module._ge_restore_node(message.node);
        sendFrame();
        break;
      case 'runCommunities':
        module._ge_run_label_propagation(message.iterations);
        sendFrame();
        break;
      case 'runKMeans':
        module._ge_run_spatial_kmeans(message.clusters, message.iterations);
        sendFrame();
        break;
      case 'resetLayout':
        module._ge_reset_layout(Date.now() >>> 0);
        sendFrame();
        break;
      default:
        break;
    }
  } catch (error) {
    post({ type: 'error', message: error instanceof Error ? error.message : String(error) });
  }
};

void ensureWasm().catch((error) => post({ type: 'error', message: error instanceof Error ? error.message : String(error) }));

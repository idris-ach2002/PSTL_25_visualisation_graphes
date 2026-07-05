export type EdgeTuple = [source: number, target: number, weight: number];

export type SourceKind = 'demo' | 'dot' | 'edge-list' | 'numeric-table';
export type ImportSeverity = 'ok' | 'warning' | 'blocked';
export type SimilarityMode = 'cosine' | 'correlation' | 'euclidean';
export type CommunityMode = 'label-propagation' | 'degree-buckets' | 'none';
export type RepulsionMode = 'degree-weighted' | 'uniform' | 'inter-community';
export type ColoringMode = 'community' | 'degree' | 'uniform';
export type RenderMode = 'flat' | 'space3d';
export type EdgeRenderStyle = 'simple' | 'scientific' | 'premium' | 'neon';
export type NodeRenderShape = 'sphere' | 'glass' | 'crystal';
export type FocusMode = 'none' | 'selected' | 'neighbors' | 'community';
export type LabelMode = 'none' | 'selected' | 'important' | 'all';
export type ImportSampleMode = 'first' | 'random' | 'top-degree';
export type LayoutPreset = 'force' | 'circle' | 'grid' | 'communities' | 'radial';

export type GraphHoverInfo =
  | { kind: 'node'; id: number; label: string; degree: number; community: number; x: number; y: number; z: number; screenX: number; screenY: number }
  | { kind: 'edge'; index: number; source: number; target: number; sourceLabel: string; targetLabel: string; weight: number; screenX: number; screenY: number }
  | null;

export type BuildOptions = {
  maxWebNodes: number;
  similarityMode: SimilarityMode;
  automaticThresholds: boolean;
  edgeThreshold: number;
  antiEdgeThreshold: number;
  kNearest: number;
  maxExactNodes: number;
  communityMode: CommunityMode;
  width: number;
  height: number;
  spatialCells: number;
  kmeansEnabled: boolean;
  kmeansClusters: number;
};

export type RenderOptions = {
  nodeBaseSize: number;
  degreeFactor: number;
  minDegree: number;
  minEdgeWeight: number;
  curvedEdges: boolean;
  curveAngle: number;
  curveSegments: number;
  edgeLineWidth: number;
  edgeStyle: EdgeRenderStyle;
  edgeColor: string;
  edgeGlow: number;
  edgeFlow: number;
  coloringMode: ColoringMode;
  backgroundColor: string;
  uniformNodeColor: string;
  showStats: boolean;
  showLabels: boolean;
  labelMode: LabelMode;
  renderMode: RenderMode;
  qualityScale: number;
  nodeShading: boolean;
  edgeOpacity: number;
  depthStrength: number;
  perspective: number;
  rotationX: number;
  rotationY: number;
  rotationZ: number;
  showWorldGrid: boolean;
  hdrEnabled: boolean;
  hdrExposure: number;
  bloomStrength: number;
  nodeShape: NodeRenderShape;
  nodeRimStrength: number;
  nodeSpecularStrength: number;
  nodeInnerGlow: number;
  edgeSoftness: number;
  edgeTaper: boolean;
  communityFilter: number;
  focusNode: number;
  focusMode: FocusMode;
};

export type ImportAnalysis = {
  fileName: string;
  sourceKind: SourceKind | 'unknown';
  nodeCount: number;
  edgeCount: number;
  accepted: boolean;
  severity: ImportSeverity;
  message: string;
  advice: string;
  warnings: string[];
  estimatedMemory?: string;
};


export type GraphInsights = {
  density: number;
  averageDegree: number;
  maxDegree: number;
  maxDegreeNode: number;
  isolatedNodes: number;
  communityCount: number;
  connectedComponents: number;
  largestComponentSize: number;
  interpretation: string[];
};

export type ActionLogEntry = {
  time: string;
  label: string;
  detail?: string;
};

export type ParsedGraph = {
  name: string;
  nodeCount: number;
  edges: EdgeTuple[];
  labels?: string[];
  sourceKind: SourceKind;
  warnings: string[];
  metadata?: {
    columns?: number;
    rows?: number;
    positiveEdges?: number;
    antiEdges?: number;
    edgeThreshold?: number;
    antiEdgeThreshold?: number;
    similarityMode?: SimilarityMode;
    density?: number;
  };
};

export type EngineOptions = {
  repulsion: number;
  attraction: number;
  antiRepulsion: number;
  damping: number;
  friction: number;
  timeStep: number;
  theta: number;
  stepsPerFrame: number;
  simulationRate: number;
  repulsionMode: RepulsionMode;
  kmeansEnabled: boolean;
  kmeansClusters: number;
};

export type GraphFrame = {
  type: 'frame';
  nodeCount: number;
  edgeCount: number;
  liveNodeCount: number;
  iteration: number;
  positions: Float32Array;
  colors: Float32Array;
  edges: Uint32Array;
  weights: Float32Array;
  degrees: Int32Array;
  communities: Int32Array;
  deleted: Uint8Array;
};

export type WorkerOutMessage =
  | GraphFrame
  | { type: 'ready' }
  | { type: 'loaded'; nodeCount: number; edgeCount: number; liveNodeCount: number; iteration: number }
  | { type: 'error'; message: string };

export type WorkerInMessage =
  | { type: 'loadGraph'; graph: ParsedGraph; options: EngineOptions; buildOptions: BuildOptions }
  | { type: 'loadDemo'; nodeCount: number; edgeCount: number; options: EngineOptions }
  | { type: 'setRunning'; running: boolean }
  | { type: 'setOptions'; options: EngineOptions }
  | { type: 'requestFrame' }
  | { type: 'setNodePosition'; node: number; x: number; y: number }
  | { type: 'deleteNode'; node: number }
  | { type: 'restoreNode'; node: number }
  | { type: 'runCommunities'; iterations: number }
  | { type: 'runKMeans'; clusters: number; iterations: number }
  | { type: 'resetLayout' };

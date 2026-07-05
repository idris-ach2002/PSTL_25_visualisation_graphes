import type { GraphHoverInfo, RenderOptions } from '../types/graph';

export type GraphRenderData = {
  positions: Float32Array;
  colors: Float32Array;
  edges: Uint32Array;
  weights: Float32Array;
  degrees: Int32Array;
  communities: Int32Array;
  deleted: Uint8Array;
  labels?: string[];
  nodeCount: number;
  edgeCount: number;
};

export type Camera = {
  x: number;
  y: number;
  zoom: number;
};

type ProgramInfo = {
  program: WebGLProgram;
  attrs: Record<string, number>;
  uniforms: Record<string, WebGLUniformLocation>;
};

type Bounds = {
  minX: number;
  maxX: number;
  minY: number;
  maxY: number;
  centerX: number;
  centerY: number;
  radius: number;
};

export const DEFAULT_RENDER_OPTIONS: RenderOptions = {
  nodeBaseSize: 10.5,
  degreeFactor: 0.95,
  minDegree: 0,
  minEdgeWeight: 0,
  curvedEdges: true,
  curveAngle: 7,
  curveSegments: 12,
  edgeLineWidth: 1.35,
  edgeStyle: 'simple',
  edgeColor: '#dff6ff',
  edgeGlow: 0,
  edgeFlow: 0,
  coloringMode: 'community',
  backgroundColor: '#030814',
  uniformNodeColor: '#72f7ff',
  showStats: true,
  showLabels: false,
  labelMode: 'selected',
  renderMode: 'flat',
  qualityScale: 1.5,
  nodeShading: true,
  edgeOpacity: 0.72,
  depthStrength: 680,
  perspective: 1750,
  rotationX: 58,
  rotationY: -18,
  rotationZ: -8,
  showWorldGrid: true,
  hdrEnabled: false,
  hdrExposure: 1,
  bloomStrength: 0,
  nodeShape: 'sphere',
  nodeRimStrength: 0.42,
  nodeSpecularStrength: 0.54,
  nodeInnerGlow: 0.12,
  edgeSoftness: 0.45,
  edgeTaper: false,
  communityFilter: -1,
  focusNode: -1,
  focusMode: 'none'
};

function compileShader(gl: WebGL2RenderingContext, type: number, source: string): WebGLShader {
  const shader = gl.createShader(type);
  if (!shader) throw new Error('Impossible de créer un shader WebGL.');
  gl.shaderSource(shader, source);
  gl.compileShader(shader);
  if (!gl.getShaderParameter(shader, gl.COMPILE_STATUS)) {
    const log = gl.getShaderInfoLog(shader) ?? 'Erreur inconnue';
    gl.deleteShader(shader);
    throw new Error(log);
  }
  return shader;
}

function createProgram(gl: WebGL2RenderingContext, vertex: string, fragment: string, attrs: string[], uniforms: string[]): ProgramInfo {
  const program = gl.createProgram();
  if (!program) throw new Error('Impossible de créer un programme WebGL.');
  const vs = compileShader(gl, gl.VERTEX_SHADER, vertex);
  const fs = compileShader(gl, gl.FRAGMENT_SHADER, fragment);
  gl.attachShader(program, vs);
  gl.attachShader(program, fs);
  gl.linkProgram(program);
  gl.deleteShader(vs);
  gl.deleteShader(fs);
  if (!gl.getProgramParameter(program, gl.LINK_STATUS)) {
    const log = gl.getProgramInfoLog(program) ?? 'Erreur inconnue';
    gl.deleteProgram(program);
    throw new Error(log);
  }
  const attrMap: Record<string, number> = {};
  const uniformMap: Record<string, WebGLUniformLocation> = {};
  for (const name of attrs) attrMap[name] = gl.getAttribLocation(program, name);
  for (const name of uniforms) {
    const location = gl.getUniformLocation(program, name);
    if (!location) throw new Error(`Uniform manquant : ${name}`);
    uniformMap[name] = location;
  }
  return { program, attrs: attrMap, uniforms: uniformMap };
}

function clamp(value: number, min: number, max: number): number {
  return Math.max(min, Math.min(max, value));
}

function hexToRgb(hex: string): [number, number, number] {
  const clean = hex.replace('#', '').trim();
  if (!/^[0-9a-fA-F]{6}$/.test(clean)) return [0.49, 0.83, 0.99];
  const n = parseInt(clean, 16);
  return [((n >> 16) & 255) / 255, ((n >> 8) & 255) / 255, (n & 255) / 255];
}

function rgbToCss(r: number, g: number, b: number): string {
  const toHex = (v: number) => Math.round(clamp(v, 0, 1) * 255).toString(16).padStart(2, '0');
  return `#${toHex(r)}${toHex(g)}${toHex(b)}`;
}

function escapeXml(value: string): string {
  return value.replace(/&/g, '&amp;').replace(/</g, '&lt;').replace(/>/g, '&gt;').replace(/"/g, '&quot;');
}

function lerp(a: number, b: number, t: number): number {
  return a + (b - a) * t;
}

function radians(degrees: number): number {
  return degrees * Math.PI / 180;
}

function hashUnit(value: number): number {
  const x = Math.sin((value + 1) * 12.9898) * 43758.5453;
  return x - Math.floor(x);
}

const commonProjectionShader = `
vec3 rotateX(vec3 p, float a) {
  float s = sin(a);
  float c = cos(a);
  return vec3(p.x, p.y * c - p.z * s, p.y * s + p.z * c);
}
vec3 rotateY(vec3 p, float a) {
  float s = sin(a);
  float c = cos(a);
  return vec3(p.x * c + p.z * s, p.y, -p.x * s + p.z * c);
}
vec3 rotateZ(vec3 p, float a) {
  float s = sin(a);
  float c = cos(a);
  return vec3(p.x * c - p.y * s, p.x * s + p.y * c, p.z);
}
vec3 projectGraphPoint(vec3 p, vec2 center, vec3 rotation, float renderMode, float perspective, float depthStrength) {
  if (renderMode < 0.5) {
    return vec3(p.xy, 0.0);
  }
  vec3 local = vec3(p.xy - center, p.z * depthStrength);
  local = rotateX(local, rotation.x);
  local = rotateY(local, rotation.y);
  local = rotateZ(local, rotation.z);
  float denom = max(120.0, perspective - local.z);
  float scale = clamp(perspective / denom, 0.38, 2.5);
  return vec3(center + local.xy * scale, local.z);
}
`;

const nodeVertexShader = `#version 300 es
precision highp float;
in vec3 a_position;
in vec3 a_color;
in float a_degree;
in float a_deleted;
uniform vec2 u_resolution;
uniform vec2 u_camera;
uniform vec2 u_graphCenter;
uniform vec3 u_rotation;
uniform float u_zoom;
uniform float u_pointScale;
uniform float u_nodeBaseSize;
uniform float u_degreeFactor;
uniform float u_renderMode;
uniform float u_perspective;
uniform float u_depthStrength;
uniform float u_nodeShading;
uniform float u_nodeShape;
out vec3 v_color;
out float v_deleted;
out float v_depth;
out float v_nodeShading;
out float v_nodeShape;
${commonProjectionShader}
void main() {
  vec3 projected = projectGraphPoint(a_position, u_graphCenter, u_rotation, u_renderMode, u_perspective, u_depthStrength);
  vec2 screen = (projected.xy + u_camera) * u_zoom;
  vec2 clip = (screen / u_resolution) * 2.0 - 1.0;
  float depthClip = clamp(-projected.z / max(1.0, u_depthStrength * 4.0), -0.92, 0.92);
  gl_Position = vec4(clip * vec2(1.0, -1.0), depthClip, 1.0);
  float perspectiveScale = u_renderMode < 0.5 ? 1.0 : clamp(u_perspective / max(120.0, u_perspective - projected.z), 0.45, 2.4);
  float degreeSize = sqrt(max(a_degree, 1.0)) * u_degreeFactor;
  gl_PointSize = max(1.0, u_nodeBaseSize + degreeSize) * u_pointScale * perspectiveScale;
  v_color = a_color;
  v_deleted = a_deleted;
  v_depth = projected.z;
  v_nodeShading = u_nodeShading;
  v_nodeShape = u_nodeShape;
}`;

const nodeFragmentShader = `#version 300 es
precision highp float;
in vec3 v_color;
in float v_deleted;
in float v_depth;
in float v_nodeShading;
in float v_nodeShape;
uniform float u_hdrEnabled;
uniform float u_hdrExposure;
uniform float u_bloomStrength;
uniform float u_nodeRimStrength;
uniform float u_nodeSpecularStrength;
uniform float u_nodeInnerGlow;
out vec4 outColor;
vec3 tonemap(vec3 color) {
  vec3 exposed = color * max(0.25, u_hdrExposure);
  vec3 mapped = vec3(1.0) - exp(-exposed);
  return mix(color, mapped, clamp(u_hdrEnabled, 0.0, 1.0));
}
void main() {
  vec2 uv = gl_PointCoord * 2.0 - 1.0;
  float circleDist = dot(uv, uv);
  float diamondDist = abs(uv.x) * 0.92 + abs(uv.y) * 0.92;
  float shapeDist = v_nodeShape > 1.5 ? diamondDist : circleDist;
  if (shapeDist > 1.0 || v_deleted > 0.5) discard;

  float z = v_nodeShape > 1.5 ? sqrt(max(0.0, 1.0 - diamondDist * 0.82)) : sqrt(max(0.0, 1.0 - circleDist));
  vec3 normal = normalize(vec3(uv * 0.92, z));
  vec3 keyLight = normalize(vec3(-0.44, -0.62, 1.0));
  vec3 fillLight = normalize(vec3(0.56, 0.18, 0.72));
  float diffuse = max(dot(normal, keyLight), 0.0) * 0.86 + max(dot(normal, fillLight), 0.0) * 0.22;
  vec3 viewDir = vec3(0.0, 0.0, 1.0);
  vec3 reflectDir = reflect(-keyLight, normal);
  float specular = pow(max(dot(viewDir, reflectDir), 0.0), 56.0) * u_nodeSpecularStrength;
  float glassSpark = pow(max(dot(viewDir, reflect(-fillLight, normal)), 0.0), 18.0) * 0.22 * u_nodeSpecularStrength;
  float rim = pow(1.0 - max(normal.z, 0.0), 2.05) * u_nodeRimStrength;
  float inner = exp(-circleDist * 2.8) * u_nodeInnerGlow;
  vec3 saturated = mix(v_color, min(vec3(1.0), v_color * 1.42), 0.34);
  vec3 lit = saturated * (0.30 + diffuse * 0.92) + vec3(specular + glassSpark) + saturated * inner;
  vec3 glassTint = mix(lit, vec3(0.82, 0.96, 1.0), (v_nodeShape > 0.5 && v_nodeShape < 1.5) ? 0.16 : 0.0);
  vec3 color = mix(v_color, glassTint, clamp(v_nodeShading, 0.0, 1.0));
  color += saturated * rim * (0.36 + u_bloomStrength * 0.22);
  color = tonemap(color * (1.0 + u_bloomStrength * 0.18));
  float edge = v_nodeShape > 1.5 ? diamondDist : sqrt(circleDist);
  float border = smoothstep(0.78, 1.0, edge);
  color = mix(color, color * 0.46 + vec3(0.12, 0.20, 0.28) * border, border * 0.44 * v_nodeShading);
  float alpha = 1.0 - smoothstep(0.88, 1.02, edge);
  alpha = max(alpha, (1.0 - smoothstep(0.96, 1.0, edge)) * 0.82);
  outColor = vec4(color, alpha);
}`;

const edgeVertexShader = `#version 300 es
precision highp float;
in vec3 a_start;
in vec3 a_end;
in vec4 a_color;
in vec2 a_extrude;
in float a_weight;
uniform vec2 u_resolution;
uniform vec2 u_camera;
uniform vec2 u_graphCenter;
uniform vec3 u_rotation;
uniform float u_zoom;
uniform float u_renderMode;
uniform float u_perspective;
uniform float u_depthStrength;
uniform float u_edgeWidth;
uniform float u_edgeStyle;
uniform float u_edgeTaper;
out vec4 v_color;
out float v_depth;
out float v_side;
out float v_along;
out float v_weight;
${commonProjectionShader}
void main() {
  vec3 projectedStart = projectGraphPoint(a_start, u_graphCenter, u_rotation, u_renderMode, u_perspective, u_depthStrength);
  vec3 projectedEnd = projectGraphPoint(a_end, u_graphCenter, u_rotation, u_renderMode, u_perspective, u_depthStrength);
  vec2 screenStart = (projectedStart.xy + u_camera) * u_zoom;
  vec2 screenEnd = (projectedEnd.xy + u_camera) * u_zoom;
  vec2 delta = screenEnd - screenStart;
  float lengthPx = max(length(delta), 1.0);
  vec2 direction = delta / lengthPx;
  vec2 normal = vec2(-direction.y, direction.x);
  float weightBoost = u_edgeStyle < 0.5 ? 1.0 : clamp(0.88 + sqrt(abs(a_weight)) * 0.34, 0.78, 1.65);
  float taper = mix(1.0, 0.62 + 0.38 * sin(3.14159265 * a_extrude.y), clamp(u_edgeTaper, 0.0, 1.0));
  float widthPx = u_edgeWidth * weightBoost * taper;
  float cap = min(max(widthPx * 0.70, 0.0), lengthPx * 0.18);
  vec2 base = mix(screenStart, screenEnd, a_extrude.y);
  base += direction * (a_extrude.y < 0.5 ? -cap : cap);
  vec2 screen = base + normal * a_extrude.x * widthPx * 0.5;
  vec2 clip = (screen / u_resolution) * 2.0 - 1.0;
  float depth = mix(projectedStart.z, projectedEnd.z, a_extrude.y);
  float depthClip = clamp(-depth / max(1.0, u_depthStrength * 4.0), -0.94, 0.94);
  gl_Position = vec4(clip * vec2(1.0, -1.0), depthClip, 1.0);
  v_color = a_color;
  v_depth = depth;
  v_side = a_extrude.x;
  v_along = a_extrude.y;
  v_weight = a_weight;
}`;

const edgeFragmentShader = `#version 300 es
precision highp float;
in vec4 v_color;
in float v_depth;
in float v_side;
in float v_along;
in float v_weight;
uniform float u_edgeOpacity;
uniform float u_edgeGlow;
uniform float u_edgeFlow;
uniform float u_edgeStyle;
uniform float u_edgePass;
uniform float u_time;
uniform float u_renderMode;
uniform float u_hdrEnabled;
uniform float u_hdrExposure;
uniform float u_bloomStrength;
uniform float u_edgeSoftness;
out vec4 outColor;
vec3 tonemap(vec3 color) {
  vec3 exposed = color * max(0.25, u_hdrExposure);
  vec3 mapped = vec3(1.0) - exp(-exposed);
  return mix(color, mapped, clamp(u_hdrEnabled, 0.0, 1.0));
}
void main() {
  float side = abs(v_side);
  float aa = max(fwidth(side) * (1.45 + u_edgeSoftness * 2.2), 0.010);
  float bodyMask = 1.0 - smoothstep(0.86 - aa, 1.0, side);
  float core = exp(-side * side * mix(13.0, 6.4, clamp(u_edgeSoftness, 0.0, 1.0)));
  float inner = 1.0 - smoothstep(0.22, 0.94, side);
  float outer = 1.0 - smoothstep(0.02, 1.0, side);
  float depthFade = u_renderMode < 0.5 ? 1.0 : clamp(1.18 - abs(v_depth) / 2600.0, 0.60, 1.0);

  vec3 sourceColor = clamp(v_color.rgb, 0.0, 1.0);

  // Mode SIMPLE : arête unie, lisible, sans néon, sans flux et sans effet pointillé.
  // Le centre légèrement plus clair augmente la netteté sans transformer l'arête en tube lumineux.
  if (u_edgeStyle < 0.5) {
    if (u_edgePass < 0.5) {
      float halo = outer * outer * depthFade * u_edgeOpacity * u_edgeGlow * 0.32;
      vec3 haloColor = tonemap(mix(sourceColor, vec3(0.58, 0.86, 1.0), 0.34) * (1.0 + u_bloomStrength * 0.25));
      outColor = vec4(haloColor, halo);
      return;
    }
    vec3 centerLight = mix(sourceColor, vec3(1.0), 0.24 + u_bloomStrength * 0.08);
    vec3 color = mix(sourceColor * 0.76, centerLight, core * (0.50 + u_bloomStrength * 0.10));
    color = tonemap(color * (1.0 + u_bloomStrength * 0.16));
    float alpha = v_color.a * u_edgeOpacity * depthFade * bodyMask * (0.76 + 0.24 * inner);
    outColor = vec4(color, alpha);
    return;
  }

  float signedWeight = v_weight;
  float weightPower = clamp(sqrt(abs(signedWeight)), 0.45, 1.85);
  vec3 darkInk = signedWeight < 0.0 ? vec3(0.46, 0.02, 0.04) : vec3(0.015, 0.055, 0.12);
  vec3 neon = signedWeight < 0.0 ? vec3(1.0, 0.15, 0.20) : mix(vec3(0.10, 0.70, 1.0), sourceColor, 0.38);
  vec3 cinema = mix(sourceColor, neon, 0.50 + 0.11 * u_edgeStyle);

  if (u_edgePass < 0.5) {
    float halo = outer * outer * depthFade * u_edgeOpacity * u_edgeGlow;
    vec3 haloColor = mix(darkInk, cinema, 0.82);
    float haloAlpha = halo * (0.20 + 0.12 * weightPower + 0.05 * u_edgeStyle);
    outColor = vec4(haloColor, haloAlpha);
    return;
  }

  float shimmer = 0.0;
  if (u_edgeFlow > 0.001) {
    float wave = 0.5 + 0.5 * sin((v_along * 2.8 - u_time * 0.95 + abs(signedWeight) * 1.7) * 6.2831853);
    shimmer = wave * u_edgeFlow * core;
  }

  vec3 base = mix(darkInk, sourceColor, 0.70);
  vec3 luminous = mix(sourceColor, neon, 0.64);
  vec3 color = mix(base, luminous, clamp(core * 0.78 + shimmer * 0.35, 0.0, 1.0));
  color += neon * core * (0.06 + 0.04 * u_edgeStyle);
  color = tonemap(color * (1.0 + u_bloomStrength * 0.2));
  float alpha = v_color.a * u_edgeOpacity * depthFade * bodyMask * (0.68 + 0.32 * inner + 0.14 * shimmer);
  outColor = vec4(color, alpha);
}`;

export class GraphRenderer {
  private readonly canvas: HTMLCanvasElement;
  private readonly gl: WebGL2RenderingContext;
  private readonly nodeProgram: ProgramInfo;
  private readonly edgeProgram: ProgramInfo;
  private readonly vaoNodes: WebGLVertexArrayObject;
  private readonly vaoEdges: WebGLVertexArrayObject;
  private readonly positionBuffer: WebGLBuffer;
  private readonly colorBuffer: WebGLBuffer;
  private readonly degreeBuffer: WebGLBuffer;
  private readonly deletedBuffer: WebGLBuffer;
  private readonly edgeStartBuffer: WebGLBuffer;
  private readonly edgeEndBuffer: WebGLBuffer;
  private readonly edgeColorBuffer: WebGLBuffer;
  private readonly edgeExtrudeBuffer: WebGLBuffer;
  private readonly edgeWeightBuffer: WebGLBuffer;
  private edgeVertexCount = 0;
  private data: GraphRenderData | null = null;
  private renderOptions: RenderOptions = DEFAULT_RENDER_OPTIONS;
  private bounds: Bounds = { minX: -1, maxX: 1, minY: -1, maxY: 1, centerX: 0, centerY: 0, radius: 1 };
  private nodeDepths: Float32Array = new Float32Array(0);
  camera: Camera = { x: 0, y: 0, zoom: 1 };

  constructor(canvas: HTMLCanvasElement, renderOptions: RenderOptions = DEFAULT_RENDER_OPTIONS) {
    this.canvas = canvas;
    this.renderOptions = renderOptions;
    const gl = canvas.getContext('webgl2', { antialias: true, alpha: true, powerPreference: 'high-performance' });
    if (!gl) throw new Error('WebGL2 indisponible sur ce navigateur.');
    this.gl = gl;
    const projectionUniforms = ['u_resolution', 'u_camera', 'u_graphCenter', 'u_rotation', 'u_zoom', 'u_renderMode', 'u_perspective', 'u_depthStrength'];
    this.nodeProgram = createProgram(
      gl,
      nodeVertexShader,
      nodeFragmentShader,
      ['a_position', 'a_color', 'a_degree', 'a_deleted'],
      [...projectionUniforms, 'u_pointScale', 'u_nodeBaseSize', 'u_degreeFactor', 'u_nodeShading', 'u_nodeShape', 'u_hdrEnabled', 'u_hdrExposure', 'u_bloomStrength', 'u_nodeRimStrength', 'u_nodeSpecularStrength', 'u_nodeInnerGlow']
    );
    this.edgeProgram = createProgram(
      gl,
      edgeVertexShader,
      edgeFragmentShader,
      ['a_start', 'a_end', 'a_color', 'a_extrude', 'a_weight'],
      [...projectionUniforms, 'u_edgeOpacity', 'u_edgeWidth', 'u_edgeGlow', 'u_edgeFlow', 'u_edgeStyle', 'u_edgePass', 'u_time', 'u_hdrEnabled', 'u_hdrExposure', 'u_bloomStrength', 'u_edgeSoftness', 'u_edgeTaper']
    );

    const vaoNodes = gl.createVertexArray();
    const vaoEdges = gl.createVertexArray();
    const positionBuffer = gl.createBuffer();
    const colorBuffer = gl.createBuffer();
    const degreeBuffer = gl.createBuffer();
    const deletedBuffer = gl.createBuffer();
    const edgeStartBuffer = gl.createBuffer();
    const edgeEndBuffer = gl.createBuffer();
    const edgeColorBuffer = gl.createBuffer();
    const edgeExtrudeBuffer = gl.createBuffer();
    const edgeWeightBuffer = gl.createBuffer();
    if (!vaoNodes || !vaoEdges || !positionBuffer || !colorBuffer || !degreeBuffer || !deletedBuffer || !edgeStartBuffer || !edgeEndBuffer || !edgeColorBuffer || !edgeExtrudeBuffer || !edgeWeightBuffer) {
      throw new Error('Allocation GPU impossible.');
    }
    this.vaoNodes = vaoNodes;
    this.vaoEdges = vaoEdges;
    this.positionBuffer = positionBuffer;
    this.colorBuffer = colorBuffer;
    this.degreeBuffer = degreeBuffer;
    this.deletedBuffer = deletedBuffer;
    this.edgeStartBuffer = edgeStartBuffer;
    this.edgeEndBuffer = edgeEndBuffer;
    this.edgeColorBuffer = edgeColorBuffer;
    this.edgeExtrudeBuffer = edgeExtrudeBuffer;
    this.edgeWeightBuffer = edgeWeightBuffer;

    gl.enable(gl.BLEND);
    gl.blendFunc(gl.SRC_ALPHA, gl.ONE_MINUS_SRC_ALPHA);
    gl.clearDepth(1);
  }

  resize(): void {
    const ratio = Math.min(2.25, Math.max(window.devicePixelRatio || 1, this.renderOptions.qualityScale || 1.5));
    const width = Math.max(1, Math.floor(this.canvas.clientWidth * ratio));
    const height = Math.max(1, Math.floor(this.canvas.clientHeight * ratio));
    if (this.canvas.width !== width || this.canvas.height !== height) {
      this.canvas.width = width;
      this.canvas.height = height;
    }
    this.gl.viewport(0, 0, width, height);
  }

  setRenderOptions(options: RenderOptions): void {
    const previous = this.renderOptions;
    const next = { ...DEFAULT_RENDER_OPTIONS, ...options };
    const requiresGpuRebuild = !previous || [
      'curvedEdges',
      'curveAngle',
      'curveSegments',
      'minDegree',
      'minEdgeWeight',
      'edgeStyle',
      'edgeColor',
      'coloringMode',
      'uniformNodeColor',
      'communityFilter',
      'focusNode',
      'focusMode'
    ].some((key) => previous[key as keyof RenderOptions] !== next[key as keyof RenderOptions]);
    this.renderOptions = next;
    if (requiresGpuRebuild && this.data) this.uploadData(this.data);
  }

  setData(data: GraphRenderData): void {
    this.data = data;
    this.bounds = this.computeBounds(data);
    this.uploadData(data);
  }

  private computeBounds(data: GraphRenderData): Bounds {
    let minX = Number.POSITIVE_INFINITY;
    let maxX = Number.NEGATIVE_INFINITY;
    let minY = Number.POSITIVE_INFINITY;
    let maxY = Number.NEGATIVE_INFINITY;
    for (let i = 0; i < data.nodeCount; i++) {
      if (!this.isNodeVisible(data, i)) continue;
      const x = data.positions[2 * i];
      const y = data.positions[2 * i + 1];
      minX = Math.min(minX, x); maxX = Math.max(maxX, x);
      minY = Math.min(minY, y); maxY = Math.max(maxY, y);
    }
    if (!Number.isFinite(minX) || !Number.isFinite(maxX)) {
      return { minX: -1, maxX: 1, minY: -1, maxY: 1, centerX: 0, centerY: 0, radius: 1 };
    }
    const centerX = (minX + maxX) * 0.5;
    const centerY = (minY + maxY) * 0.5;
    const radius = Math.max(1, Math.hypot(maxX - minX, maxY - minY) * 0.5);
    return { minX, maxX, minY, maxY, centerX, centerY, radius };
  }

  private maxDegree(data: GraphRenderData): number {
    let max = 1;
    for (let i = 0; i < data.degrees.length; i++) max = Math.max(max, data.degrees[i]);
    return max;
  }

  private nodeColors(data: GraphRenderData): Float32Array {
    if (this.renderOptions.coloringMode === 'community') {
      const colors = new Float32Array(data.colors);
      this.applyFocusDimming(data, colors);
      return colors;
    }
    const colors = new Float32Array(data.nodeCount * 3);
    if (this.renderOptions.coloringMode === 'uniform') {
      const [r, g, b] = hexToRgb(this.renderOptions.uniformNodeColor);
      for (let i = 0; i < data.nodeCount; i++) {
        colors[3 * i] = r;
        colors[3 * i + 1] = g;
        colors[3 * i + 2] = b;
      }
      this.applyFocusDimming(data, colors);
      return colors;
    }
    const max = this.maxDegree(data);
    for (let i = 0; i < data.nodeCount; i++) {
      const t = clamp(Math.sqrt(Math.max(0, data.degrees[i])) / Math.sqrt(max), 0, 1);
      colors[3 * i] = lerp(0.18, 0.95, t);
      colors[3 * i + 1] = lerp(0.62, 0.92, 1 - Math.abs(t - 0.5) * 2);
      colors[3 * i + 2] = lerp(1.0, 0.16, t);
    }
    this.applyFocusDimming(data, colors);
    return colors;
  }

  private applyFocusDimming(data: GraphRenderData, colors: Float32Array): void {
    const focusNode = this.renderOptions.focusNode ?? -1;
    if (focusNode < 0 || (this.renderOptions.focusMode ?? 'none') !== 'none') return;
    for (let i = 0; i < data.nodeCount; i++) {
      if (i === focusNode || this.isNeighborOf(data, focusNode, i)) continue;
      colors[3 * i] *= 0.24;
      colors[3 * i + 1] *= 0.24;
      colors[3 * i + 2] *= 0.28;
    }
  }

  private isNeighborOf(data: GraphRenderData, focusNode: number, node: number): boolean {
    if (focusNode < 0 || focusNode >= data.nodeCount) return false;
    if (node === focusNode) return true;
    for (let e = 0; e < data.edgeCount; e++) {
      const a = data.edges[2 * e];
      const b = data.edges[2 * e + 1];
      if ((a === focusNode && b === node) || (b === focusNode && a === node)) return true;
    }
    return false;
  }

  private passesFocusFilter(data: GraphRenderData, node: number): boolean {
    const focusNode = this.renderOptions.focusNode ?? -1;
    const mode = this.renderOptions.focusMode ?? 'none';
    if (mode === 'none' || focusNode < 0) return true;
    if (mode === 'selected') return node === focusNode;
    if (mode === 'neighbors') return this.isNeighborOf(data, focusNode, node);
    if (mode === 'community') return data.communities[node] === data.communities[focusNode];
    return true;
  }

  private isNodeDimmed(data: GraphRenderData, node: number): boolean {
    const focusNode = this.renderOptions.focusNode ?? -1;
    if (focusNode < 0 || (this.renderOptions.focusMode ?? 'none') !== 'none') return false;
    return !this.isNeighborOf(data, focusNode, node);
  }

  private nodeDeletedMask(data: GraphRenderData): Float32Array {
    const mask = new Float32Array(data.nodeCount);
    for (let i = 0; i < data.nodeCount; i++) {
      mask[i] = this.isNodeVisible(data, i) ? 0 : 1;
    }
    return mask;
  }

  private isNodeVisible(data: GraphRenderData, node: number): boolean {
    if (node < 0 || node >= data.nodeCount || data.deleted[node] || data.degrees[node] < this.renderOptions.minDegree) return false;
    const communityFilter = this.renderOptions.communityFilter ?? -1;
    if (communityFilter >= 0 && data.communities[node] !== communityFilter) return false;
    return this.passesFocusFilter(data, node);
  }

  private computeNodeDepths(data: GraphRenderData): Float32Array {
    const depths = new Float32Array(data.nodeCount);
    const maxDegree = this.maxDegree(data);
    const communityScale = 17;
    for (let i = 0; i < data.nodeCount; i++) {
      const community = data.communities[i] >= 0 ? data.communities[i] : 0;
      const communityBand = ((community % communityScale) / Math.max(1, communityScale - 1)) - 0.5;
      const degreeNorm = Math.sqrt(Math.max(0, data.degrees[i])) / Math.sqrt(maxDegree);
      const angle = hashUnit(community * 71 + i * 5) * 6.2831853;
      const radialRelief = Math.sin(angle + community * 0.63) * 0.28 + Math.cos(angle * 0.7) * 0.16;
      const jitter = hashUnit(i * 17 + community * 31) - 0.5;
      // Vraie coordonnée Z utilisée par la caméra orbitale.
      // Elle sépare les communautés en couches et donne du relief aux hubs sans modifier le layout 2D.
      depths[i] = communityBand * 1.35 + (degreeNorm - 0.5) * 0.82 + radialRelief + jitter * 0.16;
    }
    return depths;
  }

  private buildNodePositions(data: GraphRenderData, depths: Float32Array): Float32Array {
    const positions = new Float32Array(data.nodeCount * 3);
    for (let i = 0; i < data.nodeCount; i++) {
      positions[3 * i] = data.positions[2 * i];
      positions[3 * i + 1] = data.positions[2 * i + 1];
      positions[3 * i + 2] = depths[i];
    }
    return positions;
  }

  private edgeColor(data: GraphRenderData, a: number, b: number, weight: number, edgeCount: number, nodeColors: Float32Array): [number, number, number, number] {
    const veryDense = edgeCount > 100000;
    const dense = edgeCount > 25000;
    let alpha = veryDense ? 0.22 : dense ? 0.34 : 0.82;
    const focusNode = this.renderOptions.focusNode ?? -1;
    if (focusNode >= 0 && (this.renderOptions.focusMode ?? 'none') === 'none' && a !== focusNode && b !== focusNode) alpha *= 0.18;

    if (this.renderOptions.edgeStyle === 'simple') {
      const [r, g, bl] = hexToRgb(this.renderOptions.edgeColor);
      return [r, g, bl, alpha];
    }

    const intensity = clamp(Math.sqrt(Math.abs(weight || 1)), 0.18, 1.0);
    if (weight < 0) {
      return [lerp(0.90, 1.0, intensity), lerp(0.08, 0.22, intensity), lerp(0.12, 0.18, intensity), Math.min(0.92, alpha + 0.10)];
    }
    const ar = nodeColors[3 * a] ?? 0.2;
    const ag = nodeColors[3 * a + 1] ?? 0.8;
    const ab = nodeColors[3 * a + 2] ?? 1.0;
    const br = nodeColors[3 * b] ?? 0.2;
    const bg = nodeColors[3 * b + 1] ?? 0.8;
    const bb = nodeColors[3 * b + 2] ?? 1.0;
    const r = lerp(0.06, (ar + br) * 0.5, 0.72);
    const g = lerp(0.46, (ag + bg) * 0.5, 0.72);
    const bl = lerp(0.95, (ab + bb) * 0.5, 0.72);
    return [clamp(r, 0.04, 0.95), clamp(g, 0.26, 1.0), clamp(bl, 0.55, 1.0), alpha];
  }

  private pushRibbonSegment(
    starts: number[],
    ends: number[],
    colors: number[],
    extrudes: number[],
    weights: number[],
    x1: number,
    y1: number,
    z1: number,
    x2: number,
    y2: number,
    z2: number,
    color: [number, number, number, number],
    weight: number
  ): void {
    // Two triangles forming a screen-space ribbon. Width is applied in the vertex shader,
    // so arêtes stay crisp when zooming and on high-DPI screens.
    const vertices: Array<[number, number]> = [
      [-1, 0], [1, 0], [-1, 1],
      [-1, 1], [1, 0], [1, 1]
    ];
    for (const [side, along] of vertices) {
      starts.push(x1, y1, z1);
      ends.push(x2, y2, z2);
      colors.push(...color);
      extrudes.push(side, along);
      weights.push(weight || 1);
    }
  }

  private buildEdgeBuffers(data: GraphRenderData, depths: Float32Array): { starts: Float32Array; ends: Float32Array; colors: Float32Array; extrudes: Float32Array; weights: Float32Array; count: number } {
    const starts: number[] = [];
    const ends: number[] = [];
    const colors: number[] = [];
    const extrudes: number[] = [];
    const weights: number[] = [];
    const curved = this.renderOptions.curvedEdges;
    const nodeColors = this.nodeColors(data);
    const segments = clamp(Math.round(this.renderOptions.curveSegments), 1, 32);
    const angle = clamp(this.renderOptions.curveAngle, 0, 85) * Math.PI / 180;
    for (let e = 0; e < data.edgeCount; e++) {
      const a = data.edges[2 * e];
      const b = data.edges[2 * e + 1];
      const weight = data.weights[e] ?? 1;
      if (!this.isNodeVisible(data, a) || !this.isNodeVisible(data, b)) continue;
      if (Math.abs(weight) < this.renderOptions.minEdgeWeight) continue;
      const x1 = data.positions[2 * a];
      const y1 = data.positions[2 * a + 1];
      const z1 = depths[a];
      const x2 = data.positions[2 * b];
      const y2 = data.positions[2 * b + 1];
      const z2 = depths[b];
      const color = this.edgeColor(data, a, b, weight, data.edgeCount, nodeColors);
      if (!curved || segments <= 1) {
        this.pushRibbonSegment(starts, ends, colors, extrudes, weights, x1, y1, z1, x2, y2, z2, color, weight);
        continue;
      }
      const dx = x2 - x1;
      const dy = y2 - y1;
      const length = Math.sqrt(dx * dx + dy * dy) || 1;
      const nx = -dy / length;
      const ny = dx / length;
      const direction = ((a + b + e) % 2 === 0) ? 1 : -1;
      const offset = clamp(Math.tan(angle) * length * 0.18, 0, 96) * direction;
      const cx = (x1 + x2) * 0.5 + nx * offset;
      const cy = (y1 + y2) * 0.5 + ny * offset;
      const cz = (z1 + z2) * 0.5 + direction * 0.04;
      let px = x1;
      let py = y1;
      let pz = z1;
      for (let s = 1; s <= segments; s++) {
        const t = s / segments;
        const u = 1 - t;
        const qx = u * u * x1 + 2 * u * t * cx + t * t * x2;
        const qy = u * u * y1 + 2 * u * t * cy + t * t * y2;
        const qz = u * u * z1 + 2 * u * t * cz + t * t * z2;
        this.pushRibbonSegment(starts, ends, colors, extrudes, weights, px, py, pz, qx, qy, qz, color, weight);
        px = qx;
        py = qy;
        pz = qz;
      }
    }
    return {
      starts: new Float32Array(starts),
      ends: new Float32Array(ends),
      colors: new Float32Array(colors),
      extrudes: new Float32Array(extrudes),
      weights: new Float32Array(weights),
      count: starts.length / 3
    };
  }

  private uploadData(data: GraphRenderData): void {
    const gl = this.gl;
    const depths = this.computeNodeDepths(data);
    this.nodeDepths = depths;
    const nodePositions = this.buildNodePositions(data, depths);
    gl.bindVertexArray(this.vaoNodes);

    gl.bindBuffer(gl.ARRAY_BUFFER, this.positionBuffer);
    gl.bufferData(gl.ARRAY_BUFFER, nodePositions, gl.DYNAMIC_DRAW);
    gl.enableVertexAttribArray(this.nodeProgram.attrs.a_position);
    gl.vertexAttribPointer(this.nodeProgram.attrs.a_position, 3, gl.FLOAT, false, 0, 0);

    gl.bindBuffer(gl.ARRAY_BUFFER, this.colorBuffer);
    gl.bufferData(gl.ARRAY_BUFFER, this.nodeColors(data), gl.DYNAMIC_DRAW);
    gl.enableVertexAttribArray(this.nodeProgram.attrs.a_color);
    gl.vertexAttribPointer(this.nodeProgram.attrs.a_color, 3, gl.FLOAT, false, 0, 0);

    gl.bindBuffer(gl.ARRAY_BUFFER, this.degreeBuffer);
    gl.bufferData(gl.ARRAY_BUFFER, Float32Array.from(data.degrees), gl.DYNAMIC_DRAW);
    gl.enableVertexAttribArray(this.nodeProgram.attrs.a_degree);
    gl.vertexAttribPointer(this.nodeProgram.attrs.a_degree, 1, gl.FLOAT, false, 0, 0);

    gl.bindBuffer(gl.ARRAY_BUFFER, this.deletedBuffer);
    gl.bufferData(gl.ARRAY_BUFFER, this.nodeDeletedMask(data), gl.DYNAMIC_DRAW);
    gl.enableVertexAttribArray(this.nodeProgram.attrs.a_deleted);
    gl.vertexAttribPointer(this.nodeProgram.attrs.a_deleted, 1, gl.FLOAT, false, 0, 0);

    const edgeBuffers = this.buildEdgeBuffers(data, depths);
    this.edgeVertexCount = edgeBuffers.count;
    gl.bindVertexArray(this.vaoEdges);

    gl.bindBuffer(gl.ARRAY_BUFFER, this.edgeStartBuffer);
    gl.bufferData(gl.ARRAY_BUFFER, edgeBuffers.starts, gl.DYNAMIC_DRAW);
    gl.enableVertexAttribArray(this.edgeProgram.attrs.a_start);
    gl.vertexAttribPointer(this.edgeProgram.attrs.a_start, 3, gl.FLOAT, false, 0, 0);

    gl.bindBuffer(gl.ARRAY_BUFFER, this.edgeEndBuffer);
    gl.bufferData(gl.ARRAY_BUFFER, edgeBuffers.ends, gl.DYNAMIC_DRAW);
    gl.enableVertexAttribArray(this.edgeProgram.attrs.a_end);
    gl.vertexAttribPointer(this.edgeProgram.attrs.a_end, 3, gl.FLOAT, false, 0, 0);

    gl.bindBuffer(gl.ARRAY_BUFFER, this.edgeColorBuffer);
    gl.bufferData(gl.ARRAY_BUFFER, edgeBuffers.colors, gl.DYNAMIC_DRAW);
    gl.enableVertexAttribArray(this.edgeProgram.attrs.a_color);
    gl.vertexAttribPointer(this.edgeProgram.attrs.a_color, 4, gl.FLOAT, false, 0, 0);

    gl.bindBuffer(gl.ARRAY_BUFFER, this.edgeExtrudeBuffer);
    gl.bufferData(gl.ARRAY_BUFFER, edgeBuffers.extrudes, gl.DYNAMIC_DRAW);
    gl.enableVertexAttribArray(this.edgeProgram.attrs.a_extrude);
    gl.vertexAttribPointer(this.edgeProgram.attrs.a_extrude, 2, gl.FLOAT, false, 0, 0);

    gl.bindBuffer(gl.ARRAY_BUFFER, this.edgeWeightBuffer);
    gl.bufferData(gl.ARRAY_BUFFER, edgeBuffers.weights, gl.DYNAMIC_DRAW);
    gl.enableVertexAttribArray(this.edgeProgram.attrs.a_weight);
    gl.vertexAttribPointer(this.edgeProgram.attrs.a_weight, 1, gl.FLOAT, false, 0, 0);

    gl.bindVertexArray(null);
  }

  private applyProjectionUniforms(program: ProgramInfo): void {
    const gl = this.gl;
    const width = this.canvas.width;
    const height = this.canvas.height;
    const cameraX = this.camera.x + width / (2 * this.camera.zoom);
    const cameraY = this.camera.y + height / (2 * this.camera.zoom);
    gl.uniform2f(program.uniforms.u_resolution, width, height);
    gl.uniform2f(program.uniforms.u_camera, cameraX, cameraY);
    gl.uniform2f(program.uniforms.u_graphCenter, this.bounds.centerX, this.bounds.centerY);
    gl.uniform3f(program.uniforms.u_rotation, radians(this.renderOptions.rotationX), radians(this.renderOptions.rotationY), radians(this.renderOptions.rotationZ));
    gl.uniform1f(program.uniforms.u_zoom, this.camera.zoom);
    gl.uniform1f(program.uniforms.u_renderMode, this.renderOptions.renderMode === 'space3d' ? 1 : 0);
    gl.uniform1f(program.uniforms.u_perspective, this.renderOptions.perspective);
    gl.uniform1f(program.uniforms.u_depthStrength, this.renderOptions.depthStrength);
  }

  render(): void {
    const gl = this.gl;
    this.resize();
    const [br, bg, bb] = hexToRgb(this.renderOptions.backgroundColor);
    gl.clearColor(br, bg, bb, 1);
    gl.clear(gl.COLOR_BUFFER_BIT | gl.DEPTH_BUFFER_BIT);
    if (!this.data) return;

    if (this.renderOptions.renderMode === 'space3d') {
      gl.enable(gl.DEPTH_TEST);
      gl.depthFunc(gl.LEQUAL);
    } else {
      gl.disable(gl.DEPTH_TEST);
    }

    // Draw arêtes first as anti-aliased GPU ribbons. We render two passes:
    // 1) a wider contextual halo/ink shadow for contrast on white backgrounds;
    // 2) a crisp illuminated core for readability and premium visual quality.
    if (this.renderOptions.renderMode === 'space3d') {
      gl.enable(gl.DEPTH_TEST);
      gl.depthFunc(gl.LEQUAL);
    } else {
      gl.disable(gl.DEPTH_TEST);
    }
    gl.depthMask(false);
    gl.useProgram(this.edgeProgram.program);
    this.applyProjectionUniforms(this.edgeProgram);
    const pixelScale = Math.min(2.25, Math.max(window.devicePixelRatio || 1, this.renderOptions.qualityScale || 1.5));
    const styleValue = this.renderOptions.edgeStyle === 'simple' ? 0 : this.renderOptions.edgeStyle === 'scientific' ? 1 : this.renderOptions.edgeStyle === 'premium' ? 2 : 3;
    gl.uniform1f(this.edgeProgram.uniforms.u_edgeOpacity, this.renderOptions.edgeOpacity);
    gl.uniform1f(this.edgeProgram.uniforms.u_edgeGlow, this.renderOptions.edgeGlow);
    gl.uniform1f(this.edgeProgram.uniforms.u_edgeFlow, this.renderOptions.edgeFlow);
    gl.uniform1f(this.edgeProgram.uniforms.u_edgeStyle, styleValue);
    gl.uniform1f(this.edgeProgram.uniforms.u_time, performance.now() * 0.001);
    gl.uniform1f(this.edgeProgram.uniforms.u_hdrEnabled, this.renderOptions.hdrEnabled ? 1 : 0);
    gl.uniform1f(this.edgeProgram.uniforms.u_hdrExposure, this.renderOptions.hdrExposure);
    gl.uniform1f(this.edgeProgram.uniforms.u_bloomStrength, this.renderOptions.bloomStrength);
    gl.uniform1f(this.edgeProgram.uniforms.u_edgeSoftness, this.renderOptions.edgeSoftness);
    gl.uniform1f(this.edgeProgram.uniforms.u_edgeTaper, this.renderOptions.edgeTaper ? 1 : 0);
    gl.bindVertexArray(this.vaoEdges);

    if (this.renderOptions.edgeGlow > 0.001) {
      gl.blendFunc(gl.SRC_ALPHA, gl.ONE);
      gl.uniform1f(this.edgeProgram.uniforms.u_edgePass, 0);
      gl.uniform1f(this.edgeProgram.uniforms.u_edgeWidth, this.renderOptions.edgeLineWidth * pixelScale * (3.2 + this.renderOptions.edgeGlow * 2.1));
      gl.drawArrays(gl.TRIANGLES, 0, this.edgeVertexCount);
    }

    gl.blendFunc(gl.SRC_ALPHA, gl.ONE_MINUS_SRC_ALPHA);
    gl.uniform1f(this.edgeProgram.uniforms.u_edgePass, 1);
    gl.uniform1f(this.edgeProgram.uniforms.u_edgeWidth, this.renderOptions.edgeLineWidth * pixelScale);
    gl.drawArrays(gl.TRIANGLES, 0, this.edgeVertexCount);
    gl.depthMask(true);

    if (this.renderOptions.renderMode === 'space3d') {
      gl.enable(gl.DEPTH_TEST);
      gl.depthFunc(gl.LEQUAL);
    }

    gl.useProgram(this.nodeProgram.program);
    this.applyProjectionUniforms(this.nodeProgram);
    gl.uniform1f(this.nodeProgram.uniforms.u_pointScale, Math.min(2.25, Math.max(window.devicePixelRatio || 1, this.renderOptions.qualityScale || 1.5)));
    gl.uniform1f(this.nodeProgram.uniforms.u_nodeBaseSize, this.renderOptions.nodeBaseSize);
    gl.uniform1f(this.nodeProgram.uniforms.u_degreeFactor, this.renderOptions.degreeFactor);
    gl.uniform1f(this.nodeProgram.uniforms.u_nodeShading, this.renderOptions.nodeShading ? 1 : 0);
    gl.uniform1f(this.nodeProgram.uniforms.u_nodeShape, this.renderOptions.nodeShape === 'crystal' ? 2 : this.renderOptions.nodeShape === 'glass' ? 1 : 0);
    gl.uniform1f(this.nodeProgram.uniforms.u_hdrEnabled, this.renderOptions.hdrEnabled ? 1 : 0);
    gl.uniform1f(this.nodeProgram.uniforms.u_hdrExposure, this.renderOptions.hdrExposure);
    gl.uniform1f(this.nodeProgram.uniforms.u_bloomStrength, this.renderOptions.bloomStrength);
    gl.uniform1f(this.nodeProgram.uniforms.u_nodeRimStrength, this.renderOptions.nodeRimStrength);
    gl.uniform1f(this.nodeProgram.uniforms.u_nodeSpecularStrength, this.renderOptions.nodeSpecularStrength);
    gl.uniform1f(this.nodeProgram.uniforms.u_nodeInnerGlow, this.renderOptions.nodeInnerGlow);
    gl.bindVertexArray(this.vaoNodes);
    gl.drawArrays(gl.POINTS, 0, this.data.nodeCount);
    gl.bindVertexArray(null);
    gl.disable(gl.DEPTH_TEST);
  }


  getZoomPercent(): number {
    return Math.round(this.camera.zoom * 100);
  }

  setZoomPercent(percent: number): void {
    this.camera.zoom = clamp(percent / 100, 0.03, 24);
  }


  private pixelRatio(): number {
    return Math.min(2.25, Math.max(window.devicePixelRatio || 1, this.renderOptions.qualityScale || 1.5));
  }

  private rotatePoint(x: number, y: number, z: number): { x: number; y: number; z: number } {
    let px = x;
    let py = y;
    let pz = z;
    const rx = radians(this.renderOptions.rotationX);
    const ry = radians(this.renderOptions.rotationY);
    const rz = radians(this.renderOptions.rotationZ);

    let s = Math.sin(rx), c = Math.cos(rx);
    let ny = py * c - pz * s;
    let nz = py * s + pz * c;
    py = ny;
    pz = nz;

    s = Math.sin(ry); c = Math.cos(ry);
    let nx = px * c + pz * s;
    nz = -px * s + pz * c;
    px = nx;
    pz = nz;

    s = Math.sin(rz); c = Math.cos(rz);
    nx = px * c - py * s;
    ny = px * s + py * c;
    return { x: nx, y: ny, z: pz };
  }

  private projectGraphPointCpu(x: number, y: number, z: number): { x: number; y: number; z: number; scale: number } {
    if (this.renderOptions.renderMode !== 'space3d') return { x, y, z: 0, scale: 1 };
    const localX = x - this.bounds.centerX;
    const localY = y - this.bounds.centerY;
    const localZ = z * this.renderOptions.depthStrength;
    const rotated = this.rotatePoint(localX, localY, localZ);
    const denom = Math.max(120, this.renderOptions.perspective - rotated.z);
    const scale = clamp(this.renderOptions.perspective / denom, 0.38, 2.5);
    return {
      x: this.bounds.centerX + rotated.x * scale,
      y: this.bounds.centerY + rotated.y * scale,
      z: rotated.z,
      scale
    };
  }

  private graphPointToCanvasPixels(x: number, y: number, z = 0): { x: number; y: number; z: number; scale: number } {
    const projected = this.projectGraphPointCpu(x, y, z);
    const cameraX = this.camera.x + this.canvas.width / (2 * this.camera.zoom);
    const cameraY = this.camera.y + this.canvas.height / (2 * this.camera.zoom);
    return {
      x: (projected.x + cameraX) * this.camera.zoom,
      y: (projected.y + cameraY) * this.camera.zoom,
      z: projected.z,
      scale: projected.scale
    };
  }

  private clientToCanvasPixels(clientX: number, clientY: number): { x: number; y: number } {
    const rect = this.canvas.getBoundingClientRect();
    const ratio = this.pixelRatio();
    return {
      x: (clientX - rect.left) * ratio,
      y: (clientY - rect.top) * ratio
    };
  }

  getRotation(): { x: number; y: number; z: number } {
    return { x: this.renderOptions.rotationX, y: this.renderOptions.rotationY, z: this.renderOptions.rotationZ };
  }

  pickEdge(clientX: number, clientY: number, radiusPx = 8): { index: number; source: number; target: number; weight: number } | null {
    if (!this.data) return null;
    const pointer = this.clientToCanvasPixels(clientX, clientY);
    const ratio = this.pixelRatio();
    const thresholdPx = Math.max(radiusPx * ratio, this.renderOptions.edgeLineWidth * 2.2 * ratio);
    let bestIndex = -1;
    let bestSource = -1;
    let bestTarget = -1;
    let bestWeight = 0;
    let bestDist = thresholdPx * thresholdPx;

    const distanceToSegment = (px: number, py: number, ax: number, ay: number, bx: number, by: number): number => {
      const dx = bx - ax;
      const dy = by - ay;
      const len2 = Math.max(1e-6, dx * dx + dy * dy);
      const t = clamp(((px - ax) * dx + (py - ay) * dy) / len2, 0, 1);
      const sx = ax + dx * t;
      const sy = ay + dy * t;
      return (px - sx) * (px - sx) + (py - sy) * (py - sy);
    };

    for (let e = 0; e < this.data.edgeCount; e++) {
      const a = this.data.edges[2 * e];
      const b = this.data.edges[2 * e + 1];
      const weight = this.data.weights[e] ?? 1;
      if (!this.isNodeVisible(this.data, a) || !this.isNodeVisible(this.data, b) || Math.abs(weight) < this.renderOptions.minEdgeWeight) continue;
      const x1 = this.data.positions[2 * a];
      const y1 = this.data.positions[2 * a + 1];
      const z1 = this.nodeDepths[a] ?? 0;
      const x2 = this.data.positions[2 * b];
      const y2 = this.data.positions[2 * b + 1];
      const z2 = this.nodeDepths[b] ?? 0;

      let dist2 = Number.POSITIVE_INFINITY;
      if (this.renderOptions.curvedEdges && this.renderOptions.curveSegments > 1) {
        const dx = x2 - x1;
        const dy = y2 - y1;
        const length = Math.sqrt(dx * dx + dy * dy) || 1;
        const nx = -dy / length;
        const ny = dx / length;
        const direction = ((a + b + e) % 2 === 0) ? 1 : -1;
        const angle = clamp(this.renderOptions.curveAngle, 0, 85) * Math.PI / 180;
        const offset = clamp(Math.tan(angle) * length * 0.18, 0, 96) * direction;
        const cx = (x1 + x2) * 0.5 + nx * offset;
        const cy = (y1 + y2) * 0.5 + ny * offset;
        const cz = (z1 + z2) * 0.5 + direction * 0.04;
        const samples = Math.max(2, Math.min(24, Math.round(this.renderOptions.curveSegments)));
        let prev = this.graphPointToCanvasPixels(x1, y1, z1);
        for (let s = 1; s <= samples; s++) {
          const t = s / samples;
          const u = 1 - t;
          const qx = u * u * x1 + 2 * u * t * cx + t * t * x2;
          const qy = u * u * y1 + 2 * u * t * cy + t * t * y2;
          const qz = u * u * z1 + 2 * u * t * cz + t * t * z2;
          const next = this.graphPointToCanvasPixels(qx, qy, qz);
          dist2 = Math.min(dist2, distanceToSegment(pointer.x, pointer.y, prev.x, prev.y, next.x, next.y));
          prev = next;
        }
      } else {
        const pa = this.graphPointToCanvasPixels(x1, y1, z1);
        const pb = this.graphPointToCanvasPixels(x2, y2, z2);
        dist2 = distanceToSegment(pointer.x, pointer.y, pa.x, pa.y, pb.x, pb.y);
      }

      if (dist2 < bestDist) {
        bestDist = dist2;
        bestIndex = e;
        bestSource = a;
        bestTarget = b;
        bestWeight = weight;
      }
    }
    return bestIndex >= 0 ? { index: bestIndex, source: bestSource, target: bestTarget, weight: bestWeight } : null;
  }

  getNodeHoverInfo(node: number, screenX: number, screenY: number): GraphHoverInfo {
    if (!this.data || node < 0 || node >= this.data.nodeCount) return null;
    return {
      kind: 'node',
      id: node,
      label: this.data.labels?.[node] ?? String(node),
      degree: this.data.degrees[node] ?? 0,
      community: this.data.communities[node] ?? 0,
      x: this.data.positions[2 * node] ?? 0,
      y: this.data.positions[2 * node + 1] ?? 0,
      z: this.nodeDepths[node] ?? 0,
      screenX,
      screenY
    };
  }

  getEdgeHoverInfo(edge: { index: number; source: number; target: number; weight: number }, screenX: number, screenY: number): GraphHoverInfo {
    if (!this.data) return null;
    return {
      kind: 'edge',
      index: edge.index,
      source: edge.source,
      target: edge.target,
      sourceLabel: this.data.labels?.[edge.source] ?? String(edge.source),
      targetLabel: this.data.labels?.[edge.target] ?? String(edge.target),
      weight: edge.weight,
      screenX,
      screenY
    };
  }

  screenToWorld(clientX: number, clientY: number): { x: number; y: number } {
    const rect = this.canvas.getBoundingClientRect();
    const ratio = Math.min(2.25, Math.max(window.devicePixelRatio || 1, this.renderOptions.qualityScale || 1.5));
    const x = (clientX - rect.left) * ratio;
    const y = (clientY - rect.top) * ratio;
    return {
      x: x / this.camera.zoom - this.camera.x - this.canvas.width / (2 * this.camera.zoom),
      y: y / this.camera.zoom - this.camera.y - this.canvas.height / (2 * this.camera.zoom)
    };
  }

  worldToScreen(x: number, y: number): { x: number; y: number } {
    return {
      x: (x + this.camera.x + this.canvas.width / (2 * this.camera.zoom)) * this.camera.zoom,
      y: (y + this.camera.y + this.canvas.height / (2 * this.camera.zoom)) * this.camera.zoom
    };
  }

  pickNode(clientX: number, clientY: number, radiusPx = 14): number | null {
    if (!this.data) return null;
    if (this.renderOptions.renderMode === 'space3d') {
      const pointer = this.clientToCanvasPixels(clientX, clientY);
      const ratio = this.pixelRatio();
      let best = -1;
      let bestDepth = Number.NEGATIVE_INFINITY;
      let bestDist2 = Number.POSITIVE_INFINITY;
      for (let i = 0; i < this.data.nodeCount; i++) {
        if (!this.isNodeVisible(this.data, i)) continue;
        const x = this.data.positions[2 * i];
        const y = this.data.positions[2 * i + 1];
        const z = this.nodeDepths[i] ?? 0;
        const screen = this.graphPointToCanvasPixels(x, y, z);
        const degreeSize = Math.sqrt(Math.max(this.data.degrees[i] ?? 1, 1)) * this.renderOptions.degreeFactor;
        const pointRadius = Math.max(radiusPx, (this.renderOptions.nodeBaseSize + degreeSize) * screen.scale * 0.62) * ratio;
        const dx = pointer.x - screen.x;
        const dy = pointer.y - screen.y;
        const dist2 = dx * dx + dy * dy;
        if (dist2 <= pointRadius * pointRadius) {
          // En 3D, si deux points se superposent à l'écran, on prend celui qui est devant la caméra.
          if (screen.z > bestDepth || (Math.abs(screen.z - bestDepth) < 1e-3 && dist2 < bestDist2)) {
            bestDepth = screen.z;
            bestDist2 = dist2;
            best = i;
          }
        }
      }
      return best >= 0 ? best : null;
    }

    const world = this.screenToWorld(clientX, clientY);
    const radiusWorld = radiusPx / this.camera.zoom;
    let best = -1;
    let bestDist2 = radiusWorld * radiusWorld;
    for (let i = 0; i < this.data.nodeCount; i++) {
      if (!this.isNodeVisible(this.data, i)) continue;
      const dx = this.data.positions[2 * i] - world.x;
      const dy = this.data.positions[2 * i + 1] - world.y;
      const dist2 = dx * dx + dy * dy;
      if (dist2 < bestDist2) {
        bestDist2 = dist2;
        best = i;
      }
    }
    return best >= 0 ? best : null;
  }

  getNodePosition(node: number): { x: number; y: number } | null {
    if (!this.data || node < 0 || node >= this.data.nodeCount) return null;
    return { x: this.data.positions[2 * node], y: this.data.positions[2 * node + 1] };
  }

  resetCamera(): void {
    this.camera = { x: 0, y: 0, zoom: 1 };
  }

  focusNode(node: number): void {
    if (!this.data || node < 0 || node >= this.data.nodeCount) return;
    const x = this.data.positions[2 * node];
    const y = this.data.positions[2 * node + 1];
    this.resize();
    this.camera.x = -x;
    this.camera.y = -y;
    this.camera.zoom = clamp(Math.max(this.camera.zoom, 1.8), 0.03, 24);
  }

  zoomBy(factor: number): void {
    this.camera.zoom = clamp(this.camera.zoom * factor, 0.03, 24);
  }

  fitView(padding = 80): void {
    if (!this.data) return;
    const bounds = this.computeBounds(this.data);
    this.resize();
    const graphW = Math.max(1, bounds.maxX - bounds.minX);
    const graphH = Math.max(1, bounds.maxY - bounds.minY);
    const zoomX = (this.canvas.width - padding * 2) / graphW;
    const zoomY = (this.canvas.height - padding * 2) / graphH;
    this.camera.zoom = clamp(Math.min(zoomX, zoomY), 0.03, 24);
    this.camera.x = -bounds.centerX;
    this.camera.y = -bounds.centerY;
  }

  getData(): GraphRenderData | null {
    return this.data;
  }

  exportSvg(title = 'graph-export'): string {
    const data = this.data;
    if (!data) return '<svg xmlns="http://www.w3.org/2000/svg" width="1200" height="800" />';
    const bounds = this.computeBounds(data);
    const width = 1600;
    const height = 1000;
    const margin = 50;
    const scale = Math.min((width - 2 * margin) / Math.max(1, bounds.maxX - bounds.minX), (height - 2 * margin) / Math.max(1, bounds.maxY - bounds.minY));
    const sx = (x: number) => margin + (x - bounds.minX) * scale;
    const sy = (y: number) => margin + (y - bounds.minY) * scale;
    const nodeColors = this.nodeColors(data);
    const edgeParts: string[] = [];
    for (let e = 0; e < data.edgeCount; e++) {
      const a = data.edges[2 * e];
      const b = data.edges[2 * e + 1];
      const weight = data.weights[e] ?? 1;
      if (!this.isNodeVisible(data, a) || !this.isNodeVisible(data, b) || Math.abs(weight) < this.renderOptions.minEdgeWeight) continue;
      const x1 = sx(data.positions[2 * a]);
      const y1 = sy(data.positions[2 * a + 1]);
      const x2 = sx(data.positions[2 * b]);
      const y2 = sy(data.positions[2 * b + 1]);
      const stroke = this.renderOptions.edgeStyle === 'simple' ? this.renderOptions.edgeColor : (weight < 0 ? '#ff5555' : '#6b7890');
      const dash = weight < 0 && this.renderOptions.edgeStyle !== 'simple' ? ' stroke-dasharray="7 5"' : '';
      if (this.renderOptions.curvedEdges) {
        const dx = x2 - x1;
        const dy = y2 - y1;
        const length = Math.sqrt(dx * dx + dy * dy) || 1;
        const nx = -dy / length;
        const ny = dx / length;
        const direction = ((a + b + e) % 2 === 0) ? 1 : -1;
        const angle = clamp(this.renderOptions.curveAngle, 0, 85) * Math.PI / 180;
        const offset = clamp(Math.tan(angle) * length * 0.18, 0, 96) * direction;
        const cx = (x1 + x2) * 0.5 + nx * offset;
        const cy = (y1 + y2) * 0.5 + ny * offset;
        edgeParts.push(`<path d="M ${x1.toFixed(2)} ${y1.toFixed(2)} Q ${cx.toFixed(2)} ${cy.toFixed(2)} ${x2.toFixed(2)} ${y2.toFixed(2)}" fill="none" stroke="${stroke}" stroke-width="${this.renderOptions.edgeLineWidth}" opacity="${this.renderOptions.edgeOpacity.toFixed(2)}"${dash}/>`);
      } else {
        edgeParts.push(`<line x1="${x1.toFixed(2)}" y1="${y1.toFixed(2)}" x2="${x2.toFixed(2)}" y2="${y2.toFixed(2)}" stroke="${stroke}" stroke-width="${this.renderOptions.edgeLineWidth}" opacity="${this.renderOptions.edgeOpacity.toFixed(2)}"${dash}/>`);
      }
    }
    const nodeParts: string[] = [];
    const labelParts: string[] = [];
    for (let i = 0; i < data.nodeCount; i++) {
      if (!this.isNodeVisible(data, i)) continue;
      const x = sx(data.positions[2 * i]);
      const y = sy(data.positions[2 * i + 1]);
      const r = Math.max(1.8, this.renderOptions.nodeBaseSize * 0.55 + Math.sqrt(Math.max(1, data.degrees[i])) * this.renderOptions.degreeFactor * 0.5);
      const fill = rgbToCss(nodeColors[3 * i], nodeColors[3 * i + 1], nodeColors[3 * i + 2]);
      nodeParts.push(`<circle cx="${x.toFixed(2)}" cy="${y.toFixed(2)}" r="${r.toFixed(2)}" fill="${fill}" stroke="rgba(0,0,0,.32)" stroke-width="0.65" opacity="0.94"/>`);
      if (this.renderOptions.showLabels && data.nodeCount <= 800) {
        labelParts.push(`<text x="${(x + r + 2).toFixed(2)}" y="${(y + 3).toFixed(2)}" font-family="Inter,Arial,sans-serif" font-size="10" fill="#1f2937">${escapeXml(data.labels?.[i] ?? String(i))}</text>`);
      }
    }
    return `<?xml version="1.0" encoding="UTF-8"?>\n<svg xmlns="http://www.w3.org/2000/svg" width="${width}" height="${height}" viewBox="0 0 ${width} ${height}" role="img" aria-label="${escapeXml(title)}">\n  <rect width="100%" height="100%" fill="${escapeXml(this.renderOptions.backgroundColor)}"/>\n  <g id="edges">\n    ${edgeParts.join('\n    ')}\n  </g>\n  <g id="nodes">\n    ${nodeParts.join('\n    ')}\n  </g>\n  <g id="labels">\n    ${labelParts.join('\n    ')}\n  </g>\n</svg>\n`;
  }
}

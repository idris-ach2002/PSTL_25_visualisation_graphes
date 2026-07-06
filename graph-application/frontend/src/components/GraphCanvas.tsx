import { forwardRef, useEffect, useImperativeHandle, useMemo, useRef, useState } from 'react';
import type { CSSProperties } from 'react';
import { GraphRenderer, type GraphRenderData } from '../rendering/GraphRenderer';
import type { GraphHoverInfo, RenderOptions } from '../types/graph';

type Props = {
  data: GraphRenderData | null;
  running: boolean;
  selectedNode: number | null;
  renderOptions: RenderOptions;
  onSelectNode: (node: number | null) => void;
  onMoveNode: (node: number, x: number, y: number) => void;
  onMoveComplete: (node: number, from: { x: number; y: number }, to: { x: number; y: number }) => void;
  onDeleteNode: (node: number) => void;
  onZoomChange: (zoomPercent: number) => void;
  onRenderOptionsChange: (options: RenderOptions) => void;
};

export type GraphCanvasHandle = {
  exportPng: (fileName?: string) => void;
  exportSvg: (fileName?: string) => void;
  fitView: () => void;
  resetCamera: () => void;
  zoomIn: () => void;
  zoomOut: () => void;
  setZoomPercent: (percent: number) => void;
  focusNode: (node: number) => void;
};

function downloadBlob(content: BlobPart, fileName: string, type: string): void {
  const blob = content instanceof Blob ? content : new Blob([content], { type });
  const url = URL.createObjectURL(blob);
  const link = document.createElement('a');
  link.href = url;
  link.download = fileName;
  document.body.appendChild(link);
  link.click();
  link.remove();
  URL.revokeObjectURL(url);
}

function hoverTitle(info: Exclude<GraphHoverInfo, null>): string {
  return info.kind === 'node' ? `Nœud ${info.label}` : `Arête ${info.sourceLabel} → ${info.targetLabel}`;
}

export const GraphCanvas = forwardRef<GraphCanvasHandle, Props>(function GraphCanvas(
  { data, selectedNode, renderOptions, onSelectNode, onMoveNode, onMoveComplete, onDeleteNode, onZoomChange, onRenderOptionsChange },
  ref
) {
  const canvasRef = useRef<HTMLCanvasElement | null>(null);
  const rendererRef = useRef<GraphRenderer | null>(null);
  const draggingRef = useRef<{ kind: 'pan' | 'node' | 'orbit'; node?: number; lastX: number; lastY: number; start?: { x: number; y: number } } | null>(null);
  const renderOptionsRef = useRef<RenderOptions>(renderOptions);
  const lastGraphKeyRef = useRef<string>('');
  const [hoverInfo, setHoverInfo] = useState<GraphHoverInfo>(null);

  const notifyZoom = () => {
    const zoom = rendererRef.current?.getZoomPercent();
    if (zoom != null) onZoomChange(zoom);
  };

  useEffect(() => {
    if (!canvasRef.current) return;
    const renderer = new GraphRenderer(canvasRef.current, renderOptions);
    rendererRef.current = renderer;
    let raf = 0;
    const draw = () => {
      renderer.render();
      raf = requestAnimationFrame(draw);
    };
    draw();
    return () => cancelAnimationFrame(raf);
    // Le renderer est créé une seule fois ; les options sont poussées par l'effet dédié.
    // eslint-disable-next-line react-hooks/exhaustive-deps
  }, []);

  useEffect(() => {
    renderOptionsRef.current = renderOptions;
    if (rendererRef.current) rendererRef.current.setRenderOptions(renderOptions);
  }, [renderOptions]);

  useEffect(() => {
    const renderer = rendererRef.current;
    if (!data || !renderer) return;
    renderer.setData(data);
    const key = `${data.nodeCount}:${data.edgeCount}:${data.labels?.length ?? 0}`;
    if (key !== lastGraphKeyRef.current) {
      lastGraphKeyRef.current = key;
      window.setTimeout(() => {
        renderer.fitView(42);
        renderer.zoomBy(1.34);
        notifyZoom();
      }, 40);
    }
  }, [data]);

  useImperativeHandle(ref, () => ({
    exportPng(fileName = 'graph-export.png') {
      const canvas = canvasRef.current;
      const renderer = rendererRef.current;
      if (!canvas || !renderer) return;
      renderer.render();
      canvas.toBlob((blob) => {
        if (blob) downloadBlob(blob, fileName, 'image/png');
      }, 'image/png');
    },
    exportSvg(fileName = 'graph-export.svg') {
      const renderer = rendererRef.current;
      if (!renderer) return;
      downloadBlob(renderer.exportSvg(fileName), fileName, 'image/svg+xml;charset=utf-8');
    },
    fitView() {
      rendererRef.current?.fitView(42);
      rendererRef.current?.zoomBy(1.22);
      notifyZoom();
    },
    resetCamera() {
      rendererRef.current?.resetCamera();
      notifyZoom();
    },
    zoomIn() {
      rendererRef.current?.zoomBy(1.18);
      notifyZoom();
    },
    zoomOut() {
      rendererRef.current?.zoomBy(0.85);
      notifyZoom();
    },
    setZoomPercent(percent: number) {
      rendererRef.current?.setZoomPercent(percent);
      notifyZoom();
    },
    focusNode(node: number) {
      rendererRef.current?.focusNode(node);
      notifyZoom();
    }
  }), []);

  useEffect(() => {
    const canvas = canvasRef.current;
    const renderer = rendererRef.current;
    if (!canvas || !renderer) return;

    const resolveHover = (event: PointerEvent | MouseEvent) => {
      const rect = canvas.getBoundingClientRect();
      const localX = event.clientX - rect.left;
      const localY = event.clientY - rect.top;
      const node = renderer.pickNode(event.clientX, event.clientY, 16);
      if (node != null) {
        setHoverInfo(renderer.getNodeHoverInfo(node, localX, localY));
        return;
      }
      const edge = renderer.pickEdge(event.clientX, event.clientY, 9);
      if (edge) {
        setHoverInfo(renderer.getEdgeHoverInfo(edge, localX, localY));
        return;
      }
      setHoverInfo(null);
    };

    const onWheel = (event: WheelEvent) => {
      event.preventDefault();
      const before = renderer.screenToWorld(event.clientX, event.clientY);
      const factor = event.deltaY < 0 ? 1.12 : 0.89;
      renderer.camera.zoom = Math.max(0.03, Math.min(24, renderer.camera.zoom * factor));
      const after = renderer.screenToWorld(event.clientX, event.clientY);
      renderer.camera.x += after.x - before.x;
      renderer.camera.y += after.y - before.y;
      notifyZoom();
    };

    const onPointerDown = (event: PointerEvent) => {
      canvas.setPointerCapture(event.pointerId);
      setHoverInfo(null);
      const node = renderer.pickNode(event.clientX, event.clientY);
      const is3D = renderOptionsRef.current.renderMode === 'space3d';
      if (node != null && event.button === 0) {
        onSelectNode(node);
        if (!is3D || event.shiftKey) {
          const start = renderer.getNodePosition(node) ?? renderer.screenToWorld(event.clientX, event.clientY);
          draggingRef.current = { kind: 'node', node, lastX: event.clientX, lastY: event.clientY, start };
        } else {
          draggingRef.current = { kind: 'orbit', lastX: event.clientX, lastY: event.clientY };
        }
      } else {
        onSelectNode(null);
        draggingRef.current = { kind: is3D && !event.shiftKey ? 'orbit' : 'pan', lastX: event.clientX, lastY: event.clientY };
      }
    };

    const onPointerMove = (event: PointerEvent) => {
      const drag = draggingRef.current;
      if (!drag) {
        resolveHover(event);
        return;
      }
      setHoverInfo(null);
      if (drag.kind === 'pan') {
        const dx = event.clientX - drag.lastX;
        const dy = event.clientY - drag.lastY;
        renderer.camera.x += dx / renderer.camera.zoom;
        renderer.camera.y += dy / renderer.camera.zoom;
        drag.lastX = event.clientX;
        drag.lastY = event.clientY;
      } else if (drag.kind === 'orbit') {
        const dx = event.clientX - drag.lastX;
        const dy = event.clientY - drag.lastY;
        const current = renderOptionsRef.current;
        const next = {
          ...current,
          renderMode: 'space3d' as const,
          rotationY: Math.max(-180, Math.min(180, current.rotationY + dx * 0.42)),
          rotationX: Math.max(-89, Math.min(89, current.rotationX + dy * 0.42))
        };
        renderOptionsRef.current = next;
        renderer.setRenderOptions(next);
        onRenderOptionsChange(next);
        drag.lastX = event.clientX;
        drag.lastY = event.clientY;
      } else if (drag.node != null) {
        const world = renderer.screenToWorld(event.clientX, event.clientY);
        onMoveNode(drag.node, world.x, world.y);
      }
    };

    const onPointerUp = (event: PointerEvent) => {
      const drag = draggingRef.current;
      draggingRef.current = null;
      if (drag?.kind === 'node' && drag.node != null && drag.start) {
        const end = renderer.getNodePosition(drag.node);
        if (end && Math.hypot(end.x - drag.start.x, end.y - drag.start.y) > 0.1) {
          onMoveComplete(drag.node, drag.start, end);
        }
      }
      resolveHover(event);
      try {
        canvas.releasePointerCapture(event.pointerId);
      } catch {
        // Ignore les libérations tardives de pointer capture.
      }
    };

    const onDblClick = (event: MouseEvent) => {
      const node = renderer.pickNode(event.clientX, event.clientY);
      if (node != null) onDeleteNode(node);
    };

    const onPointerLeave = () => {
      draggingRef.current = null;
      setHoverInfo(null);
    };

    canvas.addEventListener('wheel', onWheel, { passive: false });
    canvas.addEventListener('pointerdown', onPointerDown);
    canvas.addEventListener('pointermove', onPointerMove);
    canvas.addEventListener('pointerup', onPointerUp);
    canvas.addEventListener('pointercancel', onPointerUp);
    canvas.addEventListener('pointerleave', onPointerLeave);
    canvas.addEventListener('dblclick', onDblClick);
    return () => {
      canvas.removeEventListener('wheel', onWheel);
      canvas.removeEventListener('pointerdown', onPointerDown);
      canvas.removeEventListener('pointermove', onPointerMove);
      canvas.removeEventListener('pointerup', onPointerUp);
      canvas.removeEventListener('pointercancel', onPointerUp);
      canvas.removeEventListener('pointerleave', onPointerLeave);
      canvas.removeEventListener('dblclick', onDblClick);
    };
  }, [onDeleteNode, onMoveComplete, onMoveNode, onRenderOptionsChange, onSelectNode]);


  const miniMap = useMemo(() => {
    if (!data || data.nodeCount === 0) return null;
    let minX = Number.POSITIVE_INFINITY;
    let maxX = Number.NEGATIVE_INFINITY;
    let minY = Number.POSITIVE_INFINITY;
    let maxY = Number.NEGATIVE_INFINITY;
    const visibleNodes: number[] = [];
    for (let i = 0; i < data.nodeCount; i++) {
      if (data.deleted[i] || data.degrees[i] < renderOptions.minDegree) continue;
      const communityFilter = renderOptions.communityFilter ?? -1;
      if (communityFilter >= 0 && data.communities[i] !== communityFilter) continue;
      visibleNodes.push(i);
      const x = data.positions[2 * i];
      const y = data.positions[2 * i + 1];
      minX = Math.min(minX, x); maxX = Math.max(maxX, x);
      minY = Math.min(minY, y); maxY = Math.max(maxY, y);
    }
    if (!Number.isFinite(minX)) return null;
    const w = 160;
    const h = 104;
    const margin = 8;
    const sx = (x: number) => margin + (x - minX) / Math.max(1, maxX - minX) * (w - margin * 2);
    const sy = (y: number) => margin + (y - minY) / Math.max(1, maxY - minY) * (h - margin * 2);
    const edges = [] as Array<{ x1: number; y1: number; x2: number; y2: number }>;
    const maxEdges = Math.min(data.edgeCount, 220);
    for (let e = 0; e < maxEdges; e++) {
      const a = data.edges[2 * e];
      const b = data.edges[2 * e + 1];
      if (data.deleted[a] || data.deleted[b]) continue;
      edges.push({ x1: sx(data.positions[2 * a]), y1: sy(data.positions[2 * a + 1]), x2: sx(data.positions[2 * b]), y2: sy(data.positions[2 * b + 1]) });
    }
    const nodes = visibleNodes.slice(0, 260).map((id) => ({ id, x: sx(data.positions[2 * id]), y: sy(data.positions[2 * id]) }));
    return { w, h, edges, nodes };
  }, [data, renderOptions.communityFilter, renderOptions.minDegree]);

  const zoomInLocal = () => {
    rendererRef.current?.zoomBy(1.18);
    notifyZoom();
  };

  const zoomOutLocal = () => {
    rendererRef.current?.zoomBy(0.85);
    notifyZoom();
  };

  const fitLocal = () => {
    rendererRef.current?.fitView(42);
    rendererRef.current?.zoomBy(1.18);
    notifyZoom();
  };

  return (
    <div
      className={`canvasShell ${renderOptions.renderMode === 'space3d' ? 'canvas3d' : 'canvas2d'} ${renderOptions.showWorldGrid ? 'withGrid' : ''}`}
      style={{ '--graph-bg': renderOptions.backgroundColor } as CSSProperties}
    >
      <canvas ref={canvasRef} className="graphCanvas" />
      <div className="canvasModeBadge">
        <strong>{renderOptions.renderMode === 'space3d' ? '3D orbitale' : '2D simple'}</strong>
        <span>{renderOptions.renderMode === 'space3d' ? 'Glisser pour orbiter' : 'Glisser pour déplacer'} · Molette / pincement pour zoomer</span>
      </div>
      <div className="canvasTouchBar" aria-label="Contrôles tactiles du graphe">
        <button type="button" onClick={zoomOutLocal}>−</button>
        <button type="button" onClick={fitLocal}>Fit</button>
        <button type="button" onClick={zoomInLocal}>+</button>
      </div>
      {miniMap && (
        <svg className="miniMap" viewBox={`0 0 ${miniMap.w} ${miniMap.h}`} aria-label="Mini-map du graphe">
          <rect width={miniMap.w} height={miniMap.h} rx="10" />
          {miniMap.edges.map((edge, index) => <line key={index} x1={edge.x1} y1={edge.y1} x2={edge.x2} y2={edge.y2} />)}
          {miniMap.nodes.map((node) => <circle key={node.id} className={node.id === selectedNode ? 'selected' : ''} cx={node.x} cy={node.y} r={node.id === selectedNode ? 3.2 : 1.7} />)}
        </svg>
      )}
      <div className="canvasHelp">
        2D : glisser fond = déplacer · 3D : glisser fond = orbiter · Maj+glisser = déplacer · Molette = zoom · survol = détails
      </div>
      {selectedNode != null && <div className="selectedBadge">Nœud sélectionné : {selectedNode}</div>}
      {hoverInfo && (
        <div className="graphTooltip" style={{ left: hoverInfo.screenX + 14, top: hoverInfo.screenY + 14 }}>
          <strong>{hoverTitle(hoverInfo)}</strong>
          {hoverInfo.kind === 'node' ? (
            <>
              <span>ID : {hoverInfo.id}</span>
              <span>Degré : {hoverInfo.degree}</span>
              <span>Communauté : {hoverInfo.community}</span>
              <span>X : {hoverInfo.x.toFixed(2)} · Y : {hoverInfo.y.toFixed(2)}</span>
            </>
          ) : (
            <>
              <span>Index : {hoverInfo.index}</span>
              <span>Source : {hoverInfo.source}</span>
              <span>Cible : {hoverInfo.target}</span>
              <span>Poids : {hoverInfo.weight.toFixed(4)}</span>
            </>
          )}
        </div>
      )}
    </div>
  );
});

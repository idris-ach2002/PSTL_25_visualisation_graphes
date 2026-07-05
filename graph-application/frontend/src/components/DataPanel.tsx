import { useMemo, useState } from 'react';
import type { GraphRenderData } from '../rendering/GraphRenderer';

const ROWS_PER_PAGE = 100;

type Props = {
  data: GraphRenderData | null;
};

function pageCount(total: number): number {
  return Math.max(1, Math.ceil(total / ROWS_PER_PAGE));
}

export function DataPanel({ data }: Props) {
  const [active, setActive] = useState<'nodes' | 'edges'>('nodes');
  const [nodePage, setNodePage] = useState(1);
  const [edgePage, setEdgePage] = useState(1);

  const nodeRows = useMemo(() => {
    if (!data) return [];
    const start = (nodePage - 1) * ROWS_PER_PAGE;
    const end = Math.min(data.nodeCount, start + ROWS_PER_PAGE);
    return Array.from({ length: end - start }, (_, offset) => {
      const id = start + offset;
      return {
        id,
        label: data.labels?.[id] ?? String(id),
        community: data.communities[id] ?? id,
        degree: data.degrees[id] ?? 0,
        x: data.positions[2 * id] ?? 0,
        y: data.positions[2 * id + 1] ?? 0,
        deleted: Boolean(data.deleted[id])
      };
    });
  }, [data, nodePage]);

  const edgeRows = useMemo(() => {
    if (!data) return [];
    const start = (edgePage - 1) * ROWS_PER_PAGE;
    const end = Math.min(data.edgeCount, start + ROWS_PER_PAGE);
    return Array.from({ length: end - start }, (_, offset) => {
      const id = start + offset;
      const source = data.edges[2 * id] ?? 0;
      const target = data.edges[2 * id + 1] ?? 0;
      const weight = data.weights[id] ?? 1;
      return {
        id,
        source,
        target,
        sourceLabel: data.labels?.[source] ?? String(source),
        targetLabel: data.labels?.[target] ?? String(target),
        weight,
        kind: weight < 0 ? 'anti-arête' : 'arête'
      };
    });
  }, [data, edgePage]);

  const totalNodes = data?.nodeCount ?? 0;
  const totalEdges = data?.edgeCount ?? 0;
  const nodesPages = pageCount(totalNodes);
  const edgesPages = pageCount(totalEdges);

  return (
    <section className="dataPanel panelLarge">
      <div className="dataHeader">
        <div>
          <h2>Données du graphe</h2>
          <p>{totalNodes.toLocaleString('fr-FR')} nœuds · {totalEdges.toLocaleString('fr-FR')} arêtes · pagination de {ROWS_PER_PAGE} lignes</p>
        </div>
        <div className="tabsCompact">
          <button type="button" className={active === 'nodes' ? 'active' : ''} onClick={() => setActive('nodes')}>Sommets</button>
          <button type="button" className={active === 'edges' ? 'active' : ''} onClick={() => setActive('edges')}>Arêtes</button>
        </div>
      </div>

      {!data && <p className="warning">Aucun graphe chargé.</p>}

      {data && active === 'nodes' && (
        <>
          <div className="pageControls">
            <button type="button" onClick={() => setNodePage((p) => Math.max(1, p - 1))}>Précédent</button>
            <label>
              Page
              <input type="number" min={1} max={nodesPages} value={nodePage} onChange={(event) => setNodePage(Math.max(1, Math.min(nodesPages, Number(event.target.value) || 1)))} />
            </label>
            <span>/ {nodesPages.toLocaleString('fr-FR')}</span>
            <button type="button" onClick={() => setNodePage((p) => Math.min(nodesPages, p + 1))}>Suivant</button>
          </div>
          <div className="tableScroller">
            <table>
              <thead><tr><th>ID</th><th>Label</th><th>Communauté</th><th>Degré</th><th>X</th><th>Y</th><th>Supprimé</th></tr></thead>
              <tbody>
                {nodeRows.map((row) => (
                  <tr key={row.id}>
                    <td>{row.id}</td><td>{row.label}</td><td>{row.community}</td><td>{row.degree}</td><td>{row.x.toFixed(2)}</td><td>{row.y.toFixed(2)}</td><td>{row.deleted ? 'oui' : 'non'}</td>
                  </tr>
                ))}
              </tbody>
            </table>
          </div>
        </>
      )}

      {data && active === 'edges' && (
        <>
          <div className="pageControls">
            <button type="button" onClick={() => setEdgePage((p) => Math.max(1, p - 1))}>Précédent</button>
            <label>
              Page
              <input type="number" min={1} max={edgesPages} value={edgePage} onChange={(event) => setEdgePage(Math.max(1, Math.min(edgesPages, Number(event.target.value) || 1)))} />
            </label>
            <span>/ {edgesPages.toLocaleString('fr-FR')}</span>
            <button type="button" onClick={() => setEdgePage((p) => Math.min(edgesPages, p + 1))}>Suivant</button>
          </div>
          <div className="tableScroller">
            <table>
              <thead><tr><th>#</th><th>Début</th><th>Fin</th><th>Poids</th><th>Type</th></tr></thead>
              <tbody>
                {edgeRows.map((row) => (
                  <tr key={row.id}>
                    <td>{row.id}</td><td>{row.source} · {row.sourceLabel}</td><td>{row.target} · {row.targetLabel}</td><td>{row.weight.toFixed(3)}</td><td>{row.kind}</td>
                  </tr>
                ))}
              </tbody>
            </table>
          </div>
        </>
      )}
    </section>
  );
}

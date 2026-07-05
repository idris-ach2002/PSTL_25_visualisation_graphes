import type { ActionLogEntry, GraphInsights, ParsedGraph } from '../types/graph';

type SelectedDetails = {
  id: number;
  label: string;
  x: number;
  y: number;
  degree: number;
  community: number;
  deleted: boolean;
  neighbors: number[];
  topNeighbors: Array<{ id: number; label: string; degree: number }>;
} | null;

type Props = {
  graph: ParsedGraph | null;
  nodeCount: number;
  edgeCount: number;
  liveNodeCount: number;
  iteration: number;
  selectedNode: number | null;
  selectedDetails?: SelectedDetails;
  hiddenNodes: number;
  displayedEdges: number;
  hiddenEdges: number;
  deletedNodes: number;
  insights: GraphInsights | null;
  actionLog: ActionLogEntry[];
  error: string | null;
  onFocusSelected: (mode: 'none' | 'selected' | 'neighbors' | 'community') => void;
  onClearFocus: () => void;
  onCopySelected: () => void;
  onCenterSelected: () => void;
};

function format(value: number): string {
  return value.toLocaleString('fr-FR');
}

function percent(value: number): string {
  return `${(value * 100).toFixed(value < 0.01 ? 3 : 2)} %`;
}

function StatRow({ label, value }: { label: string; value: string | number }) {
  return (
    <div className="statRow">
      <span>{label}</span>
      <strong>{typeof value === 'number' ? format(value) : value}</strong>
    </div>
  );
}

export function StatsPanel({
  graph,
  nodeCount,
  edgeCount,
  liveNodeCount,
  iteration,
  selectedNode,
  selectedDetails,
  hiddenNodes,
  displayedEdges,
  hiddenEdges,
  deletedNodes,
  insights,
  actionLog,
  error,
  onFocusSelected,
  onClearFocus,
  onCopySelected,
  onCenterSelected
}: Props) {
  const positiveEdges = graph?.metadata?.positiveEdges ?? undefined;
  const antiEdges = graph?.metadata?.antiEdges ?? undefined;
  return (
    <aside className="statsPanel" aria-label="Statistiques du graphe">
      <h2>Analyse du graphe</h2>
      <StatRow label="Nœuds affichés" value={Math.max(0, liveNodeCount - hiddenNodes)} />
      <StatRow label="Arêtes affichées" value={displayedEdges} />
      <StatRow label="Densité" value={insights ? percent(insights.density) : '-'} />
      <StatRow label="Degré moyen" value={insights ? insights.averageDegree.toFixed(2) : '-'} />
      <StatRow label="Hub principal" value={insights ? `#${insights.maxDegreeNode} (${insights.maxDegree})` : '-'} />
      <StatRow label="Communautés" value={insights?.communityCount ?? '-'} />
      <StatRow label="Composantes" value={insights?.connectedComponents ?? '-'} />
      <StatRow label="Plus grande composante" value={insights?.largestComponentSize ?? '-'} />
      <StatRow label="Nœuds isolés" value={insights?.isolatedNodes ?? '-'} />
      <StatRow label="Itérations" value={iteration} />

      {insights && (
        <div className="insightBox">
          {insights.interpretation.map((line) => <p key={line}>{line}</p>)}
        </div>
      )}

      <div className="sideDivider" />
      <h2>Sélection avancée</h2>
      {selectedDetails ? (
        <>
          <StatRow label="ID" value={selectedDetails.id} />
          <StatRow label="Label" value={selectedDetails.label} />
          <StatRow label="Communauté" value={selectedDetails.community} />
          <StatRow label="Degré" value={selectedDetails.degree} />
          <StatRow label="Voisins" value={selectedDetails.neighbors.length} />
          <StatRow label="X" value={selectedDetails.x.toFixed(2)} />
          <StatRow label="Y" value={selectedDetails.y.toFixed(2)} />
          <StatRow label="Supprimé" value={selectedDetails.deleted ? 'oui' : 'non'} />
          <div className="buttonGrid compactActions">
            <button type="button" onClick={() => onFocusSelected('neighbors')}>Voisins</button>
            <button type="button" onClick={() => onFocusSelected('selected')}>Isoler</button>
            <button type="button" onClick={() => onFocusSelected('community')}>Communauté</button>
            <button type="button" onClick={() => onFocusSelected('none')}>Surbrillance</button>
            <button type="button" onClick={onCenterSelected}>Centrer</button>
            <button type="button" onClick={onCopySelected}>Copier</button>
          </div>
          <button type="button" className="ghostButton" onClick={onClearFocus}>Réinitialiser focus/filtres</button>
          {selectedDetails.topNeighbors.length > 0 && (
            <div className="neighborList">
              <strong>Voisins principaux</strong>
              {selectedDetails.topNeighbors.map((neighbor) => (
                <span key={neighbor.id}>#{neighbor.id} · {neighbor.label} · degré {neighbor.degree}</span>
              ))}
            </div>
          )}
        </>
      ) : (
        <div className="emptySelection">
          <span>Survole un nœud ou clique dessus pour afficher ses détails.</span>
          <span>ID: - · X: - · Y: -</span>
        </div>
      )}

      <div className="sideDivider" />
      <h2>Source</h2>
      {graph ? (
        <div className="sourceInfo">
          <p><strong>{graph.name}</strong></p>
          <p>Type : {graph.sourceKind}</p>
          <p>Total nœuds : {format(nodeCount)} · total arêtes : {format(edgeCount)}</p>
          <p>Nœuds cachés : {format(hiddenNodes)} · arêtes cachées : {format(hiddenEdges)}</p>
          <p>Nœuds supprimés : {format(deletedNodes)}</p>
          {positiveEdges != null && <p>Arêtes positives : {format(positiveEdges)}</p>}
          {antiEdges != null && antiEdges > 0 && <p>Anti-arêtes : {format(antiEdges)}</p>}
          {graph.metadata?.edgeThreshold != null && <p>Seuil : {graph.metadata.edgeThreshold.toFixed(4)}</p>}
          {graph.metadata?.antiEdgeThreshold != null && <p>Anti-seuil : {graph.metadata.antiEdgeThreshold.toFixed(4)}</p>}
        </div>
      ) : (
        <p className="mutedText">Aucun graphe chargé.</p>
      )}

      <div className="sideDivider" />
      <h2>Historique</h2>
      <div className="actionLog">
        {actionLog.length === 0 ? <p className="mutedText">Aucune action enregistrée.</p> : actionLog.slice(0, 8).map((entry, index) => (
          <div key={`${entry.time}-${index}`}>
            <span>{entry.time}</span>
            <strong>{entry.label}</strong>
            {entry.detail && <small>{entry.detail}</small>}
          </div>
        ))}
      </div>

      {graph?.warnings.map((warning) => <p className="warning" key={warning}>{warning}</p>)}
      {selectedNode != null && !selectedDetails && <p className="warning">Le nœud sélectionné n’est plus visible.</p>}
      {error && <p className="error">{error}</p>}
    </aside>
  );
}

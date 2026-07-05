import { useMemo, useState } from 'react';
import type { BuildOptions, CommunityMode, EdgeRenderStyle, EngineOptions, FocusMode, LabelMode, LayoutPreset, NodeRenderShape, RenderMode, RenderOptions, RepulsionMode, SimilarityMode } from '../types/graph';

type Props = {
  running: boolean;
  options: EngineOptions;
  buildOptions: BuildOptions;
  renderOptions: RenderOptions;
  canUndo: boolean;
  canRedo: boolean;
  onToggleRunning: () => void;
  onOptionsChange: (options: EngineOptions) => void;
  onBuildOptionsChange: (options: BuildOptions) => void;
  onRenderOptionsChange: (options: RenderOptions) => void;
  onLoadFile: (file: File) => void;
  onLoadProject: (file: File) => void;
  onDemo: (nodeCount?: number) => void;
  onStep: () => void;
  onReset: () => void;
  onRebuild: () => void;
  onRunCommunities: () => void;
  onRunKMeans: () => void;
  onUndo: () => void;
  onRedo: () => void;
  onExportProject: () => void;
  onExportPng: () => void;
  onExportSvg: () => void;
  onFitView: () => void;
  onResetCamera: () => void;
  onZoomIn: () => void;
  onZoomOut: () => void;
  zoomPercent: number;
  onZoomPercentChange: (percent: number) => void;
  onApplyLayout: (layout: LayoutPreset) => void;
  onPresentationMode: () => void;
  maxWebNodeHardLimit: number;
};

type SidebarSection = 'project' | 'construction' | 'simulation' | 'nodes' | 'edges' | 'camera' | 'actions';

function NumberSlider({ label, min, max, step, value, onChange }: { label: string; min: number; max: number; step: number; value: number; onChange: (value: number) => void }) {
  return (
    <label className="sliderControl">
      <span>{label}</span>
      <input type="range" min={min} max={max} step={step} value={value} onChange={(event) => onChange(Number(event.target.value))} />
      <strong>{Number.isInteger(value) ? value : value.toFixed(step < 0.01 ? 3 : 2)}</strong>
    </label>
  );
}

function SelectControl<T extends string>({ label, value, values, labels, onChange }: { label: string; value: T; values: T[]; labels?: Record<T, string>; onChange: (value: T) => void }) {
  return (
    <label className="fieldControl">
      <span>{label}</span>
      <select value={value} onChange={(event) => onChange(event.target.value as T)}>
        {values.map((entry) => <option value={entry} key={entry}>{labels?.[entry] ?? entry}</option>)}
      </select>
    </label>
  );
}

function CheckControl({ label, checked, onChange }: { label: string; checked: boolean; onChange: (checked: boolean) => void }) {
  return (
    <label className="checkControl">
      <input type="checkbox" checked={checked} onChange={(event) => onChange(event.target.checked)} />
      <span>{label}</span>
    </label>
  );
}

export function Toolbar({
  running,
  options,
  buildOptions,
  renderOptions,
  canUndo,
  canRedo,
  onToggleRunning,
  onOptionsChange,
  onBuildOptionsChange,
  onRenderOptionsChange,
  onLoadFile,
  onLoadProject,
  onDemo,
  onStep,
  onReset,
  onRebuild,
  onRunCommunities,
  onRunKMeans,
  onUndo,
  onRedo,
  onExportProject,
  onExportPng,
  onExportSvg,
  onFitView,
  onResetCamera,
  onZoomIn,
  onZoomOut,
  zoomPercent,
  onZoomPercentChange,
  onApplyLayout,
  onPresentationMode,
  maxWebNodeHardLimit
}: Props) {
  const [activeSection, setActiveSection] = useState<SidebarSection>('project');
  const [demoNodeCount, setDemoNodeCount] = useState(360);

  const sections = useMemo<Array<{ id: SidebarSection; label: string; hint: string }>>(() => [
    { id: 'project', label: 'Projet', hint: 'Importer, ouvrir, démo et limite web' },
    { id: 'construction', label: 'Construction', hint: 'CSV/DOT, similarité, seuils, communautés' },
    { id: 'simulation', label: 'Simulation', hint: 'Forces, vitesse, reset et clustering' },
    { id: 'nodes', label: 'Nœuds', hint: 'Taille, forme, coloration et filtres' },
    { id: 'edges', label: 'Arêtes', hint: 'Couleur, épaisseur, courbure et netteté' },
    { id: 'camera', label: 'Caméra / Orbite', hint: 'Zoom, qualité pixels, mode 2D ou caméra orbitale 3D' },
    { id: 'actions', label: 'Actions', hint: 'Undo, zoom, export PNG/SVG/JSON' }
  ], []);

  const applyReal3DPreset = () => {
    onRenderOptionsChange({
      ...renderOptions,
      backgroundColor: '#030814',
      renderMode: 'space3d',
      qualityScale: 1.5,
      hdrEnabled: false,
      hdrExposure: 1,
      bloomStrength: 0,
      nodeShape: 'sphere',
      nodeBaseSize: Math.max(renderOptions.nodeBaseSize, 10.5),
      degreeFactor: Math.max(renderOptions.degreeFactor, 0.9),
      nodeShading: true,
      nodeRimStrength: 0.42,
      nodeSpecularStrength: 0.54,
      nodeInnerGlow: 0.12,
      edgeStyle: 'simple',
      edgeColor: '#dff6ff',
      edgeLineWidth: 1.35,
      edgeOpacity: 0.74,
      edgeGlow: 0,
      edgeFlow: 0,
      edgeSoftness: 0.45,
      edgeTaper: false,
      curvedEdges: true,
      curveAngle: 7,
      curveSegments: 12,
      showWorldGrid: true,
      depthStrength: 980,
      perspective: 1450,
      rotationX: 62,
      rotationY: -34,
      rotationZ: -8
    });
  };

  const applySimple2DPreset = () => {
    onRenderOptionsChange({
      ...renderOptions,
      backgroundColor: '#030814',
      renderMode: 'flat',
      qualityScale: 1.5,
      hdrEnabled: false,
      hdrExposure: 1,
      bloomStrength: 0,
      nodeShape: 'sphere',
      nodeBaseSize: Math.max(renderOptions.nodeBaseSize, 10.5),
      degreeFactor: Math.max(renderOptions.degreeFactor, 0.9),
      nodeShading: true,
      edgeStyle: 'simple',
      edgeColor: '#dff6ff',
      edgeLineWidth: 1.35,
      edgeOpacity: 0.72,
      edgeGlow: 0,
      edgeFlow: 0,
      edgeSoftness: 0.45,
      edgeTaper: false,
      showWorldGrid: true
    });
  };


  const set3DView = (rotationX: number, rotationY: number, rotationZ = 0) => {
    onRenderOptionsChange({
      ...renderOptions,
      renderMode: 'space3d',
      depthStrength: Math.max(renderOptions.depthStrength, 920),
      perspective: Math.min(renderOptions.perspective, 1650),
      rotationX,
      rotationY,
      rotationZ
    });
  };


  return (
    <aside className="toolbar sidebarWithSections">
      <div className="sectionSwitcher" role="tablist" aria-label="Catégories de paramètres">
        {sections.map((section) => (
          <button
            key={section.id}
            type="button"
            role="tab"
            aria-selected={activeSection === section.id}
            className={activeSection === section.id ? 'active' : ''}
            onClick={() => setActiveSection(section.id)}
          >
            <span>{section.label}</span>
            <small>{section.hint}</small>
          </button>
        ))}
      </div>

      {activeSection === 'project' && (
        <section className="panel loadPanel activePanel">
          <h2>Projet</h2>
          <p className="mutedText">Charge un graphe, ouvre un projet ou génère une démo optimisée pour le rendu web.</p>
          <label className="fileButton">
            Import CSV / DOT
            <input
              type="file"
              accept=".csv,.dot,.txt,text/csv,text/plain"
              onChange={(event) => {
                const file = event.target.files?.[0];
                if (file) onLoadFile(file);
                event.currentTarget.value = '';
              }}
            />
          </label>
          <label className="fileButton secondaryFile">
            Ouvrir projet JSON
            <input
              type="file"
              accept=".json,application/json"
              onChange={(event) => {
                const file = event.target.files?.[0];
                if (file) onLoadProject(file);
                event.currentTarget.value = '';
              }}
            />
          </label>
          <NumberSlider label="Limite import web" min={100} max={maxWebNodeHardLimit} step={50} value={buildOptions.maxWebNodes} onChange={(maxWebNodes) => onBuildOptionsChange({ ...buildOptions, maxWebNodes, maxExactNodes: Math.min(maxWebNodes, buildOptions.maxExactNodes || maxWebNodes) })} />
          <NumberSlider label="Nœuds démo" min={40} max={400} step={10} value={demoNodeCount} onChange={setDemoNodeCount} />
          <button type="button" onClick={() => onDemo(demoNodeCount)}>Générer une démo</button>
          <div className="buttonRow two topGap">
            <button type="button" onClick={applySimple2DPreset}>Preset 2D simple</button>
            <button type="button" onClick={onFitView}>Centrer</button>
          </div>
          <div className="webLimitNote">
            <strong>Mode web contrôlé</strong>
            <span>Limite active : {buildOptions.maxWebNodes.toLocaleString('fr-FR')} nœuds, réglable jusqu’à {maxWebNodeHardLimit.toLocaleString('fr-FR')}. La démo reste plafonnée à 400 nœuds.</span>
          </div>
        </section>
      )}

      {activeSection === 'construction' && (
        <section className="panel forcePanel activePanel">
          <h2>Construction du graphe</h2>
          <p className="mutedText">Paramètres utilisés lors de l’import ou de la reconstruction du graphe.</p>
          <SelectControl<SimilarityMode>
            label="Similarité"
            value={buildOptions.similarityMode}
            values={['cosine', 'correlation', 'euclidean']}
            labels={{ cosine: 'Cosinus', correlation: 'Corrélation', euclidean: 'Distance euclidienne' }}
            onChange={(similarityMode) => onBuildOptionsChange({ ...buildOptions, similarityMode })}
          />
          <CheckControl label="Seuils automatiques" checked={buildOptions.automaticThresholds} onChange={(automaticThresholds) => onBuildOptionsChange({ ...buildOptions, automaticThresholds })} />
          <NumberSlider label="Seuil arêtes" min={-1} max={1} step={0.01} value={buildOptions.edgeThreshold} onChange={(edgeThreshold) => onBuildOptionsChange({ ...buildOptions, edgeThreshold })} />
          <NumberSlider label="Anti-seuil" min={-1} max={1} step={0.01} value={buildOptions.antiEdgeThreshold} onChange={(antiEdgeThreshold) => onBuildOptionsChange({ ...buildOptions, antiEdgeThreshold })} />
          <NumberSlider label="Voisins kNN" min={1} max={24} step={1} value={buildOptions.kNearest} onChange={(kNearest) => onBuildOptionsChange({ ...buildOptions, kNearest })} />
          <NumberSlider label="Calcul exact max" min={100} max={buildOptions.maxWebNodes} step={50} value={Math.min(buildOptions.maxExactNodes, buildOptions.maxWebNodes)} onChange={(maxExactNodes) => onBuildOptionsChange({ ...buildOptions, maxExactNodes })} />
          <SelectControl<CommunityMode>
            label="Communautés"
            value={buildOptions.communityMode}
            values={['label-propagation', 'degree-buckets', 'none']}
            labels={{ 'label-propagation': 'Label propagation', 'degree-buckets': 'K-means spatial', none: 'Aucune' }}
            onChange={(communityMode) => onBuildOptionsChange({ ...buildOptions, communityMode })}
          />
          <NumberSlider label="Largeur espace" min={400} max={6000} step={100} value={buildOptions.width} onChange={(width) => onBuildOptionsChange({ ...buildOptions, width })} />
          <NumberSlider label="Hauteur espace" min={300} max={4000} step={100} value={buildOptions.height} onChange={(height) => onBuildOptionsChange({ ...buildOptions, height })} />
          <CheckControl label="K-Means dynamique" checked={buildOptions.kmeansEnabled} onChange={(kmeansEnabled) => onBuildOptionsChange({ ...buildOptions, kmeansEnabled })} />
          <NumberSlider label="Clusters K-Means" min={2} max={64} step={1} value={buildOptions.kmeansClusters} onChange={(kmeansClusters) => onBuildOptionsChange({ ...buildOptions, kmeansClusters })} />
          <button type="button" className="primary" onClick={onRebuild}>Reconstruire avec ces paramètres</button>
        </section>
      )}

      {activeSection === 'simulation' && (
        <section className="panel activePanel">
          <h2>Simulation</h2>
          <p className="mutedText">Réglages dynamiques. Ces valeurs s’appliquent sans réimporter le fichier.</p>
          <div className="buttonRow">
            <button type="button" className="primary" onClick={onToggleRunning}>{running ? 'Pause' : 'Lancer'}</button>
            <button type="button" onClick={onStep}>Step</button>
            <button type="button" onClick={onReset}>Reset</button>
          </div>
          <NumberSlider label="Pas/frame" min={1} max={24} step={1} value={options.stepsPerFrame} onChange={(stepsPerFrame) => onOptionsChange({ ...options, stepsPerFrame })} />
          <NumberSlider label="Fréquence Hz" min={10} max={240} step={5} value={options.simulationRate} onChange={(simulationRate) => onOptionsChange({ ...options, simulationRate })} />
          <NumberSlider label="Répulsion" min={500} max={50000} step={500} value={options.repulsion} onChange={(repulsion) => onOptionsChange({ ...options, repulsion })} />
          <NumberSlider label="Attraction" min={0.001} max={0.08} step={0.001} value={options.attraction} onChange={(attraction) => onOptionsChange({ ...options, attraction })} />
          <NumberSlider label="Répulsion anti-arêtes" min={100} max={20000} step={100} value={options.antiRepulsion} onChange={(antiRepulsion) => onOptionsChange({ ...options, antiRepulsion })} />
          <NumberSlider label="Damping" min={0.5} max={0.98} step={0.01} value={options.damping} onChange={(damping) => onOptionsChange({ ...options, damping })} />
          <NumberSlider label="Barnes-Hut θ" min={0.3} max={1.4} step={0.01} value={options.theta} onChange={(theta) => onOptionsChange({ ...options, theta })} />
          <SelectControl<RepulsionMode>
            label="Mode répulsion"
            value={options.repulsionMode}
            values={['degree-weighted', 'uniform', 'inter-community']}
            labels={{ 'degree-weighted': 'Pondéré degré', uniform: 'Uniforme', 'inter-community': 'Inter-communautés' }}
            onChange={(repulsionMode) => onOptionsChange({ ...options, repulsionMode })}
          />
          <div className="buttonRow two">
            <button type="button" onClick={onRunCommunities}>Communautés</button>
            <button type="button" onClick={onRunKMeans}>K-Means</button>
          </div>
        </section>
      )}

      {activeSection === 'nodes' && (
        <section className="panel activePanel">
          <h2>Nœuds</h2>
          <p className="mutedText">Forme, volume et lisibilité des sommets. Le mode verre est le plus réaliste.</p>
          <SelectControl<NodeRenderShape>
            label="Forme"
            value={renderOptions.nodeShape}
            values={['sphere', 'glass', 'crystal']}
            labels={{ sphere: 'Sphère nette', glass: 'Verre', crystal: 'Cristal' }}
            onChange={(nodeShape) => onRenderOptionsChange({ ...renderOptions, nodeShape })}
          />
          <NumberSlider label="Taille nœud" min={1} max={30} step={0.5} value={renderOptions.nodeBaseSize} onChange={(nodeBaseSize) => onRenderOptionsChange({ ...renderOptions, nodeBaseSize })} />
          <NumberSlider label="Facteur degré" min={0} max={5} step={0.05} value={renderOptions.degreeFactor} onChange={(degreeFactor) => onRenderOptionsChange({ ...renderOptions, degreeFactor })} />
          <SelectControl
            label="Coloration"
            value={renderOptions.coloringMode}
            values={['community', 'degree', 'uniform']}
            labels={{ community: 'Communauté', degree: 'Degré', uniform: 'Uniforme' }}
            onChange={(coloringMode) => onRenderOptionsChange({ ...renderOptions, coloringMode })}
          />
          <label className="fieldControl">
            <span>Couleur uniforme</span>
            <input type="color" value={renderOptions.uniformNodeColor} onChange={(event) => onRenderOptionsChange({ ...renderOptions, uniformNodeColor: event.target.value })} />
          </label>
          <NumberSlider label="Degré minimum" min={0} max={50} step={1} value={renderOptions.minDegree} onChange={(minDegree) => onRenderOptionsChange({ ...renderOptions, minDegree })} />
          <NumberSlider label="Poids min. arêtes" min={0} max={5} step={0.05} value={renderOptions.minEdgeWeight} onChange={(minEdgeWeight) => onRenderOptionsChange({ ...renderOptions, minEdgeWeight })} />
          <NumberSlider label="Filtre communauté (-1 = toutes)" min={-1} max={64} step={1} value={renderOptions.communityFilter} onChange={(communityFilter) => onRenderOptionsChange({ ...renderOptions, communityFilter })} />
          <SelectControl<FocusMode>
            label="Focus sélection"
            value={renderOptions.focusMode}
            values={['none', 'selected', 'neighbors', 'community']}
            labels={{ none: 'Surbrillance simple', selected: 'Isoler sélection', neighbors: 'Sélection + voisins', community: 'Communauté sélectionnée' }}
            onChange={(focusMode) => onRenderOptionsChange({ ...renderOptions, focusMode })}
          />
          <SelectControl<LabelMode>
            label="Labels"
            value={renderOptions.labelMode}
            values={['none', 'selected', 'important', 'all']}
            labels={{ none: 'Aucun', selected: 'Sélection', important: 'Nœuds importants', all: 'Tous' }}
            onChange={(labelMode) => onRenderOptionsChange({ ...renderOptions, labelMode, showLabels: labelMode !== 'none' })}
          />
          <CheckControl label="Labels export SVG" checked={renderOptions.showLabels} onChange={(showLabels) => onRenderOptionsChange({ ...renderOptions, showLabels })} />
        </section>
      )}

      {activeSection === 'edges' && (
        <section className="panel activePanel">
          <h2>Arêtes</h2>
          <p className="mutedText">Arêtes simples par défaut, mais avec rendu GPU haute résolution, anti-crénelage et option de halo discret.</p>
          <SelectControl<EdgeRenderStyle>
            label="Style arêtes"
            value={renderOptions.edgeStyle}
            values={['simple', 'scientific', 'premium', 'neon']}
            labels={{ simple: 'Simple claire', scientific: 'Scientifique net', premium: 'Premium contrasté', neon: 'Néon dynamique' }}
            onChange={(edgeStyle) => onRenderOptionsChange({ ...renderOptions, edgeStyle, edgeFlow: edgeStyle === 'simple' ? 0 : renderOptions.edgeFlow })}
          />
          <label className="fieldControl">
            <span>Couleur arêtes</span>
            <input type="color" value={renderOptions.edgeColor} onChange={(event) => onRenderOptionsChange({ ...renderOptions, edgeColor: event.target.value })} />
          </label>
          <div className="edgeColorPresets" aria-label="Presets couleur arêtes">
            <button type="button" onClick={() => onRenderOptionsChange({ ...renderOptions, edgeColor: '#ffffff', edgeStyle: 'simple', edgeFlow: 0 })}>Blanc</button>
            <button type="button" onClick={() => onRenderOptionsChange({ ...renderOptions, edgeColor: '#edfaff', edgeStyle: 'simple', edgeFlow: 0 })}>Blanc bleuté</button>
            <button type="button" onClick={() => onRenderOptionsChange({ ...renderOptions, edgeColor: '#8fd5ff', edgeStyle: 'simple', edgeFlow: 0 })}>Bleu clair</button>
          </div>
          <CheckControl label="Arêtes courbes" checked={renderOptions.curvedEdges} onChange={(curvedEdges) => onRenderOptionsChange({ ...renderOptions, curvedEdges })} />
          <CheckControl label="Amincir aux extrémités" checked={renderOptions.edgeTaper} onChange={(edgeTaper) => onRenderOptionsChange({ ...renderOptions, edgeTaper })} />
          <NumberSlider label="Angle courbure" min={0} max={85} step={1} value={renderOptions.curveAngle} onChange={(curveAngle) => onRenderOptionsChange({ ...renderOptions, curveAngle })} />
          <NumberSlider label="Segments courbe" min={1} max={36} step={1} value={renderOptions.curveSegments} onChange={(curveSegments) => onRenderOptionsChange({ ...renderOptions, curveSegments })} />
          <NumberSlider label="Épaisseur" min={0.6} max={12} step={0.05} value={renderOptions.edgeLineWidth} onChange={(edgeLineWidth) => onRenderOptionsChange({ ...renderOptions, edgeLineWidth })} />
          <NumberSlider label="Opacité" min={0.05} max={1} step={0.01} value={renderOptions.edgeOpacity} onChange={(edgeOpacity) => onRenderOptionsChange({ ...renderOptions, edgeOpacity })} />
          <NumberSlider label="Douceur AA" min={0} max={1} step={0.01} value={renderOptions.edgeSoftness} onChange={(edgeSoftness) => onRenderOptionsChange({ ...renderOptions, edgeSoftness })} />
          <NumberSlider label="Halo discret" min={0} max={1} step={0.01} value={renderOptions.edgeGlow} onChange={(edgeGlow) => onRenderOptionsChange({ ...renderOptions, edgeGlow })} />
          {renderOptions.edgeStyle !== 'simple' && <NumberSlider label="Flux lumineux" min={0} max={1} step={0.01} value={renderOptions.edgeFlow} onChange={(edgeFlow) => onRenderOptionsChange({ ...renderOptions, edgeFlow })} />}
        </section>
      )}

      {activeSection === 'camera' && (
        <section className="panel activePanel">
          <h2>Caméra / Orbite</h2>
          <p className="mutedText">Mode 2D simple par défaut. En 3D, le graphe devient un volume WebGL : glisse le fond pour orbiter autour du graphe, Maj+glisser pour déplacer la caméra.</p>
          <div className="buttonRow two">
            <button type="button" className="primary" onClick={applySimple2DPreset}>2D simple</button>
            <button type="button" onClick={applyReal3DPreset}>Activer 3D orbitale</button>
          </div>
          <div className="buttonRow three">
            <button type="button" onClick={() => set3DView(62, -34, -8)}>Iso</button>
            <button type="button" onClick={() => set3DView(0, 0, 0)}>Face</button>
            <button type="button" onClick={() => set3DView(0, 88, 0)}>Côté</button>
          </div>
          <SelectControl<RenderMode>
            label="Mode"
            value={renderOptions.renderMode}
            values={['flat', 'space3d']}
            labels={{ flat: '2D simple', space3d: '3D orbitale WebGL' }}
            onChange={(renderMode) => onRenderOptionsChange({ ...renderOptions, renderMode })}
          />
          <NumberSlider label="Zoom affichage %" min={30} max={450} step={5} value={zoomPercent} onChange={onZoomPercentChange} />
          <NumberSlider label="Qualité pixels" min={1} max={2.25} step={0.25} value={renderOptions.qualityScale} onChange={(qualityScale) => onRenderOptionsChange({ ...renderOptions, qualityScale })} />
          <label className="fieldControl">
            <span>Fond</span>
            <input type="color" value={renderOptions.backgroundColor} onChange={(event) => onRenderOptionsChange({ ...renderOptions, backgroundColor: event.target.value })} />
          </label>
          <CheckControl label="Grille de fond" checked={renderOptions.showWorldGrid} onChange={(showWorldGrid) => onRenderOptionsChange({ ...renderOptions, showWorldGrid })} />
          <CheckControl label="Éclairage des nœuds" checked={renderOptions.nodeShading} onChange={(nodeShading) => onRenderOptionsChange({ ...renderOptions, nodeShading })} />
          <NumberSlider label="Profondeur 3D" min={120} max={2200} step={20} value={renderOptions.depthStrength} onChange={(depthStrength) => onRenderOptionsChange({ ...renderOptions, depthStrength })} />
          <NumberSlider label="Perspective" min={700} max={3600} step={50} value={renderOptions.perspective} onChange={(perspective) => onRenderOptionsChange({ ...renderOptions, perspective })} />
          <NumberSlider label="Rotation X" min={-80} max={80} step={1} value={renderOptions.rotationX} onChange={(rotationX) => onRenderOptionsChange({ ...renderOptions, rotationX })} />
          <NumberSlider label="Rotation Y" min={-80} max={80} step={1} value={renderOptions.rotationY} onChange={(rotationY) => onRenderOptionsChange({ ...renderOptions, rotationY })} />
          <NumberSlider label="Rotation Z" min={-180} max={180} step={1} value={renderOptions.rotationZ} onChange={(rotationZ) => onRenderOptionsChange({ ...renderOptions, rotationZ })} />
        </section>
      )}

      {activeSection === 'actions' && (
        <section className="panel activePanel">
          <h2>Actions</h2>
          <div className="buttonRow two">
            <button type="button" onClick={onUndo} disabled={!canUndo}>Annuler</button>
            <button type="button" onClick={onRedo} disabled={!canRedo}>Rétablir</button>
          </div>
          <div className="buttonRow three">
            <button type="button" onClick={onZoomIn}>+</button>
            <button type="button" onClick={onZoomOut}>−</button>
            <button type="button" onClick={onFitView}>Fit</button>
          </div>
          <button type="button" onClick={onResetCamera}>Réinitialiser caméra</button>
          <div className="sideDivider" />
          <h3>Layouts rapides</h3>
          <div className="buttonGrid">
            <button type="button" onClick={() => onApplyLayout('force')}>Force</button>
            <button type="button" onClick={() => onApplyLayout('circle')}>Circulaire</button>
            <button type="button" onClick={() => onApplyLayout('grid')}>Grille</button>
            <button type="button" onClick={() => onApplyLayout('communities')}>Communautés</button>
            <button type="button" onClick={() => onApplyLayout('radial')}>Radial sélection</button>
          </div>
          <button type="button" onClick={onPresentationMode}>Mode présentation</button>
          <div className="buttonRow two topGap">
            <button type="button" onClick={onExportPng}>PNG</button>
            <button type="button" onClick={onExportSvg}>SVG</button>
          </div>
          <button type="button" onClick={onExportProject}>Enregistrer projet JSON</button>
        </section>
      )}
    </aside>
  );
}

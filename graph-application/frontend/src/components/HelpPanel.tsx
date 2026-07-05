type Props = {
  nodeLimit: number;
  onDemo: (nodeCount?: number) => void;
  onOpenFile: () => void;
};

export function HelpPanel({ nodeLimit, onDemo, onOpenFile }: Props) {
  return (
    <section className="helpPanel">
      <header className="helpHero">
        <div>
          <p className="eyebrow">Guide intégré</p>
          <h1>MonGraphe Web : visualiser, analyser, présenter</h1>
          <p>
            Cette version web privilégie la lisibilité et l’interaction. La limite active est de {nodeLimit.toLocaleString('fr-FR')} nœuds maximum,
            paramétrable jusqu’à 1 000. La génération de démo est volontairement plafonnée à 400 nœuds.
          </p>
        </div>
        <div className="buttonRow two">
          <button type="button" onClick={() => onDemo(360)}>Charger une démo</button>
          <button type="button" onClick={onOpenFile}>Importer un fichier</button>
        </div>
      </header>

      <div className="docGrid">
        <article className="docCard">
          <h2>1. Import intelligent</h2>
          <p>L’assistant d’import analyse le fichier avant le chargement : type détecté, nombre de nœuds, arêtes et compatibilité avec la limite web.</p>
          <ul>
            <li>CSV numérique : une ligne devient un nœud.</li>
            <li>CSV edge-list : colonnes source/target/weight.</li>
            <li>DOT : syntaxe Graphviz <code>a -- b</code> ou <code>a -&gt; b</code>.</li>
          </ul>
          <div className="docExample">Fichier trop grand → premiers nœuds, échantillon aléatoire ou nœuds les plus connectés.</div>
        </article>

        <article className="docCard">
          <h2>2. Lire le graphe</h2>
          <p>Survole un nœud ou une arête pour afficher ses informations. Clique sur un nœud pour activer le panneau de sélection avancée.</p>
          <ul>
            <li><strong>Voisins</strong> : garde la sélection et ses connexions directes.</li>
            <li><strong>Isoler</strong> : ne garde que le nœud sélectionné.</li>
            <li><strong>Communauté</strong> : masque les autres groupes.</li>
          </ul>
        </article>

        <article className="docCard">
          <h2>3. Vues et caméra</h2>
          <p>Le mode par défaut est la 2D simple. Le mode 3D orbitale permet de faire tourner le graphe dans l’espace.</p>
          <ul>
            <li>Molette : zoom.</li>
            <li>Glisser en 2D : déplacer la vue.</li>
            <li>Glisser en 3D : orbiter autour du graphe.</li>
            <li>Maj + glisser : déplacer la caméra.</li>
          </ul>
        </article>

        <article className="docCard">
          <h2>4. Layouts rapides</h2>
          <p>Les layouts rapides aident à comprendre la structure sans relancer tout le moteur.</p>
          <ul>
            <li>Force : remet le moteur de spatialisation.</li>
            <li>Circulaire : utile pour vérifier la connectivité.</li>
            <li>Communautés : sépare visuellement les groupes.</li>
            <li>Radial : met un nœud sélectionné au centre.</li>
          </ul>
        </article>

        <article className="docCard">
          <h2>5. Analyse automatique</h2>
          <p>Le panneau de droite calcule densité, degré moyen, hub principal, composantes connexes et interprétation rapide du graphe.</p>
          <div className="docPipeline">
            <span>Fichier</span><b>→</b><span>Analyse</span><b>→</b><span>WASM</span><b>→</b><span>WebGL</span><b>→</b><span>Export</span>
          </div>
        </article>

        <article className="docCard">
          <h2>6. Exports et projets</h2>
          <p>Tu peux exporter une image PNG, un SVG vectoriel, ou sauvegarder un projet JSON complet.</p>
          <ul>
            <li>PNG : rendu visuel actuel.</li>
            <li>SVG : image vectorielle pour rapport.</li>
            <li>JSON : graphe, paramètres, rendu et historique.</li>
          </ul>
        </article>
      </div>
    </section>
  );
}

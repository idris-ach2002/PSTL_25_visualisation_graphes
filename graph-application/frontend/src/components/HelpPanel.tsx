type Props = {
  nodeLimit: number;
  onDemo: (nodeCount?: number) => void;
  onOpenFile: () => void;
};

export function HelpPanel({ nodeLimit, onDemo, onOpenFile }: Props) {
  return (
    <section className="helpPanel">
      <header className="helpHero uxHero">
        <div>
          <p className="eyebrow">Guide intégré</p>
          <h1>Explorer un graphe sans se perdre</h1>
          <p>
            MonGraphe Web a été optimisé pour une utilisation sur téléphone, tablette et ordinateur. Le mode 2D sert à analyser,
            le mode 3D orbitale sert à présenter et comprendre la structure sous plusieurs angles.
          </p>
        </div>
        <div className="heroActions">
          <button type="button" onClick={() => onDemo(360)}>Charger une démo</button>
          <button type="button" onClick={onOpenFile}>Importer un fichier</button>
        </div>
      </header>

      <div className="guideLayout">
        <aside className="guideSummary">
          <strong>Parcours conseillé</strong>
          <ol>
            <li>Importer ou générer une démo.</li>
            <li>Contrôler la limite de nœuds.</li>
            <li>Choisir un template visuel.</li>
            <li>Filtrer le graphe.</li>
            <li>Sélectionner un nœud.</li>
            <li>Exporter ou présenter.</li>
          </ol>
          <div className="deviceMatrix">
            <span>Phone</span><strong>barre mobile</strong>
            <span>Tablet</span><strong>panneaux adaptatifs</strong>
            <span>PC</span><strong>interface complète</strong>
          </div>
        </aside>

        <div className="docGrid uxDocGrid">
          <article className="docCard featuredDocCard">
            <h2>1. Utilisation rapide</h2>
            <p>Commence par une démo ou un fichier CSV/DOT. L’assistant vérifie le format, estime la taille du graphe et évite un import trop lourd.</p>
            <div className="docExample">Limite active : {nodeLimit.toLocaleString('fr-FR')} nœuds · démo plafonnée à 400 nœuds.</div>
          </article>

          <article className="docCard">
            <h2>2. Templates visuels</h2>
            <p>Dans le panneau Projet, utilise les templates rapides :</p>
            <ul>
              <li><strong>Analyse 2D</strong> : simple, lisible, stable.</li>
              <li><strong>Présentation 3D</strong> : caméra orbitale et profondeur.</li>
              <li><strong>Mobile lisible</strong> : nœuds plus grands, rendu allégé.</li>
            </ul>
          </article>

          <article className="docCard">
            <h2>3. Contrôles tactiles</h2>
            <p>Sur mobile et tablette, la barre rapide en bas donne accès au graphe, aux paramètres, à l’analyse et au guide.</p>
            <ul>
              <li>Pincer ou utiliser + / − pour zoomer.</li>
              <li>Glisser le fond pour déplacer en 2D.</li>
              <li>En 3D, glisser pour orbiter.</li>
            </ul>
          </article>

          <article className="docCard">
            <h2>4. Lecture du graphe</h2>
            <p>Survole un nœud ou une arête pour voir ses informations. Clique sur un nœud pour ouvrir la sélection avancée.</p>
            <ul>
              <li><strong>Voisins</strong> : garde les connexions directes.</li>
              <li><strong>Communauté</strong> : isole un groupe.</li>
              <li><strong>Centrer</strong> : replace la caméra sur le nœud.</li>
            </ul>
          </article>

          <article className="docCard">
            <h2>5. Interface par device</h2>
            <p>L’interface s’adapte selon la largeur disponible.</p>
            <div className="responsiveCards">
              <span>≤ 760 px : panneaux empilés, barre mobile, canvas prioritaire.</span>
              <span>761–1180 px : analyse masquée par défaut, paramètres compacts.</span>
              <span>≥ 1180 px : interface complète avec paramètres et statistiques.</span>
            </div>
          </article>

          <article className="docCard">
            <h2>6. Exports et présentation</h2>
            <p>Le mode présentation masque les panneaux pour mettre le graphe au centre. Les exports PNG/SVG reprennent la vue courante.</p>
            <div className="docPipeline">
              <span>Import</span><b>→</b><span>Analyse</span><b>→</b><span>Template</span><b>→</b><span>Export</span>
            </div>
          </article>
        </div>
      </div>
    </section>
  );
}

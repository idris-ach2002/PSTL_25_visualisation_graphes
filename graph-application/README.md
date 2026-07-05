# MonGraphe Web WASM

Migration web performante de l'application desktop JavaFX/JOGL/JNI de visualisation de graphes.

La version web utilise :

- React + TypeScript pour l'interface ;
- WebGL2 pour le rendu GPU ;
- WebAssembly pour le moteur de simulation écrit en C ;
- Web Worker pour éviter de bloquer l'interface ;
- Docker pour compiler les dépendances et lancer l'application avec une commande.


## Rendu graphique amélioré

La vue du graphe propose maintenant deux modes :
- **2D nette** : rendu plat, précis et lisible ;
- **3D cinématique** : profondeur pseudo-3D, perspective, rotations, grille de fond et éclairage sphérique des nœuds.

Le mode 3D reste basé sur WebGL2 et sur des buffers GPU, afin de garder de bonnes performances même sur des graphes volumineux.

## Lancement

```bash
./run_app.sh
```

Puis ouvrir :

```text
http://localhost:8080
```

Mode développement :

```bash
./run_app.sh dev
```

Puis ouvrir :

```text
http://localhost:5173
```

## Interface

L'interface est organisée comme une application desktop :

- menu supérieur : Fichier, Édition, Affichage, Outils, Aide ;
- barre d'outils : Select, Move, Delete, zoom, play/pause, step ;
- onglets : Overview, Data, Preview ;
- panneau gauche : chargement, construction CSV, ForceAtlas, paramètres dynamiques, preview ;
- zone centrale : rendu WebGL2 du graphe ;
- panneau droit : statistiques, sélection, source ;
- barre de statut : état du moteur, itération, mode actif.

## Fonctionnalités principales

- Import CSV / DOT ;
- construction de graphe depuis CSV numérique ;
- similarité cosinus, corrélation et distance euclidienne ;
- seuils automatiques ou manuels ;
- anti-arêtes ;
- simulation de forces dans le moteur C/WASM ;
- communautés par label propagation ;
- K-Means spatial ;
- affichage Data paginé ;
- preview : tailles, couleurs, filtres, arêtes courbes ;
- export PNG ;
- export SVG ;
- sauvegarde et ouverture de projet JSON ;
- undo / redo pour suppression et déplacement de nœuds.

## Structure

```text
graph_web_migration/
├── frontend/                 # React + TypeScript + WebGL2
├── wasm-engine/              # moteur C compilé en WebAssembly
├── legacy/                   # ancien moteur C conservé comme référence
├── Dockerfile                # build production
├── Dockerfile.dev            # build développement
├── docker-compose.yml
├── nginx.conf
└── run_app.sh
```

## Notes

Cette migration n'est pas un lancement JavaFX dans le navigateur. Le rendu et l'interface sont natifs web. Le moteur est compilé en WebAssembly pour rester proche de l'approche C performante du projet original.


## Correctif chargement WASM

Le moteur WASM est servi comme asset public via `/wasm/graph-engine.js` et `/wasm/graph-engine.wasm`, ce qui évite les blocages de bundling Worker/Vite en production Docker.

## Correction rendu des arêtes

Cette version remplace le rendu `gl.LINES` par des rubans GPU en triangles. Cela évite la limite classique de WebGL où `lineWidth` reste souvent bloqué à 1px selon le navigateur/GPU. Les arêtes sont donc plus nettes, plus visibles, compatibles avec le zoom, les écrans haute densité et le mode 3D cinématique.

## Rendu haute qualité des arêtes

Cette version ajoute un rendu d'arêtes plus premium : les arêtes ne sont plus seulement des segments ou rubans simples. Elles sont dessinées en deux passes GPU :

1. un halo / contour de contraste pour rendre les relations lisibles sur fond clair ;
2. un cœur anti-crénelé avec léger éclairage et variation selon le poids de l'arête.

Les options ajoutées dans Preview > Arêtes sont :

- Style arêtes : Scientifique net, Premium contrasté, Néon dynamique ;
- Halo / contraste ;
- Flux lumineux ;
- Épaisseur et opacité conservées.

Le rendu reste en WebGL2 avec buffers et triangles, donc compatible avec de gros graphes sans créer d'objet DOM par arête.

## Version enhanced : limite web, assistant et documentation intégrée

Cette version ajoute une contrainte explicite de visualisation web : **1 000 nœuds maximum paramétrables**.
Le navigateur reste très fluide pour l’exploration interactive, le rendu WebGL2, la simulation WASM et les exports. Si un fichier importé dépasse cette limite, l’application bloque le chargement et affiche une fenêtre explicative invitant l’utilisateur à importer un fichier plus petit.

### Fonctionnalités ajoutées

- Analyse des imports CSV/DOT/edge-list avant chargement effectif.
- Blocage propre au-dessus de 1 000 nœuds avec pop-up utilisateur.
- Démo générée automatiquement dans une taille compatible web.
- Barre de recherche rapide par identifiant ou label de nœud.
- Boutons de presets : **Optimiser**, **Clarté dark**, **Lecture**.
- Nouvel onglet **Help / Doc** avec guide intégré et exemples visuels.
- Documentation dans l’application : flux CSV → analyse → WASM → WebGL, explication des onglets, raccourcis et préparation des fichiers.

### Lancement

```bash
./run_app.sh fresh
```

Puis ouvrir :

```text
http://localhost:8080
```

## Mise à jour : sections de paramètres + rendu fluide

La barre latérale gauche est maintenant organisée par catégories afin d'éviter un panneau trop long :

- **Projet** : import CSV/DOT, projet JSON, démo et limite web.
- **Construction** : similarité, seuils, kNN, communautés, espace de layout.
- **Simulation** : ForceAtlas, vitesse, répulsion, attraction, K-Means.
- **Nœuds** : forme, taille, coloration, filtres et labels.
- **Arêtes** : couleur, épaisseur, courbure, anti-crénelage, halo discret.
- **Caméra / Zoom** : zoom, qualité pixels raisonnable, profondeur 3D et rotations.
- **Actions** : undo/redo, zoom, export PNG/SVG/JSON.

Le rendu a été renforcé pour profiter de la limite web à 1 000 nœuds : rendu 2D simple par défaut, zoom initial agrandi, nœuds plus visibles, arêtes simples nettes, mode 3D réelle WebGL optionnel et infobulles au survol des nœuds/arêtes.

## Mise à jour complète : lots 1 à 4

Cette version ajoute les lots fonctionnels demandés en une seule livraison :

- limite web passée à **1 000 nœuds maximum**, réglable simplement depuis le panneau **Projet** ;
- génération de démo plafonnée à **400 nœuds** ;
- assistant d’import avec analyse du fichier avant chargement ;
- échantillonnage intelligent : premiers nœuds, aléatoire ou nœuds les plus connectés ;
- panneau d’analyse : densité, degré moyen, hub principal, composantes, communautés et interprétation ;
- sélection avancée : isoler un nœud, afficher ses voisins, filtrer sa communauté, centrer la caméra et copier les infos ;
- filtres : degré, poids, communauté et focus de sélection ;
- mini-map dans le canvas ;
- layouts rapides : force, circulaire, grille, communautés, radial ;
- sauvegarde JSON enrichie avec historique ;
- mode présentation ;
- documentation Help / Doc enrichie avec exemples intégrés.

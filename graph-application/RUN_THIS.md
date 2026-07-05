# Lancement rapide

Depuis la racine du projet :

```bash
./run_app.sh
```

L'application est ensuite disponible sur :

```text
http://localhost:8080
```

Modes disponibles :

```bash
./run_app.sh prod     # build Docker + lancement Nginx production
./run_app.sh dev      # lancement Vite dev server sur http://localhost:5173
./run_app.sh build    # build seulement
./run_app.sh logs     # logs du service production
./run_app.sh down     # arrêt des conteneurs
./run_app.sh clean    # arrêt + suppression volumes/images locales du compose
```

# Interface

La vue web imite maintenant davantage une application desktop type Gephi / MonGraphe :

- barre de titre ;
- menus Fichier, Édition, Affichage, Outils, Aide ;
- sous-menus avec raccourcis ;
- barre d'outils Select / Move / Delete / zoom / play ;
- onglets Overview, Data, Preview ;
- panneau gauche ForceAtlas ;
- panneau droit Statistiques / Sélection / Source ;
- barre de statut en bas.

Raccourcis :

```text
Ctrl+O : ouvrir un fichier CSV/DOT
Ctrl+S : enregistrer le projet JSON
Ctrl+Z : annuler
Ctrl+Y : rétablir
F11    : plein écran
```


## Correctif chargement WASM

Le moteur WASM est servi comme asset public via `/wasm/graph-engine.js` et `/wasm/graph-engine.wasm`, ce qui évite les blocages de bundling Worker/Vite en production Docker.

## Note sur la limite web

L'application web limite volontairement les imports à **1 000 nœuds**.
Cette limite évite de transformer la version navigateur en outil lourd côté client et garantit une expérience fluide pour la démonstration, l'exploration et l'export.

Si un fichier dépasse cette taille, l'application affiche une pop-up et conseille d'importer un autre fichier, de filtrer le graphe ou de créer un échantillon.

## Rendu conseillé

Pour le rendu le plus propre :

1. Charger un graphe inférieur ou égal à la limite active. La démo reste plafonnée à 400 nœuds.
2. Ouvrir la catégorie **Caméra / Zoom** dans le panneau gauche.
3. Cliquer sur **2D simple ou 3D réelle**.
4. Ajuster dans **Arêtes** : couleur, épaisseur, douceur AA et halo discret.
5. Ajuster dans **Nœuds** : taille, coloration et labels.

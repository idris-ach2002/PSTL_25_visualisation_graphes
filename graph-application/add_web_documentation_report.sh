#!/usr/bin/env bash
set -euo pipefail

# add_web_documentation_report.sh
# Génère une documentation détaillée, type rapport technique complet,
# pour la version web-vision de MonGraphe Web.
#
# À lancer depuis la racine du sous-projet graph-application/.

if [ ! -f "docker-compose.yml" ] || [ ! -d "frontend" ] || [ ! -d "wasm-engine" ]; then
  echo "Erreur : lance ce script depuis la racine du projet graph-application."
  echo "On doit y trouver docker-compose.yml, frontend/ et wasm-engine/."
  exit 1
fi

mkdir -p docs

cat > README.md <<'EOF'
# MonGraphe Web

**MonGraphe Web** est une application web de visualisation interactive de graphes, issue de la migration d'une application desktop JavaFX / JOGL / JNI vers une architecture web moderne fondée sur **React**, **TypeScript**, **WebGL**, **WebAssembly** et **Docker**.

Cette version web vise un objectif précis : proposer une application accessible depuis le navigateur, simple à lancer, fluide, visuellement lisible et suffisamment riche pour explorer des graphes dans un cadre pédagogique, expérimental ou démonstratif.

---

## Résumé du projet

L'application permet de :

- importer des graphes depuis des fichiers **CSV**, **DOT** ou edge-list ;
- analyser les fichiers avant import ;
- contrôler une limite de nœuds paramétrable ;
- visualiser les graphes en **2D simple** ou en **3D orbitale** ;
- explorer les nœuds, arêtes, degrés, communautés et composantes ;
- filtrer les graphes par degré, poids ou communauté ;
- sélectionner des éléments et afficher leurs informations ;
- sauvegarder un projet au format JSON ;
- exporter des rendus PNG / SVG ;
- consulter une documentation intégrée depuis l'interface ;
- lancer l'application sans dépendance locale complexe grâce à Docker.

---

## Philosophie de la version web

La version web ne cherche pas à reproduire exactement les capacités d'un outil desktop lourd sur des graphes massifs.  
Elle assume une limite contrôlée du nombre de nœuds afin de privilégier :

- la fluidité dans le navigateur ;
- la qualité de rendu ;
- la lisibilité ;
- la facilité d'utilisation ;
- l'interactivité ;
- la portabilité ;
- la simplicité de lancement.

Par défaut :

- l'import externe est limité à **1 000 nœuds** ;
- la démo générée est limitée à **400 nœuds** ;
- ces limites sont modifiables depuis l'application.

---

## Lancement rapide

Depuis la racine du projet :

```bash
./run_app.sh fresh
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

---

## Structure simplifiée

```text
graph-application/
├── frontend/              # Application React + TypeScript + WebGL
├── wasm-engine/           # Moteur C compilé en WebAssembly
├── legacy/                # Ancien moteur natif conservé comme référence
├── docs/                  # Documentation détaillée
├── scripts/               # Scripts utilitaires
├── Dockerfile             # Build production
├── Dockerfile.dev         # Build développement
├── docker-compose.yml     # Orchestration Docker
├── nginx.conf             # Serveur Nginx production
└── run_app.sh             # Script principal de lancement
```

---

## Documentation détaillée

### Rapport complet

- [Rapport technique complet](docs/REPORT.md)

### Utilisation

- [Guide d'installation](docs/INSTALLATION.md)
- [Guide utilisateur détaillé](docs/USER_GUIDE.md)
- [Import, formats et limites web](docs/IMPORT_AND_LIMITS.md)
- [Fonctionnalités détaillées](docs/FEATURES.md)
- [Guide de dépannage](docs/TROUBLESHOOTING.md)

### Technique

- [Architecture détaillée](docs/ARCHITECTURE.md)
- [Moteur WebAssembly](docs/WASM_ENGINE.md)
- [Rendu WebGL, 2D et 3D orbitale](docs/RENDERING.md)
- [Développement local](docs/DEVELOPMENT.md)
- [Docker et déploiement](docs/DOCKER.md)

### Projet

- [Workflow Git](docs/GIT_WORKFLOW.md)
- [Feuille de route](docs/ROADMAP.md)
- [Index de la documentation](docs/README.md)

---

## Technologies

| Domaine | Technologies |
|---|---|
| Interface | React, TypeScript, Vite |
| Rendu | Canvas WebGL |
| Calcul | C, WebAssembly, Web Worker |
| Packaging | Docker, Docker Compose |
| Production | Nginx |
| Documentation | Markdown |

---

## Commandes principales

```bash
# Build propre + lancement production
./run_app.sh fresh

# Lancement production normal
./run_app.sh prod

# Mode développement
./run_app.sh dev

# Arrêt des conteneurs
./run_app.sh down

# Logs Docker
./run_app.sh logs

# Nettoyage
./run_app.sh clean
```

---

## Statut

Cette branche correspond à la version **web-vision**.  
Elle remplace l'application desktop par une application web complète, tout en conservant une partie de l'ancien moteur natif dans `legacy/` pour garder une trace de la base technique initiale.
EOF

cat > docs/README.md <<'EOF'
# Index de la documentation

Cette documentation est organisée comme un dossier technique complet.  
Elle couvre à la fois l'utilisation, l'architecture, les choix de migration, les limites, le rendu, le moteur, Docker et la maintenance Git.

---

## Lecture recommandée

Pour comprendre rapidement le projet :

1. [README principal](../README.md)
2. [Rapport technique complet](REPORT.md)
3. [Guide d'installation](INSTALLATION.md)
4. [Guide utilisateur](USER_GUIDE.md)
5. [Architecture](ARCHITECTURE.md)

---

## Documentation utilisateur

| Fichier | Rôle |
|---|---|
| [INSTALLATION.md](INSTALLATION.md) | Installer et lancer l'application |
| [USER_GUIDE.md](USER_GUIDE.md) | Utiliser l'interface |
| [IMPORT_AND_LIMITS.md](IMPORT_AND_LIMITS.md) | Comprendre l'import et les limites |
| [FEATURES.md](FEATURES.md) | Découvrir les fonctionnalités |
| [TROUBLESHOOTING.md](TROUBLESHOOTING.md) | Résoudre les problèmes fréquents |

---

## Documentation technique

| Fichier | Rôle |
|---|---|
| [ARCHITECTURE.md](ARCHITECTURE.md) | Architecture globale |
| [WASM_ENGINE.md](WASM_ENGINE.md) | Moteur C / WebAssembly |
| [RENDERING.md](RENDERING.md) | Rendu WebGL, 2D, 3D |
| [DEVELOPMENT.md](DEVELOPMENT.md) | Développement local |
| [DOCKER.md](DOCKER.md) | Docker, Nginx, production |

---

## Documentation projet

| Fichier | Rôle |
|---|---|
| [GIT_WORKFLOW.md](GIT_WORKFLOW.md) | Commits et workflow |
| [ROADMAP.md](ROADMAP.md) | Évolutions possibles |
| [REPORT.md](REPORT.md) | Rapport détaillé complet |

---

## Objectif de séparation

La documentation est volontairement séparée afin de limiter les fichiers trop génériques.

Chaque fichier correspond à une responsabilité :

- comprendre le projet ;
- installer ;
- utiliser ;
- importer ;
- développer ;
- maintenir ;
- déployer ;
- dépanner ;
- expliquer les choix techniques.
EOF

cat > docs/REPORT.md <<'EOF'
# Rapport technique complet — MonGraphe Web

## Table des matières

1. [Introduction](#1-introduction)
2. [Contexte du projet](#2-contexte-du-projet)
3. [Objectifs de la migration web](#3-objectifs-de-la-migration-web)
4. [Différence entre l'application desktop et l'application web](#4-différence-entre-lapplication-desktop-et-lapplication-web)
5. [Architecture générale](#5-architecture-générale)
6. [Frontend React / TypeScript](#6-frontend-react--typescript)
7. [Moteur WebAssembly](#7-moteur-webassembly)
8. [Rendu WebGL](#8-rendu-webgl)
9. [Mode 2D et mode 3D orbitale](#9-mode-2d-et-mode-3d-orbitale)
10. [Import des graphes](#10-import-des-graphes)
11. [Limite de nœuds et stratégie web](#11-limite-de-nœuds-et-stratégie-web)
12. [Analyse du graphe](#12-analyse-du-graphe)
13. [Interactions utilisateur](#13-interactions-utilisateur)
14. [Filtres, sélection et exploration](#14-filtres-sélection-et-exploration)
15. [Exports et sauvegarde projet](#15-exports-et-sauvegarde-projet)
16. [Dockerisation](#16-dockerisation)
17. [Documentation intégrée](#17-documentation-intégrée)
18. [Sécurité, robustesse et limites](#18-sécurité-robustesse-et-limites)
19. [Dépannage](#19-dépannage)
20. [Évolutions possibles](#20-évolutions-possibles)
21. [Conclusion](#21-conclusion)

---

# 1. Introduction

MonGraphe Web est une application de visualisation interactive de graphes exécutée dans le navigateur.  
Elle résulte d'une transformation importante d'un projet initialement conçu comme application desktop JavaFX, utilisant un rendu OpenGL via JOGL et un moteur natif appelé depuis Java par JNI.

La migration web répond à plusieurs objectifs :

- rendre l'application plus accessible ;
- supprimer la dépendance à un environnement JavaFX local ;
- simplifier le lancement avec Docker ;
- proposer une interface plus moderne ;
- permettre l'exécution depuis un navigateur ;
- conserver une partie de la logique de calcul dans un moteur bas niveau ;
- exploiter le GPU côté client avec WebGL ;
- offrir une expérience proche d'une application desktop tout en restant web.

Le projet conserve l'esprit de l'application initiale : charger un graphe, le spatialiser, l'explorer, filtrer ses données et produire une visualisation exploitable.

---

# 2. Contexte du projet

Le projet original était structuré autour de deux grands blocs :

```text
graph-ui      → interface JavaFX, contrôleurs, rendu JOGL
graph-native  → moteur C, calculs, similarité, spatialisation
```

Cette architecture avait plusieurs avantages :

- séparation partielle entre interface et moteur ;
- utilisation d'un moteur C performant ;
- rendu accéléré avec OpenGL ;
- compatibilité avec CSV et DOT ;
- fonctionnalités d'export.

Cependant, elle présentait aussi plusieurs limites pratiques :

- installation plus lourde ;
- dépendance à Java, Maven, JavaFX, JOGL et JNI ;
- difficultés liées aux bibliothèques natives ;
- portabilité plus fragile ;
- lancement complexe sur certaines machines ;
- intégration OpenGL / JavaFX délicate ;
- maintenance difficile pour certains utilisateurs non spécialistes.

La branche `web-vision` propose une nouvelle orientation : transformer le projet en application web autonome, dockerisée et utilisable directement dans le navigateur.

---

# 3. Objectifs de la migration web

La migration ne consiste pas seulement à changer l'interface.  
Elle modifie profondément le modèle d'exécution.

## 3.1 Objectifs fonctionnels

L'application web doit permettre de :

- charger un fichier de graphe ;
- analyser le fichier avant l'import ;
- prévenir l'utilisateur si le graphe est trop volumineux ;
- visualiser le graphe en 2D ;
- explorer le graphe en 3D orbitale ;
- afficher les informations des nœuds et arêtes ;
- filtrer les éléments visibles ;
- calculer et afficher des statistiques ;
- exporter une visualisation ;
- sauvegarder un projet ;
- guider l'utilisateur avec une documentation intégrée.

## 3.2 Objectifs techniques

La migration vise aussi à :

- supprimer la dépendance à JavaFX ;
- conserver une logique de calcul performante ;
- utiliser WebAssembly pour le moteur ;
- utiliser WebGL pour le rendu ;
- isoler les calculs dans un Web Worker ;
- réduire les blocages du thread principal ;
- simplifier le build avec Docker ;
- fournir une architecture lisible et maintenable.

## 3.3 Objectifs UX

La version web doit être plus accessible pour un utilisateur non technique :

- interface proche d'une application desktop ;
- menus et onglets ;
- panneaux de paramètres par catégorie ;
- messages d'erreur lisibles ;
- import guidé ;
- limite paramétrable ;
- aide intégrée ;
- actions explicites.

---

# 4. Différence entre l'application desktop et l'application web

## 4.1 Version desktop

La version desktop utilisait :

```text
JavaFX
JOGL / OpenGL
JNI
Moteur C natif
Maven
Bibliothèques natives
```

Elle était adaptée à une exécution locale, mais demandait un environnement correctement configuré.

## 4.2 Version web

La version web utilise :

```text
React
TypeScript
WebGL
WebAssembly
Web Worker
Docker
Nginx
```

Elle s'exécute dans le navigateur et délègue :

- l'interface à React ;
- le rendu à WebGL ;
- les calculs lourds à WebAssembly ;
- l'isolation du calcul à un worker ;
- le packaging à Docker.

## 4.3 Changement de stratégie

La version desktop peut viser des graphes plus grands, selon la machine et l'optimisation native.  
La version web assume une limite plus stricte, car l'objectif est une visualisation fluide et lisible dans un navigateur.

Le choix n'est donc pas seulement technique : c'est un choix d'expérience utilisateur.

---

# 5. Architecture générale

L'architecture peut être représentée ainsi :

```text
Utilisateur
   ↓
Interface React / TypeScript
   ↓
Composants UI : menus, panneaux, onglets
   ↓
Web Worker
   ↓
Moteur C compilé en WebAssembly
   ↓
Données typées : positions, arêtes, communautés
   ↓
Renderer WebGL
   ↓
Canvas 2D / 3D orbitale
```

## 5.1 Organisation des dossiers

```text
graph-application/
├── frontend/
│   ├── src/
│   │   ├── App.tsx
│   │   ├── components/
│   │   ├── engine/
│   │   ├── rendering/
│   │   ├── types/
│   │   └── main.tsx
│   ├── public/
│   └── package.json
├── wasm-engine/
│   ├── Makefile
│   └── src/
│       └── graph_engine.c
├── legacy/
│   └── graph-native-original/
├── docs/
├── scripts/
├── Dockerfile
├── Dockerfile.dev
├── docker-compose.yml
├── nginx.conf
└── run_app.sh
```

## 5.2 Responsabilités

| Bloc | Responsabilité |
|---|---|
| `frontend/src/App.tsx` | Orchestration générale de l'application |
| `components/` | Interface utilisateur |
| `engine/GraphParser.ts` | Analyse et parsing des fichiers |
| `engine/graph.worker.ts` | Communication avec le moteur WASM |
| `rendering/GraphRenderer.ts` | Rendu WebGL |
| `wasm-engine/src/graph_engine.c` | Simulation et données graphe |
| `docs/` | Documentation Markdown |
| `legacy/` | Référence de l'ancien moteur natif |

---

# 6. Frontend React / TypeScript

Le frontend est le cœur visible de l'application.

Il gère :

- les menus ;
- les onglets ;
- les panneaux latéraux ;
- les interactions utilisateur ;
- la configuration ;
- les filtres ;
- les imports ;
- les exports ;
- l'aide intégrée.

## 6.1 Pourquoi React ?

React facilite la construction d'une interface modulaire.

Chaque partie de l'interface peut être isolée dans un composant :

```text
Toolbar
DataPanel
StatsPanel
GraphCanvas
HelpPanel
ImportAssistantDialog
LimitDialog
```

Cette séparation rend l'application plus maintenable.

## 6.2 Pourquoi TypeScript ?

TypeScript apporte :

- une meilleure robustesse ;
- une documentation implicite par les types ;
- une réduction des erreurs lors des refactorisations ;
- une meilleure lisibilité des structures de données.

Pour une application manipulant des nœuds, arêtes, statistiques, états de sélection et paramètres visuels, le typage est utile.

## 6.3 Composants principaux

### App.tsx

`App.tsx` orchestre :

- l'état global ;
- le graphe courant ;
- la limite de nœuds ;
- les filtres ;
- les layouts ;
- les exports ;
- les actions ;
- les panneaux.

### GraphCanvas.tsx

Ce composant contient le canvas WebGL.

Il transmet au renderer :

- les données du graphe ;
- les paramètres de caméra ;
- les paramètres de rendu ;
- les états de survol ;
- les états de sélection.

### DataPanel.tsx

Affiche les données sous forme tabulaire.

Il sert à consulter :

- les nœuds ;
- les arêtes ;
- les degrés ;
- les communautés.

### StatsPanel.tsx

Affiche l'analyse du graphe :

- nombre de nœuds ;
- nombre d'arêtes ;
- densité ;
- degré moyen ;
- hub ;
- composantes ;
- communautés.

### HelpPanel.tsx

Fournit une documentation intégrée directement dans l'application.

---

# 7. Moteur WebAssembly

Le moteur WebAssembly remplace le modèle JNI de l'ancienne application.

## 7.1 Rôle

Le moteur C gère :

- la structure interne du graphe ;
- les positions ;
- les arêtes ;
- les degrés ;
- les couleurs ;
- les communautés ;
- les nœuds supprimés ;
- la simulation ;
- certains traitements algorithmiques.

## 7.2 Pourquoi conserver du C ?

Le C est adapté à :

- la manipulation de tableaux continus ;
- les calculs répétitifs ;
- le contrôle mémoire ;
- l'exécution en WebAssembly ;
- les échanges avec WebGL via des buffers.

## 7.3 Compilation

La compilation se fait avec Emscripten.

Dans Docker :

```text
C source → emcc → graph-engine.js + graph-engine.wasm
```

Le moteur est ensuite chargé depuis le worker.

## 7.4 Worker

Le Web Worker empêche l'interface de se bloquer pendant les calculs.

Sans worker, une simulation de graphe pourrait rendre l'interface moins réactive.

Le worker reçoit des messages :

```text
init
loadGraph
step
setParams
deleteNode
restoreNode
```

Puis renvoie les résultats au thread principal.

---

# 8. Rendu WebGL

Le rendu WebGL est responsable de l'affichage du graphe.

## 8.1 Pourquoi WebGL ?

Un graphe peut contenir beaucoup d'éléments visuels :

- centaines de nœuds ;
- centaines ou milliers d'arêtes ;
- labels ;
- surbrillances ;
- infobulles ;
- mini-map.

Il serait coûteux de dessiner chaque élément comme un composant HTML.

WebGL permet de dessiner directement sur le GPU.

## 8.2 Pipeline de rendu

Le pipeline suit cette logique :

```text
Données du graphe
   ↓
Préparation des buffers
   ↓
Projection caméra
   ↓
Dessin des arêtes
   ↓
Dessin des nœuds
   ↓
Surbrillance
   ↓
Overlay UI
```

## 8.3 Arêtes

Les arêtes sont rendues simplement et clairement.

La stratégie actuelle privilégie :

- couleur claire ;
- bonne lisibilité sur fond sombre ;
- épaisseur contrôlée ;
- surbrillance au hover ou à la sélection ;
- absence d'effet trop lourd.

## 8.4 Nœuds

Les nœuds doivent rester visibles et lisibles.

Ils peuvent être colorés selon :

- leur communauté ;
- leur degré ;
- leur état de sélection ;
- leur état de survol.

---

# 9. Mode 2D et mode 3D orbitale

## 9.1 Mode 2D simple

Le mode 2D est le mode par défaut.

Il est recommandé pour :

- l'analyse ;
- la sélection ;
- les filtres ;
- les exports ;
- les graphes denses ;
- les utilisateurs non spécialistes.

## 9.2 Mode 3D orbitale

Le mode 3D permet de voir le graphe depuis plusieurs angles.

Contrairement à une simple projection 2.5D, la caméra peut orbiter autour du graphe.

Contrôles :

| Action | Contrôle |
|---|---|
| Orbiter | glisser la souris |
| Déplacer | Maj + glisser |
| Zoomer | molette |
| Vue face | bouton Face |
| Vue côté | bouton Côté |
| Vue isométrique | bouton Iso |

## 9.3 Intérêt de la 3D

La 3D est utile pour :

- explorer une structure complexe ;
- comprendre la profondeur ;
- présenter le graphe de manière plus dynamique ;
- analyser certaines séparations spatiales.

Cependant, la 3D peut aussi compliquer la lecture.  
C'est pourquoi la 2D reste le mode par défaut.

---

# 10. Import des graphes

L'import est une étape sensible.

Un mauvais import peut produire :

- trop de nœuds ;
- trop d'arêtes ;
- un graphe illisible ;
- une interface ralentie.

## 10.1 Assistant d'import

L'assistant analyse le fichier avant chargement.

Il peut afficher :

- format détecté ;
- nombre de lignes ;
- nombre de colonnes ;
- nombre de nœuds estimé ;
- nombre d'arêtes estimé ;
- compatibilité avec la limite ;
- recommandations.

## 10.2 CSV

Dans un CSV, chaque ligne peut devenir un nœud.

Les arêtes peuvent être construites à partir de similarités.

## 10.3 DOT

Dans un fichier DOT, le graphe est déjà décrit.

L'application extrait :

- les nœuds ;
- les arêtes ;
- les labels possibles.

## 10.4 Edge-list

Une edge-list décrit une arête par ligne.

Exemple :

```text
A B
B C
C D
```

Chaque identifiant distinct devient un nœud.

---

# 11. Limite de nœuds et stratégie web

## 11.1 Limite par défaut

La limite par défaut est :

```text
1 000 nœuds pour les imports externes
400 nœuds pour la démo générée
```

## 11.2 Pourquoi limiter ?

Le navigateur n'est pas le même environnement qu'une application desktop native.

Même si WebGL et WebAssembly sont performants, il faut prendre en compte :

- le thread principal ;
- la mémoire navigateur ;
- les variations de GPU ;
- les laptops peu puissants ;
- les navigateurs différents ;
- les interactions temps réel ;
- le mode 3D.

La limite évite de proposer une expérience instable.

## 11.3 Limite paramétrable

La limite est modifiable dans l'application.

Cela permet d'adapter l'expérience :

- à une machine puissante ;
- à un fichier plus simple ;
- à une démonstration ;
- à un besoin pédagogique.

## 11.4 Échantillonnage

Si le fichier dépasse la limite, plusieurs stratégies peuvent être proposées :

- premiers nœuds ;
- échantillon aléatoire ;
- nœuds les plus connectés ;
- annulation.

---

# 12. Analyse du graphe

L'application calcule des statistiques pour aider à comprendre le graphe.

## 12.1 Statistiques principales

- nombre de nœuds ;
- nombre d'arêtes ;
- densité ;
- degré moyen ;
- degré maximal ;
- hub principal ;
- communautés ;
- composantes connexes.

## 12.2 Densité

La densité donne une idée de la quantité d'arêtes par rapport au maximum possible.

Un graphe dense peut devenir difficile à lire.

## 12.3 Degré

Le degré indique le nombre de relations d'un nœud.

Un nœud de fort degré peut être un hub.

## 12.4 Communautés

Les communautés aident à repérer des groupes de nœuds plus fortement liés.

---

# 13. Interactions utilisateur

## 13.1 Survol

Au survol d'un nœud ou d'une arête, l'application affiche une infobulle.

Pour un nœud :

- ID ;
- label ;
- degré ;
- communauté ;
- position ;
- coordonnées 3D si disponibles.

Pour une arête :

- source ;
- cible ;
- poids ;
- index.

## 13.2 Sélection

La sélection permet d'afficher des informations persistantes dans le panneau latéral.

Actions possibles :

- centrer la caméra ;
- isoler les voisins ;
- filtrer la communauté ;
- copier les informations ;
- supprimer ou restaurer un nœud.

## 13.3 Recherche

La recherche permet de retrouver rapidement un nœud par ID ou label.

Une bonne recherche doit :

- filtrer les résultats ;
- centrer la caméra ;
- mettre le nœud en évidence ;
- afficher ses voisins.

---

# 14. Filtres, sélection et exploration

## 14.1 Filtres disponibles

- degré minimum ;
- poids minimum ;
- communauté ;
- nœuds isolés ;
- focus sélection.

## 14.2 Objectif des filtres

Les filtres ne servent pas seulement à masquer des données.  
Ils servent à rendre une structure plus lisible.

Un graphe complet peut être difficile à interpréter.  
Un graphe filtré peut révéler une organisation.

## 14.3 Focus sélection

Le focus sur un nœud permet de réduire temporairement la vue aux éléments liés.

C'est utile pour analyser localement le graphe.

## 14.4 Layouts

Les layouts disponibles permettent de changer la lecture :

- ForceAtlas ;
- circulaire ;
- grille ;
- communautés ;
- radial.

Chaque layout correspond à une intention différente.

---

# 15. Exports et sauvegarde projet

## 15.1 Export PNG

L'export PNG produit une image raster.

Il est utile pour :

- un rapport ;
- une présentation ;
- une capture rapide ;
- une démonstration.

## 15.2 Export SVG

L'export SVG produit une image vectorielle.

Il est utile pour :

- les documents ;
- l'impression ;
- la modification dans un logiciel vectoriel ;
- l'agrandissement sans perte.

## 15.3 Projet JSON

Le projet JSON sauvegarde l'état de travail.

Il peut contenir :

- nœuds ;
- arêtes ;
- positions ;
- communautés ;
- caméra ;
- filtres ;
- paramètres ;
- mode de rendu.

Cela permet de reprendre une analyse sans tout recommencer.

---

# 16. Dockerisation

Docker simplifie fortement le lancement.

## 16.1 Sans Docker

Il faudrait installer :

- Node.js ;
- npm ;
- Emscripten ;
- un serveur statique ;
- potentiellement différentes dépendances système.

## 16.2 Avec Docker

La commande :

```bash
./run_app.sh fresh
```

s'occupe de tout.

## 16.3 Build multi-stage

Le Dockerfile est organisé en plusieurs étapes :

1. compilation WebAssembly ;
2. compilation frontend ;
3. serveur Nginx.

Ce modèle réduit la complexité pour l'utilisateur final.

---

# 17. Documentation intégrée

La documentation existe sous deux formes :

- documentation Markdown dans `docs/` ;
- onglet Help / Doc dans l'application.

## 17.1 Documentation Markdown

Elle sert aux développeurs, enseignants, évaluateurs et contributeurs.

## 17.2 Documentation intégrée

Elle sert à l'utilisateur final.

Elle permet d'apprendre l'application sans quitter l'interface.

---

# 18. Sécurité, robustesse et limites

## 18.1 Pas d'exécution serveur des fichiers utilisateur

L'import se fait côté navigateur.

Cela évite d'envoyer les fichiers à un backend externe.

## 18.2 Limite de taille

La limite protège l'utilisateur contre les fichiers trop lourds.

## 18.3 WebGL

WebGL dépend du navigateur et du GPU.

Un rendu peut varier selon :

- Chrome ;
- Firefox ;
- pilotes graphiques ;
- accélération matérielle ;
- machine utilisée.

## 18.4 Limites assumées

L'application ne vise pas :

- les millions de nœuds ;
- l'analyse distribuée ;
- le stockage cloud ;
- la collaboration temps réel ;
- le remplacement complet d'un outil desktop spécialisé.

---

# 19. Dépannage

## 19.1 Port occupé

Erreur :

```text
Bind for 0.0.0.0:8080 failed
```

Solution :

```bash
docker ps
docker stop <id>
```

## 19.2 Cache navigateur

Si l'interface semble ancienne :

```bash
./run_app.sh fresh
```

Puis :

```text
Ctrl + F5
```

## 19.3 Erreur WebGL

Tester :

- autre navigateur ;
- accélération matérielle ;
- mode 2D ;
- limite plus basse.

## 19.4 Fichier trop grand

Réduire le fichier ou utiliser l'échantillonnage.

---

# 20. Évolutions possibles

## 20.1 Court terme

- améliorer les messages d'erreur ;
- ajouter plus d'exemples ;
- documenter le format JSON projet ;
- améliorer les raccourcis clavier ;
- améliorer le picking en 3D.

## 20.2 Moyen terme

- ajouter des palettes ;
- ajouter un mode comparaison ;
- ajouter plusieurs graphes ouverts ;
- améliorer l'export haute résolution ;
- ajouter un panneau de performance.

## 20.3 Long terme

- backend optionnel ;
- sauvegarde cloud ;
- partage de graphe ;
- collaboration ;
- WebGPU expérimental.

---

# 21. Conclusion

MonGraphe Web transforme une application desktop spécialisée en application web moderne.

La migration introduit :

- une interface plus accessible ;
- un lancement simplifié ;
- une architecture React / TypeScript ;
- un moteur WebAssembly ;
- un rendu WebGL ;
- une limite web contrôlée ;
- une documentation détaillée ;
- une expérience utilisateur plus guidée.

Le projet ne remplace pas exactement l'application desktop d'origine.  
Il propose une nouvelle orientation : une application web interactive, portable, lisible et adaptée à des graphes de taille maîtrisée.

Cette stratégie permet de valoriser le travail initial tout en ouvrant la voie à une application plus simple à distribuer, à présenter et à maintenir.
EOF

cat > docs/INSTALLATION.md <<'EOF'
# Guide d'installation détaillé

## 1. Objectif

Ce document explique comment installer, lancer et vérifier MonGraphe Web.

L'objectif est de permettre à un utilisateur de lancer l'application sans installer manuellement les dépendances de compilation comme Node.js, Emscripten ou Nginx.

La méthode recommandée repose sur Docker.

---

## 2. Prérequis

### 2.1 Docker

Docker doit être installé.

Vérification :

```bash
docker --version
```

### 2.2 Docker Compose

Vérification :

```bash
docker compose version
```

### 2.3 Navigateur

Utiliser un navigateur moderne :

- Chrome ;
- Chromium ;
- Firefox ;
- Edge.

Le navigateur doit supporter WebGL.

---

## 3. Lancement production

Depuis la racine du projet :

```bash
./run_app.sh fresh
```

Cette commande effectue un lancement propre.

Elle est recommandée après :

- un changement de code ;
- une nouvelle extraction du ZIP ;
- une mise à jour du projet ;
- un problème de cache.

---

## 4. Déroulement du build

Le build production se déroule en plusieurs étapes :

```text
1. Compilation du moteur C avec Emscripten
2. Génération du WebAssembly
3. Installation des dépendances frontend
4. Build Vite / React
5. Copie du build dans Nginx
6. Exposition sur le port 8080
```

---

## 5. Accès à l'application

Après lancement :

```text
http://localhost:8080
```

---

## 6. Mode développement

Pour modifier l'application :

```bash
./run_app.sh dev
```

Puis :

```text
http://localhost:5173
```

Le mode développement utilise Vite.

---

## 7. Arrêter l'application

```bash
./run_app.sh down
```

---

## 8. Nettoyer

```bash
./run_app.sh clean
```

---

## 9. Problèmes fréquents

### Port occupé

Si le port 8080 est déjà utilisé :

```bash
docker ps
```

Puis :

```bash
docker stop <id_du_conteneur>
```

### Ancienne version visible

Faire :

```bash
./run_app.sh fresh
```

Puis recharger avec :

```text
Ctrl + F5
```

### Docker trop lent

Le premier build est plus long car Docker télécharge les images :

- Node ;
- Emscripten ;
- Nginx.

Les builds suivants sont plus rapides grâce au cache Docker.
EOF

cat > docs/USER_GUIDE.md <<'EOF'
# Guide utilisateur détaillé

## 1. Présentation de l'interface

MonGraphe Web propose une interface proche d'une application desktop.

Elle contient :

- une barre de menus ;
- une barre d'outils ;
- des onglets ;
- un panneau de paramètres ;
- un canvas de visualisation ;
- un panneau d'analyse ;
- une barre de statut.

---

## 2. Logique générale

Le flux d'utilisation recommandé est :

```text
Importer ou générer un graphe
        ↓
Analyser les statistiques
        ↓
Choisir un layout
        ↓
Explorer le graphe
        ↓
Filtrer ou sélectionner
        ↓
Exporter ou sauvegarder
```

---

## 3. Panneau Projet

Le panneau Projet contient :

- import fichier ;
- génération de démo ;
- limite maximale de nœuds ;
- sauvegarde projet ;
- chargement projet.

C'est le point d'entrée principal.

---

## 4. Panneau Construction

Il contient les paramètres liés à la création du graphe depuis un CSV :

- mesure de similarité ;
- seuils ;
- création d'arêtes ;
- création éventuelle d'anti-arêtes ;
- stratégie de construction.

---

## 5. Panneau Simulation

Il contient les paramètres de layout :

- vitesse ;
- répulsion ;
- attraction ;
- nombre d'itérations ;
- K-Means ;
- communautés.

---

## 6. Panneau Nœuds

Il permet de contrôler :

- taille des nœuds ;
- visibilité ;
- labels ;
- couleur ;
- sélection ;
- survol.

---

## 7. Panneau Arêtes

Il permet de contrôler :

- couleur des arêtes ;
- épaisseur ;
- opacité ;
- poids minimum ;
- style simple.

---

## 8. Panneau Caméra

Il permet de contrôler :

- zoom ;
- mode 2D ;
- mode 3D orbitale ;
- vue face ;
- vue côté ;
- vue isométrique ;
- recentrage.

---

## 9. Onglet Overview

C'est l'espace principal de visualisation.

On y manipule le graphe.

---

## 10. Onglet Data

Affiche les données du graphe sous forme tabulaire.

Utile pour vérifier les IDs, labels, degrés et communautés.

---

## 11. Onglet Preview

Permet de préparer une vue propre pour export.

---

## 12. Onglet Help / Doc

Explique l'application directement depuis l'interface.

---

## 13. Contrôles

### Mode 2D

| Action | Contrôle |
|---|---|
| Déplacer | glisser le fond |
| Zoomer | molette |
| Sélectionner | clic sur nœud |
| Voir infos | survol |
| Recentrer | bouton Fit |

### Mode 3D

| Action | Contrôle |
|---|---|
| Orbiter | glisser |
| Déplacer | Maj + glisser |
| Zoomer | molette |
| Vue face | bouton Face |
| Vue côté | bouton Côté |
| Vue iso | bouton Iso |

---

## 14. Sélection

Lorsqu'un nœud est sélectionné, le panneau latéral peut afficher :

- ID ;
- label ;
- degré ;
- communauté ;
- voisins ;
- position ;
- actions disponibles.

---

## 15. Conseils

Pour une bonne visualisation :

- rester sous 1 000 nœuds ;
- utiliser la 2D pour l'analyse ;
- utiliser la 3D pour la présentation ;
- filtrer les nœuds isolés ;
- utiliser les communautés ;
- augmenter le seuil pour réduire les arêtes trop nombreuses.
EOF

cat > docs/IMPORT_AND_LIMITS.md <<'EOF'
# Import, formats et limites web

## 1. Rôle de l'import

L'import transforme un fichier utilisateur en graphe exploitable par l'application.

Cette étape est critique car elle détermine :

- le nombre de nœuds ;
- le nombre d'arêtes ;
- la densité ;
- la lisibilité ;
- la performance.

---

## 2. Formats

### CSV

Chaque ligne peut être interprétée comme une entité.

Exemple :

```csv
id,x,y,z
A,1.0,2.0,3.0
B,1.1,2.1,2.9
C,8.0,3.0,1.0
```

### DOT

Exemple :

```dot
graph G {
  A -- B;
  B -- C;
  C -- A;
}
```

### Edge-list

Exemple :

```text
A B
B C
C D
```

---

## 3. Assistant d'import

Avant de charger réellement le graphe, l'application analyse :

- le format ;
- les lignes ;
- les colonnes ;
- les nœuds estimés ;
- les arêtes estimées ;
- le respect de la limite.

---

## 4. Limite par défaut

```text
Import externe : 1 000 nœuds
Démo générée : 400 nœuds
```

---

## 5. Pourquoi limiter ?

Un graphe trop grand peut provoquer :

- ralentissements ;
- illisibilité ;
- surconsommation mémoire ;
- interaction difficile ;
- perte d'intérêt visuel.

La version web privilégie une visualisation de qualité.

---

## 6. Limite paramétrable

La limite est modifiable dans le panneau Projet.

Cela permet d'adapter l'application au contexte.

Valeurs recommandées :

| Usage | Limite conseillée |
|---|---|
| Démo rapide | 150 à 300 |
| Présentation fluide | 300 à 500 |
| Analyse web normale | 500 à 1 000 |
| Machine puissante | plus de 1 000 avec prudence |

---

## 7. Échantillonnage

Si un graphe dépasse la limite, plusieurs choix sont possibles :

- premiers nœuds ;
- échantillon aléatoire ;
- nœuds les plus connectés ;
- annulation.

---

## 8. Recommandations CSV

Pour un CSV exploitable :

- nettoyer les colonnes inutiles ;
- garder les colonnes numériques pertinentes ;
- éviter les valeurs manquantes ;
- normaliser les données ;
- réduire les doublons.

---

## 9. Recommandations DOT

Pour un DOT propre :

- vérifier les nœuds isolés ;
- éviter les attributs non nécessaires ;
- réduire les graphes trop denses ;
- tester d'abord un petit extrait.
EOF

cat > docs/FEATURES.md <<'EOF'
# Fonctionnalités détaillées

## 1. Import intelligent

L'application analyse les fichiers avant chargement.

Fonctions :

- détection format ;
- estimation nœuds ;
- estimation arêtes ;
- contrôle limite ;
- message utilisateur ;
- stratégie d'échantillonnage.

---

## 2. Démo générée

La démo permet de tester l'application sans fichier externe.

Elle est limitée à 400 nœuds pour garantir la fluidité.

---

## 3. Visualisation 2D

Mode par défaut.

Fonctions :

- zoom ;
- déplacement ;
- sélection ;
- survol ;
- labels ;
- mini-map ;
- export.

---

## 4. Visualisation 3D orbitale

Fonctions :

- orbite caméra ;
- vues prédéfinies ;
- profondeur réelle ;
- zoom ;
- survol ;
- sélection.

---

## 5. Analyse

Statistiques calculées :

- nœuds ;
- arêtes ;
- densité ;
- degré moyen ;
- hub ;
- communautés ;
- composantes.

---

## 6. Filtres

Filtres :

- degré minimum ;
- poids minimum ;
- communauté ;
- nœuds isolés ;
- focus voisinage.

---

## 7. Sélection

Actions :

- centrer ;
- isoler ;
- copier infos ;
- afficher voisins ;
- filtrer communauté.

---

## 8. Layouts

Layouts :

- ForceAtlas simplifié ;
- circulaire ;
- grille ;
- communautés ;
- radial.

---

## 9. Exports

Formats :

- PNG ;
- SVG ;
- JSON projet.

---

## 10. Documentation intégrée

Accessible dans l'onglet Help / Doc.

Elle permet d'apprendre l'application depuis l'interface.
EOF

cat > docs/ARCHITECTURE.md <<'EOF'
# Architecture détaillée

## 1. Vue globale

```text
React UI
  ↓
State App
  ↓
GraphCanvas
  ↓
GraphRenderer WebGL
  ↓
Web Worker
  ↓
WASM Engine
```

---

## 2. Frontend

Le frontend contient :

```text
frontend/src/
├── App.tsx
├── components/
├── engine/
├── rendering/
├── types/
└── main.tsx
```

---

## 3. Composants

| Composant | Rôle |
|---|---|
| App.tsx | État principal |
| GraphCanvas.tsx | Canvas et événements |
| GraphRenderer.ts | Rendu WebGL |
| DataPanel.tsx | Données tabulaires |
| StatsPanel.tsx | Analyse |
| Toolbar.tsx | Actions rapides |
| HelpPanel.tsx | Documentation intégrée |
| ImportAssistantDialog.tsx | Import guidé |
| LimitDialog.tsx | Avertissement limite |

---

## 4. Données

Les données principales sont :

- Node ;
- Edge ;
- GraphData ;
- GraphStats ;
- RenderOptions ;
- CameraState.

---

## 5. Communication worker

Le thread principal envoie les commandes.

Le worker exécute les calculs.

Le renderer affiche le dernier état disponible.

---

## 6. Pourquoi cette architecture ?

Elle sépare :

- interface ;
- calcul ;
- rendu ;
- parsing ;
- documentation ;
- configuration.

Cela rend le projet plus lisible et plus facile à faire évoluer.
EOF

cat > docs/WASM_ENGINE.md <<'EOF'
# Moteur WebAssembly détaillé

## 1. Rôle

Le moteur WebAssembly exécute la partie calculatoire du graphe.

Il est écrit en C et compilé avec Emscripten.

---

## 2. Fichiers

```text
wasm-engine/
├── Makefile
└── src/
    └── graph_engine.c
```

---

## 3. Données internes

Le moteur manipule des tableaux :

- positions ;
- couleurs ;
- arêtes ;
- degrés ;
- communautés ;
- poids ;
- suppression.

---

## 4. Fonctions exposées

Exemples :

- initialisation ;
- simulation ;
- récupération des positions ;
- suppression nœud ;
- restauration nœud ;
- propagation de labels ;
- K-Means spatial.

---

## 5. Intérêt du WASM

WASM permet :

- exécution dans le navigateur ;
- performance proche du natif pour certains calculs ;
- conservation d'une logique C ;
- intégration avec JavaScript ;
- isolation possible dans un worker.

---

## 6. Limites

Le moteur actuel est adapté à une application web interactive.

Il n'est pas encore un moteur de calcul distribué ou orienté graphes massifs.
EOF

cat > docs/RENDERING.md <<'EOF'
# Rendu WebGL détaillé

## 1. Objectif

Le rendu doit concilier :

- lisibilité ;
- fluidité ;
- précision ;
- compatibilité web ;
- interaction.

---

## 2. Mode 2D

Le mode 2D est prioritaire.

Il est plus lisible pour l'analyse.

---

## 3. Mode 3D orbitale

Le mode 3D permet une exploration spatiale.

Il utilise une vraie caméra orbitale.

---

## 4. Nœuds

Les nœuds sont dessinés dans le canvas.

Ils peuvent changer selon :

- sélection ;
- survol ;
- degré ;
- communauté ;
- filtre.

---

## 5. Arêtes

Les arêtes restent simples pour éviter le bruit visuel.

Elles doivent être :

- visibles ;
- fines ;
- claires ;
- non agressives ;
- renforcées au besoin.

---

## 6. Picking

Le picking permet de savoir quel élément est sous la souris.

Il sert aux infobulles et à la sélection.

---

## 7. Mini-map

La mini-map donne une vue globale.

Elle aide quand le zoom est élevé.

---

## 8. Performance

Pour maintenir la fluidité :

- limiter les allocations ;
- utiliser WebGL ;
- éviter les nœuds HTML ;
- plafonner la démo ;
- filtrer les graphes denses.
EOF

cat > docs/DEVELOPMENT.md <<'EOF'
# Développement local détaillé

## 1. Installer les dépendances

```bash
cd frontend
npm ci
```

---

## 2. Lancer en dev

```bash
./run_app.sh dev
```

---

## 3. Compiler

```bash
cd frontend
npm run build
```

---

## 4. Compiler le WASM

```bash
make -C wasm-engine wasm
```

---

## 5. Modifier le frontend

Les fichiers importants :

- `App.tsx` ;
- `components/` ;
- `GraphCanvas.tsx` ;
- `GraphRenderer.ts` ;
- `GraphParser.ts`.

---

## 6. Modifier le moteur

Fichier principal :

```text
wasm-engine/src/graph_engine.c
```

Après modification, reconstruire.

---

## 7. Bonnes pratiques

- tester en 2D et en 3D ;
- tester avec petit et moyen graphe ;
- vérifier l'import CSV ;
- vérifier l'import DOT ;
- vérifier la limite ;
- vérifier le build Docker.
EOF

cat > docs/DOCKER.md <<'EOF'
# Docker et déploiement détaillé

## 1. Rôle de Docker

Docker permet de lancer le projet sans installer manuellement les dépendances.

---

## 2. Build multi-stage

Étapes :

1. Emscripten ;
2. Node ;
3. Nginx.

---

## 3. Production

```bash
./run_app.sh fresh
```

---

## 4. Développement

```bash
./run_app.sh dev
```

---

## 5. Nginx

Nginx sert les fichiers compilés.

---

## 6. Changer le port

Dans `docker-compose.yml` :

```yaml
ports:
  - "8081:80"
```

---

## 7. Nettoyage

```bash
docker compose down --remove-orphans
docker system prune
```
EOF

cat > docs/TROUBLESHOOTING.md <<'EOF'
# Dépannage détaillé

## 1. Port occupé

```bash
docker ps
docker stop <id>
```

---

## 2. Page bloquée

```bash
./run_app.sh fresh
```

Puis `Ctrl + F5`.

---

## 3. WebGL indisponible

Tester :

- autre navigateur ;
- accélération matérielle ;
- mode 2D ;
- limite plus basse.

---

## 4. npm lent

Vérifier :

```bash
cat frontend/.npmrc
```

---

## 5. WASM non chargé

Vérifier dans l'onglet Network du navigateur que le `.wasm` est servi.

---

## 6. Import refusé

Réduire le graphe ou modifier la limite.

---

## 7. Docker cache une ancienne version

```bash
docker compose build --no-cache graph-web
```
EOF

cat > docs/GIT_WORKFLOW.md <<'EOF'
# Workflow Git détaillé

## 1. Branche

La migration web se fait sur :

```text
web-vision
```

---

## 2. Commits recommandés

Exemples :

```text
chore(infra): add Docker runtime and launch workflow
feat(wasm): add WebAssembly graph simulation engine
feat(frontend): scaffold Vite React application shell
feat(import): add parsers, samples and configurable node limits
feat(rendering): add WebGL renderer with 2D and orbit 3D
feat(analysis): add data, statistics and graph action panels
docs(help): add embedded user guide and examples
feat(app): integrate UX, filters, layouts and project workflow
docs: add detailed markdown documentation
```

---

## 3. Avant push

```bash
git status
./run_app.sh fresh
git log --oneline --decorate -n 15
```

---

## 4. Push

```bash
git push -u origin web-vision
```

---

## 5. Nettoyage

Éviter de pousser :

- scripts temporaires ;
- fichiers de build ;
- dossiers `node_modules` ;
- fichiers locaux non nécessaires.
EOF

cat > docs/ROADMAP.md <<'EOF'
# Feuille de route détaillée

## 1. Court terme

- améliorer le picking 3D ;
- ajouter plus de datasets exemples ;
- ajouter raccourcis clavier ;
- améliorer export SVG ;
- améliorer messages d'erreur.

---

## 2. Moyen terme

- mode comparaison 2D / 3D ;
- plusieurs graphes ouverts ;
- palettes avancées ;
- export haute résolution ;
- profil de performance.

---

## 3. Long terme

- backend optionnel ;
- sauvegarde cloud ;
- partage par lien ;
- collaboration ;
- WebGPU expérimental.

---

## 4. Idées avancées

- clustering plus avancé ;
- layouts supplémentaires ;
- moteur de recommandations visuelles ;
- storytelling de graphe ;
- mode présentation animé.

---

## 5. Limite stratégique

La priorité doit rester :

- lisibilité ;
- fluidité ;
- simplicité ;
- qualité d'expérience.
EOF

echo "Documentation détaillée générée."
echo
find docs -maxdepth 1 -type f -name "*.md" | sort
echo "README.md"
echo
echo "Commande de commit recommandée :"
echo "  git add README.md docs/"
echo "  git commit -m \"docs: add detailed markdown report and documentation\""

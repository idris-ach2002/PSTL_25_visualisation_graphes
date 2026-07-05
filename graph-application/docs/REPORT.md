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

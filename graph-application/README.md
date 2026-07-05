# PSTL_25_visualisation_graphes

Application de visualisation de graphes graphes exécutée dans le navigateur avec interface gérée par React, TypeScript, WebGL, WebAssembly, Web Worker, Docker, Nginx, rendu OpenGL et calculs natifs en C.

## À quoi sert le projet

L'application permet de :
- charger un graphe depuis un fichier CSV ou DOT ;
- visualiser le graphe de manière interactive ;
- appliquer des layouts et des algorithmes de communautés ;
- exporter des rendus graphiques.

Le projet est composé de deux parties :
- `frontend` : interface graphique utilisé côté client?
- `wasm_engine` : moteur natif en C.
---

## Guide

Pour comprendre rapidement le projet :

1. [Rapport technique complet](docs/REPORT.md)
2. [Guide d'installation](docs/INSTALLATION.md)
3. [Guide utilisateur](docs/USER_GUIDE.md)
4. [Architecture](docs/ARCHITECTURE.md)

---

## Documentation utilisateur

| Fichier | Rôle |
|---|---|
| [INSTALLATION.md](docs/INSTALLATION.md) | Installer et lancer l'application |
| [USER_GUIDE.md](docs/USER_GUIDE.md) | Utiliser l'interface |
| [IMPORT_AND_LIMITS.md](docs/IMPORT_AND_LIMITS.md) | Comprendre l'import et les limites |
| [FEATURES.md](docs/FEATURES.md) | Découvrir les fonctionnalités |
| [TROUBLESHOOTING.md](docs/TROUBLESHOOTING.md) | Résoudre les problèmes fréquents |

---

## Documentation technique

| Fichier | Rôle |
|---|---|
| [ARCHITECTURE.md](docs/ARCHITECTURE.md) | Architecture globale |
| [WASM_ENGINE.md](docs/WASM_ENGINE.md) | Moteur C / WebAssembly |
| [RENDERING.md](docs/RENDERING.md) | Rendu WebGL, 2D, 3D |
| [DEVELOPMENT.md](docs/DEVELOPMENT.md) | Développement local |
| [DOCKER.md](docs/DOCKER.md) | Docker, Nginx, production |

---

## Documentation projet

| Fichier | Rôle |
|---|---|
| [GIT_WORKFLOW.md](docs/GIT_WORKFLOW.md) | Commits et workflow |
| [ROADMAP.md](docs/ROADMAP.md) | Évolutions possibles |
| [REPORT.md](docs/REPORT.md) | Rapport détaillé complet |

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

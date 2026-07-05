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

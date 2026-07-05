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

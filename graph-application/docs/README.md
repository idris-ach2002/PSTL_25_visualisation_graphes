# Index de la documentation MonGraphe Web

Cette documentation est organisée par responsabilité. Elle accompagne la branche `web-vision`, qui transforme l'application desktop JavaFX / JOGL / JNI en application web React / TypeScript / WebGL / WebAssembly.

---

## Lecture conseillée

1. [Rapport technique complet](REPORT.md)
2. [Installation](INSTALLATION.md)
3. [Guide utilisateur](USER_GUIDE.md)
4. [Déploiement Cloudflare Pages](DEPLOYMENT_CLOUDFLARE.md)
5. [Architecture](ARCHITECTURE.md)

---

## Documentation utilisateur

| Fichier | Rôle |
|---|---|
| [INSTALLATION.md](INSTALLATION.md) | Installer et lancer l'application localement |
| [USER_GUIDE.md](USER_GUIDE.md) | Utiliser l'interface, les menus et les onglets |
| [IMPORT_AND_LIMITS.md](IMPORT_AND_LIMITS.md) | Importer CSV / DOT / edge-list et comprendre la limite de nœuds |
| [FEATURES.md](FEATURES.md) | Parcourir les fonctionnalités principales |
| [TROUBLESHOOTING.md](TROUBLESHOOTING.md) | Résoudre les erreurs fréquentes |

---

## Documentation technique

| Fichier | Rôle |
|---|---|
| [ARCHITECTURE.md](ARCHITECTURE.md) | Comprendre la structure globale du projet |
| [WASM_ENGINE.md](WASM_ENGINE.md) | Comprendre le moteur C compilé en WebAssembly |
| [RENDERING.md](RENDERING.md) | Comprendre le rendu WebGL, la 2D et la 3D orbitale |
| [DEVELOPMENT.md](DEVELOPMENT.md) | Modifier et tester le projet localement |
| [DOCKER.md](DOCKER.md) | Comprendre Docker, Nginx et le lancement local |
| [DEPLOYMENT_CLOUDFLARE.md](DEPLOYMENT_CLOUDFLARE.md) | Déployer l'application sur Cloudflare Pages |

---

## Documentation projet

| Fichier | Rôle |
|---|---|
| [GIT_WORKFLOW.md](GIT_WORKFLOW.md) | Organisation Git, commits et branche de migration |
| [ROADMAP.md](ROADMAP.md) | Évolutions possibles |
| [REPORT.md](REPORT.md) | Rapport détaillé complet |

---

## Responsabilités de la documentation

- `README.md` à la racine du dépôt sert de point d'entrée.
- `REPORT.md` sert de rapport technique complet.
- Les fichiers spécialisés évitent de mélanger installation, usage, architecture, déploiement et maintenance.
- La documentation Cloudflare est séparée car le déploiement n'utilise pas le même flux que le lancement Docker local.

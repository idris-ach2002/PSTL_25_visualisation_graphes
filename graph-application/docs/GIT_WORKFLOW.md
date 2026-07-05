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

---

## 6. Commits de déploiement Cloudflare

Pour les fichiers liés au déploiement Cloudflare, utiliser par exemple :

```bash
git add .github/workflows/cloudflare-pages.yml \
        graph-application/scripts/build-cloudflare.sh \
        graph-application/scripts/deploy-cloudflare.sh \
        graph-application/frontend/public/_headers \
        graph-application/docs/DEPLOYMENT_CLOUDFLARE.md \
        graph-application/docs/README.md \
        README.md

git commit -m "ci: add Cloudflare Pages deployment workflow"
```

Ce commit doit rester séparé des commits fonctionnels de rendu ou d'interface, car il concerne uniquement la chaîne de livraison.

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

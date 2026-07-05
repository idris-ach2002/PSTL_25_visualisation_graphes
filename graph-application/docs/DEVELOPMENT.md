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

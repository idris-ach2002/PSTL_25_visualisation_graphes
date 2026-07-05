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

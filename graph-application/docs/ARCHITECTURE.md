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

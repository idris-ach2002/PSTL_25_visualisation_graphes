# Rendu WebGL détaillé

## 1. Objectif

Le rendu doit concilier :

- lisibilité ;
- fluidité ;
- précision ;
- compatibilité web ;
- interaction.

---

## 2. Mode 2D

Le mode 2D est prioritaire.

Il est plus lisible pour l'analyse.

---

## 3. Mode 3D orbitale

Le mode 3D permet une exploration spatiale.

Il utilise une vraie caméra orbitale.

---

## 4. Nœuds

Les nœuds sont dessinés dans le canvas.

Ils peuvent changer selon :

- sélection ;
- survol ;
- degré ;
- communauté ;
- filtre.

---

## 5. Arêtes

Les arêtes restent simples pour éviter le bruit visuel.

Elles doivent être :

- visibles ;
- fines ;
- claires ;
- non agressives ;
- renforcées au besoin.

---

## 6. Picking

Le picking permet de savoir quel élément est sous la souris.

Il sert aux infobulles et à la sélection.

---

## 7. Mini-map

La mini-map donne une vue globale.

Elle aide quand le zoom est élevé.

---

## 8. Performance

Pour maintenir la fluidité :

- limiter les allocations ;
- utiliser WebGL ;
- éviter les nœuds HTML ;
- plafonner la démo ;
- filtrer les graphes denses.

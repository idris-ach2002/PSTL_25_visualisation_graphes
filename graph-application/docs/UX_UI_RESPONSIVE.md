# Amélioration UX / UI responsive

## Objectif

Cette évolution améliore l'interface de MonGraphe Web sur trois familles de supports :

- téléphone ;
- tablette ;
- ordinateur.

L'objectif est de conserver la richesse fonctionnelle de l'application tout en rendant l'interface plus lisible, plus élégante et plus simple à utiliser.

---

## Principes de design

La refonte visuelle repose sur cinq principes :

1. **Priorité au graphe** : le canvas reste l'élément central.
2. **Panneaux adaptatifs** : les paramètres et l'analyse deviennent contextuels sur petit écran.
3. **Contrôles tactiles** : zoom et recentrage sont accessibles sans clavier ni souris.
4. **Templates rapides** : l'utilisateur peut choisir une configuration adaptée au contexte.
5. **Guide intégré** : l'aide explique les gestes et le parcours utilisateur.

---

## Téléphone

Sur téléphone, l'interface devient verticale.

Adaptations :

- barre mobile fixe en bas ;
- accès rapide à Paramètres, Graphe, Analyse et Guide ;
- menus desktop masqués ;
- canvas prioritaire ;
- panneaux empilés ;
- boutons plus grands ;
- infobulles repositionnées en bas ;
- modales adaptées façon bottom sheet.

---

## Tablette

Sur tablette, l'application conserve une logique de bureau allégée.

Adaptations :

- sections latérales compactes ;
- boutons et onglets scrollables horizontalement ;
- statistiques masquées si l'espace est insuffisant ;
- canvas agrandi ;
- paramètres accessibles sans casser la visualisation.

---

## Ordinateur

Sur ordinateur, l'application conserve l'interface complète.

Structure :

```text
Paramètres | Graphe WebGL | Analyse
```

Améliorations :

- panneaux arrondis et hiérarchisés ;
- barre supérieure plus claire ;
- meilleur contraste ;
- meilleure lisibilité des tableaux ;
- guide enrichi ;
- templates visuels rapides.

---

## Templates ajoutés

Trois templates rapides sont disponibles dans le panneau Projet.

### Analyse 2D

Mode sobre, stable et lisible.

Recommandé pour :

- lire les relations ;
- filtrer ;
- sélectionner ;
- exporter une vue claire.

### Présentation 3D

Mode orienté démonstration.

Recommandé pour :

- soutenance ;
- présentation orale ;
- exploration visuelle.

### Mobile lisible

Mode allégé pour petit écran.

Effets :

- nœuds plus grands ;
- rendu plus léger ;
- labels désactivés par défaut ;
- épaisseur d'arêtes renforcée.

---

## Contrôles tactiles

Le canvas dispose maintenant de contrôles rapides visibles sur mobile :

- `−` : dézoomer ;
- `Fit` : recentrer ;
- `+` : zoomer.

Le badge de mode indique si la vue courante est en 2D simple ou en 3D orbitale.

---

## Accessibilité et confort

La refonte ajoute :

- meilleurs contrastes ;
- zones de clic plus grandes ;
- réduction des effets si l'utilisateur demande moins d'animations ;
- navigation plus simple sur écran étroit ;
- meilleure lisibilité du guide.

# Guide utilisateur détaillé

## 1. Présentation de l'interface

MonGraphe Web propose une interface proche d'une application desktop.

Elle contient :

- une barre de menus ;
- une barre d'outils ;
- des onglets ;
- un panneau de paramètres ;
- un canvas de visualisation ;
- un panneau d'analyse ;
- une barre de statut.

---

## 2. Logique générale

Le flux d'utilisation recommandé est :

```text
Importer ou générer un graphe
        ↓
Analyser les statistiques
        ↓
Choisir un layout
        ↓
Explorer le graphe
        ↓
Filtrer ou sélectionner
        ↓
Exporter ou sauvegarder
```

---

## 3. Panneau Projet

Le panneau Projet contient :

- import fichier ;
- génération de démo ;
- limite maximale de nœuds ;
- sauvegarde projet ;
- chargement projet.

C'est le point d'entrée principal.

---

## 4. Panneau Construction

Il contient les paramètres liés à la création du graphe depuis un CSV :

- mesure de similarité ;
- seuils ;
- création d'arêtes ;
- création éventuelle d'anti-arêtes ;
- stratégie de construction.

---

## 5. Panneau Simulation

Il contient les paramètres de layout :

- vitesse ;
- répulsion ;
- attraction ;
- nombre d'itérations ;
- K-Means ;
- communautés.

---

## 6. Panneau Nœuds

Il permet de contrôler :

- taille des nœuds ;
- visibilité ;
- labels ;
- couleur ;
- sélection ;
- survol.

---

## 7. Panneau Arêtes

Il permet de contrôler :

- couleur des arêtes ;
- épaisseur ;
- opacité ;
- poids minimum ;
- style simple.

---

## 8. Panneau Caméra

Il permet de contrôler :

- zoom ;
- mode 2D ;
- mode 3D orbitale ;
- vue face ;
- vue côté ;
- vue isométrique ;
- recentrage.

---

## 9. Onglet Overview

C'est l'espace principal de visualisation.

On y manipule le graphe.

---

## 10. Onglet Data

Affiche les données du graphe sous forme tabulaire.

Utile pour vérifier les IDs, labels, degrés et communautés.

---

## 11. Onglet Preview

Permet de préparer une vue propre pour export.

---

## 12. Onglet Help / Doc

Explique l'application directement depuis l'interface.

---

## 13. Contrôles

### Mode 2D

| Action | Contrôle |
|---|---|
| Déplacer | glisser le fond |
| Zoomer | molette |
| Sélectionner | clic sur nœud |
| Voir infos | survol |
| Recentrer | bouton Fit |

### Mode 3D

| Action | Contrôle |
|---|---|
| Orbiter | glisser |
| Déplacer | Maj + glisser |
| Zoomer | molette |
| Vue face | bouton Face |
| Vue côté | bouton Côté |
| Vue iso | bouton Iso |

---

## 14. Sélection

Lorsqu'un nœud est sélectionné, le panneau latéral peut afficher :

- ID ;
- label ;
- degré ;
- communauté ;
- voisins ;
- position ;
- actions disponibles.

---

## 15. Conseils

Pour une bonne visualisation :

- rester sous 1 000 nœuds ;
- utiliser la 2D pour l'analyse ;
- utiliser la 3D pour la présentation ;
- filtrer les nœuds isolés ;
- utiliser les communautés ;
- augmenter le seuil pour réduire les arêtes trop nombreuses.

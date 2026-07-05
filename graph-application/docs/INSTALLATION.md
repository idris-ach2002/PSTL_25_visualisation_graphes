# Guide d'installation détaillé

## 1. Objectif

Ce document explique comment installer, lancer et vérifier MonGraphe Web.

L'objectif est de permettre à un utilisateur de lancer l'application sans installer manuellement les dépendances de compilation comme Node.js, Emscripten ou Nginx.

La méthode recommandée repose sur Docker.

---

## 2. Prérequis

### 2.1 Docker

Docker doit être installé.

Vérification :

```bash
docker --version
```

### 2.2 Docker Compose

Vérification :

```bash
docker compose version
```

### 2.3 Navigateur

Utiliser un navigateur moderne :

- Chrome ;
- Chromium ;
- Firefox ;
- Edge.

Le navigateur doit supporter WebGL.

---

## 3. Lancement production

Depuis la racine du projet :

```bash
./run_app.sh fresh
```

Cette commande effectue un lancement propre.

Elle est recommandée après :

- un changement de code ;
- une nouvelle extraction du ZIP ;
- une mise à jour du projet ;
- un problème de cache.

---

## 4. Déroulement du build

Le build production se déroule en plusieurs étapes :

```text
1. Compilation du moteur C avec Emscripten
2. Génération du WebAssembly
3. Installation des dépendances frontend
4. Build Vite / React
5. Copie du build dans Nginx
6. Exposition sur le port 8080
```

---

## 5. Accès à l'application

Après lancement :

```text
http://localhost:8080
```

---

## 6. Mode développement

Pour modifier l'application :

```bash
./run_app.sh dev
```

Puis :

```text
http://localhost:5173
```

Le mode développement utilise Vite.

---

## 7. Arrêter l'application

```bash
./run_app.sh down
```

---

## 8. Nettoyer

```bash
./run_app.sh clean
```

---

## 9. Problèmes fréquents

### Port occupé

Si le port 8080 est déjà utilisé :

```bash
docker ps
```

Puis :

```bash
docker stop <id_du_conteneur>
```

### Ancienne version visible

Faire :

```bash
./run_app.sh fresh
```

Puis recharger avec :

```text
Ctrl + F5
```

### Docker trop lent

Le premier build est plus long car Docker télécharge les images :

- Node ;
- Emscripten ;
- Nginx.

Les builds suivants sont plus rapides grâce au cache Docker.

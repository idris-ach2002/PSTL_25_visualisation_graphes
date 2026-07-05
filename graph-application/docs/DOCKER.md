# Docker et déploiement détaillé

## 1. Rôle de Docker

Docker permet de lancer le projet sans installer manuellement les dépendances.

---

## 2. Build multi-stage

Étapes :

1. Emscripten ;
2. Node ;
3. Nginx.

---

## 3. Production

```bash
./run_app.sh fresh
```

---

## 4. Développement

```bash
./run_app.sh dev
```

---

## 5. Nginx

Nginx sert les fichiers compilés.

---

## 6. Changer le port

Dans `docker-compose.yml` :

```yaml
ports:
  - "8081:80"
```

---

## 7. Nettoyage

```bash
docker compose down --remove-orphans
docker system prune
```

---

## 8. Différence entre Docker local et Cloudflare Pages

Docker reste la solution recommandée pour lancer l'application localement :

```bash
./run_app.sh fresh
```

En revanche, Docker n'est pas utilisé comme runtime de production sur Cloudflare Pages.

La production Cloudflare repose sur un build statique :

```text
frontend/dist/
```

Ce dossier contient :

- le HTML ;
- les fichiers JavaScript et CSS générés par Vite ;
- le moteur WebAssembly généré ;
- les fichiers publics nécessaires à l'application.

Le workflow de production est documenté dans :

```text
docs/DEPLOYMENT_CLOUDFLARE.md
```

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

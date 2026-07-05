# Déploiement Cloudflare Pages

## 1. Objectif

Ce document décrit la stratégie de déploiement recommandée pour **MonGraphe Web**.

L'application actuelle est une application statique après compilation :

```text
React / TypeScript / Vite
WebGL côté navigateur
WebAssembly servi comme fichier statique
Pas de backend obligatoire
Pas de base de données obligatoire
```

La cible la plus adaptée est donc **Cloudflare Pages**.

Le déploiement ne repose pas sur Docker en production. Docker reste utile pour le lancement local et pour reproduire l'environnement de compilation du moteur WebAssembly.

---

## 2. Choix retenu

La stratégie retenue est l'option propre suivante :

```text
GitHub Actions
    ↓
Compilation WASM avec l'image Docker Emscripten
    ↓
Compilation frontend avec Node.js
    ↓
Publication du dossier frontend/dist sur Cloudflare Pages avec Wrangler
```

Avantages :

- pas de mise en sommeil liée à un serveur gratuit ;
- pas de backend à maintenir ;
- build reproductible ;
- pas besoin de committer les fichiers `.wasm` générés ;
- logs de build visibles dans GitHub Actions ;
- production et previews de branches possibles ;
- Docker reste local, mais n'est pas utilisé comme runtime de production.

---

## 3. Fichiers ajoutés

```text
.github/workflows/cloudflare-pages.yml
```

Workflow CI/CD GitHub Actions.

```text
graph-application/scripts/build-cloudflare.sh
```

Script de build statique complet.

```text
graph-application/scripts/deploy-cloudflare.sh
```

Déploiement manuel avec Wrangler.

```text
graph-application/frontend/public/_headers
```

Règles de headers HTTP pour Cloudflare Pages.

---

## 4. Créer le projet Cloudflare Pages

Dans le dashboard Cloudflare :

1. Aller dans **Workers & Pages**.
2. Créer un projet **Pages**.
3. Choisir un nom, par exemple :

```text
mongraphe-web
```

Le projet peut être créé comme projet Pages vide ou via Direct Upload. Le workflow GitHub Actions se chargera ensuite de publier le dossier `frontend/dist`.

---

## 5. Créer le token API Cloudflare

Créer un token API Cloudflare avec une permission permettant de modifier Cloudflare Pages.

Variables nécessaires côté GitHub :

```text
CLOUDFLARE_API_TOKEN
CLOUDFLARE_ACCOUNT_ID
```

Le token ne doit jamais être commité dans le dépôt.

---

## 6. Ajouter les secrets GitHub

Dans GitHub :

```text
Repository → Settings → Secrets and variables → Actions → New repository secret
```

Ajouter :

```text
CLOUDFLARE_API_TOKEN=<token_cloudflare>
CLOUDFLARE_ACCOUNT_ID=<account_id_cloudflare>
```

Optionnellement, ajouter une variable de repository :

```text
CLOUDFLARE_PAGES_PROJECT=mongraphe-web
```

Si cette variable n'est pas définie, le workflow utilise `mongraphe-web` par défaut.

---

## 7. Fonctionnement du workflow GitHub Actions

Le workflow se trouve ici :

```text
.github/workflows/cloudflare-pages.yml
```

Il est déclenché par :

- push sur `web-vision` ;
- push sur `main` ;
- pull request modifiant l'application ;
- déclenchement manuel depuis l'onglet Actions.

Étapes exécutées :

1. checkout du dépôt ;
2. compilation du moteur C avec l'image Docker `emscripten/emsdk:3.1.64` ;
3. vérification de `graph-engine.js` et `graph-engine.wasm` ;
4. installation Node.js ;
5. `npm ci` ;
6. `npm run build` ;
7. vérification de `frontend/dist` ;
8. upload de l'artefact GitHub ;
9. déploiement Cloudflare Pages si les secrets sont présents.

---

## 7.1 Production ou preview ?

Cloudflare Pages détermine si un déploiement est une production ou une preview à partir de la branche envoyée.

Le workflow publie par défaut avec la branche Git courante :

```text
web-vision → branche Cloudflare web-vision
main       → branche Cloudflare main
```

Deux choix propres sont possibles :

### Choix A — production sur `web-vision`

Dans Cloudflare Pages, définir `web-vision` comme branche de production.
C'est pratique tant que la migration web vit sur cette branche.

### Choix B — production sur `main`

Garder `main` comme branche de production, puis merger `web-vision` dans `main` quand la version web est validée.
C'est plus classique pour un projet finalisé.

Si `web-vision` n'est pas définie comme branche de production, Cloudflare créera une preview pour cette branche.

---

## 8. Build local avant déploiement

Pour vérifier localement le build Cloudflare :

```bash
cd graph-application
./scripts/build-cloudflare.sh
```

Ce script produit :

```text
frontend/dist/
```

Ce dossier est le dossier statique publiable.

---

## 9. Déploiement manuel

Si tu veux déployer sans attendre GitHub Actions :

```bash
cd graph-application
export CLOUDFLARE_ACCOUNT_ID="..."
export CLOUDFLARE_API_TOKEN="..."
export CLOUDFLARE_PAGES_PROJECT="mongraphe-web"
./scripts/deploy-cloudflare.sh
```

Le script :

1. compile le WebAssembly ;
2. compile le frontend ;
3. publie `frontend/dist` avec Wrangler.

---

## 10. Pourquoi ne pas utiliser Docker en production ?

Le Dockerfile reste utile pour lancer l'application localement avec Nginx :

```bash
./run_app.sh fresh
```

Mais pour Cloudflare Pages, Docker n'est pas nécessaire en production.

Cloudflare Pages sert des fichiers statiques depuis son CDN. L'application n'a pas besoin d'un conteneur qui tourne en continu.

---

## 11. Pourquoi ne pas committer le WebAssembly généré ?

Les fichiers suivants sont générés :

```text
frontend/public/wasm/graph-engine.js
frontend/public/wasm/graph-engine.wasm
```

Ils sont produits automatiquement par :

```bash
make -C wasm-engine wasm
```

La stratégie retenue est de ne pas les versionner. Le workflow les régénère à chaque build.

Avantages :

- moins de binaires dans Git ;
- build reproductible ;
- pas d'oubli après modification du moteur C ;
- historique Git plus propre.

---

## 12. Headers Cloudflare

Le fichier suivant est ajouté :

```text
frontend/public/_headers
```

Il permet de :

- éviter le cache agressif sur `index.html` ;
- donner le bon type MIME au WebAssembly ;
- mettre en cache long les assets Vite hashés ;
- ajouter quelques headers de sécurité simples.

Le cache du dossier `/wasm/` reste en revalidation afin d'éviter qu'un navigateur conserve une ancienne version du moteur WebAssembly après mise à jour.

---

## 13. Commandes utiles

### Build local Cloudflare

```bash
./scripts/build-cloudflare.sh
```

### Déploiement manuel

```bash
./scripts/deploy-cloudflare.sh
```

### Lancement local Docker

```bash
./run_app.sh fresh
```

### Vérifier le résultat du build

```bash
ls -lh frontend/dist
ls -lh frontend/dist/wasm
```

---

## 14. Résumé

La production recommandée est :

```text
Cloudflare Pages + GitHub Actions + Wrangler
```

Le développement local reste :

```text
Docker Compose + run_app.sh
```

Cette séparation est propre :

- Docker pour développer et tester localement ;
- Cloudflare Pages pour héberger gratuitement sans sleep ;
- GitHub Actions pour compiler et déployer automatiquement ;
- WebAssembly généré à chaque pipeline, sans binaire commité.

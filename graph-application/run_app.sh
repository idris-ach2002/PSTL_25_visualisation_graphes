#!/usr/bin/env bash
set -Eeuo pipefail

ROOT_DIR="$(cd "$(dirname "${BASH_SOURCE[0]}")" && pwd)"
cd "$ROOT_DIR"

COMPOSE="docker compose"
if ! docker compose version >/dev/null 2>&1; then
  if command -v docker-compose >/dev/null 2>&1; then
    COMPOSE="docker-compose"
  else
    echo "Erreur: Docker Compose est introuvable." >&2
    echo "Installe Docker puis relance: bash run_app.sh" >&2
    exit 1
  fi
fi

MODE="${1:-prod}"

case "$MODE" in
  prod|start)
    echo "Build + lancement production sur http://localhost:8080"
    echo "Arrêt de l'ancien conteneur pour éviter le cache/stale assets..."
    $COMPOSE down --remove-orphans >/dev/null 2>&1 || true
    $COMPOSE up --build graph-web
    ;;
  fresh|no-cache)
    echo "Build propre sans cache + lancement production sur http://localhost:8080"
    $COMPOSE down --remove-orphans >/dev/null 2>&1 || true
    $COMPOSE build --no-cache --progress=plain graph-web
    $COMPOSE up graph-web
    ;;
  dev)
    echo "Build + lancement développement sur http://localhost:5173"
    $COMPOSE --profile dev up --build graph-web-dev
    ;;
  build)
    echo "Build production sans lancement"
    $COMPOSE build --no-cache --progress=plain graph-web
    ;;
  down|stop)
    echo "Arrêt des conteneurs"
    $COMPOSE down --remove-orphans
    ;;
  clean)
    echo "Nettoyage conteneurs, volumes et images locales du projet"
    $COMPOSE down --volumes --remove-orphans
    docker image prune -f
    ;;
  logs)
    $COMPOSE logs -f
    ;;
  *)
    cat <<USAGE
Usage: bash run_app.sh [prod|fresh|dev|build|down|clean|logs]

  prod   Build puis lance l'application sur http://localhost:8080
  fresh  Rebuild sans cache Docker puis lance l'application
  dev    Lance Vite en mode développement sur http://localhost:5173
  build  Build production détaillé sans lancer le conteneur
  down   Arrête les conteneurs
  clean  Supprime volumes/conteneurs liés au projet puis prune les images inutilisées
  logs   Affiche les logs Docker Compose
USAGE
    exit 2
    ;;
esac

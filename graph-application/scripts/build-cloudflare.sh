#!/usr/bin/env bash
set -Eeuo pipefail

# Build statique complet pour Cloudflare Pages.
# Cette commande compile d'abord le moteur C en WebAssembly, puis compile le frontend Vite.
# Elle ne lance aucun serveur et ne dépend d'aucun backend.

ROOT_DIR="$(cd "$(dirname "${BASH_SOURCE[0]}")/.." && pwd)"
cd "$ROOT_DIR"

EMSDK_IMAGE="${EMSDK_IMAGE:-emscripten/emsdk:3.1.64}"
WASM_JS="frontend/public/wasm/graph-engine.js"
WASM_BIN="frontend/public/wasm/graph-engine.wasm"
DIST_DIR="frontend/dist"

echo "==> MonGraphe Web: build Cloudflare Pages"
echo "Projet : $ROOT_DIR"
echo

build_wasm_with_local_emcc() {
  echo "==> Compilation WebAssembly avec emcc local"
  make -C wasm-engine wasm
}

build_wasm_with_docker() {
  echo "==> Compilation WebAssembly avec Docker image ${EMSDK_IMAGE}"
  if ! command -v docker >/dev/null 2>&1; then
    echo "Erreur : emcc est absent et Docker est introuvable." >&2
    echo "Installe Emscripten ou Docker, puis relance le script." >&2
    exit 1
  fi

  DOCKER_USER_ARGS=()
  if [ -z "${CI:-}" ] && command -v id >/dev/null 2>&1; then
    DOCKER_USER_ARGS=(--user "$(id -u):$(id -g)")
  fi

  docker run --rm \
    "${DOCKER_USER_ARGS[@]}" \
    -v "$ROOT_DIR":/app \
    -w /app \
    "$EMSDK_IMAGE" \
    make -C wasm-engine wasm
}

if command -v emcc >/dev/null 2>&1; then
  build_wasm_with_local_emcc
else
  build_wasm_with_docker
fi

echo "==> Vérification des assets WASM"
test -s "$WASM_JS"
test -s "$WASM_BIN"
ls -lh frontend/public/wasm

echo
if ! command -v npm >/dev/null 2>&1; then
  echo "Erreur : npm est introuvable. Installe Node.js ou utilise la GitHub Action." >&2
  exit 1
fi

echo "==> Installation dépendances frontend"
cd frontend
npm ci --no-audit --no-fund

echo "==> Build frontend Vite"
npm run build
cd "$ROOT_DIR"

echo "==> Vérification du dossier dist"
test -s "$DIST_DIR/index.html"
test -s "$DIST_DIR/wasm/graph-engine.js"
test -s "$DIST_DIR/wasm/graph-engine.wasm"

find "$DIST_DIR" -maxdepth 2 -type f | sort

echo
echo "Build Cloudflare terminé. Dossier à publier : $DIST_DIR"

#!/usr/bin/env bash
set -Eeuo pipefail

# Déploiement manuel vers Cloudflare Pages avec Wrangler.
# Pré-requis :
#   export CLOUDFLARE_ACCOUNT_ID="..."
#   export CLOUDFLARE_API_TOKEN="..."
#   export CLOUDFLARE_PAGES_PROJECT="mongraphe-web"   # optionnel, valeur par défaut ci-dessous

ROOT_DIR="$(cd "$(dirname "${BASH_SOURCE[0]}")/.." && pwd)"
cd "$ROOT_DIR"

PROJECT_NAME="${CLOUDFLARE_PAGES_PROJECT:-mongraphe-web}"
DEPLOY_BRANCH="${CLOUDFLARE_DEPLOY_BRANCH:-$(git rev-parse --abbrev-ref HEAD 2>/dev/null || echo manual)}"
DIST_DIR="frontend/dist"

echo "==> Build local avant déploiement"
"$ROOT_DIR/scripts/build-cloudflare.sh"

if ! command -v npx >/dev/null 2>&1; then
  echo "Erreur : npx est introuvable. Installe Node.js." >&2
  exit 1
fi

if [ -z "${CLOUDFLARE_API_TOKEN:-}" ]; then
  echo "Erreur : variable CLOUDFLARE_API_TOKEN manquante." >&2
  echo "Crée un token Cloudflare Pages Edit, puis exporte-le avant de relancer." >&2
  exit 1
fi

if [ -z "${CLOUDFLARE_ACCOUNT_ID:-}" ]; then
  echo "Erreur : variable CLOUDFLARE_ACCOUNT_ID manquante." >&2
  exit 1
fi

echo "==> Déploiement Cloudflare Pages"
echo "Projet Cloudflare : $PROJECT_NAME"
echo "Branche Pages     : $DEPLOY_BRANCH"
echo "Dossier publié    : $DIST_DIR"

CLOUDFLARE_ACCOUNT_ID="$CLOUDFLARE_ACCOUNT_ID" \
CLOUDFLARE_API_TOKEN="$CLOUDFLARE_API_TOKEN" \
npx wrangler pages deploy "$DIST_DIR" \
  --project-name="$PROJECT_NAME" \
  --branch="$DEPLOY_BRANCH"

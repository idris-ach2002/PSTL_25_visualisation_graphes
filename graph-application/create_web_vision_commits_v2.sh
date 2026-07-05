#!/usr/bin/env bash
set -euo pipefail

# create_web_vision_commits_v2.sh
# Script corrigé pour un dépôt dont la racine Git est PSTL_25_visualisation_graphes
# et dont l'application remplacée se trouve dans graph-application/.
#
# À lancer depuis :
#   .../PSTL_25_visualisation_graphes/graph-application
# ou depuis :
#   .../PSTL_25_visualisation_graphes

CURRENT_BRANCH="$(git rev-parse --abbrev-ref HEAD)"
REPO_ROOT="$(git rev-parse --show-toplevel)"

if [ -d "$REPO_ROOT/graph-application" ]; then
  APP_DIR="$REPO_ROOT/graph-application"
else
  APP_DIR="$REPO_ROOT"
fi

cd "$REPO_ROOT"

echo "Dépôt Git       : $REPO_ROOT"
echo "Dossier app     : $APP_DIR"
echo "Branche actuelle: $CURRENT_BRANCH"
echo

if [ "$CURRENT_BRANCH" != "web-vision" ]; then
  echo "ATTENTION : tu n'es pas sur la branche web-vision."
  read -r -p "Continuer quand même ? [y/N] " answer
  case "$answer" in
    y|Y|yes|YES|o|O|oui|OUI) ;;
    *) echo "Abandon."; exit 1 ;;
  esac
fi

if ! git diff --cached --quiet; then
  echo "Il existe déjà des fichiers stagés."
  echo "Pour éviter de mélanger les commits, fais d'abord : git reset"
  exit 1
fi

rel() {
  python3 - "$REPO_ROOT" "$1" <<'PY'
import os, sys
root, path = sys.argv[1], sys.argv[2]
print(os.path.relpath(path, root))
PY
}

stage_if_exists() {
  for p in "$@"; do
    if [ -e "$APP_DIR/$p" ]; then
      git add -- "$(rel "$APP_DIR/$p")"
    else
      echo "Ignoré, chemin absent : graph-application/$p"
    fi
  done
}

commit_if_staged() {
  local message="$1"
  if git diff --cached --quiet; then
    echo "Aucun changement à committer pour : $message"
  else
    echo
    echo "Commit : $message"
    git status --short
    git commit -m "$message"
  fi
}

echo "Création des commits web-vision..."
echo

# 1) Suppressions de l'ancien projet desktop + archivage du moteur natif original.
# On ne fait git add -u que sur graph-application pour éviter d'impacter autre chose.
git add -u -- "$(rel "$APP_DIR")"
stage_if_exists legacy
commit_if_staged "chore(legacy): replace desktop app with archived native engine"

# 2) Infra Docker, scripts et docs racine du sous-projet.
stage_if_exists \
  .dockerignore \
  Dockerfile \
  Dockerfile.dev \
  docker-compose.yml \
  Makefile \
  nginx.conf \
  run_app.sh \
  RUN_THIS.md \
  README.md \
  docs \
  scripts
commit_if_staged "chore(infra): add Docker runtime and launch workflow"

# 3) Moteur WebAssembly.
stage_if_exists wasm-engine
commit_if_staged "feat(wasm): add WebAssembly graph simulation engine"

# 4) Socle frontend.
stage_if_exists \
  frontend/package.json \
  frontend/package-lock.json \
  frontend/tsconfig.json \
  frontend/vite.config.ts \
  frontend/index.html \
  frontend/src/main.tsx \
  frontend/src/styles.css \
  frontend/src/types \
  frontend/src/wasm
commit_if_staged "feat(frontend): scaffold Vite React application shell"

# 5) Import, parsing, limite web configurable, samples.
stage_if_exists \
  frontend/public \
  frontend/src/engine/GraphParser.ts \
  frontend/src/components/ImportAssistantDialog.tsx \
  frontend/src/components/LimitDialog.tsx
commit_if_staged "feat(import): add parsers, samples and configurable node limits"

# 6) Worker + rendu WebGL.
stage_if_exists \
  frontend/src/engine/graph.worker.ts \
  frontend/src/rendering \
  frontend/src/components/GraphCanvas.tsx
commit_if_staged "feat(rendering): add WebGL renderer with 2D and orbit 3D"

# 7) Analyse, données, toolbar.
stage_if_exists \
  frontend/src/components/DataPanel.tsx \
  frontend/src/components/StatsPanel.tsx \
  frontend/src/components/Toolbar.tsx
commit_if_staged "feat(analysis): add data, statistics and graph action panels"

# 8) Help / documentation intégrée.
stage_if_exists frontend/src/components/HelpPanel.tsx
commit_if_staged "docs(help): add embedded user guide and examples"

# 9) Intégration finale App.
stage_if_exists frontend/src/App.tsx
commit_if_staged "feat(app): integrate UX, filters, layouts and project workflow"

# 10) Script de commit lui-même, optionnel mais utile pour traçabilité.
if [ -f "$APP_DIR/create_web_vision_commits_v2.sh" ]; then
  git add -- "$(rel "$APP_DIR/create_web_vision_commits_v2.sh")"
  commit_if_staged "chore(git): add migration commit helper script"
fi

echo
echo "Terminé."
echo
echo "Historique récent :"
git --no-pager log --oneline --decorate -n 14

echo
echo "État final :"
git status --short

echo
echo "À tester :"
echo "  cd \"$APP_DIR\""
echo "  ./run_app.sh fresh"
echo
echo "À pousser si tout est bon :"
echo "  git push -u origin web-vision"

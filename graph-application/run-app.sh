#!/usr/bin/env bash
set -euo pipefail

ROOT_DIR="$(cd "$(dirname "$0")" && pwd)"

cd "$ROOT_DIR/graph-native"
make

cd "$ROOT_DIR/graph-ui"
exec "$ROOT_DIR/tools/apache-maven-3.9.6/bin/mvn" javafx:run

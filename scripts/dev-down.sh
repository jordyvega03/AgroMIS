#!/usr/bin/env bash
set -euo pipefail

COMPOSE_FILE="infra/docker/docker-compose.dev.yml"
PROJECT_NAME="agromis"

ROOT_DIR="$(cd "$(dirname "${BASH_SOURCE[0]}")/.." && pwd)"
cd "$ROOT_DIR"

echo "==> Deteniendo stack de desarrollo AgroMIS..."
docker compose -f "$COMPOSE_FILE" -p "$PROJECT_NAME" down

echo "==> Stack detenido. Los volumenes se conservaron."
echo "    Para eliminar datos: docker compose -f $COMPOSE_FILE -p $PROJECT_NAME down -v"

#!/usr/bin/env bash
set -euo pipefail

COMPOSE_FILE="infra/docker/docker-compose.dev.yml"
PROJECT_NAME="agromis"
SERVICE="${1:-}"

ROOT_DIR="$(cd "$(dirname "${BASH_SOURCE[0]}")/.." && pwd)"
cd "$ROOT_DIR"

if [ -n "$SERVICE" ]; then
    docker compose -f "$COMPOSE_FILE" -p "$PROJECT_NAME" logs -f "$SERVICE"
else
    docker compose -f "$COMPOSE_FILE" -p "$PROJECT_NAME" logs -f
fi

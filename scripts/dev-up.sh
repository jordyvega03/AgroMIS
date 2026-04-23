#!/usr/bin/env bash
set -euo pipefail

COMPOSE_FILE="infra/docker/docker-compose.dev.yml"
PROJECT_NAME="agromis"

ROOT_DIR="$(cd "$(dirname "${BASH_SOURCE[0]}")/.." && pwd)"
cd "$ROOT_DIR"

echo "==> Levantando stack de desarrollo AgroMIS..."
docker compose -f "$COMPOSE_FILE" -p "$PROJECT_NAME" up -d --build

echo ""
echo "==> Esperando que los servicios esten healthy..."
TIMEOUT=180
ELAPSED=0
INTERVAL=5

while [ "$ELAPSED" -lt "$TIMEOUT" ]; do
    UNHEALTHY=$(docker compose -f "$COMPOSE_FILE" -p "$PROJECT_NAME" ps --format json \
        | python3 -c "import sys,json; data=[json.loads(l) for l in sys.stdin if l.strip()]; \
          print(sum(1 for s in data if s.get('Health','') not in ('','healthy','')))" 2>/dev/null || echo "0")

    if [ "$UNHEALTHY" -eq 0 ]; then
        break
    fi

    echo "   Servicios pendientes: $UNHEALTHY — esperando ${INTERVAL}s..."
    sleep "$INTERVAL"
    ELAPSED=$((ELAPSED + INTERVAL))
done

echo ""
echo "==> Stack listo. Servicios disponibles:"
echo "   PostgreSQL:        localhost:5432  (user: agromis / pass: agromis_dev_pass)"
echo "   Redpanda (Kafka):  localhost:19092"
echo "   Redpanda Console:  http://localhost:8080"
echo "   Redis:             localhost:6379"
echo "   Keycloak:          http://localhost:8081  (admin/admin)"
echo "   MinIO:             http://localhost:9001  (agromis/agromis_minio_pass)"
echo "   Prometheus:        http://localhost:9090"
echo "   Grafana:           http://localhost:3000  (admin/admin)"
echo "   Loki:              localhost:3100"
echo "   Tempo:             localhost:3200"
echo "   Jaeger UI:         http://localhost:16686"
echo ""
echo "   Para ver logs: ./scripts/dev-logs.sh [servicio]"
echo "   Para apagar:   ./scripts/dev-down.sh"

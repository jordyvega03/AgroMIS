#!/usr/bin/env bash
# Registra schemas Avro en el Schema Registry de Redpanda
# Uso: ./register-schemas.sh local | staging | production
# Idempotente: si el schema ya existe y es compatible, no falla
set -euo pipefail

ENV="${1:-local}"
SCHEMAS_DIR="$(dirname "$0")/schemas"

case "$ENV" in
  local)      SR="http://localhost:18081" ;;
  staging)    SR="${SCHEMA_REGISTRY_URL:-http://redpanda.agromis-staging:8081}" ;;
  production) SR="${SCHEMA_REGISTRY_URL:-http://redpanda.agromis-prod:8081}" ;;
  *) echo "Ambiente invalido: $ENV"; exit 1 ;;
esac

echo "==> Registrando schemas en Schema Registry [$ENV] ($SR)"

register_schema() {
  local SUBJECT="$1"
  local FILE="$2"

  # Convertir el archivo .avsc a JSON escapado para la peticion
  SCHEMA=$(python3 -c "
import json, sys
with open('$FILE') as f:
    schema = json.load(f)
print(json.dumps({'schema': json.dumps(schema), 'schemaType': 'AVRO'}))
")

  STATUS=$(curl -s -o /dev/null -w "%{http_code}" \
    -X POST "$SR/subjects/$SUBJECT/versions" \
    -H "Content-Type: application/vnd.schemaregistry.v1+json" \
    -d "$SCHEMA")

  if [ "$STATUS" = "200" ] || [ "$STATUS" = "409" ]; then
    echo "   [OK] $SUBJECT (HTTP $STATUS)"
  else
    echo "   [ERROR] $SUBJECT (HTTP $STATUS)"
    exit 1
  fi
}

# Configurar compatibilidad BACKWARD por defecto
curl -s -X PUT "$SR/config" \
  -H "Content-Type: application/vnd.schemaregistry.v1+json" \
  -d '{"compatibility": "BACKWARD"}' > /dev/null

register_schema "agromis.reports.planting.v1-value"     "$SCHEMAS_DIR/planting-report-submitted-v1.avsc"
register_schema "agromis.projections.updated.v1-value"  "$SCHEMAS_DIR/projection-updated-v1.avsc"
register_schema "agromis.prices.observed.v1-value"      "$SCHEMAS_DIR/price-observed-v1.avsc"
register_schema "agromis.weather.observed.v1-value"     "$SCHEMAS_DIR/weather-observation-ingested-v1.avsc"

echo ""
echo "==> Schemas registrados. Listando subjects:"
curl -s "$SR/subjects" | python3 -m json.tool

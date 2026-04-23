#!/usr/bin/env bash
# Crea topics de Redpanda para el ambiente especificado
# Uso: ./create-topics.sh local | staging | production
set -euo pipefail

ENV="${1:-local}"

case "$ENV" in
  local)    BROKER="localhost:19092" ;;
  staging)  BROKER="${REDPANDA_BROKER:-redpanda.agromis-staging:9092}" ;;
  production) BROKER="${REDPANDA_BROKER:-redpanda.agromis-prod:9092}" ;;
  *) echo "Ambiente invalido: $ENV. Usar: local | staging | production"; exit 1 ;;
esac

echo "==> Creando topics en Redpanda [$ENV] ($BROKER)"

create_topic() {
  local NAME="$1"
  local PARTITIONS="$2"
  local REPLICATION="$3"
  shift 3
  local CONFIGS=("$@")

  CONFIG_ARGS=""
  for c in "${CONFIGS[@]}"; do
    CONFIG_ARGS="$CONFIG_ARGS --topic-config $c"
  done

  if rpk topic describe "$NAME" --brokers "$BROKER" &>/dev/null; then
    echo "   [EXISTE] $NAME"
  else
    rpk topic create "$NAME" \
      --brokers "$BROKER" \
      --partitions "$PARTITIONS" \
      --replicas "$REPLICATION" \
      $CONFIG_ARGS
    echo "   [CREADO] $NAME (${PARTITIONS}p / ${REPLICATION}r)"
  fi
}

# Particiones segun ambiente
case "$ENV" in
  local)
    P_MAIN=3; P_DLQ=1; R=1
    ;;
  staging)
    P_MAIN=6; P_DLQ=3; R=3
    ;;
  production)
    P_MAIN=24; P_DLQ=3; R=3
    ;;
esac

create_topic "agromis.reports.planting.v1"      "$P_MAIN"  "$R" "retention.ms=2592000000" "compression.type=lz4"
create_topic "agromis.projections.updated.v1"   "$P_MAIN"  "$R" "retention.ms=7776000000" "cleanup.policy=compact,delete"
create_topic "agromis.prices.observed.v1"       "$P_MAIN"  "$R" "retention.ms=2592000000"
create_topic "agromis.weather.observed.v1"      "$P_MAIN"  "$R" "retention.ms=1209600000"
create_topic "agromis.reputation.changed.v1"    "3"        "$R" "cleanup.policy=compact"
create_topic "agromis.notifications.dispatch.v1" "$P_MAIN" "$R" "retention.ms=604800000"

# DLQs
create_topic "agromis.reports.planting.v1.dlq"   "$P_DLQ" "$R" "retention.ms=2592000000"
create_topic "agromis.prices.observed.v1.dlq"    "$P_DLQ" "$R" "retention.ms=2592000000"

echo ""
echo "==> Topics creados. Listando:"
rpk topic list --brokers "$BROKER"

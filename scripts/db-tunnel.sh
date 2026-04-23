#!/usr/bin/env bash
# Abre un tunel SSM Port Forwarding hacia RDS (sin bastion SSH)
# Uso: ./scripts/db-tunnel.sh staging [local-port]
# Requiere: aws CLI + plugin Session Manager
set -euo pipefail

ENV="${1:-staging}"
LOCAL_PORT="${2:-5433}"

case "$ENV" in
  staging)
    INSTANCE_ID=$(aws ec2 describe-instances \
      --filters "Name=tag:Name,Values=agromis-${ENV}-ssm-bastion" \
               "Name=instance-state-name,Values=running" \
      --query "Reservations[0].Instances[0].InstanceId" \
      --output text --region us-east-1)
    RDS_HOST=$(aws rds describe-db-instances \
      --db-instance-identifier "agromis-${ENV}-postgres" \
      --query "DBInstances[0].Endpoint.Address" \
      --output text --region us-east-1)
    ;;
  production)
    INSTANCE_ID=$(aws ec2 describe-instances \
      --filters "Name=tag:Name,Values=agromis-${ENV}-ssm-bastion" \
               "Name=instance-state-name,Values=running" \
      --query "Reservations[0].Instances[0].InstanceId" \
      --output text --region us-east-1)
    RDS_HOST=$(aws rds describe-db-instances \
      --db-instance-identifier "agromis-${ENV}-postgres" \
      --query "DBInstances[0].Endpoint.Address" \
      --output text --region us-east-1)
    ;;
  *)
    echo "Ambiente invalido. Usar: staging | production"
    exit 1
    ;;
esac

if [ -z "$INSTANCE_ID" ] || [ "$INSTANCE_ID" = "None" ]; then
  echo "Error: No se encontro la instancia SSM para $ENV"
  exit 1
fi

echo "==> Abriendo tunel SSM:"
echo "    Instancia:   $INSTANCE_ID"
echo "    RDS Host:    $RDS_HOST"
echo "    Puerto local: $LOCAL_PORT"
echo ""
echo "    Conectar con:"
echo "    psql -h localhost -p $LOCAL_PORT -U agromis -d agromis"
echo ""
echo "    Ctrl+C para cerrar el tunel"

aws ssm start-session \
  --target "$INSTANCE_ID" \
  --document-name AWS-StartPortForwardingSessionToRemoteHost \
  --parameters "{\"host\":[\"$RDS_HOST\"],\"portNumber\":[\"5432\"],\"localPortNumber\":[\"$LOCAL_PORT\"]}" \
  --region us-east-1

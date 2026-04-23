#!/usr/bin/env bash
# Instala Velero y configura backup diario del estado K8s a S3
set -euo pipefail

ENV="${1:-staging}"
BUCKET="agromis-backups-${ENV}"
REGION="us-east-1"

echo "==> Instalando Velero para $ENV"

helm repo add vmware-tanzu https://vmware-tanzu.github.io/helm-charts
helm repo update

helm upgrade --install velero vmware-tanzu/velero \
  --namespace velero --create-namespace \
  --set "configuration.backupStorageLocation[0].name=aws" \
  --set "configuration.backupStorageLocation[0].provider=aws" \
  --set "configuration.backupStorageLocation[0].bucket=$BUCKET" \
  --set "configuration.backupStorageLocation[0].config.region=$REGION" \
  --set "configuration.volumeSnapshotLocation[0].name=aws" \
  --set "configuration.volumeSnapshotLocation[0].provider=aws" \
  --set "configuration.volumeSnapshotLocation[0].config.region=$REGION" \
  --set "initContainers[0].name=velero-plugin-for-aws" \
  --set "initContainers[0].image=velero/velero-plugin-for-aws:v1.9.0" \
  --set "initContainers[0].volumeMounts[0].mountPath=/target" \
  --set "initContainers[0].volumeMounts[0].name=plugins" \
  --wait

echo "==> Creando schedule de backup diario"
velero schedule create agromis-daily \
  --schedule="0 2 * * *" \
  --ttl 720h0m0s \
  --include-namespaces "agromis-${ENV}" || echo "Schedule ya existe"

echo "==> Velero instalado. Verificar:"
velero backup-location get
velero schedule get

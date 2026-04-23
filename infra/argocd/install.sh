#!/usr/bin/env bash
# Instala Argo CD en el cluster y configura el ApplicationSet para AgroMIS
set -euo pipefail

NAMESPACE="argocd"

echo "==> Instalando Argo CD"
kubectl create namespace "$NAMESPACE" --dry-run=client -o yaml | kubectl apply -f -

helm repo add argo https://argoproj.github.io/argo-helm
helm repo update

helm upgrade --install argocd argo/argo-cd \
  --namespace "$NAMESPACE" \
  --set global.domain=argocd.agromis.io \
  --set server.ingress.enabled=true \
  --set server.ingress.ingressClassName=nginx \
  --set "server.ingress.annotations.cert-manager\\.io/cluster-issuer=letsencrypt-prod" \
  --set server.ingress.tls=true \
  --wait --timeout 10m

echo "==> Aplicando proyecto y ApplicationSet"
kubectl apply -f applications/projects/agromis-project.yaml
kubectl apply -f applications/agromis-staging-appset.yaml

echo "==> Argo CD listo"
echo "    URL: https://argocd.agromis.io"
echo "    Password inicial: kubectl -n argocd get secret argocd-initial-admin-secret -o jsonpath='{.data.password}' | base64 -d"

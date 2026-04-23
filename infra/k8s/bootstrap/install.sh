#!/usr/bin/env bash
# Script idempotente de bootstrap para cluster EKS vacio
# Uso: ./install.sh staging | production
set -euo pipefail

ENV="${1:-staging}"
VALUES_DIR="$(dirname "$0")/values"

echo "==> Bootstrap del cluster AgroMIS [$ENV]"

# ---- Ingress NGINX -------------------------------------------------------
echo "--> Ingress NGINX"
helm repo add ingress-nginx https://kubernetes.github.io/ingress-nginx
helm repo update
helm upgrade --install ingress-nginx ingress-nginx/ingress-nginx \
  --namespace ingress-nginx --create-namespace \
  --values "$VALUES_DIR/ingress-nginx-values.yaml" \
  --wait --timeout 10m

# ---- cert-manager --------------------------------------------------------
echo "--> cert-manager"
helm repo add jetstack https://charts.jetstack.io
helm upgrade --install cert-manager jetstack/cert-manager \
  --namespace cert-manager --create-namespace \
  --set installCRDs=true \
  --values "$VALUES_DIR/cert-manager-values.yaml" \
  --wait --timeout 10m

# ClusterIssuers Let's Encrypt
kubectl apply -f - <<'EOF'
apiVersion: cert-manager.io/v1
kind: ClusterIssuer
metadata:
  name: letsencrypt-staging
spec:
  acme:
    server: https://acme-staging-v02.api.letsencrypt.org/directory
    email: sre@agromis.io
    privateKeySecretRef:
      name: letsencrypt-staging
    solvers:
      - http01:
          ingress:
            class: nginx
---
apiVersion: cert-manager.io/v1
kind: ClusterIssuer
metadata:
  name: letsencrypt-prod
spec:
  acme:
    server: https://acme-v02.api.letsencrypt.org/directory
    email: sre@agromis.io
    privateKeySecretRef:
      name: letsencrypt-prod
    solvers:
      - http01:
          ingress:
            class: nginx
EOF

# ---- External Secrets Operator -------------------------------------------
echo "--> External Secrets Operator"
helm repo add external-secrets https://charts.external-secrets.io
helm upgrade --install external-secrets external-secrets/external-secrets \
  --namespace external-secrets --create-namespace \
  --values "$VALUES_DIR/external-secrets-values.yaml" \
  --wait --timeout 10m

# ---- kube-prometheus-stack (Prometheus + Grafana) -------------------------
echo "--> kube-prometheus-stack"
helm repo add prometheus-community https://prometheus-community.github.io/helm-charts
helm upgrade --install kube-prometheus-stack prometheus-community/kube-prometheus-stack \
  --namespace monitoring --create-namespace \
  --values "$VALUES_DIR/kube-prometheus-stack-values.yaml" \
  --wait --timeout 15m

# ---- Loki ------------------------------------------------------------------
echo "--> Loki"
helm repo add grafana https://grafana.github.io/helm-charts
helm upgrade --install loki grafana/loki \
  --namespace monitoring \
  --values "$VALUES_DIR/loki-values.yaml" \
  --wait --timeout 10m

# ---- Tempo -----------------------------------------------------------------
echo "--> Tempo"
helm upgrade --install tempo grafana/tempo \
  --namespace monitoring \
  --values "$VALUES_DIR/tempo-values.yaml" \
  --wait --timeout 10m

# ---- Cluster Autoscaler ----------------------------------------------------
echo "--> Cluster Autoscaler"
helm repo add autoscaler https://kubernetes.github.io/autoscaler
helm upgrade --install cluster-autoscaler autoscaler/cluster-autoscaler \
  --namespace kube-system \
  --values "$VALUES_DIR/cluster-autoscaler-values.yaml" \
  --wait --timeout 10m

echo ""
echo "==> Bootstrap completado para ambiente: $ENV"
echo "    kubectl get pods -A  # verificar que todo esta Running"

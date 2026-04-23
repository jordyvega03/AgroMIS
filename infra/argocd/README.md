# Argo CD — GitOps para AgroMIS

## Instalacion

```bash
chmod +x install.sh
./install.sh
```

## Flujo GitOps

1. Merge a `main` en GitHub dispara el workflow `cd-staging.yml`
2. El workflow construye la imagen Docker y la publica en ECR
3. El workflow actualiza el tag en `infra/k8s/apps/<service>/values-staging.yaml`
4. Argo CD detecta el cambio en el repo y sincroniza automaticamente
5. El deploy aparece en la UI de Argo CD con estado y health checks

## URL

- Staging: https://argocd.agromis.io

## Obtener password inicial

```bash
kubectl -n argocd get secret argocd-initial-admin-secret \
  -o jsonpath='{.data.password}' | base64 -d
```

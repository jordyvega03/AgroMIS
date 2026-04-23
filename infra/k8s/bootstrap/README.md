# Bootstrap del Cluster K8s

Script idempotente que instala todos los operadores y herramientas base en un cluster EKS vacio.

## Requisitos

- `kubectl` configurado para el cluster target
- `helm` >= 3.14
- Permisos de admin en el cluster

## Uso

```bash
# Staging
./install.sh staging

# Production
./install.sh production
```

## Componentes instalados

| Componente | Namespace | Descripcion |
|-----------|-----------|-------------|
| Ingress NGINX | `ingress-nginx` | Controlador de ingress con NLB en AWS |
| cert-manager | `cert-manager` | Gestion automatica de certificados TLS (Let's Encrypt) |
| External Secrets Operator | `external-secrets` | Sincroniza secrets desde AWS Secrets Manager |
| kube-prometheus-stack | `monitoring` | Prometheus + Grafana + Alertmanager |
| Loki | `monitoring` | Agregacion de logs |
| Tempo | `monitoring` | Distributed tracing |
| Cluster Autoscaler | `kube-system` | Escalado automatico de nodos EKS |

## Verificacion

```bash
kubectl get pods -n ingress-nginx
kubectl get pods -n cert-manager
kubectl get pods -n external-secrets
kubectl get pods -n monitoring
kubectl get clusterissuer
```

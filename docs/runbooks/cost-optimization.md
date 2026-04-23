# Runbook: Optimizacion de Costos (FinOps)

## Dashboards

| Dashboard | URL |
|-----------|-----|
| Kubecost | https://kubecost.agromis.io |
| AWS Cost Explorer | https://console.aws.amazon.com/cost-management/home |
| AWS Budgets | https://console.aws.amazon.com/billing/home#/budgets |

## Alertas configuradas

| Ambiente | Limite | Alerta al 80% | Alerta al 100% |
|----------|--------|--------------|----------------|
| Staging | $500/mes | Email SRE | Email SRE |
| Production | $3000/mes | Email SRE (75%) | Email SRE (90% forecast) |

## Ver costo por servicio K8s (Kubecost)

1. Ir a https://kubecost.agromis.io
2. Allocations → Group by: Namespace, Deployment, o Pod
3. Filtrar por rango de fechas

## Comandos rapidos de analisis

```bash
# Ver costo estimado por namespace hoy
kubectl cost namespace --show-efficiency

# Ver pods mas costosos
kubectl cost pod --all-namespaces | sort -k4 -rn | head -20
```

## Oportunidades de optimizacion frecuentes

### 1. Nodos spot para batch

Los node groups `batch` ya usan instancias SPOT. Verificar que los jobs de ML del `projection-engine` tengan tolerations al taint `workload=batch`.

### 2. Scale-down en horas no laborables (staging)

Staging puede apagarse fuera de horario laboral (9pm - 6am GT = 3am - 12pm UTC):

```bash
# Apagar todos los deployments en staging
kubectl scale deployment --all -n agromis-staging --replicas=0

# Restaurar
kubectl scale deployment --all -n agromis-staging --replicas=2
```

### 3. Rightsizing de instancias RDS

Revisar mensualmente en AWS Trusted Advisor si las instancias RDS estan sobredimensionadas.

### 4. S3 lifecycle policies

Verificar que las lifecycle policies de S3 esten aplicando transiciones a STANDARD_IA y GLACIER segun lo configurado en el modulo `s3-datalake`.

## Reporte semanal

El canal #finops en Slack recibe un resumen semanal de costos via un Lambda programado. Si no llega, revisar la funcion `agromis-cost-reporter` en AWS Lambda.

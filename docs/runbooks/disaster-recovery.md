# Runbook: Disaster Recovery

## Objetivos de recuperacion

| Metrica | Staging | Produccion |
|---------|---------|------------|
| RPO (perdida maxima de datos) | 24h | 1h |
| RTO (tiempo maximo de recuperacion) | 4h | 1h |

## Estrategia de backups

### Base de datos (RDS PostgreSQL)
- Backups automaticos de RDS: diarios con retencion de 30 dias (prod) / 7 dias (staging)
- Snapshots cross-region: `us-east-1 → us-west-2`, diarios en prod
- Point-in-time recovery disponible con granularidad de 5 minutos

### Estado de Kubernetes
- Velero hace backup diario del namespace `agromis-production` a S3
- TTL de backups: 30 dias

### Codigo y configuracion
- El repositorio Git ES el source of truth de toda la configuracion
- Terraform state en S3 con versioning habilitado

## Escenarios de DR

### Escenario 1: Perdida de pods/deployments

```bash
# ArgoCD sincroniza automaticamente si hay drift
# Si ArgoCD no esta disponible, aplicar manualmente:
kubectl apply -f infra/k8s/apps/backend/
```

### Escenario 2: Perdida de namespace completo

```bash
# Restaurar desde Velero
velero restore create --from-backup agromis-daily-TIMESTAMP \
  --include-namespaces agromis-production
```

### Escenario 3: Fallo de RDS (corrupcion de datos)

```bash
# 1. Identificar snapshot a restaurar
aws rds describe-db-snapshots \
  --db-instance-identifier agromis-production-postgres \
  --query 'DBSnapshots[*].[DBSnapshotIdentifier,SnapshotCreateTime]' \
  --output table

# 2. Restaurar desde snapshot
aws rds restore-db-instance-from-db-snapshot \
  --db-instance-identifier agromis-production-postgres-restored \
  --db-snapshot-identifier <SNAPSHOT_ID> \
  --db-instance-class db.m6g.large \
  --multi-az

# 3. Actualizar el secret en AWS SM con el nuevo endpoint
# 4. ESO actualizara el K8s Secret automaticamente en < 1 min
# 5. Rolling restart de los pods para reconectar
kubectl rollout restart deployment -n agromis-production
```

### Escenario 4: Fallo total de region us-east-1

1. Activar cuenta AWS en region us-west-2
2. Restaurar RDS desde snapshot cross-region
3. Ejecutar `terraform apply` en environments/production/ apuntando a us-west-2
4. Re-aplicar bootstrap de cluster: `./infra/k8s/bootstrap/install.sh production`
5. ArgoCD sincroniza el estado desde Git

## Drill de DR

Ejecutar drill completo una vez por trimestre documentando:
- Tiempo hasta recovery funcional
- Datos perdidos (debe ser < RPO)
- Incidencias encontradas y acciones correctivas

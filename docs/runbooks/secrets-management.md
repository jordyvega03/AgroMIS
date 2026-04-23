# Runbook: Gestion de Secrets

## Principios

- Ningun secret va a Git, ni siquiera cifrado con SealedSecrets.
- Los secrets viven en **AWS Secrets Manager**.
- **External Secrets Operator (ESO)** los sincroniza automaticamente a Kubernetes Secrets.
- La rotacion en AWS SM actualiza el K8s Secret en < 1 minuto (refreshInterval configurado).

## Estructura de secrets en AWS SM

```
agromis/
  staging/
    rds          → {host, port, dbname, username, password}
    redis        → {host, port, password}
    keycloak     → {client_secret, admin_password}
    redpanda     → {bootstrap_servers, schema_registry_url, api_key}
    minio        → {access_key, secret_key}
  production/
    (misma estructura)
```

## Como agregar un nuevo secret

### 1. Crear el secret en AWS SM

```bash
aws secretsmanager create-secret \
  --name agromis/staging/mi-nuevo-servicio \
  --secret-string '{"api_key": "valor-secreto"}' \
  --region us-east-1
```

### 2. Crear un ExternalSecret en K8s

Copiar `infra/k8s/external-secrets/examples/db-credentials.yaml` como base y ajustar:
- `metadata.name`: nombre del Secret K8s resultante
- `metadata.namespace`: namespace del servicio
- `spec.data[].remoteRef.key`: ruta en AWS SM
- `spec.data[].remoteRef.property`: campo dentro del JSON

### 3. Aplicar

```bash
kubectl apply -f mi-external-secret.yaml -n agromis-staging
kubectl get externalsecret -n agromis-staging
kubectl get secret mi-nuevo-servicio -n agromis-staging
```

## Verificar sincronizacion

```bash
kubectl get externalsecret -A
# Columna READY debe ser True
```

## Rotar un secret manualmente

```bash
# Actualizar en AWS SM
aws secretsmanager put-secret-value \
  --secret-id agromis/staging/rds \
  --secret-string '{"password": "nueva-password-segura"}'

# ESO lo detecta en el siguiente refreshInterval (maximo 1 min)
# Verificar:
kubectl get secret agromis-db-credentials -n agromis-staging \
  -o jsonpath='{.data.QUARKUS_DATASOURCE_PASSWORD}' | base64 -d
```

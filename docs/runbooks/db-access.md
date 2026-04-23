# Runbook: Acceso a Base de Datos

## Arquitectura de acceso

RDS PostgreSQL no tiene IP publica. El acceso de desarrolladores se hace via **SSM Port Forwarding** desde una instancia EC2 en la VPC privada. No hay bastion SSH ni VPN.

## Requisitos

- AWS CLI configurado con rol `agromis-developer` o `agromis-sre`
- Plugin Session Manager instalado:
  ```bash
  # macOS
  brew install session-manager-plugin

  # Linux
  curl "https://s3.amazonaws.com/session-manager-downloads/plugin/latest/ubuntu_64bit/session-manager-plugin.deb" -o /tmp/ssm.deb
  sudo dpkg -i /tmp/ssm.deb
  ```
- psql o cualquier cliente Postgres

## Abrir tunel a staging

```bash
./scripts/db-tunnel.sh staging
# Puerto local por defecto: 5433
```

## Abrir tunel a produccion

```bash
./scripts/db-tunnel.sh production 5434
```

## Conectar una vez el tunel esta activo

```bash
psql -h localhost -p 5433 -U agromis -d agromis
```

La password esta en AWS Secrets Manager en `agromis/staging/rds` → campo `password`.

## Obtener la password desde Secrets Manager

```bash
aws secretsmanager get-secret-value \
  --secret-id agromis/staging/rds \
  --query SecretString \
  --output text | python3 -c "import sys,json; print(json.load(sys.stdin)['password'])"
```

## Auditoria

Todos los accesos via SSM quedan registrados en **AWS CloudTrail** bajo el evento `StartSession`. Los logs incluyen el usuario IAM, el timestamp y la instancia accedida.

## Acceso de emergencia a produccion

- Requiere MFA habilitado en el usuario IAM
- Solo roles `agromis-sre` pueden acceder a produccion
- Toda sesion debe documentarse en el canal #incidents de Slack

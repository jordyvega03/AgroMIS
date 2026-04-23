# Docker Compose — Entorno de Desarrollo

Stack local completo para AgroMIS. Un solo comando levanta todas las dependencias.

## Requisitos

- Docker >= 24
- Docker Compose v2 (incluido en Docker Desktop)
- Python 3 (para health check en dev-up.sh)

## Comandos

```bash
# Levantar todo el stack
./scripts/dev-up.sh

# Apagar el stack (conserva datos)
./scripts/dev-down.sh

# Ver logs de todos los servicios
./scripts/dev-logs.sh

# Ver logs de un servicio especifico
./scripts/dev-logs.sh postgres
./scripts/dev-logs.sh redpanda

# Eliminar todo incluyendo volumenes (resetear datos)
docker compose -f infra/docker/docker-compose.dev.yml -p agromis down -v
```

## Servicios y puertos

| Servicio | URL / Puerto | Credenciales |
|---------|-------------|--------------|
| PostgreSQL | `localhost:5432` | `agromis` / `agromis_dev_pass` |
| Redpanda (Kafka) | `localhost:19092` | — |
| Redpanda Console | http://localhost:8080 | — |
| Redis | `localhost:6379` | — |
| Keycloak | http://localhost:8081 | `admin` / `admin` |
| MinIO | http://localhost:9001 | `agromis` / `agromis_minio_pass` |
| Prometheus | http://localhost:9090 | — |
| Grafana | http://localhost:3000 | `admin` / `admin` |
| Loki | `localhost:3100` | — |
| Tempo | `localhost:3200` | — |
| Jaeger UI | http://localhost:16686 | — |

## Verificacion

```bash
# Postgres + extensiones
psql -h localhost -U agromis -d agromis -c "SELECT PostGIS_version();"
psql -h localhost -U agromis -d agromis -c "SELECT default_version FROM pg_available_extensions WHERE name='timescaledb';"

# Redpanda
rpk topic list --brokers localhost:19092
```

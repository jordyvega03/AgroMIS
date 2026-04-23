# AgroMIS Platform

Sistema de Informacion Agricola para Centroamerica.

## Descripcion

AgroMIS es una plataforma de informacion agricola multi-pais, multi-tenancy, diseñada para Centroamerica. Centraliza reportes de siembra, proyecciones de cosecha, precios de mercado y datos meteorologicos para productores, cooperativas, ministerios e investigadores.

## Arquitectura

- **Backend**: Quarkus modular monolith (Java 21)
- **Projection Engine**: FastAPI + Prophet (Python)
- **Mobile**: Flutter (Android/iOS)
- **Web**: React + MapLibre
- **Infra**: AWS EKS + RDS PostgreSQL/PostGIS/TimescaleDB + Redpanda

Ver documentacion completa en [docs/architecture/](docs/architecture/).

## Como arrancar el entorno local

### Requisitos

- Docker >= 24 y Docker Compose v2
- Java 21 (GraalVM recomendado)
- Flutter 3.x
- Node.js 20+
- Python 3.12+
- Terraform 1.7+
- Helm 3.14+
- kubectl

### Levantar dependencias locales

```bash
./scripts/dev-up.sh
```

Esto levanta: PostgreSQL+PostGIS+TimescaleDB, Redpanda, Redis, Keycloak, MinIO, Prometheus, Grafana, Loki, Tempo.

### Apagar el entorno

```bash
./scripts/dev-down.sh
```

### Ver logs

```bash
./scripts/dev-logs.sh [servicio]
```

### Aplicar migraciones de base de datos

```bash
./gradlew :migrations:flywayMigrate
```

## Estructura del monorepo

```
agromis-platform/
├── backend/           # Quarkus modular monolith
├── projection-engine/ # Python FastAPI + ML
├── mobile/            # Flutter
├── web/               # React
├── infra/             # IaC (Terraform, K8s, Docker)
├── docs/              # Arquitectura, ADRs, runbooks
├── scripts/           # Shell helpers
└── tests/             # Load tests y pruebas de integracion
```

## Links

- [Documento de Arquitectura](docs/architecture/AGRO_MIS_ARCHITECTURE.md)
- [ADRs](docs/adr/README.md)
- [Runbooks](docs/runbooks/)
- [Guia de contribucion](docs/contributing.md)

## Licencia

Apache 2.0 — ver [LICENSE](LICENSE).

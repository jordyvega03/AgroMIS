# Épica 01 — Infraestructura Base & DevOps

> **Objetivo**: dejar el terreno listo para que equipos de backend y móvil puedan construir sin fricciones. Todo lo que aquí se construya es reusable por las demás épicas.
> **Duración estimada**: 3 semanas
> **Tareas**: 18

---

## TASK-001 · Inicializar monorepo con estructura base · [P0] [INFRA] [DOC]

### Contexto
No existe nada. Partimos de cero. Vamos a crear un monorepo Git con la estructura que alojará backend Java, proyecto Python (motor de proyecciones), mobile Flutter, web React, infra-as-code y documentación.

### Objetivo
Repositorio `agromis-platform` inicializado con estructura de carpetas, `README`, `.gitignore`, `.editorconfig`, `LICENSE`, plantillas de PR y CODEOWNERS.

### Archivos a crear
```
agromis-platform/
├── README.md
├── LICENSE                    # Apache 2.0
├── .gitignore                 # Java + Python + Flutter + Node + IDE
├── .editorconfig
├── .github/
│   ├── CODEOWNERS
│   ├── PULL_REQUEST_TEMPLATE.md
│   └── ISSUE_TEMPLATE/
│       ├── bug_report.md
│       └── feature_request.md
├── backend/                   # Quarkus modular monolith (vacío)
├── projection-engine/         # Python FastAPI (vacío)
├── mobile/                    # Flutter (vacío)
├── web/                       # React (vacío)
├── infra/
│   ├── terraform/             # IaC
│   ├── k8s/                   # manifests
│   └── docker/                # dev compose
├── docs/
│   ├── architecture/          # copiar AGRO_MIS_ARCHITECTURE.md aquí
│   ├── adr/                   # Architecture Decision Records
│   └── runbooks/
└── scripts/                   # shell helpers
```

### Criterios de aceptación
- [ ] `git log --oneline` muestra commit inicial firmado
- [ ] `README.md` explica qué es el proyecto, cómo arrancar dev env, links a docs
- [ ] `.gitignore` cubre: `target/`, `build/`, `.idea/`, `.vscode/`, `node_modules/`, `.dart_tool/`, `__pycache__/`, `.env*`, `*.log`
- [ ] `CODEOWNERS` define owners por carpeta (placeholder por ahora)
- [ ] Branch default: `main`. Branch protection rules definidas (no push directo, require PR + 1 review)

### Verificación
```bash
git status  # clean
tree -L 2 -a  # estructura correcta
```

### Dependencias
Ninguna.

---

## TASK-002 · ADR-000 e índice de ADRs · [P0] [DOC]

### Contexto
Las decisiones técnicas deben quedar documentadas. Adoptamos formato **MADR** (Markdown Architectural Decision Records).

### Objetivo
Plantilla de ADR creada + primeros 12 ADRs del documento de arquitectura transcritos al formato MADR en `docs/adr/`.

### Archivos a crear
- `docs/adr/000-use-madr.md` — meta-ADR sobre usar MADR
- `docs/adr/001-modular-monolith.md`
- `docs/adr/002-postgres-as-backbone.md`
- `docs/adr/003-shared-db-multi-tenant-rls.md`
- `docs/adr/004-h3-geospatial-indexing.md`
- `docs/adr/005-jsonb-event-store.md`
- `docs/adr/006-no-satellite-in-mvp.md`
- `docs/adr/007-sms-ussd-first-class.md`
- `docs/adr/008-prophet-baseline-ml.md`
- `docs/adr/009-flutter-for-mobile.md`
- `docs/adr/010-redpanda-over-kafka.md`
- `docs/adr/011-maplibre-over-mapbox.md`
- `docs/adr/012-k-anonymity-default.md`
- `docs/adr/README.md` — índice con estado (proposed/accepted/deprecated/superseded)

### Formato MADR por archivo
```markdown
# ADR-XXX: Título

## Status
Accepted (2026-04-20)

## Context
Qué problema estamos resolviendo.

## Decision
Qué decidimos.

## Consequences
- Positivas: ...
- Negativas: ...
- Neutrales: ...

## Alternatives considered
- Alternativa A: por qué no
- Alternativa B: por qué no
```

### Criterios de aceptación
- [ ] 12 ADRs + plantilla creados
- [ ] `docs/adr/README.md` lista todos con estado
- [ ] Cada ADR referencia secciones del documento de arquitectura principal

### Dependencias
TASK-001

---

## TASK-003 · Docker Compose para entorno dev local · [P0] [INFRA]

### Contexto
Cada dev debe poder levantar todas las dependencias locales con un solo comando. Esto incluye PostgreSQL+PostGIS+Timescale, Redpanda, Redis, Keycloak, MinIO (S3-compat), Prometheus, Grafana, Loki.

### Objetivo
`docker-compose.dev.yml` funcional que levanta stack completo en < 3 min.

### Archivos a crear
- `infra/docker/docker-compose.dev.yml`
- `infra/docker/postgres/init/01-extensions.sql` (crea `postgis`, `timescaledb`, `pgcrypto`, `uuid-ossp`, `h3`)
- `infra/docker/postgres/init/02-databases.sql` (crea `agromis`, `keycloak`, `agromis_timescale`)
- `infra/docker/keycloak/realm-export.json` (realm `agromis` preconfigurado)
- `infra/docker/grafana/provisioning/datasources/` (Prometheus, Loki)
- `infra/docker/prometheus/prometheus.yml`
- `scripts/dev-up.sh`, `scripts/dev-down.sh`, `scripts/dev-logs.sh`
- `infra/docker/README.md`

### Servicios y puertos
| Servicio | Imagen | Puerto local |
|----------|--------|--------------|
| PostgreSQL | `timescale/timescaledb-ha:pg16-all` (incluye PostGIS + Timescale) | 5432 |
| Redpanda | `redpandadata/redpanda:latest` | 9092, 9644 |
| Redpanda Console | `redpandadata/console:latest` | 8080 |
| Redis | `redis:7-alpine` | 6379 |
| Keycloak | `quay.io/keycloak/keycloak:24.0` | 8081 |
| MinIO | `minio/minio:latest` | 9000, 9001 |
| Prometheus | `prom/prometheus:latest` | 9090 |
| Grafana | `grafana/grafana:latest` | 3000 |
| Loki | `grafana/loki:latest` | 3100 |
| Tempo | `grafana/tempo:latest` | 3200 |
| Jaeger UI | vía Tempo | 16686 |

### Criterios de aceptación
- [ ] `./scripts/dev-up.sh` levanta todos los servicios y termina healthy en < 3 min
- [ ] `psql -h localhost -U agromis` conecta y las extensiones están instaladas (`SELECT PostGIS_version(); SELECT default_version FROM pg_available_extensions WHERE name='timescaledb';`)
- [ ] `rpk topic list` desde el host funciona
- [ ] Keycloak en http://localhost:8081 muestra realm `agromis`
- [ ] Grafana en http://localhost:3000 (admin/admin) ya tiene datasources Prometheus y Loki
- [ ] Volúmenes persistentes nombrados (no anónimos) para no perder datos en `docker-compose down`

### Verificación
```bash
./scripts/dev-up.sh
docker compose -f infra/docker/docker-compose.dev.yml ps  # todos healthy
psql -h localhost -U agromis -d agromis -c "SELECT PostGIS_version();"
```

### Dependencias
TASK-001

---

## TASK-004 · Terraform backbone: VPC + EKS + RDS + ElastiCache · [P0] [INFRA]

### Contexto
Necesitamos infra cloud para staging y producción. Terraform como IaC. Proveedor AWS (según ADR-006 del doc — o Azure si cambian). Este task asume AWS.

### Objetivo
Módulos Terraform que aprovisionan VPC, EKS, RDS PostgreSQL, ElastiCache Redis. Stack staging desplegable con `terraform apply`.

### Archivos a crear
```
infra/terraform/
├── modules/
│   ├── vpc/
│   ├── eks/
│   ├── rds-postgres/
│   ├── elasticache-redis/
│   ├── s3-datalake/
│   └── iam-oidc/
├── environments/
│   ├── staging/
│   │   ├── main.tf
│   │   ├── variables.tf
│   │   ├── outputs.tf
│   │   └── backend.tf         # S3 + DynamoDB lock
│   └── production/
└── README.md
```

### Especificaciones técnicas
- **Región primaria**: `us-east-1`
- **VPC**: 3 AZs, subnets públicas (NAT), privadas (nodes), aisladas (DB)
- **EKS**: v1.29, 2 node groups (`core` t3.large on-demand, `batch` t3.medium spot), IRSA habilitado
- **RDS**: PostgreSQL 16, Multi-AZ en prod, single-AZ en staging, `db.t4g.medium` staging / `db.m6g.large` prod, storage gp3 encriptado con KMS CMK, automated backups 7/30 días
- **Parameter group** custom con `shared_preload_libraries = 'timescaledb,pg_stat_statements'`
- **ElastiCache Redis**: 7.x, cluster mode disabled para MVP, `cache.t4g.small` staging
- **S3**: 3 buckets: `agromis-datalake-{env}`, `agromis-raw-{env}`, `agromis-backups-{env}` con versioning, lifecycle a IA tras 90d, a Glacier tras 365d
- **State backend**: S3 + DynamoDB lock por environment

### Criterios de aceptación
- [ ] `terraform init` y `terraform plan` pasan en `environments/staging/`
- [ ] `terraform apply` aprovisiona staging en < 25 min
- [ ] `aws eks update-kubeconfig --name agromis-staging` y `kubectl get nodes` funciona
- [ ] RDS accesible solo desde EKS (security group), no público
- [ ] Tags AWS consistentes: `Project=agromis`, `Environment=staging|prod`, `ManagedBy=terraform`
- [ ] Outputs incluyen endpoints para consumir en otros módulos

### Notas
- Habilitar **cluster autoscaler** y **metrics-server** via Helm en task separado (TASK-007).
- IAM OIDC provider habilitado para IRSA (IAM Roles for Service Accounts).

### Dependencias
TASK-001

---

## TASK-005 · Esquema inicial PostgreSQL + migrations con Flyway · [P0] [INFRA] [DB]

### Contexto
Necesitamos un lugar canónico donde vivan las migraciones de DB. Flyway como herramienta (más simple que Liquibase, alineado con Quarkus).

### Objetivo
Estructura Flyway en `backend/migrations/` + primera migración que crea schema compartido (extensiones, tabla `countries`, `tenants`, `crop_catalog`), inicializa RLS, crea rol app.

### Archivos a crear
```
backend/migrations/
├── build.gradle.kts                # proyecto Gradle standalone para correr migraciones
├── src/main/resources/db/migration/
│   ├── V001__initial_extensions.sql
│   ├── V002__base_tables.sql
│   ├── V003__rls_policies.sql
│   ├── V004__seed_countries.sql
│   └── V005__seed_crop_catalog.sql
└── README.md
```

### Contenido específico

**V001__initial_extensions.sql**
```sql
CREATE EXTENSION IF NOT EXISTS "uuid-ossp";
CREATE EXTENSION IF NOT EXISTS "pgcrypto";
CREATE EXTENSION IF NOT EXISTS "postgis";
CREATE EXTENSION IF NOT EXISTS "timescaledb";
-- h3 extension is optional — instalar si h3-pg disponible
-- CREATE EXTENSION IF NOT EXISTS "h3";

-- Función UUID v7 (hasta que PG la tenga nativa)
CREATE OR REPLACE FUNCTION uuid_generate_v7() RETURNS uuid AS $$
  SELECT encode(
    set_bit(
      set_bit(
        overlay(uuid_send(gen_random_uuid())
                PLACING substring(int8send(floor(extract(epoch from clock_timestamp()) * 1000)::bigint) from 3)
                FROM 1 FOR 6),
        52, 1),
      53, 1)::uuid
  , 'hex')::uuid;
$$ LANGUAGE SQL VOLATILE;
```

**V002__base_tables.sql**
```sql
CREATE TABLE countries (
    code CHAR(2) PRIMARY KEY,
    name VARCHAR(100) NOT NULL,
    iso_3 CHAR(3) NOT NULL,
    currency_code CHAR(3) NOT NULL,
    timezone VARCHAR(50) NOT NULL,
    default_language CHAR(3) NOT NULL,
    active BOOLEAN NOT NULL DEFAULT true,
    metadata JSONB NOT NULL DEFAULT '{}'
);

CREATE TABLE crop_catalog (
    id UUID PRIMARY KEY DEFAULT uuid_generate_v7(),
    country_code CHAR(2) NOT NULL REFERENCES countries(code),
    crop_code VARCHAR(32) NOT NULL,
    common_name VARCHAR(100) NOT NULL,
    scientific_name VARCHAR(200),
    typical_cycle_days SMALLINT,
    category VARCHAR(40),           -- VEGETABLE | GRAIN | FRUIT | ...
    active BOOLEAN NOT NULL DEFAULT true,
    UNIQUE (country_code, crop_code)
);

-- tenant = concept lógico; 1:1 con country en MVP
CREATE TABLE tenants (
    id UUID PRIMARY KEY DEFAULT uuid_generate_v7(),
    code VARCHAR(32) NOT NULL UNIQUE,
    country_code CHAR(2) NOT NULL REFERENCES countries(code),
    name VARCHAR(200) NOT NULL,
    active BOOLEAN NOT NULL DEFAULT true,
    created_at TIMESTAMPTZ NOT NULL DEFAULT now()
);
```

**V003__rls_policies.sql**
```sql
-- Rol app que ejecuta todas las queries desde Quarkus
CREATE ROLE agromis_app LOGIN PASSWORD :'app_password';  -- passed via flyway placeholder
GRANT CONNECT ON DATABASE agromis TO agromis_app;
GRANT USAGE ON SCHEMA public TO agromis_app;
GRANT SELECT, INSERT, UPDATE, DELETE ON ALL TABLES IN SCHEMA public TO agromis_app;
GRANT USAGE, SELECT ON ALL SEQUENCES IN SCHEMA public TO agromis_app;
ALTER DEFAULT PRIVILEGES IN SCHEMA public
  GRANT SELECT, INSERT, UPDATE, DELETE ON TABLES TO agromis_app;

-- RLS se habilitará tabla por tabla en migraciones posteriores.
-- Aquí solo se define la función helper:
CREATE OR REPLACE FUNCTION current_country_code() RETURNS TEXT AS $$
  SELECT current_setting('app.current_country', true);
$$ LANGUAGE SQL STABLE;
```

**V004__seed_countries.sql** — insert GT, SV, HN, NI, CR, PA, BZ con metadata.

**V005__seed_crop_catalog.sql** — insert para GT los cultivos piloto: TOMATO, ONION, POTATO, MAIZE, BEAN, CARROT, CABBAGE.

### Criterios de aceptación
- [ ] `./gradlew :migrations:flywayMigrate` aplica todas las migraciones en dev
- [ ] `./gradlew :migrations:flywayInfo` muestra 5 migrations `Success`
- [ ] `SELECT * FROM countries` devuelve 7 filas
- [ ] `SELECT * FROM crop_catalog WHERE country_code='GT'` devuelve ≥ 7 filas
- [ ] Rol `agromis_app` existe y puede conectarse
- [ ] Función `current_country_code()` existe

### Dependencias
TASK-003 (Postgres local corriendo)

---

## TASK-006 · Helm charts base para servicios Quarkus · [P0] [INFRA]

### Contexto
Cuando empecemos a desplegar servicios backend, necesitamos un chart Helm reutilizable. Un solo chart parametrizable por values.yaml.

### Objetivo
Helm chart `agromis-service` generico para cualquier servicio Quarkus con convenciones consistentes (probes, metrics, config, secrets, HPA).

### Archivos a crear
```
infra/k8s/charts/agromis-service/
├── Chart.yaml
├── values.yaml
├── values-staging.yaml
├── values-production.yaml
└── templates/
    ├── _helpers.tpl
    ├── deployment.yaml
    ├── service.yaml
    ├── ingress.yaml
    ├── configmap.yaml
    ├── secret.yaml           # placeholder — real secrets via External Secrets Operator
    ├── hpa.yaml
    ├── pdb.yaml              # PodDisruptionBudget
    ├── servicemonitor.yaml   # Prometheus Operator CR
    └── networkpolicy.yaml
```

### Especificaciones
- **Probes**: `/q/health/live`, `/q/health/ready`, `/q/health/started` (endpoints Quarkus)
- **Metrics**: scrape `/q/metrics` en puerto 8080, ServiceMonitor para Prometheus Operator
- **Resources defaults**: requests 250m/512Mi, limits 1/1Gi
- **HPA**: min 2, max 10, target CPU 70%
- **PDB**: minAvailable 1
- **NetworkPolicy**: egress permitido solo a DB, Kafka, S3, OIDC, otros servicios del mismo namespace
- **Service account** por servicio con IRSA annotation

### values.yaml template
```yaml
image:
  repository: ""
  tag: ""
  pullPolicy: IfNotPresent

replicas: 2

resources:
  requests: { cpu: 250m, memory: 512Mi }
  limits: { cpu: 1000m, memory: 1Gi }

env:
  # llave=valor
config:
  # pares para ConfigMap

secretEnvFrom: []  # lista de secrets referenciados

ingress:
  enabled: false
  host: ""
  className: nginx

serviceAccount:
  create: true
  annotations: {}   # IRSA role ARN

hpa:
  enabled: true
  minReplicas: 2
  maxReplicas: 10

podSecurityContext:
  runAsNonRoot: true
  runAsUser: 1001
  fsGroup: 1001

securityContext:
  allowPrivilegeEscalation: false
  readOnlyRootFilesystem: true
  capabilities:
    drop: [ALL]
```

### Criterios de aceptación
- [ ] `helm lint infra/k8s/charts/agromis-service` pasa
- [ ] `helm template` renderiza sin errores con values staging
- [ ] Chart incluye README con tabla de values parametrizables

### Dependencias
TASK-004

---

## TASK-007 · Bootstrap cluster K8s: ingress, cert-manager, external-secrets, prometheus · [P0] [INFRA]

### Contexto
El EKS recién creado está vacío. Necesitamos operadores base antes de desplegar nada de aplicación.

### Objetivo
Script idempotente que instala Ingress-NGINX, cert-manager, External Secrets Operator, Prometheus Operator + Grafana (kube-prometheus-stack), Loki, Tempo, Cluster Autoscaler.

### Archivos a crear
- `infra/k8s/bootstrap/README.md`
- `infra/k8s/bootstrap/install.sh`
- `infra/k8s/bootstrap/values/` — values.yaml por cada helm chart
  - `ingress-nginx-values.yaml`
  - `cert-manager-values.yaml`
  - `external-secrets-values.yaml`
  - `kube-prometheus-stack-values.yaml`
  - `loki-values.yaml`
  - `tempo-values.yaml`
  - `cluster-autoscaler-values.yaml`

### Criterios de aceptación
- [ ] `./install.sh staging` instala todo en < 20 min sobre cluster EKS vacío
- [ ] `kubectl get pods -n ingress-nginx` todos `Running`
- [ ] ClusterIssuer de Let's Encrypt configurado (staging y prod)
- [ ] Grafana accesible vía ingress con cert TLS válido
- [ ] Prometheus targets incluyen kube-state-metrics, node-exporter, apiserver

### Dependencias
TASK-004 (cluster existe)

---

## TASK-008 · CI pipeline base en GitHub Actions · [P0] [INFRA]

### Contexto
Cada PR debe correr lint + tests + security scan automáticamente. Cada merge a `main` debe construir y publicar imagen.

### Objetivo
Workflow `ci.yml` que detecta qué cambió (backend/mobile/web/infra) y corre los jobs relevantes.

### Archivos a crear
- `.github/workflows/ci.yml`
- `.github/workflows/cd-staging.yml`
- `.github/workflows/dependabot.yml` (config)
- `.github/dependabot.yml`
- `.github/actions/` — composite actions reutilizables
  - `setup-java/action.yml`
  - `setup-flutter/action.yml`
  - `docker-build-push/action.yml`

### Jobs del CI
| Job | Trigger | Pasos |
|-----|---------|-------|
| `detect-changes` | todos los PRs | path filters → outputs |
| `backend-lint-test` | cambios en `backend/**` | Gradle check, Testcontainers, JaCoCo |
| `python-lint-test` | cambios en `projection-engine/**` | ruff + pytest + coverage |
| `mobile-lint-test` | cambios en `mobile/**` | flutter analyze + flutter test |
| `web-lint-test` | cambios en `web/**` | pnpm lint + vitest |
| `infra-validate` | cambios en `infra/**` | terraform fmt + validate + tflint + checkov + helm lint |
| `security-scan` | todos | Trivy FS, Gitleaks, OSV-scanner |

### Criterios de aceptación
- [ ] PR de prueba dispara jobs correctos según paths cambiados
- [ ] Jobs completan en < 8 min promedio
- [ ] Coverage mínimo enforced: 70% backend, 60% mobile
- [ ] Check de commits firmados habilitado
- [ ] Status checks requeridos en branch protection

### Dependencias
TASK-001

---

## TASK-009 · CD pipeline staging con Argo CD · [P1] [INFRA]

### Contexto
Tras merge a `main`, las imágenes deben desplegarse automáticamente a staging vía GitOps.

### Objetivo
Argo CD instalado en cluster staging + `ApplicationSet` que watcheaa el repo y sincroniza los charts.

### Archivos a crear
- `infra/argocd/README.md`
- `infra/argocd/install.sh`
- `infra/argocd/applications/`
  - `agromis-staging-appset.yaml` — ApplicationSet con generator de lista de servicios
  - `projects/agromis-project.yaml`
- `infra/argocd/sealed-secrets/` — configuración sealed-secrets para secrets GitOps

### Criterios de aceptación
- [ ] Argo CD UI accesible vía ingress
- [ ] Bumpear tag en `infra/k8s/apps/<service>/values-staging.yaml` dispara sync automático
- [ ] Auto-prune habilitado para staging, manual para prod
- [ ] Notificaciones a Slack sobre deployments

### Dependencias
TASK-007, TASK-008

---

## TASK-010 · Observabilidad: OpenTelemetry instrumentation + dashboards base · [P1] [OBS]

### Contexto
Los servicios deben emitir traces, logs estructurados y métricas desde el primer deploy. OTel collector en cluster.

### Objetivo
OTel collector desplegado + dashboards Grafana base (golden signals) + alerts básicas en Alertmanager.

### Archivos a crear
- `infra/k8s/otel/otel-collector-values.yaml`
- `infra/k8s/otel/instrumentation-java.yaml` — CR de operator auto-instrumentation
- `infra/grafana/dashboards/golden-signals.json`
- `infra/grafana/dashboards/jvm-quarkus.json`
- `infra/grafana/dashboards/postgres.json`
- `infra/grafana/dashboards/kafka.json`
- `infra/prometheus/rules/sre-alerts.yaml`

### Criterios de aceptación
- [ ] Un servicio Quarkus de prueba emite traces visibles en Tempo
- [ ] Logs estructurados JSON visibles en Loki con correlation-id
- [ ] Dashboard "Golden Signals" muestra rate/errors/duration/saturation por servicio
- [ ] Alerta `HighErrorRate` dispara con tasa > 5% por 5 min

### Dependencias
TASK-007

---

## TASK-011 · Redpanda topics iniciales + Schema Registry · [P0] [INFRA] [EVENT]

### Contexto
Los eventos del dominio viven en Redpanda. Los topics deben crearse con configuración explícita (no auto-create).

### Objetivo
Script idempotente que crea topics iniciales en dev y staging + Schema Registry configurado para Avro.

### Archivos a crear
- `infra/kafka/topics.yaml` — declarativo
- `infra/kafka/create-topics.sh`
- `infra/kafka/schemas/` — copiar schemas del Annex A del doc arquitectura
  - `planting-report-submitted-v1.avsc`
  - `projection-updated-v1.avsc`
  - `price-observed-v1.avsc`
  - `weather-observation-ingested-v1.avsc`
  - `event-envelope-v1.avsc`
- `infra/kafka/register-schemas.sh`

### Topics iniciales
| Topic | Particiones dev / prod | Retention |
|-------|-----------------------|-----------|
| `agromis.reports.planting.v1` | 3 / 24 | 30d |
| `agromis.projections.updated.v1` | 3 / 12 | 90d compacted |
| `agromis.prices.observed.v1` | 3 / 12 | 30d |
| `agromis.weather.observed.v1` | 3 / 12 | 14d |
| `agromis.reputation.changed.v1` | 3 / 6 | compacted |
| `agromis.notifications.dispatch.v1` | 3 / 24 | 7d |
| `*.dlq` | 1 / 3 | 30d |

### Criterios de aceptación
- [ ] `./create-topics.sh local` crea todos los topics en dev
- [ ] `rpk topic list` muestra topics con particiones correctas
- [ ] Schemas registrados con compatibilidad `BACKWARD`
- [ ] `./register-schemas.sh local` es idempotente

### Dependencias
TASK-003

---

## TASK-012 · External Secrets: integración Vault / AWS Secrets Manager · [P1] [INFRA] [SEC]

### Contexto
Secrets no van a Git, ni siquiera con SealedSecrets. Los services leen de AWS Secrets Manager vía External Secrets Operator.

### Objetivo
External Secrets Operator configurado + primer ClusterSecretStore + ejemplo ExternalSecret template.

### Archivos a crear
- `infra/k8s/external-secrets/cluster-secret-store.yaml`
- `infra/k8s/external-secrets/examples/db-credentials.yaml`
- `docs/runbooks/secrets-management.md`

### Criterios de aceptación
- [ ] ExternalSecret de prueba genera Secret sincronizado desde AWS SM
- [ ] Rotación manual en AWS SM actualiza Secret K8s en < 1 min
- [ ] Runbook documenta cómo agregar nuevo secret

### Dependencias
TASK-007

---

## TASK-013 · Bastion host + VPN para acceso DB · [P1] [INFRA] [SEC]

### Contexto
DB no es pública. Devs necesitan acceso para debugging. Opción: bastion EC2 pequeño o AWS Session Manager + RDS IAM auth.

### Objetivo
Acceso controlado a RDS vía SSM Port Forwarding (sin bastion, sin VPN).

### Archivos a crear
- `infra/terraform/modules/bastion-ssm/`
- `scripts/db-tunnel.sh` — abre túnel SSM a RDS
- `docs/runbooks/db-access.md`

### Criterios de aceptación
- [ ] `./scripts/db-tunnel.sh staging` abre túnel local al RDS
- [ ] Acceso auditado en CloudTrail
- [ ] Solo roles IAM `agromis-developer` y `agromis-sre` pueden abrir túnel

### Dependencias
TASK-004

---

## TASK-014 · Backups automáticos y DR plan · [P1] [INFRA]

### Contexto
Necesitamos restore testeable. Backups nativos de RDS + snapshots cross-region + backup de configs K8s.

### Objetivo
Backups automáticos configurados + procedimiento de restore documentado y testeado una vez.

### Archivos a crear
- `infra/terraform/modules/backups/`
- `docs/runbooks/disaster-recovery.md`
- `docs/runbooks/restore-procedure.md`
- `scripts/backup-k8s-state.sh` (Velero install + schedule)

### Criterios de aceptación
- [ ] RDS automated backups 30 días en prod, 7 en staging
- [ ] Cross-region snapshots en prod (us-east-1 → us-west-2) diarios
- [ ] Velero instalado, backup diario a S3
- [ ] Runbook incluye pasos de restore verificados una vez (test drill)

### Dependencias
TASK-004

---

## TASK-015 · Política de tags de release + semver · [P2] [DOC]

### Contexto
Sin disciplina de versionado, el CD es caos. Adoptamos SemVer + Conventional Commits + release-please.

### Objetivo
`release-please` automatiza changelog y tags desde conventional commits.

### Archivos a crear
- `.github/workflows/release-please.yml`
- `.release-please-manifest.json`
- `release-please-config.json`
- `docs/contributing.md` sección Conventional Commits

### Criterios de aceptación
- [ ] Merge a main con `feat:` o `fix:` crea/actualiza PR de release
- [ ] Tags semver generados por componente (backend, mobile, web)
- [ ] CHANGELOG.md por componente

### Dependencias
TASK-008

---

## TASK-016 · Load test baseline con k6 · [P2] [TEST]

### Contexto
Antes de producción necesitamos baseline de performance.

### Objetivo
Scripts k6 para flujos críticos + ejecución weekly contra staging.

### Archivos a crear
- `tests/load/README.md`
- `tests/load/scenarios/submit-report.js`
- `tests/load/scenarios/query-gis-tiles.js`
- `tests/load/scenarios/get-projection.js`
- `tests/load/common/auth.js`
- `.github/workflows/load-test-weekly.yml`

### Criterios de aceptación
- [ ] `k6 run scenarios/submit-report.js` corre localmente contra staging
- [ ] Targets definidos: submit-report p95 < 500ms a 100 rps sostenidos
- [ ] Resultados publicados a Grafana (k6 InfluxDB output)

### Dependencias
TASK-009, y al menos algunos endpoints funcionales (TASK-309 en EPIC 03)

---

## TASK-017 · Security baseline: CIS benchmarks + image scanning · [P2] [SEC]

### Contexto
Seguridad no es opcional. Pre-producción necesitamos baseline.

### Objetivo
kube-bench en CronJob + Trivy operator + Polaris + alertas.

### Archivos a crear
- `infra/k8s/security/kube-bench-cronjob.yaml`
- `infra/k8s/security/trivy-operator-values.yaml`
- `infra/k8s/security/polaris-values.yaml`
- `docs/runbooks/security-findings-triage.md`

### Criterios de aceptación
- [ ] kube-bench corre semanal, resultados a Prometheus
- [ ] Trivy operator scanea todas las imágenes desplegadas
- [ ] Polaris muestra score de buenas prácticas > 90%

### Dependencias
TASK-007

---

## TASK-018 · Cost tracking: Kubecost + AWS Cost Explorer tags · [P3] [INFRA]

### Contexto
FinOps desde día 1. Saber cuánto cuesta cada ambiente y servicio.

### Objetivo
Kubecost instalado + budgets alerting + dashboards.

### Archivos a crear
- `infra/k8s/kubecost/values.yaml`
- `infra/terraform/modules/cost-alerts/` — AWS Budgets
- `docs/runbooks/cost-optimization.md`

### Criterios de aceptación
- [ ] Kubecost muestra costo por namespace/pod
- [ ] Budget AWS alerta cuando staging > USD 500/mes, prod > USD 3000/mes
- [ ] Weekly cost report en Slack

### Dependencias
TASK-007

---

## Resumen épica 01

| # | Tarea | Prioridad | Dependencias clave |
|---|-------|-----------|--------------------|
| 001 | Init monorepo | P0 | — |
| 002 | ADRs | P0 | 001 |
| 003 | Docker compose dev | P0 | 001 |
| 004 | Terraform VPC/EKS/RDS | P0 | 001 |
| 005 | Flyway migrations base | P0 | 003 |
| 006 | Helm chart base | P0 | 004 |
| 007 | Bootstrap cluster | P0 | 004 |
| 008 | CI pipelines | P0 | 001 |
| 009 | CD Argo | P1 | 007, 008 |
| 010 | Observability | P1 | 007 |
| 011 | Kafka topics | P0 | 003 |
| 012 | External secrets | P1 | 007 |
| 013 | DB access | P1 | 004 |
| 014 | Backups + DR | P1 | 004 |
| 015 | Release process | P2 | 008 |
| 016 | Load tests | P2 | 009 |
| 017 | Security baseline | P2 | 007 |
| 018 | Cost tracking | P3 | 007 |

**Paralelización**: tareas 001, 002 secuencial. Después 003, 004, 008 pueden ir en paralelo. 005 tras 003. 007 tras 004. 006 tras 004. 011 tras 003. El resto tras sus deps explícitas.

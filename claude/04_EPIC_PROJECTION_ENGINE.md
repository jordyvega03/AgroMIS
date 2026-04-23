# Épica 04 — Motor de Proyecciones (Python / FastAPI)

> **Objetivo**: construir el **Core Domain** del sistema — el motor que convierte reportes + clima + precios en proyecciones de sobreoferta/escasez. Servicio separado del monolith Java (por stack y perfil de carga).
> **Duración estimada**: 4 semanas
> **Tareas**: 14

---

## Contexto general de la épica

Stack:
- **Python 3.12**
- **FastAPI** para HTTP
- **uvicorn + gunicorn** para serving
- **pandas / polars** para feature engineering
- **statsmodels / prophet** para baselines
- **scikit-learn / xgboost** para Fase 2 (no MVP)
- **MLflow** como registry de modelos
- **Prefect** para orquestación de training
- **psycopg[binary]** + **SQLAlchemy** para PG
- **confluent-kafka-python** para eventing
- **fastavro** para serialización
- **pytest** + **pytest-asyncio** para tests

Ubicación en repo: `projection-engine/`

El servicio:
1. **Consume eventos** desde Kafka: `PlantingReportSubmitted`, `PriceObserved`, `WeatherObservationIngested`, `DemandDeclared`.
2. **Mantiene un feature store** (tabla PG con features pre-computadas).
3. **Ejecuta training batch** nocturno (Prefect).
4. **Sirve queries de proyección** vía FastAPI (lee de tabla `projections` que es CQRS-write).
5. **Publica eventos** `ProjectionUpdated.v1` al cambiar resultados.

---

## TASK-401 · Scaffold proyecto Python + FastAPI · [P0] [INFRA]

### Contexto
Crear el proyecto Python con estructura, deps, Dockerfile.

### Objetivo
Esqueleto funcional con `uv` como package manager (más rápido que poetry).

### Archivos a crear
```
projection-engine/
├── pyproject.toml
├── uv.lock
├── .python-version              # 3.12
├── Dockerfile
├── .dockerignore
├── README.md
├── src/
│   └── projection_engine/
│       ├── __init__.py
│       ├── main.py               # FastAPI app
│       ├── api/
│       ├── domain/
│       ├── infrastructure/
│       ├── application/
│       ├── training/
│       └── config.py             # Pydantic Settings
├── tests/
│   ├── conftest.py
│   ├── unit/
│   └── integration/
├── pipelines/                   # Prefect flows
└── scripts/
```

### Deps en `pyproject.toml`
```toml
[project]
name = "projection-engine"
version = "0.1.0"
requires-python = ">=3.12"
dependencies = [
    "fastapi>=0.110",
    "uvicorn[standard]>=0.27",
    "pydantic>=2.6",
    "pydantic-settings>=2.2",
    "sqlalchemy>=2.0",
    "psycopg[binary]>=3.1",
    "alembic>=1.13",
    "confluent-kafka>=2.3",
    "fastavro>=1.9",
    "pandas>=2.2",
    "numpy>=1.26",
    "scikit-learn>=1.4",
    "prophet>=1.1.5",
    "statsmodels>=0.14",
    "mlflow>=2.11",
    "prefect>=2.16",
    "structlog>=24.1",
    "opentelemetry-api",
    "opentelemetry-sdk",
    "opentelemetry-instrumentation-fastapi",
    "opentelemetry-instrumentation-sqlalchemy",
    "prometheus-client",
    "httpx",
]

[project.optional-dependencies]
dev = [
    "pytest>=8.0",
    "pytest-asyncio",
    "pytest-cov",
    "pytest-httpx",
    "testcontainers[postgres,kafka,redis]>=4.0",
    "ruff>=0.3",
    "mypy>=1.9",
    "hypothesis>=6.99",
]
```

### Criterios de aceptación
- [ ] `uv sync` instala deps
- [ ] `uv run uvicorn projection_engine.main:app --reload` arranca
- [ ] `GET /health` retorna 200
- [ ] `GET /docs` muestra OpenAPI
- [ ] `ruff check && mypy src/` pasa

### Dependencias
EPIC01: 001

---

## TASK-402 · Config, logging, OTel instrumentation · [P0] [OBS]

### Contexto
Observabilidad desde día 1. Structured logs + traces + metrics.

### Objetivo
Setup de `structlog` + OTel + Prometheus metrics.

### Archivos a crear
- `src/projection_engine/config.py` — Pydantic Settings
- `src/projection_engine/observability/logging.py`
- `src/projection_engine/observability/tracing.py`
- `src/projection_engine/observability/metrics.py`

### Config ejemplo
```python
from pydantic_settings import BaseSettings, SettingsConfigDict

class Settings(BaseSettings):
    model_config = SettingsConfigDict(env_file=".env", env_prefix="PE_")

    db_url: str
    kafka_brokers: str
    schema_registry_url: str
    kafka_consumer_group: str = "projection-engine"
    mlflow_tracking_uri: str = "http://mlflow:5000"
    otel_exporter_otlp_endpoint: str = "http://otel-collector:4317"
    environment: str = "dev"
```

### Métricas custom
- `projections_computed_total{country, crop, model_version}`
- `projection_computation_seconds` (histogram)
- `model_inference_latency_seconds`
- `feature_freshness_seconds` (gauge por feature type)

### Criterios de aceptación
- [ ] Logs JSON visibles en stdout con trace_id correlated
- [ ] `/metrics` expone métricas Prometheus
- [ ] Traces visibles en Tempo desde endpoint FastAPI de prueba

### Dependencias
TASK-401, EPIC01: 010

---

## TASK-403 · Database connection + migrations (Alembic) · [P0] [DB]

### Contexto
Aunque las tablas principales (projections, feature_store) viven en la misma DB que el monolith Java (compartida), el motor Python tiene sus propias migraciones para schemas bajo su ownership.

### Objetivo
Alembic configurado + primera migración con tablas `projection_features`, `projection_models`.

### Archivos a crear
- `alembic.ini`
- `migrations/env.py`
- `migrations/versions/001_feature_store.py`
- `src/projection_engine/infrastructure/db.py`

### Schema decisión
- Tabla `projections` (writable por Python) — **Python es el OWNER**. Java solo lee.
- Tabla `projection_features` — ownership Python.
- Tabla `projection_models` — ownership Python.

### Migración inicial (crea solo lo que es de Python)
```sql
CREATE TABLE projection_features (
    id BIGSERIAL PRIMARY KEY,
    feature_type VARCHAR(40) NOT NULL,    -- 'planting_volume_90d', 'precip_acc_30d', etc.
    country_code CHAR(2) NOT NULL,
    corridor_id VARCHAR(64),
    h3_cell CHAR(15),
    crop_code VARCHAR(32),
    as_of DATE NOT NULL,
    value DOUBLE PRECISION NOT NULL,
    computed_at TIMESTAMPTZ NOT NULL DEFAULT now(),
    UNIQUE (feature_type, country_code, corridor_id, crop_code, as_of, h3_cell)
);

CREATE INDEX idx_features_lookup ON projection_features
    (feature_type, country_code, crop_code, as_of DESC);

CREATE TABLE projection_models (
    id VARCHAR(80) PRIMARY KEY,            -- 'prophet-tomato-gt-v3.2'
    crop_code VARCHAR(32) NOT NULL,
    country_code CHAR(2) NOT NULL,
    corridor_id VARCHAR(64),
    algo VARCHAR(40) NOT NULL,
    version VARCHAR(20) NOT NULL,
    trained_at TIMESTAMPTZ NOT NULL,
    training_data_from DATE NOT NULL,
    training_data_to DATE NOT NULL,
    metrics JSONB NOT NULL,                -- MAPE, RMSE, etc.
    mlflow_run_id VARCHAR(64),
    artifact_uri TEXT NOT NULL,
    active BOOLEAN NOT NULL DEFAULT false
);
```

### Criterios de aceptación
- [ ] `alembic upgrade head` funciona contra dev DB
- [ ] Conexión SQLAlchemy con async driver
- [ ] Tests usan `SharedPostgresContainer` con schema limpio por test

### Dependencias
TASK-401, EPIC02: 205 (compartimos DB)

---

## TASK-404 · Kafka consumer: PlantingReportSubmitted · [P1] [EVENT]

### Contexto
Primer consumer. Materializa reportes en feature store.

### Objetivo
Consumer Avro idempotente que actualiza `projection_features` agregadas.

### Archivos a crear
- `src/projection_engine/infrastructure/kafka/consumer_base.py`
- `src/projection_engine/infrastructure/kafka/serde.py`
- `src/projection_engine/application/handlers/on_planting_report.py`

### Comportamiento
- Consume topic `agromis.reports.planting.v1`
- Deserializa Avro con Schema Registry
- Upserta features: `planting_volume_by_corridor_crop_window`, `n_reports_by_h3_cell`
- Idempotencia por `event_id`

### Criterios de aceptación
- [ ] Consumer corre en background task
- [ ] Tests con `testcontainers-kafka` publican evento → verifican actualización DB
- [ ] DLQ para eventos malformados
- [ ] Consumer lag expuesto como métrica

### Dependencias
TASK-403, EPIC02: 204

---

## TASK-405 · Kafka consumers: Price + Weather + Demand · [P1] [EVENT]

### Contexto
Consumers adicionales que alimentan features.

### Objetivo
Consumers para `agromis.prices.observed.v1`, `agromis.weather.observed.v1`, `agromis.buyers.demand.v1`.

### Archivos a crear
- `src/projection_engine/application/handlers/on_price_observed.py`
- `src/projection_engine/application/handlers/on_weather_observed.py`
- `src/projection_engine/application/handlers/on_demand_declared.py`

### Features derivadas
- `price_rolling_30d_median`
- `precip_accumulated_30d`, `precip_accumulated_90d`
- `temp_avg_growing_season`
- `declared_demand_volume_next_quarter`

### Criterios de aceptación
- [ ] Cada consumer procesa > 1000 msg/s en benchmark
- [ ] Features actualizadas en < 30s tras evento

### Dependencias
TASK-404

---

## TASK-406 · Feature engineering library · [P1] [DOMAIN]

### Contexto
Funciones puras reutilizables para construir features.

### Objetivo
Módulo `projection_engine.domain.features` con funciones testeadas.

### Archivos a crear
- `src/projection_engine/domain/features/planting.py`
- `src/projection_engine/domain/features/weather.py`
- `src/projection_engine/domain/features/prices.py`
- `src/projection_engine/domain/features/calendar.py`
- `src/projection_engine/domain/features/pipeline.py`

### Ejemplo función
```python
import pandas as pd
from datetime import date, timedelta

def build_features(
    corridor: str,
    crop: str,
    as_of: date,
    horizon_days: int,
    session,
) -> pd.DataFrame:
    """Construye dataframe de features para el par (corridor, crop) en fecha `as_of`."""
    reports = fetch_planting_features(session, corridor, crop, as_of)
    weather = fetch_weather_features(session, corridor, as_of, horizon_days)
    prices = fetch_price_features(session, crop, as_of, years_back=5)
    calendar = add_calendar_features(as_of, country=corridor.split("-")[0], crop=crop)

    return (
        pd.concat([reports, weather, prices, calendar], axis=1)
        .pipe(add_seasonality_features, crop=crop)
        .pipe(add_lag_features, lags=[7, 14, 30, 60])
        .pipe(fillna_with_priors, crop=crop, corridor=corridor)
    )
```

### Criterios de aceptación
- [ ] Hypothesis tests para propiedades de features (no-NaN en happy path, bounded)
- [ ] Coverage 90%
- [ ] Benchmark: 1 corridor × crop features < 100ms

### Dependencias
TASK-403

---

## TASK-407 · Baseline model (Prophet per corridor × crop) · [P1] [DOMAIN]

### Contexto
Primera versión del modelo: Prophet por combinación `(country, corridor, crop)`.

### Objetivo
Pipeline de entrenamiento Prophet + predicción.

### Archivos a crear
- `src/projection_engine/domain/models/prophet_baseline.py`
- `src/projection_engine/domain/models/priors.py`       # fallback bayesiano para cold-start
- `src/projection_engine/domain/models/protocol.py`      # ModelProtocol interface

### Protocol
```python
from typing import Protocol
import pandas as pd

class ForecastModel(Protocol):
    model_id: str
    def fit(self, features: pd.DataFrame, target: pd.Series) -> None: ...
    def predict(self, features: pd.DataFrame, horizon_days: int) -> "ProjectionResult": ...
    def explain(self, features: pd.DataFrame) -> dict: ...
```

### Cold start strategy
- Corridor con < 20 reports → fallback a priors bayesianos construidos desde:
  - Media histórica MAGA del corridor para el cultivo
  - Corredores vecinos (usando H3 k-ring)
- La response incluye `confidence_interval` muy amplio + `n_reports` bajo

### Criterios de aceptación
- [ ] Entrena sobre dataset sintético en < 10s por combinación
- [ ] Predicción returns estructura estándar `ProjectionResult` con CI
- [ ] Cold-start fallback activa cuando `n_reports < 20`

### Dependencias
TASK-406

---

## TASK-408 · Training pipeline (Prefect) · [P1] [INFRA]

### Contexto
Orquestación nocturna. Prefect flow que entrena modelos por country × corridor × crop.

### Objetivo
Flow Prefect ejecutable localmente y schedulable.

### Archivos a crear
- `pipelines/daily_training.py`
- `pipelines/tasks/extract.py`
- `pipelines/tasks/train.py`
- `pipelines/tasks/register.py`
- `pipelines/deployments/staging.yaml`

### Flow outline
```python
from prefect import flow, task

@flow(name="daily-training")
def daily_training(country: str, date: str | None = None):
    crops = get_active_crops(country)
    corridors = get_corridors(country)
    for crop in crops:
        for corridor in corridors:
            training_data = extract.submit(country, corridor, crop)
            model = train.submit(training_data)
            metrics = evaluate.submit(model, training_data)
            if metrics.mape < 0.4:
                register.submit(model, metrics)
            else:
                log_warning(f"Model {corridor}-{crop} rejected: MAPE={metrics.mape}")
```

### Criterios de aceptación
- [ ] Flow corre localmente con datos sintéticos
- [ ] MLflow logs runs con metrics
- [ ] Job schedulable (concurrency 4 por defecto)

### Dependencias
TASK-407

---

## TASK-409 · Projection computation + persistence · [P1] [DOMAIN] [DB]

### Contexto
Tras entrenar, se computan proyecciones para horizontes 30/60/90 días y se persisten en tabla `projections` (leída por Java).

### Objetivo
Task que genera projection rows + publica eventos `ProjectionUpdated.v1`.

### Archivos a crear
- `src/projection_engine/application/compute_projections.py`
- `src/projection_engine/infrastructure/kafka/producer.py`
- `src/projection_engine/infrastructure/repositories/projections.py`

### Criterios de aceptación
- [ ] Para cada (country, corridor, crop, horizon) genera fila en `projections`
- [ ] Evento Avro publicado con schema correcto
- [ ] Solo publica si `significant_change=true` (delta > 5% vs previa)
- [ ] Transacción: upsert + outbox-like (en Python usamos tx + publish after commit con idempotencia cliente)

### Dependencias
TASK-408, EPIC02: 204 (schema registry)

---

## TASK-410 · FastAPI endpoints de proyección · [P1] [API]

### Contexto
API para consulta síncrona de proyecciones. Consumida por el monolith Java (caching agresivo) y por dashboards.

### Objetivo
Endpoints REST + auth.

### Archivos a crear
- `src/projection_engine/api/v1/projections.py`
- `src/projection_engine/api/v1/schemas.py`
- `src/projection_engine/api/deps.py`            # auth dependency
- `src/projection_engine/api/v1/health.py`

### Endpoints
```http
GET /v1/projections?country=GT&crop=TOMATO&corridor=GT-CHIMALTENANGO&horizon_days=60
→ ProjectionResponse

POST /v1/projections/what-if
Body: { parcel_id, candidate_crops, planting_date }
→ lista de alternativas ranqueadas
```

### Auth
- M2M: client credentials OAuth2 (cliente `agromis-service` en Keycloak)
- No se expone directo al público

### Criterios de aceptación
- [ ] OpenAPI docs completos
- [ ] P95 < 150ms para query típica
- [ ] Auth validada con introspection Keycloak
- [ ] Tests integración con Keycloak testcontainer

### Dependencias
TASK-409

---

## TASK-411 · What-if alternative crop recommender · [P2] [DOMAIN]

### Contexto
"Si no siembro tomate, ¿qué me conviene?" Recommender basado en proyecciones + expected margin.

### Objetivo
Endpoint `POST /v1/projections/what-if`.

### Algoritmo simple para MVP
```
Para una parcela dada + fecha planificada:
  1. Obtener cultivos viables (crop_catalog filtrado por país + clima)
  2. Para cada candidato:
      - Buscar proyección actual en su corredor
      - Estimar margen esperado = (precio_proyectado × yield_típico) - (costo_producción_estimado)
      - Penalizar por surplus_probability alta
  3. Retornar top 3 ranqueados
```

### Criterios de aceptación
- [ ] Costos de producción en tabla `crop_production_costs` (seed inicial)
- [ ] Response incluye rationale por candidato (para explicabilidad)
- [ ] Tests con datos sintéticos

### Dependencias
TASK-410

---

## TASK-412 · Model monitoring + drift detection · [P2] [OBS]

### Contexto
Una vez que la ventana de cosecha cierra, comparamos proyectado vs real. Si drift > X → flag retraining.

### Objetivo
Job semanal que mide accuracy y alerta.

### Archivos a crear
- `pipelines/weekly_monitoring.py`

### Métricas
- `model_mape_by_crop_corridor` (gauge)
- `model_predictions_count_by_version`
- `model_drift_detected_total`

### Criterios de aceptación
- [ ] Si MAPE last 4 weeks > 30% → alerta Slack
- [ ] Dashboard Grafana muestra accuracy histórico

### Dependencias
TASK-409

---

## TASK-413 · MLflow registry integration · [P2] [INFRA]

### Contexto
MLflow como single source of truth de versiones de modelos.

### Objetivo
MLflow server desplegado + integración end-to-end.

### Archivos a crear
- `infra/k8s/mlflow/values.yaml`
- `src/projection_engine/infrastructure/mlflow_registry.py`

### Criterios de aceptación
- [ ] Training flow registra run + modelo como artifact
- [ ] Serving carga "latest production" desde registry
- [ ] Promoción modelo dev → staging → production vía UI

### Dependencias
TASK-408

---

## TASK-414 · Deploy Python service a K8s (Helm) · [P1] [INFRA]

### Contexto
Empaquetar servicio + helm chart similar al de Java pero adaptado.

### Objetivo
Chart `projection-engine` con values dev/staging/prod.

### Archivos a crear
- `infra/k8s/charts/projection-engine/`
  - `Chart.yaml`
  - `values.yaml`
  - `templates/deployment.yaml` — con dos containers: api + consumer
  - `templates/cronjob-training.yaml`
  - `templates/servicemonitor.yaml`

### Especificaciones
- **API deployment**: 2-6 replicas, HPA CPU 70%
- **Consumer deployment**: 1-3 replicas (Kafka consumer group scales independiente)
- **Training CronJob**: schedule 0 5 * * * (05:00 UTC)
- **Resources**: API 500m/1Gi, Consumer 250m/512Mi, Training job 2/4Gi con toleration para spot

### Criterios de aceptación
- [ ] Deploy staging funciona end-to-end
- [ ] Training CronJob corre una vez manualmente y completa
- [ ] HPA funciona bajo carga sintética

### Dependencias
TASK-410, EPIC01: 006

---

## Resumen épica 04

| # | Tarea | Prioridad | Deps |
|---|-------|-----------|------|
| 401 | Scaffold Python + FastAPI | P0 | EPIC01:001 |
| 402 | Config + OTel | P0 | 401 |
| 403 | DB + Alembic | P0 | 401, EPIC02:205 |
| 404 | Consumer planting reports | P1 | 403 |
| 405 | Consumer prices/weather/demand | P1 | 404 |
| 406 | Feature engineering lib | P1 | 403 |
| 407 | Prophet baseline | P1 | 406 |
| 408 | Prefect training flow | P1 | 407 |
| 409 | Projection computation | P1 | 408 |
| 410 | FastAPI endpoints | P1 | 409 |
| 411 | What-if recommender | P2 | 410 |
| 412 | Monitoring + drift | P2 | 409 |
| 413 | MLflow registry | P2 | 408 |
| 414 | K8s deploy | P1 | 410 |

**Dependencia clave con el monolith**: el contexto `projections` en Java (TASK-315) consume los eventos que este servicio produce (TASK-409). Deben coordinarse para que el schema Avro esté registrado antes.

# Épica 05 — Integración Externa (scrapers, clima, SMS/USSD)

> **Objetivo**: alimentar al sistema con datos externos no generados por los usuarios: precios de mercados mayoristas (MAGA, SIMPAH, CENADA), observaciones y forecasts meteorológicos (NOAA GFS, AEMET, estaciones locales), y canales de entrada/salida alternativos (SMS, USSD, IVR).
> **Duración estimada**: 3 semanas
> **Tareas**: 12

---

## Contexto general de la épica

El `integration-module` es un contexto especial: vive dentro del monolith Java pero concentra todos los **anticorruption layers** (ACL) hacia sistemas externos. Cada fuente externa tiene:

1. **Cliente** (HTTP, scraper, FTP, webhook listener)
2. **Mapper** (datos externos → eventos del dominio)
3. **Scheduler** (cron, event-driven)
4. **Resilience** (circuit breaker, retry, backfill)
5. **Publisher** (emite evento canónico al bus)

Ningún otro contexto del monolith consume datos externos directamente — todo pasa por Integration.

---

## TASK-501 · Integration module skeleton + scheduler · [P0] [INFRA]

### Contexto
Crear el contexto `integration` con infra común a todos los scrapers/connectors.

### Objetivo
Módulo Gradle `contexts/integration/` con: scheduler (Quartz-like vía `quarkus-scheduler`), circuit breaker base (SmallRye Fault Tolerance), retry policy estándar, logging estructurado.

### Archivos a crear
```
contexts/integration/
├── integration-domain/
│   └── src/main/java/gt/agromis/integration/domain/
│       ├── DataSource.java                  # enum: MAGA, SIMPAH, CENADA, NOAA_GFS, AEMET, LOCAL_STATION
│       ├── IngestResult.java                # records: success_count, failure_count, errors[]
│       └── IngestRun.java                   # aggregate para auditar cada corrida
├── integration-application/
│   └── src/main/java/gt/agromis/integration/application/
│       ├── IngestJob.java                   # abstracción base
│       ├── IngestOrchestrator.java
│       └── commands/
│           └── TriggerIngestCommand.java
├── integration-infrastructure/
│   └── src/main/java/gt/agromis/integration/infrastructure/
│       ├── schedule/JobScheduler.java
│       ├── http/ResilientHttpClient.java    # envuelve RestClient con CB + retry
│       ├── scraping/HtmlScraper.java        # JSoup helpers
│       └── scraping/SeleniumScraperPool.java # solo si es necesario — expensive
└── integration-interfaces/
    └── src/main/java/gt/agromis/integration/interfaces/
        └── IngestAdminResource.java          # admin ops: trigger manual, ver history
```

### Migración `V100__ingest_runs.sql`
```sql
CREATE TABLE ingest_runs (
    id UUID PRIMARY KEY DEFAULT uuid_generate_v7(),
    source VARCHAR(40) NOT NULL,
    country_code CHAR(2),
    started_at TIMESTAMPTZ NOT NULL,
    finished_at TIMESTAMPTZ,
    status VARCHAR(20) NOT NULL,              -- RUNNING | SUCCESS | FAILED | PARTIAL
    records_processed INT DEFAULT 0,
    records_failed INT DEFAULT 0,
    error_sample TEXT,
    metadata JSONB NOT NULL DEFAULT '{}',
    triggered_by VARCHAR(40) NOT NULL         -- 'SCHEDULED' | 'MANUAL:userId'
);

CREATE INDEX idx_ingest_runs_source_date ON ingest_runs (source, started_at DESC);
```

### Endpoint admin
```http
POST /v1/admin/integration/ingest/{source}/trigger
GET /v1/admin/integration/runs?source=MAGA&limit=20
```

### Criterios de aceptación
- [ ] Abstracción `IngestJob` con métodos `name()`, `schedule()`, `run()`, `onSuccess()`, `onFailure()`
- [ ] Cada job registra start/end en `ingest_runs`
- [ ] `ResilientHttpClient` tiene CB abierto tras 5 fallos en 60s, half-open tras 30s
- [ ] Métrica `ingest_runs_total{source,status}`
- [ ] Admin endpoint permite trigger manual con auth `SYSTEM_ADMIN`

### Dependencias
EPIC02: 205, 206, 207

---

## TASK-502 · Scraper MAGA (Guatemala) · [P1] [EVENT]

### Contexto
MAGA publica precios diarios del CENMA y terminal mayoristas. Web accesible vía PDF y web scraping. Formato: tabla HTML simple con columnas producto/medida/precio promedio.

### Objetivo
Scraper diario que obtiene precios GT mayoristas y emite eventos `PriceObserved.v1`.

### Archivos a crear
- `contexts/integration/integration-infrastructure/src/main/java/gt/agromis/integration/infrastructure/maga/`
  - `MagaScraper.java` — implementa `IngestJob`
  - `MagaPriceParser.java` — parser HTML con JSoup
  - `MagaProductDictionary.java` — mapeo nombres MAGA → crop_code canónico
  - `MagaScraperConfig.java`
- `contexts/integration/integration-infrastructure/src/test/resources/maga-sample-*.html` — fixtures

### Comportamiento
- Schedule: diariamente a las 06:00 CST (= 12:00 UTC)
- URL base: configurable (`PE_MAGA_BASE_URL`) — por defecto apuntando a publicación MAGA
- Descarga la página de "precios de productos agrícolas día anterior"
- Parsea tabla HTML
- Para cada fila: normaliza unidad (quintal → kg), mapea producto, emite evento

### Ejemplo de mapeo
```
MAGA dice          → canónico (crop_catalog GT)
"Tomate Mayor 1ª"  → TOMATO + grade=A
"Cebolla seca"     → ONION + grade=STANDARD
"Papa Grande"      → POTATO + size=LARGE
"Frijol Negro"     → BEAN_BLACK
```

### Dictionary config (YAML)
```yaml
# contexts/integration/integration-infrastructure/src/main/resources/maga-dictionary.yml
mappings:
  - pattern: "tomate.*may.*"
    crop_code: TOMATO
    grade: A
  - pattern: "cebolla\\s*seca"
    crop_code: ONION
    grade: STANDARD
  # ...
defaults:
  market_id: GT-CENMA
  currency: GTQ
  source: MAGA
```

### Evento publicado
```json
{
  "event_type": "PriceObserved.v1",
  "country_code": "GT",
  "market_id": "GT-CENMA",
  "crop_code": "TOMATO",
  "unit": "KG",
  "price_min": 5.50,
  "price_max": 7.00,
  "price_mode": 6.00,
  "currency": "GTQ",
  "observed_at": "2026-04-19",
  "source": "MAGA"
}
```

### Resiliencia
- Si HTML cambia de estructura → parser devuelve `ParseError`, se marca run `FAILED`, no se emiten eventos parciales, alerta Slack
- Si MAGA caído → retry cada 30min hasta 3 veces, luego skip día + alerta
- Backfill: endpoint admin puede dispararlo para fecha pasada

### Criterios de aceptación
- [ ] Test con HTML fixture valida parser (10+ fixtures de distintos días reales)
- [ ] Integration test con mock server devuelve evento correcto
- [ ] Si productos reconocidos < 70% esperados → warning + detalle en `ingest_runs.metadata`
- [ ] Métricas: `price_ingest_records_total{source=MAGA}`, `price_ingest_duration_seconds`
- [ ] Dictionary actualizable sin redeploy (hot reload via config)

### Dependencias
TASK-501, EPIC03: 309 (tabla price_observations)

---

## TASK-503 · Scraper SIMPAH (Honduras) + CENADA (Costa Rica) · [P2] [EVENT]

### Contexto
Preparar el sistema para multi-país desde el inicio requiere al menos un scraper adicional funcional. SIMPAH (HN) y CENADA (CR) son referencias centrales.

### Objetivo
Scrapers análogos a MAGA para HN y CR, reutilizando la misma abstracción `IngestJob`.

### Archivos a crear
- `contexts/integration/integration-infrastructure/.../simpah/` — HN
- `contexts/integration/integration-infrastructure/.../cenada/` — CR
- Diccionarios de mapeo por país
- Fixtures HTML

### Especificaciones
- Schedule: diariamente, staggered (SIMPAH 07:00, CENADA 08:00)
- Dictionaries independientes (`simpah-dictionary.yml`, `cenada-dictionary.yml`)
- Mismos eventos `PriceObserved.v1` con `country_code` y `market_id` distintos

### Criterios de aceptación
- [ ] Eventos emitidos con `country_code=HN` y `country_code=CR` respectivamente
- [ ] Si país no activo en `countries.active=false` → job skip silencioso
- [ ] Markets seedeados: `HN-SIMPAH`, `CR-CENADA`

### Dependencias
TASK-502 (reutiliza patrón)

---

## TASK-504 · Weather ingest: NOAA GFS forecast · [P1] [EVENT]

### Contexto
NOAA GFS publica forecasts globales cada 6h (runs 00/06/12/18 UTC). Formato GRIB2. Descargamos solo las tiles que cubren CA y variables relevantes.

### Objetivo
Job que descarga forecasts GFS, extrae variables para corridors, emite `WeatherObservationIngested.v1`.

### Archivos a crear
- `contexts/integration/integration-infrastructure/.../weather/`
  - `NoaaGfsClient.java` — descarga desde NOAA NOMADS/AWS Open Data
  - `Grib2Parser.java` — usa lib `cdm-modules` o `netcdf-java`
  - `WeatherExtractor.java` — para cada corridor, interpola valor
  - `NoaaGfsIngestJob.java`

### Variables extraídas
- `temp_2m` (temperatura a 2m) — promedio, min, max
- `precip_rate` (tasa de precipitación acumulada en ventana)
- `rh_2m` (humedad relativa)
- `wind_10m` (velocidad viento a 10m)

### Horizontes extraídos
- Forecast 0h, 24h, 48h, 72h, 168h (7d)

### Schedule
- Cada 6h, 90 min después del run GFS (para dar tiempo a publicación)
- Fallback: si run más reciente no disponible, usa el anterior

### Archivos temporales
- Descarga a `/tmp/gfs-{run_ts}/*.grib2`
- Limpia tras parse exitoso
- Tamaño por run: ~500 MB → descarga solo subset (bbox CA + variables)

### Criterios de aceptación
- [ ] Descarga subset GFS en < 5 min
- [ ] Emite 1 evento por corridor por horizonte (ej: 12 corridors × 5 horizontes = 60 eventos)
- [ ] Valores interpolados con bilinear (PostGIS `ST_Value` tras raster import)
- [ ] Archivos temporales limpiados incluso si falla
- [ ] Métricas de freshness: `weather_last_run_age_minutes`

### Dependencias
TASK-501, TASK-316 (corridors), EPIC03: 317

---

## TASK-505 · Weather alerts consumer · [P1] [EVENT]

### Contexto
Cuando el forecast indica riesgo (heladas, lluvias extremas, sequía prolongada) se genera alerta. Esta tarea integra con `alerts` context.

### Objetivo
Rule engine weather + publicación de `WeatherAlertIssued.v1`.

### Archivos a crear
- `contexts/integration/integration-application/.../WeatherAlertRules.java`
- `contexts/integration/integration-application/.../OnWeatherObservation.java`

### Reglas iniciales
| Condición | Severity | Audiencia |
|-----------|----------|-----------|
| Temp min forecast 48h < 4°C en altiplano | HIGH | Farmers con parcelas > 2000m |
| Precip acumulada 7d > 200mm | HIGH | Farmers con cultivos sensibles (tomate, papa) |
| Precip acumulada 30d < 10% de promedio histórico | MEDIUM | Farmers en corredor |
| Viento sostenido > 60 km/h | MEDIUM | Farmers con parcelas sin barrera rompevientos |

### Criterios de aceptación
- [ ] Evento `WeatherAlertIssued.v1` consumido por Alerts context (EPIC03: 312)
- [ ] Dedup: misma alerta no se emite 2 veces en 12h
- [ ] Rules configurables en YAML

### Dependencias
TASK-504, EPIC03: 312

---

## TASK-506 · AEMET + local weather stations · [P2] [EVENT]

### Contexto
AEMET (España) tiene datos históricos gratuitos útiles para entrenar modelos. Estaciones meteorológicas locales (INSIVUMEH en GT, COPECO en HN) son claves para ground truth.

### Objetivo
Cliente AEMET + adapter genérico "local station" configurable.

### Archivos a crear
- `contexts/integration/integration-infrastructure/.../weather/AemetClient.java`
- `contexts/integration/integration-infrastructure/.../weather/LocalStationClient.java` — genérico
- Config YAML con stations

### Criterios de aceptación
- [ ] AEMET client solo para datos históricos (no realtime en MVP)
- [ ] Al menos 1 station GT integrada (INSIVUMEH si expone API pública o mock por ahora)
- [ ] Datos de station tienen prioridad sobre GFS para corridors cercanos

### Dependencias
TASK-504

---

## TASK-507 · Backfill job: históricos de precios · [P1] [EVENT]

### Contexto
Para entrenar modelos necesitamos histórico. MAGA publica archivos anuales. Backfill manual desde admin.

### Objetivo
Endpoint admin que dispara backfill + job que consume lote.

### Archivos a crear
- `contexts/integration/integration-interfaces/.../BackfillResource.java`
- `contexts/integration/integration-application/.../PriceBackfillJob.java`
- `contexts/integration/integration-infrastructure/.../backfill/MagaHistoricalImporter.java`

### Endpoints
```http
POST /v1/admin/integration/backfill/prices
Body: {
  "source": "MAGA",
  "country_code": "GT",
  "from": "2020-01-01",
  "to": "2025-12-31",
  "dry_run": false
}
→ 202 Accepted { "run_id": "..." }

GET /v1/admin/integration/runs/{run_id}
→ progreso con percent, ETA
```

### Criterios de aceptación
- [ ] Procesa ≥ 5 años de datos en < 2h
- [ ] Idempotente: re-correr no duplica datos
- [ ] Progress reporting cada 5% completion
- [ ] Dry-run muestra conteos sin insertar

### Dependencias
TASK-502

---

## TASK-508 · SMS gateway adapter (Twilio primary + fallback) · [P1] [API] [SEC]

### Contexto
SMS es canal crítico para áreas sin datos. Doble proveedor para resiliencia.

### Objetivo
Adapter SMS con provider primario (Twilio) + fallback (SMS local via telco partner API).

### Archivos a crear
- `contexts/integration/integration-infrastructure/.../sms/`
  - `SmsProvider.java` (interface)
  - `TwilioSmsProvider.java`
  - `LocalTelcoSmsProvider.java` (stub si no hay acuerdo aún — interface lista)
  - `SmsProviderSelector.java` — failover
  - `SmsCostTracker.java`

### Criterios de aceptación
- [ ] Cost cap por tenant (daily + monthly)
- [ ] Webhook de delivery report (Twilio) actualiza status
- [ ] Métrica `sms_sent_total{provider,country,outcome}`
- [ ] Failover automático tras 3 fallos consecutivos

### Dependencias
TASK-501

---

## TASK-509 · SMS inbound handler (2-way SMS for illiterate farmers) · [P2] [API]

### Contexto
Farmer envía SMS con código corto ("TOMATE 150 LIBRAS CHIMALTENANGO") → se parsea y registra como observation/report preliminar.

### Objetivo
Webhook receptor + parser + conversión a reporte DRAFT.

### Archivos a crear
- `contexts/integration/integration-interfaces/.../SmsInboundWebhook.java`
- `contexts/integration/integration-infrastructure/.../sms/SmsReportParser.java`
- `contexts/integration/integration-infrastructure/.../sms/ShortCodeDictionary.java`

### Formato esperado
```
REP TOMATE 1.5 HA CHIMAL 10MAY
```
Parser traduce a command `SubmitPlantingReportCommand` con `source=SMS` y confirma por SMS de vuelta.

### Criterios de aceptación
- [ ] Farmer ya registrado (lookup por phone_e164 + country) requerido
- [ ] Formato inválido → respuesta SMS con instrucción breve
- [ ] Reporte se crea en status `SUBMITTED_UNVERIFIED` (requiere confirmación posterior)
- [ ] Test end-to-end con mock Twilio webhook

### Dependencias
TASK-508, EPIC03: 305

---

## TASK-510 · USSD gateway (integración básica) · [P3] [API]

### Contexto
USSD es *crítico* en zonas sin smartphone. Integración con gateway telco (ej: Africa's Talking, o partner local).

### Objetivo
Endpoint USSD que sirve menú básico: ver precio último del cultivo, registrar siembra simple.

### Archivos a crear
- `contexts/integration/integration-interfaces/.../UssdResource.java`
- `contexts/integration/integration-infrastructure/.../ussd/UssdSessionStore.java` — Redis
- `contexts/integration/integration-infrastructure/.../ussd/UssdMenuBuilder.java`

### Menú ejemplo
```
Main (*123#)
  1. Ver precios
     → pedir cultivo → mostrar último precio mercado ref
  2. Registrar siembra
     → pedir cultivo, área, fecha
  3. Mis reportes
  4. Alertas
```

### Criterios de aceptación
- [ ] Sesiones stateful (TTL 5min) en Redis
- [ ] Menús disponibles en ES e idiomas locales (kʼicheʼ)
- [ ] Stub end-to-end con provider simulado

### Dependencias
TASK-508

---

## TASK-511 · Integration observability dashboard · [P2] [OBS]

### Contexto
Dashboard Grafana específico para integraciones: estado de cada fuente, lag, errores recientes.

### Objetivo
Dashboard + alerts.

### Archivos a crear
- `infra/grafana/dashboards/integrations-health.json`
- `infra/prometheus/rules/integration-alerts.yaml`

### Alertas
- `MagaSourceDown`: sin ingest exitoso en 48h
- `NoaaForecastStale`: último run > 18h
- `SmsProviderDegraded`: tasa éxito < 90% en 1h
- `HighSmsCostBurn`: costo diario > 1.5x promedio semanal

### Criterios de aceptación
- [ ] Dashboard muestra status por fuente con semáforo
- [ ] Alertas enrutadas a Slack según severity

### Dependencias
TASK-501, EPIC01: 010

---

## TASK-512 · Runbooks de integración · [P2] [DOC]

### Contexto
Cuando MAGA cambia formato, cuando NOAA está caído, cuando SMS falla — los responders necesitan pasos claros.

### Objetivo
Runbooks por cada integración.

### Archivos a crear
- `docs/runbooks/integration-maga-down.md`
- `docs/runbooks/integration-maga-parser-broken.md`
- `docs/runbooks/integration-noaa-stale.md`
- `docs/runbooks/integration-sms-failover.md`
- `docs/runbooks/integration-backfill.md`

### Formato runbook
```markdown
# Runbook: MAGA scraper down

## Symptoms
- Alert `MagaSourceDown` fired
- Dashboard shows red for MAGA source

## Diagnosis
1. Check `ingest_runs` last rows for MAGA:
   `SELECT * FROM ingest_runs WHERE source='MAGA' ORDER BY started_at DESC LIMIT 5;`
2. Verify MAGA site manually: curl <URL>
3. Check logs: `kubectl logs -l app=backend -c integration | grep MagaScraper`

## Resolution
- If site changed structure: open P1, assign parser fix
- If temporary outage: no action, will retry automatically
- If parser broken: use backfill endpoint with dates range after fix

## Escalation
- On-call: @sre-agromis
- Owner: @data-team
```

### Criterios de aceptación
- [ ] 5 runbooks completos con symptoms/diagnosis/resolution
- [ ] Linked desde alerts

### Dependencias
TASK-511

---

## Resumen épica 05

| # | Tarea | Prioridad | Deps clave |
|---|-------|-----------|------------|
| 501 | Integration skeleton | P0 | EPIC02:205, 206 |
| 502 | MAGA scraper | P1 | 501, EPIC03:309 |
| 503 | SIMPAH + CENADA | P2 | 502 |
| 504 | NOAA GFS weather | P1 | 501, EPIC03:316 |
| 505 | Weather alerts | P1 | 504, EPIC03:312 |
| 506 | AEMET + stations | P2 | 504 |
| 507 | Price backfill | P1 | 502 |
| 508 | SMS gateway | P1 | 501 |
| 509 | SMS inbound parser | P2 | 508, EPIC03:305 |
| 510 | USSD gateway | P3 | 508 |
| 511 | Integration dashboards | P2 | 501, EPIC01:010 |
| 512 | Runbooks | P2 | 511 |

**Nota de seguridad**: todos los endpoints inbound (webhooks SMS/USSD) deben validar firma HMAC del provider antes de aceptar.

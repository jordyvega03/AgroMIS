# Épica 03 — Backend: Módulos CORE (Bounded Contexts)

> **Objetivo**: implementar los 8 bounded contexts definidos en la arquitectura. Cada contexto sigue la estructura DDD-layered ya establecida en EPIC 02. Esta épica se paraleliza entre 2+ devs por contexto.
> **Duración estimada**: 8 semanas
> **Tareas**: 35

---

## Orden recomendado

**Fase A (semanas 5-7)** — Base de negocio:
1. Farmer Portal (301-303)
2. Planting Reports (304-308)

**Fase B (semanas 8-10)** — Cadena de valor:
3. Prices (309-311)
4. Alerts (312-314)
5. Projections adapter (315-316) — consumidor de eventos del motor Python

**Fase C (semanas 11-13)** — Complementarios:
6. GIS Read Model (317-321)
7. Buyers (322-324)
8. Incentives / Reputation (325-329)

**Fase D (semanas 14-16)** — Integración y endurecimiento:
9. Integration Module bridges (330-331)
10. Cross-context tests (332-335)

---

## 3.1 Farmer Portal

---

## TASK-301 · Farmer aggregate + repositorio · [P1] [DOMAIN] [DB]

### Contexto
Primer aggregate real del dominio. Farmer es la entidad central: pequeño agricultor con teléfono verificado, parcelas (dominio aparte), preferencias de canal, reputación (agregada después).

### Objetivo
Agregado `Farmer` + repositorio JPA + migración de tablas + tests de invariantes.

### Archivos a crear
```
contexts/farmers/farmers-domain/src/main/java/gt/agromis/farmers/domain/
├── Farmer.java                   # aggregate root
├── FarmerId.java                 # identity
├── PhoneE164.java                # VO con validación
├── FarmerStatus.java             # enum: ACTIVE, SUSPENDED, DELETED
├── PreferredChannel.java         # enum: PUSH, SMS, WHATSAPP, VOICE
├── LanguageCode.java             # VO ISO 639-3
├── Farmers.java                  # repository interface
└── events/
    ├── FarmerRegisteredV1.java
    ├── FarmerPhoneVerifiedV1.java
    └── FarmerPreferencesChangedV1.java

contexts/farmers/farmers-infrastructure/src/main/java/gt/agromis/farmers/infrastructure/
├── JpaFarmerEntity.java
├── JpaFarmers.java
└── FarmerAvroMapper.java         # dominio → Avro event
```

### Migración `V020__farmers.sql`
```sql
CREATE TABLE farmers (
    id UUID PRIMARY KEY DEFAULT uuid_generate_v7(),
    country_code CHAR(2) NOT NULL REFERENCES countries(code),
    phone_e164 VARCHAR(20) NOT NULL,
    phone_verified_at TIMESTAMPTZ,
    full_name VARCHAR(200),
    national_id_encrypted BYTEA,
    preferred_language CHAR(3) NOT NULL DEFAULT 'spa',
    preferred_channel VARCHAR(16) NOT NULL DEFAULT 'PUSH',
    cooperative_id UUID,
    registered_at TIMESTAMPTZ NOT NULL DEFAULT now(),
    registered_by_extensionist_id UUID,
    last_active_at TIMESTAMPTZ,
    status VARCHAR(16) NOT NULL DEFAULT 'ACTIVE',
    metadata JSONB NOT NULL DEFAULT '{}',
    version BIGINT NOT NULL DEFAULT 0,    -- optimistic locking
    UNIQUE (country_code, phone_e164)
) PARTITION BY LIST (country_code);

-- Partitions para países MVP
CREATE TABLE farmers_gt PARTITION OF farmers FOR VALUES IN ('GT');
CREATE TABLE farmers_sv PARTITION OF farmers FOR VALUES IN ('SV');
CREATE TABLE farmers_hn PARTITION OF farmers FOR VALUES IN ('HN');
CREATE TABLE farmers_ni PARTITION OF farmers FOR VALUES IN ('NI');
CREATE TABLE farmers_cr PARTITION OF farmers FOR VALUES IN ('CR');
CREATE TABLE farmers_pa PARTITION OF farmers FOR VALUES IN ('PA');
CREATE TABLE farmers_bz PARTITION OF farmers FOR VALUES IN ('BZ');

CREATE INDEX idx_farmers_phone ON farmers (country_code, phone_e164);
CREATE INDEX idx_farmers_coop ON farmers (cooperative_id) WHERE cooperative_id IS NOT NULL;

-- RLS
ALTER TABLE farmers ENABLE ROW LEVEL SECURITY;
CREATE POLICY farmer_country_isolation ON farmers
    USING (country_code = current_country_code());
```

### Invariantes del agregado
- Un farmer no puede estar `ACTIVE` sin phone verificado
- `phone_e164` debe pasar regex E.164
- `preferred_language` ∈ catálogo soportado del country
- No se puede cambiar `country_code` después de creación (inmutable)

### Factory methods
```java
public static Farmer register(PhoneE164 phone, CountryCode country, String fullName) { ... }
public void verifyPhone(Instant verifiedAt) { ... }
public void changePreferences(PreferredChannel channel, LanguageCode language) { ... }
public void suspend(String reason) { ... }
```

### Criterios de aceptación
- [ ] Tests unitarios de todas las invariantes (happy + error paths)
- [ ] `FarmerRegisteredV1` publicado vía outbox al crear
- [ ] RLS funciona: contexto GT no ve farmers SV
- [ ] `national_id_encrypted` cifrado con pgcrypto (función helper reutilizable)
- [ ] Optimistic locking funciona (test: 2 updates concurrentes → uno falla)

### Dependencias
EPIC02: 202, 205, 209

---

## TASK-302 · Registro de farmer vía REST · [P1] [API]

### Contexto
Endpoint público (sin auth de farmer, pero sí client credential) para que la app móvil registre nuevos agricultores. Verificación por SMS OTP.

### Objetivo
Endpoints `POST /v1/farmers`, `POST /v1/farmers/{id}/verify-phone`, `POST /v1/farmers/request-otp`.

### Archivos a crear
```
contexts/farmers/farmers-application/src/main/java/gt/agromis/farmers/application/
├── commands/
│   ├── RegisterFarmerCommand.java
│   ├── RegisterFarmerHandler.java
│   ├── RequestPhoneOtpCommand.java
│   ├── RequestPhoneOtpHandler.java
│   ├── VerifyPhoneCommand.java
│   └── VerifyPhoneHandler.java
└── ports/OtpSender.java             # port; adapter en infra

contexts/farmers/farmers-interfaces/src/main/java/gt/agromis/farmers/interfaces/
├── FarmersResource.java
└── dto/
    ├── RegisterFarmerRequest.java
    ├── VerifyPhoneRequest.java
    └── FarmerResponse.java

contexts/farmers/farmers-infrastructure/src/main/java/gt/agromis/farmers/infrastructure/
├── TwilioOtpSender.java             # adapter
└── OtpStore.java                    # Redis
```

### Contrato API

```http
POST /v1/farmers
Content-Type: application/json
Idempotency-Key: <uuid>
X-Country-Code: GT

{
  "phone_e164": "+50212345678",
  "country_code": "GT",
  "full_name": "Juan Pérez",
  "preferred_language": "spa",
  "preferred_channel": "SMS"
}

→ 201 Created
Location: /v1/farmers/0197-....
{
  "id": "0197-...",
  "status": "PENDING_VERIFICATION",
  "phone_e164": "+502...8",   # mascarado parcial
  "otp_sent": true
}
```

```http
POST /v1/farmers/{id}/verify-phone
{ "otp_code": "482931" }

→ 200 OK  (phone verified, status → ACTIVE)
```

### Criterios de aceptación
- [ ] Registro con teléfono inválido → 400 problem+json
- [ ] Registro duplicado (mismo phone+country) → 409 Conflict con el id existente
- [ ] OTP mock en tests via `@Alternative`
- [ ] OTP Redis TTL 5 min; re-request antes de TTL → 429
- [ ] Métricas: `farmers_registered_total{country}`, `farmers_otp_verified_total`

### Dependencias
TASK-301, EPIC02: 206, 208

---

## TASK-303 · Parcels + geo · [P1] [DOMAIN] [DB] [API]

### Contexto
Un farmer tiene ≥ 1 parcela. Parcela = polígono geo + área + tenencia + cultivos históricos.

### Objetivo
Agregado Parcel + endpoints + H3 indexing automático.

### Archivos a crear
- Migración `V021__parcels.sql`
- `contexts/farmers/farmers-domain/.../Parcel.java`, `ParcelId.java`, `Tenure.java`
- `contexts/farmers/farmers-infrastructure/.../JpaParcelEntity.java` con `@JdbcTypeCode(SqlTypes.GEOMETRY)`
- `contexts/farmers/farmers-interfaces/.../ParcelsResource.java`

### Migración V021
```sql
CREATE TABLE parcels (
    id UUID PRIMARY KEY DEFAULT uuid_generate_v7(),
    farmer_id UUID NOT NULL,
    country_code CHAR(2) NOT NULL,
    name VARCHAR(100),
    geometry GEOMETRY(Polygon, 4326) NOT NULL,
    centroid GEOGRAPHY(Point, 4326) GENERATED ALWAYS AS (ST_Centroid(geometry)::geography) STORED,
    h3_res5 CHAR(15),
    h3_res7 CHAR(15),
    h3_res9 CHAR(15),
    area_ha NUMERIC(10,4) NOT NULL CHECK (area_ha > 0),
    altitude_m INTEGER,
    tenure VARCHAR(16) NOT NULL,
    soil_type VARCHAR(40),
    irrigation_type VARCHAR(40),
    created_at TIMESTAMPTZ NOT NULL DEFAULT now(),
    updated_at TIMESTAMPTZ NOT NULL DEFAULT now(),
    version BIGINT NOT NULL DEFAULT 0,
    FOREIGN KEY (farmer_id, country_code) REFERENCES farmers(id, country_code)
);

CREATE INDEX idx_parcels_geom ON parcels USING GIST (geometry);
CREATE INDEX idx_parcels_h3_7 ON parcels (h3_res7);
CREATE INDEX idx_parcels_farmer ON parcels (farmer_id);
ALTER TABLE parcels ENABLE ROW LEVEL SECURITY;
CREATE POLICY parcel_country_iso ON parcels USING (country_code = current_country_code());
```

### Endpoints
```http
POST /v1/farmers/{farmerId}/parcels
Body: {
  "name": "La Parcela Norte",
  "geometry": { "type": "Polygon", "coordinates": [[...]] },
  "area_ha": 0.75,
  "tenure": "OWNED",
  "altitude_m": 1890
}

GET /v1/farmers/{farmerId}/parcels
GET /v1/parcels/{id}
PATCH /v1/parcels/{id}
DELETE /v1/parcels/{id}       # soft delete
```

### H3 indexing
- Al insertar/actualizar → trigger DB o lógica app calcula `h3_res5/7/9` desde centroid
- Usar `h3-java` lib del shared kernel

### Validaciones
- Polígono válido (ST_IsValid)
- Área calculada (ST_Area proyectada) debe diferir de `area_ha` declarada en ≤ 20%; si más → warning (no bloquea)
- Máximo 50 parcelas por farmer

### Criterios de aceptación
- [ ] Crear parcela con polígono GeoJSON funciona
- [ ] H3 indexes calculados automáticamente
- [ ] Tests con parcelas inválidas (self-intersecting, demasiado grande > 500ha)
- [ ] Evento `FarmerParcelAddedV1` publicado

### Dependencias
TASK-301

---

## 3.2 Planting Reports (CORE Domain)

---

## TASK-304 · PlantingReport aggregate · [P1] [DOMAIN] [DB]

### Contexto
El aggregate más importante del sistema. Declaración de intención de sembrar cultivo X en parcela Y con área Z en fecha F. Es la fuente primaria de la señal que alimenta al motor de proyección.

### Objetivo
Agregado con todo su ciclo de vida: DRAFT → SUBMITTED → VALIDATED → HARVESTED / REJECTED / ABANDONED.

### Archivos a crear
- Migración `V030__planting_reports.sql` (ya diseñada en doc arquitectura §4.4.3)
- `contexts/reports/reports-domain/src/main/java/gt/agromis/reports/domain/`
  - `PlantingReport.java` (aggregate root)
  - `PlantingReportId.java`
  - `PlantingReportStatus.java`
  - `PlantedArea.java` (VO)
  - `PlantingDates.java` (VO encapsula expected/actual planting/harvest dates con invariantes)
  - `ExpectedYield.java`
  - `ValidationScore.java`
  - `IdempotencyKey.java` (VO)
  - `PlantingReports.java` (repo)
  - `events/PlantingReportSubmittedV1.java`
  - `events/PlantingReportValidatedV1.java`
  - `events/PlantingReportRejectedV1.java`
  - `events/HarvestReportedV1.java`

### Invariantes del agregado
- `planted_area_ha > 0`
- `expected_planting_date <= expected_harvest_date`
- `expected_harvest_date - expected_planting_date` está dentro del rango típico del cultivo (con tolerancia)
- El `crop_code` debe existir en `crop_catalog` del mismo `country_code`
- `idempotency_key` único por farmer_id
- Un reporte no puede transicionar de `HARVESTED` a nada más
- `validation_score` solo se puede asignar si status ∈ {SUBMITTED, VALIDATED}

### Factory + command methods
```java
public static PlantingReport submit(
    FarmerId farmer, ParcelId parcel, CountryCode country,
    CropCode crop, PlantedArea area, PlantingDates dates,
    ExpectedYield yield, IdempotencyKey idempotencyKey,
    Instant submittedOfflineAt, Instant submittedAt
) { ... }

public void validate(ExtensionistId by, ValidationScore score);
public void reject(String reason, ExtensionistId by);
public void markHarvested(ActualYield yield, Instant harvestedAt);
public void abandon(String reason);
```

### Criterios de aceptación
- [ ] 100% coverage en invariantes
- [ ] Tests de state machine (transiciones válidas e inválidas)
- [ ] Evento publicado en cada transición relevante
- [ ] Validación cross-context: crop_code existe (inyectar `CropsLookup` port)

### Dependencias
TASK-301, TASK-303, TASK-214 (crop catalog)

---

## TASK-305 · Submit planting report endpoint (single) · [P1] [API]

### Contexto
Endpoint simple para submit de un reporte. Usado por la UI web del extensionista (no por app móvil; ver TASK-306 para batch).

### Objetivo
`POST /v1/reports/planting` funcional.

### Archivos a crear
- `contexts/reports/reports-application/.../commands/SubmitPlantingReportCommand.java` + Handler
- `contexts/reports/reports-interfaces/.../ReportsResource.java`
- `contexts/reports/reports-interfaces/dto/SubmitPlantingReportRequest.java`

### Contrato
```http
POST /v1/reports/planting
Idempotency-Key: <client-generated-uuid>
Body: {
  "farmer_id": "...",
  "parcel_id": "...",
  "crop_code": "TOMATO",
  "crop_variety": "Tropic",
  "planted_area_ha": 0.35,
  "expected_planting_date": "2026-05-10",
  "expected_harvest_date": "2026-08-15",
  "expected_yield_kg_ha": 28000,
  "submitted_offline_at": "2026-04-19T13:42:11-06:00",
  "planting_method": "DIRECT_SEED",
  "irrigation": "DRIP"
}

→ 201 Created
{ "id": "...", "status": "SUBMITTED", "server_received_at": "..." }
```

### Criterios de aceptación
- [ ] Idempotency-Key repetido → mismo resultado (no duplica)
- [ ] Validación cross-aggregate: parcel pertenece a farmer, farmer pertenece a country del token
- [ ] Evento `PlantingReportSubmittedV1` publicado vía outbox
- [ ] Test: 100 submits paralelos con mismo Idempotency-Key → 1 solo reporte creado

### Dependencias
TASK-304

---

## TASK-306 · Sync batch endpoint (mobile offline) · [P1] [API] [EVENT]

### Contexto
El endpoint clave de la app móvil offline-first. Recibe batch de mutaciones pendientes del cliente y devuelve resultados + pull de cambios del server.

### Objetivo
`POST /v1/sync/batch` con Protocol Buffers + JSON fallback.

### Archivos a crear
- `contexts/reports/reports-interfaces/.../SyncResource.java`
- `contexts/reports/reports-interfaces/dto/SyncBatchRequest.java`
- `contexts/reports/reports-interfaces/dto/SyncBatchResponse.java`
- `contexts/reports/reports-application/.../commands/SyncBatchCommand.java`
- Schema proto: `contracts/proto/sync.proto`

### Contrato (JSON view)
```json
// Request
{
  "client_id": "device-uuid",
  "app_version": "1.4.2",
  "last_sync_cursor": "2026-04-18T10:00:00Z#342",
  "mutations": [
    {
      "local_id": "01963c14-...",
      "type": "SUBMIT_PLANTING_REPORT",
      "idempotency_key": "...",
      "payload": { /* same as TASK-305 body */ }
    },
    { "local_id": "...", "type": "UPDATE_PARCEL", ... }
  ]
}

// Response
{
  "results": [
    { "local_id": "01963c14-...", "status": "ACCEPTED", "server_id": "..." },
    { "local_id": "...", "status": "DUPLICATE", "server_id": "..." },
    { "local_id": "...", "status": "REJECTED", "error": { "code": "...", "message": "..." } }
  ],
  "pulled_changes": {
    "projections": [ {...}, {...} ],
    "alerts": [ {...} ],
    "new_cursor": "2026-04-19T14:32:11Z#567"
  }
}
```

### Comportamiento
- Procesar mutaciones en orden recibido
- Cada mutación dentro de su propia `@Transactional`
- Si una falla, las demás continúan (no rollback del batch)
- `pulled_changes` incluye:
  - Proyecciones nuevas relevantes para las parcelas del farmer (since cursor)
  - Alertas nuevas en su inbox
  - Cambios en su reputación
- Cursor = timestamp + sequence

### Criterios de aceptación
- [ ] Batch de 50 mutations procesa en < 2s p95
- [ ] Protobuf endpoint `Accept: application/x-protobuf` reduce payload > 40%
- [ ] Mutations con timestamp offline > 30 días → warning pero se aceptan
- [ ] Tests de concurrencia: mismo device sync 2 veces simultáneas → idempotente
- [ ] Métrica `sync_batch_mutations_total{status}`, `sync_batch_duration_seconds`

### Dependencias
TASK-305, TASK-303 (parcels updates también vía sync)

---

## TASK-307 · Validación de reportes por extensionista · [P2] [API] [EVENT]

### Contexto
Extensionistas (usuarios con rol `FARMER_EXTENSIONIST`) validan reportes en campo. La validación incrementa el `validation_score` y dispara aumento de reputación del farmer.

### Objetivo
`POST /v1/reports/planting/{id}/validate` funcional + consumer que procesa eventos.

### Archivos a crear
- `contexts/reports/reports-application/.../commands/ValidateReportCommand.java` + Handler
- `contexts/reports/reports-interfaces/.../ValidationsResource.java`

### Contrato
```http
POST /v1/reports/planting/{id}/validate
Authorization: Bearer <extensionist token>
Body: {
  "verdict": "CONFIRMED" | "REJECTED" | "PARTIAL",
  "field_visit_id": "uuid",
  "observed_area_ha": 0.33,         # puede diferir del declarado
  "observed_crop": "TOMATO",
  "notes": "Planta en etapa..."
}
→ 200 { "validation_score": 0.85, "farmer_reputation_delta": 50 }
```

### Reglas
- Un extensionista solo puede validar reportes de su jurisdicción (municipios asignados)
- Un reporte puede tener múltiples validaciones (consenso)
- El `validation_score` final = función(n_validaciones, verdicts, reputación de los extensionistas)

### Criterios de aceptación
- [ ] Validación cross-jurisdicción → 403
- [ ] Evento `PlantingReportValidatedV1` publicado
- [ ] Integration test con múltiples validaciones converge a score estable

### Dependencias
TASK-305

---

## TASK-308 · Harvest reports · [P2] [DOMAIN] [API]

### Contexto
Cuando el ciclo completa, el farmer reporta cosecha real. Este dato cierra el loop: permite evaluar accuracy del modelo y ajustar reputación.

### Objetivo
`POST /v1/reports/harvest` + state transition del planting report asociado.

### Archivos a crear
- Migración `V031__harvest_reports.sql`
- `contexts/reports/.../HarvestReport.java` (entidad, no aggregate — es satélite del PlantingReport)
- `contexts/reports/.../commands/ReportHarvestCommand.java`

### Criterios de aceptación
- [ ] Solo se puede reportar harvest si planting está `VALIDATED` o `SUBMITTED` y fecha actual ≥ expected_harvest_date - 14 días
- [ ] Evento `HarvestReportedV1` publicado
- [ ] Planting report transiciona a `HARVESTED`

### Dependencias
TASK-305

---

## 3.3 Price Intelligence

---

## TASK-309 · PriceObservation + TimescaleDB ingest · [P1] [DOMAIN] [DB]

### Contexto
Las series de precios viven en TimescaleDB. El contexto `prices` es casi CRUD pero con optimizaciones agregadas.

### Objetivo
Entidad `PriceObservation` + ingest por source + continuous aggregates.

### Archivos a crear
- Migración `V040__price_observations.sql` (ya en doc arquitectura §4.4.4)
- `contexts/prices/prices-domain/src/main/java/gt/agromis/prices/domain/`
  - `PriceObservation.java` (entity, no aggregate)
  - `Market.java`, `MarketId.java`
  - `Currency.java`, `Unit.java`, `Grade.java`
  - `PricePoint.java` (VO: min/max/mode/currency/unit)
  - `Source.java` (MAGA, SIMPAH, MANUAL, ...)
  - `Prices.java` (repo)
- `contexts/prices/prices-infrastructure/.../TimescalePrices.java`
- `contexts/prices/prices-application/.../commands/IngestPriceObservationCommand.java`

### Criterios de aceptación
- [ ] Hypertable creada con chunks mensuales
- [ ] Continuous aggregate `price_daily_median` materializa
- [ ] Bulk insert de 10K observaciones < 3s
- [ ] Outlier detection: observation con `price_mode` > 3σ del rolling median 30d → flag `anomaly_flag=true` pero se inserta

### Dependencias
EPIC02: 202, 205

---

## TASK-310 · Markets catalog + manual entry endpoint · [P1] [API] [DB]

### Contexto
Catálogo de mercados de referencia por país. MVP GT: CENMA, La Terminal, Mercado San Martín. Operadores con reputación alta pueden ingresar precios manualmente.

### Objetivo
Endpoints admin para markets + endpoint operador para submit manual.

### Archivos a crear
- Migración `V041__markets.sql`
- Seed de markets GT
- `POST /v1/prices/manual` (con rate limit específico)

### Criterios de aceptación
- [ ] Seed inserta 5+ mercados GT clasificados por nivel (mayorista/minorista)
- [ ] Submit manual requiere reputación ≥ SILVER
- [ ] Validación: price_mode ∈ [price_min, price_max] si todos provided

### Dependencias
TASK-309

---

## TASK-311 · Price query API · [P1] [API]

### Contexto
La app móvil consume precios últimos + histórico corto. El motor de proyección consume series completas. Dos casos de uso distintos.

### Objetivo
Endpoints de consulta con cache Redis agresivo para "último precio".

### Archivos a crear
- `contexts/prices/prices-application/.../queries/` (latest, series, compare)
- `contexts/prices/prices-interfaces/.../PricesResource.java`

### Endpoints
```http
GET /v1/prices/latest?country=GT&crops=TOMATO,ONION
  → { "TOMATO": { "mode": 6.00, "currency": "GTQ", "unit": "KG", "observed_at": "..." }, ... }

GET /v1/prices/series?crop=TOMATO&market=GT-CENMA&from=2026-01-01&to=2026-04-20
  → array de price points diarios

GET /v1/prices/compare?crop=TOMATO&country=GT&markets=GT-CENMA,GT-SAN-MARTIN
```

### Cache strategy
- `latest`: Redis TTL 15 min, keyed por `country+crop`
- `series`: HTTP cache headers, ETag, 304 si no cambió
- Invalidación en event `PriceObserved.v1`

### Criterios de aceptación
- [ ] `latest` sirve desde cache en < 10ms p95
- [ ] `series` < 200ms para rango 90 días
- [ ] Invalidación cache funciona tras nuevo precio

### Dependencias
TASK-309, TASK-310

---

## 3.4 Alerts & Notifications

---

## TASK-312 · Alert rules engine · [P1] [DOMAIN] [EVENT]

### Contexto
Las alertas se generan en respuesta a eventos (ProjectionUpdated, PriceAnomalyDetected, WeatherAlertIssued). Engine de reglas DSL simple.

### Objetivo
Engine de reglas con DSL declarativo + evaluación por evento.

### Archivos a crear
- Migración `V050__alerts.sql`
- `contexts/alerts/alerts-domain/src/main/java/gt/agromis/alerts/domain/`
  - `AlertRule.java` (entity con criteria JSON)
  - `Alert.java` (aggregate — una instancia entregada a un farmer)
  - `AlertId.java`, `AlertSeverity.java`
  - `events/AlertGeneratedV1.java`
- `contexts/alerts/alerts-application/.../eventhandlers/`
  - `OnProjectionUpdated.java` (listener Kafka)
  - `OnPriceAnomalyDetected.java`
  - `OnWeatherAlertIssued.java`

### DSL de rules (JSON)
```json
{
  "rule_id": "surplus-warning",
  "event_type": "ProjectionUpdated.v1",
  "conditions": {
    "surplus_probability": { "op": "gt", "value": 0.7 },
    "horizon_days": { "op": "lte", "value": 90 }
  },
  "audience": {
    "type": "FARMERS_WITH_PLANTING",
    "crop": "$event.crop_code",
    "corridor": "$event.corridor_id"
  },
  "template_id": "surplus_warning_v1",
  "severity": "HIGH",
  "cooldown_hours": 24
}
```

### Criterios de aceptación
- [ ] Rule engine evalúa regla ante evento
- [ ] Cooldown previene spam (misma rule+farmer no 2 veces en 24h)
- [ ] Alert persistida + evento `AlertGeneratedV1` publicado (para que Dispatch lo tome)
- [ ] Tests con reglas configurables

### Dependencias
TASK-301 (farmers con preferencias)

---

## TASK-313 · Notification dispatch (push + SMS + WhatsApp) · [P1] [API] [EVENT]

### Contexto
Consumer del topic `alerts.generated` que resuelve canal óptimo y despacha vía proveedor.

### Objetivo
Dispatcher multi-canal con circuit breaker por proveedor.

### Archivos a crear
- `contexts/alerts/alerts-infrastructure/`
  - `TwilioSmsAdapter.java`
  - `WhatsAppCloudApiAdapter.java`
  - `FcmPushAdapter.java`
  - `ChannelResolver.java`
  - `TemplateRenderer.java` (Mustache)
  - `DispatchMetrics.java`
- `contexts/alerts/alerts-application/.../DispatchNotificationHandler.java`

### Channel resolver logic
```
1. Si farmer.preferred_channel disponible y healthy → usar
2. Si no, fallback order: PUSH → WHATSAPP → SMS → VOICE
3. Respect quiet hours (farmer preference)
4. Respect daily cap (max_per_day)
5. Respect cost cap por tenant
```

### Criterios de aceptación
- [ ] Test con SMS (Twilio sandbox) + WhatsApp mock + FCM dry-run
- [ ] Circuit breaker: Twilio caído → fallback WhatsApp automático
- [ ] Evento `NotificationDispatchedV1` publicado
- [ ] Retries con exp backoff hasta 3 veces
- [ ] Cost guard: superar cap diario → alerta SRE + degradación

### Dependencias
TASK-312, proveedores configurados (vars de entorno)

---

## TASK-314 · Alerts inbox API · [P1] [API]

### Contexto
App móvil muestra historial de alertas. Endpoint paginado.

### Objetivo
`GET /v1/alerts/inbox` + `POST /v1/alerts/{id}/ack`.

### Criterios de aceptación
- [ ] Paginación cursor-based
- [ ] Filtros por read/unread, severity, fecha
- [ ] Ack idempotente

### Dependencias
TASK-312

---

## 3.5 Projections adapter

---

## TASK-315 · Projections domain + read model · [P1] [DOMAIN] [DB]

### Contexto
El motor de proyecciones vive en Python (ver EPIC 04). Este contexto es un adapter: consume eventos `ProjectionUpdated.v1` y materializa en tabla para serving vía API.

### Objetivo
Tabla `projections` + consumer + API de consulta.

### Archivos a crear
- Migración `V060__projections.sql` (ya en doc §4.4.5)
- `contexts/projections/projections-domain/.../Projection.java` (read model, no aggregate)
- `contexts/projections/projections-application/.../OnProjectionUpdated.java` (listener)
- `contexts/projections/projections-interfaces/.../ProjectionsResource.java`

### Endpoints
```http
GET /v1/projections?country=GT&crop=TOMATO&corridor=GT-CHIMALTENANGO&horizon=60
GET /v1/projections/suggestions?parcel_id=...&horizon=90
  → cultivos alternativos ranqueados
```

### Criterios de aceptación
- [ ] Upsert idempotente en recepción de evento
- [ ] Query API < 100ms p95
- [ ] Métrica: `projections_stored_total{country,crop}`

### Dependencias
TASK-303 (parcels para what-if), EPIC04: 401+ (producer)

---

## TASK-316 · Corridors catalog · [P1] [DB]

### Contexto
"Corredor productivo" es entidad de dominio distinta a municipio. Agrupa zonas agroclimáticamente homogéneas.

### Objetivo
Tabla `corridors` + seed para GT + endpoint de listado.

### Archivos a crear
- Migración `V061__corridors.sql`
- Seed GT (al menos 10 corredores: Chimaltenango, Altiplano Occidental, Boca Costa, Petén, etc.)
- `GET /v1/corridors?country=GT`

### Schema
```sql
CREATE TABLE corridors (
    id VARCHAR(64) PRIMARY KEY,             -- "GT-CHIMALTENANGO"
    country_code CHAR(2) NOT NULL,
    name VARCHAR(100) NOT NULL,
    geometry GEOMETRY(MultiPolygon, 4326) NOT NULL,
    altitude_range_m INT4RANGE,
    climate_zone VARCHAR(40),
    primary_crops TEXT[],
    active BOOLEAN NOT NULL DEFAULT true
);
CREATE INDEX idx_corridors_geom ON corridors USING GIST (geometry);
```

### Criterios de aceptación
- [ ] Seed GT con geometrías reales (shapefile MAGA disponible públicamente)
- [ ] Función `find_corridor_for_point(lat, lon, country)` útil en otros contextos

### Dependencias
EPIC02: 205

---

## 3.6 GIS Read Model

---

## TASK-317 · GIS aggregations + H3 materialized views · [P2] [DOMAIN] [DB]

### Contexto
El mapa de calor necesita agregados rápidos por H3 cell. Mantenemos tablas materializadas actualizadas por eventos.

### Objetivo
Tablas de agregados por H3 + refresh incremental.

### Archivos a crear
- Migración `V070__gis_aggregates.sql`
- `contexts/gis/gis-application/.../eventhandlers/`
  - `OnPlantingReportAggregation.java`
  - `OnProjectionAggregation.java`

### Schema
```sql
CREATE TABLE gis_agg_intentions (
    h3_cell CHAR(15) NOT NULL,
    resolution SMALLINT NOT NULL,             -- 5, 7, 9
    country_code CHAR(2) NOT NULL,
    crop_code VARCHAR(32) NOT NULL,
    window_start DATE NOT NULL,               -- mes de siembra esperada
    window_end DATE NOT NULL,
    n_reports INT NOT NULL DEFAULT 0,
    total_area_ha NUMERIC(14,4) NOT NULL DEFAULT 0,
    last_updated TIMESTAMPTZ NOT NULL DEFAULT now(),
    PRIMARY KEY (h3_cell, resolution, crop_code, window_start)
);

CREATE TABLE gis_agg_projections (
    h3_cell CHAR(15) NOT NULL,
    resolution SMALLINT NOT NULL,
    country_code CHAR(2) NOT NULL,
    crop_code VARCHAR(32) NOT NULL,
    horizon_end DATE NOT NULL,
    surplus_probability NUMERIC(4,3),
    shortage_probability NUMERIC(4,3),
    ci_low NUMERIC(4,3),
    ci_high NUMERIC(4,3),
    n_reports INT NOT NULL,
    computed_at TIMESTAMPTZ NOT NULL,
    PRIMARY KEY (h3_cell, resolution, crop_code, horizon_end)
);
```

### K-anonimato
- Default: si `n_reports < 5` → la celda no se expone (se vuelve NULL en la query final)
- Configurable por tenant

### Criterios de aceptación
- [ ] En recepción de `PlantingReportSubmittedV1`, incrementa agregados res 5/7/9
- [ ] Update atómico (upsert)
- [ ] Query agregada < 50ms para bbox típico

### Dependencias
TASK-303, TASK-304, TASK-315

---

## TASK-318 · MVT tile server · [P2] [API]

### Contexto
Servir tiles vectoriales Mapbox (MVT) para MapLibre cliente. Tiles generados on-demand con cache agresivo.

### Objetivo
`GET /v1/gis/tiles/{z}/{x}/{y}.mvt?layer=...&crop=...&window=...` funcional.

### Archivos a crear
- `contexts/gis/gis-interfaces/.../TilesResource.java`
- `contexts/gis/gis-infrastructure/.../MvtTileBuilder.java`
- Usa PostGIS `ST_AsMVT`

### Ejemplo query PostGIS
```sql
WITH bbox AS (SELECT ST_TileEnvelope(:z, :x, :y) AS geom),
     cells AS (
       SELECT
         h3_cell,
         surplus_probability,
         h3_cell_to_boundary(h3_cell) AS geom
       FROM gis_agg_projections
       WHERE resolution = :resolution
         AND crop_code = :crop
         AND horizon_end <= :horizon_end
         AND n_reports >= 5       -- k-anonymity
         AND h3_cell_to_boundary(h3_cell) && (SELECT geom FROM bbox)
     )
SELECT ST_AsMVT(cells.*, 'projection_layer', 4096, 'geom') FROM cells;
```

### Cache
- HTTP `Cache-Control: public, max-age=900, s-maxage=900`
- ETag basado en max(computed_at) de celdas en la tile
- CDN-friendly

### Criterios de aceptación
- [ ] Tile z=8 típico < 50ms p95
- [ ] Tile vacío (zoom muy alto en zona sin data) < 10ms
- [ ] K-anonymity: celdas con < 5 reports no aparecen
- [ ] Content-Type `application/vnd.mapbox-vector-tile`

### Dependencias
TASK-317

---

## TASK-319 · Aggregate GIS endpoint (JSON) · [P2] [API]

### Contexto
Para casos donde el cliente no quiere MVT sino JSON (dashboards, exports).

### Objetivo
`GET /v1/gis/aggregate?bbox=...&resolution=7&crop=...&metric=...` devuelve JSON.

### Criterios de aceptación
- [ ] Response < 100ms para bbox de país
- [ ] Respeta k-anonymity

### Dependencias
TASK-317

---

## TASK-320 · Tile pre-rendering for hot zones · [P3] [INFRA]

### Contexto
Zonas activas (top 20% de reportes) tienen tiles renderizados offline cada noche.

### Objetivo
CronJob que pre-renderiza y cachea en Redis.

### Archivos a crear
- `contexts/gis/gis-infrastructure/.../TilePreRenderJob.java`
- K8s CronJob config

### Criterios de aceptación
- [ ] Hot zones (definidas por query) → tiles pre-cached
- [ ] Job completa en < 15 min
- [ ] Cache hit rate > 80% en horario pico

### Dependencias
TASK-318

---

## TASK-321 · Corridors overlay endpoint · [P3] [API]

### Contexto
Cliente a veces quiere ver corridors como overlay sobre el mapa.

### Objetivo
`GET /v1/gis/corridors.geojson?country=GT`.

### Criterios de aceptación
- [ ] GeoJSON simplificado (ST_Simplify) para reducir tamaño
- [ ] Cache 24h

### Dependencias
TASK-316

---

## 3.7 Buyers

---

## TASK-322 · Buyer aggregate + registration · [P2] [DOMAIN] [API]

### Contexto
Comprador registrado con KYC mínimo. Puede declarar demandas.

### Objetivo
Agregado Buyer + endpoints.

### Archivos a crear
- Migración `V080__buyers.sql`
- `contexts/buyers/buyers-domain/.../Buyer.java`, `BusinessId.java` (NIT/RTN/RUT)
- `contexts/buyers/buyers-interfaces/.../BuyersResource.java`

### Criterios de aceptación
- [ ] Validación formato NIT por país
- [ ] Verification workflow manual por admin
- [ ] Evento `BuyerVerifiedV1` publicado

### Dependencias
EPIC02: 205

---

## TASK-323 · DemandDeclaration entity + CRUD · [P2] [API] [EVENT]

### Contexto
Comprador declara "necesito X mt de cultivo Y en corridor Z entre fechas A-B".

### Objetivo
Endpoints + evento `DemandDeclaredV1`.

### Archivos a crear
- Migración `V081__demand_declarations.sql`
- `contexts/buyers/.../DemandDeclaration.java`
- Endpoints `POST/GET/DELETE /v1/buyers/demands`

### Criterios de aceptación
- [ ] Validaciones: volumen > 0, ventana en el futuro, corridor existe
- [ ] Evento `DemandDeclaredV1` consumido por Projection Engine

### Dependencias
TASK-322, TASK-316

---

## TASK-324 · Buyer dashboard endpoints · [P2] [API]

### Contexto
Dashboard del comprador: ver proyecciones relevantes a sus demandas.

### Objetivo
`GET /v1/buyers/dashboard` + `GET /v1/buyers/demands/{id}/matches`.

### Criterios de aceptación
- [ ] Solo muestra agregados H3 (k-anon); nunca farmers individuales
- [ ] Matches ordenados por fit (volumen × proximidad × fecha)

### Dependencias
TASK-323, TASK-317

---

## 3.8 Incentives / Reputation

---

## TASK-325 · Reputation aggregate + score calculation · [P2] [DOMAIN]

### Contexto
Score 0-1000 por farmer. Actualizado por eventos (report submitted, validated, harvest matches expectation).

### Objetivo
Agregado Reputation con historial.

### Archivos a crear
- Migración `V090__reputation.sql`
- `contexts/incentives/incentives-domain/.../Reputation.java`, `ReputationTier.java`, `ReputationDelta.java`

### Schema
```sql
CREATE TABLE reputation_snapshots (
    id UUID PRIMARY KEY DEFAULT uuid_generate_v7(),
    farmer_id UUID NOT NULL,
    country_code CHAR(2) NOT NULL,
    score INT NOT NULL,
    tier VARCHAR(16) NOT NULL,           -- BRONZE, SILVER, GOLD, PLATINUM
    updated_at TIMESTAMPTZ NOT NULL DEFAULT now(),
    reason VARCHAR(80) NOT NULL,
    delta INT NOT NULL,
    previous_score INT NOT NULL
);

CREATE INDEX idx_reputation_farmer ON reputation_snapshots (farmer_id, updated_at DESC);
```

### Tiers
- BRONZE: 0-249
- SILVER: 250-499
- GOLD: 500-799
- PLATINUM: 800-1000

### Rules (inicial)
- Report submitted: +10 (tentative — confirmable tras validación)
- Report validated by extensionist: +50
- Harvest matches projection (< 20% error): +30
- Reported but no harvest in 30d after expected: -20
- Fraud suspected: -200 + tier bloqueado

### Criterios de aceptación
- [ ] Cada cambio de score crea snapshot (inmutable)
- [ ] Evento `ReputationChangedV1` publicado
- [ ] Tier recalculado automáticamente al cambiar score

### Dependencias
TASK-301

---

## TASK-326 · Reputation event handlers · [P2] [EVENT]

### Contexto
Consumer de múltiples eventos que ajustan reputation.

### Objetivo
Event handlers con reglas.

### Archivos a crear
- `contexts/incentives/incentives-application/.../eventhandlers/`
  - `OnReportSubmitted.java`
  - `OnReportValidated.java`
  - `OnHarvestReported.java`

### Criterios de aceptación
- [ ] Saga orquestada si múltiples eventos llegan desordenados
- [ ] Idempotencia: evento repetido no duplica delta

### Dependencias
TASK-325

---

## TASK-327 · Incentives redemption API · [P2] [API]

### Contexto
Farmer canjea puntos por saldo móvil (mock en MVP; integración real en Fase 2).

### Objetivo
`POST /v1/incentives/redeem`.

### Archivos a crear
- Migración `V091__incentive_redemptions.sql`
- `contexts/incentives/.../RedeemIncentiveCommand.java`
- Mock adapter `MockTelecomTopupAdapter`

### Criterios de aceptación
- [ ] Deducción + registro de redemption atómica
- [ ] Saga que compensa si topup falla
- [ ] Métrica cost tracking

### Dependencias
TASK-325

---

## TASK-328 · Fraud detection baseline · [P3] [DOMAIN]

### Contexto
Detección simple: muchos reportes en poco tiempo desde mismo device, patrones geo sospechosos.

### Objetivo
Rules engine simple + flag automático.

### Archivos a crear
- `contexts/incentives/.../FraudDetector.java`

### Rules iniciales
- Más de 10 reportes/hora desde mismo device → flag
- Parcelas con overlap > 50% entre distintos farmers → flag
- Reportes con planted_area = suma(parcelas) pero área real estimada << → flag

### Criterios de aceptación
- [ ] Flag dispara evento `FraudSuspectedV1`
- [ ] Dashboard admin para review

### Dependencias
TASK-325

---

## TASK-329 · Cooperative aggregate · [P2] [DOMAIN] [API]

### Contexto
Cooperativa agrupa farmers. Un admin puede reportar por sus miembros.

### Objetivo
Entidad Cooperative + link/unlink.

### Archivos a crear
- Migración `V092__cooperatives.sql`
- `contexts/farmers/.../Cooperative.java`

### Criterios de aceptación
- [ ] Un farmer puede estar en máximo 1 cooperativa
- [ ] Admin con `COOPERATIVE_ADMIN` role puede submit reports on-behalf

### Dependencias
TASK-301

---

## 3.9 Integration bridges

---

## TASK-330 · Consumer adapter: Projection events → projections context · [P1] [EVENT]

### Contexto
El motor Python publica `ProjectionUpdated.v1`. Java context lo consume.

### Objetivo
Kafka consumer con mapping Avro → dominio Java.

### Archivos a crear
- `contexts/projections/projections-infrastructure/.../ProjectionAvroConsumer.java`
- Mapper

### Criterios de aceptación
- [ ] Evento Python → row en `projections` table
- [ ] Idempotencia

### Dependencias
TASK-315, EPIC04: 413

---

## TASK-331 · Consumer adapter: Weather events · [P2] [EVENT]

### Contexto
Integration module publica weather; varios contextos pueden consumirlo (alerts por ejemplo).

### Objetivo
Consumer compartido en `shared-eventing`.

### Criterios de aceptación
- [ ] Weather alert dispara Alert rule

### Dependencias
TASK-312, EPIC05: 505

---

## 3.10 Cross-context tests

---

## TASK-332 · End-to-end test: farmer journey · [P1] [TEST]

### Contexto
Un test que ejercita el flujo completo: register farmer → add parcel → submit report → receive projection → receive alert.

### Objetivo
Test integration con Testcontainers que corre el ciclo completo.

### Archivos a crear
- `backend/app/src/test/java/.../e2e/FarmerJourneyIT.java`

### Criterios de aceptación
- [ ] Test pasa en < 2 min
- [ ] Assertions cubren state final + eventos publicados

### Dependencias
TASK-301 - TASK-315

---

## TASK-333 · Contract tests entre contextos (Pact) · [P2] [TEST]

### Contexto
Aunque sea monolith modular, los contextos tienen APIs. Los consumidores declaran contratos.

### Objetivo
Pact broker + primeros contratos.

### Archivos a crear
- `backend/tests/contract/`
- Pact broker en `infra/docker` para dev

### Criterios de aceptación
- [ ] Alerts context declara consumer-pact contra Projections
- [ ] CI valida contracts

### Dependencias
EPIC02: 209

---

## TASK-334 · Chaos test: Kafka partition loss · [P3] [TEST]

### Contexto
Simular caída de partición Kafka y validar que outbox se recupera.

### Objetivo
Test que para una partición Redpanda y valida que eventos se persisten y reenvían.

### Criterios de aceptación
- [ ] Kafka baja 30s → 0 eventos perdidos
- [ ] Métricas de outbox lag se estabilizan

### Dependencias
EPIC02: 205

---

## TASK-335 · Performance baseline: critical flows · [P3] [TEST]

### Contexto
Antes de producción medir baselines.

### Objetivo
k6 scenarios para: submit batch, query tile, get projection, get inbox.

### Criterios de aceptación
- [ ] Baselines documentados en `docs/perf/baselines.md`
- [ ] Regresión > 20% falla CI

### Dependencias
EPIC01: 016

---

## Resumen épica 03

| Contexto | Tareas | Prioridad principal |
|----------|--------|---------------------|
| Farmer Portal | 301-303 | P1 |
| Planting Reports | 304-308 | P1 core, P2 harvest |
| Price Intelligence | 309-311 | P1 |
| Alerts | 312-314 | P1 |
| Projections adapter | 315-316 | P1 |
| GIS Read Model | 317-321 | P2 |
| Buyers | 322-324 | P2 |
| Incentives / Reputation | 325-329 | P2 |
| Integration bridges | 330-331 | P1/P2 |
| Cross-context tests | 332-335 | P1/P3 |

**Paralelización máxima después de fase A**: dev-1 trabaja Prices+Alerts, dev-2 GIS+Projections adapter, dev-3 Buyers+Incentives.

# Épica 02 — Backend: Foundations & Shared Kernel

> **Objetivo**: construir la base técnica del modular monolith Quarkus: estructura Gradle multi-módulo, shared kernel (eventing, tenancy, auth, outbox), conventions de testing. Todo lo que vivirá en `backend/` de aquí en adelante depende de estas tareas.
> **Duración estimada**: 4 semanas
> **Tareas**: 22

---

## TASK-201 · Proyecto Gradle multi-módulo Quarkus · [P0] [INFRA]

### Contexto
El backend es un modular monolith. Un solo deployable, múltiples módulos Gradle con fronteras duras. Cada módulo representa un bounded context DDD.

### Objetivo
Estructura `backend/` con Gradle 8 + Kotlin DSL + version catalog + módulos stub para todos los bounded contexts.

### Archivos a crear
```
backend/
├── settings.gradle.kts
├── build.gradle.kts                    # root, aplica plugins a subproyectos
├── gradle/libs.versions.toml           # version catalog
├── gradle.properties
├── gradlew, gradlew.bat, gradle/wrapper/
├── buildSrc/                           # convenciones custom
│   └── src/main/kotlin/agromis.conventions.gradle.kts
├── shared/
│   ├── shared-domain/                  # DDD building blocks base
│   ├── shared-eventing/                # Kafka + outbox
│   ├── shared-tenancy/                 # multi-country
│   ├── shared-auth/                    # Keycloak integration
│   ├── shared-testing/                 # Testcontainers setup
│   └── shared-web/                     # JAX-RS conventions
├── contexts/
│   ├── farmers/
│   ├── reports/
│   ├── projections/                    # solo adapter; motor en projection-engine/
│   ├── prices/
│   ├── alerts/
│   ├── buyers/
│   ├── incentives/
│   ├── gis/
│   └── integration/
├── app/                                # módulo de ensamblaje (main class Quarkus)
│   └── build.gradle.kts
└── migrations/                         # flyway (ya creado en TASK-005)
```

### Cada módulo de contexto tiene sub-módulos
```
contexts/farmers/
├── farmers-domain/         # sin deps framework
├── farmers-application/    # use cases (command/query handlers)
├── farmers-infrastructure/ # adapters (persistence, kafka)
└── farmers-interfaces/     # REST, CLI
```

### Version catalog (`libs.versions.toml`)
```toml
[versions]
quarkus = "3.9.0"
java = "21"
kotlin = "NONE"
testcontainers = "1.19.7"
h3 = "4.1.1"
mapstruct = "1.5.5.Final"

[libraries]
quarkus-bom = { module = "io.quarkus.platform:quarkus-bom", version.ref = "quarkus" }
quarkus-resteasy-reactive = { module = "io.quarkus:quarkus-resteasy-reactive" }
quarkus-resteasy-reactive-jackson = { module = "io.quarkus:quarkus-resteasy-reactive-jackson" }
quarkus-hibernate-orm-panache = { module = "io.quarkus:quarkus-hibernate-orm-panache" }
quarkus-jdbc-postgresql = { module = "io.quarkus:quarkus-jdbc-postgresql" }
quarkus-smallrye-reactive-messaging-kafka = { module = "io.quarkus:quarkus-smallrye-reactive-messaging-kafka" }
quarkus-oidc = { module = "io.quarkus:quarkus-oidc" }
quarkus-flyway = { module = "io.quarkus:quarkus-flyway" }
quarkus-micrometer-registry-prometheus = { module = "io.quarkus:quarkus-micrometer-registry-prometheus" }
quarkus-opentelemetry = { module = "io.quarkus:quarkus-opentelemetry" }
quarkus-smallrye-health = { module = "io.quarkus:quarkus-smallrye-health" }
quarkus-smallrye-openapi = { module = "io.quarkus:quarkus-smallrye-openapi" }
quarkus-cache = { module = "io.quarkus:quarkus-cache" }
quarkus-redis-client = { module = "io.quarkus:quarkus-redis-client" }
hibernate-spatial = { module = "org.hibernate.orm:hibernate-spatial" }
h3-java = { module = "com.uber:h3", version.ref = "h3" }
testcontainers-postgresql = { module = "org.testcontainers:postgresql" }
testcontainers-redpanda = { module = "org.testcontainers:redpanda" }
testcontainers-keycloak = { module = "com.github.dasniko:testcontainers-keycloak" }
```

### Criterios de aceptación
- [ ] `./gradlew build` compila (aunque módulos vacíos)
- [ ] `./gradlew :contexts:farmers:farmers-domain:dependencies` no muestra deps de Quarkus (domain puro)
- [ ] `./gradlew :app:quarkusDev` arranca Quarkus en puerto 8080
- [ ] Endpoint `/q/health/ready` responde 200
- [ ] Plugins version-catalog, spotless (formatting), errorprone aplicados

### Dependencias
TASK-001

---

## TASK-202 · Shared kernel: domain building blocks · [P0] [DOMAIN]

### Contexto
Todos los bounded contexts comparten abstracciones mínimas de DDD: AggregateRoot, Entity, ValueObject, DomainEvent. Estas clases NO dependen de Quarkus.

### Objetivo
Módulo `shared-domain` con abstracciones base y testeadas.

### Archivos a crear
```
shared/shared-domain/src/main/java/gt/agromis/shared/domain/
├── AggregateRoot.java
├── Entity.java
├── ValueObject.java             # marker interface
├── DomainEvent.java              # interface
├── DomainEventPublisher.java     # colector in-memory para test; impl real en infrastructure
├── Identity.java                 # wrapper UUID v7
├── BusinessException.java
├── InvariantViolatedException.java
└── time/Clock.java               # inyectable para tests
```

### Snippets clave

**AggregateRoot.java**
```java
package gt.agromis.shared.domain;

import java.time.Instant;
import java.util.ArrayList;
import java.util.Collections;
import java.util.List;

public abstract class AggregateRoot<ID extends Identity<?>> extends Entity<ID> {

    private final List<DomainEvent> uncommittedEvents = new ArrayList<>();
    private long version;

    protected AggregateRoot(ID id) {
        super(id);
    }

    protected final void raise(DomainEvent event) {
        uncommittedEvents.add(event);
    }

    public final List<DomainEvent> pullEvents() {
        List<DomainEvent> copy = Collections.unmodifiableList(new ArrayList<>(uncommittedEvents));
        uncommittedEvents.clear();
        return copy;
    }

    public long version() { return version; }

    protected void incrementVersion() { this.version++; }
}
```

**DomainEvent.java**
```java
public interface DomainEvent {
    String eventId();           // UUID v7
    String aggregateId();
    String eventType();         // "PlantingReportSubmitted.v1"
    Instant occurredAt();
    String countryCode();       // siempre presente para multi-tenancy
}
```

### Criterios de aceptación
- [ ] Tests unitarios con 100% coverage en shared-domain
- [ ] Sin dependencias Quarkus/JPA/Jackson
- [ ] Javadoc en cada clase pública

### Dependencias
TASK-201

---

## TASK-203 · Shared kernel: multi-tenancy (country context) · [P0] [DOMAIN] [SEC]

### Contexto
Cada request HTTP o mensaje Kafka trae un `country_code`. Debe propagarse a todos los servicios y llegar a Postgres como variable de sesión (`SET app.current_country = 'GT'`) para que RLS funcione.

### Objetivo
Módulo `shared-tenancy` con `TenantContext` (ThreadLocal + request-scoped), filtro JAX-RS, interceptor JPA, header propagation en Kafka.

### Archivos a crear
```
shared/shared-tenancy/src/main/java/gt/agromis/shared/tenancy/
├── TenantContext.java              # RequestScoped holder
├── TenantResolver.java             # extrae de JWT o header X-Country-Code
├── TenantFilter.java               # JAX-RS @Provider
├── TenantJpaInterceptor.java        # set app.current_country on each tx
├── TenantKafkaHeaderPropagator.java
├── CountryCode.java                # value object validado
└── UnauthorizedTenantException.java
```

### Claves JWT esperadas
```json
{
  "sub": "farmer-uuid",
  "country_code": "GT",
  "tenant_id": "uuid",
  "roles": ["FARMER"]
}
```

### Criterios de aceptación
- [ ] Request sin `country_code` en JWT → 401
- [ ] Request con `country_code=XX` no válido → 400
- [ ] En cualquier `@Transactional`, query a PG hace `SET app.current_country`
- [ ] Header `X-Country-Code` se inyecta en mensajes Kafka outbound
- [ ] Tests de integración con Testcontainers validan el flujo end-to-end

### Dependencias
TASK-202, TASK-207 (auth)

---

## TASK-204 · Shared kernel: eventing (Kafka + Schema Registry) · [P0] [EVENT]

### Contexto
Contextos publican y consumen eventos de dominio. Los eventos van serializados en Avro con Schema Registry. Necesitamos abstracción tipada sobre Quarkus Reactive Messaging.

### Objetivo
Módulo `shared-eventing` con convention de publicación/consumo, serialización Avro, retry/DLQ, headers de trazabilidad.

### Archivos a crear
```
shared/shared-eventing/
├── src/main/java/gt/agromis/shared/eventing/
│   ├── EventPublisher.java              # fachada
│   ├── EventEnvelope.java               # metadata común
│   ├── AvroEventSerde.java
│   ├── KafkaHeaders.java                # constantes de headers
│   ├── TraceContextPropagator.java
│   ├── DeadLetterHandler.java
│   ├── IdempotencyKey.java
│   └── annotations/
│       ├── Publishes.java
│       └── Subscribes.java
├── src/main/resources/
│   └── META-INF/beans.xml
└── build.gradle.kts
```

### Configuración `application.yml` esperada
```yaml
mp:
  messaging:
    outgoing:
      agromis-events-out:
        connector: smallrye-kafka
        topic: ${topic.name}  # inyectado por call site
        value.serializer: io.confluent.kafka.serializers.KafkaAvroSerializer
        schema.registry.url: ${kafka.schema-registry.url}
    incoming:
      # configurado por cada listener
```

### Criterios de aceptación
- [ ] Test de integración con Testcontainers Redpanda publica + consume evento con round-trip Avro
- [ ] Headers de trazabilidad (`trace-id`, `correlation-id`) propagados
- [ ] Evento fallido tras 3 retries va al topic `<topic>.dlq`
- [ ] Idempotent consumer: evento duplicado (misma `idempotency_key`) no se procesa dos veces

### Dependencias
TASK-011, TASK-201

---

## TASK-205 · Shared kernel: Outbox pattern · [P0] [EVENT] [DB]

### Contexto
Para garantizar publicación confiable de eventos de dominio, la tx SQL escribe a tabla `domain_events` (outbox) junto con el state del aggregate; un worker asíncrono empuja al bus Kafka.

### Objetivo
Tabla outbox + worker + abstracción para que los handlers solo invoquen `outbox.append(event)`.

### Archivos a crear
- Migración `V010__domain_events_outbox.sql`
- `shared/shared-eventing/src/main/java/gt/agromis/shared/eventing/outbox/`
  - `OutboxEntry.java`
  - `OutboxRepository.java`
  - `OutboxAppender.java`
  - `OutboxPublisher.java`        # @Scheduled worker
  - `OutboxPublishingMetrics.java`

### Migración V010
```sql
CREATE TABLE domain_events (
    id UUID PRIMARY KEY DEFAULT uuid_generate_v7(),
    aggregate_type VARCHAR(50) NOT NULL,
    aggregate_id UUID NOT NULL,
    event_type VARCHAR(80) NOT NULL,
    event_version SMALLINT NOT NULL,
    country_code CHAR(2) NOT NULL,
    payload_avro BYTEA NOT NULL,
    metadata JSONB NOT NULL,
    target_topic VARCHAR(120) NOT NULL,
    occurred_at TIMESTAMPTZ NOT NULL,
    sequence_number BIGSERIAL NOT NULL,
    published_at TIMESTAMPTZ,
    publish_attempts SMALLINT NOT NULL DEFAULT 0,
    last_publish_error TEXT
) PARTITION BY RANGE (occurred_at);

CREATE TABLE domain_events_2026_04 PARTITION OF domain_events
    FOR VALUES FROM ('2026-04-01') TO ('2026-05-01');
CREATE TABLE domain_events_2026_05 PARTITION OF domain_events
    FOR VALUES FROM ('2026-05-01') TO ('2026-06-01');
-- generar siguientes particiones mensualmente via cron

CREATE INDEX idx_outbox_unpublished ON domain_events (occurred_at)
    WHERE published_at IS NULL;
CREATE INDEX idx_outbox_aggregate ON domain_events (aggregate_id, sequence_number);
```

### Worker behavior
- Cada 500ms: `SELECT ... FROM domain_events WHERE published_at IS NULL ORDER BY sequence_number LIMIT 100`
- Publica al topic indicado en `target_topic`
- Marca `published_at`
- Si falla: incrementa `publish_attempts`, log error, retry con backoff; tras 10 intentos → alerta

### Criterios de aceptación
- [ ] Escribir aggregate + outbox entry en misma tx → evento llega a Kafka
- [ ] Si Kafka está caído, la tx de negocio completa OK
- [ ] Tras restaurar Kafka, eventos acumulados se publican en orden
- [ ] Métrica Prometheus: `outbox_lag_seconds`, `outbox_unpublished_count`
- [ ] Particiones mensuales automáticas (CronJob)

### Dependencias
TASK-204, TASK-005

---

## TASK-206 · Shared kernel: base REST + OpenAPI + problem+json · [P0] [API]

### Contexto
Convenciones consistentes en todos los endpoints REST: versionado URL, problem+json para errores (RFC 7807), pagination, idempotency-key.

### Objetivo
Módulo `shared-web` con infraestructura JAX-RS común.

### Archivos a crear
```
shared/shared-web/src/main/java/gt/agromis/shared/web/
├── Problem.java                           # RFC 7807
├── GlobalExceptionMapper.java
├── IdempotencyKeyFilter.java
├── IdempotencyStore.java                  # Redis-backed
├── pagination/Page.java
├── pagination/PageRequest.java
├── pagination/PageParam.java              # query param extractor
├── versioning/ApiVersion.java
└── validation/ValidationErrorMapper.java
```

### Ejemplo problem+json response
```json
{
  "type": "https://agromis.gt/errors/invariant-violated",
  "title": "Planting area must be positive",
  "status": 422,
  "detail": "Reported area 0.0 is not greater than 0",
  "instance": "/v1/reports/abc",
  "errors": [
    { "field": "planted_area_ha", "code": "MIN_VALUE", "message": "must be > 0" }
  ],
  "traceId": "abc123"
}
```

### Criterios de aceptación
- [ ] Excepción de dominio `InvariantViolatedException` → 422 problem+json
- [ ] Excepción `EntityNotFoundException` → 404 problem+json
- [ ] Validación Jakarta → 400 con lista `errors[]`
- [ ] `Idempotency-Key` en POST: primera llamada procesa; réplicas con misma key → mismo resultado (cached en Redis TTL 24h)
- [ ] OpenAPI generado en `/q/openapi` contiene `components.responses.Problem`

### Dependencias
TASK-201

---

## TASK-207 · Shared kernel: autenticación Keycloak + OIDC · [P0] [SEC]

### Contexto
Keycloak como IDP. Quarkus tiene soporte OIDC out-of-the-box.

### Objetivo
Integración Quarkus OIDC + realm `agromis` con roles + mappers que incluyen `country_code` en JWT.

### Archivos a crear
- `infra/docker/keycloak/realm-export.json` (update con roles y mappers)
- `shared/shared-auth/src/main/java/gt/agromis/shared/auth/`
  - `Roles.java` (constantes)
  - `CurrentUser.java` (RequestScoped — resuelve del JWT)
  - `CountryCodeMapper.java`
  - `ReputationTierClaim.java`
- `backend/app/src/main/resources/application.yml` sección oidc

### Realm `agromis`
- Clients: `agromis-mobile` (public), `agromis-web` (public + PKCE), `agromis-service` (confidential para M2M)
- Roles: `FARMER`, `FARMER_EXTENSIONIST`, `COOPERATIVE_ADMIN`, `BUYER`, `GOV_VIEWER`, `SYSTEM_ADMIN`
- Protocol mapper que incluye `country_code` (atributo del user) en el token
- Token lifespan: access 15min, refresh 30d

### Criterios de aceptación
- [ ] Endpoint `/v1/ping` con `@RolesAllowed("FARMER")` retorna 401 sin token, 403 con token sin rol, 200 con rol
- [ ] `CurrentUser.countryCode()` devuelve el claim del JWT
- [ ] Tests de integración con `testcontainers-keycloak` — setup realm + issue token
- [ ] Flujo PKCE probado para web client

### Dependencias
TASK-203, TASK-003 (Keycloak local)

---

## TASK-208 · Shared kernel: CQRS mediator (command/query handlers) · [P0] [DOMAIN]

### Contexto
El equipo ya tiene experiencia con patrón Mediator/CQRS (referencia a proyectos previos con `workplan-api` usando CQRS). Vamos a adoptar el mismo estilo para consistencia.

### Objetivo
Abstracción liviana de mediator para dispatch de commands/queries a sus handlers.

### Archivos a crear
```
shared/shared-domain/src/main/java/gt/agromis/shared/application/
├── Command.java                    # marker
├── Query.java                      # marker
├── CommandHandler.java             # <C extends Command, R>
├── QueryHandler.java
├── Mediator.java                   # fachada
├── MediatorImpl.java               # CDI-backed
└── annotations/
    ├── Handles.java
    └── Validated.java              # integra jakarta.validation
```

### Ejemplo de uso
```java
public record RegisterFarmerCommand(
    @NotNull String phoneE164,
    @NotNull String countryCode,
    String fullName
) implements Command<FarmerId> {}

@ApplicationScoped
public class RegisterFarmerHandler implements CommandHandler<RegisterFarmerCommand, FarmerId> {
    @Inject Farmers farmers;
    @Inject OutboxAppender outbox;

    @Override @Transactional
    public FarmerId handle(RegisterFarmerCommand cmd) {
        Farmer farmer = Farmer.register(cmd.phoneE164(), CountryCode.of(cmd.countryCode()), cmd.fullName());
        farmers.save(farmer);
        farmer.pullEvents().forEach(outbox::append);
        return farmer.id();
    }
}

// En el resource
FarmerId id = mediator.send(new RegisterFarmerCommand(phone, "GT", name));
```

### Criterios de aceptación
- [ ] `Mediator.send(command)` descubre handler vía CDI
- [ ] Validación Jakarta aplicada automáticamente a commands marcados `@Validated`
- [ ] Si no hay handler para un command → error de arranque (fail fast)
- [ ] Test con > 90% coverage

### Dependencias
TASK-202

---

## TASK-209 · Shared kernel: testing infrastructure · [P0] [TEST]

### Contexto
Sin buena base de testing, el proyecto degrada. Testcontainers + fixtures + BDD helpers.

### Objetivo
Módulo `shared-testing` con abstracciones comunes.

### Archivos a crear
```
shared/shared-testing/src/main/java/gt/agromis/shared/testing/
├── IntegrationTest.java              # meta-annotation
├── containers/
│   ├── SharedPostgresContainer.java  # singleton reusable across tests
│   ├── SharedRedpandaContainer.java
│   ├── SharedRedisContainer.java
│   └── SharedKeycloakContainer.java
├── fixtures/
│   ├── FarmerFixtures.java
│   ├── ParcelFixtures.java
│   └── CropFixtures.java
├── EventCollector.java               # para assert de eventos publicados
├── AuthTokenFactory.java             # genera JWTs válidos para tests
└── DbCleaner.java                    # truncate en @BeforeEach
```

### Criterios de aceptación
- [ ] Primer test de integración que usa `@IntegrationTest` arranca Postgres + Redpanda + Keycloak en < 30s (containers reutilizados)
- [ ] `EventCollector` permite `assertThat(events).containsEvent(FarmerRegistered.class)`
- [ ] Fixtures ofrecen `.aFarmer().withCountry("GT").build()` estilo builder

### Dependencias
TASK-201

---

## TASK-210 · Health checks y readiness probes · [P0] [OBS]

### Contexto
Kubernetes necesita health checks precisos. Quarkus tiene `smallrye-health`, pero necesitamos custom checks (DB, Kafka, Redis).

### Objetivo
Health checks custom + probe strategy documentada.

### Archivos a crear
```
shared/shared-web/src/main/java/gt/agromis/shared/web/health/
├── DatabaseReadinessCheck.java
├── KafkaReadinessCheck.java
├── RedisReadinessCheck.java
├── OutboxLivenessCheck.java         # detecta outbox stuck
└── BuildInfoHealth.java              # readiness ok + metadata build
```

### Probe strategy
- **Startup probe**: `/q/health/started` — 60s grace, Kafka/DB conectando
- **Liveness**: `/q/health/live` — solo checks internos in-memory (JVM alive)
- **Readiness**: `/q/health/ready` — todos los depedencies externos healthy

### Criterios de aceptación
- [ ] DB caída → readiness 503, liveness 200
- [ ] Kafka caída > 60s → readiness 503
- [ ] Outbox con lag > 5min → readiness degraded (still 200 pero warning header)

### Dependencias
TASK-201

---

## TASK-211 · Métricas y tracing OpenTelemetry · [P0] [OBS]

### Contexto
Instrumentación consistente en todos los endpoints/handlers/kafka consumers.

### Objetivo
Auto-instrumentación + custom business metrics + trace context propagado a DB y Kafka.

### Archivos a crear
- `shared/shared-web/src/main/java/gt/agromis/shared/web/observability/`
  - `BusinessMetrics.java`           # Micrometer counters/gauges registry
  - `TraceContextInterceptor.java`
  - `KafkaTraceInjector.java`
- `backend/app/src/main/resources/application.yml` sección OTel

### Métricas de negocio ejemplo
- `agromis_reports_submitted_total{country, crop}`
- `agromis_projections_computed_seconds{country, crop}`
- `agromis_notifications_sent_total{channel, outcome}`

### Criterios de aceptación
- [ ] `/q/metrics` expone métricas JVM + business + HTTP
- [ ] Trace completo de `POST /v1/reports` → DB → outbox → Kafka visible en Tempo
- [ ] Sampling configurable por env (100% dev, 10% prod)

### Dependencias
TASK-201, TASK-010

---

## TASK-212 · Configuración y secrets pattern · [P1] [SEC]

### Contexto
Config por env (dev/staging/prod) con pattern consistente. Secrets via ExternalSecrets en K8s, `.env` local en dev.

### Objetivo
Estructura `application.yml` + sobre-escrituras por profile + template para secrets.

### Archivos a crear
- `backend/app/src/main/resources/application.yml` (base)
- `backend/app/src/main/resources/application-dev.yml`
- `backend/app/src/main/resources/application-staging.yml`
- `backend/app/src/main/resources/application-prod.yml`
- `.env.example` en raíz
- `docs/runbooks/configuration.md`

### Criterios de aceptación
- [ ] Arrancar con `QUARKUS_PROFILE=staging` usa config correcta
- [ ] Ningún secret en el repo (solo `${ENV_VAR:-default}`)
- [ ] Config dump con `/q/dev-ui` en dev solo

### Dependencias
TASK-201, TASK-012

---

## TASK-213 · Docker image multi-stage + distroless · [P0] [INFRA]

### Contexto
Imágenes de los servicios deben ser mínimas y seguras.

### Objetivo
Dockerfile multi-stage para Quarkus native + JVM + distroless + firma con cosign.

### Archivos a crear
- `backend/app/src/main/docker/Dockerfile.jvm`
- `backend/app/src/main/docker/Dockerfile.native`
- `.dockerignore`
- `.github/workflows/build-push-backend.yml`

### Criterios de aceptación
- [ ] Imagen JVM: `quarkus-app` runable, < 200MB
- [ ] Imagen native: < 100MB, cold start < 1s
- [ ] User non-root (`1001`), read-only FS
- [ ] SBOM generado con Syft como attestation OCI

### Dependencias
TASK-201, TASK-008

---

## TASK-214 · Crop catalog API (primer contexto real — simple) · [P0] [API] [DB]

### Contexto
Necesitamos un contexto "sencillo" para validar todo el shared kernel end-to-end antes de atacar contextos complejos. Catálogo de cultivos es ideal: read-mostly, multi-tenant, sin eventos, sin offline.

### Objetivo
Contexto `crops` con endpoints GET (listar y detalle), leyendo de tabla `crop_catalog` ya seedeada.

### Archivos a crear
```
contexts/crops/  (nuevo sub-módulo — agregar a settings.gradle.kts)
├── crops-domain/src/main/java/gt/agromis/crops/domain/
│   ├── Crop.java                    # entity
│   ├── CropCode.java                # VO
│   └── Crops.java                   # repository interface
├── crops-infrastructure/
│   └── JpaCrops.java
└── crops-interfaces/
    └── CropsResource.java            # GET /v1/crops, GET /v1/crops/{code}
```

### Endpoints
```http
GET /v1/crops?category=VEGETABLE
Authorization: Bearer <token con country_code=GT>
Response: [{ code, commonName, scientificName, category, typicalCycleDays }, ...]
  (filtrado por country via RLS)

GET /v1/crops/{code}
Response: { code, commonName, ... }
```

### Criterios de aceptación
- [ ] Test integración: listar cultivos con token GT → solo cultivos GT
- [ ] Token sin country_code → 401
- [ ] OpenAPI actualizado automáticamente
- [ ] Coverage > 80%

### Dependencias
TASK-205, TASK-206, TASK-207, TASK-209

---

## TASK-215 · Local dev loop: hot reload + profile dev optimizado · [P1] [INFRA]

### Contexto
Developer experience. `quarkus:dev` tiene dev UI excelente pero necesitamos optimizar.

### Objetivo
Config dev con hot reload + dev services deshabilitados (usamos docker-compose) + dev UI custom.

### Archivos a crear/modificar
- `backend/app/src/main/resources/application-dev.yml`
- `backend/README.md` sección "Running locally"

### Criterios de aceptación
- [ ] `./gradlew :app:quarkusDev` conecta a PG/Kafka/Redis del docker-compose
- [ ] Cambio en código Java → recarga en < 2s
- [ ] Dev UI muestra todos los módulos custom

### Dependencias
TASK-201, TASK-003

---

## TASK-216 · Testing: BDD-style integration tests framework · [P1] [TEST]

### Contexto
Tests de integración extensos. Adoptar estilo dado-cuando-entonces para lectura.

### Objetivo
DSL fluent para tests de integración reusable.

### Archivos a crear
- `shared/shared-testing/src/main/java/gt/agromis/shared/testing/bdd/`
  - `Scenario.java`
  - `GivenStep.java`
  - `WhenStep.java`
  - `ThenStep.java`
  - `Actors.java`                  # builder de usuarios de prueba

### Ejemplo de test escrito con el framework
```java
@Test
void farmerCanRegister() {
    scenario()
        .given(anonymous())
        .when(postingTo("/v1/farmers").withBody(validFarmer("GT")))
        .then().status(201)
              .headerExists("Location")
              .eventPublished(FarmerRegisteredV1.class);
}
```

### Criterios de aceptación
- [ ] Primer test escrito en BDD style pasa
- [ ] Docs en `shared-testing/README.md`

### Dependencias
TASK-209

---

## TASK-217 · Archunit rules: enforce DDD boundaries · [P1] [TEST] [DOC]

### Contexto
Sin reglas, los devs terminan importando `JpaRepository` desde `domain/`. Archunit garantiza.

### Objetivo
Tests Archunit que fallan el build si se rompen las reglas de DDD.

### Archivos a crear
- `shared/shared-testing/src/test/java/gt/agromis/architecture/`
  - `LayeredArchitectureTest.java`
  - `NamingConventionsTest.java`
  - `NoFrameworkInDomainTest.java`
  - `BoundedContextIsolationTest.java`

### Reglas
- `domain` no depende de `application`, `infrastructure`, `interfaces`
- `domain` no importa `jakarta.persistence`, `io.quarkus`, `com.fasterxml.jackson`
- Nombres: commands terminan en `Command`, queries en `Query`, events en `V1/V2/...`
- Contextos no se importan entre sí excepto via `shared`

### Criterios de aceptación
- [ ] Correr los tests detecta al menos un violation forzado (test negativo)
- [ ] Build falla si un dev importa algo prohibido

### Dependencias
TASK-202

---

## TASK-218 · Rate limiting + API gateway local · [P1] [API] [SEC]

### Contexto
Rate limiting por IP, por user, por tenant. Implementado en edge (API Gateway en prod) pero también en app (defensa en profundidad).

### Objetivo
Filter Quarkus con bucket4j + Redis como backing store.

### Archivos a crear
- `shared/shared-web/src/main/java/gt/agromis/shared/web/ratelimit/`
  - `RateLimitFilter.java`
  - `RateLimitPolicy.java`         # por ruta
  - `RateLimitStore.java`          # Redis

### Policies iniciales
| Path | Limit |
|------|-------|
| `POST /v1/farmers` | 5/min por IP |
| `POST /v1/reports` | 30/min por user |
| `POST /v1/sync/batch` | 10/min por user |
| `GET /*` | 100/min por user |

### Criterios de aceptación
- [ ] Exceeder límite → 429 con `Retry-After` header
- [ ] Métrica `http_requests_rate_limited_total`

### Dependencias
TASK-206

---

## TASK-219 · Audit log infrastructure · [P1] [SEC] [DB]

### Contexto
Cambios sensibles (creación de farmer, validación de reporte, otorgar incentivo) deben auditarse.

### Objetivo
Tabla audit + interceptor + API de consulta (solo admins).

### Archivos a crear
- Migración `V011__audit_log.sql`
- `shared/shared-audit/src/main/java/gt/agromis/shared/audit/`
  - `AuditEvent.java`
  - `Auditable.java`
  - `AuditInterceptor.java`
  - `AuditLogResource.java`          # GET /v1/admin/audit

### Criterios de aceptación
- [ ] Cada command marcado `@Auditable` registra entrada
- [ ] Entrada inmutable (append-only, GRANT solo INSERT)
- [ ] Query por `user_id`, `resource_id`, rango fechas

### Dependencias
TASK-208

---

## TASK-220 · Feature flags con Unleash · [P2] [INFRA]

### Contexto
Rollouts graduales y kill switches. Unleash OSS self-hosted.

### Objetivo
Unleash desplegado + client Quarkus integrado.

### Archivos a crear
- `infra/k8s/unleash/` (helm values)
- `shared/shared-web/src/main/java/gt/agromis/shared/web/features/FeatureFlags.java`

### Criterios de aceptación
- [ ] Flag `use_new_projection_model` leído en código: `if (flags.isEnabled("use_new_projection_model"))`
- [ ] UI Unleash accesible para PM

### Dependencias
TASK-007

---

## TASK-221 · Documentación: API contract docs + README por módulo · [P1] [DOC]

### Contexto
Cada contexto necesita doc mínimo.

### Objetivo
README.md por contexto + OpenAPI agregado publicado.

### Archivos a crear
- `backend/README.md` (overview)
- `contexts/*/README.md` (template)
- `.github/workflows/publish-openapi.yml`
- `docs/api/index.md`

### Criterios de aceptación
- [ ] Cada módulo tiene README con: propósito, puertos (APIs, eventos), dependencias, cómo correr tests
- [ ] OpenAPI combinado publicado a GitHub Pages en cada merge a main

### Dependencias
TASK-201

---

## TASK-222 · Seed data & DevRunbook · [P1] [DB] [DOC]

### Contexto
Un dev nuevo debe poder `git clone → script → tener data realista localmente en 5 min`.

### Objetivo
Script seed con data demo + DevRunbook.

### Archivos a crear
- `backend/migrations/src/main/resources/db/seed-dev/`
  - `R__seed_farmers.sql`             # repeatable migration
  - `R__seed_parcels.sql`
  - `R__seed_reports.sql`
- `scripts/seed-dev.sh`
- `docs/runbooks/developer-onboarding.md`

### Criterios de aceptación
- [ ] `./scripts/seed-dev.sh` inserta ~100 farmers, 200 parcels, 500 reports distribuidos en GT
- [ ] DevRunbook ejecutado por dev nuevo → productivo en < 1h

### Dependencias
TASK-214

---

## Resumen épica 02

| # | Tarea | Prioridad | Deps |
|---|-------|-----------|------|
| 201 | Gradle multi-módulo | P0 | EPIC01:001 |
| 202 | Shared domain | P0 | 201 |
| 203 | Multi-tenancy | P0 | 202, 207 |
| 204 | Eventing Kafka | P0 | EPIC01:011, 201 |
| 205 | Outbox | P0 | 204, EPIC01:005 |
| 206 | REST base + OpenAPI | P0 | 201 |
| 207 | Auth OIDC | P0 | 203, EPIC01:003 |
| 208 | CQRS Mediator | P0 | 202 |
| 209 | Testing infra | P0 | 201 |
| 210 | Health checks | P0 | 201 |
| 211 | OTel | P0 | 201, EPIC01:010 |
| 212 | Config & secrets | P1 | 201, EPIC01:012 |
| 213 | Docker images | P0 | 201, EPIC01:008 |
| 214 | Crop catalog (first context) | P0 | 205, 206, 207, 209 |
| 215 | Dev loop | P1 | 201, EPIC01:003 |
| 216 | BDD tests | P1 | 209 |
| 217 | Archunit | P1 | 202 |
| 218 | Rate limit | P1 | 206 |
| 219 | Audit log | P1 | 208 |
| 220 | Feature flags | P2 | EPIC01:007 |
| 221 | Docs APIs | P1 | 201 |
| 222 | Seed data | P1 | 214 |

**Hito crítico**: al completar 201–214, el equipo tiene cimientos para paralelizar todos los contextos restantes (EPIC 03) entre 2+ devs.

# AgroMIS — Backlog Técnico MVP

> **Índice maestro de épicas y tareas para ejecución con Claude Code**
> Stack: Java 21 + Quarkus 3.x · PostgreSQL 16 + PostGIS + Timescale · Flutter 3 · Kafka/Redpanda
> Alcance: MVP Guatemala con arquitectura preparada para multi-país
> Última actualización: 2026

---

## Cómo usar este backlog

Cada tarea de este backlog está diseñada para **ejecutarse en una sesión de Claude Code de 1–4 horas**. La estructura de cada tarea es consistente:

```
TASK-XXX: Nombre de la tarea
├── Contexto            → qué está hecho antes, qué asume
├── Objetivo            → qué produce (output)
├── Archivos a crear/modificar
├── Criterios de aceptación (checklist binario)
├── Comandos de verificación (cómo probar que funciona)
└── Dependencias        → qué tareas deben estar hechas antes
```

**Flujo sugerido para Claude Code**:

1. Abrir el repo `agromis-platform` en Claude Code.
2. Copiar la tarea completa (desde `TASK-XXX` hasta el final) como prompt inicial de la sesión.
3. Revisar plan que propone Claude Code antes de ejecutar.
4. Ejecutar, revisar diff, commit con el `task-id` en el mensaje.

**Regla de oro**: no saltar dependencias. Si `TASK-042` depende de `TASK-030`, no iniciar `042` sin haber cerrado `030`.

---

## Épicas

| # | Épica | Archivo | # Tareas | Duración estimada |
|---|-------|---------|---------|-------------------|
| 01 | Infraestructura base & DevOps | [`01_EPIC_INFRA.md`](./01_EPIC_INFRA.md) | 18 | 3 semanas |
| 02 | Backend — Foundations & Shared Kernel | [`02_EPIC_BACKEND_FOUNDATIONS.md`](./02_EPIC_BACKEND_FOUNDATIONS.md) | 22 | 4 semanas |
| 03 | Backend — Módulos CORE (bounded contexts) | [`03_EPIC_BACKEND_MODULES.md`](./03_EPIC_BACKEND_MODULES.md) | 35 | 8 semanas |
| 04 | Motor de Proyecciones (Python microservicio) | [`04_EPIC_PROJECTION_ENGINE.md`](./04_EPIC_PROJECTION_ENGINE.md) | 14 | 4 semanas |
| 05 | Integración externa (scrapers, clima) | [`05_EPIC_INTEGRATIONS.md`](./05_EPIC_INTEGRATIONS.md) | 12 | 3 semanas |
| 06 | Mobile App (Flutter offline-first) | [`06_EPIC_MOBILE.md`](./06_EPIC_MOBILE.md) | 28 | 8 semanas |
| 07 | Dashboard Web (GIS) | [`07_EPIC_WEB_DASHBOARD.md`](./07_EPIC_WEB_DASHBOARD.md) | 16 | 4 semanas |
| | **Total** | | **145** | **~34 semanas** (paralelizable ~16-20 con 2 streams) |

---

## Orden de ejecución recomendado (critical path)

```
Semana 1-2:   EPIC 01 tareas 1-8     (repo, docker, k8s dev, postgres)
                            └──────→ EPIC 02 tareas 1-5   (quarkus skeleton, multi-tenancy)

Semana 3-4:   EPIC 01 tareas 9-18    (kafka, observability, CI/CD)
              EPIC 02 tareas 6-15    (auth, eventing, shared libs)
              EPIC 06 tareas 1-4     (flutter skeleton — paralelo)

Semana 5-8:   EPIC 03 tareas 1-15    (Farmer Portal + Planting Reports)
              EPIC 06 tareas 5-15    (mobile offline + reports flow)
              EPIC 05 tareas 1-6     (scrapers MAGA + NOAA)

Semana 9-12:  EPIC 03 tareas 16-25   (Prices + Alerts + GIS read model)
              EPIC 04 tareas 1-10    (projection engine baseline)
              EPIC 07 tareas 1-10    (web dashboard skeleton + mapa)

Semana 13-16: EPIC 03 tareas 26-35   (Buyers + Incentives + integración)
              EPIC 04 tareas 11-14   (projection event consumer + API)
              EPIC 06 tareas 16-28   (mobile SMS fallback + sync robusto)
              EPIC 07 tareas 11-16   (dashboard drill-down)

Semana 17-18: Estabilización, pentest, load tests, onboarding pilotos
```

---

## Convenciones de código (aplican a todas las épicas)

### Repositorio
- Monorepo único: `agromis-platform/`
- Gestor de builds: **Gradle 8.x** con version catalogs
- Java: **21 LTS** con records, sealed classes, pattern matching
- Flutter: **3.19+** con Dart 3

### Estructura de paquetes Java
```
gt.agromis.<context>.<layer>
  donde context ∈ {farmers, reports, projections, prices, alerts, buyers, incentives, gis, integration, shared}
  donde layer ∈ {domain, application, infrastructure, interfaces}
```

### Convenciones DDD aplicadas
- `domain/` — entidades, value objects, domain services, domain events (sin dependencias framework)
- `application/` — use cases (Command/Query handlers estilo mediator), saga orchestrators
- `infrastructure/` — repos JDBC/JPA, Kafka producers, clientes HTTP externos
- `interfaces/` — REST resources (JAX-RS), Kafka listeners, CLI

### Commit style
```
[TASK-042] feat(reports): add PlantingReport aggregate with invariants

Body opcional con contexto.
```

### Branch naming
`feature/TASK-042-planting-report-aggregate`

### Versionado API
URL-based: `/v1/...` — breaking changes requieren `/v2/...` con deprecation period mínimo 6 meses.

### Identificadores
- **UUID v7** en todos los aggregates (ordenamiento temporal natural para particionamiento e índices).
- Generación: server-side por default; client-side en móvil para idempotencia offline.

---

## Prioridades por fase del MVP

### Fase 0 — Fundaciones (semanas 1-4) · BLOQUEANTE
Sin esto no se puede construir nada. Tareas marcadas **[P0]** en cada épica.

### Fase 1 — Ciclo mínimo funcional (semanas 5-10) · CORE
Reporte de siembra + sync offline + proyección básica + alerta push. Tareas **[P1]**.

### Fase 2 — Completitud MVP (semanas 11-16) · SHIP
Precios, compradores, incentivos, dashboard gubernamental, SMS fallback. Tareas **[P2]**.

### Fase 3 — Estabilización (semanas 17-18) · READY
Load tests, security audit, documentación operacional, onboarding asistido. Tareas **[P3]**.

---

## Tareas transversales (no pertenecen a una épica única)

Estas tareas se ejecutan durante todo el proyecto y se mencionan en cada épica cuando aplican:

- **[TRANS-01] Actualizar ADRs** cuando una tarea toma una decisión no documentada
- **[TRANS-02] Actualizar OpenAPI** en cada tarea que expone endpoints nuevos
- **[TRANS-03] Actualizar Schema Registry** en cada tarea que publica eventos nuevos
- **[TRANS-04] Agregar test de integración** con Testcontainers para todo lo que toca DB/Kafka
- **[TRANS-05] Agregar dashboard Grafana** cuando un servicio nuevo expone métricas

---

## Glosario de etiquetas usadas en tareas

| Etiqueta | Significado |
|----------|-------------|
| `[P0]` | Bloqueante — debe hacerse primero |
| `[P1]` | Core MVP — ciclo funcional mínimo |
| `[P2]` | Completitud MVP |
| `[P3]` | Estabilización |
| `[INFRA]` | Toca infraestructura (Docker, K8s, cloud) |
| `[DOMAIN]` | Lógica de dominio pura |
| `[API]` | Expone endpoint HTTP |
| `[EVENT]` | Publica o consume eventos Kafka |
| `[DB]` | Incluye migración de DB |
| `[UI]` | Frontend (mobile o web) |
| `[TEST]` | Tarea principalmente de testing |
| `[DOC]` | Documentación |
| `[SEC]` | Tocar seguridad/auth |
| `[OBS]` | Observabilidad |

---

## Siguiente paso

Abrir [`01_EPIC_INFRA.md`](./01_EPIC_INFRA.md) y comenzar con `TASK-001`.

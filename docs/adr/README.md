# Architecture Decision Records (ADRs)

Este directorio contiene las decisiones de arquitectura del proyecto AgroMIS en formato MADR.

## Como crear un nuevo ADR

1. Copia la plantilla: `cp 000-use-madr.md NNN-titulo-kebab.md`
2. Llena todas las secciones
3. Abre un PR con el ADR como unico cambio
4. Requiere aprobacion de al menos un arquitecto (`@agromis-team/architects`)

## Estados

| Estado | Descripcion |
|--------|-------------|
| `Proposed` | Propuesto, en discusion |
| `Accepted` | Aceptado y vigente |
| `Deprecated` | Obsoleto pero no reemplazado |
| `Superseded` | Reemplazado por otro ADR (indicar cual) |

## Indice

| # | Titulo | Estado | Fecha |
|---|--------|--------|-------|
| [000](000-use-madr.md) | Usar MADR como formato de ADR | Accepted | 2026-04-20 |
| [001](001-modular-monolith.md) | Monolito Modular como arquitectura de backend | Accepted | 2026-04-20 |
| [002](002-postgres-as-backbone.md) | PostgreSQL como base de datos principal | Accepted | 2026-04-20 |
| [003](003-shared-db-multi-tenant-rls.md) | Base de datos compartida con aislamiento via RLS | Accepted | 2026-04-20 |
| [004](004-h3-geospatial-indexing.md) | H3 de Uber como indice geoespacial jerarquico | Accepted | 2026-04-20 |
| [005](005-jsonb-event-store.md) | JSONB como almacenamiento de eventos de dominio | Accepted | 2026-04-20 |
| [006](006-no-satellite-in-mvp.md) | Excluir integracion satelital del MVP | Accepted | 2026-04-20 |
| [007](007-sms-ussd-first-class.md) | SMS y USSD como canales de primera clase | Accepted | 2026-04-20 |
| [008](008-prophet-baseline-ml.md) | Prophet como modelo ML baseline para proyecciones | Accepted | 2026-04-20 |
| [009](009-flutter-for-mobile.md) | Flutter para la aplicacion movil | Accepted | 2026-04-20 |
| [010](010-redpanda-over-kafka.md) | Redpanda en lugar de Apache Kafka | Accepted | 2026-04-20 |
| [011](011-maplibre-over-mapbox.md) | MapLibre GL en lugar de Mapbox GL | Accepted | 2026-04-20 |
| [012](012-k-anonymity-default.md) | k-Anonimato por defecto en datos de agricultores | Accepted | 2026-04-20 |

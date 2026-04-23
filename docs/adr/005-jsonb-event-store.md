# ADR-005: JSONB como almacenamiento de eventos de dominio

## Status
Accepted (2026-04-20)

## Context
AgroMIS necesita un event store para guardar los eventos de dominio (PlantingReportSubmitted, PriceObserved, etc.) de forma persistente antes de publicarlos a Redpanda. Esto garantiza durabilidad y permite replay. Necesitamos simplicidad en el MVP sin introducir un event store dedicado (EventStoreDB, Axon).

## Decision
Usar una tabla PostgreSQL `domain_events` con columna `payload JSONB` para almacenar el envelope de cada evento. La estructura es:

```sql
CREATE TABLE domain_events (
    id          UUID PRIMARY KEY DEFAULT uuid_generate_v7(),
    stream_id   VARCHAR(200) NOT NULL,
    event_type  VARCHAR(200) NOT NULL,
    payload     JSONB NOT NULL,
    metadata    JSONB NOT NULL DEFAULT '{}',
    occurred_at TIMESTAMPTZ NOT NULL DEFAULT now(),
    published   BOOLEAN NOT NULL DEFAULT false
);
```

Un outbox worker lee `published = false` y publica a Redpanda (Transactional Outbox Pattern).

## Consequences
- Positivas: Cero dependencias adicionales en MVP; ACID garantizado; JSONB indexable; replay posible.
- Negativas: No es un event store de verdad (sin snapshots nativos, sin projections engine integrado).
- Neutrales: Se puede migrar a EventStoreDB en el futuro si el volumen lo requiere.

## Alternatives considered
- EventStoreDB: excelente pero agrega operaciones de otro motor.
- Kafka log como source of truth: durabilidad limitada por retention; no es un store transaccional.
- Tabla relacional tipada: schema rigido, costoso de evolucionar.

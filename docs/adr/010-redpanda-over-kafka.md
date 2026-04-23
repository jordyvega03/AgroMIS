# ADR-010: Redpanda en lugar de Apache Kafka

## Status
Accepted (2026-04-20)

## Context
AgroMIS necesita un bus de eventos para la arquitectura event-driven. Apache Kafka es el estandar de la industria pero requiere ZooKeeper (o KRaft en versiones recientes) y tiene overhead operativo considerable. El equipo es pequeno y necesita simplicidad operativa sin sacrificar compatibilidad con el ecosistema Kafka.

## Decision
Usar **Redpanda** en lugar de Apache Kafka. Redpanda es un broker Kafka-compatible escrito en C++ (sin JVM, sin ZooKeeper). Expone la misma API de Kafka (productores, consumidores, Schema Registry, `rpk` como reemplazo de `kafka-cli`).

## Consequences
- Positivas: Sin ZooKeeper; menor footprint de memoria (~10x menos que Kafka); misma API; latencia de cola de microsegundos vs milisegundos; un solo binario para dev y prod.
- Negativas: Ecosistema algo mas joven que Kafka; Kafka Streams no compatible (usamos Quarkus Messaging en su lugar); soporte enterprise comercial si se necesita.
- Neutrales: Redpanda Cloud disponible como alternativa managed si el equipo SRE es muy pequeno.

## Alternatives considered
- Apache Kafka: operacionalmente complejo, requiere JVM, ZooKeeper/KRaft.
- AWS MSK: managed pero vendor lock-in y costo alto en staging.
- RabbitMQ: no es un log distribuido; semanticas diferentes (no replay de eventos).
- NATS JetStream: menor ecosistema; no compatible con clientes Kafka existentes.

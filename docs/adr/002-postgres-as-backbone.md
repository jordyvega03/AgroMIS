# ADR-002: PostgreSQL como base de datos principal

## Status
Accepted (2026-04-20)

## Context
AgroMIS maneja datos geoespaciales (parcelas, zonas), series de tiempo (precios, clima, cosechas), JSON semiestructurado (eventos, metadata) y relaciones tradicionales (usuarios, cultivos, tenants). Necesitamos una DB que cubra todos esos casos sin introducir multiples tecnologias de almacenamiento en el MVP.

## Decision
Usar **PostgreSQL 16** como backbone de datos, extendido con:
- **PostGIS** para geometrias geoespaciales (parcelas, zonas agroclimáticas).
- **TimescaleDB** para hypertables de series de tiempo (precios, observaciones meteorologicas).
- **JSONB** para almacenar eventos de dominio y metadata flexible.
- **pgcrypto / uuid-ossp** para UUIDs y criptografia.

## Consequences
- Positivas: Un solo motor que dominar, ACID completo, ecosistema maduro, Flyway para migraciones, soporte nativo en AWS RDS.
- Negativas: TimescaleDB + PostGIS pueden generar queries complejos; requiere expertise especifico del equipo.
- Neutrales: Si las series de tiempo superan los limites de Timescale, se puede migrar a QuestDB o InfluxDB sin cambiar la DB relacional principal.

## Alternatives considered
- MongoDB: no soporta geoespacial avanzado ni series de tiempo nativamente; transacciones limitadas.
- MySQL: PostGIS soporte inferior; TimescaleDB no disponible.
- DynamoDB: sin JOINs, sin geoespacial nativo, costo impredecible.

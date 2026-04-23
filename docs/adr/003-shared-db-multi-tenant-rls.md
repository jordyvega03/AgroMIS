# ADR-003: Base de datos compartida con aislamiento via Row-Level Security

## Status
Accepted (2026-04-20)

## Context
AgroMIS opera en multiples paises de Centroamerica (Guatemala, El Salvador, Honduras, Nicaragua, Costa Rica, Panama, Belize). Cada pais es un tenant logico. Necesitamos aislamiento de datos entre tenants con el menor overhead operativo posible.

## Decision
Usar un unico schema PostgreSQL con **Row-Level Security (RLS)** para aislar datos por tenant (`country_code`). La aplicacion establece `SET app.current_country = 'GT'` al inicio de cada request, y las politicas RLS filtran automaticamente todas las queries. No hay schemas separados por tenant ni bases de datos separadas.

## Consequences
- Positivas: Un solo esquema, migraciones unicas, queries cross-tenant posibles para reportes globales, menor costo de infraestructura en MVP.
- Negativas: Un bug en las politicas RLS puede exponer datos entre tenants; requiere tests exhaustivos de aislamiento.
- Neutrales: Si un pais requiere soberania de datos (datos en su propio territorio), se puede migrar ese tenant a una instancia separada.

## Alternatives considered
- Schema por tenant: N esquemas = N conjuntos de migraciones; complejo de operar.
- Base de datos por tenant: costo alto, overhead operativo por cada nuevo pais.
- Application-level filtering: propenso a errores humanos (olvidar el WHERE country_code = ?).

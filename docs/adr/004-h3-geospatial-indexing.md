# ADR-004: H3 de Uber como indice geoespacial jerarquico

## Status
Accepted (2026-04-20)

## Context
Los reportes de siembra y las proyecciones necesitan agregacion geoespacial en multiples niveles de zoom (parcela → municipio → departamento → pais). PostGIS permite consultas geoespaciales exactas pero el rendimiento de agregacion a gran escala es costoso. Necesitamos un indice que permita agregaciones eficientes a diferentes resoluciones.

## Decision
Usar **H3** (sistema de indexado hexagonal de Uber) para almacenar la celda H3 de cada parcela en la columna `h3_cell_r7` (resolucion 7, ~5 km²). Las agregaciones se calculan usando la jerarquia H3 (r7 → r6 → r5 → r4) sin necesidad de JOINs geoespaciales costosos. La extension `h3-pg` permite operaciones H3 directamente en PostgreSQL.

## Consequences
- Positivas: Agregaciones geoespaciales O(1) por nivel de zoom; compatible con MapLibre para renderizado de heatmaps.
- Negativas: Las celdas hexagonales no coinciden exactamente con fronteras politicas; requiere la extension `h3-pg` que puede no estar disponible en RDS managed.
- Neutrales: Para consultas exactas de interseccion (ej. "todas las parcelas dentro de este poligono") se sigue usando PostGIS.

## Alternatives considered
- Solo PostGIS con ST_Within: correcto pero lento para millones de parcelas.
- S2 de Google: menos adopcion en el ecosistema PostgreSQL.
- Geohash: rectangular, menos uniforme que hexagonal para vecindad.

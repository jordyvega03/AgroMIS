# ADR-011: MapLibre GL en lugar de Mapbox GL

## Status
Accepted (2026-04-20)

## Context
AgroMIS necesita visualizacion de mapas interactivos en la web y en mobile para mostrar parcelas, zonas agroclimáticas y heatmaps de produccion. Mapbox GL JS fue el estandar pero cambio a licencia propietaria en v2, requiriendo un token de acceso y pago por uso.

## Decision
Usar **MapLibre GL JS** (fork open-source de Mapbox GL JS v1) para el frontend web y **flutter_map** con tiles compatibles para mobile. Los tiles se sirven desde:
- **Desarrollo**: servidor de tiles local (tileserver-gl).
- **Produccion**: OpenStreetMap Tile CDN o tiles propios en S3 + Lambda si el volumen lo justifica.

Para datos agricolas propios (parcelas, zonas) se usa **GeoJSON sobre la API backend** sin servicio de tiles dedicado en el MVP.

## Consequences
- Positivas: Sin costo por tiles en desarrollo; sin vendor lock-in; compatible con el ecosistema Mapbox (mismo SDK API); licencia BSD.
- Negativas: Tiles de OSM no tienen la calidad visual de Mapbox Satellite; para imagenes satelitales se necesitara tiles propios o pago.
- Neutrales: Se puede cambiar el proveedor de tiles en produccion sin cambiar el codigo de la app.

## Alternatives considered
- Mapbox GL JS v2+: costo por MAU; token requerido en cada deploy; licencia propietaria.
- Leaflet.js: menor rendimiento para visualizaciones vectoriales complejas; no WebGL.
- Google Maps: costo alto a escala; vendor lock-in; menor flexibilidad de estilos.

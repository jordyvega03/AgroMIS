# Load Tests con k6

## Requisitos

- [k6](https://k6.io/docs/get-started/installation/) instalado localmente
- Variables de entorno configuradas

## Variables de entorno

| Variable | Descripcion | Default |
|----------|-------------|---------|
| `API_URL` | URL base del API | `https://api-staging.agromis.io` |
| `KEYCLOAK_URL` | URL de Keycloak | `https://keycloak.agromis.io` |
| `KC_REALM` | Realm de Keycloak | `agromis` |
| `TEST_USERNAME` | Usuario de carga | `load-test-user` |
| `TEST_PASSWORD` | Password del usuario | `load-test-pass` |

## Ejecutar localmente

```bash
# Reporte de siembra (target: p95 < 500ms a 100 RPS)
k6 run tests/load/scenarios/submit-report.js

# Consulta de proyecciones
k6 run tests/load/scenarios/get-projection.js

# Tiles GIS
k6 run tests/load/scenarios/query-gis-tiles.js
```

## Targets de rendimiento

| Endpoint | RPS | p95 target | p99 target |
|----------|-----|-----------|-----------|
| POST /farming/reports | 100 | < 500ms | < 1s |
| GET /projections | 200 | < 800ms | < 2s |
| GET /gis/tiles | 100 | < 1000ms | < 2s |

## Ejecucion automatica

Los tests corren cada lunes a las 6am UTC via `.github/workflows/load-test-weekly.yml`.
Los resultados se suben como artefactos del workflow de GitHub Actions.

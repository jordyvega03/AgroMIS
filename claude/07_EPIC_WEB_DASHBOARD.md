# Épica 07 — Dashboard Web (GIS, React + MapLibre)

> **Objetivo**: dashboard web para 3 audiencias: **gobierno/MAGA** (visión macro, alertas de sobreoferta/escasez), **compradores** (ver demanda vs disponibilidad agregada), **extensionistas** (gestionar roster de farmers + validaciones). El dashboard es la interfaz donde el valor del sistema se hace visible.
> **Duración estimada**: 4 semanas
> **Tareas**: 16

---

## Contexto general de la épica

### Stack
- **React 18** + **TypeScript 5**
- **Vite** como bundler
- **TanStack Query** (React Query v5) para estado server
- **Zustand** para estado client limitado
- **MapLibre GL JS** para mapas
- **Recharts** para visualizaciones no-geo
- **Tailwind CSS** + **shadcn/ui** (Radix) para componentes
- **React Hook Form** + **Zod** para forms
- **date-fns** para dates (tree-shakeable)
- **oidc-client-ts** para PKCE con Keycloak

### Estructura
```
web/
├── package.json
├── vite.config.ts
├── tsconfig.json
├── tailwind.config.ts
├── index.html
├── public/
├── src/
│   ├── main.tsx
│   ├── App.tsx
│   ├── router.tsx
│   ├── api/
│   │   ├── client.ts
│   │   ├── hooks/          # TanStack Query wrappers
│   │   └── types/          # tipos generados desde OpenAPI
│   ├── features/
│   │   ├── auth/
│   │   ├── dashboard/       # gov dashboard
│   │   ├── gis-map/
│   │   ├── buyers/
│   │   ├── extensionist/
│   │   ├── admin/
│   │   └── reports/
│   ├── components/          # shared UI
│   ├── hooks/               # shared hooks
│   ├── stores/              # zustand stores
│   ├── lib/
│   │   ├── auth/
│   │   ├── formatting/
│   │   └── geo/
│   └── i18n/
└── tests/
```

### Audiencias y rutas
| Ruta | Audiencia | Role |
|------|-----------|------|
| `/gov` | MAGA, ministerios | GOV_VIEWER |
| `/buyers` | Compradores | BUYER |
| `/field` | Extensionistas | FARMER_EXTENSIONIST |
| `/admin` | Admins | SYSTEM_ADMIN |

---

## TASK-701 · Scaffold Vite + React + TypeScript · [P0] [UI]

### Contexto
Proyecto web inicializado con toolchain moderna.

### Objetivo
Proyecto Vite funcional con TypeScript strict + Tailwind + shadcn/ui + linting.

### Archivos a crear
- `web/package.json`
- `web/vite.config.ts`
- `web/tsconfig.json` (strict, noUncheckedIndexedAccess)
- `web/tailwind.config.ts`
- `web/postcss.config.js`
- `web/.eslintrc.cjs`, `.prettierrc`
- `web/src/main.tsx`, `App.tsx`
- `web/src/index.css` (Tailwind base)
- `web/README.md`

### Deps base
```json
{
  "dependencies": {
    "react": "^18.3.1",
    "react-dom": "^18.3.1",
    "react-router-dom": "^6.22.3",
    "@tanstack/react-query": "^5.28.0",
    "@tanstack/react-query-devtools": "^5.28.0",
    "zustand": "^4.5.2",
    "maplibre-gl": "^4.1.3",
    "react-map-gl": "^7.1.7",
    "recharts": "^2.12.4",
    "zod": "^3.22.4",
    "react-hook-form": "^7.51.0",
    "@hookform/resolvers": "^3.3.4",
    "date-fns": "^3.6.0",
    "oidc-client-ts": "^3.0.1",
    "react-oidc-context": "^3.1.0",
    "axios": "^1.6.8",
    "clsx": "^2.1.0",
    "tailwind-merge": "^2.2.2"
  },
  "devDependencies": {
    "@types/react": "^18.2.69",
    "@types/react-dom": "^18.2.22",
    "@vitejs/plugin-react": "^4.2.1",
    "typescript": "^5.4.3",
    "vite": "^5.2.6",
    "tailwindcss": "^3.4.3",
    "eslint": "^8.57.0",
    "prettier": "^3.2.5",
    "vitest": "^1.4.0",
    "@testing-library/react": "^14.2.2",
    "msw": "^2.2.10"
  }
}
```

### Criterios de aceptación
- [ ] `pnpm dev` arranca en `http://localhost:5173`
- [ ] Hot reload funciona
- [ ] TypeScript strict sin errores
- [ ] ESLint + Prettier configurados y pre-commit hook
- [ ] Layout base con Tailwind funcional

### Dependencias
EPIC01: 001

---

## TASK-702 · OpenAPI typed client generation · [P0] [UI] [API]

### Contexto
Tipos TypeScript generados desde OpenAPI del backend. Evita drift manual.

### Objetivo
Script que genera tipos + client a partir del `openapi.json` expuesto por backend.

### Archivos a crear
- `web/scripts/generate-api-types.ts`
- `web/src/api/generated/` (output, gitignored)
- `web/package.json` script `generate:api`

### Herramienta
- `openapi-typescript` para tipos
- `orval` o `openapi-fetch` para client (elegir uno)

### Criterios de aceptación
- [ ] `pnpm generate:api` baja el openapi.json y genera tipos
- [ ] Tipos usables en: `const report: components["schemas"]["PlantingReport"]`
- [ ] Client hooks TanStack Query tipados

### Dependencias
TASK-701, backend con OpenAPI (EPIC02: 221)

---

## TASK-703 · Auth: OIDC PKCE flow + route guards · [P0] [SEC] [UI]

### Contexto
Login vía Keycloak + protección de rutas por rol.

### Objetivo
Login funcional + role-based guards.

### Archivos a crear
- `web/src/lib/auth/oidc.ts`                     # config
- `web/src/lib/auth/AuthProvider.tsx`
- `web/src/lib/auth/useAuth.ts`
- `web/src/lib/auth/ProtectedRoute.tsx`
- `web/src/lib/auth/RoleGuard.tsx`
- `web/src/features/auth/login_page.tsx`

### Flow
1. User hits `/gov` sin auth → redirect `/login`
2. `/login` inicia PKCE contra Keycloak
3. Callback `/auth/callback` recibe code, exchange por token
4. Token en memoria (NO localStorage — XSS risk). Refresh token en httpOnly cookie si BFF disponible; en MVP directo en memoria.
5. Silent renew vía iframe cuando expira
6. Logout → Keycloak logout endpoint

### Criterios de aceptación
- [ ] Login completa y llega a dashboard correspondiente por rol
- [ ] Protected route bloquea sin auth
- [ ] Role guard bloquea si rol no matchea
- [ ] Silent renew funciona (token nuevo antes de expirar)
- [ ] Logout limpia state y redirige

### Dependencias
TASK-701, EPIC02: 207

---

## TASK-704 · App shell + navegación por rol · [P0] [UI]

### Contexto
Layout consistente con sidebar + header. Menú dinámico según roles del usuario.

### Objetivo
Shell con nav funcional.

### Archivos a crear
- `web/src/components/layout/AppShell.tsx`
- `web/src/components/layout/Sidebar.tsx`
- `web/src/components/layout/Header.tsx`
- `web/src/components/layout/CountrySelector.tsx`
- `web/src/router.tsx`

### Nav items por role
| Role | Items |
|------|-------|
| GOV_VIEWER | Dashboard, Mapa, Alertas Nacionales, Reportes, Estadísticas |
| BUYER | Dashboard, Demandas, Matches, Mapa |
| FARMER_EXTENSIONIST | Mis Farmers, Validaciones, Reportes por aprobar, Mapa |
| SYSTEM_ADMIN | Usuarios, Integraciones, Configuración, Auditoría |

### Criterios de aceptación
- [ ] Sidebar muestra items según roles del token
- [ ] User con múltiples roles ve intersección útil (ej extensionista admin → ambas secciones)
- [ ] Country selector solo si usuario tiene acceso a múltiples (`country_codes` claim)
- [ ] Responsive: sidebar colapsa en mobile (aunque target es desktop)

### Dependencias
TASK-703

---

## TASK-705 · API client + TanStack Query setup · [P0] [UI]

### Contexto
Wrapper sobre axios con auth + error handling + TanStack Query provider.

### Objetivo
Query hooks reutilizables tipados.

### Archivos a crear
- `web/src/api/client.ts`
- `web/src/api/queryClient.ts`
- `web/src/api/hooks/useProjections.ts`
- `web/src/api/hooks/usePrices.ts`
- `web/src/api/hooks/useAlerts.ts`
- `web/src/api/hooks/useFarmers.ts`
- `web/src/api/hooks/useReports.ts`
- `web/src/api/errors.ts`

### Especificaciones
- Query key factory pattern: `queryKeys.projections.list({ country, crop })`
- Global error handler: 401 → trigger auth refresh; 403 → show forbidden; 5xx → toast error
- Stale time defaults: 30s para listas, 5min para catalogs
- Devtools en dev

### Criterios de aceptación
- [ ] Mock server con MSW para tests
- [ ] Tests con render hook + provider wrapper
- [ ] Typed response: `const { data } = useProjections({country: "GT"})` infiere tipo

### Dependencias
TASK-702, TASK-703

---

## TASK-706 · Mapa base MapLibre + tiles MVT · [P1] [UI]

### Contexto
Componente de mapa es central. Reutilizable en múltiples vistas.

### Objetivo
`<MapView>` componente con base tiles + capa MVT del backend.

### Archivos a crear
- `web/src/components/map/MapView.tsx`
- `web/src/components/map/layers/ProjectionLayer.tsx`
- `web/src/components/map/layers/CorridorsLayer.tsx`
- `web/src/components/map/controls/LayerSwitcher.tsx`
- `web/src/components/map/controls/LegendPanel.tsx`
- `web/src/components/map/styles/base-style.ts`

### Base style
- OpenStreetMap raster tiles (free, sin key) para MVP
- Upgrade a vector tiles self-hosted en Fase 2

### Layer MVT
```typescript
map.addSource('projections', {
  type: 'vector',
  tiles: [`${API_BASE}/v1/gis/tiles/{z}/{x}/{y}.mvt?layer=projections&crop=${crop}&horizon=${horizon}`],
  minzoom: 5,
  maxzoom: 10
});

map.addLayer({
  id: 'projections-fill',
  type: 'fill',
  source: 'projections',
  'source-layer': 'projection_layer',
  paint: {
    'fill-color': [
      'interpolate', ['linear'], ['get', 'surplus_probability'],
      0, '#22c55e',    // verde = OK
      0.5, '#f59e0b',  // ámbar
      1.0, '#ef4444'   // rojo = sobreoferta
    ],
    'fill-opacity': 0.7
  }
});
```

### Criterios de aceptación
- [ ] Mapa carga centrado en país del usuario
- [ ] Tile layer se actualiza al cambiar filtros
- [ ] Click en celda H3 → popup con: crop, probabilidad, n_reports (agregados)
- [ ] Tile caching respeta HTTP Cache-Control
- [ ] Legend dinámica muestra escala de colores + significado

### Dependencias
TASK-705, EPIC03: 318

---

## TASK-707 · Gov dashboard: vista macro · [P1] [UI]

### Contexto
Vista para MAGA/gobierno. Alertas nacionales, KPIs, mapa grande.

### Objetivo
Dashboard funcional con datos reales.

### Archivos a crear
- `web/src/features/dashboard/GovDashboardPage.tsx`
- `web/src/features/dashboard/widgets/NationalKpiCards.tsx`
- `web/src/features/dashboard/widgets/ActiveAlertsPanel.tsx`
- `web/src/features/dashboard/widgets/CropStatusTable.tsx`
- `web/src/features/dashboard/widgets/TopCorridorsChart.tsx`

### Widgets
- **KPIs**: reports último mes, farmers activos, alertas HIGH abiertas, accuracy proyecciones
- **Alerts panel**: lista alertas HIGH activas con drill-down
- **Crop status table**: por cultivo, estado (OK / WARNING / SURPLUS / SHORTAGE), corridors afectados, acciones recomendadas
- **Map** (embebido, TASK-706): heatmap país completo
- **Chart corridors top 10**: barras horizontales con volumen proyectado

### Criterios de aceptación
- [ ] Datos reales desde backend
- [ ] Filtros por crop, horizonte, corridor
- [ ] Export a CSV de table
- [ ] Loading skeletons

### Dependencias
TASK-706

---

## TASK-708 · GIS exploración page · [P1] [UI]

### Contexto
Página fullscreen con mapa + filtros para exploración interactiva.

### Objetivo
Mapa + panel filtros lateral.

### Archivos a crear
- `web/src/features/gis-map/GisMapPage.tsx`
- `web/src/features/gis-map/widgets/FiltersPanel.tsx`
- `web/src/features/gis-map/widgets/TimelineSlider.tsx`
- `web/src/features/gis-map/widgets/CellDetailDrawer.tsx`

### Filtros
- Crop (multi-select)
- Horizonte (30/60/90 días)
- Resolución H3 (res 5/7/9) — auto según zoom
- Metric (surplus_prob / shortage_prob / n_reports / area_ha)
- Fecha as_of (slider timeline)

### Timeline
- Permite "scrubbing" en el tiempo — ver cómo cambió la proyección semana a semana

### Criterios de aceptación
- [ ] URL sincronizada con filtros (shareable links)
- [ ] Timeline anima transiciones entre fechas
- [ ] Cell click → drawer con detail + historial
- [ ] Performance: mapa fluido a 30fps mínimo

### Dependencias
TASK-706

---

## TASK-709 · Buyer panel: demandas + matches · [P2] [UI]

### Contexto
Comprador registra demandas y ve matches con agregados H3.

### Objetivo
CRUD demandas + página matches.

### Archivos a crear
- `web/src/features/buyers/BuyerDashboardPage.tsx`
- `web/src/features/buyers/DemandsListPage.tsx`
- `web/src/features/buyers/DemandFormPage.tsx`
- `web/src/features/buyers/MatchesPage.tsx`

### Formulario demand
- Crop
- Volume MT
- Ventana fechas
- Corridors target (multi-select con mapa)
- Price range opcional
- Grade requirements

### Matches view
- Tabla: corridor, volumen estimado disponible, timing fit, distance, ranking
- Mapa embebido con highlight matches
- Opción "contactar" (stub MVP — genera email draft)

### Criterios de aceptación
- [ ] Solo muestra agregados (≥ k farmers)
- [ ] Nunca expone datos individuales
- [ ] Export matches a CSV
- [ ] Sort/filter functional

### Dependencias
TASK-706, EPIC03: 322-324

---

## TASK-710 · Extensionist: roster + validations queue · [P1] [UI]

### Contexto
Extensionista gestiona su roster de farmers asignados + cola de validaciones pendientes.

### Objetivo
Dashboard extensionista funcional.

### Archivos a crear
- `web/src/features/extensionist/ExtensionistDashboardPage.tsx`
- `web/src/features/extensionist/FarmersRosterPage.tsx`
- `web/src/features/extensionist/FarmerDetailPage.tsx`
- `web/src/features/extensionist/ValidationQueuePage.tsx`
- `web/src/features/extensionist/ValidationDetailPage.tsx`

### UX validation queue
- Lista reports status `SUBMITTED` en su jurisdicción
- Ordenable por priority (older first, by crop, by area)
- Click report → detail con:
  - Data del reporte
  - Foto(s) si el farmer adjuntó
  - Ubicación en mapa
  - Historial farmer (reputación, reports anteriores)
  - Botones: Confirmar / Rechazar / Revisar en campo
- Al validar → evento disparado + UI refresh

### Criterios de aceptación
- [ ] Solo reports en jurisdicción del extensionista
- [ ] Bulk actions: validar múltiples a la vez
- [ ] Progress: "25 de 40 validados esta semana"

### Dependencias
TASK-704, EPIC03: 307

---

## TASK-711 · Admin: users + integrations monitoring · [P2] [UI]

### Contexto
Admin panel para gestión usuario-role + ver estado de integraciones.

### Objetivo
Pantallas admin funcionales (CRUD users es delegado a Keycloak UI; aquí solo read + assign roles).

### Archivos a crear
- `web/src/features/admin/UsersListPage.tsx`
- `web/src/features/admin/IntegrationsStatusPage.tsx`
- `web/src/features/admin/AuditLogPage.tsx`

### Integrations status
- Tabla: source, last run, next run, status, records last 24h
- Buttons: trigger manual run, view runs history
- Link a dashboard Grafana

### Audit log
- Tabla paginada con filtros (user, action, date range, resource)
- Export

### Criterios de aceptación
- [ ] Solo accesible con `SYSTEM_ADMIN`
- [ ] Trigger manual con confirmación
- [ ] Audit log search < 500ms p95

### Dependencias
TASK-704, EPIC03: 219, EPIC05: 501

---

## TASK-712 · Charts library + visualizaciones complejas · [P2] [UI]

### Contexto
Varios dashboards necesitan charts no-geo: series de tiempo, distribuciones, comparaciones.

### Objetivo
Componentes de chart reusables sobre Recharts con estilo consistente.

### Archivos a crear
- `web/src/components/charts/TimeSeriesChart.tsx`
- `web/src/components/charts/StackedBarChart.tsx`
- `web/src/components/charts/DistributionHistogram.tsx`
- `web/src/components/charts/GaugeChart.tsx`
- `web/src/components/charts/theme.ts`

### Criterios de aceptación
- [ ] Todos los charts respetan brand colors
- [ ] Tooltips consistentes
- [ ] Responsive
- [ ] Export PNG funcional

### Dependencias
TASK-701

---

## TASK-713 · i18n + number/date formatting · [P2] [UI]

### Contexto
Multi-país implica múltiples locales de formato (fechas, números, monedas).

### Objetivo
i18n completo ES + helpers de formato por país.

### Archivos a crear
- `web/src/i18n/index.ts` (react-i18next)
- `web/src/i18n/locales/es.json`
- `web/src/lib/formatting/number.ts`
- `web/src/lib/formatting/date.ts`
- `web/src/lib/formatting/currency.ts`

### Criterios de aceptación
- [ ] Currency format usa `Intl.NumberFormat` con locale apropiado
- [ ] Date format usa `date-fns` con locale
- [ ] Switcher de idioma funcional (aunque solo ES en MVP)

### Dependencias
TASK-701

---

## TASK-714 · Accessibility + keyboard navigation · [P2] [UI]

### Contexto
WCAG AA baseline. Dashboards gubernamentales requieren compliance.

### Objetivo
Audit y fixes de accesibilidad.

### Tareas
- Tab navigation completa
- ARIA labels en componentes custom
- Contrast ratios verificados
- Screen reader tested en flows principales
- Focus management en modals

### Criterios de aceptación
- [ ] Lighthouse accessibility score ≥ 90 en todas las rutas
- [ ] axe DevTools sin errores críticos
- [ ] Navegación sin mouse funciona en dashboard principal

### Dependencias
Features principales completas

---

## TASK-715 · Performance + bundle optimization · [P2] [UI]

### Contexto
Bundle inicial debe ser pequeño. Lazy load rutas.

### Objetivo
First paint < 2s en 3G.

### Acciones
- Route-based code splitting
- Dynamic imports de MapLibre (solo cuando se usa)
- Tree-shaking de Recharts
- Image optimization
- Cache headers adecuados

### Criterios de aceptación
- [ ] Main chunk < 300KB gzip
- [ ] Lighthouse performance ≥ 85
- [ ] LCP < 2.5s en staging

### Dependencias
TASK-701 + features core

---

## TASK-716 · Deploy web app (S3 + CloudFront) · [P1] [INFRA]

### Contexto
SPA servida desde CDN.

### Objetivo
Deploy automatizado desde CI tras merge a main.

### Archivos a crear
- `infra/terraform/modules/web-hosting/`
- `.github/workflows/deploy-web.yml`
- `web/scripts/deploy.sh`

### Especificaciones
- Bucket S3 con static website hosting
- CloudFront distribution con OAI
- Cache: HTML no-cache, assets con hash immutable
- Custom domain + ACM cert
- 404 → `index.html` para SPA routing

### Criterios de aceptación
- [ ] Deploy staging automático tras merge
- [ ] Deploy prod requiere approval
- [ ] Invalidación CloudFront automática
- [ ] CSP headers configurados
- [ ] Tiempo deploy < 5 min

### Dependencias
TASK-715, EPIC01: 004

---

## Resumen épica 07

| # | Tarea | Prioridad | Deps clave |
|---|-------|-----------|------------|
| 701 | Scaffold Vite | P0 | EPIC01:001 |
| 702 | OpenAPI types | P0 | 701, EPIC02:221 |
| 703 | Auth OIDC | P0 | 701, EPIC02:207 |
| 704 | App shell | P0 | 703 |
| 705 | API client | P0 | 702, 703 |
| 706 | Map + MVT | P1 | 705, EPIC03:318 |
| 707 | Gov dashboard | P1 | 706 |
| 708 | GIS exploration | P1 | 706 |
| 709 | Buyer panel | P2 | 706, EPIC03:322 |
| 710 | Extensionist | P1 | 704, EPIC03:307 |
| 711 | Admin | P2 | 704 |
| 712 | Charts | P2 | 701 |
| 713 | i18n | P2 | 701 |
| 714 | Accessibility | P2 | features |
| 715 | Performance | P2 | features |
| 716 | Deploy | P1 | 715, EPIC01:004 |

**Critical path**: 701 → 702/703 → 704/705 → 706 → 707 (first valuable dashboard).

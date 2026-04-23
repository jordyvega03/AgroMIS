# Épica 06 — Mobile App (Flutter, offline-first)

> **Objetivo**: app móvil Flutter para los agricultores. Offline-first, pensada para dispositivos de gama baja, conectividad intermitente, usuarios con baja alfabetización digital. Canal primario de ingreso de datos al sistema.
> **Duración estimada**: 8 semanas
> **Tareas**: 28

---

## Contexto general de la épica

### Principios no-negociables
1. **Offline-first**: toda acción debe funcionar sin internet; sync es background.
2. **Low-end friendly**: target Android 7.0+ (API 24), 1GB RAM, 8GB storage.
3. **Ligera en datos**: cada payload < 50KB cuando sea posible; Protocol Buffers en sync.
4. **Multilingüe**: ES obligatorio + estructura para kʼicheʼ, kaqchikel, qʼeqchiʼ.
5. **Accesible**: audio instructions, iconos grandes, UI sin jerga.

### Stack
- **Flutter 3.19+** / Dart 3
- **Riverpod** para state management (más simple y testable que Bloc para este caso)
- **drift** (SQLite) para local DB con generación de código
- **dio** para HTTP + interceptors
- **flutter_secure_storage** para tokens
- **flutter_map** + tiles MapLibre para mapas
- **flutter_background_service** para sync worker
- **freezed** + `json_serializable` para modelos inmutables
- **go_router** para navegación declarativa
- **flutter_localizations** + ARB files para i18n

### Estructura de carpetas
```
mobile/
├── pubspec.yaml
├── lib/
│   ├── main.dart
│   ├── app.dart
│   ├── core/
│   │   ├── config/
│   │   ├── network/            # dio clients + interceptors
│   │   ├── storage/            # drift db
│   │   ├── auth/
│   │   ├── sync/
│   │   └── errors/
│   ├── features/
│   │   ├── onboarding/
│   │   ├── auth/
│   │   ├── parcels/
│   │   ├── reports/
│   │   ├── prices/
│   │   ├── alerts/
│   │   ├── projections/
│   │   └── profile/
│   ├── shared/
│   │   ├── widgets/
│   │   ├── theme/
│   │   ├── extensions/
│   │   └── utils/
│   └── l10n/
│       ├── app_es.arb
│       ├── app_quc.arb         # kʼicheʼ
│       └── app_cak.arb
├── assets/
│   ├── audio/                  # instrucciones de voz
│   ├── images/
│   └── icons/
├── test/
├── integration_test/
└── android/ios/                 # platform-specific
```

---

## 6.1 Foundations

---

## TASK-601 · Scaffold Flutter project + clean architecture · [P0] [UI]

### Contexto
Proyecto Flutter con estructura lista para crecer. Clean architecture liviana: feature-based folders, capas (data/domain/presentation) dentro de cada feature.

### Objetivo
Proyecto Flutter que arranca en Android e iOS con splash screen y pantalla "Hola".

### Archivos a crear
- `mobile/pubspec.yaml` con deps base
- `mobile/lib/main.dart`, `app.dart`
- `mobile/lib/core/config/app_config.dart`
- `mobile/lib/shared/theme/app_theme.dart`
- `mobile/lib/features/splash/splash_page.dart`
- `mobile/README.md`

### Deps base (`pubspec.yaml`)
```yaml
dependencies:
  flutter:
    sdk: flutter
  flutter_riverpod: ^2.5.1
  riverpod_annotation: ^2.3.5
  go_router: ^13.2.0
  freezed_annotation: ^2.4.1
  json_annotation: ^4.8.1
  dio: ^5.4.2
  drift: ^2.16.0
  sqlite3_flutter_libs: ^0.5.20
  path_provider: ^2.1.2
  path: ^1.9.0
  flutter_secure_storage: ^9.0.0
  flutter_localizations:
    sdk: flutter
  intl: ^0.19.0
  logger: ^2.2.0
  connectivity_plus: ^5.0.2

dev_dependencies:
  build_runner: ^2.4.9
  riverpod_generator: ^2.4.0
  freezed: ^2.4.7
  json_serializable: ^6.7.1
  drift_dev: ^2.16.0
  flutter_test:
    sdk: flutter
  integration_test:
    sdk: flutter
  mocktail: ^1.0.3
```

### Criterios de aceptación
- [ ] `flutter pub get && flutter run` arranca en Android emulator
- [ ] `flutter analyze` clean
- [ ] Splash screen muestra logo + "AgroMIS" 1.5s → redirige a home
- [ ] Theme base: primary color verde campo, typography Inter
- [ ] `flutter test` pasa

### Dependencias
EPIC01: 001

---

## TASK-602 · Drift (SQLite) local database schema · [P0] [UI]

### Contexto
DB local es el core del offline-first. Esquema refleja los aggregates backend simplificados + tablas de sync.

### Objetivo
DB drift funcional con migraciones + DAOs base.

### Archivos a crear
- `mobile/lib/core/storage/database.dart`
- `mobile/lib/core/storage/tables/farmers.dart`
- `mobile/lib/core/storage/tables/parcels.dart`
- `mobile/lib/core/storage/tables/reports.dart`
- `mobile/lib/core/storage/tables/prices.dart`
- `mobile/lib/core/storage/tables/alerts.dart`
- `mobile/lib/core/storage/tables/pending_mutations.dart`
- `mobile/lib/core/storage/tables/sync_state.dart`
- `mobile/lib/core/storage/daos/` — DAOs por tabla

### Tablas clave

```dart
// pending_mutations — queue de operaciones por sincronizar
class PendingMutations extends Table {
  IntColumn get id => integer().autoIncrement()();
  TextColumn get localId => text()();                 // uuid v7 generado en cliente
  TextColumn get type => text()();                    // SUBMIT_REPORT, UPDATE_PARCEL...
  TextColumn get payloadJson => text()();
  TextColumn get idempotencyKey => text().unique()();
  DateTimeColumn get createdAt => dateTime()();
  DateTimeColumn get lastAttemptAt => dateTime().nullable()();
  IntColumn get attempts => integer().withDefault(const Constant(0))();
  TextColumn get status => text().withDefault(const Constant('PENDING'))();
  TextColumn get lastError => text().nullable()();
}

// sync_state — cursor + metadata sync
class SyncState extends Table {
  TextColumn get key => text()();
  TextColumn get value => text()();
  DateTimeColumn get updatedAt => dateTime()();
  @override Set<Column> get primaryKey => {key};
}

class Reports extends Table {
  TextColumn get id => text()();                      // server_id cuando exista, sino local_id
  TextColumn get localId => text()();
  TextColumn get farmerId => text()();
  TextColumn get parcelId => text()();
  TextColumn get cropCode => text()();
  RealColumn get plantedAreaHa => real()();
  DateTimeColumn get expectedPlantingDate => dateTime()();
  DateTimeColumn get expectedHarvestDate => dateTime()();
  TextColumn get status => text()();
  BoolColumn get synced => boolean().withDefault(const Constant(false))();
  DateTimeColumn get createdAt => dateTime()();
  DateTimeColumn get updatedAt => dateTime()();
  @override Set<Column> get primaryKey => {id};
}
```

### Migraciones
- Estrategia: `onUpgrade` con stepByStep + version bumps
- Versión inicial 1

### Criterios de aceptación
- [ ] `flutter pub run build_runner build` genera código drift
- [ ] Test: insertar farmer + query → OK
- [ ] Test: migración de v1 a v2 simulada sin pérdida
- [ ] Índices en campos de búsqueda (phone, parcel_id, synced flag)

### Dependencias
TASK-601

---

## TASK-603 · Dio HTTP client + interceptors + retry · [P0] [UI]

### Contexto
Todas las llamadas al backend van por un único client con auth, logging, error mapping, retry.

### Objetivo
`ApiClient` singleton con interceptors.

### Archivos a crear
- `mobile/lib/core/network/api_client.dart`
- `mobile/lib/core/network/interceptors/auth_interceptor.dart`
- `mobile/lib/core/network/interceptors/logging_interceptor.dart`
- `mobile/lib/core/network/interceptors/retry_interceptor.dart`
- `mobile/lib/core/network/interceptors/connectivity_interceptor.dart`
- `mobile/lib/core/network/api_exception.dart`
- `mobile/lib/core/network/problem_detail.dart`       # mapear RFC 7807

### Especificaciones
- Base URL inyectado desde `AppConfig` (different per env: dev/staging/prod)
- Auth interceptor añade `Authorization: Bearer <token>` + `X-Country-Code`
- Retry con exponential backoff: 3 intentos con 1s, 3s, 9s (solo en 5xx, timeout, network)
- Logging redacta `Authorization` headers
- Connectivity interceptor: si no hay red → `NoNetworkException` inmediato sin intentar

### Criterios de aceptación
- [ ] `ApiClient.get()` funciona con mock server
- [ ] Token auto-attached
- [ ] 401 dispara refresh flow (TASK-605)
- [ ] `ProblemDetail` parseado y re-throw como `ApiException`
- [ ] Tests con `mocktail` para cada interceptor

### Dependencias
TASK-601

---

## TASK-604 · Config y entornos (dev/staging/prod) · [P0] [UI]

### Contexto
3 flavors Android + configs iOS distintos. Build scripts.

### Objetivo
Build flavors funcionales con distintos `applicationId`, `baseUrl`, `apiKey`.

### Archivos a crear
- `mobile/android/app/build.gradle` (flavors)
- `mobile/ios/Runner/Info-*.plist`
- `mobile/lib/core/config/flavor_config.dart`
- `mobile/lib/main_dev.dart`, `main_staging.dart`, `main_prod.dart`
- `mobile/scripts/build.sh`

### Flavors
| Flavor | baseUrl | applicationId |
|--------|---------|---------------|
| dev | http://10.0.2.2:8080 (localhost from emulator) | gt.agromis.mobile.dev |
| staging | https://api-staging.agromis.gt | gt.agromis.mobile.staging |
| prod | https://api.agromis.gt | gt.agromis.mobile |

### Criterios de aceptación
- [ ] `flutter run --flavor dev -t lib/main_dev.dart` arranca
- [ ] Iconos y splash distintos por flavor (staging tiene badge)

### Dependencias
TASK-601

---

## TASK-605 · Auth flow: login + OIDC PKCE + token refresh · [P0] [SEC] [UI]

### Contexto
Keycloak como IDP. Flujo PKCE con device-native flow. Token seguro en flutter_secure_storage.

### Objetivo
Login funcional + gestión de tokens.

### Archivos a crear
- `mobile/lib/features/auth/auth_repository.dart`
- `mobile/lib/features/auth/auth_controller.dart`
- `mobile/lib/features/auth/login_page.dart`
- `mobile/lib/features/auth/otp_page.dart`           # para verificación phone
- `mobile/lib/core/auth/token_storage.dart`
- `mobile/lib/core/auth/token_refresher.dart`

### Flujo MVP
1. Usuario nuevo: flujo "Registrarse" → ingresa teléfono + país → solicita OTP → verifica OTP → crea cuenta → login automático
2. Usuario existente: "Iniciar sesión" → teléfono → OTP → login
3. Background: token refresh automático cuando expira (access token 15min)

### Criterios de aceptación
- [ ] Login con credenciales válidas → obtiene token y navega a home
- [ ] Token refresh transparente cuando 401 en API call
- [ ] Logout limpia tokens, DB encriptada y redirige a login
- [ ] Token almacenado con `flutter_secure_storage` (Keystore/Keychain)
- [ ] Biometría opcional (fingerprint) para rápido login (si disponible)

### Dependencias
TASK-603, EPIC03: 302 (registro backend)

---

## TASK-606 · i18n base + locales detection · [P1] [UI]

### Contexto
Spanish como default pero arquitectura lista para lenguas mayas.

### Objetivo
i18n funcional con ARB files + switcher en settings.

### Archivos a crear
- `mobile/lib/l10n/app_es.arb`
- `mobile/lib/l10n/app_quc.arb`       # kʼicheʼ (stub)
- `mobile/lib/l10n/app_cak.arb`       # kaqchikel (stub)
- `mobile/l10n.yaml`
- `mobile/lib/features/settings/language_settings_page.dart`

### Criterios de aceptación
- [ ] `l10n.yaml` generado ok con `flutter gen-l10n`
- [ ] Switch en settings cambia idioma en caliente
- [ ] Persiste elección en drift `sync_state`

### Dependencias
TASK-601

---

## TASK-607 · Theme + design system básico · [P1] [UI]

### Contexto
Sistema de diseño consistente. Colores, typography, component library minimal.

### Objetivo
Widgets reusables con style consistente.

### Archivos a crear
- `mobile/lib/shared/theme/app_colors.dart`
- `mobile/lib/shared/theme/app_typography.dart`
- `mobile/lib/shared/theme/app_spacing.dart`
- `mobile/lib/shared/widgets/`
  - `primary_button.dart`
  - `big_icon_button.dart`         # botones grandes para usuarios low-tech
  - `labeled_input.dart`
  - `section_header.dart`
  - `empty_state.dart`
  - `error_state.dart`
  - `loading_state.dart`
  - `offline_banner.dart`
  - `audio_help_button.dart`       # botón "play instruction audio"

### Criterios de aceptación
- [ ] Widget catalog page (dev only) muestra todos los widgets
- [ ] Golden tests para widgets críticos
- [ ] Contrast ratio AA cumplido (test automatizado)

### Dependencias
TASK-601

---

## 6.2 Core features

---

## TASK-608 · Onboarding flow (welcome + language + permissions) · [P1] [UI]

### Contexto
Primera vez que abre la app: bienvenida, selección de idioma, solicitud de permisos (location, notifications), intro breve animada.

### Objetivo
4 pantallas onboarding completadas una vez.

### Archivos a crear
- `mobile/lib/features/onboarding/onboarding_controller.dart`
- `mobile/lib/features/onboarding/pages/welcome_page.dart`
- `mobile/lib/features/onboarding/pages/language_selection_page.dart`
- `mobile/lib/features/onboarding/pages/permissions_page.dart`
- `mobile/lib/features/onboarding/pages/intro_page.dart`

### Criterios de aceptación
- [ ] Se muestra solo una vez (persiste flag `onboarding_completed`)
- [ ] Skip solo si idioma elegido
- [ ] Permissions: explicación clara antes de pedir (rationale dialog)

### Dependencias
TASK-606

---

## TASK-609 · Home page con navegación principal · [P1] [UI]

### Contexto
Hub central. Bottom nav con: Inicio, Mis Parcelas, Precios, Alertas, Perfil.

### Objetivo
Navegación funcional con go_router + shell route.

### Archivos a crear
- `mobile/lib/app_router.dart`
- `mobile/lib/features/home/home_shell.dart`
- `mobile/lib/features/home/home_page.dart`   # dashboard con resumen

### Home dashboard incluye
- Bienvenida con nombre del farmer
- Próxima cosecha esperada
- Precio del día de su cultivo principal
- CTA grande: "Registrar nueva siembra"
- Lista alertas activas (top 3)

### Criterios de aceptación
- [ ] Navegación entre tabs preserva state (IndexedStack)
- [ ] Deep links funcionan: `agromis://alerts/123`
- [ ] Back button en Android respeta navigation stack

### Dependencias
TASK-605

---

## TASK-610 · Parcels: list + create (dibujar polígono) · [P1] [UI]

### Contexto
Usuario debe poder dibujar polígono de su parcela o editarlo. Usa `flutter_map` + tiles MapLibre.

### Objetivo
Pantalla lista parcelas + pantalla para crear/editar con drawing.

### Archivos a crear
- `mobile/lib/features/parcels/parcels_list_page.dart`
- `mobile/lib/features/parcels/parcel_editor_page.dart`
- `mobile/lib/features/parcels/widgets/polygon_drawer.dart`
- `mobile/lib/features/parcels/widgets/area_calculator.dart`
- `mobile/lib/features/parcels/parcels_controller.dart`
- `mobile/lib/features/parcels/parcels_repository.dart`

### UX flujo crear parcela
1. Tap "+ Nueva parcela"
2. Mapa centrado en ubicación GPS actual (con permiso)
3. Instrucción: "Camina el perímetro de tu parcela" — botón "Iniciar trayecto"
4. Usuario puede: (a) caminar con GPS tracking, o (b) tap manual en mapa para poner vertices, o (c) ingresar área numérica sin geometría (fallback)
5. Área calculada en tiempo real
6. Nombre + cultivo predominante opcional
7. Guardar → pending mutation

### Criterios de aceptación
- [ ] Polígono mínimo 3 vértices
- [ ] Área calculada en hectáreas con precisión
- [ ] Walk-mode usa location stream con sampling inteligente (>10m distance)
- [ ] Tiles offline para áreas frecuentes (descarga opcional de zona)
- [ ] Tests widget para `PolygonDrawer`

### Dependencias
TASK-602, TASK-607

---

## TASK-611 · Reports: submit planting report flow · [P1] [UI]

### Contexto
**Flujo más importante de la app**. Debe ser rapidísimo (< 90s end-to-end) y funcionar 100% offline.

### Objetivo
Wizard 4 pasos para reportar siembra.

### Archivos a crear
- `mobile/lib/features/reports/report_wizard_page.dart`
- `mobile/lib/features/reports/steps/step_1_parcel.dart`
- `mobile/lib/features/reports/steps/step_2_crop.dart`
- `mobile/lib/features/reports/steps/step_3_dates.dart`
- `mobile/lib/features/reports/steps/step_4_review.dart`
- `mobile/lib/features/reports/widgets/crop_picker.dart`
- `mobile/lib/features/reports/widgets/area_input.dart`
- `mobile/lib/features/reports/reports_controller.dart`
- `mobile/lib/features/reports/reports_repository.dart`

### UX cada paso
1. **Parcela**: lista visual parcelas (grid con mini-mapa thumbnail). Si solo 1 → skip.
2. **Cultivo + área**: crop picker con iconos + área numérica con slider visual (comparación con parcela total).
3. **Fechas**: "Sembrado hoy / hace X días" shortcut + calendar picker. Fecha cosecha autocompletada con typical cycle del crop + editable.
4. **Revisar**: resumen + audio prompt "Voy a enviar este reporte" + botón "Confirmar".

### Offline behavior
- Cada paso guarda draft en drift (resumible)
- Al confirmar: se inserta en `reports` (synced=false) + `pending_mutations`
- Mensaje: "Reporte guardado. Se enviará cuando haya internet" + check verde

### Criterios de aceptación
- [ ] Flow completa en < 90s por usuario experimentado (usability test)
- [ ] 100% funcional sin red
- [ ] Validaciones en cliente idénticas al server (compartir specs en `contracts/`)
- [ ] Audio prompts disponibles en español

### Dependencias
TASK-610, TASK-617 (sync worker)

---

## TASK-612 · Reports: list + detail + edit draft · [P1] [UI]

### Contexto
Farmer debe ver sus reportes pasados, estado de sync, y re-enviar fallidos.

### Objetivo
Lista + detail + badge status.

### Archivos a crear
- `mobile/lib/features/reports/reports_list_page.dart`
- `mobile/lib/features/reports/report_detail_page.dart`

### Status badges
- 🟡 "Pendiente de enviar" (local only)
- 🔵 "Enviado" (synced)
- ✅ "Validado" (extensionista confirmó)
- 🌾 "Cosechado"
- ❌ "Rechazado" (con motivo)

### Criterios de aceptación
- [ ] Pull-to-refresh dispara sync
- [ ] Reporte rechazado → tap muestra razón + ofrece corregir
- [ ] Borrar reporte pending posible; synced requiere reason

### Dependencias
TASK-611

---

## TASK-613 · Prices: latest prices for my crops · [P1] [UI]

### Contexto
Vista de precios para farmer. Solo lectura. Muestra últimos precios para cultivos relevantes (sus cultivos sembrados + top 5 de su región).

### Objetivo
Pantalla de precios con cards + tendencia mini-chart.

### Archivos a crear
- `mobile/lib/features/prices/prices_page.dart`
- `mobile/lib/features/prices/widgets/price_card.dart`
- `mobile/lib/features/prices/widgets/trend_sparkline.dart`
- `mobile/lib/features/prices/prices_repository.dart`

### UX
- Pull-to-refresh
- Card por cultivo: precio actual + sparkline 30d + flechita ↑↓ con %
- Tap → historial 90d con line chart (`fl_chart`)
- Offline: muestra último cached con timestamp "Actualizado hace X días"

### Criterios de aceptación
- [ ] Precios cacheados en drift con TTL 24h
- [ ] Chart renderiza sin internet si hay cache
- [ ] Comparación contra precio de hace 7d visible

### Dependencias
TASK-602, EPIC03: 311

---

## TASK-614 · Alerts: inbox + notifications handling · [P1] [UI]

### Contexto
Alertas llegan por push (FCM) cuando hay red. Guardadas en drift al llegar. Inbox muestra historial.

### Objetivo
Pantalla inbox + handling de notificaciones push.

### Archivos a crear
- `mobile/lib/features/alerts/alerts_inbox_page.dart`
- `mobile/lib/features/alerts/alert_detail_page.dart`
- `mobile/lib/features/alerts/widgets/alert_card.dart`
- `mobile/lib/core/notifications/fcm_service.dart`
- `mobile/lib/core/notifications/local_notifications.dart`

### Criterios de aceptación
- [ ] FCM token registrado con backend en login
- [ ] Notificación tap → abre detail correspondiente
- [ ] Notificaciones en foreground se muestran como in-app banner
- [ ] Badge en bottom nav con conteo unread
- [ ] "Silenciar" temporal posible

### Dependencias
TASK-605, EPIC03: 313

---

## TASK-615 · Projections: what-if recommendations · [P2] [UI]

### Contexto
Farmer elige parcela → sistema sugiere qué sembrar con rationale. Convierte proyecciones en acción.

### Objetivo
Pantalla recomendaciones con top 3 cultivos sugeridos.

### Archivos a crear
- `mobile/lib/features/projections/recommendations_page.dart`
- `mobile/lib/features/projections/widgets/crop_recommendation_card.dart`
- `mobile/lib/features/projections/projections_repository.dart`

### Card content
- Icono cultivo
- Cultivo + variedad sugerida
- "Margen esperado: Q XXX/ha" (± rango)
- Risk badge: "Bajo riesgo de sobreoferta"
- Mejor ventana siembra: fecha
- Botón "Saber más" → detalle con rationale

### Criterios de aceptación
- [ ] Respeta país/corredor automáticamente
- [ ] Offline: muestra último cache con "Actualizado hace X"
- [ ] Tap → puede iniciar report con datos pre-filled

### Dependencias
TASK-610, EPIC04: 411

---

## 6.3 Sync & offline

---

## TASK-616 · Pending mutations queue · [P0] [UI]

### Contexto
Mecanismo fundamental offline-first. Cada write va a una queue local.

### Objetivo
`MutationQueue` con API simple: `enqueue()`, `next()`, `markComplete()`, `markFailed()`.

### Archivos a crear
- `mobile/lib/core/sync/mutation_queue.dart`
- `mobile/lib/core/sync/mutation_types.dart`
- `mobile/lib/core/sync/mutation_processor.dart`

### Especificaciones
- FIFO por default
- Back-off exponencial en failures: 1min, 5min, 30min, 2h, 12h, 24h (max)
- Tras N intentos (ej 10) → mover a dead-letter + notificar UI
- Conflicts (409 del server): merge strategy por tipo de mutation

### Criterios de aceptación
- [ ] `enqueue` + restart app → mutation persiste
- [ ] Tests de concurrencia: 2 workers no procesan misma mutation
- [ ] Idempotencia por `idempotency_key`

### Dependencias
TASK-602

---

## TASK-617 · Sync worker (background service) · [P0] [UI]

### Contexto
Background service (WorkManager en Android, BGTaskScheduler en iOS) que drena la queue cuando hay red.

### Objetivo
Sync funcional incluso con app cerrada (best-effort).

### Archivos a crear
- `mobile/lib/core/sync/sync_worker.dart`
- `mobile/lib/core/sync/sync_controller.dart`           # foreground trigger
- `mobile/android/app/src/main/.../SyncWorker.kt` (nativo Android)
- `mobile/ios/Runner/SyncWorker.swift` (iOS equivalente)

### Flow del sync
```
1. Check connectivity → si no hay red, exit
2. Fetch token actual, refresh si necesario
3. Drain pending_mutations:
   - Pop batch (max 50)
   - POST /v1/sync/batch con batch
   - Parse response: marcar ACCEPTED/DUPLICATE/REJECTED
4. Pull remote changes:
   - Using cursor from sync_state
   - Update local projections, alerts, prices
5. Update sync_state cursor
6. Emit event para UI refresh
```

### Triggers
- App foreground: pull-to-refresh o auto cada 15min
- Background: al cambiar conectividad a online
- Scheduled: cada 30min con WorkManager (respetando battery)

### Criterios de aceptación
- [ ] Sync funciona con app en foreground
- [ ] Sync funciona con app cerrada al recuperar conectividad
- [ ] No sync en modo bajo consumo
- [ ] Test: submit 20 reports offline → online → todos llegan
- [ ] Conflict resolution: mismo report editado local + remoto → usa regla "local wins para drafts, server wins para validados"

### Dependencias
TASK-616, EPIC03: 306

---

## TASK-618 · Conflict resolution strategies · [P1] [UI]

### Contexto
Cuando local y remoto divergen, necesitamos estrategia clara por tipo de entidad.

### Objetivo
`ConflictResolver` con estrategias pluggable.

### Archivos a crear
- `mobile/lib/core/sync/conflict_resolver.dart`
- `mobile/lib/core/sync/strategies/`
  - `last_write_wins.dart`
  - `server_wins.dart`
  - `manual_resolution.dart`

### Reglas por entidad
| Entidad | Strategy | Notes |
|---------|----------|-------|
| PlantingReport DRAFT | local wins | usuario está editando |
| PlantingReport SUBMITTED+ | server wins | ya fue validado/procesado |
| Parcel | manual resolution | UI muestra ambas versiones |
| Profile | last-write-wins | |

### Criterios de aceptación
- [ ] Cada estrategia testeada con fixtures
- [ ] Manual resolution UI muestra diff claro

### Dependencias
TASK-617

---

## TASK-619 · Tile cache offline for mapa local · [P2] [UI]

### Contexto
Mapas son caros en datos. Cachear tiles de zonas frecuentes.

### Objetivo
Manager de tile cache con descarga on-demand.

### Archivos a crear
- `mobile/lib/core/storage/tile_cache.dart`
- `mobile/lib/features/parcels/widgets/offline_area_downloader.dart`

### Criterios de aceptación
- [ ] Usuario puede "Descargar mapa de esta zona" → 5-10 MB
- [ ] Tiles servidos desde cache primero
- [ ] Purge LRU cuando excede 100 MB

### Dependencias
TASK-610

---

## 6.4 Auxiliary features

---

## TASK-620 · Profile + settings · [P2] [UI]

### Contexto
Configuración de usuario.

### Archivos a crear
- `mobile/lib/features/profile/profile_page.dart`
- `mobile/lib/features/profile/edit_profile_page.dart`
- `mobile/lib/features/settings/settings_page.dart`

### Settings include
- Idioma
- Canal preferido (push/SMS/WhatsApp)
- Quiet hours
- Data usage (Wi-Fi only toggle)
- Logout

### Criterios de aceptación
- [ ] Cambios en preferences sync al backend
- [ ] Toggle Wi-Fi only evita sync en datos móviles

### Dependencias
TASK-605

---

## TASK-621 · Audio instructions service · [P2] [UI]

### Contexto
Usuarios con baja alfabetización necesitan audio. Pre-grabados en ES + kʼicheʼ (futuro).

### Objetivo
`AudioPlayer` widget + assets.

### Archivos a crear
- `mobile/lib/shared/widgets/audio_help_button.dart`
- `mobile/lib/shared/services/audio_service.dart`
- `mobile/assets/audio/es/*.mp3` (instrucciones pre-grabadas)

### Criterios de aceptación
- [ ] Tap botón audio → reproduce mp3 correspondiente
- [ ] Maneja audio session bien (no colisión con calls)

### Dependencias
TASK-607

---

## TASK-622 · Extensionist mode (role FARMER_EXTENSIONIST) · [P2] [UI]

### Contexto
Mismo app con flujos extra si rol `FARMER_EXTENSIONIST`.

### Objetivo
Feature flag basado en role + UI extra.

### Archivos a crear
- `mobile/lib/features/extensionist/validation_page.dart`
- `mobile/lib/features/extensionist/farmers_roster_page.dart`

### UX extra
- Nueva tab: "Mis agricultores" (roster)
- Tap en farmer → ver reportes pendientes de validación
- Validación: visitar parcela → tomar foto → confirmar/rechazar

### Criterios de aceptación
- [ ] Rol se lee del token
- [ ] Navegación dinámica según role
- [ ] Fotos sync comprimidas (< 500KB)

### Dependencias
TASK-609, EPIC03: 307

---

## TASK-623 · Cooperative admin mode · [P3] [UI]

### Contexto
Rol `COOPERATIVE_ADMIN` puede submit reports on-behalf de miembros.

### Objetivo
Flujo batch: "Reportar siembras de cooperativa" con selector de member.

### Criterios de aceptación
- [ ] Submit on-behalf registra `registered_by` correcto
- [ ] Auditable

### Dependencias
TASK-622, EPIC03: 329

---

## 6.5 Quality & polish

---

## TASK-624 · Error tracking Sentry + crash reporter · [P1] [OBS]

### Contexto
Crashes en prod deben ser visibles.

### Objetivo
Sentry integrado con PII stripping.

### Archivos a crear
- `mobile/lib/core/observability/sentry_setup.dart`

### Criterios de aceptación
- [ ] Crash forzado en dev aparece en Sentry
- [ ] Phone number, national ID never in payload
- [ ] Release tagged con `flavor + version`

### Dependencias
TASK-601

---

## TASK-625 · Analytics opt-in · [P2] [OBS]

### Contexto
Eventos de uso para mejorar UX. Opt-in claro.

### Objetivo
Wrapper over amplitude o Firebase Analytics con eventos core.

### Criterios de aceptación
- [ ] Opt-in en onboarding
- [ ] Revocable desde settings
- [ ] Eventos: `report_submitted`, `price_viewed`, `alert_opened`

### Dependencias
TASK-608

---

## TASK-626 · Integration tests: end-to-end critical flows · [P1] [TEST]

### Contexto
Tests automatizados de flows críticos.

### Archivos a crear
- `mobile/integration_test/register_and_submit_report_test.dart`
- `mobile/integration_test/offline_sync_test.dart`
- `mobile/integration_test/price_view_test.dart`

### Criterios de aceptación
- [ ] Corre en CI en emulator
- [ ] Duración < 5 min
- [ ] Flaky rate < 2%

### Dependencias
TASK-611, TASK-617

---

## TASK-627 · Performance optimization + app size · [P2] [UI]

### Contexto
Low-end devices son target.

### Objetivo
APK < 30MB, cold start < 3s en device de 2GB RAM.

### Acciones
- Tree-shaking assets
- Lazy-load routes
- Image compression automática
- Defer heavy features (mapas) hasta uso

### Criterios de aceptación
- [ ] APK release < 30MB
- [ ] Cold start < 3s en Moto G9 (o similar)
- [ ] Memory footprint < 200MB en uso normal

### Dependencias
TASK-620 y features core completas

---

## TASK-628 · Play Store + App Store release prep · [P2] [DOC]

### Contexto
Publicación en stores.

### Archivos a crear
- `mobile/fastlane/` (metadata, screenshots)
- `mobile/store/screenshots/`
- `docs/mobile/release-checklist.md`

### Criterios de aceptación
- [ ] Internal track publicado en Play Store
- [ ] TestFlight build subido
- [ ] Screenshots en 3 tamaños

### Dependencias
TASK-624

---

## Resumen épica 06

| Área | Tareas | Prioridad |
|------|--------|-----------|
| Foundations | 601-607 | P0 |
| Core features | 608-615 | P1 |
| Sync & offline | 616-619 | P0/P1 |
| Auxiliary | 620-623 | P2/P3 |
| Quality | 624-628 | P1/P2 |

**Critical path**: 601 → 602/603 → 605 → 616/617 → 611 (first full offline flow funcional).

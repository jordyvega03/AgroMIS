# ADR-009: Flutter para la aplicacion movil

## Status
Accepted (2026-04-20)

## Context
AgroMIS necesita una aplicacion movil para Android e iOS. El equipo de mobile es pequeno y no puede mantener dos codebases nativas. Los agricultores usan predominantemente Android (dispositivos de gama baja), pero las cooperativas y ministerios tambien usan iOS.

## Decision
Usar **Flutter** para el desarrollo de la aplicacion movil. Un unico codebase Dart compila a Android e iOS nativo. Se prioriza el soporte Android y la optimizacion para dispositivos de gama baja (RAM < 2GB, procesadores entry-level).

Caracteristicas criticas de la implementacion:
- **Offline-first**: datos locales con SQLite (drift), sincronizacion en background cuando hay conectividad.
- **Bajo consumo de datos**: compresion de imagenes antes de subir, protobuf para sincronizacion.
- **Soporte 2G/3G**: timeouts generosos, retry con backoff exponencial.

## Consequences
- Positivas: Un solo equipo para ambas plataformas; excelente rendimiento en gama baja; ecosistema maduro para mapas (flutter_map + MapLibre).
- Negativas: Dart es un lenguaje menos comun que Kotlin/Swift; algunas APIs nativas requieren plugins o platform channels.
- Neutrales: La app PWA (React) cubre el caso de escritorio/tablet para usuarios con mejor conectividad.

## Alternatives considered
- React Native: bridge JS-nativo puede tener problemas de rendimiento en gama baja.
- Kotlin Multiplatform: UI nativa por plataforma requiere mas codigo de UI.
- Native separado (Kotlin + Swift): dos codebases, equipo duplicado.

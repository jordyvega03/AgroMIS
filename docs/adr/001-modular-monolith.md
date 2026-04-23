# ADR-001: Monolito Modular como arquitectura de backend

## Status
Accepted (2026-04-20)

## Context
El equipo es pequeno (< 8 personas en etapa inicial) y el dominio de AgroMIS aun esta siendo explorado. Los microservicios imponen overhead operativo (descubrimiento de servicios, contratos inter-servicio, transacciones distribuidas) que no se justifica en esta etapa. Ref: seccion "Backend Architecture" del documento de arquitectura principal.

## Decision
Implementar el backend en Quarkus como **monolito modular**: un unico desplegable que contiene modulos con fronteras bien definidas (farming, market, weather, reputation, notification, auth). Los modulos se comunican a traves de interfaces Java (no HTTP), y cada modulo tiene su propio paquete, tests y migraciones.

## Consequences
- Positivas: Despliegue simple, transacciones locales ACID, refactor de modulos sin contratos distribuidos, menor latencia intra-servicio.
- Negativas: Escalar un modulo implica escalar todo el proceso; merge conflicts si el equipo crece mucho.
- Neutrales: Se puede extraer un modulo a microservicio en el futuro si el volumen lo justifica (strangler fig pattern).

## Alternatives considered
- Microservicios desde dia 1: overhead operativo demasiado alto para un equipo pequeño.
- Monolito sin modulos: deuda tecnica garantizada; no escala organizacionalmente.

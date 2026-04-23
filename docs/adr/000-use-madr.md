# ADR-000: Usar MADR como formato de Decision de Arquitectura

## Status
Accepted (2026-04-20)

## Context
El equipo necesita documentar decisiones tecnicas de forma estructurada y persistente para que futuros miembros entiendan el razonamiento detras de cada eleccion. Sin un formato estandar, las decisiones quedan dispersas en correos, Slack o conversaciones orales.

## Decision
Adoptamos el formato **MADR** (Markdown Architectural Decision Records) para todas las decisiones de arquitectura del proyecto AgroMIS. Cada ADR vive en `docs/adr/` con nombre `NNN-titulo-kebab-case.md`. El numero es secuencial y nunca se reutiliza.

## Consequences
- Positivas: Decision visible en Git, versionada, buscable, revisable en PR.
- Negativas: Requiere disciplina del equipo para crear ADRs al tomar decisiones.
- Neutrales: Los ADRs pueden evolucionar de estado (proposed → accepted → deprecated → superseded).

## Alternatives considered
- RFC en Notion: no esta en el repositorio, se pierde contexto con el tiempo.
- Comentarios en codigo: demasiado granulares, no capturan el contexto de negocio.
- Sin documentacion formal: inaceptable para un sistema de esta escala.

# ADR-008: Prophet como modelo ML baseline para proyecciones

## Status
Accepted (2026-04-20)

## Context
AgroMIS necesita generar proyecciones de cosecha y precio por cultivo. En el MVP no tenemos suficientes datos historicos propios para entrenar modelos complejos. Necesitamos un modelo que funcione razonablemente con datos escasos y que el equipo pueda operar sin MLOps avanzado.

## Decision
Usar **Meta Prophet** como modelo baseline para proyecciones de series de tiempo (precios y estimaciones de cosecha). Prophet maneja estacionalidad multiple, tendencias y datos faltantes de forma automatica. El motor de proyecciones es un servicio Python separado (`projection-engine/`) que expone una API REST y consume datos de PostgreSQL/Redpanda.

La arquitectura define una interfaz `ProjectionModel` para que modelos mas sofisticados (XGBoost, LightGBM, modelos de cultivo DSSAT) puedan reemplazar o complementar a Prophet en el futuro.

## Consequences
- Positivas: Rapido de implementar; documentacion excelente; soporta incertidumbre (intervalos de prediccion); sin GPU necesario.
- Negativas: No captura relaciones entre variables (precio del maiz no afecta prediccion de frijol en el modelo base); no es el estado del arte.
- Neutrales: Los modelos se reentrenan semanalmente por cultivo y pais con los datos mas recientes.

## Alternatives considered
- LSTM/Transformer: requiere mucho mas datos historicos de los que tenemos en MVP.
- SARIMA: mas complejo de tunear por cultivo/pais; Prophet es esencialmente SARIMA con mejor UX.
- Modelo de cultivo DSSAT: excelente pero requiere datos agronomicos detallados que no tenemos aun.

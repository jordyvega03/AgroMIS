# ADR-006: Excluir integracion satelital del MVP

## Status
Accepted (2026-04-20)

## Context
Existe interes en integrar imagenes satelitales (NDVI, deteccion de estres hidrico) para enriquecer las proyecciones de cosecha. Sin embargo, esto requiere integracion con APIs de Sentinel-2/Landsat, pipelines de procesamiento de raster y almacenamiento de alto volumen.

## Decision
**No incluir integracion satelital en el MVP (v1.0)**. Los datos de campo vienen exclusivamente de reportes manuales de agricultores y sensores IoT basicos. La arquitectura deja una interfaz `SatelliteDataPort` sin implementacion para facilitar la integracion futura.

## Consequences
- Positivas: Reduce alcance tecnico del MVP en ~3 meses de trabajo; permite lanzar antes.
- Negativas: Las proyecciones de cosecha del MVP seran menos precisas que con datos satelitales.
- Neutrales: El puerto de integracion satelital queda definido en la arquitectura para V2.

## Alternatives considered
- Incluir Sentinel-2 desde el inicio: costo de computo alto, complejidad de GCP/Copernicus APIs, riesgo de retraso del MVP.
- Comprar datos a Planet.com: costo prohibitivo para escala Centroamerica en MVP.

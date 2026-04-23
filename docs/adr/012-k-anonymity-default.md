# ADR-012: k-Anonimato por defecto en datos de agricultores

## Status
Accepted (2026-04-20)

## Context
AgroMIS agrega datos sensibles de agricultores individuales (ubicacion exacta de parcelas, volumenes de cosecha, precios de venta). Exponer estos datos de forma granular puede crear riesgo de seguridad fisica para los agricultores (extorsion, robo). Ademas, regulaciones de proteccion de datos en algunos paises centroamericanos lo requieren.

## Decision
Implementar **k-Anonimato (k >= 5 por defecto)** en todas las APIs publicas que devuelvan datos de produccion o precio a nivel de agricultor individual. Si un grupo de query tiene menos de k registros, los datos se suprimen o se agregan al nivel superior (municipio → departamento).

Reglas especificas:
- Coordenadas de parcela: solo disponibles para el dueno y roles autorizados (extension worker, auditor).
- Datos de cosecha en APIs publicas: agregados a nivel de municipio minimo.
- Precios de venta individuales: nunca expuestos en APIs publicas.

## Consequences
- Positivas: Proteccion de privacidad de agricultores vulnerables; cumplimiento con principios GDPR/LGPD; confianza del agricultor en el sistema.
- Negativas: Reduce granularidad de datos para investigadores; implementacion no trivial en queries geoespaciales.
- Neutrales: Los investigadores con acuerdos de datos pueden acceder a microdatos anonimizados bajo contrato.

## Alternatives considered
- Differential privacy: matematicamente mas fuerte pero mas complejo de implementar correctamente; puede degradar demasiado la utilidad de los datos.
- Sin restricciones: inaceptable dado el contexto de seguridad en Centroamerica.
- Acceso por roles sin anonimizacion: protege contra acceso no autorizado pero no contra abuso de acceso autorizado.

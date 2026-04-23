# ADR-007: SMS y USSD como canales de primera clase

## Status
Accepted (2026-04-20)

## Context
Una parte significativa de los agricultores en Centroamerica no tiene smartphones ni acceso a internet de banda ancha. Excluirlos del sistema reduciria drasticamente el alcance y el valor de los datos recolectados. La penetracion de telefonia movil basica (2G/SMS) es mucho mayor que la de smartphones.

## Decision
El modulo de notificaciones y el flujo de reporte de siembra soportan **SMS y USSD como canales de primera clase**, no como afterthought. La arquitectura define un `NotificationPort` con adaptadores para: Push (FCM), SMS (Twilio/local carrier), USSD (Africa's Talking o gateway local), Email. Todos los formularios criticos tienen version USSD de arbol de menus.

## Consequences
- Positivas: Alcance a agricultores sin smartphone; datos mas completos y representativos; diferenciador vs competidores.
- Negativas: Flujos USSD estan limitados a menus de texto; UX restrictiva; requiere integracion con gateways locales por pais.
- Neutrales: SMS tiene costo por mensaje; se puede usar USSD gratuito (cobrado al operador) dependiendo del acuerdo.

## Alternatives considered
- Solo app movil: excluye ~60% del segmento objetivo.
- WhatsApp Business API: cobertura mejor que SMS pero no universal y dependiente de Meta.

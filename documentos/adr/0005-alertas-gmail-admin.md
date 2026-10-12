# ADR-0005: Alertas por correo de Gmail al admin

**Fecha**: 2026-10-10 (registrada originalmente el 2026-10-09)
**Estado**: accepted
**Decisores**: cliente (respuesta del 2026-10-09: "el admin"; documento del cliente: "Y con cada etapa el sistema alertará … notificaciones al Gmail")

## Contexto

Cada etapa y cada vencimiento deben notificarse (`requerimientos.md`, 4.2 #16). Hoy el cliente usa recordatorios manuales en Slack, con errores conocidos (`requerimientos.md`, 1.8). El demo simula los correos en memoria.

## Decisión

El sistema envía un correo al admin en cada avance de etapa y en cada vencimiento, con el motivo visible en la etapa. Los correos salen desde el servidor, por una cola con reintentos. La web reemplaza los recordatorios de Slack.

## Alternativas consideradas

### Slack
- **Pros**: el equipo ya lo usa.
- **Contras**: la calculadora actual mostró errores (canal inexistente, comandos rechazados).
- **Por qué no**: el cliente pidió Gmail, y la web reemplaza a la calculadora y a Slack.

## Consecuencias

### Positivas
- Un único canal de alertas, con registro de lo enviado y de lo que falló.

### Negativas
- Si solo recibe correo el admin, los responsables se enteran al entrar al sistema.

### Riesgos
- Un responsable no se entera a tiempo. Mitigación: bandeja y línea de tiempo en cada dashboard.

## Diferencias entre fuentes (sin resolver)

- El registro de decisiones dice "Gmail al admin". La tabla "Por confirmar con el cliente" de `requerimientos.md` y `arquitectura.md` (8.1) proponen, como valor provisional, "correo al admin y al responsable; el admin puede apagarlo". Queda pendiente hasta que el cliente responda.
- El canal técnico (Gmail API o SMTP de Workspace) es una propuesta separada: ADR-0013.

## Pendiente del cliente

- ¿El responsable de la etapa también recibe correo?
- ¿La web reemplaza por completo la calculadora y Slack? (respuesta recibida: "si", sin más detalle).

# ADR-0013: Canal técnico de correo: Gmail API o SMTP de Workspace

**Fecha**: 2026-10-10
**Estado**: proposed
**Decisores**: equipo de desarrollo (propuesta de `arquitectura.md`, 4.6 y 8.2)

## Contexto

ADR-0005 decide que las alertas se envían por Gmail. `requerimientos.md` (sección 3) deja abierto el mecanismo: "Gmail API o SMTP de Google Workspace". El correo sale solo desde el servidor (R8).

## Decisión (propuesta)

Usar Gmail API con una cuenta de servicio y delegación limitada al buzón remitente. En la arquitectura es el adaptador `GmailNotificador` del puerto `Notificador`, así que cambiar a SMTP no toca el dominio.

## Alternativas consideradas

### SMTP de Google Workspace
- **Pros**: configuración simple y estándar.
- **Contras**: necesita una contraseña de aplicación o un relay, y da menos control sobre los errores.
- **Por qué no se descarta**: es una alternativa válida; se decide al empezar la fase 3.

### Delegación de dominio completa
- **Pros**: menos configuración.
- **Contras**: la cuenta de servicio podría enviar como cualquier usuario del dominio.
- **Por qué no**: va contra el mínimo privilegio.

## Consecuencias

### Positivas
- El puerto `Notificador` aísla la elección.

### Negativas
- Gmail API requiere configurar la consola de Google Cloud y la administración de Workspace.

## Pendiente

- Cuenta de servicio y buzón remitente en el Workspace del cliente.

# ADR-0017 OAuth de usuario para Gmail personal y Mi unidad

**Fecha:** 2026-10-17  
**Estado:** accepted (equipo, configuración expresamente acordada por Richard)  
**Decisor:** Richard. Encargo: implementar exclusivamente fase 3, parte A, paso 3.1, sin operaciones reales Google, pruebas o builds.

## Contexto

ADR-0003 describe la opción original de cuenta de servicio y unidad compartida de Workspace; ADR-0013 mantenía propuesto el mecanismo técnico de Gmail. Richard confirmó que la empresa utiliza una cuenta Gmail personal e indicó usar autorización OAuth de usuario, sin delegación Workspace. Esta decisión registra esa configuración acordada, sin presentarla como una nueva respuesta del cliente de los requisitos originales.

## Decisión

Cliente OAuth de **aplicación web**, retorno local exacto `http://localhost:8080/api/v1/integraciones/google/oauth2/callback`, permisos `gmail.send`, `drive.file`, `openid` y `userinfo.email`. Gmail API y Drive API actuarán como la cuenta que autorice; Drive usará **Mi unidad**, no una unidad compartida de organización. La aplicación Google está **En pruebas**.

El inicio exige un admin vigente de DECOKASA; estado de un solo uso y cookie temporal de vínculo protegen el retorno, junto con nonce y PKCE. Solo el backend canjea el código, valida la cuenta esperada y guarda credenciales/tokens fuera del árbol Git, con cifrado de tokens, llave separada y permisos privados. Se solicita acceso offline. Los JWT y el login de DECOKASA no cambian.

Esta variante sustituye para la integración acordada la autenticación/propiedad Workspace de ADR-0003 y resuelve el canal Gmail API pendiente en ADR-0013. Sus textos se conservan como antecedentes. No modifica historial/outbox, destinatarios, límite de archivo, reintentos ni carpetas pendientes.

## Alternativas consideradas

- **Cuenta de servicio, unidad compartida y delegación de dominio:** no corresponde a la cuenta personal confirmada; se descarta para este alcance por instrucción expresa de Richard.
- **SMTP:** alternativa aislable tras el puerto de correo, pero el canal acordado es Gmail API; no se implementan ambos.
- **Cliente de escritorio o OAuth en SPA:** no corresponde al backend web que conservará la autorización offline. Cliente web confidencial y tokens solo en servidor.
- **Scope Drive completo:** innecesario para los archivos/carpetas propios de la integración. `drive.file` limita el alcance; una carpeta preexistente no queda autorizada solo por conocer su ID.

## Consecuencias

- No requiere administrador Workspace ni suplantación delegada. La persona autorizante debe controlar la cuenta Gmail de empresa y consentir en un navegador con acceso a Google.
- La cuenta personal posee los archivos y consume su cuota; no hay propiedad organizacional equivalente a unidad compartida. Los empleados siguen usando el backend para archivos y correo.
- En modo externo En pruebas, con Gmail/Drive, la autorización y el refresh token caducan a los siete días. No hay garantía de automatización permanente; reconexión, publicación y cliente de producción son asuntos posteriores.
- El paso 3.1 solo prepara autorización y almacenamiento. No crea carpeta Drive, copia archivos, envía correo, añade colas o implementa 3.2–3.5.
- Entrega codificada **sin compilar ni ejecutar pruebas**; no acredita funcionamiento real. Comprobaciones y eventual autorización manual se reservan para un encargo posterior de parte B.

Referencia: [plan de fase 3](../../.claude/PRPs/plans/fase-3-drive-gmail.plan.md) y [guía de autorización manual](../google-oauth-paso-3.1.md). No se realizó una autorización local durante el encargo.

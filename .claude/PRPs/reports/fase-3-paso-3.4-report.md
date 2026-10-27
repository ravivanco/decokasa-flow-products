# Informe de implementación — fase 3, parte A, paso 3.4

Fecha: 2026-10-17. Rama: `fase-3-drive-gmail`. El plan existente se mantiene activo; 3.5 y parte B no están completados.

| Tarea | Entrega de código |
|---|---|
| Copia | Misma cola adjunto; id reservado, sesión cifrada, offset, comprobación de identidad/checksum y enlace real |
| Reintentos | Inicial + tres; base 1/5/15 min, fechas persistidas, sin sleep o quinto intento nuevo |
| Recuperación | Lease/token y cerrojo local; recuperación del mismo intento y consulta de offset remoto |
| Idempotencia | Reservas antes de crear, conciliación 409/timeout y sesión PATCH sobre el mismo ID si procede |
| Carpetas | Mecanismo país/pedido creado por aplicación aprobado expresamente por Richard durante el encargo |
| Persistencia | V6 aditiva preparada, sin aplicar; V1–V5 intactas |
| Desactivación | OAuth/Drive false y COPIA_DRIVE_AUTOMATICA=false; sin tareas registradas por defecto |

Concreciones: no jitter en este encargo; no purga/retención inventada, rearme automático de FALLIDO, correo o pantallas. Los repositorios se decoran con transacciones cortas en composición; ninguna incluye Google. La clasificación de renovación offline conserva los errores temporales para el presupuesto de copia, sin cambiar el consentimiento o habilitar OAuth.

Pruebas, compilación/análisis ejecutable, builds, migraciones, servidor y Google: **no realizados**, por instrucción expresa. No se modifica ningún archivo de pruebas, no se declara PASS y no se archiva el plan ni cierra el PRD. Sin commit/push.

Detalle, decisiones aprobadas, límites, riesgos y casos reservados a parte B: [informe 3.4](../../../documentos/fase-3-paso-3.4.md).

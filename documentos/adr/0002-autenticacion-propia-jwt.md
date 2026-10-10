# ADR-0002: Autenticación propia con JWT y BCrypt

**Fecha**: 2026-10-10 (registrada originalmente el 2026-10-09)
**Estado**: accepted
**Decisores**: cliente (stack: "Login propio con roles, sin depender de Google") y equipo

## Contexto

Los usuarios entran con correo, nombre de usuario o teléfono, más contraseña (`requerimientos.md`, 4.2 #1). David trabaja desde China, donde los servicios de Google pueden estar bloqueados. El demo compara una contraseña única escrita en el código y todo lo decide en el navegador (`auditoria-demo.md`, S1 a S4).

## Decisión

El backend autentica con Spring Security, guarda las contraseñas con BCrypt y emite un JWT con el rol del usuario. Los permisos por rol y por etapa se validan en el servidor.

## Alternativas consideradas

### Google Sign-In
- **Pros**: sin contraseñas propias y con el Workspace que la empresa ya tiene.
- **Contras**: depende de Google para entrar.
- **Por qué no**: desde China no se puede depender de Google.

### Sesión con cookie en el servidor
- **Pros**: se revoca fácil.
- **Contras**: no es lo que propone el documento del cliente.
- **Por qué no**: el stack del cliente dice JWT. Se puede revisar en otro ADR.

## Consecuencias

### Positivas
- El acceso no depende de Google.
- Corrige los hallazgos críticos S1 a S4 del demo.

### Negativas
- El equipo mantiene la gestión de contraseñas.

### Riesgos
- Robo del token. Mitigación: la propuesta de abajo.

## Propuestas asociadas (no aprobadas)

- **Propuesta del equipo**: JWT de vida corta y bloqueo temporal tras N intentos fallidos, con N y la duración configurables (`arquitectura.md`, 4.1 y 8.2). El cliente no lo pidió.

## Pendiente del cliente

- Recuperación de contraseña por correo: sin definir.

# ADR-0015: Datos iniciales: países y roles en migraciones, usuarios reales fuera de git

**Fecha**: 2026-10-12
**Estado**: accepted (aprobado por el equipo el 2026-10-12)
**Decisores**: equipo de desarrollo (Richard)

## Contexto

El manual (paso 1.5) pedía sembrar en la migración los 6 usuarios de `requerimientos.md` 1.2 con contraseñas temporales. El repositorio de GitHub es público (comprobado el 2026-10-12) y ya contiene nombres, correos y teléfonos del demo (`auditoria-demo.md`, S5). Si se siembran en una migración, los datos personales y las contraseñas quedan publicados y en el historial de git para siempre.

## Decisión

- Las migraciones Flyway cargan solo datos no personales: los **países** (EC activo, CO inactivo) y los **7 roles** iniciales (`admin`, `asistente_compras`, `marketing`, `compras_china`, `logistica`, `asistente_china` y `consulta`).
- Las pruebas usan solo **usuarios ficticios** (por ejemplo, `admin@ejemplo.test`) y contraseñas generadas dentro de la propia prueba.
- Los **usuarios reales** se cargan fuera de git, desde un archivo local ignorado por `.gitignore`, o los crea el admin desde la aplicación.
- **Ninguna contraseña real ni clave** (JWT, base de datos, Google, Claude) se guarda en el repositorio: ni en migraciones, código, pruebas, `application.yml` ni `.env.example`. Las claves se leen de variables de entorno.

**Propuesta asociada (forma de carga, ver el plan de la fase 1, tarea 10):** un cargador de arranque, activo solo con `decokasa.semilla.habilitada=true`, lee el archivo de `SEMILLA_ARCHIVO` cuando la tabla `usuario` está vacía y crea cada usuario con el mismo caso de uso que usa el admin. La contraseña temporal sale de `SEMILLA_CONTRASENA_TEMPORAL`. El repositorio solo lleva `backend/semilla/usuarios.ejemplo.json` con datos ficticios:

```json
[
  { "nombre": "Admin de Prueba", "correo": "admin@ejemplo.test", "usuario": "admin1",
    "telefono": "+593990000001", "rol": "admin" }
]
```

## Alternativas consideradas

### Sembrar los usuarios reales en una migración
- **Pros**: un solo paso y sin archivos externos.
- **Contras**: publica datos personales y contraseñas en un repositorio público; no se pueden borrar del historial.
- **Por qué no**: riesgo de seguridad y de privacidad.

### Sembrar usuarios con contraseñas temporales conocidas
- **Pros**: arranque inmediato del entorno.
- **Contras**: cualquiera que lea el repositorio conoce las contraseñas.
- **Por qué no**: mismo riesgo.

## Consecuencias

### Positivas
- El repositorio no agrega datos personales ni secretos nuevos.
- Las pruebas son reproducibles en cualquier máquina sin datos reales.

### Negativas
- Para tener los usuarios reales en un entorno nuevo hay que preparar el archivo local o crearlos a mano.

### Riesgos
- Que alguien suba el archivo local por error. Mitigación: patrón `backend/semilla/*.local.json` en `.gitignore` y revisión de seguridad en cada PR.

## Diferencias entre fuentes

- El manual (paso 1.5) pedía sembrar los 6 usuarios con contraseñas temporales y habla de "6 roles". Esta decisión lo reemplaza; el rol `consulta` se conserva como 7.º rol por decisión del equipo (alcance pendiente del cliente).

## Pendiente del cliente

- Si el repositorio pasa a privado. No cambia esta decisión.
- Alcance del rol `consulta`.

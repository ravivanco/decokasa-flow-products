# Arranque manual del backend

Desde esta carpeta, ejecutar en PowerShell:

```powershell
.\gradlew.bat bootRun
```

Requiere Java 21 y PostgreSQL activo. Para el servidor local administrado con pgAdmin,
la conexión utilizada es `localhost:5433/decokasa`, usuario `postgres`.

Spring carga `backend/.env.local` al arrancar desde `backend/`. El archivo está ignorado
por Git y contiene `DB_URL`, `DB_USUARIO`, `DB_CONTRASENA` y `JWT_SECRETO`.
La plantilla sin secretos es `.env.example`; para otro equipo, copiarla a `.env.local`
y completar los valores. JWT_SECRETO debe tener al menos 32 bytes.
Las variables de la terminal tienen prioridad sobre el archivo: revisar o retirar
valores anteriores si siguen apuntando a otro servidor o usuario.

Comprobar el arranque en `http://localhost:8080/api/v1/salud`: responde `{"estado":"ok"}`.
Para detenerlo en una terminal interactiva, usar Ctrl+C.

Para probar Swagger, detener el arranque anterior con Ctrl+C y ejecutar:

```powershell
.\gradlew.bat bootRun --args="--spring.profiles.active=dev"
```

Abrir `http://localhost:8080/swagger-ui/index.html`. La documentación JSON está en
`http://localhost:8080/v3/api-docs`. Solo se habilitan con el perfil `dev`.
Para endpoints protegidos, ejecutar primero `POST /api/v1/auth/login` con un usuario
existente y copiar `tokenAcceso` a Authorize (sin escribir el prefijo Bearer).
Swagger documenta los endpoints implementados; el contrato de todas las fases sigue
en `documentos/api/openapi.yaml`. Las rutas de negocio mantienen sus permisos.

Flyway aplica las migraciones de `src/main/resources/db/migration/`. No ejecutar esos
SQL manualmente en pgAdmin: debe registrar cada versión en `flyway_schema_history`.
En la base local existente, V1 se había ejecutado manualmente. Se compararon columnas,
restricciones, índices y semillas con V1 y se registró un baseline 1 el 2026-10-14,
sin recrear tablas. La opción temporal de baseline se retiró después del arranque.
Esta regularización corresponde solo a esa base, no a bases nuevas.

Docker Compose usa las variables del `.env` de la raíz y su PostgreSQL independiente.
La configuración local del backend no modifica las credenciales de Docker.

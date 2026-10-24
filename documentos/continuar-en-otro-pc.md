# Continuar en otro PC

## Descargar el proyecto

```powershell
git clone --branch fase-7-colombia-ia https://github.com/ravivanco/decokasa-flow-products.git
Set-Location decokasa-flow-products
```

La rama contiene el avance de las fases anteriores y el checkpoint de fase 7. No equivale a una entrega aprobada. Consulta los informes en `documentos/pruebas/`: la última revalidación reporta 942 pruebas aprobadas y cobertura global 77,33 %, inferior al 80 %. La prueba real de Drive/Gmail sigue pendiente.

## Preparar el equipo

Instala Git, Java 21, Node.js compatible con `frontend/package.json` y Docker Desktop. El backend incluye el wrapper de Gradle. Consulta `backend/README.md`, los archivos de ejemplo y los planes antes de arrancar servicios; el arranque puede aplicar migraciones a la base elegida.

## Accesos que no viajan por Git

Este repositorio es público. Transfiere mediante un medio privado seguro, separado del repositorio:

- `.env` y `backend/.env.local` si necesitas conservar su configuración. Las rutas del equipo anterior deben adaptarse.
- El JSON OAuth de Google y la llave de cifrado externa. Los tokens cifrados, si se transfieren, necesitan su llave correspondiente.
- La clave API propia de la aplicación, si existe; la IA debe seguir apagada hasta verificar el piloto.
- La semilla local de usuarios reales, si existe; nunca incluirla en una migración o commit.
- Una copia de seguridad de PostgreSQL y el almacenamiento de archivos, si necesitas los mismos datos. Clonar Git no copia la base, volúmenes Docker ni documentos.

No pegues estos valores en chats, informes, capturas o commits. Puedes crear configuraciones nuevas usando `.env.example`, `backend/.env.example` y `frontend/.env.example`, y autorizar Google de nuevo en el equipo nuevo.

El acceso a GitHub y el inicio de sesión de las herramientas de desarrollo se configuran por separado en el equipo nuevo. Las skills personales instaladas fuera del proyecto tampoco se trasladan al clonar.

Mantén OAuth, Drive, Gmail, IA y procesamientos automáticos desactivados al preparar el equipo. La prueba real autorizada debe seguir limitada a un archivo ficticio y un correo al buzón aprobado, con confirmación personal del consentimiento de Google.

Se versionaron también las evidencias auxiliares revisadas, capturas ficticias, inventarios y resultados binarios históricos. No son necesarios para ejecutar la aplicación. El reporte `documentos/pruebas/fase-3-evidencias/ejecucion-02/TEST-com.decokasa.seguimiento.archivos.CargasB1IT.xml` supera 100 MiB y se conserva como `.xml.zip`; puede extraerse después de clonar.

El checkout auxiliar local y los archivos privados no se trasladan por Git. La traza `documentos/pruebas/fase-4-evidencias/verificacion-4.5-4.6/browser-artifacts/pantallas-catalogo-guarda--faa5d-serva-borrador-sin-reenviar/trace.zip` contiene datos de autenticación y tampoco se publica; debe conservarse por un medio privado si se necesita.

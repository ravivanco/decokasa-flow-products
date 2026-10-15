# Fase 1, parte B — Paso 1.B1: backend y persistencia

Fecha: 2026-10-14 (America/Guayaquil). Rama comprobada: `fase-1-base-acceso`.
HEAD inicial: `0fe8fcc97bd723028ac873f85b5953628cf059b8`.

## Alcance y fuentes

Se revisaron el nuevo `documentos/manual-paso-a-paso.docx` (texto OOXML del paso 1.B1 y sus límites), `CLAUDE.md`, el [plan](../../.claude/PRPs/plans/fase-1-base-acceso.plan.md), [OpenAPI 0.5.0-borrador](../api/openapi.yaml) y los informes de implementación [1.7](../../.claude/PRPs/reports/fase-1-paso-1.7-report.md) y [1.8](../../.claude/PRPs/reports/fase-1-paso-1.8-report.md). La auditoría del 13 de octubre aporta antecedentes; sus resultados históricos no sustituyen esta ejecución.

Skills aplicadas: `ecc-springboot-tdd`, `ecc-tdd-workflow` y revisión de cambios Java con `ecc-java-reviewer` (Spring Boot). Se respeta la instrucción del usuario sobre las propuestas de las skills: no se implementan correcciones de producción ni se crean checkpoints Git. No se ejecutan comandos de instalación o despliegue sugeridos por el plan.

El objetivo probado es entrar por tres identificadores, aplicar el bloqueo acordado, mantener sesiones con rotación limitada, autorizar con datos vigentes y conservar consistencia frente a operaciones simultáneas. Se reutilizaron las suites existentes, sin duplicarlas, omitir casos ni reducir el umbral JaCoCo.

## Conservación del trabajo

`git status --short` inicial mostró trabajo modificado, eliminado y sin seguimiento. Incluía cambios de producción en `SeguridadWebConfig.java` y `application.yml`, la prueba `SaludControllerTest.java`, frontend, `CLAUDE.md`, plan e informes. Todo se conservó; ninguna de esas modificaciones se atribuye a este paso.

Cambios de este paso: cuatro pruebas añadidas en tres archivos existentes y este informe:

- `backend/src/test/java/com/decokasa/seguimiento/MigracionesIT.java`.
- `backend/src/test/java/com/decokasa/seguimiento/usuarios/adapter/in/web/SesionHttpIT.java`.
- `backend/src/test/java/com/decokasa/seguimiento/usuarios/config/DesactivarMientrasRenuevaIT.java`.

Las integraciones usan PostgreSQL `postgres:17` de Testcontainers (`@ServiceConnection` o contenedor propio de migraciones); las pruebas unitarias conservan sus dobles y `RelojFijo`. Se usan personas ficticias y correos `@ejemplo.test`. No se conectaron las pruebas a la base de Compose, no se ejecutó `docker compose down`, no se borraron volúmenes ni datos del entorno local. La preparación y limpieza de fixtures existente ocurre exclusivamente dentro de las bases efímeras de prueba.

## Matriz de garantías y evidencia

Rutas de clases relativas a `backend/src/test/java/com/decokasa/seguimiento/`. El comando de integración y cobertura es `cd backend; .\gradlew.bat check` (en la ejecución se fijó directamente `backend/` como directorio de trabajo).

| Requisito / decisión del plan | Pruebas existentes o completadas | Garantía comprobada |
| --- | --- | --- |
| Migración, D5 | `MigracionesIT` | V1 sobre base vacía, tablas, países y siete roles, ausencia de usuarios reales, restricciones y relaciones. Nuevo: validar y reiniciar Flyway no reaplica V1; queda una aplicada, ninguna pendiente y versión actual 1. |
| Login, T12–T14 | `usuarios/application/IniciarSesionTest`, `usuarios/adapter/in/web/AuthControllerIT.entraConCorreoUsuarioOTelefono` | Correo y usuario sin distinguir mayúsculas; teléfono con espacios; mismo usuario y todos sus roles. |
| Contraseña incorrecta | `AuthControllerIT.unaContrasenaIncorrectaResponde401YSumaUnIntento` | 401 `NO_AUTENTICADO`, respuesta Problem y contador persistido. |
| Cinco fallos y bloqueo, D2 | `AuthControllerIT.trasCincoFallosLaCuentaSeBloqueaQuinceMinutos`, `IniciarSesionTest`, `usuarios/domain/UsuarioTest` | Los cinco fallos devuelven 401; el quinto guarda el bloqueo; la contraseña correcta durante bloqueo devuelve 423 `CUENTA_BLOQUEADA`. Duración exacta y desbloqueo al minuto 15 con reloj fijo; duración persistida entre 14 y 15 minutos por HTTP. |
| JWT, D3 | `AuthControllerIT.elTokenDeAccesoLlevaElIdYTodosLosRolesYDuraQuinceMinutos` | `sub`, roles y `exp - iat = 900`; respuesta `expiraEn = 900`; token manipulado rechazado en prueba separada. |
| Renovación rotativa, D3/D3b | `usuarios/application/RenovarSesionTest`, `usuarios/domain/SesionRenovableTest`, `usuarios/adapter/in/web/SesionHttpIT` | Rotación, cookie protegida, reutilización revoca familia, vencimiento exacto a las 8 h con reloj fijo. Nuevo: tres rotaciones HTTP con un solo token abierto, misma familia y mismo vencimiento en todas las filas; el sucesor vencido da 401 y no crea otra fila. |
| Sin roles / desactivado, D4 | `AuthControllerIT.desactivadosSinRolesEInexistentesRecibenLaMismaRespuestaQueUnaContrasenaIncorrecta`, `SesionHttpIT`, pruebas de aplicación | Login rechazado con el mismo cuerpo 401; renovación rechazada y revocada; usuario conservado al desactivar. |
| Autorización vigente, A1 | `usuarios/adapter/in/web/UsuarioControllerIT` y `CrearUsuarioTest`, `ActualizarUsuarioTest`, `ConsultasTest`, `CrearRolTest`, `ActualizarRolTest` | El JWT anterior que aún dice admin no permite administrar tras desactivar o retirar admin. Se rechazan consultas y mutaciones sin cambios de estado; múltiples roles incluyendo admin sí permiten. |
| Duplicados, D9/M2 | `CrearUsuarioTest`, `ActualizarUsuarioTest`, `MigracionesIT`, `usuarios/adapter/out/persistence/RestriccionesUnicasUsuarioTest`, `usuarios/adapter/in/web/AltasConcurrentesIT` | Unicidad de correo, usuario y teléfono; 409 `DUPLICADO` con campo y sin detalles SQL; dos altas guardan una sola. Tres campos por diez rondas. |
| Versiones, D10 | `usuarios/adapter/in/web/VersionUsuarioIT`, `EdicionConcurrenteIT`, `usuarios/adapter/out/persistence/UsuarioJpaRepositorioIT`, `ActualizarUsuarioTest` | Versión obligatoria; 400 para ausente/inválida; versión vieja da 409 sin cambiar usuario ni revocar sesiones; dos ediciones guardan una; login no cambia versión ni pierde contadores. |
| Login concurrente, A4 | `usuarios/adapter/in/web/IntentosConcurrentesIT`, `EdicionConcurrenteIT` | Ocho logins fallidos simultáneos: exactamente cinco 401 y tres 423, contador cinco, sin 500 (cinco rondas); edición no pisa intentos (diez rondas). |
| Renovación concurrente, A2 | `usuarios/adapter/in/web/RenovacionConcurrenteIT` | Dos renovaciones: exactamente 200/401, un sucesor, familia revocada, mismo vencimiento (25 rondas). |
| Desactivación concurrente, N1 | `usuarios/config/DesactivarMientrasRenuevaIT` | Dos órdenes forzados de renovación/desactivación existentes. Nuevos: si login toma primero la fila, la desactivación revoca también su nueva sesión; si desactivación toma primero la fila, login falla sin crear sesión. Estado final inactivo y cero tokens sin revocar. Señales obligatorias con límite de 10 s y resultados con límite de 30 s. |

## Ejecuciones y resultados

1. Inspección: `git status --short`, `git branch --show-current`, `git rev-parse HEAD`, enumeración de archivos y lectura de fuentes/pruebas con PowerShell. Rama correcta. `rg` no está instalado; se usó `Get-ChildItem` y `Select-String`. El perfil de PowerShell intentaba crear un enlace de fnm fuera del workspace; los siguientes comandos usaron `login: false`.
2. Lectura DOCX: `ZipFile.OpenRead` encontró el documento abierto por otro proceso. Se leyó con `FileShare.ReadWrite` y `ZipArchive`, sin cerrar Word ni modificar el archivo.
3. Desde `backend/`, `.\gradlew.bat check` en sandbox: no llegó a ejecutar pruebas; acceso denegado al lock de la caché `~/.gradle/wrapper`. Se repitió con permisos ampliados para caché y Docker.
4. Base antes de añadir pruebas: `.\gradlew.bat check` → `BUILD SUCCESSFUL in 1m 2s`, **302 pruebas, 0 fallos, 0 omisiones**. JaCoCo: **895 líneas cubiertas / 957 totales = 93,52 %** (62 no cubiertas). `jacocoTestCoverageVerification` y ArchUnit aprobados.
5. Primera ejecución con cuatro pruebas nuevas: `.\gradlew.bat check` → `BUILD FAILED in 55s`, **306 pruebas, 1 fallo**. Único fallo: `MigracionesIT.reiniciarFlywayValidaLaMigracionSinReaplicarla`, expectativa `targetSchemaVersion == "1"`, valor real `null` cuando no hay migraciones nuevas. Las otras tres pruebas nuevas pasaron. Se corrigió exclusivamente la expectativa de la prueba: consultar `flyway.info().current()` y exigir una aplicada y cero pendientes. No fue un RED de negocio ni se presenta como defecto de producción.
6. Ejecución final después de corregir esa expectativa y exigir las señales de concurrencia: `.\gradlew.bat check` → **`BUILD SUCCESSFUL in 53s`**, **306 pruebas, 0 fallos, 0 errores, 0 omisiones**. Se ejecutaron compilación de pruebas, `test`, `jacocoTestCoverageVerification`, `check` y `jacocoTestReport`. JaCoCo: **895 cubiertas y 62 no cubiertas; 93,52 % de líneas**, superior al 80 % exigido, sin exclusiones añadidas. ArchUnit incluido en la ejecución.
7. Extracción de evidencia: `Get-ChildItem backend/build/test-results/test/TEST-*.xml` y lectura XML con `Get-Content` para sumar atributos `tests`, `failures`, `errors` y `skipped`; lectura de `backend/build/reports/jacoco/test/jacocoTestReport.xml`, contador `LINE`. Resultado: `tests=306 failures=0 errors=0 skipped=0`, `covered=895 missed=62`.
8. `git -c core.safecrlf=false diff --check --` seguido de los tres archivos de prueba y este informe: salida vacía, sin errores de espacios. Rama y HEAD conservados; sin staging, commit ni push.

Evidencia local generada por la ejecución final (Gradle puede reemplazarla en futuras ejecuciones): [resultados JUnit HTML](../../backend/build/reports/tests/test/index.html), [JaCoCo HTML](../../backend/build/reports/jacoco/test/html/index.html), [JaCoCo XML](../../backend/build/reports/jacoco/test/jacocoTestReport.xml). Las clases nuevas están dentro de los XML `TEST-com.decokasa.seguimiento.MigracionesIT.xml`, `TEST-com.decokasa.seguimiento.usuarios.adapter.in.web.SesionHttpIT.xml` y `TEST-com.decokasa.seguimiento.usuarios.config.DesactivarMientrasRenuevaIT.xml` en `backend/build/test-results/test/`.

No hubo cambios de producción ni ciclo RED/GREEN de una corrección de negocio: se añadieron pruebas de caracterización sobre implementación existente. No se fabricó un RED mediante cambios de producción. Las evidencias de la ejecución final reemplazan los reportes temporales de la ejecución fallida.

## Hallazgos y pendientes

- No se identificó un defecto de producción con los casos ejecutados. No hay un reproductor de defecto nuevo que devolver a Claude Code. El fallo de Flyway descrito arriba pertenecía a la prueba añadida; no requiere corrección de producción.
- La revisión Java se limita a las adiciones: SQL parametrizado, fixtures ficticios, recursos reutilizados, esperas acotadas, comprobación de resultados y estado persistido. No se modificaron las comprobaciones existentes ni la configuración de cobertura.
- El límite exacto de 8 h y los 15 min se prueban con reloj fijo en unidades; en HTTP/PostgreSQL se mueve el vencimiento del fixture para evitar esperas reales. Esto no verifica ocho horas de funcionamiento continuo.
- El versionado de roles sigue pendiente explícito de D10; no se impuso un requisito todavía no aprobado. Los pendientes históricos de seguridad del informe 1.7 no se consideran resueltos por estas pruebas.
- No se ejecutó 1.B2 ni se verificó el frontend o un despliegue. La fase 1 permanece abierta; su cierre corresponde a Richard después de los pasos restantes. Sin commit ni push.

## Revalidación por solicitud reiterada — 2026-10-14, 16:21 UTC−5

Se comprobaron nuevamente rama, cambios pendientes, manual DOCX, las actualizaciones de `CLAUDE.md`, decisiones del plan, contrato e informe del backend. Se revisaron los cuatro casos añadidos y las comprobaciones existentes de los cinco 401 seguidos del bloqueo 423. No se detectaron casos adicionales faltantes dentro del alcance solicitado; se conservaron todas las pruebas y el trabajo previo. En esta revalidación solo se amplía este informe.

| Comando desde `backend/` | Resultado nuevo |
| --- | --- |
| `.\gradlew.bat check` | `BUILD SUCCESSFUL in 1s`; siete tareas `UP-TO-DATE`. Esta invocación reutilizó resultados vigentes. |
| `.\gradlew.bat check --rerun-tasks` | **`BUILD SUCCESSFUL in 58s`; siete tareas ejecutadas**, incluidas compilación, pruebas y verificación de cobertura. PostgreSQL 17 mediante Testcontainers. |

La suma de los XML JUnit de esta ejecución nueva da **306 pruebas, cero fallos, cero errores y cero omisiones**. El contador `LINE` del XML JaCoCo regenerado confirma **895 cubiertas / 957 totales = 93,52 %**, sin cambiar el umbral del 80 %. Los enlaces a los reportes locales anteriores apuntan ahora a esta ejecución.

`git branch --show-current` confirma `fase-1-base-acceso`; `git rev-parse HEAD` conserva `0fe8fcc97bd723028ac873f85b5953628cf059b8`. `git diff --check` de las tres pruebas no produjo errores. No hubo nuevos bloqueos ni defectos de producción reproducibles; permanecen los límites y pendientes documentados arriba. No se modificó producción, no se borraron datos locales ni volúmenes, no hubo commit ni push, y no se avanzó a 1.B2 ni al cierre de fase.

## Paso 1.B2 — frontend y navegador, 2026-10-14

Esta sección continúa exclusivamente 1.B2; las conclusiones anteriores corresponden a 1.B1 y se conservan. Se releyeron el paso 1.B2 del DOCX, el traspaso de los pasos 1.4/1.5 en `CLAUDE.md` y el informe histórico 1.8. Skills: `ecc-react-test` y `playwright-best-practices`, con sus referencias de estructura, localizadores, esperas y varias pestañas. Rama: `fase-1-base-acceso`. No se modificó código ni configuración de producción por este encargo.

### Pruebas con Vitest, Testing Library y MSW

Se conservaron las suites existentes de formularios, rutas, sesiones, usuarios, roles, CSP y ausencia del demo. Se completaron siete archivos existentes y se añadieron dos suites para unidades sin suite previa (`main.test.tsx` y `ErrorRuta.test.tsx`). Total: **155 pruebas en 17 archivos, cero fallos y cero omisiones**; antes del encargo había 132 pruebas.

| Garantía añadida | Archivo bajo `frontend/src/` | Resultado con MSW / unidades |
| --- | --- | --- |
| Red caída produce `SIN_CONEXION`; JSON y esquema inválidos producen `RESPUESTA_INVALIDA` | `api/cliente.test.ts` | PASS |
| La configuración real de main reintenta red/503 dos veces (tres envíos totales), no 403/409/contrato inválido; mutaciones no se repiten | `main.test.tsx` | PASS; se captura el QueryClient de main sin montar la aplicación y solo se reduce `retryDelay` para la prueba |
| ErrorRuta presenta mensaje seguro para error inesperado, Response 404 y red, sin detalle técnico, con botón de recarga | `components/ErrorRuta.test.tsx` | PASS; no demuestra la navegación real del botón de recarga |
| Renovación temporal 0/503 conserva sesión y admite reintento; recuperación inicial sin red no inventa sesión; 401/403 olvida token | `auth/coordinadorSesion.test.ts` | PASS; dobles de canal/servidor, no API real |
| 401 seguido de renovación sin red informa el problema sin perder usuario ni redirigir a login | `auth/SesionProvider.test.tsx` | PASS, MSW |
| Login y guardado de rol sin red conservan formulario y no se reenvían solos | `components/LoginScreen.test.tsx`, `components/RolesManagementView.test.tsx` | PASS, MSW |
| Reaplicar conserva únicamente campos propios sobre versión vigente y datos remotos; avisa si ambos tocaron nombre; solo guarda por acción explícita | `components/UsersManagementView.test.tsx` | PASS en dos variantes; se conserva también el caso existente de descartar/cargar datos actuales |
| Consulta y asistente_china muestran Provisional y alcance pendiente, sin links de gestión | `components/dashboards/DashboardRol.test.tsx` | PASS en dos variantes; se conserva deduplicación de paneles y rutas existentes |

Comandos ejecutados desde `frontend/` con Node `C:/Users/Richard/AppData/Roaming/fnm/node-versions/v24.21.0/installation/node.exe`:

1. `node node_modules/vitest/vitest.mjs run`: sandbox bloqueó procesos nativos (`spawn EPERM`). Se repitió con permisos ampliados.
2. Ejecución inicial sin sobrescribir entorno: **97 pasan / 35 fallan de 132**. Se aisló con `run src/App.test.tsx` y `run src/App.test.tsx -t 'con sesión'`, reportes en `.tmp/b2-baseline-app.json` y `.tmp/b2-one.json`.
3. Se detectó una incorporación concurrente del acceso automático de desarrollo en `SesionProvider.tsx` y `.env.local` con `VITE_ACCESO_LOCAL=true` (fechas alrededor de 16:24). No se eliminó ni corrigió. `import.meta.env.DEV` lo activa también en Vitest; las pruebas de autenticación se ejecutaron con `$env:VITE_ACCESO_LOCAL='false'` en el proceso, sin cambiar `.env.local`.
4. `node node_modules/vitest/vitest.mjs run --reporter=json --outputFile=../.tmp/b2-vitest.json`: 140 pasan / 2 fallan; los dos fallos eran de fixtures nuevos de reaplicación que respondían al detalle cuando la pantalla recarga la lista. Se corrigió solo el fixture para modelar el endpoint real. No fue un defecto de producción ni un RED de negocio.
5. Mismo comando con `../.tmp/b2-vitest-final.json`: **145 pasan**. Tras ampliar reintentos/formularios: `../.tmp/b2-vitest-completo.json`, **153 pasan**.
6. `node node_modules/vitest/vitest.mjs run --coverage --reporter=json --outputFile=../.tmp/b2-vitest-cobertura.json`, con acceso local false: **155 pasan**, cero fallos/omisiones. Cobertura V8: líneas **93,31 % (461/494)**, ramas 82,65 %, funciones 89,04 %, sentencias 90,76 %. Son los archivos instrumentados por esta ejecución, incluidos fixtures: no es cobertura de todos los archivos del frontend. No se agregaron exclusiones ni umbrales para forzar un resultado. Reporte HTML: `frontend/coverage/index.html`.

Estos resultados prueban componentes y comportamiento simulado. No acreditan cookies HttpOnly, coordinación nativa, CSP del servidor ni el funcionamiento del backend.

### Preparación del recorrido real y conservación de datos

Docker inicialmente no tenía servicios activos. `docker compose ps` falló en sandbox por permisos del pipe; se repitió con permisos ampliados. `docker compose up -d --build --wait` compiló los artefactos actuales pero encontró el puerto 8080 reservado/no disponible en Windows. No se detuvo ningún proceso ajeno. Se usó un override solo de prueba: backend publicado en **18080**, frontend en el origen configurado **http://localhost:3000**, proxy interno sin cambiar.

Comando efectivo: `docker compose -f docker-compose.yml -f .tmp/b2-compose.yaml up -d --wait`. Se conserva un equivalente reproducible en `pruebas-navegador/puertos.compose.yaml`. Los tres servicios llegaron a healthy. El build de Docker fue preparación para el navegador; no se ejecutó la verificación general de 1.B3.

Se añadió `pruebas-navegador/expiracion.compose.yaml` y un nginx exclusivo de pruebas para un segundo origen **http://localhost:3001**, backend 18081. Mismo código y PostgreSQL existente, acceso de **3 s**, renovación absoluta de **120 s**, Origin permitido 3001 y semilla deshabilitada. Se eligieron 120 s en vez de 20 s para observar la tolerancia del validador JWT antes de que expire la renovación. No se cambió la configuración del servicio principal ni el reloj del cliente o del servidor.

Comando: `docker compose -f docker-compose.yml -f .tmp/b2-compose.yaml -f pruebas-navegador/expiracion.compose.yaml up -d --wait backend-expiracion frontend-expiracion`. Dos intentos iniciales fallaron al resolver `extends`; se corrigieron únicamente las rutas del Compose de pruebas. Ambos servicios auxiliares llegaron a healthy.

Las seis cuentas base ficticias ya existían y funcionaron sin modificar sus roles, contraseñas o datos. El caso de conflicto creó dos cuentas nuevas por ejecución (`b2admin…@ejemplo.test`, con admin+logistica, y `b2objetivo…@ejemplo.test`, marketing). Al terminar se desactivan solo esas cuentas creadas por la prueba con PATCH y su versión vigente; no se borran filas. Quedan conservadas como fixtures inactivos. No se cargaron datos reales, no se imprimieron secretos ni se borraron datos o volúmenes.

### Playwright con API real — separado de MSW

Suite nueva: `pruebas-navegador/fase-1.spec.mjs`; configuración y reproducción en `pruebas-navegador/README.md`. Se reutilizaron Playwright del runtime y Chrome ya instalados, sin modificar dependencias de producción. Sin mocks de respuestas, sin `skip`/`fixme`, cero reintentos automáticos, contextos aislados salvo las pestañas que comparten sesión. La interceptación del caso de identidad únicamente retiene el envío de un login; se continúa hacia el backend real.

Comando desde raíz, después de definir `PLAYWRIGHT_TEST_MODULE` y `B2_CHROME_PATH` como indica el README:

```powershell
node <runtime>/playwright/cli.js test --config pruebas-navegador/playwright.config.mjs
```

Primera ejecución: **8 aprobadas / 1 fallida en 16,4 s**. La fallida es el caso de identidad descrito abajo. Segunda ejecución con expiración parametrizada: **9 aprobadas / 1 fallida, cero omitidas, en 135,8 s**. La única fallida sigue siendo B2-D01.

Repetición de carreras: `node <runtime>/playwright/cli.js test --config pruebas-navegador/playwright.config.mjs --grep 'dos pestañas|login distinto' --repeat-each=5`, con `B2_REPORT=../.tmp/b2-repeticiones.json` y `B2_OUTPUT_DIR=../.tmp/b2-repeticiones`. Resultado: **cinco aprobadas del mismo usuario y cinco fallidas del cambio de identidad, en 19,5 s**, cero omitidas/flaky. B2-D01 se reprodujo en las cinco repeticiones adicionales; no fue un fallo intermitente de localizadores.

| Recorrido real | Evidencia |
| --- | --- |
| Seis usuarios ficticios: admin, marketing, revisión, China, logística y consulta | Los seis entran y ven nombre/panel correcto; consulta/asistente_china aparecen juntos como un solo panel provisional. Cada no-admin obtiene Sin permiso en `/usuarios`. |
| Cookie en localhost:3000 | HttpOnly=true, Secure=true, SameSite=Strict, Path=/api/v1/auth; no legible con document.cookie; storages vacíos. Evidencia de Chromium en localhost, no prueba un despliegue HTTPS remoto. |
| Recarga/cierre | Recarga llama al backend, renueva con 200 y cambia cookie; conserva identidad; cerrar elimina cookie y recarga posterior sigue en login. |
| Pestañas con mismo usuario | Dos recargas paralelas renuevan con 200/200; Web Locks/BroadcastChannel reales; cerrar en una lleva la otra al login. |
| Múltiples roles y permisos | Segundo admin ficticio con admin+logistica ve ambos paneles y administra; usuarios sin admin no ven links de gestión y la ruta muestra Sin permiso. |
| Conflicto entre administradores | Dos contextos y administradores diferentes editan el mismo objetivo ficticio; PATCH viejo devuelve 409. Cargar datos actuales muestra cambio remoto; segundo conflicto permite reaplicar, avisa del nombre coincidente y solo guarda tras pulsar Guardar. |
| Demo/CSP | Sin accesos rápidos, contraseña demo, simulador de roles o Acceso local en el build servido por nginx. Se verificó cabecera CSP y `securitypolicyviolation` vacío en los recorridos instrumentados; sin pageerror en los seis dashboards. No es la auditoría completa de seguridad de 1.B3. |
| Expiración real parametrizada | PASS en 121,9 s: configuración 3 s/120 s, primer 401 a los 62,914 s, renovación 200 tras ese 401, cookie distinta y mismo vencimiento absoluto (tolerancia de 1 s por Max-Age), límite agotado → renovación 401 y login. No equivale a esperar 15 min u ocho horas reales. |

### B2-D01 — cambio silencioso de identidad entre pestañas (reproducido)

**Resultado observado:** A muestra `Hola, Admin Prueba`; B inicia sesión como Marketing; A continúa mostrando Admin y conserva sus links. Tras recargar A, `/auth/renovar` usa la cookie compartida de B y A muestra `Hola, Marketing Prueba`. La prueba queda **en rojo**: esperaba conservar la identidad previa y recibió la otra. No se presenta este caso como aprobado ni se corrigió producción.

Pasos reproducibles para Claude:

1. En un mismo contexto de Chromium abrir dos pestañas anónimas en `http://localhost:3000/login`.
2. En B preparar `marketingprueba` y la contraseña ficticia. Demorar únicamente el envío de `POST /api/v1/auth/login` (por breakpoint de red o la puerta de Playwright).
3. En A iniciar sesión como `adminprueba` y esperar el dashboard Admin. Así B tiene un login en curso cuando llega el aviso de sesión iniciada.
4. Liberar el login real de B. B muestra Marketing. A sigue mostrando Admin.
5. Recargar A: cambia a Marketing sin una confirmación de cambio de identidad.
6. Reproductor automatizado: comando Playwright del README con `--grep 'login distinto'`.

Causa candidata para investigar, no corrección aplicada: `coordinadorSesion.conectar` solo atiende `iniciada` si su estado es anónimo. La cookie de renovación pertenece al origen y se reemplaza con el segundo login; `renovar` guarda la identidad devuelta sin contrastarla con la que estaba abierta. El vaciado de QueryClient solo ocurre al quedar anónimo. La secuencia demuestra incoherencia de identidad; no demuestra escalada de permisos en el backend.

**Política pendiente:** decidir explícitamente si un login distinto debe cerrar las demás sesiones de pestaña, sincronizar la identidad con aviso y limpiar cachés, o impedir el solapamiento. La expectativa de la regresión prohíbe la sustitución silenciosa; no impone una implementación concreta. Claude debe resolver el comportamiento acordado y después volver a ejecutar las pruebas. No convertir el fallo en verde aceptando sin más el usuario nuevo.

### Límites y pendientes de 1.B2

- Corregir B2-D01 en Claude y revalidar; el acceso automático de desarrollo se conservó como cambio concurrente y se aisló del entorno de pruebas mediante variable de proceso.
- Observación temporal para revisión: acceso configurado a 3 s fue aceptado por el servidor hasta aproximadamente 63 s. El código usa `JwtValidators.createDefaultWithIssuer`; el intervalo es compatible con una tolerancia de unos 60 s. No se cambió ni se presupone aprobado un requisito de tolerancia cero. La prueba mide la decisión real, no solo `exp`; revisar la tolerancia deseada al evaluar expiración de seguridad.
- Las duraciones normales de 15 min/8 h se comprobaron en backend 1.B1; en navegador se verificó la expiración real con parámetros reducidos. No se afirma un recorrido continuo de ocho horas ni un despliegue HTTPS remoto.
- No se ejecutaron lint, auditoría general de seguridad ni comprobaciones de 1.B3. No se realizó commit/push ni se marcó la fase completa.

### Evidencias conservadas y estado final

- [Resultados resumidos sin tokens](evidencias/1-b2/resultados.json): 155/0 Vitest, 9/1 Playwright, tiempos reales y repetición 5/5 de las carreras.
- [A antes de recargar: Admin](evidencias/1-b2/identidad-antes.png) y [A después de recargar: Marketing](evidencias/1-b2/identidad-despues.png). Capturas revisadas: cambian el nombre, el panel y los links de gestión sin intervención de login en A.
- Reportes completos locales: `.tmp/b2-vitest-cobertura.json`, `.tmp/b2-playwright.json`, `.tmp/b2-repeticiones.json`; contexto y capturas de fallos en `.tmp/b2-playwright/` y `.tmp/b2-repeticiones/`. Las trazas se desactivaron para evitar persistir credenciales/cookies en artefactos de red. La evidencia resumida conserva solo resultados, tiempos y nombres ficticios.
- `git diff --check` de los archivos de prueba modificados: sin errores. HEAD sigue en `0fe8fcc97bd723028ac873f85b5953628cf059b8`; todo el trabajo previo y concurrente se conserva. Las adiciones del encargo son pruebas, configuración auxiliar de pruebas, README e informe/evidencias.
- Se detuvieron únicamente `frontend-expiracion` y `backend-expiracion` con `docker compose ... stop frontend-expiracion backend-expiracion`; se conservaron los contenedores y datos. El entorno principal queda healthy: PostgreSQL 5432, backend 18080 y frontend 3000. No se ejecutó `down`, eliminación de contenedores, volúmenes o datos.
- **1.B2 tiene un defecto pendiente de corrección y revalidación; no está aprobado como totalmente verde.** No se avanzó a 1.B3 y la fase continúa abierta.

## Revalidación exclusiva de B2-D01 — 2026-10-14, 17:02 UTC−5

**Resultado: la corrección del defecto de identidad queda verificada en los casos descritos abajo.** No se reprodujo la sustitución silenciosa: la pestaña adopta la cuenta vigente con aviso antes de recargar, retira los permisos y datos anteriores y conserva la misma identidad tras recarga. Este resultado sustituye el estado pendiente de B2-D01 de las secciones históricas; no declara completa la fase ni extiende la revisión a 1.B3.

Se leyó el [traspaso de la corrección](../../.claude/PRPs/reports/fase-1-b2-identidad-traspaso.md), este informe y las evidencias anteriores. Se aplicaron `ecc-react-test` y `playwright-best-practices`: comprobaciones de comportamiento, localizadores accesibles, puertas explícitas y esperas por estado real, sin esperas arbitrarias para forzar carreras. Rama confirmada: `fase-1-base-acceso`. Se conservó todo el trabajo existente.

### Adaptación del reproductor: mismo defecto, sin bloqueo artificial

El montaje anterior retenía el POST de B mientras esperaba que A terminara su login. Con los logins serializados, B era dueño del Web Lock y A no podía terminar: ese timeout ya no demostraría un defecto de identidad.

El reproductor actualizado, dentro de la misma suite y con el mismo nombre, hace lo siguiente:

1. Abre A y B anónimas y prepara credenciales de Admin y Marketing.
2. Retiene primero el envío del login de A. Envía la intención de login de B mientras A está pendiente y comprueba con `navigator.locks.query()` que hay una solicitud esperando `decokasa-renovacion`.
3. Libera A y **solo después** retiene el envío de B cuando llega a la red. A ya puede mostrar Admin; no se espera a A mientras B impide su ejecución.
4. En A abre Usuarios, espera la lista real y abre un formulario con un borrador identificable. La caché y el formulario anteriores existen realmente antes del cambio.
5. Libera B. Antes de recargar exige Marketing en el encabezado, aviso explícito con el nombre vigente, ausencia de links de administración, filas y formulario anteriores, y Sin permiso en la ruta administrativa. Comprueba `/auth/yo` con el token recuperado y 403 de `/usuarios` con ese token.
6. Conserva la comprobación anterior de igualdad de identidad antes/después de recargar, y añade cierre compartido posterior y ausencia de tokens en storages/cookies legibles.

Se conserva el problema original: dos intenciones de login de usuarios diferentes se solapan en el mismo contexto, A ya tenía la identidad y los datos de Admin, y B cambia la cookie compartida. **No se debilitó el caso aceptando Marketing solamente después de recargar**: se exige sincronización comunicada antes de la recarga y se comprueba la retirada de la cuenta anterior. La evidencia roja original se conserva en `evidencias/1-b2/`; no se modificó producción para fabricar un contrafactual rojo.

Las interceptaciones solo retrasan envíos o la entrega de respuestas obtenidas con `route.fetch()` del backend real. No se inventaron estados 200/201/401, cuerpos ni cookies. En particular, el 401 de la escritura antigua se obtuvo esperando a que el servidor rechazara realmente su JWT.

### Regresiones con Vitest / Testing Library / MSW

Se completaron las suites existentes, sin duplicarlas:

- `auth/coordinadorSesion.test.ts`: aviso remoto invalida inmediatamente token y señal, cambia generación y comunica la recuperación; una renovación tardía no reabre después de cerrar; una renovación que descubre otra identidad sin aviso también invalida y comunica el cambio.
- `auth/SesionProvider.test.tsx`: caché de consultas y mutaciones vaciada antes de mostrar la cuenta nueva, borrador y formulario retirados, aviso con cuenta vigente y links anteriores ausentes.
- `test/renderizarApp.tsx`: devuelve también el QueryClient de prueba para observar la caché real del provider. No sustituye el provider ni cambia sus decisiones.

La regresión afectada inicial, antes de las adiciones de esta revalidación, aprobó **67 pruebas**. La ejecución final aprobó **81 pruebas en ocho archivos, cero fallos y cero omisiones**: coordinador, SesionProvider, RutaProtegida, LoginScreen, UsersManagementView, RolesManagementView, App y cliente HTTP. Incluye la renovación agrupada, 401/403, fallos temporales, cierre, formularios y permisos existentes. Son las regresiones seleccionadas; no se afirma haber ejecutado nuevamente toda la suite del frontend o medido cobertura nueva.

### Resultados con API real, separados de MSW

Se reconstruyó el frontend de los entornos locales para servir el código de la corrección, en lugar del artefacto anterior. Origen principal: `http://localhost:3000`, backend 18080. Origen auxiliar de expiración: `http://localhost:3001`, acceso configurado a 3 s y renovación a 120 s. Las dos pruebas temporales son recorridos reales con parámetros reducidos; no simulan quince minutos ni ocho horas de funcionamiento continuo.

| Caso real | Resultado y evidencia comprobada |
| --- | --- |
| Login distinto con caché y formulario previos | PASS. Marketing y aviso explícito antes de recargar; `/auth/yo` coincide y `/usuarios` da 403; se retiran formulario, filas y links anteriores. Identidad estable tras recarga. Cierre en B lleva A al login. Sin pageerror ni violaciones CSP en la transición instrumentada. |
| Caché caliente y peticiones con otra cuenta administradora | PASS. Dos fixtures ficticios nuevos. Se añade un dato a PostgreSQL después de calentar la caché de A; tras el cambio aparece ese dato sin conservar el borrador. Los GET reales de Usuarios y Roles de la aplicación llevan `sub` del JWT de la cuenta visible nueva. La identidad no se infiere solo de una etiqueta en pantalla. |
| Lectura tardía de Admin | PASS. Se retiene una respuesta 200 real de Usuarios, se cambia a Marketing y se libera. No reaparecen filas, links o identidad de Admin. La entrega se intenta después de la cancelación del cliente. |
| Escritura tardía confirmada | PASS. El servidor confirma 201 al crear un rol ficticio y se retiene su respuesta. Tras cambiar a Marketing y liberarla, no se restaura el diálogo, lista o identidad anteriores. Un solo envío. La escritura ya confirmada se conserva; no se confunde descartar una respuesta con revertir una transacción. |
| Operación antigua rechazada con 401 real | PASS. Se espera el rechazo real del JWT corto, se envía un POST de rol y se retiene el 401 real. Tras cambiar a Marketing se libera: un único POST con el sujeto Admin y **cero reenvíos con Marketing**, incluso tras navegación y recarga. No se fabricó un 401 ni se alteró el reloj. |
| Renovación y cierre compartidos | PASS. Dos recargas simultáneas obtienen 200/200, sin reutilización accidental de cookie. El cierre se propaga a la otra pestaña. |
| Expiración / renovación / recarga | PASS. El servidor rechaza el acceso, la aplicación renueva con 200 y rota cookie sin extender su límite absoluto. Al agotarse la renovación, recarga obtiene 401 y muestra login. |

Las seis pruebas de identidad, respuestas tardías, pestañas y expiración aprobaron en **214,99 s**. La comprobación adicional de caché y sujetos de peticiones aprobó en **8,56 s**. Total: **siete casos distintos reales aprobados**, cero fallos/omisiones en esas ejecuciones.

Se repitió el caso corregido **cinco veces**, junto con **cinco repeticiones** de renovación/cierre entre pestañas: **10/10 aprobadas en 40,5 s**, cero omitidas y cero flaky, sin reintentos automáticos. No se convirtió el fallo histórico en `skip`, `fixme` o resultado esperado.

### Comandos ejecutados y artefactos

Node utilizado: `C:/Users/Richard/AppData/Roaming/fnm/node-versions/v24.21.0/installation/node.exe`. Runner Playwright y Chrome: los mismos paths del README de `pruebas-navegador/`; no se instalaron paquetes ni se cambiaron dependencias.

| Comando / directorio | Resultado |
| --- | --- |
| `git branch --show-current`, `git status --short`, lectura del traspaso y fuentes / raíz | Rama correcta y trabajo existente conservado. |
| `node frontend/node_modules/vitest/vitest.mjs run --root frontend` con siete archivos afectados, `--reporter=json --outputFile=../.tmp/b2-identidad-base-vitest.json` / raíz; `VITE_ACCESO_LOCAL=false` | 67/67 antes de completar los casos. |
| `docker compose -f docker-compose.yml -f pruebas-navegador/puertos.compose.yaml up -d --build --no-deps --wait frontend` / raíz | Frontend corregido servido por nginx, healthy. El build es preparación del recorrido, no verificación general de 1.B3. |
| `node <runtime>/playwright/cli.js test --config pruebas-navegador/playwright.config.mjs --grep 'login distinto\|lectura tardía\|dos pestañas'` con `B2_REPORT=../.tmp/b2-identidad-real-inicial.json` | Primera comprobación: 3/3, 14,4 s. |
| `docker compose -f docker-compose.yml -f pruebas-navegador/puertos.compose.yaml -f pruebas-navegador/expiracion.compose.yaml up -d --build --wait backend-expiracion frontend-expiracion` | Auxiliares actualizados y healthy; se reutilizó la compilación del backend, sin cambiar sus fuentes. |
| `node node_modules/vitest/vitest.mjs run src/auth/coordinadorSesion.test.ts src/auth/SesionProvider.test.tsx src/auth/RutaProtegida.test.tsx src/components/LoginScreen.test.tsx src/components/UsersManagementView.test.tsx src/components/RolesManagementView.test.tsx src/App.test.tsx src/api/cliente.test.ts --reporter=json --outputFile=../.tmp/b2-identidad-vitest.json` / frontend; `VITE_ACCESO_LOCAL=false` | **81/81**, ocho archivos. |
| `node <runtime>/playwright/cli.js test --config pruebas-navegador/playwright.config.mjs --grep 'login distinto\|tardía\|operación antigua\|dos pestañas\|acceso vence'`; `B2_REPORT=../.tmp/b2-identidad-real.json`, `B2_OUTPUT_DIR=../.tmp/b2-identidad-real` | **6/6**, 214,99 s. |
| Mismo runner con `--grep 'caché caliente'`; `B2_REPORT=../.tmp/b2-identidad-cache.json`, `B2_OUTPUT_DIR=../.tmp/b2-identidad-cache` | **1/1**, 8,56 s. |
| Mismo runner con `--grep 'login distinto\|dos pestañas' --repeat-each=5`; `B2_REPORT=../.tmp/b2-identidad-repetida.json`, `B2_OUTPUT_DIR=../.tmp/b2-identidad-repetida` | **10/10**, 40,5 s. |
| `Get-FileHash -Algorithm SHA256` al inicio y al final sobre los cuatro archivos de producción de la corrección | Hashes idénticos en coordinador, provider, cliente y consultas. |
| `git diff --check --` sobre los archivos de pruebas modificados | Sin errores de espacios. |
| `docker compose` con los tres archivos de configuración anteriores y `stop frontend-expiracion backend-expiracion`, seguido de `docker compose ps` | Solo auxiliares detenidos; PostgreSQL, backend y frontend principales siguen healthy. Sin borrados. |

En la tabla, `\|` representa el carácter de alternativa `|` dentro del argumento entre comillas del filtro, escapado únicamente para que se muestre correctamente en Markdown. No es una tubería entre comandos.

Evidencia duradera: [resumen de ejecuciones, sujetos ficticios y hashes sin tokens](evidencias/1-b2-identidad-revalidacion/resultados.json) y [captura del cambio comunicado antes de recargar](evidencias/1-b2-identidad-revalidacion/cambio-comunicado.png). La captura se revisó: aviso de cambio y Marketing visibles, Sin permiso en la ruta previa de Usuarios y solo Inicio en el menú. Los JSON completos locales de cada comando permanecen en `.tmp/`, separados de los históricos.

### Conservación, hallazgos y pendientes

- No se detectó un nuevo defecto de producción en los casos ejecutados. B2-D01 ya no se reproduce con el montaje compatible con la serialización, incluidas cinco repeticiones.
- Cambios de este encargo: pruebas del coordinador y provider, ayuda de renderizado de pruebas, suite/README de navegador e informe/evidencias. **No se modificó producción.** Se conservan el traspaso, las pruebas previas y las evidencias históricas.
- La prueba de caché creó dos cuentas ficticias que se desactivaron con su versión vigente y se conservaron. La escritura confirmada dejó un rol ficticio sin asignar, identificado en el JSON de evidencia. La operación que recibió 401 no creó un rol. No se alteraron las seis cuentas base ni se eliminaron datos, contenedores o volúmenes.
- La evidencia de caché en memoria se comprueba directamente con QueryClient en MSW; la evidencia real añade caché calentada, dato posterior visible y GET de la aplicación con el sujeto nuevo. No se confunden ambos niveles.
- El fallback sin BroadcastChannel se ejercita en unidad mediante renovación que descubre otra identidad; no se afirma haber probado todos los navegadores o coordinación inmediata donde faltan Web Locks/BroadcastChannel. Los recorridos reales utilizaron Chromium con ambas APIs.
- Sin bloqueos pendientes de esta verificación. No se ejecutaron lint, auditoría de seguridad, verificación general ni otros pasos de 1.B3; sin commit/push. **La fase sigue abierta.**

## Paso 1.B3 — seguridad y verificación, 2026-10-15

**Verificación realizada; fase abierta para validación de Richard.** Alcance exclusivo: 1.B3 del manual DOCX, leído mediante OOXML con acceso compartido sin modificarlo. Se aplicaron `security-review`, `ecc-springboot-verification`, `ecc-verification-loop` y `ecc-test-coverage` (incluida su referencia original). Las instrucciones de no modificar producción prevalecen sobre las propuestas de corrección automática de las skills. No se añadieron pruebas: la cobertura global ya supera el mínimo. No se hizo commit, push ni trabajo de fase 2.

Rama comprobada: `fase-1-base-acceso`; HEAD `0fe8fcc97bd723028ac873f85b5953628cf059b8`. Se conservaron los cambios modificados, eliminados y sin seguimiento que ya existían. Esta ejecución solo amplía este informe; los builds generan sus artefactos habituales. Se leyeron íntegramente las secciones de 1.B1, 1.B2 y revalidación B2-D01, sus JSON de evidencia y el informe histórico 1.7. No se repitieron los recorridos ya aprobados. Los hashes actuales de coordinadorSesion, SesionProvider, cliente y consultas coinciden con los cuatro hashes de `evidencias/1-b2-identidad-revalidacion/resultados.json`; no hay evidencia que invalide esa revalidación.

### Comandos y resultados actuales e históricos

Comandos PowerShell sin perfil (`login: false`): el perfil intenta escribir enlaces fnm fuera del workspace. Para npm se antepuso al PATH `C:/Users/Richard/AppData/Roaming/fnm/node-versions/v24.21.0/installation`; `VITE_ACCESO_LOCAL=false` solo en el proceso de verificación, conservando los archivos locales.

| Directorio / comando | Resultado actual |
| --- | --- |
| Raíz: `git branch --show-current`, `git rev-parse HEAD`, `git status --short`, `git diff --stat` | Rama/HEAD anteriores; trabajo previo conservado. |
| frontend: `npm run lint` | PASS, código 0; ejecuta `tsc --noEmit`, comprobación de tipos, no ESLint. |
| frontend: `npm run test` | PASS, código 0; **159/159 pruebas, 17 archivos**, 21,70 s. Log local `.tmp/b3-frontend-test.log`. |
| frontend: `npm run build` | PASS, código 0; 498 ms. Log `.tmp/b3-frontend-build.log`. Aviso de chunk JS de **625,79 kB** (196,46 kB gzip), superior a 500 kB; no se modificó el empaquetado. |
| backend: `.\gradlew.bat check` | PASS, `BUILD SUCCESSFUL in 6s`, **7 tareas UP-TO-DATE**. Reutiliza resultados vigentes; no se presenta como una ejecución nueva de 306 pruebas. |
| Raíz: `Start-Process -FilePath 'C:/Users/Richard/AppData/Local/Programs/DockerDesktop/Docker Desktop.exe' -WindowStyle Hidden` | Docker estaba apagado; se inició con permiso ampliado. |
| Raíz: `docker compose -f docker-compose.yml -f pruebas-navegador/puertos.compose.yaml up -d --wait --no-build` y mismo prefijo con `ps --format json` | PASS; postgres, backend y frontend **running/healthy simultáneamente**, puertos 5432, 18080 y 3000. Se reutilizan imágenes existentes. Aviso de dos auxiliares de expiración presentes; se conservaron, sin `--remove-orphans`. |
| HTTP con `Invoke-WebRequest -UseBasicParsing` | GET `http://localhost:3000/` 200 con CSP; GET `/api/v1/salud` por el frontend 200 `estado=ok`; GET `http://localhost:18080/actuator/health` 200 `status=UP`. Comprueba frontend → backend → PostgreSQL (salud incluye DB). |
| OPTIONS `/api/v1/usuarios`, `Access-Control-Request-Method: GET`, `Access-Control-Request-Headers: Authorization` | Origin `http://localhost:3000`: 200, Allow-Origin exacto y Allow-Credentials=true. Origin `https://ajeno.ejemplo.test`: 403. |
| POST `/api/v1/auth/renovar` y `/api/v1/auth/cerrar-sesion`, sin Origin ni cookie | Ambos 403; la comprobación de Origin precede a la operación. No altera sesiones existentes. |

Primeros intentos en sandbox: lint pasó; test/build no arrancaron correctamente por `spawn EPERM` y carga nativa de Tailwind; Gradle no pudo abrir el lock de su caché. Se repitieron fuera del sandbox con autorización y pasaron. Docker falló primero por ausencia del daemon y luego por permiso del pipe; fuera del sandbox y tras iniciar Desktop se verificó correctamente. Son incidencias de entorno superadas, no defectos de producción.

Histórico conservado: 1.B1 ejecutó realmente **306 pruebas, 0 fallos/errores/omisiones**, el 14 de octubre a las 16:21, con PostgreSQL Testcontainers. La invocación actual de Gradle confirma que sus entradas siguen vigentes; no se forzó `--rerun-tasks`, respetando la instrucción de no repetir. 1.B2 midió 155 pruebas y cobertura frontend V8 de 93,31 % sobre los archivos instrumentados; después B2-D01 aprobó 81 regresiones seleccionadas. La suite completa actual tiene 159 pruebas. No se atribuye al comando actual una nueva medición de cobertura frontend.

### Cobertura backend y análisis de brechas

Lectura XML de `backend/build/test-results/test/TEST-*.xml`: **306 pruebas, failures=0, errors=0, skipped=0**. Reportes fechados el 14 de octubre, no regenerados hoy. Contador raíz LINE de `backend/build/reports/jacoco/test/jacocoTestReport.xml`: **895 cubiertas, 62 no cubiertas, 957 totales = 93,52 %**, PASS frente al 80 %. Ramas: 231/314 = 73,57 %; el umbral solicitado es de líneas. No se cambiaron exclusiones, umbral ni configuración.

Clases por debajo de 80 % de líneas: `SwaggerConfig` 0/16 (configuración del perfil dev), `SeguimientoApplication` 1/3 (arranque), `BCryptCodificador` 7/9 y `GeneradorTokenSeguro` 7/9. Son brechas documentadas; no se elimina código ni se añade cobertura superficial para mejorar el porcentaje. JaCoCo HTML/XML y JUnit HTML enlazados en 1.B1 siguen siendo las fuentes locales. ArchUnit forma parte de `check`. SpotBugs, PMD, Checkstyle y OWASP dependency-check no están configurados en build.gradle: no se presentan como aprobados. No se ejecutó una auditoría CVE actual ni una exploración exhaustiva del historial Git; no se afirma ausencia de vulnerabilidades de dependencias o secretos históricos.

### Permisos por endpoint y autorización vigente

Inspección de `SeguridadWebConfig`, los tres controladores y todos los casos de uso involucrados, contrastada con las pruebas ya aprobadas de 1.B1:

| Método y endpoint | Protección vigente / evidencia |
| --- | --- |
| GET `/api/v1/salud`, `/actuator/health` y subrutas health | Públicos, sin detalles de salud; exposición actuator limitada a health. Salud actual 200. |
| POST `/api/v1/auth/login` | Público, valida cuerpo y credenciales; cuenta activa y con roles, bloqueo D2. Pruebas AuthControllerIT/IniciarSesionTest vigentes. Login sin Origin sigue permitido por diseño; el CORS sí rechaza un Origin ajeno. |
| POST `/api/v1/auth/renovar` | Público en filtro; exige Origin exacto permitido y cookie válida; relee cuenta/roles, consume token atómicamente y rota con límite absoluto. SesionHttpIT/RenovacionConcurrenteIT/DesactivarMientrasRenuevaIT vigentes. |
| POST `/api/v1/auth/cerrar-sesion` | Exige Origin; revoca renovación y expira cookie; admite ausencia de cookie con Origin permitido. SesionHttpIT vigente. |
| GET `/api/v1/auth/yo` | JWT válido; ObtenerUsuarioActualCasoDeUso relee usuario activo y con roles, y permisos actuales; no confía solo en claims. |
| GET/POST `/api/v1/usuarios`, GET/PATCH `/api/v1/usuarios/{usuarioId}` | Filtro ROL_admin y autorización de admin activo **actual en base** en cada caso de uso; PATCH exige versión. UsuarioControllerIT y VersionUsuarioIT vigentes. |
| GET/POST `/api/v1/roles`, PUT `/api/v1/roles/{rolId}` | Mismo doble control de admin vigente. PUT sigue sin versión (M2 roles abierto). |
| GET Swagger y `/v3/api-docs` | Públicos únicamente con perfil dev; deshabilitados por defecto. SaludControllerTest comprueba que no son públicos sin dev. No se certifica un despliegue dev expuesto a Internet. |
| Otras rutas | `anyRequest().authenticated()`; no existe una ruta de negocio adicional pública declarada. |

`Autorizacion.exigirAdmin` consulta la base y exige `puedeIniciarSesion` y rol admin; los siete accesos de consulta/alta/edición lo invocan, incluido el constructor público de CrearUsuario. El acceso especial de carga inicial no lo expone un controlador. Un JWT antiguo con admin no permite administrar tras desactivación o retirada del rol (A1); el caso con varios roles incluyendo admin está probado. No se amplían permisos de asistente_china/consulta, todavía pendientes del cliente.

### CORS, Origin, CSP, cookies y secretos

- CORS: lista explícita de orígenes por variable, credenciales habilitadas, cabeceras Authorization/Content-Type y métodos limitados. No se encontró wildcard en la configuración revisada. Origin ausente o no permitido se rechaza en renovar/cerrar; las pruebas existentes cubren origen ajeno sin tocar sesión. CSRF de Spring está deshabilitado: las operaciones Bearer y las dos rutas con cookie siguen el diseño Origin + SameSite aprobado.
- Cookie de renovación: código `HttpOnly`, `Secure`, `SameSite=Strict`, Path `/api/v1/auth`, sin Domain y Max-Age por tiempo restante; cierre usa Max-Age=0. Rotación/reutilización/límite absoluto acreditados en 1.B1. Chromium localhost lo acreditó en 1.B2; **no equivale a HTTPS remoto**. Token de acceso solo en memoria según revisión y pruebas; sin persistencia en storages.
- CSP actual de nginx: default/script/connect/font self; object none, base/form self y frame-ancestors none. Sin unsafe-eval ni unsafe-inline para scripts. `style-src 'unsafe-inline'` sigue como excepción de compatibilidad documentada para Sonner/Radix, sin plan de retirada implementado. La API JSON no lleva CSP; no se confunde con ausencia de CSP en el documento HTML. M4 tiene evidencia de corrección en frontend. HSTS sigue pendiente (B4).
- Secretos: application.yml usa variables sin valor secreto de respaldo; JWT exige al menos 32 bytes. `.gitignore` excluye `.env*` salvo ejemplos y semillas `*.local.json`; `git ls-files` de `.env`, ambos `.env.local` y semillas locales no devolvió archivos. Escaneo de fuentes main Java/YAML y TS/TSX por claves privadas, formatos de tokens y asignaciones literales sospechosas: **0 coincidencias**; no se imprimieron valores. Búsqueda de System.out, console.log, HTML peligroso y escrituras en storages en esas fuentes: sin coincidencias. Este escaneo por patrones no certifica todo el historial ni la entropía/rotación de secretos desplegados. No se leyeron valores de `.env` ni se imprimieron cookies/tokens.

### Hallazgos pendientes y reproducción para Claude Code

No se encontró un defecto crítico o alto nuevo en el alcance revisado. Esto no constituye una auditoría de penetración completa. A1/A2/A4/N1 y M2 usuarios conservan la evidencia verde de 1.B1; A3, M4 y B2-D01 conservan la evidencia de navegador y regresiones, sin declararlos nuevamente corregidos por esta inspección.

| Hallazgo | Evaluación actual / reproducción segura para Claude Code |
| --- | --- |
| M1, medio: sin límite por IP | Abierto: no hay filtro de rate limiting en la cadena revisada. Reproducir en entorno aislado con una cuenta ficticia: cinco contraseñas erróneas desde un cliente bloquean 15 min la cuenta; no hay cuota por IP previa a BCrypt. No se volvió a bloquear ninguna cuenta local. La cuota concreta sigue propuesta, no se implementa. |
| M2 roles, medio | Abierto y pendiente explícito D10. Dos administradores leen el mismo rol ficticio; A hace PUT cambiando nombre; B hace PUT con su copia anterior y otros permisos: no se envía ni compara versión y prevalece el último guardado. `ActualizarRolCasoDeUso` y el contrato actual sustentan la reproducción; no se mutaron roles para repetirla. Requiere contrato/migración acordados. |
| M3, medio | Abierto: carga inicial toma una contraseña temporal común por configuración y no hay flujo obligatorio de cambio. Reproducir en base efímera con dos fixtures y un valor temporal de prueba: ambos acceden sin cambio obligatorio. No se muestran valores ni se reutilizan credenciales reales. |
| B1, bajo | Abierto: no hay invariante de último admin en actualización. Reproducir solo en base efímera: único admin se desactiva o pierde admin mediante PATCH con versión vigente; queda sin administración. No se ejecutó en la base compartida. |
| B2, bajo | Sigue pendiente el registro de eventos de seguridad; no hay evidencia nueva de implementación. Confirmar en entorno aislado eventos de login fallido/bloqueo/renovación reutilizada sin registrar secretos. |
| B3, bajo | Sigue pendiente aud/jti y tolerancia de reloj: no se observa su incorporación en configuración revisada. El límite de 15 min nominal no elimina el margen histórico de 60 s probado en B2. |
| B4, bajo | HSTS no configurado; entorno local HTTP no acredita HTTPS de producción. Resolver/verificar en despliegue de fase 8 según decisión histórica. |
| I1 / I2, informativos | 423 revela existencia (D2 aprobado); cerrar revoca renovación, pero JWT válido conserva su vida restante nominal y tolerancia. No se marcan corregidos; autorización administrativa sigue consultando estado actual. |
| Aviso build | Chunk JS >500 kB; reproducción `npm run build`. No impide el build ni demuestra por sí solo degradación medida. |

Estos reproductores documentan defectos/riesgos pendientes para Claude Code; las verificaciones destructivas quedan limitadas a un entorno efímero futuro. No se envió ningún mensaje externo ni se modificó producción para corregirlos.

### Evidencias de seis perfiles, China y checklist de salida

Se inspeccionaron los seis casos `dashboard de ...` en [resultados de 1.B2](evidencias/1-b2/resultados.json): **adminprueba, marketingprueba, revisionprueba, chinaprueba, logisticaprueba y consultaprueba**, todos `passed` (1148/1025/957/1001/980/948 ms). Incluyen cookie, recarga y cierre, panel/nombre correctos y rechazo de Usuarios para no-admin. El caso China es un **perfil ficticio local**. Consulta/asistente_china comparte panel provisional. El fallo histórico de identidad del mismo JSON queda sustituido únicamente por la revalidación específica B2-D01 (siete casos y diez repeticiones aprobadas), cuyos cuatro hashes siguen coincidiendo; no se oculta el rojo histórico.

- [x] Rama correcta; cambios previos conservados; sin commit/push ni avance de fase.
- [x] Frontend lint/tipos, suite completa y build actuales en verde.
- [x] Backend check en verde reutilizando resultados vigentes; 306 pruebas históricas válidas, cobertura 93,52 % ≥80 %.
- [x] Tres servicios healthy conjuntamente; comprobación HTTP por proxy y salud DB.
- [x] Permisos de cada endpoint, autorización vigente, CORS/Origin/CSP/cookies revisados; escaneo acotado de secretos sin coincidencias.
- [x] Evidencias locales de los seis perfiles comprobadas; B2-D01 con revalidación preservada.
- [x] Hallazgos y reproductores documentados sin atribuir correcciones sin evidencia; sin críticos/altos nuevos identificados en este alcance.
- [ ] Acceso **desde China**: pendiente. No hay navegador/red situados en China ni URL HTTPS remota apropiada aportada. Requiere recorrido real desde esa red con fecha, origen y resultado, sin credenciales en evidencia. No se simula con geolocalización ni con el perfil chinaprueba.
- [ ] Verificación remota HTTPS/cookies/HSTS y auditoría CVE/historial exhaustiva: no acreditadas por esta ejecución local; límites expresos, sin declararlas aprobadas.
- [ ] Resolución o aceptación explícita de los hallazgos pendientes por quien corresponda; ninguna propuesta se convierte en aprobación.
- [ ] **Richard valida el cierre de fase 1. La fase no se marca completa.**

No se borraron datos, contenedores ni volúmenes; los tres servicios principales permanecen funcionando. La falta inicial de Docker quedó resuelta. El bloqueo externo que permanece es la comprobación real desde China.

## Validación de Richard — cierre de desarrollo de fase 1, 2026-10-15

**Estado vigente: fase 1 cerrada en desarrollo por validación expresa de Richard.** Su instrucción en esta conversación fue: «Valido los resultados de la fase 1 para su cierre de desarrollo». Esta sección actualiza las referencias históricas a fase abierta y el último punto pendiente del checklist: la validación de Richard para desarrollo ya se recibió. Los demás pendientes conservan su estado.

La validación **no autoriza producción ni el uso de cuentas reales con contraseña temporal compartida**. Tampoco declara corregidos ni acepta para producción los hallazgos abiertos. Se actualizó exclusivamente el estado de fase 1 en el [PRD](../../.claude/PRPs/prds/decokasa.prd.md); no se declara terminado el proyecto ni se cambia el estado de las otras fases.

Base de la validación: resultados y comandos de 1.B1/1.B2/1.B3 conservados arriba; frontend lint, 159 pruebas y build aprobados; backend check aprobado reutilizando resultados vigentes de 306 pruebas, 0 fallos/errores/omisiones, y 895/957 líneas cubiertas (93,52 %). Gradle declaró sus siete tareas UP-TO-DATE: no se presenta esa comprobación como ejecución nueva. Evidencias duraderas: [seis perfiles ficticios e histórico](evidencias/1-b2/resultados.json) y [revalidación de B2-D01 con hashes](evidencias/1-b2-identidad-revalidacion/resultados.json). JUnit y JaCoCo locales están referenciados en 1.B1. Los tres servicios Docker se comprobaron healthy conjuntamente en 1.B3. No se ejecutaron pruebas nuevas para registrar el cierre.

### Hallazgos que se conservan y destino

Los detalles, impactos y reproductores de la tabla 1.B3 permanecen vigentes. No quedan críticos ni altos abiertos identificados dentro de ese alcance; esta afirmación no sustituye las auditorías no realizadas.

| Pendiente | Gravedad conservada | Destino / condición pendiente |
| --- | --- | --- |
| M1: límite por IP | Media | Backlog de seguridad/identidad para Claude Code; acordar política antes de implementar y verificar en entorno aislado. |
| M2: versionado de roles | Media | Backlog de identidad para Claude Code; decisión D10 pendiente, contrato y migración propios antes de implementación. |
| M3: contraseña temporal compartida | Media | Backlog de identidad para Claude Code; resolver provisión segura/cambio de contraseña y verificarlo **antes de usar cuentas reales**. La validación no autoriza ese uso. |
| B1: último administrador | Baja | Backlog de seguridad/identidad; acordar y probar la protección sin poner en riesgo la base compartida. |
| B2: registro de eventos de seguridad | Baja | Backlog de auditoría de seguridad; definición e implementación pendientes, sin secretos en registros. |
| B3: aud/jti y tolerancia temporal | Baja | Backlog de seguridad de sesión; decisión de contrato JWT y pruebas pendientes. |
| B4: HSTS | Baja | Seguridad del despliegue remoto de fase 8; verificar sobre HTTPS real. |
| I1: 423 revela existencia | Informativa | Seguimiento de diseño D2 aprobado; cualquier cambio requiere decisión expresa. |
| I2: vigencia residual del JWT al cerrar | Informativa | Seguimiento de diseño de sesión; conservar el límite y la autorización vigente, sin declarar revocación inmediata del acceso. |
| Aviso de archivo JS >500 kB | Aviso de rendimiento, sin gravedad de seguridad asignada | Backlog de rendimiento frontend; medir impacto y evaluar división del bundle. |

Los destinos anteriores registran seguimiento; no crean una autorización para implementar cambios ahora ni una aprobación de riesgos en producción. Los elementos que requerían decisión siguen requiriéndola.

### Verificaciones que continúan pendientes

- **Acceso real desde China:** pendiente hasta disponer de red/navegador situados allí y un entorno remoto apropiado. El perfil ficticio chinaprueba solo acredita acceso local por rol.
- **Seguridad del despliegue remoto:** pendiente comprobar HTTPS, cookies en ese origen, HSTS y configuración efectiva del entorno. Localhost no certifica producción.
- **Comprobaciones no realizadas:** auditoría CVE actual e historial exhaustivo de secretos; SpotBugs, PMD, Checkstyle y OWASP dependency-check no configurados. No se consideran aprobados.
- Se conservan los límites ya descritos: cobertura frontend histórica sobre archivos instrumentados; clases backend con cobertura inferior al 80 % individual aunque el global cumple; recorridos Chromium con Web Locks/BroadcastChannel, sin certificación de todos los navegadores; duraciones reducidas en recorridos temporales, sin acreditar ocho horas de ejecución continua.
- **Producción:** sin autorización. El proyecto completo y sus fases restantes siguen pendientes de sus propias implementaciones y validaciones.

Cambios de este registro: únicamente este informe y el PRD. Se conserva todo el trabajo previo. Sin git add, commit, push o merge; Richard preparará manualmente el commit y el push.

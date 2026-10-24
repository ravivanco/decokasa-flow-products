# Auditoría de implementación de Decokasa

Fecha: 13 de octubre de 2026. Rama revisada: `fase-1-base-acceso`.

**Dictamen: existe una base técnica sólida de usuarios y autenticación en el backend, pero la aplicación completa todavía no está preparada para operar con datos reales. El frontend servido sigue siendo un demo y no utiliza esa autenticación.**

## 1. Alcance y método

Se recorrió la carpeta del proyecto y se revisaron estructura, código propio, pruebas, migraciones, configuración, Docker, contrato y documentación técnica. La lectura detallada se concentró en los puntos de entrada, sesión, autorización, casos de uso, persistencia y componentes con lógica de negocio. El inventario incluye también documentación, recursos y herramientas de agentes.

No se revisó línea por línea el código de terceros de `node_modules`, los binarios de Gradle, las imágenes, los documentos Word ni los archivos generados. No se inspeccionó el contenido de `.env` ni se reprodujeron secretos. Los documentos Markdown se usaron como referencia; los Word no se renderizaron. No se realizó una auditoría visual en navegador, un pentest externo ni una consulta actual de vulnerabilidades de dependencias o de visibilidad de GitHub.

La revisión incluye los cambios locales existentes, también archivos sin seguimiento en Git. Se preservó ese trabajo. Solo se agregó este informe; las verificaciones regeneraron artefactos de build y pruebas.

## 2. Cómo está implementado

| Área | Implementación actual | Evaluación |
| --- | --- | --- |
| Frontend | React 19, Vite, TypeScript estricto y Tailwind 4 | Base instalada; interfaz principal heredada del demo |
| Navegación | `main.tsx` monta `App`; `App` cambia pantallas con `currentModule` | React Router está instalado, pero no gobierna la aplicación abierta |
| Datos del navegador | Pedidos, bodegas, correos y usuario con `useState` y `initialData` | Sin persistencia ni sincronización entre usuarios |
| Cliente API | `api/cliente.ts` y esquemas Zod | Preparados, pero no conectados al recorrido principal |
| Sesión del navegador | `SesionProvider` devuelve sus hijos; rutas sin componentes | Implementación pendiente |
| Backend | Spring Boot 4.1.1, Java 21 y Gradle | Monolito con separación hexagonal efectiva en usuarios |
| Dominio y aplicación | Java sin dependencias de Spring/JPA; interfaces de puertos | Buena separación y pruebas de arquitectura |
| Persistencia | PostgreSQL 17, JPA y Flyway | Implementados usuarios, roles, países y renovación |
| Seguridad API | JWT HS256, BCrypt, CORS, validación de Origin y refresh con cookie | Implementada y ampliamente probada, con pendientes descritos abajo |
| Negocio de importaciones | Motor de etapas y fechas en TypeScript | Demostración; todavía no hay módulos backend de pedidos/flujos/plazos |
| Drive y Gmail | Enlaces y notificaciones construidos en memoria | Integraciones reales pendientes |
| Despliegue | Compose: PostgreSQL, backend y nginx para frontend | Entorno local funcional; falta preparación operacional para producción |

### Flujo actual de la interfaz

`main.tsx → App.tsx → usuario demo administrador → pantallas por estado → funciones de orderState → datos en memoria`.

El cliente HTTP y las rutas nuevas no forman parte de ese flujo. Instalar React Router, TanStack Query, React Hook Form o shadcn no equivale a haber migrado la aplicación a esas herramientas.

### Flujo implementado en el backend

`Controlador HTTP → puerto de entrada → transacción en config → caso de uso → dominio y puertos de salida → adaptadores JPA/BCrypt/JWT → PostgreSQL`.

El módulo `usuarios` tiene altas, consulta, edición, activación/desactivación, roles, login, consulta del usuario actual, renovación y cierre de sesión. La migración V1 crea siete tablas: `pais`, `rol`, `rol_permiso`, `usuario`, `usuario_rol`, `usuario_pais` y `token_renovacion`.

## 3. Evidencia de ejecución

| Comprobación ejecutada | Resultado |
| --- | --- |
| `npm run lint` | Falla: cinco diagnósticos de TypeScript |
| `npm run test` | Falla: 11 archivos de pruebas fallidos y 4 aprobados, según la caché de la ejecución |
| `npm run build` | Pasa; JS principal de 420,24 kB, 112,52 kB gzip; CSS de 91,32 kB |
| `gradlew.bat check --offline --rerun-tasks` | Pasa: 298 pruebas, cero fallos/errores |
| JaCoCo de la ejecución | 95,30 % de cobertura de líneas; pasa el umbral de 80 % |
| `docker compose ps --format json` | PostgreSQL, backend y frontend en ejecución y saludables |

Vite y Gradle fueron bloqueados inicialmente por los permisos del sandbox; las ejecuciones fuera de este permitieron comprobar los resultados reales. Esos bloqueos iniciales no se atribuyen al código.

Los errores de TypeScript incluyen el módulo inexistente `auth/coordinadorSesion`, tres parámetros sin tipo derivados de ese archivo y un argumento `unknown` incompatible con `HttpResponse.json` en `SesionProvider.test.tsx:113`.

Los fallos del frontend afectan sesión, rutas protegidas, login, gestión de usuarios y roles, dashboards por rol, retiro del demo, CSP y una prueba del cliente HTTP. Hay pruebas escritas antes de la implementación, como indica el plan: eso explica parte del rojo, pero la fase todavía no cumple su puerta de salida.

El build pasa porque el script usa solamente `vite build`: no ejecuta `tsc` ni pruebas. Su éxito no demuestra que la aplicación cumpla el contrato o la autenticación.

Los contenedores ya existían y no se reconstruyeron. Su estado saludable demuestra disponibilidad, pero no garantiza que contengan los cambios locales actuales ni la corrección de todos los flujos.

## 4. Hallazgos prioritarios

Severidad referida a un uso con datos reales. Las capacidades planificadas para fases posteriores se distinguen de defectos del código existente.

### H01 — Bloqueante: frontend con identidad demo y sin autenticación real

Evidencia: `frontend/src/App.tsx:49` inicia `currentUser` con `USUARIOS_DEMO[0]`; `App.tsx:439` abre el simulador y `App.tsx:470` cambia el usuario. `LoginScreen` compara credenciales locales; `UsersManagementView` conserva cambio de sesión.

Cualquier visitante obtiene la experiencia de administrador del demo y puede representar otros roles. Esto **no demuestra un bypass de los endpoints protegidos del backend**, que sí exigen JWT y autorización: demuestra que la interfaz desplegada no tiene identidad real y presenta operaciones simuladas como parte del producto.

Acción: completar el paso 1.8, montar router y sesión desde `main`, conectar login/renovación/cierre a la API y eliminar todos los accesos demo del código de producción.

### H02 — Bloqueante: pedidos, permisos e historial solo en el navegador

Evidencia: estados de `App.tsx`, `utils/orderState.ts`, `utils/permissions.ts` y ausencia de módulos backend de pedidos/flujos/plazos.

Una recarga elimina los cambios; dos usuarios no comparten estado. El historial puede construirse con datos del cliente y no constituye una auditoría confiable. No hay control concurrente ni aislamiento de pedidos por país en servidor.

Acción: implementar las fases de negocio con transacciones, autorización en cada acción, persistencia e historial escrito por el servidor. Es alcance pendiente del plan, no una regresión del módulo usuarios.

### H03 — Alto: mensajes de Drive y Gmail sin integración real

Evidencia: `App.tsx:228` anuncia una subida exitosa; `StagePanel.tsx:274-281` fabrica nombre PDF y tamaño de 8,4 MB; `App` construye correos con destinatario y horas fijas.

El usuario puede interpretar que un archivo está respaldado o que un aviso se envió cuando solo se modificó memoria.

Acción: integrar adaptadores de archivos/correo y mostrar estados reales de pendiente, éxito y error. Hasta entonces, identificar claramente la demostración.

### H04 — Alto: se pueden renombrar códigos esenciales de roles

Evidencia: `ActualizarRolCasoDeUso.java:34` reemplaza el código recibido; `Autorizacion` y `SeguridadWebConfig` reconocen literalmente `admin`.

Si se cambia el código del rol administrador, las asignaciones siguen vinculadas al mismo ID, pero el rol deja de reconocerse como `admin`. Puede perderse toda capacidad administrativa. También es posible retirar el rol o desactivar al último administrador mediante la edición de usuarios: no aparece una regla que preserve al menos uno.

Acción: definir códigos de sistema inmutables y una regla transaccional que preserve un administrador activo. Esta consecuencia se deduce del código; no se ejecutaron cambios sobre usuarios reales.

### H05 — Medio: roles sin control de concurrencia y duplicados incompletamente tratados

Evidencia: `RolEntity` carece de `@Version`; el PUT no exige versión; `RolJpaRepositorio.guardar` usa `save` sin la traducción de restricciones que sí existe para usuarios.

Dos ediciones pueden sobrescribirse. Dos altas o cambios concurrentes al mismo código pueden pasar la comprobación previa y terminar en la restricción única, con un 500 genérico en lugar de 409 de dominio.

Acción: extender a roles el control de versiones y la traducción específica de duplicados; probar las carreras con PostgreSQL. La carrera de roles no fue reproducida en esta auditoría.

### H06 — Alto antes de exposición pública: sin límite de solicitudes por IP

Evidencia: seguridad y login implementan bloqueo por cuenta, pero no un limitador de peticiones por IP/origen.

El bloqueo tras cinco fallos no limita el volumen contra identificadores inexistentes; esas solicitudes igualmente calculan BCrypt. Permite abuso de recursos y bloqueo intencional de cuentas conocidas.

Acción: aplicar límites al login y renovación en el servidor o proxy, con respuestas y pruebas definidas. No se realizaron pruebas de carga ni ataques.

### H07 — Medio: semilla con contraseña común y carga parcial

Evidencia: `CargadorUsuariosIniciales.java:59-63` aplica una sola contraseña temporal; `UsuariosConfig` ejecuta una transacción por alta; el cargador omite toda carga si ya hay algún usuario.

Si falla una fila intermedia, las anteriores quedan confirmadas y el siguiente arranque omite el resto. Además, no hay flujo implementado para obligar a cambiar la contraseña temporal común.

Acción: validar el archivo completo antes de persistir, elegir atomicidad o reanudación idempotente y diseñar credenciales iniciales individuales/cambio obligatorio.

### H08 — Medio: CSP del frontend pendiente

Evidencia: `frontend/nginx.conf` incluye algunas cabeceras, pero no `Content-Security-Policy`; nueve pruebas de CSP fallan.

Falta una defensa adicional frente a ejecución de scripts inyectados. No se encontró ni se demostró una vulnerabilidad XSS explotable con esta revisión.

Acción: definir CSP compatible con los recursos utilizados y verificarla en el navegador sobre el build servido por nginx.

### H09 — Medio: renovación compartida entre pestañas sin implementar

Evidencia: existe `coordinadorSesion.test.ts`, pero falta `coordinadorSesion.ts`; el proveedor de sesión es un esqueleto. El backend revoca la familia ante reutilización del token.

Sin coordinación, dos renovaciones simultáneas pueden consumir la misma cookie y cerrar una sesión legítima. El backend sí prueba su consumo atómico: falta la parte del navegador.

Acción: implementar una renovación compartida por solicitudes y pestañas, incluyendo recuperación de errores y cierre de sesión coordinado.

### H10 — Medio: defectos de estado y acciones duplicadas en el demo

Evidencia: `OrderDetail.tsx:70` copia la etapa inicial en un estado que no sigue automáticamente los cambios del pedido; `TrackingSearchView` guarda una copia de pedido; `App.tsx:311` accede a `pedidos[0].id` sin comprobar la lista. `StagePanel.tsx:310` asigna bodega al cambiar el select y `:322` vuelve a hacerlo al confirmar.

Puede mostrarse información vieja, fallar la lista vacía o registrar dos veces una asignación.

Acción: derivar datos desde IDs actuales, delimitar estado por pedido/etapa y confirmar la bodega una sola vez. Reproducir los casos al migrar los componentes.

### H11 — Medio: fechas simuladas y calendario incompleto

Evidencia: `dateUtils.ts:3` fija hoy en 2026-10-09; `isBusinessDay` solo excluye sábado y domingo. Los eventos del demo usan horas fijas.

El semáforo no refleja la fecha real y no existe soporte de feriados en ese cálculo.

Acción: trasladar plazos al módulo backend previsto, usar reloj del servidor y definir calendario Ecuador según requisitos aprobados. El cliente debería formatear fechas, no decidir vencimientos oficiales.

### H12 — Medio: calidad y accesibilidad de la interfaz sin migrar

Evidencia: `Toast.tsx` no tiene región viva; `TrackingSearchView` usa un input sin etiqueta asociada; siguen modales manuales y pestañas sin el sistema accesible de componentes. `StagePanel` tiene 726 líneas, `OrdersListView` 491 y `OrderDetail` 465. Persisten colores literales y textos de 10/11 px.

Los componentes de shadcn y tokens existen, pero gran parte del demo no los utiliza. `lint` solo comprueba tipos; no hay ESLint para hooks o accesibilidad.

Acción: migrar progresivamente a componentes comunes, asociar etiquetas, gestionar foco/teclado y anunciar avisos. Se trata de hallazgos de código; no se midió conformidad WCAG ni apariencia visual en navegador.

### H13 — Medio: consultas sin paginación y con lecturas adicionales por usuario

Evidencia: `UsuarioJpaRepositorio.java:83` carga todos los usuarios; `:137` consulta roles al convertir cada usuario; el filtro activo se realiza en el caso de uso sobre la lista completa.

El coste aumenta con el número de usuarios, y las colecciones pueden generar consultas adicionales. No se midió rendimiento.

Acción: filtrar/paginar en base de datos y revisar el plan de carga con métricas SQL antes de ampliar el volumen. Para una plantilla pequeña puede ser aceptable temporalmente.

### H14 — Medio: verificaciones no obligatorias al construir y sin CI en el repositorio

Evidencia: no hay carpeta `.github/workflows`; Docker del frontend solo ejecuta build; Docker del backend excluye tests; `npm run build` no valida tipos.

Se puede construir una imagen mientras el frontend está en rojo. Separar pruebas de Docker es razonable, siempre que otra puerta de integración las exija; esa puerta no está definida en el repositorio revisado.

Acción: hacer obligatorios tipos, pruebas y `gradlew check` antes de integrar. Añadir pruebas de recorrido navegador/API para login, sesión y gestión; MSW no verifica cookies reales ni la integración con nginx.

### H15 — Medio para producción: operación y documentación por completar

Compose publica frontend en todas las interfaces (`3000:80`), mientras base y API están limitadas a loopback. El entorno revisado sirve HTTP y no define TLS, backups/restauración, rotación de secretos, retención de tokens ni monitorización operacional. Los tags de imágenes son móviles y no hay bloqueo de dependencias Gradle configurado.

La cookie del backend es siempre `Secure`: debe probarse con el origen y HTTPS definitivos. El localhost de desarrollo no basta como validación del comportamiento en un dominio real.

`CLAUDE.md` contiene descripciones históricas incompatibles entre sí (backend inexistente/esqueleto junto con paso 1.7 terminado). La auditoría anterior conserva afirmaciones que han cambiado: hay `strict`, algunas cabeceras y un cliente con `import.meta.env`.

Acción: definir despliegue de producción, recuperación y mantenimiento; limitar el puerto local si no se necesita acceso de red; actualizar documentación con estado implementado y pendiente. No se recomienda cambiar versiones basándose únicamente en esta auditoría, que no verificó avisos externos.

## 5. Lo que está bien resuelto

- Dominio y casos de uso desacoplados de frameworks, con siete reglas ArchUnit ejecutadas.
- Flyway controla el esquema y Hibernate valida; `open-in-view` está deshabilitado.
- Contraseñas con BCrypt y límite de bytes; tokens de renovación almacenados como hash.
- Refresh con rotación, consumo atómico y vencimiento absoluto; cookie HttpOnly, Secure y SameSite Strict.
- Gestión administrativa revalida usuario activo y rol actual en base de datos.
- Edición de usuarios con versión; conflictos y duplicados específicos traducidos a errores de dominio.
- Pruebas de concurrencia para intentos de login, renovación, edición y desactivación.
- Transacciones fuera del núcleo y confirmación deliberada de contadores/revocaciones ante ciertos errores.
- Errores HTTP estructurados y ocultamiento de detalles internos en respuestas 500.
- Secretos fuera de configuración versionada; `git ls-files .env` no devolvió archivos. Esto no constituye un escaneo de todo el historial.
- Backend de contenedor con usuario sin privilegios y endpoints de salud con información limitada.
- Fuentes servidas localmente, TypeScript estricto y contrato OpenAPI documentado.

## 6. Orden recomendado

1. **Cerrar la fase 1 del frontend:** router, sesión, login real, usuarios/roles y dashboards; retirar datos/accesos demo; corregir tipos y pruebas; implementar coordinación de renovación y CSP.
2. **Endurecer usuarios/roles:** proteger códigos de sistema y último administrador, versionar roles, tratar duplicados concurrentes, limitar solicitudes y corregir carga inicial.
3. **Implementar el negocio en servidor según el plan:** pedidos, flujos, plazos, permisos, persistencia e historial; conectar pantallas a datos reales.
4. **Implementar Drive y correo:** resultados reales, reintentos y registro verificable; después tiempo real y demás fases previstas.
5. **Cerrar entrega operacional:** CI, recorridos integrados, accesibilidad, HTTPS, backups con restauración probada, monitorización y documentación vigente.

La fortaleza actual está en el módulo backend de acceso. La mayor distancia hasta un producto utilizable está en la integración del frontend y en los módulos de importación todavía pendientes. No se asigna un porcentaje global de avance: la documentación, las pantallas demo y la funcionalidad persistente tienen pesos distintos y faltaría una estimación por historias para obtenerlo.

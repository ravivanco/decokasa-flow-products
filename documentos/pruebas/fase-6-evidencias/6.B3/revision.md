# Revisión 6.B3 — 2026-10-21

Alcance: revisión local del motor A/B, persistencia, guía y vigencia de evidencias. Rama `fase-6-flujo-b`. No se cambian producción, tests, migraciones, skills ni decisiones aprobadas. Sin API, PostgreSQL o Google nuevos; solo se reproduce el arranque/parada del servidor estático. La validación de cierre pertenece a Richard.

## Fuentes y método

Manual DOCX vigente (extracción `manual-fase-6.txt`); plan `.claude/PRPs/plans/fase-6-flujo-b.plan.md`, incluidos resultados por paso y decisiones de Richard; informe 6.B1/6.B2; OpenAPI; arquitectura R1–R9; ADR 0004, 0011, 0012 y 0018; skill `.claude/skills/agregar-flujo-decokasa/SKILL.md`; código y migraciones V17/V18; reproductores y resultados archivados.

Skills aplicadas: `code-review` (modo local), `ecc:database-reviewer`, `ecc:verification-loop`, `ecc:learn-eval`. Los bucles de verificación se acotan por instrucción expresa: se reutilizan suites vigentes en vez de ejecutar todas otra vez. Las sugerencias genéricas de RLS, índices, despliegue o nuevas herramientas de las skills no se convierten en requisitos del proyecto.

## Motor y base de datos

- V17 inserta B/EC v1 con 12 etapas, 16 transiciones y los tres documentos tipados aprobados: Solicitud, Documentos codificados y Packing List. B10/B11 exigen al menos un documento, B10 además bodega activa. B2/B6/B8/B11 conservan plazo PENDIENTE. B12 es automática y sin rol. No se aplican documentos o plazos por analogía.
- V18 completa únicamente `revisa_etapa` de B2/B4, por F6-D4 aprobada por Richard, no por el cliente. **No es una nueva versión del flujo**: cambia dos atributos de lectura de v1. Es una excepción explícita de completado de definición, precedente V3/V11; no debe convertirse en permiso general para alterar reglas de pedidos existentes. A no cambia. Las decisiones de lectura y las de plazo de B2 son independientes.
- FK de pedido a definición, unicidad país/tipo/versión e índice parcial de vigente conservados. Creación resuelve vigente; acción, adjuntos y consulta resuelven `pedido.flujoId()`. Un pedido no se mueve de versión al publicar otra. El repositorio también permite pedir una versión explícita para el detalle.
- V17/V18 no reescriben pedidos, adjuntos ni historial. Los bloques DO verifican conteos y marcas principales; no equivalen a una comparación exhaustiva de cada fila. Esa comparación adicional ya está en `MigracionesFase6IT` para pedidos A previos y definiciones A, con checksum Flyway y protección append-only.
- Un solo agregado y casos de uso para A/B. `EtapasHabilitadas.segunDefinicion()` se concreta con la definición del pedido; no es una autorización global por número. `DefinicionFlujo` rechaza transiciones desde automáticas. Fin A11/B12 no admite operaciones manuales.
- `PermisosEtapa.paraCrear` exige rol inicial vigente antes de reservar numeración: admin solo no crea B; admin+Compras China sí por ese rol. Actuar/descargar vuelve a comprobar cuenta, roles y asignación en servidor; los roles múltiples suman permisos. P-06/P-17 siguen con su origen provisional.
- Documento válido pertenece a pedido, etapa y entrada; se consulta al módulo de archivos por puerto y se comprueba disponibilidad/integridad. Reentrada no reutiliza adjuntos de la entrada anterior. El producto conserva fuera de su modelo los detalles comerciales de los archivos.
- Pedido bloqueado antes de actuar; cambios, Fin, eventos y outbox se confirman juntos. Reloj del servidor por puerto; el evento del sistema de Fin no lo fabrica el navegador. Doble envío, versión/entrada obsoleta y rollback ya tienen pruebas vigentes. Google no participa en esa transacción.
- Fronteras: núcleo sin imports Spring/JPA/Google; contratos compartidos y puertos de entrada entre módulos, sin acceso al dominio interno del otro módulo. Decoradores transaccionales en config, según ADR-0012. No se identifica una dependencia circular nueva. Las pruebas ArchUnit de 6.B1 complementan esta lectura, no son nuevas ejecuciones de 6.B3.
- Frontend: detalle por versión del pedido, listas y bandejas por definiciones A/B, claves de consulta con sesión/filtros; A3 y B3 separados. La elección inicial no concede permisos: responde el backend. La lista de tipos soportados sigue siendo A/B, deliberadamente; agregar C requiere ampliar tipos y contrato además de la siembra. No hay promesa de soporte de cualquier tipo solo con SQL.

No se acredita un defecto nuevo de ejecución de producción en este alcance. Esto no certifica ausencia de todo defecto ni rendimiento de 500 pedidos.

## Hallazgos y propuestas para Claude

### F6-B3-H01 — Media — procedimiento incompleto para publicar otra versión

Ubicación: skill, sección 1, frase «Una migración que solo inserta la nueva versión», frente a `V2__flujos_pedidos_historial.sql:33` (`definicion_flujo_vigente_unica`). La propia skill reconoce el índice, pero no explica el cambio de vigencia. Su descripción incluye expresamente nuevas versiones, no solo otro tipo de flujo.

Reproductor **documental/estático**, no ejecutado contra una base en 6.B3: con EC/A v1 vigente, insertar v2 vigente=true con UUID y versión distintos deja dos filas dentro del índice parcial y PostgreSQL responde 23505. Insertarla vigente=false evita el error pero no la publica: creación sigue seleccionando v1. No es un fallo observado de V17: B era un tipo distinto, sin versión vigente anterior.

Propuesta: separar alta de otro flujo y publicación de versión. Documentar en una sola transacción desactivar la selección vigente anterior y publicar la nueva; conservar inmutables sus etapas/transiciones y todos los `pedido.flujo_id`. No borrar ni reasignar pedidos. Incluir control de concurrencia/una vigente y caso de traspaso a Codex para v1 en curso + v2 nueva. No implementar una migración futura sin alcance aprobado.

### F6-B3-H02 — Media — falta la guía independiente prometida por el plan

Ubicación: plan 6.5, línea 147: `documentos/agregar-flujo.md` más skill. `Test-Path` y revisión local confirman que el primer archivo no existe; la skill sí contiene una guía sustancial y separa correctamente parte A/Claude de parte B/Codex. El manual no exige ese nombre de archivo, pero el plan lo promete: no presentar ambos artefactos como entregados.

Reproductor: `Test-Path documentos/agregar-flujo.md` desde la raíz devuelve False. Propuesta: entregar la guía breve con enlace a una única fuente de instrucciones (la skill), o reconciliar explícitamente el plan con Richard si decide que la skill basta. Evitar copiar dos manuales que diverjan. No se crea el archivo ni se da por aceptado un cambio de alcance en esta revisión.

### P6-B3-ADR — propuesta, no decisión aprobada

ADR-0004 ya aprueba datos versionados; ADR-0011/0012 cubren fronteras/transacciones y ADR-0018 la marca de llegada. Falta el registro específico de F6-D3: habilitación por definición en lugar del conjunto global numérico, guard de automáticas y consecuencia de creación por rol inicial. El plan reconoce que hoy está solo allí; la skill pide ADR ante ampliación del motor.

Recomiendo un ADR complementario, estado **proposed**, sin numeración reservada, enlazado a 0004/0011/0012/0018. Contexto: B11 no cabía en la habilitación global A1–A10. Alternativas: sumar números globales (ambiguos entre flujos), dos motores (duplicación), definición del pedido (implementada). Consecuencias: núcleo único y compatibilidad de constructores, obligación de validar automáticas y versiones, separación de permisos/visibilidad. Referenciar F6-D2/D3 y el origen de cada aprobación; no atribuir la propuesta al cliente. Richard decide su aceptación. No se crea un ADR aceptado por inferencia.

Nota de mantenimiento: el final del plan aún dice «sin verificar» y «última migración V16» como estado histórico de parte A. También hay ADR antiguos con pendientes ya resueltos posteriormente (por ejemplo outbox). El informe actual es la evidencia de verificación; proponer enlaces de actualización que conserven la historia, sin resucitar bloqueos superados ni borrar pendientes reales.

## Diagnóstico del lanzador estático

`servidor-repro.mjs` conserva exactamente el texto y la lógica de `6.B2/servidor.mjs` (la escritura de PowerShell agrega BOM UTF-8); al residir en 6.B3, su dist/flag se aíslan allí. Flag de parada precreado, puerto estático loopback 44284; no se arranca API/DB, ni se solicita `/api`. Original y logs de 6.B2 intactos.

Primera ejecución confinada: salida nativa 1 con `spawn EPERM` al cargar configuración Vite, evidencia `lanzador.log`. Es una restricción de ejecución, no un defecto del producto. Se repite con permiso de ejecución fuera del sandbox, misma fuente y captura distinta.

Repetición permitida: build terminado, `FRONT_B2_LISTO`, parada por flag, **`$LASTEXITCODE=0` y `$?=false`**. PowerShell 5 convierte stderr de Node/Vite en `NativeCommandError` al redirigir `*>`; los avisos conservados hacen que el estado del comando de PowerShell sea falso aunque Node salga normalmente. El wrapper reproduce esa divergencia y captura explícitamente ambas variables antes de emitir JSON y `exit $nativeCode`. No se ocultan ni suprimen avisos.

Conclusión: se reproduce una explicación compatible para el 1 del lanzador histórico; **no se puede recuperar retroactivamente el código nativo no registrado de aquella ejecución**. Los tres recorridos, respuestas API, exportaciones, cero errores JS, lint/build explícito 0 y parada sin escucha son evidencias independientes. El aviso no invalida 6.B2 ni permite declarar que aquel lanzador devolvió 0. Propuesta para el arnés futuro: registrar código nativo y estado PowerShell separadamente, con salida explícita; conservar stderr y comprobar cierre del servidor.

No se repite API/E2E/check backend sin cambio de producto que lo justifique. El build incidental de esta reproducción no sustituye una suite ni mide cobertura nueva.

## Learn-eval

Candidato: publicar definiciones versionadas sin mover pedidos existentes, con traspaso separado a Codex. Búsqueda de solapamiento en skills globales `~/.claude/skills` y proyecto: el contenido pertenece ya a `agregar-flujo-decokasa`, ADR-0004 y patrones de migración. MEMORY.md raíz, `.claude/MEMORY.md` del proyecto y `~/.claude/MEMORY.md` no existen en las rutas consultadas. Es reutilizable dentro del proyecto, pero otra skill duplicaría la existente. Veredicto **Drop para una skill nueva**; H01 propone mejorar la existente posteriormente, sin escribirla ni instalar aprendizajes. La peculiaridad del lanzador queda como evidencia de este arnés, no como una nueva regla global sin necesidad concreta.

## Resultado y límites

Reutilizados: backend 878/878 y líneas 5382/5895 = 91,30 % (6.B1); frontend 329/329, lint/build 0 y 19 afectadas finales (6.B2); críticos A/B 3/3 y teclado/axe 2/2 (6.B2). Frontend líneas 1444/2113 = 68,33 %, objetivo adicional 80 % pendiente. No son cifras medidas otra vez en 6.B3.

Las migraciones vacía/previa, A conservado, doce etapas B, permisos, rollback y Fin tienen evidencia local vigente. Los recorridos A/B usan API real y documentos ficticios reales del módulo, no fixtures que salten etapas. No se mantiene el bloqueo documental antiguo A6/A9/A10: V16 lo resolvió por decisión explícita de Richard, conservando requisitos reales.

Criterio manual de recorridos conjuntos: acreditado localmente; doce etapas B: acreditado localmente; guía vigente: parcial por H01/H02 y ADR propuesto. Revisión no equivale a autorización de cierre. Siguen meta de días, plazos pendientes, decisiones provisionales, destinatarios/avisos de demora, aceptación del polling por el cliente, diferencia frente a 500, cuota física temporal y 3.B3 Google real pospuesto. Fases abiertas para Richard.

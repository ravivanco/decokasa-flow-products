"""Publish only measured evidence. Run after medir.py final; does not change production."""
from pathlib import Path
import json

base=Path(__file__).resolve().parent
informe=base.parents[1]/'fase-3-informe.md'
data=json.loads((base/'final-metricas.json').read_text(encoding='utf-8'))
runs=[]
for p in sorted(base.glob('20*.json')):
    r=json.loads(p.read_text(encoding='utf-8-sig'))
    if 'etapa' in r:runs.append((p,r))
check=[r for _,r in runs if r['etapa']=='check'][-1]
assert not data['produccion_o_protegidos_modificados'], 'Se debe investigar cualquier cambio protegido antes de publicar'

def pct(x):return f"{x['porcentaje']:.2f}".replace('.',',')+' %'
def frac(x):return f"{x['cubiertas']}/{x['total']}"
g=data['global_lineas'];a=data['archivos_lineas'];t=data['totales'];expanded=data['archivos_mas_adaptador_de_pedidos_lineas']
status='APROBADA LOCALMENTE' if check['salida']==0 and not t['fallos'] and not t['errores'] and all(data['objetivo_lineas_80'].values()) else 'CON BLOQUEOS'
note=(f"> **Estado vigente — 2026-10-19: revalidación local {status}.** Check oficial del backend: salida {check['salida']}; "
      f"{t['casos']} casos, {t['fallos']} fallos, {t['errores']} errores y {t['omitidas']} omitidos. "
      f"Cobertura de líneas global **{pct(g)} ({frac(g)})** y del módulo archivos **{pct(a)} ({frac(a)})**, "
      "sin exclusiones nuevas ni reducción del 80 %. Producción intacta. Cuota lógica validada; espacio físico temporal sigue limitado por capacidad operativa, no por esta cuota. "
      "OAuth/workers/schedulers desactivados. Google real 3.B3 pospuesto por decisión de Richard y fase 3 abierta. Véase la revalidación al final; los estados siguientes son históricos.\n\n")
table='\n'.join(f"| `{r['fecha']}` · {r['etapa']} | `{r['comando']}` | {r['salida']} | [log](fase-3-evidencias/revalidacion-local/{p.stem}.log) |" for p,r in runs)
counts={x['clase'].rsplit('.',1)[-1]:x['casos'] for x in data['pruebas']}
section=f'''
## Revalidación de pendientes locales — 2026-10-19

**Resultado: {status}.** Encargo exclusivo de pruebas/cobertura, usando `ecc:springboot-tdd`, `ecc:tdd-workflow` y `ecc:test-coverage`, con revisión Java de los cambios de pruebas. No se modificó producción ni se hicieron commits de checkpoints: prevalece la prohibición expresa de Git de escritura.

### Entorno, aislamiento y ciclo de evidencia

- Rama observada `fase-3-drive-gmail`, sin checkout. Java 21, Gradle wrapper 9.7.1, Spring Boot 4.1.1, PostgreSQL 17 Testcontainers. No instalaciones o cambios de dependencias.
- Cada fixture SQL ligero usa esquema UUID dentro de un contenedor de su clase, sin reuse. Los contextos Spring de cargas se recrean por caso con contenedor propio y nuevo directorio: las reservas retenidas de un caso no contaminan el siguiente. No se elevan cuotas para facilitar tests ni se desactivan triggers para limpiar reservas.
- Runner `ejecutar.ps1` fija OAuth/Drive/Gmail/automatismos/semilla en false, importación privada vacía y DB_URL en 127.0.0.1:1: una prueba sin Testcontainer falla cerrada. Almacenamiento exclusivo temporal por ejecución; ningún dato de desarrollo o del ensayo pospuesto.
- HTTP Google simulado antes de construir los adaptadores. Las pruebas OAuth usan JSON/llave/tokens ficticios propios, HTTP simulado y verificador JWT sustituido: prueban lógica de claims, state y cifrado, **no firma JWK o consentimiento reales**. No se leen credenciales privadas reales. Relojes controlados para esperas/caducidades; concurrencia sincronizada con barreras, sin sleeps de minutos.
- RED inicial: check con 541 casos y 14 fallos, sin defecto productivo nuevo confirmado. Había constantes V6/tablas antiguas, contaminación del cupo global entre fixtures y expectativas antiguas de borrado. Se corrigieron solo fixtures/aserciones: V8/lista exacta, aislamiento de casos y conservación exacta de preparado/reserva según la decisión aprobada. La prueba de vacío/exacto/exceso es ahora parametrizada con propietario independiente por caso: no libera reservas fallidas ni aumenta cupos para permitir el siguiente archivo.
- GREEN focalizado: 60 casos de cuotas/correo/protocolo Gmail/migraciones, sin fallos. GREEN de archivos antes de las últimas seis aserciones: 175 casos y 82,37 % de líneas del módulo. Check intermedio: 662 casos, sin fallos/omisiones, global 90,64 % y archivos 86,34 %. Después se añadieron el caso HTTP de cupo/rollback de inicio y cinco variantes del rechazo Gmail, y se repitió el check oficial completo.

### Comandos y resultados

Desde `documentos`, `& .\\pruebas\\fase-3-evidencias\\revalidacion-local\\ejecutar.ps1 <etapa>` prepara el aislamiento y ejecuta desde `backend` el comando literal de cada fila. Los scripts de copia solo admiten fuentes `src/test/java`, comprueban hashes y no son un arnés alternativo de compilación.

| Marca/etapa | Comando Gradle desde backend | Salida | Evidencia |
|---|---|---|---|
{table}

Se usó siempre el source set/test/check oficial, **sin el antiguo arnés que excluía notificaciones**. La ejecución de cobertura `jacocoTestReport -x test` solo generó XML a partir del `.exec` recién producido por el baseline; no omitió pruebas de la validación final. La copia automática `*-baseline-jacoco.xml` anterior a esa generación era un XML histórico: no se usa como medición del baseline ni del resultado final.

### Casos cubiertos y evidencia de comportamiento

| Alcance | Suite/casos finales | Garantías comprobadas |
|---|---|---|
| Correo persistente | CorreoReintentosLocalesIT: {counts.get('CorreoReintentosLocalesIT')} | Inicial + dos; fechas 1/5 min; Retry-After mayor/menor; reinicio sin reset; terminal tras tres; sin reenvío de permanente/incierto/transitorio genérico; éxito conserva identidad; lease perdido y rechazo tardío no rearman |
| Protocolo Gmail | GmailProtocoloLocalTest: {counts.get('GmailProtocoloLocalTest')} | Rechazos estructurados 403/429 reconocidos, motivos mixtos/desconocidos y códigos discordantes; Retry-After segundos/fecha/pasada/inválida/desbordada; transporte/408/5xx/2xx sin ID inciertos; credencial fallida antes del POST; cero red real |
| Cuotas SQL | CuotasLocalesIT: {counts.get('CuotasLocalesIT')} | Defaults binarios y configuraciones pequeñas; inválidas; límite exacto/byte extra global y personal; dos/diez cupos; pausa/fallo/reinicio; reserva/confirmación idempotente; ajuste multipart; rollback; propiedad inmutable; carreras de bytes/cupos/idempotencia y aborto seguro REPEATABLE READ |
| Carga real aislada | CargasB1IT: {counts.get('CargasB1IT')} | Archivo de 104857600 bytes en 25 bloques; hash/asociación pedido/etapa/entrada/evento; permisos vigentes; reanudación y tail sin confirmar; incompletos no válidos; rollback conserva reserva y bytes; HTTP 409 del tercer inicio no deja sesión; finalizar libera un cupo sin duplicar bytes |
| Migraciones | MigracionesArchivosB1IT: {counts.get('MigracionesArchivosB1IT')}; MigracionesIT: {counts.get('MigracionesIT')}; V2RestriccionesIT: {counts.get('V2RestriccionesIT')} | V1–V8 desde vacío y reaplicación sin cambios; fase 2 poblada conserva archivo/huella/propietario/relaciones/historial; V6 con completa y parcial recupera exactamente dos asignaciones; exceso histórico no se borra y bloquea nueva admisión |
| Retención multipart | Backend24IT: {counts.get('Backend24IT')} | Regresión del backend, límites y permisos, archivo preparado/final retenido al fallar registro y reserva pendiente; rollback de negocio/outbox sin avisos enviables |
| Almacén/OAuth local | AlmacenGoogleLocalTest: {counts.get('AlmacenGoogleLocalTest')}; OAuthFlujoLocalTest: {counts.get('OAuthFlujoLocalTest')} | Cifrado persistente/ACL, corrupción, identidad/propiedad y vencimiento de grant; sesiones Drive ligadas a UUID; callback fijo, metadatos opcionales, state de un uso/nonce/PKCE, cuenta/claims/scopes y renovación simulada sin extender siete días |

También se repitieron las suites existentes de disco/parciales/Drive: recuperación, estados, inicial + tres reintentos de Drive, concurrencia, lease e idempotencia/reconciliación con proveedor simulado. No se añade whitelist de formatos ni interpretación comercial del contenido. Las pruebas de cuota con tamaños agregados grandes son de metadatos; el caso de archivo exactamente 100 MiB sí escribe/verifica bytes reales en disco exclusivo.

### Cobertura final medida — criterio LINE del check vigente

| Alcance | Cubiertas/total | Porcentaje | Objetivo |
|---|---|---|---|
| Backend global completo | {frac(g)} | **{pct(g)}** | ≥80 % |
| Módulo archivos, paquete raíz y todos sus subpaquetes | {frac(a)} | **{pct(a)}** | ≥80 % |
| Compatibilidad ampliada: archivos + adaptador de entrada de pedidos para archivos | {frac(expanded)} | {pct(expanded)} | Sin excluir ese adaptador del global |

Global y módulo son denominadores diferentes; no se promedian ni se suman. Para comparar el mismo código previo a estas pruebas: baseline fresco **3859/4746 = 81,31 % global** y **1039/1611 = 64,49 % archivos**. La lectura inicial por coincidencia de texto incluía además 12 líneas de `pedidos/adapter/in/archivos` (1049/1623 = 64,63 %); el cálculo final delimita el módulo por prefijo y publica también el agregado ampliado. Ningún archivo se retira del reporte global. La antigua medición 64,83 % (1012/1561) corresponde a otro código/denominador y no es la medición actual.

Ramas, informadas sin confundirlas con el umbral LINE: global {pct(data['global_ramas'])}, archivos {pct(data['archivos_ramas'])}. Hay ramas/clases aún por mejorar; superar 80 % de líneas no prueba todas las rutas o la integración externa. El detalle de fuentes bajo 80 % está en el JSON, sin exclusiones o modificaciones de JaCoCo/Gradle.

Evidencia final preservada: [métricas y fuentes con huecos](fase-3-evidencias/revalidacion-local/final-metricas.json), [JaCoCo completo](fase-3-evidencias/revalidacion-local/final-jacoco.xml), [XML JUnit por suite](fase-3-evidencias/revalidacion-local/final-junit/), [medidor reproducible](fase-3-evidencias/revalidacion-local/medir.py). Total final **{t['casos']} casos / {t['fallos']} fallos / {t['errores']} errores / {t['omitidas']} omitidos**.

### Defectos, conservación y pendientes

No se confirmó un defecto productivo nuevo en los casos ejecutados. Los fallos iniciales documentados eran expectativas/aislamiento de fixtures adaptados al contrato provisional, no correcciones productivas. B1-P01/P02 conservan su cierre histórico y fueron ejercitados por la suite completa, sin cambiar su implementación.

Comparación SHA-256: **{data['archivos_protegidos']} archivos protegidos sin cambios**, incluyendo todo `backend/src/main`, `build.gradle`, Header.tsx, índice Git y configuración privada. Solo cambian pruebas y documentación/evidencias. No se modifica el umbral ni se crean exclusiones de código. Contextos/testcontainers propios terminan con las suites; no se detienen servicios ajenos ni se hace limpieza de volúmenes compartidos.

**La cuota sigue siendo lógica.** Representaciones parcial/final, temporales de preparación, spool servlet/proxy y huérfanos físicos no tienen un límite físico agregado garantizado por esta implementación. Un volumen de 10 GiB no queda validado como suficiente. B3-SEG-01 permanece abierto para dimensionamiento/espacio físico y revisión operativa; no se aprueban por deducción reserva 3×, margen libre, borrado o TTL. Las cargas fallidas conservan reserva/cupo; la recuperación/liberación administrativa de abandonadas continúa pendiente.

Las decisiones provisionales de reintentos/cuota quedan **revalidadas localmente para los casos ejecutados**, sin convertirlas en política definitiva de producción. Destinatarios definitivos, resolución administrativa de incertidumbre, retención y validación externa siguen pendientes. **OAuth, workers y schedulers desactivados; sin Google real, bases compartidas o Git de escritura. 3.B3 real sigue pospuesto por decisión de Richard y la fase 3 abierta.**
'''
text=informe.read_text(encoding='utf-8')
header='# Fase 3 — informe de pruebas\n\n'
assert text.startswith(header)
assert '## Revalidación de pendientes locales — 2026-10-19' not in text, 'No duplicar el informe'
informe.write_text(header+note+text[len(header):]+section,encoding='utf-8')
print('Informe actualizado con resultados medidos:',status)

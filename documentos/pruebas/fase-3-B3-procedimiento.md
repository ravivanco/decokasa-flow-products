# 3.B3 — Procedimiento propuesto para ensayo real aislado

**Ensayo real pospuesto por decisión de Richard, 2026-10-19. Recursos propios detenidos, evidencias conservadas.** No ejecutar las operaciones siguientes hasta una instrucción posterior de reanudación. 3.B3 y fase 3 siguen abiertos.

Preparado el 2026-10-19. **No ejecutado ni autorizado por esta preparación.** Actualización: Richard aprobó el buzón empresa como receptor exclusivamente de 3.B3 y el arnés ya está preparado y comprobado offline; véase [instrucciones ejecutables del arnés](arnes-b3/README.md). No autorizar cuentas, activar integraciones/workers, crear carpetas remotas, subir ni enviar hasta recibir aprobación expresa de esas operaciones. Este documento no modifica producción ni resuelve F3-D5/F3-D8 de producción.

## Datos que deben quedar aprobados antes de empezar

| Campo | Valor y estado |
|---|---|
| Remitente OAuth | `importadora.latinoamericaworld@gmail.com`, ya aprobado y coincidente con configuración local; cuenta aún no autorizada mediante este ensayo |
| Destinatario **aprobado solo para 3.B3** | `importadora.latinoamericaworld@gmail.com`, un aviso de prueba a sí misma. El arnés lo vincula al admin exclusivo activo con rol admin y asociación EC. No se convierte en destinatario definitivo de producción. Este autoenvío no acredita entrega entre buzones distintos |
| País propuesto | EC, solo datos ficticios en una base nueva. Aprobar el alcance del ensayo; no se cambia la política por país de producción |
| Carpeta Drive propuesta | **Mi unidad / EC / `<número real generado para el pedido del ensayo>`**. La aplicación crea y persiste nuevos IDs. No escribir un número inventado ni reutilizar IDs de producción |
| Raíz alternativa | No existe configuración para una raíz `Pruebas/3.B3`. El código consulta `root` y crea país/pedido directamente. Usar la estructura aprobada; si se exige otra raíz, detener y aprobar un cambio posterior, sin sustituirla mediante SQL |
| Archivo | `ficticio-3b3-100MiB.bin`, exactamente **104857600 bytes = 100 MiB**, sin contenido comercial. Diferente de 100 MB decimales = 100000000 bytes del manual |
| Volumen autorizado propuesto | Un archivo, un pedido ficticio, una transición confirmada y un aviso inicial al único receptor aprobado. Sin invitaciones, cambios de permisos Drive, borrados ni reenvíos automáticos de correo |
| Evidencia | Identificador único de ensayo; registro privado de IDs/enlaces reales fuera del repositorio; informe compartible sanitizado |

Con una base nueva se generará otra carpeta EC si hace falta: **el nombre visible por sí solo no identifica una carpeta exclusiva**. Confirmar los IDs recién persistidos y su padre; no adoptar una carpeta EC preexistente de la cuenta. Su creación en Mi unidad de la empresa también requiere aprobación del ensayo. No añadir año, etapa o sufijos a la estructura aprobada.

## Bloqueo técnico de correo y ejecución controlada

`NotificacionesConfig.directorioDestinatarios()` devuelve siempre un conjunto vacío. No hay variable `CORREO_DESTINATARIO`, pantalla para configurarlo ni endpoint de reenvío. Poner flags en true o rellenar a mano el destinatario de una fila **no habilita** el procesador: comprueba también el directorio vigente.

El arnés **ya está preparado en documentos/pruebas/arnes-b3**, fuera de producción: contexto aislado con directorio explícito del único receptor aprobado, EC y admin vigente; adaptadores Google reales; base/disco/tokens exclusivos; scheduling desactivado; invocación de los puertos existentes `ReintentarCopiasPendientes.ejecutar()` y `EnviarNotificacionesPendientes.procesar()`. Compilación y ocho guardas offline aprobadas. No se arrancó el contexto real, Docker o Google. Los comandos reales de arranque y confirmaciones por operación están en su README; el arranque y montajes aún requieren validación cuando se autorice ejecutarlos.

El encolado será el oyente transaccional existente. Crear la transición **después** de configurar ese directorio de ensayo; no convertir intenciones históricas nulas ni introducir avisos manuales en SQL. Un fallo/resultado incierto detiene el envío; no volver a invocar para forzarlo. La autorización de un solo intento de prueba no aprueba una política productiva de reintentos.

Drive dispone además de scheduler real, pero `COPIA_DRIVE_LOTE=1` limita el lote, **no el número de pasadas**. La recomendación es el arnés de una invocación; activar el scheduler sería una alternativa que necesita aprobación específica, cola completamente aislada y parada controlada. Hoy ambos workers siguen desactivados.

## 1. Preparar entorno local exclusivo, después de aprobar el ensayo

1. Registrar un ID único y una ventana de ejecución. Conservar el checkout actual; no cambiar rama, credenciales existentes ni archivos productivos.
2. El arranque posterior del arnés reserva PostgreSQL 17 Testcontainers exclusivo, base y usuario nuevos y volumen nombrado exclusivo etiquetado por UUID/raíz; publica solo en loopback con puerto aleatorio. No acepta una URL externa ni usa la base por defecto `localhost:5433/decokasa`, Docker Compose o una base compartida. Para recuperar el mismo ensayo conserva su volumen exclusivo; no adopta volúmenes sin etiquetas coincidentes. Las migraciones V1–V6 se aplicarán solo en ese arranque autorizado; no se ejecutaron en esta preparación.
3. Preparar, fuera del repositorio, directorios privados diferentes para bytes duraderos, archivo ficticio y evidencias. Revisar ACL del disco completo de ensayo y espacio para original, parcial, final, descarga y evidencia; los 100 MiB por archivo no reservan ni limitan el disco global. Mantener un solo archivo en el alcance; no establecer ni ejecutar retención/borrado.
4. Usar el JSON OAuth externo ya configurado, sin copiarlo. Preparar un **contenedor de tokens exclusivo del ensayo**, inicialmente inexistente, en otro directorio privado; puede usarse la llave externa existente sin sustituirla. Mantener el contenedor habitual intacto. No compartir la ruta del contenedor entre procesos.
5. El arnés genera propiedades privadas externas para DB/JWT propios; crea sus propiedades de Spring en memoria y desactiva `spring.config.import` para no cargar el `.env.local` productivo. De este último solo extrae rutas Google y cuenta, sin reutilizar DB/JWT/tokens habituales. No copiar propiedades privadas a informes, consola, comandos, capturas o variables `VITE_*`.
6. El backend debe escuchar en `SERVER_ADDRESS=127.0.0.1`, `SERVER_PORT=8080`, sin túneles ni exposición LAN. Comprobar que el puerto esté libre; si lo ocupa otro proceso, detener la preparación y no finalizar ese proceso ajeno. El callback sigue usando **localhost**, no 127.0.0.1.
7. Frontend local con `VITE_ACCESO_LOCAL=false`, `VITE_API_URL=/api/v1`, proxy al backend de ensayo. Admin de la base exclusiva mediante login normal, sin datos empresariales ni bypass de rol. Definir CORS con el origen local exacto, sin comodines.
8. Registrar hash y procedencia del artefacto backend que se usará. Hay un JAR local existente, pero su presencia no demuestra correspondencia con las fuentes actuales. Una compilación o ejecución futura se hará solo dentro del encargo autorizado; no se compiló nada en esta preparación.

### Matriz de activación futura recomendada

Valores del **proceso aislado**, sin editar valores por defecto del repositorio. Cada cambio requiere reiniciar ese proceso; no es un interruptor dinámico.

| Momento | OAuth | Drive | Gmail | Worker Drive | Worker correo |
|---|---|---|---|---|---|
| Preparación actual | false | false | false | false | false |
| Consentimiento autorizado | true | false | false | false | false |
| Carga local del archivo | true | false | false | false | false |
| Invocación única de copia por arnés revisado | true | true | false | false | false |
| Invocación única de correo por arnés revisado | true | false | true | false | false |
| Arranque posterior al cierre del ensayo | false | false | false | false | false |

Nombres reales: `GOOGLE_OAUTH_HABILITADA`, `GOOGLE_DRIVE_HABILITADA`, `GOOGLE_GMAIL_HABILITADA`, `COPIA_DRIVE_AUTOMATICA`, `CORREO_AUTOMATICO`. El arnés futuro usará lote 1 para ambos puertos. Si se aprueba el scheduler Drive como alternativa, solo su columna cambia temporalmente a true; el de correo no sirve mientras siga vacío el directorio.

## 2. Autorizar manualmente la cuenta, solo cuando se autorice ese paso

1. Richard comprueba en Google Cloud el cliente **web**, APIs Gmail/Drive, público Externo/En pruebas, cuenta remitente en Usuarios de prueba y ámbitos `openid`, `userinfo.email`, `gmail.send`, `drive.file`. Confirmar allí la URI exacta `http://localhost:8080/api/v1/integraciones/google/oauth2/callback`. El JSON puede omitir `redirect_uris`; si lo incluye, debe contener esa URI. No se ha comprobado la configuración de Cloud ni una autorización real.
2. Arrancar solo el backend aislado con OAuth true y todos los demás flags false. Abrir `http://localhost:8080/api/v1/salud` en el navegador del operador. Inicio y retorno deben ocurrir en ese navegador/host para conservar la cookie de vínculo; no iniciar con curl/Postman y trasladar la URL a otro navegador.
3. No usar el `window.prompt` de contraseña visible de la guía histórica durante grabaciones o pantalla compartida. El siguiente **snippet de operador, no ejecutado ni incorporado al producto**, muestra un campo de contraseña enmascarado, hace login local e inicio en el mismo origen y no imprime respuestas o tokens. Ejecutarlo solo tras revisar el bloque, en esa página local y después de autorizar OAuth. No usar DevTools con preservación/exportación de red, HAR, trazas o capturas activadas.

```javascript
void (() => {
  if (location.origin !== 'http://localhost:8080') throw new Error('Origen local incorrecto');
  const dialogo = document.createElement('dialog');
  const formulario = document.createElement('form');
  const usuario = document.createElement('input');
  usuario.placeholder = 'Admin del entorno aislado'; usuario.autocomplete = 'username';
  const clave = document.createElement('input');
  clave.type = 'password'; clave.autocomplete = 'current-password';
  clave.placeholder = 'Contraseña local';
  const boton = document.createElement('button');
  boton.type = 'submit'; boton.textContent = 'Iniciar autorización Google';
  const mensaje = document.createElement('p');
  formulario.append(usuario, clave, boton, mensaje); dialogo.append(formulario);
  document.body.append(dialogo); dialogo.showModal();
  formulario.onsubmit = async evento => {
    evento.preventDefault(); boton.disabled = true;
    let sesion;
    try {
      const peticion = fetch('/api/v1/auth/login', {
        method: 'POST', credentials: 'same-origin',
        headers: { 'Content-Type': 'application/json' },
        body: JSON.stringify({ identificador: usuario.value, contrasena: clave.value }),
      });
      clave.value = '';
      const acceso = await peticion;
      if (!acceso.ok) throw new Error();
      sesion = await acceso.json();
      const inicio = await fetch('/api/v1/integraciones/google/oauth2/inicio', {
        method: 'POST', credentials: 'same-origin',
        headers: { Authorization: `Bearer ${sesion.tokenAcceso}` },
      });
      if (!inicio.ok) throw new Error();
      const destino = new URL((await inicio.json()).urlAutorizacion);
      if (destino.origin !== 'https://accounts.google.com'
          || destino.pathname !== '/o/oauth2/v2/auth') throw new Error();
      sesion = null; dialogo.remove(); location.assign(destino.href);
    } catch {
      sesion = null; clave.value = ''; boton.disabled = false;
      mensaje.textContent = 'No se inició la conexión. Revisa el entorno y el admin, sin compartir secretos.';
    }
  };
})();
```

4. Seleccionar únicamente la cuenta remitente aprobada. La visita al consentimiento de Google es la excepción administrativa deliberada; el navegador del empleado no sube archivos ni envía correo a APIs Google. El backend intercambia el código y verifica state, cookie, PKCE S256, nonce, firma RS256, emisor, audiencia, caducidad, email verificado y admin vigente antes de guardar.
5. El callback consume el estado una vez y redirige a resultado limpio. No capturar ni copiar la URL que contiene `code`/`state`. La página de resultado 200 sola no acredita autorización: registrar además creación del contenedor cifrado exclusivo, sus ACL y, después, el resultado del adaptador, sin abrir tokens.
6. El backend impone un máximo conservador de siete días al grant de En pruebas, o menos si Google lo indica; registrar hora de consentimiento y vencimiento esperado. Caducidad/revocación requieren detener y nueva autorización explícita; no habilitar renovaciones mediante reenvíos de correo. Conservar la política de Google referenciada en la guía y confirmar su condición en Cloud al realizar el ensayo.

## 3. Crear el archivo ficticio exacto — instrucción futura, no ejecutada

En el directorio externo ya aprobado, crear un archivo nuevo; no sobrescribir uno existente. Contendrá ceros y no afirma representar un documento comercial. No crear una lista de extensiones/MIME permitidos.

```powershell
$archivoEnsayoB3 = Join-Path $directorioB3 'ficticio-3b3-100MiB.bin'
$flujoEnsayoB3 = [IO.File]::Open($archivoEnsayoB3, [IO.FileMode]::CreateNew,
    [IO.FileAccess]::Write, [IO.FileShare]::None)
try { $flujoEnsayoB3.SetLength(104857600); $flujoEnsayoB3.Flush($true) }
finally { $flujoEnsayoB3.Dispose() }
if ((Get-Item -LiteralPath $archivoEnsayoB3).Length -ne 104857600) { throw 'Tamaño incorrecto' }
Get-FileHash -LiteralPath $archivoEnsayoB3 -Algorithm SHA256
```

Crear solo tras aprobar la preparación del entorno. Registrar bytes exactos y SHA-256; la visualización redondeada de Explorer no sirve como medida. Se esperan **25 bloques de 4194304 bytes**.

## 4. Cargar, copiar y comprobar recuperación

1. Con ambos workers y adaptadores de envío/copia desactivados, crear un pedido ficticio mediante la aplicación y registrar el número **real** devuelto y sus UUID. Cargar el archivo por bloques en la etapa y entrada vigentes usando el tipo documental realmente configurado para esa etapa; no introducir uno para forzar una transición.
2. Pausar y volver a seleccionar el mismo archivo; comprobar offset confirmado, rangos y hashes sin adelantar progreso. Antes de completar no debe existir adjunto válido ni cumplir el requisito documental. Finalizar y registrar los vínculos reales pedido/etapa/entrada/evento/adjunto. Comprobar PENDIENTE y descarga por backend con el mismo SHA-256, sin enlace Drive inventado.
3. Parar el proceso aislado y conservar DB y disco. Tras reiniciarlo, verificar la misma sesión/adjunto pendiente; no crear otra base, sesión, ID reservado o archivo para simular recuperación.
4. Autorizar por separado la invocación única de Drive con OAuth/Drive true y Gmail/ambos schedulers false, usando el arnés revisado. Su única cola contiene el adjunto del ensayo. Debe crear país/pedido con IDs persistidos, copiar, reconciliar respuesta/offset y confirmar metadatos antes de registrar COPIADO, ID y enlace reales.
5. En Drive, el operador verifica **esa carpeta por ID**, propiedad de la cuenta, padre, nombre, tamaño 104857600, checksum disponible y un solo archivo. No usar una coincidencia por nombre como evidencia de idempotencia. Obtener el enlace desde el backend con permiso vigente y abrirlo solo en esta comprobación manual autorizada; verificar también que la descarga por backend sigue funcionando.
6. Reiniciar y ejecutar otra pasada controlada: un adjunto ya COPIADO no debe generar una nueva copia. Si se autoriza una interrupción durante una transferencia pendiente, registrar antes un offset entre cero y total y recuperar conservando disco, sesión cifrada e ID reservado. Si el archivo terminó antes de detenerse, no declarar ensayada la interrupción real; documentar ese caso pendiente. No compartir la URI reanudable ni sus parámetros.
7. Fallo o cuota: conservar bytes y estados; inicial + tres reintentos Drive siguen siendo la política aprobada. No acelerar fechas mediante SQL ni borrar pendientes. Cualquier intervención fuera de esta ventana requiere un encargo nuevo.

## 5. Correo real, solo cuando se desbloquee y apruebe

1. Instalar exclusivamente en el arnés el directorio explícito aprobado **antes** de realizar la transición. Debe comprobar la identidad del admin receptor, país y buzón; no reutilizar la cuenta remitente como receptor por deducción.
2. Con envío aún apagado, confirmar una sola transición válida desde la aplicación. Comprobar un solo aviso con evento real y receptor aprobado, estado PENDIENTE y cero intentos, en la misma transacción que acción/historial. No duplicar el oyente ni fabricar la notificación desde el arnés.
3. Reiniciar con OAuth/Gmail true, Drive y ambos schedulers false. Invocar una vez `EnviarNotificacionesPendientes.procesar()` desde el arnés aprobado. Enviar fuera de la transacción de negocio y conservar Message-ID estable. No existe hoy endpoint HTTP para esta operación.
4. Evidenciar destinatario, un intento y resultado. ENVIADA confirma aceptación del proveedor; el receptor debe comprobar manualmente recepción (y spam si procede) y correspondencia con el pedido/evento ficticios. Registrar hora de recepción separada de la hora de aceptación. No pedir `gmail.readonly` para automatizar esta comprobación.
5. Ante timeout, resultado incierto, fallo posterior al POST o falta de recepción, **detener**, conservar la fila y observar el buzón; no volver a pulsar ni cambiar estado/intentos por SQL. No prometer entrega exactamente una vez. La política de recuperación/reintentos de correo continúa pendiente incluso si este envío sale bien.

## 6. Parada y evidencias

1. Al terminar cada operación, salir del arnés de una invocación. Si hubo backend/frontend en consola, usar Ctrl+C en **esas consolas** y esperar cierre. Si fuera necesario intervenir un PID, comprobar que corresponde al proceso concreto lanzado para este ensayo; no finalizar todos los procesos Java/Node ni un servicio ajeno.
2. Detener únicamente el contenedor PostgreSQL del ensayo, sin eliminarlo ni borrar su volumen. No ejecutar `down -v`, prune, DELETE de colas, limpieza de parciales o borrado de carpetas Drive. Una retención o limpieza posterior necesita aprobación.
3. Antes de cualquier nuevo arranque, devolver los cinco flags a false en la configuración externa del ensayo. Cerrar consolas para descartar sus variables de proceso y comprobar que no quedan listeners del ensayo. Cambiar el archivo/env no desactiva una JVM ya en ejecución: primero hay que detenerla.
4. Conservar base, parciales, bytes aceptados, ID reservado, offset, estados, contenedor cifrado y llave externa bajo ACL privada. No copiar tokens o llave al repositorio ni al paquete de evidencias. Revocar en Google es una acción remota posterior que debe aprobar Richard, no un paso automático de limpieza.
5. El informe compartible contendrá ID del ensayo, horas/zona, comandos sin valores sensibles, hash del artefacto y archivo, bytes, bloques, estados, contadores de intentos y resultado observado. Mantener los IDs/enlaces reales que se necesiten para correlación en un anexo privado externo; en Git usar referencias redactadas. No adjuntar dumps SQL completos, JSON OAuth, contenedores/llaves, Authorization, cookies, access/refresh tokens, code/state/nonce/verifier, HAR OAuth o URLs de sesión reanudable.
6. Capturar pantalla solo después del retorno limpio y sin paneles de secretos. Registrar hosts/rutas genéricas y estados de la API de negocio, sin headers, query OAuth o cuerpos privados. Distinguir la visita manual de consentimiento y de verificación Drive/Gmail de las llamadas servidor-servidor; las transferencias de la aplicación deben ir del navegador al backend y del backend a Google.

La evidencia simulada de 3.B1/3.B2 permanece separada de esta evidencia real futura. La cobertura de archivos sigue en **64,83 %** según B1; el último global completo es **80,34 %**, con otro denominador. No hay medición nueva ni resolución de esa deuda. El ensayo no cierra la fase por sí mismo: faltan decisiones, controles pendientes y validación expresa de Richard.

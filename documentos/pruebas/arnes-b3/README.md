# Arnes opt-in de 3.B3

**Estado vigente: ensayo real pospuesto por decisión de Richard (2026-10-19).** Consola detenida y sin contenedores del arnés activos; archivo y evidencias conservados. No ejecutar Iniciar/autorizar/copia/correo por la autorización anterior: esperar una instrucción nueva de reanudación. Las instrucciones siguientes se conservan para ese momento; no cierran 3.B3 o la fase.

Preparado, compilado y comprobado con guardas offline. **No se ha iniciado el entorno real ni autorizado Google.** Todo el codigo nuevo vive en documentos/pruebas/arnes-b3; el init script crea un source set opt-in y no modifica produccion, build.gradle ni las suites normales.

Richard aprobo `importadora.latinoamericaworld@gmail.com` como receptor **solo para 3.B3**. Es tambien el remitente previamente configurado. No hay parametro para otro receptor, directorio productivo nuevo, endpoint de envio ni scheduler habilitado. El admin del ensayo debe estar activo, tener rol admin y asociacion EC vigente. F3-D8 de produccion, reintentos de correo, cuota agregada y cobertura de archivos siguen pendientes.

## Lo ejecutado hoy

Desde documentos, PowerShell:

```powershell
.\pruebas\arnes-b3\arnes-b3.ps1 -Accion Comprobar
```

Equivale a Gradle `-I documentos/pruebas/arnes-b3/b3.init.gradle b3ComprobarGuardas b3Classpath --offline --console=plain --no-daemon`, desde backend. Compila solo el source set opt-in usando las dependencias existentes y ejecuta ocho pruebas de guardas sin Spring, Docker o Google. El `main` sin argumentos/consola retorna antes de iniciar nada. No se calcula ni acredita cobertura global nueva. La accion por defecto `Estado` tampoco inicia servicios.

## Pasos exactos para autorizar Google manualmente, cuando decidas iniciar el ensayo

Los comandos siguientes son **futuros**; no se ejecutaron. Iniciar creara una base exclusiva, aplicara sus migraciones y abrira un backend local; aun asi no autoriza Google por si solo. No ejecutar si no se ha aprobado ese arranque aislado.

1. Tener Docker local disponible, Java 21 y `localhost:8080` libre. Si el puerto esta ocupado, no finalizar el proceso ajeno. Mantener la rama fase-3-drive-gmail.
2. Crear un directorio nuevo fuera del repositorio, cuyo ultimo nombre sea un UUID. Aplicar ACL solo a ese directorio nuevo, nunca a la carpeta personal completa:

```powershell
$idEnsayoB3 = [Guid]::NewGuid().ToString()
$directorioB3 = Join-Path $env:USERPROFILE ('.decokasa\ensayos\3b3\' + $idEnsayoB3)
New-Item -ItemType Directory -Path $directorioB3 -ErrorAction Stop | Out-Null
$identidadB3 = [Security.Principal.WindowsIdentity]::GetCurrent().User
$aclEnsayoB3 = New-Object Security.AccessControl.DirectorySecurity
$aclEnsayoB3.SetOwner($identidadB3)
$aclEnsayoB3.SetAccessRuleProtection($true, $false)
$reglaEnsayoB3 = New-Object Security.AccessControl.FileSystemAccessRule(
    $identidadB3, 'FullControl', 'ContainerInherit,ObjectInherit', 'None', 'Allow')
$aclEnsayoB3.AddAccessRule($reglaEnsayoB3)
Set-Acl -LiteralPath $directorioB3 -AclObject $aclEnsayoB3
```

3. Desde documentos, en **PowerShell interactivo normal**, sin transcripcion/grabacion ni redireccion de stdin:

```powershell
.\pruebas\arnes-b3\arnes-b3.ps1 -Accion Iniciar -DirectorioEnsayo $directorioB3
```

Escribir `INICIAR ENTORNO AISLADO 3.B3` cuando se solicite. Elegir una contrasena nueva para el usuario **b3-admin**, mediante entrada enmascarada (12-72 bytes y reglas de contrasena del dominio). No aparece en consola ni se guarda en texto claro: su hash queda en la base del ensayo. En futuros arranques se pide esa misma contrasena y se comprueba, sin sustituirla.

Se usa PostgreSQL 17 Testcontainers sin reuse, puerto aleatorio solo en 127.0.0.1 y volumen nuevo `decokasa-b3-<UUID>` etiquetado con ensayo y raiz. Nunca se admite URL de una base externa/compartida. La contrasena aleatoria PostgreSQL y JWT del ensayo se guardan en `ensayo-privado.properties` bajo ACL privada externa; **ese archivo no es evidencia compartible**. Los bytes quedan en `archivos/`; los tokens en `tokens-3b3.enc`, cifrados con la llave externa existente. Se leen solo las rutas Google y cuenta del `.env.local` ignorado; no se reutilizan su DB, JWT o contenedor habitual de tokens. No se copian JSON/llave al ensayo o al repositorio.

4. En el menu escribir `autorizar` y confirmar exactamente:

```text
AUTORIZAR GOOGLE SOLO 3.B3
```

Solo este modo habilita el callback OAuth. Drive, Gmail y ambos schedulers siguen false. El arnes queda esperando en su consola; **todavia no pulsar Enter**.

5. En Google Cloud, comprobar cliente web, Gmail API/Drive API, cuenta empresa en Usuarios de prueba, modo Externo/En pruebas y los cuatro scopes aprobados. Confirmar callback exacto:

```text
http://localhost:8080/api/v1/integraciones/google/oauth2/callback
```

No descargar otra credencial por ausencia de redirect_uris. Si el campo existe debe contener esa URI. No usar 127.0.0.1 como callback ni registrar /resultado.

6. Abrir en el navegador `http://localhost:8080/api/v1/salud`. Ejecutar el **snippet enmascarado del apartado 2** de [fase-3-B3-procedimiento.md](../fase-3-B3-procedimiento.md), revisandolo antes. Usar identificador **b3-admin** y la contrasena exclusiva elegida. El snippet hace login e inicio en el mismo navegador y origen sin imprimir tokens; no trasladar un inicio de curl/Postman al navegador. No exportar HAR, headers, cookies o la query del callback.
7. Seleccionar **importadora.latinoamericaworld@gmail.com** y consentir solo los scopes aprobados. El backend valida state, PKCE, nonce, cuenta y admin y guarda `tokens-3b3.enc`. Comprobar retorno limpio y existencia/ACL del contenedor sin abrir su contenido. El estado 200 de la pagina por si solo no demuestra autorizacion. El backend conserva el limite de siete dias o menor de En pruebas; una revocacion puede invalidar antes.
8. Volver a la consola y pulsar Enter: el contexto se reinicia con OAuth/Drive/Gmail/schedulers false. `estado` solo muestra contadores y existencia de tokens; no consulta Google. Para terminar aqui, escribir `salir`. **Autorizar no inicia una copia o correo.**

## Alcance posterior de la ejecucion de prueba — no realizado

1. Tras autorizar expresamente la carga local, usar el frontend real con `VITE_ACCESO_LOCAL=false`, API `/api/v1`, puerto localhost:3000 y proxy localhost:8080. Login como b3-admin. Crear **un pedido ficticio EC** y usar su numero real; no generar el pedido mediante SQL. Los datos de otros paises o mas de un pedido/adjunto bloquean el paso remoto.
2. Crear fuera de Git el archivo `ficticio-3b3-100MiB.bin` en `$directorioB3`, con el ejemplo de archivo del procedimiento y **104857600 bytes**. Registrar SHA-256. Cargar por la aplicacion en la etapa/entrada vigentes: 25 bloques de 4 MiB, pausa/reanudacion y finalizacion. Los parciales no son adjuntos validos. El arnes no crea ni carga este archivo automaticamente.
3. Con la aprobacion futura de la copia real, escribir `copia` y confirmar `COPIAR UN ARCHIVO EN DRIVE 3.B3`. Se activa solo OAuth/Drive para **una pasada del puerto existente**, lote 1, sin scheduler. La aplicacion crea Mi unidad/EC/numero real con IDs exclusivos del ensayo y conserva sus politicas Drive. No se cambia la raiz ni permisos de comparticion. El modo vuelve a false al terminar. Comprobar COPIADO, metadatos e ID/enlace reales y descarga por backend. Una pasada puede dejar PENDIENTE/FALLIDO; no se ejecuta un bucle de reintentos desde este arnes.
4. Realizar **una sola transicion valida ENVIAR**, con el adjunto exigido por la etapa. El directorio test-only ya esta presente desde el arranque; el oyente de produccion encola el aviso al unico receptor aprobado en la misma transaccion. No insertar avisos ni modificar destinatarios historicos por SQL. No crear mas transiciones para forzar un envio.
5. Cuando se autorice ese envio real, escribir `correo` y confirmar `ENVIAR UN CORREO SOLO 3.B3`. Se exige un pedido, un archivo de 100 MiB ya COPIADO, ninguna carga parcial y exactamente un aviso PENDIENTE de la transicion ENVIAR del admin, al receptor aprobado y con cero intentos/ninguna incertidumbre. Se activa solo OAuth/Gmail para una invocacion de `procesar()`, lote 1, sin scheduler.
6. Antes de invocar el puerto se crea `correo-invocado.NO-BORRAR` con creacion exclusiva. Un segundo intento queda bloqueado incluso tras reiniciar. La persistencia outbox aporta ademas su marca de incertidumbre y fencing. Fallo, cancelacion o timeout no autorizan borrar la marca o reenviar. Puede quedar consumida antes de llegar a Google: es una parada conservadora del ensayo, no una promesa de entrega exactamente una vez ni una politica definitiva de reintentos.
7. La aceptacion del proveedor se registra aparte de la recepcion. Comprobar manualmente el buzón aprobado (incluido spam); este autoenvio no acredita entrega entre buzones distintos. No se solicitan permisos de lectura Gmail. Un resultado incierto detiene el ensayo; no se elimina una fila, restablecen intentos ni se reenvia para buscar verde.

Las frases de consola son confirmaciones locales por operacion, no autorizaciones concedidas en este encargo. No hay endpoint de envio general, selector de destinatarios o activacion automatica. El directorio de produccion sigue vacio.

## Parada, recuperacion y evidencias

- `salir` cierra el contexto y hace STOP solo al contenedor que arranco este proceso. Ctrl+C usa el mismo cierre. No elimina volumen ni datos; Testcontainers puede retirar el contenedor, pero el volumen nombrado creado por el arnes se conserva y no se registra para borrado. No usar prune/down -v.
- El cerrojo de raiz se mantiene hasta detener el contexto/contenedor. El arnes rechaza un volumen sin sus etiquetas o perteneciente a otra raiz y un contenedor activo del mismo ensayo. Para reiniciar, conservar **la misma raiz UUID**, propiedades privadas, volumen, disco y tokens; no usar otra carpeta para simular recuperacion ni duplicar el directorio.
- No hay perfil/flags persistidos true. Cada arranque vuelve a false; los cinco valores de produccion permanecen intactos. La reanudacion Drive solo procede mediante otra confirmacion y autorizacion del ensayo; el correo no vuelve a intentarse. El montaje persistente y el arranque real siguen pendientes de comprobar: no se iniciaron para validar este codigo.
- Las evidencias automaticas externas contienen hora, estados y contadores. No incluyen JSON/llave/tokens, Authorization, cuerpo MIME, IDs/enlaces remotos o URI reanudable. Tras una excepcion, conservar ademas la base/outbox para revisar el resultado incierto. No compartir el directorio entero: contiene secretos privados y bytes duraderos.
- Copiar al informe solo un resumen sanitizado y hashes. Registrar manualmente las comprobaciones de carpeta por ID y recepcion en un anexo privado; no copiar query OAuth, HAR o sesiones reanudables. No borrar parciales/aceptados ni carpetas Drive sin otra aprobacion.

No se considera resuelta la cuota agregada: la restriccion de un archivo limita este ensayo, no protege globalmente el almacenamiento de produccion. La politica de reintentos de correo permanece pendiente; archivos conserva su cobertura historica de 64,83 %. La fase sigue abierta.

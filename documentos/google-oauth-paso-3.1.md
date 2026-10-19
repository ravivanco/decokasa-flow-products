# Autorización inicial de Google del paso 3.1

Implementación de parte A, pendiente de comprobación independiente en parte B. No se ejecutaron pruebas ni builds y no se realizó ninguna autorización, subida o envío real durante este encargo.

Se reutiliza `archivos` y el puerto público `ConsultarResponsables` de usuarios. No hay un nuevo módulo de archivos, tablas, migraciones, outbox, worker, creación de carpetas, envío Gmail ni pantallas nuevas. El código nuevo se limita al consentimiento OAuth de la cuenta personal de empresa y a guardar sus tokens de forma privada.

## Configuración acordada

| Campo | Valor |
|---|---|
| Cliente Google | Aplicación web; `DECOKASA backend local` |
| Público y publicación | Externo, En pruebas; cuenta Gmail de empresa en Usuarios de prueba |
| Orígenes JavaScript | Vacío |
| Retorno autorizado | `http://localhost:8080/api/v1/integraciones/google/oauth2/callback` |
| Ámbitos | `openid`, `https://www.googleapis.com/auth/userinfo.email`, `https://www.googleapis.com/auth/gmail.send`, `https://www.googleapis.com/auth/drive.file` |
| Drive futuro | Mi unidad de la cuenta Gmail autorizada, no unidad compartida Workspace |

La aplicación no emplea cuenta de servicio, delegación de dominio o SMTP. El consentimiento administrativo requiere acceso a Google en el navegador del admin. El login de DECOKASA sigue usando sus JWT propios; no se sustituye por «Entrar con Google».

## Preparación manual del almacén privado

Estos pasos son instrucciones para Richard: no se ejecutaron. No compartas JSON, secretos, tokens ni capturas con sus valores.

1. Conserva el JSON descargado de Google **fuera de `decoWeb`**. Debe ser de tipo `web`; un JSON de escritorio o de cuenta de servicio se rechaza. Puede omitir `redirect_uris`. Si ese campo está presente, debe ser una lista de cadenas que incluya exactamente el callback acordado; una lista vacía, una URI diferente o un valor nulo/inválido se rechazan. El backend siempre usa el callback fijo, nunca un retorno enviado por el navegador ni comodines. La URI debe estar registrada en Google Cloud aunque no figure en el JSON. No se necesita leer o pegar el contenido del archivo en esta conversación.
2. Prepara dos directorios privados: uno para credenciales/llave y otro para el contenedor cifrado de tokens. Por ejemplo, `C:\Users\Richard\.decokasa\secretos` y `C:\Users\Richard\.decokasa\google-tokens`. No son parte del repositorio.
3. En Windows, usa Propiedades → Seguridad → Opciones avanzadas para **deshabilitar y retirar permisos heredados** y dejar acceso únicamente a la cuenta Windows que ejecutará el backend (el propietario). Aplica también esta restricción al archivo JSON, a la llave y a ambos directorios. El adaptador rechaza ACL que concedan acceso a otras identidades, incluidos grupos generales. No cambies permisos de Downloads o de tu carpeta personal completa: mueve el JSON a su directorio dedicado y protege solo esos directorios/archivos.
4. Genera **una sola vez** una llave binaria aleatoria de 32 bytes fuera del proyecto. En PowerShell puedes usar el siguiente bloque, cambiando únicamente la ruta privada si lo necesitas. No imprime la llave y se niega a sobrescribir una existente:

```powershell
$rutaLlaveGoogle = 'C:\Users\Richard\.decokasa\secretos\google-aes.key'
if (Test-Path -LiteralPath $rutaLlaveGoogle) { throw 'La llave ya existe. No la reemplaces.' }
$bytesLlaveGoogle = New-Object byte[] 32
$azarLlaveGoogle = [System.Security.Cryptography.RandomNumberGenerator]::Create()
try {
    $azarLlaveGoogle.GetBytes($bytesLlaveGoogle)
    [System.IO.File]::WriteAllBytes($rutaLlaveGoogle, $bytesLlaveGoogle)
} finally {
    [Array]::Clear($bytesLlaveGoogle, 0, $bytesLlaveGoogle.Length)
    $azarLlaveGoogle.Dispose()
}
```

5. No crees un archivo de tokens vacío: se generará después de un consentimiento correcto. Elige su destino, por ejemplo `C:\Users\Richard\.decokasa\google-tokens\oauth-tokens-local.enc`. El directorio debe existir y ser privado. La llave y el contenedor son archivos distintos; nunca guardes la llave dentro de la imagen o junto al código versionado.
6. Conserva respaldo cifrado y acceso restringido a la llave. Perder o sustituir la llave impide leer los tokens. No borres un almacén previo para resolver un error sin un plan de reconexión; el adaptador se niega a reemplazar una cuenta o cliente distintos del ya conectado.

En Linux, la misma política exige directorios `700` y archivos `600`, propiedad del usuario que ejecuta el backend. Se rechazan enlaces simbólicos y rutas relativas o bajo un árbol que tenga `.git`. El cifrado es AES-256-GCM con nonce aleatorio, versión autenticada, bloqueo entre procesos y reemplazo atómico. El refresh token no se almacena en PostgreSQL ni en texto claro; tampoco se devuelve por HTTP.

## Arranque local posterior autorizado por Richard

Configura las siguientes variables **en el proceso nativo del backend** o en su `.env.local` ignorado, sin incluir secretos directos. Las rutas deben apuntar a archivos externos y no a copias dentro del repositorio:

```text
GOOGLE_OAUTH_HABILITADA=true
GOOGLE_OAUTH_CLIENTE_RUTA=C:/Users/Richard/.decokasa/secretos/oauth-client-local.json
GOOGLE_OAUTH_LLAVE_RUTA=C:/Users/Richard/.decokasa/secretos/google-aes.key
GOOGLE_OAUTH_TOKENS_RUTA=C:/Users/Richard/.decokasa/google-tokens/oauth-tokens-local.enc
GOOGLE_CUENTA_EMAIL=REEMPLAZAR_POR_EL_GMAIL_REAL_DE_LA_EMPRESA
```

La cuenta debe ser la Gmail personal real que autorizará y la que figura como usuario de prueba. Se permiten dominios `gmail.com` y `googlemail.com`, sin tratar alias distintos como la misma dirección automáticamente. `login_hint` es solo una ayuda: el ID token firmado debe coincidir con la cuenta configurada.

El callback es fijo; no hay una variable para cambiarlo inadvertidamente a otro host o puerto. La integración queda desactivada por defecto y no hace conexiones a Google al arrancar. Si faltan rutas, permisos privados o cuenta válida, el inicio responde 503 `GOOGLE_NO_DISPONIBLE`, sin mostrar contenido del JSON, excepciones del proveedor o rutas privadas.

**El compose base no monta los secretos Google ni pasa estas variables al backend.** Para la primera conexión se documenta el arranque nativo con las variables anteriores, PostgreSQL y el resto de la configuración local ya existente. El arranque/compilación posterior se realiza solo cuando Richard lo autorice; no se hizo ahora. Un montaje Docker futuro necesitará permisos acordes al UID del backend, directorio de tokens externo escribible y credenciales/llave de solo lectura. No se afirma soporte Docker Google comprobado.

## Cómo completar manualmente el consentimiento

Realiza estos pasos después de la revisión de parte B y de autorizar el arranque local. No se ha abierto ni ejecutado este flujo durante la implementación.

1. En Google Cloud confirma Gmail API y Google Drive API habilitadas, cliente web, callback exacto, ámbitos acordados y Gmail de empresa como usuario de prueba. No registres el endpoint `/resultado` como callback ni cambies localhost por 127.0.0.1.
2. Arranca el backend nativo local en `localhost:8080` con su conexión de base y JWT existentes y las variables privadas anteriores. La creación de tokens ocurre solo tras completar OAuth; arrancar no envía correos ni sube archivos.
3. Abre **en un navegador normal de tu equipo** `http://localhost:8080/api/v1/salud`. Es importante usar ese host, puerto y navegador para que se conserve la cookie de vínculo del inicio al retorno. No inicies con curl/Postman y luego pegues la URL en otro navegador: el callback rechazará ese intento.
4. Como no se implementa una pantalla nueva en 3.1, usa la consola del navegador en esa página para hacer login normal y solicitar el inicio. El siguiente bloque pide usuario/contraseña local, mantiene el JWT de DECOKASA solo en memoria y navega a la URL de consentimiento. No muestra tokens. No pegues aquí el JSON Google ni actives registro de red/cuerpos o capturas de credenciales.

```javascript
void (async () => {
  const identificador = window.prompt('Usuario o correo del admin de DECOKASA');
  if (!identificador) return;
  const contrasena = window.prompt('Contraseña del admin de DECOKASA');
  if (!contrasena) return;
  const acceso = await fetch('/api/v1/auth/login', {
    method: 'POST', credentials: 'same-origin',
    headers: { 'Content-Type': 'application/json' },
    body: JSON.stringify({ identificador, contrasena }),
  });
  if (!acceso.ok) throw new Error('No se pudo iniciar la sesión local.');
  const sesion = await acceso.json();
  const respuesta = await fetch('/api/v1/integraciones/google/oauth2/inicio', {
    method: 'POST', credentials: 'same-origin',
    headers: { Authorization: `Bearer ${sesion.tokenAcceso}` },
  });
  if (!respuesta.ok) throw new Error('No se pudo iniciar OAuth. Revisa permisos de admin y configuración privada.');
  const inicio = await respuesta.json();
  const url = new URL(inicio.urlAutorizacion);
  if (url.origin !== 'https://accounts.google.com') throw new Error('Destino inesperado.');
  window.location.assign(url.href);
})().catch(() => window.alert('No se pudo iniciar la conexión. Revisa la configuración sin compartir secretos.'));
```

El prompt estándar no oculta visualmente la contraseña; úsalo solo en un equipo privado, sin grabación ni compartir pantalla, y no guardes el bloque con contraseñas escritas. Si el navegador bloquea pegar en la consola, sigue sus instrucciones de seguridad solo después de revisar este bloque. No se habilita ningún bypass del login.

5. Elige la cuenta Gmail configurada y acepta los permisos solicitados. Si eliges otra cuenta, omites un permiso, cancelas o dejas pasar los cinco minutos del inicio, vuelve a comenzar desde el paso 3. No se guarda una conexión parcial como correcta.
6. Google vuelve al callback con un código de un solo uso. El backend exige estado vigente, cookie HttpOnly del navegador y admin que siga activo con rol admin. El canje y las claves públicas de Google se consultan desde el backend; se verifican firma RS256, emisor, audiencia, expiración, nonce, email verificado, cuenta y ámbitos.
7. El callback redirige con 303 a una URL local limpia `/api/v1/integraciones/google/oauth2/resultado?codigo=200`, o al resultado de error 400/403/503; no conserva código OAuth, state o tokens en esa página. La pantalla de resultado es informativa, no un endpoint autenticado para consultar conexión: abrirla directamente no acredita que exista una autorización.
8. Al ver la confirmación, puedes cerrar la pestaña. El contenedor cifrado queda en la ruta externa. No abras, pegues o envíes su contenido. No se crea todavía la carpeta Drive ni se envía correo de comprobación. Una petición repetida al callback no puede canjear de nuevo el mismo estado.

## Acceso offline y modo En pruebas

El inicio solicita `access_type=offline`, `prompt=consent select_account`, nonce y PKCE S256. El access token caduca según `expires_in`; el refresh token queda cifrado para el uso posterior autorizado de los adaptadores. Si una respuesta omite refresh token, solo se conserva uno anterior de la misma cuenta y cliente que aún esté dentro del plazo conocido; si no existe, la conexión falla y requiere un consentimiento nuevo.

Con aplicación externa **En pruebas** y ámbitos Gmail/Drive, Google limita a 100 usuarios de prueba y la autorización/refresh token caducan a los **7 días**. La excepción para solo perfil/correo/OpenID no aplica a este conjunto de permisos. El almacén registra un límite conservador de siete días o el plazo menor que devuelva Google; conservar un refresh token anterior no reinicia su fecha. Una revocación puede invalidarlo antes. La renovación operativa de access tokens por los futuros workers no forma parte de este paso; no hay tarea programada ni egress de arranque.

No se promete funcionamiento permanente mientras Google siga En pruebas. Tras caducar o revocar, será necesario repetir la conexión administrativa. Publicar la app o cambiar el cliente/URI de producción es una decisión posterior; no se realizó. Los pasos 3.2–3.5 deberán conservar colas y archivos ante pérdida de autorización y nunca revertir un pedido confirmado por un fallo de Google.

## Drive y alcance preservado

Mi unidad pertenece a la cuenta que consiente y usa su cuota personal (compartida con Gmail/Fotos). Una carpeta compartida no equivale a una unidad compartida Workspace. `drive.file` no concede acceso a cualquier carpeta preexistente solo por copiar su ID: en un paso futuro la aplicación creará su propia carpeta raíz con consentimiento/activación autorizados, sin ampliar a `drive` general.

En 3.1 no se crea esa carpeta, no se solicita `driveId`, no se cambia `adjunto`, no se borra el volumen local ni se publica un enlace. Se conservan el límite provisional de 100 MiB y todos los identificadores, historial, permisos y pendientes de fase 2. El tráfico de archivos/correo seguirá siendo del backend, especialmente para los empleados en China; solo el consentimiento inicial del admin visita Google.

## Pendiente para la parte B

No ejecutado ni demostrado todavía:

- Compilación y arranque del cambio en el stack del proyecto; conformidad de OpenAPI y arquitectura.
- Rechazo de anónimos, no-admin, admin desactivado o sin rol al inicio y durante el retorno; preservación del login existente y sus cookies.
- Estado inexistente, repetido, caducado, cookie ausente/distinta, parámetros duplicados, cancelación, PKCE/nonce inválidos y selección de otra cuenta.
- Canje con proveedor controlado: permisos parciales, firma/emisor/audiencia/caducidad erróneos, timeouts, límites de lectura y errores sanitizados sin secretos en respuestas/logs.
- Almacén externo: ACL/600, rechazo de rutas internas/symlinks, integridad del cifrado, llave incorrecta, falta de refresh token, cuenta/cliente distinto, concurrencia y escrituras atómicas.
- Caducidad real de En pruebas y comportamiento tras reinicio. El estado temporal del inicio no sobrevive a reiniciar el backend: debe iniciarse de nuevo, pero los tokens cifrados sí permanecen.
- Una autorización manual real requiere aprobación específica; durante las comprobaciones automáticas se deben emplear dobles y credenciales ficticias. No autoriza subidas ni envíos reales.

Fuentes: [OAuth web y offline](https://developers.google.com/identity/protocols/oauth2/web-server), [OpenID Connect](https://developers.google.com/identity/openid-connect/openid-connect), [modo En pruebas](https://support.google.com/cloud/answer/15549945), [ámbitos de Drive](https://developers.google.com/workspace/drive/api/guides/api-specific-auth). Guía escrita para el alcance acordado; no constituye evidencia de la parte B.

## Preparación local posterior del JSON

Se localizó el cliente OAuth dentro de la carpeta `json` del repositorio, ignorado y sin seguimiento ni preparación en Git. Se trasladó a `C:\Users\Richard\json`, fuera del proyecto y conservando la huella del archivo, sin mostrar o copiar sus valores al repositorio.

Se restringieron al propietario los permisos del JSON y su carpeta; se prepararon una llave aleatoria externa de 32 bytes y un directorio privado de tokens, sin generar tokens. Las rutas se guardaron en `backend/.env.local`, ignorado por Git, conservando sus valores anteriores. OAuth se mantiene **desactivado**.

La revisión estructural confirmó tipo `web` y presencia de ID/secreto, pero el JSON omite `redirect_uris`. Richard confirmó que registró en Google Cloud `http://localhost:8080/api/v1/integraciones/google/oauth2/callback`. No se alteró el JSON ni se solicitó otra descarga para añadir metadata ausente. La dirección Gmail de empresa se guardó en `GOOGLE_CUENTA_EMAIL`, solo en la configuración local ignorada. OAuth continúa desactivado hasta una habilitación expresa posterior.

**Ajuste aprobado e implementado:** `GoogleOAuthUsuario.leerCliente` admite que `redirect_uris` esté ausente. Si existe, exige lista válida y coincidencia exacta con el callback fijo. No se aceptan valores del navegador, espacios añadidos, otro host/puerto/ruta ni comodines como sustitutos de esa coincidencia. State de un solo uso, cookie de vínculo, PKCE, nonce, comprobación de cuenta y admin vigente permanecen sin cambios. Este ajuste no comprueba la configuración de la consola Google ni una autorización real: ambas siguen pendientes de comprobación posterior; se conserva la confirmación de Richard sobre el registro de la URI.

Pasos manuales restantes: autorizar por separado la revisión de parte B y el arranque local; habilitar expresamente OAuth cuando Richard lo indique; después iniciar el flujo como admin desde el mismo navegador, elegir la cuenta configurada y consentir. No hace falta otra descarga solo porque falte `redirect_uris`. La secuencia detallada está en «Cómo completar manualmente el consentimiento». El modo En pruebas mantiene su límite de siete días y no se ejecuta ahora ningún consentimiento.

No se ejecutaron pruebas, builds, autorizaciones, envíos, subidas, commit o push. La preparación privada y la revisión de Git no sustituyen las comprobaciones funcionales pendientes de parte B.

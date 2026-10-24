# ADR-0025: Los documentos se copian a Mi unidad/DECOKASA/{país}/{pedido}/{rol}/{persona}

**Fecha**: 2026-10-24
**Estado**: accepted (equipo, Richard, 2026-10-24) para la organización de carpetas. Implementación de la parte A **sin compilar ni verificar**; la comprueba Codex (parte B).
**Decisores**: Richard (equipo). No es una respuesta del cliente.

## Contexto

Desde el paso 3.4 la copia crea `Mi unidad/{país}/{número de pedido}` con IDs reservados y persistidos en `carpeta_drive` (ADR-0017: OAuth de usuario, Mi unidad, scope `drive.file`). Richard aprobó una organización más detallada, que agrupa los documentos por quién los subió y con qué rol, y los separa de otros archivos de la cuenta bajo una carpeta propia. Colombia (ADR-0024) ya tiene pedidos, así que el país es EC o CO.

## Decisión

Cada adjunto se copia a `Mi unidad/DECOKASA/{EC|CO}/{número de pedido}/{rol}/{persona}/`:

- **Rol:** el rol con el que la persona intervino al subirlo. Lo decidió el servidor con la regla P-06 (`PermisosEtapa.paraActuar`): el rol responsable de la etapa si la persona lo tiene (y la etapa está libre o asignada a ella); si no, `admin`. Quien tiene varios roles queda, por tanto, en la carpeta del rol responsable de la etapa, y en `admin` solo si actuó como admin. Se lee del evento ARCHIVO_SUBIDO enlazado al adjunto (relación SUBIDO), que guarda ese rol y no cambia aunque después cambien los roles de la persona.
- **Persona:** el autor (`adjunto.subido_por`, igual al usuario del evento). La carpeta se llama con su nombre al crearla y los 8 últimos dígitos de su UUID; la identidad es el UUID, no el nombre.
- **País y número:** los del pedido en la base (`PedidosParaCopias`), nunca valores del navegador.
- **Claves estables** en `carpeta_drive` (sin migración nueva): `d`, `d/pais/{EC|CO}`, `d/p/{pedido}`, `d/p/{pedido}/{rol}` y `d/p/{pedido}/{rol}/{usuario}`, con UUID en base64url (22 caracteres; máximo 90 de los 100 admitidos). La misma clave se guarda en `appProperties.carpetaClave` de la carpeta.
- **Creación bajo demanda e idempotente:** cada carpeta se crea con la primera copia que la necesita: se reserva un ID de Drive, se persiste (una reserva concurrente perdedora usa la ganadora) y se crea con ese ID; ante 409 o respuesta incierta se consulta ese mismo ID. Es el mecanismo existente del paso 3.4, sin cambios.
- **Nombres:** el nombre visible de la carpeta de persona lo fija la primera reserva; un cambio posterior de nombre no crea otra carpeta. País, número y código de rol no cambian.
- **Sin cambios en permisos:** no se comparten carpetas ni se crean enlaces públicos; el enlace del documento sigue siendo el `webViewLink` que Drive devuelve.
- **Sin migración de documentos:** lo ya copiado con la organización anterior no se mueve ni se borra y conserva su enlace. Una copia que ya había empezado en una carpeta de pedido anterior confirmada termina en ella.

## Alternativas consideradas

### Mantener `Mi unidad/{país}/{pedido}`
- **Pros**: sin cambios.
- **Contras**: no separa los documentos por responsable; mezcla la raíz de Mi unidad con otros archivos de la cuenta.
- **Por qué no**: Richard aprobó la organización nueva.

### Rol o persona por nombre visible
- **Pros**: claves legibles.
- **Contras**: un cambio de nombre crearía otra carpeta; dos personas con el mismo nombre compartirían carpeta.
- **Por qué no**: la petición exige identificadores internos.

### Rol vigente de la persona en el momento de la copia
- **Pros**: no depende del historial.
- **Contras**: con varios roles es ambiguo, y un cambio de roles entre la subida y la copia cambiaría el destino.
- **Por qué no**: el rol de intervención ya está validado y guardado con la subida (R7).

### Mover los documentos ya copiados a la estructura nueva
- **Pros**: una sola organización.
- **Contras**: cambia enlaces ya entregados y añade operaciones remotas de riesgo.
- **Por qué no**: la petición prohíbe mover o borrar documentos existentes.

## Consecuencias

### Positivas
- Cada documento queda junto a los de la misma persona y rol en su pedido, separado por país.
- No hace falta migración: las claves caben en `carpeta_drive` y el rol y el autor ya estaban persistidos.

### Negativas
- Hasta cinco carpetas nuevas en la primera copia (DECOKASA, país, pedido, rol y persona), cada una con su reserva, creación y comprobación; cada copia vuelve a comprobar los cinco niveles, como ya hacía con dos.
- Conviven dos organizaciones si ya hubiera copias anteriores (no consta que existan: la copia real sigue pospuesta, 3.B3).
- El constructor anterior de `CopiarPendientesCasoDeUso` se conserva solo para las pruebas existentes, con la organización anterior; la aplicación usa el nuevo.

### Riesgos
- Una carpeta cambiada a mano en Drive (nombre, padre, papelera) bloquea las copias que la usan (`METADATOS_DIVERGENTES`), igual que antes. No se repara sola.
- Con `drive.file`, la aplicación no ve una carpeta "DECOKASA" creada a mano: crea la suya, y en Mi unidad pueden verse dos carpetas con ese nombre.
- Un adjunto sin evento de subida coherente (no debería ocurrir: se insertan en la misma transacción) no se copia y queda FALLIDO por configuración, sin inventar una carpeta.

## Pendiente del cliente

- Quién tiene acceso directo a Drive (requerimientos 4.7, **TBD**). Esta decisión no comparte nada.

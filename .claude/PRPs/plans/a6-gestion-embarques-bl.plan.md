# Plan: Gestión de embarques, Aprobación de embarque y Aduana (flujo A de Ecuador, v2)

> **Estado: implementado en local el 2026-11-03, sin commit ni despliegue.** Ver «Resultado de la implementación local».
> - Prototipo interactivo: https://claude.ai/artifact/6vFsBEGs7n7XnU8iqnxnPR (privado).
> - Implementación, commits y despliegue solo después de resolver las decisiones de la sección «Decisiones por confirmar».
> - n8n y la lectura automática del Packing List quedan aplazados. El backend de DECOKASA valida el envío y registra el historial.
> - Skills usadas: `agregar-flujo-decokasa`, `ecc:prp-plan`, `ecc:database-migrations` y `shadcn`.

## Summary

- **Fases.** La Fase 1 conserva A1–A5. La Fase 2 agrega «Aprobación de embarque» entre «Gestión de embarques (BL)» y «Aduana Ecuador».
- **Gestión de embarques.** cdtulcan elige cuántos BL crear y guarda cada uno por separado. Puede vincular pedidos a un BL compartido. «Enviar a aprobación» valida en el servidor y registra una foto del envío.
- **Aprobación.** Admin y Asistente de compras comparan solicitud y Packing Lists. «Aprobar» lleva a Aduana. «Rechazar» exige motivo y devuelve el pedido a cdtulcan.
- **Aduana.** Checklist de 16 documentos, un botón de carga por requisito y contador de avance.
- **Cronómetro.** Cada etapa activa muestra el tiempo restante según `venceEn` del servidor, con «Vencido» y «Sin plazo».
- **Sesión.** Token de acceso de 20 minutos. Los enlaces de recuperación no cambian.
- **Avisos.** Correo con la cola existente de ADR-0032. WhatsApp simulado hasta confirmar proveedor, número y credenciales.
- **Versión.** Se publica A/EC v2, con 12 etapas. Los pedidos en curso siguen en v1 hasta Fin.

## User Stories

- **Compras China:** quiero registrar cada BL y enviarlos juntos a aprobación, para dejar trazado qué pedidos y qué Packing List presenté.
- **Asistente de compras o Admin:** quiero ver la solicitud junto a los Packing Lists, para aprobar o devolver con un motivo.
- **Responsable de Aduana:** quiero ver qué documentos faltan, para completar la etapa sin una lista externa.

## Metadata

- **Complexity:** XL, en 5 entregas.
- **Source PRD:** N/A. Pedido directo de Richard, 2026-11-03. Requisitos de origen: `requerimientos.md` 1.4 y 4.4.
- **Estimated Files:** unos 60.

---

## Resultado de la implementación local (2026-11-03)

**Cambios respecto a este plan, por indicación de Richard:**

- Un BL en revisión en otro pedido **no se congela**. Si cambia su Packing List, el pedido afectado debe revisar la versión nueva antes de aprobar.
- Con una aprobación previa, se conserva la versión aprobada y se bloquea la sustitución. No hay `BL_EN_APROBACION`; se usa `TRANSICION_NO_PERMITIDA`.
- Se pueden vincular pedidos anteriores (v1), solo como información.
- `aprobaciones_requeridas` no se creó: hay una aprobación provisional.
- El Packing List se liga al BL con `POST .../embarques/{blId}/packing-list` después de la carga, en lugar de `carga_archivo.bl_id`. La carga reanudable no cambió.
- WhatsApp simulado solo para el rol `compras_china`, y sin teléfonos.
- Los requisitos de Aduana quedan `PENDIENTES` en V33. Los 16 ficticios están en `backend/semilla/aduana-ficticia.sql`, que solo corre en una base de prueba.

**Verificado:** migración sobre una copia de la base ficticia y recorrido en Chrome con 46 comprobaciones y revisión en pantalla del filtro y del Inicio. Almacenamiento en disco local, buzón simulado y WhatsApp simulado. Unitarias y ArchUnit en verde, salvo `TareaCopiaB1Test`, que falla por su cuenta de líneas de log.

**Sin verificar:** pruebas de integración (Docker apagado), modo Drive, y Vercel + Render.

## Ampliación del 2026-11-14 (V34)

Pedido del cliente: importadora por BL; checklist oficial de 16 documentos en Aduana; cuenta regresiva de cada etapa desde la creación, con alerta `PLAN_VENCIDO` por la aplicación, el correo y WhatsApp; WhatsApp Cloud API lista y apagada (`WHATSAPP_MODO=simulado`); token de 15 minutos. Verificado en la base de prueba con 14 comprobaciones; WhatsApp real sin probar, por falta de credenciales.

## Decisiones por confirmar

Son el bloqueo real antes de implementar. La columna «Propuesta» dice qué asume este plan.

### Del cliente

| # | Decisión | Propuesta | Si no se decide |
|---|---|---|---|
| C1 | ¿Basta **una** aprobación autorizada o el cliente quiere **dos**? | **Una** de Admin o Asistente de compras. El prototipo permite probar las dos | Bloquea la entrega 3 |
| C2 | Lista oficial de los **16 documentos** de Aduana | Los nombres del prototipo son provisionales y no se siembran | Bloquea publicar v2 (ver T1) |
| C3 | ¿Los 16 documentos se exigen **por pedido o por BL**? | Por pedido, con el motor actual. Por BL necesita otro diseño | Bloquea la entrega 4 |
| C4 | ¿Aduana, Autorización de salida y Bodega avanzan por pedido o por BL? | Por pedido | Sigue el avance por pedido |
| C5 | Plazo de A7 Aprobación de embarque | 24 horas hábiles, igual que A3 | Se siembra `PENDIENTE` y se muestra «Sin plazo» |
| C6 | Plazo al devolver por rechazo | Ver «Cronómetro y plazos»: 2 días hábiles para corregir | Se aplica la regla actual: 10 días completos |
| C7 | ¿Quién corrige un BL ya aprobado en otro pedido? | Solo Admin, con una corrección controlada y nueva revisión del pedido afectado | Queda bloqueado: 409 `BL_BLOQUEADO` |
| C8 | ¿`MEDU-4823910` y `MEDU4823910` son el mismo BL? ¿El código es único por país? | Se conservan los guiones; único por país | Se aplica la propuesta |

### De Richard

| # | Decisión | Propuesta |
|---|---|---|
| R1 | Aprobar que el token de acceso pase a 20 minutos en ADR-0002, código y `requerimientos.md` 4.7 | Aprobar. Ver «Sesión» |
| R2 | Quitar el botón anterior «Confirmar que toda la mercancía está asignada» | Sí. Pasa a ser una casilla obligatoria dentro de «Enviar a aprobación» |
| R3 | Quien envió no aprueba su propio envío | Sí. Afecta sobre todo a Admin, que puede actuar en A6 y en A7 |
| R4 | ¿Cuándo se activa el correo real? | Decisión aparte. Este plan no cambia ningún interruptor |
| R5 | WhatsApp: proveedor, número y credenciales | Meta WhatsApp Cloud API directa. Ver «Avisos» |

---

## Origen de cada regla

| Regla | Origen | Estado |
|---|---|---|
| Fase 1 = A1–A5; Fase 2 desde A6 | Richard, 2026-11-03 | Para implementar |
| «Aprobación de embarque» entre A6 y Aduana | Richard, 2026-11-03 | Para implementar |
| Varios BL por pedido, guardados por separado, y BL compartidos | Richard, 2026-11-03 | Para implementar |
| «Enviar a aprobación» valida campos y archivos; registra BL, pedidos y versión de cada Packing List | Richard, 2026-11-03 | Para implementar |
| Admin y Asistentes de Compras aprueban o rechazan; el rechazo exige motivo | Richard, 2026-11-03 | Para implementar |
| Checklist de 16 documentos en Aduana | Richard, 2026-11-03 | Lista oficial ⏸ C2 |
| Cronómetro, «Vencido» y «Sin plazo» | Richard, 2026-11-03 | Para implementar |
| Token de acceso de 20 minutos | Richard, 2026-11-03 | Falta registrar el cambio (R1) |
| Aviso a Compras al enviar y a cdtulcan al rechazar | Richard, 2026-11-03 | Ya cubierto por ADR-0032 |
| Un BL en un envío pendiente queda congelado | **[P]** | Pendiente de aprobación |
| Código y Packing List se bloquean al aprobar | **[P]** | Pendiente de aprobación |
| Tránsito automático = 45 días corridos (V25), como referencia | **[P]** | Pendiente de aprobación |
| Plazo de A6: 10 días hábiles | Cliente (`requerimientos.md` 4.4) | Sin cambios |

No se extiende nada por analogía a B7 ni a Colombia.

---

## Recorrido

```
Fase 1 · Pedido y fabricación     Fase 2 · Logística
A1 A2 A3 A4 A5                    A6 Gestión de embarques (BL)
                                  A7 Aprobación de embarque   ← nueva
                                  A8 Aduana Ecuador
                                  A9 Autorización de salida
                                  A10 Bodega
                                  A11 Incidencias
                                  A12 Fin
```

| Estado | Quién actúa | Acción | Resultado |
|---|---|---|---|
| A6 | cdtulcan o Admin | Enviar a aprobación | Se valida, se guarda el envío N y pasa a A7 |
| A7 | Asistente de compras o Admin | Aprobar | Se bloquean sus BL y pasa a A8 |
| A7 | Asistente de compras o Admin | Rechazar con motivo | Vuelve a A6 con una entrada nueva; los BL y archivos siguen ahí |
| A6 tras rechazo | cdtulcan | Corregir y reenviar | Envío N+1, con su propia foto |
| A8 | Compras China (rol actual de Aduana) | Aprobar con los 16 confirmados | Pasa a A9 |

### Ejemplo del prototipo: BL-003 compartido

1. cdtulcan guarda BL-003 en el pedido 24 y vincula el pedido 23.
2. Envía los pedidos 23 (BL-001, BL-002, BL-003) y 24 (BL-003).
3. Marcela compara. El Packing List v1 trae 500 m² de césped en lugar de 600, y rechaza señalando BL-003.
4. Como BL-003 está en aprobación en los dos pedidos, se devuelven juntos con el mismo motivo.
5. cdtulcan reemplaza el Packing List: nace la v2 y la v1 se conserva.
6. Reenvía los dos pedidos. Cada uno tiene su envío 2 con la v2.
7. Marcela aprueba y ambos pasan a Aduana.

---

## Reglas del envío

### Validación en el servidor

`POST /pedidos/{id}/acciones` con `ENVIAR` desde una etapa con `gestiona_embarques`, en una transacción:

1. Bloquea el pedido y después sus BL, en orden fijo por id.
2. Exige `confirmaAsignacion = true`.
3. Exige al menos un BL vinculado.
4. Para cada BL exige código, fecha de salida y Packing List con adjunto confirmado. En modo Drive significa estado `COPIADO`.
5. Si falta algo, responde 400 `REQUISITO_FALTANTE` con `faltantes[]`. No guarda nada.
6. Inserta el envío y su foto, ejecuta la transición y escribe los eventos.

La llegada estimada puede quedar «por definir». Una carga en curso no cuenta: un adjunto solo existe cuando el servidor lo confirmó (ADR-0034). Si un BL no va a salir, se desvincula y queda en el historial.

### Qué se registra

- Número de envío, quién envió, cuándo y la entrada de A6.
- Por cada BL: código, fechas, origen de la llegada, versión del Packing List y adjunto.
- Por cada BL: pedidos vinculados en ese momento, quién vinculó cada uno y cuándo.
- Cada rechazo, corrección y decisión, con su autor y motivo.

La foto no se actualiza nunca. Si después cambia un archivo o una fecha, el envío anterior sigue mostrando lo presentado.

### BL compartidos

| Situación | Regla [P] |
|---|---|
| El BL está en un envío pendiente de cualquier pedido | No se edita ni se vincula: 409 `BL_EN_APROBACION` |
| Se rechaza un pedido por un BL que también está pendiente en otro | El diálogo lo avisa y devuelve los dos en una transacción, porque sin eso el BL no se puede corregir |
| Se reemplaza el Packing List | Nace una versión nueva. Queda un evento `CORRECCION` en todos los pedidos del BL |
| Un pedido se aprueba | Sus BL se bloquean: código y Packing List ya no cambian |
| Otro pedido vincula después un BL bloqueado | Se permite: solo añade el vínculo. Su envío presentará la versión vigente |
| Se rechaza un pedido por un BL bloqueado por otra aprobación | No se puede corregir hasta decidir C7 |

Los rechazos conservan los BL y sus archivos. No se aplica la regla de «documentos en la entrada actual»: el requisito se evalúa sobre el BL. Así cdtulcan no vuelve a subir lo que estaba bien.

---

## Aprobación de embarque

- **Quién:** rol de etapa `asistente_compras` del país del pedido, persona asignada (P-06) o Admin. `asistente_china` no aprueba.
- **Qué ve:** documentos de solicitud y Packing Lists, uno junto a otro. El sistema no compara ni calcula diferencias.
- **Permiso ya existente:** ADR-0028 deja a quien actúa en la etapa actual abrir todos los documentos del pedido.
- **Falta:** abrir un Packing List subido desde **otro** pedido del BL, y versiones anteriores. Lo resuelve una ruta por versión que comprueba el vínculo, o la foto del envío.
- **Aprobar:** `APROBAR 7 → 8`.
- **Rechazar:** `RECHAZAR 7 → 6`, con `requiere_observacion = true`, y opcionalmente los BL señalados.
- **Una o dos aprobaciones (C1):** la columna `aprobaciones_requeridas` de la transición vale 1. Con 1 el motor no cambia. Con 2, la primera decisión queda en `envio_embarque_decision` sin avanzar; la segunda debe ser de otra persona.
- **Sin autoaprobación (R3):** quien envió recibe 403 `SIN_PERMISO` al aprobar su envío.

---

## Aduana

- **Motor existente:** los 16 tipos son filas de `documento_requerido` en `APROBAR 8 → 9`. El motor ya exige cada tipo en la entrada actual y calcula `puedeCompletarse`.
- **Se añade:** `documento_requerido.orden`, para mostrar el checklist en orden.
- **Marcar un requisito:** el estado sale del servidor. Se marca solo cuando existe un adjunto confirmado de ese tipo en la entrada actual. El progreso local no marca nada.
- **Carga:** un botón por requisito con el tipo ya fijado, sobre la carga reanudable actual.
- **Contador:** `N/16`, calculado con la respuesta del servidor.
- **Por BL (C3):** exigiría relacionar cada adjunto con su BL y otra regla del motor. No se construye sin decisión.

---

## Cronómetro y plazos

### Pantalla

| Estado | Condición del servidor | Texto |
|---|---|---|
| A tiempo | `venceEn` futuro | «Quedan 2 d 04 h 10 min» |
| Por vencer | Menos de 24 horas | «Por vencer · 05 h 10 min» |
| Vencido | `vencida = true` | «Vencido hace 1 d 02 h» |
| Sin plazo | Sin `venceEn` (`PENDIENTE` o `SIN_PLAZO`) | «Sin plazo» |

- `venceEn`, `vencida` y `estadoPlazo` ya están en la API. Se añade `servidorAhora`, para que un reloj de PC desfasado no cambie el resultado.
- El contador muestra tiempo corrido hasta `venceEn`. Las horas hábiles ya están incluidas en esa fecha. Por eso un fin de semana también descuenta del número visible.
- Aduana conserva hoy el plazo `PENDIENTE`, así que se verá «Sin plazo». La referencia de 45 días no es un plazo.

### Propuesta al devolver por rechazo (C6)

**Hoy:** el plazo normal se reinicia completo en cada reentrada (`Pedido.java`, línea 47). A6 volvería a tener 10 días hábiles por corregir un archivo, y el retraso se escondería.

**Propuesta:**

- La transición `RECHAZAR 7 → 6` define un **plazo de corrección de 2 días hábiles**, en vez de los 10.
- Cada envío reinicia completo el plazo de A7, porque es una revisión nueva.
- El historial conserva cada entrada, y el contador `reentradas` sigue visible.
- Es genérico: si `plazo_reentrada_*` es nulo, el comportamiento actual no cambia.

Alternativa descartada: continuar el saldo de los 10 días. Si ya se agotó, el pedido volvería vencido, sin tiempo real para corregir.

---

## Sesión

**Identificación:** el token de 15 minutos es el JWT de acceso (`JWT_DURACION_ACCESO`, `decokasa.sesion.duracion-acceso`). No es la cookie de renovación de 8 horas ni el enlace de recuperación.

### Verificado el 2026-11-03

Entorno ficticio, modo local, sin Google y sin cambios de código:

| Prueba | Resultado |
|---|---|
| `JWT_DURACION_ACCESO=20m` | `exp - iat = 1200` segundos y `expiraEn = 1200` |
| Carga de 14 MiB en 4 fragmentos, con un token que caduca a mitad | `PUT` 200 ×3 → `PUT` 401 → `POST /auth/renovar` 200 → `PUT` 200 → finalización 200 |
| Integridad | Un solo adjunto, SHA-256 igual al del archivo, sesión abierta, sin alertas |
| Enlaces de recuperación | Propiedad independiente: `RECUPERACION_DURACION`, por defecto `PT30M`. No se toca |

**Hallazgos:**

- El token fue aceptado unos 50 segundos después de una vida de 3 segundos y rechazado a los 75. Coincide con la tolerancia por defecto de 60 segundos de Spring Security, que el código no cambia. La vida real sería de unos 21 minutos.
- El navegador renueva solo al recibir un 401, no antes de caducar. Basta porque cada fragmento se repite.
- No hay una prueba automática de 401 durante un fragmento. Se añade.

**Sin verificar:** modo Drive, Vercel + Render y el límite de 8 horas durante una carga. Al pasar ese límite la sesión termina, pero la carga es reanudable tras volver a entrar. Se prueba en la entrega 2.

### Cambios al aprobar R1

| Archivo | Cambio |
|---|---|
| `application.yml`, `docker-compose.yml`, `.env.example` | `20m` por defecto |
| `usuarios/domain/PoliticaSesion.java` | `ACORDADA` a 20 minutos |
| `usuarios/application/usecase/Autorizacion.java` | Comentario |
| `IniciarSesionTest`, `RenovarSesionTest`, `AuthControllerIT` | Expectativas |
| `ADR-0002`, `requerimientos.md` 4.7, `openapi.yaml`, `CLAUDE.md` | Texto |
| Render | Revisar si `JWT_DURACION_ACCESO` está definida y actualizarla |

**Efecto de seguridad:** un JWT robado o de una cuenta desactivada vive 5 minutos más. La administración se decide con el estado actual en la base (A1) y un cambio de contraseña invalida los JWT anteriores (ADR-0033).

---

## Avisos

### Correo

**No hace falta código nuevo para los destinatarios:** `RegistrarAvisosDeEtapaCasoDeUso` ya lo resuelve desde la versión del flujo.

| Acción | Tipo | Reciben |
|---|---|---|
| Enviar a aprobación | `LLEGADA` | Asistentes de compras activos del país, o la persona asignada, y Admin |
| Rechazar | `DEVOLUCION` | cdtulcan por rol de A6 o asignación, y Admin |
| Aprobar | `LLEGADA` | Responsables de Aduana y Admin |

- Va a la dirección registrada del usuario. Quien actuó no se avisa a sí mismo. Admin siempre recibe.
- Cada pedido devuelto por un BL compartido genera su propio aviso.
- **Se añade:** el motivo, los BL señalados y un texto claro en la plantilla.
- **Hoy no sale ningún correo.** Hacen falta `GOOGLE_GMAIL_HABILITADA`, `CORREO_AUTOMATICO` y `NOTIFICACIONES_ENVIAR_DESDE`. Los avisos siguen en Mi bandeja y en la campana. Activarlo es la decisión R4.

Esto sale de leer el código; no se ejecutó un envío real.

### WhatsApp

**Revisión:** el repositorio y sus documentos no tienen ninguna integración de WhatsApp ni n8n. Entiendo «la API existente» como la API oficial de Meta. Si el cliente ya tiene otra, hay que decirlo en R5.

**Propuesta de conexión directa:** Spring Boot llama a WhatsApp Cloud API, sin intermediarios.

- Envío: `POST /{PHONE_NUMBER_ID}/messages` con `type: template`.
- Fuera de la ventana de 24 horas iniciada por el usuario solo se pueden enviar plantillas, y los avisos de DECOKASA casi siempre serán ese caso. Hacen falta plantillas aprobadas por Meta.
- El precio es por mensaje. La tarifa se confirma con la tabla vigente de Meta antes de contratar.
- Los estados de entrega llegan por webhook. La firma del webhook se confirma con la documentación de Meta al implementar.

Fuentes: [precios](https://developers.facebook.com/documentation/business-messaging/whatsapp/pricing), [Messages API](https://developers.facebook.com/docs/whatsapp/cloud-api/reference/messages/), [webhooks](https://developers.facebook.com/documentation/business-messaging/whatsapp/webhooks/overview/).

**Diseño:**

- Puerto `MensajeroWhatsApp`, con adaptadores `WhatsAppSimulado` (por defecto) y `WhatsAppCloud` (cuando haya credenciales).
- `WHATSAPP_MODO=simulado`: guarda la fila con estado `SIMULADO`, sin llamadas de red.
- Tabla `notificacion_whatsapp`, separada del outbox de correo, con la misma idea: se escribe en la transacción y se envía después.
- Logs sin teléfono ni contenido.

**Antes de activarlo:**

- `Telefono` admite números sin `+` ni código de país; WhatsApp necesita el número internacional. Hay que revisar los teléfonos registrados.
- Consentimiento de cada persona para recibir WhatsApp.
- El equipo en China puede no recibirlo sin VPN, igual que ocurre con Google. El correo debe seguir siendo el canal principal.

---

## Modelo de datos

### V32: esquema (solo aditivo)

```sql
alter table definicion_etapa add column fase_numero smallint, add column fase_nombre text,
  add column gestiona_embarques boolean not null default false;
alter table transicion_permitida
  add column exige_embarques_listos boolean not null default false,
  add column aprobaciones_requeridas smallint not null default 1
      check (aprobaciones_requeridas between 1 and 2),
  add column plazo_reentrada_cantidad integer,
  add column plazo_reentrada_tipo text;                 -- nulos: se usa el plazo normal de la etapa
alter table documento_requerido add column orden smallint;

create table embarque_bl (
  id uuid primary key,
  pais_codigo text not null references pais(codigo),
  codigo text not null check (char_length(codigo) between 3 and 40),
  codigo_normalizado text not null,
  fecha_salida date,
  llegada_estimada date,
  llegada_origen text not null default 'SIN_DATOS'
      check (llegada_origen in ('AUTOMATICA','MANUAL','SIN_DATOS')),
  transito_dias smallint,
  bloqueado_en timestamptz,
  creado_por uuid not null references usuario(id), creado_en timestamptz not null,
  actualizado_en timestamptz not null, version bigint not null default 0,
  constraint embarque_bl_codigo_unico unique (pais_codigo, codigo_normalizado),
  constraint embarque_bl_llegada_coherente check ((llegada_origen = 'SIN_DATOS') = (llegada_estimada is null)),
  constraint embarque_bl_llegada_despues check (llegada_estimada is null or fecha_salida is null
      or llegada_estimada >= fecha_salida)
);

-- Solo inserción: cada reemplazo crea una versión. El archivo es un adjunto normal y se guarda una vez.
create table embarque_bl_packing_list (
  id uuid primary key,
  bl_id uuid not null references embarque_bl(id),
  version integer not null,
  adjunto_id uuid not null unique references adjunto(id),
  subido_por uuid not null references usuario(id), subido_en timestamptz not null,
  constraint embarque_bl_pl_version unique (bl_id, version)
);
alter table embarque_bl add column packing_list_vigente_id uuid references embarque_bl_packing_list(id);

create table embarque_bl_pedido (
  bl_id uuid not null references embarque_bl(id),
  pedido_id uuid not null references pedido(id),
  entrada_id uuid not null references entrada_etapa(id),
  orden smallint not null,
  vinculado_por uuid not null references usuario(id), vinculado_en timestamptz not null,
  desvinculado_por uuid references usuario(id), desvinculado_en timestamptz,
  constraint embarque_bl_pedido_desvinculo check ((desvinculado_en is null) = (desvinculado_por is null))
);
create unique index embarque_bl_pedido_vigente on embarque_bl_pedido (bl_id, pedido_id) where desvinculado_en is null;
create unique index embarque_bl_pedido_orden on embarque_bl_pedido (pedido_id, orden) where desvinculado_en is null;

create table envio_embarque (
  id uuid primary key,
  pedido_id uuid not null references pedido(id),
  entrada_id uuid not null references entrada_etapa(id),
  numero integer not null,
  confirma_asignacion boolean not null check (confirma_asignacion),
  enviado_por uuid not null references usuario(id), enviado_en timestamptz not null,
  estado text not null default 'PENDIENTE' check (estado in ('PENDIENTE','RECHAZADO','APROBADO')),
  cerrado_en timestamptz,
  constraint envio_embarque_numero unique (pedido_id, numero)
);
create unique index envio_embarque_pendiente on envio_embarque (pedido_id) where estado = 'PENDIENTE';

-- Foto del envío. Solo inserción, protegida por trigger como entrada_etapa.
create table envio_embarque_bl (
  envio_id uuid not null references envio_embarque(id),
  bl_id uuid not null references embarque_bl(id),
  codigo text not null, fecha_salida date not null,
  llegada_estimada date, llegada_origen text not null,
  packing_list_id uuid not null references embarque_bl_packing_list(id),
  primary key (envio_id, bl_id)
);
create table envio_embarque_bl_pedido (
  envio_id uuid not null, bl_id uuid not null,
  pedido_id uuid not null references pedido(id),
  vinculado_por uuid not null references usuario(id), vinculado_en timestamptz not null,
  primary key (envio_id, bl_id, pedido_id),
  foreign key (envio_id, bl_id) references envio_embarque_bl(envio_id, bl_id)
);
-- Solo inserción. Una decisión por persona y envío.
create table envio_embarque_decision (
  id uuid primary key,
  envio_id uuid not null references envio_embarque(id),
  decision text not null check (decision in ('APROBADA','RECHAZADA')),
  usuario_id uuid not null references usuario(id),
  motivo text, bl_senalados uuid[] not null default '{}',
  decidido_en timestamptz not null,
  constraint envio_decision_motivo check (decision <> 'RECHAZADA' or length(trim(motivo)) > 0),
  constraint envio_decision_persona unique (envio_id, usuario_id)
);

alter table carga_archivo add column bl_id uuid references embarque_bl(id);
-- Se recrea el CHECK de evento_pedido.tipo con BL_CREADO, BL_ACTUALIZADO, BL_VINCULADO, BL_DESVINCULADO,
-- BL_PACKING_LIST_CORREGIDO y ENVIO_EMBARQUE_DECIDIDO.
```

**Por qué así:**

- La foto del envío evita reconstruir lo presentado con datos que luego cambian.
- `embarque_bl_pedido` y `embarque_bl_packing_list` no borran filas, con el mismo criterio que `entrada_etapa`.
- Sin `CONCURRENTLY`: las tablas son nuevas o pequeñas y Flyway es transaccional.
- Esquema y datos van separados: V32 esquema, V33 flujo v2.
- Reversión solo hacia adelante: una migración nueva reactiva v1. Los pedidos nacidos en v2 siguen en v2.

### V33: A/EC v2

- Desactiva v1 comprobando que cambió una fila; inserta v2 completa con `vigente = true`.
- **12 etapas y 16 transiciones.** No se comparten filas con v1.

| Etapa v2 | Viene de | Cambios |
|---|---|---|
| A1–A5 | v1 | Fase 1. Sin cambios |
| A6 Gestión de embarques (BL) | A6 | Fase 2, `gestiona_embarques = true`. `ENVIAR → 7` con `exige_embarques_listos`, sin documento requerido |
| A7 Aprobación de embarque | **Nueva** | `asistente_compras`, `revisa_etapa = 6`. `APROBAR → 8`; `RECHAZAR → 6` con observación y plazo de reentrada |
| A8 Aduana Ecuador | A7 | `registra_llegada`, estimación de 45 días, `APROBAR → 9` con los 16 documentos, `REPORTAR_DEMORA` |
| A9 Autorización de salida | A8 | Número nuevo |
| A10 Bodega | A9 | Número nuevo, `requiere_bodega`, algún documento |
| A11 Incidencias | A10 | Número nuevo, algún documento |
| A12 Fin | A11 | Automática |

El bloque `DO` comprueba una sola vigente, v1 intacta, B/EC y Colombia sin cambios, y ningún pedido en v2.

### Pedidos en curso

- Siguen en v1 hasta Fin. `pedido.flujo_id` no cambia y un trigger (V2) lo protege.
- Un pedido en A6 v1 conserva «Embarque», su regla de Packing List y sus adjuntos. No crea BL ni pasa por Aprobación de embarque.
- Los Packing Lists anteriores no se convierten en BL porque no tienen código ni fechas.
- Los pedidos nuevos nacen en v2.

---

## Tema de atención: el número de etapa cambia de significado

El plan anterior no cambiaba números. Este sí: **A7 es Aduana en v1 y Aprobación de embarque en v2.** La skill (§1-bis.5) exige resolverlo antes de publicar.

| Sitio | Problema | Solución |
|---|---|---|
| `ListaPedidos`, `BandejaPedidos`, `TableroTrabajo` | Dibujan con la definición vigente: un pedido v1 de 11 etapas se vería con 12 | Añadir `versionFlujo` a `PedidoResumen` y cargar la definición de cada versión |
| Filtros por etapa | «A7» mezclaría dos etapas | Filtrar por versión y etapa |
| `EstadisticasDePedidosSql.java:152` | Agrupa por `tipo_flujo, numero` con `min(nombre)`: une Aduana y Aprobación | Agrupar también por versión o por nombre |
| Consultas 114 y 122 de ese archivo | Unen por `flujo_id`, así que son correctas | Prueba de regresión |
| `EtapasHabilitadas.segunDefinicion()` | Sale de la definición | Sin cambios |

`nombreEtapaActual`, avisos, plazos y cronograma ya usan la versión del pedido.

---

## Permisos

| Operación | Quién | Condiciones |
|---|---|---|
| Crear o editar un BL, subir o reemplazar su Packing List, vincular o desvincular | Quien puede actuar en A6 del pedido (P-06) | BL sin envío pendiente ni bloqueo |
| Enviar a aprobación | Quien puede actuar en A6 | Validación del servidor |
| Aprobar o rechazar | Quien puede actuar en A7 | No puede ser quien envió (R3) |
| Ver BL, envíos e historial | Quien puede consultar el pedido | País asignado |
| Abrir un Packing List | P-17 y ADR-0028 evaluados sobre el pedido | BL vinculado, o incluido en un envío del pedido. Consulta sigue sin abrirlo |
| Buscar un BL | Quien puede actuar en A6 del pedido destino | Código exacto y mismo país |

---

## Códigos y llegada

- **Duplicados:** `trim`, mayúsculas y sin espacios internos. La unicidad la asegura el índice; 23505 se traduce a 409 `BL_DUPLICADO`, como `RestriccionesUnicasUsuario`.
- **Llegada automática:** salida + `estimacion_*` de la etapa con `registra_llegada`.
- **Llegada manual:** se conserva si cambia la salida, y no puede ser anterior a ella.
- **Sin datos:** «Llegada por definir».

---

## API (OpenAPI 0.29.0-borrador)

```
GET    /api/v1/pedidos/{id}/embarques
POST   /api/v1/pedidos/{id}/embarques
PATCH  /api/v1/pedidos/{id}/embarques/{blId}                      {version, ...}
POST   /api/v1/pedidos/{id}/embarques/vinculos                    {codigo}
POST   /api/v1/embarques/{blId}/vinculos                          {pedidoId}
DELETE /api/v1/pedidos/{id}/embarques/{blId}/vinculo
GET    /api/v1/pedidos/{id}/envios-embarque                       envíos, foto y decisiones
GET    /api/v1/pedidos/{id}/embarques/{blId}/packing-lists/{plId}/contenido
POST   /api/v1/pedidos/{id}/etapas/6/archivos/cargas              {..., blId}
POST   /api/v1/pedidos/{id}/acciones                              ENVIAR {confirmaAsignacion}
                                                                  APROBAR
                                                                  RECHAZAR {observacion, blSenalados[], devolverTambien[]}
```

Códigos nuevos: `BL_DUPLICADO`, `BL_BLOQUEADO` y `BL_EN_APROBACION`. Se reutilizan `REQUISITO_FALTANTE`, `VERSION_DESACTUALIZADA` y `SIN_PERMISO`.

---

## Files to Change

### Backend

| File | Action | Justification |
|---|---|---|
| `db/migration/V32__embarques_y_aprobacion.sql` | CREATE | Esquema |
| `db/migration/V33__flujo_a_ecuador_v2.sql` | CREATE | Versión 2 |
| `flujos/domain/*`, `flujos/adapter/**` | UPDATE | Fases y atributos nuevos |
| `pedidos/domain/ReglaDeTransicion.java` | UPDATE | Atributos con constructores compatibles |
| `pedidos/domain/Pedido.java` | UPDATE | Plazo de reentrada, genérico |
| `pedidos/application/port/out/EmbarquesDelPedido.java` | CREATE | Faltantes, registrar envío, decidir, bloquear |
| `pedidos/application/usecase/EjecutarAccionEtapaCasoDeUso.java` | UPDATE | Validar y registrar en la misma transacción |
| `pedidos/application/port/in/VistasDePedido.java`, `PedidoRespuestas.java` | UPDATE | `versionFlujo` en el resumen y `servidorAhora` |
| `pedidos/adapter/out/persistence/EstadisticasDePedidosSql.java` | UPDATE | Agrupar por versión |
| `embarques/**` | CREATE | Dominio, casos de uso, SQL, controlador y configuración |
| `archivos/application/usecase/GestionarCargaCasoDeUso.java`, `CargaADriveCasoDeUso.java` | UPDATE | `blId` y versión del Packing List en la misma transacción |
| `archivos/application/usecase/DescargarArchivoCasoDeUso.java` | UPDATE | Variante desde el BL o el envío |
| `historial/**` | UPDATE | Textos de eventos |
| `notificaciones/application/usecase/RegistrarAvisosDeEtapaCasoDeUso.java` | UPDATE | Motivo y BL señalados en la plantilla |
| `notificaciones/application/port/out/MensajeroWhatsApp.java`, `adapter/out/whatsapp/*` | CREATE | WhatsApp simulado |
| Archivos de sesión | UPDATE | Ver «Sesión» |
| `arquitectura/ReglasArquitecturaTest.java` | UPDATE | Módulo `embarques` |

### Frontend

| File | Action | Justification |
|---|---|---|
| `api/embarques.ts`, `api/cargas.ts`, `api/pedidos.ts` | CREATE/UPDATE | Contrato |
| `components/pedidos/EmbarquesEtapa.tsx`, `BloqueBl.tsx`, `DialogoVincularBl.tsx` | CREATE | A6 |
| `components/pedidos/DialogoEnviarEmbarque.tsx` | CREATE | Foto previa y confirmación |
| `components/pedidos/AprobacionEmbarque.tsx`, `CompararDocumentos.tsx`, `DialogoRechazoEmbarque.tsx` | CREATE | A7 |
| `components/pedidos/ChecklistAduana.tsx` | CREATE | A8 |
| `components/pedidos/Cronometro.tsx` | CREATE | Tiempo restante |
| `components/pedidos/PanelEtapa.tsx`, `DetallePedido.tsx` | UPDATE | Mostrar cada sección según la definición |
| `components/pedidos/LineaDeTiempo.tsx`, `LineaCompacta.tsx` | UPDATE | Fases |
| `ListaPedidos.tsx`, `BandejaPedidos.tsx`, `TableroTrabajo.tsx` | UPDATE | Definición por versión |

Solo componentes shadcn ya instalados.

### Documentación

ADR-0035 (embarques, aprobación y versión), ADR-0002 (sesión), ADR nuevo de WhatsApp, `requerimientos.md`, `modelo-datos.md`, `arquitectura.md`, `openapi.yaml` y el manual de usuario.

## NOT Building

- n8n y cualquier orquestador externo.
- Lectura automática del Packing List y formularios de productos o cantidades.
- Comparación automática entre solicitud y Packing List.
- Avance de Aduana, Autorización o Bodega por BL (C4).
- Checklist por BL hasta decidir C3.
- BL en el flujo B y en Colombia.
- Migrar pedidos de v1 a v2.
- Envío real por WhatsApp.
- Activar el correo real.
- Consulta a navieras.

---

## Entregas

### Entrega 1: base y sesión

1. Inventario de producción, solo lectura: pedidos por versión y etapa.
2. V32.
3. Motor: atributos, puerto y plazo de reentrada.
4. Sesión de 20 minutos con prueba de 401 durante un fragmento.
5. `versionFlujo` en listas y estadísticas por versión.

**Validación:** regresión de A v1, B y Colombia.

### Entrega 2: BL y envío

1. Módulo `embarques`: dominio, API y SQL.
2. Packing List por BL con versiones.
3. Envío con validación y foto.
4. Pantallas de A6.
5. Renovación durante una carga en modo Drive, en un entorno aislado.

### Entrega 3: aprobación e historial

1. Decisiones, rechazo con BL compartido y bloqueo.
2. Pantalla de A7, comparación e historial.
3. Plantillas de correo con motivo.

### Entrega 4: Aduana y cronómetro

1. V33 con la lista oficial.
2. Checklist de Aduana.
3. Cronómetro en cada etapa activa.
4. Llegada del pedido en Aduana: la más tardía de sus BL. Prevalecen la llegada real y la demora.

### Entrega 5: WhatsApp simulado y documentación

1. Puerto, simulador y tabla.
2. Manual y capturas.

**V33 se publica al final**, cuando estén listas las pantallas. Hasta entonces los pedidos nuevos siguen naciendo en v1.

---

## Testing Strategy

| # | Prueba | Esperado |
|---|---|---|
| 1 | Migración limpia y con pedidos en curso | v2 con 12 etapas y 16 transiciones; v1 intacta |
| 2 | Pedido v1 en A7 tras publicar v2 | Se muestra Aduana y sigue reglas de v1 |
| 3 | Estadísticas con A7 v1 y A7 v2 | Dos filas distintas |
| 4 | Guardar 3 BL por separado | Orden 1, 2 y 3; eventos `BL_CREADO` |
| 5 | Código duplicado y altas simultáneas | Una 201 y otra 409 |
| 6 | Enviar sin BL | 400 `REQUISITO_FALTANTE` |
| 7 | Enviar con Packing List sin confirmar | 400; nada guardado |
| 8 | Enviar sin confirmar asignación | 400 |
| 9 | Enviar con todo listo | Foto correcta con pedidos, autor del vínculo y versión |
| 10 | Dos envíos simultáneos | Uno entra; el índice parcial rechaza el otro |
| 11 | Reemplazar Packing List tras enviar | La foto conserva la versión presentada |
| 12 | Editar un BL en envío pendiente de otro pedido | 409 `BL_EN_APROBACION` |
| 13 | Rechazar sin motivo | 400 |
| 14 | Rechazar BL compartido | Los dos pedidos vuelven a A6 en una transacción |
| 15 | Fallo al devolver el segundo pedido | Nada cambia |
| 16 | Reenviar tras rechazo | Envío 2; no pide subir de nuevo lo correcto |
| 17 | Aprobar | Pasa a A8; BL bloqueados |
| 18 | Quien envió intenta aprobar | 403 |
| 19 | `asistente_china`, marketing o consulta intentan aprobar | 403 |
| 20 | Dos aprobaciones, si C1 lo pide | La misma persona no cuenta dos veces |
| 21 | Marcela abre un Packing List subido desde otro pedido | 200, mismo SHA-256 |
| 22 | Abrir una versión anterior presentada | 200 |
| 23 | Consulta abre un Packing List | 403 |
| 24 | Checklist con 15 de 16 | Aprobar responde 400 con el faltante |
| 25 | Carga de Aduana sin confirmar | No marca el requisito |
| 26 | Reloj del navegador desfasado | El cronómetro usa `servidorAhora` |
| 27 | `venceEn` pasado, futuro y nulo | Vencido, tiempo restante y Sin plazo |
| 28 | Rechazo | Plazo de corrección de 2 días; A7 se reinicia al reenviar |
| 29 | 401 durante un fragmento | Renueva, repite y un solo adjunto |
| 30 | Finalización que falla y se reintenta | Sin adjunto duplicado |
| 31 | Enlace de recuperación con token de 20 minutos | Caduca a los 30 minutos |
| 32 | Avisos al enviar y rechazar | Filas para los destinatarios correctos, sin quien actuó |
| 33 | WhatsApp simulado | Fila `SIMULADO`, cero llamadas de red |
| 34 | Pedido en A6 v1 | Sin bloques BL ni Aprobación de embarque |

## Validation Commands

```bash
cd backend && ./gradlew check
cd frontend && npm run lint && npm run test && npm run build
```

- Aplicar V32 y V33 sobre una copia restaurada y comparar conteos.
- Recorrido E2E en el entorno ficticio con el ejemplo del prototipo.

## Acceptance Criteria

- [ ] Decisiones C1–C3 y R1–R3 resueltas
- [ ] Los pedidos v1 no cambian y los nuevos nacen en v2
- [ ] El historial muestra cada envío, rechazo, corrección y decisión
- [ ] El envío registra BL, pedidos vinculados y versión de cada Packing List
- [ ] Un requisito de Aduana solo se marca con confirmación del servidor
- [ ] El cronómetro distingue tiempo restante, Vencido y Sin plazo
- [ ] El token dura 20 minutos y los enlaces de recuperación no cambian
- [ ] Sin correo real ni WhatsApp real activados por este trabajo
- [ ] `./gradlew check`, `npm run test` y `npm run build` en verde

## Risks

| # | Risk | Likelihood | Impact | Mitigation |
|---|---|---|---|---|
| T1 | Sin lista oficial, v2 no se puede publicar: `PENDIENTES` bloquearía Aduana y sembrar nombres provisionales exigiría documentos inventados | Alta | Alto | Publicar v2 con la lista. Si urge, v2 sin documentos en Aduana y una v3 con la lista |
| T2 | Pedidos v1 y v2 mezclados en listas y estadísticas | Alta | Alto | Entrega 1 antes de publicar V33 |
| T3 | Un BL compartido bloquea a otro pedido | Media | Medio | Congelar, devolver juntos y avisar |
| T4 | Rechazo sobre un BL ya aprobado en otro pedido | Media | Alto | Decisión C7 |
| T5 | El cliente elige por BL en Aduana o Bodega | Media | Alto | Otra versión del flujo |
| T6 | Marcela revisa todos los pedidos | Media | Medio | Cronómetro y avisos; revisar C5 |
| T7 | WhatsApp no llega al equipo en China | Alta | Bajo | El correo sigue como canal principal |
| T8 | Interbloqueos en BL compartidos | Baja | Medio | Orden fijo: pedidos por id y después BL por id |

## Notes

- `cdtulcan` (David Tulcan) tiene los roles Compras China y Asistente de compras en China. El rol que aprueba es `asistente_compras`, así que no puede aprobar salvo que también sea Admin.
- Los datos de la verificación de sesión quedaron en la base ficticia del manual: adjuntos de prueba en DK-EC-2026-0005. No tocan producción.
- Siguiente paso: resolver las decisiones. Después, `/ecc:prp-implement` por entregas, y Codex prueba (parte B).

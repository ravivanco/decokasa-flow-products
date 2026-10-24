# ADR-0023: El piloto del asistente se aísla con datos ficticios controlados y conserva los textos 30 días

**Fecha**: 2026-10-22
**Estado**: accepted (equipo, Richard, 2026-10-22) **solo para el piloto**. La conservación en producción y el envío de documentos reales **no están aprobados** (F7-D6). Pendiente de revisión por Codex (parte B); sin implementar.
**Decisores**: Richard (F7-D3, F7-D5 y precisiones del 2026-10-22). No es una respuesta del cliente.

## Contexto

Enviar documentos reales del cliente al proveedor requiere una aprobación expresa que no existe (requerimientos 4.7). El piloto se hace con datos ficticios, pero una variable de entorno no demuestra que un archivo sea ficticio. Las conversaciones contienen texto de documentos (citas) y deben borrarse a tiempo. Además, el borrado se hace en lugares distintos: la base activa, las copias de seguridad y el proveedor.

## Decisión

**Aislamiento (F7-D3):**
- Un entorno propio, con su base, sus archivos, su `.env` y la clave nueva, marcado `decokasa.ia.entorno=PILOTO_FICTICIO`. Sin Google: los PDF se leen del almacenamiento local del piloto. Solo **PDF**, detectados por su contenido (`adjunto.tipo_detectado`). Los demás formatos se excluyen y la respuesta lo informa.
- **Control de archivos:**
  1. Cada PDF se crea desde cero, con texto inventado.
  2. Richard lo revisa con una lista de comprobación: sin datos, nombres, identificaciones, contactos, precios, logos ni marcas reales.
  3. Se registra en un manifiesto local fuera de Git (`nombre`, `sha256`, `paginas`, `origen`, `revisado_por`, `revisado_en`).
  4. Solo se cargan y se envían archivos cuyo `adjunto.sha256` está en el manifiesto; los demás se excluyen como `NO_AUTORIZADO_PILOTO`.
  5. `consulta_ia.documentos_enviados` permite auditarlo.
- Fuera de `PILOTO_FICTICIO` no se envía ningún documento hasta F7-D6.

**Conservación del piloto (F7-D5).** Las conversaciones las guarda el servidor y solo las ve su autor; el admin ve el consumo, no textos ajenos. Nunca hay preguntas, respuestas, documentos ni secretos en los logs. Cada lugar se borra a su modo y con sus límites:

| Ámbito | Qué | Cómo y cuándo |
|---|---|---|
| **Datos activos** | `conversacion_ia` y `mensaje_ia` (preguntas, respuestas y referencias con texto citado) | Borrado físico al vencer `expira_en` (30 días). El proceso es diario, así que el borrado ocurre como mucho alrededor de un día después del vencimiento. `consulta_ia` se conserva sin texto, y su `conversacion_id` pasa a nulo |
| **Navegador** | Caché en memoria | Nunca en `localStorage` ni en `sessionStorage`; desaparece al cerrar sesión o al recargar |
| **Copias de seguridad del piloto** | Volcados de la base, si se hacen | No hay ninguna por defecto. Si se hacen, rotan en 30 días o menos: el texto desaparece **cuando la copia expira**, no al instante |
| **Proveedor (Anthropic)** | Lo enviado en cada petición | **Independiente de este sistema.** Enviar los PDF dentro de la petición, en lugar de subirlos con la Files API, evita que queden archivos guardados allí, pero **no implica retención cero**. La retención del proveedor se revisa en F7-D6 con su documentación y sus condiciones vigentes |

No se promete el borrado inmediato de todas las copias.

## Alternativas consideradas

### Confiar en la variable de entorno
- **Pros**: no exige procedimiento.
- **Contras**: no prueba qué contiene cada archivo.
- **Por qué no**: Richard pidió un control de los archivos que se cargan.

### Marcar los documentos ficticios en la base habitual
- **Pros**: no hace falta otro entorno.
- **Contras**: mezcla datos reales y ficticios en un mismo lugar.
- **Por qué no**: el entorno separado lo evita por diseño.

### No guardar el texto
- **Pros**: menos datos guardados.
- **Contras**: el historial pasaría por el navegador, que podría falsificarlo, y no habría material para evaluar la calidad.
- **Por qué no**: Richard aprobó 30 días para el piloto.

## Consecuencias

### Positivas
- Ningún dato real del cliente llega al proveedor en el piloto.
- El borrado está definido en cada lugar, con su límite real.

### Negativas
- Mantener un segundo entorno y revisar cada PDF a mano.

### Riesgos
- Un PDF real cargado por error. Mitigación: la revisión humana, el manifiesto con `sha256`, la exclusión de todo archivo ajeno al manifiesto y la auditoría de lo enviado.

## Pendiente (F7-D6)

- Documentos reales: quién aprueba, cómo se registra y qué cubre.
- Conservación en producción (la propuesta de 24 horas **no está aprobada**).
- Retención y región del proveedor.
- Relacionados: ADR-0015, 0020, 0021 y 0022.

# ADR-0022: El consumo del asistente se limita con una reserva atómica del peor caso antes de cada llamada

**Fecha**: 2026-10-22
**Estado**: accepted (equipo, Richard, 2026-10-22) **solo para el piloto con datos ficticios**. Pendiente de revisión por Codex (parte B); sin implementar.
**Decisores**: Richard (F7-D2: límites aprobados para el piloto). No es una respuesta del cliente.

## Contexto

Cada consulta cuesta según los tokens de entrada y de salida, y varias consultas pueden llegar a la vez. Un contador que se revisa y se actualiza en pasos separados deja pasar consultas concurrentes por encima del presupuesto. Claude API ofrece además un límite de gasto por workspace y un tope por organización, pero solo como respaldo: cuando se alcanzan, rechazan con errores propios (400 `invalid_request_error` o 429 `enforced_spend_limit_reached`, sin `retry-after`).

## Decisión

- **Límites del piloto (F7-D2):**

  | Límite | Valor |
  |---|---|
  | Entrada por consulta | 60 000 tokens como máximo (bajo el umbral de 100 000 de Haiku 5.5) |
  | Respuesta | `max_tokens` 4 000 |
  | Preguntas por conversación | 10 |
  | Consultas por usuario | 20 por día, con 1 en curso |
  | Consultas totales | 200 por día |
  | Presupuesto del servidor | US$10 por mes |
  | Respaldo del workspace del piloto | US$15 por mes, configurado por Richard |

- **Reserva antes de llamar:** se toman los tokens de entrada exactos (`count_tokens`) y se suma `max_tokens`, valorando todo con la tabla de precios versionada del modelo, en micro-USD enteros. Sin precio configurado, la IA no responde.
- **Atomicidad:** un `UPDATE … SET reservado = reservado + :max WHERE … AND consumido + reservado + :max <= limite`, sobre filas de presupuesto mensual, de uso diario por usuario (con `en_curso`) y de uso diario total. Si alguna afecta 0 filas, se responde 429 `LIMITE_CONSUMO` sin llamar. Las consultas concurrentes se serializan en la fila y cada una ve las reservas anteriores. Como se reserva el peor caso, no se puede superar el límite.
- **Liquidación:** con el `usage` real se suma lo consumido y se libera el resto de la reserva. Si es seguro que no hubo cobro, se libera todo. Si el resultado es dudoso (tiempo agotado o corte), la reserva se da por consumida. Los reintentos automáticos del SDK se desactivan o cada intento reserva por separado. Las reservas huérfanas, más allá del tiempo de espera, se dan por consumidas.
- **Respaldo externo:** ante el límite del workspace o de la organización, no se reintenta; la IA se desactiva (con su motivo) y se responde 503 `IA_NO_DISPONIBLE`. La reactiva el admin.

## Alternativas consideradas

### Contar después de cada respuesta
- **Pros**: simple.
- **Contras**: las consultas concurrentes pueden pasarse del presupuesto.
- **Por qué no**: el presupuesto es un tope, no un aviso.

### Depender solo del límite de la Console
- **Pros**: no exige código.
- **Contras**: no limita por usuario ni por día, y corta el servicio de golpe.
- **Por qué no**: queda como respaldo.

### Un cerrojo global en memoria
- **Pros**: fácil de programar.
- **Contras**: no sirve con varias instancias ni sobrevive a un reinicio.
- **Por qué no**: PostgreSQL ya es la fuente de verdad y ADR-0012 sitúa las transacciones en el borde.

## Consecuencias

### Positivas
- El tope es estricto y se puede auditar.
- Una consulta que se pasaría no llega a llamar al proveedor, así que no cuesta nada.

### Negativas
- Reservar el peor caso subestima la capacidad real del mes.
- `count_tokens` añade una llamada por consulta.

### Riesgos
- Que los precios cambien. Mitigación: tabla `precio_modelo_ia` con su fuente y fecha de vigencia; sin precio, la IA no responde.

## Pendiente

- Los límites de producción.
- Relacionados: ADR-0012, 0020 y 0021.

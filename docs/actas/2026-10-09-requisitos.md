# Acta de requisitos — Decokasa Tracking

| Campo | Valor |
|---|---|
| Proyecto | Decokasa Tracking (seguimiento de importaciones China → Ecuador) |
| Product Owner | Mirian Franco (Sistemas, Decokasa S.A.S.) |
| Desarrollo | Equipo de desarrollo Decokasa Tracking |
| Levantamiento | 07/10/2026 (interrogatorio interno + consulta por WhatsApp a la Product Owner) |
| Entrega comprometida | 13/11/2026 |
| Versión de requisitos | 1.0 |

## 1. Fuentes

- `docs/clienteDocs/Cliente/Decokasa_Confirmacion_Proceso_Cliente_MEJORADO.docx` (respuestas de Mirian del 02/10/2026; se cita por número de pregunta: 1.x, 2.x, A1…I4).
- `docs/clienteDocs/Cliente/PROCEDIMIENTO PARA LA COMPRA DE MERCADERÍA.docx` (en adelante, **Procedimiento**).
- `docs/clienteDocs/Cliente/FlujoProcesogeneral.txt` (notas del flujo).
- `docs/clienteDocs/Cliente/Orden_Pedido_Panel Exterior WPC.xlsx` (pedido real de referencia).
- Demo actual en `frontend/` (solo como referencia de comportamiento).

## 2. Regla de precedencia

1. Para el tramo **desde la elaboración de la Orden de Pedido hasta el inicio de fabricación** prevalece el **Procedimiento**.
2. Para el resto del proceso prevalecen las decisiones de esta acta sobre los documentos anteriores.
3. Lo que esta acta marca como **Pendiente** no se considera confirmado.

## 3. Decisiones confirmadas

| ID | Tema | Decisión | Origen |
|---|---|---|---|
| D-01 | Autorización de fabricación | David Tulcán revisa la Orden de Pedido y él mismo autoriza el inicio de fabricación. **No existe etapa de aprobación de la Junta.** | Procedimiento §3.2, §6; decisión del 07/10 |
| D-02 | Número de etapas | El flujo del pedido tiene **10 etapas**. | Consecuencia de D-01 |
| D-03 | Ciclo de corrección | Si David encuentra errores, devuelve a Marcela con observaciones específicas; Marcela informa a Mirian; Mirian corrige (o lo pide a un colaborador) y verifica; Marcela comprueba y reenvía a David. Revisión: máximo 3 días. Corrección: máximo 3 días. Cada devolución abre una corrección y una nueva revisión; el cronograma se recalcula. | Procedimiento §4–§5; P4 |
| D-04 | N.º de Orden de Fabricación | Al autorizar, David registra el N.º de Orden de Fabricación de Latinoamérica World (obligatorio). El contrato de fabricación en PDF es opcional. | Procedimiento §6, §8; P9 |
| D-05 | Bloqueo de fabricación | No se inicia la fabricación mientras existan observaciones pendientes. | Procedimiento §8 |
| D-06 | Cálculo del cronograma | Días hábiles lunes a viernes, sin feriados. Cronograma = suma de los plazos por etapa (123 días hábiles con 10 etapas). | B4, B5, P5 |
| D-07 | Meta | 99 **días hábiles**, configurable; se muestra como desviación entre llegada estimada y meta. | B6, P5, respuesta de Mirian |
| D-08 | Pago | Sin etapa propia: check informativo opcional "Pago realizado" en Desaduanización, sin montos, no bloquea. | A1, P6 |
| D-09 | Commercial Invoice | Lo sube Compras China en la etapa de Aprobación de embarque y Packing List; obligatorio para dar el check de esa etapa. | D3, P7 |
| D-10 | Cierre documental | Anderson completa los documentos; Marcela (Compras Ecuador) da la validación final que finaliza el pedido. | 1.13, P8 |
| D-11 | Etiquetas | Campo "Tipo de etiqueta" (Neutral / Norma) y marca "Urgente" separada; informativos, no alteran plazos. | Notas de flujo, P10 |
| D-12 | Google Drive | Cuenta Gmail dedicada de Decokasa (se gestionará más adelante). Los archivos se guardan en el servidor y se puede pegar un enlace de Drive. La copia automática a Drive **no entra** el 13/11. | H1–H4, P11, P12, respuesta de Mirian |
| D-13 | Alcance adicional | Flujo de producto nuevo y exportación a PDF y Excel entran en versión mínima. | Tabla 4, P13 |
| D-14 | Alertas | Solo dentro del sistema: aviso 3 días antes del vencimiento, aviso de vencido, mensaje de retraso visible para todos y escalamiento a Compras Ecuador. | 1.8, B8, B9, P14 |
| D-15 | Roles | Mirian elabora la Orden de Pedido (Administradora). Andrea Quishpe (Marketing) sube el Zip de etiquetas. Marcela (Compras Ecuador) envía a revisión, gestiona correcciones y valida el cierre. Andrea Collaguazo (Compras China) tiene las mismas etapas que David. | Procedimiento §3, 2.3, P15 |
| D-16 | BL y contenedores | Se sube el PDF del BL (obligatorio) y se escriben a mano el número de BL y los números de contenedor (opcional). No se modela la relación entre BL, contenedores y pedidos. | E5, E8, P16 |
| D-17 | Hosting | Servidor privado con Docker; el acceso desde China sin VPN se prueba en S1. Mirian contratará el servidor y el dominio de producción. | P17, respuesta de Mirian |
| D-18 | Migración | Solo los 7 pedidos en curso, cargados desde un Excel que entrega Mirian antes del 04/11. | I1, I2, P18 |
| D-19 | Versiones del pedido | Los datos del pedido se bloquean al iniciar fabricación; el Excel del pedido admite nuevas versiones con observación obligatoria. La "nueva versión del pedido completo" queda fuera del 13/11. | D1, E3, E4, P19 |
| D-20 | Producto nuevo | "Investigación" y "revisión de origen" son **un solo paso**. | Respuesta de Mirian |
| D-21 | Estados | Dos niveles: estado general del pedido (En proceso, En corrección, En pausa, Cancelado, Finalizado, Reabierto) y etiqueta de seguimiento por etapa. | 2.4, decisión del 07/10 |
| D-22 | Numeración | El número consecutivo de la Orden de Pedido del Procedimiento es la parte secuencial del código del pedido (importación-año-secuencial-producto). | Procedimiento §4.B, E2 |

## 4. Pendientes

| ID | Pendiente | Valor provisional |
|---|---|---|
| PD-01 | Plazo del paso "Investigación / revisión de origen": 7 días (+10 si visitan fábricas) según 1.14, o 10 días según F3. | 7 días hábiles (+10 si visitan fábricas) |
| PD-02 | Los +10 días de revisión cuando el pedido incluye producto nuevo (B11, F3) frente al máximo de 3 días del Procedimiento. | 3 días (prevalece el Procedimiento) |
| PD-03 | Checklists de las etapas distintas de Revisión: el cliente solo entregó el de la sección 2.5. | Propuesta derivada de los documentos, a validar |
| PD-04 | "Eliminar pedidos" (1.15) frente a "nada se elimina" (C9) y la auditoría. | No se elimina; se cancela con motivo |
| PD-05 | Documentos que pueden ver la Junta y el rol Consulta (C8 solo habla de editores). | Junta ve y descarga todo; Consulta solo ve el seguimiento |
| PD-06 | Formato de las fotos de inspección (2.3 dice Excel). | Imágenes, PDF o Excel |
| PD-07 | Significado exacto de "urgente / neutral / norma" y de los "3 días" de etiquetas. | D-11 |
| PD-08 | Fecha de disponibilidad de la cuenta Gmail. | Fuera del 13/11 |
| PD-09 | Ejemplos reales de Packing List, Commercial Invoice, BL y contrato (no están en `docs/clienteDocs/`). | Se tratan como archivos sin leer su contenido |
| PD-10 | Puntos 6.1 y 6.2 de la confirmación (sin firmar). | Mirian indicó que se verán en el transcurso |
| PD-11 | Informar a Mirian que el cronograma pasa de 125 a 123 días hábiles al eliminar la etapa Junta. | — |

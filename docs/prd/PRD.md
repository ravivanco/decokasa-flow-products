# PRD — Decokasa Tracking

## 0. Control del documento

| Campo | Valor |
|---|---|
| Producto | Decokasa Tracking — seguimiento de pedidos de importación marítima China → Ecuador |
| Cliente | Decokasa S.A.S. |
| Product Owner | Mirian Franco (Sistemas) |
| Versión | 1.0 (borrador para validación) |
| Fecha | 07/10/2026 |
| Entrega comprometida | 13/11/2026 |
| Fuentes | Acta `docs/actas/2026-10-09-requisitos.md` (decisiones D-xx y pendientes PD-xx); Confirmación del proceso del cliente (citada por pregunta: 1.x, 2.x, A1…I4); **Procedimiento para la compra de mercadería** (en adelante, *Procedimiento*); notas `FlujoProcesogeneral.txt`; pedido real `Orden_Pedido_Panel Exterior WPC.xlsx` |
| Precedencia | 1) Procedimiento, para el tramo desde la Orden de Pedido hasta el inicio de fabricación. 2) Acta, para el resto. 3) Documentos anteriores. Lo marcado **⚠️ Pendiente** no está confirmado. |

---

## 1. Problema y objetivo del sistema

### 1.1 Situación actual
Decokasa importa mercadería desde China (proveedor/importadora Latinoamérica World). Hoy los pedidos se gestionan con Excel y WhatsApp (1.2). Consecuencias observadas en las fuentes:

- No hay una vista única de en qué etapa está cada pedido ni de quién debe actuar.
- Se edita el pedido original después de enviado, lo que causa problemas (E3).
- Errores en códigos, cantidades, modelos y especificaciones llegan a fabricación (Procedimiento §1).
- Los retrasos se detectan tarde y afectan ventas, stock y generan pérdidas (B2, B3).

### 1.2 Objetivo
Ofrecer un sistema web de **seguimiento y comunicación** donde cada pedido avanza por etapas con responsable, plazo en días hábiles, checklist, documentos y observaciones, con una línea de estados tipo rastreo de envíos, semáforo de plazos y registro de auditoría.

### 1.3 Fuera del propósito
El sistema **no** maneja pagos, costos, valores ni inventario (1.2).

### 1.4 Criterios de éxito para el 13/11/2026
| Criterio | Cómo se comprueba |
|---|---|
| Los 7 pedidos en curso están cargados con su etapa actual y fechas reales | Mirian valida cada pedido antes de la entrega (D-18) |
| Compras China (David) usa el sistema desde China sin VPN | Prueba de acceso desde la oficina de China (D-17) |
| Cada check, devolución y corrección queda con responsable, fecha, hora y motivo | Revisión del historial de un pedido de prueba |
| Un pedido nuevo recorre las 10 etapas sin intervención técnica | Prueba de punta a punta con los usuarios reales |

---

## 2. Usuarios, roles y permisos

### 2.1 Personas y rol asignado
| Persona | Cargo | País | Rol en el sistema |
|---|---|---|---|
| Mirian Franco | Sistemas / Encargada de Compras | Ecuador | **Administrador** |
| Marcela Catucuamba | Asistente de Compras | Ecuador | **Compras Ecuador** (Editor) |
| David Tulcán | Compras China (socio) | China | **Compras China** (Editor) |
| Andrea Collaguazo | Asistente de Compras en China | China | **Compras China** (Editor, mismas etapas que David) |
| Andrea Quishpe | Marketing | Ecuador | **Marketing** (Editor) |
| Anderson Enriquez | Logística Ecuador importaciones | Ecuador | **Logística** (Editor) |
| David, Daniel, Juan, Diego | Junta de Socios | China / Ecuador | **Junta** |
| Otros usuarios de lectura | — | — | **Consulta** |

Fuentes: 2.1, C1, C2, Procedimiento §3, D-15.

### 2.2 Qué puede hacer cada rol
| Rol | Puede |
|---|---|
| **Administrador** | Crear usuarios y asignar roles (1.15). Elaborar la Orden de Pedido y crear el pedido (D-15). Corregir la Orden de Pedido cuando es devuelta (Procedimiento §5). Editar pedidos antes del bloqueo (1.15, RN-11). Pausar, cancelar y reabrir pedidos (E13, C7). Ver y descargar todos los documentos (C8). |
| **Compras Ecuador** | Enviar la Orden de Pedido a revisión, recibir devoluciones, verificar la corrección y reenviar (Procedimiento §3.1). Dar la validación final del Cierre documental (D-10). Recibir el escalamiento de alertas (1.8). |
| **Compras China** | Revisar la Orden de Pedido, devolverla con observaciones o autorizar la fabricación y registrar el N.º de Orden de Fabricación (D-01, D-04). Operar Fabricación, Aprobación de embarque y Packing List, Reporte de mercadería (subir), BL y contenedores y Viaje marítimo (2.1, 2.2). Elaborar ficha técnica y catálogo de producto nuevo (2.2, F2). |
| **Marketing** | Subir el Zip de etiquetas en la creación del pedido (2.3, D-15). Registrar la codificación de producto nuevo (F5, F6). |
| **Logística** | Dar el check del Reporte de mercadería (E9). Operar Desaduanización y Bodega (2.2, C3). Completar los documentos del Cierre documental (D-10). |
| **Junta** | Ver todos los pedidos. Pausar, cancelar y reabrir pedidos en cualquier etapa (E13, C7). No aprueba la fabricación (D-01). |
| **Consulta** | Ver el seguimiento de todos los pedidos (1.15). |

Reglas comunes a los Editores: dar check, devolver, revertir checks y subir documentos **solo en sus etapas** (1.6, C6); ver y descargar documentos **de su etapa** (C8).

⚠️ Pendiente PD-05: documentos que ven la Junta y Consulta. Provisional: la Junta ve y descarga todo; Consulta solo ve el seguimiento, sin documentos.

### 2.3 Suplencias
Andrea Collaguazo puede actuar en todas las etapas de Compras China (D-15). No se definieron otros suplentes (2.2: "No aplica").

---

## 3. Alcance de la entrega del 13/11/2026

### 3.1 Qué entra
- Inicio de sesión con usuario y contraseña; 7 roles; creación de usuarios y asignación de roles por el Administrador (1.15, C5).
- Pedidos: crear, listar, buscar y ver el rastreo (línea de estados de 10 etapas).
- Cronograma automático en días hábiles, semáforo y meta de 99 días hábiles (D-06, D-07).
- Panel de etapa: checklist, check, devolución con observaciones, reversión de check con motivo.
- Ciclo de revisión y corrección del Procedimiento con N.º de Orden de Fabricación (sección 5).
- Documentos: subir, adjuntar enlace de Drive, versiones sin borrado (H3, C9).
- Observaciones por etapa (G1).
- Estados generales: pausar, cancelar, reabrir (sección 6).
- Bodega: verificación contra Packing List e incidencias con fotos; informe de incidencias (E10, E11).
- Registro de auditoría (1.16).
- Alertas dentro del sistema (D-14).
- Flujo de producto nuevo, versión mínima (D-13).
- Exportación a PDF y Excel de la lista de pedidos y del cronograma de un pedido, versión mínima (D-13).
- Carga de los 7 pedidos en curso (D-18).
- Interfaz en español.

### 3.2 Qué no entra
| Función | Motivo |
|---|---|
| Copia automática de archivos a Google Drive | Cuenta Gmail aún no disponible (D-12) |
| Alertas por correo o WhatsApp | Marcadas como deseables; solo alertas internas (D-14, G2) |
| Seguimiento automático de la naviera | "Para después"; se registra manualmente (G3) |
| Tableros y reportes con gráficos | Deseable (tabla 4) |
| Visualizador del Excel del pedido en pantalla | Deseable (tabla 4) |
| Chat o comentarios por pedido | Deseable; se cubre con observaciones por etapa (G1) |
| Selector de idioma español / inglés | Deseable (tabla 4, G4) |
| Nueva versión del pedido completo | Fuera del 13/11 (D-19) |
| Relación detallada BL ↔ contenedores ↔ pedidos | Requiere análisis adicional (E8, D-16) |
| Migración de pedidos históricos | No requerida (I2) |
| Pantalla para editar plazos, responsables y checklists | No priorizada; los valores se mantienen parametrizables (RNF-08) |

### 3.3 Condicionado a dependencias externas
| Elemento | Depende de | Si no llega a tiempo |
|---|---|---|
| Carga de los 7 pedidos en curso | Excel de Mirian antes del 04/11/2026 (D-18) | Los pedidos se crean sin fechas reales y el semáforo no refleja la situación |
| Publicación en producción | Servidor y dominio que contratará Mirian (D-17) | Se entrega en un servidor de prueba |
| Validación de acceso desde China | Prueba con David en S1 (D-17) | Riesgo de que Compras China no pueda operar |
| Checklists de las etapas 3 a 10 | Validación de Mirian (PD-03) | Se usan los checklists propuestos en la sección 4.3 |

---

## 4. Flujo principal de pedidos

### 4.1 Creación del pedido
- Datos mínimos: nombre o descripción, fecha del pedido, Excel del pedido (proforma) y Zip de etiquetas (E1, D1).
- Código único: importación – año – número secuencial – producto (ej.: `IMP-2026-006-PANEL-EXT-WPC`). El número consecutivo de la Orden de Pedido del Procedimiento es la parte secuencial (D-22).
- Tipo de etiqueta (Neutral / Norma) y marca Urgente, informativos (D-11).
- Bodega de destino (Chongón por defecto; Quito, Ibarra u otra) y puerto de llegada (1.10, E12).

### 4.2 Cronograma y semáforo
- Días hábiles de lunes a viernes, sin feriados; el Año Nuevo Chino se considera al planificar pero no se descuenta (B4).
- Cada etapa empieza cuando la anterior terminó (o se estima que termine); el total es la suma de plazos: **123 días hábiles** (D-06).
- Meta configurable de **99 días hábiles**: se muestra la desviación entre la llegada estimada y la meta (D-07).
- Semáforo por etapa: verde (en plazo), naranja (vence en 3 días o menos), rojo (vencida), gris (no iniciada) (B8, demo).

### 4.3 Las 10 etapas

> Los checklists de las etapas 1 y 2 provienen del Procedimiento y de la sección 2.5. Los de las etapas 3 a 10 son una **propuesta derivada de los documentos del cliente — ⚠️ Pendiente de validación (PD-03)**.

**Etapa 1 — Solicitud y creación**
| Campo | Valor |
|---|---|
| Responsable | Mirian Franco (Administrador) elabora la Orden de Pedido; Andrea Quishpe (Marketing) sube el Zip de etiquetas |
| Plazo | Sin plazo (es la fecha de inicio) |
| Checklist | Todos los campos completos · Códigos de producto correctos · Descripciones corresponden al producto · Cantidades y etiquetas correctas · Modelos, medidas, colores y especificaciones detallados · Sin campos vacíos (Procedimiento §4.A) |
| Documentos obligatorios | Excel del pedido (proforma) · Zip de etiquetas |
| Condición para avanzar | Checklist completo y documentos subidos; Compras Ecuador (Marcela) envía la Orden de Pedido a revisión |

**Etapa 2 — Revisión y corrección**
| Campo | Valor |
|---|---|
| Responsable | David Tulcán (Compras China); suplente Andrea Collaguazo |
| Plazo | 3 días por revisión; cada devolución agrega 3 días de corrección y 3 de nueva revisión (D-03). ⚠️ PD-02: +10 días con producto nuevo — provisional: no aplica |
| Checklist | Información correcta y pedido apto para fabricación (Procedimiento §3.2). Puntos de la sección 2.5: formato establecido y fechas del cronograma · observaciones de Compras China revisadas · información cargada correcta y completa · etiquetas, productos y fechas de vencimiento completos (⚠️ PD-03: qué puntos de 2.5 aplican a esta etapa) |
| Documentos obligatorios | Ninguno adicional |
| Condición para avanzar | Sin observaciones pendientes, autorización de David y N.º de Orden de Fabricación Latinoamérica registrado (D-04, D-05) |

**Etapa 3 — Fabricación**
| Campo | Valor |
|---|---|
| Responsable | David Tulcán (Compras China) |
| Plazo | 35 días; ampliable hasta +10 días acumulados con motivo (B2) |
| Checklist (propuesta) | Inicio de fabricación confirmado · Fin de fabricación confirmado |
| Documentos obligatorios | Ninguno (contrato de fabricación y fotos de inspección opcionales) |
| Condición para avanzar | Checklist completo y check de Compras China |

**Etapa 4 — Aprobación de embarque y Packing List**
| Campo | Valor |
|---|---|
| Responsable | David Tulcán (Compras China) |
| Plazo | 10 días; el Packing List se sube dentro de estos 10 días (B7) |
| Checklist (propuesta) | Embarque aprobado · Packing List subido · Commercial Invoice subido |
| Documentos obligatorios | Packing List (Excel) · Commercial Invoice (Excel) |
| Condición para avanzar | Ambos documentos subidos y check de Compras China (D-09) |

**Etapa 5 — Reporte de mercadería**
| Campo | Valor |
|---|---|
| Responsable | David sube el reporte; Anderson Enriquez (Logística) da el check (E9) |
| Plazo | 5 días (B10) |
| Checklist (propuesta) | Reporte de China subido · Mercadería enviada verificada como correcta |
| Documentos obligatorios | Reporte de mercadería (Excel) |
| Condición para avanzar | Reporte subido y check de Logística |

**Etapa 6 — BL y contenedores**
| Campo | Valor |
|---|---|
| Responsable | David Tulcán (Compras China) |
| Plazo | 5 días (B10) |
| Checklist (propuesta) | BL subido |
| Documentos obligatorios | BL (PDF). Opcionales: certificado de origen; número de BL y números de contenedor en texto libre; naviera, forwarder, puertos, barco y fecha de zarpe (D-16, E7) |
| Condición para avanzar | BL subido y check de Compras China |

**Etapa 7 — Viaje marítimo**
| Campo | Valor |
|---|---|
| Responsable | David Tulcán (Compras China) |
| Plazo | 40 días hábiles (B5) |
| Checklist (propuesta) | Zarpe registrado · Llegada a puerto registrada (seguimiento manual, G3) |
| Documentos obligatorios | Ninguno |
| Condición para avanzar | Check de Compras China al registrar la llegada |

**Etapa 8 — Desaduanización**
| Campo | Valor |
|---|---|
| Responsable | Anderson Enriquez (Logística) (C3) |
| Plazo | 15 días (hasta 1 mes en el peor caso) |
| Checklist (propuesta) | Subestado actualizado (En trámite / Retenido / Liberado) · Mercadería liberada. Check informativo opcional "Pago realizado", sin montos y sin bloquear (D-08) |
| Documentos obligatorios | Ninguno |
| Condición para avanzar | Subestado "Liberado" y check de Logística |

**Etapa 9 — Bodega**
| Campo | Valor |
|---|---|
| Responsable | Anderson Enriquez (Logística) |
| Plazo | 5 días (B10) |
| Checklist (propuesta) | Mercadería recibida en la bodega de destino · Verificada contra el Packing List (E10) · Incidencias registradas con fotos y observaciones, o "sin incidencias" (1.12). Punto de 2.5: toda la mercadería recibida corresponde al pedido |
| Documentos obligatorios | Informe de incidencias de bodega (generado por el sistema) y respaldo de Logística (E11) |
| Condición para avanzar | Checklist completo y check de Logística |

**Etapa 10 — Cierre documental**
| Campo | Valor |
|---|---|
| Responsable | Anderson Enriquez completa; Marcela Catucuamba (Compras Ecuador) valida (D-10) |
| Plazo | 5 días (B10) |
| Checklist (propuesta) | Documentos obligatorios de todas las etapas completos (Anderson) · Validación final (Marcela) |
| Documentos obligatorios | Los que falten de etapas anteriores |
| Condición para avanzar | Validación final de Compras Ecuador → pedido **Finalizado** |

Total: 0 + 3 + 35 + 10 + 5 + 5 + 40 + 15 + 5 + 5 = **123 días hábiles**.

### 4.4 Documentos
- Formatos: PDF, Excel, Word, JPG/PNG y Zip; máximo 100 MB por archivo (D4, D5).
- Nadie elimina documentos: se reemplazan y queda el historial de versiones (C9).
- Se puede subir un archivo, pegar un enlace de Drive, o ambos (H3).
- Opcionales que no bloquean: certificado de origen, fotos de inspección, contrato de fabricación (D2, D-04). ⚠️ PD-06: formato de fotos de inspección — provisional: imágenes, PDF o Excel.

### 4.5 Observaciones, alertas y auditoría
- Observaciones por etapa dentro del sistema; no se traducen (G1, G5).
- Alertas internas 3 días antes del vencimiento y al vencer; escalamiento a Compras Ecuador; mensaje de retraso visible para todos (D-14).
- Todo cambio queda registrado: quién, cuándo (fecha y hora) y por qué (1.7, 1.16).

---

## 5. Ciclo de revisión y corrección

Fuente: Procedimiento §3–§8 y acta D-01, D-03, D-04, D-05.

| Paso | Quién | Qué ocurre | Plazo | Estado general |
|---|---|---|---|---|
| 1. Elaboración | Mirian | Elabora la Orden de Pedido en el formato oficial y verifica que esté completa | — | En proceso |
| 2. Envío | Marcela | Envía la Orden de Pedido a David | — | En proceso |
| 3. Revisión | David | Verifica que la información sea correcta y que el pedido pueda pasar a fabricación | Máx. 3 días hábiles | En proceso |
| 4a. Devolución | David | Si hay errores, devuelve a Marcela con observaciones **específicas** | — | **En corrección** |
| 5. Corrección | Marcela → Mirian | Marcela informa a Mirian; Mirian corrige directamente o pide la corrección a un colaborador, y verifica el resultado | Máx. 3 días hábiles | En corrección |
| 6. Reenvío | Marcela | Comprueba que el archivo corresponde a la orden observada y reenvía a David (nueva versión del Excel con observación) | — | En proceso |
| 7. Nueva revisión | David | Vuelve al paso 3; el cronograma se recalcula | Máx. 3 días hábiles | En proceso |
| 4b. Autorización | David | Si está correcta, autoriza el inicio de fabricación | — | En proceso |
| 8. N.º de orden | David | Registra el N.º de Orden de Fabricación Latinoamérica (obligatorio) y, si existe, el contrato en PDF | — | En proceso (etapa Fabricación) |

No se puede autorizar la fabricación mientras existan observaciones pendientes (D-05). No existe aprobación de la Junta (D-01).

---

## 6. Estados del pedido

### 6.1 Estado general
| Estado | Significado | Quién / cuándo |
|---|---|---|
| **En proceso** | El pedido avanza por sus etapas | Estado por defecto al crear el pedido y al reenviar una corrección |
| **En corrección** | Devuelto en revisión con observaciones | Automático cuando David devuelve |
| **En pausa** | Detenido temporalmente | Administrador o Junta, con motivo, en cualquier etapa (E13) |
| **Cancelado** | Pedido anulado | Administrador o Junta, con motivo, en cualquier etapa (E13) |
| **Finalizado** | Todo validado | Automático con la validación final de Compras Ecuador (D-10) |
| **Reabierto** | Un pedido finalizado se reabrió | Junta o Administrador, con motivo (C7) |

### 6.2 Etiqueta de seguimiento por etapa
| Etapa | Etiqueta (tabla 2.4) |
|---|---|
| 1 | Creado |
| 2 | En revisión |
| 3 | En fabricación |
| 4 | Pendiente de embarque |
| 5–6 | ⚠️ Pendiente: el cliente eliminó "Documentos de embarque" (2.4) y no definió reemplazo. Provisional: "Pendiente de embarque" |
| 7 | En tránsito |
| 8 | En trámite / Retenido / Liberado |
| 9 | En bodega |
| 10 | En cierre documental |

---

## 7. Flujo de producto nuevo

Flujo independiente con su propio cronograma (F4). Cuando termina, el producto puede incluirse en un pedido (F1).

| Paso | Nombre | Responsable | Plazo (días hábiles) | Documentos |
|---|---|---|---|---|
| 1 | Origen / solicitud (lo solicita Ecuador o lo propone China) | Compras Ecuador o Compras China | Sin plazo | — |
| 2 | Investigación y revisión de origen (un solo paso, D-20) | Compras Ecuador | ⚠️ PD-01: 7 (+10 si visitan fábricas) o 10. Provisional: 7 (+10) | — |
| 3 | Ficha técnica | Compras China | Sin plazo | Ficha técnica (PDF), obligatoria; se envía junto con el catálogo (F2) |
| 4 | Catálogo de colores y diseños y aprobación | Compras China elabora; Compras Ecuador verifica y aprueba | 10 | Catálogo (Excel o PDF), obligatorio |
| 5 | Codificación | Sistemas / Marketing | 4, desde la aprobación del catálogo (F5) | — (el código solo se registra, F7) |
| 6 | Incorporación al pedido | — | Sin plazo | — |

- Si Compras Ecuador rechaza el producto, el rechazo y su motivo quedan registrados; el motivo es obligatorio (F8).
- No se registra la aceptación del público de productos de prueba (F9).

---

## 8. Reglas de negocio

### Generales y permisos
- **RN-01** Todo usuario autenticado puede ver todos los pedidos y su seguimiento.
- **RN-02** Solo el Administrador crea usuarios y asigna roles.
- **RN-03** Solo los usuarios con un rol autorizado para una etapa pueden dar check, devolver, revertir o subir documentos en esa etapa.
- **RN-04** Una etapa se completa solo cuando todos los puntos de su checklist están marcados, sus documentos obligatorios están subidos y el responsable da el check.
- **RN-05** Un pedido no avanza a la siguiente etapa mientras la etapa actual no esté completa.
- **RN-06** Cualquier editor asignado a una etapa puede revertir un check de esa etapa indicando un motivo obligatorio; la reversión es visible para todos.

### Pedido
- **RN-07** Para crear un pedido son obligatorios: nombre o descripción, fecha del pedido, Excel del pedido y Zip de etiquetas.
- **RN-08** Cada pedido recibe un código único con importación, año, número secuencial consecutivo y producto.
- **RN-09** El pedido registra un tipo de etiqueta (Neutral o Norma) y una marca Urgente; ninguno modifica plazos.
- **RN-10** La bodega de destino es Chongón por defecto y puede cambiarse a Quito, Ibarra u otra escrita; se registra el puerto de llegada.
- **RN-11** Los datos del pedido quedan bloqueados al iniciar la etapa de Fabricación; el Excel del pedido puede reemplazarse por una nueva versión solo con una observación obligatoria.
- **RN-12** ⚠️ Provisional (PD-04): los pedidos no se eliminan; se cancelan con motivo.

### Cronograma y plazos
- **RN-13** Los plazos se cuentan en días hábiles de lunes a viernes, sin descontar feriados.
- **RN-14** Cada etapa empieza en la fecha real (o estimada) de fin de la anterior; su fin estimado es el inicio más su plazo.
- **RN-15** Los plazos por etapa son: Solicitud 0, Revisión 3, Fabricación 35, Aprobación de embarque y Packing List 10, Reporte 5, BL 5, Viaje 40, Desaduanización 15, Bodega 5, Cierre 5.
- **RN-16** La meta del pedido es de 99 días hábiles, configurable; el sistema muestra la diferencia entre la fecha estimada de llegada y la meta.
- **RN-17** La fecha de inicio de Fabricación se calcula automáticamente; si se edita a mano, el sistema muestra una advertencia sobre el riesgo de retrasos en ventas, pérdidas y quiebre de stock.
- **RN-18** La Fabricación puede ampliarse con motivo hasta un máximo de 10 días hábiles acumulados.
- **RN-19** El semáforo de una etapa en curso es naranja cuando faltan 3 días hábiles o menos para su vencimiento y rojo cuando está vencida.

### Revisión y autorización
- **RN-20** Compras Ecuador envía la Orden de Pedido a revisión de Compras China.
- **RN-21** Compras China tiene como máximo 3 días hábiles para cada revisión.
- **RN-22** Una devolución exige observaciones y deja el pedido en estado En corrección.
- **RN-23** Cada corrección tiene como máximo 3 días hábiles; al reenviarla se inicia una nueva revisión de 3 días y el cronograma se recalcula.
- **RN-24** No se puede autorizar el inicio de fabricación mientras existan observaciones pendientes.
- **RN-25** Para autorizar el inicio de fabricación es obligatorio registrar el N.º de Orden de Fabricación Latinoamérica; el contrato en PDF es opcional.
- **RN-26** ⚠️ Provisional (PD-02): la revisión no suma días adicionales cuando el pedido incluye producto nuevo.

### Documentos
- **RN-27** Documentos obligatorios: Excel del pedido y Zip de etiquetas (etapa 1); Packing List y Commercial Invoice (etapa 4); Reporte de mercadería (etapa 5); BL (etapa 6); informe de incidencias de bodega (etapa 9).
- **RN-28** Se aceptan PDF, Excel, Word, JPG, PNG y Zip, hasta 100 MB por archivo.
- **RN-29** Ningún documento se elimina; reemplazarlo crea una nueva versión y conserva las anteriores.
- **RN-30** Los editores ven y descargan los documentos de su etapa; el Administrador, los de todas. ⚠️ Provisional (PD-05): la Junta ve y descarga todos; Consulta no ve documentos.
- **RN-31** Un documento puede subirse como archivo, como enlace de Drive o ambos.
- **RN-32** Certificado de origen, fotos de inspección y contrato de fabricación son opcionales y no bloquean ninguna etapa.

### Etapas específicas
- **RN-33** El check de Aprobación de embarque requiere el Packing List y el Commercial Invoice subidos.
- **RN-34** En Reporte de mercadería, Compras China sube el reporte y Logística da el check.
- **RN-35** En BL y contenedores, el PDF del BL es obligatorio; el número de BL, los números de contenedor y los datos del embarque son opcionales.
- **RN-36** El avance del Viaje marítimo se registra manualmente y es informativo.
- **RN-37** Desaduanización tiene los subestados En trámite, Retenido y Liberado; solo puede completarse en Liberado; un retenido solo se notifica.
- **RN-38** Desaduanización incluye un check informativo "Pago realizado" sin montos, que no bloquea el avance.
- **RN-39** En Bodega se verifica lo recibido contra el Packing List, se registran incidencias con fotos y observaciones, el sistema genera el informe de incidencias y Logística sube su respaldo.
- **RN-40** Si el pedido llega incompleto, se registran las partes que no llegaron y el motivo; el pedido se finaliza cuando llega la última parte.
- **RN-41** En Cierre documental, Logística marca los documentos como completos y Compras Ecuador da la validación final, que deja el pedido Finalizado.

### Estados
- **RN-42** El estado general del pedido es uno de: En proceso, En corrección, En pausa, Cancelado, Finalizado, Reabierto; además se muestra la etiqueta de seguimiento de la etapa actual.
- **RN-43** Solo el Administrador o la Junta pueden pausar, reanudar o cancelar un pedido, en cualquier etapa y con motivo obligatorio.
- **RN-44** Solo la Junta o el Administrador pueden reabrir un pedido Finalizado, con motivo obligatorio.

### Alertas y auditoría
- **RN-45** El sistema genera una alerta interna 3 días hábiles antes del vencimiento de una etapa y otra al vencer; las alertas de vencimiento se escalan a Compras Ecuador y el aviso de retraso del pedido es visible para todos los usuarios.
- **RN-46** Todo cambio (check, reversión, devolución, documento, estado, fecha) registra usuario, fecha, hora y motivo cuando corresponde.
- **RN-47** Las observaciones se registran por etapa y no se traducen.

### Producto nuevo
- **RN-48** El producto nuevo sigue un flujo independiente de 6 pasos con cronograma propio.
- **RN-49** Plazos: Investigación y revisión de origen 7 días hábiles (+10 si visitan fábricas, ⚠️ PD-01); Catálogo y aprobación 10; Codificación 4 desde la aprobación del catálogo.
- **RN-50** La ficha técnica y el catálogo son obligatorios.
- **RN-51** Un rechazo de producto nuevo exige motivo y queda registrado.
- **RN-52** El código del producto se registra; el sistema no lo genera.
- **RN-53** Un producto nuevo puede incluirse en un pedido solo después de completar la Codificación.

### Exportación y migración
- **RN-54** La lista de pedidos y el cronograma de un pedido se pueden exportar a PDF y a Excel.
- **RN-55** Se cargan únicamente los 7 pedidos en curso, con su etapa actual y las fechas reales de las etapas cumplidas.

---

## 9. Requisitos no funcionales

- **RNF-01** Acceso sin VPN desde China: la interfaz no carga ningún recurso alojado por Google ni servicios bloqueados en China continental (H4).
- **RNF-02** Cualquier integración futura con Google Drive se hace desde el servidor; los usuarios nunca se conectan a Google desde el navegador (H4, D-12).
- **RNF-03** La interfaz está en español (G4, alcance del 13/11).
- **RNF-04** Archivos de hasta 100 MB en los formatos de RN-28 (D4, D5).
- **RNF-05** El registro de auditoría no puede modificarse ni eliminarse (1.16, C9).
- **RNF-06** Los permisos se validan en el servidor; ocultar opciones en la interfaz no es suficiente (C5).
- **RNF-07** Acceso con usuario y contraseña personal; las credenciales y secretos del sistema no se guardan en el repositorio (C5, CLAUDE.md).
- **RNF-08** Plazos, responsables, checklists, documentos obligatorios y meta son parametrizables sin cambiar el código (acta, CLAUDE.md).
- **RNF-09** Capacidad para el equipo actual (unos 10 usuarios y 7 pedidos en curso) y para el crecimiento previsto en 2027 (unos 7 contenedores al mes). ⚠️ Pendiente: número de usuarios esperado en 2027 (2.1 sin responder).
- **RNF-10** La prueba de acceso desde China se realiza en S1, antes de construir el resto (D-17).
- **RNF-11** Restricción técnica del proyecto: backend Java 21, frontend Node.js 22, PostgreSQL 17, Docker (CLAUDE.md).
- **RNF-12** ⚠️ Pendiente: frecuencia de copias de seguridad de base de datos y archivos.
- **RNF-13** ⚠️ Pendiente: zona horaria de referencia para fechas y horas (Ecuador UTC-5 o China UTC+8).

---

## 10. Supuestos, dependencias y preguntas abiertas

### 10.1 Supuestos
- **S-01** Un pedido En pausa o Cancelado no admite checks hasta reanudarse o reabrirse.
- **S-02** Un pedido Reabierto vuelve a Finalizado con una nueva validación final de Compras Ecuador.
- **S-03** El Packing List, el Commercial Invoice, el BL y el contrato se tratan como archivos; el sistema no lee su contenido (PD-09).
- **S-04** Los usuarios de China son hispanohablantes; el idioma inglés no es necesario el 13/11.

### 10.2 Dependencias
| Dependencia | Responsable | Fecha |
|---|---|---|
| Excel con los 7 pedidos en curso | Mirian | Antes del 04/11/2026 |
| Prueba de acceso desde China | David | S1 (12–17/10/2026) |
| Servidor y dominio de producción | Mirian | Antes de la entrega |
| Validación de checklists propuestos | Mirian | Antes del sprint que implementa cada etapa |
| Cuenta Gmail dedicada | Mirian | Después del 13/11 |

### 10.3 Preguntas abiertas
| ID | Pregunta | Valor provisional |
|---|---|---|
| PD-01 | Plazo del paso de investigación y revisión de origen | 7 (+10 si visitan fábricas) |
| PD-02 | +10 días de revisión con producto nuevo frente al máximo de 3 días del Procedimiento | No aplica |
| PD-03 | Checklists de las etapas 3 a 10 y qué puntos de 2.5 aplican a Revisión | Propuesta de la sección 4.3 |
| PD-04 | ¿Se eliminan pedidos o solo se cancelan? | Solo se cancelan |
| PD-05 | Documentos visibles para Junta y Consulta | Junta todo; Consulta ninguno |
| PD-06 | Formato de fotos de inspección | Imágenes, PDF o Excel |
| PD-07 | Significado de urgente / neutral / norma y de los "3 días" | Campo y marca informativos |
| PD-08 | Fecha de la cuenta Gmail | Fuera del 13/11 |
| PD-09 | Ejemplos reales de Packing List, Commercial Invoice, BL y contrato | Archivos sin lectura de contenido |
| PD-10 | Puntos 6.1 y 6.2 de la confirmación | Se verán en el transcurso |
| PD-11 | Informar a Mirian que el cronograma pasa de 125 a 123 días hábiles | — |
| PD-12 | Etiqueta de seguimiento para las etapas 5 y 6 | "Pendiente de embarque" |
| PD-13 | Usuarios esperados en 2027, copias de seguridad y zona horaria | RNF-09, RNF-12, RNF-13 |

---

## 11. Trazabilidad

| Decisión del acta | Reglas | Fuente original |
|---|---|---|
| D-01 Autoriza David, sin Junta | RN-20, RN-24, RN-25 | Procedimiento §3.2, §6 |
| D-02 10 etapas | RN-15 | — |
| D-03 Ciclo de corrección | RN-21, RN-22, RN-23 | Procedimiento §4–§5 |
| D-04 N.º de Orden de Fabricación | RN-25, RN-32 | Procedimiento §6, §8 |
| D-05 Sin fabricación con observaciones pendientes | RN-24 | Procedimiento §8 |
| D-06 Cronograma en días hábiles, suma por etapa | RN-13, RN-14, RN-15 | B4, B5 |
| D-07 Meta 99 días hábiles | RN-16 | B6 |
| D-08 Pago informativo | RN-38 | A1 |
| D-09 Commercial Invoice en etapa 4 | RN-27, RN-33 | D3 |
| D-10 Cierre: Anderson + Marcela | RN-41 | 1.13 |
| D-11 Etiquetas | RN-09 | Notas de flujo |
| D-12 Drive | RN-31, RNF-02 | H1–H4 |
| D-13 Producto nuevo y exportación | RN-48 a RN-54 | Tabla 4 |
| D-14 Alertas internas | RN-19, RN-45 | 1.8, B8, B9 |
| D-15 Roles | RN-02, RN-03, RN-20, RN-34, RN-41 | Procedimiento §3, 2.3 |
| D-16 BL y contenedores | RN-27, RN-35 | E5, E8 |
| D-17 Hosting y prueba desde China | RNF-01, RNF-10 | H4 |
| D-18 Migración de 7 pedidos | RN-55 | I1, I2 |
| D-19 Bloqueo y versiones | RN-11, RN-29 | D1, E3 |
| D-20 Investigación = revisión de origen | RN-48, RN-49 | Respuesta de Mirian |
| D-21 Estados en dos niveles | RN-22, RN-42, RN-43, RN-44 | 2.4, E13, C7 |
| D-22 Numeración | RN-08 | Procedimiento §4.B, E2 |
| Confirmación 1.6, 1.7, C6 | RN-04, RN-05, RN-06 | — |
| Confirmación 1.15, C8, C9 | RN-01, RN-02, RN-29, RN-30 | — |
| Confirmación B2, B3 | RN-17, RN-18 | — |
| Confirmación 1.11, 1.12, E6, E10, E11 | RN-37, RN-39, RN-40 | — |
| Confirmación F7, F8 | RN-51, RN-52 | — |

---
*Estado: BORRADOR 1.0 — requisitos funcionales. Sin diseño de base de datos, endpoints ni diagramas.*

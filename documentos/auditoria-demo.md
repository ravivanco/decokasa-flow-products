# Auditoría del demo de AI Studio contra los requerimientos

> **Revisión vigente: 2026-10-13, sección 11.** Las secciones 1–10 conservan la auditoría histórica del demo (9–10 de octubre); sus conteos, rutas de línea y resultados de herramientas no describen automáticamente el árbol actual. La sección 11 actualiza clasificación, seguridad, calidad y consistencia documental sin aprobar propuestas pendientes.

**Fecha:** 2026-10-09 · **Fase 0, paso 0.3** · **Alcance:** `frontend/src/` contra las secciones 4.2 a 4.5 de `documentos/requerimientos.md`.

**Método:**

- Lectura del código fuente contra los requerimientos (secciones 2 a 6).
- Revisión de calidad React (`/ecc:react-review`) y auditoría técnica de diseño y accesibilidad (`/impeccable audit`), integradas sin duplicados en la sección 7.
- Chequeos automáticos: `npm run lint` y `npm audit`, más el detector de Impeccable y el cálculo de contrastes de color.
- Revisión de seguridad (`/ecc:security-scan` con AgentShield, más revisión manual) en la sección 10.
- No se ejecutó el demo en el navegador ni se modificó ningún archivo de código.

## 1. Resumen

El demo cubre bien la **forma** del sistema: los 2 flujos con sus etapas, roles, bandejas por rol, línea de tiempo, búsqueda, bodegas y correos simulados. Le falta todo lo que es **real**: no hay backend, ni persistencia, ni autenticación, ni Google Drive o Gmail. Todo vive en memoria en `App.tsx` y se pierde al recargar.

| Sección | ✅ Cumple | 🟡 Parcial | ❌ Falta | ⏸ No aplica aún |
| --- | --- | --- | --- | --- |
| 4.2 Restricciones (37 requisitos) | 19 | 13 | 4 | 1 |
| 4.3 Actores y superficies (7) | 4 | 3 | 0 | 0 |
| 4.4 Transiciones flujo A (14) | 6 | 8 | 0 | 0 |
| 4.5 Transiciones flujo B (6) | 2 | 4 | 0 | 0 |
| **Total (64)** | **31** | **28** | **4** | **1** |

"Cumple" significa que el comportamiento en pantalla coincide con el requisito. Ninguno está implementado de forma persistente ni segura todavía.

**Hallazgos que más pesan:**

1. **Errores en el flujo B** (sección 6): en B3 (Codificación) aparecen los botones de Revisión, y en B4 (Revisión) aparece el botón de devolución de China.
2. **Los documentos obligatorios no se exigen:** se puede dar check en Embarque sin Packing List / BL, y en Bodega sin elegir bodega.
3. **No existe "devolver por error" sin urgencia:** toda devolución desde China obliga a elegir 4, 12 o 24 h.
4. **La urgencia se borra al primer avance:** debería cubrir Codificación y Revisión hasta volver a Análisis.
5. **No se pueden crear usuarios ni asignar roles.** La pantalla de usuarios es solo de lectura y sirve para cambiar de sesión.

**Calidad técnica (sección 7):** la puntuación de diseño es 9/20 (nivel Pobre) y la revisión React no tiene hallazgos críticos. Hay 19 hallazgos consolidados: 7 P1, 7 P2 y 5 P3. El principal problema es la accesibilidad: modales, teclado, etiquetas, contraste del semáforo y tamaño de texto. No hay riesgos de seguridad propios de React ni dependencias vulnerables. Casi todo se corrige al migrar cada componente a shadcn/ui en la fase 1.

**Seguridad (sección 10):** el demo no puede pasar a producción. Hay 11 hallazgos: 4 críticos, 2 altos, 3 medios y 2 bajos. Los críticos: cualquiera entra como admin o cambia de rol, la contraseña está en el código y los permisos solo existen en el navegador. Hay además datos de empleados reales en el código y fechas, archivos y correos inventados. Los 270 "críticos" que marcó AgentShield en `package-lock.json` son falsos positivos.

## 2. Sección 4.2 — Restricciones

### 2.1 Política fija (del cliente)

| # | Requisito | Estado | Dónde | Nota |
| --- | --- | --- | --- | --- |
| 1 | Login con correo, usuario o teléfono + contraseña | ✅ | `components/LoginScreen.tsx:29-35` | Compara contra `DEMO_PASSWORD`, igual para todos |
| 2 | Cada rol ve su dashboard | ✅ | `components/DashboardView.tsx:92-133` | |
| 3 | Solo el admin crea usuarios y asigna roles | ❌ | `components/UsersManagementView.tsx` | Solo lista los usuarios de `USUARIOS_DEMO` y permite cambiar de sesión |
| 4 | Crea flujo A el admin; flujo B, David | ✅ | `utils/permissions.ts:37-45` | Solo en el cliente |
| 5 | Número secuencial, único, no se reutiliza | 🟡 | `components/OrdersListView.tsx:82-84` | Usa `pedidos.length + 1` y fija `DK-EC-2026`; puede repetirse y no cambia de año ni de país |
| 6 | Flujo A con productos existentes y nuevos; flujo B solo de David | ✅ | `types.ts:221-222`, `OrdersListView.tsx:97-98` | |
| 7 | Cada etapa con check, archivos y observaciones | ✅ | `components/StagePanel.tsx` | |
| 8 | Rechazo o devolución con observación y archivos extra | 🟡 | `StagePanel.tsx:566`, `orderState.ts:365` | La observación es obligatoria; no se pueden adjuntar archivos al rechazar ni al devolver |
| 9 | Revisión ✗ vuelve a Codificación | ✅ | `utils/orderState.ts:289-292` | |
| 10 | Análisis 3 días; los errores vuelven a Codificación | 🟡 | `data/initialData.ts` (A4 `plazoDiasHabiles: 3`), `orderState.ts:360` | El plazo está bien; los errores solo vuelven con urgencia (no hay devolución simple) |
| 11 | Producto nuevo vuelve con urgencia 4/12/24 h hasta A4/B5 | 🟡 | `orderState.ts:373-382`, `orderState.ts:258` | `fechaLimite` es una hora fija y la urgencia se borra al primer check (A2→A3), no al volver a Análisis |
| 12 | Embarque 10 días con Packing List / BL | 🟡 | `initialData.ts` (A6 y B7 marcan PL y BL obligatorios), `StagePanel.tsx:99` | `missingDocs` se calcula pero no bloquea el check |
| 13 | Demora en aduana: observación y recálculo | 🟡 | `orderState.ts:449-502`, `StagePanel.tsx:712` | El recálculo funciona; la nueva fecha está fija en el código (`'2026-10-16'`) |
| 14 | Bodega de una lista abierta que mantiene Anderson | ✅ | `components/WarehouseCatalogView.tsx:25`, `StagePanel.tsx:294-325` | Admin y Logística pueden editarla |
| 15 | Archivos en Google Drive, 100 MB por archivo | ❌ | `StagePanel.tsx:268-281`, `orderState.ts:621-687` | Subida simulada: siempre crea un PDF de "8.4 MB" con un enlace falso; no hay selector de archivo ni validación |
| 16 | Cada etapa y cada vencimiento notifican al admin; el motivo queda visible | 🟡 | `App.tsx:102-191` | Hay correo simulado al check, al rechazo, a la urgencia y a la demora. No hay al elegir bodega ni al vencer un plazo |
| 17 | Búsqueda con flujo, etapa actual y resumen de etapas anteriores | ✅ | `components/TrackingSearchView.tsx:96-213` | |
| 18 | Dos países; primero Ecuador | ✅ | `LoginScreen.tsx:80-110`, `App.tsx:299-308` | Colombia se puede elegir pero no tiene proceso, como se pidió |
| 19 | API de Claude más adelante | ⏸ | — | `@google/genai` está instalado pero no se usa |

### 2.2 Respuestas del cliente (2026-10-09)

| Requisito | Estado | Dónde | Nota |
| --- | --- | --- | --- |
| Plazos en días hábiles, hora de Ecuador | 🟡 | `utils/dateUtils.ts:26-65` | Cuenta días hábiles, pero solo por día: no maneja horas (4/12/24 h) ni zona horaria (usa mediodía UTC) |
| Codificación, Revisión, Salida y Bodega: 24 h | ✅ | `initialData.ts` (`plazoHorasHabiles: 24`, `plazoDiasHabiles: 1`) | |
| Fin de semana pasa al lunes, también la llegada | ✅ | `dateUtils.ts:45-65` | El tránsito de 45 días se cuenta en días hábiles; en la calculadora original eran días corridos. Confirmar con el cliente |
| Un retraso arrastra las fechas siguientes | ✅ | `orderState.ts:61-96` | |
| Fin se marca solo | ✅ | `orderState.ts:218-238` | En ambos flujos |
| Línea horizontal en cada dashboard | 🟡 | `components/HorizontalStatusLine.tsx` | Solo se usa en `OrderDetail.tsx:225`; no aparece en los dashboards ni en la búsqueda |
| El admin actúa en cualquier etapa o la reasigna | 🟡 | `permissions.ts:4`, `StagePanel.tsx:346` | Puede actuar; reasignar no existe (el texto lo menciona, pero no hay función) |
| Los documentos solo se suben, sin validar contenido | ✅ | `StagePanel.tsx` | |
| Solo web | ✅ | — | |

### 2.3 Invariantes

| Requisito | Estado | Dónde | Nota |
| --- | --- | --- | --- |
| Una sola etapa activa | ✅ | `types.ts:223` (`etapaActualNumero`) | |
| Solo el responsable o un admin actúa | 🟡 | `permissions.ts:3-10`, `StagePanel.tsx:355` | Solo en el cliente; además, cualquiera puede cambiarse a admin desde el encabezado (`Header.tsx:105`) |
| El historial no se edita ni se borra | 🟡 | `orderState.ts` (cada acción antepone un evento) | No hay función que lo edite, pero está en memoria y se pierde al recargar |
| País y flujo no cambian después de creado | ✅ | `types.ts:213-214` | |
| Archivo ligado a la etapa y al evento | 🟡 | `orderState.ts:637-680` | Ligado a la etapa sí; el evento del historial no guarda el id del archivo |
| El plazo se reinicia en cada reentrada | ❌ | `orderState.ts:61-90` | Las fechas se recalculan desde la fecha del pedido, no desde la reentrada a la etapa |

### 2.4 Límites de confianza

| Requisito | Estado | Dónde | Nota |
| --- | --- | --- | --- |
| El navegador nunca llama a Google | ✅ | — | No hay llamadas reales (todo está simulado) |
| Autenticación propia (JWT + BCrypt) | ❌ | `LoginScreen.tsx:35`, `data/initialData.ts:13` | Contraseña única en texto plano dentro del código |
| Credenciales de Google y Claude solo en el servidor | ✅ | `frontend/.env.example` | No hay credenciales en el código. `.env.example` trae `GEMINI_API_KEY` de AI Studio, que sobra |

## 3. Sección 4.3 — Actores y superficies

| Actor | Estado | Dónde | Nota |
| --- | --- | --- | --- |
| Admin (Mirian) | 🟡 | `DashboardView.tsx`, `OrdersListView.tsx`, `GmailNotificationsView.tsx`, `AuditTrailView.tsx` | Falta la gestión de usuarios y roles, y la reasignación |
| Marketing / Codificación (Andrea Q.) | ✅ | `DashboardView.tsx:67-69` (`misTareasPendientes`), `orderState.ts:151` | Bandeja con aviso de urgencia y semáforo de plazo |
| Asistente de compras (Marcela) | ✅ | `StagePanel.tsx:372-385` | Revisión y productos propuestos con ✓/✗ |
| Compras China (David) | 🟡 | `StagePanel.tsx:389-411`, `OrdersListView.tsx:51` | Falta la devolución por error sin urgencia; PL/BL no son obligatorios; la fecha de demora está fija |
| Logística (Anderson) | ✅ | `StagePanel.tsx:294-325`, `WarehouseCatalogView.tsx` | Salida, bodega, incidencias y catálogo |
| Asistente de compras en China (Andrea C.) | ✅ | `DashboardView.tsx:102`, `types.ts:7` | Dashboard de consulta. Qué ve exactamente sigue sin definir |
| Sistema | 🟡 | `orderState.ts:218-238` | Fin automático sí. Faltan las tareas programadas, la detección de vencimientos y la copia real a Drive |

## 4. Sección 4.4 — Transiciones del flujo A

| De → A | Acción | Estado | Dónde | Nota |
| --- | --- | --- | --- | --- |
| A1 → A2 | Enviar (admin), solicitud obligatoria | 🟡 | `orderState.ts:177` | La solicitud es obligatoria en la definición, pero no se exige |
| A2 → A3 | Enviar (marketing), documentos codificados | 🟡 | `orderState.ts:177` | Documento no exigido; la urgencia no se mantiene hasta A4 |
| A3 → A4 | ✓ Aprobar (asist. compras), 24 h | ✅ | `orderState.ts:177` | |
| A3 → A2 | ✗ Rechazar con observación | ✅ | `orderState.ts:280`, `StagePanel.tsx:566` | |
| A4 → A5 | ✓ Aprobar (compras China), 3 días | ✅ | `orderState.ts:177` | |
| A4 → A2 | Devolver por error | 🟡 | `orderState.ts:360` | Solo existe con urgencia |
| A4 → A2 | Devolver por producto nuevo con urgencia | 🟡 | `orderState.ts:360-444` | El plazo no llega hasta A4 (ver 2.1 #11) |
| A5 → A6 | ✓ al terminar, con observaciones | ✅ | `initialData.ts` (A5 de 39 días) | Plazo tomado de la calculadora (8 + 31); el cliente aún no lo define |
| A6 → A7 | ✓ Enviado, con PL/BL | 🟡 | `StagePanel.tsx:99` | PL/BL no se exigen |
| A7 → A8 | ✓ Llegada | ✅ | `initialData.ts` (A7 de 45 días) | |
| A7 → A7 | Reportar demora con observación | 🟡 | `StagePanel.tsx:712` | La fecha nueva está fija en el código |
| A8 → A9 | ✓ Autorización de salida, 24 h | ✅ | `orderState.ts:177` | |
| A9 → A10 | ✓ con bodega elegida y documentos | 🟡 | `StagePanel.tsx:294-325` | Se puede dar check sin confirmar la bodega |
| A10 → A11 | ✓ con todos los documentos; Fin automático | 🟡 | `orderState.ts:218-238` | Fin automático sí; el expediente obligatorio no se exige |

## 5. Sección 4.5 — Transiciones del flujo B

| De → A | Acción | Estado | Dónde | Nota |
| --- | --- | --- | --- | --- |
| B1 → B2 | Enviar (compras China) | 🟡 | `orderState.ts:177` | La propuesta obligatoria no se exige |
| B2 → B3 / B1 | ✓ / ✗ (asist. compras) | 🟡 | `orderState.ts:290-291`, `orderState.ts:343` | El motor lo hace bien, pero el estado queda como "Devuelto a codificación" al volver a B1, y el correo dice "volvió a Codificación" (`App.tsx:140-141`) |
| B3 → B4 | Enviar (marketing) | 🟡 | `StagePanel.tsx:361`, `StagePanel.tsx:372` | Error: en B3 se muestran "Aprobar Revisión (✓)" y "Rechazar" |
| B4 → B5 / B3 | ✓ / ✗ (asist. compras) | 🟡 | `StagePanel.tsx:389` | Error: en B4 se muestra el botón de David "Devolver con urgencia" |
| B5 → B6 / B3 | ✓ / devolver / producto nuevo | ✅ | `orderState.ts:370` | Vuelve a B3, como corrigió el cliente. Mismo pendiente que A4: no hay devolución sin urgencia |
| B6 → B12 | Igual que A5–A11; B12 Fin automático | ✅ | `orderState.ts:218-220` | Con los mismos pendientes que A6, A7, A9 y A10 |

## 6. Errores encontrados

| # | Error | Dónde | Efecto |
| --- | --- | --- | --- |
| 1 | La condición `stageNumber === 3` no distingue flujos | `StagePanel.tsx:361` y `StagePanel.tsx:372` | En B3 (Codificación) aparecen "Aprobar Revisión" y "Rechazar". Un rechazo ahí deja el pedido en B3 con estado "Devuelto a codificación" |
| 2 | La condición `stageNumber === 4` no distingue flujos | `StagePanel.tsx:389` | En B4 (Revisión) aparece "Devolver con urgencia", que es una acción de David |
| 3 | El estado del rechazo siempre es "Devuelto a codificación" | `orderState.ts:343` | Al rechazar en B2 el pedido vuelve a B1, pero el estado dice Codificación |
| 4 | El correo de rechazo siempre dice "Revisión" y "volvió a Codificación" | `App.tsx:140-141` | Texto incorrecto en el flujo B |
| 5 | La sesión arranca iniciada como admin | `App.tsx:49` | Se salta la pantalla de login |
| 6 | Se calculan los documentos obligatorios pero no se usan | `StagePanel.tsx:99` | Ver 2.1 #12 y la sección 4 |

## 7. Calidad técnica: React, diseño y accesibilidad

Integra la revisión de calidad React (`/ecc:react-review`, 0 críticos, 5 altos, 11 medios) y la auditoría técnica de diseño (`/impeccable audit`). Los hallazgos que aparecían en las dos se juntaron en uno solo, y no se repiten los hallazgos funcionales de las secciones 2 a 6.

### 7.1 Chequeos automáticos

| Chequeo | Resultado |
| --- | --- |
| `npm run lint` (`tsc --noEmit`) | 0 errores. `tsconfig.json` no tiene `strict`, así que el chequeo es poco exigente |
| ESLint | No existe: no hay reglas de hooks (`react-hooks`) ni de accesibilidad (`jsx-a11y`) |
| `npm audit --omit=dev` | 0 vulnerabilidades |
| Seguridad propia de React | Limpia: sin `dangerouslySetInnerHTML`, sin `href`/`src` con datos del usuario, sin `localStorage`, sin variables `VITE_*` en el código |
| Reglas de hooks y mutación de estado | Correctas: sin hooks condicionales; `orderState.ts` copia los arrays antes de modificarlos |
| Detector de Impeccable | 1 aviso (`animate-bounce`, `HorizontalStatusLine.tsx:106`), y es un falso positivo: la clase real es `animate-bounce-subtle`, que no está definida y no hace nada |

### 7.2 Puntuación de diseño (Impeccable)

| Dimensión | Nota (0-4) | Motivo |
| --- | --- | --- |
| Accesibilidad | 1 | Modales, teclado, etiquetas y contraste del semáforo (Q1 a Q5) |
| Rendimiento | 3 | Sin problemas reales; solo `backdrop-blur` en modales |
| Theming | 1 | Tokens definidos en `index.css` pero ignorados (Q8) |
| Responsive | 2 | Hay breakpoints (127 usos de `sm:`), pero el texto en px fijos no escala (Q5) |
| Integridad | 2 | Identidad de marca coherente, pero sin sistema: tokens ignorados y modales copiados (Q8, Q14) |
| **Total** | **9/20** | Nivel **Pobre** (6 a 9) |

### 7.3 Hallazgos consolidados

Severidad: **P1** hay que corregirlo antes de producción (incumple WCAG AA o puede causar errores); **P2** conviene corregirlo; **P3** detalle menor. Origen: **R** = revisión React, **D** = auditoría de diseño, **R+D** = las dos.

En la última columna, **Sí** significa que el problema está en un componente que se reutiliza y hay que corregirlo al migrarlo. **No** significa que desaparece al reescribir o descartar ese código.

| # | Hallazgo | Sev. | Dónde | Origen | ¿Sigue al migrar? |
| --- | --- | --- | --- | --- | --- |
| Q1 | Ningún modal es un diálogo accesible: sin `role="dialog"` ni `aria-modal`, sin trampa de foco, sin cierre con Escape; el fondo sigue alcanzable con Tab (WCAG 2.4.3, 4.1.2) | P1 | `App.tsx:439`, `OrdersListView.tsx:331`, `OrderDetail.tsx:345, 388, 431`, `StagePanel.tsx:475, 531, 584, 663`, `WarehouseCatalogView.tsx:154` | R+D | Sí en OrdersListView, OrderDetail y WarehouseCatalogView. Usar `Dialog`/`AlertDialog` de shadcn |
| Q2 | Elementos clicables que no son botones: no se usan con teclado (WCAG 2.1.1). Con teclado no se puede abrir un pedido desde el dashboard | P1 | `DashboardView.tsx:171-173, 288-291`, `OrdersListView.tsx:359-360, 373-374` (selector de flujo), `StagePanel.tsx:189-191` (checklist), `Header.tsx:102-106` | R+D | Sí en DashboardView y OrdersListView |
| Q3 | Unos 30 campos sin etiqueta accesible: `<label>` sin `htmlFor`, y buscadores y filtros con solo `placeholder`. Solo hay 2 `aria-label` en todos los componentes (WCAG 1.3.1, 4.1.2) | P1 | Labels: `LoginScreen.tsx:132, 155`, `OrdersListView.tsx:355, 391, 420, 435, 449`, `StagePanel.tsx:492, 543, 596, 622, 675, 689`, `WarehouseCatalogView.tsx:166, 180, 194`, `TimelineCalculatorView.tsx:125, 140`. Solo placeholder: `OrdersListView.tsx:174, 185, 198`, `TrackingSearchView.tsx:55`, `WarehouseCatalogView.tsx:83`, `GmailNotificationsView.tsx:57, 67`, `AuditTrailView.tsx:67, 78` | R+D | Sí en buscadores y filtros. Los formularios pasan a React Hook Form + shadcn |
| Q4 | Contraste insuficiente en el texto del semáforo: verde `#16A34A` sobre blanco 3.3:1 y naranja `#EA580C` 3.56:1 (AA pide 4.5:1). Números blancos sobre `bg-emerald-500` en la línea de tiempo, unos 2.5:1 (WCAG 1.4.3) | P1 | `OrdersListView.tsx:291-292`, `DashboardView.tsx:332-333`, `TrackingSearchView.tsx:132-133`, y 12 usos más; `HorizontalStatusLine.tsx:104` | D | Sí. Usar `#15803D` y `#C2410C` para texto; los colores actuales, solo en iconos y fondos |
| Q5 | Texto demasiado pequeño y fijo en px: 70 usos de `text-[10px]` y 52 de `text-[11px]`, incluidos responsables, estados y fechas. No crece con la configuración de letra del navegador (WCAG 1.4.4) | P1 | Todos los componentes; por ejemplo `StagePanel.tsx:209-212` | R+D | Sí. Mínimo de 12 px, en rem, desde los tokens |
| Q6 | Estado que se queda viejo. `OrderDetail` copia `pedido.etapaActualNumero` a su estado y tras dar check sigue mostrando la etapa anterior. `StagePanel` conserva textos y modales al cambiar de etapa, y una observación a medio escribir puede publicarse en otra etapa. `TrackingSearchView` guarda una copia del pedido que no se actualiza, con un código fijo `'DK-EC-2026-0004'` | P1 | `OrderDetail.tsx:70, 315`, `StagePanel.tsx:68-86`, `TrackingSearchView.tsx:16-20, 32, 82` | R | Sí en OrderDetail y TrackingSearchView. Usar `key` por pedido y etapa, o derivar el valor en lugar de copiarlo |
| Q7 | Sin ESLint (`react-hooks`, `jsx-a11y`) y sin `strict` en TypeScript: nada detecta automáticamente los hallazgos Q1 a Q3 ni los de hooks | P1 | `frontend/package.json:5-11`, `frontend/tsconfig.json` | R | Sí: es configuración del proyecto nuevo |
| Q8 | Los tokens de color existen pero no se usan: `index.css` define `--color-brand-*` y `--color-semaforo-*`, y los componentes escriben 653 hex a mano (`#2E2E2E` 193 veces, `#6C6B6D` 191, `#515151` 116, `#FFD100` 86) | P2 | `src/index.css:3-13` y todos los componentes | D | Sí. Se resuelve con el sistema de diseño (paso 0.11) |
| Q9 | Navegación móvil y menú sin ARIA: el menú cerrado sigue alcanzable con Tab fuera de pantalla; el botón hamburguesa no tiene `aria-expanded`; el ítem activo no tiene `aria-current`; los botones de cerrar que son solo una X no tienen nombre | P2 | `Sidebar.tsx:64-67, 73-77, 101-115`, `Header.tsx:36-43`, `OrdersListView.tsx:348` | R+D | Sí. Usar `Sheet` de shadcn y `NavLink` |
| Q10 | Los toasts no se anuncian: sin `role="status"` ni `aria-live`, y la región no existe hasta que aparece un toast | P2 | `Toast.tsx:15-19` | R+D | Sí, o cambiar por Sonner |
| Q11 | Las pestañas del detalle no tienen semántica ARIA (`tablist`, `tab`, `aria-selected`) ni navegación con flechas; son 5 botones copiados | P2 | `OrderDetail.tsx:235-300` | R | Sí. Usar `Tabs` de shadcn |
| Q12 | Sin alternativa para movimiento reducido (0 reglas de `prefers-reduced-motion`). Además, las 24 clases `animate-in`, `fade-in` y `zoom-in` no hacen nada porque falta el plugin; empezarán a animar al instalar shadcn | P2 | Por ejemplo `Toast.tsx:23`, `OrdersListView.tsx:142` | R+D | Sí. Definir la alternativa al instalar `tw-animate-css` |
| Q13 | Jerarquía de encabezados con saltos de h1 a h3, y la etapa seleccionada en la línea de tiempo solo cambia de color, sin `aria-current="step"` | P2 | `GmailNotificationsView.tsx:130`, `WarehouseCatalogView.tsx:111`, `OrderDocumentsTab.tsx:15`, `OrderHistoryTab.tsx:13`, `OrderScheduleTab.tsx:14`, `HorizontalStatusLine.tsx:86-98` | R | Sí |
| Q14 | Componentes muy grandes y modales copiados: StagePanel 726 líneas, App 515, OrdersListView 491, OrderDetail 465. Hay 3 modales casi iguales en OrderDetail y 4 en StagePanel | P2 | `OrderDetail.tsx:345-461`, `StagePanel.tsx:475-723` | R+D | Sí en OrderDetail y OrdersListView. Crear un solo diálogo de "confirmar con motivo" |
| Q15 | `App.tsx` concentra todo el estado: repite 4 veces la construcción del correo (líneas 109, 131, 153, 176); `pedidos[0].id` falla con la lista vacía (310-312); el detalle se ve en blanco si no hay pedido elegido (360) | P3 | `App.tsx:47-515` | R | No: se reescribe con React Router + TanStack Query |
| Q16 | El formulario de crear pedido tiene 6 `useState` sueltos, no limpia todos los campos al enviar ni al cancelar, y si falta el nombre no avisa nada | P3 | `OrdersListView.tsx:41-48, 79, 136-138, 345, 473` | R | No: pasa a React Hook Form + Zod |
| Q17 | La bodega se guarda en cada cambio del `select` (un toast y un evento por cambio), y "Confirmar Bodega" repite la acción. Complementa el hallazgo funcional 2.1 #14 | P3 | `StagePanel.tsx:85-87, 306-326` | R | No: StagePanel se reescribe; no repetir el patrón |
| Q18 | Campos numéricos que no se pueden borrar: al vaciarlos saltan a 1 o a 90 | P3 | `StagePanel.tsx:683`, `TimelineCalculatorView.tsx:141` | R | No: los dos se reescriben |
| Q19 | Detalles menores: la clase `animate-bounce-subtle` no está definida; `backdrop-blur` en los modales; bordes redondeados mezclados sin regla (`rounded-xl`, `2xl`, `3xl`) | P3 | `HorizontalStatusLine.tsx:106`, `OrderDetail.tsx:346, 389, 432` | D | Sí, al aplicar el sistema de diseño |

### 7.4 Lo que se queda en cada componente reutilizado

Para usar al migrar cada componente en la fase 1:

| Componente | Hallazgos que corregir al migrarlo |
| --- | --- |
| `HorizontalStatusLine` | Q4, Q13, Q19 |
| `DashboardView` | Q2, Q4, Q5 |
| `OrdersListView` | Q1, Q2, Q3, Q4, Q14 |
| `OrderDetail` | Q1, Q6, Q11, Q14 |
| `OrderDocumentsTab`, `OrderHistoryTab`, `OrderScheduleTab` | Q5, Q13 |
| `TrackingSearchView` | Q3, Q4, Q6 |
| `WarehouseCatalogView` | Q1, Q3, Q13 |
| `GmailNotificationsView` | Q3, Q13 |
| `AuditTrailView` | Q3 |
| `Header` | Q9 (Q2 desaparece al quitar el cambio rápido de rol) |
| `Sidebar` | Q9 |
| `Toast` | Q10, Q12 |

Q5, Q7, Q8 y Q12 son de todo el proyecto y se resuelven una sola vez: ESLint y `strict` al crear el frontend nuevo (fase 1), y los tokens y tamaños en el sistema de diseño (paso 0.11).

### 7.5 Lo que funciona bien

- **Contrastes principales:** texto oscuro sobre amarillo 9.29:1; `#515151` sobre gris claro 7.22:1; `#6C6B6D` sobre blanco 5.3:1.
- **El semáforo no depende solo del color:** combina color, icono y texto.
- **Pantallas angostas:** las tablas están dentro de contenedores con desplazamiento horizontal, y la línea de tiempo se desplaza a lo ancho (`min-w-max`).
- **Rendimiento:** no hay imágenes pesadas ni animaciones caras.
- **Hooks:** bien usados, y el único `useEffect` tiene dependencias correctas.

## 8. Clasificación de componentes

### 8.1 Se reutilizan (se conserva la pantalla y se conecta a la API)

| Archivo | Qué cambia |
| --- | --- |
| `components/HorizontalStatusLine.tsx` | Llevarla también a los dashboards y a la búsqueda |
| `components/DashboardView.tsx` | Datos desde la API; bandeja por rol |
| `components/OrdersListView.tsx` | La lista se conserva; el formulario de crear pasa a React Hook Form + Zod y el número lo genera el backend |
| `components/OrderDetail.tsx` | Datos desde la API |
| `components/OrderDocumentsTab.tsx` | Subida real con progreso y estado de copia a Drive |
| `components/OrderHistoryTab.tsx` | Datos desde la API |
| `components/OrderScheduleTab.tsx` | Fechas calculadas en el backend |
| `components/TrackingSearchView.tsx` | Búsqueda en la API; agregar la línea horizontal |
| `components/WarehouseCatalogView.tsx` | Datos desde la API |
| `components/GmailNotificationsView.tsx` | Muestra el registro real de correos enviados y fallidos |
| `components/AuditTrailView.tsx` | Datos desde la API |
| `components/Header.tsx` | Quitar el cambio rápido de rol |
| `components/Sidebar.tsx` | Rutas con React Router |
| `components/Toast.tsx` | Se puede mantener o cambiar por el toast de shadcn/ui |

### 8.2 Se reescriben

| Archivo | Por qué |
| --- | --- |
| `App.tsx` | Concentra todo el estado y la navegación; pasa a React Router + TanStack Query, sin datos en memoria |
| `main.tsx` | Agregar el proveedor de TanStack Query y el router |
| `components/LoginScreen.tsx` | Autenticación real con JWT; quitar los botones de ingreso rápido y la contraseña de demo |
| `components/UsersManagementView.tsx` | Hoy solo lista usuarios; debe crear, editar, activar y desactivar usuarios y asignar roles |
| `components/StagePanel.tsx` | Corregir los errores 1, 2 y 6; agregar devolución sin urgencia, archivos en rechazos y devoluciones, fecha de demora elegida por David, bodega obligatoria y reasignación |
| `components/TimelineCalculatorView.tsx` | Quitar los comandos de Slack; el cronograma viene del backend |
| `utils/orderState.ts` | La lógica pasa al dominio del backend (fase 2). Sirve como referencia de reglas; en el frontend no debe quedar lógica de negocio |
| `utils/permissions.ts` | Los permisos pasan al servidor; en el cliente quedan solo los colores y nombres de rol |
| `utils/dateUtils.ts` | Los cálculos de plazo pasan al backend; en el cliente quedan los formateadores de fecha. Quitar `SIMULATED_TODAY` |
| `types.ts` | Alinearlo con el contrato OpenAPI del paso 0.8 |
| `index.css` | Pasar a los tokens del sistema de diseño (paso 0.11) |

### 8.3 Se descartan

| Archivo o parte | Por qué |
| --- | --- |
| `data/initialData.ts` | Los datos pasan a migraciones y semillas del backend. Las definiciones de etapas sirven de referencia para la migración del paso 2.2 |
| `components/OrderProductsTab.tsx` | Sigue producto por producto; el cliente indicó que ese detalle va en los archivos |
| `components/HelpView.tsx` | Copia de los requerimientos dentro de la app; se reemplaza por el manual de usuario de la fase 8 |
| `utils/translations.ts` | No lo usa ningún archivo; la app es solo en español |
| Ventana para cambiar de usuario en `App.tsx:439-470` | Permite hacerse pasar por cualquier rol |
| `metadata.json` | Configuración de AI Studio |
| Dependencias `@google/genai`, `express`, `dotenv`, `motion`, `tsx`, `@types/express`, `autoprefixer` | Ningún archivo de `src/` las importa, y `autoprefixer` no tiene configuración de PostCSS (sección 10, S7) |

## 9. Lo que no se pudo comprobar

- El comportamiento visual: no se ejecutó el demo en el navegador. El diseño responsive, el tacto en pantalla y el foco visible se evaluaron leyendo el código, sin capturas.
- La revisión React leyó solo fragmentos de `OrderProductsTab`, `UsersManagementView`, `TimelineCalculatorView` y `orderState.ts`, y no leyó `HelpView`. Esos archivos se reescriben o se descartan (sección 8).
- Si los correos de los usuarios demo son reales, y si el repositorio de GitHub es privado: no se pudo consultar porque `gh` no está instalado (sección 10, S5).
- Si los 45 días de tránsito deben ser hábiles o corridos (ver 2.2).
- Qué debe ver la asistente de compras en China (sigue sin definir en los requerimientos).

## 10. Seguridad y preparación para producción (paso 0.4)

**Fecha:** 2026-10-10. **Alcance:** `frontend/src/`, `frontend/package.json` y los archivos de configuración del frontend. Solo revisión; no se modificó código.

**Veredicto: el demo no puede pasar a producción.** Hay 4 hallazgos críticos: cualquiera entra como admin o cambia de rol, y la contraseña está escrita en el código. Es lo esperable en un demo sin backend. Las fases 1 a 3 y 8 del manual los resuelven.

### 10.1 Herramientas y resultados

| Herramienta | Resultado |
| --- | --- |
| AgentShield (`npx ecc-agentshield scan --path frontend`) | Nota B (80/100), con 270 hallazgos "críticos" de clave de Azure en `package-lock.json`. **Todos son falsos positivos:** son los hashes `sha512` del campo `integrity` del lockfile (comprobado en `package-lock.json:3908`). AgentShield revisa configuraciones de agentes; analizó solo 2 archivos y no revisa el código TSX |
| `npm audit --omit=dev` | 0 vulnerabilidades (sección 7.1) |
| Revisión manual de `src/`, `package.json`, `vite.config.ts`, `index.html`, `.env.example` y `metadata.json` | Hallazgos S1 a S11 |

### 10.2 Hallazgos

Severidad: **Crítico** permite tomar el control del sistema; **Alto** expone datos o hace pasar como reales datos inventados; **Medio** aumenta el riesgo o confunde; **Bajo** solo afecta al desarrollo o se resuelve en el despliegue. Todos bloquean el paso a producción, salvo los Bajos.

| # | Hallazgo | Sev. | Dónde | Relación con lo ya documentado | Se resuelve en |
| --- | --- | --- | --- | --- | --- |
| S1 | Contraseña única de demo (`Demo2026`) escrita en el código y comparada en el navegador; cualquiera que abra el bundle la ve | Crítico | `data/initialData.ts:13`, `components/LoginScreen.tsx:35` | Amplía 2.4 (falta la autenticación propia) | Fase 1: login con JWT y BCrypt en el servidor |
| S2 | Cualquier usuario puede tomar otro rol, admin incluido: cambio rápido en el encabezado, ventana de cambio de usuario, botones de ingreso rápido en el login y "cambiar de sesión" en la gestión de usuarios | Crítico | `Header.tsx:102-106`, `App.tsx:439-470`, `LoginScreen.tsx:45-50, 198`, `UsersManagementView.tsx` (`onSwitchUser`) | La sección 8.3 ya descarta la ventana de `App.tsx`; aquí se suman las otras tres entradas | Fase 1 |
| S3 | La aplicación arranca con sesión iniciada como admin, sin pasar por el login | Crítico | `App.tsx:49` | Es el error 5 de la sección 6; aquí se marca como riesgo de seguridad | Fase 1 |
| S4 | Todos los permisos y el historial se deciden en el navegador: los botones solo se desactivan en pantalla, y quien controle el navegador puede actuar en cualquier etapa o crear eventos de historial con cualquier usuario y fecha | Crítico | `utils/permissions.ts`, `StagePanel.tsx:355`, `utils/orderState.ts` (eventos `hist-${Date.now()}`) | Amplía 2.3 (responsable de la etapa e historial inmutable) | Fases 1 y 2: permisos y eventos en el servidor |
| S5 | Datos de personas reales en el código y en GitHub: los 7 usuarios con nombre, correo corporativo y teléfono. Además, los correos se repiten en las notificaciones de ejemplo y en `App.tsx` (`mfranco@decokasa.ec` aparece 9 veces). Los teléfonos parecen inventados (números secuenciales como `+593 99 123 4567`); los correos siguen el formato de la empresa y podrían ser reales | Alto | `data/initialData.ts:18-125`, `App.tsx:113, 135, 157, 180` | Nuevo | Ahora: confirmar con el cliente si los correos son reales y que el repositorio es privado. Fase 1: los usuarios pasan a la base de datos y las semillas usan datos de prueba |
| S6 | Datos y fechas inventados que la pantalla muestra como reales: "hoy" fijo (`SIMULATED_TODAY`, 40 usos); horas fijas en los eventos del historial (`'12:30'`, `'14:15'`… en 10 lugares); subida que siempre "crea" un PDF de 8.4 MB con un enlace de Drive inventado; fecha de demora fija; destinatario de correo fijo; pedidos y correos de ejemplo cargados al iniciar | Alto | `utils/dateUtils.ts:3`, `utils/orderState.ts:209-759`, `orderState.ts:645-646`, `StagePanel.tsx:268-281, 712`, `App.tsx:113`, `data/initialData.ts` (`PEDIDOS_INICIALES`, `NOTIFICACIONES_GMAIL_INICIALES`) | Reúne datos ya citados por separado en 2.1 #13 y #15, 2.2 y 8.3 | Fases 1 a 3: fecha y hora del servidor (hora de Ecuador), subida real y datos desde la base |
| S7 | Dependencias sin usar que no deben ir a producción: `@google/genai`, `express`, `dotenv` y `motion` (de ejecución); `tsx`, `@types/express` y `autoprefixer` (de desarrollo; no hay configuración de PostCSS y Tailwind 4 no lo necesita). `express` y `@google/genai` son librerías de servidor y sugieren capacidades que la app no tiene | Medio | `frontend/package.json:13-34` | La sección 8.3 ya las descarta; aquí se suma `autoprefixer` y se marca el riesgo | Fase 1 (paso 1.2) |
| S8 | Restos de la configuración de AI Studio que invitan a poner una clave en el frontend: `GEMINI_API_KEY` y `APP_URL` en `.env.example`; el README pide poner la clave en `.env.local`; `metadata.json` declara la capacidad `SERVER_SIDE_GEMINI_API`. Si alguien la renombra con prefijo `VITE_`, la clave termina en el bundle público | Medio | `frontend/.env.example`, `frontend/README.md`, `frontend/metadata.json` | La sección 8.3 descarta `metadata.json`; aquí se suman `.env.example` y el README | Fase 1. La clave de Claude vive solo en el backend (fase 7) |
| S9 | La subida de archivos no valida nada: ni tipo, ni tamaño, ni nombre. El enlace de Drive se arma con el nombre del archivo sin codificar | Medio | `StagePanel.tsx:268-281`, `utils/orderState.ts:646` | Amplía 2.1 #15 (subida simulada) | Fase 3: validación en el servidor (tipo, 100 MB, nombre) y enlaces que entrega Drive |
| S10 | El servidor de desarrollo escucha en toda la red (`vite --host=0.0.0.0`): cualquier equipo de la misma red puede abrir el demo | Bajo | `frontend/package.json:7` | Nuevo | Usar solo en desarrollo; producción sirve el build con nginx (fase 8) |
| S11 | Sin cabeceras de seguridad ni política de contenido (CSP): no hay servidor que las envíe | Bajo | `frontend/index.html` | Nuevo | Fase 8: cabeceras y CSP en nginx |

**Conteo:** 4 críticos, 2 altos, 3 medios y 2 bajos (11 en total).

### 10.3 Lo que está bien

- No hay claves reales en el código ni en el repositorio. `.env` está en `.gitignore`, y `.env.example` solo trae valores de ejemplo.
- `vite.config.ts` no inyecta variables de entorno en el bundle, y el código no usa `import.meta.env`.
- No hay `dangerouslySetInnerHTML`, ni enlaces con datos del usuario, ni tokens en `localStorage` (sección 7.1).
- `index.html` no carga scripts ni fuentes de terceros.

### 10.4 Orden de corrección

1. **Ahora, sin código:** confirmar con el cliente si los correos de S5 son reales. El 2026-10-12 se comprobó que el repositorio de GitHub es **público**; queda decidir si pasa a privado.
2. **Fase 1:** S1, S2, S3, S7 y S8, junto con el login real y la limpieza de `package.json`.
3. **Fase 2:** S4, con permisos y eventos validados en el servidor.
4. **Fase 3:** S9 y la parte de S6 que corresponde a la subida de archivos.
5. **Fase 8:** S10 y S11, más la auditoría final del paso 8.5.

## 11. Reauditoría del árbol actual y consistencia documental — 2026-10-13

### 11.1 Alcance, fuentes y veredicto

**No apto para producción ni para dar por terminado el paso 1.8.** El frontend servido sigue siendo el demo: `main.tsx:11` monta `App`, `App.tsx:49` inicia como admin y carga pedidos, bodegas y correos en memoria. Se añadió un cliente API, esquemas y pruebas, pero no están integrados con la aplicación montada. `auth/SesionProvider.tsx:5` solo devuelve sus hijos y `rutas.tsx:7` contiene rutas sin elementos. Tener dependencias, pruebas o un backend implementado no demuestra integración frontend.

Se revisó el árbol de trabajo de `fase-1-base-acceso`, HEAD `0fe8fcc`, incluyendo cambios locales preexistentes: pruebas modificadas, `src/api/`, pruebas de coordinación de sesión, roles, dashboards, CSP y limpieza del demo, y primitivas UI nuevas. No se sobrescribieron esos cambios ni se corrigió código: esta entrega modifica únicamente este informe. Los resultados son una fotografía del árbol revisado, no del commit limpio ni de una versión desplegada.

Fuentes contrastadas: `requerimientos.md` completo (especialmente 4.2–4.9 y respuestas de la sección 5), `.claude/PRPs/prds/decokasa.prd.md`, los 16 ADR y su índice, `arquitectura.md`, `api/openapi.yaml` 0.5.0-borrador, las tablas de `modelo-datos.md`, los seis `.mmd` y `diseno/DESIGN.md`. La revisión de código incluye componentes, utilidades, montaje, modelos, sesión, cliente API y soporte de pruebas.

Se aplicaron `/ecc:react-review` (revisión React y revisión TypeScript complementaria), `impeccable audit` y `/ecc:security-scan`. Las instrucciones se localizaron en `C:/Users/Richard/.claude/plugins/marketplaces/ecc/commands/react-review.md`, sus agentes `react-reviewer.md` y `typescript-reviewer.md`, `C:/Users/Richard/.claude/skills/impeccable/SKILL.md` y `C:/Users/Richard/.claude/plugins/marketplaces/ecc/skills/security-scan/SKILL.md`. AgentShield audita configuración de agentes; no sustituye la revisión de seguridad del TSX.

**Criterio de aprobación:** una respuesta del cliente y una decisión técnica aprobada por Richard son orígenes distintos. Un ADR `proposed`, `[P]`, `⏸`, `TBD`, `x-origen: propuesta` o `x-pendiente` sigue pendiente aunque aparezca en un formulario, una tabla, un diagrama o un campo obligatorio del contrato borrador. Este informe registra diferencias; no cambia su estado.

### 11.2 Comprobaciones realizadas

| Comprobación | Resultado actual y límite |
| --- | --- |
| `npm run lint` | Falla: `SesionProvider.test.tsx:113` pasa `unknown` donde MSW exige `JsonBodyType`; `coordinadorSesion.test.ts:11` importa un módulo inexistente; líneas 59, 60 y 137 tienen parámetros `any` implícitos. Es TypeScript, no ESLint |
| Configuración TypeScript | `strict: true` ya existe. La parte de Q7 que decía «sin strict» está resuelta; faltan ESLint y las reglas de hooks y accesibilidad |
| `npm run test` | Reejecutado con reporte JSON fuera del sandbox: 125 pruebas, 48 pasan y 77 fallan; 15 archivos, 4 pasan y 11 fallan. Además hay un archivo que no puede recolectar pruebas por módulo ausente (incluido en los 11 fallidos). Fallos en rutas, sesión, pantallas pendientes, accesos demo, CSP y observación de credenciales auth en MSW. No se declara verde la fase 1 ni cobertura aprobada |
| `npm run build` | Pasa fuera del sandbox; JS principal 420.24 kB (112.52 kB gzip), CSS 91.32 kB (15.83 kB gzip). Compilar el demo no implica cumplir requisitos ni pasar TypeScript, que se ejecuta aparte |
| `npm audit --omit=dev --json` | 0 vulnerabilidades reportadas en dependencias de producción al ejecutar esta revisión. No cubre dependencias de desarrollo ni demuestra ausencia de vulnerabilidades propias |
| Impeccable `context --target frontend/src` | Ejecutado. No encontró PRODUCT.md ni DESIGN.md en sus ubicaciones convencionales; se leyó directamente el sistema de diseño real, `documentos/diseno/DESIGN.md`. No se creó ni reemplazó contexto de producto |
| Impeccable `detect --json frontend/src` | Un aviso `bounce-easing`, `HorizontalStatusLine.tsx:106`. Coincide con `animate-bounce-subtle`, clase propia sin definición; el patrón regex no prueba que exista una animación de rebote efectiva. No se cuenta como defecto visual demostrado |
| AgentShield 1.6.0, `scan --path .claude --format json` | A/100, 0 hallazgos, **0 archivos escaneados**. Resultado sin cobertura; no es aval de seguridad |
| AgentShield 1.6.0, `scan --path . --format json` | Reejecutado: B/80, 3 archivos escaneados y 12 avisos sobre defensas textuales ausentes en CLAUDE.md: 2 críticos, 3 altos, 6 medios y 1 bajo **según la herramienta**. Son avisos de configuración del agente, no 12 vulnerabilidades del producto ni los 270 hashes del escaneo histórico |
| Referencias OpenAPI | YAML parseado correctamente: versión 3.1.0, contrato 0.5.0-borrador, 34 `operationId` sin duplicados; las 252 referencias internas se resuelven al recorrer el árbol YAML. No equivale a validar toda la semántica con un validador OpenAPI 3.1 |
| Diagramas | Se contrastaron semánticamente los seis archivos. La validación de render del README es del 2026-10-10; no se declara un render nuevo |

Los archivos temporales de evidencia de herramientas están en `.tmp/auditoria-impeccable.json`, `.tmp/auditoria-agentshield.json`, `.tmp/auditoria-agentshield-raiz.json` y `.tmp/auditoria-npm.json`; el resumen útil y sus límites quedan aquí. No se ejecutaron autofixes, instalaciones ni publicación.

### 11.3 Clasificación vigente para migración

**Reutilizar** significa conservar la presentación o una primitiva y adaptar su interfaz; no copiar datos, permisos ni reglas del demo como implementación real. **Reescribir** significa sustituir la responsabilidad o composición principal, aunque se aproveche el diseño visual. **Descartar** significa retirarlo del runtime MVP, conservando solo referencias de prueba ficticias si son necesarias. Esta matriz precisa la sección 8 histórica; el número «14 componentes reutilizados» del PRD no debe usarse como criterio de aceptación.

| Archivo / grupo | Decisión | Qué conservar y qué debe cambiar |
| --- | --- | --- |
| `App.tsx` | Reescribir | Montaje de rutas, sesión y consultas; retirar estado de negocio en memoria, autoadmin, cambio de identidad y correos fabricados |
| `main.tsx`, `rutas.tsx`, `auth/SesionProvider.tsx` | Reescribir / completar | Un solo montaje real con router, proveedor de sesión y QueryClient. Hoy producción y el helper de pruebas montan árboles diferentes |
| `components/LoginScreen.tsx` | Reescribir | Estructura de formulario como referencia; autenticación `/auth/login`, errores 401/423, etiquetas y estados de carga. Descartar contraseña común, ingreso rápido y CO habilitado |
| `components/UsersManagementView.tsx` | Reescribir | Gestión API: crear, editar, activar/desactivar, varios roles y PATCH con `version`; resolver 409 recargando el usuario. Retirar `onSwitchUser` |
| Pantalla de roles y dashboards de fase 1 | Implementar, sin componente que reutilizar | `RolesManagementView.test.tsx` y `dashboards/DashboardRol.test.tsx` son pruebas, no pantallas. Los dashboards vacíos son la entrega de fase 1, no el seguimiento completo |
| `components/Header.tsx`, `Sidebar.tsx` | Reescribir composición | Aprovechar layout; identidad de sesión, enlaces reales, autorización de opciones, menú móvil accesible y país EC. Quitar simulador |
| `components/DashboardView.tsx` | Reescribir composición | Aprovechar jerarquía y tarjetas; bandeja API por roles, línea horizontal por pedido y estados vacíos reales. No importar pedidos demo |
| `components/OrdersListView.tsx` | Reutilizar listado; reescribir creación | Filtros y tabla como presentación; API paginada y formulario validado. El backend genera el número; no usar `pedidos.length + 1` |
| `components/OrderDetail.tsx` | Reutilizar presentación; reescribir coordinación | Composición de detalle y secciones; adaptar `PedidoDetalle`, acciones disponibles, Tabs/Dialog accesibles y selección de etapa sin estado obsoleto |
| `components/StagePanel.tsx` | Reescribir | Acciones obtenidas del flujo/servidor; distinguir A/B, exigir evidencias y bodega, devolución por error separada, adjuntos y reasignación. Descartar checklist no aprobado |
| `components/HorizontalStatusLine.tsx` | Reutilizar con ajustes | Línea horizontal requerida; adaptar `lineaDeTiempo`, estados y nombres del contrato, `aria-current="step"`, contraste y selección accesible |
| `components/OrderHistoryTab.tsx` | Reutilizar presentación | Lista de eventos y adjuntos desde API; timestamps del servidor y títulos accesibles. La inmutabilidad se garantiza en backend, no en este componente |
| `components/OrderScheduleTab.tsx` | Reutilizar presentación | Fechas/meta recibidas del backend, sin cálculo autoritativo local ni plazos pendientes presentados como definitivos |
| `components/OrderDocumentsTab.tsx` | Reutilizar listado; reescribir operaciones | Carga/descarga autorizada vía backend, tamaño real, progreso y estados PENDIENTE/COPIADO/FALLIDO; quitar botones de éxito simulado |
| `components/TrackingSearchView.tsx` | Reutilizar presentación; reescribir datos | Búsqueda API por número y línea horizontal; guardar ID o respuesta vigente, quitar pedido fijo preseleccionado y copia desactualizada |
| `components/WarehouseCatalogView.tsx` | Reutilizar listado de tarjetas; reescribir formulario | Persistencia API, estados de carga/error y diálogo accesible. Logística mantiene el catálogo; facultad del admin sigue pendiente |
| `components/GmailNotificationsView.tsx` | Reutilizar presentación | Registro API de enviados, fallidos e intentos; no confundir «leído» local con entrega real de correo |
| `components/AuditTrailView.tsx` | Reutilizar presentación | Auditoría API paginada y autorizada, sin reconstruir historia global desde arrays locales |
| `components/TimelineCalculatorView.tsx` | Reescribir | Conservar comparación cronograma/meta; fechas del backend. Retirar Slack, 12 días de Embarque y duraciones no confirmadas |
| `components/Toast.tsx` | Descartar implementación | Sustituir por `components/ui/sonner.tsx` integrado, con feedback accesible; evitar dos sistemas de avisos |
| `components/OrderProductsTab.tsx` | Descartar del MVP | El cliente sigue pedidos y deja el detalle de productos en archivos; cantidades recibidas e inventario no pertenecen al alcance aprobado |
| `components/HelpView.tsx` | Descartar copia del levantamiento | Reemplazar después por manual operativo verificado; hoy afirma propuestas y plazos no aprobados como hechos (C8) |
| `components/ui/{alert,badge,button,card,checkbox,dialog,dropdown-menu,field,input,label,select,separator,sonner,table}.tsx` | Reutilizar | Primitivas shadcn/Radix y tokens existentes; requieren composición correcta, nombres accesibles y traducción de «Close» en Dialog |
| `api/cliente.ts`, `api/esquemas.ts` | Reutilizar endureciendo | Base HTTP, errores y Zod; aclarar política de cookies, alinear validación de contrato y conectar sesión. No son autenticación completa |
| `types.ts` | Reescribir | El modelo de demo usa `email`, `usuarioLogin`, un `rol` y un `pais`; contrato usa `correo`, `usuario`, `roles`, `paises`, `version`. Evitar dos modelos paralelos de sesión |
| `utils/orderState.ts` | Descartar del runtime; referencia para reescritura backend | Ninguna transición, auditoría, urgencia ni supuesto Drive local es autoridad de negocio. Convertir errores conocidos en casos de dominio |
| `utils/permissions.ts` | Reescribir | Solo ayudas de presentación basadas en sesión/capacidades; autorización obligatoria en servidor. No conservar la unión cerrada de roles demo como catálogo |
| `utils/dateUtils.ts` | Reutilizar formateadores; reescribir/retirar cálculos | Formatear fechas del contrato; eliminar `SIMULATED_TODAY` y dejar plazos, urgencias y arrastre en backend |
| `data/initialData.ts`, `utils/translations.ts` | Descartar del runtime | Sin datos de empleados en bundle; etapas como referencia de migración, no configuración aprobada. Traducciones no usadas y sin requisito multidioma |
| `index.css`, `lib/utils.ts` | Reutilizar con ajustes | Tokens de DESIGN y `cn`; aplicar tokens en pantallas antiguas y ajustar reducción de movimiento (Q24) |
| `*.test.*`, `test/*`, `vite-env.d.ts` | Reutilizar / completar | Pruebas y MSW ficticios separados del runtime; corregir tipado, imports faltantes y montaje, sin tratar pruebas rojas como funcionalidad entregada |

### 11.4 Cobertura funcional actual frente a requisitos

| Requisito / decisión | Estado del frontend auditado | Evidencia y fase pertinente |
| --- | --- | --- |
| Login real, dashboard por rol, usuarios y roles (§4.2 #1–3; §4.7) | No integrado | `main`, `App`, sesión y rutas; Usuarios solo lista demo, Roles no tiene pantalla. Bloquea salida de fase 1 |
| Varios roles, cuenta activa, D3b y D10 aprobados por equipo | Parcial en contrato/esquemas, ausente en UI activa | `api/esquemas.ts` incluye `roles` y `version`; `types.ts` y App usan modelo antiguo; falta renovación y edición versionada |
| Número secuencial único (§4.2 #5) | Simulado | `OrdersListView.tsx:82–84` usa longitud local y año fijo. Formato DK sigue propuesto; fase 2 |
| Flujos A/B y devolución por error (§4.4–4.5) | Demo parcial con errores | B3/B4 reciben botones de otras etapas; B10 Bodega se etiqueta como check final de Incidencias (`StagePanel.tsx:365`); devolución simple sin urgencia ausente; fases 2, 4 y 6 |
| Urgencia 4/12/24 hasta A4/B5 (§4.2 #11) | Incorrecta | `orderState.ts:258` borra urgencia al primer avance; `:373–380` fija horas y hace iguales los vencimientos de Prioritario/Normal |
| Documentos obligatorios, bodega y Fin automático | Parcial | Fin automático existe en demo; `StagePanel.tsx:99` calcula faltantes sin bloquear. Función de avance local no garantiza evidencias/bodega |
| Drive y Gmail reales (§4.2 #15–16) | Simulados | `orderState.ts:645–646`, App y StagePanel crean enlaces/correos inventados; no hay operación real de esos servicios desde la UI. Fase 3 |
| Hora de Ecuador, reentrada, recálculo y arrastre | Parcial e incorrecto | Día simulado, reentrada calculada desde pedido, fecha fija de demora y calculadora heredada. Fases 2/4/5; jornada, feriados y tránsito no se deciden aquí |
| Línea horizontal por dashboard y búsqueda | Parcial | Se usa en detalle, no en todas las superficies exigidas; fase 5. No es bloqueo adicional de dashboards vacíos de fase 1 |
| Reasignación e historial de solo inserción | No implementados de forma real | Arrays locales y controles por rol no equivalen a persistencia ni autoridad; fase 2 |
| Ecuador primero, CO inactivo | Incumplido en demo activo | Selector CO en Login y alternancia en App; contrato/modelo indican CO inactivo en v1 |
| Solo web, sin seguimiento individual de producto | Web sí; extras fuera de alcance presentes | OrderProductsTab, pausar/cancelar, checklist y Slack deben retirarse o esperar aprobación, no convertirse en requisitos |

No se recalcula el «31 de 64 cumple» histórico: mezclaba coincidencia visual con simulación, y no es una métrica de implementación segura. Las brechas de fases 2–6 son trabajo futuro previsto; las de acceso y montaje impiden cerrar fase 1.

### 11.5 Seguridad vigente y estado de S1–S11

| ID histórico | Estado al 2026-10-13 | Evidencia / acción |
| --- | --- | --- |
| S1, crítico para producción | Sigue abierto | `initialData.ts:13`, `LoginScreen.tsx:33`: contraseña común pública y comprobación local. Eliminar la autenticación demo |
| S2, crítico para producción | Sigue abierto | `Header.tsx:102`, modal de App y `UsersManagementView.tsx:141`: cambio de identidad sin credenciales. Eliminar entradas de simulador |
| S3, crítico para producción | Sigue abierto | `App.tsx:49`: admin inicial. Arranque con recuperación de sesión real, nunca usuario fijo |
| S4, crítico para producción | Sigue abierto en el motor de pedidos frontend | `permissions.ts`, `orderState.ts`: permisos, eventos y acciones locales. Trasladarlos al backend; no se demuestra un bypass del backend de usuarios con esto |
| S5, alto | Sigue presente en código de demo | Nombres, correos y teléfonos en `initialData.ts` y destinatarios en App. Retirar datos personales del bundle; la privacidad actual del remoto y si los datos son reales no se revalidaron. La nota de repo público es histórica |
| S6, alto | Sigue abierto | Día/hora, correos y Drive ficticios mostrados como éxito. Usar estados/respuestas oficiales; nunca afirmar subida o envío antes de confirmación |
| S7, medio | Resuelto en package.json actual para las dependencias enumeradas | Ya no figuran `@google/genai`, `express`, `dotenv`, `motion`, `tsx`, `@types/express` ni `autoprefixer`. Mantener depuración y auditoría; no repetir el hallazgo viejo |
| S8, medio | Configuración antigua retirada de los archivos revisados | No hay Gemini inyectado en montaje/configuración actual. `VITE_API_URL` es configuración pública de destino, nunca debe contener secretos. No se certifica todo el historial Git |
| S9, medio | Sigue abierto en la simulación; carga real ausente | No hay selector ni carga válida de archivo. No validar contenido de negocio no exime de tamaño, nombre, formato aprobado y autorización técnica del servidor |
| S10, bajo | Sigue como configuración de desarrollo | `package.json`, `vite --host=0.0.0.0`; no presentarlo como servidor apto para producción |
| S11, antes bajo | Parcial: nginx tiene cabeceras, falta CSP | `nginx.conf` tiene nosniff, DENY y no-referrer para documento, sin Content-Security-Policy; 9 expectativas de CSP fallan. Endurecimiento M4 del paso 1.8 pendiente, no posponerlo automáticamente a fase 8 |

**S12 — Informativo, aclaración de cookies y límite de la prueba.** `api/cliente.ts:57–62` usa `credentials: 'same-origin'` cuando `credenciales` es falso: permite cookies del mismo origen; no significa omitir todas las cookies. `cliente.test.ts:74` espera precisamente `same-origin`, por lo que no hay discrepancia estática en esa segunda llamada. La cookie de renovación aprobada tiene Path `/api/v1/auth`, lo que acota su envío; esto no demuestra CSRF ni envío de esa cookie a `/usuarios`, ni obliga a cambiar a `omit`. Aclarar el comentario «cookies solo cuando se pide». En la ejecución nueva sí falla la primera aserción (`cliente.test.ts:73`): MSW observa `same-origin` para `/auth/renovar` aunque el cliente pasa `include`. Investigar interceptación/normalización de Request y verificar la petición real antes de atribuirlo a un fallo del navegador o del servidor; no se probó envío real de cookies.

**S13 — Configuración de agentes, separado del producto.** Los 12 avisos de AgentShield en CLAUDE.md piden defensas explícitas de instrucciones, filtrado de datos, rol, inyección indirecta, contenido dañino, salidas, idiomas, Unicode, contexto, ingeniería social, validación y abuso. No detectaron aquí secretos, comandos de exfiltración ni permisos peligrosos concretos. Revisar su utilidad en contexto; no copiar severidades «crítico» a la seguridad del frontend ni agregar prohibiciones genéricas para elevar una nota.

Fortalezas comprobadas: no se encontró `dangerouslySetInnerHTML`, `eval` ni almacenamiento de tokens en local/sessionStorage en el código revisado; fuentes locales y proxy `/api`; cliente API centraliza errores y permite validar respuestas; `esquemaUsuario.version` refleja D10. La ausencia de tokens almacenados también se explica por la sesión aún sin implementar: no demuestra que la futura renovación sea segura. No se inspeccionó ni se imprimió `.env` con secretos.

### 11.6 Calidad React, UX y sistema de diseño

Los Q1–Q19 históricos siguen como lista de migración, con estas actualizaciones: Q7 tiene `strict` resuelto pero ESLint pendiente; Q12 ya tiene una regla de movimiento reducido, aunque su implementación requiere revisar feedback; Q8 tiene tokens nuevos, pero las pantallas del demo siguen usando amarillo y colores literales. No se reutilizan automáticamente los conteos antiguos de tamaños, colores ni componentes.

| ID | Prioridad | Evidencia actual | Impacto y corrección concreta |
| --- | --- | --- | --- |
| Q1/Q2/Q3 | P1 | `StagePanel.tsx:475,189,455,492`; `DashboardView.tsx:171`; `LoginScreen.tsx:132,155` | Modales sin semántica/foco/Escape, divs clicables y campos sin nombre accesible. Impiden operar con teclado o lector; integrar Dialog, Button/Checkbox y labels asociados |
| Q4/Q8 | P1 / P2 | `OrdersListView.tsx:291–292`, `HorizontalStatusLine.tsx:104`, App y pantallas con `#FFD100` | Semáforos/texto no adoptan colores accesibles ni identidad de DESIGN. Usar tokens apropiados de texto/fondo; no reutilizar contraste del demo como contraste de los tokens nuevos |
| Q6 | P1 | `OrderDetail.tsx:70,303`; `StagePanel.tsx:68–86`; `TrackingSearchView.tsx:17` | El resultado de búsqueda conserva una copia y los borradores/bodega pueden cruzar etapas. Derivar datos vigentes y definir identidad/reset por pedido-etapa. Conservar seleccionada una etapa terminada puede servir para consultarla: seguir automáticamente la actual no se considera requisito aprobado |
| Q7 | P1 | `package.json:12`, `tsconfig.json` | Chequeo de tipos no verifica hooks ni JSX accesible. Añadir ESLint react-hooks/jsx-a11y con alcance y reglas definidos; no renombrar tsc como lint suficiente |
| Q9/Q10/Q11/Q13 | P2 | Sidebar/Header; `Toast.tsx:21`; pestañas y títulos de OrderDetail | Estado de menú/pestañas/etapa y feedback no tienen semántica adecuada. Sheet/NavLink/Tabs, nombres de iconos y región de anuncios |
| Q14/Q15 | P2 | App y StagePanel concentran montaje, modales y negocio | Migración difícil y errores repetidos. Separar shell, consultas, formularios y acciones; no memoizar todo sin medición |
| Q20 | P0 para cierre de fase 1 | `main.tsx`, `rutas.tsx`, `test/renderizarApp.tsx:11` | El helper afirma montar como main, pero monta router/providers que main no usa. Unificar composición; tests rojos de rutas no validan el shell demo servido |
| Q21 | P1 para validación de la entrega | `SesionProvider.test.tsx:113`, `coordinadorSesion.test.ts:11,59,60,137` | Cinco diagnósticos TypeScript impiden avalar el árbol. Completar módulo y tipar MSW/parámetros, preservando intención de pruebas previas |
| Q22 | P2 | `api/esquemas.ts:7–13,41–46`, `api/cliente.ts:70` | UUID/correo/códigos/países demasiado permisivos y campos de Problema opcionales pese al contrato. Validar CodigoRol por patrón `^[a-z_]{3,40}$`, nunca enum de siete roles; CodigoPais sí es EC/CO, y roles exige unicidad. La validación es opcional en llamar; el cast `as T` no valida |
| Q23 | P2 | `App.tsx:68`, `TimelineCalculatorView.tsx:79,86` | Temporizadores sin limpieza y dos mecanismos potenciales de toast. Centralizar feedback y cancelar temporizadores al desmontar |
| Q24 | P2 | `HorizontalStatusLine.tsx:40`, `index.css:142–150` | `scrollIntoView({behavior:'smooth'})` fuerza movimiento desde JavaScript; `scroll-behavior:auto` en CSS no anula esa elección explícita. Consultar prefers-reduced-motion y usar auto cuando corresponda. Revisar el feedback de la regla global de `0.01ms` es una recomendación técnica; sin prueba visual no se afirma que ya perjudique la comprensión |

**Integridad de implementación: falla para el producto entregable.** Los tokens y componentes nuevos coexisten con una aplicación demo que simula operaciones reales y copia propuestas como hechos. No hay evidencia de rendimiento de red, Web Vitals ni accesibilidad en navegador.

Puntuación Impeccable **orientativa, estática** (no certificación WCAG):

| Dimensión | /4 | Evidencia |
| --- | --- | --- |
| Accesibilidad | 1 | Persisten diálogos manuales, divs clicables y etiquetas no asociadas; primitivas accesibles aún sin integrar |
| Rendimiento | 3 | Build compacto para una SPA; sin medición runtime ni red. No se justifica virtualización o memoización sin datos |
| Theming / tokens | 2 | Sistema de diseño y CSS existen, pero vistas antiguas usan otra paleta. No se exige dark mode, que no es requisito |
| Responsive | 2 | Breakpoints y scroll de tablas presentes; menú/foco y objetivos táctiles requieren prueba real |
| Integridad | 1 | Datos y acciones demo dominan la aplicación montada |
| **Total** | **9/20** | **Pobre; estimación estática actual, independiente del 9/20 histórico** |

Aspectos a conservar: separación visual de pestañas; línea horizontal; semáforo con texto/icono además de color; tablas con scroll; primitivas UI y fuentes locales. No se detectaron hooks condicionales ni mutación directa de estado React; la dependencia del efecto de HorizontalStatusLine es correcta. Esto no sustituye ESLint ni validación de interacción.

### 11.7 Consistencia entre requisitos, PRD, ADR, OpenAPI y tablas

**Resultado: consistente en el recorrido de negocio principal, no completamente consistente en estados de decisión ni diseño de autorización.** Cada fila indica una discrepancia o una confirmación; ninguna decide por el cliente.

| ID | Prioridad / estado | Fuentes y contraste | Acción documental recomendada |
| --- | --- | --- | --- |
| C1 | P1, desactualización | PRD `Open Questions`, línea 61: «Sin valor aprobado; bloquea» varios roles. Requisitos 4.7, arquitectura 4.1, OpenAPI Usuario.roles, modelo usuario_rol y ER ya registran decisión de Richard del 2026-10-12 | Retirar bloqueo técnico del PRD y conservar origen **equipo**, no respuesta del cliente |
| C2 | P2, estados antiguos | `arquitectura.md:62,130` mantiene transacciones y cookie como [P]; ADR-0012 aprobó transacciones (no outbox) y ADR-0002 aprobó D3b. `modelo-datos.md` §7 aún dice que ADR-0012 podría no aprobarse; secuencia 05 marca apertura [P] | Actualizar etiquetas solo para la porción ya aprobada. Outbox sigue proposed |
| C3 | P1, propuesta presentada como definitiva | ADR-0004 sigue proposed; PRD Proposed Solution/Technical Approach/fase 2 y algunas tablas describen motor de datos/versionado sin marca local. Requisitos 4.6 también lo incluyen dentro del diseño | Mantener distinción entre contenido aprobado de A/B y mecanismo técnico propuesto; no elevar ADR-0004 a accepted |
| C4 | P2, inventario y cobertura ER | Modelo anuncia 21 tablas. C4 contenedores dice 20; README diagramas dice 20. ER contiene usuario_rol, pero no configuracion_plazos; tampoco relación usuario→adjunto (subido_por) ni usuario→pedido para demora_reportada_por | Corregir conteo y explicitar ER resumido o incorporar relaciones faltantes. Tablas feriado y secuencia_pedido siguen condicionales; 21 es inventario de diseño, no 21 tablas aprobadas/implementadas |
| C5 | P1, autorización ambigua | Requisitos invariante: rol responsable o admin. Arquitectura :176 y secuencia 05 validan asignado o admin; OpenAPI :53 permite asignado **o cualquiera con el rol** **o admin**. Modelo permite asignado_a de cualquier usuario | Definir si la asignación restringe al resto del rol y si puede ser de otro rol. Reasignar está aprobado, esa política concreta no; no ampliar permisos en UI por inferencia |
| C6 | P2, semántica de fin | Requisitos: una sola etapa activa y Fin automático. OpenAPI PedidoDetalle dice etapa actual ACTUAL; modelo prevé índice de **a lo sumo una** ACTUAL; diagramas terminan en Fin | Precisar representación final: ninguna ACTUAL o Fin ACTUAL, valores de etapaActual y timestamps. Un índice parcial no garantiza exactamente una; completar invariantes del dominio sin inventar comportamiento aprobado |
| C7 | P1, decisiones pendientes embebidas | Nueva fecha de demora requerida en AccionReportarDemora; reasignación solo actual; etapas versionadas; formatos de archivo, destinatario responsable, meta 90, 31 fabricación, 45 tránsito y urgencias hábiles son diseños/valores provisionales | Conservar x-pendiente/[P]/TBD al copiar a planes, UI, semillas y tests. Un campo required en borrador no cierra la pregunta de negocio |
| C8 | P1, propuestas convertidas en textos de ayuda | HelpView :45 fija Incidencias 24 h, :46 consulta de asistente China, :60–62 urgencias hábiles, :94 correo admin+responsable, :107 formato DK establecido | Retirar la copia del runtime; manual futuro debe decir «pendiente/provisional» donde corresponda. Incidencias no tiene plazo aprobado |
| C9 | P2, cronología y estado de implementación | Requisitos §3 todavía afirma repo vacío; PRD conserva notas de ADR pendiente y 14 componentes; auditoría vieja dice sin strict y sin cabeceras; CLAUDE describe frontend demo sin llamadas HTTP | Actualizar textos históricos con fecha/alcance. Hay un cliente HTTP aún no conectado, strict y nginx; ni pruebas nuevas ni build demuestran paso 1.8 completo |
| C10 | P2, límite del diseño de concurrencia | OpenAPI etapaEsperada es propuesta; compara número de etapa, no reentrada/version. Modelo pedido.version es [P]. Recorrido A3→A2→A3 puede volver al mismo número y permitir un comando viejo | Revisar estrategia de concurrencia de pedidos antes de fase 2. D10 ya aprobado aplica a usuarios; no extenderlo por cuenta propia a pedidos o roles |
| C11 | Consistente con límite | D3b, 5 fallos/15 min, JWT 15 min, renovación 8 h y D10 usuario.version concuerdan en requisitos 4.7, ADR-0002, OpenAPI y tablas de usuarios | Mantener decisiones del equipo. El frontend activo aún no las implementa; roles.version sigue pendiente |
| C12 | Consistente en negocio | Tablas A/B de requisitos, arquitectura y OpenAPI: B2→B1 al rechazar, B4/B5→B3; Fin A11/B12 automático; evidencias de Embarque y Bodega | Reutilizar como trazabilidad de casos, no copiar los errores de StagePanel/orderState |
| C13 | Consistente con pendiente | Ecuador primero, Colombia y Claude después; correo obligatorio al admin; tamaño máximo 104857600 bytes; no validación del contenido de negocio | No activar CO, destinatario extra, IA o formatos por la presencia de campos/servicios en diseño |
| C14 | P2, documentación de casos secundarios | Arquitectura 4.2 ELEGIR_BODEGA muestra destino A10/B11 junto a Aprobar; OpenAPI y estados muestran elección en la misma etapa | Aclarar por acción: elegir mantiene A9/B10; aprobar avanza. No interpretar la tabla comprimida como avance al elegir |

Tablas revisadas: `pais`, `rol`, `rol_permiso`, `usuario`, `usuario_rol`, `usuario_pais`, `token_renovacion`, `definicion_flujo`, `definicion_etapa`, `transicion_permitida`, `documento_requerido`, `secuencia_pedido`, `pedido`, `etapa_pedido`, `evento_pedido`, `evento_adjunto`, `adjunto`, `notificacion`, `bodega`, `configuracion_plazos`, `feriado`. La fase 1 corresponde a las primeras siete; las restantes describen fases futuras, con las condiciones documentadas. La lista de permisos de rol sigue pendiente: tabla vacía, UI sin pantalla y catálogo no equivalen a permisos configurables implementados.

El vínculo archivo–evento tiene correspondencia en requisito, `evento_adjunto` y `Adjunto.eventoId`; falta en el motor demo. Historial inmutable, país/flujo estables y fechas de servidor están representados en diseño, pero no se verificaron aquí sus futuras migraciones de pedidos ni garantías transaccionales.

### 11.8 Revisión individual de los seis diagramas

| Diagrama | Resultado del contraste semántico |
| --- | --- |
| `01-c4-contexto.mmd` | Actores, Drive en servidor, alertas al admin y Claude fase 7 coinciden. Marca consulta de asistente China y correo adicional como pendientes, canal Gmail/SMTP como propuesta. El rol consulta añadido por equipo no aparece: aclarar que muestra actores de negocio, no catálogo exhaustivo de roles |
| `02-c4-contenedores.mmd` | React/backend/PostgreSQL, proxy de confianza y jobs del mismo proceso son coherentes. Corregir «20 tablas» y [P] en monolito hexagonal (ADR-0011 ya accepted); «6 roles» no refleja los 7 iniciales. Conservar outbox, canal correo y tecnología WebSocket como propuestas; no exigir STOMP en fase 1 |
| `03-estados-flujo-a.mmd` | Las 14 transiciones de §4.4 coinciden; elección de bodega/demora permanecen en etapa y Fin es automático. Plazos de Fabricación/Incidencias, fecha de demora, tránsito y semántica de urgencia están marcados. No interpreta checklist, pausar o cancelar como estados aprobados |
| `04-estados-flujo-b.mmd` | B2 rechazo→B1, B4/B5 devolución→B3 y B12 automático coinciden. B6–B11 son equivalentes A5–A10; 24 h B2 aparece en §4.5. Mantiene pendiente permiso de admin para crear B. Precisar por referencia que las notas de urgencia/tránsito de A también aplican a B |
| `05-secuencia-transicion.mmd` | Rechazo A3→A2, observación, 409 ETAPA_CAMBIO, historial y correo son coherentes con REST. Apertura de transacción ya aprobada no debe llevar [P]; revisar autorización C5. Mantener escritura outbox y transporte correo como [P], y explicitar que respuesta 200 representa commit confirmado, no envío de correo confirmado |
| `06-entidad-relacion.mmd` | Relación N:M usuario_rol coincide con decisión de equipo; eventos/adjuntos, versiones de flujo y tokens están representados. Es resumido: omite configuracion_plazos y relaciones citadas en C4; versión administrativa usuario de D10 no se dibuja. No confundir columnas omitidas con ausencia en tablas. Sus comentarios conservan las marcas de secuencia/feriados pendientes |

### 11.9 Decisiones que continúan abiertas y orden de trabajo

Siguen pendientes del cliente: formato de pedido, meta ajustada, feriados/jornada aplicable, horas hábiles o corridas de urgencias, rechazo durante urgencia, tránsito de 45 días, plazos Fabricación/Incidencias y documentos concretos de Incidencias, origen de nueva fecha de demora, correo al responsable, formatos admitidos, visibilidad asistente China y consulta, facultad admin para crear B/mantener bodegas, reasignación a otro rol, recuperación de contraseña, acceso directo Drive, marca, Colombia/IA y producción. El límite de 100 MB y el correo al admin sí están confirmados. No usar «sin validar documentos» para eliminar controles técnicos de carga.

Siguen como propuestas técnicas: ADR-0004 (motor por datos), outbox de ADR-0012, canal de correo de ADR-0013 y tecnología de tiempo real de ADR-0014; también ArchUnit en ADR-0011, paquete base en ADR-0010 y subcarpetas Drive por año/etapa en ADR-0003. Sí están aprobados por el equipo monorepo/Gradle, hexagonal, transacciones fuera del núcleo, datos reales fuera de Git, Spring Boot 4.1, varios roles y las políticas de sesión/D10 registradas. La implementación existente de otras propuestas técnicas no constituye por sí sola un cambio de estado de ADR. Colombia y Claude están confirmados para después; sus procesos y caso de uso siguen sin definir, no su exclusión de v1.

1. **Cerrar fase 1:** integrar montaje/sesión/router, retirar autoadmin y cambio de identidad, implementar Usuarios/Roles/dashboards vacíos, D3b y D10, y corregir tipos/pruebas. Resolver CSP del paso 1.8 y coordinación de renovación antes de declararlo terminado.
2. **Sin decidir negocio nuevo:** sincronizar estados ya aprobados en PRD/arquitectura/diagramas y etiquetar propuestas/pendientes. Resolver C5 con una decisión explícita antes del motor de acciones.
3. **Fases 2–6:** migrar listas/detalles al contrato, reglas al servidor y archivos/correos reales; priorizar B3/B4, urgencia, evidencias y reentrada. Las propuestas no aprobadas conservan alternativas y trazabilidad.
4. **Calidad de interfaces:** `$impeccable harden` para formularios/errores/estado; `$impeccable adapt` para menú móvil, teclado, texto y tacto; `$impeccable colorize` para adoptar tokens; `$impeccable polish` como última pasada. Son recomendaciones, no comandos ejecutados ni nuevas funciones aprobadas.

**Límites:** no hubo pruebas visuales en navegador, capturas desktop/móvil, lector de pantalla, touch real, red desde China, E2E contra backend, revisión exhaustiva de seguridad backend ni render nuevo de Mermaid. No se certifican WCAG, rendimiento, historial Git limpio, privacidad del remoto, ausencia total de secretos o disponibilidad de Drive/Gmail. Los resultados de tests corresponden al montaje actual de prueba, que difiere del servido; mantener Q20 visible evita una falsa aprobación.

### 11.10 Revalidación tras instalar las skills en Codex

Se aplicaron las skills ya instaladas en `C:/Users/Richard/.codex/skills/{ecc-react-review,impeccable,ecc-security-scan}`. React y TypeScript se revalidaron en revisiones separadas de lectura, conforme al flujo de react-review. El launcher context de Impeccable ya se había ejecutado en esta sesión; no se repitió. Su detector se volvió a ejecutar y mantiene únicamente el aviso de rebote no demostrado visualmente.

**Ejecución nueva:** tipos exit 1 (los mismos cinco diagnósticos), Vitest exit 1 (48 pasan, 77 fallan), build exit 0 (mismos tamaños del bundle), AgentShield B/80 con 12 avisos y 3 archivos escaneados. El resumen JSON de Vitest cuenta 29 suites anidadas; no debe confundirse con los 15 archivos de pruebas. El cliente API pasa 6 de sus 7 pruebas; la fallida es la primera observación de credenciales auth descrita en S12. La consulta npm audit de 11.2 corresponde a la ejecución anterior de esta misma sesión; no se hizo otra consulta de vulnerabilidades en esta revalidación.

Evidencia nueva: `.tmp/reauditoria-tipos.log`, `.tmp/reauditoria-vitest.json`, `.tmp/reauditoria-vitest.log`, `.tmp/reauditoria-build.log`, `.tmp/reauditoria-agentshield.json`, `.tmp/reauditoria-impeccable.json`. Se contrastaron de nuevo PRD, índice/estados ADR, OpenAPI, inventario de tablas y seis diagramas; C1–C14 y la revisión individual de 11.8 siguen vigentes. Se corrigieron referencias de montaje/cast, la descripción de tarjetas/lista, la precisión de Q6/Q24 y la interpretación de S12; se añadió el error de etiqueta en B10. El dictamen y la clasificación principal no cambian.

**Precisión de S12 tras inspeccionar el harness instalado:** `frontend/node_modules/@mswjs/interceptors/src/interceptors/http/http-parser.ts:81` reconstruye la petición con `credentials: 'same-origin'` fijo. Esta normalización explica por qué observar `request.credentials` en el handler puede perder el `include` original. Verificar las opciones originales de fetch o el comportamiento en navegador; el fallo de esa aserción no se atribuye al envío real de cookies del producto.

Esta revalidación actualiza únicamente `auditoria-demo.md`, sin corregir el frontend, editar requisitos/ADR/contrato ni tocar cambios locales ajenos. Ninguna propuesta pendiente se transformó en requisito aprobado.

## 12. Revisión documental y del alcance de implementación

**Fecha:** 2026-10-13. **Skills:** `code-review` y `ecc-quality-gate`. **Dictamen documental: requiere correcciones antes de considerar las fuentes sincronizadas.** El recorrido de negocio coincide en sus transiciones principales; persisten diferencias de estado, autorización y alcance. Este dictamen no es una aprobación del cliente ni una aprobación de implementación.

Esta pasada revisa documentos y el alcance previsto. No añade funcionalidad ni ejecuta TypeScript, suites, cobertura, build, Docker o pruebas de aceptación. Los resultados de ejecución de las secciones 11.2 y 11.10 son antecedentes de otra revisión y no son puertas de salida de esta revisión documental. El árbol contiene trabajo de fase 1 y cambios locales previos: no se pretende volverlo una implementación de fase 0 ni revisar esos cambios de código como parte de este encargo.

### 12.1 Diferencias que deben resolverse en las fuentes

Severidad referida a claridad y riesgo de implementar un alcance incorrecto, no a vulnerabilidades de ejecución. Las filas D01–D09 precisan C1–C14; D10–D12 incorporan el contraste con el plan de fase 1.

| ID | Severidad | Diferencia y evidencia | Corrección documental necesaria |
| --- | --- | --- | --- |
| D01 | Alta | PRD `Open Questions` dice «Sin valor aprobado; bloquea la relación usuario–rol» (`.claude/PRPs/prds/decokasa.prd.md:61`). Requisitos 4.7 y plan D4 registran varios roles aprobados por Richard como equipo | Retirar el bloqueo técnico del PRD; conservar explícitamente que no es respuesta del cliente y que una respuesta futura puede cambiarlo |
| D02 | Media | Arquitectura mantiene transacciones [P] (`arquitectura.md:62`), y cookie de renovación [P] (`:130`). Modelo §7 todavía condiciona R2 a aprobar ADR-0012 (`modelo-datos.md:426`); secuencia 05 también marca apertura de transacción [P] | Sincronizar con ADR-0002 D3b y ADR-0012 accepted **solo en transacciones**. No aprobar outbox por arrastre |
| D03 | Alta | ADR-0004 sigue proposed, pero PRD presenta motor por datos/versionado como solución y fase prevista sin marca local | Mantener el objetivo de A/B aprobado separado de su mecanismo propuesto. Registrar decisión técnica antes de tratar ADR-0004 como compromiso definitivo |
| D04 | Media | Modelo anuncia 21 tablas (`modelo-datos.md:5`); C4 contenedores anuncia 20 y seis roles (`02-c4-contenedores.mmd:5,11`); README del ER anuncia 20. ER no muestra configuracion_plazos | Actualizar inventario y declarar diagrama resumido o completarlo. Hay siete roles iniciales por decisión del equipo; tablas propuestas no cuentan como aprobadas/implementadas |
| D05 | Alta | Requisitos invariante: rol responsable o admin. Arquitectura :176 y secuencia 05 validan asignado o admin; OpenAPI :53 admite asignado o cualquier integrante del rol o admin | Decidir si asignar excluye al resto del rol y si se permite reasignar a otro rol. No extender facultades del cliente por interpretación del contrato |
| D06 | Media | Arquitectura :155 agrupa «Elegir bodega / Aprobar» con destino A10/B11; contrato y estados distinguen elegir (se queda) y aprobar (avanza) | Separar las dos acciones y sus requisitos en la tabla; la selección por sí sola no finaliza Bodega |
| D07 | Media | Un índice único parcial en etapa_pedido garantiza a lo sumo una ACTUAL, no exactamente una. Requisitos hablan de una activa y contrato describe ACTUAL; Fin es automático en diagramas | Especificar representación de pedido finalizado y comprobar la invariancia en diseño del dominio, sin inventar una respuesta del cliente |
| D08 | Alta | HelpView fija 24 h de Incidencias, consulta de asistente China, urgencias hábiles, correo al responsable y formato DK como hechos; fuentes los mantienen TBD/provisionales | No usar esta copia de ayuda como requisito ni evidencia de aprobación. Mantener etiquetas pendientes al redactar el manual operativo |
| D09 | Media | Requisitos §3 dice que el repositorio está vacío; PRD conserva notas de ADR pendiente y 14 componentes reutilizados. El plan :235 dice «Frontend: no hay pruebas todavía» | Marcar antecedentes por fecha y separar situación actual, plan y decisiones. Los conteos de componentes no son criterios de aceptación |
| D10 | Alta | Plan :43 y :553 propone cambiar contraseña al editar; `SolicitudActualizarUsuario` (`openapi.yaml:1529`) no contiene contraseña. Recuperación sigue TBD; ADR-0002 no incorpora una función de cambio | Excluir esa propuesta del compromiso actual de fase 1 o decidirla y actualizar fuente/contrato antes de implementarla. No crear el campo unilateralmente |
| D11 | Alta | El plan asigna M4/CSP al paso 1.8 (`:545`), pero su tabla de riesgos la pospone a fase 8 (`:744`) | Mantener una única ubicación de entrega: el endurecimiento M4 ya registrado para 1.8; fase 8 verifica producción. No reinterpretar esto como requisito nuevo del cliente |
| D12 | Media | El plan resumen :5 afirma «ninguna credencial ni permiso vive en el navegador»; D3b del mismo plan :52 exige token de acceso en memoria y el contrato expone permisos para presentación | Decir que contraseñas/secretos no se persisten ni la autorización se decide solo en navegador. El token de acceso en memoria y permisos de UI son parte del diseño aprobado por equipo |

También debe precisarse C10: `etapaEsperada` es propuesta y un número de etapa no distingue reentradas A3→A2→A3. D10 usuario.version no autoriza por sí mismo versionar pedidos, bodegas o roles. Es una decisión técnica a resolver, no una aprobación faltante atribuida al cliente.

### 12.2 Decisiones pendientes y evidencia de origen

**Del cliente / negocio**, sin respuesta concluyente registrada:

| Grupo | Pendientes | Fuente para resolverlos |
| --- | --- | --- |
| Numeración y calendario | Formato DK, meta ajustada, feriados, tránsito hábil/corrido, urgencias hábiles/corridas y rechazo durante urgencia | `requerimientos.md:553`, `arquitectura.md:348`, ADR-0006 y `modelo-datos.md:379` |
| Etapas y documentos | Plazos Fabricación/Incidencias, cuáles son todos los documentos de Incidencias, fecha de demora escrita por David o calculada, formatos aceptados | Requisitos 4.4/4.5 y sección 5; arquitectura 8.1; ADR-0003/0006 |
| Accesos y facultades | Qué ve asistente China y consulta; admin crea B/mantiene bodegas; reasignar a otro rol; acceso directo Drive; recuperación de contraseña | Arquitectura 8.1 y requisitos 4.3/4.7/sección 5 |
| Alertas | Si recibe correo el responsable además del admin | ADR-0005: correo al admin confirmado; adicional sigue pendiente |
| Alcance futuro y operación | Proceso de Colombia, caso de uso Claude, marca visible, alojamiento, respaldos/operación y privacidad del repositorio | Requisitos sección 5, PRD Open Questions, DESIGN §1, ADR-0009/0015. «Después de v1» ya está registrado; sus definiciones permanecen abiertas |

**Del equipo / diseño**, sin aprobación suficiente en el ADR correspondiente:

- ADR-0004: mecanismo de flujos por datos/versionados.
- ADR-0012: outbox; no reabrir la parte de transacciones ya accepted.
- ADR-0013: Gmail API o SMTP y mecanismo de delegación.
- ADR-0014: WebSocket/STOMP o consultas periódicas. Sí está aprobada la exclusión antes de fase 5.
- ADR-0010/0011/0003: paquete base, ArchUnit y subcarpetas Drive por año/etapa.
- D10: versión de roles pendiente explícita; estrategia de concurrencia de pedidos no se decide por analogía.
- Plan T15: propuesta de cambiar contraseña; permiso/asignación por etapa y representación final requieren aclaración de diseño y, donde amplíen facultades, respuesta de negocio.

**Ya registradas como decisiones del equipo**, no del cliente: varios roles, siete roles iniciales, datos reales fuera de Git, monorepo/Gradle, hexagonal, transacciones externas, Spring Boot 4.1, políticas de sesión D2/D3/D3b y versión de usuario D10. Evidencia: `requerimientos.md:455`, plan D1–D10 y ADR-0002/0010/0011/0012/0015/0016. Este informe cita el registro; no transforma esa evidencia en aceptación del cliente.

### 12.3 Alcance de implementación que puede mantenerse

- **Fase documental actual:** auditoría, trazabilidad de requisitos, PRD, arquitectura, ADR, OpenAPI, tablas, seis diagramas y sistema de diseño. Su validación no requiere funcionalidad nueva ni suites del frontend/backend.
- **Fase 1 prevista:** base técnica, login/renovación/cierre, usuarios y roles, Ecuador activo, navegación protegida y dashboards vacíos según roles. El plan D6 exige mantener las pantallas demo de pedidos sin ruta hasta su fase; tenerlas en disco no las convierte en alcance funcional de acceso.
- **Fases 2–6:** pedidos/motor A1–A4, Drive/Gmail, etapas restantes de A, seguimiento/plazos y B. Los mecanismos propuestos conservan su condición antes de fijarlos en implementación.
- **Fase 7:** Colombia e IA solo con sus definiciones; no adelantar procesos, datos ni envío de documentos. **Fase 8:** operación de producción, según decisiones pendientes y fases previas.
- No ampliar v1 a producto por producto/inventario, Slack, pausar/cancelar, checklist, recuperación/cambio de contraseña o nuevas facultades del admin por existir en el demo o en una propuesta. El borrador de contrato permite prepararlas, pero no aprobarlas por inferencia.

### 12.4 Validación documental y puerta de calidad

Validación estructural realizada sobre 26 Markdown de requisitos, PRD, plan, arquitectura, modelo, auditoría, manual, ADR/plantilla/índice y README de diagramas: **sin enlaces locales Markdown a archivos inexistentes y sin número impar de fences de código**. No se validaron enlaces web ni todos los destinos por ancla. OpenAPI parsea como YAML: **34 operaciones, 252 referencias internas resueltas, ningún operationId duplicado**. Hay exactamente seis `.mmd`, contrastados semánticamente en 11.8; no se ejecutó render de Mermaid en esta pasada. Esto no equivale a validar toda la especificación OpenAPI con un validador 3.1.

`ecc-quality-gate` se aplicó en modo comprobación, sin autofix, sobre este informe. Su resolver no encuentra Prettier/Biome configurado para `documentos/`: el hook omite el formateador. **Estado del gate de formato: omitido por herramienta no configurada, no PASS de formato.** El hook comprueba formato de un archivo, no coherencia de requisitos, pruebas ni aprobación de alcance; su exit 0 no permite declarar esos resultados. La revisión semántica y `git diff --check` se registran por separado.

Evidencia de estructura: `.tmp/validacion-documental.json`. **Resultado:** estructura documental comprobada, coherencia pendiente de D01–D12, sin aprobación de cliente añadida ni cambios al contrato, plan, requisitos o código. La entrega es esta lista de diferencias y decisiones; no declara completada una fase de implementación.

# Auditoría del demo de AI Studio contra los requerimientos

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

1. **Ahora, sin código:** confirmar con el cliente si los correos de S5 son reales y que el repositorio de GitHub es privado.
2. **Fase 1:** S1, S2, S3, S7 y S8, junto con el login real y la limpieza de `package.json`.
3. **Fase 2:** S4, con permisos y eventos validados en el servidor.
4. **Fase 3:** S9 y la parte de S6 que corresponde a la subida de archivos.
5. **Fase 8:** S10 y S11, más la auditoría final del paso 8.5.

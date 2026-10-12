# Sistema de diseño — Decokasa

**Fecha:** 2026-10-12 · **Fase 0, paso 0.11** · **Base técnica:** shadcn/ui sobre Tailwind CSS 4 (ADR-0001).
**Fuentes:** logos entregados por el cliente en `documentos/logos/` (`logo1.jpeg` a `logo4.jpeg`), `documentos/auditoria-demo.md` (secciones 7 y 10), `documentos/requerimientos.md` y `documentos/api/openapi.yaml`.

El sistema usa los colores de los logos del cliente: **morado** como color principal, **naranja** como acento y **blanco** como fondo. La interfaz es una herramienta de trabajo diario: tablas, bandejas y la línea de tiempo de cada pedido. Por eso prima la legibilidad y la accesibilidad sobre la decoración.

**Marcas:**
- Sin marca: viene de los logos del cliente, de un requisito o de un ADR accepted.
- **[P]**: propuesta del equipo, no aprobada.
- **⏸**: pendiente de confirmar con el cliente.

## 1. Identidad y nombres ⏸

Los cuatro logos dicen **"LATINOAMÉRICA WORLD"**, con el lema **"Importando y Exportando el Mundo."**. El proyecto, el sistema y la empresa en los requerimientos se llaman **Decokasa** (DECOKASA S.A.S.).

- En la documentación y en el código, el proyecto se sigue llamando **Decokasa**.
- **⏸ Falta confirmar con el cliente:**
  - qué relación hay entre DECOKASA S.A.S. y Latinoamérica World;
  - qué nombre se ve en la pantalla de inicio de sesión, en el encabezado, en el título de la pestaña y en los correos.
- **Opciones para presentar al cliente [P]:**
  - **A:** logo de Latinoamérica World y, junto a él, el texto "Decokasa · Seguimiento de pedidos".
  - **B:** solo Latinoamérica World, con "Seguimiento de pedidos" como nombre del sistema.
  - **C:** Decokasa como marca principal y Latinoamérica World solo en el pie de página.
- Mientras no se decida, los textos de la interfaz usan "Seguimiento de pedidos" sin nombre de marca. Cambiarlo después es cambiar un texto, no el diseño.

## 2. Logos

Las imágenes originales se conservan sin cambios en `documentos/logos/`. Todas son JPEG, sin transparencia.

| Archivo | Tamaño | Contenido | Fondo | Uso recomendado [P] |
| --- | --- | --- | --- | --- |
| `logo1.jpeg` | 1416 × 648 | Isotipo, nombre y lema, en blanco y naranja | Morado | Pantalla de inicio de sesión, portadas y banners anchos |
| `logo2.jpeg` | 1600 × 1600 | La misma versión en formato cuadrado | Morado | Pantalla de carga, imagen de perfil, redes; fuente para el ícono de la app |
| `logo3.jpeg` | 1416 × 648 | Isotipo, nombre y lema, en morado y naranja | Blanco | Fondos claros: encabezado de los correos, documentos impresos o en PDF |
| `logo4.jpeg` | 623 × 905 | Solo el isotipo (América con puntos), sin texto; figura **blanca** con puntos naranjas | **Fondo claro (≈ #F0F0F0) que forma parte de la imagen.** El JPEG no es transparente | **Todavía no se puede usar en la interfaz.** Sobre fondo claro, la figura blanca casi no se ve. Sobre morado, el fondo claro del JPEG se ve como un rectángulo gris alrededor del isotipo. **No está listo para favicon ni para la barra lateral:** ⏸ hace falta una versión transparente (SVG o PNG) |

**Reglas de uso [P]:**
- **Fondos:** sobre morado, `logo1` o `logo2` tal como vienen, porque su fondo ya es morado; sobre blanco, `logo3`. No usar `logo3` sobre morado.
- **`logo4` no se usa** hasta tener una versión transparente: ni sobre fondos claros ni sobre morado, porque su fondo claro es parte de la imagen.
- **Mientras tanto, para la barra lateral y el favicon [P]:** un recorte cuadrado de `logo2`, que tiene el isotipo sobre morado. Es una solución temporal; conviene reemplazarla cuando llegue la versión transparente.
- **Área libre:** alrededor del logo, al menos la altura de la letra "W" de WORLD.
- **Tamaño mínimo:** logo completo de 120 px de ancho; isotipo de 24 px de alto.
- **No modificar:** no cambiar los colores, no estirar, no agregar sombras ni efectos.
- **El lema:** solo donde el logo se ve grande (login, portadas). En el encabezado de la aplicación, va sin lema.

**⏸ Pedir al cliente:**
- Los logos en formato vectorial (SVG) o en PNG con fondo transparente. **Lo más urgente es el isotipo transparente:** sin él no hay favicon ni ícono de barra lateral definitivos.
- Una versión del isotipo en morado y naranja para fondos claros.

Con JPEG no se puede quitar el fondo sin perder calidad ni hacer un favicon nítido.

## 3. Color

### 3.1 Colores de marca

Los HEX son **aproximados**: se midieron como la mediana de los píxeles de cada color en los cuatro JPEG, con Python (Pillow). La compresión JPEG altera ligeramente los colores, así que **⏸ conviene pedir al cliente los códigos oficiales** (HEX, RGB o Pantone).

| Color | HEX aproximado | Dónde aparece | Medición |
| --- | --- | --- | --- |
| Morado de marca | `#31275B` | Fondo de `logo1` y `logo2`; isotipo y texto de `logo3` | Igual en los 4 archivos |
| Naranja de marca | `#FE5E00` | Los cuatro puntos naranjas | Igual en los 4 archivos |
| Blanco | `#FFFFFF` | Isotipo y texto sobre morado; fondo de `logo3` | — |

**El amarillo `#FFD100` del demo no está confirmado como color oficial** y no se usa. Lo reemplazan el morado y el naranja.

### 3.2 Escalas derivadas [P]

Para tener fondos suaves, bordes y estados (pasar el mouse, presionar), se derivan tonos a partir de los dos colores de marca. Son una propuesta y se pueden ajustar.

| Token | HEX | Uso |
| --- | --- | --- |
| `morado-50` | `#F4F3F8` | Fondo de filas seleccionadas y de secciones |
| `morado-100` | `#E6E4EF` | Fondo al pasar el mouse sobre una fila |
| `morado-200` | `#CBC7DD` | Bordes de las tarjetas activas |
| `morado-600` | `#4A4078` | Botón principal al pasar el mouse |
| `morado-800` | `#31275B` | **Marca:** barra lateral, encabezado, botón principal |
| `morado-900` | `#221B40` | Botón principal presionado; texto de títulos |
| `naranja-50` | `#FFF1E8` | Fondo del badge de urgencia |
| `naranja-500` | `#FE5E00` | **Marca:** acentos, íconos, puntos decorativos y el anillo de la etapa actual, siempre junto a un texto |
| `naranja-700` | `#B23F00` | Naranja para **texto**, botones con texto blanco y el **anillo de foco** |
| `gris-50` | `#F8F9FB` | Fondo de la aplicación |
| `gris-200` | `#E3E5EA` | Bordes y separadores |
| `gris-600` | `#475569` | Texto secundario |
| `gris-900` | `#1E1B2E` | Texto principal |

### 3.3 Contrastes (WCAG 2.2)

Calculados con la fórmula de WCAG. Para texto normal hace falta 4.5:1 (nivel AA); para texto grande (24 px, o 19 px en negrita) y para bordes o íconos, 3:1.

| Combinación | Contraste | Resultado |
| --- | --- | --- |
| Blanco sobre morado `#31275B` | **13.36:1** | AA y AAA. Principal |
| Morado `#31275B` sobre blanco | **13.36:1** | AA y AAA |
| Morado sobre `morado-50` | 12.1:1 | AA |
| Naranja `#FE5E00` sobre blanco | **3.08:1** | **No sirve para texto normal.** Sí para texto grande, íconos y bordes |
| Blanco sobre naranja `#FE5E00` | 3.08:1 | Igual: no sirve para botones con texto normal |
| Naranja sobre morado | 4.34:1 | No llega a 4.5:1 para texto normal; sí para íconos y acentos |
| Naranja de texto `#B23F00` sobre blanco | **5.83:1** | AA |
| Blanco sobre `#B23F00` | 5.83:1 | AA: botón naranja con texto |
| Gris `#475569` sobre blanco | 7.58:1 | AA, para texto secundario |
| Naranja `#FE5E00` sobre el fondo de la app `#F8F9FB` | **2.92:1** | **No llega a 3:1:** no sirve como anillo de foco ni como único indicador de estado |
| Badges: texto sobre su fondo claro (`#15803D`/`#ECFDF3`, `#B45309`/`#FFFBEB`, `#B91C1C`/`#FEF2F2`, `#B23F00`/`#FFF1E8`, `#475569`/`#F1F5F9`) | 4.76 a 6.92:1 | AA |
| Texto principal `#1E1B2E` sobre `#F8F9FB` | 15.93:1 | AA y AAA |

**Reglas de uso:**
1. **Botón principal:** fondo morado con texto blanco.
2. **El naranja `#FE5E00` no se usa como color de texto** sobre blanco ni como fondo de botones con texto, **ni como anillo de foco** (2.92:1 sobre el fondo de la app). Se usa en acentos: íconos, puntos decorativos y el anillo de la etapa actual, que siempre va junto a un texto.
3. **Para texto o botones naranjas:** `#B23F00`.
4. **Gris `#6C6B6D` del demo:** no se usa para texto pequeño sobre fondos grises. El texto secundario es `gris-600`.

### 3.4 Colores de estado (semáforo)

Corrige el hallazgo Q4 de la auditoría (contraste del semáforo de 3.3:1). El estado **nunca depende solo del color**: siempre va con ícono y texto.

| Estado | Texto e ícono | Fondo del badge | Contraste sobre blanco | Contraste sobre el fondo del badge | Ícono (lucide-react) |
| --- | --- | --- | --- | --- | --- |
| A tiempo | `#15803D` | `#ECFDF3` | 5.02:1 | 4.76:1 | `CircleCheck` |
| Por vencer | `#B45309` | `#FFFBEB` | 5.02:1 | 4.84:1 | `Clock` |
| Vencida | `#B91C1C` | `#FEF2F2` | 6.47:1 | 5.91:1 | `CircleAlert` |
| Sin plazo, en pausa o finalizado | `#475569` | `#F1F5F9` | 7.58:1 | 6.92:1 | `CircleMinus` / `Flag` |

Todas las combinaciones **cumplen AA (4.5:1 o más)**, pero no todas superan 5:1: dentro del badge, "A tiempo" y "Por vencer" quedan en 4.76:1 y 4.84:1, con poco margen. **[P]** Si se cambia el fondo de un badge, hay que recalcular el contraste antes de aprobarlo.

**[P] "Por vencer" usa un ámbar (`#B45309`) y no el naranja de marca,** para no confundir un aviso de plazo con el color de la marca.

## 4. Tipografía [P]

En los logos, el nombre "LATINOAMÉRICA WORLD" parece estar en **Montserrat** (negrita) y el lema en **Roboto** (negrita cursiva). Es una estimación visual; **⏸ confirmar con el cliente** o con quien diseñó el logo.

| Uso | Fuente | Pesos |
| --- | --- | --- |
| Marca y títulos de pantalla | Montserrat | 600 y 700 |
| Interfaz, tablas y formularios | Inter, con números tabulares (`tabular-nums`) para fechas, números de pedido y plazos | 400, 500 y 600 |

- **Las fuentes se sirven desde el propio servidor** (paquetes `@fontsource`), **no desde Google Fonts**: David trabaja desde China, donde los servicios de Google pueden estar bloqueados (ADR-0002, ADR-0003).
- **Fuente de respaldo:** `system-ui, -apple-system, "Segoe UI", Roboto, sans-serif`.

**Escala de tamaños, siempre en `rem`** (corrige Q5: había 122 tamaños fijos de 10 y 11 px que no crecen con la configuración del navegador):

| Token | rem | px (con base 16) | Uso |
| --- | --- | --- | --- |
| `text-xs` | 0.75 | 12 | **Mínimo absoluto:** metadatos, pies de tabla |
| `text-sm` | 0.875 | 14 | Celdas de tabla, etiquetas, badges |
| `text-base` | 1 | 16 | Texto de lectura, campos de formulario |
| `text-lg` | 1.125 | 18 | Títulos de tarjeta |
| `text-xl` | 1.25 | 20 | Títulos de sección |
| `text-2xl` | 1.5 | 24 | Título de pantalla |
| `text-3xl` | 1.875 | 30 | Número de pedido en el detalle |

- **Nunca menos de 12 px**, y sin tamaños arbitrarios como `text-[10px]`.
- **Altura de línea:** 1.5 para el texto y 1.25 para los títulos.

## 5. Espaciado, forma y elevación [P]

- **Espaciado:** base de 4 px, la escala por defecto de Tailwind (`1` = 0.25 rem). Se usan sobre todo 2, 3, 4, 6 y 8.
  - Separación entre campos: 4.
  - Padding de las tarjetas: 6.
  - Separación entre secciones: 8.
- **Radios:** tres valores, para unificar los `rounded-xl`, `2xl` y `3xl` mezclados del demo (Q19).
  - `--radius-sm` 6 px: badges y campos.
  - `--radius-md` 8 px: botones.
  - `--radius-lg` 12 px: tarjetas y diálogos.
- **Sombras:** solo `shadow-sm` en las tarjetas y `shadow-lg` en los diálogos. No se usa `backdrop-blur` (Q19).
- **Tamaño táctil:** mínimo 40 px de alto en escritorio y 44 px en móvil para botones y elementos clicables.
- **Densidad de las tablas:** filas de 44 px y padding horizontal de 3.

## 6. Tokens para Tailwind 4 y shadcn/ui

Referencia para la fase 1; no se aplica todavía al código. Reemplaza los tokens sin uso de `frontend/src/index.css` y los 653 colores escritos a mano del demo (auditoría Q8). Los componentes usan **solo** estos tokens, nunca HEX sueltos.

```css
@import "tailwindcss";

@theme {
  /* Marca (HEX aproximados, ver sección 3.1) */
  --color-marca-morado: #31275B;
  --color-marca-naranja: #FE5E00;
  --color-marca-naranja-texto: #B23F00;
  /* Estados */
  --color-estado-ok: #15803D;
  --color-estado-aviso: #B45309;
  --color-estado-vencido: #B91C1C;
  --color-estado-neutro: #475569;
  /* Tipografía */
  --font-sans: "Inter", system-ui, -apple-system, "Segoe UI", Roboto, sans-serif;
  --font-titulo: "Montserrat", var(--font-sans);
  /* Radios */
  --radius-sm: 0.375rem;
  --radius-md: 0.5rem;
  --radius-lg: 0.75rem;
}

/* Variables que usa shadcn/ui */
:root {
  --background: #F8F9FB;
  --foreground: #1E1B2E;
  --card: #FFFFFF;
  --card-foreground: #1E1B2E;
  --primary: #31275B;
  --primary-foreground: #FFFFFF;
  --secondary: #F4F3F8;
  --secondary-foreground: #31275B;
  --accent: #FFF1E8;
  --accent-foreground: #B23F00;
  --muted: #F1F5F9;
  --muted-foreground: #475569;
  --destructive: #B91C1C;
  --border: #E3E5EA;
  --input: #E3E5EA;
  --ring: #B23F00; /* naranja oscuro: 5.83:1 sobre blanco. Sobre fondo morado, el foco es blanco */
  --radius: 0.5rem;
}
```

- **Modo oscuro [P]:** no está en los requerimientos y queda fuera de v1.
- **Movimiento reducido:** con `prefers-reduced-motion`, las animaciones de `tw-animate-css` que trae shadcn pasan a un cambio inmediato (auditoría Q12).

## 7. Componentes base

Todos se construyen con componentes de shadcn/ui (basados en Radix), que ya resuelven foco, teclado y ARIA. Los datos vienen del contrato `openapi.yaml`.

### 7.1 Tabla de pedidos

**Base:** `Table` de shadcn. **[P]** `@tanstack/react-table` para ordenar y paginar en el cliente; la paginación real la hace el servidor (`pagina`, `tamano`).

| Columna | Dato del contrato | Formato |
| --- | --- | --- |
| Número | `numero` | **Enlace** al detalle, en `tabular-nums` |
| Pedido | `nombre` | Texto, recortado con un `title` con el nombre completo |
| Flujo | `tipoFlujo` | Badge neutro "A · Ecuador" / "B · China" |
| Etapa actual | `etapaActual`, `nombreEtapaActual` | "A4 · Análisis y gestión" |
| Responsable | `responsableActual` | Nombre |
| Vence | `venceEn` | Fecha y hora de Ecuador, más el badge de vencimiento (7.6) |
| Urgencia | `urgencia` | Badge de urgencia (7.6), solo si hay |

**Accesibilidad:**
- Usa `<caption>` (puede estar oculto visualmente) y `<th scope="col">`; las columnas ordenables llevan `aria-sort`.
- **Se entra al pedido por el enlace del número, no por un `onClick` en la fila** (Q2). Toda la fila puede seguir siendo clicable con el mouse, pero el enlace es lo que se recorre con Tab.
- Los filtros (búsqueda, flujo, etapa, vencidos) tienen `<Label>` visible o `aria-label` (Q3).

**Estados:**
- Cargando: filas `Skeleton`.
- Sin resultados: mensaje con la acción para limpiar los filtros.
- Error: mensaje con "Reintentar".
- **En móvil:** desplazamiento horizontal con la primera columna fija.

### 7.2 Línea de tiempo horizontal

Muestra el recorrido del pedido "como el rastreo de un envío", con la etapa actual marcada (respuesta del cliente). Se usa en el detalle, en los dashboards y en la búsqueda (hoy solo está en el detalle; ver la auditoría).

- **Estructura:** lista ordenada `<ol>` con una etapa por elemento: 11 en el flujo A y 12 en el B, según `lineaDeTiempo`. Cada etapa es un `<button>` para ver su detalle.
- **Cada etapa muestra:** el número (A1…A11 / B1…B12), el nombre, el ícono de estado y la fecha de vencimiento o de salida.

**Estados visuales:**

| Estado | Aspecto |
| --- | --- |
| COMPLETADA | Círculo morado con ✓ blanco; texto morado |
| ACTUAL | Anillo **naranja de marca** de 3 px, nombre en negrita y la etiqueta de texto "Etapa actual"; `aria-current="step"` (Q13). El anillo es solo un acento (2.79:1 sobre `naranja-50`); el estado lo dan el texto y el atributo |
| PENDIENTE | Círculo con borde gris; texto `gris-600` |
| DEVUELTA | Círculo con borde `estado-aviso` e ícono `Undo2`; texto "devuelta" |
| Fin automático | Ícono `Flag`; sin acciones |

**Reglas:**
- Las etapas están unidas por una línea: morada en el tramo recorrido y gris en el que falta.
- En pantallas angostas, desplazamiento horizontal con `min-w-max`; al abrir, se centra la etapa actual.
- El estado nunca depende solo del color: siempre lleva ícono y texto.
- La línea de tiempo tiene `aria-label`, por ejemplo "Recorrido del pedido DK-EC-2026-0001".

### 7.3 Panel de etapa con check

Una `Card` con la etapa seleccionada.

- **Encabezado:** nombre de la etapa, responsable o asignado, plazo y vencimiento, y los badges de urgencia y de vencimiento.
- **Acciones:** se muestran solo las de `accionesDisponibles` que devuelve el servidor, para no repetir los errores 1 y 2 del demo, que mostraba acciones de otra etapa en B3 y B4.

**Botones de acción:**

| Acción | Botón | Diálogo |
| --- | --- | --- |
| APROBAR / ENVIAR | Principal, morado ("Aprobar ✓", "Enviar") | `AlertDialog` de confirmación. Observación opcional, salvo en Fabricación |
| RECHAZAR | Contorno rojo ("Rechazar ✗") | `Dialog` con observación **obligatoria** |
| DEVOLVER_POR_ERROR | Contorno | `Dialog` con observación obligatoria y archivos opcionales |
| DEVOLVER_POR_PRODUCTO_NUEVO | Contorno ámbar | `Dialog` con observación y `RadioGroup` de urgencia (4, 12 o 24 h) |
| REPORTAR_DEMORA | Contorno | `Dialog` con observación y selector de fecha (⏸ quién la calcula) |
| ELEGIR_BODEGA | — | `Select` con su etiqueta; guarda **solo al confirmar**, no en cada cambio (Q17) |

**Reglas:**
- **Diálogos accesibles (Q1):** `Dialog` y `AlertDialog` de shadcn, con `role="dialog"`, `aria-modal`, título asociado, foco atrapado, cierre con Esc y regreso del foco al botón que los abrió.
- **Requisitos faltantes:** si el servidor responde 422 `REQUISITO_FALTANTE`, se muestra la lista `faltantes` dentro del panel. Por ejemplo: "Falta adjuntar: Packing List, BL".
- **Conflicto de etapa:** si responde 409 `ETAPA_CAMBIO`, aparece "Otro usuario actualizó este pedido" con el botón "Ver estado actual".
- **Estado propio de cada etapa:** el panel se reinicia al cambiar de etapa o de pedido (`key` por pedido y etapa), para que un texto a medio escribir no se publique en otra etapa (Q6).

### 7.4 Observaciones

- **Lista:** cada observación muestra autor, rol, fecha y hora de Ecuador, el texto y los archivos adjuntos. Va de la más reciente a la más antigua.
- **Nueva observación:** `Textarea` con `<Label>` visible, contador de caracteres (máximo 4000, según el contrato) y el botón "Publicar observación".
- **Aviso de publicación:** al publicar, un aviso en una región `aria-live="polite"`, que es el toast Sonner de shadcn (Q10).

### 7.5 Archivos

- **Lista por etapa:** nombre, tipo de documento, tamaño, quién lo subió y cuándo, y un badge con el estado de la copia a Drive:
  - "Pendiente", gris, con spinner;
  - "En Drive", verde, con enlace;
  - "Falló la copia", rojo; solo el admin ve el detalle.
- **Subida:** un `<input type="file">` con su etiqueta, más una zona para arrastrar archivos (la alternativa de teclado es el botón "Elegir archivo"). Durante la subida, una barra de `Progress` con `aria-valuenow`.
- **Validación antes de enviar:** tamaño máximo de 100 MB. Si el servidor responde 413, el mensaje es "El archivo supera 100 MB". Si responde 415, el mensaje es que el tipo de archivo no se acepta (⏸ formatos aceptados).
- **Documentos obligatorios:** cada uno se marca con su estado (adjunto o falta).

### 7.6 Badges de urgencia y de vencimiento

**Base:** `Badge` de shadcn. Llevan ícono y texto, en `text-sm`.

| Badge | Texto | Colores | Ícono |
| --- | --- | --- | --- |
| Urgente | "Urgente · 4 h" | Texto `#B23F00` sobre `naranja-50`, con borde naranja de marca | `Zap` |
| Prioritario | "Prioritario · 12 h" | Texto `#B45309` sobre `#FFFBEB` | `ArrowUp` |
| Normal | "Normal · 24 h" | Texto `gris-600` sobre `#F1F5F9` | `Clock` |
| A tiempo | "A tiempo · vence 14/10" | Estado ok (3.4) | `CircleCheck` |
| Por vencer | "Vence hoy 16:00" / "Vence en 1 día hábil" | Estado aviso | `Clock` |
| Vencida | "Vencida hace 2 días hábiles" | Estado vencido | `CircleAlert` |

Los badges de vencimiento muestran la fecha en hora de Ecuador y en días hábiles (ADR-0006). **⏸** Que las urgencias se cuenten en horas hábiles o corridas cambia el texto, no el diseño.

## 8. Accesibilidad: recomendaciones de la auditoría

Cada fila corrige un hallazgo de `auditoria-demo.md`, sección 7.

| Hallazgo | Regla del sistema de diseño |
| --- | --- |
| Q1 Modales inaccesibles | Solo `Dialog`, `AlertDialog` y `Sheet` de shadcn: foco atrapado, Esc, título asociado y foco de vuelta al cerrar |
| Q2 Elementos clicables que no son botones | Toda acción es `<button>` o `<a>`. Nunca un `onClick` en un `div`. El checklist usa `Checkbox` y el selector de flujo, `RadioGroup` |
| Q3 Campos sin etiqueta | Cada campo con `<Label htmlFor>` o con `Form` de shadcn más React Hook Form. Búsquedas y filtros con `aria-label` |
| Q4 Contraste del semáforo | Colores de estado de la sección 3.4, todos con AA: entre 4.76:1 y 6.92:1 sobre el fondo del badge, y entre 5.02:1 y 7.58:1 sobre blanco. Siempre con ícono y texto |
| Q5 Texto chico y fijo | Escala en `rem` de la sección 4, con un mínimo de 12 px |
| Foco visible | Anillo `--ring` naranja oscuro `#B23F00` (5.83:1) de 2 px y separado del borde, con `focus-visible`; blanco sobre la barra lateral morada (13.36:1). Nunca `outline: none` sin reemplazo |
| Q9 Menú móvil | `Sheet` de shadcn; con el menú cerrado no se llega a sus elementos con Tab. Botón hamburguesa con `aria-expanded`. `NavLink` con `aria-current="page"`. Botones de solo ícono con `aria-label` |
| Q10 Avisos | Sonner (`aria-live`) |
| Q11 Pestañas del detalle | `Tabs` de shadcn (`tablist`, flechas del teclado) |
| Q12 Movimiento | Respeta `prefers-reduced-motion` |
| Q13 Encabezados y etapa actual | Un `h1` por pantalla, sin saltos de nivel; `aria-current="step"` en la línea de tiempo |
| Teclado | Todo se puede usar con Tab, Enter, Espacio y Esc, sin trampas de foco fuera de los diálogos |

**Cómo verificarlo [P]:** ESLint con `jsx-a11y` (Q7), `/ecc:accessibility` al cerrar las fases 1, 2 y 5, y Playwright con axe-core en las pruebas de punta a punta.

## 9. Pendientes del cliente

| ⏸ Pendiente | Qué afecta |
| --- | --- |
| Relación entre DECOKASA S.A.S. y Latinoamérica World; qué nombre se ve en el login, el encabezado, la pestaña y los correos | Sección 1, textos de la interfaz |
| **Isotipo con fondo transparente (SVG o PNG): `logo4.jpeg` no sirve como está** | Sección 2: favicon e ícono de la barra lateral |
| Logos completos en SVG o PNG con fondo transparente, y un isotipo para fondos claros | Sección 2: encabezado, correos y documentos |
| Códigos oficiales de color (HEX, RGB o Pantone) | Sección 3; los HEX actuales son aproximados |
| Tipografía oficial de la marca | Sección 4 |
| Formatos de archivo aceptados | Sección 7.5 |
| Quién calcula la nueva fecha en una demora de aduana | Sección 7.3 |
| Urgencias en horas hábiles o corridas | Sección 7.6 |

## 10. Siguiente uso

- **Fase 1, paso 1.2:** inicializar shadcn/ui en `frontend/` con los tokens de la sección 6.
- **Fase 1:** migrar cada componente reutilizado, aplicando las reglas de la sección 8 y la lista de `auditoria-demo.md` 7.4.
- **Antes de la fase 1:** presentar al cliente la sección 1 (nombres) y la sección 9 (pendientes).

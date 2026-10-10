# ADR-0007: Los documentos solo se suben, sin validar su contenido

**Fecha**: 2026-10-10 (registrada originalmente el 2026-10-09)
**Estado**: accepted
**Decisores**: cliente (respuesta del 2026-10-09: "solo se sube el documento, es cosa de cada usuario")

## Contexto

En Codificación hay que revisar cantidades, etiquetas, modelos, medidas y colores (`requerimientos.md`, 1.4). Se preguntó si eso se valida leyendo el documento o con un formulario estructurado en el sistema.

## Decisión

El sistema solo recibe y guarda los archivos. No lee ni valida su contenido; cada usuario responde por lo que sube. El control de calidad es la revisión humana de la etapa de Revisión. El detalle de productos va dentro de los archivos.

## Alternativas consideradas

### Formulario estructurado de productos
- **Pros**: el sistema podría validar campos vacíos y cantidades.
- **Contras**: más trabajo para los usuarios y para el desarrollo.
- **Por qué no**: el cliente lo descartó.

## Consecuencias

### Positivas
- Menos alcance y menos trabajo de captura.
- La pestaña de productos del demo se descarta (`auditoria-demo.md`, 8.3).

### Negativas
- Los errores de contenido solo se detectan en Revisión o en China.

### Riesgos
- Las devoluciones siguen ocurriendo. Se miden con la métrica "devoluciones por pedido" del PRD.

## Nota

Sí se validan el tipo, el tamaño y el nombre del archivo, por seguridad (ADR-0003). Eso no es validar el contenido.

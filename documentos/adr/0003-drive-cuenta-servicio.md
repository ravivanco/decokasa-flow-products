# ADR-0003: Archivos en Google Drive mediante cuenta de servicio desde el servidor

**Fecha**: 2026-10-10 (registrada originalmente el 2026-10-09)
**Estado**: accepted
**Decisores**: cliente (stack: "Drive API desde el servidor con cuenta de servicio"; confirmó que tiene Google Workspace)

## Contexto

Los documentos de cada etapa se guardan en Google Drive, con un máximo de 100 MB por archivo (`requerimientos.md`, 4.2 #15). David sube archivos desde China. El demo simula la subida con un archivo falso (`auditoria-demo.md`, 2.1 #15 y S9).

## Decisión

El navegador sube el archivo al backend. El backend lo valida, lo guarda temporalmente y lo copia a una unidad compartida de Drive con una cuenta de servicio. La copia se reintenta si falla. En la arquitectura es el puerto `AlmacenArchivos`, con el adaptador `GoogleDriveAlmacen`.

## Alternativas consideradas

### Subida directa desde el navegador a Drive
- **Pros**: menos tráfico por el servidor.
- **Contras**: el navegador tendría que hablar con Google.
- **Por qué no**: desde China no se puede tocar Google; límite de confianza R8.

### Guardar los archivos en la base de datos
- **Pros**: todo en un solo lugar.
- **Contras**: una base muy pesada y respaldos lentos.
- **Por qué no**: el cliente pidió Drive.

## Consecuencias

### Positivas
- Funciona desde China.
- La clave de Google vive solo en el servidor.

### Negativas
- El servidor necesita espacio temporal y una cola de copias.

### Riesgos
- Cuotas y permisos de la cuenta de servicio. Se prueba en la fase 3.

## Propuestas asociadas (no aprobadas)

- **Propuesta del equipo**: dentro de la carpeta por país y pedido (`requerimientos.md`, 4.7), crear subcarpetas por año y etapa (`arquitectura.md`, 4.5).

## Pendiente del cliente

- Formatos de archivo aceptados. Propuesta: PDF, Word, Excel, imágenes y ZIP.
- Quién tiene acceso directo a la carpeta de Drive.
- Crear la cuenta de servicio en el Workspace del cliente.

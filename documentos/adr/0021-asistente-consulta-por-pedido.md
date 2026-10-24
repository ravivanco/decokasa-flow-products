# ADR-0021: El asistente responde sobre un solo pedido, con permisos comprobados en el servidor y referencias verificadas

**Fecha**: 2026-10-22
**Estado**: accepted (equipo, Richard, 2026-10-22) **solo para el piloto con datos ficticios**. Pendiente de revisión por Codex (parte B); sin implementar.
**Decisores**: Richard (F7-D1 opción C, F7-D4 y precisiones del 2026-10-22). No es una respuesta del cliente.

## Contexto

El chat responde sobre pedidos, etapas y mercadería descrita en documentos, en modo de solo lectura, dentro de los permisos vigentes del usuario y con referencias al pedido, al documento y a la página. Si no hay evidencia, debe decirlo. Hoy leer un pedido no filtra por país (P-13), y descargar un archivo sigue P-17/P-06. Un modelo puede citar mal o inventar, y los documentos pueden contener instrucciones.

## Decisión

1. **Una conversación pertenece a un único pedido** y a su autor. Las consultas sobre varios pedidos quedan fuera de esta entrega.
2. **Roles (F7-D4):** `admin`, `asistente_compras`, `marketing`, `compras_china` y `logistica`. `consulta` y `asistente_china` no dan acceso por sí solos, pero no quitan el que dé otro rol.
3. **Permisos antes de enviar y antes de mostrar.** Antes de cada consulta y antes de devolver una conversación guardada, el servidor comprueba, con el estado vigente en la base, la cuenta, el rol, el país del pedido, la lectura del pedido y la descarga (P-17) de cada documento. Conocer un identificador no basta. Una conversación ajena o inexistente responde 404.
4. **Lo que se envía lo elige el servidor:** los datos del pedido (como texto del servidor) y solo los documentos que el usuario puede descargar, que son PDF por su contenido y, en el piloto, están en el manifiesto (ADR-0023). El modelo no tiene herramientas de escritura ni de búsqueda.
5. **Referencias verificadas.** Las citas de la API se aceptan solo si apuntan a un documento autorizado **enviado en esa misma consulta** y a una página que existe en ese PDF. Las demás se descartan y no cuentan como evidencia. Sin ninguna válida, `evidenciaSuficiente=false` y la respuesta lo dice. La referencia al pedido la arma el servidor, no el modelo.
6. **Permisos que cambian.** Si el usuario ya no puede leer el pedido o su país, la conversación no se muestra (404). Si perdió el permiso de descarga de un documento enviado o citado en un mensaje, ese mensaje se devuelve **retenido** (sin texto ni referencias) y no se reenvía al modelo en los turnos siguientes.
7. **Instrucciones dentro de los documentos:** los documentos y las observaciones son contenido, nunca instrucciones. El prompt de sistema es fijo y el chat no puede ejecutar acciones, así que una instrucción inyectada no puede modificar nada; lo que queda es el riesgo de una respuesta engañosa, que se acota con las citas verificadas.

## Alternativas consideradas

### Preguntas sobre varios pedidos
- **Pros**: más útil para el seguimiento general.
- **Contras**: requiere búsqueda previa, más tokens por pregunta y más superficie de permisos.
- **Por qué no**: Richard eligió la opción C (F7-D1); queda para otra entrega, con su propio ADR.

### Herramientas de búsqueda en manos del modelo
- **Pros**: más autonomía.
- **Contras**: el modelo decide qué datos pide.
- **Por qué no**: los permisos los decide el servidor, no el modelo.

### Aceptar las citas del modelo tal cual
- **Pros**: más simple.
- **Contras**: una cita mal formada o inventada parecería evidencia.
- **Por qué no**: Richard pidió no presentar citas inventadas como evidencia.

## Consecuencias

### Positivas
- La fuga de datos entre pedidos o documentos queda acotada por diseño.
- Cada respuesta es verificable desde la pantalla.

### Negativas
- No responde preguntas generales sobre varios pedidos.
- Comprobar los permisos en cada consulta y al mostrar cada conversación añade lecturas a la base.

### Riesgos
- Mientras no exista el acceso por país (Colombia, ADR pendiente), todos los usuarios tienen solo EC. La comprobación de país usa `usuario_pais` en cuanto exista otro país.

## Pendiente

- Acceso por país para Colombia, que espera sus definiciones.
- Documentos reales: F7-D6.
- Relacionados: ADR-0004, 0007, 0019, 0020 y 0023.

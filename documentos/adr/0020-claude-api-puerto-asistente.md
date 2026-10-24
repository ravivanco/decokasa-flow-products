# ADR-0020: Claude API detrás del puerto `AsistenteIA`, con credenciales propias y sin escalado automático de modelo

**Fecha**: 2026-10-22
**Estado**: accepted (equipo, Richard, 2026-10-22) **solo para el piloto con datos ficticios**. Pendiente de revisión por Codex (parte B); sin implementar.
**Decisores**: Richard (decisión del equipo). No es una respuesta del cliente.

## Contexto

El cliente pidió conectar el sistema a la API de Claude sin decir para qué (requerimientos 1.3). Richard definió el 2026-10-22 un chat de consulta de solo lectura (requerimientos §5, "Definiciones de la fase 7") y confirmó Claude API como proveedor. El manual (7.4) pide un puerto `AsistenteIA` con su adaptador en el backend, y el modelo y la clave solo en el servidor. ADR-0011 exige un dominio sin Spring ni SDK externos, y ADR-0015, ningún secreto en Git.

## Decisión

- Módulo nuevo `asistente`. Su dominio y sus casos de uso no dependen de Spring ni del SDK. El puerto de salida es `AsistenteIA`; el adaptador usa el **SDK oficial de Java** (`com.anthropic:anthropic-java`). Hay además un adaptador falso para las pruebas.
- **Credenciales propias de Decokasa**, nuevas e independientes de las herramientas de desarrollo. Se leen de `decokasa.ia.claude-api-key` (`DECOKASA_CLAUDE_API_KEY`). El cliente se construye con `.apiKey(...)` y una URL base explícita, **nunca** con `fromEnv()`, que tomaría `ANTHROPIC_API_KEY`, `ANTHROPIC_AUTH_TOKEN` o un perfil local. Sin clave, la IA queda desactivada y no se busca otra fuente. La clave nunca aparece en logs, respuestas ni `toString()`.
- **Modelo** configurable (`decokasa.ia.modelo`); el inicial es `claude-haiku-5-5`, sujeto a verificar su disponibilidad para la cuenta nueva.
- **Sin cambio automático a modelos más caros:** sin el parámetro `fallbacks` del servidor, sin herramienta advisor y sin escalado por código. Ante un `refusal` o un error, se responde eso.
- **Desactivada por defecto** (`IA_HABILITADA=false`). Si la IA falla o está apagada, el resto del sistema funciona igual.

## Alternativas consideradas

### Llamar a la API desde el navegador
- **Pros**: menos backend.
- **Contras**: expone la clave y no permite aplicar permisos.
- **Por qué no**: la regla del proyecto es que los permisos se validan en el servidor y que las claves solo viven allí (requerimientos 4.7).

### `AnthropicOkHttpClient.fromEnv()` con `ANTHROPIC_API_KEY`
- **Pros**: es la configuración habitual del SDK.
- **Contras**: puede reutilizar sin querer la clave o el perfil de desarrollo del equipo.
- **Por qué no**: Richard pidió no reutilizar credenciales existentes.

### Respaldo automático a otro modelo (`fallbacks`)
- **Pros**: menos rechazos.
- **Contras**: puede cambiar a un modelo más caro sin control.
- **Por qué no**: Richard lo excluyó expresamente.

### Managed Agents o un agente con herramientas
- **Pros**: más flexibilidad.
- **Contras**: más costo y más superficie, y la consulta no necesita un sandbox.
- **Por qué no**: una consulta de un solo pedido se resuelve con una llamada a la Messages API.

## Consecuencias

### Positivas
- El proveedor queda aislado tras un puerto, y las pruebas usan el adaptador falso.
- Ninguna credencial del entorno de desarrollo puede filtrarse al producto.

### Negativas
- Una dependencia nueva en el backend.
- La gestión de la cuenta, de la clave y de los límites de gasto en la Console es manual (Richard).

### Riesgos
- Que el modelo no esté disponible para la cuenta nueva. Mitigación: comprobar `GET /v1/models/claude-haiku-5-5` en la primera llamada autorizada.
- Que los precios cambien. Mitigación: tabla de precios versionada (ADR-0022) y documentación oficial revisada antes de implementar.

## Pendiente

- Llamadas reales, compras y la cuenta: no autorizadas.
- Retención de datos del proveedor y documentos reales: F7-D6 (ADR-0023).
- Relacionados: ADR-0001, 0011, 0015, 0021, 0022 y 0023.

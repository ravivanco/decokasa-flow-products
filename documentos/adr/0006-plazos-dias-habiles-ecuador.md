# ADR-0006: Plazos en días hábiles con hora de Ecuador

**Fecha**: 2026-10-10 (registrada originalmente el 2026-10-09)
**Estado**: accepted
**Decisores**: cliente (respuestas del 2026-10-09: "laborables", "Ecuador", fin de semana pasa al lunes, el retraso arrastra las fechas)

## Contexto

Cada etapa tiene un plazo máximo (`requerimientos.md`, 4.2 y 4.4). La calculadora actual suma días corridos y mueve al lunes solo la fecha final (`requerimientos.md`, 1.8). El demo cuenta días hábiles, pero sin horas ni zona horaria, y con una fecha fija (`auditoria-demo.md`, 2.2 y S6).

## Decisión

Los plazos se cuentan en días hábiles (lunes a viernes) con la hora de Ecuador (`America/Guayaquil`). Una fecha que cae en fin de semana pasa al lunes, también la llegada del barco. Un retraso en una etapa arrastra todas las fechas siguientes. La fecha y la hora salen siempre del reloj del servidor (`arquitectura.md`, R5).

Plazos confirmados: 24 h en Codificación, Revisión, Autorización de salida y Bodega; 3 días en Análisis; 10 días en Embarque.

## Alternativas consideradas

### Días calendario (como la calculadora actual)
- **Pros**: cálculo más simple.
- **Contras**: no es lo que confirmó el cliente.
- **Por qué no**: el cliente respondió "laborables".

## Consecuencias

### Positivas
- Plazos coherentes para Ecuador y China, sin depender de la hora de cada navegador.

### Negativas
- Hay que mantener un calendario laboral y, si se aprueba, feriados.

### Riesgos
- Etapas cuya duración real es en días corridos (el tránsito marítimo) quedan mal medidas. Por eso el tipo de plazo (hábil o corrido) es configurable por etapa.

## Diferencias entre fuentes (sin resolver)

- La calculadora original cuenta los 45 días de tránsito como días corridos. `arquitectura.md` usa días hábiles como valor provisional. No está decidido.

## Pendiente del cliente

- ¿Se descuentan los feriados de Ecuador y de China (Año Nuevo chino)? Propuesta: calendario de Ecuador editable por el admin.
- ¿Las urgencias de 4, 12 y 24 h son horas hábiles o corridas?
- Plazos de Fabricación e Incidencias.
- Meta de días ("se ajusta").
- Si Revisión rechaza durante una urgencia, ¿se mantiene o se reinicia el vencimiento de la urgencia?

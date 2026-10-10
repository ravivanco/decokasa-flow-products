# ADR-0009: Ecuador primero; Colombia después

**Fecha**: 2026-10-10 (registrada originalmente el 2026-10-09)
**Estado**: accepted
**Decisores**: cliente ("primero el sistema lo implementaremos en Ecuador"; respuesta del 2026-10-09 sobre Colombia: "eso se mira más adelante")

## Contexto

El sistema es para Ecuador y Colombia, con procesos distintos (`requerimientos.md`, 4.2 #18). El proceso de Colombia todavía no está definido.

## Decisión

La primera versión implementa solo Ecuador. El país forma parte del modelo desde el inicio (pedido, usuario y definición de flujo), así que Colombia se agrega después como nuevas definiciones de flujo, sin cambiar el motor (ADR-0004).

## Alternativas consideradas

### Ecuador y Colombia a la vez
- **Pros**: un solo lanzamiento.
- **Contras**: el proceso de Colombia no existe todavía.
- **Por qué no**: el cliente lo dejó para más adelante.

### Ignorar el país hasta que llegue Colombia
- **Pros**: un modelo más simple al principio.
- **Contras**: obligaría a migrar datos y código después.
- **Por qué no**: el cliente ya anunció que vienen dos países.

## Consecuencias

### Positivas
- Se sale a producción sin esperar a Colombia (la fase 7 puede ir después de la 8).

### Negativas
- Un campo de país que en v1 siempre vale EC.

## Pendiente del cliente

- Proceso de Colombia: etapas, responsables, plazos y bodegas.
- Quién elige el país al entrar (respuesta: "admin y usuarios, pero eso se mira más adelante").

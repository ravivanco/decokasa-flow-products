# ADR-0001: Stack tecnológico React + Spring Boot + PostgreSQL

**Fecha**: 2026-10-10 (decisión registrada originalmente el 2026-10-09 en `requerimientos.md`)
**Estado**: accepted
**Decisores**: cliente (documento `Latinoamérica.docx`, sección "Stack principal")

## Contexto

DECOKASA necesita una web interna para seguir pedidos de importación entre Ecuador y China. El documento del cliente trae una tabla de stack recomendado (`requerimientos.md`, 1.6) y el registro de decisiones la da por elegida ("Lo eligió el cliente"). Ya existe un demo en React exportado de Google AI Studio que conviene reutilizar.

## Decisión

Frontend con React 19 + TypeScript + Vite, Tailwind CSS 4, shadcn/ui, TanStack Query, React Router, React Hook Form y Zod. Backend con Spring Boot 3 y Java 21, Spring Security, Spring Data JPA y Flyway. Base de datos PostgreSQL en Docker. Documentación de la API con springdoc-openapi.

## Alternativas consideradas

### Rehacer el frontend con otro framework
- **Pros**: libertad total de diseño.
- **Contras**: se pierde el demo, que ya cubre la forma del sistema (`auditoria-demo.md`: 31 de 64 requisitos cumplidos).
- **Por qué no**: el cliente eligió React para reutilizar el demo.

### Backend en Node.js
- **Pros**: un solo lenguaje en todo el proyecto.
- **Contras**: no es lo que eligió el cliente.
- **Por qué no**: el cliente pidió Spring Boot por las reglas de negocio, la seguridad y las tareas programadas.

## Consecuencias

### Positivas
- El demo se reutiliza: 14 componentes se conservan (`auditoria-demo.md`, sección 8).
- PostgreSQL es relacional y transaccional, apto para el historial auditable.

### Negativas
- Dos lenguajes (TypeScript y Java) y dos procesos de compilación.

### Riesgos
- Cambiar el stack más adelante obliga a un ADR nuevo (`CLAUDE.md`).

## Diferencias entre fuentes (sin resolver)

- La sección 1.6 de `requerimientos.md` se titula "Stack técnico **propuesto**", mientras que el registro de decisiones y `CLAUDE.md` lo tratan como elegido y fijo.
- La sección 1.6 dice "PostgreSQL 16 o 17". `docker-compose.yml` usa la 17. No hay una elección explícita del cliente.
- La sección 1.6 incluye WebSocket (STOMP) para el tiempo real. El "stack fijo" de `CLAUDE.md` no lo incluye. Ver ADR-0014.
- La herramienta de compilación del backend no está en `requerimientos.md`. Ver ADR-0010.

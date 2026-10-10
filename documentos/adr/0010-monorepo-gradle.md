# ADR-0010: Monorepo con frontend/ y backend/, backend con Gradle

**Fecha**: 2026-10-10
**Estado**: proposed
**Decisores**: equipo de desarrollo (no viene del cliente)

## Contexto

El proyecto tiene un frontend React y un backend Spring Boot que cambian juntos en cada fase. El manual (`manual-paso-a-paso.md`) y `CLAUDE.md` ya usan la estructura `frontend/` + `backend/` en un solo repositorio, con Gradle. `requerimientos.md` no dice nada sobre la organización del repositorio ni sobre la herramienta de compilación.

## Decisión

Un solo repositorio (`ravivanco/decokasa-flow-products`) con `frontend/`, `backend/` y `documentos/`. El backend se compila con Gradle. El paquete base propuesto es `com.decokasa.seguimiento` (`arquitectura.md`, 5). Las ramas siguen el flujo del manual: fases desde `develop`, PR hacia `develop` y `main` solo para versiones.

## Alternativas consideradas

### Dos repositorios
- **Pros**: despliegues y permisos separados.
- **Contras**: hay que coordinar versiones y PR entre repositorios.
- **Por qué no**: el equipo es pequeño y cada fase toca las dos partes.

### Maven
- **Pros**: más común en proyectos Spring y con configuración declarativa.
- **Contras**: más verboso; el manual ya usa `/ecc:gradle-build`.
- **Por qué no**: sin preferencia del cliente; se eligió Gradle para seguir el manual. Es reversible antes de la fase 1.

## Consecuencias

### Positivas
- Un PR por fase con frontend, backend y documentación juntos.

### Negativas
- El CI debe distinguir qué parte cambió.

## Diferencias entre fuentes (sin resolver)

- `requerimientos.md` no menciona ni monorepo ni Gradle. El manual y `CLAUDE.md` los presentan como parte del "stack fijo". Por eso este ADR queda como **proposed** hasta que el equipo lo apruebe.
- El intento anterior del repositorio, ya vaciado (commits DKT-01 en `develop`), preparaba `.gitignore` para Maven. Queda solo como antecedente.

# Skills del manual paso a paso instaladas para Codex

**Fecha:** 2026-10-13. **Fuente:** `documentos/manual-paso-a-paso.docx` (texto extraído directamente del Word).

El Word menciona 64 nombres distintos: 61 están instalados en `C:/Users/Richard/.codex/skills/`; 39 son skills y 22 son adaptaciones de comandos o agentes ECC. No se ejecutaron las instrucciones de estas skills ni se modificaron requisitos, código del producto o configuración de Claude.

Los recursos de las skills se copiaron completos desde las instalaciones locales existentes. `claude-api` se descargó del repositorio oficial `anthropics/skills`, ruta `skills/claude-api`, mediante el instalador de Codex. Los SKILL.md se comprobaron por SHA-256 tras la copia y se verificó el número de archivos de cada carpeta.

## Invocación

En Codex usa `$ecc-react-review`, `$ecc-security-scan`, `$ecc-prp-plan`, etc. El prefijo `ecc:` del manual se representa como `ecc-` en los nombres instalados. Las adaptaciones leen el comando/agente original incluido en `references/original.md`; las referencias adicionales conservan su origen local en `C:/Users/Richard/.claude/plugins/marketplaces/ecc`. No son comandos slash nativos de Claude ni instalan conectores, credenciales, MCP, hooks o permisos. Una herramienta externa requerida deberá estar disponible al usar la skill.

Las skills estarán disponibles en el próximo turno. Las cuatro skills que ya existían en el proyecto también se copiaron al directorio personal para que puedan utilizarse desde otros proyectos.

## Nombres sin una skill instalable localizada

- `/init`: comando propio de Claude Code, sin SKILL.md localizado. No se creó una sustitución que afirme ser el original.
- `/simplify`: comando propio de Claude Code, sin SKILL.md localizado. No se creó una sustitución que afirme ser el original.
- `/agregar-flujo-decokasa`: el manual indica crear esta skill después de implementar el flujo B. No existe todavía; no se inventaron migraciones, reglas o decisiones pendientes para fabricarla.

## Inventario

| Nombre en el Word | Nombre para Codex | Resultado |
| --- | --- | --- |
| `agregar-flujo-decokasa` | `agregar-flujo-decokasa` | No disponible |
| `claude-api` | `claude-api` | Instalada |
| `code-review` | `code-review` | Instalada; adaptacion de comando/agente |
| `design-doc-mermaid` | `design-doc-mermaid` | Instalada |
| `ecc:accessibility` | `ecc-accessibility` | Instalada |
| `ecc:api-connector-builder` | `ecc-api-connector-builder` | Instalada |
| `ecc:api-design` | `ecc-api-design` | Instalada |
| `ecc:architecture-decision-records` | `ecc-architecture-decision-records` | Instalada |
| `ecc:benchmark` | `ecc-benchmark` | Instalada |
| `ecc:browser-qa` | `ecc-browser-qa` | Instalada |
| `ecc:canary-watch` | `ecc-canary-watch` | Instalada |
| `ecc:contract-first` | `ecc-contract-first` | Instalada |
| `ecc:dashboard-builder` | `ecc-dashboard-builder` | Instalada |
| `ecc:database-migrations` | `ecc-database-migrations` | Instalada |
| `ecc:database-reviewer` | `ecc-database-reviewer` | Instalada; adaptacion de comando/agente |
| `ecc:deployment-patterns` | `ecc-deployment-patterns` | Instalada |
| `ecc:design-system` | `ecc-design-system` | Instalada |
| `ecc:docker-patterns` | `ecc-docker-patterns` | Instalada |
| `ecc:e2e-testing` | `ecc-e2e-testing` | Instalada |
| `ecc:error-handling` | `ecc-error-handling` | Instalada |
| `ecc:evolve` | `ecc-evolve` | Instalada; adaptacion de comando/agente |
| `ecc:github-ops` | `ecc-github-ops` | Instalada |
| `ecc:gradle-build` | `ecc-gradle-build` | Instalada; adaptacion de comando/agente |
| `ecc:hexagonal-architecture` | `ecc-hexagonal-architecture` | Instalada |
| `ecc:java-reviewer` | `ecc-java-reviewer` | Instalada; adaptacion de comando/agente |
| `ecc:jira` | `ecc-jira` | Instalada; adaptacion de comando/agente |
| `ecc:jpa-patterns` | `ecc-jpa-patterns` | Instalada |
| `ecc:learn` | `ecc-learn` | Instalada; adaptacion de comando/agente |
| `ecc:learn-eval` | `ecc-learn-eval` | Instalada; adaptacion de comando/agente |
| `ecc:postgres-patterns` | `ecc-postgres-patterns` | Instalada |
| `ecc:production-audit` | `ecc-production-audit` | Instalada |
| `ecc:prp-commit` | `ecc-prp-commit` | Instalada; adaptacion de comando/agente |
| `ecc:prp-implement` | `ecc-prp-implement` | Instalada; adaptacion de comando/agente |
| `ecc:prp-plan` | `ecc-prp-plan` | Instalada; adaptacion de comando/agente |
| `ecc:prp-pr` | `ecc-prp-pr` | Instalada; adaptacion de comando/agente |
| `ecc:prp-prd` | `ecc-prp-prd` | Instalada; adaptacion de comando/agente |
| `ecc:quality-gate` | `ecc-quality-gate` | Instalada; adaptacion de comando/agente |
| `ecc:react-patterns` | `ecc-react-patterns` | Instalada |
| `ecc:react-performance` | `ecc-react-performance` | Instalada |
| `ecc:react-review` | `ecc-react-review` | Instalada; adaptacion de comando/agente |
| `ecc:react-test` | `ecc-react-test` | Instalada; adaptacion de comando/agente |
| `ecc:resume-session` | `ecc-resume-session` | Instalada; adaptacion de comando/agente |
| `ecc:save-session` | `ecc-save-session` | Instalada; adaptacion de comando/agente |
| `ecc:security-scan` | `ecc-security-scan` | Instalada |
| `ecc:skill-create` | `ecc-skill-create` | Instalada; adaptacion de comando/agente |
| `ecc:springboot-patterns` | `ecc-springboot-patterns` | Instalada |
| `ecc:springboot-security` | `ecc-springboot-security` | Instalada |
| `ecc:springboot-tdd` | `ecc-springboot-tdd` | Instalada |
| `ecc:springboot-verification` | `ecc-springboot-verification` | Instalada |
| `ecc:tdd-workflow` | `ecc-tdd-workflow` | Instalada |
| `ecc:test-coverage` | `ecc-test-coverage` | Instalada; adaptacion de comando/agente |
| `ecc:update-codemaps` | `ecc-update-codemaps` | Instalada; adaptacion de comando/agente |
| `ecc:update-docs` | `ecc-update-docs` | Instalada; adaptacion de comando/agente |
| `ecc:verification-loop` | `ecc-verification-loop` | Instalada |
| `ecc:vite-patterns` | `ecc-vite-patterns` | Instalada |
| `impeccable` | `impeccable` | Instalada |
| `init` | `init` | No disponible |
| `performance-optimization` | `performance-optimization` | Instalada |
| `playwright-best-practices` | `playwright-best-practices` | Instalada |
| `security-review` | `security-review` | Instalada |
| `shadcn` | `shadcn` | Instalada |
| `simplify` | `simplify` | No disponible |
| `tanstack-query-best-practices` | `tanstack-query-best-practices` | Instalada |
| `web-perf` | `web-perf` | Instalada |

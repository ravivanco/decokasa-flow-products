# Inventario revisado del avance de fase 6

Rama exigida: `fase-6-flujo-b`. Selección de 300 rutas, detalladas en `fase-6-seleccion-normal.txt` (278) y `fase-6-seleccion-ignorados.txt` (22 logs). Las listas usan rutas desde la raíz del repositorio.

- Código backend/frontend: generalización del motor, habilitación y creación por definición, pantallas A/B y permisos. Sin Header.tsx.
- Migraciones: V17 y V18, sin alterar versiones anteriores.
- Pruebas: nuevas reglas B/migraciones/habilitación, adaptación de expectativas anteriores y frontend. Infraestructura API real y Playwright solo bajo evidencias de fase 6.
- Documentación: plan, PRD, contrato, arquitectura, modelo, requisitos, guía, skill propia del proyecto, ADR-0019 e índice, contador de CLAUDE.md, excepción mínima de .gitignore e informe.
- Evidencias: resultados JUnit XML, JaCoCo XML, cobertura frontend resumida, logs de intentos iniciales/finales, JSON sanitizados, tres capturas ficticias, reproductores, revisión documental y comprobaciones de conservación.

Excluidos: todos los `.tmp`, builds/dist, credenciales ficticias temporales, bytes de archivos de prueba, binarios JUnit, .vscode, cambios/evidencias de fases 2–5, documentos ajenos y Header.tsx. Ningún recurso privado de Google ni archivo de 100 MiB. No se hace selección por carpetas globales de skills.

Guardas antes de preparar: rama correcta, índice previamente vacío (si no, detener sin descartar), archivos explícitos existentes, logs ignorados solo de fase 6, scan acotado de credenciales sin coincidencias, referencias del informe existentes y conservadas. La skill no está ignorada gracias a su excepción exacta.

`fase-6-preparar-indice.py` construye las listas y la auditoría; **no ejecuta git add**. Los avisos originales de logs/XML no se corrigen para limpiar whitespace. Se registran por separado resultados de `git diff --cached --check` y `--stat` en la auditoría. El estado real del índice se contrasta con ambas listas al terminar.

H01/H02 cerrados en sus alcances documentales; ADR-0019 sigue proposed, outbox aprobado aunque ADR-0012/CLAUDE conservan texto histórico, listas/tableros por definición vigente siguen pendientes. Pruebas locales aprobadas sin repetición; cobertura frontend adicional 80 % pendiente. 3.B3 pospuesto, fases abiertas. Richard ejecuta manualmente commit y decide cierres.

Comando propuesto, desde la raíz y únicamente después de revisar el índice:

```powershell
git commit -m "Implementar flujo B y verificar recorridos A/B de fase 6"
```

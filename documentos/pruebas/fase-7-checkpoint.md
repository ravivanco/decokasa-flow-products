# Checkpoint de fase 7 para continuar en otro PC

**Trabajo bloqueado, no aprobado.** Rama `fase-7-colombia-ia`. 7.B1 sigue pendiente por F7-B1-H01 (colisión de tipos Resultado: nueve errores de compilación) y F7-B1-H02 (Colombia conserva `pais.activo=false`). No se corrigen esos defectos ni se repiten pruebas al preparar este checkpoint.

[Informe y reproductores](fase-7-informe.md). Evidencia existente: frontend 335/335, lint/build aprobados y cobertura de líneas 67,60 %; SQL 108/110, dos aserciones del mismo indicador de Colombia. Check backend fallido: sin cobertura backend actual ni recorridos reales CO/EC acreditados. Los resultados aprobados parciales no aprueban el checkpoint completo.

## Contenido exacto

[Selección normal](fase-7-seleccion-normal.txt), [logs ignorados revisados](fase-7-seleccion-ignorados.txt), [auditoría](fase-7-checkpoint-auditoria.json). Total: **220 rutas** (204 normales y 16 logs). Incluye:

- Implementación backend de países, permisos, bodegas, archivos, notificaciones, calendarios y módulo asistente completo, con sus defectos conservados para corregir después.
- Migraciones V19, V20 y V21; no se cambian migraciones anteriores.
- Frontend por país y asistente, pruebas nuevas y ajustes históricos de pruebas.
- Dependencia SDK en Gradle, configuración de despliegue y `.env.example` con claves vacías e IA desactivada; ninguna credencial real.
- Plan/PRD, contrato, requisitos, arquitectura, modelo, ADR-0020–0024 e índice, CLAUDE.md y reglas de exclusión.
- Informe, logs originales, JUnit XML de SQL, resúmenes de cobertura, diagnóstico, snapshot de conservación y arnés SQL independiente. Se conservan los intentos fallidos necesarios para explicar los reproductores.

No se incluyen Header.tsx, otras fases pendientes ajenas, .vscode, builds/dist/node_modules, resultados binarios JUnit, entornos virtuales, cachés, scripts transitorios de ajuste (`ajustar-sql.py`), `.tmp`, .env locales, claves, tokens, archivos OAuth ni PDFs/manifiesto privados del piloto. Los datos ficticios locales del arnés no son necesarios para reconstruir los fixtures que sí están en las pruebas. El manual DOCX ya está rastreado previamente; no se duplica.

**Header.tsx queda separado y NO viajará con este commit.** Su cambio local sigue en el directorio de trabajo, sin preparar ni descartar. Las pruebas frontend archivadas se ejecutaron con ese archivo local presente; el otro PC recibirá la versión ya confirmada de Header.tsx. Esa diferencia se conserva explícita, sin repetir pruebas por esta preparación.

## Guardas y revisión

Rama comprobada e índice inicialmente vacío. Preparar solo las dos listas y contrastar igualdad exacta con el índice. `git add -f` se limita a los 16 logs revisados, ignorados por `*.log`; no a secretos, credenciales, carpetas ni otras evidencias.

Revisión acotada: patrones de claves privadas, Google OAuth/tokens, claves Anthropic, JWT literales, contraseñas en URL y asignaciones no vacías de secretos en ejemplos. Sin coincidencias; `.env.example` tiene la clave vacía y no contiene un secreto. Ningún seleccionado supera 10 MiB. Los nombres de variables, rutas locales de ejecución y decisiones documentadas no se confunden con credenciales. Enlaces del informe/checkpoint deben existir y su destino estar seleccionado o rastreado previamente. El script de inventario no escribe Git ni arregla código.

Los avisos de `git diff --cached --check` y el resultado de `--stat` se registran en la auditoría al preparar. No se alteran los logs o XML originales para eliminar whitespace.

Resultado de preparación: **220 rutas y coincidencia exacta con la selección**, sin Header.tsx ni archivos ajenos en el índice. Scan acotado de patrones de secretos también sobre el contenido preparado: sin coincidencias. `--stat` termina en 0; `--check` termina en **2 con 12 avisos de espacios finales** (dos en el plan y diez en XML de evidencia). No es un check limpio. Git también avisa de futura conversión LF→CRLF. Se conservan los originales; la auditoría contiene la primera captura y se repite la comprobación tras incorporar esta anotación.

## Continuar en otro PC

El índice solo prepara el commit local. El contenido no estará disponible en el otro PC hasta que Richard haga el commit y lo transfiera por su procedimiento habitual; aquí no se hace commit, push ni merge.

En el otro PC: obtener esa rama/checkpoint, leer primero este documento y el informe, reconstruir dependencias con los gestores del proyecto (Java 21/Gradle, Node/npm y Docker/PostgreSQL de tests). No copiar node_modules, caché de Gradle, base compartida ni volúmenes del equipo anterior. El validador OpenAPI temporal no viaja: se utilizó `openapi-spec-validator` 0.9.0 en un venv de documentos/.tmp.

Mantener IA, OAuth y automatismos desactivados, sin secretos por Git y sin llamadas reales. Corregir los dos defectos de producción mediante una solicitud posterior y después revalidar 7.B1, incluyendo API real, aislamiento y recorridos hasta Fin que quedaron bloqueados. No presentar el SQL independiente o MSW como sustituto de la API.

Siguen pendientes 7.B2, cobertura backend actual, objetivo adicional frontend 80 %, límite de listas/tableros con definición vigente, meta y demás decisiones abiertas. 3.B3 pospuesto; ninguna fase se cierra por guardar el checkpoint. ADR-0019 sigue proposed.

Comando para Richard, desde la raíz tras revisar el índice:

```powershell
git commit -m "Guardar checkpoint bloqueado de fase 7: Colombia e IA"
```

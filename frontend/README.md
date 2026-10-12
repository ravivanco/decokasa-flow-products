# Frontend de Decokasa

React 19 + Vite + TypeScript + Tailwind 4 + shadcn/ui, con React Router, TanStack Query, React Hook Form y Zod. Las reglas del proyecto están en `../CLAUDE.md` y el sistema de diseño en `../documentos/diseno/DESIGN.md`.

## Comandos

Requiere Node.js 24.

```bash
npm install
npm run dev        # http://localhost:3000; /api se envía al backend en http://localhost:8080
npm run lint       # tipos con tsc (strict)
npm run test       # pruebas con Vitest
npm run build      # compilación de producción en dist/
npm run clean      # borra dist/ (funciona en PowerShell y en Bash)
```

## Configuración

Copia `.env.example` a `.env.local` solo si la API no está en la dirección por defecto. El frontend nunca guarda claves: las de Google y Claude viven solo en el backend.

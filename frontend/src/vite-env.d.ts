/// <reference types="vite/client" />

// Variables públicas del frontend (frontend/.env.example). Terminan en el código del navegador: nunca secretos.
interface ImportMetaEnv {
  /** Base completa de la API, incluida la versión. Vacía o sin definir: `/api/v1` en el mismo origen. */
  readonly VITE_API_URL?: string;
  /** Acceso local sin login; solo tiene efecto durante `npm run dev` y nunca en un build de producción. */
  readonly VITE_ACCESO_LOCAL?: string;
}

interface ImportMeta {
  readonly env: ImportMetaEnv;
}

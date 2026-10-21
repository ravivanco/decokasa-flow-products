/// <reference types="vite/client" />

// Variables públicas del frontend (frontend/.env.example). Terminan en el código del navegador: nunca secretos.
interface ImportMetaEnv {
  /** Base completa de la API, incluida la versión. Vacía o sin definir: `/api/v1` en el mismo origen. */
  readonly VITE_API_URL?: string;
  /** Acceso local sin login; solo tiene efecto durante `npm run dev` y nunca en un build de producción. */
  readonly VITE_ACCESO_LOCAL?: string;
  /** Intervalo de actualización de las pantallas de seguimiento en ms (paso 5.4): 10 000–300 000; 30 000 por defecto. */
  readonly VITE_ACTUALIZACION_MS?: string;
}

interface ImportMeta {
  readonly env: ImportMetaEnv;
}

import './lib/configurarZod';
import { QueryClient, QueryClientProvider } from '@tanstack/react-query';
import { StrictMode } from 'react';
import { createRoot } from 'react-dom/client';
import { createBrowserRouter, RouterProvider } from 'react-router';
// Fuentes servidas por el propio servidor, sin Google Fonts (DESIGN.md 4): Inter para la interfaz, Montserrat para títulos.
import '@fontsource/inter/400.css';
import '@fontsource/inter/500.css';
import '@fontsource/inter/600.css';
import '@fontsource/montserrat/600.css';
import '@fontsource/montserrat/700.css';
import { CODIGO_RESPUESTA_INVALIDA, esErrorApi } from './api/cliente';
import { SesionProvider } from './auth/SesionProvider';
import { Toaster } from './components/ui/sonner';
import { rutas } from './rutas';
import './index.css';

/**
 * Las consultas reintentan hasta dos veces solo lo que puede pasar solo: sin red o error del servidor (5xx). Nunca un
 * 4xx (la respuesta no cambiará) ni una respuesta fuera del contrato (es un defecto). Las mutaciones no se reintentan
 * solas: un 409 lo resuelve la persona y repetir una escritura sin pedirlo podría duplicarla.
 */
function reintentarConsulta(intentos: number, error: unknown): boolean {
  if (intentos >= 2) return false;
  if (!esErrorApi(error)) return false;
  if (error.codigo === CODIGO_RESPUESTA_INVALIDA) return false;
  return error.status === 0 || error.status >= 500;
}

const consultas = new QueryClient({
  defaultOptions: {
    queries: {
      staleTime: 30_000,
      retry: reintentarConsulta,
    },
    mutations: { retry: false },
  },
});

const router = createBrowserRouter(rutas);

createRoot(document.getElementById('root')!).render(
  <StrictMode>
    <QueryClientProvider client={consultas}>
      <SesionProvider>
        <RouterProvider router={router} />
      </SesionProvider>
      <Toaster />
    </QueryClientProvider>
  </StrictMode>,
);

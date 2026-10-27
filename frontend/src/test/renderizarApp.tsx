import { QueryClient, QueryClientProvider } from '@tanstack/react-query';
import { render, screen, waitFor } from '@testing-library/react';
import userEvent from '@testing-library/user-event';
import { createMemoryRouter, RouterProvider } from 'react-router';
import { SesionProvider } from '@/auth/SesionProvider';
import { rutas } from '@/rutas';
import { CLAVE, sesionDe, sesionGuardada, type UsuarioApi } from './apiFicticia';
import { servidor } from './servidor';

// Monta la aplicación real (rutas, sesión y TanStack Query) en una ruta inicial, igual que main.tsx (plan, tarea 19).
export function renderizarApp(rutaInicial: string) {
  const router = createMemoryRouter(rutas, { initialEntries: [rutaInicial] });
  const consultas = new QueryClient({ defaultOptions: { queries: { retry: false }, mutations: { retry: false } } });
  const usuario = userEvent.setup();
  render(
    <QueryClientProvider client={consultas}>
      <SesionProvider>
        <RouterProvider router={router} />
      </SesionProvider>
    </QueryClientProvider>,
  );
  return { router, usuario, consultas };
}

/**
 * Abre la ruta pedida con una sesión ya abierta: la cookie de renovación es válida y la aplicación recupera la
 * sesión al cargar (D3b), sin pasar por el login. Devuelve también cuántas veces se llamó a /auth/renovar.
 */
export function abrirConSesion(usuarioApi: UsuarioApi, ruta: string) {
  const sesion = sesionGuardada(usuarioApi);
  servidor.use(...sesion.manejadores);
  return { ...renderizarApp(ruta), renovaciones: sesion.registro };
}

/** Abre la ruta pedida, que sin sesión lleva al login, e inicia sesión como el usuario ficticio dado. */
export async function entrarComo(usuarioApi: UsuarioApi, rutaInicial = '/login') {
  servidor.use(...sesionDe(usuarioApi));
  const app = renderizarApp(rutaInicial);
  await app.usuario.type(await screen.findByLabelText('Correo, usuario o teléfono'), usuarioApi.correo);
  await app.usuario.type(screen.getByLabelText('Contraseña'), CLAVE);
  await app.usuario.click(screen.getByRole('button', { name: 'Ingresar' }));
  await waitFor(() => expect(app.router.state.location.pathname).not.toBe('/login'));
  return app;
}

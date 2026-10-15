import { screen, waitFor } from '@testing-library/react';
import { describe, expect, it } from 'vitest';
import { admin, ana, catalogos, eva, mario } from '@/test/apiFicticia';
import { abrirConSesion, entrarComo, renderizarApp } from '@/test/renderizarApp';
import { servidor } from '@/test/servidor';

// Rutas protegidas por sesión y por rol (plan, tareas 14 y 19). La protección real está en el servidor (R6);
// aquí se comprueba que la pantalla no muestre lo que el rol no puede usar. Datos ficticios.
describe('Rutas protegidas', () => {
  it('sin sesión, /usuarios lleva al login', async () => {
    const app = renderizarApp('/usuarios');

    await waitFor(() => expect(app.router.state.location.pathname).toBe('/login'));
    expect(await screen.findByLabelText('Correo, usuario o teléfono')).toBeInTheDocument();
  });

  it('sin sesión, /inicio también lleva al login', async () => {
    const app = renderizarApp('/inicio');

    await waitFor(() => expect(app.router.state.location.pathname).toBe('/login'));
  });

  it('después de iniciar sesión vuelve a la página que se pidió', async () => {
    servidor.use(...catalogos());

    const app = await entrarComo(admin, '/usuarios');

    await waitFor(() => expect(app.router.state.location.pathname).toBe('/usuarios'));
    expect(await screen.findByRole('heading', { name: 'Usuarios' })).toBeInTheDocument();
  });

  it('con el rol admin, /usuarios muestra la gestión', async () => {
    servidor.use(...catalogos());

    abrirConSesion(admin, '/usuarios');

    expect(await screen.findByRole('heading', { name: 'Usuarios' })).toBeInTheDocument();
  });

  it('sin el rol admin, /usuarios muestra "Sin permiso" y no la gestión', async () => {
    servidor.use(...catalogos());

    const app = abrirConSesion(mario, '/usuarios');

    expect(await screen.findByRole('heading', { name: 'Sin permiso' })).toBeInTheDocument();
    expect(app.router.state.location.pathname).toBe('/usuarios');
    expect(screen.queryByRole('heading', { name: 'Usuarios' })).not.toBeInTheDocument();
    expect(screen.queryByRole('button', { name: 'Nuevo usuario' })).not.toBeInTheDocument();
  });

  it('sin el rol admin, /roles también muestra "Sin permiso"', async () => {
    servidor.use(...catalogos());

    abrirConSesion(mario, '/roles');

    expect(await screen.findByRole('heading', { name: 'Sin permiso' })).toBeInTheDocument();
  });

  it('con varios roles que incluyen admin entra a /usuarios', async () => {
    servidor.use(...catalogos());

    abrirConSesion(eva, '/usuarios');

    expect(await screen.findByRole('heading', { name: 'Usuarios' })).toBeInTheDocument();
  });

  it('con varios roles sin admin no entra a /usuarios', async () => {
    servidor.use(...catalogos());

    abrirConSesion(ana, '/usuarios');

    expect(await screen.findByRole('heading', { name: 'Sin permiso' })).toBeInTheDocument();
  });
});

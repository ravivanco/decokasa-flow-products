import { screen, within } from '@testing-library/react';
import { describe, expect, it } from 'vitest';
import { admin, ana, catalogos } from '@/test/apiFicticia';
import { abrirConSesion } from '@/test/renderizarApp';
import { servidor } from '@/test/servidor';

// Diseño de la aplicación con sesión (plan, tareas 19 y 21): encabezado con la marca, el usuario y "Cerrar sesión";
// menú según el rol; sin cambio de usuario ni de país sin autenticación (auditoría S2). Datos ficticios.
describe('App', () => {
  it('con sesión muestra la marca, el nombre del usuario y "Cerrar sesión"', async () => {
    abrirConSesion(ana, '/inicio');

    const encabezado = await screen.findByRole('banner');
    expect(within(encabezado).getAllByText(/LATINOAM\u00c9RICA/i).length).toBeGreaterThan(0);
    expect(within(encabezado).getByText('Ana Prueba')).toBeInTheDocument();
    expect(within(encabezado).getByRole('button', { name: 'Cerrar sesión' })).toBeInTheDocument();
  });

  // Paso 2.5: el menú suma Mi bandeja y Pedidos para quien tiene sesión; Usuarios y Roles siguen siendo solo del admin.
  it('el menú de un usuario sin admin ofrece Inicio, Mi bandeja y Pedidos, sin Usuarios ni Roles', async () => {
    abrirConSesion(ana, '/inicio');

    const menu = await screen.findByRole('navigation', { name: 'Menú principal' });
    expect(within(menu).getByRole('link', { name: 'Inicio' })).toHaveAttribute('aria-current', 'page');
    expect(within(menu).getByRole('link', { name: 'Mi bandeja' })).toHaveAttribute('href', '/bandeja');
    expect(within(menu).getByRole('link', { name: 'Pedidos' })).toHaveAttribute('href', '/pedidos');
    expect(within(menu).queryByRole('link', { name: 'Usuarios' })).not.toBeInTheDocument();
    expect(within(menu).queryByRole('link', { name: 'Roles' })).not.toBeInTheDocument();
  });

  it.each([['consulta'], ['asistente_china'], ['consulta', 'asistente_china']])(
    'con solo roles de consulta (%s) el menú no ofrece Mi bandeja, pero sí Pedidos',
    async (...roles) => {
      abrirConSesion({ ...ana, roles }, '/inicio');

      const menu = await screen.findByRole('navigation', { name: 'Menú principal' });
      expect(within(menu).queryByRole('link', { name: 'Mi bandeja' })).not.toBeInTheDocument();
      expect(within(menu).getByRole('link', { name: 'Pedidos' })).toBeInTheDocument();
    },
  );

  it('un rol de consulta no quita la bandeja que da otro rol', async () => {
    abrirConSesion({ ...ana, roles: ['consulta', 'marketing'] }, '/inicio');

    const menu = await screen.findByRole('navigation', { name: 'Menú principal' });
    expect(within(menu).getByRole('link', { name: 'Mi bandeja' })).toBeInTheDocument();
  });

  it('el menú del admin ofrece Usuarios y Roles', async () => {
    servidor.use(...catalogos());

    abrirConSesion(admin, '/inicio');

    const menu = await screen.findByRole('navigation', { name: 'Menú principal' });
    expect(within(menu).getByRole('link', { name: 'Usuarios' })).toHaveAttribute('href', '/usuarios');
    expect(within(menu).getByRole('link', { name: 'Roles' })).toHaveAttribute('href', '/roles');
  });

  it('no ofrece cambiar de usuario, de rol ni de país sin autenticación', async () => {
    abrirConSesion(ana, '/inicio');

    await screen.findByRole('banner');
    expect(screen.queryByText(/Simulador de Roles/i)).not.toBeInTheDocument();
    expect(screen.queryByRole('button', { name: /cambiar (de )?(usuario|rol|país)/i })).not.toBeInTheDocument();
  });
});

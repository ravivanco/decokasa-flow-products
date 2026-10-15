import { screen } from '@testing-library/react';
import { describe, expect, it } from 'vitest';
import { ana, type UsuarioApi } from '@/test/apiFicticia';
import { abrirConSesion } from '@/test/renderizarApp';

// Dashboard vacío por rol (plan, tarea 24; D7). Sin datos simulados: las bandejas llegan en la fase 2. Datos ficticios.
describe('Dashboard por rol', () => {
  const usuarioCon = (roles: string[]): UsuarioApi => ({ ...ana, roles });

  it.each(['consulta', 'asistente_china'])('%s muestra el alcance provisional sin acciones de gestión', async (rol) => {
    abrirConSesion(usuarioCon([rol]), '/inicio');
    await screen.findByRole('heading', { name: `Hola, ${ana.nombre}` });
    expect(screen.getByText('Provisional')).toBeInTheDocument();
    expect(screen.getByText(/Alcance provisional, pendiente de confirmar con el cliente/)).toHaveTextContent(
      'solo consulta de pedidos, sin acciones ni permisos de gestión');
    expect(screen.queryByRole('link', { name: 'Usuarios' })).not.toBeInTheDocument();
    expect(screen.queryByRole('link', { name: 'Roles' })).not.toBeInTheDocument();
  });

  it.each([
    ['admin', 'Administración'],
    ['asistente_compras', 'Revisión'],
    ['marketing', 'Codificación'],
    ['compras_china', 'Compras China'],
    ['logistica', 'Logística'],
    ['asistente_china', 'Consulta'],
    ['consulta', 'Consulta'],
    ['rol_creado_por_el_admin', 'Consulta'],
  ])('con el rol %s muestra el dashboard "%s" vacío', async (rol, titulo) => {
    abrirConSesion(usuarioCon([rol]), '/inicio');

    expect(await screen.findByRole('heading', { level: 1, name: `Hola, ${ana.nombre}` })).toBeInTheDocument();
    expect(screen.getByRole('heading', { level: 2, name: titulo })).toBeInTheDocument();
    expect(screen.getByText('Aún no hay pedidos; las bandejas llegan en la fase 2.')).toBeInTheDocument();
  });

  it('con varios roles muestra un dashboard por cada uno, sin repetir', async () => {
    abrirConSesion(usuarioCon(['compras_china', 'logistica', 'consulta', 'asistente_china']), '/inicio');

    await screen.findByRole('heading', { level: 1, name: `Hola, ${ana.nombre}` });
    const titulos = screen.getAllByRole('heading', { level: 2 }).map((titulo) => titulo.textContent);
    expect(titulos).toEqual(['Compras China', 'Logística', 'Consulta']);
  });

  it('/ lleva a /inicio', async () => {
    const app = abrirConSesion(usuarioCon(['logistica']), '/');

    expect(await screen.findByRole('heading', { level: 2, name: 'Logística' })).toBeInTheDocument();
    expect(app.router.state.location.pathname).toBe('/inicio');
  });

  it('una ruta que no existe muestra "Página no encontrada"', async () => {
    abrirConSesion(usuarioCon(['logistica']), '/pedidos');

    expect(await screen.findByRole('heading', { name: 'Página no encontrada' })).toBeInTheDocument();
  });
});

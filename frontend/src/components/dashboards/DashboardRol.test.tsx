import { screen, within } from '@testing-library/react';
import { beforeEach, describe, expect, it } from 'vitest';
import { http, HttpResponse } from 'msw';
import { servidor } from '@/test/servidor';
import { definicionFlujoA, pagina } from '@/test/pedidosFicticios';
import { ana, type UsuarioApi } from '@/test/apiFicticia';
import { abrirConSesion } from '@/test/renderizarApp';

// Dashboard por rol (plan, tareas 17 y 24; D7). Sin datos simulados: desde el paso 2.5 cada panel lleva a la bandeja
// real, y los paneles provisionales de consulta llevan a Pedidos (sin bandeja). Datos ficticios.
describe('Dashboard por rol', () => {
  // Los paneles de fase 5 ya leen la API; este fixture vacío solo delimita las pruebas de composición por rol.
  beforeEach(() => servidor.use(
    http.get('*/api/v1/pedidos/bandeja', () => HttpResponse.json(pagina([]))),
    http.get('*/api/v1/pedidos', () => HttpResponse.json(pagina([]))),
    http.get('*/api/v1/flujos/EC/A', () => HttpResponse.json(definicionFlujoA)),
  ));
  const usuarioCon = (roles: string[]): UsuarioApi => ({ ...ana, roles });

  it.each(['consulta', 'asistente_china'])('%s muestra el alcance provisional sin acciones de gestión', async (rol) => {
    abrirConSesion(usuarioCon([rol]), '/inicio');
    await screen.findByRole('heading', { name: `Hola, ${ana.nombre}` });
    expect(screen.getByText('Consulta · provisional')).toHaveAttribute('title',
      'Alcance provisional, pendiente de confirmar con el cliente: solo consulta de pedidos, sin acciones ni permisos de gestión.');
    expect(await screen.findByText('Pedidos creados')).toBeVisible();
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
  ])('con el rol %s muestra estadísticas y la etiqueta "%s" sin datos simulados', async (rol, titulo) => {
    abrirConSesion(usuarioCon([rol]), '/inicio');

    expect(await screen.findByRole('heading', { level: 1, name: `Hola, ${ana.nombre}` })).toBeInTheDocument();
    expect(within(screen.getByRole('list', { name: 'Mis roles' })).getByText(
      titulo === 'Consulta' ? 'Consulta · provisional' : titulo)).toBeVisible();
    expect(await screen.findByText('Pedidos creados')).toBeVisible();
    const provisional = titulo === 'Consulta';
    for (const enlace of screen.getAllByRole('link', { name: provisional ? 'Pedidos' : 'Mi bandeja' })) {
      expect(enlace).toHaveAttribute('href', provisional ? '/pedidos' : '/bandeja');
    }
    expect(screen.queryByText('Aún no hay pedidos; las bandejas llegan en la fase 2.')).not.toBeInTheDocument();
  });

  it('con varios roles muestra un Inicio estadístico y etiquetas sin repetir', async () => {
    abrirConSesion(usuarioCon(['compras_china', 'logistica', 'consulta', 'asistente_china']), '/inicio');

    await screen.findByRole('heading', { level: 1, name: `Hola, ${ana.nombre}` });
    const etiquetas = within(screen.getByRole('list', { name: 'Mis roles' })).getAllByRole('listitem');
    expect(etiquetas.map((etiqueta) => etiqueta.textContent)).toEqual(['Compras China', 'Logística', 'Consulta · provisional']);
    expect(screen.queryByRole('heading', { name: 'Mi trabajo' })).not.toBeInTheDocument();
    expect(await screen.findByText('Pedidos creados')).toBeVisible();
  });

  it('/ lleva a /inicio', async () => {
    const app = abrirConSesion(usuarioCon(['logistica']), '/');

    expect(await screen.findByText('Pedidos creados')).toBeVisible();
    expect(app.router.state.location.pathname).toBe('/inicio');
  });

  // /pedidos existe desde el paso 2.5: se usa otra ruta que no existe.
  it('una ruta que no existe muestra "Página no encontrada"', async () => {
    abrirConSesion(usuarioCon(['logistica']), '/ruta-que-no-existe');

    expect(await screen.findByRole('heading', { name: 'Página no encontrada' })).toBeInTheDocument();
  });
});

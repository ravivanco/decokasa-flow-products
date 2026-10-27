import '@/test/radix';
import { screen, waitFor, within } from '@testing-library/react';
import { http, HttpResponse } from 'msw';
import { beforeEach, describe, expect, it } from 'vitest';
import { admin, ana, problema } from '@/test/apiFicticia';
import { flujoAReal, flujoBReal } from '@/test/flujosFase6';
import { apiPedido, pagina, pedidoDetalle, resumenDe } from '@/test/pedidosFicticios';
import { abrirConSesion } from '@/test/renderizarApp';
import { servidor } from '@/test/servidor';

beforeEach(() => servidor.use(
  http.get('*/api/v1/flujos/EC/A', () => HttpResponse.json(flujoAReal)),
  http.get('*/api/v1/flujos/EC/B', () => HttpResponse.json(flujoBReal)),
));

function detalleB() {
  const original = pedidoDetalle();
  return { ...original, tipoFlujo: 'B', lineaDeTiempo: flujoBReal.etapas.map((etapa) => ({
    ...original.lineaDeTiempo[0], numero: etapa.numero, nombre: etapa.nombre,
    rolResponsable: etapa.rolResponsable, estadoPlazo: etapa.estadoPlazo,
    estado: etapa.numero === 1 ? 'ACTUAL' : 'PENDIENTE',
    entradaId: etapa.numero === 1 ? original.lineaDeTiempo[0].entradaId : null,
  })) };
}

describe('Fase 6: creación y definiciones separadas, API simulada explícita', () => {
  it('Compras China crea B y conserva el control del servidor', async () => {
    const api = apiPedido(detalleB());
    const cuerpos: unknown[] = [];
    servidor.use(...api.manejadores, http.post('*/api/v1/pedidos', async ({ request }) => {
      cuerpos.push(await request.json()); return HttpResponse.json(api.estado.detalle, { status: 201 });
    }));
    const app = abrirConSesion({ ...ana, roles: ['compras_china'] }, '/pedidos/nuevo');
    await app.usuario.type(await screen.findByLabelText('Nombre del pedido'), 'Propuesta ficticia');
    expect(screen.queryByLabelText('Incluye productos nuevos además de los existentes')).not.toBeInTheDocument();
    await app.usuario.click(screen.getByRole('button', { name: 'Crear pedido propuesto' }));
    await waitFor(() => expect(app.router.state.location.pathname).toBe(`/pedidos/${api.estado.detalle.id}`));
    expect(cuerpos).toEqual([{ tipoFlujo: 'B', nombre: 'Propuesta ficticia', incluyeProductosNuevos: false, pais: 'EC' }]);
  });

  it('admin y Compras China deben elegir el flujo sin valor predeterminado', async () => {
    let envios = 0;
    servidor.use(http.post('*/api/v1/pedidos', () => { envios++; return problema(403, 'SIN_PERMISO', 'Permiso retirado.'); }));
    const app = abrirConSesion({ ...admin, roles: ['admin', 'compras_china'] }, '/pedidos/nuevo');
    await app.usuario.type(await screen.findByLabelText('Nombre del pedido'), 'Borrador que conservar');
    expect(screen.getAllByRole('radio').every((radio) => radio.getAttribute('aria-checked') === 'false')).toBe(true);
    await app.usuario.click(screen.getByRole('button', { name: 'Crear pedido' }));
    expect(await screen.findByText('Elige qué pedido vas a crear.')).toBeInTheDocument();
    expect(envios).toBe(0);
    await app.usuario.click(screen.getByRole('radio', { name: /Crear pedido propuesto/ }));
    await app.usuario.click(screen.getByRole('button', { name: 'Crear pedido propuesto' }));
    expect(await screen.findByText(/Tu cuenta ya no tiene permiso/)).toBeInTheDocument();
    expect(screen.getByLabelText('Nombre del pedido')).toHaveValue('Borrador que conservar');
    expect(app.router.state.location.pathname).toBe('/pedidos/nuevo');
    expect(envios).toBe(1);
  });

  it('admin sin China solo tiene acceso al formulario A', async () => {
    abrirConSesion(admin, '/pedidos/nuevo?flujo=B');
    await screen.findByLabelText('Nombre del pedido');
    expect(screen.getByRole('heading', { name: 'Nuevo pedido' })).toBeInTheDocument();
    expect(screen.queryByRole('radio', { name: /Crear pedido propuesto/ })).not.toBeInTheDocument();
    expect(screen.getByLabelText('Incluye productos nuevos además de los existentes')).toBeInTheDocument();
  });

  it('bandeja no mezcla A3 Revisión con B3 Codificación', async () => {
    const a = resumenDe(pedidoDetalle({ etapaActual: 3 }));
    const b = { ...a, id: '0193f0a0-0000-7000-8000-000000000002', numero: 'DK-EC-2026-0002', tipoFlujo: 'B', nombreEtapaActual: 'Codificación' };
    servidor.use(http.get('*/api/v1/pedidos/bandeja', () => HttpResponse.json(pagina([a, b]))));
    abrirConSesion({ ...ana, roles: ['marketing', 'asistente_compras'] }, '/bandeja');
    const revision = await screen.findByRole('heading', { name: 'A3 · Revisión y aprobación' });
    const codificacion = await screen.findByRole('heading', { name: 'B3 · Codificación' });
    expect(within(revision.closest('section')!).getByRole('link', { name: a.numero })).toBeInTheDocument();
    expect(within(codificacion.closest('section')!).getByRole('link', { name: b.numero })).toBeInTheDocument();
    expect(within(codificacion.closest('section')!).queryByRole('link', { name: a.numero })).not.toBeInTheDocument();
  });

  it('filtro B3 envía flujo B y etapa 3 al servidor y a la URL', async () => {
    const consultas: URLSearchParams[] = [];
    servidor.use(http.get('*/api/v1/pedidos', ({ request }) => {
      consultas.push(new URL(request.url).searchParams); return HttpResponse.json(pagina([]));
    }));
    const app = abrirConSesion({ ...ana, roles: ['consulta'] }, '/pedidos');
    await app.usuario.click(await screen.findByLabelText('Flujo y etapa actual'));
    await app.usuario.click(screen.getByRole('option', { name: 'B3 · Codificación' }));
    await waitFor(() => expect(consultas.at(-1)?.get('tipoFlujo')).toBe('B'));
    expect(consultas.at(-1)?.get('etapa')).toBe('3');
    expect(app.router.state.location.search).toContain('flujo=B');
    expect(app.router.state.location.search).toContain('etapa=3');
  });
});

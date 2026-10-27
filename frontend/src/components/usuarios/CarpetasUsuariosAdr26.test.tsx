import { screen, waitFor, within } from '@testing-library/react';
import { http, HttpResponse } from 'msw';
import { afterEach, describe, expect, it, vi } from 'vitest';
import { admin, mario, problema } from '@/test/apiFicticia';
import { apiPedido, PEDIDO_ID, pedidoDetalle } from '@/test/pedidosFicticios';
import { abrirConSesion } from '@/test/renderizarApp';
import { servidor } from '@/test/servidor';
import { MENSAJE_SIN_PERMISO } from '@/api/mensajes';
// ADR-0027 sustituye la programación global por usuario; conserva las garantías por pedido.
const avisos = vi.hoisted(() => ({ exito: vi.fn() }));
vi.mock('sonner', async (original) => ({ ...(await original<typeof import('sonner')>()),
  toast: Object.assign(vi.fn(), { success: avisos.exito, error: vi.fn() }) }));
afterEach(() => vi.clearAllMocks());
const url = '*/api/v1/pedidos/' + PEDIDO_ID + '/carpeta-drive';
const estado = (valor: string) => ({ pedidoId: PEDIDO_ID, estado: valor,
  intentos: valor === 'FALLIDA' ? 4 : 0, ultimoError: valor === 'FALLIDA' ? 'CUOTA' : null,
  programadaEn: valor === 'SIN_PROGRAMAR' ? null : '2026-10-26T15:00:00Z',
  creadaEn: valor === 'CREADA' ? '2026-10-26T15:00:01Z' : null });
function preparar(valor: string) {
  servidor.use(...apiPedido(pedidoDetalle()).manejadores);
  servidor.use(http.get(url, () => HttpResponse.json(estado(valor))));
}
describe('Carpetas por pedido ADR-0027, HTTP simulado', () => {
  it.each([['SIN_PROGRAMAR','Sin programar'], ['PENDIENTE','Pendiente'], ['FALLIDA','Fallida'], ['CREADA','Creada']])(
    'muestra %s sin anunciar creación anticipada ni enlaces Google', async (valor, texto) => {
      preparar(valor); abrirConSesion(admin, '/pedidos/' + PEDIDO_ID);
      const region = await screen.findByRole('region', { name: 'Carpeta de Drive del pedido' });
      expect(await within(region).findByText(texto, { exact: true })).toBeVisible();
      expect(within(region).queryByRole('link')).not.toBeInTheDocument();
      expect(within(region).queryByRole('button', { name: /la carpeta/ }) !== null).toBe(['SIN_PROGRAMAR','FALLIDA'].includes(valor));
    });
  it.each([['SIN_PROGRAMAR','Programar'], ['FALLIDA','Reintentar']])(
    '%s programa una sola vez y relee PENDIENTE sin éxito inventado', async (valor, accion) => {
      preparar(valor); let envios = 0;
      servidor.use(http.post(url, () => {
        envios++; servidor.use(http.get(url, () => HttpResponse.json(estado('PENDIENTE'))));
        return HttpResponse.json(estado('PENDIENTE'), { status: 202 });
      }));
      const app = abrirConSesion(admin, '/pedidos/' + PEDIDO_ID);
      await app.usuario.click(await screen.findByRole('button', { name: accion + ' la carpeta' }));
      await waitFor(() => expect(avisos.exito).toHaveBeenCalledWith(expect.stringContaining('quedó pendiente')));
      expect(avisos.exito).not.toHaveBeenCalledWith(expect.stringContaining('ya está creada'));
      expect(envios).toBe(1);
      await waitFor(() => expect(screen.queryByRole('button', { name: accion + ' la carpeta' })).not.toBeInTheDocument());
    });
  it('respeta 403 con JWT admin anterior y conserva el estado', async () => {
    preparar('SIN_PROGRAMAR'); let envios = 0;
    servidor.use(http.post(url, () => { envios++; return problema(403, 'SIN_PERMISO', 'Permiso retirado.'); }));
    const app = abrirConSesion(admin, '/pedidos/' + PEDIDO_ID);
    await app.usuario.click(await screen.findByRole('button', { name: 'Programar la carpeta' }));
    expect(await screen.findByText(MENSAJE_SIN_PERMISO)).toBeVisible();
    expect(avisos.exito).not.toHaveBeenCalled(); expect(envios).toBe(1);
    expect(within(screen.getByRole('region', { name: 'Carpeta de Drive del pedido' })).getByText('Sin programar')).toBeVisible();
  });
  it('un error de consulta no inventa estado ni permite programar', async () => {
    preparar('SIN_PROGRAMAR');
    servidor.use(http.get(url, () => problema(503, 'SIN_CONEXION', 'Servicio ficticio no disponible.')));
    abrirConSesion(admin, '/pedidos/' + PEDIDO_ID);
    const region = await screen.findByRole('region', { name: 'Carpeta de Drive del pedido' });
    expect(await within(region).findByRole('alert')).toBeVisible();
    expect(within(region).queryByRole('button')).not.toBeInTheDocument();
    expect(within(region).queryByText('Sin programar')).not.toBeInTheDocument();
  });
  it('no admin no consulta ni programa carpetas', async () => {
    preparar('SIN_PROGRAMAR'); let lecturas = 0, escrituras = 0;
    servidor.use(http.get(url, () => { lecturas++; return HttpResponse.json(estado('CREADA')); }),
      http.post(url, () => { escrituras++; return HttpResponse.json({}); }));
    abrirConSesion(mario, '/pedidos/' + PEDIDO_ID);
    await screen.findByRole('heading', { name: 'Historial' });
    expect(screen.queryByRole('region', { name: 'Carpeta de Drive del pedido' })).not.toBeInTheDocument();
    expect(lecturas).toBe(0); expect(escrituras).toBe(0);
  });
});

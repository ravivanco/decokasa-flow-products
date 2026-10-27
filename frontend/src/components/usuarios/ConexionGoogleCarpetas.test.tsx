import { act, screen, waitFor, within } from '@testing-library/react';
import { http, HttpResponse } from 'msw';
import { afterEach, describe, expect, it, vi } from 'vitest';
import { admin, catalogos, problema } from '@/test/apiFicticia';
import { abrirConSesion } from '@/test/renderizarApp';
import { servidor } from '@/test/servidor';
import { apiPedido, PEDIDO_ID, pedidoDetalle } from '@/test/pedidosFicticios';

// Revalidación 2026-10-26 (ADR-0017 y ADR-0026): botón de conexión Google y refresco de carpetas pendientes. HTTP
// simulado con MSW; ninguna llamada real a Google.
const navegacion = vi.hoisted(() => ({ irA: vi.fn() }));
vi.mock('@/lib/navegacion', () => navegacion);
afterEach(() => {
  vi.clearAllMocks();
  vi.useRealTimers();
});

const estado = (usuarioId: string, valor: string) => ({
  usuarioId, estado: valor, intentos: valor === 'SIN_PROGRAMAR' ? 0 : 1, ultimoError: null,
  programadaEn: valor === 'SIN_PROGRAMAR' ? null : '2026-10-26T15:00:00Z',
  creadaEn: valor === 'CREADA' ? '2026-10-26T15:00:20Z' : null,
});

function conCarpetas(filas: () => unknown[]) {
  let lecturas = 0;
  // En llamadas separadas: el último use() tiene prioridad sobre el manejador de carpetas de catalogos().
  servidor.use(...catalogos());
  servidor.use(http.get('*/api/v1/usuarios/carpetas-drive', () => { lecturas++; return HttpResponse.json(filas()); }));
  return () => lecturas;
}

describe('Conexión con Google Drive desde Usuarios', () => {
  it('inicia el consentimiento y navega solo a accounts.google.com', async () => {
    conCarpetas(() => []);
    let inicios = 0;
    const destino = 'https://accounts.google.com/o/oauth2/v2/auth?client_id=ficticio&state=ficticio';
    servidor.use(http.post('*/api/v1/integraciones/google/oauth2/inicio', () => { inicios++; return HttpResponse.json({ urlAutorizacion: destino }); }));
    const app = abrirConSesion(admin, '/usuarios');
    await app.usuario.click(await screen.findByRole('button', { name: 'Conectar Google Drive' }));
    await waitFor(() => expect(navegacion.irA).toHaveBeenCalledWith(destino));
    expect(inicios).toBe(1);
  });

  it('no navega si el servidor devuelve otro destino', async () => {
    conCarpetas(() => []);
    servidor.use(http.post('*/api/v1/integraciones/google/oauth2/inicio', () =>
      HttpResponse.json({ urlAutorizacion: 'https://accounts.google.com.ejemplo.test/o/oauth2/auth' })));
    const app = abrirConSesion(admin, '/usuarios');
    await app.usuario.click(await screen.findByRole('button', { name: 'Conectar Google Drive' }));
    expect(await screen.findByText(/destino inesperado/)).toBeVisible();
    expect(navegacion.irA).not.toHaveBeenCalled();
  });

  it('con la integración apagada (404) lo explica y no navega', async () => {
    conCarpetas(() => []);
    servidor.use(http.post('*/api/v1/integraciones/google/oauth2/inicio', () => problema(404, 'NO_ENCONTRADO', 'No existe.')));
    const app = abrirConSesion(admin, '/usuarios');
    await app.usuario.click(await screen.findByRole('button', { name: 'Conectar Google Drive' }));
    expect(await screen.findByText('La conexión con Google no está habilitada en este servidor.')).toBeVisible();
    expect(navegacion.irA).not.toHaveBeenCalled();
  });

  it('respeta el 403 del servidor y no navega', async () => {
    conCarpetas(() => []);
    servidor.use(http.post('*/api/v1/integraciones/google/oauth2/inicio', () => problema(403, 'SIN_PERMISO', 'Sin permiso.')));
    const app = abrirConSesion(admin, '/usuarios');
    await app.usuario.click(await screen.findByRole('button', { name: 'Conectar Google Drive' }));
    await waitFor(() => expect(screen.getByRole('alert')).toBeVisible());
    expect(navegacion.irA).not.toHaveBeenCalled();
  });
});

describe('Refresco de carpetas de pedidos ADR-0027', () => {
  function preparar(valor: () => string) {
    let lecturas = 0;
    servidor.use(...apiPedido(pedidoDetalle()).manejadores);
    servidor.use(http.get('*/api/v1/pedidos/' + PEDIDO_ID + '/carpeta-drive', () => {
      lecturas++; const { usuarioId: _, ...fila } = estado(PEDIDO_ID, valor());
      return HttpResponse.json({ ...fila, pedidoId: PEDIDO_ID });
    }));
    return () => lecturas;
  }
  it('consulta pendientes cada 10 s y se detiene al quedar creada', async () => {
    vi.useFakeTimers({ shouldAdvanceTime: true }); let valor = 'PENDIENTE';
    const lecturas = preparar(() => valor);
    abrirConSesion(admin, '/pedidos/' + PEDIDO_ID);
    const region = await screen.findByRole('region', {name:'Carpeta de Drive del pedido'});
    expect(await within(region).findByText('Pendiente', {exact:true})).toBeVisible();
    const inicial = lecturas(); valor = 'CREADA';
    await act(async () => { await vi.advanceTimersByTimeAsync(10_000); });
    await waitFor(() => expect(within(region).getByText('Creada', {exact:true})).toBeVisible());
    expect(lecturas()).toBe(inicial + 1);
    await act(async () => { await vi.advanceTimersByTimeAsync(30_000); });
    expect(lecturas()).toBe(inicial + 1);
  });
  it('sin pendientes no consulta peri?dicamente', async () => {
    vi.useFakeTimers({ shouldAdvanceTime: true }); const lecturas = preparar(() => 'CREADA');
    abrirConSesion(admin, '/pedidos/' + PEDIDO_ID);
    const region = await screen.findByRole('region', {name:'Carpeta de Drive del pedido'});
    await within(region).findByText('Creada', {exact:true}); const inicial=lecturas();
    await act(async () => { await vi.advanceTimersByTimeAsync(30_000); });
    expect(lecturas()).toBe(inicial);
  });
});

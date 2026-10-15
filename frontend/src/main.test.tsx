import type { QueryClient } from '@tanstack/react-query';
import type { ReactElement } from 'react';
import { http, HttpResponse } from 'msw';
import { beforeAll, describe, expect, it, vi } from 'vitest';
import { z } from 'zod';
import { llamar } from '@/api/cliente';
import { problema } from '@/test/apiFicticia';
import { servidor } from '@/test/servidor';

const { pintar } = vi.hoisted(() => ({ pintar: vi.fn() }));
vi.mock('react-dom/client', () => ({ createRoot: () => ({ render: pintar }) }));
vi.mock('react-router', async (original) => ({ ...await original<typeof import('react-router')>(),
  createBrowserRouter: vi.fn(() => ({})), RouterProvider: () => null }));

describe('Política de consultas de main (configuración real)', () => {
  let cliente: QueryClient;
  beforeAll(async () => {
    await import('./main');
    const strict = pintar.mock.calls[0][0] as ReactElement<{ children: ReactElement<{ client: QueryClient }> }>;
    cliente = strict.props.children.props.client;
    const opciones = cliente.getDefaultOptions();
    cliente.setDefaultOptions({ ...opciones, queries: { ...opciones.queries, retryDelay: 0 } });
  });

  it.each([[0, 3], [503, 3], [403, 1], [409, 1], [200, 1]])(
    'estado %s hace exactamente %s intentos', async (status, cantidad) => {
      let llamadas = 0;
      servidor.use(http.get('*/api/v1/prueba-reintentos', () => {
        llamadas++;
        return status === 0 ? HttpResponse.error() : status === 200 ? HttpResponse.json({ roto: true })
          : problema(status, 'SIN_PERMISO', 'detalle interno');
      }));
      await expect(cliente.fetchQuery({ queryKey: ['reintentos', status],
        queryFn: () => llamar('/prueba-reintentos', { esquema: z.array(z.string()) }) })).rejects.toThrow();
      expect(llamadas).toBe(cantidad);
    });

  it('las mutaciones nunca se reintentan automáticamente ante 503', async () => {
    let llamadas = 0;
    servidor.use(http.post('*/api/v1/prueba-reintentos', () => {
      llamadas++;
      return problema(503, 'DESCONOCIDO', 'fallo');
    }));
    const mutacion = cliente.getMutationCache().build(cliente, {
      mutationFn: () => llamar('/prueba-reintentos', { metodo: 'POST' }),
    });
    await expect(mutacion.execute(undefined)).rejects.toThrow();
    expect(llamadas).toBe(1);
    cliente.clear();
  });
});

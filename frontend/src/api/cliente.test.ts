import { http, HttpResponse } from 'msw';
import { describe, expect, it, vi } from 'vitest';
import { z } from 'zod';
import { problema } from '@/test/apiFicticia';
import { servidor } from '@/test/servidor';
import { baseApi, ErrorApi, llamar } from './cliente';

// Cliente HTTP (plan, tarea 18): base de la API según VITE_API_URL, errores RFC 9457 como ErrorApi y cookies solo
// en las rutas de sesión (D3b). Datos ficticios.
describe('Cliente de la API', () => {
  it('convierte una caída de red en SIN_CONEXION sin filtrar detalles', async () => {
    servidor.use(http.get('*/api/v1/roles', () => HttpResponse.error()));
    await expect(llamar('/roles')).rejects.toMatchObject({ status: 0, codigo: 'SIN_CONEXION' });
  });

  it.each(['no-json', 'contrato'])('identifica una respuesta inválida: %s', async (tipo) => {
    servidor.use(http.get('*/api/v1/roles', () => tipo === 'no-json'
      ? new HttpResponse('<html>interno</html>') : HttpResponse.json({ codigo: 5 })));
    await expect(llamar('/roles', { esquema: z.array(z.object({ codigo: z.string() })) }))
      .rejects.toMatchObject({ status: 200, codigo: 'RESPUESTA_INVALIDA' });
  });

  it('sin VITE_API_URL usa /api/v1 en el mismo origen', () => {
    expect(baseApi(undefined)).toBe('/api/v1');
    expect(baseApi('')).toBe('/api/v1');
    expect(baseApi('   ')).toBe('/api/v1');
  });

  it('con VITE_API_URL usa esa base completa, sin la barra final', () => {
    expect(baseApi('https://api.ejemplo.test/api/v1/')).toBe('https://api.ejemplo.test/api/v1');
  });

  it('convierte un problema (application/problem+json) en un ErrorApi con su código y sus campos', async () => {
    servidor.use(
      http.post('*/api/v1/usuarios', () =>
        problema(409, 'DUPLICADO', 'Ya existe un usuario con ese correo.', [
          { campo: 'correo', mensaje: 'Ya existe un usuario con ese correo.' },
        ]),
      ),
    );

    const error = await llamar('/usuarios', { metodo: 'POST', cuerpo: {} }).catch((e: unknown) => e);

    expect(error).toBeInstanceOf(ErrorApi);
    expect(error).toMatchObject({
      status: 409,
      codigo: 'DUPLICADO',
      errores: [{ campo: 'correo', mensaje: 'Ya existe un usuario con ese correo.' }],
    });
  });

  it('un error sin cuerpo de problema sigue siendo un ErrorApi, sin detalles técnicos', async () => {
    servidor.use(http.get('*/api/v1/roles', () => new HttpResponse('<html>error</html>', { status: 502 })));

    const error = await llamar('/roles').catch((e: unknown) => e);

    expect(error).toMatchObject({ status: 502, codigo: 'DESCONOCIDO' });
    expect((error as Error).message).not.toContain('<html>');
  });

  it('valida la respuesta con su esquema y rechaza una forma distinta a la del contrato', async () => {
    servidor.use(http.get('*/api/v1/roles', () => HttpResponse.json([{ codigo: 123 }])));

    await expect(llamar('/roles', { esquema: z.array(z.object({ codigo: z.string() })) })).rejects.toThrow();
  });

  it('un 204 no tiene cuerpo', async () => {
    servidor.use(http.post('*/api/v1/auth/cerrar-sesion', () => new HttpResponse(null, { status: 204 })));

    await expect(llamar('/auth/cerrar-sesion', { metodo: 'POST', credenciales: true })).resolves.toBeUndefined();
  });

  it('envía las cookies solo cuando se pide (rutas de sesión) y el token en Authorization', async () => {
    // El fetch de Node no conserva `credentials` en la petición que ve MSW: se mira lo que recibe fetch.
    const llamadas = vi.spyOn(globalThis, 'fetch');
    servidor.use(http.all('*/api/v1/*', () => HttpResponse.json({})));

    await llamar('/auth/renovar', { metodo: 'POST', credenciales: true });
    await llamar('/usuarios', { token: 'token-de-prueba' });

    const vistos = llamadas.mock.calls.map(([, init]) => ({
      credenciales: init?.credentials,
      autorizacion: (init?.headers as Record<string, string> | undefined)?.Authorization ?? null,
    }));
    llamadas.mockRestore();
    expect(vistos[0]).toEqual({ credenciales: 'include', autorizacion: null });
    expect(vistos[1]).toEqual({ credenciales: 'same-origin', autorizacion: 'Bearer token-de-prueba' });
  });
});

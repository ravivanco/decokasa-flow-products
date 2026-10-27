import '@/test/radix';
import { screen, waitFor, within } from '@testing-library/react';
import { http, HttpResponse } from 'msw';
import { describe, expect, it } from 'vitest';
import { admin, ana, problema } from '@/test/apiFicticia';
import { abrirConSesion } from '@/test/renderizarApp';
import { servidor } from '@/test/servidor';

const base = '*/api/v1';
const id = '00000000-0000-4000-8000-000000000031';
function fila(cambios: Record<string, unknown> = {}) {
  return { id, pedidoId: id, eventoId: id, destinatario: null, asunto: 'Transicion ficticia B2',
    estado: 'PENDIENTE', creadaEn: '2026-10-19T10:00:00-05:00', enviadaEn: null,
    intentos: 0, ultimoError: null, resultadoIncierto: false, bloqueo: 'DESTINATARIO_PENDIENTE', ...cambios };
}
function api(elementos: unknown[], status = 200) {
  let consultas = 0;
  servidor.use(
    http.get(`${base}/notificaciones`, () => {
      consultas++;
      return status === 200 ? HttpResponse.json({ elementos, pagina: 0, tamano: 20, total: elementos.length })
        : problema(status, 'SIN_PERMISO', 'Acceso denegado');
    }),
    http.get(`${base}/archivos/copias-fallidas`, () => HttpResponse.json([])),
  );
  return () => consultas;
}

describe('3.B2 registro de correo con API ficticia, sin Google', () => {
  it('muestra la intencion bloqueada sin inventar destinatario ni botones de envio', async () => {
    api([fila()]);
    abrirConSesion(admin, '/notificaciones');
    expect(await screen.findByText('Transicion ficticia B2')).toBeVisible();
    expect(screen.getByText('Pendiente de aprobación')).toBeVisible();
    expect(screen.getByText('Bloqueado: falta destinatario aprobado.')).toBeVisible();
    expect(screen.queryByRole('button', { name: /^(enviar|reenviar|reintentar)/i })).not.toBeInTheDocument();
    expect(screen.queryByRole('link', { name: /drive/i })).not.toBeInTheDocument();
  });
  it('representa resultado incierto, destinatario e intentos persistidos sin exito inventado', async () => {
    api([fila({ destinatario: 'admin@ejemplo.test', bloqueo: null, estado: 'FALLIDA',
      resultadoIncierto: true, intentos: 1, ultimoError: 'RESULTADO_INCIERTO' })]);
    abrirConSesion(admin, '/notificaciones');
    const row = (await screen.findByText('admin@ejemplo.test')).closest('tr')!;
    expect(within(row).getByText('Resultado incierto')).toBeVisible();
    expect(within(row).getByText('RESULTADO_INCIERTO')).toBeVisible();
    expect(within(row).getByText('1')).toBeVisible();
    expect(within(row).queryByText('ENVIADA')).not.toBeInTheDocument();
  });
  it('muestra aceptacion simulada sin prometer recepcion ni lectura', async () => {
    api([fila({ destinatario: 'admin@ejemplo.test', bloqueo: null, estado: 'ENVIADA',
      enviadaEn: '2026-10-19T10:01:00-05:00', intentos: 1 })]);
    abrirConSesion(admin, '/notificaciones');
    expect(await screen.findByText('ENVIADA')).toBeVisible();
    expect(screen.getByText(/no confirma recepción ni lectura/)).toBeVisible();
  });
  it('impide al usuario no admin consultar el registro desde la ruta', async () => {
    const consultas = api([fila()]);
    const app = abrirConSesion(ana, '/notificaciones');
    await screen.findByRole('banner');
    expect(await screen.findByRole('heading', { name: /Sin permiso/i })).toBeVisible();
    expect(consultas()).toBe(0);
    expect(screen.queryByRole('link', { name: 'Correos' })).not.toBeInTheDocument();
  });
  it('respeta el rechazo del servidor aun cuando la sesion dice admin y no reintenta', async () => {
    const consultas = api([], 403);
    abrirConSesion(admin, '/notificaciones');
    await screen.findByText(/Tu cuenta ya no tiene permiso/);
    expect(consultas()).toBe(1);
    expect(screen.queryByText('Transicion ficticia B2')).not.toBeInTheDocument();
  });
  it('muestra el estado vacio sin filas exitosas de demostracion', async () => {
    api([]);
    abrirConSesion(admin, '/notificaciones');
    expect(await screen.findByText('No hay correos en este estado.')).toBeVisible();
    expect(screen.queryByText('ENVIADA')).not.toBeInTheDocument();
  });
});

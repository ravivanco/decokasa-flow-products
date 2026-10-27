import '@/test/radix';
import { screen, waitFor, within } from '@testing-library/react';
import { http, HttpResponse } from 'msw';
import { describe, expect, it } from 'vitest';
import { type Aviso } from '@/api/avisos';
import { admin, ana, problema } from '@/test/apiFicticia';
import { accion, apiPedido, definicionFlujoA, pagina, PEDIDO_ID, pedidoDetalle } from '@/test/pedidosFicticios';
import { abrirConSesion } from '@/test/renderizarApp';
import { servidor } from '@/test/servidor';

// Evidencia de componentes React + MSW. No es integración HTTP con el backend.
const HORA = '2026-10-27T15:00:00Z';
function ejemplos(n = 12): Aviso[] {
  return Array.from({ length: n }, (_, i) => ({
    eventoId: `00000000-0000-4000-8000-${String(i + 1).padStart(12, '0')}`,
    pedidoId: PEDIDO_ID, numero: `SIMULADO-${i + 1}`, pais: 'EC', tipoFlujo: i < 8 ? 'A' : 'B',
    tipo: (['CREACION', 'LLEGADA', 'DEVOLUCION', 'REASIGNACION', 'CIERRE'] as const)[i % 5],
    etapaOrigen: i === 0 ? null : 3, nombreOrigen: i === 0 ? null : 'Revisión',
    etapaDestino: i === 0 ? 1 : 2, nombreDestino: i === 0 ? 'Solicitud' : 'Codificación',
    actor: 'Persona ficticia', motivo: i % 5 === 2 ? 'Corregir código ficticio' : null,
    ocurridoEn: HORA, comoAdmin: true, comoResponsable: false, leido: false,
  }));
}
function preparar(inicial = ejemplos()) {
  let lista = inicial; let marca = '2026-10-26T15:00:00Z'; let lecturas = 0; let marcas = 0;
  const api = apiPedido(pedidoDetalle());
  servidor.use(...api.manejadores,
    http.get('*/api/v1/flujos/EC/A', () => HttpResponse.json(definicionFlujoA)),
    http.get('*/api/v1/pedidos/bandeja', () => HttpResponse.json(pagina([]))),
    http.get('*/api/v1/avisos', () => {
      lecturas++;
      return HttpResponse.json({ avisos: lista, noLeidos: lista.filter(a => !a.leido).length, leidasHasta: marca });
    }),
    http.post('*/api/v1/avisos/leidos', async ({ request }) => {
      expect(await request.text()).toBe(''); marcas++; marca = HORA;
      lista = lista.map(a => ({ ...a, leido: true }));
      return HttpResponse.json({ leidasHasta: marca });
    }),
  );
  return { api, set: (nueva: Aviso[]) => { lista = nueva; }, lecturas: () => lecturas, marcas: () => marcas };
}
const region = () => screen.getByRole('region', { name: /Avisos de etapa/ });
const campana = () => screen.getByLabelText(/^Notificaciones:/, { selector: 'button' });

describe('Avisos ADR-0032: campana y bandeja con API MSW', () => {
  it('12 ejemplos: contador suma no leídos y demoras; menú muestra los mismos primeros seis y enlaces', async () => {
    const datos = preparar();
    servidor.use(http.get('*/api/v1/alertas', () => HttpResponse.json([{
      pedidoId: PEDIDO_ID, numero: 'DEMORA-FICTICIA', nombrePedido: 'Prueba', pais: 'EC', tipoFlujo: 'A',
      etapa: 7, nombreEtapa: 'Aduana', tipo: 'DEMORA_REPORTADA', vence: null,
      nuevaFechaLlegada: HORA, motivo: 'Demora ficticia', asignadoA: null,
    }])));
    const app = abrirConSesion(ana, '/bandeja');
    await within(await screen.findByRole('region', { name: /Avisos de etapa/ })).findByText(/SIMULADO-12 /);
    expect(within(region()).getAllByRole('listitem')).toHaveLength(12);
    expect(campana()).toHaveAccessibleName('Notificaciones: 12 avisos nuevos y 1 alerta activa');
    expect(within(campana()).getByText('13')).toBeVisible();
    const titulos = within(region()).getAllByRole('link').map(a => a.textContent?.replace(' (nuevo)', ''));
    await app.usuario.click(campana());
    const menu = await screen.findByRole('menu');
    expect(within(menu).getByText('Avisos de etapa')).toBeVisible();
    expect(within(menu).getByText('Retrasos y demoras (1)')).toBeVisible();
    for (let i = 1; i <= 6; i++) {
      const enlace = within(menu).getByRole('menuitem', { name: new RegExp(`^SIMULADO-${i} `) });
      expect(enlace).toHaveAttribute('href', `/pedidos/${PEDIDO_ID}`);
      expect(titulos[i - 1]).toContain(`SIMULADO-${i}`);
    }
    expect(within(menu).queryByRole('menuitem', { name: /^SIMULADO-7 / })).not.toBeInTheDocument();
    expect(within(menu).getByRole('menuitem', { name: 'Ver los 12 avisos en Mi bandeja' })).toHaveAttribute('href', '/bandeja#avisos');
    await waitFor(() => expect(datos.marcas()).toBe(1));
  });

  it('abrir campana marca todos hasta la hora del servidor, conserva historial y deja las demoras', async () => {
    const datos = preparar(ejemplos(2)); const app = abrirConSesion(ana, '/bandeja');
    await screen.findByRole('button', { name: 'Marcar como leídos' });
    await app.usuario.click(campana());
    await waitFor(() => expect(campana()).toHaveAccessibleName('Notificaciones: 0 avisos nuevos y 0 alertas activas'));
    expect(within(await screen.findByRole('menu')).getAllByText(/\(nuevo\)/)).toHaveLength(2);
    await app.usuario.keyboard('{Escape}');
    expect(within(region()).getAllByRole('listitem')).toHaveLength(2);
    expect(screen.queryByRole('button', { name: 'Marcar como leídos' })).not.toBeInTheDocument();
    await app.usuario.click(campana());
    expect(within(await screen.findByRole('menu')).queryByText(/\(nuevo\)/)).not.toBeInTheDocument();
    expect(datos.marcas()).toBe(1);
  });

  it('botón en bandeja marca lectura sin eliminar elementos ni llamar por cada aviso', async () => {
    const datos = preparar(ejemplos(3)); const app = abrirConSesion(admin, '/bandeja');
    await app.usuario.click(await screen.findByRole('button', { name: 'Marcar como leídos' }));
    await waitFor(() => expect(campana()).toHaveAccessibleName('Notificaciones: 0 avisos nuevos y 0 alertas activas'));
    expect(within(region()).getAllByRole('listitem')).toHaveLength(3);expect(datos.marcas()).toBe(1);
    expect(within(region()).getByText('Corregir código ficticio')).toBeVisible();
  });

  it('vacío explícito en ambas superficies, contador cero y no marca leído', async () => {
    const datos = preparar([]); const app = abrirConSesion(ana, '/bandeja');
    await within(await screen.findByRole('region', { name: /Avisos de etapa/ })).findByText('No hay avisos de etapa en los últimos 30 días.');
    expect(campana()).toHaveAccessibleName('Notificaciones: 0 avisos nuevos y 0 alertas activas');
    await app.usuario.click(campana());
    expect(within(await screen.findByRole('menu')).getByText('No hay avisos de etapa en los últimos 30 días.')).toBeVisible();
    expect(datos.marcas()).toBe(0);
  });

  it('carga en bandeja y menú antes de recibir la respuesta', async () => {
    preparar([]); let liberar!: () => void; const espera = new Promise<void>(resolve => { liberar = resolve; });
    servidor.use(http.get('*/api/v1/avisos', async () => { await espera; return HttpResponse.json({ avisos: [], noLeidos: 0, leidasHasta: HORA }); }));
    const app = abrirConSesion(ana, '/bandeja');
    expect(await screen.findByLabelText('Cargando los avisos')).toHaveAttribute('aria-busy', 'true');
    await app.usuario.click(campana());
    expect(within(await screen.findByRole('menu')).getByText('Cargando…')).toBeVisible();
    liberar(); await within(screen.getByRole('menu')).findByText('No hay avisos de etapa en los últimos 30 días.');
  });

  it('errores de consulta visibles en bandeja y menú', async () => {
    preparar([]);servidor.use(http.get('*/api/v1/avisos', () => problema(503, 'ERROR_INTERNO', 'Ficticio')));
    const app = abrirConSesion(ana, '/bandeja');
    expect(await screen.findByText(/No se pudieron cargar los avisos\. Se reintentará/)).toBeVisible();
    await app.usuario.click(campana());
    expect(within(await screen.findByRole('menu')).getByText('No se pudieron cargar los avisos.')).toBeVisible();
  });

  it('refresca avisos tras ENVIAR con la mutación real del frontend, sin duplicarlos', async () => {
    const datos = preparar([]);datos.api.estado.detalle = pedidoDetalle({ etapaActual: 1, acciones: [accion('ENVIAR', 2)] });
    servidor.use(http.post(`${datos.api.base}/acciones`, async () => {
      datos.set(ejemplos(1));datos.api.estado.detalle = pedidoDetalle({ etapaActual: 2 });
      return HttpResponse.json(datos.api.estado.detalle);
    }));
    const app = abrirConSesion(admin, `/pedidos/${PEDIDO_ID}`);
    await app.usuario.click(await screen.findByRole('button', { name: 'Enviar' }));
    const dialogo = await screen.findByRole('dialog', { name: 'Enviar' });
    await app.usuario.click(within(dialogo).getByRole('button', { name: 'Enviar' }));
    await waitFor(() => expect(campana()).toHaveAccessibleName('Notificaciones: 1 aviso nuevo y 0 alertas activas'));
    await app.router.navigate('/bandeja');
    await within(await screen.findByRole('region', { name: /Avisos de etapa/ })).findByText(/SIMULADO-1 /);
    expect(within(region()).getAllByRole('listitem')).toHaveLength(1);
    await app.consultas.invalidateQueries({ predicate: q => q.queryKey.includes('avisos') });
    expect(within(region()).getAllByRole('listitem')).toHaveLength(1);expect(datos.lecturas()).toBeGreaterThan(1);
  });
});

import { screen, waitFor, within } from '@testing-library/react';
import { http, HttpResponse } from 'msw';
import { describe, expect, it } from 'vitest';
import { admin, ana, type UsuarioApi } from '@/test/apiFicticia';
import {
  accion,
  adjunto,
  apiPedido,
  type Detalle,
  entradaDe,
  PEDIDO_ID,
  pedidoDetalle,
} from '@/test/pedidosFicticios';
import { abrirConSesion } from '@/test/renderizarApp';
import { servidor } from '@/test/servidor';

// Acciones A1–A4 (paso 2.5): solo las de accionesDisponibles; observación obligatoria al rechazar y devolver;
// urgencia 4/12/24 h al devolver por producto nuevo; archivos de la entrada actual (P-18). La etapa solo cambia
// con la respuesta del servidor, las mutaciones no se reintentan y los errores del contrato se muestran. Datos
// ficticios.

const ruta = `/pedidos/${PEDIDO_ID}`;

function problemaJson(status: number, codigo: string, extra: Record<string, unknown> = {}) {
  return HttpResponse.json(
    { title: codigo, status, detail: `Detalle técnico ${codigo}`, codigo, ...extra },
    { status, headers: { 'Content-Type': 'application/problem+json' } },
  );
}

/** Registra POST /acciones con la respuesta dada y devuelve los cuerpos enviados. */
function acciones(api: ReturnType<typeof apiPedido>, responder: (cuerpo: Record<string, unknown>) => Response) {
  const cuerpos: Record<string, unknown>[] = [];
  servidor.use(
    http.post(`${api.base}/acciones`, async ({ request }) => {
      const cuerpo = (await request.json()) as Record<string, unknown>;
      cuerpos.push(cuerpo);
      api.registrar('POST', '/acciones', cuerpo);
      return responder(cuerpo);
    }),
  );
  return cuerpos;
}

async function abrir(usuario: UsuarioApi, detalle: Detalle, adjuntos: Record<number, unknown[]> = {}) {
  const api = apiPedido(detalle, { adjuntos });
  servidor.use(...api.manejadores);
  const app = abrirConSesion(usuario, ruta);
  await screen.findByRole('heading', { level: 2, name: new RegExp(`A${detalle.etapaActual} ·`) });
  return { api, app };
}

// Con un diálogo modal abierto, el resto de la página queda oculto para la accesibilidad (aria-hidden): se busca
// también ahí para comprobar que la etapa no cambió mientras el diálogo sigue abierto.
const etapaActual = (codigo: string) =>
  screen.findByRole('button', { name: new RegExp(`^${codigo} .*, etapa actual$`), hidden: true });

describe('Acciones de la etapa actual', () => {
  it('ENVIAR en A1 presenta solo los archivos de la entrada actual y muestra el pedido que devuelve el servidor', async () => {
    const enA1 = pedidoDetalle({
      etapaActual: 1,
      acciones: [accion('ENVIAR', 2, { documentosObligatorios: ['Solicitud'] })],
    });
    const archivos = {
      1: [
        adjunto('0193f0a0-0000-7000-b000-000000000001', 1, entradaDe(1), 'Solicitud', 'solicitud-actual.pdf'),
        adjunto('0193f0a0-0000-7000-b000-000000000002', 1, entradaDe(1, 1), 'Solicitud', 'solicitud-vieja.pdf'),
      ],
    };
    const { api, app } = await abrir(admin, enA1, archivos);
    const cuerpos = acciones(api, () => {
      api.estado.detalle = pedidoDetalle({ etapaActual: 2 });
      return HttpResponse.json(api.estado.detalle);
    });
    const historialAntes = api.cuenta('GET', '/historial');

    // Aparece en la lista de archivos y en el formulario de observación.
    await screen.findAllByText('solicitud-actual.pdf');
    await app.usuario.click(screen.getByRole('button', { name: 'Enviar' }));
    const dialogo = await screen.findByRole('dialog', { name: 'Enviar' });
    expect(within(dialogo).getByText('solicitud-actual.pdf')).toBeInTheDocument();
    expect(within(dialogo).queryByText('solicitud-vieja.pdf')).not.toBeInTheDocument();
    expect(within(dialogo).getByText('· adjunto')).toBeInTheDocument();
    await app.usuario.click(within(dialogo).getByRole('button', { name: 'Enviar' }));

    await waitFor(() => expect(screen.queryByRole('dialog')).not.toBeInTheDocument());
    expect(cuerpos).toEqual([
      { accion: 'ENVIAR', etapaEsperada: 1, adjuntoIds: ['0193f0a0-0000-7000-b000-000000000001'] },
    ]);
    expect(await etapaActual('A2')).toHaveAttribute('aria-current', 'step');
    await waitFor(() => expect(api.cuenta('GET', '/historial')).toBeGreaterThan(historialAntes));
  });

  it('sin respuesta exitosa no avanza la etapa ni reintenta la acción (500)', async () => {
    const { api, app } = await abrir(admin, pedidoDetalle({ etapaActual: 1, acciones: [accion('ENVIAR', 2)] }));
    const cuerpos = acciones(api, () => problemaJson(500, 'ERROR_INTERNO'));

    await app.usuario.click(await screen.findByRole('button', { name: 'Enviar' }));
    const dialogo = await screen.findByRole('dialog', { name: 'Enviar' });
    await app.usuario.click(within(dialogo).getByRole('button', { name: 'Enviar' }));

    expect(await within(dialogo).findByText('No se pudo ejecutar la acción. Intenta de nuevo.')).toBeInTheDocument();
    expect(cuerpos).toHaveLength(1);
    expect(await etapaActual('A1')).toHaveAttribute('aria-current', 'step');
    expect(screen.queryByRole('button', { name: /^A2 .*, etapa actual$/, hidden: true })).not.toBeInTheDocument();
  });

  it('RECHAZAR en A3 exige observación antes de enviar', async () => {
    const usuario = { ...ana, roles: ['asistente_compras'] };
    const enA3 = pedidoDetalle({ etapaActual: 3, acciones: [accion('APROBAR', 4), accion('RECHAZAR', 2)] });
    const { api, app } = await abrir(usuario, enA3);
    const cuerpos = acciones(api, () => HttpResponse.json(pedidoDetalle({ etapaActual: 2 })));

    await app.usuario.click(await screen.findByRole('button', { name: 'Rechazar' }));
    const dialogo = await screen.findByRole('dialog', { name: 'Rechazar' });
    await app.usuario.click(within(dialogo).getByRole('button', { name: 'Rechazar' }));
    expect(within(dialogo).getByText('Escribe la observación: esta acción la exige.')).toBeInTheDocument();
    expect(cuerpos).toHaveLength(0);

    await app.usuario.type(within(dialogo).getByLabelText('Observación'), 'Códigos ficticios incorrectos');
    await app.usuario.click(within(dialogo).getByRole('button', { name: 'Rechazar' }));

    await waitFor(() => expect(cuerpos).toHaveLength(1));
    expect(cuerpos[0]).toEqual({
      accion: 'RECHAZAR',
      etapaEsperada: 3,
      observacion: 'Códigos ficticios incorrectos',
      adjuntoIds: [],
    });
    expect(await etapaActual('A2')).toBeInTheDocument();
  });

  it('APROBAR en A3 deja el pedido en A4 según la respuesta', async () => {
    const usuario = { ...ana, roles: ['asistente_compras'] };
    const { api, app } = await abrir(usuario, pedidoDetalle({ etapaActual: 3, acciones: [accion('APROBAR', 4)] }));
    const cuerpos = acciones(api, () => HttpResponse.json(pedidoDetalle({ etapaActual: 4 })));

    await app.usuario.click(await screen.findByRole('button', { name: 'Aprobar' }));
    const dialogo = await screen.findByRole('dialog', { name: 'Aprobar' });
    expect(within(dialogo).getByLabelText('Observación (opcional)')).toBeInTheDocument();
    await app.usuario.click(within(dialogo).getByRole('button', { name: 'Aprobar' }));

    expect(await etapaActual('A4')).toBeInTheDocument();
    expect(cuerpos).toEqual([{ accion: 'APROBAR', etapaEsperada: 3, adjuntoIds: [] }]);
  });

  it('DEVOLVER_POR_PRODUCTO_NUEVO en A4 exige urgencia y envía el nivel elegido', async () => {
    const usuario = { ...ana, roles: ['compras_china'] };
    const enA4 = pedidoDetalle({
      etapaActual: 4,
      acciones: [
        accion('APROBAR', 5),
        accion('DEVOLVER_POR_ERROR', 2, { requiereObservacion: true }),
        accion('DEVOLVER_POR_PRODUCTO_NUEVO', 2, { requiereObservacion: true, requiereUrgencia: true }),
      ],
    });
    const { api, app } = await abrir(usuario, enA4);
    const cuerpos = acciones(api, () =>
      HttpResponse.json(pedidoDetalle({ etapaActual: 2, urgencia: { nivel: 'URGENTE', horas: 4 } })),
    );

    await app.usuario.click(await screen.findByRole('button', { name: 'Devolver por producto nuevo' }));
    const dialogo = await screen.findByRole('dialog', { name: 'Devolver por producto nuevo' });
    await app.usuario.type(within(dialogo).getByLabelText('Observación'), 'Modelo nuevo ficticio');
    await app.usuario.click(within(dialogo).getByRole('button', { name: 'Devolver por producto nuevo' }));
    expect(within(dialogo).getByText('Elige la urgencia.')).toBeInTheDocument();
    expect(cuerpos).toHaveLength(0);

    await app.usuario.click(within(dialogo).getByRole('radio', { name: 'Urgente · 4 h' }));
    await app.usuario.click(within(dialogo).getByRole('button', { name: 'Devolver por producto nuevo' }));

    await waitFor(() => expect(cuerpos).toHaveLength(1));
    expect(cuerpos[0]).toEqual({
      accion: 'DEVOLVER_POR_PRODUCTO_NUEVO',
      etapaEsperada: 4,
      observacion: 'Modelo nuevo ficticio',
      nivelUrgencia: 'URGENTE',
      adjuntoIds: [],
    });
    expect(await screen.findByText('Urgente · 4 h')).toBeInTheDocument();
  });

  it('DEVOLVER_POR_ERROR en A4 exige observación y no pide urgencia', async () => {
    const usuario = { ...ana, roles: ['compras_china'] };
    const enA4 = pedidoDetalle({
      etapaActual: 4,
      acciones: [accion('DEVOLVER_POR_ERROR', 2, { requiereObservacion: true })],
    });
    const { api, app } = await abrir(usuario, enA4);
    const cuerpos = acciones(api, () => HttpResponse.json(pedidoDetalle({ etapaActual: 2 })));

    await app.usuario.click(await screen.findByRole('button', { name: 'Devolver por error' }));
    const dialogo = await screen.findByRole('dialog', { name: 'Devolver por error' });
    expect(within(dialogo).queryByRole('radio')).not.toBeInTheDocument();
    await app.usuario.click(within(dialogo).getByRole('button', { name: 'Devolver por error' }));
    expect(within(dialogo).getByText('Escribe la observación: esta acción la exige.')).toBeInTheDocument();

    await app.usuario.type(within(dialogo).getByLabelText('Observación'), 'Error de codificación ficticio');
    await app.usuario.click(within(dialogo).getByRole('button', { name: 'Devolver por error' }));
    await waitFor(() => expect(cuerpos).toHaveLength(1));
    expect(cuerpos[0]).toMatchObject({ accion: 'DEVOLVER_POR_ERROR', etapaEsperada: 4 });
    expect(cuerpos[0]).not.toHaveProperty('nivelUrgencia');
  });

  it('409 ETAPA_CAMBIO: avisa, trae el estado vigente y no reenvía nada', async () => {
    const { api, app } = await abrir(admin, pedidoDetalle({ etapaActual: 1, acciones: [accion('ENVIAR', 2)] }));
    const cuerpos = acciones(api, () => {
      api.estado.detalle = pedidoDetalle({ etapaActual: 2 });
      return problemaJson(409, 'ETAPA_CAMBIO');
    });
    const detalleAntes = api.cuenta('GET', '/detalle');

    await app.usuario.click(await screen.findByRole('button', { name: 'Enviar' }));
    const dialogo = await screen.findByRole('dialog', { name: 'Enviar' });
    await app.usuario.click(within(dialogo).getByRole('button', { name: 'Enviar' }));

    expect(await within(dialogo).findByText('Otro usuario actualizó este pedido')).toBeInTheDocument();
    await waitFor(() => expect(api.cuenta('GET', '/detalle')).toBeGreaterThan(detalleAntes));
    await app.usuario.click(within(dialogo).getByRole('button', { name: 'Ver estado actual' }));
    expect(await etapaActual('A2')).toBeInTheDocument();
    expect(cuerpos).toHaveLength(1);
  });

  it('422 REQUISITO_FALTANTE muestra lo que falta y no avanza', async () => {
    const { api, app } = await abrir(admin, pedidoDetalle({ etapaActual: 1, acciones: [accion('ENVIAR', 2)] }));
    const cuerpos = acciones(api, () => problemaJson(422, 'REQUISITO_FALTANTE', { faltantes: ['Solicitud'] }));

    await app.usuario.click(await screen.findByRole('button', { name: 'Enviar' }));
    const dialogo = await screen.findByRole('dialog', { name: 'Enviar' });
    await app.usuario.click(within(dialogo).getByRole('button', { name: 'Enviar' }));

    expect(await within(dialogo).findByText('Falta: Solicitud.')).toBeInTheDocument();
    expect(await etapaActual('A1')).toBeInTheDocument();
    expect(cuerpos).toHaveLength(1);
  });

  it('400 VALIDACION en adjuntoIds muestra el error del campo, no el detalle técnico', async () => {
    const enA1 = pedidoDetalle({ etapaActual: 1, acciones: [accion('ENVIAR', 2)] });
    const archivos = { 1: [adjunto('0193f0a0-0000-7000-b000-000000000003', 1, entradaDe(1), 'Solicitud')] };
    const { api, app } = await abrir(admin, enA1, archivos);
    acciones(api, () =>
      problemaJson(400, 'VALIDACION', {
        errores: [{ campo: 'adjuntoIds', mensaje: 'Un archivo no pertenece a la entrada actual.' }],
      }),
    );

    await screen.findAllByText('Solicitud ficticio.pdf');
    await app.usuario.click(screen.getByRole('button', { name: 'Enviar' }));
    const dialogo = await screen.findByRole('dialog', { name: 'Enviar' });
    await app.usuario.click(within(dialogo).getByRole('button', { name: 'Enviar' }));

    expect(await within(dialogo).findByText('Un archivo no pertenece a la entrada actual.')).toBeInTheDocument();
    expect(within(dialogo).queryByText(/Detalle técnico/)).not.toBeInTheDocument();
  });

  it('403 SIN_PERMISO muestra el aviso de permisos', async () => {
    const { api, app } = await abrir(admin, pedidoDetalle({ etapaActual: 1, acciones: [accion('ENVIAR', 2)] }));
    acciones(api, () => problemaJson(403, 'SIN_PERMISO'));

    await app.usuario.click(await screen.findByRole('button', { name: 'Enviar' }));
    const dialogo = await screen.findByRole('dialog', { name: 'Enviar' });
    await app.usuario.click(within(dialogo).getByRole('button', { name: 'Enviar' }));

    expect(await within(dialogo).findByText(/Tu cuenta ya no tiene permiso para esta acción/)).toBeInTheDocument();
  });

  it('con documentos faltantes la acción no se puede confirmar y se dice qué falta', async () => {
    const enA1 = pedidoDetalle({
      etapaActual: 1,
      acciones: [
        accion('ENVIAR', 2, {
          documentosObligatorios: ['Solicitud'],
          documentosFaltantes: ['Solicitud'],
          puedeCompletarse: false,
        }),
      ],
    });
    const { api, app } = await abrir(admin, enA1);
    const cuerpos = acciones(api, () => HttpResponse.json(pedidoDetalle({ etapaActual: 2 })));

    expect(await screen.findByText(/falta subir Solicitud\./)).toBeInTheDocument();
    await app.usuario.click(screen.getByRole('button', { name: 'Enviar' }));
    const dialogo = await screen.findByRole('dialog', { name: 'Enviar' });
    expect(within(dialogo).getByText('· falta')).toBeInTheDocument();
    expect(within(dialogo).getByRole('button', { name: 'Enviar' })).toBeDisabled();
    expect(cuerpos).toHaveLength(0);
  });

  it('con documentos pendientes del cliente la acción no se puede completar', async () => {
    const enA1 = pedidoDetalle({
      etapaActual: 1,
      acciones: [accion('ENVIAR', 2, { requisitosDocumentales: 'PENDIENTES', puedeCompletarse: false })],
    });
    await abrir(admin, enA1);

    expect(await screen.findByText('Documentos pendientes de definición')).toBeInTheDocument();
    expect(screen.getByRole('button', { name: 'Enviar' })).toBeDisabled();
  });
});

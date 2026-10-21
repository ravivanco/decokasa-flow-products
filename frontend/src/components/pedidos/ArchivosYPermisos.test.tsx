import '@/test/radix';
import { File as FileNode } from 'node:buffer';
import { webcrypto } from 'node:crypto';
import { screen, waitFor, within } from '@testing-library/react';
import { http, HttpResponse } from 'msw';
import { afterEach, beforeEach, describe, expect, it, vi } from 'vitest';
import { admin, ana, catalogos, mario, problema, type UsuarioApi } from '@/test/apiFicticia';
import {
  accion,
  adjunto,
  apiPedido,
  type Detalle,
  entradaDe,
  PEDIDO_ID,
  pedidoDetalle,
  persona,
} from '@/test/pedidosFicticios';
import { abrirConSesion } from '@/test/renderizarApp';
import { servidor } from '@/test/servidor';

// Archivos (P-05, P-17, P-18), observaciones (P-19), reasignación (P-07) y permisos con varios roles. Los botones son
// pistas: el servidor decide y sus errores se muestran. Datos ficticios.

const avisos = vi.hoisted(() => ({ exito: vi.fn(), error: vi.fn() }));
vi.mock('sonner', async (original) => ({
  ...(await original<typeof import('sonner')>()),
  toast: Object.assign(vi.fn(), { success: avisos.exito, error: avisos.error }),
}));

const ruta = `/pedidos/${PEDIDO_ID}`;
const ARCHIVO_A2 = '0193f0a0-0000-7000-b000-000000000021';
const con = (roles: string[]): UsuarioApi => ({ ...ana, roles });

async function abrir(usuario: UsuarioApi, detalle: Detalle, adjuntos: Record<number, unknown[]> = {}) {
  const api = apiPedido(detalle, { adjuntos });
  servidor.use(...api.manejadores);
  const app = abrirConSesion(usuario, ruta);
  await screen.findByRole('heading', { level: 2, name: new RegExp(`A${detalle.etapaActual} ·`) });
  return { api, app };
}

const enA2 = (acciones = [accion('ENVIAR', 3, { documentosObligatorios: ['Documentos codificados'] })]) =>
  pedidoDetalle({ etapaActual: 2, acciones });

beforeEach(() => {
  avisos.exito.mockClear();
  avisos.error.mockClear();
});

describe('Subida de archivos', () => {
  beforeEach(() => { vi.stubGlobal('crypto', webcrypto); sessionStorage.clear(); });
  afterEach(() => vi.unstubAllGlobals());

  it('sube a la entrada actual con el tipo de documento y vuelve a leer archivos y detalle', async () => {
    vi.stubGlobal('crypto', webcrypto);
    const { api, app } = await abrir(con(['marketing']), enA2());
    const archivo = new FileNode(['contenido ficticio'], 'codigos.pdf', { type: 'application/pdf' });
    const cargaId = '00000000-0000-4000-8000-000000000090';
    const carga = { id: cargaId, pedidoId: PEDIDO_ID, etapa: 2, entradaId: entradaDe(2),
      nombre: archivo.name, tipoDocumento: 'Documentos codificados', tamanoBytes: archivo.size,
      offsetConfirmado: 0, fragmentoBytes: 4194304, estado: 'RECIBIENDO', adjuntoId: null, eventoId: null,
      creadaEn: '2026-10-19T15:00:00Z', actualizadaEn: '2026-10-19T15:00:00Z', fragmentos: [] as unknown[] };
    const inicios: unknown[] = [];
    const rangos: string[] = [];
    let confirmar!: () => void;
    const acuse = new Promise<void>(resolve => { confirmar = resolve; });
    servidor.use(
      http.post(`${api.base}/etapas/2/archivos/cargas`, async ({request}) => {
        inicios.push(await request.json()); return HttpResponse.json(carga, { status: 201 });
      }),
      http.put(`*/api/v1/archivos/cargas/${cargaId}/fragmentos`, async ({request}) => {
        rangos.push(request.headers.get('Content-Range') ?? '');
        await acuse;
        return HttpResponse.json({ ...carga, offsetConfirmado: archivo.size,
          fragmentos: [{inicio:0,tamano:archivo.size,sha256:request.headers.get('X-Fragmento-SHA256')}] });
      }),
      http.post(`*/api/v1/archivos/cargas/${cargaId}/finalizacion`, () => {
        api.estado.adjuntos[2] = [adjunto(ARCHIVO_A2, 2, entradaDe(2), 'Documentos codificados', 'codigos.pdf')];
        return HttpResponse.json(api.estado.adjuntos[2][0] as Record<string, unknown>, { status: 201 });
      }),
    );
    const archivosAntes = api.cuenta('GET', '/etapas/2/archivos');
    const detalleAntes = api.cuenta('GET', '/detalle');

    await app.usuario.upload(
      screen.getByLabelText('Archivo'),
      archivo as unknown as File,
    );
    await app.usuario.click(screen.getByRole('button', { name: 'Subir o reanudar' }));

    await waitFor(() => expect(rangos).toHaveLength(1));
    expect(screen.getByRole('progressbar', {name:'Bytes confirmados de la carga'})).toHaveAttribute('value','0');
    expect(avisos.exito).not.toHaveBeenCalled();
    expect(sessionStorage.length).toBe(1);
    const referencia = JSON.parse(sessionStorage.getItem(sessionStorage.key(0)!)!);
    expect(Object.keys(referencia).sort()).toEqual(['cargaId', 'claveIdempotencia']);
    expect(referencia.cargaId).toBe(cargaId);
    expect(referencia.claveIdempotencia).toMatch(/^[0-9a-f-]{36}$/);
    expect(rangos[0]).toBe(`bytes 0-${archivo.size-1}/${archivo.size}`);
    expect(inicios[0]).toMatchObject({nombre:'codigos.pdf', entradaEsperadaId:entradaDe(2), tipoDocumento:'Documentos codificados',tamanoBytes:archivo.size});
    confirmar();
    expect(await screen.findAllByText('codigos.pdf')).not.toHaveLength(0);
    await waitFor(() => expect(api.cuenta('GET', '/etapas/2/archivos')).toBeGreaterThan(archivosAntes));
    await waitFor(() => expect(api.cuenta('GET', '/detalle')).toBeGreaterThan(detalleAntes));
  });

  it('rechaza en el navegador un archivo de más de 100 MiB sin enviarlo', async () => {
    const { api, app } = await abrir(con(['marketing']), enA2());
    let subidas = 0;
    servidor.use(
      http.post(`${api.base}/etapas/2/archivos/cargas`, () => {
        subidas++;
        return HttpResponse.json({});
      }),
    );
    const grande = new File(['x'], 'grande.zip');
    Object.defineProperty(grande, 'size', { value: 100 * 1024 * 1024 + 1 });

    await app.usuario.upload(screen.getByLabelText('Archivo'), grande);

    expect(await screen.findByText(/supera el tamaño máximo permitido/)).toBeInTheDocument();
    expect(screen.getByRole('button', { name: 'Subir o reanudar' })).toBeDisabled();
    expect(subidas).toBe(0);
  });

  it.each([
    [413, 'ARCHIVO_DEMASIADO_GRANDE', /supera el tamaño máximo permitido \(100 MiB/],
    [409, 'ETAPA_CAMBIO', /Otra persona actualizó este pedido/],
    [403, 'SIN_PERMISO', /Tu cuenta ya no tiene permiso/],
  ])('un %i %s del servidor se muestra y no se reintenta', async (status, codigo, mensaje) => {
    const { api, app } = await abrir(con(['marketing']), enA2());
    let subidas = 0;
    servidor.use(
      http.post(`${api.base}/etapas/2/archivos/cargas`, () => {
        subidas++;
        return problema(status, codigo, 'Detalle técnico');
      }),
    );

    await app.usuario.upload(screen.getByLabelText('Archivo'), new FileNode(['x'], 'doc.pdf') as unknown as File);
    await app.usuario.click(screen.getByRole('button', { name: 'Subir o reanudar' }));

    expect(await screen.findByText(mensaje)).toBeInTheDocument();
    expect(subidas).toBe(1);
  });
});

describe('Descarga de archivos', () => {
  const crear = vi.fn(() => 'blob:prueba');
  const clic = vi.spyOn(HTMLAnchorElement.prototype, 'click').mockImplementation(() => {});

  beforeEach(() => {
    URL.createObjectURL = crear;
    URL.revokeObjectURL = vi.fn();
    crear.mockClear();
    clic.mockClear();
  });

  it('descarga a través del servidor con el nombre original', async () => {
    const archivos = { 2: [adjunto(ARCHIVO_A2, 2, entradaDe(2), 'Documentos codificados', 'codigos.pdf')] };
    const { app } = await abrir(con(['marketing']), enA2(), archivos);
    let descargas = 0;
    servidor.use(
      http.get(`*/api/v1/archivos/${ARCHIVO_A2}/contenido`, () => {
        descargas++;
        return new HttpResponse(new Uint8Array([1, 2, 3]), {
          headers: { 'Content-Type': 'application/octet-stream' },
        });
      }),
    );

    await app.usuario.click(await screen.findByRole('button', { name: 'Descargar codigos.pdf' }));

    await waitFor(() => expect(clic).toHaveBeenCalledTimes(1));
    expect(descargas).toBe(1);
    expect(crear).toHaveBeenCalledTimes(1);
    expect((clic.mock.contexts[0] as HTMLAnchorElement).download).toBe('codigos.pdf');
  });

  it.each([
    [403, 'SIN_PERMISO', /No tienes permiso para descargar este archivo/],
    [409, 'ARCHIVO_NO_DISPONIBLE', /no está disponible o está dañado/],
  ])('un %i %s se explica y no entrega nada', async (status, codigo, mensaje) => {
    const archivos = { 2: [adjunto(ARCHIVO_A2, 2, entradaDe(2), 'Documentos codificados', 'codigos.pdf')] };
    const { app } = await abrir(con(['marketing']), enA2(), archivos);
    servidor.use(http.get(`*/api/v1/archivos/${ARCHIVO_A2}/contenido`, () => problema(status, codigo, 'No.')));

    await app.usuario.click(await screen.findByRole('button', { name: 'Descargar codigos.pdf' }));

    await waitFor(() => expect(avisos.error).toHaveBeenCalledWith(expect.stringMatching(mensaje)));
    expect(clic).not.toHaveBeenCalled();
  });
});

describe('Observaciones', () => {
  it('publica en la entrada actual y recarga detalle e historial sin mover el pedido', async () => {
    const { api, app } = await abrir(con(['marketing']), enA2());
    const cuerpos: unknown[] = [];
    servidor.use(
      http.post(`${api.base}/observaciones`, async ({ request }) => {
        cuerpos.push(await request.json());
        return HttpResponse.json(
          {
            id: '0193f0a0-0000-7000-a000-0000000000f1',
            pedidoId: PEDIDO_ID,
            etapa: 2,
            tipo: 'OBSERVACION_AGREGADA',
            usuario: persona(ana.id, ana.nombre, ['marketing']),
            rolIntervencion: 'marketing',
            fecha: '2026-10-16T09:00:00-05:00',
            observacion: 'Nota ficticia',
          },
          { status: 201 },
        );
      }),
    );
    const historialAntes = api.cuenta('GET', '/historial');
    const detalleAntes = api.cuenta('GET', '/detalle');

    await app.usuario.type(screen.getByLabelText('Nueva observación'), 'Nota ficticia');
    await app.usuario.click(screen.getByRole('button', { name: 'Publicar observación' }));

    await waitFor(() => expect(cuerpos).toHaveLength(1));
    expect(cuerpos[0]).toEqual({
      etapaEsperada: 2,
      entradaEsperadaId: entradaDe(2),
      texto: 'Nota ficticia',
      adjuntoIds: [],
    });
    await waitFor(() => expect(api.cuenta('GET', '/historial')).toBeGreaterThan(historialAntes));
    await waitFor(() => expect(api.cuenta('GET', '/detalle')).toBeGreaterThan(detalleAntes));
    expect(screen.getByRole('button', { name: /^A2 .*, etapa actual$/ })).toHaveAttribute('aria-current', 'step');
    expect(screen.getByLabelText('Nueva observación')).toHaveValue('');
  });

  it('no publica una observación vacía', async () => {
    const { api, app } = await abrir(con(['marketing']), enA2());
    let publicaciones = 0;
    servidor.use(
      http.post(`${api.base}/observaciones`, () => {
        publicaciones++;
        return HttpResponse.json({});
      }),
    );

    await app.usuario.click(screen.getByRole('button', { name: 'Publicar observación' }));

    expect(screen.getByText('Escribe la observación.')).toBeInTheDocument();
    expect(publicaciones).toBe(0);
  });

  it('un 409 ETAPA_CAMBIO se muestra, conserva el texto y trae el estado vigente', async () => {
    const { api, app } = await abrir(con(['marketing']), enA2());
    let publicaciones = 0;
    servidor.use(
      http.post(`${api.base}/observaciones`, () => {
        publicaciones++;
        return problema(409, 'ETAPA_CAMBIO', 'La etapa cambió.');
      }),
    );
    const detalleAntes = api.cuenta('GET', '/detalle');

    await app.usuario.type(screen.getByLabelText('Nueva observación'), 'Nota tardía');
    await app.usuario.click(screen.getByRole('button', { name: 'Publicar observación' }));

    expect(await screen.findByText(/Otra persona actualizó este pedido/)).toBeInTheDocument();
    expect(screen.getByLabelText('Nueva observación')).toHaveValue('Nota tardía');
    await waitFor(() => expect(api.cuenta('GET', '/detalle')).toBeGreaterThan(detalleAntes));
    expect(publicaciones).toBe(1);
  });
});

describe('Reasignación', () => {
  it('el admin reasigna a una persona activa con el rol responsable', async () => {
    const inactiva = { ...mario, id: '00000000-0000-4000-8000-000000000099', nombre: 'Ines Inactiva', activo: false };
    servidor.use(...catalogos([admin, mario, ana, inactiva]));
    const { api, app } = await abrir(admin, enA2());
    const cuerpos: unknown[] = [];
    servidor.use(
      http.put(`${api.base}/etapas/2/asignacion`, async ({ request }) => {
        cuerpos.push(await request.json());
        api.estado.detalle = pedidoDetalle({
          etapaActual: 2,
          asignaciones: { 2: { persona: persona(mario.id, mario.nombre, mario.roles), vigente: true } },
        });
        return HttpResponse.json(api.estado.detalle);
      }),
    );

    await app.usuario.click(screen.getByRole('button', { name: 'Reasignar' }));
    const dialogo = await screen.findByRole('dialog', { name: 'Reasignar etapa' });
    await waitFor(() => expect(within(dialogo).getByRole('combobox', { name: 'Persona' })).toBeEnabled());
    await app.usuario.click(within(dialogo).getByRole('combobox', { name: 'Persona' }));
    const opciones = (await screen.findAllByRole('option')).map((opcion) => opcion.textContent);
    expect(opciones).toEqual([mario.nombre]);
    await app.usuario.click(screen.getByRole('option', { name: mario.nombre }));
    await app.usuario.type(within(dialogo).getByLabelText('Motivo (opcional)'), 'Cobertura ficticia');
    await app.usuario.click(within(dialogo).getByRole('button', { name: 'Reasignar' }));

    await waitFor(() => expect(cuerpos).toHaveLength(1));
    expect(cuerpos[0]).toEqual({ usuarioId: mario.id, motivo: 'Cobertura ficticia' });
    await waitFor(() => expect(screen.queryByRole('dialog')).not.toBeInTheDocument());
    expect(screen.getByText(mario.nombre)).toBeInTheDocument();
  });

  it('una persona que no es admin no ve "Reasignar"', async () => {
    await abrir(con(['marketing']), enA2());

    expect(screen.queryByRole('button', { name: 'Reasignar' })).not.toBeInTheDocument();
  });
});

describe('Permisos con varios roles', () => {
  it('consulta + marketing: actúa en A2 con lo que da el servidor, sin perderlo por el rol de consulta', async () => {
    await abrir(con(['consulta', 'marketing']), enA2());

    expect(screen.getByRole('button', { name: 'Enviar' })).toBeInTheDocument();
    expect(screen.getByRole('button', { name: 'Subir o reanudar' })).toBeInTheDocument();
    expect(screen.getByRole('button', { name: 'Publicar observación' })).toBeInTheDocument();
  });

  it('solo consulta: sin acciones, sin subida, sin observaciones y sin descarga', async () => {
    const archivos = { 2: [adjunto(ARCHIVO_A2, 2, entradaDe(2), 'Documentos codificados', 'codigos.pdf')] };
    await abrir(con(['consulta']), enA2([]), archivos);

    expect(await screen.findByText('codigos.pdf')).toBeInTheDocument();
    expect(screen.getByText(/No tienes acciones en esta etapa/)).toBeInTheDocument();
    expect(screen.queryByRole('button', { name: 'Subir o reanudar' })).not.toBeInTheDocument();
    expect(screen.queryByRole('button', { name: 'Publicar observación' })).not.toBeInTheDocument();
    expect(screen.queryByRole('button', { name: 'Descargar codigos.pdf' })).not.toBeInTheDocument();
  });

  it('asistente_compras en A3 puede descargar los archivos de A2, que revisa (P-17)', async () => {
    const archivos = { 2: [adjunto(ARCHIVO_A2, 2, entradaDe(2), 'Documentos codificados', 'codigos.pdf')] };
    const { app } = await abrir(con(['asistente_compras']), pedidoDetalle({ etapaActual: 3 }), archivos);

    await app.usuario.click(screen.getByRole('button', { name: /^A2 Codificación\s*, completada\s*, salió el\s*15[- ]oct\.?$/ }));

    expect(await screen.findByRole('button', { name: 'Descargar codigos.pdf' })).toBeInTheDocument();
    expect(screen.queryByRole('button', { name: 'Subir o reanudar' })).not.toBeInTheDocument();
  });
});

// Copia confirmada no implica publicar una URL hasta consultar el backend con permiso vigente.
describe('3.B2 enlaces confirmados', () => {
  it.each([403, 409])('rechazo %i no publica enlace y conserva descarga por backend', async (estado) => {
    const copiado = {...adjunto(ARCHIVO_A2, 2, entradaDe(2), 'Documentos codificados', 'confirmado.bin'), estadoCopia:'COPIADO'};
    const {app} = await abrir(con(['marketing']), enA2(), {2:[copiado]});
    let consultas = 0;
    servidor.use(http.get(`*/api/v1/archivos/${ARCHIVO_A2}/enlace`, () => {
      consultas++; return problema(estado, estado===403?'SIN_PERMISO':'ARCHIVO_NO_DISPONIBLE','No confirmado.');
    }));
    expect(screen.queryByRole('link', {name:'Abrir en Drive'})).not.toBeInTheDocument();
    await app.usuario.click(await screen.findByRole('button', {name:'Consultar enlace de Drive'}));
    await waitFor(() => expect(consultas).toBe(1));
    await waitFor(() => expect(screen.getByRole('button', {name:'Consultar enlace de Drive'})).toBeEnabled());
    expect(screen.queryByRole('link', {name:'Abrir en Drive'})).not.toBeInTheDocument();
    expect(screen.getByRole('button', {name:'Descargar confirmado.bin'})).toBeEnabled();
  });
  it('una respuesta con URL ajena a Drive no se convierte en enlace', async () => {
    const copiado = {...adjunto(ARCHIVO_A2, 2, entradaDe(2), 'Documentos codificados'), estadoCopia:'COPIADO'};
    const {app} = await abrir(con(['marketing']), enA2(), {2:[copiado]});
    servidor.use(http.get(`*/api/v1/archivos/${ARCHIVO_A2}/enlace`, () => HttpResponse.json({url:'https://ajeno.ejemplo.test/ficticio'})));
    await app.usuario.click(await screen.findByRole('button', {name:'Consultar enlace de Drive'}));
    await screen.findByText(/La respuesta del servidor no tiene la forma esperada/);
    expect(screen.queryByRole('link', {name:'Abrir en Drive'})).not.toBeInTheDocument();
  });
});

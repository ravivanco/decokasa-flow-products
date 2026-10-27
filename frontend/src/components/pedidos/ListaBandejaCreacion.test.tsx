import { screen, waitFor, within } from '@testing-library/react';
import { http, HttpResponse } from 'msw';
import { beforeEach, describe, expect, it } from 'vitest';
import { admin, ana, problema, type UsuarioApi } from '@/test/apiFicticia';
import {
  apiPedido,
  definicionFlujoA,
  NUMERO,
  PEDIDO_ID,
  pagina,
  pedidoDetalle,
  resumenDe,
} from '@/test/pedidosFicticios';
import { abrirConSesion } from '@/test/renderizarApp';
import { servidor } from '@/test/servidor';

// Lista (P-13), búsqueda, bandeja (P-06) y creación de pedidos (flujo A, solo admin). El servidor filtra, pagina y
// decide qué entra en la bandeja; la pantalla solo muestra y agrupa lo que recibe. Datos ficticios.

const con = (roles: string[]): UsuarioApi => ({ ...ana, roles });
const flujo = http.get('*/api/v1/flujos/EC/A', () => HttpResponse.json(definicionFlujoA));
// Desde fase 6 se consultan las definiciones también al abrir creación.
beforeEach(() => servidor.use(flujo));

/** GET /pedidos con la respuesta dada; registra los parámetros de cada consulta. */
function listado(responder: (parametros: URLSearchParams) => Response) {
  const consultas: URLSearchParams[] = [];
  servidor.use(
    flujo,
    http.get('*/api/v1/pedidos', ({ request }) => {
      const parametros = new URL(request.url).searchParams;
      consultas.push(parametros);
      return responder(parametros);
    }),
  );
  return consultas;
}

describe('Lista de pedidos', () => {
  it('muestra los pedidos con su enlace al detalle, etapa y vencimiento', async () => {
    listado(() => HttpResponse.json(pagina([resumenDe(pedidoDetalle({ etapaActual: 2 }))])));

    abrirConSesion(con(['consulta']), '/pedidos');

    const enlace = await screen.findByRole('link', { name: NUMERO });
    expect(enlace).toHaveAttribute('href', `/pedidos/${PEDIDO_ID}`);
    const fila = enlace.closest('tr') as HTMLElement;
    expect(within(fila).getByText('A2')).toBeInTheDocument();
    expect(within(fila).getByText(/Codificación/)).toBeInTheDocument();
    expect(within(fila).getByText(/A tiempo · vence/)).toBeInTheDocument();
  });

  it('busca por número o nombre en el servidor y deja el filtro en la dirección', async () => {
    const consultas = listado((parametros) =>
      HttpResponse.json(parametros.get('texto') ? pagina([]) : pagina([resumenDe(pedidoDetalle())])),
    );
    const app = abrirConSesion(con(['consulta']), '/pedidos');
    await screen.findByRole('link', { name: NUMERO });

    await app.usuario.type(screen.getByLabelText('Buscar por número o nombre'), 'DK-EC-2026-9999');
    await app.usuario.click(screen.getByRole('button', { name: 'Buscar' }));

    expect(await screen.findByText('Ningún pedido coincide con los filtros.')).toBeInTheDocument();
    expect(consultas.at(-1)?.get('texto')).toBe('DK-EC-2026-9999');
    expect(consultas.at(-1)?.get('pagina')).toBe('0');
    expect(app.router.state.location.search).toContain('texto=DK-EC-2026-9999');

    await app.usuario.click(screen.getByRole('button', { name: 'Limpiar filtros' }));
    expect(await screen.findByRole('link', { name: NUMERO })).toBeInTheDocument();
    expect(app.router.state.location.search).toBe('');
  });

  it('filtra solo vencidos en el servidor', async () => {
    const consultas = listado(() => HttpResponse.json(pagina([resumenDe(pedidoDetalle({ vencida: true }))])));
    const app = abrirConSesion(con(['consulta']), '/pedidos');
    await screen.findByRole('link', { name: NUMERO });

    await app.usuario.click(screen.getByLabelText('Solo vencidos'));

    await waitFor(() => expect(consultas.at(-1)?.get('vencido')).toBe('true'));
  });

  it('pagina con el servidor', async () => {
    const consultas = listado((parametros) =>
      HttpResponse.json(pagina([resumenDe(pedidoDetalle())], 45, Number(parametros.get('pagina')), 20)),
    );
    const app = abrirConSesion(con(['consulta']), '/pedidos');
    expect(await screen.findByText('Página 1 de 3 · 45 pedidos')).toBeInTheDocument();

    await app.usuario.click(screen.getByRole('button', { name: 'Siguiente' }));

    expect(await screen.findByText('Página 2 de 3 · 45 pedidos')).toBeInTheDocument();
    expect(consultas.at(-1)?.get('pagina')).toBe('1');
  });

  it('un error del servidor ofrece reintentar', async () => {
    let intentos = 0;
    listado(() => {
      intentos++;
      return intentos === 1
        ? problema(400, 'VALIDACION', 'Filtro inválido.')
        : HttpResponse.json(pagina([resumenDe(pedidoDetalle())]));
    });
    const app = abrirConSesion(con(['consulta']), '/pedidos');

    await app.usuario.click(await screen.findByRole('button', { name: 'Reintentar' }));

    expect(await screen.findByRole('link', { name: NUMERO })).toBeInTheDocument();
  });

  it('"Nuevo pedido" no aparece para quien no es admin', async () => {
    listado(() => HttpResponse.json(pagina([])));
    abrirConSesion(con(['marketing', 'consulta']), '/pedidos');
    expect(await screen.findByText('Todavía no hay pedidos.')).toBeInTheDocument();
    expect(screen.queryByRole('link', { name: 'Nuevo pedido' })).not.toBeInTheDocument();
  });

  it('el admin ve "Nuevo pedido"', async () => {
    listado(() => HttpResponse.json(pagina([])));
    abrirConSesion(admin, '/pedidos');
    expect(await screen.findByRole('link', { name: 'Nuevo pedido' })).toHaveAttribute('href', '/pedidos/nuevo?flujo=A');
  });
});

describe('Bandeja', () => {
  it('agrupa por etapa lo que devuelve el servidor (Codificación, Revisión, Análisis)', async () => {
    const enA4 = { ...resumenDe(pedidoDetalle({ etapaActual: 4 })), id: 'p-4', numero: 'DK-EC-2026-0004' };
    const enA2 = { ...resumenDe(pedidoDetalle({ etapaActual: 2 })), id: 'p-2', numero: 'DK-EC-2026-0002' };
    const enA3 = { ...resumenDe(pedidoDetalle({ etapaActual: 3 })), id: 'p-3', numero: 'DK-EC-2026-0003' };
    const consultas: string[] = [];
    servidor.use(
      flujo,
      http.get('*/api/v1/pedidos/bandeja', ({ request }) => {
        consultas.push(request.url);
        return HttpResponse.json(pagina([enA4, enA2, enA3], 3, 0, 100));
      }),
    );

    abrirConSesion(con(['marketing', 'asistente_compras', 'compras_china']), '/bandeja');

    await within(await screen.findByRole('region', {name:'Bandeja por etapa'})).findByRole('link', { name: 'DK-EC-2026-0002' });
    const titulos = within(screen.getByRole('region', {name:'Bandeja por etapa'})).getAllByRole('heading', { level: 2 }).map((titulo) => titulo.textContent).filter(t=>/^A[0-9]+ ?/.test(t ?? ''));
    expect(titulos).toEqual(['A2 · Codificación', 'A3 · Revisión y aprobación', 'A4 · Análisis y gestión']);
    expect(consultas).toHaveLength(5);
  });

  it('sin pendientes lo dice y ofrece ver todos los pedidos', async () => {
    servidor.use(flujo, http.get('*/api/v1/pedidos/bandeja', () => HttpResponse.json(pagina([], 0, 0, 100))));

    abrirConSesion(con(['marketing']), '/bandeja');

    expect(await screen.findByText('No tienes pedidos pendientes.')).toBeInTheDocument();
    expect(screen.getByRole('link', { name: 'Ver todos los pedidos' })).toHaveAttribute('href', '/pedidos');
  });
});

describe('Crear pedido', () => {
  it('el admin crea un pedido del flujo A y pasa a su detalle', async () => {
    const api = apiPedido(pedidoDetalle());
    const cuerpos: unknown[] = [];
    servidor.use(
      ...api.manejadores,
      http.post('*/api/v1/pedidos', async ({ request }) => {
        cuerpos.push(await request.json());
        return HttpResponse.json(api.estado.detalle, { status: 201 });
      }),
    );
    const app = abrirConSesion(admin, '/pedidos/nuevo');

    await app.usuario.type(await screen.findByLabelText('Nombre del pedido'), '  Lámparas ficticias  ');
    await app.usuario.click(screen.getByLabelText('Incluye productos nuevos además de los existentes'));
    await app.usuario.click(screen.getByRole('button', { name: 'Crear pedido' }));

    await waitFor(() => expect(app.router.state.location.pathname).toBe(`/pedidos/${PEDIDO_ID}`));
    expect(cuerpos).toEqual([
      { tipoFlujo: 'A', nombre: 'Lámparas ficticias', incluyeProductosNuevos: true, pais: 'EC' },
    ]);
    expect(await screen.findByRole('heading', { level: 1, name: NUMERO })).toBeInTheDocument();
  });

  it('sin nombre no envía nada', async () => {
    let creados = 0;
    servidor.use(
      http.post('*/api/v1/pedidos', () => {
        creados++;
        return HttpResponse.json({});
      }),
    );
    const app = abrirConSesion(admin, '/pedidos/nuevo');

    await app.usuario.click(await screen.findByRole('button', { name: 'Crear pedido' }));

    expect(await screen.findByText('Escribe el nombre del pedido.')).toBeInTheDocument();
    expect(creados).toBe(0);
  });

  it('un 400 VALIDACION marca el campo indicado por el servidor', async () => {
    servidor.use(
      http.post('*/api/v1/pedidos', () =>
        problema(400, 'VALIDACION', 'Datos inválidos.', [{ campo: 'nombre', mensaje: 'Nombre repetido en la prueba.' }]),
      ),
    );
    const app = abrirConSesion(admin, '/pedidos/nuevo');

    await app.usuario.type(await screen.findByLabelText('Nombre del pedido'), 'Duplicado');
    await app.usuario.click(screen.getByRole('button', { name: 'Crear pedido' }));

    expect(await screen.findAllByText('Nombre repetido en la prueba.')).not.toHaveLength(0);
    expect(screen.getByLabelText('Nombre del pedido')).toHaveAttribute('aria-invalid', 'true');
    expect(app.router.state.location.pathname).toBe('/pedidos/nuevo');
  });

  it('un 403 del servidor se muestra y no navega', async () => {
    servidor.use(http.post('*/api/v1/pedidos', () => problema(403, 'SIN_PERMISO', 'Sin permiso.')));
    const app = abrirConSesion(admin, '/pedidos/nuevo');

    await app.usuario.type(await screen.findByLabelText('Nombre del pedido'), 'Pedido');
    await app.usuario.click(screen.getByRole('button', { name: 'Crear pedido' }));

    expect(await screen.findByText(/Tu cuenta ya no tiene permiso/)).toBeInTheDocument();
    expect(app.router.state.location.pathname).toBe('/pedidos/nuevo');
  });

  it('quien no es admin ve "Sin permiso" en /pedidos/nuevo', async () => {
    abrirConSesion(con(['marketing']), '/pedidos/nuevo');

    expect(await screen.findByRole('heading', { name: 'Sin permiso' })).toBeInTheDocument();
    expect(screen.queryByLabelText('Nombre del pedido')).not.toBeInTheDocument();
  });
});

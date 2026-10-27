import '@/test/radix';
import { screen, waitFor, within } from '@testing-library/react';
import { http, HttpResponse } from 'msw';
import { afterEach, beforeEach, describe, expect, it, vi } from 'vitest';
import { admin, problema } from '@/test/apiFicticia';
import { flujoAReal, flujoBReal } from '@/test/flujosFase6';
import { pagina, pedidoDetalle, resumenDe } from '@/test/pedidosFicticios';
import { abrirConSesion } from '@/test/renderizarApp';
import { servidor } from '@/test/servidor';

beforeEach(() => {
  sessionStorage.clear();
  for (const pais of ['EC', 'CO']) for (const tipo of ['A', 'B']) {
    servidor.use(http.get(`*/api/v1/flujos/${pais}/${tipo}`, () => HttpResponse.json({ ...(tipo === 'A' ? flujoAReal : flujoBReal), pais })));
  }
});
afterEach(() => { sessionStorage.clear(); vi.restoreAllMocks(); });
const ambos = { ...admin, paises: ['EC', 'CO'] };
async function cambiar(app: ReturnType<typeof abrirConSesion>, pais: 'Ecuador' | 'Colombia') {
  await app.usuario.click(screen.getByRole('combobox', { name: 'País' }));
  await app.usuario.click(await screen.findByRole('option', { name: pais }));
}

describe('7.B1 país vigente: comportamiento frontend con API simulada explícita', () => {
  it('un país se elige solo y guarda únicamente su código, nunca credenciales', async () => {
    abrirConSesion(admin, '/pedidos/nuevo');
    await screen.findByLabelText('Nombre del pedido');
    expect(screen.getByText('Ecuador')).toBeVisible();
    expect(screen.queryByRole('combobox', { name: 'País' })).not.toBeInTheDocument();
    expect(sessionStorage.getItem(`decokasa.pais.${admin.id}`)).toBeNull();
    expect(Object.values(sessionStorage)).toEqual([]);
  });
  it('sin países no muestra formularios ni permite elegir uno no asignado', async () => {
    abrirConSesion({ ...admin, paises: [] }, '/pedidos/nuevo');
    expect(await screen.findByText('Sin países asignados')).toBeVisible();
    expect(screen.queryByLabelText('Nombre del pedido')).not.toBeInTheDocument();
    expect(screen.queryByRole('button', { name: 'Colombia' })).not.toBeInTheDocument();
  });
  it('exige elección y crea solo con el país activo; conserva el borrador ante 403', async () => {
    const cuerpos: unknown[] = [];
    const app = abrirConSesion(ambos, '/pedidos/nuevo');
    servidor.use(http.post('*/api/v1/pedidos', async ({ request }) => {
      cuerpos.push(await request.json()); return problema(403, 'SIN_PERMISO', 'Denegado ficticio');
    }));
    expect(await screen.findByRole('heading', { name: 'Elige un país' })).toBeVisible();
    expect(screen.queryByLabelText('Nombre del pedido')).not.toBeInTheDocument();
    await app.usuario.click(screen.getByRole('button', { name: 'Colombia' }));
    await app.usuario.type(await screen.findByLabelText('Nombre del pedido'), 'Borrador CO ficticio');
    await app.usuario.click(screen.getByRole('button', { name: 'Crear pedido' }));
    await waitFor(() => expect(cuerpos).toHaveLength(1));
    expect(cuerpos).toEqual([{ pais: 'CO', tipoFlujo: 'A', nombre: 'Borrador CO ficticio', incluyeProductosNuevos: false }]);
    expect(screen.getByLabelText('Nombre del pedido')).toHaveValue('Borrador CO ficticio');
  });
  it('Seguir conserva el borrador; descartar cambia de país sin trasladarlo', async () => {
    const app = abrirConSesion(ambos, '/pedidos/nuevo');
    await app.usuario.click(await screen.findByRole('button', { name: 'Colombia' }));
    await app.usuario.type(await screen.findByLabelText('Nombre del pedido'), 'Privado CO ficticio');
    await cambiar(app, 'Ecuador');
    const dialogo = await screen.findByRole('dialog', { name: '¿Cambiar a Ecuador?' });
    await app.usuario.click(within(dialogo).getByRole('button', { name: 'Seguir en Colombia' }));
    expect(screen.getByLabelText('Nombre del pedido')).toHaveValue('Privado CO ficticio');
    await cambiar(app, 'Ecuador');
    await app.usuario.click(within(await screen.findByRole('dialog')).getByRole('button', { name: 'Descartar y cambiar' }));
    await waitFor(() => expect(screen.getByLabelText('Nombre del pedido')).toHaveValue(''));
    expect(sessionStorage.getItem(`decokasa.pais.${admin.id}`)).toBe('EC');
  });
  it('cancela la lectura anterior y una respuesta tardía no repuebla la lista ni la caché', async () => {
    let soltar!: () => void; const espera = new Promise<void>(resolve => { soltar = resolve; });
    let inicio = false; let cancelada = false;
    const fetchReal = globalThis.fetch;
    vi.spyOn(globalThis, 'fetch').mockImplementation((input, init) => {
      if (String(input).includes('/pedidos?') && String(input).includes('pais=EC')) {
        init?.signal?.addEventListener('abort', () => { cancelada = true; });
      }
      return fetchReal(input, init);
    });
    const ec = { ...resumenDe(pedidoDetalle()), nombre: 'Dato exclusivo EC ficticio' };
    servidor.use(http.get('*/api/v1/pedidos', async ({ request }) => {
      if (new URL(request.url).searchParams.get('pais') === 'EC') {
        inicio = true;
        await espera; return HttpResponse.json(pagina([ec]));
      }
      return HttpResponse.json(pagina([]));
    }));
    const app = abrirConSesion(ambos, '/pedidos');
    await app.usuario.click(await screen.findByRole('button', { name: 'Ecuador' }));
    await waitFor(() => expect(inicio).toBe(true));
    await cambiar(app, 'Colombia');
    soltar();
    await waitFor(() => expect(cancelada).toBe(true));
    await screen.findByText('Todavía no hay pedidos.');
    expect(screen.queryByText(ec.nombre)).not.toBeInTheDocument();
    expect(app.consultas.getQueryCache().getAll().some(q => q.queryKey.includes('pais:EC'))).toBe(false);
  });
  it('un 403 tras revocar CO relee permisos y retira su vista y caché sin cerrar sesión', async () => {
    let revocado = false;
    servidor.use(http.get('*/api/v1/pedidos', () => revocado ? problema(403, 'SIN_PERMISO', 'País retirado') : HttpResponse.json(pagina([]))));
    const app = abrirConSesion(ambos, '/pedidos');
    servidor.use(http.get('*/api/v1/auth/yo', () => HttpResponse.json({ ...ambos, paises: revocado ? ['EC'] : ambos.paises, permisos: [] })));
    await app.usuario.click(await screen.findByRole('button', { name: 'Colombia' }));
    await screen.findByText('Todavía no hay pedidos.');
    revocado = true;
    await app.consultas.invalidateQueries({ predicate: q => q.queryKey.includes('pais:CO') });
    expect(await screen.findByText('Ya no tienes acceso a Colombia. Sus datos dejaron de mostrarse.')).toBeVisible();
    await waitFor(() => expect(sessionStorage.getItem(`decokasa.pais.${admin.id}`)).toBe('EC'));
    expect(app.consultas.getQueryCache().getAll().some(q => q.queryKey.includes('pais:CO'))).toBe(false);
    expect(app.router.state.location.pathname).toBe('/pedidos');
    expect(screen.queryByRole('heading', { name: 'Iniciar sesión' })).not.toBeInTheDocument();
  });
});

import { act, screen, waitFor, within } from '@testing-library/react';
import { http, HttpResponse } from 'msw';
import { afterEach, describe, expect, it, vi } from 'vitest';
import { admin, ana, catalogos, mario, problema, rolesApi } from '@/test/apiFicticia';
import { abrirConSesion, renderizarApp } from '@/test/renderizarApp';
import { servidor } from '@/test/servidor';

// Sesión en el navegador según D3b (ADR-0002, aprobado el 2026-10-12; plan, tarea 18): el acceso vive solo en
// memoria, la renovación en una cookie HttpOnly que JavaScript no lee; al recargar se recupera con /auth/renovar.
// Datos ficticios.
describe('Sesión en el navegador', () => {
  it('un cambio por canal vacía caché, mutaciones y formulario antes de mostrar la cuenta vigente', async () => {
    const oyentes = new Set<(evento: { data: { tipo: string } }) => void>();
    vi.stubGlobal('BroadcastChannel', class {
      addEventListener(_tipo: string, oyente: (evento: { data: { tipo: string } }) => void) { oyentes.add(oyente); }
      removeEventListener(_tipo: string, oyente: (evento: { data: { tipo: string } }) => void) { oyentes.delete(oyente); }
      postMessage() {}
      close() {}
    });
    let cuenta = admin;
    servidor.use(...catalogos(), http.post('*/api/v1/auth/renovar', () =>
      HttpResponse.json({ tokenAcceso: `token-${cuenta.usuario}`, expiraEn: 900, usuario: cuenta })));
    const app = renderizarApp('/usuarios');
    try {
      await app.usuario.click(await screen.findByRole('button', { name: 'Nuevo usuario' }));
      await app.usuario.type(within(screen.getByRole('dialog')).getByLabelText('Nombre'), 'Borrador anterior');
      expect(app.consultas.getQueryCache().getAll().length).toBeGreaterThan(0);
      app.consultas.getMutationCache().build(app.consultas, { mutationKey: ['anterior'] });
      cuenta = mario;
      act(() => { oyentes.forEach(oyente => oyente({ data: { tipo: 'iniciada' } })); });
      expect(app.consultas.getQueryCache().getAll()).toHaveLength(0);
      expect(app.consultas.getMutationCache().getAll()).toHaveLength(0);
      await waitFor(() => expect(screen.getByRole('banner')).toHaveTextContent(mario.nombre));
      expect(screen.getByRole('alert')).toHaveTextContent('Ahora estás usando la cuenta de Mario Prueba');
      expect(screen.queryByRole('dialog')).not.toBeInTheDocument();
      expect(screen.queryByDisplayValue('Borrador anterior')).not.toBeInTheDocument();
      expect(screen.queryByRole('link', { name: 'Usuarios' })).not.toBeInTheDocument();
      expect(screen.queryByText(admin.nombre)).not.toBeInTheDocument();
    } finally { vi.unstubAllGlobals(); }
  });
  it('si renovar tras un 401 falla por red conserva al usuario e informa el fallo', async () => {
    let renovaciones = 0;
    servidor.use(
      http.post('*/api/v1/auth/renovar', () => {
        renovaciones++;
        return renovaciones === 1
          ? HttpResponse.json({ tokenAcceso: 'inicial', expiraEn: 900, usuario: admin }) : HttpResponse.error();
      }),
      http.get('*/api/v1/usuarios', () => problema(401, 'NO_AUTENTICADO', 'vencido')),
      http.get('*/api/v1/roles', () => HttpResponse.json(rolesApi)),
    );
    const app = renderizarApp('/usuarios');
    expect(await screen.findByRole('alert')).toHaveTextContent('No hay conexión con el servidor.');
    expect(screen.getByRole('banner')).toHaveTextContent(admin.nombre);
    expect(screen.queryByLabelText('Contraseña')).not.toBeInTheDocument();
    expect(app.router.state.location.pathname).toBe('/usuarios');
    expect(renovaciones).toBe(2);
  });
  afterEach(() => {
    vi.restoreAllMocks();
    localStorage.clear();
    sessionStorage.clear();
  });

  // --- Recuperación al recargar ---

  it('al recargar con una cookie válida recupera la sesión sin pedir credenciales', async () => {
    servidor.use(...catalogos());

    const app = abrirConSesion(admin, '/usuarios');

    await waitFor(() => expect(app.renovaciones.llamadas).toBe(1));
    expect(await screen.findByRole('heading', { name: 'Usuarios' })).toBeInTheDocument();
    expect(app.router.state.location.pathname).toBe('/usuarios');
    expect(screen.queryByLabelText('Contraseña')).not.toBeInTheDocument();
  });

  it('sin cookie válida (401 al renovar) muestra el login', async () => {
    let intentos = 0;
    servidor.use(
      http.post('*/api/v1/auth/renovar', () => {
        intentos++;
        return problema(401, 'NO_AUTENTICADO', 'La sesión terminó.');
      }),
    );

    const app = renderizarApp('/inicio');

    await waitFor(() => expect(intentos).toBe(1));
    await waitFor(() => expect(app.router.state.location.pathname).toBe('/login'));
  });

  it('mientras recupera la sesión muestra un aviso de carga y no el formulario de login', async () => {
    let responder: () => void = () => {};
    const respuesta = new Promise<void>((resolver) => {
      responder = resolver;
    });
    servidor.use(
      ...catalogos(),
      http.post('*/api/v1/auth/renovar', async () => {
        await respuesta;
        return HttpResponse.json({ tokenAcceso: 'token-renovado-1', expiraEn: 900, usuario: admin });
      }),
      http.get('*/api/v1/auth/yo', () => HttpResponse.json({ ...admin, permisos: [] })),
    );

    renderizarApp('/usuarios');

    expect((await screen.findByText('Cargando…')).closest('[role="status"]')).not.toBeNull();
    expect(screen.queryByLabelText('Correo, usuario o teléfono')).not.toBeInTheDocument();
    responder();
    expect(await screen.findByRole('heading', { name: 'Usuarios' })).toBeInTheDocument();
  });

  // --- Token de acceso en memoria y renovación ante un 401 ---

  it('envía el token de acceso en la cabecera Authorization de cada llamada a la API', async () => {
    const autorizaciones: (string | null)[] = [];
    servidor.use(
      http.get('*/api/v1/usuarios', ({ request }) => {
        autorizaciones.push(request.headers.get('Authorization'));
        return HttpResponse.json([admin, ana]);
      }),
      http.get('*/api/v1/roles', () => HttpResponse.json(rolesApi)),
    );

    abrirConSesion(admin, '/usuarios');

    await waitFor(() => expect(autorizaciones).toContain('Bearer token-renovado-1'));
  });

  it('ante un 401 de la API renueva una vez y repite la llamada con el token nuevo', async () => {
    const autorizaciones: (string | null)[] = [];
    servidor.use(
      http.get('*/api/v1/usuarios', ({ request }) => {
        const autorizacion = request.headers.get('Authorization');
        autorizaciones.push(autorizacion);
        return autorizacion === 'Bearer token-renovado-1'
          ? problema(401, 'NO_AUTENTICADO', 'El token venció.')
          : HttpResponse.json([admin, ana]);
      }),
      http.get('*/api/v1/roles', () => HttpResponse.json(rolesApi)),
    );

    const app = abrirConSesion(admin, '/usuarios');

    await waitFor(() => expect(autorizaciones).toEqual(['Bearer token-renovado-1', 'Bearer token-renovado-2']));
    expect(app.renovaciones.llamadas).toBe(2);
    expect(await screen.findByRole('row', { name: /Ana Prueba/ })).toBeInTheDocument();
  });

  it('varias llamadas que reciben 401 a la vez comparten una sola renovación (A3)', async () => {
    const vistos: string[] = [];
    const rechazar = (ruta: string, cuerpo: object) =>
      http.get(`*/api/v1/${ruta}`, ({ request }) => {
        const autorizacion = request.headers.get('Authorization') ?? '';
        vistos.push(`${ruta} ${autorizacion}`);
        return autorizacion === 'Bearer token-renovado-1'
          ? problema(401, 'NO_AUTENTICADO', 'El token venció.')
          : HttpResponse.json(cuerpo);
      });
    servidor.use(rechazar('usuarios', [admin, ana]), rechazar('roles', rolesApi));

    const app = abrirConSesion(admin, '/usuarios');

    expect(await screen.findByRole('row', { name: /Ana Prueba/ })).toBeInTheDocument();
    await waitFor(() => expect(vistos).toContain('roles Bearer token-renovado-2'));
    // Una renovación al cargar y una sola más para las dos llamadas rechazadas.
    expect(app.renovaciones.llamadas).toBe(2);
    expect(vistos).toContain('usuarios Bearer token-renovado-2');
  });

  it('si la renovación tras un 401 también falla, vuelve al login', async () => {
    let renovaciones = 0;
    servidor.use(
      http.post('*/api/v1/auth/renovar', () => {
        renovaciones++;
        return renovaciones === 1
          ? HttpResponse.json({ tokenAcceso: 'token-renovado-1', expiraEn: 900, usuario: admin })
          : problema(401, 'NO_AUTENTICADO', 'La sesión terminó.');
      }),
      http.get('*/api/v1/auth/yo', () => HttpResponse.json({ ...admin, permisos: [] })),
      http.get('*/api/v1/usuarios', () => problema(401, 'NO_AUTENTICADO', 'El token venció.')),
      http.get('*/api/v1/roles', () => HttpResponse.json(rolesApi)),
    );

    const app = renderizarApp('/usuarios');

    await waitFor(() => expect(renovaciones).toBe(2));
    await waitFor(() => expect(app.router.state.location.pathname).toBe('/login'));
  });

  // --- Nada de tokens al alcance de JavaScript persistente ---

  it('no guarda ningún token en localStorage, sessionStorage ni en cookies legibles', async () => {
    const escrituras = vi.spyOn(Storage.prototype, 'setItem');
    servidor.use(...catalogos());

    const app = abrirConSesion(admin, '/usuarios');

    await waitFor(() => expect(app.renovaciones.llamadas).toBe(1));
    await screen.findByRole('heading', { name: 'Usuarios' });
    const guardado = [
      ...Object.values({ ...localStorage }),
      ...Object.values({ ...sessionStorage }),
      document.cookie,
      ...escrituras.mock.calls.map(([, valor]) => String(valor)),
    ].join(' ');
    expect(guardado).not.toContain('token-renovado');
  });

  // --- Cerrar sesión ---

  it('cerrar sesión llama a /auth/cerrar-sesion, olvida el acceso y vuelve al login', async () => {
    let cierres = 0;
    servidor.use(
      ...catalogos(),
      http.post('*/api/v1/auth/cerrar-sesion', () => {
        cierres++;
        return new HttpResponse(null, { status: 204 });
      }),
    );
    const app = abrirConSesion(admin, '/usuarios');

    await app.usuario.click(await screen.findByRole('button', { name: 'Cerrar sesión' }));

    await waitFor(() => expect(cierres).toBe(1));
    await waitFor(() => expect(app.router.state.location.pathname).toBe('/login'));
    servidor.use(http.post('*/api/v1/auth/renovar', () => problema(401, 'NO_AUTENTICADO', 'La sesión terminó.')));
    await app.router.navigate('/usuarios');
    await waitFor(() => expect(app.router.state.location.pathname).toBe('/login'));
  });
});

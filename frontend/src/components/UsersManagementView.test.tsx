import { screen, waitFor, within } from '@testing-library/react';
import { http, HttpResponse } from 'msw';
import { describe, expect, it } from 'vitest';
import { admin, ana, beto, catalogos, mario, problema, rolesApi } from '@/test/apiFicticia';
import { abrirConSesion } from '@/test/renderizarApp';
import { servidor } from '@/test/servidor';

// Gestión de usuarios del admin (plan, tareas 14 y 22): uno o varios roles con selección múltiple, al menos uno,
// desactivar para retirar el acceso, límites de campos iguales a la base y la API, 72 bytes en UTF-8. Datos ficticios.
// El admin entra con la sesión recuperada al cargar (D3b); el login tiene sus propias pruebas.
describe('Gestión de usuarios', () => {
  type App = ReturnType<typeof abrirConSesion>;

  async function abrirComoAdmin() {
    return abrirConSesion(admin, '/usuarios');
  }

  async function abrirAlta() {
    const app = await abrirComoAdmin();
    await app.usuario.click(await screen.findByRole('button', { name: 'Nuevo usuario' }));
    const dialogo = await screen.findByRole('dialog', { name: 'Nuevo usuario' });
    return { app, dialogo };
  }

  async function llenarAlta(
    app: App,
    dialogo: HTMLElement,
    datos: Partial<Record<'nombre' | 'correo' | 'usuario' | 'telefono' | 'contrasena', string>> = {},
  ) {
    const valores = {
      nombre: 'Carla Prueba',
      correo: 'carla@ejemplo.test',
      usuario: 'carlaprueba',
      telefono: '+593 99 000 0009',
      contrasena: 'Clave-Nueva-1',
      ...datos,
    };
    const campos = within(dialogo);
    if (valores.nombre) await app.usuario.type(campos.getByLabelText('Nombre'), valores.nombre);
    if (valores.correo) await app.usuario.type(campos.getByLabelText('Correo'), valores.correo);
    if (valores.usuario) await app.usuario.type(campos.getByLabelText('Usuario'), valores.usuario);
    if (valores.telefono) await app.usuario.type(campos.getByLabelText('Teléfono'), valores.telefono);
    if (valores.contrasena) await app.usuario.type(campos.getByLabelText('Contraseña inicial'), valores.contrasena);
  }

  async function marcarRoles(app: App, dialogo: HTMLElement, nombres: string[]) {
    const grupo = within(dialogo).getByRole('group', { name: 'Roles' });
    for (const nombre of nombres) {
      await app.usuario.click(within(grupo).getByRole('checkbox', { name: nombre }));
    }
  }

  function capturarAltas() {
    const enviados: unknown[] = [];
    servidor.use(
      http.post('*/api/v1/usuarios', async ({ request }) => {
        enviados.push(await request.json());
        return HttpResponse.json({ ...ana, id: '00000000-0000-4000-8000-000000000099' }, { status: 201 });
      }),
    );
    return enviados;
  }

  function esperarRespuesta() {
    let responder: () => void = () => {};
    const respuesta = new Promise<void>((resolver) => {
      responder = resolver;
    });
    return { respuesta, responder: () => responder() };
  }

  // --- Lista, carga y errores ---

  it('mientras carga la lista muestra "Cargando usuarios…"', async () => {
    const espera = esperarRespuesta();
    servidor.use(
      http.get('*/api/v1/usuarios', async () => {
        await espera.respuesta;
        return HttpResponse.json([admin, ana]);
      }),
      http.get('*/api/v1/roles', () => HttpResponse.json(rolesApi)),
    );

    abrirConSesion(admin, '/usuarios');

    expect((await screen.findByText('Cargando usuarios…')).closest('[role="status"]')).not.toBeNull();
    espera.responder();
    expect(await screen.findByRole('row', { name: /Ana Prueba/ })).toBeInTheDocument();
  });

  it('muestra cada usuario con todos sus roles y su estado', async () => {
    servidor.use(...catalogos([admin, mario, ana, beto]));

    await abrirComoAdmin();

    const filaAna = await screen.findByRole('row', { name: /Ana Prueba/ });
    expect(filaAna).toHaveTextContent('ana@ejemplo.test');
    expect(filaAna).toHaveTextContent('Compras China');
    expect(filaAna).toHaveTextContent('Logística');
    expect(filaAna).toHaveTextContent('Activo');
    expect(screen.getByRole('row', { name: /Beto Prueba/ })).toHaveTextContent('Inactivo');
  });

  it('si la lista no carga muestra un error', async () => {
    servidor.use(
      http.get('*/api/v1/usuarios', () => problema(500, 'INTERNO', 'Error inesperado.')),
      http.get('*/api/v1/roles', () => HttpResponse.json(rolesApi)),
    );

    abrirConSesion(admin, '/usuarios');

    expect((await screen.findByText('No se pudieron cargar los usuarios.')).closest('[role="alert"]')).not.toBeNull();
  });

  // --- Alta con selección múltiple de roles ---

  it('crea un usuario con varios roles elegidos en la selección múltiple', async () => {
    servidor.use(...catalogos());
    const enviados = capturarAltas();
    const { app, dialogo } = await abrirAlta();

    await llenarAlta(app, dialogo);
    await marcarRoles(app, dialogo, ['Compras China', 'Logística']);
    await app.usuario.click(within(dialogo).getByRole('button', { name: 'Guardar' }));

    await waitFor(() => expect(enviados).toHaveLength(1));
    const cuerpo = enviados[0] as { roles: string[]; correo: string; contrasenaInicial: string };
    expect([...cuerpo.roles].sort()).toEqual(['compras_china', 'logistica']);
    expect(cuerpo.correo).toBe('carla@ejemplo.test');
    expect(cuerpo.contrasenaInicial).toBe('Clave-Nueva-1');
    await waitFor(() => expect(screen.queryByRole('dialog')).not.toBeInTheDocument());
  });

  it('la selección de roles ofrece los 7 roles del catálogo', async () => {
    servidor.use(...catalogos());
    const { dialogo } = await abrirAlta();

    const grupo = within(dialogo).getByRole('group', { name: 'Roles' });
    expect(within(grupo).getAllByRole('checkbox')).toHaveLength(7);
    expect(within(grupo).getByRole('checkbox', { name: 'Consulta' })).not.toBeChecked();
  });

  it('exige al menos un rol y no envía el formulario sin roles', async () => {
    servidor.use(...catalogos());
    const enviados = capturarAltas();
    const { app, dialogo } = await abrirAlta();

    await llenarAlta(app, dialogo);
    await app.usuario.click(within(dialogo).getByRole('button', { name: 'Guardar' }));

    expect(await within(dialogo).findByText('Elige al menos un rol.')).toBeInTheDocument();
    expect(enviados).toHaveLength(0);
  });

  it('valida los límites de los campos, iguales a la base y la API', async () => {
    servidor.use(...catalogos());
    const enviados = capturarAltas();
    const { app, dialogo } = await abrirAlta();

    await llenarAlta(app, dialogo, { nombre: '', correo: 'carla-sin-arroba', usuario: 'ca', telefono: '123' });
    await marcarRoles(app, dialogo, ['Logística']);
    await app.usuario.click(within(dialogo).getByRole('button', { name: 'Guardar' }));

    const campos = within(dialogo);
    expect(await campos.findByText('Escribe el nombre.')).toBeInTheDocument();
    expect(campos.getByText('Escribe un correo válido.')).toBeInTheDocument();
    expect(campos.getByText('El usuario debe tener entre 3 y 50 caracteres.')).toBeInTheDocument();
    expect(campos.getByText('Escribe un teléfono válido, de 7 a 15 dígitos.')).toBeInTheDocument();
    expect(campos.getByLabelText('Usuario')).toHaveAttribute('aria-invalid', 'true');
    expect(enviados).toHaveLength(0);
  });

  it('rechaza una contraseña de 73 bytes aunque tenga solo 37 caracteres', async () => {
    servidor.use(...catalogos());
    const enviados = capturarAltas();
    const { app, dialogo } = await abrirAlta();

    await llenarAlta(app, dialogo, { contrasena: 'ñ'.repeat(36) + 'a' });
    await marcarRoles(app, dialogo, ['Logística']);
    await app.usuario.click(within(dialogo).getByRole('button', { name: 'Guardar' }));

    expect(await within(dialogo).findByText(/como máximo 72 bytes/)).toBeInTheDocument();
    expect(enviados).toHaveLength(0);
  });

  it('acepta una contraseña de exactamente 72 bytes y la envía sin recortar', async () => {
    servidor.use(...catalogos());
    const enviados = capturarAltas();
    const { app, dialogo } = await abrirAlta();

    await llenarAlta(app, dialogo, { contrasena: 'ñ'.repeat(36) });
    await marcarRoles(app, dialogo, ['Logística']);
    await app.usuario.click(within(dialogo).getByRole('button', { name: 'Guardar' }));

    await waitFor(() => expect(enviados).toHaveLength(1));
    expect((enviados[0] as { contrasenaInicial: string }).contrasenaInicial).toBe('ñ'.repeat(36));
  });

  it('mientras guarda deshabilita el botón y muestra "Guardando…"', async () => {
    const espera = esperarRespuesta();
    servidor.use(
      ...catalogos(),
      http.post('*/api/v1/usuarios', async () => {
        await espera.respuesta;
        return HttpResponse.json(ana, { status: 201 });
      }),
    );
    const { app, dialogo } = await abrirAlta();

    await llenarAlta(app, dialogo);
    await marcarRoles(app, dialogo, ['Logística']);
    await app.usuario.click(within(dialogo).getByRole('button', { name: 'Guardar' }));

    expect(await within(dialogo).findByRole('button', { name: 'Guardando…' })).toBeDisabled();
    espera.responder();
    await waitFor(() => expect(screen.queryByRole('dialog')).not.toBeInTheDocument());
  });

  it('un correo duplicado (409) se muestra en su campo y el diálogo sigue abierto', async () => {
    servidor.use(
      ...catalogos(),
      http.post('*/api/v1/usuarios', () =>
        problema(409, 'DUPLICADO', 'Ya existe un usuario con ese correo.', [
          { campo: 'correo', mensaje: 'Ya existe un usuario con ese correo.' },
        ]),
      ),
    );
    const { app, dialogo } = await abrirAlta();

    await llenarAlta(app, dialogo);
    await marcarRoles(app, dialogo, ['Logística']);
    await app.usuario.click(within(dialogo).getByRole('button', { name: 'Guardar' }));

    expect(await within(dialogo).findByText('Ya existe un usuario con ese correo.')).toBeInTheDocument();
    expect(within(dialogo).getByLabelText('Correo')).toHaveAttribute('aria-invalid', 'true');
    expect(screen.getByRole('dialog', { name: 'Nuevo usuario' })).toBeInTheDocument();
  });

  // --- Edición: roles y desactivación ---

  it('cambia los roles de un usuario a varios', async () => {
    const cambios: unknown[] = [];
    servidor.use(
      ...catalogos(),
      http.patch('*/api/v1/usuarios/:id', async ({ request, params }) => {
        cambios.push({ id: params.id, ...((await request.json()) as object) });
        return HttpResponse.json({ ...ana, roles: ['marketing', 'logistica'] });
      }),
    );
    const app = await abrirComoAdmin();

    await app.usuario.click(await screen.findByRole('button', { name: 'Editar a Ana Prueba' }));
    const dialogo = await screen.findByRole('dialog', { name: 'Editar usuario' });
    const grupo = within(dialogo).getByRole('group', { name: 'Roles' });
    expect(within(grupo).getByRole('checkbox', { name: 'Compras China' })).toBeChecked();
    expect(within(grupo).getByRole('checkbox', { name: 'Logística' })).toBeChecked();
    await app.usuario.click(within(grupo).getByRole('checkbox', { name: 'Compras China' }));
    await app.usuario.click(within(grupo).getByRole('checkbox', { name: 'Marketing y codificación' }));
    await app.usuario.click(within(dialogo).getByRole('button', { name: 'Guardar' }));

    await waitFor(() => expect(cambios).toHaveLength(1));
    const cambio = cambios[0] as { id: string; roles: string[] };
    expect(cambio.id).toBe(ana.id);
    expect([...cambio.roles].sort()).toEqual(['logistica', 'marketing']);
  });

  it('al editar no deja quitar todos los roles; para retirar el acceso se desactiva', async () => {
    const cambios: unknown[] = [];
    servidor.use(
      ...catalogos(),
      http.patch('*/api/v1/usuarios/:id', async ({ request }) => {
        cambios.push(await request.json());
        return HttpResponse.json(ana);
      }),
    );
    const app = await abrirComoAdmin();

    await app.usuario.click(await screen.findByRole('button', { name: 'Editar a Ana Prueba' }));
    const dialogo = await screen.findByRole('dialog', { name: 'Editar usuario' });
    const grupo = within(dialogo).getByRole('group', { name: 'Roles' });
    await app.usuario.click(within(grupo).getByRole('checkbox', { name: 'Compras China' }));
    await app.usuario.click(within(grupo).getByRole('checkbox', { name: 'Logística' }));
    await app.usuario.click(within(dialogo).getByRole('button', { name: 'Guardar' }));

    expect(await within(dialogo).findByText('Elige al menos un rol.')).toBeInTheDocument();
    expect(cambios).toHaveLength(0);
  });

  it('desactiva una cuenta desde la lista', async () => {
    const cambios: unknown[] = [];
    servidor.use(
      ...catalogos(),
      http.patch('*/api/v1/usuarios/:id', async ({ request, params }) => {
        cambios.push({ id: params.id, ...((await request.json()) as object) });
        return HttpResponse.json({ ...ana, activo: false });
      }),
    );
    const app = await abrirComoAdmin();

    await app.usuario.click(await screen.findByRole('button', { name: 'Desactivar a Ana Prueba' }));

    await waitFor(() => expect(cambios).toEqual([{ id: ana.id, activo: false }]));
  });
});

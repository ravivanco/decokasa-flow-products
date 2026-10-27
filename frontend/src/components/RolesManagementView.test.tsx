import { screen, waitFor, within } from '@testing-library/react';
import { http, HttpResponse } from 'msw';
import { describe, expect, it } from 'vitest';
import { admin, catalogos, problema, rolesApi } from '@/test/apiFicticia';
import { abrirConSesion } from '@/test/renderizarApp';
import { servidor } from '@/test/servidor';

// Gestión de roles del admin (plan, tarea 22; contrato: GET y POST /roles, PUT /roles/{rolId}). Los roles todavía no
// tienen versión (pendiente explícito de D10). Datos ficticios.
describe('Gestión de roles', () => {
  it('un fallo de red al guardar conserva los valores y requiere reintento manual', async () => {
    let llamadas = 0;
    servidor.use(...catalogos(), http.post('*/api/v1/roles', () => { llamadas++; return HttpResponse.error(); }));
    const app = abrirConSesion(admin, '/roles');
    await app.usuario.click(await screen.findByRole('button', { name: 'Nuevo rol' }));
    const dialogo = within(await screen.findByRole('dialog', { name: 'Nuevo rol' }));
    await app.usuario.type(dialogo.getByLabelText('Código'), 'rol_ficticio');
    await app.usuario.type(dialogo.getByLabelText('Nombre'), 'Rol ficticio');
    await app.usuario.click(dialogo.getByRole('button', { name: 'Guardar' }));
    expect(await dialogo.findByRole('alert')).toHaveTextContent('No hay conexión con el servidor.');
    expect(dialogo.getByLabelText('Nombre')).toHaveValue('Rol ficticio');
    expect(llamadas).toBe(1);
    expect(dialogo.getByRole('button', { name: 'Guardar' })).toBeEnabled();
  });
  it('lista los roles del catálogo con su código y su nombre', async () => {
    servidor.use(...catalogos());

    abrirConSesion(admin, '/roles');

    expect(await screen.findByRole('heading', { name: 'Roles' })).toBeInTheDocument();
    const fila = await screen.findByRole('row', { name: /compras_china/ });
    expect(fila).toHaveTextContent('Compras China');
    expect(screen.getAllByRole('row')).toHaveLength(rolesApi.length + 1);
  });

  it('crea un rol con código, nombre y permisos', async () => {
    const enviados: unknown[] = [];
    servidor.use(
      ...catalogos(),
      http.post('*/api/v1/roles', async ({ request }) => {
        const cuerpo = (await request.json()) as object;
        enviados.push(cuerpo);
        return HttpResponse.json({ id: '01a12d68-b800-7000-8000-000000000099', ...cuerpo }, { status: 201 });
      }),
    );
    const app = abrirConSesion(admin, '/roles');

    await app.usuario.click(await screen.findByRole('button', { name: 'Nuevo rol' }));
    const dialogo = await screen.findByRole('dialog', { name: 'Nuevo rol' });
    await app.usuario.type(within(dialogo).getByLabelText('Código'), 'bodega_quito');
    await app.usuario.type(within(dialogo).getByLabelText('Nombre'), 'Bodega Quito');
    await app.usuario.type(within(dialogo).getByLabelText('Permisos'), 'bodegas.ver, pedidos.ver');
    await app.usuario.click(within(dialogo).getByRole('button', { name: 'Guardar' }));

    await waitFor(() => expect(enviados).toHaveLength(1));
    expect(enviados[0]).toEqual({
      codigo: 'bodega_quito',
      nombre: 'Bodega Quito',
      permisos: ['bodegas.ver', 'pedidos.ver'],
    });
    await waitFor(() => expect(screen.queryByRole('dialog')).not.toBeInTheDocument());
  });

  it('valida el código y el nombre como el contrato y no envía', async () => {
    let llamadas = 0;
    servidor.use(
      ...catalogos(),
      http.post('*/api/v1/roles', () => {
        llamadas++;
        return HttpResponse.json({}, { status: 201 });
      }),
    );
    const app = abrirConSesion(admin, '/roles');

    await app.usuario.click(await screen.findByRole('button', { name: 'Nuevo rol' }));
    const dialogo = await screen.findByRole('dialog', { name: 'Nuevo rol' });
    await app.usuario.type(within(dialogo).getByLabelText('Código'), 'Bodega Quito');
    await app.usuario.click(within(dialogo).getByRole('button', { name: 'Guardar' }));

    expect(
      await within(dialogo).findByText('Usa de 3 a 40 letras minúsculas o guiones bajos, sin espacios.'),
    ).toBeInTheDocument();
    expect(within(dialogo).getByText('Escribe el nombre del rol.')).toBeInTheDocument();
    expect(within(dialogo).getByLabelText('Código')).toHaveAttribute('aria-invalid', 'true');
    expect(llamadas).toBe(0);
  });

  it('un código repetido (409 DUPLICADO) se muestra en su campo y el diálogo sigue abierto', async () => {
    servidor.use(
      ...catalogos(),
      http.post('*/api/v1/roles', () =>
        problema(409, 'DUPLICADO', 'Ya existe un rol con ese código.', [
          { campo: 'codigo', mensaje: 'Ya existe un rol con ese código.' },
        ]),
      ),
    );
    const app = abrirConSesion(admin, '/roles');

    await app.usuario.click(await screen.findByRole('button', { name: 'Nuevo rol' }));
    const dialogo = await screen.findByRole('dialog', { name: 'Nuevo rol' });
    await app.usuario.type(within(dialogo).getByLabelText('Código'), 'logistica');
    await app.usuario.type(within(dialogo).getByLabelText('Nombre'), 'Otra logística');
    await app.usuario.click(within(dialogo).getByRole('button', { name: 'Guardar' }));

    expect(await within(dialogo).findByText('Ya existe un rol con ese código.')).toBeInTheDocument();
    expect(within(dialogo).getByLabelText('Código')).toHaveAttribute('aria-invalid', 'true');
    expect(screen.getByRole('dialog', { name: 'Nuevo rol' })).toBeInTheDocument();
  });

  it('edita el nombre y los permisos de un rol con PUT', async () => {
    const cambios: unknown[] = [];
    servidor.use(
      ...catalogos(),
      http.put('*/api/v1/roles/:id', async ({ request, params }) => {
        const cuerpo = (await request.json()) as object;
        cambios.push({ id: params.id, ...cuerpo });
        return HttpResponse.json({ id: params.id, ...cuerpo });
      }),
    );
    const app = abrirConSesion(admin, '/roles');

    await app.usuario.click(await screen.findByRole('button', { name: 'Editar el rol Logística' }));
    const dialogo = await screen.findByRole('dialog', { name: 'Editar rol' });
    const nombre = within(dialogo).getByLabelText('Nombre');
    expect(nombre).toHaveValue('Logística');
    await app.usuario.clear(nombre);
    await app.usuario.type(nombre, 'Logística Ecuador');
    await app.usuario.click(within(dialogo).getByRole('button', { name: 'Guardar' }));

    const logistica = rolesApi.find((rol) => rol.codigo === 'logistica')!;
    await waitFor(() =>
      expect(cambios).toEqual([{ id: logistica.id, codigo: 'logistica', nombre: 'Logística Ecuador', permisos: [] }]),
    );
  });
});

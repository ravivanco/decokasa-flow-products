import { screen, waitFor } from '@testing-library/react';
import { http, HttpResponse } from 'msw';
import { describe, expect, it } from 'vitest';
import { ana, CLAVE, problema, sesionDe } from '@/test/apiFicticia';
import { renderizarApp } from '@/test/renderizarApp';
import { servidor } from '@/test/servidor';

// Pantalla de inicio de sesión (plan, tareas 14 y 20; requisito 4.2 #1; ADR-0002). Datos ficticios.
describe('Pantalla de inicio de sesión', () => {
  async function escribirCredenciales(app: ReturnType<typeof renderizarApp>, identificador: string, clave: string) {
    await app.usuario.type(await screen.findByLabelText('Correo, usuario o teléfono'), identificador);
    await app.usuario.type(screen.getByLabelText('Contraseña'), clave);
    await app.usuario.click(screen.getByRole('button', { name: 'Ingresar' }));
  }

  it('muestra cada campo con su etiqueta y la contraseña oculta', async () => {
    renderizarApp('/login');

    expect(await screen.findByLabelText('Correo, usuario o teléfono')).toBeInTheDocument();
    expect(screen.getByLabelText('Contraseña')).toHaveAttribute('type', 'password');
    expect(screen.getByRole('button', { name: 'Ingresar' })).toBeEnabled();
  });

  it('no ofrece accesos rápidos, cambio de rol ni contraseñas de demostración (auditoría S1 y S2)', async () => {
    renderizarApp('/login');
    await screen.findByLabelText('Correo, usuario o teléfono');

    expect(screen.getAllByRole('button')).toHaveLength(1);
    expect(screen.queryByText(/Demo2026/)).not.toBeInTheDocument();
  });

  it('con los campos vacíos explica qué falta y no llama a la API', async () => {
    let llamadas = 0;
    servidor.use(
      http.post('*/api/v1/auth/login', () => {
        llamadas++;
        return HttpResponse.json({});
      }),
    );
    const app = renderizarApp('/login');

    await app.usuario.click(await screen.findByRole('button', { name: 'Ingresar' }));

    expect(await screen.findByText('Escribe tu correo, usuario o teléfono.')).toBeInTheDocument();
    expect(screen.getByText('Escribe tu contraseña.')).toBeInTheDocument();
    expect(llamadas).toBe(0);
  });

  it('envía el identificador y la contraseña y entra a /inicio', async () => {
    let cuerpo: unknown;
    servidor.use(
      ...sesionDe(ana),
      http.post('*/api/v1/auth/login', async ({ request }) => {
        cuerpo = await request.json();
        return HttpResponse.json({ tokenAcceso: 'token-de-prueba', expiraEn: 900, usuario: ana });
      }),
    );
    const app = renderizarApp('/login');

    await escribirCredenciales(app, '+593 99 000 0001', CLAVE);

    await waitFor(() => expect(app.router.state.location.pathname).toBe('/inicio'));
    expect(cuerpo).toEqual({ identificador: '+593 99 000 0001', contrasena: CLAVE });
  });

  it('mientras espera la respuesta deshabilita el botón y muestra "Ingresando…"', async () => {
    let responder: () => void = () => {};
    const respuesta = new Promise<void>((resolver) => {
      responder = resolver;
    });
    servidor.use(
      ...sesionDe(ana),
      http.post('*/api/v1/auth/login', async () => {
        await respuesta;
        return HttpResponse.json({ tokenAcceso: 'token-de-prueba', expiraEn: 900, usuario: ana });
      }),
    );
    const app = renderizarApp('/login');

    await escribirCredenciales(app, 'ana@ejemplo.test', CLAVE);

    expect(await screen.findByRole('button', { name: 'Ingresando…' })).toBeDisabled();
    responder();
    await waitFor(() => expect(app.router.state.location.pathname).toBe('/inicio'));
  });

  it('con credenciales incorrectas muestra el aviso en una alerta y sigue en el login', async () => {
    servidor.use(
      http.post('*/api/v1/auth/login', () => problema(401, 'NO_AUTENTICADO', 'Credenciales incorrectas.')),
    );
    const app = renderizarApp('/login');

    await escribirCredenciales(app, 'ana@ejemplo.test', 'Clave-Mala-1');

    expect(await screen.findByRole('alert')).toHaveTextContent('Credenciales incorrectas');
    expect(app.router.state.location.pathname).toBe('/login');
    expect(screen.getByRole('button', { name: 'Ingresar' })).toBeEnabled();
  });

  it('con la cuenta bloqueada avisa que espere 15 minutos (D2)', async () => {
    servidor.use(
      http.post('*/api/v1/auth/login', () =>
        problema(423, 'CUENTA_BLOQUEADA', 'La cuenta está bloqueada por 15 minutos tras 5 intentos fallidos.'),
      ),
    );
    const app = renderizarApp('/login');

    await escribirCredenciales(app, 'ana@ejemplo.test', CLAVE);

    const alerta = await screen.findByRole('alert');
    expect(alerta).toHaveTextContent(/bloqueada/i);
    expect(alerta).toHaveTextContent(/15 minutos/);
  });

  it('si el servidor falla muestra un error general sin detalles técnicos', async () => {
    servidor.use(http.post('*/api/v1/auth/login', () => problema(500, 'INTERNO', 'Error inesperado.')));
    const app = renderizarApp('/login');

    await escribirCredenciales(app, 'ana@ejemplo.test', CLAVE);

    expect(await screen.findByRole('alert')).toHaveTextContent('No se pudo iniciar sesión. Intenta de nuevo.');
  });
});

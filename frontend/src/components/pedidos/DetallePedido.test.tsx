import { screen, within } from '@testing-library/react';
import { http } from 'msw';
import { describe, expect, it } from 'vitest';
import { admin, problema } from '@/test/apiFicticia';
import { apiPedido, NUMERO, PEDIDO_ID, pedidoDetalle } from '@/test/pedidosFicticios';
import { abrirConSesion } from '@/test/renderizarApp';
import { servidor } from '@/test/servidor';

// Detalle del pedido (paso 2.5): encabezado, línea de tiempo y resumen de etapa (P-20). El resumen es el texto del
// servidor: se muestra tal cual, como texto, y no aparece nada si es null. Datos ficticios.

const ruta = `/pedidos/${PEDIDO_ID}`;

describe('Detalle del pedido', () => {
  it('muestra el número y marca la etapa actual en la línea de tiempo', async () => {
    servidor.use(...apiPedido(pedidoDetalle({ etapaActual: 3 })).manejadores);

    abrirConSesion(admin, ruta);

    expect(await screen.findByRole('heading', { level: 1, name: NUMERO })).toBeInTheDocument();
    const recorrido = screen.getByRole('list', { name: `Recorrido del pedido ${NUMERO}` });
    const actual = within(recorrido).getByRole('button', { name: /A3 Revisión y aprobación, etapa actual/ });
    expect(actual).toHaveAttribute('aria-current', 'step');
    expect(within(recorrido).getByRole('button', { name: /A2 Codificación, completada/ })).not.toHaveAttribute(
      'aria-current',
    );
  });

  it('muestra el resumen del servidor como texto, conservando "Entrada anterior"', async () => {
    const texto =
      'Entrada anterior: Rechazada por Ana Prueba (Asistente de compras) el 15/10/2026 16:00 · vuelve a A2.';
    servidor.use(...apiPedido(pedidoDetalle({ etapaActual: 3, resumenes: { 3: texto } })).manejadores);

    abrirConSesion(admin, ruta);

    const aviso = await screen.findByText('Lo ocurrido en la etapa');
    expect(aviso.closest('[data-slot="alert"]')).toHaveTextContent(texto);
  });

  it('presenta el resumen como nota informativa con nombre accesible y sin alerta urgente', async () => {
    const texto = 'Entrada anterior: Rechazada con observación ficticia.';
    servidor.use(...apiPedido(pedidoDetalle({ etapaActual: 3, resumenes: { 3: texto } })).manejadores);

    abrirConSesion(admin, ruta);

    const nota = await screen.findByRole('note', { name: 'Lo ocurrido en la etapa' });
    expect(nota).toHaveTextContent(texto);
    expect(nota).toHaveAccessibleName('Lo ocurrido en la etapa');
    expect(nota).not.toHaveAttribute('role', 'alert');
    expect(within(nota).queryByRole('alert')).not.toBeInTheDocument();
  });

  it('muestra el resumen de la etapa que se consulta en la línea de tiempo', async () => {
    const texto = 'Enviada por Admin Prueba (Administrador) el 15/10/2026 10:00 · pasa a A2.';
    servidor.use(...apiPedido(pedidoDetalle({ etapaActual: 2, resumenes: { 1: texto } })).manejadores);

    const app = abrirConSesion(admin, ruta);

    const a1 = await screen.findByRole('button', { name: /A1 Crear solicitud de pedido, completada/ });
    expect(screen.queryByText('Lo ocurrido en la etapa')).not.toBeInTheDocument();
    await app.usuario.click(a1);

    expect(await screen.findByText(texto)).toBeInTheDocument();
  });

  it('el resumen es texto: no interpreta HTML', async () => {
    const texto = 'Aprobada. Observación: «<b>negrita</b> <img src=x onerror=alert(1)>».';
    servidor.use(...apiPedido(pedidoDetalle({ etapaActual: 3, resumenes: { 3: texto } })).manejadores);

    abrirConSesion(admin, ruta);

    const aviso = (await screen.findByText('Lo ocurrido en la etapa')).closest('[data-slot="alert"]');
    expect(aviso).toHaveTextContent(texto);
    expect(aviso?.querySelector('b, img')).toBeNull();
  });

  it('sin resumen (null) no muestra ningún texto en su lugar', async () => {
    servidor.use(...apiPedido(pedidoDetalle({ etapaActual: 3 })).manejadores);

    abrirConSesion(admin, ruta);

    await screen.findByRole('heading', { level: 1, name: NUMERO });
    await screen.findByRole('heading', { name: /A3 · Revisión y aprobación/ });
    expect(screen.queryByText('Lo ocurrido en la etapa')).not.toBeInTheDocument();
  });

  it('un pedido que no existe muestra "Pedido no encontrado"', async () => {
    servidor.use(
      http.get(`*/api/v1/pedidos/${PEDIDO_ID}`, () => problema(404, 'NO_ENCONTRADO', 'El pedido no existe.')),
    );

    abrirConSesion(admin, ruta);

    expect(await screen.findByRole('heading', { name: 'Pedido no encontrado' })).toBeInTheDocument();
  });

  it('una cuenta sin permiso de consulta (403) ve el aviso y no los datos', async () => {
    servidor.use(http.get(`*/api/v1/pedidos/${PEDIDO_ID}`, () => problema(403, 'SIN_PERMISO', 'Sin permiso.')));

    abrirConSesion(admin, ruta);

    expect(await screen.findByRole('heading', { name: 'No se pudo cargar el pedido' })).toBeInTheDocument();
    expect(screen.getByText(/ya no tiene permiso/)).toBeInTheDocument();
    expect(screen.queryByText(NUMERO)).not.toBeInTheDocument();
  });
});

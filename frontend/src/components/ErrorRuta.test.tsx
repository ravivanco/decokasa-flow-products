import { render, screen } from '@testing-library/react';
import { createMemoryRouter, RouterProvider } from 'react-router';
import { describe, expect, it } from 'vitest';
import { ErrorApi } from '@/api/cliente';
import { ErrorRuta } from './ErrorRuta';

describe('ErrorRuta', () => {
  it.each([
    [new Error('detalle secreto SQL'), 'Ocurrió un error inesperado en esta pantalla.'],
    [new Response('detalle secreto SQL', { status: 404 }), 'La página no está disponible.'],
    [new ErrorApi(0, 'SIN_CONEXION', 'detalle secreto SQL'), 'No hay conexión con el servidor.'],
  ])('presenta un error seguro con opción de recargar', async (error, mensaje) => {
    const router = createMemoryRouter([{ path: '/', loader: () => { throw error; },
      element: <p>No se debe mostrar</p>, errorElement: <ErrorRuta /> }]);
    render(<RouterProvider router={router} />);
    expect(await screen.findByRole('heading', { name: 'Algo salió mal' })).toBeInTheDocument();
    expect(screen.getByRole('alert')).toHaveTextContent(mensaje);
    expect(screen.getByRole('button', { name: 'Recargar la página' })).toBeEnabled();
    expect(screen.queryByText(/detalle secreto SQL/)).not.toBeInTheDocument();
  });
});

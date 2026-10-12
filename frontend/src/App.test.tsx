import { render, screen } from '@testing-library/react';
import { describe, expect, it } from 'vitest';
import App from './App';

// Prueba de humo: la limpieza de dependencias y la configuración nueva no deben romper el demo.
// El comportamiento del arranque (sesión, login) cambia en el paso 1.8; esta prueba solo exige que monte.
describe('App', () => {
  it('se renderiza sin errores y muestra la marca', () => {
    render(<App />);
    expect(screen.getAllByText(/Decokasa/i).length).toBeGreaterThan(0);
  });
});

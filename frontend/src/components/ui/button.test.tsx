import { render, screen } from '@testing-library/react';
import userEvent from '@testing-library/user-event';
import { describe, expect, it, vi } from 'vitest';
import { Button } from './button';

// Reglas de DESIGN.md aplicadas al botón base de shadcn/ui.
describe('Button', () => {
  it('usa el color primario del tema (morado de marca)', () => {
    render(<Button>Guardar</Button>);
    expect(screen.getByRole('button', { name: 'Guardar' })).toHaveClass('bg-primary', 'text-primary-foreground');
  });

  it('muestra un anillo de foco con el color de foco completo, sin transparencia (DESIGN.md 3.3)', () => {
    render(<Button>Guardar</Button>);
    const boton = screen.getByRole('button', { name: 'Guardar' });
    expect(boton.className).toMatch(/focus-visible:ring-ring(\s|$)/);
    expect(boton.className).not.toMatch(/focus-visible:ring-ring\/\d+/);
  });

  it('mide al menos 40 px de alto en su tamaño por defecto (DESIGN.md 5)', () => {
    render(<Button>Guardar</Button>);
    expect(screen.getByRole('button', { name: 'Guardar' })).toHaveClass('h-10');
  });

  it('se activa con el teclado', async () => {
    const alPulsar = vi.fn();
    render(<Button onClick={alPulsar}>Guardar</Button>);
    await userEvent.tab();
    expect(screen.getByRole('button', { name: 'Guardar' })).toHaveFocus();
    await userEvent.keyboard('{Enter}');
    expect(alPulsar).toHaveBeenCalledTimes(1);
  });
});

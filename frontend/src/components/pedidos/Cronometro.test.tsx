import { render, screen } from '@testing-library/react';
import { describe, expect, it } from 'vitest';
import { Cronometro, duracionCronometro } from './Cronometro';

describe('Cronometro', () => {
  const ahora = '2026-11-03T15:00:00Z';

  it('cuenta desde la hora del servidor, no desde el reloj del equipo', () => {
    render(<Cronometro venceEn="2026-11-05T17:30:00Z" servidorAhora={ahora} estadoPlazo="DEFINIDO" />);
    expect(screen.getByText(/Quedan 2 d 02:(29|30):[0-9]{2}/)).toBeInTheDocument();
  });

  it('distingue Vencido', () => {
    render(<Cronometro venceEn="2026-11-03T13:00:00Z" servidorAhora={ahora} estadoPlazo="DEFINIDO" />);
    expect(screen.getByText(/Vencido hace 02:/)).toBeInTheDocument();
  });

  it.each(['PENDIENTE', 'SIN_PLAZO'] as const)('muestra Sin plazo con %s', (estadoPlazo) => {
    render(<Cronometro venceEn={null} servidorAhora={ahora} estadoPlazo={estadoPlazo} />);
    expect(screen.getByText('Sin plazo')).toBeInTheDocument();
  });

  it('formatea días, horas y minutos', () => {
    expect(duracionCronometro(90_061_000)).toBe('1 d 01 h 01 min');
  });
});

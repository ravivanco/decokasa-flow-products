import { describe, expect, it } from 'vitest';
import { enUnidad, formatearDuracion, unidadPara } from './duracion';

describe('Duraciones medidas, sin ceros por redondear', () => {
  it.each([[30,'30 s'],[60,'1 min'],[3600,'1 h'],[172800,'2 días']] as const)('presenta %s segundos como %s', (segundos,texto) => {
    expect(formatearDuracion(segundos)).toBe(texto);
  });
  it('una duración positiva menor de un segundo no se convierte en cero', () => {
    expect(formatearDuracion(0.4)).toBe('<1 s');
  });
  it('una única unidad de gráfico conserva valores pequeños positivos', () => {
    expect(enUnidad(1,unidadPara(30))).toBe('<0,1 min');
    expect(enUnidad(3600,unidadPara(200000))).toBe('<0,1 d');
    expect(enUnidad(0,unidadPara(30))).toBe('0 min');
  });
});

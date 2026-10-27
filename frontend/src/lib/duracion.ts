// Duraciones medidas por el servidor, en segundos (ADR-0029). Una duración corta se muestra en minutos u horas,
// nunca como «0 días». En un gráfico, todas las marcas usan la misma unidad para poder compararlas.

const numero = (valor: number, decimales: number) =>
  valor.toLocaleString('es-EC', { maximumFractionDigits: decimales, minimumFractionDigits: 0 });

/** Texto legible de una duración: segundos, minutos, horas o días según su tamaño. */
export function formatearDuracion(segundos: number): string {
  if (segundos > 0 && segundos < 1) return '<1 s';
  if (segundos < 60) return `${numero(segundos, 0)} s`;
  if (segundos < 3_600) return `${numero(segundos / 60, 0)} min`;
  if (segundos < 48 * 3_600) return `${numero(segundos / 3_600, 1)} h`;
  return `${numero(segundos / 86_400, 1)} días`;
}

export interface Unidad {
  nombre: 'minutos' | 'horas' | 'días';
  sufijo: string;
  segundos: number;
}

const MINUTOS: Unidad = { nombre: 'minutos', sufijo: 'min', segundos: 60 };
const HORAS: Unidad = { nombre: 'horas', sufijo: 'h', segundos: 3_600 };
const DIAS: Unidad = { nombre: 'días', sufijo: 'd', segundos: 86_400 };

/** Unidad común de un gráfico, elegida por su valor más alto. */
export function unidadPara(maximoSegundos: number): Unidad {
  if (maximoSegundos < 3_600) return MINUTOS;
  if (maximoSegundos < 48 * 3_600) return HORAS;
  return DIAS;
}

export function enUnidad(segundos: number, unidad: Unidad): string {
  const valor = segundos / unidad.segundos;
  if (valor > 0 && valor < 0.1) return `<${numero(0.1, 1)} ${unidad.sufijo}`;
  return `${numero(valor, valor < 10 ? 1 : 0)} ${unidad.sufijo}`;
}

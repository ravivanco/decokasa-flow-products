import { Semaforo } from '../types';

export const SIMULATED_TODAY = '2026-10-06'; // Martes 06/10/2026

/**
 * Parsea una fecha en formato YYYY-MM-DD sin desfasar horas de zona
 */
export function parseDate(dateStr: string): Date {
  const [year, month, day] = dateStr.split('-').map(Number);
  return new Date(Date.UTC(year, month - 1, day, 12, 0, 0));
}

/**
 * Convierte un objeto Date a formato YYYY-MM-DD
 */
export function formatDateISO(date: Date): string {
  const y = date.getUTCFullYear();
  const m = String(date.getUTCMonth() + 1).padStart(2, '0');
  const d = String(date.getUTCDate()).padStart(2, '0');
  return `${y}-${m}-${d}`;
}

/**
 * Verifica si es día hábil (lunes a viernes)
 */
export function isBusinessDay(date: Date): boolean {
  const day = date.getUTCDay();
  return day !== 0 && day !== 6; // 0 = Domingo, 6 = Sábado
}

/**
 * Ajusta la fecha al siguiente día hábil si cae en fin de semana
 */
export function rollToNextBusinessDay(dateStr: string): string {
  const date = parseDate(dateStr);
  while (!isBusinessDay(date)) {
    date.setUTCDate(date.getUTCDate() + 1);
  }
  return formatDateISO(date);
}

/**
 * Suma días hábiles a una fecha. Si days === 0, retorna la fecha (o lunes si es fin de semana).
 */
export function addBusinessDays(startDateStr: string, days: number): string {
  const current = parseDate(startDateStr);

  while (!isBusinessDay(current)) {
    current.setUTCDate(current.getUTCDate() + 1);
  }

  if (days <= 0) {
    return formatDateISO(current);
  }

  let added = 0;
  while (added < days) {
    current.setUTCDate(current.getUTCDate() + 1);
    if (isBusinessDay(current)) {
      added++;
    }
  }

  return formatDateISO(current);
}

/**
 * Cuenta la diferencia en días hábiles entre dos fechas.
 * Retorna positivo si date2Str > date1Str, negativo si date2Str < date1Str.
 */
export function countBusinessDaysDiff(date1Str: string, date2Str: string): number {
  if (date1Str === date2Str) return 0;

  const d1 = parseDate(date1Str);
  const d2 = parseDate(date2Str);

  const isNegative = d2.getTime() < d1.getTime();
  const start = isNegative ? d2 : d1;
  const end = isNegative ? d1 : d2;

  let count = 0;
  const cur = new Date(start.getTime());

  while (cur.getTime() < end.getTime()) {
    cur.setUTCDate(cur.getUTCDate() + 1);
    if (isBusinessDay(cur)) {
      count++;
    }
  }

  return isNegative ? -count : count;
}

/**
 * Formatea una fecha para mostrar en UI: SIEMPRE dd/mm/aaaa
 */
export function formatDisplayDate(dateStr?: string): string {
  if (!dateStr) return '—';
  try {
    const parts = dateStr.split('-');
    if (parts.length === 3) {
      const [y, m, d] = parts;
      return `${d.padStart(2, '0')}/${m.padStart(2, '0')}/${y}`;
    }
    return dateStr;
  } catch {
    return dateStr;
  }
}

/**
 * Determina el semáforo para una etapa
 * - A tiempo: verde #16A34A
 * - Por vencer (faltan 3 días hábiles o menos): naranja #EA580C
 * - Retrasado: rojo #DC2626
 * - Pausado/Cancelado: gris #6C6B6D
 */
export function getStageTrafficLight(
  fechaFinEstimada: string,
  completada: boolean,
  fechaRealFin?: string,
  todayStr: string = SIMULATED_TODAY
): {
  semaforo: Semaforo;
  diasDiferencia: number;
  esAtrasado: boolean;
} {
  if (completada) {
    if (fechaRealFin && fechaRealFin > fechaFinEstimada) {
      const diasAtraso = countBusinessDaysDiff(fechaFinEstimada, fechaRealFin);
      return { semaforo: 'rojo', diasDiferencia: -diasAtraso, esAtrasado: true };
    }
    return { semaforo: 'verde', diasDiferencia: 0, esAtrasado: false };
  }

  const diff = countBusinessDaysDiff(todayStr, fechaFinEstimada);

  if (diff < 0) {
    // Venció
    return {
      semaforo: 'rojo',
      diasDiferencia: Math.abs(diff),
      esAtrasado: true,
    };
  } else if (diff <= 3) {
    // 3 días hábiles o menos
    return {
      semaforo: 'naranja',
      diasDiferencia: diff,
      esAtrasado: false,
    };
  } else {
    // A tiempo
    return {
      semaforo: 'verde',
      diasDiferencia: diff,
      esAtrasado: false,
    };
  }
}

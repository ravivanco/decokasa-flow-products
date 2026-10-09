import { Semaforo } from '../types';

export const SIMULATED_TODAY = '2026-10-09'; // Viernes 09/10/2026 (Fecha de requerimientos)

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
 * Ajusta la fecha al siguiente lunes si cae en fin de semana
 */
export function rollToNextBusinessDay(dateStr: string): string {
  const date = parseDate(dateStr);
  while (!isBusinessDay(date)) {
    date.setUTCDate(date.getUTCDate() + 1);
  }
  return formatDateISO(date);
}

/**
 * Suma días hábiles (lunes a viernes). Si days === 0, retorna la fecha (o lunes si es fin de semana).
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
 * Suma días calendario y si cae en fin de semana pasa al lunes (Lógica de la calculadora existente de Decokasa)
 */
export function addCalendarDaysWithWeekendRoll(startDateStr: string, days: number): {
  fechaFinal: string;
  ajusteDias: number;
} {
  const current = parseDate(startDateStr);
  current.setUTCDate(current.getUTCDate() + days);

  const dayOfWeek = current.getUTCDay();
  let ajusteDias = 0;
  if (dayOfWeek === 6) { // Sábado -> pasa a lunes (+2)
    ajusteDias = 2;
    current.setUTCDate(current.getUTCDate() + 2);
  } else if (dayOfWeek === 0) { // Domingo -> pasa a lunes (+1)
    ajusteDias = 1;
    current.setUTCDate(current.getUTCDate() + 1);
  }

  return {
    fechaFinal: formatDateISO(current),
    ajusteDias,
  };
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
      return `${parts[2]}/${parts[1]}/${parts[0]}`;
    }
    return dateStr;
  } catch {
    return dateStr;
  }
}

/**
 * Formato día de la semana + fecha: ej. Vie 09/10/2026
 */
export function formatDisplayDateWithDay(dateStr?: string): string {
  if (!dateStr) return '—';
  const d = parseDate(dateStr);
  const days = ['Dom', 'Lun', 'Mar', 'Mié', 'Jue', 'Vie', 'Sáb'];
  const dayName = days[d.getUTCDay()];
  return `${dayName} ${formatDisplayDate(dateStr)}`;
}

/**
 * Semáforo dinámico de 4 estados:
 * - verde #16A34A: a tiempo (> 3 días hábiles restantes)
 * - naranja #EA580C: por vencer (<= 3 días hábiles restantes)
 * - rojo #DC2626: retrasado (< 0 días hábiles)
 * - gris #6C6B6D: en pausa o cancelado
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
  textoEstado: string;
} {
  if (completada) {
    if (fechaRealFin) {
      const diff = countBusinessDaysDiff(fechaRealFin, fechaFinEstimada);
      if (diff < 0) {
        return {
          semaforo: 'rojo',
          diasDiferencia: diff,
          esAtrasado: true,
          textoEstado: `Completada con ${Math.abs(diff)} día(s) hábiles de atraso`,
        };
      }
    }
    return {
      semaforo: 'verde',
      diasDiferencia: 0,
      esAtrasado: false,
      textoEstado: 'Completada a tiempo',
    };
  }

  const diffHoy = countBusinessDaysDiff(todayStr, fechaFinEstimada);

  if (diffHoy < 0) {
    return {
      semaforo: 'rojo',
      diasDiferencia: diffHoy,
      esAtrasado: true,
      textoEstado: `Retrasada por ${Math.abs(diffHoy)} día(s) hábiles`,
    };
  }

  if (diffHoy <= 3) {
    return {
      semaforo: 'naranja',
      diasDiferencia: diffHoy,
      esAtrasado: false,
      textoEstado: diffHoy === 0 ? 'Vence hoy (días hábiles)' : `Por vencer: faltan ${diffHoy} día(s) hábiles`,
    };
  }

  return {
    semaforo: 'verde',
    diasDiferencia: diffHoy,
    esAtrasado: false,
    textoEstado: `A tiempo (faltan ${diffHoy} días hábiles)`,
  };
}

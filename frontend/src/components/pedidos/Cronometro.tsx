import { CircleAlertIcon, CircleMinusIcon, TimerIcon } from 'lucide-react';
import { useEffect, useMemo, useState } from 'react';
import type { EstadoPlazo } from '@/api/pedidos';
import { Badge } from '@/components/ui/badge';
import { cn } from '@/lib/utils';

// Cuenta regresiva de la etapa activa. El vencimiento y la hora salen del servidor: con `servidorAhora` se corrige la
// diferencia con el reloj del equipo, así que un reloj desfasado no cambia el resultado. Es tiempo corrido hasta el
// vencimiento; las horas hábiles ya están incluidas en esa fecha.

const UN_DIA = 86_400_000;

export function duracionCronometro(milisegundos: number): string {
  const minutos = Math.floor(Math.abs(milisegundos) / 60_000);
  const dias = Math.floor(minutos / 1440);
  const horas = Math.floor((minutos % 1440) / 60);
  const resto = minutos % 60;
  return `${dias > 0 ? `${dias} d ` : ''}${String(horas).padStart(2, '0')} h ${String(resto).padStart(2, '0')} min`;
}

/** Hora del servidor que avanza cada segundo: corrige el desfase del reloj del equipo con `servidorAhora`. */
export function useAhoraServidor(servidorAhora: string | null | undefined, intervalo = 1000) {
  const desfase = useMemo(
    () => (servidorAhora ? new Date(servidorAhora).getTime() - Date.now() : 0),
    [servidorAhora],
  );
  const [ahora, setAhora] = useState(() => Date.now() + desfase);
  useEffect(() => {
    setAhora(Date.now() + desfase);
    if (intervalo <= 0) return undefined;
    const reloj = setInterval(() => setAhora(Date.now() + desfase), intervalo);
    return () => clearInterval(reloj);
  }, [desfase, intervalo]);
  return ahora;
}

/** Reloj que corre segundo a segundo: «3 d 04:12:09» o «13:33:05». */
export function duracionReloj(milisegundos: number): string {
  const segundos = Math.floor(Math.abs(milisegundos) / 1000);
  const dias = Math.floor(segundos / 86_400);
  const dos = (n: number) => String(n).padStart(2, '0');
  const reloj = `${dos(Math.floor((segundos % 86_400) / 3600))}:${dos(Math.floor((segundos % 3600) / 60))}:${dos(segundos % 60)}`;
  return dias > 0 ? `${dias} d ${reloj}` : reloj;
}

interface Props {
  venceEn: string | null | undefined;
  servidorAhora: string | null | undefined;
  estadoPlazo: EstadoPlazo | undefined;
}

export function Cronometro({ venceEn, servidorAhora, estadoPlazo }: Props) {
  // Cuenta regresiva viva: avanza cada segundo.
  const ahora = useAhoraServidor(servidorAhora);

  if (!venceEn || estadoPlazo === 'SIN_PLAZO' || estadoPlazo === 'PENDIENTE') {
    return (
      <Badge variant="outline" className="h-auto gap-1 rounded-sm border-transparent bg-estado-neutro-fondo py-0.5 text-sm text-estado-neutro">
        <CircleMinusIcon aria-hidden="true" />
        Sin plazo
      </Badge>
    );
  }
  const resto = new Date(venceEn).getTime() - ahora;
  const vencido = resto < 0;
  return (
    <Badge
      variant="outline"
      className={cn(
        'h-auto gap-1 rounded-sm border-transparent py-0.5 text-sm whitespace-normal tabular-nums',
        vencido ? 'bg-estado-vencido-fondo text-estado-vencido'
          : resto < UN_DIA ? 'bg-estado-aviso-fondo text-estado-aviso' : 'bg-estado-ok-fondo text-estado-ok',
      )}
    >
      {vencido ? <CircleAlertIcon aria-hidden="true" /> : <TimerIcon aria-hidden="true" />}
      {vencido ? `Vencido hace ${duracionReloj(resto)}` : `Quedan ${duracionReloj(resto)}`}
    </Badge>
  );
}

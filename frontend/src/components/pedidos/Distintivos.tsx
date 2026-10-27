import { ArrowUpIcon, CircleAlertIcon, CircleCheckIcon, CircleMinusIcon, ClockIcon, FlagIcon, ZapIcon } from 'lucide-react';
import type { EstadoPlazo, NivelUrgencia } from '@/api/pedidos';
import { Badge } from '@/components/ui/badge';
import { cn } from '@/lib/utils';
import { formatearFechaHora, HORAS_URGENCIA, NOMBRE_URGENCIA } from './formato';

// Badges de urgencia y de vencimiento (DESIGN.md 3.4 y 7.6). El estado nunca depende solo del color: siempre lleva
// ícono y texto. Los colores salen de los tokens de estado de index.css.

const TONOS = {
  ok: 'border-transparent bg-estado-ok-fondo text-estado-ok',
  aviso: 'border-transparent bg-estado-aviso-fondo text-estado-aviso',
  vencido: 'border-transparent bg-estado-vencido-fondo text-estado-vencido',
  neutro: 'border-transparent bg-estado-neutro-fondo text-estado-neutro',
  urgente: 'border-marca-naranja bg-naranja-50 text-marca-naranja-texto',
} as const;

// DESIGN.md 5: los badges usan el radio pequeño (6 px), no la píldora por defecto de shadcn.
const BASE = 'h-auto gap-1 rounded-sm py-0.5 text-sm [&>svg]:size-3.5!';

/**
 * @param venceEn vencimiento propio de la urgencia, si se conoce (el historial no lo trae)
 * @param vencida la urgencia ya venció según el servidor (paso 5.3); se dice con texto e ícono, no solo con color
 */
export function DistintivoUrgencia({
  nivel,
  venceEn,
  vencida = false,
}: {
  nivel: NivelUrgencia;
  venceEn?: string | null;
  vencida?: boolean;
}) {
  const Icono = vencida ? CircleAlertIcon : nivel === 'URGENTE' ? ZapIcon : nivel === 'PRIORITARIO' ? ArrowUpIcon : ClockIcon;
  const tono = vencida ? 'vencido' : nivel === 'URGENTE' ? 'urgente' : nivel === 'PRIORITARIO' ? 'aviso' : 'neutro';
  return (
    <Badge
      variant="outline"
      className={cn(BASE, TONOS[tono])}
      title={venceEn ? `La urgencia vence el ${formatearFechaHora(venceEn)} (hora de Ecuador)` : undefined}
    >
      <Icono aria-hidden="true" />
      {NOMBRE_URGENCIA[nivel]} · {HORAS_URGENCIA[nivel]} h{vencida ? ' · vencida' : ''}
    </Badge>
  );
}

/**
 * Vencimiento de la etapa actual con lo que da el servidor: `vencida` y `venceEn`. "Por vencer" necesita contar días
 * hábiles (ADR-0006) y el contrato no lo entrega; no se calcula en el navegador.
 */
export function DistintivoVencimiento({
  venceEn,
  vencida,
  finalizado,
  estadoPlazo,
}: {
  venceEn: string | null | undefined;
  vencida: boolean;
  finalizado: boolean;
  estadoPlazo?: EstadoPlazo;
}) {
  if (finalizado) {
    return (
      <Badge variant="outline" className={cn(BASE, TONOS.neutro)}>
        <FlagIcon aria-hidden="true" />
        Finalizado
      </Badge>
    );
  }
  if (estadoPlazo === 'PENDIENTE') {
    return <Badge variant="outline" className={cn(BASE, TONOS.aviso)}><ClockIcon aria-hidden="true" />Plazo pendiente del cliente</Badge>;
  }
  if (vencida) {
    return (
      <Badge variant="outline" className={cn(BASE, TONOS.vencido)}>
        <CircleAlertIcon aria-hidden="true" />
        Vencida · {formatearFechaHora(venceEn)}
      </Badge>
    );
  }
  if (venceEn) {
    return (
      <Badge variant="outline" className={cn(BASE, TONOS.ok)}>
        <CircleCheckIcon aria-hidden="true" />
        A tiempo · vence {formatearFechaHora(venceEn)}
      </Badge>
    );
  }
  return (
    <Badge variant="outline" className={cn(BASE, TONOS.neutro)}>
      <CircleMinusIcon aria-hidden="true" />
      Sin vencimiento
    </Badge>
  );
}

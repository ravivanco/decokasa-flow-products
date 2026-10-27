import { AlarmClockIcon, BellIcon, TruckIcon } from 'lucide-react';
import { useState } from 'react';
import { Link } from 'react-router';
import { type Alerta, useAlertas } from '@/api/estadisticas';
import { esAdmin, useUsuarioActual } from '@/auth/SesionProvider';
import { formatearFecha, formatearFechaHora, mensajeDeErrorPedido } from '@/components/pedidos/formato';
import { Alert, AlertDescription } from '@/components/ui/alert';
import { Badge } from '@/components/ui/badge';
import { Button } from '@/components/ui/button';
import { Skeleton } from '@/components/ui/skeleton';
import { cn } from '@/lib/utils';

// Alertas y retrasos (ADR-0029): en Mi bandeja y, resumidas, en la campana de la cabecera; las dos usan la misma
// consulta (mismas reglas). Retrasos de las etapas a mi cargo (P-06), sin correos ni estados leído/descartado: una
// alerta desaparece cuando deja de cumplirse y un pedido finalizado nunca aparece. Tipos distintos, siempre con icono
// y texto además del color.

export const TIPO: Record<Alerta['tipo'], { texto: string; icono: typeof BellIcon; tono: string }> = {
  URGENCIA_VENCIDA: { texto: 'Urgencia vencida', icono: AlarmClockIcon, tono: 'border-estado-vencido text-estado-vencido' },
  ETAPA_VENCIDA: { texto: 'Etapa vencida', icono: AlarmClockIcon, tono: 'border-estado-vencido text-estado-vencido' },
  DEMORA_REPORTADA: { texto: 'Demora reportada', icono: TruckIcon, tono: 'border-estado-aviso text-estado-aviso' },
};

/** Tiempo transcurrido desde el vencimiento, en días u horas. */
export function transcurrido(desde: string, ahora = Date.now()): string {
  const horas = Math.max(0, Math.floor((ahora - new Date(desde).getTime()) / 3_600_000));
  if (horas < 24) return `hace ${horas} h`;
  const dias = Math.floor(horas / 24);
  return `hace ${dias} ${dias === 1 ? 'día' : 'días'}`;
}

export function MisAlertas({ className }: { className?: string }) {
  const usuario = useUsuarioActual();
  const admin = esAdmin(usuario);
  const [ambito, setAmbito] = useState<'MIAS' | 'TODAS'>('MIAS');
  const alertas = useAlertas(ambito);
  const lista = alertas.data ?? [];
  const visibles = lista;

  return (
    <section id="alertas" tabIndex={-1} aria-labelledby="mis-alertas" className={cn('flex scroll-mt-4 flex-col gap-3 rounded-lg border bg-card p-4 outline-none', className)}>
      <div className="flex flex-wrap items-center justify-between gap-2">
        <h2 id="mis-alertas" className="flex items-center gap-2 font-heading text-base font-semibold">
          <BellIcon aria-hidden="true" className="size-4" />
          {ambito === 'MIAS' ? 'Retrasos y demoras' : 'Retrasos y demoras de todo mi ámbito'}
          {alertas.data && <Badge variant={lista.length > 0 ? 'destructive' : 'secondary'}>{lista.length}</Badge>}
        </h2>
        {admin && (
          <div role="group" aria-label="Ámbito de las alertas" className="flex gap-1">
            <Button size="sm" variant={ambito === 'MIAS' ? 'default' : 'outline'} aria-pressed={ambito === 'MIAS'} onClick={() => setAmbito('MIAS')}>Mías</Button>
            <Button size="sm" variant={ambito === 'TODAS' ? 'default' : 'outline'} aria-pressed={ambito === 'TODAS'} onClick={() => setAmbito('TODAS')}>Todas</Button>
          </div>
        )}
      </div>
      {alertas.isPending && <Skeleton className="h-16 w-full" />}
      {alertas.isError && (
        <Alert variant="destructive">
          <AlertDescription>{mensajeDeErrorPedido(alertas.error, 'No se pudieron cargar las alertas.')}</AlertDescription>
        </Alert>
      )}
      {alertas.data && lista.length === 0 && (
        <p className="text-sm text-muted-foreground">
          {ambito === 'MIAS' ? 'No tienes retrasos ni demoras en las etapas a tu cargo.' : 'No hay alertas activas en tu ámbito.'}
        </p>
      )}
      {visibles.length > 0 && (
        <ul className="flex flex-col divide-y">
          {visibles.map((a) => {
            const tipo = TIPO[a.tipo];
            const Icono = tipo.icono;
            return (
              <li key={`${a.pedidoId}-${a.tipo}`} className="flex flex-wrap items-start justify-between gap-2 py-2 first:pt-0">
                <div className="flex min-w-0 flex-col gap-0.5">
                  <p className="flex flex-wrap items-center gap-2 text-sm">
                    <span className={cn('inline-flex items-center gap-1 rounded border px-1.5 py-0.5 text-xs font-medium', tipo.tono)}>
                      <Icono aria-hidden="true" className="size-3.5" />
                      {tipo.texto}
                    </span>
                    <Link to={`/pedidos/${a.pedidoId}`} className="font-medium underline-offset-2 hover:underline">{a.numero}</Link>
                    <span className="truncate text-muted-foreground">{a.nombrePedido}</span>
                  </p>
                  <p className="text-xs text-muted-foreground">
                    {a.tipoFlujo}{a.etapa} · {a.nombreEtapa} · {a.pais}
                    {a.asignadoA ? ` · ${a.asignadoA}` : ''}
                  </p>
                  {a.motivo && <p className="text-xs">Motivo: {a.motivo}</p>}
                </div>
                <p className="shrink-0 text-right text-xs tabular-nums">
                  {a.vence ? (
                    <>
                      <span className="block font-medium text-estado-vencido">Venció {transcurrido(a.vence)}</span>
                      <span className="text-muted-foreground">{formatearFechaHora(a.vence)}</span>
                    </>
                  ) : (
                    <span className="text-muted-foreground">
                      Nueva llegada estimada: {a.nuevaFechaLlegada ? formatearFecha(a.nuevaFechaLlegada) : 'sin fecha'}
                    </span>
                  )}
                </p>
              </li>
            );
          })}
        </ul>
      )}
    </section>
  );
}

import { ArrowRightIcon, BellDotIcon, CheckCheckIcon, CircleCheckBigIcon, CornerDownLeftIcon, FilePlus2Icon, LogInIcon, UserRoundCogIcon } from 'lucide-react';
import { Link } from 'react-router';
import { type Aviso, useAvisos, useMarcarAvisosLeidos } from '@/api/avisos';
import { formatearFechaHora } from '@/components/pedidos/formato';
import { Badge } from '@/components/ui/badge';
import { Button } from '@/components/ui/button';
import { Skeleton } from '@/components/ui/skeleton';
import { transcurrido } from '@/components/dashboards/MisAlertas';
import { cn } from '@/lib/utils';

// Avisos de etapa (ADR-0032) en Mi bandeja. Son los mismos de la campana (misma consulta y misma marca de lectura); los
// retrasos y las demoras son otra sección. Cada aviso dice qué pasó, entre qué etapas, quién lo hizo, cuándo y, en las
// devoluciones, el motivo.

const ICONO = {
  CREACION: FilePlus2Icon,
  LLEGADA: LogInIcon,
  DEVOLUCION: CornerDownLeftIcon,
  REASIGNACION: UserRoundCogIcon,
  CIERRE: CircleCheckBigIcon,
} as const;

/** Qué pasó, con palabras (además del icono). */
const TIPO_AVISO: Record<Aviso['tipo'], string> = {
  CREACION: 'Pedido creado',
  LLEGADA: 'Llegó a una etapa',
  DEVOLUCION: 'Devuelto',
  REASIGNACION: 'Reasignado',
  CIERRE: 'Finalizado',
};

export function tituloAviso(aviso: Aviso): string {
  const destino = `${aviso.tipoFlujo}${aviso.etapaDestino} ${aviso.nombreDestino}`;
  switch (aviso.tipo) {
    case 'CREACION':
      return `${aviso.numero} creado en ${destino}`;
    case 'LLEGADA':
      return `${aviso.numero} llegó a ${destino}`;
    case 'DEVOLUCION':
      return `${aviso.numero} volvió a ${destino}`;
    case 'REASIGNACION':
      return `${aviso.numero} reasignado en ${destino}`;
    case 'CIERRE':
      return `${aviso.numero} finalizado`;
  }
}

export function detalleAviso(aviso: Aviso): string {
  const partes: string[] = [];
  if (aviso.etapaOrigen != null && aviso.tipo !== 'REASIGNACION') {
    partes.push(`${aviso.tipo === 'DEVOLUCION' ? 'Devuelto desde' : 'Desde'} ${aviso.tipoFlujo}${aviso.etapaOrigen} ${aviso.nombreOrigen ?? ''}`.trim());
  }
  if (aviso.actor) partes.push(aviso.actor);
  partes.push(haceCuanto(aviso.ocurridoEn));
  return partes.join(' · ');
}

export function IconoAviso({ aviso, className }: { aviso: Aviso; className?: string }) {
  const Icono = ICONO[aviso.tipo];
  return (
    <Icono
      aria-hidden="true"
      className={cn(aviso.tipo === 'DEVOLUCION' ? 'text-estado-aviso' : aviso.tipo === 'CIERRE' ? 'text-estado-ok' : 'text-primary', className)}
    />
  );
}

export function haceCuanto(momento: string, ahora = Date.now()): string {
  const minutos = Math.max(0, Math.floor((ahora - new Date(momento).getTime()) / 60_000));
  if (minutos < 1) return 'hace un momento';
  if (minutos < 60) return `hace ${minutos} min`;
  return transcurrido(momento, ahora);
}

export function AvisosDeEtapa({ className }: { className?: string }) {
  const avisos = useAvisos();
  const marcar = useMarcarAvisosLeidos();
  const lista = avisos.data?.avisos ?? [];
  const noLeidos = avisos.data?.noLeidos ?? 0;

  return (
    <section id="avisos" tabIndex={-1} aria-labelledby="avisos-etapa" className={cn('flex scroll-mt-4 flex-col gap-3 rounded-lg border bg-card p-4 outline-none', className)}>
      <div className="flex flex-wrap items-center justify-between gap-2">
        <h2 id="avisos-etapa" className="flex items-center gap-2 font-heading text-base font-semibold">
          <BellDotIcon aria-hidden="true" className="size-4" />
          Avisos de etapa
          {avisos.data && (
            <Badge variant={noLeidos > 0 ? 'default' : 'secondary'}>
              {noLeidos > 0 ? `${noLeidos} ${noLeidos === 1 ? 'nuevo' : 'nuevos'}` : lista.length}
            </Badge>
          )}
        </h2>
        {noLeidos > 0 && (
          <Button size="sm" variant="outline" disabled={marcar.isPending} onClick={() => marcar.mutate()}>
            <CheckCheckIcon data-icon="inline-start" />
            Marcar como leídos
          </Button>
        )}
      </div>

      {avisos.isPending ? (
        <div className="flex flex-col gap-2" aria-busy="true" aria-label="Cargando los avisos">
          <Skeleton className="h-12 w-full" />
          <Skeleton className="h-12 w-full" />
        </div>
      ) : avisos.isError ? (
        <p className="text-sm text-destructive">No se pudieron cargar los avisos. Se reintentará en la próxima actualización.</p>
      ) : lista.length === 0 ? (
        <p className="text-sm text-muted-foreground">No hay avisos de etapa en los últimos 30 días.</p>
      ) : (
        <ul className="flex max-h-[30rem] flex-col divide-y overflow-y-auto rounded-md border">
          {lista.map((aviso) => (
            <li key={aviso.eventoId}>
              <Link
                to={`/pedidos/${aviso.pedidoId}`}
                className={cn('grid grid-cols-[auto_minmax(0,1fr)] items-start gap-x-3 gap-y-1 px-3 py-2.5 outline-none hover:bg-morado-50 focus-visible:ring-3 focus-visible:ring-ring focus-visible:ring-inset', !aviso.leido && 'bg-accent/40')}
              >
                <span className={cn('mt-0.5 flex size-7 items-center justify-center rounded-full', aviso.tipo === 'DEVOLUCION' ? 'bg-estado-aviso-fondo' : aviso.tipo === 'CIERRE' ? 'bg-estado-ok-fondo' : 'bg-secondary')}>
                  <IconoAviso aviso={aviso} className="size-4" />
                </span>
                <span className="flex min-w-0 flex-col gap-0.5">
                  <span className="flex flex-wrap items-center gap-x-2 gap-y-0.5">
                    {!aviso.leido && <Badge className="h-auto rounded-sm py-0 text-xs">Nuevo</Badge>}
                    <span className="text-xs font-medium text-muted-foreground">{TIPO_AVISO[aviso.tipo]}</span>
                    <span className="text-xs text-muted-foreground">· {aviso.comoResponsable ? 'Tu etapa' : 'Como administración'}</span>
                  </span>
                  <span className={cn('text-sm', !aviso.leido ? 'font-semibold' : 'font-medium')}>{tituloAviso(aviso)}</span>
                  <span className="text-xs text-muted-foreground">
                    {detalleAviso(aviso)} ·{' '}
                    <time dateTime={aviso.ocurridoEn} className="tabular-nums">{formatearFechaHora(aviso.ocurridoEn)}</time>
                  </span>
                  {aviso.motivo && (
                    <span className={cn('mt-0.5 rounded-sm px-2 py-1 text-xs', aviso.tipo === 'DEVOLUCION' ? 'bg-estado-aviso-fondo' : 'bg-muted')}>
                      <span className="font-medium">{aviso.tipo === 'DEVOLUCION' ? 'Motivo: ' : 'Observación: '}</span>
                      {aviso.motivo}
                    </span>
                  )}
                  <span className="mt-0.5 inline-flex items-center gap-1 text-xs font-medium text-primary">
                    Abrir el pedido <ArrowRightIcon aria-hidden="true" className="size-3" />
                  </span>
                </span>
              </Link>
            </li>
          ))}
        </ul>
      )}
    </section>
  );
}

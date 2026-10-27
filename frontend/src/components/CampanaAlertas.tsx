import { BellIcon } from 'lucide-react';
import { useState } from 'react';
import { Link } from 'react-router';
import { type Aviso, useAvisos, useMarcarAvisosLeidos } from '@/api/avisos';
import { useAlertas } from '@/api/estadisticas';
import { IconoAviso, detalleAviso, tituloAviso } from '@/components/dashboards/AvisosDeEtapa';
import { TIPO, transcurrido } from '@/components/dashboards/MisAlertas';
import { Button } from '@/components/ui/button';
import {
  DropdownMenu,
  DropdownMenuContent,
  DropdownMenuGroup,
  DropdownMenuItem,
  DropdownMenuLabel,
  DropdownMenuSeparator,
  DropdownMenuTrigger,
} from '@/components/ui/dropdown-menu';
import { cn } from '@/lib/utils';

// Campana (ADR-0029 y ADR-0032). Dos cosas distintas:
// - Avisos de etapa: cada transición para la admin y el trabajo que me llega a mí. Son los mismos de Mi bandeja. Abrir
//   la campana los marca como leídos en el servidor; mientras sigue abierta, los nuevos se ven resaltados.
// - Retrasos y demoras: las alertas activas de Mi bandeja (misma consulta y misma caché). Siguen mientras dure el
//   retraso, así que no se "leen".
// El contador suma los avisos no leídos y las alertas activas.

const MAXIMO_AVISOS = 6;
const MAXIMO_ALERTAS = 3;

export function CampanaAlertas() {
  const alertas = useAlertas('MIAS');
  const avisos = useAvisos();
  const marcar = useMarcarAvisosLeidos();
  // Avisos no leídos en el momento de abrir: siguen resaltados aunque el servidor ya los marcó.
  const [nuevosAlAbrir, setNuevosAlAbrir] = useState<Set<string>>(new Set());

  const listaAlertas = alertas.data ?? [];
  const listaAvisos = avisos.data?.avisos ?? [];
  const noLeidos = avisos.data?.noLeidos ?? 0;
  const total = noLeidos + listaAlertas.length;
  const etiqueta =
    alertas.isError && avisos.isError
      ? 'Notificaciones: no se pudieron cargar'
      : `Notificaciones: ${noLeidos} ${noLeidos === 1 ? 'aviso nuevo' : 'avisos nuevos'} y ${listaAlertas.length} ${
          listaAlertas.length === 1 ? 'alerta activa' : 'alertas activas'
        }`;

  function alCambiar(abierta: boolean) {
    if (!abierta) {
      setNuevosAlAbrir(new Set());
      return;
    }
    setNuevosAlAbrir(new Set(listaAvisos.filter((a) => !a.leido).map((a) => a.eventoId)));
    if (noLeidos > 0) marcar.mutate();
  }

  return (
    <DropdownMenu onOpenChange={alCambiar}>
      <DropdownMenuTrigger asChild>
        <Button variant="secondary" size="sm" className="relative px-2.5" aria-label={etiqueta}>
          <BellIcon />
          {total > 0 && (
            <span aria-hidden="true" className="absolute -top-1 -right-1 flex h-5 min-w-5 items-center justify-center rounded-full bg-estado-vencido px-1 text-[0.7rem] font-semibold text-white tabular-nums">
              {total > 99 ? '99+' : total}
            </span>
          )}
        </Button>
      </DropdownMenuTrigger>
      <DropdownMenuContent align="end" className="max-h-[min(36rem,calc(100vh-5rem))] w-88 max-w-[calc(100vw-2rem)] overflow-y-auto">
        <DropdownMenuLabel>Avisos de etapa</DropdownMenuLabel>
        {avisos.isPending && <p className="px-2 py-1.5 text-sm text-muted-foreground">Cargando…</p>}
        {avisos.isError && <p className="px-2 py-1.5 text-sm text-destructive">No se pudieron cargar los avisos.</p>}
        {avisos.data && listaAvisos.length === 0 && (
          <p className="px-2 py-1.5 text-sm text-muted-foreground">
            No hay avisos de etapa en los últimos 30 días.
          </p>
        )}
        {listaAvisos.length > 0 && (
          <DropdownMenuGroup>
            {listaAvisos.slice(0, MAXIMO_AVISOS).map((aviso) => (
              <ElementoAviso key={aviso.eventoId} aviso={aviso} nuevo={!aviso.leido || nuevosAlAbrir.has(aviso.eventoId)} />
            ))}
          </DropdownMenuGroup>
        )}

        <DropdownMenuSeparator />
        <DropdownMenuLabel>{alertas.data ? `Retrasos y demoras (${listaAlertas.length})` : 'Retrasos y demoras'}</DropdownMenuLabel>
        {alertas.isPending && <p className="px-2 py-1.5 text-sm text-muted-foreground">Cargando…</p>}
        {alertas.isError && <p className="px-2 py-1.5 text-sm text-destructive">No se pudieron cargar las alertas.</p>}
        {alertas.data && listaAlertas.length === 0 && (
          <p className="px-2 py-1.5 text-sm text-muted-foreground">No tienes retrasos ni demoras en tus etapas.</p>
        )}
        {listaAlertas.length > 0 && (
          <DropdownMenuGroup>
            {listaAlertas.slice(0, MAXIMO_ALERTAS).map((a) => {
              const tipo = TIPO[a.tipo];
              const Icono = tipo.icono;
              return (
                <DropdownMenuItem key={`${a.pedidoId}-${a.tipo}`} asChild>
                  <Link to={`/pedidos/${a.pedidoId}`} className="flex items-start gap-2">
                    <Icono aria-hidden="true" className={tipo.tono.split(' ').find((c) => c.startsWith('text-'))} />
                    <span className="flex min-w-0 flex-col">
                      <span className="text-sm font-medium">{a.numero} · {tipo.texto}</span>
                      <span className="truncate text-xs text-muted-foreground">
                        {a.tipoFlujo}{a.etapa} {a.nombreEtapa}{a.vence ? ` · venció ${transcurrido(a.vence)}` : ''}
                      </span>
                    </span>
                  </Link>
                </DropdownMenuItem>
              );
            })}
          </DropdownMenuGroup>
        )}
        <DropdownMenuSeparator />
        <DropdownMenuItem asChild>
          <Link to="/bandeja#avisos" className="font-medium">
            {listaAvisos.length > MAXIMO_AVISOS ? `Ver los ${listaAvisos.length} avisos en Mi bandeja` : 'Avisos en Mi bandeja'}
          </Link>
        </DropdownMenuItem>
        <DropdownMenuItem asChild>
          <Link to="/bandeja#alertas" className="font-medium">
            {listaAlertas.length > MAXIMO_ALERTAS ? `Ver las ${listaAlertas.length} alertas en Mi bandeja` : 'Ir a Mi bandeja'}
          </Link>
        </DropdownMenuItem>
      </DropdownMenuContent>
    </DropdownMenu>
  );
}

function ElementoAviso({ aviso, nuevo }: { aviso: Aviso; nuevo: boolean }) {
  return (
    <DropdownMenuItem asChild>
      <Link to={`/pedidos/${aviso.pedidoId}`} className={cn('flex items-start gap-2', nuevo && 'bg-accent/60')}>
        <IconoAviso aviso={aviso} />
        <span className="flex min-w-0 flex-col">
          <span className="text-sm font-medium">
            {tituloAviso(aviso)}
            {nuevo && <span className="sr-only"> (nuevo)</span>}
          </span>
          <span className="truncate text-xs text-muted-foreground">{detalleAviso(aviso)}</span>
          {aviso.tipo === 'DEVOLUCION' && aviso.motivo && (
            <span className="line-clamp-2 text-xs">Motivo: {aviso.motivo}</span>
          )}
        </span>
        {nuevo && <span aria-hidden="true" className="mt-1.5 ml-auto size-2 shrink-0 rounded-full bg-primary" />}
      </Link>
    </DropdownMenuItem>
  );
}

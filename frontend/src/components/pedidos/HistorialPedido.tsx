import {
  ArrowRightLeftIcon,
  CheckIcon,
  FileUpIcon,
  FlagIcon,
  type LucideIcon,
  MessageSquareTextIcon,
  PaperclipIcon,
  PlusIcon,
  SendIcon,
  Undo2Icon,
  XIcon,
} from 'lucide-react';
import { type EventoHistorial, type Persona, type TipoEvento, useAdjuntosDeEtapas, useHistorial } from '@/api/pedidos';
import { ciudadDeBodega, type Bodega } from '@/api/esquemas';
import { Alert, AlertDescription } from '@/components/ui/alert';
import { Button } from '@/components/ui/button';
import { Skeleton } from '@/components/ui/skeleton';
import { cn } from '@/lib/utils';
import { DistintivoUrgencia } from './Distintivos';
import { codigoEtapa, formatearFecha, formatearFechaHora, mensajeDeErrorPedido, NOMBRE_EVENTO, nombreRol } from './formato';

// Historial del pedido (requisito 4.2: solo inserción). Del más reciente al más antiguo, como lo devuelve el servidor,
// con el actor y el rol con el que intervino en ese momento (aunque después cambien sus roles). Los archivos de cada
// evento se nombran con los metadatos de su etapa.

const ICONO: Partial<Record<TipoEvento, LucideIcon>> = {
  PEDIDO_CREADO: PlusIcon,
  ETAPA_ENVIADA: SendIcon,
  ETAPA_APROBADA: CheckIcon,
  ETAPA_RECHAZADA: XIcon,
  PEDIDO_DEVUELTO: Undo2Icon,
  OBSERVACION_AGREGADA: MessageSquareTextIcon,
  ARCHIVO_SUBIDO: FileUpIcon,
  ETAPA_REASIGNADA: ArrowRightLeftIcon,
  PEDIDO_FINALIZADO: FlagIcon,
};

const TONO: Partial<Record<TipoEvento, string>> = {
  ETAPA_RECHAZADA: 'bg-estado-vencido-fondo text-estado-vencido',
  PEDIDO_DEVUELTO: 'bg-estado-aviso-fondo text-estado-aviso',
  ETAPA_APROBADA: 'bg-estado-ok-fondo text-estado-ok',
  ETAPA_ENVIADA: 'bg-secondary text-primary',
};

interface Props {
  pedidoId: string;
  tipoFlujo: string;
  /** Personas conocidas (asignadas en la línea de tiempo) para nombrar las reasignaciones. */
  personas: Map<string, Persona>;
  bodega?: Bodega | null;
}

export function HistorialPedido({ pedidoId, tipoFlujo, personas, bodega }: Props) {
  const historial = useHistorial(pedidoId);
  const etapasConArchivos = [
    ...new Set((historial.data ?? []).filter((e) => (e.adjuntoIds?.length ?? 0) > 0).map((e) => e.etapa)),
  ].sort((a, b) => a - b);
  const adjuntos = useAdjuntosDeEtapas(pedidoId, etapasConArchivos);

  return (
    <section aria-labelledby="historial-titulo" className="flex flex-col gap-4">
      <h2 id="historial-titulo" className="font-heading text-xl font-semibold">
        Historial
      </h2>
      {historial.isPending ? (
        <div className="flex flex-col gap-3" aria-busy="true" aria-label="Cargando el historial">
          {[0, 1, 2].map((fila) => (
            <Skeleton key={fila} className="h-16 w-full" />
          ))}
        </div>
      ) : historial.isError ? (
        <Alert variant="destructive">
          <AlertDescription className="flex flex-wrap items-center justify-between gap-3">
            <p>{mensajeDeErrorPedido(historial.error, 'No se pudo cargar el historial.')}</p>
            <Button variant="outline" size="sm" onClick={() => void historial.refetch()}>
              Reintentar
            </Button>
          </AlertDescription>
        </Alert>
      ) : historial.data.length === 0 ? (
        <p className="text-sm text-muted-foreground">Sin eventos todavía.</p>
      ) : (
        <ol className="flex flex-col">
          {historial.data.map((evento, indice) => (
            <Evento
              key={evento.id}
              evento={evento}
              tipoFlujo={tipoFlujo}
              ultimo={indice === historial.data.length - 1}
              nombreArchivo={(id) => adjuntos.get(id)?.nombre}
              personas={personas}
              bodega={bodega}
            />
          ))}
        </ol>
      )}
    </section>
  );
}

function Evento({
  evento,
  tipoFlujo,
  ultimo,
  nombreArchivo,
  personas,
  bodega,
}: {
  evento: EventoHistorial;
  tipoFlujo: string;
  ultimo: boolean;
  nombreArchivo: (id: string) => string | undefined;
  personas: Map<string, Persona>;
  bodega?: Bodega | null;
}) {
  const Icono = ICONO[evento.tipo] ?? MessageSquareTextIcon;
  const detalle = evento.detalle;
  const archivos = evento.adjuntoIds ?? [];
  const nombrePersona = (id: string | null | undefined) => (id ? (personas.get(id)?.nombre ?? 'otra persona') : null);
  const anterior = nombrePersona(detalle?.asignadoAnterior);
  const nueva = nombrePersona(detalle?.asignadoA) ?? 'otra persona';

  return (
    <li className="relative flex gap-3 pb-5">
      {!ultimo && <span aria-hidden="true" className="absolute top-9 bottom-0 left-4 w-px bg-border" />}
      <span
        aria-hidden="true"
        className={cn(
          'relative z-10 flex size-8 shrink-0 items-center justify-center rounded-full bg-muted text-muted-foreground',
          TONO[evento.tipo],
        )}
      >
        <Icono className="size-4" />
      </span>
      <div className="flex min-w-0 flex-1 flex-col gap-1.5 pt-1">
        <div className="flex flex-wrap items-baseline justify-between gap-x-3 gap-y-0.5">
          <p className="font-medium">
            {NOMBRE_EVENTO[evento.tipo]}
            <span className="font-normal text-muted-foreground tabular-nums">
              {' · '}
              {codigoEtapa(tipoFlujo, evento.etapa)}
              {detalle?.etapaDestino ? ` → ${codigoEtapa(tipoFlujo, detalle.etapaDestino)}` : ''}
            </span>
          </p>
          <time dateTime={evento.fecha} className="text-sm text-muted-foreground tabular-nums">
            {formatearFechaHora(evento.fecha)}
          </time>
        </div>
        <p className="text-sm text-muted-foreground">
          {evento.usuario
            ? `${evento.usuario.nombre}${evento.rolIntervencion ? ` · ${nombreRol(evento.rolIntervencion)}` : ''}`
            : 'Sistema'}
        </p>
        {(evento.nivelUrgencia || detalle?.motivoDevolucion || detalle?.urgenciaTerminada) && (
          <div className="flex flex-wrap items-center gap-2 text-sm">
            {detalle?.motivoDevolucion && (
              <span>{detalle.motivoDevolucion === 'ERROR' ? 'Devolución por error' : 'Devolución por producto nuevo'}</span>
            )}
            {evento.nivelUrgencia && <DistintivoUrgencia nivel={evento.nivelUrgencia} />}
            {detalle?.urgenciaTerminada && <span className="text-estado-ok">La urgencia terminó</span>}
          </div>
        )}
        {evento.tipo === 'ETAPA_REASIGNADA' && (
          <p className="text-sm">{anterior ? `De ${anterior} a ${nueva}` : `Asignada a ${nueva}`}</p>
        )}
        {evento.tipo === 'DEMORA_REPORTADA' && detalle?.nuevaFechaEstimadaLlegada && (
          <dl className="grid gap-1 text-sm">
            {detalle.fechaEstimadaAnterior && <div><dt className="inline text-muted-foreground">Estimación anterior: </dt><dd className="inline tabular-nums">{formatearFecha(detalle.fechaEstimadaAnterior)}</dd></div>}
            {detalle.fechaEstimadaSolicitada && <div><dt className="inline text-muted-foreground">Fecha solicitada: </dt><dd className="inline tabular-nums">{formatearFecha(detalle.fechaEstimadaSolicitada)}</dd></div>}
            <div><dt className="inline text-muted-foreground">Llegada estimada confirmada: </dt><dd className="inline font-medium tabular-nums">{formatearFecha(detalle.nuevaFechaEstimadaLlegada)}</dd></div>
          </dl>
        )}
        {evento.tipo === 'BODEGA_ASIGNADA' && detalle?.bodegaId && <p className="text-sm text-muted-foreground">
          {bodega && bodega.id === detalle.bodegaId ? `Bodega: ${bodega.nombre} · ${ciudadDeBodega(bodega)} (nombre vigente)` : 'Asignación de bodega registrada por el servidor.'}
        </p>}
        {evento.observacion && (
          <p className="max-w-prose rounded-md bg-muted/60 px-3 py-2 text-sm whitespace-pre-wrap">{evento.observacion}</p>
        )}
        {archivos.length > 0 && (
          <ul aria-label="Archivos del evento" className="flex flex-wrap gap-x-3 gap-y-1 text-sm">
            {archivos.map((id) => (
              <li key={id} className="flex min-w-0 items-center gap-1 text-muted-foreground">
                <PaperclipIcon aria-hidden="true" className="size-3.5 shrink-0" />
                <span className="truncate">{nombreArchivo(id) ?? 'Archivo'}</span>
              </li>
            ))}
          </ul>
        )}
      </div>
    </li>
  );
}

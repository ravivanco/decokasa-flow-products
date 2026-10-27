import {
  ArrowRightLeftIcon,
  CheckIcon,
  FileUpIcon,
  FlagIcon,
  HistoryIcon,
  type LucideIcon,
  MessageSquareTextIcon,
  PaperclipIcon,
  PlusIcon,
  RefreshCwIcon,
  SendIcon,
  Undo2Icon,
  XIcon,
} from 'lucide-react';
import { useState } from 'react';
import {
  type DocumentoPedido,
  type EventoHistorial,
  type Persona,
  type TipoEvento,
  useAdjuntosDeEtapas,
  useDocumentosPedido,
  useHistorial,
  useHistorialEnCache,
} from '@/api/pedidos';
import { useAbridorDocumentos } from './DocumentosPedido';
import { ciudadDeBodega, type Bodega } from '@/api/esquemas';
import { Alert, AlertDescription } from '@/components/ui/alert';
import { Badge } from '@/components/ui/badge';
import { Button } from '@/components/ui/button';
import { Sheet, SheetBody, SheetContent, SheetDescription, SheetHeader, SheetTitle, SheetTrigger } from '@/components/ui/sheet';
import { Skeleton } from '@/components/ui/skeleton';
import { cn } from '@/lib/utils';
import { DistintivoUrgencia } from './Distintivos';
import { codigoEtapa, formatearFecha, formatearFechaHora, mensajeDeErrorPedido, NOMBRE_EVENTO, nombreRol } from './formato';

// Historial del pedido (requisito 4.2: solo inserción). Del más reciente al más antiguo, como lo devuelve el servidor,
// con el actor y el rol con el que intervino en ese momento (aunque después cambien sus roles). Los archivos de cada
// evento se nombran con los metadatos de su etapa.
//
// Mejora visual del 2026-10-27: el historial ya no ocupa la página del pedido. Se abre bajo demanda en un panel lateral
// («Ver historial»), y solo entonces se descarga; la lista se pinta por tramos. Cada evento lleva una clase visible
// (avance, rechazo, devolución, corrección, nota o seguimiento) con icono y texto. Ningún evento se oculta ni cambia:
// los filtros solo eligen qué clase mirar y «Todo» muestra la lista completa.

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

type Clase = 'avance' | 'rechazo' | 'devolucion' | 'correccion' | 'nota' | 'seguimiento';

const CLASE: Record<Clase, { texto: string; tono: string; icono?: LucideIcon }> = {
  avance: { texto: 'Avance', tono: 'bg-secondary text-primary' },
  rechazo: { texto: 'Rechazo', tono: 'bg-estado-vencido-fondo text-estado-vencido' },
  devolucion: { texto: 'Devolución', tono: 'bg-estado-aviso-fondo text-estado-aviso' },
  correccion: { texto: 'Corrección', tono: 'bg-estado-ok-fondo text-estado-ok', icono: RefreshCwIcon },
  nota: { texto: 'Nota o archivo', tono: 'bg-muted text-muted-foreground' },
  seguimiento: { texto: 'Seguimiento', tono: 'bg-muted text-muted-foreground' },
};

type Filtro = 'todo' | 'avances' | 'devoluciones' | 'correcciones' | 'notas';
const FILTROS: { id: Filtro; texto: string; clases: Clase[] | null }[] = [
  { id: 'todo', texto: 'Todo', clases: null },
  { id: 'avances', texto: 'Avances', clases: ['avance'] },
  { id: 'devoluciones', texto: 'Rechazos y devoluciones', clases: ['rechazo', 'devolucion'] },
  { id: 'correcciones', texto: 'Correcciones', clases: ['correccion'] },
  { id: 'notas', texto: 'Notas y archivos', clases: ['nota'] },
];

const TRAMO = 40;

/**
 * Clase de cada evento. «Corrección»: el envío o la aprobación de una etapa que antes había recibido un rechazo o una
 * devolución (el reenvío tras corregir). Se deduce del propio historial, sin cambiar ningún evento.
 */
function clasificar(eventos: EventoHistorial[]): Map<string, Clase> {
  const clases = new Map<string, Clase>();
  const devueltas = new Set<number>();
  for (const evento of [...eventos].reverse()) {
    switch (evento.tipo) {
      case 'ETAPA_RECHAZADA':
      case 'PEDIDO_DEVUELTO':
        clases.set(evento.id, evento.tipo === 'ETAPA_RECHAZADA' ? 'rechazo' : 'devolucion');
        if (evento.detalle?.etapaDestino != null) devueltas.add(evento.detalle.etapaDestino);
        break;
      case 'ETAPA_ENVIADA':
      case 'ETAPA_APROBADA':
        if (devueltas.has(evento.etapa)) {
          clases.set(evento.id, 'correccion');
          devueltas.delete(evento.etapa);
        } else {
          clases.set(evento.id, 'avance');
        }
        break;
      case 'PEDIDO_CREADO':
      case 'PEDIDO_FINALIZADO':
        clases.set(evento.id, 'avance');
        break;
      case 'OBSERVACION_AGREGADA':
      case 'ARCHIVO_SUBIDO':
        clases.set(evento.id, 'nota');
        break;
      default:
        clases.set(evento.id, 'seguimiento');
    }
  }
  return clases;
}

interface Props {
  pedidoId: string;
  numeroPedido: string;
  tipoFlujo: string;
  /** Personas conocidas (asignadas en la línea de tiempo) para nombrar las reasignaciones. */
  personas: Map<string, Persona>;
  bodega?: Bodega | null;
}

/** Botón «Ver historial» y su panel lateral. El historial se pide al abrir (el contenido del panel se monta entonces). */
export function PanelHistorial(props: Props) {
  const enCache = useHistorialEnCache(props.pedidoId);
  const cantidad = enCache.data?.length;
  return (
    <Sheet>
      <SheetTrigger asChild>
        <Button variant="outline" size="sm">
          <HistoryIcon data-icon="inline-start" />
          Ver historial
          {cantidad !== undefined && (
            <Badge variant="secondary" className="tabular-nums" aria-label={`${cantidad} eventos`}>{cantidad}</Badge>
          )}
        </Button>
      </SheetTrigger>
      <SheetContent className="sm:max-w-2xl" textoCerrar="Cerrar el historial">
        <SheetHeader>
          <SheetTitle>Historial de {props.numeroPedido}</SheetTitle>
          
        </SheetHeader>
        <SheetBody>
          <HistorialPedido {...props} />
        </SheetBody>
      </SheetContent>
    </Sheet>
  );
}

export function HistorialPedido({ pedidoId, tipoFlujo, personas, bodega }: Props) {
  const historial = useHistorial(pedidoId);
  const [filtro, setFiltro] = useState<Filtro>('todo');
  const [mostrados, setMostrados] = useState(TRAMO);
  const eventos = historial.data ?? [];
  const etapasConArchivos = [...new Set(eventos.filter((e) => (e.adjuntoIds?.length ?? 0) > 0).map((e) => e.etapa))].sort((a, b) => a - b);
  const adjuntos = useAdjuntosDeEtapas(pedidoId, etapasConArchivos);
  // ADR-0028: los archivos de cada evento se abren desde aquí, con la autorización del backend en cada apertura.
  const documentos = useDocumentosPedido(pedidoId);
  const porId = new Map((documentos.data ?? []).map((documento) => [documento.id, documento]));
  const abridor = useAbridorDocumentos();
  const clases = clasificar(eventos);
  const elegido = FILTROS.find((f) => f.id === filtro) ?? FILTROS[0];
  const filtrados = elegido.clases ? eventos.filter((e) => elegido.clases?.includes(clases.get(e.id) ?? 'seguimiento')) : eventos;
  const visibles = filtrados.slice(0, mostrados);
  const cuenta = (f: (typeof FILTROS)[number]) => (f.clases ? eventos.filter((e) => f.clases?.includes(clases.get(e.id) ?? 'seguimiento')).length : eventos.length);

  if (historial.isPending) {
    return (
      <div className="flex flex-col gap-3" aria-busy="true" aria-label="Cargando el historial">
        {[0, 1, 2].map((fila) => <Skeleton key={fila} className="h-16 w-full" />)}
      </div>
    );
  }
  if (historial.isError) {
    return (
      <Alert variant="destructive">
        <AlertDescription className="flex flex-wrap items-center justify-between gap-3">
          <p>{mensajeDeErrorPedido(historial.error, 'No se pudo cargar el historial.')}</p>
          <Button variant="outline" size="sm" onClick={() => void historial.refetch()}>Reintentar</Button>
        </AlertDescription>
      </Alert>
    );
  }
  if (eventos.length === 0) return <p className="text-sm text-muted-foreground">Sin eventos todavía.</p>;

  return (
    <div className="flex flex-col gap-4">
      <div role="group" aria-label="Qué eventos mostrar" className="flex flex-wrap gap-1.5">
        {FILTROS.map((f) => (
          <Button
            key={f.id}
            size="xs"
            variant={filtro === f.id ? 'default' : 'outline'}
            aria-pressed={filtro === f.id}
            onClick={() => { setFiltro(f.id); setMostrados(TRAMO); }}
          >
            {f.texto}
            <span className="tabular-nums opacity-80">{cuenta(f)}</span>
          </Button>
        ))}
      </div>
      <p className="sr-only" role="status">{`${filtrados.length} eventos en «${elegido.texto}».`}</p>
      {filtrados.length === 0 ? (
        <p className="text-sm text-muted-foreground">No hay eventos de esta clase.</p>
      ) : (
        <ol className="flex flex-col" aria-label="Eventos del pedido">
          {visibles.map((evento, indice) => (
            <Evento
              key={evento.id}
              evento={evento}
              clase={clases.get(evento.id) ?? 'seguimiento'}
              tipoFlujo={tipoFlujo}
              ultimo={indice === visibles.length - 1}
              nombreArchivo={(id) => porId.get(id)?.nombre ?? adjuntos.get(id)?.nombre}
              documento={(id) => porId.get(id)}
              onAbrir={abridor.abrir}
              ocupado={abridor.ocupado}
              personas={personas}
              bodega={bodega}
            />
          ))}
        </ol>
      )}
      {filtrados.length > visibles.length && (
        <Button variant="outline" size="sm" className="self-start" onClick={() => setMostrados((n) => n + TRAMO)}>
          Mostrar {Math.min(TRAMO, filtrados.length - visibles.length)} más ({filtrados.length - visibles.length} restantes)
        </Button>
      )}
      {abridor.error && (
        <Alert variant="destructive">
          <AlertDescription>{abridor.error}</AlertDescription>
        </Alert>
      )}
      {abridor.elemento}
    </div>
  );
}

function Evento({
  evento,
  clase,
  tipoFlujo,
  ultimo,
  nombreArchivo,
  documento,
  onAbrir,
  ocupado,
  personas,
  bodega,
}: {
  evento: EventoHistorial;
  clase: Clase;
  tipoFlujo: string;
  ultimo: boolean;
  nombreArchivo: (id: string) => string | undefined;
  documento: (id: string) => DocumentoPedido | undefined;
  onAbrir: (documento: DocumentoPedido) => void;
  ocupado: boolean;
  personas: Map<string, Persona>;
  bodega?: Bodega | null;
}) {
  const estilo = CLASE[clase];
  const Icono = estilo.icono ?? ICONO[evento.tipo] ?? MessageSquareTextIcon;
  const detalle = evento.detalle;
  const archivos = evento.adjuntoIds ?? [];
  const nombrePersona = (id: string | null | undefined) => (id ? (personas.get(id)?.nombre ?? 'otra persona') : null);
  const anterior = nombrePersona(detalle?.asignadoAnterior);
  const nueva = nombrePersona(detalle?.asignadoA) ?? 'otra persona';

  return (
    <li className="relative flex gap-3 pb-5">
      {!ultimo && <span aria-hidden="true" className="absolute top-9 bottom-0 left-4 w-px bg-border" />}
      <span aria-hidden="true" className={cn('relative z-10 flex size-8 shrink-0 items-center justify-center rounded-full', estilo.tono)}>
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
        <div className="flex flex-wrap items-center gap-x-2 gap-y-1 text-sm text-muted-foreground">
          <span className={cn('inline-flex items-center rounded-sm px-1.5 text-xs font-medium', estilo.tono)}>
            {estilo.texto}{clase === 'correccion' ? ' · reenvío tras devolución' : ''}
          </span>
          <span>
            {evento.usuario
              ? `${evento.usuario.nombre}${evento.rolIntervencion ? ` · ${nombreRol(evento.rolIntervencion)}` : ''}`
              : 'Sistema'}
          </span>
        </div>
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
          <p className="max-w-prose rounded-md bg-muted/60 px-3 py-2 text-sm whitespace-pre-wrap">
            {(clase === 'rechazo' || clase === 'devolucion') && <span className="font-medium">Motivo: </span>}
            {evento.observacion}
          </p>
        )}
        {archivos.length > 0 && (
          <ul aria-label="Archivos del evento" className="flex flex-wrap gap-x-3 gap-y-1 text-sm">
            {archivos.map((id) => {
              const doc = documento(id);
              const abrible = doc !== undefined && doc.puedeAbrir && doc.disponibilidad !== 'NO_DISPONIBLE';
              return (
                <li key={id} className="flex max-w-full min-w-0 items-center gap-1 text-muted-foreground">
                  <PaperclipIcon aria-hidden="true" className="size-3.5 shrink-0" />
                  {abrible ? (
                    <Button
                      variant="link"
                      size="sm"
                      className="block h-auto min-h-0 max-w-full min-w-0 truncate p-0"
                      disabled={ocupado}
                      onClick={() => onAbrir(doc)}
                      aria-label={`Abrir ${doc.nombre}`}
                    >
                      {doc.nombre}
                    </Button>
                  ) : (
                    <span className="truncate">
                      {nombreArchivo(id) ?? 'Archivo'}
                      {doc?.disponibilidad === 'NO_DISPONIBLE' ? ' (no disponible)' : ''}
                    </span>
                  )}
                </li>
              );
            })}
          </ul>
        )}
      </div>
    </li>
  );
}

import {
  ArrowRightIcon,
  CalendarCheck2Icon,
  CalendarClockIcon,
  CalendarIcon,
  CalendarRangeIcon,
  CircleCheckIcon,
  CircleHelpIcon,
  FileTextIcon,
  HourglassIcon,
  type LucideIcon,
  MinusIcon,
  PlayIcon,
  RotateCwIcon,
  TableIcon,
  TrendingDownIcon,
  TrendingUpIcon,
  TruckIcon,
} from 'lucide-react';
import { type ReactNode, useId, useState } from 'react';
import {
  type Cronograma,
  type EstadoCronograma,
  type PedidoDetalle,
  type ProcedenciaFecha,
  useCronograma,
  useDocumentosPedido,
} from '@/api/pedidos';
import { Alert, AlertDescription, AlertTitle } from '@/components/ui/alert';
import { Badge } from '@/components/ui/badge';
import { Button } from '@/components/ui/button';
import { Sheet, SheetBody, SheetContent, SheetDescription, SheetFooter, SheetHeader, SheetTitle } from '@/components/ui/sheet';
import { Skeleton } from '@/components/ui/skeleton';
import { Table, TableBody, TableCaption, TableCell, TableHead, TableHeader, TableRow } from '@/components/ui/table';
import { cn } from '@/lib/utils';
import { useAbridorDocumentos } from './DocumentosPedido';
import { codigoEtapa, formatearFecha, formatearFechaHora, mensajeDeErrorPedido, nombreRol } from './formato';

// Cronograma del pedido (paso 5.3; ADR-0030). Todo lo calcula el servidor con el calendario del país; aquí no se suma
// ninguna fecha. Se distinguen tres cosas: la planificación original (guardada al crear el pedido o reconstruida si es
// anterior), la estimación actual y las fechas reales. Las duraciones de referencia (31 y 45 días de la calculadora
// original, 24 h de B2) estiman fechas pero no son plazos: no generan alertas. La meta sigue siendo una propuesta
// pendiente del cliente: la diferencia es informativa y no permite declarar cumplimiento.
//
// Presentación (mejora visual del 2026-10-27): tarjetas de llegada, una agenda mensual compacta con el fin de cada etapa
// (planificado, estimado o real, cada uno con su forma además del color) y un panel lateral al pulsar un día o una
// alerta. Las fechas del cronograma son días civiles: solo se muestra una hora cuando el pedido la tiene registrada
// (salida real de la etapa o llegada confirmada), y siempre en hora de Ecuador. La tabla completa sigue disponible.

type Etapa = Cronograma['etapas'][number];
type DuracionEtapa = NonNullable<Etapa['duracion']>;
type TipoHito = 'PLANIFICADA' | 'ESTIMADA' | 'REAL';

interface Hito {
  clave: string;
  fecha: string;
  tipo: TipoHito;
  etapa: Etapa;
}

/** Lo que muestra el panel lateral: los hitos de un día o los de una etapa (desde una alerta). */
type Seleccion = { modo: 'dia'; fecha: string } | { modo: 'etapa'; numero: number; motivo?: string };

const ESTADO: Record<EstadoCronograma, { texto: string; icono: LucideIcon; tono: string }> = {
  REAL: { texto: 'Real', icono: CircleCheckIcon, tono: 'text-estado-ok' },
  EN_CURSO: { texto: 'En curso', icono: PlayIcon, tono: 'text-primary' },
  EXCEDIDA: { texto: 'Sigue abierta', icono: HourglassIcon, tono: 'text-estado-aviso' },
  ESTIMADA: { texto: 'Estimada', icono: CalendarRangeIcon, tono: 'text-muted-foreground' },
  SIN_ESTIMAR: { texto: 'Sin estimar', icono: CircleHelpIcon, tono: 'text-estado-aviso' },
};

const TIPO_HITO: Record<TipoHito, { texto: string; ayuda: string }> = {
  PLANIFICADA: { texto: 'Planificada', ayuda: 'Fin previsto en la planificación original.' },
  ESTIMADA: { texto: 'Estimada', ayuda: 'Fin según la estimación actual. Es una previsión, no una confirmación.' },
  REAL: { texto: 'Real', ayuda: 'Fin registrado en el pedido.' },
};

const PROCEDENCIA_LLEGADA: Record<ProcedenciaFecha, string> = {
  REAL: 'Fecha real registrada.',
  LLEGADA_REAL: 'Llegada confirmada en aduana.',
  LLEGADA_ESTIMADA: 'Fecha escrita al reportar la demora; no está confirmada.',
  CALCULADA: '',
  PROYECTADA: 'La aduana sigue abierta después de lo estimado: se supone que llega hoy y la fecha avanza cada día.',
};

const FUENTE: Record<NonNullable<DuracionEtapa['fuente']>, string> = {
  CALCULADORA_ORIGINAL: 'Referencia de la calculadora original',
  REQUERIMIENTOS_4_5: 'Referencia de requerimientos 4.5, sin confirmar',
};

const ZONA = 'America/Guayaquil';
const diaEcuador = new Intl.DateTimeFormat('en-CA', { timeZone: ZONA, year: 'numeric', month: '2-digit', day: '2-digit' });
const horaEcuador = new Intl.DateTimeFormat('es-EC', { timeZone: ZONA, hour: '2-digit', minute: '2-digit', hour12: false });
const fechaLarga = new Intl.DateTimeFormat('es-EC', { timeZone: 'UTC', weekday: 'long', day: 'numeric', month: 'long', year: 'numeric' });
const mesLargo = new Intl.DateTimeFormat('es-EC', { timeZone: 'UTC', month: 'long', year: 'numeric' });
const DIA_MS = 86_400_000;
const SEMANA = ['L', 'M', 'X', 'J', 'V', 'S', 'D'];

/** Fecha civil (AAAA-MM-DD) como milisegundos UTC: solo para recorrer el calendario, nunca para sumar plazos. */
const aMs = (fecha: string) => {
  const [a, m, d] = fecha.split('-').map(Number);
  return Date.UTC(a, m - 1, d);
};
const aIso = (ms: number) => new Date(ms).toISOString().slice(0, 10);
/** Día de Ecuador de un instante. */
const diaDe = (instante: string | null | undefined) => (instante ? diaEcuador.format(new Date(instante)) : null);

interface Props {
  pedido: PedidoDetalle;
  /** Etapa que registra la llegada (definición del flujo), para marcarla como «Llegada». */
  etapaLlegada?: number;
  /** Lleva el panel de la etapa del pedido a esa etapa. */
  onIrAEtapa?: (numero: number) => void;
}

export function CronogramaPedido({ pedido, etapaLlegada, onIrAEtapa }: Props) {
  const cronograma = useCronograma(pedido.id);

  return (
    <section
      id="cronograma"
      aria-labelledby="cronograma-titulo"
      className="flex scroll-mt-4 flex-col gap-4 rounded-lg border bg-card p-4 shadow-sm sm:p-5"
    >
      <div className="flex flex-wrap items-baseline justify-between gap-x-3 gap-y-1">
        <h2 id="cronograma-titulo" className="font-heading text-xl font-semibold">
          Cronograma de llegada
        </h2>
        <p className="text-xs text-muted-foreground">Días del calendario del país del pedido · horas en hora de Ecuador (UTC−5)</p>
      </div>

      {cronograma.isPending ? (
        <div className="flex flex-col gap-3" aria-busy="true" aria-label="Cargando el cronograma">
          <Skeleton className="h-20 w-full" />
          <Skeleton className="h-56 w-full" />
        </div>
      ) : cronograma.isError ? (
        <Alert variant="destructive">
          <AlertDescription className="flex flex-wrap items-center justify-between gap-3">
            <p>{mensajeDeErrorPedido(cronograma.error, 'No se pudo cargar el cronograma.')}</p>
            <Button variant="outline" size="sm" onClick={() => void cronograma.refetch()}>
              <RotateCwIcon data-icon="inline-start" />
              Reintentar
            </Button>
          </AlertDescription>
        </Alert>
      ) : (
        <Contenido datos={cronograma.data} pedido={pedido} etapaLlegada={etapaLlegada} onIrAEtapa={onIrAEtapa} />
      )}
    </section>
  );
}

function Contenido({ datos, pedido, etapaLlegada, onIrAEtapa }: { datos: Cronograma } & Props) {
  const tipoFlujo = pedido.tipoFlujo;
  const nombreEtapa = (etapa: { numero: number; nombre: string }) => `${codigoEtapa(tipoFlujo, etapa.numero)} · ${etapa.nombre}`;
  const [seleccion, setSeleccion] = useState<Seleccion | null>(null);
  const [verTabla, setVerTabla] = useState(false);
  const idTabla = useId();
  const excedida = datos.etapas.find((etapa) => etapa.estado === 'EXCEDIDA');
  const faltantesFinalizacion = datos.faltantesFinalizacion ?? [];
  const hitos = hitosDe(datos);

  return (
    <>
      <ResumenLlegada datos={datos} pedido={pedido} />

      {(datos.faltantes.length > 0 || excedida || (pedido.demoraAduana && !pedido.finalizado)) && (
        <ul className="flex flex-col gap-2" aria-label="Alertas del cronograma">
          {pedido.demoraAduana && !pedido.finalizado && etapaLlegada !== undefined && (
            <li>
              <AlertaHito
                icono={TruckIcon}
                titulo={`Demora reportada · nueva llegada estimada ${formatearFecha(pedido.demoraAduana.nuevaFechaEstimadaLlegada)}`}
                texto={pedido.demoraAduana.motivo}
                onAbrir={() => setSeleccion({ modo: 'etapa', numero: etapaLlegada, motivo: pedido.demoraAduana?.motivo })}
              />
            </li>
          )}
          {excedida && (
            <li>
              <AlertaHito
                icono={HourglassIcon}
                titulo={`${nombreEtapa(excedida)} sigue abierta después de lo estimado`}
                texto={`${excedida.fechaOriginal ? `La planificación original la terminaba el ${formatearFecha(excedida.fechaOriginal)}. ` : ''}La estimación supone que termina hoy, así que la llegada se mueve cada día que siga abierta. ${
                  excedida.duracion?.base === 'REFERENCIA'
                    ? 'Su duración es solo una referencia, no un plazo aprobado: no genera alerta.'
                    : 'Su plazo y sus alertas no cambian.'
                }`}
                onAbrir={() => setSeleccion({ modo: 'etapa', numero: excedida.numero })}
              />
            </li>
          )}
          {datos.faltantes.map((faltante) => (
            <li key={faltante.numero}>
              <AlertaHito
                icono={CircleHelpIcon}
                titulo="No se puede estimar la llegada"
                texto={`Falta la duración de ${nombreEtapa(faltante)}: el flujo de este país no tiene plazo ni duración de referencia documentados para esa etapa. No se usan valores supuestos.`}
                onAbrir={() => setSeleccion({ modo: 'etapa', numero: faltante.numero })}
              />
            </li>
          ))}
        </ul>
      )}

      <Agenda
        hitos={hitos}
        fechaPedido={datos.fechaPedido}
        etapaLlegada={etapaLlegada}
        tipoFlujo={tipoFlujo}
        onElegir={(fecha) => setSeleccion({ modo: 'dia', fecha })}
      />

      <div className="flex flex-col gap-2 border-t pt-3 text-sm">
        <div>
          <Button variant="ghost" size="sm" aria-expanded={verTabla} aria-controls={idTabla} onClick={() => setVerTabla((v) => !v)}>
            <TableIcon data-icon="inline-start" />
            {verTabla ? 'Ocultar la tabla de fechas' : 'Ver todas las fechas en tabla'}
          </Button>
        </div>
        {verTabla && <TablaFechas id={idTabla} datos={datos} tipoFlujo={tipoFlujo} />}
      </div>

      <PanelHitos
        seleccion={seleccion}
        onCerrar={() => setSeleccion(null)}
        hitos={hitos}
        datos={datos}
        pedido={pedido}
        etapaLlegada={etapaLlegada}
        onIrAEtapa={onIrAEtapa}
      />
    </>
  );
}

/** Fin de cada etapa en sus tres versiones, sin calcular nada: solo las fechas que devolvió el servidor. */
function hitosDe(datos: Cronograma): Hito[] {
  return datos.etapas.flatMap((etapa) => {
    const hitos: Hito[] = [];
    if (etapa.fechaOriginal) hitos.push({ clave: `${etapa.numero}-P`, fecha: etapa.fechaOriginal, tipo: 'PLANIFICADA', etapa });
    if (etapa.fechaEstimada) hitos.push({ clave: `${etapa.numero}-E`, fecha: etapa.fechaEstimada, tipo: 'ESTIMADA', etapa });
    if (etapa.fechaReal) hitos.push({ clave: `${etapa.numero}-R`, fecha: etapa.fechaReal, tipo: 'REAL', etapa });
    return hitos;
  });
}

function ResumenLlegada({ datos, pedido }: { datos: Cronograma; pedido: PedidoDetalle }) {
  const original = datos.original;
  const llegadaReal = datos.llegada?.procedencia === 'LLEGADA_REAL';
  const diferencia = datos.diferenciaContraOriginal;
  const dias = diferencia == null ? 0 : Math.abs(diferencia);
  const textoDias = `${dias} ${dias === 1 ? 'día' : 'días'}`;
  const desvio =
    diferencia == null
      ? { icono: MinusIcon, valor: '—', nota: 'Sin planificación original o sin llegada estimada para comparar.', tono: '' }
      : diferencia > 0
        ? { icono: TrendingUpIcon, valor: `${textoDias} de retraso`, nota: '', tono: 'text-estado-aviso' }
        : diferencia < 0
          ? { icono: TrendingDownIcon, valor: `${textoDias} de adelanto`, nota: 'La llegada actual es anterior a la planificada.', tono: 'text-estado-ok' }
          : { icono: CircleCheckIcon, valor: 'Según lo planificado', nota: 'Misma fecha que la planificación original.', tono: 'text-estado-ok' };

  return (
    <dl className="grid grid-cols-1 overflow-hidden rounded-lg border sm:grid-cols-2 xl:grid-cols-4 [&>div]:border-b sm:[&>div:nth-child(odd)]:border-r xl:[&>div]:border-r xl:[&>div:last-child]:border-r-0 xl:[&>div]:border-b-0 sm:[&>div:nth-last-child(-n+2)]:border-b-0">
      <Tarjeta icono={CalendarIcon} titulo="Llegada prevista original">
        <Valor>{original?.llegada ? formatearFecha(original.llegada) : 'Sin fecha prevista'}</Valor>
        {/* <Nota>
          {!original
            ? 'Este pedido no tiene planificación original.'
            : original.origen === 'GUARDADA'
              ? `Guardada al crear el pedido${original.calculadaEn ? `, el ${formatearFechaHora(original.calculadaEn)}` : ''}.`
              : 'Reconstruida: el pedido es anterior a este cálculo y se calcula al consultar con las reglas vigentes. No es lo que se previó entonces.'}
        </Nota> */}
      </Tarjeta>
      <Tarjeta icono={CalendarClockIcon} titulo="Llegada estimada actual">
        {llegadaReal ? (
          <>
            <Valor apagado>Ya registrada</Valor>
            <Nota>La llegada real reemplaza a la estimación.</Nota>
          </>
        ) : datos.llegada ? (
          <>
            <Valor>{formatearFecha(datos.llegada.fecha)}</Valor>
            <Nota>{PROCEDENCIA_LLEGADA[datos.llegada.procedencia]}</Nota>
          </>
        ) : (
          <>
            <Valor apagado>Sin estimar</Valor>
            <Nota>Todavía no se puede estimar.</Nota>
          </>
        )}
      </Tarjeta>
      <Tarjeta icono={CalendarCheck2Icon} titulo="Llegada real">
        {pedido.llegadaAduanaEn ? (
          <>
            <Valor>{formatearFechaHora(pedido.llegadaAduanaEn)}</Valor>
            <Nota>Confirmada en aduana · hora de Ecuador.</Nota>
          </>
        ) : llegadaReal && datos.llegada ? (
          <>
            <Valor>{formatearFecha(datos.llegada.fecha)}</Valor>
            <Nota>Confirmada en aduana.</Nota>
          </>
        ) : (
          <>
            <Valor apagado>Sin confirmar</Valor>
          </>
        )}
      </Tarjeta>
      <Tarjeta icono={desvio.icono} titulo="Frente al plan original" tono={desvio.tono}>
        <Valor className={desvio.tono}>{desvio.valor}</Valor>
        <Nota>{desvio.nota}</Nota>
      </Tarjeta>
    </dl>
  );
}

function Tarjeta({ icono: Icono, titulo, tono, children }: { icono: LucideIcon; titulo: string; tono?: string; children: ReactNode }) {
  return (
    <div className="flex min-w-0 flex-col gap-1 bg-card px-4 py-3">
      <dt className="flex items-center gap-1.5 text-xs font-medium text-muted-foreground">
        <Icono aria-hidden="true" className={cn('size-3.5', tono)} />
        {titulo}
      </dt>
      {children}
    </div>
  );
}

function Valor({ children, apagado, className }: { children: ReactNode; apagado?: boolean; className?: string }) {
  return (
    <dd className={cn('font-heading text-lg font-semibold tabular-nums', apagado && 'text-muted-foreground', className)}>{children}</dd>
  );
}

function Nota({ children }: { children: ReactNode }) {
  return <dd className="text-xs leading-snug text-muted-foreground">{children}</dd>;
}

function AlertaHito({ icono: Icono, titulo, texto, onAbrir }: { icono: LucideIcon; titulo: string; texto: string; onAbrir: () => void }) {
  return (
    <Alert role="note" className="flex flex-col gap-2 sm:flex-row sm:items-start sm:justify-between">
      <div className="flex min-w-0 gap-2">
        <Icono aria-hidden="true" className="mt-0.5 size-4 shrink-0 text-estado-aviso" />
        <div className="flex min-w-0 flex-col gap-0.5">
          <AlertTitle>{titulo}</AlertTitle>
          <AlertDescription className="line-clamp-2">{texto}</AlertDescription>
        </div>
      </div>
      <Button variant="outline" size="sm" className="shrink-0 self-start" onClick={onAbrir}>
        Ver detalle
      </Button>
    </Alert>
  );
}

/** Marca de un hito: forma distinta por tipo (además del color) para no depender solo del color. */
function Marca({ tipo, className }: { tipo: TipoHito; className?: string }) {
  return (
    <span
      aria-hidden="true"
      className={cn(
        'inline-block shrink-0',
        tipo === 'PLANIFICADA' && 'size-2 rounded-full border-[1.5px] border-primary',
        tipo === 'ESTIMADA' && 'size-[0.45rem] rotate-45 bg-marca-naranja',
        tipo === 'REAL' && 'size-2 rounded-full bg-estado-ok',
        className,
      )}
    />
  );
}

function Agenda({
  hitos,
  fechaPedido,
  etapaLlegada,
  tipoFlujo,
  onElegir,
}: {
  hitos: Hito[];
  fechaPedido: string;
  etapaLlegada?: number;
  tipoFlujo: string;
  onElegir: (fecha: string) => void;
}) {
  const hoy = diaEcuador.format(new Date());
  // fechaPedido puede venir como día civil o como instante; un día civil se usa tal cual (sin pasar por UTC).
  const inicioPedido = /^\d{4}-\d{2}-\d{2}$/.test(fechaPedido) ? fechaPedido : (diaDe(fechaPedido) ?? fechaPedido.slice(0, 10));
  const porDia = new Map<string, Hito[]>();
  for (const hito of hitos) porDia.set(hito.fecha, [...(porDia.get(hito.fecha) ?? []), hito]);

  if (hitos.length === 0) {
    return (
      <p className="rounded-md border border-dashed px-4 py-6 text-center text-sm text-muted-foreground">
        Todavía no hay fechas para mostrar en la agenda.
      </p>
    );
  }

  const fechas = [inicioPedido, ...hitos.map((h) => h.fecha)].map(aMs);
  const primero = new Date(Math.min(...fechas));
  const ultimo = new Date(Math.max(...fechas));
  const meses: { anio: number; mes: number }[] = [];
  for (let a = primero.getUTCFullYear(), m = primero.getUTCMonth(); a < ultimo.getUTCFullYear() || (a === ultimo.getUTCFullYear() && m <= ultimo.getUTCMonth()); m === 11 ? (a++, m = 0) : m++) {
    meses.push({ anio: a, mes: m });
  }

  return (
    <div className="flex flex-col gap-3">
      <ul className="flex flex-wrap items-center gap-x-4 gap-y-1 text-xs text-muted-foreground" aria-label="Leyenda de la agenda">
        {(['PLANIFICADA', 'ESTIMADA', 'REAL'] as const).map((tipo) => (
          <li key={tipo} className="flex items-center gap-1.5"><Marca tipo={tipo} />{TIPO_HITO[tipo].texto}</li>
        ))}
        {etapaLlegada !== undefined && (
          <li className="flex items-center gap-1.5"><TruckIcon aria-hidden="true" className="size-3.5" />Llegada ({codigoEtapa(tipoFlujo, etapaLlegada)})</li>
        )}
        <li className="flex items-center gap-1.5"><span aria-hidden="true" className="inline-block size-3 rounded-sm bg-secondary ring-1 ring-primary/40" />Hoy</li>
      </ul>
      <div className="grid gap-x-6 gap-y-4 [grid-template-columns:repeat(auto-fill,minmax(min(100%,14rem),1fr))]">
        {meses.map(({ anio, mes }) => {
          const inicio = Date.UTC(anio, mes, 1);
          const desplazamiento = (new Date(inicio).getUTCDay() + 6) % 7;
          const dias = new Date(Date.UTC(anio, mes + 1, 0)).getUTCDate();
          const nombreMes = mesLargo.format(new Date(inicio));
          return (
            <section key={`${anio}-${mes}`} aria-label={nombreMes} className="flex flex-col gap-1.5">
              <h3 className="text-sm font-semibold first-letter:uppercase">{nombreMes}</h3>
              <div role="grid" aria-label={`Días de ${nombreMes}`} className="grid grid-cols-7 gap-0.5 text-center">
                <div role="row" className="contents">
                  {SEMANA.map((dia) => <span key={dia} role="columnheader" className="pb-0.5 text-[0.75rem] font-medium text-muted-foreground">{dia}</span>)}
                </div>
                <div role="row" className="contents">
                  {Array.from({ length: desplazamiento }, (_, i) => <span key={`v${i}`} role="gridcell" aria-hidden="true" />)}
                  {Array.from({ length: dias }, (_, i) => {
                    const fecha = aIso(inicio + i * DIA_MS);
                    const delDia = porDia.get(fecha) ?? [];
                    const tipos = (['PLANIFICADA', 'ESTIMADA', 'REAL'] as const).filter((t) => delDia.some((h) => h.tipo === t));
                    const llegada = etapaLlegada !== undefined && delDia.some((h) => h.etapa.numero === etapaLlegada);
                    const esHoy = fecha === hoy;
                    const esInicio = fecha === inicioPedido;
                    const clase = cn(
                      'relative flex h-9 flex-col items-center justify-center gap-0.5 rounded-md text-sm tabular-nums',
                      esHoy && 'bg-secondary ring-1 ring-primary/40',
                      esInicio && 'font-semibold text-primary',
                    );
                    if (delDia.length === 0) {
                      return (
                        <span key={fecha} role="gridcell" className={cn(clase, 'text-muted-foreground')} aria-label={esHoy ? `${i + 1}, hoy` : esInicio ? `${i + 1}, creación del pedido` : undefined}>
                          {i + 1}
                        </span>
                      );
                    }
                    const descripcion = delDia
                      .map((h) => `${codigoEtapa(tipoFlujo, h.etapa.numero)} ${h.etapa.nombre}, ${TIPO_HITO[h.tipo].texto.toLowerCase()}`)
                      .join('; ');
                    return (
                      <span key={fecha} role="gridcell">
                        <button
                          type="button"
                          onClick={() => onElegir(fecha)}
                          aria-label={`${fechaLarga.format(new Date(aMs(fecha)))}${esHoy ? ' (hoy)' : ''}: ${descripcion}`}
                          className={cn(
                            clase,
                            'w-full border border-morado-200 bg-card font-medium outline-none hover:border-primary hover:bg-morado-50 focus-visible:ring-3 focus-visible:ring-ring',
                            esHoy && 'bg-secondary',
                          )}
                        >
                          {llegada && <TruckIcon aria-hidden="true" className="absolute top-0.5 right-0.5 size-3 text-primary" />}
                          <span className="leading-none">{i + 1}</span>
                          <span className="flex items-center gap-0.5" aria-hidden="true">
                            {tipos.map((tipo) => <Marca key={tipo} tipo={tipo} />)}
                          </span>
                        </button>
                      </span>
                    );
                  })}
                </div>
              </div>
            </section>
          );
        })}
      </div>
    </div>
  );
}

function PanelHitos({
  seleccion,
  onCerrar,
  hitos,
  datos,
  pedido,
  etapaLlegada,
  onIrAEtapa,
}: {
  seleccion: Seleccion | null;
  onCerrar: () => void;
  hitos: Hito[];
  datos: Cronograma;
} & Props) {
  const tipoFlujo = pedido.tipoFlujo;
  const elegidos = !seleccion
    ? []
    : seleccion.modo === 'dia'
      ? hitos.filter((h) => h.fecha === seleccion.fecha)
      : hitos.filter((h) => h.etapa.numero === seleccion.numero);
  const etapaSola = seleccion?.modo === 'etapa' ? datos.etapas.find((e) => e.numero === seleccion.numero) : undefined;
  const titulo = !seleccion
    ? ''
    : seleccion.modo === 'dia'
      ? fechaLarga.format(new Date(aMs(seleccion.fecha)))
      : etapaSola
        ? `${codigoEtapa(tipoFlujo, etapaSola.numero)} · ${etapaSola.nombre}`
        : 'Etapa';
  // Etapas distintas del panel, en orden del flujo, cada una con sus hitos.
  const etapas = [...new Map(elegidos.map((h) => [h.etapa.numero, h.etapa])).values()].sort((a, b) => a.numero - b.numero);
  if (etapaSola && etapas.length === 0) etapas.push(etapaSola);

  return (
    <Sheet open={seleccion !== null} onOpenChange={(abierto) => { if (!abierto) onCerrar(); }}>
      <SheetContent textoCerrar="Cerrar el detalle de fechas">
        <SheetHeader>
          <SheetTitle className="first-letter:uppercase">{titulo}</SheetTitle>
          <SheetDescription>
            <span className="font-medium text-foreground tabular-nums">{pedido.numero}</span> · {pedido.nombre}
          </SheetDescription>
        </SheetHeader>
        <SheetBody className="flex flex-col gap-4">
          {pedido.descripcion && <p className="line-clamp-4 text-sm whitespace-pre-wrap text-muted-foreground">{pedido.descripcion}</p>}
          {seleccion?.modo === 'etapa' && seleccion.motivo && (
            <p className="rounded-md bg-estado-aviso-fondo px-3 py-2 text-sm text-estado-aviso">
              <span className="font-medium">Motivo de la demora: </span>
              <span className="text-foreground">{seleccion.motivo}</span>
            </p>
          )}
          {etapas.map((etapa) => (
            <DetalleEtapa
              key={etapa.numero}
              etapa={etapa}
              hitos={elegidos.filter((h) => h.etapa.numero === etapa.numero)}
              pedido={pedido}
              llegada={etapa.numero === etapaLlegada}
              onIrAEtapa={onIrAEtapa ? (numero) => { onCerrar(); onIrAEtapa(numero); } : undefined}
            />
          ))}
        </SheetBody>
        <SheetFooter>
          <Button variant="outline" onClick={onCerrar}>Cerrar</Button>
        </SheetFooter>
      </SheetContent>
    </Sheet>
  );
}

function DetalleEtapa({
  etapa,
  hitos,
  pedido,
  llegada,
  onIrAEtapa,
}: {
  etapa: Etapa;
  hitos: Hito[];
  pedido: PedidoDetalle;
  llegada: boolean;
  onIrAEtapa?: (numero: number) => void;
}) {
  const tipoFlujo = pedido.tipoFlujo;
  const enLinea = pedido.lineaDeTiempo.find((e) => e.numero === etapa.numero);
  const estado = ESTADO[etapa.estado];
  const IconoEstado = estado.icono;
  const actual = !pedido.finalizado && pedido.etapaActual === etapa.numero;
  const responsable = enLinea?.asignadoA?.nombre ?? (enLinea?.rolResponsable ? `${nombreRol(enLinea.rolResponsable)} (por rol)` : 'Automática');
  const documentos = useDocumentosPedido(pedido.id);
  const abridor = useAbridorDocumentos();
  const relacionados = (documentos.data ?? []).filter((d) => d.etapa === etapa.numero);

  /** Hora registrada que corresponde a ese día; nunca se inventa una hora para una fecha que solo tiene día. */
  function horaReal(fecha: string): string | null {
    if (llegada && pedido.llegadaAduanaEn && diaDe(pedido.llegadaAduanaEn) === fecha) return horaEcuador.format(new Date(pedido.llegadaAduanaEn));
    if (enLinea?.salida && diaDe(enLinea.salida) === fecha) return horaEcuador.format(new Date(enLinea.salida));
    return null;
  }

  return (
    <article aria-label={`${codigoEtapa(tipoFlujo, etapa.numero)} ${etapa.nombre}`} className="flex flex-col gap-3 rounded-lg border p-4">
      <header className="flex flex-wrap items-start justify-between gap-2">
        <h3 className="font-heading text-base font-semibold">
          <span className="tabular-nums">{codigoEtapa(tipoFlujo, etapa.numero)}</span> · {etapa.nombre}
        </h3>
        {llegada && <Badge variant="outline"><TruckIcon />Llegada</Badge>}
      </header>
      <ul className="flex flex-col gap-2">
        {hitos.map((hito) => {
          const hora = hito.tipo === 'REAL' ? horaReal(hito.fecha) : null;
          return (
            <li key={hito.clave} className="flex items-start gap-2 text-sm">
              <Marca tipo={hito.tipo} className="mt-1.5" />
              <div className="flex min-w-0 flex-col">
                <span>
                  <span className="font-medium">{TIPO_HITO[hito.tipo].texto}</span> · fin de etapa el{' '}
                  <span className="tabular-nums">{formatearFecha(hito.fecha)}</span>
                  {hora ? <> a las <span className="tabular-nums">{hora}</span> (hora de Ecuador)</> : ''}
                </span>
                <span className="text-xs text-muted-foreground">
                  {TIPO_HITO[hito.tipo].ayuda}
                  {hito.tipo !== 'REAL' && !hora ? ' Solo día: el cálculo no fija una hora.' : ''}
                  {hito.tipo === 'ESTIMADA' && etapa.movidaAlLunes ? ' Cayó en fin de semana y pasó al lunes.' : ''}
                </span>
              </div>
            </li>
          );
        })}
        {hitos.length === 0 && <li className="text-sm text-muted-foreground">Esta etapa no tiene fechas que mostrar.</li>}
      </ul>
      <dl className="grid gap-x-4 gap-y-2 text-sm sm:grid-cols-2">
        <div><dt className="text-xs text-muted-foreground">Estado</dt>
          <dd className={cn('inline-flex items-center gap-1.5', estado.tono)}><IconoEstado aria-hidden="true" className="size-4" />{estado.texto}{actual ? ' · etapa actual' : ''}</dd></div>
        <div><dt className="text-xs text-muted-foreground">Responsable</dt><dd>{responsable}</dd></div>
        <div><dt className="text-xs text-muted-foreground">Duración usada</dt><dd><TextoDuracion duracion={etapa.duracion} /></dd></div>
        {enLinea?.entrada && (
          <div><dt className="text-xs text-muted-foreground">Entrada a la etapa</dt><dd className="tabular-nums">{formatearFechaHora(enLinea.entrada)}</dd></div>
        )}
        {actual && pedido.motivoRetraso && (
          <div className="sm:col-span-2"><dt className="text-xs text-muted-foreground">Motivo del retraso</dt><dd>{pedido.motivoRetraso}</dd></div>
        )}
        {llegada && pedido.demoraAduana && (
          <div className="sm:col-span-2"><dt className="text-xs text-muted-foreground">Motivo de la demora</dt>
            <dd>{pedido.demoraAduana.motivo} <span className="text-muted-foreground">({pedido.demoraAduana.reportadaPor.nombre}, {formatearFechaHora(pedido.demoraAduana.reportadaEn)})</span></dd></div>
        )}
      </dl>
      {relacionados.length > 0 && (
        <div className="flex flex-col gap-1.5">
          <p className="text-xs font-medium text-muted-foreground">Documentos de la etapa</p>
          <ul className="flex flex-col gap-1">
            {relacionados.map((documento) => {
              const abrible = documento.puedeAbrir && documento.disponibilidad !== 'NO_DISPONIBLE';
              return (
                <li key={documento.id} className="flex min-w-0 items-center gap-2 text-sm">
                  <FileTextIcon aria-hidden="true" className="size-4 shrink-0 text-muted-foreground" />
                  {abrible ? (
                    <Button variant="link" size="sm" className="block h-auto min-h-0 max-w-full min-w-0 truncate p-0" disabled={abridor.ocupado} onClick={() => abridor.abrir(documento)}>
                      {documento.nombre}
                    </Button>
                  ) : (
                    <span className="truncate text-muted-foreground">{documento.nombre}{documento.disponibilidad === 'NO_DISPONIBLE' ? ' (no disponible)' : ' (sin permiso para abrirlo)'}</span>
                  )}
                </li>
              );
            })}
          </ul>
          {abridor.error && <p role="alert" className="text-sm text-destructive">{abridor.error}</p>}
          {abridor.elemento}
        </div>
      )}
      {onIrAEtapa && (
        <Button variant="secondary" size="sm" className="self-start" onClick={() => onIrAEtapa(etapa.numero)}>
          Ir a la etapa en el pedido
          <ArrowRightIcon data-icon="inline-end" />
        </Button>
      )}
    </article>
  );
}

function TablaFechas({ id, datos, tipoFlujo }: { id: string; datos: Cronograma; tipoFlujo: string }) {
  return (
    // En pantallas angostas el desplazamiento horizontal vive en este marco, región enfocable con nombre (WCAG 2.1.1,
    // F5-B3-H02); el contenedor interno de la tabla no desplaza.
    <div
      id={id}
      role="region"
      aria-label="Fechas de cada etapa (desplazable)"
      tabIndex={0}
      className="overflow-x-auto rounded-lg border outline-none focus-visible:ring-3 focus-visible:ring-ring [&>[data-slot=table-container]]:overflow-visible"
    >
      <Table className="min-w-[52rem]">
        <TableCaption className="sr-only">Fechas de cada etapa del pedido: original, estimada y real</TableCaption>
        <TableHeader>
          <TableRow className="bg-secondary hover:bg-secondary">
            <TableHead scope="col" className="px-3">Etapa</TableHead>
            <TableHead scope="col" className="px-3">Original</TableHead>
            <TableHead scope="col" className="px-3">Estimada</TableHead>
            <TableHead scope="col" className="px-3">Real</TableHead>
            <TableHead scope="col" className="px-3">Estado</TableHead>
            <TableHead scope="col" className="px-3">Duración usada</TableHead>
          </TableRow>
        </TableHeader>
        <TableBody>
          {datos.etapas.map((etapa) => {
            const estado = ESTADO[etapa.estado];
            const Icono = estado.icono;
            return (
              <TableRow key={etapa.numero}>
                <TableCell className="px-3">
                  <span className="font-medium tabular-nums">{codigoEtapa(tipoFlujo, etapa.numero)}</span>
                  <span className="text-muted-foreground"> · {etapa.nombre}</span>
                </TableCell>
                <TableCell className="px-3 tabular-nums">{formatearFecha(etapa.fechaOriginal)}</TableCell>
                <TableCell className="px-3 tabular-nums"><FechaEstimada etapa={etapa} /></TableCell>
                <TableCell className="px-3 tabular-nums">{formatearFecha(etapa.fechaReal)}</TableCell>
                <TableCell className="px-3">
                  <span className={cn('inline-flex items-center gap-1.5', estado.tono)}>
                    <Icono aria-hidden="true" className="size-4" />
                    {estado.texto}
                  </span>
                </TableCell>
                <TableCell className="px-3"><TextoDuracion duracion={etapa.duracion} /></TableCell>
              </TableRow>
            );
          })}
        </TableBody>
      </Table>
    </div>
  );
}

function FechaEstimada({ etapa }: { etapa: Etapa }) {
  if (!etapa.fechaEstimada) return <>{etapa.fechaReal ? '' : '—'}</>;
  return (
    <>
      {formatearFecha(etapa.fechaEstimada)}
      {etapa.procedencia === 'PROYECTADA' && <span className="block text-xs text-estado-aviso">Hoy: sigue abierta</span>}
      {etapa.procedencia === 'LLEGADA_ESTIMADA' && <span className="block text-xs text-muted-foreground">Por la demora reportada</span>}
      {etapa.procedencia === 'CALCULADA' && etapa.movidaAlLunes && <span className="block text-xs text-muted-foreground">Pasó al lunes</span>}
    </>
  );
}

function TextoDuracion({ duracion }: { duracion: Etapa['duracion'] }) {
  if (!duracion) return <span className="text-muted-foreground">—</span>;
  switch (duracion.base) {
    case 'PLAZO':
      return (
        <span>
          {cantidadConUnidad(duracion)}
          <span className="block text-xs text-muted-foreground">Plazo del flujo</span>
        </span>
      );
    case 'REFERENCIA':
      return (
        <span>
          {cantidadConUnidad(duracion)}
          <span className="block text-xs text-muted-foreground">
            {duracion.fuente ? FUENTE[duracion.fuente] : 'Referencia'}; sin plazo aprobado
          </span>
        </span>
      );
    case 'SIN_PLAZO':
      return <span className="text-muted-foreground">No suma tiempo</span>;
    case 'SIN_DATO':
      return <span className="text-estado-aviso">Sin duración documentada</span>;
  }
}

function cantidadConUnidad({ cantidad, tipo }: DuracionEtapa): string {
  if (cantidad == null || !tipo) return '—';
  switch (tipo) {
    case 'HORAS_HABILES':
      return `${cantidad} h hábiles`;
    case 'DIAS_HABILES':
      return `${cantidad} ${cantidad === 1 ? 'día hábil' : 'días hábiles'}`;
    case 'DIAS_CORRIDOS':
      return `${cantidad} ${cantidad === 1 ? 'día corrido' : 'días corridos'}`;
  }
}

function textoDiferenciaMeta(diferencia: number | null | undefined): string {
  if (diferencia === null || diferencia === undefined) return '—';
  if (diferencia === 0) return 'igual a la meta propuesta';
  const dias = Math.abs(diferencia);
  return `${dias} ${dias === 1 ? 'día' : 'días'} ${diferencia > 0 ? 'por encima' : 'por debajo'} de la meta propuesta`;
}

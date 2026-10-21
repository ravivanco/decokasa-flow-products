import {
  CalendarClockIcon,
  CalendarRangeIcon,
  CircleCheckIcon,
  CircleHelpIcon,
  type LucideIcon,
  PlayIcon,
  RotateCwIcon,
} from 'lucide-react';
import { type EstadoCronograma, type ProcedenciaFecha, useCronograma } from '@/api/pedidos';
import { Alert, AlertDescription, AlertTitle } from '@/components/ui/alert';
import { Badge } from '@/components/ui/badge';
import { Button } from '@/components/ui/button';
import { Skeleton } from '@/components/ui/skeleton';
import { Table, TableBody, TableCaption, TableCell, TableHead, TableHeader, TableRow } from '@/components/ui/table';
import { cn } from '@/lib/utils';
import { codigoEtapa, formatearFecha, mensajeDeErrorPedido } from './formato';

// Cronograma del pedido frente a la meta (paso 5.3; F5-D1 opción A). Todo lo calcula el servidor con días hábiles y
// la hora de Ecuador; aquí no se suma ninguna fecha. Se distinguen las fechas planificadas, estimadas y reales, y un
// plazo pendiente del cliente se muestra como "sin estimar", nunca como cero. La meta es una propuesta pendiente del
// cliente: la diferencia es informativa y no permite declarar cumplimiento. Sustituye a la calculadora antigua.

const ESTADO: Record<EstadoCronograma, { texto: string; icono: LucideIcon; tono: string }> = {
  REAL: { texto: 'Real', icono: CircleCheckIcon, tono: 'text-estado-ok' },
  ESTIMADA: { texto: 'Estimada', icono: CalendarClockIcon, tono: 'text-estado-aviso' },
  EN_CURSO: { texto: 'En curso', icono: PlayIcon, tono: 'text-primary' },
  PLANIFICADA: { texto: 'Planificada', icono: CalendarRangeIcon, tono: 'text-muted-foreground' },
  SIN_ESTIMAR: { texto: 'Sin estimar', icono: CircleHelpIcon, tono: 'text-estado-aviso' },
};

const PROCEDENCIA: Record<ProcedenciaFecha, string> = {
  REAL: 'fecha real registrada',
  LLEGADA_REAL: 'llegada confirmada',
  LLEGADA_ESTIMADA: 'llegada estimada por demora, no confirmada',
  PLANIFICADA: 'fecha planificada con los plazos vigentes, no confirmada',
};

export function CronogramaPedido({ pedidoId, tipoFlujo }: { pedidoId: string; tipoFlujo: string }) {
  const cronograma = useCronograma(pedidoId);

  return (
    <section
      aria-labelledby="cronograma-titulo"
      className="flex flex-col gap-4 rounded-lg border bg-card p-4 shadow-sm sm:p-6"
    >
      <h2 id="cronograma-titulo" className="font-heading text-xl font-semibold">
        Cronograma frente a la meta
      </h2>

      {cronograma.isPending ? (
        <div className="flex flex-col gap-3" aria-busy="true" aria-label="Cargando el cronograma">
          <Skeleton className="h-6 w-80" />
          <Skeleton className="h-40 w-full" />
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
        <>
          <div className="flex flex-col gap-2">
            <div className="flex flex-wrap items-center gap-2">
              <p className="font-medium">
                Meta propuesta: <span className="tabular-nums">{cronograma.data.meta.dias}</span> días de pedido a
                llegada
              </p>
              <Badge variant="outline" className="h-auto rounded-sm py-0.5 text-sm">
                {cronograma.data.meta.origen === 'ADMIN'
                  ? 'Configurada por el admin · pendiente del cliente'
                  : 'Propuesta · pendiente del cliente'}
              </Badge>
            </div>
            <p className="text-sm text-muted-foreground">
              No es un objetivo aprobado por el cliente: la diferencia es informativa y no indica cumplimiento.
            </p>
          </div>

          {cronograma.data.calculable && cronograma.data.llegada ? (
            <dl className="grid gap-3 text-sm sm:grid-cols-3">
              <div className="flex flex-col gap-0.5">
                <dt className="text-muted-foreground">Llegada</dt>
                <dd className="font-medium tabular-nums">
                  {formatearFecha(cronograma.data.llegada.fecha)}
                  <span className="block font-normal text-muted-foreground">
                    {PROCEDENCIA[cronograma.data.llegada.procedencia]}
                  </span>
                </dd>
              </div>
              <div className="flex flex-col gap-0.5">
                <dt className="text-muted-foreground">Total</dt>
                <dd className="font-medium tabular-nums">
                  {cronograma.data.totalDias} días corridos desde el pedido (
                  {formatearFecha(cronograma.data.fechaPedido)})
                </dd>
              </div>
              <div className="flex flex-col gap-0.5">
                <dt className="text-muted-foreground">Frente a la meta propuesta</dt>
                <dd className="font-medium tabular-nums">{textoDiferencia(cronograma.data.diferenciaContraMeta)}</dd>
              </div>
            </dl>
          ) : (
            <Alert role="note">
              <CircleHelpIcon />
              <AlertTitle>Total no calculable</AlertTitle>
              <AlertDescription>
                {cronograma.data.faltantes.length > 0
                  ? `Faltan plazos que el cliente todavía no definió: ${cronograma.data.faltantes
                      .map((etapa) => `${codigoEtapa(tipoFlujo, etapa.numero)} · ${etapa.nombre}`)
                      .join('; ')}. No se usan valores supuestos.`
                  : 'Todavía no se conoce la fecha de llegada.'}
              </AlertDescription>
            </Alert>
          )}

          {/* La tabla no tiene controles: en pantallas angostas el desplazamiento horizontal vive en este marco, que es
              una región enfocable con nombre para moverse con las flechas (WCAG 2.1.1, F5-B3-H02). El contenedor interno
              de la tabla no desplaza, para que no quede un segundo desplazamiento sin foco. */}
          <div
            role="region"
            aria-label="Fechas de cada etapa (desplazable)"
            tabIndex={0}
            className="overflow-x-auto rounded-lg border outline-none focus-visible:ring-3 focus-visible:ring-ring [&>[data-slot=table-container]]:overflow-visible"
          >
            <Table className="min-w-[44rem]">
              <TableCaption className="sr-only">Fechas de cada etapa del pedido</TableCaption>
              <TableHeader>
                <TableRow className="bg-secondary hover:bg-secondary">
                  <TableHead scope="col" className="px-3">Etapa</TableHead>
                  <TableHead scope="col" className="px-3">Planificada</TableHead>
                  <TableHead scope="col" className="px-3">Estimada</TableHead>
                  <TableHead scope="col" className="px-3">Real</TableHead>
                  <TableHead scope="col" className="px-3">Estado</TableHead>
                </TableRow>
              </TableHeader>
              <TableBody>
                {cronograma.data.etapas.map((etapa) => {
                  const estado = ESTADO[etapa.estado];
                  const Icono = estado.icono;
                  return (
                    <TableRow key={etapa.numero}>
                      <TableCell className="px-3">
                        <span className="font-medium tabular-nums">{codigoEtapa(tipoFlujo, etapa.numero)}</span>
                        <span className="text-muted-foreground"> · {etapa.nombre}</span>
                      </TableCell>
                      <TableCell className="px-3 tabular-nums">
                        {etapa.fechaPlanificada ? (
                          <>
                            {formatearFecha(etapa.fechaPlanificada)}
                            {etapa.movidaAlLunes && (
                              <span className="block text-xs text-muted-foreground">Pasó al lunes</span>
                            )}
                          </>
                        ) : etapa.plazoPendiente ? (
                          <span className="text-estado-aviso">Plazo pendiente del cliente</span>
                        ) : (
                          '—'
                        )}
                      </TableCell>
                      <TableCell className="px-3 tabular-nums">{formatearFecha(etapa.fechaEstimada)}</TableCell>
                      <TableCell className="px-3 tabular-nums">{formatearFecha(etapa.fechaReal)}</TableCell>
                      <TableCell className="px-3">
                        <span className={cn('inline-flex items-center gap-1.5', estado.tono)}>
                          <Icono aria-hidden="true" className="size-4" />
                          {estado.texto}
                        </span>
                      </TableCell>
                    </TableRow>
                  );
                })}
              </TableBody>
            </Table>
          </div>
          <p className="text-xs text-muted-foreground">
            Fechas en días de Ecuador. Plazos en días u horas hábiles (lunes a viernes); feriados y jornada laboral
            siguen pendientes del cliente. Una fecha estimada o planificada no es una fecha confirmada.
          </p>
        </>
      )}
    </section>
  );
}

function textoDiferencia(diferencia: number | null | undefined): string {
  if (diferencia === null || diferencia === undefined) return '—';
  if (diferencia === 0) return 'Igual a la meta propuesta';
  const dias = Math.abs(diferencia);
  return `${dias} ${dias === 1 ? 'día' : 'días'} ${diferencia > 0 ? 'por encima' : 'por debajo'} de la meta propuesta`;
}

import { CircleAlertIcon, HistoryIcon, UserCogIcon } from 'lucide-react';
import type { ReactNode } from 'react';
import type { Usuario } from '@/api/esquemas';
import {
  type AccionDisponible,
  type Adjunto,
  type DefinicionFlujo,
  type EtapaEnLinea,
  type PedidoDetalle,
  useAdjuntosDeEtapa,
} from '@/api/pedidos';
import { Alert, AlertDescription, AlertTitle } from '@/components/ui/alert';
import { Badge } from '@/components/ui/badge';
import { Button } from '@/components/ui/button';
import { Card, CardContent, CardDescription, CardHeader, CardTitle } from '@/components/ui/card';
import { Separator } from '@/components/ui/separator';
import { ArchivosEtapa } from './ArchivosEtapa';
import { codigoEtapa, formatearFechaHora, NOMBRE_ACCION, nombreRol, podriaActuarEn } from './formato';
import { NuevaObservacion } from './NuevaObservacion';

// Panel de la etapa elegida en la línea de tiempo (DESIGN.md 7.3). En la etapa actual muestra solo las acciones que
// devolvió el servidor (`accionesDisponibles`, P-06): así una persona con varios roles ve lo que le toca y nada más.
// Subir archivos y agregar observaciones siguen la misma regla (P-05, P-19); el servidor la vuelve a aplicar.
// El resumen de la etapa (requisito 4.2 #17, P-20) es el texto que arma el servidor con el historial; aquí solo se
// muestra tal cual, como texto (React lo escapa), incluido "Entrada anterior" si viene. Si es null no se muestra nada.

interface Props {
  pedido: PedidoDetalle;
  etapa: EtapaEnLinea;
  flujo: DefinicionFlujo | undefined;
  usuario: Usuario;
  onAccion: (accion: AccionDisponible, adjuntos: Adjunto[]) => void;
  onReasignar: () => void;
}

const ESTADO: Record<EtapaEnLinea['estado'], string> = {
  ACTUAL: 'Etapa actual',
  COMPLETADA: 'Completada',
  PENDIENTE: 'Pendiente',
  DEVUELTA: 'Devuelta',
};

export function PanelEtapa({ pedido, etapa, flujo, usuario, onAccion, onReasignar }: Props) {
  const esActual = etapa.estado === 'ACTUAL' && !pedido.finalizado;
  const adjuntos = useAdjuntosDeEtapa(pedido.id, etapa.numero);
  const actuales = (adjuntos.data ?? []).filter((adjunto) => adjunto.entradaId === etapa.entradaId);
  const acciones = esActual ? pedido.accionesDisponibles : [];
  // El servidor solo da acciones a quien puede intervenir en la etapa actual habilitada: la misma regla que para subir
  // archivos y agregar observaciones.
  const puedeIntervenir = esActual && acciones.length > 0;
  const esAdmin = usuario.roles.includes('admin');
  const definicion = flujo?.etapas.find((d) => d.numero === etapa.numero);

  // Pista de P-17: admin, quien puede actuar en la etapa del archivo o en una etapa que la revisa (dato del flujo).
  const revisoras = (flujo?.etapas ?? [])
    .filter((d) => d.revisaEtapa === etapa.numero)
    .map((d) => pedido.lineaDeTiempo.find((e) => e.numero === d.numero))
    .filter((e): e is EtapaEnLinea => e !== undefined);
  const puedeDescargar =
    esAdmin || podriaActuarEn(etapa, usuario) || revisoras.some((revisora) => podriaActuarEn(revisora, usuario));

  const documentos = esActual
    ? acciones.flatMap((accion) => accion.documentosObligatorios)
    : (definicion?.acciones ?? []).flatMap((accion) => accion.documentosObligatorios);
  const tiposSugeridos = [...new Set(documentos)];
  const incompletas = acciones.filter((accion) => !accion.puedeCompletarse);

  return (
    <Card>
      <CardHeader className="gap-3">
        <div className="flex flex-wrap items-start justify-between gap-3">
          <div className="flex flex-col gap-1">
            <CardTitle>
              <h2 className="font-heading text-xl font-semibold">
                <span className="tabular-nums">{codigoEtapa(pedido.tipoFlujo, etapa.numero)}</span> · {etapa.nombre}
              </h2>
            </CardTitle>
            <CardDescription>{nombreRol(etapa.rolResponsable)}</CardDescription>
          </div>
          <Badge variant={esActual ? 'default' : 'outline'} className="h-auto rounded-sm py-0.5 text-sm">
            {ESTADO[etapa.estado]}
          </Badge>
        </div>
        <dl className="grid gap-x-6 gap-y-3 text-sm sm:grid-cols-2 xl:grid-cols-4">
          <Dato termino="Asignada a">
            {etapa.asignadoA ? (
              <>
                {etapa.asignadoA.nombre}
                {etapa.asignacionVigente === false && (
                  <span className="block font-normal text-estado-vencido">
                    Ya no puede actuar: solo el admin actúa o reasigna
                  </span>
                )}
              </>
            ) : (
              <span className="font-normal text-muted-foreground">Cualquiera con el rol</span>
            )}
          </Dato>
          <Dato termino="Entrada">{formatearFechaHora(etapa.entrada)}</Dato>
          <Dato termino={etapa.salida ? 'Salida' : 'Vence'}>
            {etapa.salida ? (
              formatearFechaHora(etapa.salida)
            ) : etapa.estadoPlazo === 'PENDIENTE' ? (
              <span className="font-normal text-estado-aviso">Plazo pendiente del cliente</span>
            ) : etapa.estadoPlazo === 'SIN_PLAZO' ? (
              <span className="font-normal text-muted-foreground">Sin plazo</span>
            ) : (
              formatearFechaHora(etapa.venceEn)
            )}
          </Dato>
          <Dato termino="Reentradas">{etapa.reentradas}</Dato>
        </dl>
        {etapa.resumen && (
          // role="note": es información de la etapa, no un aviso urgente. El Alert de shadcn trae role="alert", que
          // un lector de pantalla anuncia de inmediato cada vez que aparece (F-2.5-03).
          <Alert role="note" aria-labelledby={`resumen-${etapa.numero}`} className="bg-muted/40">
            <HistoryIcon />
            <AlertTitle id={`resumen-${etapa.numero}`}>Lo ocurrido en la etapa</AlertTitle>
            <AlertDescription className="max-w-prose whitespace-pre-wrap text-foreground">
              {etapa.resumen}
            </AlertDescription>
          </Alert>
        )}
      </CardHeader>

      <CardContent className="flex flex-col gap-6">
        {esActual && (
          <section aria-labelledby={`acciones-${etapa.numero}`} className="flex flex-col gap-3">
            <div className="flex flex-wrap items-center justify-between gap-3">
              <h3 id={`acciones-${etapa.numero}`} className="font-heading text-lg font-semibold">
                Acciones
              </h3>
              {esAdmin && etapa.rolResponsable && (
                <Button variant="ghost" size="sm" onClick={onReasignar}>
                  <UserCogIcon data-icon="inline-start" />
                  Reasignar
                </Button>
              )}
            </div>
            {acciones.length === 0 ? (
              <p className="text-sm text-muted-foreground">
                No tienes acciones en esta etapa. Le corresponde a{' '}
                {etapa.asignadoA?.nombre ?? `quien tenga el rol ${nombreRol(etapa.rolResponsable)}`}
                {esAdmin ? ', o la etapa todavía no está habilitada en esta fase' : ''}.
              </p>
            ) : (
              <div className="flex flex-wrap gap-2">
                {acciones.map((accion) => (
                  <Button
                    key={accion.accion}
                    variant={
                      accion.accion === 'RECHAZAR'
                        ? 'destructive'
                        : accion.accion.startsWith('DEVOLVER')
                          ? 'outline'
                          : 'default'
                    }
                    onClick={() => onAccion(accion, actuales)}
                    disabled={adjuntos.isPending}
                  >
                    {NOMBRE_ACCION[accion.accion]}
                  </Button>
                ))}
              </div>
            )}
            {incompletas.length > 0 && (
              <Alert>
                <CircleAlertIcon />
                <AlertDescription>
                  <ul className="flex flex-col gap-1">
                    {incompletas.map((accion) => (
                      <li key={accion.accion}>
                        {NOMBRE_ACCION[accion.accion]}:{' '}
                        {accion.requisitosDocumentales === 'PENDIENTES'
                          ? 'los documentos de esta etapa siguen pendientes del cliente.'
                          : accion.documentosFaltantes.length > 0
                            ? `falta subir ${accion.documentosFaltantes.join(', ')}.`
                            : 'todavía no se puede completar.'}
                      </li>
                    ))}
                  </ul>
                </AlertDescription>
              </Alert>
            )}
          </section>
        )}

        <ArchivosEtapa
          pedidoId={pedido.id}
          etapa={etapa}
          puedeSubir={puedeIntervenir}
          puedeDescargar={puedeDescargar}
          tiposSugeridos={tiposSugeridos}
        />

        {puedeIntervenir && etapa.entradaId && (
          <>
            <Separator />
            <section aria-label="Observaciones de la etapa" className="flex flex-col gap-3">
              <NuevaObservacion
                key={etapa.entradaId}
                pedidoId={pedido.id}
                etapa={etapa.numero}
                entradaId={etapa.entradaId}
                adjuntos={actuales}
              />
            </section>
          </>
        )}
      </CardContent>
    </Card>
  );
}

function Dato({ termino, children }: { termino: string; children: ReactNode }) {
  return (
    <div className="flex flex-col">
      <dt className="text-muted-foreground">{termino}</dt>
      <dd className="font-medium tabular-nums">{children}</dd>
    </div>
  );
}

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
import { MotivoRetraso } from './MotivoRetraso';
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
  /** La etapa tiene su propio espacio de trabajo (BL, revisión o checklist): el panel queda en acciones. */
  enfocada?: boolean;
}

const ESTADO: Record<EtapaEnLinea['estado'], string> = {
  ACTUAL: 'Etapa actual',
  COMPLETADA: 'Completada',
  PENDIENTE: 'Pendiente',
  DEVUELTA: 'Devuelta',
};

export function PanelEtapa({ pedido, etapa, flujo, usuario, onAccion, onReasignar, enfocada = false }: Props) {
  const esActual = etapa.estado === 'ACTUAL' && !pedido.finalizado;
  const adjuntos = useAdjuntosDeEtapa(pedido.id, etapa.numero);
  const actuales = (adjuntos.data ?? []).filter((adjunto) => adjunto.entradaId === etapa.entradaId);
  const acciones = esActual ? pedido.accionesDisponibles : [];
  // El servidor solo da acciones a quien puede intervenir en la etapa actual habilitada: la misma regla que para subir
  // archivos y agregar observaciones.
  const puedeIntervenir = esActual && acciones.length > 0;
  const esAdmin = usuario.roles.includes('admin');
  const definicion = flujo?.etapas.find((d) => d.numero === etapa.numero);

  // Pedido del cliente (2026-11-15): todos los que ven el pedido abren y descargan sus documentos; los confidenciales
  // ni siquiera llegan a quien no tiene acceso (lo decide el servidor).
  const puedeDescargar = true;

  const documentos = esActual
    ? acciones.flatMap((accion) => accion.documentosObligatorios)
    : (definicion?.acciones ?? []).flatMap((accion) => accion.documentosObligatorios);
  const tiposSugeridos = [...new Set(documentos)];
  const incompletas = acciones.filter((accion) => !accion.puedeCompletarse && accion.requisitosDocumentales !== 'PENDIENTES');
  const documentosPendientes = acciones.some((a) => a.requisitosDocumentales === 'PENDIENTES')
    || (definicion?.acciones ?? []).some((a) => a.requisitosDocumentales === 'PENDIENTES');

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
              <span className="font-normal text-muted-foreground">{etapa.rolResponsable ? 'Cualquiera con el rol' : 'Sin intervención manual'}</span>
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
          {etapa.reentradas > 0 && <Dato termino="Reentradas">{etapa.reentradas}</Dato>}
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
          <MotivoRetraso
            key={etapa.entradaId ?? 'sin-entrada'}
            pedido={pedido}
            etapa={etapa}
            puedeEditar={puedeIntervenir || esAdmin}
          />
        )}
        {documentosPendientes && <Alert role="note"><CircleAlertIcon />
          <AlertTitle>Documentos pendientes de definición</AlertTitle>
          <AlertDescription>Falta que el cliente defina los documentos de esta etapa.</AlertDescription>
        </Alert>}
        {definicion?.automatica && <Alert role="note"><AlertTitle>Etapa automática</AlertTitle>
          <AlertDescription>{pedido.finalizado && etapa.estado === 'COMPLETADA'
            ? 'El servidor registró Fin al confirmar el cierre. No requiere otra acción.'
            : 'El servidor registrará esta etapa cuando se confirme el cierre y se cumplan sus requisitos. No admite acciones manuales.'}</AlertDescription>
        </Alert>}
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
                No tienes acciones en esta etapa. Le corresponde a {etapa.asignadoA?.nombre ?? nombreRol(etapa.rolResponsable)}.
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
                    onClick={() => onAccion(accion, accion.exigeEmbarquesListos ? [] : actuales)}
                    disabled={adjuntos.isPending || adjuntos.isError || accion.requisitosDocumentales === 'PENDIENTES'}
                  >
                    {accion.exigeEmbarquesListos ? 'Enviar a aprobación' : definicion?.registraLlegada && accion.accion === 'APROBAR' ? 'Confirmar llegada' : NOMBRE_ACCION[accion.accion]}
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
                          : (accion.faltantesEmbarque?.length ?? 0) > 0
                            ? `falta: ${accion.faltantesEmbarque?.join(', ')}.`
                            : accion.documentosFaltantes.length > 0
                            ? enfocada && accion.documentosFaltantes.length > 3
                              ? `faltan ${accion.documentosFaltantes.length} documentos del checklist.`
                              : `falta subir ${accion.documentosFaltantes.join(', ')}.`
                            : 'todavía no se puede completar.'}
                      </li>
                    ))}
                  </ul>
                </AlertDescription>
              </Alert>
            )}
          </section>
        )}

        {/* En una etapa enfocada los documentos se suben en su espacio de trabajo (BL o checklist). */}
        {!enfocada && (
          <ArchivosEtapa
            pedidoId={pedido.id}
            etapa={etapa}
            puedeSubir={puedeIntervenir}
            puedeDescargar={puedeDescargar}
            tiposSugeridos={tiposSugeridos}
          />
        )}

        {puedeIntervenir && etapa.entradaId && (enfocada ? (
          <details className="group rounded-md border px-3 py-2">
            <summary className="cursor-pointer text-sm font-medium">Agregar observación</summary>
            <section aria-label="Observaciones de la etapa" className="flex flex-col gap-3 pt-3">
              <NuevaObservacion key={etapa.entradaId} pedidoId={pedido.id} etapa={etapa.numero} entradaId={etapa.entradaId} adjuntos={actuales} />
            </section>
          </details>
        ) : (
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
        ))}
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

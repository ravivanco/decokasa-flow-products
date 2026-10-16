import { CircleAlertIcon, CircleCheckIcon, LoaderIcon } from 'lucide-react';
import { type FormEvent, useId, useState } from 'react';
import { toast } from 'sonner';
import { esErrorApi } from '@/api/cliente';
import {
  type AccionDisponible,
  type Adjunto,
  NIVELES_URGENCIA,
  type NivelUrgencia,
  type PedidoDetalle,
  useEjecutarAccion,
} from '@/api/pedidos';
import { Alert, AlertDescription, AlertTitle } from '@/components/ui/alert';
import { Button } from '@/components/ui/button';
import { Checkbox } from '@/components/ui/checkbox';
import {
  Dialog,
  DialogContent,
  DialogDescription,
  DialogFooter,
  DialogHeader,
  DialogTitle,
} from '@/components/ui/dialog';
import { Field, FieldDescription, FieldError, FieldLabel, FieldLegend, FieldSet } from '@/components/ui/field';
import { RadioGroup, RadioGroupItem } from '@/components/ui/radio-group';
import { Textarea } from '@/components/ui/textarea';
import { codigoEtapa, HORAS_URGENCIA, mensajeDeErrorPedido, NOMBRE_ACCION, NOMBRE_URGENCIA } from './formato';

// Diálogo de una acción de la etapa actual (DESIGN.md 7.3). Lo que exige cada acción lo dice el servidor
// (`accionesDisponibles`): observación, urgencia y documentos. Los archivos que se presentan son los de la entrada
// actual (P-18) que la persona marque. Nada cambia en pantalla hasta que el servidor responde: si la etapa ya cambió
// (409) o faltan requisitos (422), se muestra aquí y no se reenvía nada.

interface Props {
  pedido: PedidoDetalle;
  accion: AccionDisponible;
  /** Etapa que la persona veía al abrir el diálogo; si ya no es la actual, el servidor responde 409 ETAPA_CAMBIO. */
  etapaEsperada: number;
  /** Archivos de la entrada actual de la etapa actual. */
  adjuntos: Adjunto[];
  onCerrar: () => void;
}

/** Observación obligatoria por contrato (AccionRechazar y las devoluciones), aunque el flujo no la marque. */
const OBSERVACION_OBLIGATORIA = new Set(['RECHAZAR', 'DEVOLVER_POR_ERROR', 'DEVOLVER_POR_PRODUCTO_NUEVO']);

export function DialogoAccion({ pedido, accion, etapaEsperada, adjuntos, onCerrar }: Props) {
  const ids = useId();
  const ejecutar = useEjecutarAccion();
  const [observacion, setObservacion] = useState('');
  const [nivel, setNivel] = useState<NivelUrgencia | ''>('');
  const [elegidos, setElegidos] = useState<Set<string>>(() => new Set(adjuntos.map((adjunto) => adjunto.id)));
  const [errores, setErrores] = useState<{ observacion?: string; nivel?: string }>({});
  const [fallo, setFallo] = useState<unknown>(null);

  const pideObservacion = accion.requiereObservacion || OBSERVACION_OBLIGATORIA.has(accion.accion);
  const origen = pedido.lineaDeTiempo.find((etapa) => etapa.numero === etapaEsperada);
  const destino = accion.etapaDestino
    ? pedido.lineaDeTiempo.find((etapa) => etapa.numero === accion.etapaDestino)
    : undefined;
  const tiposPresentados = new Set(adjuntos.filter((a) => elegidos.has(a.id)).map((a) => a.tipoDocumento));
  const documentosPendientes = accion.requisitosDocumentales === 'PENDIENTES';
  const etapaCambio = esErrorApi(fallo, 'ETAPA_CAMBIO');

  function alternar(id: string, marcado: boolean) {
    setElegidos((actuales) => {
      const siguiente = new Set(actuales);
      if (marcado) siguiente.add(id);
      else siguiente.delete(id);
      return siguiente;
    });
  }

  function confirmar(evento: FormEvent) {
    evento.preventDefault();
    const texto = observacion.trim();
    const nuevos: typeof errores = {};
    if (pideObservacion && !texto) nuevos.observacion = 'Escribe la observación: esta acción la exige.';
    if (texto.length > 4000) nuevos.observacion = 'Hasta 4000 caracteres.';
    if (accion.requiereUrgencia && !nivel) nuevos.nivel = 'Elige la urgencia.';
    setErrores(nuevos);
    if (Object.keys(nuevos).length > 0) return;
    setFallo(null);
    ejecutar.mutate(
      {
        pedidoId: pedido.id,
        accion: accion.accion,
        etapaEsperada,
        observacion: texto || undefined,
        nivelUrgencia: accion.requiereUrgencia && nivel ? nivel : undefined,
        adjuntoIds: [...elegidos],
      },
      {
        onSuccess: (actualizado) => {
          toast.success(
            `${NOMBRE_ACCION[accion.accion]}: el pedido ${actualizado.numero} quedó en ${codigoEtapa(actualizado.tipoFlujo, actualizado.etapaActual)} · ${actualizado.nombreEtapaActual}.`,
          );
          onCerrar();
        },
        onError: setFallo,
      },
    );
  }

  return (
    <Dialog open onOpenChange={(abierto) => !abierto && !ejecutar.isPending && onCerrar()}>
      <DialogContent className="max-h-[90svh] overflow-y-auto sm:max-w-lg">
        <DialogHeader>
          <DialogTitle>{NOMBRE_ACCION[accion.accion]}</DialogTitle>
          <DialogDescription>
            {codigoEtapa(pedido.tipoFlujo, etapaEsperada)} · {origen?.nombre}
            {destino && ` → ${codigoEtapa(pedido.tipoFlujo, destino.numero)} · ${destino.nombre}`}
          </DialogDescription>
        </DialogHeader>

        {etapaCambio ? (
          <div className="flex flex-col gap-4">
            <Alert variant="destructive">
              <CircleAlertIcon />
              <AlertTitle>Otro usuario actualizó este pedido</AlertTitle>
              <AlertDescription>
                La acción no se ejecutó. El pedido ya muestra su estado actual; revísalo antes de volver a actuar.
              </AlertDescription>
            </Alert>
            <DialogFooter>
              <Button onClick={onCerrar}>Ver estado actual</Button>
            </DialogFooter>
          </div>
        ) : (
          <form noValidate onSubmit={confirmar} className="flex flex-col gap-5">
            {documentosPendientes && (
              <Alert>
                <CircleAlertIcon />
                <AlertDescription>
                  El cliente todavía no definió los documentos de esta etapa, así que la acción no se puede completar.
                </AlertDescription>
              </Alert>
            )}

            {accion.documentosObligatorios.length > 0 && (
              <FieldSet>
                <FieldLegend variant="label">Documentos obligatorios</FieldLegend>
                <ul className="flex flex-col gap-1.5">
                  {accion.documentosObligatorios.map((documento) => {
                    const presente = tiposPresentados.has(documento);
                    return (
                      <li key={documento} className="flex items-center gap-2 text-sm">
                        {presente ? (
                          <CircleCheckIcon aria-hidden="true" className="size-4 text-estado-ok" />
                        ) : (
                          <CircleAlertIcon aria-hidden="true" className="size-4 text-estado-vencido" />
                        )}
                        <span>{documento}</span>
                        <span className={presente ? 'text-estado-ok' : 'text-estado-vencido'}>
                          {presente ? '· adjunto' : '· falta'}
                        </span>
                      </li>
                    );
                  })}
                </ul>
                {accion.documentosFaltantes.length > 0 && (
                  <FieldDescription>
                    Sube primero lo que falta en la sección Archivos de la etapa; después vuelve a esta acción.
                  </FieldDescription>
                )}
              </FieldSet>
            )}

            {adjuntos.length > 0 && (
              <FieldSet>
                <FieldLegend variant="label">Archivos que presentas con la acción</FieldLegend>
                <div className="flex flex-col gap-1">
                  {adjuntos.map((adjunto) => (
                    <Field key={adjunto.id} orientation="horizontal" className="min-h-10">
                      <Checkbox
                        id={`${ids}-${adjunto.id}`}
                        checked={elegidos.has(adjunto.id)}
                        onCheckedChange={(marcado) => alternar(adjunto.id, marcado === true)}
                      />
                      <FieldLabel htmlFor={`${ids}-${adjunto.id}`} className="min-w-0 font-normal">
                        <span className="truncate">{adjunto.nombre}</span>
                        <span className="shrink-0 text-muted-foreground">· {adjunto.tipoDocumento}</span>
                      </FieldLabel>
                    </Field>
                  ))}
                </div>
              </FieldSet>
            )}

            {accion.requiereUrgencia && (
              <FieldSet data-invalid={errores.nivel ? true : undefined}>
                <FieldLegend variant="label">Urgencia</FieldLegend>
                <FieldDescription>
                  Tiene su propio vencimiento hasta que el pedido vuelva a Análisis. Si se cuentan horas hábiles o
                  corridas sigue pendiente del cliente.
                </FieldDescription>
                <RadioGroup
                  value={nivel}
                  onValueChange={(valor) => setNivel(valor as NivelUrgencia)}
                  aria-invalid={errores.nivel ? true : undefined}
                  className="grid gap-2 sm:grid-cols-3"
                >
                  {NIVELES_URGENCIA.map((opcion) => (
                    <Field
                      key={opcion}
                      orientation="horizontal"
                      className="min-h-11 rounded-md border px-3 has-data-[state=checked]:border-primary has-data-[state=checked]:bg-morado-50"
                    >
                      <RadioGroupItem value={opcion} id={`${ids}-${opcion}`} />
                      <FieldLabel htmlFor={`${ids}-${opcion}`} className="font-normal">
                        {NOMBRE_URGENCIA[opcion]} · {HORAS_URGENCIA[opcion]} h
                      </FieldLabel>
                    </Field>
                  ))}
                </RadioGroup>
                <FieldError>{errores.nivel}</FieldError>
              </FieldSet>
            )}

            <Field data-invalid={errores.observacion ? true : undefined}>
              <FieldLabel htmlFor={`${ids}-observacion`}>
                Observación{pideObservacion ? '' : ' (opcional)'}
              </FieldLabel>
              <Textarea
                id={`${ids}-observacion`}
                value={observacion}
                maxLength={4000}
                onChange={(evento) => setObservacion(evento.target.value)}
                aria-invalid={errores.observacion ? true : undefined}
                aria-describedby={`${ids}-contador`}
              />
              <FieldDescription id={`${ids}-contador`} className="tabular-nums">
                {observacion.length} de 4000 caracteres
              </FieldDescription>
              <FieldError>{errores.observacion}</FieldError>
            </Field>

            {fallo !== null && (
              <Alert variant="destructive" aria-live="polite">
                <CircleAlertIcon />
                <AlertDescription>
                  {mensajeDeErrorPedido(fallo, 'No se pudo ejecutar la acción. Intenta de nuevo.')}
                </AlertDescription>
              </Alert>
            )}

            <DialogFooter>
              <Button type="button" variant="outline" onClick={onCerrar} disabled={ejecutar.isPending}>
                Cancelar
              </Button>
              <Button
                type="submit"
                variant={accion.accion === 'RECHAZAR' ? 'destructive' : 'default'}
                disabled={ejecutar.isPending || !accion.puedeCompletarse}
              >
                {ejecutar.isPending && <LoaderIcon data-icon="inline-start" className="animate-spin" />}
                {NOMBRE_ACCION[accion.accion]}
              </Button>
            </DialogFooter>
            {!accion.puedeCompletarse && !documentosPendientes && (
              <p className="text-right text-sm text-muted-foreground">
                Falta: {accion.documentosFaltantes.join(', ') || 'requisitos de la etapa'}.
              </p>
            )}
          </form>
        )}
      </DialogContent>
    </Dialog>
  );
}

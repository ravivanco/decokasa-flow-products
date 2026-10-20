import { CircleAlertIcon, CircleCheckIcon, LoaderIcon } from 'lucide-react';
import { type FormEvent, useId, useState } from 'react';
import { toast } from 'sonner';
import { esErrorApi } from '@/api/cliente';
import { useBodegas } from '@/api/bodegas';
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
import { Input } from '@/components/ui/input';
import { Select, SelectContent, SelectGroup, SelectItem, SelectTrigger, SelectValue } from '@/components/ui/select';
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
  registraLlegada?: boolean;
  destinoAutomatico?: boolean;
}

/** Observación obligatoria por contrato (AccionRechazar y las devoluciones), aunque el flujo no la marque. */
const OBSERVACION_OBLIGATORIA = new Set(['RECHAZAR', 'DEVOLVER_POR_ERROR', 'DEVOLVER_POR_PRODUCTO_NUEVO', 'REPORTAR_DEMORA']);

export function DialogoAccion({ pedido, accion, etapaEsperada, adjuntos, onCerrar, registraLlegada = false, destinoAutomatico = false }: Props) {
  const ids = useId();
  const ejecutar = useEjecutarAccion();
  const [observacion, setObservacion] = useState('');
  const [nivel, setNivel] = useState<NivelUrgencia | ''>('');
  const [fecha, setFecha] = useState('');
  const [bodegaId, setBodegaId] = useState('');
  const bodegas = useBodegas(true, accion.requiereBodega);
  const [elegidos, setElegidos] = useState<Set<string>>(() => new Set(adjuntos.map((adjunto) => adjunto.id)));
  const [errores, setErrores] = useState<{ observacion?: string; nivel?: string; fecha?: string; bodega?: string }>({});
  const [fallo, setFallo] = useState<unknown>(null);

  const pideObservacion = accion.requiereObservacion || OBSERVACION_OBLIGATORIA.has(accion.accion);
  const origen = pedido.lineaDeTiempo.find((etapa) => etapa.numero === etapaEsperada);
  const destino = accion.etapaDestino
    ? pedido.lineaDeTiempo.find((etapa) => etapa.numero === accion.etapaDestino)
    : undefined;
  const tiposPresentados = new Set(adjuntos.filter((a) => elegidos.has(a.id)).map((a) => a.tipoDocumento));
  const documentosPendientes = accion.requisitosDocumentales === 'PENDIENTES';
  const etapaCambio = esErrorApi(fallo, 'ETAPA_CAMBIO') || pedido.etapaActual !== etapaEsperada || pedido.finalizado;
  const bodegaVigente = bodegas.data?.some((bodega) => bodega.id === bodegaId && bodega.activa) ?? false;
  const faltanDocumentos = accion.documentosObligatorios.some((tipo) => !tiposPresentados.has(tipo));
  const sinPermiso = esErrorApi(fallo) && fallo.status === 403;
  const nombreAccion = destinoAutomatico ? 'Confirmar y finalizar' : registraLlegada ? 'Confirmar llegada' : NOMBRE_ACCION[accion.accion];

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
    // Aunque se envíe el formulario con Enter, estos bloqueos del servidor no se pueden omitir.
    if (ejecutar.isPending || etapaCambio || sinPermiso || documentosPendientes || !accion.puedeCompletarse || faltanDocumentos
      || (accion.requiereBodega && (bodegas.isFetching || bodegas.isError))) return;
    const texto = observacion.trim();
    const nuevos: typeof errores = {};
    if (pideObservacion && !texto) nuevos.observacion = 'Escribe la observación: esta acción la exige.';
    if (texto.length > 4000) nuevos.observacion = 'Hasta 4000 caracteres.';
    if (accion.requiereUrgencia && !nivel) nuevos.nivel = 'Elige la urgencia.';
    if (accion.accion === 'REPORTAR_DEMORA' && !/^\d{4}-\d{2}-\d{2}$/.test(fecha)) nuevos.fecha = 'Indica la fecha estimada de llegada.';
    if (accion.requiereBodega && !bodegaVigente) nuevos.bodega = 'Elige una bodega activa de la lista vigente.';
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
        nuevaFechaEstimadaLlegada: accion.accion === 'REPORTAR_DEMORA' ? fecha : undefined,
        bodegaId: accion.requiereBodega ? bodegaId : undefined,
        adjuntoIds: [...elegidos],
      },
      {
        onSuccess: (actualizado) => {
          toast.success(actualizado.finalizado
            ? `El servidor confirmó el cierre de ${actualizado.numero}.`
            : accion.accion === 'REPORTAR_DEMORA'
              ? 'Demora registrada. La fecha confirmada aparece en el pedido.'
              : `${NOMBRE_ACCION[accion.accion]}: ${actualizado.numero} quedó en ${codigoEtapa(actualizado.tipoFlujo, actualizado.etapaActual)} · ${actualizado.nombreEtapaActual}.`);
          onCerrar();
        },
        onError: (error) => { setFallo(error); if (accion.requiereBodega) void bodegas.refetch(); },
      },
    );
  }

  return (
    <Dialog open onOpenChange={(abierto) => !abierto && !ejecutar.isPending && onCerrar()}>
      <DialogContent className="max-h-[90svh] overflow-y-auto sm:max-w-lg">
        <DialogHeader>
          <DialogTitle>{nombreAccion}</DialogTitle>
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
                No se reenviará la acción. Revisa el estado actual del pedido antes de volver a actuar.
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
                        disabled={ejecutar.isPending}
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

            {accion.accion === 'REPORTAR_DEMORA' && (
              <Field data-invalid={errores.fecha ? true : undefined}>
                <FieldLabel htmlFor={`${ids}-fecha`}>Nueva fecha estimada de llegada</FieldLabel>
                <Input id={`${ids}-fecha`} type="date" value={fecha} onChange={(e) => setFecha(e.target.value)}
                  disabled={ejecutar.isPending} aria-invalid={errores.fecha ? true : undefined} aria-describedby={`${ids}-fecha-ayuda`} />
                <FieldDescription id={`${ids}-fecha-ayuda`}>
                  El servidor comprueba la fecha y ajusta los fines de semana al lunes. Es una estimación, no un vencimiento.
                </FieldDescription>
                <FieldError>{errores.fecha}</FieldError>
              </Field>
            )}

            {accion.requiereBodega && (
              <Field data-invalid={errores.bodega ? true : undefined}>
                <FieldLabel htmlFor={`${ids}-bodega`}>Bodega de destino</FieldLabel>
                {bodegas.isPending ? <p role="status" className="text-sm text-muted-foreground">Cargando bodegas activas…</p>
                  : bodegas.isError ? <Alert variant="destructive"><AlertDescription>
                    No se pudieron cargar las bodegas. <Button type="button" variant="outline" size="sm" onClick={() => void bodegas.refetch()}>Reintentar</Button>
                  </AlertDescription></Alert>
                  : <Select value={bodegaVigente ? bodegaId : ''} onValueChange={setBodegaId} disabled={ejecutar.isPending}>
                    <SelectTrigger id={`${ids}-bodega`} className="w-full" aria-invalid={errores.bodega ? true : undefined} aria-describedby={`${ids}-bodega-ayuda`}>
                      <SelectValue placeholder={bodegas.data?.length ? 'Elige una bodega activa' : 'No hay bodegas activas'} />
                    </SelectTrigger>
                    <SelectContent><SelectGroup>{bodegas.data?.map((bodega) => (
                      <SelectItem key={bodega.id} value={bodega.id}>{bodega.nombre} · {bodega.ciudad}</SelectItem>
                    ))}</SelectGroup></SelectContent>
                  </Select>}
                <FieldDescription id={`${ids}-bodega-ayuda`}>Se asigna una sola vez al confirmar. El servidor vuelve a comprobar que esté activa.</FieldDescription>
                {bodegaId && !bodegaVigente && !bodegas.isFetching && <p role="status" className="text-sm text-estado-aviso">La selección ya no está disponible. Elige otra bodega activa.</p>}
                <FieldError>{errores.bodega}</FieldError>
              </Field>
            )}

            {accion.requiereUrgencia && (
              <FieldSet data-invalid={errores.nivel ? true : undefined}>
                <FieldLegend variant="label">Urgencia</FieldLegend>
                <FieldDescription>
                  Tiene su propio vencimiento hasta que el pedido vuelva a Análisis. Si se cuentan horas hábiles o
                  corridas sigue pendiente del cliente.
                </FieldDescription>
                <RadioGroup
                  disabled={ejecutar.isPending}
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
                disabled={ejecutar.isPending}
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
                disabled={ejecutar.isPending || sinPermiso || documentosPendientes || !accion.puedeCompletarse || faltanDocumentos || (accion.requiereBodega && (!bodegaVigente || bodegas.isFetching || bodegas.isError))}
              >
                {ejecutar.isPending && <LoaderIcon data-icon="inline-start" className="animate-spin" />}
                {nombreAccion}
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

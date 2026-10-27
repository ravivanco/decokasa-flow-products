import { CircleAlertIcon, LoaderIcon, SaveIcon } from 'lucide-react';
import { type FormEvent, useId, useState } from 'react';
import { toast } from 'sonner';
import { usePausarActualizacion } from '@/api/actualizacion';
import { type EtapaEnLinea, type PedidoDetalle, useRegistrarMotivo } from '@/api/pedidos';
import { Alert, AlertDescription } from '@/components/ui/alert';
import { Button } from '@/components/ui/button';
import { Field, FieldDescription, FieldError, FieldLabel } from '@/components/ui/field';
import { Textarea } from '@/components/ui/textarea';
import { useBorrador } from './borradores';
import { mensajeDeErrorPedido } from './formato';

// Motivo del retraso de la etapa actual (requisito 4.2 #16; F5-D3, provisional del equipo). Se ve mientras haya un
// vencimiento activo (etapa o urgencia, según el servidor) o un motivo vigente. Lo registra o corrige quien puede
// actuar en la etapa (P-06) o el admin: aquí solo se ofrece; el servidor decide con el estado vigente. Cada cambio
// queda en el historial y no envía correo. Quien lo usa lo monta con key por entrada: una reentrada lo limpia.

interface Props {
  pedido: PedidoDetalle;
  etapa: EtapaEnLinea;
  /** Pista de la pantalla (acciones del servidor o admin); no es el permiso. */
  puedeEditar: boolean;
}

export function MotivoRetraso({ pedido, etapa, puedeEditar }: Props) {
  const ids = useId();
  const registrar = useRegistrarMotivo();
  const vigente = pedido.motivoRetraso ?? null;
  const etapaVencida = !pedido.finalizado && pedido.vencida;
  const urgenciaVencida = !pedido.finalizado && pedido.urgencia?.vencida === true;
  const vencimientoActivo = etapaVencida || urgenciaVencida;
  // Paso 5.5: el texto vive en el borrador de esta entrada; si la etapa cambia, no se pierde (BorradoresEnConflicto).
  const borrador = useBorrador('motivo', etapa.numero, etapa.entradaId ?? '', vigente ?? '');
  const texto = borrador.texto;
  const setTexto = borrador.cambiar;
  const [error, setError] = useState<string | null>(null);
  const [fallo, setFallo] = useState<unknown>(null);
  // Paso 5.4: con un motivo escrito y sin guardar, el refresco automático no cambia la pantalla.
  usePausarActualizacion(texto.trim() !== (vigente ?? '').trim());

  if (!vigente && !vencimientoActivo) return null;
  const editable = vencimientoActivo && puedeEditar && Boolean(etapa.entradaId);

  function guardar(evento: FormEvent) {
    evento.preventDefault();
    const limpio = texto.trim();
    if (!limpio) {
      setError('Escribe el motivo del retraso.');
      return;
    }
    if (!etapa.entradaId) return;
    setError(null);
    setFallo(null);
    registrar.mutate(
      { pedidoId: pedido.id, etapa: etapa.numero, entradaEsperadaId: etapa.entradaId, motivo: limpio },
      {
        onSuccess: () => {
          toast.success('Motivo del retraso guardado en el historial.');
          borrador.descartar();
        },
        onError: setFallo,
      },
    );
  }

  return (
    <section
      aria-labelledby={`${ids}-titulo`}
      className="flex flex-col gap-3 rounded-md border border-estado-vencido bg-estado-vencido-fondo p-4"
    >
      <h3 id={`${ids}-titulo`} className="flex items-center gap-2 font-heading text-lg font-semibold">
        <CircleAlertIcon aria-hidden="true" className="size-5 text-estado-vencido" />
        Motivo del retraso
      </h3>
      {vencimientoActivo && (
        <p className="text-sm">
          {etapaVencida && urgenciaVencida
            ? 'Vencieron el plazo de la etapa y el de la urgencia.'
            : etapaVencida
              ? 'Venció el plazo de la etapa.'
              : 'Venció el plazo de la urgencia.'}
        </p>
      )}
      <p className="max-w-prose text-sm whitespace-pre-wrap">
        {vigente ?? <span className="text-muted-foreground">Sin motivo registrado.</span>}
      </p>
      {editable ? (
        <form noValidate onSubmit={guardar} className="flex flex-col gap-3">
          <Field data-invalid={error ? true : undefined}>
            <FieldLabel htmlFor={`${ids}-texto`}>{vigente ? 'Corregir el motivo' : 'Registrar el motivo'}</FieldLabel>
            <Textarea
              id={`${ids}-texto`}
              value={texto}
              maxLength={4000}
              onChange={(evento) => setTexto(evento.target.value)}
              aria-invalid={error ? true : undefined}
              aria-describedby={`${ids}-ayuda`}
            />
            <FieldDescription id={`${ids}-ayuda`} className="tabular-nums">
              {texto.length} de 4000 caracteres · cada cambio queda en el historial; no envía correo
            </FieldDescription>
            <FieldError>{error}</FieldError>
          </Field>
          {fallo !== null && (
            <Alert variant="destructive" aria-live="polite">
              <CircleAlertIcon />
              <AlertDescription>
                {mensajeDeErrorPedido(fallo, 'No se pudo guardar el motivo. Intenta de nuevo.')}
              </AlertDescription>
            </Alert>
          )}
          <div className="flex justify-end">
            <Button type="submit" variant="outline" disabled={registrar.isPending}>
              {registrar.isPending ? (
                <LoaderIcon data-icon="inline-start" className="animate-spin" />
              ) : (
                <SaveIcon data-icon="inline-start" />
              )}
              Guardar motivo
            </Button>
          </div>
        </form>
      ) : vencimientoActivo ? (
        <p className="text-sm text-muted-foreground">
          Lo registra quien puede actuar en la etapa o el admin.
        </p>
      ) : null}
    </section>
  );
}

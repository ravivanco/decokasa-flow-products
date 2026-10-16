import { CircleAlertIcon, LoaderIcon, SendIcon } from 'lucide-react';
import { type FormEvent, useId, useState } from 'react';
import { toast } from 'sonner';
import { type Adjunto, useAgregarObservacion } from '@/api/pedidos';
import { Alert, AlertDescription } from '@/components/ui/alert';
import { Button } from '@/components/ui/button';
import { Checkbox } from '@/components/ui/checkbox';
import { Field, FieldDescription, FieldError, FieldLabel, FieldLegend, FieldSet } from '@/components/ui/field';
import { Textarea } from '@/components/ui/textarea';
import { mensajeDeErrorPedido } from './formato';

// Nueva observación (DESIGN.md 7.4; P-19, provisional del equipo): la agrega quien puede actuar en la etapa actual o
// el admin, sobre la entrada actual. No mueve el pedido ni reinicia plazos; queda en el historial con autor, rol y
// hora del servidor. Quien lo usa lo monta con key por entrada: lo escrito no pasa a otra etapa ni a otra entrada.

interface Props {
  pedidoId: string;
  etapa: number;
  entradaId: string;
  /** Archivos de la entrada actual que se pueden relacionar con la observación. */
  adjuntos: Adjunto[];
}

export function NuevaObservacion({ pedidoId, etapa, entradaId, adjuntos }: Props) {
  const ids = useId();
  const agregar = useAgregarObservacion();
  const [texto, setTexto] = useState('');
  const [elegidos, setElegidos] = useState<Set<string>>(() => new Set());
  const [error, setError] = useState<string | null>(null);
  const [fallo, setFallo] = useState<unknown>(null);

  function alternar(id: string, marcado: boolean) {
    setElegidos((actuales) => {
      const siguiente = new Set(actuales);
      if (marcado) siguiente.add(id);
      else siguiente.delete(id);
      return siguiente;
    });
  }

  function publicar(evento: FormEvent) {
    evento.preventDefault();
    const limpio = texto.trim();
    if (!limpio) {
      setError('Escribe la observación.');
      return;
    }
    setError(null);
    setFallo(null);
    agregar.mutate(
      { pedidoId, etapaEsperada: etapa, entradaEsperadaId: entradaId, texto: limpio, adjuntoIds: [...elegidos] },
      {
        onSuccess: () => {
          toast.success('Observación publicada en el historial.');
          setTexto('');
          setElegidos(new Set());
        },
        onError: setFallo,
      },
    );
  }

  return (
    <form noValidate onSubmit={publicar} className="flex flex-col gap-4">
      <Field data-invalid={error ? true : undefined}>
        <FieldLabel htmlFor={`${ids}-texto`}>Nueva observación</FieldLabel>
        <Textarea
          id={`${ids}-texto`}
          value={texto}
          maxLength={4000}
          onChange={(evento) => setTexto(evento.target.value)}
          aria-invalid={error ? true : undefined}
          aria-describedby={`${ids}-contador`}
        />
        <FieldDescription id={`${ids}-contador`} className="tabular-nums">
          {texto.length} de 4000 caracteres · no cambia la etapa ni sus plazos
        </FieldDescription>
        <FieldError>{error}</FieldError>
      </Field>
      {adjuntos.length > 0 && (
        <FieldSet>
          <FieldLegend variant="label">Archivos relacionados (opcional)</FieldLegend>
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
      {fallo !== null && (
        <Alert variant="destructive" aria-live="polite">
          <CircleAlertIcon />
          <AlertDescription>
            {mensajeDeErrorPedido(fallo, 'No se pudo publicar la observación. Intenta de nuevo.')}
          </AlertDescription>
        </Alert>
      )}
      <div className="flex justify-end">
        <Button type="submit" variant="outline" disabled={agregar.isPending}>
          {agregar.isPending ? (
            <LoaderIcon data-icon="inline-start" className="animate-spin" />
          ) : (
            <SendIcon data-icon="inline-start" />
          )}
          Publicar observación
        </Button>
      </div>
    </form>
  );
}

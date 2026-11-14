import { CircleAlertIcon, LoaderIcon, PaperclipIcon, RotateCwIcon } from 'lucide-react';
import { useEffect, useRef, useState } from 'react';
import { useCargaArchivo } from '@/api/cargas';
import { type Checklist, useChecklist, useMarcarRequisito } from '@/api/embarques';
import type { EtapaEnLinea, PedidoDetalle } from '@/api/pedidos';
import { Alert, AlertDescription, AlertTitle } from '@/components/ui/alert';
import { Button } from '@/components/ui/button';
import { Checkbox } from '@/components/ui/checkbox';
import { cn } from '@/lib/utils';
import { EXTENSIONES_ACEPTADAS, mensajeDeCarga } from './SeccionEmbarques';

// Checklist de la etapa (V35, pedido del cliente del 2026-11-14): cada requisito se cumple con la marca ✓ de quien
// actúa en la etapa o con un archivo de ese tipo en la entrada actual. Adjuntar es opcional. La marca la guarda el
// servidor (quién y cuándo) y el servidor vuelve a comprobarla al aprobar; la pantalla nunca marca nada por su cuenta.

interface Props {
  pedido: PedidoDetalle;
  etapa: EtapaEnLinea;
  /** Se conserva por compatibilidad; el permiso real llega del servidor en `puedeMarcar`. */
  puedeSubir?: boolean;
}

const fecha = new Intl.DateTimeFormat('es-EC', { dateStyle: 'medium', timeStyle: 'short', timeZone: 'America/Guayaquil' });

export function ChecklistDocumentos({ pedido, etapa }: Props) {
  const checklist = useChecklist(pedido.id, true);
  const datos = checklist.data;
  if (!datos?.aplica) return null;
  return (
    <section aria-labelledby="checklist-titulo" className="flex flex-col gap-3 rounded-lg border bg-card p-4 shadow-sm">
      <div className="flex flex-wrap items-center justify-between gap-3">
        <div>
          <h2 id="checklist-titulo" className="font-heading text-lg font-semibold">Checklist de documentos</h2>
          <p className="text-sm text-muted-foreground">
            Marca cada documento cuando lo tengas. Adjuntar el archivo es opcional.
          </p>
        </div>
        <p className="font-heading text-2xl font-bold tabular-nums text-primary" aria-label={`${datos.confirmados} de ${datos.total} documentos listos`}>
          {datos.confirmados}/{datos.total}
        </p>
      </div>
      {datos.pendienteDefinicion && (
        <Alert role="note">
          <CircleAlertIcon />
          <AlertTitle>Lista oficial pendiente</AlertTitle>
          <AlertDescription>El cliente todavía no definió esta lista. La etapa no se puede aprobar hasta tenerla.</AlertDescription>
        </Alert>
      )}
      {datos.total > 0 && (
        <progress aria-label="Avance del checklist" max={datos.total} value={datos.confirmados} className="h-2 w-full" />
      )}
      <ol className="grid gap-2 md:grid-cols-2">
        {datos.requisitos.map((requisito, indice) => (
          <Requisito
            key={requisito.tipoDocumento}
            numero={indice + 1}
            pedidoId={pedido.id}
            etapa={etapa}
            entradaId={datos.entradaId ?? etapa.entradaId ?? null}
            requisito={requisito}
            puedeMarcar={datos.puedeMarcar}
          />
        ))}
      </ol>
    </section>
  );
}

function Requisito({ numero, pedidoId, etapa, entradaId, requisito, puedeMarcar }: {
  numero: number;
  pedidoId: string;
  etapa: EtapaEnLinea;
  entradaId: string | null;
  requisito: Checklist['requisitos'][number];
  puedeMarcar: boolean;
}) {
  const tipo = requisito.tipoDocumento;
  const conArchivo = requisito.adjuntoId !== null;
  const cumplido = requisito.marcado || conArchivo;
  const marcar = useMarcarRequisito(pedidoId);
  const subir = useCargaArchivo();
  const entrada = useRef<HTMLInputElement>(null);
  const control = useRef<AbortController | null>(null);
  const [archivo, setArchivo] = useState<File | null>(null);
  const [error, setError] = useState<string | null>(null);
  useEffect(() => () => control.current?.abort(), []);

  function cambiarMarca(marcado: boolean) {
    if (!entradaId) return;
    setError(null);
    marcar.mutate(
      { tipoDocumento: tipo, marcado, entradaEsperadaId: entradaId },
      { onError: () => setError('No se pudo guardar la marca. Vuelve a intentarlo.') },
    );
  }

  function cargar(elegido: File) {
    if (!etapa.entradaId) return;
    setError(null);
    setArchivo(elegido);
    control.current = new AbortController();
    subir.mutate(
      { pedidoId, etapa: etapa.numero, entradaEsperadaId: etapa.entradaId, tipoDocumento: tipo, archivo: elegido,
        signal: control.current.signal, onProgreso: () => undefined },
      {
        onSuccess: () => { setArchivo(null); if (entrada.current) entrada.current.value = ''; },
        onError: (fallo) => setError(mensajeDeCarga(fallo)),
      },
    );
  }

  const idCasilla = `checklist-${numero}`;
  const detalle = requisito.marcado && requisito.marcadoPor
    ? `Marcado por ${requisito.marcadoPor}${requisito.marcadoEn ? ` · ${fecha.format(new Date(requisito.marcadoEn))}` : ''}`
    : null;

  return (
    <li className={cn('flex flex-wrap items-center gap-3 rounded-md border px-3 py-2', cumplido && 'border-estado-ok bg-estado-ok-fondo')}>
      <div className="flex min-h-10 items-center">
        <Checkbox
          id={idCasilla}
          checked={cumplido}
          // Con archivo ya está cumplido: la casilla no se puede quitar desde aquí.
          disabled={!puedeMarcar || conArchivo || marcar.isPending || !entradaId}
          onCheckedChange={(valor) => cambiarMarca(valor === true)}
        />
      </div>
      <label htmlFor={idCasilla} className="min-w-40 flex-1 text-sm">
        <span className="tabular-nums text-muted-foreground">{numero}. </span>
        {tipo}
        {requisito.nombreArchivo && (
          <span className="flex items-center gap-1 truncate text-xs text-muted-foreground">
            <PaperclipIcon aria-hidden="true" className="size-3" />
            {requisito.nombreArchivo}
          </span>
        )}
        {detalle && <span className="block text-xs text-muted-foreground">{detalle}</span>}
      </label>
      {marcar.isPending || subir.isPending ? (
        <span role="status" className="flex items-center gap-1 text-xs text-muted-foreground">
          <LoaderIcon aria-hidden="true" className="size-3 animate-spin" />
          Guardando
        </span>
      ) : puedeMarcar && !conArchivo ? (
        <>
          <input
            ref={entrada}
            type="file"
            aria-label={`Archivo de ${tipo}`}
            accept={EXTENSIONES_ACEPTADAS.join(',')}
            className="sr-only"
            onChange={(evento) => { const elegido = evento.target.files?.[0]; if (elegido) cargar(elegido); }}
          />
          {error && archivo ? (
            <Button type="button" variant="outline" size="sm" onClick={() => cargar(archivo)}>
              <RotateCwIcon data-icon="inline-start" />
              Reintentar
            </Button>
          ) : (
            <Button type="button" variant="ghost" size="sm" onClick={() => entrada.current?.click()} aria-label={`Adjuntar archivo de ${tipo} (opcional)`}>
              <PaperclipIcon data-icon="inline-start" />
              Adjuntar
            </Button>
          )}
        </>
      ) : null}
      {error && <p role="alert" className="w-full text-xs text-estado-vencido">{error}</p>}
    </li>
  );
}

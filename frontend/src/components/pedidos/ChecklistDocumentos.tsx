import { CircleAlertIcon, CircleCheckIcon, LoaderIcon, RotateCwIcon, UploadIcon } from 'lucide-react';
import { useEffect, useRef, useState } from 'react';
import { useCargaArchivo } from '@/api/cargas';
import { type Checklist, useChecklist } from '@/api/embarques';
import type { EtapaEnLinea, PedidoDetalle } from '@/api/pedidos';
import { Alert, AlertDescription, AlertTitle } from '@/components/ui/alert';
import { Button } from '@/components/ui/button';
import { cn } from '@/lib/utils';
import { EXTENSIONES_ACEPTADAS, mensajeDeCarga } from './SeccionEmbarques';

// Checklist de la etapa (cliente, 2026-11-14): cada uno de los requisitos exige subir su documento, y el ✓ aparece solo
// cuando el servidor confirma el archivo de ese tipo en la entrada actual. No hay marca manual.

interface Props {
  pedido: PedidoDetalle;
  etapa: EtapaEnLinea;
  /** Se conserva por compatibilidad; el permiso real llega del servidor en `puedeSubir`. */
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
            Sube el documento de cada ítem; se marca ✓ cuando el servidor confirma el archivo.
          </p>
        </div>
        <p className="font-heading text-2xl font-bold tabular-nums text-primary" aria-label={`${datos.confirmados} de ${datos.total} documentos subidos`}>
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
            requisito={requisito}
            puedeSubir={datos.puedeSubir}
          />
        ))}
      </ol>
    </section>
  );
}

function Requisito({ numero, pedidoId, etapa, requisito, puedeSubir }: {
  numero: number;
  pedidoId: string;
  etapa: EtapaEnLinea;
  requisito: Checklist['requisitos'][number];
  puedeSubir: boolean;
}) {
  const tipo = requisito.tipoDocumento;
  const confirmado = requisito.adjuntoId !== null;
  const subir = useCargaArchivo();
  const entrada = useRef<HTMLInputElement>(null);
  const control = useRef<AbortController | null>(null);
  const [archivo, setArchivo] = useState<File | null>(null);
  const [error, setError] = useState<string | null>(null);
  useEffect(() => () => control.current?.abort(), []);

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

  const detalle = confirmado && requisito.subidoPor
    ? `Subido por ${requisito.subidoPor}${requisito.subidoEn ? ` · ${fecha.format(new Date(requisito.subidoEn))}` : ''}`
    : null;

  return (
    <li className={cn('flex flex-wrap items-center gap-3 rounded-md border px-3 py-2', confirmado && 'border-estado-ok bg-estado-ok-fondo')}>
      <span
        role="img"
        aria-label={confirmado ? `${tipo}: documento confirmado` : `${tipo}: falta el documento`}
        className={cn('flex size-6 shrink-0 items-center justify-center rounded-full text-xs font-semibold tabular-nums',
          confirmado ? 'bg-estado-ok text-primary-foreground' : 'border bg-secondary')}
      >
        {confirmado ? <CircleCheckIcon aria-hidden="true" className="size-4" /> : numero}
      </span>
      <span className="min-w-40 flex-1 text-sm">
        {tipo}
        {requisito.nombreArchivo && <span className="block truncate text-xs text-muted-foreground">{requisito.nombreArchivo}</span>}
        {detalle && <span className="block text-xs text-muted-foreground">{detalle}</span>}
      </span>
      {confirmado ? null : subir.isPending ? (
        <span role="status" className="flex items-center gap-1 text-xs text-muted-foreground">
          <LoaderIcon aria-hidden="true" className="size-3 animate-spin" />
          Esperando confirmación
        </span>
      ) : puedeSubir ? (
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
            <Button type="button" variant="outline" size="sm" onClick={() => entrada.current?.click()} aria-label={`Subir ${tipo}`}>
              <UploadIcon data-icon="inline-start" />
              Subir
            </Button>
          )}
        </>
      ) : (
        <span className="text-xs text-muted-foreground">Pendiente</span>
      )}
      {error && <p role="alert" className="w-full text-xs text-estado-vencido">{error}</p>}
    </li>
  );
}

import { BadgeCheckIcon, DownloadIcon, EyeIcon, FileTextIcon, HistoryIcon } from 'lucide-react';
import { useEffect, useState } from 'react';
import { esErrorApi } from '@/api/cliente';
import {
  type DocumentoPedido,
  TIPOS_VISIBLES,
  useDescargarArchivo,
  useDocumentosPedido,
  useVerArchivo,
} from '@/api/pedidos';
import { Alert, AlertDescription } from '@/components/ui/alert';
import { Badge } from '@/components/ui/badge';
import { Button } from '@/components/ui/button';
import { Dialog, DialogContent, DialogDescription, DialogHeader, DialogTitle } from '@/components/ui/dialog';
import { Skeleton } from '@/components/ui/skeleton';
import { codigoEtapa, formatearFechaHora, formatearTamano, mensajeDeErrorPedido, NOMBRE_EVENTO } from './formato';

// Documentos del pedido desde DECOKASA (ADR-0028). Drive solo guarda: el contenido se pide siempre al backend, que
// autoriza en cada apertura y lo sirve del servidor o de Drive con la conexión central. Nunca se muestra un enlace de
// Drive ni se envía el documento a visores externos.

const ESTADO_COPIA: Record<DocumentoPedido['estadoCopia'], string> = {
  COPIADO: 'Guardado en Drive',
  PENDIENTE: 'Copia a Drive pendiente',
  FALLIDO: 'Copia a Drive fallida',
};

export const visible = (documento: Pick<DocumentoPedido, 'tipoContenido'>) =>
  TIPOS_VISIBLES.some((tipo) => tipo === documento.tipoContenido);

/** Mensaje de un fallo al abrir: los textos del servidor para 409 y 503 son cerrados y comprensibles. */
function mensajeApertura(error: unknown): string {
  if (esErrorApi(error, 'ARCHIVO_NO_DISPONIBLE') || esErrorApi(error, 'GOOGLE_NO_DISPONIBLE')) return error.message;
  if (esErrorApi(error) && error.status === 403) return 'No tienes permiso para abrir este documento.';
  return mensajeDeErrorPedido(error, 'No se pudo abrir el documento. Intenta de nuevo.');
}

/**
 * Abre un documento: PDF e imágenes en el visor de DECOKASA; Excel, CSV y el resto se descargan para abrirlos con la
 * aplicación del equipo. Devuelve la acción y el visor que hay que montar.
 */
export function useAbridorDocumentos() {
  const ver = useVerArchivo();
  const descargar = useDescargarArchivo();
  const [visor, setVisor] = useState<{ nombre: string; url: string; tipo: string } | null>(null);
  const [error, setError] = useState<string | null>(null);

  useEffect(() => () => { if (visor) URL.revokeObjectURL(visor.url); }, [visor]);

  function abrir(documento: Pick<DocumentoPedido, 'id' | 'nombre' | 'tipoContenido'>) {
    setError(null);
    if (visible(documento)) {
      ver.mutate(documento, {
        onSuccess: ({ url, tipo }) => setVisor({ nombre: documento.nombre, url, tipo }),
        onError: (fallo) => setError(mensajeApertura(fallo)),
      });
    } else {
      descargar.mutate(documento, { onError: (fallo) => setError(mensajeApertura(fallo)) });
    }
  }

  const elemento = (
    <Dialog open={visor !== null} onOpenChange={(abierto) => { if (!abierto) setVisor(null); }}>
      <DialogContent className="flex h-[90svh] max-w-[min(64rem,95vw)] flex-col gap-3 sm:max-w-[min(64rem,95vw)]">
        <DialogHeader>
          <DialogTitle className="truncate">{visor?.nombre}</DialogTitle>
          <DialogDescription>Vista desde DECOKASA. Para guardarlo, usa Descargar.</DialogDescription>
        </DialogHeader>
        {visor && visor.tipo === 'application/pdf' && (
          <iframe title={`Documento ${visor.nombre}`} src={visor.url} className="min-h-0 w-full flex-1 rounded-md border" />
        )}
        {visor && visor.tipo !== 'application/pdf' && (
          <div className="flex min-h-0 flex-1 items-center justify-center overflow-auto rounded-md border bg-muted/40">
            <img src={visor.url} alt={visor.nombre} className="max-h-full max-w-full object-contain" />
          </div>
        )}
      </DialogContent>
    </Dialog>
  );

  return { abrir, ocupado: ver.isPending || descargar.isPending, error, elemento };
}

interface Props {
  pedidoId: string;
  tipoFlujo: string;
  nombreEtapa: (numero: number) => string | undefined;
  etapaActual?: number;
  finalizado?: boolean;
}

/** Documentos agrupados por etapa y por entrada (cada paso por la etapa), del más antiguo al más reciente. */
export function DocumentosPedido({ pedidoId, tipoFlujo, nombreEtapa, etapaActual, finalizado }: Props) {
  const documentos = useDocumentosPedido(pedidoId);
  const abridor = useAbridorDocumentos();
  const descargar = useDescargarArchivo();
  const [errorDescarga, setErrorDescarga] = useState<string | null>(null);

  const vigentes = ordenar((documentos.data ?? []).filter((d) => d.vigente));
  const grupos = agrupar((documentos.data ?? []).filter((d) => !d.vigente));
  const nombreEtapaActual = etapaActual !== undefined ? nombreEtapa(etapaActual) : undefined;

  return (
    <section aria-label="Documentos del pedido" className="flex flex-col gap-4">
      {!finalizado && (
        <div className="flex flex-col gap-3 rounded-lg border-2 border-grafico-serie bg-secondary p-4">
          <div className="flex flex-wrap items-center justify-between gap-2">
            <h2 className="flex items-center gap-2 font-heading text-lg font-semibold">
              <BadgeCheckIcon aria-hidden="true" className="size-5 text-grafico-serie" />
              Documentos vigentes para la etapa actual
            </h2>
            <Badge>Vigente{etapaActual !== undefined ? ` · ${codigoEtapa(tipoFlujo, etapaActual)}${nombreEtapaActual ? ` ${nombreEtapaActual}` : ''}` : ''}</Badge>
          </div>
         
          {documentos.data && vigentes.length === 0 && (
            <p className="text-sm text-muted-foreground">Aún no se entregaron documentos a esta etapa.</p>
          )}
          {vigentes.length > 0 && (
            <ul className="flex flex-col gap-2">
              {vigentes.map((documento) => (
                <FilaDocumento
                  key={documento.id}
                  documento={documento}
                  tipoFlujo={tipoFlujo}
                  ocupado={abridor.ocupado || descargar.isPending}
                  onVer={() => abridor.abrir(documento)}
                  onDescargar={() => {
                    setErrorDescarga(null);
                    descargar.mutate(documento, { onError: (fallo) => setErrorDescarga(mensajeApertura(fallo)) });
                  }}
                />
              ))}
            </ul>
          )}
        </div>
      )}
      <div className="flex flex-col gap-1">
        <h2 className="flex items-center gap-2 font-heading text-base font-semibold text-muted-foreground">
          <HistoryIcon aria-hidden="true" className="size-4" />
          Documentos anteriores e historial
        </h2>
        
      </div>
      {documentos.isPending && <Skeleton className="h-24 w-full" />}
      {documentos.isError && (
        <Alert variant="destructive">
          <AlertDescription>{mensajeDeErrorPedido(documentos.error, 'No se pudieron cargar los documentos.')}</AlertDescription>
        </Alert>
      )}
      {(abridor.error || errorDescarga) && (
        <Alert variant="destructive">
          <AlertDescription>{abridor.error ?? errorDescarga}</AlertDescription>
        </Alert>
      )}
      {documentos.data && grupos.length === 0 && (
        <p className="text-sm text-muted-foreground">No hay documentos anteriores.</p>
      )}
      {grupos.map(({ etapa, entradas }) => (
        <div key={etapa} className="flex flex-col gap-2 opacity-95">
          <h3 className="text-sm font-semibold">
            {codigoEtapa(tipoFlujo, etapa)}
            {nombreEtapa(etapa) ? ` · ${nombreEtapa(etapa)}` : ''}
          </h3>
          {entradas.map((docs, indice) => (
            <div key={docs[0].entradaId} className="flex flex-col gap-1.5 rounded-md border border-dashed p-3">
              {entradas.length > 1 && (
                <p className="text-xs text-muted-foreground">
                  Paso {entradas.length - indice} por la etapa
                  {indice === 0 ? ' (el más reciente)' : ''}
                  {entradas.length - indice > 1 ? ' · reentrada tras rechazo o devolución (corrección)' : ' · original'}
                  {' · '}
                  {formatearFechaHora(docs[0].entradaEn)}
                </p>
              )}
              <ul className="flex flex-col gap-2">
                {docs.map((documento) => (
                  <FilaDocumento
                    key={documento.id}
                    documento={documento}
                    tipoFlujo={tipoFlujo}
                    ocupado={abridor.ocupado || descargar.isPending}
                    onVer={() => abridor.abrir(documento)}
                    onDescargar={() => {
                      setErrorDescarga(null);
                      descargar.mutate(documento, { onError: (fallo) => setErrorDescarga(mensajeApertura(fallo)) });
                    }}
                  />
                ))}
              </ul>
            </div>
          ))}
        </div>
      ))}
      {abridor.elemento}
    </section>
  );
}

function FilaDocumento({
  documento,
  tipoFlujo,
  ocupado,
  onVer,
  onDescargar,
}: {
  documento: DocumentoPedido;
  tipoFlujo: string;
  ocupado: boolean;
  onVer: () => void;
  onDescargar: () => void;
}) {
  const disponible = documento.disponibilidad !== 'NO_DISPONIBLE';
  return (
    <li className="flex flex-wrap items-start justify-between gap-3">
      <div className="flex min-w-0 flex-col gap-1">
        <p className="truncate font-medium">{documento.nombre}</p>
        <p className="text-sm text-muted-foreground">
          {documento.tipoDocumento} · {documento.subidoPor.nombre} · {formatearFechaHora(documento.subidoEn)} ·{' '}
          {formatearTamano(documento.tamanoBytes)}
        </p>
        <div className="flex flex-wrap gap-1.5">
          {documento.presentaciones.length > 0 ? (
            documento.presentaciones.map((p) => (
              <Badge key={p.eventoId} variant="secondary">
                Presentado: {NOMBRE_EVENTO[p.tipo as keyof typeof NOMBRE_EVENTO] ?? p.tipo} {codigoEtapa(tipoFlujo, p.etapa)} ·{' '}
                {formatearFechaHora(p.fecha)}
              </Badge>
            ))
          ) : (
            <Badge variant="outline">Solo subido (no presentado)</Badge>
          )}
          <Badge variant="outline">{ESTADO_COPIA[documento.estadoCopia]}</Badge>
        </div>
        {!disponible && (
          <p className="text-sm text-destructive">
            No disponible
          </p>
        )}
        {disponible && !documento.puedeAbrir && (
          <p className="text-sm text-muted-foreground">Sin permiso para abrirlo en la etapa actual.</p>
        )}
      </div>
      {disponible && documento.puedeAbrir && (
        <div className="flex shrink-0 gap-2">
          {visible(documento) && (
            <Button variant="outline" size="sm" disabled={ocupado} onClick={onVer} aria-label={`Ver ${documento.nombre}`}>
              <EyeIcon data-icon="inline-start" />
              Ver
            </Button>
          )}
          <Button variant="outline" size="sm" disabled={ocupado} onClick={onDescargar} aria-label={`Descargar ${documento.nombre}`}>
            <DownloadIcon data-icon="inline-start" />
            Descargar
          </Button>
        </div>
      )}
    </li>
  );
}

/** Más reciente primero; desempate estable por ID (ADR-0029). */
function ordenar(documentos: DocumentoPedido[]) {
  return [...documentos].sort((a, b) => b.subidoEn.localeCompare(a.subidoEn) || b.id.localeCompare(a.id));
}

/**
 * Agrupa por etapa y, dentro, por entrada; entradas por su fecha real descendente y etapas por su entrada más reciente.
 */
function agrupar(documentos: DocumentoPedido[]) {
  const etapas = new Map<number, Map<string, DocumentoPedido[]>>();
  for (const documento of ordenar(documentos)) {
    const entradas = etapas.get(documento.etapa) ?? new Map<string, DocumentoPedido[]>();
    entradas.set(documento.entradaId, [...(entradas.get(documento.entradaId) ?? []), documento]);
    etapas.set(documento.etapa, entradas);
  }
  const porFecha = (a: DocumentoPedido[], b: DocumentoPedido[]) =>
    b[0].entradaEn.localeCompare(a[0].entradaEn) || b[0].entradaId.localeCompare(a[0].entradaId);
  return [...etapas.entries()]
    .map(([etapa, entradas]) => ({ etapa, entradas: [...entradas.values()].sort(porFecha) }))
    .sort((a, b) => porFecha(a.entradas[0], b.entradas[0]));
}

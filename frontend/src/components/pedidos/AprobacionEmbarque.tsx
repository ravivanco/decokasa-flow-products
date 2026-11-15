import { CircleAlertIcon, ColumnsIcon, DownloadIcon, EyeIcon } from 'lucide-react';
import { useEffect, useState } from 'react';
import { esErrorApi } from '@/api/cliente';
import { tipoVisible, useEmbarques, useMutacionesEmbarques, usePackingListContenido, type VistaEmbarques } from '@/api/embarques';
import { TIPOS_HOJA, TIPOS_VISIBLES, useDescargarArchivo, useDocumentosPedido, useVerArchivo, useVerHoja } from '@/api/pedidos';
import type { Hoja } from '@/lib/hojaCalculo';
import { VisorHoja } from './VisorHoja';
import { Alert, AlertDescription, AlertTitle } from '@/components/ui/alert';
import { Badge } from '@/components/ui/badge';
import { Button } from '@/components/ui/button';
import { Dialog, DialogContent, DialogDescription, DialogHeader, DialogTitle } from '@/components/ui/dialog';

// Revisión del envío: lo presentado, las versiones nuevas por revisar y la comparación con la solicitud. La comparación
// la hace la persona: el sistema no lee los archivos ni calcula diferencias.

export function AprobacionEmbarque({ pedidoId, vista }: { pedidoId: string; vista: VistaEmbarques }) {
  // Sin recuadro propio (pedido del cliente, 2026-11-15): solo la herramienta para comparar y, cuando hace falta, el
  // aviso de una versión nueva del Packing List, que se debe marcar como revisada antes de aprobar.
  const mutaciones = useMutacionesEmbarques(pedidoId);
  const [comparando, setComparando] = useState(false);
  const envio = vista.envios.find((candidato) => candidato.estado === 'PENDIENTE');
  if (!envio) return null;
  const nuevas = vista.bls.filter((bl) => bl.revisionPendiente);
  return (
    <div className="flex flex-col gap-3">
      <Button variant="outline" className="self-end" onClick={() => setComparando(true)}>
        <ColumnsIcon data-icon="inline-start" />
        Comparar solicitud y Packing Lists
      </Button>
      {nuevas.map((bl) => (
        <Alert key={bl.id}>
          <CircleAlertIcon />
          <AlertTitle>{bl.codigo}: hay una versión nueva del Packing List</AlertTitle>
          <AlertDescription className="flex flex-col items-start gap-2">
            <span>Revisa la v{bl.packingList?.version} antes de decidir.</span>
            {vista.puedeRevisar && (
              <Button size="sm" variant="outline" disabled={mutaciones.marcarRevisada.isPending} onClick={() => mutaciones.marcarRevisada.mutate(bl.id)}>
                Marcar la v{bl.packingList?.version} como revisada
              </Button>
            )}
          </AlertDescription>
        </Alert>
      ))}
      {comparando && <Comparar pedidoId={pedidoId} onCerrar={() => setComparando(false)} />}
    </div>
  );
}

interface Visible {
  titulo: string;
  url: string | null;
  tipo: string | null;
  mensaje?: string;
  hojas?: Hoja[];
}

function Comparar({ pedidoId, onCerrar }: { pedidoId: string; onCerrar: () => void }) {
  const documentos = useDocumentosPedido(pedidoId);
  const embarques = useEmbarques(pedidoId);
  const verHoja = useVerHoja();
  const ver = useVerArchivo();
  const descargar = useDescargarArchivo();
  const packingList = usePackingListContenido(pedidoId);
  const [izquierda, setIzquierda] = useState<Visible | null>(null);
  const [derecha, setDerecha] = useState<Visible | null>(null);
  // Un solo documento: la solicitud de la etapa 1 (la más reciente si se volvió a subir tras una devolución). Antes
  // entraban también los documentos de la etapa 2 (codificación en A, revisión de productos en B).
  const solicitud = (documentos.data ?? [])
    .filter((documento) => documento.etapa === 1 && documento.tipoDocumento === 'Solicitud')
    .sort((a, b) => b.subidoEn.localeCompare(a.subidoEn))
    .slice(0, 1);
  const envio = embarques.data?.envios.find((candidato) => candidato.estado === 'PENDIENTE');

  useEffect(() => () => {
    if (izquierda?.url) URL.revokeObjectURL(izquierda.url);
    if (derecha?.url) URL.revokeObjectURL(derecha.url);
  }, [izquierda?.url, derecha?.url]);

  function descargarPackingList(blId: string, packingListId: string, nombre: string) {
    packingList.mutate({ blId, packingListId, nombre }, {
      onSuccess: ({ url }) => {
        const enlace = document.createElement('a');
        enlace.href = url;
        enlace.download = nombre;
        enlace.click();
        setTimeout(() => URL.revokeObjectURL(url), 1000);
      },
      onError: (fallo) => setDerecha({ titulo: nombre, url: null, tipo: null,
        mensaje: esErrorApi(fallo) && fallo.status === 403 ? 'No tienes permiso para descargar este archivo.' : 'No se pudo descargar el archivo.' }),
    });
  }

  function verPackingList(blId: string, packingListId: string, nombre: string) {
    packingList.mutate({ blId, packingListId, nombre }, {
      onSuccess: async ({ url, tipo }) => {
        // Excel y CSV: se leen aquí mismo y se muestran como tabla.
        if (esHojaPorNombre(nombre)) {
          const bytes = await (await fetch(url)).arrayBuffer();
          URL.revokeObjectURL(url);
          const { leerCsv, leerXlsx } = await import('@/lib/hojaCalculo');
          const hojas = nombre.toLowerCase().endsWith('.csv') ? leerCsv(new TextDecoder().decode(bytes)) : await leerXlsx(bytes);
          setDerecha({ titulo: nombre, url: null, tipo: 'hoja', hojas });
          return;
        }
        if (tipo) { setDerecha({ titulo: nombre, url, tipo }); return; }
        const enlace = document.createElement('a');
        enlace.href = url;
        enlace.download = nombre;
        enlace.click();
        URL.revokeObjectURL(url);
        setDerecha({ titulo: nombre, url: null, tipo: null, mensaje: 'Este formato se descarga para abrirlo en su programa.' });
      },
      onError: (fallo) => setDerecha({ titulo: nombre, url: null, tipo: null,
        mensaje: esErrorApi(fallo) && fallo.status === 403 ? 'No tienes permiso para abrir este archivo.' : 'No se pudo abrir el archivo.' }),
    });
  }

  return (
    <Dialog open onOpenChange={(abierto) => !abierto && onCerrar()}>
      <DialogContent className="flex h-[90svh] flex-col sm:max-w-6xl">
        <DialogHeader>
          <DialogTitle>Comparar solicitud y Packing Lists</DialogTitle>
          <DialogDescription>La comparación la hace la persona: DECOKASA no lee estos archivos.</DialogDescription>
        </DialogHeader>
        <div className="grid min-h-0 flex-1 gap-4 md:grid-cols-2">
          <div className="flex min-h-0 flex-col gap-2">
            <h3 className="font-heading font-semibold">Documentos de solicitud</h3>
            <ul className="flex flex-col gap-1">
              {solicitud.map((documento) => {
                const hoja = TIPOS_HOJA.some((tipo) => tipo === documento.tipoContenido);
                const visible = hoja || TIPOS_VISIBLES.some((tipo) => tipo === documento.tipoContenido);
                return (
                  <li key={documento.id} className="flex items-center justify-between gap-2 text-sm">
                    <span className="truncate">{documento.nombre}<Badge variant="outline" className="ml-2">{documento.tipoDocumento}</Badge></span>
                    <span className="flex shrink-0 gap-1">
                      {visible && (
                        <Button size="sm" variant="outline" aria-label={`Ver ${documento.nombre}`} disabled={ver.isPending || verHoja.isPending} onClick={() => {
                          if (hoja) verHoja.mutate(documento, { onSuccess: (hojas) => setIzquierda({ titulo: documento.nombre, url: null, tipo: 'hoja', hojas }) });
                          else ver.mutate(documento, { onSuccess: ({ url, tipo }) => setIzquierda({ titulo: documento.nombre, url, tipo }) });
                        }}>
                          <EyeIcon data-icon="inline-start" />
                          Ver
                        </Button>
                      )}
                      <Button size="sm" variant="outline" aria-label={`Descargar ${documento.nombre}`} disabled={descargar.isPending} onClick={() => descargar.mutate(documento)}>
                        <DownloadIcon data-icon="inline-start" />
                        Descargar
                      </Button>
                    </span>
                  </li>
                );
              })}
              {documentos.data && solicitud.length === 0 && <li className="text-sm text-muted-foreground">Sin documentos de solicitud.</li>}
            </ul>
            <Panel visible={izquierda} />
          </div>
          <div className="flex min-h-0 flex-col gap-2">
            <h3 className="font-heading font-semibold">Packing Lists presentados</h3>
            <ul className="flex flex-col gap-1">
              {(envio?.filas ?? []).flatMap((fila) => {
                const bl = embarques.data?.bls.find((candidato) => candidato.id === fila.blId);
                const nueva = bl?.packingList && bl.packingList.id !== fila.packingListId ? bl.packingList : null;
                return [{ id: fila.packingListId, nombre: fila.nombreArchivo, version: fila.version, blId: fila.blId, codigo: fila.codigo },
                  ...(nueva ? [{ id: nueva.id, nombre: nueva.nombre, version: nueva.version, blId: fila.blId, codigo: fila.codigo }] : [])];
              }).map((archivo) => (
                <li key={archivo.id} className="flex items-center justify-between gap-2 text-sm">
                  <span className="truncate">{archivo.codigo} · v{archivo.version} · {archivo.nombre}</span>
                  <span className="flex shrink-0 gap-1">
                    {(tipoVisible(archivo.nombre) || esHojaPorNombre(archivo.nombre)) && (
                      <Button size="sm" variant="outline" aria-label={`Ver ${archivo.nombre}`} disabled={packingList.isPending} onClick={() => verPackingList(archivo.blId, archivo.id, archivo.nombre)}>
                        <EyeIcon data-icon="inline-start" />
                        Ver
                      </Button>
                    )}
                    <Button size="sm" variant="outline" aria-label={`Descargar ${archivo.nombre}`} disabled={packingList.isPending} onClick={() => descargarPackingList(archivo.blId, archivo.id, archivo.nombre)}>
                      <DownloadIcon data-icon="inline-start" />
                      Descargar
                    </Button>
                  </span>
                </li>
              ))}
            </ul>
            <Panel visible={derecha} />
          </div>
        </div>
      </DialogContent>
    </Dialog>
  );
}

function Panel({ visible }: { visible: Visible | null }) {
  if (!visible) return <p className="flex flex-1 items-center justify-center rounded-md border border-dashed text-sm text-muted-foreground">Elige un documento.</p>;
  if (visible.hojas) return <VisorHoja hojas={visible.hojas} />;
  if (visible.mensaje || !visible.url) return <p className="rounded-md border p-3 text-sm">{visible.titulo}: {visible.mensaje}</p>;
  return visible.tipo === 'application/pdf'
    ? <iframe title={`Documento ${visible.titulo}`} src={visible.url} className="min-h-0 w-full flex-1 rounded-md border" />
    : <img src={visible.url} alt={visible.titulo} className="min-h-0 flex-1 rounded-md border object-contain" />;
}

/** Excel moderno o CSV por la extensión del nombre (el servidor ya validó el contenido al subirlo). */
function esHojaPorNombre(nombre: string): boolean {
  const extension = nombre.toLowerCase().split('.').pop();
  return extension === 'xlsx' || extension === 'csv';
}

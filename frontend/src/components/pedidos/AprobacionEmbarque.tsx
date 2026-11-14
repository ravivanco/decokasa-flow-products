import { CircleAlertIcon, ColumnsIcon, DownloadIcon, EyeIcon } from 'lucide-react';
import { useEffect, useState } from 'react';
import { esErrorApi } from '@/api/cliente';
import { tipoVisible, useEmbarques, useMutacionesEmbarques, usePackingListContenido, type VistaEmbarques } from '@/api/embarques';
import { TIPOS_VISIBLES, useDescargarArchivo, useDocumentosPedido, useVerArchivo } from '@/api/pedidos';
import { Alert, AlertDescription, AlertTitle } from '@/components/ui/alert';
import { Badge } from '@/components/ui/badge';
import { Button } from '@/components/ui/button';
import { Dialog, DialogContent, DialogDescription, DialogHeader, DialogTitle } from '@/components/ui/dialog';
import { formatearFecha } from './formato';

// Revisión del envío: lo presentado, las versiones nuevas por revisar y la comparación con la solicitud. La comparación
// la hace la persona: el sistema no lee los archivos ni calcula diferencias.

export function AprobacionEmbarque({ pedidoId, vista }: { pedidoId: string; vista: VistaEmbarques }) {
  const mutaciones = useMutacionesEmbarques(pedidoId);
  const [comparando, setComparando] = useState(false);
  const envio = vista.envios.find((candidato) => candidato.estado === 'PENDIENTE');
  if (!envio) return null;
  const nuevas = vista.bls.filter((bl) => bl.revisionPendiente);
  return (
    <section aria-labelledby="aprobacion-titulo" className="flex flex-col gap-3 rounded-lg border bg-card p-4 shadow-sm">
      <div className="flex flex-wrap items-center justify-between gap-3">
        <div>
          <h2 id="aprobacion-titulo" className="font-heading text-lg font-semibold">Aprobación de embarque · envío {envio.numero}</h2>
          <p className="text-sm text-muted-foreground">Lo presentado queda registrado aunque después cambie un archivo.</p>
        </div>
        <Button variant="outline" onClick={() => setComparando(true)}>
          <ColumnsIcon data-icon="inline-start" />
          Comparar solicitud y Packing Lists
        </Button>
      </div>
      {nuevas.map((bl) => (
        <Alert key={bl.id}>
          <CircleAlertIcon />
          <AlertTitle>{bl.codigo}: hay una versión nueva del Packing List</AlertTitle>
          <AlertDescription className="flex flex-col items-start gap-2">
            <span>Se presentó otra versión. Revisa la v{bl.packingList?.version} antes de decidir.</span>
            {vista.puedeRevisar && (
              <Button size="sm" variant="outline" disabled={mutaciones.marcarRevisada.isPending} onClick={() => mutaciones.marcarRevisada.mutate(bl.id)}>
                Marcar la v{bl.packingList?.version} como revisada
              </Button>
            )}
          </AlertDescription>
        </Alert>
      ))}
      <div className="overflow-x-auto">
        <table className="w-full min-w-[34rem] text-sm">
          <caption className="sr-only">BL presentados en el envío {envio.numero}</caption>
          <thead><tr className="border-b text-left text-muted-foreground">
            <th scope="col" className="py-1.5 pr-3 font-medium">BL</th>
            <th scope="col" className="py-1.5 pr-3 font-medium">Versión</th>
            <th scope="col" className="py-1.5 pr-3 font-medium">Salida</th>
            <th scope="col" className="py-1.5 pr-3 font-medium">Llegada</th>
            <th scope="col" className="py-1.5 font-medium">Pedidos vinculados</th>
          </tr></thead>
          <tbody>
            {envio.filas.map((fila) => (
              <tr key={fila.blId} className="border-b last:border-0">
                <td className="py-1.5 pr-3 font-medium">{fila.codigo}</td>
                <td className="py-1.5 pr-3 tabular-nums">v{fila.version}</td>
                <td className="py-1.5 pr-3 tabular-nums">{formatearFecha(fila.fechaSalida)}</td>
                <td className="py-1.5 pr-3 tabular-nums">{fila.llegadaEstimada ? formatearFecha(fila.llegadaEstimada) : 'Por definir'}</td>
                <td className="py-1.5">{fila.pedidos.map((pedido) => pedido.numero).join(', ')}</td>
              </tr>
            ))}
          </tbody>
        </table>
      </div>
      {comparando && <Comparar pedidoId={pedidoId} onCerrar={() => setComparando(false)} />}
    </section>
  );
}

interface Visible {
  titulo: string;
  url: string | null;
  tipo: string | null;
  mensaje?: string;
}

function Comparar({ pedidoId, onCerrar }: { pedidoId: string; onCerrar: () => void }) {
  const documentos = useDocumentosPedido(pedidoId);
  const embarques = useEmbarques(pedidoId);
  const ver = useVerArchivo();
  const descargar = useDescargarArchivo();
  const packingList = usePackingListContenido(pedidoId);
  const [izquierda, setIzquierda] = useState<Visible | null>(null);
  const [derecha, setDerecha] = useState<Visible | null>(null);
  const solicitud = (documentos.data ?? []).filter((documento) => documento.etapa <= 2);
  const envio = embarques.data?.envios.find((candidato) => candidato.estado === 'PENDIENTE');

  useEffect(() => () => {
    if (izquierda?.url) URL.revokeObjectURL(izquierda.url);
    if (derecha?.url) URL.revokeObjectURL(derecha.url);
  }, [izquierda?.url, derecha?.url]);

  function verPackingList(blId: string, packingListId: string, nombre: string) {
    packingList.mutate({ blId, packingListId, nombre }, {
      onSuccess: ({ url, tipo }) => {
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
                const visible = TIPOS_VISIBLES.some((tipo) => tipo === documento.tipoContenido);
                return (
                  <li key={documento.id} className="flex items-center justify-between gap-2 text-sm">
                    <span className="truncate">{documento.nombre}<Badge variant="outline" className="ml-2">{documento.tipoDocumento}</Badge></span>
                    <Button size="sm" variant="outline" disabled={ver.isPending || descargar.isPending} onClick={() => {
                      if (visible) ver.mutate(documento, { onSuccess: ({ url, tipo }) => setIzquierda({ titulo: documento.nombre, url, tipo }) });
                      else descargar.mutate(documento);
                    }}>
                      {visible ? <EyeIcon data-icon="inline-start" /> : <DownloadIcon data-icon="inline-start" />}
                      {visible ? 'Ver' : 'Descargar'}
                    </Button>
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
                  <Button size="sm" variant="outline" disabled={packingList.isPending} onClick={() => verPackingList(archivo.blId, archivo.id, archivo.nombre)}>
                    {tipoVisible(archivo.nombre) ? <EyeIcon data-icon="inline-start" /> : <DownloadIcon data-icon="inline-start" />}
                    {tipoVisible(archivo.nombre) ? 'Ver' : 'Descargar'}
                  </Button>
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
  if (visible.mensaje || !visible.url) return <p className="rounded-md border p-3 text-sm">{visible.titulo}: {visible.mensaje}</p>;
  return visible.tipo === 'application/pdf'
    ? <iframe title={`Documento ${visible.titulo}`} src={visible.url} className="min-h-0 w-full flex-1 rounded-md border" />
    : <img src={visible.url} alt={visible.titulo} className="min-h-0 flex-1 rounded-md border object-contain" />;
}

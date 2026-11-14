import { CircleAlertIcon, LinkIcon, LoaderIcon, PlusIcon, RotateCwIcon, UploadIcon } from 'lucide-react';
import { type FormEvent, useEffect, useRef, useState } from 'react';
import { Link } from 'react-router';
import { toast } from 'sonner';
import { useCargaArchivo } from '@/api/cargas';
import { esErrorApi } from '@/api/cliente';
import { type Bl, IMPORTADORAS, nombreImportadora, useAlertasSimuladas, useEmbarques, useMutacionesEmbarques, type VistaEmbarques } from '@/api/embarques';
import type { EtapaEnLinea, PedidoDetalle } from '@/api/pedidos';
import { Alert, AlertDescription } from '@/components/ui/alert';
import { Badge } from '@/components/ui/badge';
import { Button } from '@/components/ui/button';
import { Dialog, DialogContent, DialogDescription, DialogFooter, DialogHeader, DialogTitle } from '@/components/ui/dialog';
import { Field, FieldError, FieldLabel } from '@/components/ui/field';
import { Input } from '@/components/ui/input';
import { AprobacionEmbarque } from './AprobacionEmbarque';
import { formatearFecha, formatearFechaHora } from './formato';

// Sección de embarques del pedido: resumen, BL, envíos y alertas simuladas. Cada BL se guarda por separado. Primero se
// guarda el BL y después su Packing List, con la carga reanudable: un fallo al finalizar deja el BL guardado y se
// reintenta con el mismo archivo sin duplicar el adjunto.

const TIPO_PACKING_LIST = 'Packing List';
export const EXTENSIONES_ACEPTADAS = ['.pdf', '.xlsx', '.xls', '.csv', '.jpg', '.jpeg', '.png'];

export function mensajeDeCarga(fallo: unknown): string {
  if (fallo instanceof DOMException && fallo.name === 'AbortError') return 'La carga se pausó. Reinténtala con el mismo archivo.';
  if (esErrorApi(fallo) && fallo.status > 0 && fallo.message) return fallo.message;
  return 'No se confirmó el archivo. Reinténtalo: no se duplica.';
}

function mensaje(fallo: unknown): string {
  return esErrorApi(fallo) ? fallo.message : 'No se pudo completar la acción. Intenta de nuevo.';
}

interface Borrador {
  clave: number;
  codigo: string;
  salida: string;
  llegada: string;
  archivo: File | null;
  importadora: string;
  error: string | null;
  duplicado: boolean;
}

function SelectorImportadora({ id, valor, editable, onCambiar }: {
  id: string; valor: string; editable: boolean; onCambiar: (valor: string) => void;
}) {
  return (
    <Field>
      <FieldLabel htmlFor={id}>Importadora</FieldLabel>
      <select
        id={id}
        value={valor}
        disabled={!editable}
        onChange={(evento) => onCambiar(evento.target.value)}
        className="h-10 w-full rounded-md border border-input bg-card px-3 text-sm outline-none focus-visible:ring-3 focus-visible:ring-ring disabled:opacity-60"
      >
        <option value="">Elige la importadora</option>
        {IMPORTADORAS.map((importadora) => <option key={importadora.valor} value={importadora.valor}>{importadora.nombre}</option>)}
      </select>
    </Field>
  );
}

interface Props {
  pedido: PedidoDetalle;
  etapaActual: EtapaEnLinea | undefined;
}

export function SeccionEmbarques({ pedido, etapaActual }: Props) {
  const embarques = useEmbarques(pedido.id);
  const vista = embarques.data;
  if (!vista || (!vista.gestionaEmbarques && vista.bls.length === 0)) return null;
  return (
    <section aria-label="Embarques del pedido" className="flex flex-col gap-4">
      <Resumen pedido={pedido} vista={vista} />
      {vista.apruebaEmbarques && <AprobacionEmbarque pedidoId={pedido.id} vista={vista} />}
      <Bloques pedido={pedido} etapa={etapaActual} vista={vista} />
      <HistorialEnvios vista={vista} />
      <AlertasSimuladas pedidoId={pedido.id} />
    </section>
  );
}

function estadoDelPedido(vista: VistaEmbarques, pedido: PedidoDetalle) {
  const ultimo = vista.envios[0];
  if (ultimo?.estado === 'APROBADO') return { texto: 'Embarque aprobado', clase: 'bg-estado-ok-fondo text-estado-ok' };
  if (ultimo?.estado === 'PENDIENTE') return { texto: 'En aprobación', clase: 'bg-secondary text-primary' };
  if (ultimo?.estado === 'RECHAZADO') return { texto: 'Devuelto a Compras China', clase: 'bg-estado-vencido-fondo text-estado-vencido' };
  return { texto: vista.gestionaEmbarques && pedido.versionFlujo > 1 ? 'Mercancía pendiente de asignar' : 'BL vinculados', clase: 'bg-estado-aviso-fondo text-estado-aviso' };
}

function estadoEmbarque(bl: Bl, hoy: string) {
  if (bl.estado === 'SIN_PACKING_LIST') return 'Sin Packing List';
  if (bl.estado === 'SIN_SALIDA') return 'Sin fecha de salida';
  return bl.fechaSalida && bl.fechaSalida > hoy ? 'Programado' : 'En tránsito';
}

function Resumen({ pedido, vista }: { pedido: PedidoDetalle; vista: VistaEmbarques }) {
  const estado = estadoDelPedido(vista, pedido);
  const hoy = (pedido.servidorAhora ?? new Date().toISOString()).slice(0, 10);
  const rechazo = vista.envios[0]?.estado === 'RECHAZADO' ? vista.envios[0].decisiones.at(-1) : undefined;
  return (
    <div className="flex flex-col gap-3 rounded-lg border bg-card p-4 shadow-sm">
      <div className="flex flex-wrap items-center justify-between gap-3">
        <div className="flex flex-col gap-1">
          <h2 className="font-heading text-lg font-semibold">Embarques de este pedido</h2>
          <span className={`w-fit rounded-md px-3 py-1 text-sm font-semibold ${estado.clase}`}>{estado.texto}</span>
        </div>
        <p className="text-sm text-muted-foreground">BL vinculados: <b className="text-foreground tabular-nums">{vista.bls.length}</b></p>
      </div>
      {!vista.gestionaEmbarques && (
        <p className="text-sm text-muted-foreground">Pedido en una versión anterior del flujo: sus reglas no cambian. Los BL se muestran como información.</p>
      )}
      {rechazo && (
        <Alert variant="destructive">
          <CircleAlertIcon />
          <AlertDescription>
            <b>Rechazado por {rechazo.usuario}:</b> {rechazo.motivo}
            {rechazo.blSenalados.length > 0 && ` BL señalados: ${rechazo.blSenalados.join(', ')}.`}
          </AlertDescription>
        </Alert>
      )}
      {vista.bls.length > 0 && (
        <div className="overflow-x-auto">
          <table className="w-full min-w-[36rem] text-sm">
            <caption className="sr-only">Resumen de los BL del pedido</caption>
            <thead><tr className="border-b text-left text-muted-foreground">
              <th scope="col" className="py-1.5 pr-3 font-medium">BL</th>
              <th scope="col" className="py-1.5 pr-3 font-medium">Importadora</th>
              <th scope="col" className="py-1.5 pr-3 font-medium">Estado del embarque</th>
              <th scope="col" className="py-1.5 pr-3 font-medium">Salida</th>
              <th scope="col" className="py-1.5 pr-3 font-medium">Llegada estimada</th>
              <th scope="col" className="py-1.5 font-medium">Pedidos</th>
            </tr></thead>
            <tbody>
              {vista.bls.map((bl) => (
                <tr key={bl.id} className="border-b last:border-0">
                  <td className="py-1.5 pr-3 font-medium">{bl.codigo}</td>
                  <td className="py-1.5 pr-3">{nombreImportadora(bl.importadora)}</td>
                  <td className="py-1.5 pr-3"><Badge variant="outline">{estadoEmbarque(bl, hoy)}</Badge></td>
                  <td className="py-1.5 pr-3 tabular-nums">{bl.fechaSalida ? formatearFecha(bl.fechaSalida) : '—'}</td>
                  <td className="py-1.5 pr-3 tabular-nums">{bl.llegadaEstimada ? formatearFecha(bl.llegadaEstimada) : 'Llegada por definir'}</td>
                  <td className="py-1.5">{bl.pedidos.length > 1 ? `Compartido con ${bl.pedidos.length - 1} pedido(s) más` : 'Solo este pedido'}</td>
                </tr>
              ))}
            </tbody>
          </table>
        </div>
      )}
    </div>
  );
}

function Bloques({ pedido, etapa, vista }: { pedido: PedidoDetalle; etapa: EtapaEnLinea | undefined; vista: VistaEmbarques }) {
  const mutaciones = useMutacionesEmbarques(pedido.id);
  const [cantidad, setCantidad] = useState(1);
  const [borradores, setBorradores] = useState<Borrador[]>([]);
  const secuencia = useRef(1);
  // Archivo elegido al crear el BL: su bloque guardado empieza la carga solo.
  const [iniciales, setIniciales] = useState<Record<string, File>>({});

  const nuevo = (): Borrador => ({ clave: secuencia.current++, codigo: '', salida: '', llegada: '', archivo: null, importadora: '', error: null, duplicado: false });
  const cambiar = (clave: number, cambios: Partial<Borrador>) =>
    setBorradores((actuales) => actuales.map((b) => (b.clave === clave ? { ...b, ...cambios } : b)));

  async function guardar(borrador: Borrador) {
    if (borrador.codigo.trim().length < 3) { cambiar(borrador.clave, { error: 'Escribe el código del BL.' }); return; }
    try {
      const resultado = await mutaciones.crear.mutateAsync({ codigo: borrador.codigo, fechaSalida: borrador.salida || null,
        llegadaEstimada: borrador.llegada || null, importadora: borrador.importadora || null });
      const creado = resultado.bls.find((bl) => bl.codigo === borrador.codigo.trim());
      if (creado && borrador.archivo) setIniciales((actuales) => ({ ...actuales, [creado.id]: borrador.archivo as File }));
      setBorradores((actuales) => actuales.filter((b) => b.clave !== borrador.clave));
      toast.success(`BL ${borrador.codigo.trim()} guardado.`);
    } catch (fallo) {
      cambiar(borrador.clave, { error: mensaje(fallo), duplicado: esErrorApi(fallo, 'DUPLICADO') });
    }
  }

  async function vincularExistente(borrador: Borrador) {
    try {
      await mutaciones.vincular.mutateAsync(borrador.codigo);
      setBorradores((actuales) => actuales.filter((b) => b.clave !== borrador.clave));
      toast.success(`BL ${borrador.codigo.trim()} vinculado a este pedido.`);
    } catch (fallo) {
      cambiar(borrador.clave, { error: mensaje(fallo), duplicado: false });
    }
  }

  return (
    <div className="flex flex-col gap-3 rounded-lg border bg-card p-4 shadow-sm">
      {vista.puedeEditar && (
        <div className="flex flex-wrap items-end gap-3">
          <Field className="w-44">
            <FieldLabel htmlFor="cantidad-bl">¿Cuántos BL vas a crear?</FieldLabel>
            <Input id="cantidad-bl" type="number" min={1} max={20} value={cantidad} onChange={(evento) => setCantidad(Number(evento.target.value))} />
          </Field>
          <Button variant="outline" onClick={() => setBorradores(Array.from({ length: Math.min(Math.max(cantidad, 1), 20) }, nuevo))}>Crear bloques</Button>
          <Button variant="ghost" onClick={() => setBorradores((actuales) => [...actuales, nuevo()])}>
            <PlusIcon data-icon="inline-start" />
            Añadir un bloque
          </Button>
        </div>
      )}
      <div className="grid gap-3 [grid-template-columns:repeat(auto-fit,minmax(min(100%,22rem),1fr))]">
        {vista.bls.map((bl) => (
          <BloqueBl key={bl.id} pedido={pedido} etapa={etapa} vista={vista} bl={bl} inicial={iniciales[bl.id] ?? null} />
        ))}
        {borradores.map((borrador, indice) => (
          <article key={borrador.clave} aria-label={`BL nuevo ${indice + 1}`} className="flex flex-col gap-3 rounded-md border border-dashed p-3">
            <div className="flex items-center justify-between gap-2">
              <h3 className="font-heading font-semibold">BL nuevo {indice + 1}</h3>
              <Badge variant="outline">Sin guardar</Badge>
            </div>
            <Field data-invalid={borrador.error ? true : undefined}>
              <FieldLabel htmlFor={`codigo-${borrador.clave}`}>Código del BL</FieldLabel>
              <Input id={`codigo-${borrador.clave}`} value={borrador.codigo} aria-invalid={borrador.error ? true : undefined}
                onChange={(evento) => cambiar(borrador.clave, { codigo: evento.target.value, error: null, duplicado: false })} />
              <FieldError>{borrador.error}</FieldError>
            </Field>
            {borrador.duplicado && (
              <Button variant="outline" size="sm" onClick={() => void vincularExistente(borrador)}>
                <LinkIcon data-icon="inline-start" />
                Vincular {borrador.codigo.trim()} a este pedido
              </Button>
            )}
            <SelectorImportadora id={`importadora-${borrador.clave}`} valor={borrador.importadora} editable
              onCambiar={(valor) => cambiar(borrador.clave, { importadora: valor })} />
            <Field>
              <FieldLabel htmlFor={`archivo-${borrador.clave}`}>Packing List</FieldLabel>
              <Input id={`archivo-${borrador.clave}`} type="file" accept={EXTENSIONES_ACEPTADAS.join(',')}
                onChange={(evento) => cambiar(borrador.clave, { archivo: evento.target.files?.[0] ?? null })} />
            </Field>
            <FechasBl idBase={`${borrador.clave}`} salida={borrador.salida} llegada={borrador.llegada} transito={vista.transitoDias}
              editable onSalida={(valor) => cambiar(borrador.clave, { salida: valor })} onLlegada={(valor) => cambiar(borrador.clave, { llegada: valor })} />
            <div className="flex justify-between gap-2">
              <Button variant="ghost" size="sm" onClick={() => setBorradores((actuales) => actuales.filter((b) => b.clave !== borrador.clave))}>Quitar bloque</Button>
              <Button disabled={mutaciones.crear.isPending} onClick={() => void guardar(borrador)}>Guardar BL</Button>
            </div>
          </article>
        ))}
      </div>
    </div>
  );
}

function FechasBl({ idBase, salida, llegada, transito, editable, onSalida, onLlegada }: {
  idBase: string; salida: string; llegada: string; transito: number | null; editable: boolean;
  onSalida: (valor: string) => void; onLlegada: (valor: string) => void;
}) {
  return (
    <div className="grid gap-2 sm:grid-cols-2">
      <Field>
        <FieldLabel htmlFor={`salida-${idBase}`}>Fecha de salida</FieldLabel>
        <Input id={`salida-${idBase}`} type="date" value={salida} disabled={!editable} onChange={(evento) => onSalida(evento.target.value)} />
      </Field>
      <Field>
        <FieldLabel htmlFor={`llegada-${idBase}`}>Llegada estimada</FieldLabel>
        <Input id={`llegada-${idBase}`} type="date" value={llegada} disabled={!editable} onChange={(evento) => onLlegada(evento.target.value)} />
        <p className="text-xs text-muted-foreground">{transito ? `Vacía: salida + ${transito} días.` : 'Vacía: llegada por definir.'}</p>
      </Field>
    </div>
  );
}

function BloqueBl({ pedido, etapa, vista, bl, inicial }: {
  pedido: PedidoDetalle; etapa: EtapaEnLinea | undefined; vista: VistaEmbarques; bl: Bl; inicial: File | null;
}) {
  const mutaciones = useMutacionesEmbarques(pedido.id);
  const [salida, setSalida] = useState(bl.fechaSalida ?? '');
  const [llegada, setLlegada] = useState(bl.llegadaOrigen === 'MANUAL' ? bl.llegadaEstimada ?? '' : '');
  const [importadora, setImportadora] = useState(bl.importadora ?? '');
  const [anadiendo, setAnadiendo] = useState(false);
  const [error, setError] = useState<string | null>(null);
  const editable = vista.puedeEditar && !bl.bloqueado;
  const otros = bl.enAprobacionEn.filter((numero) => numero !== pedido.numero);

  function guardar() {
    setError(null);
    mutaciones.actualizar.mutate(
      { blId: bl.id, version: bl.version, fechaSalida: salida || null, llegadaEstimada: llegada || null,
        llegadaOrigen: llegada ? 'MANUAL' : 'AUTOMATICA', importadora: importadora || null },
      { onSuccess: () => toast.success(`BL ${bl.codigo} actualizado.`), onError: (fallo) => setError(mensaje(fallo)) },
    );
  }

  return (
    <article aria-label={`BL ${bl.codigo}`} className="flex flex-col gap-3 rounded-md border border-primary/40 p-3">
      <div className="flex flex-wrap items-center justify-between gap-2">
        <h3 className="font-heading font-semibold">{bl.codigo}</h3>
        <Badge variant={bl.estado === 'LISTO' ? 'secondary' : 'outline'}>
          {bl.estado === 'LISTO' ? 'Listo' : bl.estado === 'SIN_SALIDA' ? 'Sin fecha de salida' : 'Sin Packing List'}
        </Badge>
      </div>
      {bl.bloqueado && (
        <p className="text-xs text-muted-foreground">Ya hay una aprobación sobre este BL: se conserva la v{bl.aprobadoEnVersion} y su sustitución queda bloqueada hasta definir la regla de corrección.</p>
      )}
      {otros.length > 0 && !bl.bloqueado && (
        <p className="text-xs text-muted-foreground">En revisión en {otros.join(', ')}. Si cambias el Packing List, se pedirá revisar la versión nueva.</p>
      )}
      <SelectorImportadora id={`importadora-${bl.id}`} valor={importadora} editable={editable} onCambiar={setImportadora} />
      <PackingListDelBl pedido={pedido} etapa={etapa} bl={bl} editable={editable} inicial={inicial} />
      <FechasBl idBase={bl.id} salida={salida} llegada={llegada || (bl.llegadaEstimada ?? '')} transito={vista.transitoDias} editable={editable}
        onSalida={setSalida} onLlegada={setLlegada} />
      <div>
        <p className="text-xs text-muted-foreground">Pedidos en este BL</p>
        <ul className="flex flex-wrap gap-1">
          {bl.pedidos.map((vinculado) => (
            <li key={vinculado.id}>
              {vinculado.id === pedido.id
                ? <Badge>{vinculado.numero} (este)</Badge>
                : <Badge variant="outline" asChild><Link to={`/pedidos/${vinculado.id}`}>{vinculado.numero}</Link></Badge>}
            </li>
          ))}
        </ul>
      </div>
      {error && <p role="alert" className="text-sm text-estado-vencido">{error}</p>}
      {vista.puedeEditar && (
        <div className="flex flex-wrap justify-between gap-2">
          <div className="flex flex-wrap gap-2">
            <Button variant="outline" size="sm" onClick={() => setAnadiendo(true)}>Añadir pedido a este BL</Button>
            <Button variant="ghost" size="sm" onClick={() => mutaciones.desvincular.mutate(bl.id, { onError: (fallo) => setError(mensaje(fallo)) })}>Desvincular</Button>
          </div>
          {editable && <Button size="sm" disabled={mutaciones.actualizar.isPending} onClick={guardar}>Guardar BL</Button>}
        </div>
      )}
      {anadiendo && <DialogoAnadirPedido pedidoId={pedido.id} bl={bl} onCerrar={() => setAnadiendo(false)} />}
    </article>
  );
}

function PackingListDelBl({ pedido, etapa, bl, editable, inicial }: {
  pedido: PedidoDetalle; etapa: EtapaEnLinea | undefined; bl: Bl; editable: boolean; inicial: File | null;
}) {
  const subir = useCargaArchivo();
  const mutaciones = useMutacionesEmbarques(pedido.id);
  const entrada = useRef<HTMLInputElement>(null);
  const control = useRef<AbortController | null>(null);
  const iniciado = useRef(false);
  const [archivo, setArchivo] = useState<File | null>(null);
  const [adjuntoPorLigar, setAdjuntoPorLigar] = useState<string | null>(null);
  const [error, setError] = useState<string | null>(null);
  const [progreso, setProgreso] = useState(0);
  useEffect(() => () => control.current?.abort(), []);

  function ligar(adjuntoId: string) {
    mutaciones.registrarPackingList.mutate({ blId: bl.id, adjuntoId }, {
      onSuccess: () => { setAdjuntoPorLigar(null); setArchivo(null); toast.success(`Packing List de ${bl.codigo} confirmado por el servidor.`); },
      onError: (fallo) => { setAdjuntoPorLigar(adjuntoId); setError(mensaje(fallo)); },
    });
  }

  function cargar(elegido: File) {
    if (!etapa?.entradaId) return;
    setError(null);
    setArchivo(elegido);
    setProgreso(0);
    control.current = new AbortController();
    subir.mutate(
      { pedidoId: pedido.id, etapa: etapa.numero, entradaEsperadaId: etapa.entradaId, tipoDocumento: TIPO_PACKING_LIST,
        archivo: elegido, signal: control.current.signal,
        onProgreso: ({ confirmados, total }) => setProgreso(Math.round((confirmados / total) * 100)) },
      { onSuccess: (adjunto) => ligar(adjunto.id), onError: (fallo) => setError(mensajeDeCarga(fallo)) },
    );
  }

  useEffect(() => {
    if (!inicial || iniciado.current || !editable) return undefined;
    // En un turno aparte: el doble montaje de desarrollo cancelaría una carga ya empezada.
    const turno = setTimeout(() => { iniciado.current = true; cargar(inicial); }, 0);
    return () => clearTimeout(turno);
    // Solo al recibir el archivo elegido antes de guardar el BL.
    // eslint-disable-next-line react-hooks/exhaustive-deps
  }, [inicial]);

  const ocupado = subir.isPending || mutaciones.registrarPackingList.isPending;
  return (
    <div className="flex flex-col gap-1 rounded-md bg-muted/40 p-2">
      <p className="text-xs text-muted-foreground">Packing List</p>
      {bl.packingList ? (
        <p className="text-sm font-medium">{bl.packingList.nombre} · v{bl.packingList.version}</p>
      ) : !ocupado && !error && <p className="text-sm text-muted-foreground">Sin archivo confirmado.</p>}
      {ocupado && (
        <p role="status" className="flex items-center gap-1 text-sm text-muted-foreground">
          <LoaderIcon aria-hidden="true" className="size-3 animate-spin" />
          {progreso < 100 ? `Subiendo… ${progreso} %` : 'Esperando la confirmación del servidor…'}
        </p>
      )}
      {error && <p role="alert" className="text-sm text-estado-vencido">{error}</p>}
      {editable && !ocupado && (
        <div className="flex flex-wrap gap-2">
          <input ref={entrada} type="file" aria-label={`Archivo de ${bl.codigo}`} accept={EXTENSIONES_ACEPTADAS.join(',')} className="sr-only"
            onChange={(evento) => { const elegido = evento.target.files?.[0]; if (elegido) cargar(elegido); evento.target.value = ''; }} />
          {error && (adjuntoPorLigar || archivo) ? (
            <Button size="sm" variant="outline" onClick={() => { setError(null); if (adjuntoPorLigar) ligar(adjuntoPorLigar); else if (archivo) cargar(archivo); }}>
              <RotateCwIcon data-icon="inline-start" />
              Reintentar
            </Button>
          ) : (
            <Button size="sm" variant="outline" onClick={() => entrada.current?.click()}>
              <UploadIcon data-icon="inline-start" />
              {bl.packingList ? 'Reemplazar' : 'Elegir archivo'}
            </Button>
          )}
        </div>
      )}
    </div>
  );
}

function DialogoAnadirPedido({ pedidoId, bl, onCerrar }: { pedidoId: string; bl: Bl; onCerrar: () => void }) {
  const mutaciones = useMutacionesEmbarques(pedidoId);
  const [numero, setNumero] = useState('');
  const [error, setError] = useState<string | null>(null);

  function enviar(evento: FormEvent) {
    evento.preventDefault();
    setError(null);
    mutaciones.anadirPedido.mutate({ blId: bl.id, numero: numero.trim() }, {
      onSuccess: () => { toast.success(`Pedido añadido a ${bl.codigo}.`); onCerrar(); },
      onError: (fallo) => setError(mensaje(fallo)),
    });
  }

  return (
    <Dialog open onOpenChange={(abierto) => !abierto && onCerrar()}>
      <DialogContent>
        <DialogHeader>
          <DialogTitle>Añadir pedido a {bl.codigo}</DialogTitle>
          <DialogDescription>Pedido en curso del mismo país, incluidos pedidos anteriores. El Packing List se guarda una sola vez.</DialogDescription>
        </DialogHeader>
        <form onSubmit={enviar} className="flex flex-col gap-4">
          <Field data-invalid={error ? true : undefined}>
            <FieldLabel htmlFor="numero-pedido-bl">Número del pedido</FieldLabel>
            <Input id="numero-pedido-bl" value={numero} placeholder="DK-EC-2026-0023" aria-invalid={error ? true : undefined}
              onChange={(evento) => setNumero(evento.target.value)} />
            <FieldError>{error}</FieldError>
          </Field>
          <DialogFooter>
            <Button type="button" variant="outline" onClick={onCerrar}>Cancelar</Button>
            <Button type="submit" disabled={!numero.trim() || mutaciones.anadirPedido.isPending}>Vincular pedido</Button>
          </DialogFooter>
        </form>
      </DialogContent>
    </Dialog>
  );
}

function HistorialEnvios({ vista }: { vista: VistaEmbarques }) {
  if (vista.envios.length === 0) return null;
  return (
    <details className="rounded-lg border bg-card p-4 shadow-sm">
      <summary className="cursor-pointer font-heading font-semibold">Envíos y decisiones ({vista.envios.length})</summary>
      <ol className="mt-3 flex flex-col gap-3">
        {vista.envios.map((envio) => (
          <li key={envio.id} className="rounded-md border p-3 text-sm">
            <p className="font-medium">Envío {envio.numero} · {envio.estado.toLowerCase()} · {envio.enviadoPor}, {formatearFechaHora(envio.enviadoEn)}</p>
            <ul className="mt-1 list-disc pl-5 text-muted-foreground">
              {envio.filas.map((fila) => (
                <li key={fila.blId}>{fila.codigo} ({nombreImportadora(fila.importadora)}): Packing List v{fila.version}, pedidos {fila.pedidos.map((p) => `${p.numero} (vinculó ${p.vinculadoPor})`).join(', ')}</li>
              ))}
            </ul>
            {envio.revisiones.map((revision) => (
              <p key={`${revision.blId}-${revision.version}`} className="mt-1">Revisada la v{revision.version} de {revision.codigo} por {revision.revisadoPor}.</p>
            ))}
            {envio.decisiones.map((decision) => (
              <p key={decision.decididoEn} className="mt-1">
                {decision.decision === 'APROBADA' ? 'Aprobado' : 'Rechazado'} por {decision.usuario}, {formatearFechaHora(decision.decididoEn)}
                {decision.motivo ? `: ${decision.motivo}` : ''}
              </p>
            ))}
          </li>
        ))}
      </ol>
    </details>
  );
}

function AlertasSimuladas({ pedidoId }: { pedidoId: string }) {
  const alertas = useAlertasSimuladas(pedidoId);
  if (!alertas.data?.length) return null;
  return (
    <details className="rounded-lg border bg-card p-4 shadow-sm">
      <summary className="cursor-pointer font-heading font-semibold">Alertas de WhatsApp simuladas ({alertas.data.length})</summary>
      <p className="mt-2 text-xs text-muted-foreground">Simulación: no se envió ningún mensaje real.</p>
      <ul className="mt-2 flex flex-col gap-1 text-sm">
        {alertas.data.map((alerta) => (
          <li key={alerta.id}><b>{alerta.destinatario}</b>: {alerta.texto} <span className="text-xs text-muted-foreground">{formatearFechaHora(alerta.creadoEn)}</span></li>
        ))}
      </ul>
    </details>
  );
}

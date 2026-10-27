import { CloudIcon, CloudOffIcon, DownloadIcon, FileIcon, LoaderIcon, UploadIcon } from 'lucide-react';
import { type FormEvent, useEffect, useId, useRef, useState } from 'react';
import { toast } from 'sonner';
import { esErrorApi } from '@/api/cliente';
import { type ProgresoCarga, useCargaArchivo } from '@/api/cargas';
import { type Adjunto, type EtapaEnLinea, useAdjuntosDeEtapa, useDescargarArchivo } from '@/api/pedidos';
import { Alert, AlertDescription } from '@/components/ui/alert';
import { Badge } from '@/components/ui/badge';
import { Button } from '@/components/ui/button';
import { Field, FieldDescription, FieldGroup, FieldLabel } from '@/components/ui/field';
import { Input } from '@/components/ui/input';
import { Select, SelectContent, SelectGroup, SelectItem, SelectTrigger, SelectValue } from '@/components/ui/select';
import { Skeleton } from '@/components/ui/skeleton';
import { cn } from '@/lib/utils';
import { formatearFechaHora, formatearTamano, mensajeDeErrorPedido } from './formato';

// Archivos de una etapa (DESIGN.md 7.5; P-05 opción A, P-17 y P-18). Se listan los de todas sus entradas, pero solo
// los de la entrada actual cuentan para los requisitos de una acción. La subida va a la entrada que la persona ve
// (`entradaEsperadaId`): si otra persona movió el pedido, el servidor responde 409 y nada queda a medias.

/** Límite provisional del equipo (100 MiB por archivo), pendiente del cliente. El servidor lo vuelve a comprobar. */
const TAMANO_MAXIMO = 100 * 1024 * 1024;
/**
 * Formatos admitidos (ADR-0026): solo ayuda al elegir. El servidor decide por la extensión, el tipo y el contenido, y
 * responde 415 TIPO_ARCHIVO_NO_ACEPTADO si no corresponde.
 */
const FORMATOS_ACEPTADOS = '.pdf,.xlsx,.xls,.csv,.jpg,.jpeg,.png';
const EXTENSIONES_ACEPTADAS = FORMATOS_ACEPTADOS.split(',');
const OTRO = '__otro__';

interface Props {
  pedidoId: string;
  etapa: EtapaEnLinea;
  /** El servidor permitiría subir aquí: etapa actual y la persona puede intervenir (según accionesDisponibles). */
  puedeSubir: boolean;
  /** Pista de P-17 para mostrar el botón; el servidor decide y puede responder 403. */
  puedeDescargar: boolean;
  /** Tipos de documento que piden las acciones de la etapa, para elegir al subir. */
  tiposSugeridos: string[];
}

export function ArchivosEtapa({ pedidoId, etapa, puedeSubir, puedeDescargar, tiposSugeridos }: Props) {
  const adjuntos = useAdjuntosDeEtapa(pedidoId, etapa.numero);
  const descargar = useDescargarArchivo();
  const [descargando, setDescargando] = useState<string | null>(null);

  const actuales = adjuntos.data?.filter((adjunto) => adjunto.entradaId === etapa.entradaId) ?? [];
  const anteriores = adjuntos.data?.filter((adjunto) => adjunto.entradaId !== etapa.entradaId) ?? [];

  function bajar(adjunto: Adjunto) {
    setDescargando(adjunto.id);
    descargar.mutate(adjunto, {
      onError: (error) =>
        toast.error(
          esErrorApi(error) && error.status === 403
            ? 'No tienes permiso para descargar este archivo. Lo descargan el admin, quien actúa en su etapa y quien la revisa.'
            : mensajeDeErrorPedido(error, 'No se pudo descargar el archivo. Intenta de nuevo.'),
        ),
      onSettled: () => setDescargando(null),
    });
  }

  return (
    <section aria-labelledby={`archivos-${etapa.numero}`} className="flex flex-col gap-4">
      <h3 id={`archivos-${etapa.numero}`} className="font-heading text-lg font-semibold">
        Archivos
      </h3>

      {adjuntos.isPending ? (
        <div className="flex flex-col gap-2" aria-busy="true" aria-label="Cargando archivos">
          <Skeleton className="h-14 w-full" />
          <Skeleton className="h-14 w-full" />
        </div>
      ) : adjuntos.isError ? (
        <Alert variant="destructive">
          <AlertDescription className="flex flex-wrap items-center justify-between gap-3">
            <p>{mensajeDeErrorPedido(adjuntos.error, 'No se pudieron cargar los archivos de la etapa.')}</p>
            <Button variant="outline" size="sm" onClick={() => void adjuntos.refetch()}>
              Reintentar
            </Button>
          </AlertDescription>
        </Alert>
      ) : (
        <>
          <ListaAdjuntos
            adjuntos={actuales}
            vacio={
              etapa.entradaId
                ? 'Todavía no hay archivos en esta entrada de la etapa.'
                : 'El pedido aún no llega a esta etapa.'
            }
            puedeDescargar={puedeDescargar}
            descargando={descargando}
            onDescargar={bajar}
          />
          {anteriores.length > 0 && (
            <details className="rounded-lg border bg-muted/40 px-4 py-1">
              <summary className="flex min-h-10 cursor-pointer items-center rounded-sm text-sm font-medium outline-none focus-visible:ring-3 focus-visible:ring-ring">
                De entradas anteriores ({anteriores.length}): no cuentan para los requisitos actuales
              </summary>
              <div className="pb-3">
                <ListaAdjuntos
                  adjuntos={anteriores}
                  vacio=""
                  puedeDescargar={puedeDescargar}
                  descargando={descargando}
                  onDescargar={bajar}
                />
              </div>
            </details>
          )}
        </>
      )}

      {puedeSubir && etapa.entradaId && (
        // key por entrada: si el pedido vuelve a entrar en la etapa, el formulario empieza de cero (Q6).
        <FormularioSubida
          key={etapa.entradaId}
          pedidoId={pedidoId}
          etapa={etapa.numero}
          entradaId={etapa.entradaId}
          tiposSugeridos={tiposSugeridos}
        />
      )}
    </section>
  );
}

function ListaAdjuntos({
  adjuntos,
  vacio,
  puedeDescargar,
  descargando,
  onDescargar,
}: {
  adjuntos: Adjunto[];
  vacio: string;
  puedeDescargar: boolean;
  descargando: string | null;
  onDescargar: (adjunto: Adjunto) => void;
}) {
  if (adjuntos.length === 0) return vacio ? <p className="text-sm text-muted-foreground">{vacio}</p> : null;
  return (
    <ul className="flex flex-col divide-y rounded-lg border bg-card">
      {adjuntos.map((adjunto) => (
        <li key={adjunto.id} className="flex flex-wrap items-center gap-x-4 gap-y-2 px-3 py-2.5">
          <FileIcon aria-hidden="true" className="size-4 shrink-0 text-muted-foreground" />
          <div className="flex min-w-0 flex-1 flex-col">
            <span className="truncate font-medium" title={adjunto.nombre}>
              {adjunto.nombre}
            </span>
            <span className="text-sm text-muted-foreground">
              {adjunto.tipoDocumento} · <span className="tabular-nums">{formatearTamano(adjunto.tamanoBytes)}</span> ·{' '}
              {adjunto.subidoPor.nombre} · <span className="tabular-nums">{formatearFechaHora(adjunto.subidoEn)}</span>
            </span>
          </div>
          <EstadoCopia estado={adjunto.estadoCopia} />
          {/* ADR-0028: sin enlace de Drive; se abre o descarga a través del backend. */}
          {puedeDescargar && (
            <Button
              variant="outline"
              size="sm"
              onClick={() => onDescargar(adjunto)}
              disabled={descargando === adjunto.id}
              aria-label={`Descargar ${adjunto.nombre}`}
            >
              {descargando === adjunto.id ? (
                <LoaderIcon data-icon="inline-start" className="animate-spin" />
              ) : (
                <DownloadIcon data-icon="inline-start" />
              )}
              Descargar
            </Button>
          )}
        </li>
      ))}
    </ul>
  );
}

/** Copia a Drive: es de la fase 3; en la fase 2 todos los archivos quedan PENDIENTE (P-05 opción A). */
function EstadoCopia({ estado }: { estado: Adjunto['estadoCopia'] }) {
  const base = 'h-auto gap-1 rounded-sm py-0.5 text-sm [&>svg]:size-3.5!';
  if (estado === 'COPIADO') {
    return (
      <Badge variant="outline" className={cn(base, 'border-transparent bg-estado-ok-fondo text-estado-ok')}>
        <CloudIcon aria-hidden="true" />
        En Drive
      </Badge>
    );
  }
  if (estado === 'FALLIDO') {
    return (
      <Badge variant="outline" className={cn(base, 'border-transparent bg-estado-vencido-fondo text-estado-vencido')}>
        <CloudOffIcon aria-hidden="true" />
        Falló la copia
      </Badge>
    );
  }
  return (
    <Badge variant="outline" className={cn(base, 'border-transparent bg-estado-neutro-fondo text-estado-neutro')}>
      <CloudIcon aria-hidden="true" />
      Copia a Drive pendiente
    </Badge>
  );
}

function FormularioSubida({
  pedidoId,
  etapa,
  entradaId,
  tiposSugeridos,
}: {
  pedidoId: string;
  etapa: number;
  entradaId: string;
  tiposSugeridos: string[];
}) {
  const subir = useCargaArchivo();
  const control = useRef<AbortController | null>(null);
  const [progreso, setProgreso] = useState<ProgresoCarga | null>(null);
  useEffect(() => () => control.current?.abort(), []);
  const ids = useId();
  const entrada = useRef<HTMLInputElement>(null);
  const [archivo, setArchivo] = useState<File | null>(null);
  const [tipo, setTipo] = useState(tiposSugeridos[0] ?? OTRO);
  const [otroTipo, setOtroTipo] = useState('');
  const [error, setError] = useState<string | null>(null);

  const tipoDocumento = (tipo === OTRO ? otroTipo : tipo).trim();

  function elegir(lista: FileList | null) {
    setError(null);
    setProgreso(null);
    const elegido = lista?.[0] ?? null;
    if (elegido && elegido.size === 0) {
      setError('El archivo está vacío.');
      setArchivo(null);
      return;
    }
    if (elegido && elegido.size > TAMANO_MAXIMO) {
      setError('El archivo supera el tamaño máximo permitido (100 MiB, valor provisional).');
      setArchivo(null);
      return;
    }
    const nombre = elegido?.name.toLowerCase() ?? '';
    if (elegido && !EXTENSIONES_ACEPTADAS.some((extension) => nombre.endsWith(extension))) {
      setError('Formato no admitido. Sube un PDF, un Excel (.xlsx o .xls), un CSV o una imagen JPG o PNG.');
      setArchivo(null);
      return;
    }
    setArchivo(elegido);
  }

  function enviar(evento: FormEvent) {
    evento.preventDefault();
    if (subir.isPending) return;
    if (!archivo) {
      setError('Elige un archivo.');
      return;
    }
    if (!tipoDocumento || tipoDocumento.length > 100) {
      setError('Indica el tipo de documento (hasta 100 caracteres).');
      return;
    }
    setError(null);
    control.current = new AbortController();
    subir.mutate(
      { pedidoId, etapa, entradaEsperadaId: entradaId, tipoDocumento, archivo,
        signal: control.current.signal, onProgreso: setProgreso },
      {
        onSuccess: (adjunto) => {
          toast.success(`${adjunto.nombre} recibido por el servidor. Copia a Drive pendiente.`);
          setArchivo(null);
          setProgreso(null);
          if (entrada.current) entrada.current.value = '';
        },
        onError: (fallo) => {
          if (fallo instanceof DOMException && fallo.name === 'AbortError') {
            setError('Carga pausada. Vuelve a pulsar Subir o reanudar para consultar el progreso confirmado.');
          } else if (esErrorApi(fallo, 'CARGA_CONFLICTO')) {
            setError('La carga no coincide con el archivo o el progreso cambió. Elige el mismo archivo y vuelve a reanudar; no se sobrescriben los bytes confirmados.');
          } else if (esErrorApi(fallo, 'CARGA_OCUPADA')) {
            setError('Esta carga está siendo atendida en otra petición. Espera un momento y reanuda.');
          } else if (esErrorApi(fallo, 'CARGA_ALMACENAMIENTO')) {
            setError('El servidor no pudo conservar o recuperar los bytes. No se ha confirmado un adjunto nuevo. Avisa al administrador si persiste.');
          } else if (esErrorApi(fallo, 'CARGA_MEMORIA')) {
            setError('No se pudo conservar la referencia de carga en esta pestaña. Revisa el almacenamiento del navegador antes de continuar.');
          } else {
            setError(mensajeDeErrorPedido(fallo, 'No se pudo completar la carga. Conserva el archivo y vuelve a pulsar Subir o reanudar.'));
          }
        },
        onSettled: () => { control.current = null; },
      },
    );
  }

  return (
    <form noValidate onSubmit={enviar} className="flex flex-col gap-4 rounded-lg border border-dashed bg-muted/30 p-4">
      <FieldGroup className="gap-4 sm:flex-row sm:items-start">
        <Field className="sm:w-60">
          <FieldLabel htmlFor={`${ids}-tipo`}>Tipo de documento</FieldLabel>
          <Select value={tipo} onValueChange={setTipo} disabled={subir.isPending}>
            <SelectTrigger id={`${ids}-tipo`} className="w-full">
              <SelectValue />
            </SelectTrigger>
            <SelectContent>
              <SelectGroup>
                {tiposSugeridos.map((sugerido) => (
                  <SelectItem key={sugerido} value={sugerido}>
                    {sugerido}
                  </SelectItem>
                ))}
                <SelectItem value={OTRO}>Otro…</SelectItem>
              </SelectGroup>
            </SelectContent>
          </Select>
          {tipo === OTRO && (
            <Input
              aria-label="Nombre del tipo de documento"
              value={otroTipo}
              maxLength={100}
              disabled={subir.isPending}
              onChange={(evento) => setOtroTipo(evento.target.value)}
              placeholder="Por ejemplo, ficha técnica"
            />
          )}
        </Field>
        <Field className="flex-1">
          <FieldLabel htmlFor={`${ids}-archivo`}>Archivo</FieldLabel>
          <Input
            id={`${ids}-archivo`}
            ref={entrada}
            type="file"
            accept={FORMATOS_ACEPTADOS}
            disabled={subir.isPending}
            onChange={(evento) => elegir(evento.target.files)}
            className="cursor-pointer py-1.5"
            aria-describedby={`${ids}-ayuda`}
          />
          <FieldDescription id={`${ids}-ayuda`}>
            PDF, Excel (.xlsx o .xls), CSV, JPG o PNG, hasta 100 MiB.
          </FieldDescription>
        </Field>
      </FieldGroup>
      {error && (
        <Alert variant="destructive">
          <AlertDescription>{error}</AlertDescription>
        </Alert>
      )}
      {progreso && (
        <div className="flex flex-col gap-2">
          <progress aria-label="Bytes confirmados de la carga" max={progreso.total} value={progreso.confirmados} className="h-2 w-full" />
          <p role="status" aria-live="polite" className="text-sm text-muted-foreground">
            {progreso.fase === 'comprobando' ? 'Comprobando el archivo elegido. ' : ''}
            {progreso.fase === 'finalizando' ? 'Confirmando el documento. ' : ''}
            {Math.floor(100 * progreso.confirmados / progreso.total)} % recibido por el servidor.
            La copia a Drive permanece pendiente.
          </p>
        </div>
      )}
      <div className="flex flex-wrap items-center justify-end gap-3">
        {subir.isPending && (
          <span role="status" className="text-sm text-muted-foreground">
            Subiendo {archivo?.name}…
          </span>
        )}
        {subir.isPending && (
          <Button type="button" variant="ghost" disabled={progreso?.fase === 'finalizando'} onClick={() => control.current?.abort()}>Pausar</Button>
        )}
        <Button type="submit" variant="outline" disabled={subir.isPending || !archivo}>
          {subir.isPending ? (
            <LoaderIcon data-icon="inline-start" className="animate-spin" />
          ) : (
            <UploadIcon data-icon="inline-start" />
          )}
          Subir o reanudar
        </Button>
      </div>
    </form>
  );
}

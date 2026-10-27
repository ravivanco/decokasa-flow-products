import { ArrowLeftIcon, BotIcon, FileTextIcon, PlusIcon, SendIcon } from 'lucide-react';
import { useId, useState, type FormEvent } from 'react';
import { Link, useParams } from 'react-router';
import {
  type ConversacionAsistente,
  type DocumentoExcluidoAsistente,
  type MensajeAsistente,
  type ReferenciaAsistente,
  useAbrirConversacionAsistente,
  useConversacionAsistente,
  useConversacionesAsistente,
  usePreguntarAsistente,
} from '@/api/asistente';
import { esErrorApi } from '@/api/cliente';
import { mensajeDeError } from '@/api/mensajes';
import { Alert, AlertDescription, AlertTitle } from '@/components/ui/alert';
import { Badge } from '@/components/ui/badge';
import { Button } from '@/components/ui/button';
import { Card, CardContent, CardDescription, CardHeader, CardTitle } from '@/components/ui/card';
import { Label } from '@/components/ui/label';
import { Skeleton } from '@/components/ui/skeleton';
import { Textarea } from '@/components/ui/textarea';

// Chat del asistente sobre un pedido (paso 7.5; contrato 0.19.0; ADR-0020 a ADR-0023). Solo consulta: el asistente no
// ejecuta acciones. El servidor decide permisos, límites y qué documentos se envían; la pantalla solo muestra lo que él
// devuelve. Los mensajes retenidos llegan sin texto y nunca se reconstruyen aquí. Nada se guarda en el navegador fuera
// de la caché en memoria (F7-D5). Las respuestas tardías de otra cuenta o de otro país las descarta api/asistente.ts.
// Los componentes de chat de shadcn (MessageScroller, Message, Bubble) no están instalados: se compone con Card.

const MAX_PREGUNTA = 2000;

const MOTIVOS_EXCLUSION: Record<DocumentoExcluidoAsistente['motivo'], string> = {
  FORMATO_NO_ADMITIDO: 'no es PDF (en el piloto solo se envían PDF)',
  CONTEXTO_EXCEDIDO: 'no cabía en el límite de la consulta',
  NO_AUTORIZADO_PILOTO: 'no está autorizado para el piloto con datos ficticios',
};

const fechaHora = new Intl.DateTimeFormat('es-EC', { dateStyle: 'medium', timeStyle: 'short' });

function textoFecha(iso: string) {
  const fecha = new Date(iso);
  return Number.isNaN(fecha.getTime()) ? iso : fechaHora.format(fecha);
}

/** Textos claros para los errores del asistente; los mensajes del servidor para límites y validación ya lo son. */
function mensajeAsistente(error: unknown, general: string): string {
  if (esErrorApi(error, 'IA_NO_DISPONIBLE')) {
    return 'El asistente no está disponible en este momento. Inténtalo más tarde o avisa a un administrador.';
  }
  if (esErrorApi(error, 'LIMITE_CONSUMO') || esErrorApi(error, 'LIMITE_PREGUNTAS')
    || esErrorApi(error, 'CONTEXTO_EXCEDIDO') || esErrorApi(error, 'VALIDACION')) {
    return error.message;
  }
  if (esErrorApi(error) && error.status === 404) {
    return 'La conversación o el pedido ya no están disponibles para ti.';
  }
  return mensajeDeError(error, general);
}

function Referencias({ referencias }: { referencias: ReferenciaAsistente[] }) {
  if (referencias.length === 0) return null;
  return (
    <div className="flex flex-col gap-2">
      <p className="text-sm font-medium">Referencias verificadas</p>
      <ul className="flex flex-col gap-2">
        {referencias.map((referencia, indice) => (
          <li key={indice} className="flex flex-col gap-1 rounded-md border p-3 text-sm">
            <span className="flex flex-wrap items-center gap-x-2 gap-y-1 font-medium">
              {referencia.nombreArchivo ? <FileTextIcon aria-hidden="true" /> : null}
              {referencia.nombreArchivo ?? `Datos del pedido ${referencia.numeroPedido}`}
              {referencia.etapa != null && <span className="text-muted-foreground">Etapa {referencia.etapa}</span>}
              {referencia.pagina != null && <span className="text-muted-foreground">Página {referencia.pagina}</span>}
            </span>
            {referencia.textoCitado && <q className="text-muted-foreground">{referencia.textoCitado}</q>}
          </li>
        ))}
      </ul>
    </div>
  );
}

function Excluidos({ documentos }: { documentos: DocumentoExcluidoAsistente[] }) {
  if (documentos.length === 0) return null;
  return (
    <div className="flex flex-col gap-2">
      <p className="text-sm font-medium">Documentos que no se usaron</p>
      <ul className="flex flex-col gap-1 text-sm text-muted-foreground">
        {documentos.map((documento) => (
          <li key={documento.archivoId}>
            {documento.nombreArchivo}: {MOTIVOS_EXCLUSION[documento.motivo]}.
          </li>
        ))}
      </ul>
    </div>
  );
}

function Mensaje({ mensaje }: { mensaje: MensajeAsistente }) {
  const esPregunta = mensaje.autor === 'USUARIO';
  return (
    <Card className={esPregunta ? 'sm:ml-12' : 'sm:mr-12'}>
      <CardHeader>
        <CardTitle className="flex flex-wrap items-center gap-2 text-base">
          {esPregunta ? 'Tu pregunta' : 'Asistente'}
          {!esPregunta && !mensaje.retenido && (
            mensaje.evidenciaSuficiente
              ? <Badge variant="secondary">Con referencias verificadas</Badge>
              : <Badge variant="outline">Sin evidencia suficiente</Badge>
          )}
        </CardTitle>
        <CardDescription>{textoFecha(mensaje.creadoEn)}</CardDescription>
      </CardHeader>
      <CardContent className="flex flex-col gap-4">
        {mensaje.retenido ? (
          <Alert>
            <AlertTitle>Mensaje retenido</AlertTitle>
            <AlertDescription>
              Ya no tienes acceso a alguno de los documentos de este mensaje, así que no se muestra su contenido.
            </AlertDescription>
          </Alert>
        ) : (
          <>
            <p className="whitespace-pre-wrap">{mensaje.texto}</p>
            <Referencias referencias={mensaje.referencias ?? []} />
            <Excluidos documentos={mensaje.documentosExcluidos ?? []} />
          </>
        )}
      </CardContent>
    </Card>
  );
}

function FormularioPregunta({ conversacion }: { conversacion: ConversacionAsistente }) {
  const ids = useId();
  const [pregunta, setPregunta] = useState('');
  const preguntar = usePreguntarAsistente();
  const agotada = conversacion.preguntasRestantes === 0;
  const texto = pregunta.trim();

  function enviar(evento: FormEvent) {
    evento.preventDefault();
    if (!texto || texto.length > MAX_PREGUNTA || agotada || preguntar.isPending) return;
    preguntar.mutate({ conversacionId: conversacion.id, pregunta: texto }, { onSuccess: () => setPregunta('') });
  }

  return (
    <form onSubmit={enviar} className="flex flex-col gap-3">
      <Label htmlFor={`${ids}-pregunta`}>Tu pregunta sobre este pedido</Label>
      <Textarea
        id={`${ids}-pregunta`}
        value={pregunta}
        onChange={(evento) => setPregunta(evento.target.value)}
        maxLength={MAX_PREGUNTA}
        rows={3}
        disabled={agotada || preguntar.isPending}
        aria-describedby={`${ids}-ayuda`}
        aria-invalid={preguntar.isError || undefined}
      />
      <p id={`${ids}-ayuda`} className="text-sm text-muted-foreground">
        {agotada
          ? 'Esta conversación llegó a su límite de preguntas. Abre una nueva.'
          : `${pregunta.length} de ${MAX_PREGUNTA} caracteres · Te quedan ${conversacion.preguntasRestantes} preguntas en esta conversación.`}
      </p>
      {preguntar.isError && (
        <Alert variant="destructive" role="alert">
          <AlertTitle>No se pudo responder</AlertTitle>
          <AlertDescription>
            {mensajeAsistente(preguntar.error, 'No se pudo enviar la pregunta. Intenta de nuevo.')}
          </AlertDescription>
        </Alert>
      )}
      <Button type="submit" className="w-fit" disabled={!texto || agotada || preguntar.isPending}>
        <SendIcon data-icon="inline-start" />
        {preguntar.isPending ? 'Consultando…' : 'Preguntar'}
      </Button>
    </form>
  );
}

function Conversacion({ conversacionId }: { conversacionId: string }) {
  const detalle = useConversacionAsistente(conversacionId);
  if (detalle.isPending) {
    return (
      <div className="flex flex-col gap-3" aria-busy="true" aria-label="Cargando la conversación">
        <Skeleton className="h-24" />
        <Skeleton className="h-24" />
      </div>
    );
  }
  if (detalle.isError) {
    return (
      <Alert variant="destructive" role="alert">
        <AlertTitle>No se pudo cargar la conversación</AlertTitle>
        <AlertDescription>{mensajeAsistente(detalle.error, 'Intenta de nuevo en unos segundos.')}</AlertDescription>
      </Alert>
    );
  }
  return (
    <section aria-label="Mensajes" className="flex flex-col gap-4">
      {detalle.data.mensajes.length === 0 && (
        <p className="text-muted-foreground">Todavía no hay preguntas en esta conversación.</p>
      )}
      {detalle.data.mensajes.map((mensaje) => <Mensaje key={mensaje.orden} mensaje={mensaje} />)}
      <FormularioPregunta conversacion={detalle.data} />
    </section>
  );
}

export function AsistentePedido() {
  const { pedidoId = '' } = useParams();
  const conversaciones = useConversacionesAsistente(pedidoId, Boolean(pedidoId));
  const abrir = useAbrirConversacionAsistente();
  const [elegida, setElegida] = useState<string | null>(null);
  const lista = conversaciones.data?.elementos ?? [];
  const activa = elegida && lista.some((c) => c.id === elegida) ? elegida : lista[0]?.id ?? null;
  const numero = lista[0]?.numeroPedido;

  return (
    <section className="flex flex-col gap-6">
      <Button variant="ghost" asChild className="w-fit">
        <Link to={`/pedidos/${encodeURIComponent(pedidoId)}`}>
          <ArrowLeftIcon data-icon="inline-start" />
          Volver al pedido
        </Link>
      </Button>

      <header className="flex flex-col gap-2">
        <h1 className="flex items-center gap-2 font-heading text-2xl font-bold text-primary">
          <BotIcon aria-hidden="true" />
          Asistente{numero ? ` del pedido ${numero}` : ''}
        </h1>
        <Alert>
          <AlertTitle>Piloto con datos ficticios</AlertTitle>
          <AlertDescription>
            El asistente solo consulta los datos y los documentos de este pedido que tú puedes ver. No hace cambios.
            Revisa las respuestas con las referencias citadas; si no hay evidencia suficiente, lo dice.
          </AlertDescription>
        </Alert>
      </header>

      <div className="flex flex-wrap items-center gap-2">
        <Button
          onClick={() => abrir.mutate(pedidoId, { onSuccess: (nueva) => setElegida(nueva.id) })}
          disabled={!pedidoId || abrir.isPending}
        >
          <PlusIcon data-icon="inline-start" />
          Nueva conversación
        </Button>
        {lista.map((conversacion, indice) => (
          <Button
            key={conversacion.id}
            variant={conversacion.id === activa ? 'secondary' : 'outline'}
            aria-pressed={conversacion.id === activa}
            onClick={() => setElegida(conversacion.id)}
          >
            Conversación {lista.length - indice} · {textoFecha(conversacion.creadaEn)}
          </Button>
        ))}
      </div>

      {abrir.isError && (
        <Alert variant="destructive" role="alert">
          <AlertTitle>No se pudo abrir la conversación</AlertTitle>
          <AlertDescription>{mensajeAsistente(abrir.error, 'Intenta de nuevo en unos segundos.')}</AlertDescription>
        </Alert>
      )}
      {conversaciones.isError && (
        <Alert variant="destructive" role="alert">
          <AlertTitle>No se pudieron cargar las conversaciones</AlertTitle>
          <AlertDescription>
            {mensajeAsistente(conversaciones.error, 'Intenta de nuevo en unos segundos.')}
          </AlertDescription>
        </Alert>
      )}
      {conversaciones.isPending && <Skeleton className="h-10 w-64" />}
      {conversaciones.isSuccess && lista.length === 0 && (
        <p className="text-muted-foreground">No tienes conversaciones sobre este pedido. Abre una para preguntar.</p>
      )}
      {activa && <Conversacion key={activa} conversacionId={activa} />}
    </section>
  );
}

import { ArrowLeftIcon, BotIcon, RotateCwIcon } from 'lucide-react';
import { useState } from 'react';
import { Link, useParams } from 'react-router';
import { usePausarActualizacion } from '@/api/actualizacion';
import { puedeUsarAsistente } from '@/api/asistente';
import { esErrorApi } from '@/api/cliente';
import { type AccionDisponible, type Adjunto, type Persona, useDefinicionFlujo, usePedido } from '@/api/pedidos';
import { esAdmin, useUsuarioActual } from '@/auth/SesionProvider';
import { Alert, AlertDescription } from '@/components/ui/alert';
import { Badge } from '@/components/ui/badge';
import { Button } from '@/components/ui/button';
import { Skeleton } from '@/components/ui/skeleton';
import { BorradoresEnConflicto, ProveedorBorradores } from './borradores';
import { DocumentosPedido } from './DocumentosPedido';
import { CronogramaPedido } from './CronogramaPedido';
import { DialogoAccion } from './DialogoAccion';
import { DialogoReasignar } from './DialogoReasignar';
import { DistintivoUrgencia, DistintivoVencimiento } from './Distintivos';
import { codigoEtapa, formatearFecha, formatearFechaHora, mensajeDeErrorPedido, nombreFlujo } from './formato';
import { PanelHistorial } from './HistorialPedido';
import { LineaDeTiempo } from './LineaDeTiempo';
import { PanelEtapa } from './PanelEtapa';
import { ciudadDeBodega } from '@/api/esquemas';
import { esPais, NOMBRE_PAIS, usePaisActivo, useSeleccionPais } from '@/pais/PaisProvider';

// Detalle del pedido: encabezado, línea de tiempo, panel de la etapa elegida e historial. Todo sale del servidor; tras
// cada acción la pantalla muestra el pedido que él devolvió (o el vigente, si respondió con un conflicto).

// Paso 5.5: los borradores viven por encima del contenido (y por pedido), para que un refresco, un error de lectura o
// un cambio de etapa no borren en silencio el texto sin enviar.
export function DetallePedido() {
  const { pedidoId = '' } = useParams();
  return (
    <ProveedorBorradores key={pedidoId}>
      <ContenidoDetalle pedidoId={pedidoId} />
    </ProveedorBorradores>
  );
}

function ContenidoDetalle({ pedidoId }: { pedidoId: string }) {
  const usuario = useUsuarioActual();
  const paisActivo = usePaisActivo();
  const seleccion = useSeleccionPais();
  const pedido = usePedido(pedidoId);
  const datos = pedido.data;
  const flujo = useDefinicionFlujo(datos?.pais ?? 'EC', datos?.tipoFlujo ?? 'A', datos?.versionFlujo);

  const [elegida, setElegida] = useState<number | null>(null);
  const [actualVista, setActualVista] = useState<number | null>(null);
  const [accion, setAccion] = useState<{
    accion: AccionDisponible;
    etapaEsperada: number;
    adjuntos: Adjunto[];
  } | null>(null);
  const [reasignando, setReasignando] = useState(false);
  // Paso 5.4: con un diálogo de acción o de reasignación abierto no se refresca solo; al confirmar, el servidor
  // vuelve a comprobar la etapa esperada (y la entrada o la versión, según la operación).
  usePausarActualizacion(accion !== null || reasignando);

  // Si el pedido cambia de etapa (por una acción propia o ajena), el panel vuelve a la etapa actual.
  if (datos && datos.etapaActual !== actualVista) {
    setActualVista(datos.etapaActual);
    setElegida(null);
  }

  if (pedido.isPending) {
    return (
      <section className="flex flex-col gap-6" aria-busy="true" aria-label="Cargando el pedido">
        <Skeleton className="h-10 w-72" />
        <Skeleton className="h-36 w-full" />
        <Skeleton className="h-80 w-full" />
      </section>
    );
  }

  if (pedido.isError) {
    const noExiste = esErrorApi(pedido.error, 'NO_ENCONTRADO');
    return (
      <section className="flex flex-col items-start gap-4">
        <h1 className="font-heading text-2xl font-bold text-primary">
          {noExiste ? 'Pedido no encontrado' : 'No se pudo cargar el pedido'}
        </h1>
        <p className="text-muted-foreground">
          {noExiste
            ? 'El pedido no existe o la dirección es incorrecta.'
            : mensajeDeErrorPedido(pedido.error, 'Revisa tu conexión e intenta de nuevo.')}
        </p>
        <div className="flex gap-2">
          {!noExiste && (
            <Button variant="outline" onClick={() => void pedido.refetch()}>
              <RotateCwIcon data-icon="inline-start" />
              Reintentar
            </Button>
          )}
          <Button variant="outline" asChild>
            <Link to="/pedidos">Ver pedidos</Link>
          </Button>
        </div>
      </section>
    );
  }

  const detalle = pedido.data;
  // Paso 7.3: un pedido de otro país (asignado, si el servidor lo devolvió) no se muestra en el país activo. Nada de sus
  // datos llega a la pantalla; se ofrece cambiar de país, con la misma confirmación si hay cambios sin guardar.
  if (detalle.pais !== paisActivo) {
    const destino = esPais(detalle.pais) && seleccion?.asignados.includes(detalle.pais) ? detalle.pais : null;
    return (
      <section className="flex flex-col items-start gap-4">
        <h1 className="font-heading text-2xl font-bold text-primary">Pedido de otro país</h1>
        <p className="max-w-prose text-muted-foreground">
          Este pedido no es de {NOMBRE_PAIS[paisActivo]}.
          {destino ? ` Cambia a ${NOMBRE_PAIS[destino]} para verlo.` : ''}
        </p>
        <div className="flex flex-wrap gap-2">
          {destino && seleccion && (
            <Button onClick={() => seleccion.solicitarCambio(destino)}>Cambiar a {NOMBRE_PAIS[destino]}</Button>
          )}
          <Button variant="outline" asChild>
            <Link to="/pedidos">Ver pedidos de {NOMBRE_PAIS[paisActivo]}</Link>
          </Button>
        </div>
      </section>
    );
  }
  const numeroElegido = elegida ?? detalle.etapaActual;
  const etapa =
    detalle.lineaDeTiempo.find((e) => e.numero === numeroElegido) ??
    detalle.lineaDeTiempo.find((e) => e.estado === 'ACTUAL') ??
    detalle.lineaDeTiempo[0];
  const actual = detalle.lineaDeTiempo.find((e) => e.numero === detalle.etapaActual);
  const automaticas = new Set((flujo.data?.etapas ?? []).filter((e) => e.automatica).map((e) => e.numero));

  const personas = new Map<string, Persona>();
  for (const conocida of [
    detalle.creadoPor,
    detalle.responsableActual,
    ...detalle.lineaDeTiempo.map((e) => e.asignadoA),
  ]) {
    if (conocida) personas.set(conocida.id, conocida);
  }

  const nombreActual = flujo.data?.etapas.find((e) => e.numero === detalle.etapaActual)?.nombre;
  const conLlegada = flujo.data?.etapas.some((e) => e.registraLlegada || e.acciones.some((a) => a.requiereBodega));
  const etapaLlegada = flujo.data?.etapas.find((e) => e.registraLlegada)?.numero;

  // Desde el cronograma: lleva el panel a esa etapa y la muestra.
  function irAEtapa(numero: number) {
    setElegida(numero);
    requestAnimationFrame(() => document.getElementById('linea-de-tiempo')?.scrollIntoView({ block: 'start' }));
  }

  // ADR-0029: el trabajo de la etapa primero (documentos vigentes y acciones), lo secundario después.
  return (
    <section className="flex flex-col gap-4">
      <div className="flex flex-wrap items-center justify-between gap-2">
        <Button variant="ghost" size="sm" asChild className="w-fit">
          <Link to="/pedidos">
            <ArrowLeftIcon data-icon="inline-start" />
            Pedidos
          </Link>
        </Button>
        <div className="flex flex-wrap items-center gap-2">
          {/* Mejora visual del 2026-10-27: el historial se abre bajo demanda en un panel lateral. */}
          <PanelHistorial pedidoId={detalle.id} numeroPedido={detalle.numero} tipoFlujo={detalle.tipoFlujo} personas={personas} bodega={detalle.bodega} />
          {/* Paso 7.5: acceso al asistente para los roles de F7-D4; el servidor vuelve a decidirlo. */}
          {puedeUsarAsistente(usuario.roles) && (
            <Button variant="outline" size="sm" asChild className="w-fit">
              <Link to={`/pedidos/${encodeURIComponent(detalle.id)}/asistente`}>
                <BotIcon data-icon="inline-start" />
                Preguntar al asistente
              </Link>
            </Button>
          )}
        </div>
      </div>

      <header className="flex flex-col gap-2 rounded-lg border bg-card p-4">
        <div className="flex flex-wrap items-center gap-x-3 gap-y-1.5">
          <h1 className="font-heading text-2xl font-bold text-primary tabular-nums">{detalle.numero}</h1>
          <Badge variant="outline">{nombreFlujo(detalle.tipoFlujo)}</Badge>
          {esPais(detalle.pais) && <Badge variant="outline">{NOMBRE_PAIS[detalle.pais]}</Badge>}
          <Badge variant={detalle.finalizado ? 'secondary' : 'default'}>{detalle.finalizado ? 'Finalizado' : 'En curso'}</Badge>
          <DistintivoVencimiento venceEn={detalle.venceEn} vencida={detalle.vencida} finalizado={detalle.finalizado} estadoPlazo={actual?.estadoPlazo} />
          {detalle.urgencia && (
            <DistintivoUrgencia nivel={detalle.urgencia.nivel} venceEn={detalle.urgencia.venceEn} vencida={detalle.urgencia.vencida} />
          )}
          {pedido.isFetching && (
            <span role="status" className="text-xs text-muted-foreground">Actualizando…</span>
          )}
        </div>
        <p className="font-medium">{detalle.nombre}</p>
        {detalle.descripcion && (
          <p className="line-clamp-3 max-w-prose whitespace-pre-wrap text-sm text-muted-foreground">{detalle.descripcion}</p>
        )}
        <dl className="flex flex-wrap gap-x-5 gap-y-1 text-sm">
          {!detalle.finalizado && (
            <div className="flex gap-1"><dt className="text-muted-foreground">Etapa actual:</dt>
              <dd className="font-medium">{codigoEtapa(detalle.tipoFlujo, detalle.etapaActual)}{nombreActual ? ` · ${nombreActual}` : ''}</dd></div>
          )}
          {!detalle.finalizado && (
            <div className="flex gap-1"><dt className="text-muted-foreground">Responsable:</dt>
              <dd className="font-medium">{detalle.responsableActual?.nombre ?? 'Sin asignar (según el rol de la etapa)'}</dd></div>
          )}
          <div className="flex gap-1"><dt className="text-muted-foreground">Creado:</dt>
            <dd><span className="tabular-nums">{formatearFechaHora(detalle.creadoEn)}</span> por {detalle.creadoPor.nombre}</dd></div>
          {detalle.incluyeProductosNuevos && <div><dd>Incluye productos nuevos</dd></div>}
        </dl>
        {detalle.demoraAduana && !detalle.finalizado && (
          <p className="rounded-md bg-estado-aviso-fondo px-3 py-2 text-sm text-estado-aviso">
            Demora reportada · nueva llegada estimada {formatearFecha(detalle.demoraAduana.nuevaFechaEstimadaLlegada)}:{' '}
            <span className="text-foreground">{detalle.demoraAduana.motivo}</span>
          </p>
        )}
      </header>

      {flujo.isError && (
        <Alert>
          <AlertDescription>
            No se pudo cargar la definición del flujo; algunos nombres de etapa pueden faltar. Las acciones siguen
            disponibles y se comprueban al enviarlas.
          </AlertDescription>
        </Alert>
      )}

      <div id="linea-de-tiempo" className="scroll-mt-4">
      <LineaDeTiempo
        numeroPedido={detalle.numero}
        tipoFlujo={detalle.tipoFlujo}
        etapas={detalle.lineaDeTiempo}
        seleccionada={etapa.numero}
        automaticas={automaticas}
        onSeleccionar={setElegida}
      />
      </div>

      <BorradoresEnConflicto pedido={detalle} />

      <div className="grid grid-cols-[minmax(0,1fr)] items-start gap-4 xl:grid-cols-[minmax(0,1fr)_minmax(0,1fr)]">
        <DocumentosPedido
          pedidoId={detalle.id}
          tipoFlujo={detalle.tipoFlujo}
          etapaActual={detalle.etapaActual}
          finalizado={detalle.finalizado}
          nombreEtapa={(numero) => flujo.data?.etapas.find((e) => e.numero === numero)?.nombre}
        />
        <div className="flex flex-col gap-4 xl:sticky xl:top-4">
          <PanelEtapa
            key={`${etapa.numero}-${etapa.entradaId ?? 'sin-entrada'}`}
            pedido={detalle}
            etapa={etapa}
            flujo={flujo.data}
            usuario={usuario}
            onAccion={(elegidaAccion, adjuntos) =>
              setAccion({ accion: elegidaAccion, etapaEsperada: detalle.etapaActual, adjuntos })
            }
            onReasignar={() => setReasignando(true)}
          />
          {conLlegada && (
            <section aria-label="Llegada y bodega" className="rounded-lg border bg-card px-4 py-3">
              <dl className="grid gap-2 text-sm sm:grid-cols-2">
                <div><dt className="text-xs text-muted-foreground">Llegada a aduana</dt>
                  <dd className="font-medium tabular-nums">{detalle.llegadaAduanaEn ? formatearFechaHora(detalle.llegadaAduanaEn) : 'Sin llegada confirmada'}</dd></div>
                <div><dt className="text-xs text-muted-foreground">Bodega</dt>
                  <dd className="font-medium">{detalle.bodega ? `${detalle.bodega.nombre} · ${ciudadDeBodega(detalle.bodega)}${detalle.bodega.activa ? '' : ' · inactiva para nuevas asignaciones'}` : 'Sin bodega asignada'}</dd></div>
              </dl>
              {detalle.demoraAduana && (
                <p className="mt-2 text-xs text-muted-foreground">
                  Demora reportada por {detalle.demoraAduana.reportadaPor.nombre} el {formatearFechaHora(detalle.demoraAduana.reportadaEn)}. La estimación no sustituye el plazo pendiente.
                </p>
              )}
            </section>
          )}
        </div>
      </div>

      {/* Richard (2026-10-27): el recuadro «Carpeta de Drive del pedido» ya no se muestra; la carpeta se sigue creando
          en el servidor al crear el pedido (ADR-0027). */}
      <CronogramaPedido pedido={detalle} etapaLlegada={etapaLlegada} onIrAEtapa={irAEtapa} />


      {/* La acción queda fija mientras el diálogo está abierto: si el servidor responde 409, el aviso se ve aunque el
          pedido ya muestre otra etapa. */}
      {accion && (
        <DialogoAccion
          pedido={detalle}
          accion={accion.accion}
          etapaEsperada={accion.etapaEsperada}
          adjuntos={accion.adjuntos}
          registraLlegada={accion.accion.accion === 'APROBAR' && Boolean(flujo.data?.etapas.find((e) => e.numero === accion.etapaEsperada)?.registraLlegada)}
          destinoAutomatico={accion.accion.etapaDestino !== null && automaticas.has(accion.accion.etapaDestino)}
          onCerrar={() => setAccion(null)}
        />
      )}
      {reasignando && actual && (
        <DialogoReasignar pedido={detalle} etapa={actual} onCerrar={() => setReasignando(false)} />
      )}
    </section>
  );
}

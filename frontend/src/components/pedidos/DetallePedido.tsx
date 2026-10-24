import { ArrowLeftIcon, BotIcon, RotateCwIcon } from 'lucide-react';
import { useState } from 'react';
import { Link, useParams } from 'react-router';
import { usePausarActualizacion } from '@/api/actualizacion';
import { puedeUsarAsistente } from '@/api/asistente';
import { esErrorApi } from '@/api/cliente';
import { type AccionDisponible, type Adjunto, type Persona, useDefinicionFlujo, usePedido } from '@/api/pedidos';
import { useUsuarioActual } from '@/auth/SesionProvider';
import { Alert, AlertDescription } from '@/components/ui/alert';
import { Badge } from '@/components/ui/badge';
import { Button } from '@/components/ui/button';
import { Skeleton } from '@/components/ui/skeleton';
import { BorradoresEnConflicto, ProveedorBorradores } from './borradores';
import { CronogramaPedido } from './CronogramaPedido';
import { DialogoAccion } from './DialogoAccion';
import { DialogoReasignar } from './DialogoReasignar';
import { DistintivoUrgencia, DistintivoVencimiento } from './Distintivos';
import { formatearFecha, formatearFechaHora, mensajeDeErrorPedido, nombreFlujo } from './formato';
import { HistorialPedido } from './HistorialPedido';
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

  return (
    <section className="flex flex-col gap-6">
      <div className="flex flex-wrap items-center justify-between gap-2">
        <Button variant="ghost" asChild className="w-fit">
          <Link to="/pedidos">
            <ArrowLeftIcon data-icon="inline-start" />
            Pedidos
          </Link>
        </Button>
        {/* Paso 7.5: acceso al asistente para los roles de F7-D4; el servidor vuelve a decidirlo. */}
        {puedeUsarAsistente(usuario.roles) && (
          <Button variant="outline" asChild className="w-fit">
            <Link to={`/pedidos/${encodeURIComponent(detalle.id)}/asistente`}>
              <BotIcon data-icon="inline-start" />
              Preguntar al asistente
            </Link>
          </Button>
        )}
      </div>

      <header className="flex flex-col gap-3">
        <div className="flex flex-wrap items-center gap-x-4 gap-y-2">
          <h1 className="font-heading text-3xl font-bold text-primary tabular-nums">{detalle.numero}</h1>
          <Badge variant="outline" className="h-auto rounded-sm py-0.5 text-sm">
            {nombreFlujo(detalle.tipoFlujo)}
          </Badge>
          <DistintivoVencimiento venceEn={detalle.venceEn} vencida={detalle.vencida} finalizado={detalle.finalizado} estadoPlazo={actual?.estadoPlazo} />
          {detalle.urgencia && (
            <DistintivoUrgencia
              nivel={detalle.urgencia.nivel}
              venceEn={detalle.urgencia.venceEn}
              vencida={detalle.urgencia.vencida}
            />
          )}
          {pedido.isFetching && (
            <span role="status" className="text-sm text-muted-foreground">
              Actualizando…
            </span>
          )}
        </div>
        <p className="text-lg font-medium">{detalle.nombre}</p>
        {detalle.descripcion && (
          <p className="max-w-prose whitespace-pre-wrap text-muted-foreground">{detalle.descripcion}</p>
        )}
        <p className="text-sm text-muted-foreground">
          Creado por {detalle.creadoPor.nombre} el{' '}
          <span className="tabular-nums">{formatearFechaHora(detalle.creadoEn)}</span>
          {detalle.incluyeProductosNuevos ? ' · incluye productos nuevos' : ''} · versión {detalle.versionFlujo} del flujo
        </p>
      </header>

      {/* Fase 6: la sección existe si la definición del flujo del pedido tiene etapa de llegada o elige bodega. */}
      {flujo.data?.etapas.some((e) => e.registraLlegada || e.acciones.some((a) => a.requiereBodega)) && <section aria-label="Llegada y bodega" className="flex flex-col gap-4 rounded-lg border bg-card p-4 sm:p-6">
        <dl className="grid gap-4 text-sm sm:grid-cols-2">
          <div className="flex flex-col gap-1"><dt className="text-muted-foreground">Llegada a aduana confirmada</dt>
            <dd className="font-medium tabular-nums">{detalle.llegadaAduanaEn ? formatearFechaHora(detalle.llegadaAduanaEn) : 'Sin llegada confirmada'}</dd></div>
          <div className="flex flex-col gap-1"><dt className="text-muted-foreground">Bodega asignada</dt>
            <dd className="font-medium">{detalle.bodega ? `${detalle.bodega.nombre} · ${ciudadDeBodega(detalle.bodega)}${detalle.bodega.activa ? '' : ' · inactiva para nuevas asignaciones'}` : 'Sin bodega asignada'}</dd></div>
        </dl>
        {detalle.demoraAduana && <Alert role="note"><AlertDescription className="flex flex-col gap-2">
          <p className="font-medium">Última demora · llegada estimada {formatearFecha(detalle.demoraAduana.nuevaFechaEstimadaLlegada)}</p>
          <p className="max-w-prose whitespace-pre-wrap">{detalle.demoraAduana.motivo}</p>
          <p className="text-sm">Reportada por {detalle.demoraAduana.reportadaPor.nombre} · {formatearFechaHora(detalle.demoraAduana.reportadaEn)} (Ecuador). La estimación no sustituye el plazo pendiente.</p>
        </AlertDescription></Alert>}
        {detalle.finalizado && <Alert role="note"><AlertDescription>Pedido finalizado por el servidor. Fin es automático y no admite acciones manuales.</AlertDescription></Alert>}
      </section>}

      {flujo.isError && (
        <Alert>
          <AlertDescription>
            No se pudo cargar la definición del flujo: los botones de descarga y los íconos de etapa pueden quedar
            incompletos. El servidor sigue decidiendo cada operación.
          </AlertDescription>
        </Alert>
      )}

      <LineaDeTiempo
        numeroPedido={detalle.numero}
        tipoFlujo={detalle.tipoFlujo}
        etapas={detalle.lineaDeTiempo}
        seleccionada={etapa.numero}
        automaticas={automaticas}
        onSeleccionar={setElegida}
      />

      <BorradoresEnConflicto pedido={detalle} />

      <CronogramaPedido pedidoId={detalle.id} tipoFlujo={detalle.tipoFlujo} />

      <div className="grid items-start gap-6 xl:grid-cols-[minmax(0,1fr)_26rem]">
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
        <aside className="rounded-lg border bg-card p-6 shadow-sm">
          <HistorialPedido pedidoId={detalle.id} tipoFlujo={detalle.tipoFlujo} personas={personas} bodega={detalle.bodega} />
        </aside>
      </div>

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

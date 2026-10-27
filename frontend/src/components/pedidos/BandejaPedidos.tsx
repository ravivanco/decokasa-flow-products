import { AlarmClockIcon, BellDotIcon, InboxIcon } from 'lucide-react';
import { useEffect, useState } from 'react';
import { Link, useLocation } from 'react-router';
import { useAvisos } from '@/api/avisos';
import { useAlertas } from '@/api/estadisticas';
import { type PedidoResumen, useBandeja, useConteosBandeja, useDefinicionesVigentes } from '@/api/pedidos';
import { cn } from '@/lib/utils';
import { esAdmin, useUsuarioActual } from '@/auth/SesionProvider';
import { Button } from '@/components/ui/button';
import { Skeleton } from '@/components/ui/skeleton';
import { Alert, AlertDescription, AlertTitle } from '@/components/ui/alert';
import { AvisosDeEtapa } from '@/components/dashboards/AvisosDeEtapa';
import { MisAlertas } from '@/components/dashboards/MisAlertas';
import { PanelVencidos } from '@/components/dashboards/PanelVencidos';
import { ProductosPropuestos } from '@/components/dashboards/ProductosPropuestos';
import { TableroTrabajo } from '@/components/dashboards/TableroTrabajo';
import { panelesDe } from '@/components/dashboards/DashboardRol';
import { codigoEtapa, mensajeDeErrorPedido, nombreRol } from './formato';
import { Paginacion, TablaPedidos } from './TablaPedidos';

// Bandeja de trabajo (P-06; requisitos 4.2 #2 y 4.3): lo que la persona puede atender ahora, agrupado por etapa. Así
// Compras China ve Análisis, Fabricación, Embarque y Aduana; Logística, Salida, Bodega e Incidencias. Con varios
// roles se ven varias secciones, y el admin ve todo lo que está en curso. El servidor decide qué entra en la bandeja.
// Fase 6: los grupos son por flujo y etapa (A3 y B3 no se mezclan) y cada uno usa la definición de su flujo; B2 es la
// bandeja de productos propuestos de Revisión.

// ADR-0029 (aclaración del 2026-10-26): Mi bandeja reúne lo operativo que antes estaba en el Inicio: alertas y
// retrasos (destino de la campana, #alertas), «Mi trabajo» con sus pestañas, los productos propuestos, los vencidos del
// admin y, al final, la bandeja por etapa. Nada cambia de permisos: cada bloque pide sus datos al servidor.
export function BandejaPedidos() {
  const usuario = useUsuarioActual();
  const admin = esAdmin(usuario);
  const conTrabajo = admin || panelesDe(usuario.roles).some((panel) => !panel.provisional);
  const { hash } = useLocation();
  useEffect(() => {
    // Destinos de la campana y del resumen: avisos de etapa (#avisos), retrasos y demoras (#alertas) o el trabajo.
    if (hash !== '#alertas' && hash !== '#avisos' && hash !== '#trabajo') return;
    const destino = document.getElementById(hash.slice(1));
    destino?.scrollIntoView({ block: 'start' });
    destino?.focus({ preventScroll: true });
  }, [hash]);
  const [pagina, setPagina] = useState(0);
  // La agrupación por etapa repite los pedidos de «Mi trabajo»: se abre a petición. Sin «Mi trabajo» (roles de
  // consulta) se muestra siempre.
  const [verPorEtapa, setVerPorEtapa] = useState(false);
  const bandeja = useBandeja(pagina);
  const definiciones = useDefinicionesVigentes();

  const grupos = agrupar(bandeja.data?.elementos ?? []);
  const definicionDe = (tipo: 'A' | 'B', numero: number) =>
    definiciones[tipo]?.etapas.find((etapa) => etapa.numero === numero);
  const documentacionPendiente = (['A', 'B'] as const).flatMap((tipo) =>
    (definiciones[tipo]?.etapas ?? [])
      .filter((etapa) =>
        (esAdmin(usuario) || (etapa.rolResponsable !== null && usuario.roles.includes(etapa.rolResponsable)))
        && etapa.acciones.some((accion) => accion.requisitosDocumentales === 'PENDIENTES'))
      .map((etapa) => `${codigoEtapa(tipo, etapa.numero)} · ${etapa.nombre}`));

  return (
    <section className="flex flex-col gap-6">
      <div className="flex flex-col gap-1">
        <h1 className="font-heading text-2xl font-bold text-primary">Mi bandeja</h1>
      </div>

      <ResumenBandeja conTrabajo={conTrabajo} />
      <div className="grid items-start gap-4 xl:grid-cols-2">
        <AvisosDeEtapa />
        <MisAlertas />
      </div>
      {conTrabajo && <TableroTrabajo admin={admin} />}
      <ProductosPropuestos />
      {admin && <PanelVencidos />}

      <section aria-labelledby="bandeja-por-etapa" className="flex flex-col gap-4">
      <div className="flex flex-wrap items-center justify-between gap-2">
        <h2 id="bandeja-por-etapa" className="font-heading text-xl font-semibold">Pedidos por etapa</h2>
        {conTrabajo && (
          <Button variant="outline" size="sm" aria-expanded={verPorEtapa} aria-controls="bandeja-por-etapa-contenido" onClick={() => setVerPorEtapa((v) => !v)}>
            {verPorEtapa ? 'Ocultar la agrupación' : `Agrupar por etapa${grupos.length ? ` (${grupos.length} ${grupos.length === 1 ? 'grupo' : 'grupos'})` : ''}`}
          </Button>
        )}
      </div>
      {conTrabajo && !verPorEtapa ? (
        <p className="text-sm text-muted-foreground">Los mismos pedidos de «{admin ? 'Pedidos en curso' : 'Mi trabajo'}», agrupados por la etapa en la que están.</p>
      ) : (
      <div id="bandeja-por-etapa-contenido" className="flex flex-col gap-4">
      {documentacionPendiente.length > 0 && <Alert role="note">
        <AlertTitle>Requisitos documentales pendientes en la definición vigente</AlertTitle>
        <AlertDescription>{documentacionPendiente.join('; ')}.
          {' '}Estas etapas no pueden confirmarse hasta definir sus documentos. Los permisos y el estado de cada pedido los decide el servidor.
        </AlertDescription>
      </Alert>}

      {bandeja.isPending ? (
        <div className="flex flex-col gap-3" aria-busy="true" aria-label="Cargando la bandeja">
          <Skeleton className="h-6 w-48" />
          <Skeleton className="h-40 w-full" />
        </div>
      ) : bandeja.isError ? (
        <TablaPedidos
          titulo="Bandeja"
          pedidos={undefined}
          cargando={false}
          error={mensajeDeErrorPedido(bandeja.error, 'No se pudo cargar tu bandeja.')}
          onReintentar={() => void bandeja.refetch()}
          vacio={null}
        />
      ) : grupos.length === 0 ? (
        <div className="flex flex-col items-center gap-3 rounded-lg border border-dashed bg-card px-6 py-12 text-center">
          <InboxIcon aria-hidden="true" className="size-8 text-muted-foreground" />
          <p className="font-medium">No tienes pedidos pendientes.</p>
          <Button variant="outline" asChild>
            <Link to="/pedidos">Ver todos los pedidos</Link>
          </Button>
        </div>
      ) : (
        grupos.map(([clave, pedidos]) => {
          const { tipoFlujo, etapaActual: numero } = pedidos[0];
          const definicion = definicionDe(tipoFlujo, numero);
          const encabezado = `${codigoEtapa(tipoFlujo, numero)} · ${pedidos[0].nombreEtapaActual}`;
          return (
            <section key={clave} aria-labelledby={`bandeja-etapa-${clave}`} className="flex flex-col gap-3">
              <div className="flex flex-wrap items-baseline gap-x-3 gap-y-1">
                <h2 id={`bandeja-etapa-${clave}`} className="font-heading text-xl font-semibold">
                  {encabezado}
                </h2>
                <span className="text-sm text-muted-foreground tabular-nums">
                  {pedidos.length} {pedidos.length === 1 ? 'pedido' : 'pedidos'}
                  {definicion?.rolResponsable ? ` · ${nombreRol(definicion.rolResponsable)}` : ''}
                </span>
              </div>
              <TablaPedidos
                titulo={`Pedidos en ${encabezado}`}
                pedidos={pedidos}
                cargando={false}
                error={null}
                onReintentar={() => void bandeja.refetch()}
                vacio={null}
                recorrido={{ A: definiciones.A?.etapas, B: definiciones.B?.etapas }}
              />
            </section>
          );
        })
      )}

      {bandeja.data && (
        <Paginacion
          pagina={bandeja.data.pagina}
          tamano={bandeja.data.tamano}
          total={bandeja.data.total}
          ocupado={bandeja.isFetching}
          onCambiar={setPagina}
        />
      )}
      </div>
      )}
      </section>
    </section>
  );
}

/**
 * Resumen de la bandeja: tres cosas distintas, con su icono, su texto y su cantidad (nunca solo color), cada una con
 * enlace a su sección. Usa las mismas consultas que las secciones (TanStack Query las comparte; no hay peticiones
 * nuevas).
 */
function ResumenBandeja({ conTrabajo }: { conTrabajo: boolean }) {
  const avisos = useAvisos();
  const alertas = useAlertas('MIAS');
  const conteos = useConteosBandeja();
  const nuevos = avisos.data?.noLeidos;
  const retrasos = alertas.data?.length;
  const vencidos = conteos.vencidos ?? 0;
  const urgentes = conteos.conUrgencia ?? 0;
  const enlace = 'group flex min-w-0 items-start gap-3 px-4 py-3 outline-none hover:bg-morado-50 focus-visible:ring-3 focus-visible:ring-ring focus-visible:ring-inset';
  return (
    <nav aria-label="Resumen de mi bandeja" className="grid overflow-hidden rounded-lg border bg-card shadow-sm sm:grid-cols-3 [&>a+a]:border-t sm:[&>a+a]:border-t-0 sm:[&>a+a]:border-l">
      <a href="#avisos" className={enlace}>
        <BellDotIcon aria-hidden="true" className={cn('mt-0.5 size-5 shrink-0', nuevos ? 'text-primary' : 'text-muted-foreground')} />
        <span className="flex min-w-0 flex-col">
          <span className="text-sm font-medium">Avisos de etapa</span>
          <span className="text-sm text-muted-foreground">
            {nuevos === undefined ? '…' : nuevos > 0 ? <><b className="text-primary tabular-nums">{nuevos}</b> {nuevos === 1 ? 'nuevo' : 'nuevos'} sin leer</> : 'Nada nuevo'}
          </span>
        </span>
      </a>
      <a href="#alertas" className={enlace}>
        <AlarmClockIcon aria-hidden="true" className={cn('mt-0.5 size-5 shrink-0', retrasos ? 'text-estado-vencido' : 'text-muted-foreground')} />
        <span className="flex min-w-0 flex-col">
          <span className="text-sm font-medium">Retrasos y demoras</span>
          <span className="text-sm text-muted-foreground">
            {retrasos === undefined ? '…' : retrasos > 0 ? <><b className="text-estado-vencido tabular-nums">{retrasos}</b> {retrasos === 1 ? 'requiere' : 'requieren'} atención</> : 'Ninguno a tu cargo'}
          </span>
        </span>
      </a>
      {conTrabajo ? (
        <a href="#trabajo" className={enlace}>
          <InboxIcon aria-hidden="true" className="mt-0.5 size-5 shrink-0 text-muted-foreground" />
          <span className="flex min-w-0 flex-col">
            <span className="text-sm font-medium">Pedidos pendientes</span>
            <span className="text-sm text-muted-foreground">
              {conteos.pendientes === undefined ? '…' : <><b className="text-foreground tabular-nums">{conteos.pendientes}</b> en tu bandeja</>}
              {vencidos > 0 && <> · <span className="text-estado-vencido">{vencidos} con plazo vencido</span></>}
              {urgentes > 0 && <> · <span className="text-marca-naranja-texto">{urgentes} con urgencia</span></>}
            </span>
          </span>
        </a>
      ) : (
        <div className="flex items-start gap-3 px-4 py-3 text-sm text-muted-foreground">
          <InboxIcon aria-hidden="true" className="mt-0.5 size-5 shrink-0" />
          Tu rol es de consulta: no tienes pedidos asignados.
        </div>
      )}
    </nav>
  );
}

/**
 * Agrupa por flujo y etapa actual (A antes que B, y en el orden de cada flujo). Solo ordena lo que devolvió el
 * servidor; no filtra nada.
 */
function agrupar(pedidos: PedidoResumen[]): [string, PedidoResumen[]][] {
  const grupos = new Map<string, PedidoResumen[]>();
  for (const pedido of pedidos) {
    const clave = codigoEtapa(pedido.tipoFlujo, pedido.etapaActual);
    grupos.set(clave, [...(grupos.get(clave) ?? []), pedido]);
  }
  return [...grupos.entries()].sort(([, [a]], [, [b]]) =>
    a.tipoFlujo === b.tipoFlujo ? a.etapaActual - b.etapaActual : a.tipoFlujo.localeCompare(b.tipoFlujo));
}

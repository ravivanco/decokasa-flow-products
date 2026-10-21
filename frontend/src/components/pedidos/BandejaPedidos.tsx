import { InboxIcon } from 'lucide-react';
import { useState } from 'react';
import { Link } from 'react-router';
import { type PedidoResumen, useBandeja, useDefinicionesVigentes } from '@/api/pedidos';
import { esAdmin, useUsuarioActual } from '@/auth/SesionProvider';
import { Button } from '@/components/ui/button';
import { Skeleton } from '@/components/ui/skeleton';
import { Alert, AlertDescription, AlertTitle } from '@/components/ui/alert';
import { codigoEtapa, mensajeDeErrorPedido, nombreRol } from './formato';
import { Paginacion, TablaPedidos } from './TablaPedidos';

// Bandeja de trabajo (P-06; requisitos 4.2 #2 y 4.3): lo que la persona puede atender ahora, agrupado por etapa. Así
// Compras China ve Análisis, Fabricación, Embarque y Aduana; Logística, Salida, Bodega e Incidencias. Con varios
// roles se ven varias secciones, y el admin ve todo lo que está en curso. El servidor decide qué entra en la bandeja.
// Fase 6: los grupos son por flujo y etapa (A3 y B3 no se mezclan) y cada uno usa la definición de su flujo; B2 es la
// bandeja de productos propuestos de Revisión.

export function BandejaPedidos() {
  const usuario = useUsuarioActual();
  const [pagina, setPagina] = useState(0);
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
        <p className="text-muted-foreground">
          {esAdmin(usuario)
            ? 'Como administrador ves todos los pedidos en curso.'
            : 'Pedidos cuya etapa actual te corresponde: asignada a ti o, sin persona asignada, a uno de tus roles.'}
        </p>
      </div>

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
    </section>
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

import { InboxIcon } from 'lucide-react';
import { useState } from 'react';
import { Link } from 'react-router';
import { type PedidoResumen, useBandeja, useDefinicionFlujo } from '@/api/pedidos';
import { esAdmin, useUsuarioActual } from '@/auth/SesionProvider';
import { Button } from '@/components/ui/button';
import { Skeleton } from '@/components/ui/skeleton';
import { codigoEtapa, mensajeDeErrorPedido, nombreRol } from './formato';
import { Paginacion, TablaPedidos } from './TablaPedidos';

// Bandeja de trabajo (P-06; requisitos 4.2 #2 y 4.3): lo que la persona puede atender ahora, agrupado por etapa. Así
// Marketing ve Codificación (A2), Asistente de compras ve Revisión (A3) y Compras China ve Análisis (A4); con varios
// roles se ven varias secciones, y el admin ve todo lo que está en curso. El servidor decide qué entra en la bandeja.

export function BandejaPedidos() {
  const usuario = useUsuarioActual();
  const [pagina, setPagina] = useState(0);
  const bandeja = useBandeja(pagina);
  const flujo = useDefinicionFlujo('EC', 'A', undefined);

  const grupos = agrupar(bandeja.data?.elementos ?? []);
  const definicionDe = (numero: number) => flujo.data?.etapas.find((etapa) => etapa.numero === numero);

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
        grupos.map(([numero, pedidos]) => {
          const definicion = definicionDe(numero);
          const encabezado = `${codigoEtapa('A', numero)} · ${definicion?.nombre ?? pedidos[0].nombreEtapaActual}`;
          return (
            <section key={numero} aria-labelledby={`bandeja-etapa-${numero}`} className="flex flex-col gap-3">
              <div className="flex flex-wrap items-baseline gap-x-3 gap-y-1">
                <h2 id={`bandeja-etapa-${numero}`} className="font-heading text-xl font-semibold">
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

/** Agrupa por etapa actual, en el orden del flujo. Solo ordena lo que devolvió el servidor; no filtra nada. */
function agrupar(pedidos: PedidoResumen[]): [number, PedidoResumen[]][] {
  const grupos = new Map<number, PedidoResumen[]>();
  for (const pedido of pedidos) {
    grupos.set(pedido.etapaActual, [...(grupos.get(pedido.etapaActual) ?? []), pedido]);
  }
  return [...grupos.entries()].sort(([a], [b]) => a - b);
}

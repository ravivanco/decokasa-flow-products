import { PackageSearchIcon } from 'lucide-react';
import { useId, useState } from 'react';
import { Link } from 'react-router';
import { useDefinicionesVigentes, usePedidos } from '@/api/pedidos';
import { esAdmin, useUsuarioActual } from '@/auth/SesionProvider';
import { Button } from '@/components/ui/button';
import { codigoEtapa, mensajeDeErrorPedido } from '@/components/pedidos/formato';
import { Paginacion, TablaPedidos } from '@/components/pedidos/TablaPedidos';

// "Productos propuestos" para Revisión (fase 6, paso 6.5): los pedidos del flujo B que esperan en la etapa que revisa
// la propuesta de China. La etapa sale de la definición vigente (la que revisa a la primera: B2 revisa B1, V18), no de
// un número fijo. Lo ve quien tiene el rol de esa etapa o el admin; el servidor decide las acciones y la descarga de
// los documentos de B1 según P-06 y P-17 (con persona asignada, solo ella o el admin).

const TAMANO = 10;

export function ProductosPropuestos() {
  const ids = useId();
  const usuario = useUsuarioActual();
  const [pagina, setPagina] = useState(0);
  const { B: flujoB } = useDefinicionesVigentes();
  const primera = flujoB?.etapas[0];
  const revisora = flujoB?.etapas.find((etapa) => primera !== undefined && etapa.revisaEtapa === primera.numero);
  const visible =
    revisora?.rolResponsable != null && (esAdmin(usuario) || usuario.roles.includes(revisora.rolResponsable));
  const pedidos = usePedidos({ tipoFlujo: 'B', etapa: revisora?.numero, pagina, tamano: TAMANO }, visible);

  if (!visible || !revisora || !primera) return null;
  const codigo = codigoEtapa('B', revisora.numero);
  const codigoPrimera = codigoEtapa('B', primera.numero);

  return (
    <section aria-labelledby={`${ids}-titulo`} className="flex flex-col gap-4">
      <div className="flex flex-wrap items-end justify-between gap-3">
        <div className="flex flex-col gap-1">
          <h2 id={`${ids}-titulo`} className="flex items-center gap-2 font-heading text-xl font-semibold">
            <PackageSearchIcon aria-hidden="true" className="size-5" />
            Productos propuestos
          </h2>
          <p className="max-w-prose text-sm text-muted-foreground">
            Pedidos que propone China, en {codigo} · {revisora.nombre}. Desde el pedido descargas lo que se subió en{' '}
            {codigoPrimera} y lo apruebas o lo devuelves a {codigoPrimera} con una observación.
          </p>
        </div>
        <Button variant="outline" size="sm" asChild>
          <Link to={`/pedidos?flujo=B&etapa=${revisora.numero}`}>Ver en Pedidos</Link>
        </Button>
      </div>
      <TablaPedidos
        titulo={`Productos propuestos en ${codigo}`}
        pedidos={pedidos.isPlaceholderData ? undefined : pedidos.data?.elementos}
        cargando={pedidos.isPending || pedidos.isPlaceholderData}
        error={pedidos.isError ? mensajeDeErrorPedido(pedidos.error, 'No se pudieron cargar los productos propuestos.') : null}
        onReintentar={() => void pedidos.refetch()}
        vacio={<p className="text-muted-foreground">No hay productos propuestos esperando revisión.</p>}
        recorrido={{ B: flujoB?.etapas }}
      />
      {pedidos.data && !pedidos.isPlaceholderData && (
        <Paginacion
          pagina={pedidos.data.pagina}
          tamano={pedidos.data.tamano}
          total={pedidos.data.total}
          ocupado={pedidos.isFetching}
          onCambiar={setPagina}
        />
      )}
    </section>
  );
}

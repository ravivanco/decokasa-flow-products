import { CircleAlertIcon, RotateCwIcon } from 'lucide-react';
import { useId, useMemo, useState } from 'react';
import { Link } from 'react-router';
import { type PedidoResumen, usePedidos } from '@/api/pedidos';
import { Alert, AlertDescription } from '@/components/ui/alert';
import { Button } from '@/components/ui/button';
import { Skeleton } from '@/components/ui/skeleton';
import { Table, TableBody, TableCaption, TableCell, TableHead, TableHeader, TableRow } from '@/components/ui/table';
import { codigoEtapa, formatearFechaHora, mensajeDeErrorPedido } from '@/components/pedidos/formato';
import { Paginacion } from '@/components/pedidos/TablaPedidos';

// Panel de vencidos del admin (requisito 4.8; pasos 5.3 y 5.5): pedidos en curso con el plazo de la etapa actual
// vencido, por etapa y por responsable, con el motivo del retraso. El servidor filtra y pagina (GET
// /pedidos?vencido=true, P-13). El total es global; los grupos por etapa y por responsable cuentan solo los resultados
// cargados de la página, y así se dice: el panel no presenta una página como si fuera el conjunto completo. Las
// urgencias vencidas están en la pestaña "Con urgencia".

const TAMANO = 50;

export function PanelVencidos() {
  const ids = useId();
  const [pagina, setPagina] = useState(0);
  const vencidos = usePedidos({ vencido: true, pagina, tamano: TAMANO });
  const pedidos = useMemo(() => vencidos.data?.elementos ?? [], [vencidos.data]);
  const total = vencidos.data?.total ?? 0;
  const desde = pagina * TAMANO + 1;
  const hasta = pagina * TAMANO + pedidos.length;
  const porEtapa = useMemo(
    () => [...agrupar(pedidos, (pedido) => pedido.etapaActual)].sort((a, b) => a[0] - b[0]),
    [pedidos],
  );
  const porResponsable = useMemo(
    () =>
      [...agrupar(pedidos, (pedido) => pedido.responsableActual?.nombre ?? 'Sin persona asignada (por rol)')].sort(
        (a, b) => b[1].length - a[1].length,
      ),
    [pedidos],
  );

  return (
    <section aria-labelledby={`${ids}-titulo`} className="flex flex-col gap-4 rounded-lg border bg-card p-4 shadow-sm sm:p-6">
      <div className="flex flex-col gap-1">
        <h2 id={`${ids}-titulo`} className="flex items-center gap-2 font-heading text-xl font-semibold">
          <CircleAlertIcon aria-hidden="true" className="size-5 text-estado-vencido" />
          Vencidos por etapa y por responsable
        </h2>
        <p className="text-sm text-muted-foreground">
          Vista del admin. Alcance: los pedidos que el admin puede consultar (hoy, Ecuador). Plazo de la etapa actual
          vencido, con el motivo del retraso si se registró.
        </p>
      </div>

      {/* La página de resultados tiene viewport propio: carga, vacío y datos no desplazan las tarjetas siguientes. */}
      <div className="h-[32rem] overflow-auto rounded-md outline-none focus-visible:ring-3 focus-visible:ring-ring" role="region" aria-label="Resultados de vencidos" tabIndex={0}>
      {vencidos.isPending ? (
        <div className="flex flex-col gap-3" aria-busy="true" aria-label="Cargando los vencidos">
          <Skeleton className="h-6 w-56" />
          <Skeleton className="h-24 w-full" />
        </div>
      ) : vencidos.isError ? (
        <Alert variant="destructive">
          <AlertDescription className="flex flex-wrap items-center justify-between gap-3">
            <p>{mensajeDeErrorPedido(vencidos.error, 'No se pudieron cargar los vencidos.')}</p>
            <Button variant="outline" size="sm" onClick={() => void vencidos.refetch()}>
              <RotateCwIcon data-icon="inline-start" />
              Reintentar
            </Button>
          </AlertDescription>
        </Alert>
      ) : pedidos.length === 0 ? (
        <p className="text-muted-foreground">No hay pedidos con el plazo de la etapa vencido.</p>
      ) : (
        <>
          <dl className="flex flex-wrap gap-x-6 gap-y-1 text-sm" aria-live="polite">
            <div className="flex gap-1.5">
              <dt className="text-muted-foreground">Total global de vencidos:</dt>
              <dd className="font-semibold tabular-nums">{total}</dd>
            </div>
            <div className="flex gap-1.5">
              <dt className="text-muted-foreground">Resultados cargados:</dt>
              <dd className="font-semibold tabular-nums">
                {desde}–{hasta}
              </dd>
            </div>
          </dl>
          {total > pedidos.length && (
            <p role="note" className="text-sm">
              Los grupos por etapa y por responsable cuentan solo los resultados cargados, no el total. Cambia de página
              o usa{' '}
              <Link to="/pedidos?vencido=true" className="text-primary underline underline-offset-4">
                Pedidos, solo vencidos
              </Link>{' '}
              para ver el resto.
            </p>
          )}
          <div className="grid items-start gap-6 lg:grid-cols-[minmax(0,2fr)_minmax(0,1fr)]">
            <div className="flex flex-col gap-4">
              {porEtapa.map(([numero, lista]) => (
                <section key={numero} aria-labelledby={`${ids}-etapa-${numero}`} className="flex flex-col gap-2">
                  <h3 id={`${ids}-etapa-${numero}`} className="font-heading font-semibold">
                    <span className="tabular-nums">{codigoEtapa(lista[0].tipoFlujo, numero)}</span> ·{' '}
                    {lista[0].nombreEtapaActual}
                    <span className="font-normal text-muted-foreground tabular-nums">
                      {' '}
                      · {lista.length} {total > pedidos.length ? 'en los resultados cargados' : ''}
                    </span>
                  </h3>
                  <ul className="flex flex-col divide-y rounded-md border">
                    {lista.map((pedido) => (
                      <li key={pedido.id} className="flex flex-col gap-1 px-3 py-2 text-sm">
                        <div className="flex flex-wrap items-baseline gap-x-2">
                          <Link
                            to={`/pedidos/${pedido.id}`}
                            className="rounded-sm font-medium text-primary tabular-nums underline-offset-4 outline-none hover:underline focus-visible:ring-3 focus-visible:ring-ring"
                          >
                            {pedido.numero}
                          </Link>
                          <span className="truncate">{pedido.nombre}</span>
                        </div>
                        <p className="text-muted-foreground">
                          {pedido.responsableActual?.nombre ?? 'Sin persona asignada (por rol)'} · venció{' '}
                          <span className="tabular-nums">{formatearFechaHora(pedido.venceEn)}</span>
                        </p>
                        <p className="whitespace-pre-wrap">
                          <span className="text-muted-foreground">Motivo: </span>
                          {pedido.motivoRetraso ?? <span className="text-muted-foreground">Sin motivo registrado</span>}
                        </p>
                      </li>
                    ))}
                  </ul>
                </section>
              ))}
            </div>
            <div className="overflow-x-auto rounded-md border">
              <Table>
                <TableCaption className="caption-top px-3 pt-2 text-left text-xs">
                  Por responsable{total > pedidos.length ? ', en los resultados cargados' : ''}
                </TableCaption>
                <TableHeader>
                  <TableRow className="bg-secondary hover:bg-secondary">
                    <TableHead scope="col" className="px-3">Responsable</TableHead>
                    <TableHead scope="col" className="px-3 text-right">
                      {total > pedidos.length ? 'Cargados' : 'Vencidos'}
                    </TableHead>
                  </TableRow>
                </TableHeader>
                <TableBody>
                  {porResponsable.map(([responsable, lista]) => (
                    <TableRow key={responsable}>
                      <TableCell className="px-3">{responsable}</TableCell>
                      <TableCell className="px-3 text-right tabular-nums">{lista.length}</TableCell>
                    </TableRow>
                  ))}
                </TableBody>
              </Table>
            </div>
          </div>
          <Paginacion
            pagina={pagina}
            tamano={TAMANO}
            total={total}
            ocupado={vencidos.isFetching}
            onCambiar={setPagina}
          />
        </>
      )}
      </div>
    </section>
  );
}

function agrupar<K>(pedidos: PedidoResumen[], clave: (pedido: PedidoResumen) => K): Map<K, PedidoResumen[]> {
  const grupos = new Map<K, PedidoResumen[]>();
  for (const pedido of pedidos) {
    const llave = clave(pedido);
    grupos.set(llave, [...(grupos.get(llave) ?? []), pedido]);
  }
  return grupos;
}

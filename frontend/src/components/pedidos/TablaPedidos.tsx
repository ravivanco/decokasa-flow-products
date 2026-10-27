import { RotateCwIcon } from 'lucide-react';
import { type ReactNode, useSyncExternalStore } from 'react';
import { Link } from 'react-router';
import type { PedidoResumen } from '@/api/pedidos';
import { Alert, AlertDescription } from '@/components/ui/alert';
import { Badge } from '@/components/ui/badge';
import { Button } from '@/components/ui/button';
import { Skeleton } from '@/components/ui/skeleton';
import { Table, TableBody, TableCaption, TableCell, TableHead, TableHeader, TableRow } from '@/components/ui/table';
import { DistintivoUrgencia, DistintivoVencimiento } from './Distintivos';
import { codigoEtapa } from './formato';
import { type EtapaDeFlujo, LineaCompacta } from './LineaCompacta';

// Tabla de pedidos (DESIGN.md 7.1). Se entra al pedido por el enlace del número (no por un clic en la fila), la
// primera columna queda fija al desplazar en pantallas angostas y los estados de carga, vacío y error tienen su forma.
// Mejora visual del 2026-10-27: el vencimiento y la urgencia pueden partirse en dos líneas (nunca se recortan), la
// altura sigue al contenido (con tope y desplazamiento) y en pantallas angostas cada pedido es una tarjeta.

interface Props {
  titulo: string;
  /** Reserva el mismo viewport para carga, datos, vacío y error del dashboard. */
  alturaEstable?: boolean;
  pedidos: PedidoResumen[] | undefined;
  cargando: boolean;
  error: string | null;
  onReintentar: () => void;
  vacio: ReactNode;
  /**
   * Etapas de la definición vigente de cada tipo de flujo (paso 5.3). Si se pasa, la tabla agrega la columna
   * "Recorrido" con la línea compacta de cada pedido, sin pedir su detalle.
   */
  recorrido?: Partial<Record<string, EtapaDeFlujo[]>>;
}

const FLUJO: Record<string, string> = { A: 'A · Ecuador', B: 'B · China' };

// Por debajo de md (48rem) se muestran tarjetas; se pinta solo una de las dos vistas. Sin matchMedia, la tabla.
const ANGOSTA = '(max-width: 47.99rem)';
function suscribir(aviso: () => void) {
  const consulta = typeof window !== 'undefined' && window.matchMedia ? window.matchMedia(ANGOSTA) : null;
  consulta?.addEventListener('change', aviso);
  return () => consulta?.removeEventListener('change', aviso);
}
const esAngosta = () => (typeof window !== 'undefined' && window.matchMedia ? window.matchMedia(ANGOSTA).matches : false);
export function usePantallaAngosta() {
  return useSyncExternalStore(suscribir, esAngosta, () => false);
}

export function TablaPedidos({ titulo, pedidos, cargando, error, onReintentar, vacio, recorrido, alturaEstable = false }: Props) {
  const angosta = usePantallaAngosta();
  const marco = alturaEstable ? 'max-h-[32rem] min-h-32 overflow-auto rounded-lg border bg-card shadow-sm' : 'overflow-x-auto rounded-lg border bg-card shadow-sm';
  const columnas = recorrido ? 8 : 7;
  if (error) {
    return (
      <div className={alturaEstable ? marco : undefined}><Alert variant="destructive">
        <AlertDescription className="flex flex-wrap items-center justify-between gap-3">
          <p>{error}</p>
          <Button variant="outline" size="sm" onClick={onReintentar}>
            <RotateCwIcon data-icon="inline-start" />
            Reintentar
          </Button>
        </AlertDescription>
      </Alert></div>
    );
  }

  if (!cargando && pedidos?.length === 0) {
    return <div className="rounded-lg border border-dashed bg-card px-6 py-8 text-center">{vacio}</div>;
  }

  return (
    angosta ? <TarjetasPedidos titulo={titulo} pedidos={pedidos} cargando={cargando} recorrido={recorrido} /> : (
    <div className={marco} tabIndex={alturaEstable ? 0 : undefined} role={alturaEstable ? 'region' : undefined} aria-label={alturaEstable ? titulo : undefined}>
      <Table className={alturaEstable ? 'min-w-[68rem] table-fixed' : 'min-w-[56rem]'}>
        {alturaEstable && <colgroup>
          {(recorrido ? [12, 13, 8, 15, 15, 10, 17, 10] : [14, 17, 9, 17, 12, 20, 11]).map((ancho, i) => <col key={i} style={{ width: `${ancho}%` }} />)}
        </colgroup>}
        <TableCaption className="sr-only">{titulo}</TableCaption>
        <TableHeader>
          <TableRow className="bg-secondary hover:bg-secondary">
            <TableHead scope="col" className="sticky left-0 z-10 bg-secondary px-3">
              Número
            </TableHead>
            <TableHead scope="col" className="px-3">
              Pedido
            </TableHead>
            <TableHead scope="col" className="px-3">
              Flujo
            </TableHead>
            <TableHead scope="col" className="px-3">
              Etapa actual
            </TableHead>
            {recorrido && (
              <TableHead scope="col" className="px-3">
                Recorrido
              </TableHead>
            )}
            <TableHead scope="col" className="px-3">
              Responsable
            </TableHead>
            <TableHead scope="col" className="px-3">
              Vence
            </TableHead>
            <TableHead scope="col" className="px-3">
              Urgencia
            </TableHead>
          </TableRow>
        </TableHeader>
        <TableBody>
          {cargando && !pedidos
            ? Array.from({ length: 4 }, (_, fila) => (
                <TableRow key={fila} aria-hidden="true">
                  {Array.from({ length: columnas }, (_, columna) => (
                    <TableCell key={columna} className="h-11 px-3">
                      <Skeleton className="h-4 w-full max-w-36" />
                    </TableCell>
                  ))}
                </TableRow>
              ))
            : pedidos?.map((pedido) => (
                <TableRow key={pedido.id} className="hover:bg-morado-50">
                  <TableCell className="sticky left-0 z-10 h-11 bg-card px-3">
                    <Link
                      to={`/pedidos/${pedido.id}`}
                      className="rounded-sm font-medium text-primary tabular-nums underline-offset-4 outline-none hover:underline focus-visible:ring-3 focus-visible:ring-ring"
                    >
                      {pedido.numero}
                    </Link>
                  </TableCell>
                  <TableCell className="max-w-64 px-3">
                    <span className="block truncate" title={pedido.nombre}>
                      {pedido.nombre}
                    </span>
                  </TableCell>
                  <TableCell className="px-3">
                    <Badge variant="outline" className="rounded-sm text-sm">
                      {FLUJO[pedido.tipoFlujo] ?? pedido.tipoFlujo}
                    </Badge>
                  </TableCell>
                  <TableCell className="px-3">
                    <span className="font-medium tabular-nums">{codigoEtapa(pedido.tipoFlujo, pedido.etapaActual)}</span>
                    <span className="text-muted-foreground"> · {pedido.nombreEtapaActual}</span>
                  </TableCell>
                  {recorrido && (
                    <TableCell className="px-3">
                      <LineaCompacta
                        tipoFlujo={pedido.tipoFlujo}
                        etapaActual={pedido.etapaActual}
                        nombreEtapaActual={pedido.nombreEtapaActual}
                        finalizado={pedido.finalizado}
                        etapas={recorrido[pedido.tipoFlujo]}
                      />
                    </TableCell>
                  )}
                  <TableCell className="px-3">
                    {pedido.responsableActual?.nombre ?? <span className="text-muted-foreground">Por rol</span>}
                  </TableCell>
                  <TableCell className="px-3 py-2 [&_[data-slot=badge]]:whitespace-normal [&_[data-slot=badge]]:text-left">
                    <DistintivoVencimiento
                      venceEn={pedido.venceEn}
                      vencida={pedido.vencida}
                      finalizado={pedido.finalizado}
                    />
                    {pedido.motivoRetraso && (
                      <p className="mt-1 max-w-56 truncate text-xs" title={pedido.motivoRetraso}>
                        <span className="text-muted-foreground">Motivo: </span>
                        {pedido.motivoRetraso}
                      </p>
                    )}
                  </TableCell>
                  <TableCell className="px-3 py-2 [&_[data-slot=badge]]:whitespace-normal [&_[data-slot=badge]]:text-left">
                    {pedido.urgencia ? (
                      <DistintivoUrgencia
                        nivel={pedido.urgencia.nivel}
                        venceEn={pedido.urgencia.venceEn}
                        vencida={pedido.urgencia.vencida}
                      />
                    ) : (
                      <span className="text-muted-foreground">—</span>
                    )}
                  </TableCell>
                </TableRow>
              ))}
        </TableBody>
      </Table>
    </div>
    )
  );
}

/** Pantallas angostas: una tarjeta por pedido con lo mismo que la fila, sin desplazamiento horizontal. */
function TarjetasPedidos({ titulo, pedidos, cargando, recorrido }: Pick<Props, 'titulo' | 'pedidos' | 'cargando' | 'recorrido'>) {
  if (cargando && !pedidos) {
    return (
      <div className="flex flex-col gap-2" aria-busy="true" aria-label={`Cargando: ${titulo}`}>
        {[0, 1, 2].map((fila) => <Skeleton key={fila} className="h-28 w-full" />)}
      </div>
    );
  }
  return (
    <ul aria-label={titulo} className="flex flex-col gap-2">
      {pedidos?.map((pedido) => (
        <li key={pedido.id} className="flex flex-col gap-2 rounded-lg border bg-card p-3 shadow-sm">
          <div className="flex flex-wrap items-baseline justify-between gap-x-3 gap-y-1">
            <Link
              to={`/pedidos/${pedido.id}`}
              className="rounded-sm font-medium text-primary tabular-nums underline-offset-4 outline-none hover:underline focus-visible:ring-3 focus-visible:ring-ring"
            >
              {pedido.numero}
            </Link>
            <span className="text-xs text-muted-foreground">{FLUJO[pedido.tipoFlujo] ?? pedido.tipoFlujo}</span>
          </div>
          <p className="truncate text-sm" title={pedido.nombre}>{pedido.nombre}</p>
          <p className="text-sm">
            <span className="font-medium tabular-nums">{codigoEtapa(pedido.tipoFlujo, pedido.etapaActual)}</span>
            <span className="text-muted-foreground"> · {pedido.nombreEtapaActual} · {pedido.responsableActual?.nombre ?? 'Por rol'}</span>
          </p>
          {recorrido && (
            <LineaCompacta
              tipoFlujo={pedido.tipoFlujo}
              etapaActual={pedido.etapaActual}
              nombreEtapaActual={pedido.nombreEtapaActual}
              finalizado={pedido.finalizado}
              etapas={recorrido[pedido.tipoFlujo]}
            />
          )}
          <div className="flex flex-wrap gap-1.5 [&_[data-slot=badge]]:whitespace-normal [&_[data-slot=badge]]:text-left">
            <DistintivoVencimiento venceEn={pedido.venceEn} vencida={pedido.vencida} finalizado={pedido.finalizado} />
            {pedido.urgencia && (
              <DistintivoUrgencia nivel={pedido.urgencia.nivel} venceEn={pedido.urgencia.venceEn} vencida={pedido.urgencia.vencida} />
            )}
          </div>
          {pedido.motivoRetraso && (
            <p className="text-xs"><span className="text-muted-foreground">Motivo: </span>{pedido.motivoRetraso}</p>
          )}
        </li>
      ))}
    </ul>
  );
}

/** Paginación del servidor (`pagina` desde 0, `tamano`, `total`). */
export function Paginacion({
  pagina,
  tamano,
  total,
  onCambiar,
  ocupado,
}: {
  pagina: number;
  tamano: number;
  total: number;
  onCambiar: (pagina: number) => void;
  ocupado: boolean;
}) {
  if (total <= tamano) return null;
  const paginas = Math.ceil(total / tamano);
  return (
    <nav aria-label="Paginación" className="flex flex-wrap items-center justify-between gap-3 text-sm">
      <p className="text-muted-foreground tabular-nums" aria-live="polite">
        Página {pagina + 1} de {paginas} · {total} pedidos
      </p>
      <div className="flex gap-2">
        <Button variant="outline" size="sm" onClick={() => onCambiar(pagina - 1)} disabled={pagina === 0 || ocupado}>
          Anterior
        </Button>
        <Button
          variant="outline"
          size="sm"
          onClick={() => onCambiar(pagina + 1)}
          disabled={pagina + 1 >= paginas || ocupado}
        >
          Siguiente
        </Button>
      </div>
    </nav>
  );
}

import { RotateCwIcon } from 'lucide-react';
import type { ReactNode } from 'react';
import { Link } from 'react-router';
import type { PedidoResumen } from '@/api/pedidos';
import { Alert, AlertDescription } from '@/components/ui/alert';
import { Badge } from '@/components/ui/badge';
import { Button } from '@/components/ui/button';
import { Skeleton } from '@/components/ui/skeleton';
import { Table, TableBody, TableCaption, TableCell, TableHead, TableHeader, TableRow } from '@/components/ui/table';
import { DistintivoUrgencia, DistintivoVencimiento } from './Distintivos';
import { codigoEtapa } from './formato';

// Tabla de pedidos (DESIGN.md 7.1). Se entra al pedido por el enlace del número (no por un clic en la fila), la
// primera columna queda fija al desplazar en pantallas angostas y los estados de carga, vacío y error tienen su forma.

interface Props {
  titulo: string;
  pedidos: PedidoResumen[] | undefined;
  cargando: boolean;
  error: string | null;
  onReintentar: () => void;
  vacio: ReactNode;
}

const FLUJO: Record<string, string> = { A: 'A · Ecuador', B: 'B · China' };

export function TablaPedidos({ titulo, pedidos, cargando, error, onReintentar, vacio }: Props) {
  if (error) {
    return (
      <Alert variant="destructive">
        <AlertDescription className="flex flex-wrap items-center justify-between gap-3">
          <p>{error}</p>
          <Button variant="outline" size="sm" onClick={onReintentar}>
            <RotateCwIcon data-icon="inline-start" />
            Reintentar
          </Button>
        </AlertDescription>
      </Alert>
    );
  }

  if (!cargando && pedidos?.length === 0) {
    return <div className="rounded-lg border border-dashed bg-card px-6 py-10 text-center">{vacio}</div>;
  }

  return (
    <div className="overflow-x-auto rounded-lg border bg-card shadow-sm">
      <Table className="min-w-[56rem]">
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
                  {Array.from({ length: 7 }, (_, columna) => (
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
                  <TableCell className="px-3">
                    {pedido.responsableActual?.nombre ?? <span className="text-muted-foreground">Por rol</span>}
                  </TableCell>
                  <TableCell className="px-3">
                    <DistintivoVencimiento
                      venceEn={pedido.venceEn}
                      vencida={pedido.vencida}
                      finalizado={pedido.finalizado}
                    />
                  </TableCell>
                  <TableCell className="px-3">
                    {pedido.urgencia ? (
                      <DistintivoUrgencia nivel={pedido.urgencia.nivel} venceEn={pedido.urgencia.venceEn} />
                    ) : (
                      <span className="text-muted-foreground">—</span>
                    )}
                  </TableCell>
                </TableRow>
              ))}
        </TableBody>
      </Table>
    </div>
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

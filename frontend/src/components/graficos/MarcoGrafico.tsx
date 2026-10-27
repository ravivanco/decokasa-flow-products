import { TableIcon } from 'lucide-react';
import { type ReactNode, useId, useState } from 'react';
import { Alert, AlertDescription } from '@/components/ui/alert';
import { Button } from '@/components/ui/button';
import { Skeleton } from '@/components/ui/skeleton';
import { cn } from '@/lib/utils';

// Marco de cada gráfico del Inicio (ADR-0029, skill dataviz): título que nombra la serie, qué se mide y en qué
// período, leyenda cuando hay más de una serie, estados de carga/vacío/error y «Ver como tabla» con los mismos datos.

export interface TablaGrafico {
  columnas: string[];
  filas: ReactNode[][];
  /** Columnas numéricas (alineadas a la derecha). */
  numericas?: number[];
}

interface Props {
  titulo: string;
  descripcion: ReactNode;
  leyenda?: ReactNode;
  controles?: ReactNode;
  cargando?: boolean;
  error?: string | null;
  /** Texto del estado vacío; si es null, hay datos. */
  vacio?: string | null;
  tabla?: TablaGrafico;
  pie?: ReactNode;
  className?: string;
  children: ReactNode;
}

export function MarcoGrafico({ titulo, descripcion, leyenda, controles, cargando, error, vacio, tabla, pie, className, children }: Props) {
  const id = useId();
  const [verTabla, setVerTabla] = useState(false);
  const conDatos = !cargando && !error && !vacio;
  return (
    <section aria-labelledby={`${id}-titulo`} className={cn('flex min-w-0 flex-col gap-3 rounded-lg border bg-card p-4', className)}>
      <div className="flex flex-wrap items-start justify-between gap-2">
        <div className="flex min-w-0 flex-col gap-0.5">
          <h3 id={`${id}-titulo`} className="font-heading text-base font-semibold">{titulo}</h3>
          <p className="text-xs text-muted-foreground">{descripcion}</p>
        </div>
        {conDatos && tabla && (
          <Button variant="ghost" size="xs" aria-expanded={verTabla} aria-controls={`${id}-tabla`} onClick={() => setVerTabla((v) => !v)}>
            <TableIcon data-icon="inline-start" />
            {verTabla ? 'Ver gráfico' : 'Ver como tabla'}
          </Button>
        )}
      </div>
      {controles}
      {cargando ? (
        <Skeleton className="h-40 w-full" />
      ) : error ? (
        <Alert variant="destructive">
          <AlertDescription>{error}</AlertDescription>
        </Alert>
      ) : vacio ? (
        <p className="flex min-h-24 items-center justify-center rounded-md border border-dashed px-4 text-center text-sm text-muted-foreground">{vacio}</p>
      ) : verTabla && tabla ? (
        <div id={`${id}-tabla`} className="max-h-96 overflow-auto">
          <table className="w-full text-left text-sm">
            <caption className="sr-only">{titulo}</caption>
            <thead className="sticky top-0 bg-card">
              <tr className="text-xs text-muted-foreground">
                {tabla.columnas.map((columna, i) => (
                  <th key={columna} scope="col" className={cn('py-1.5 pr-3 font-medium', tabla.numericas?.includes(i) && 'text-right')}>{columna}</th>
                ))}
              </tr>
            </thead>
            <tbody>
              {tabla.filas.map((fila, f) => (
                <tr key={f} className="border-t">
                  {fila.map((celda, i) => i === 0 ? (
                    <th key={i} scope="row" className="py-1.5 pr-3 font-normal">{celda}</th>
                  ) : (
                    <td key={i} className={cn('py-1.5 pr-3', tabla.numericas?.includes(i) && 'text-right tabular-nums')}>{celda}</td>
                  ))}
                </tr>
              ))}
            </tbody>
          </table>
        </div>
      ) : (
        <>
          {leyenda}
          {children}
        </>
      )}
      {conDatos && pie}
    </section>
  );
}

/** Muestra de color de una leyenda; con {@code rayas}, el patrón de la serie «abierto». */
export function Muestra({ clase, rayas }: { clase: string; rayas?: boolean }) {
  return <span aria-hidden="true" className={cn('inline-block size-3 shrink-0 rounded-[3px]', clase, rayas && 'rayas-grafico')} />;
}

/** Tooltip de una marca: aparece al pasar el puntero o al enfocarla con el teclado. */
export function Ayuda({ children, lado = 'arriba' }: { children: ReactNode; lado?: 'arriba' | 'derecha' }) {
  return (
    <span
      role="tooltip"
      className={cn(
        'pointer-events-none absolute z-10 hidden w-max max-w-64 rounded-md border bg-popover px-2 py-1 text-xs text-popover-foreground shadow-md group-hover:block group-focus-visible:block',
        lado === 'arriba' ? 'bottom-full left-1/2 mb-1 -translate-x-1/2' : 'top-1/2 left-full ml-2 -translate-y-1/2',
      )}
    >
      {children}
    </span>
  );
}

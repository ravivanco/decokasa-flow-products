import type { ReactNode } from 'react';
import { cn } from '@/lib/utils';
import { Ayuda } from './MarcoGrafico';

// Barras propias, sin librería (ADR-0029, skill dataviz): un eje, marcas finas con extremo redondeado de 4 px,
// separación de 2 px, valor directo junto a cada barra y tooltip al pasar o enfocar. Un valor nulo («sin ocurrir»)
// se dibuja distinto de cero y nunca se cuenta como cero.

export type SerieBarra = 'principal' | 'finalizado' | 'abierto' | 'alerta';

const COLOR: Record<SerieBarra, string> = {
  principal: 'bg-grafico-serie',
  finalizado: 'bg-grafico-finalizado',
  abierto: 'bg-grafico-abierto rayas-grafico',
  alerta: 'bg-estado-vencido',
};

export interface DatoBarra {
  clave: string;
  etiqueta: ReactNode;
  /** Texto accesible de la categoría. */
  nombre: string;
  valor: number | null;
  /** Valor mostrado (con unidad); por defecto, el número. */
  texto?: string;
  ayuda?: ReactNode;
  serie?: SerieBarra;
}

interface Props {
  datos: DatoBarra[];
  orientacion?: 'horizontal' | 'vertical';
  /** Ancho de la columna de etiquetas en horizontal. */
  anchoEtiqueta?: string;
  /** Máximo del eje (por defecto, el mayor valor). */
  maximo?: number;
  sinValor?: string;
  /** Alto del gráfico vertical (clase de Tailwind). */
  alto?: string;
}

export function GraficoBarras({ datos, orientacion = 'horizontal', anchoEtiqueta = '10rem', maximo, sinValor = 'sin datos', alto = 'h-56' }: Props) {
  const tope = maximo ?? Math.max(0, ...datos.map((d) => d.valor ?? 0));
  const proporcion = (valor: number | null) => (valor === null || tope === 0 ? 0 : Math.max(valor > 0 ? 1.5 : 0, (valor / tope) * 100));
  const texto = (d: DatoBarra) => (d.valor === null ? sinValor : (d.texto ?? d.valor.toLocaleString('es-EC')));

  if (orientacion === 'vertical') {
    return (
      <div className={cn('flex items-end gap-0.5 border-b border-border pt-6 pb-0 sm:gap-1', alto)}>
        {datos.map((d) => (
          <div key={d.clave} className="flex h-full min-w-0 flex-1 flex-col items-center">
            <div
              tabIndex={0}
              role="img"
              aria-label={`${d.nombre}: ${texto(d)}`}
              className="group relative flex w-full flex-1 flex-col items-center justify-end rounded-sm outline-none focus-visible:ring-2 focus-visible:ring-ring"
            >
              <Ayuda>{d.ayuda ?? `${d.nombre}: ${texto(d)}`}</Ayuda>
              <span className={cn('mb-0.5 text-xs tabular-nums', d.valor === null ? 'text-muted-foreground' : 'font-medium')}>
                {d.valor === null ? '—' : texto(d)}
              </span>
              {d.valor !== null && (
                <span
                  className={cn('w-3/5 max-w-8 rounded-t-[4px] group-hover:brightness-110', COLOR[d.serie ?? 'principal'])}
                  style={{ height: `${proporcion(d.valor)}%` }}
                />
              )}
            </div>
            <span className="mt-1 h-4 truncate text-xs text-muted-foreground">{d.etiqueta}</span>
          </div>
        ))}
      </div>
    );
  }

  return (
    <ul className="flex flex-col gap-1.5">
      {datos.map((d) => (
        <li key={d.clave} className="grid items-center gap-2 text-sm" style={{ gridTemplateColumns: `minmax(0,${anchoEtiqueta}) minmax(0,1fr)` }}>
          <span className="flex min-w-0 items-center gap-1 truncate text-muted-foreground">{d.etiqueta}</span>
          <span
            tabIndex={0}
            role="img"
            aria-label={`${d.nombre}: ${texto(d)}`}
            className="group relative flex min-w-0 items-center gap-2 rounded-sm outline-none focus-visible:ring-2 focus-visible:ring-ring"
          >
            <Ayuda>{d.ayuda ?? `${d.nombre}: ${texto(d)}`}</Ayuda>
            {d.valor !== null && (
              <span
                className={cn('h-3 shrink-0 rounded-r-[4px] group-hover:brightness-110', COLOR[d.serie ?? 'principal'])}
                style={{ width: `calc(${proporcion(d.valor)}% - 4.5rem * ${proporcion(d.valor) / 100})` }}
              />
            )}
            <span className={cn('shrink-0 text-xs tabular-nums', d.valor === null ? 'text-muted-foreground' : 'font-medium')}>{texto(d)}</span>
          </span>
        </li>
      ))}
    </ul>
  );
}

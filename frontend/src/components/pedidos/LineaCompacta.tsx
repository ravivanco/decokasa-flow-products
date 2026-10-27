import { memo } from 'react';
import { cn } from '@/lib/utils';
import { codigoEtapa } from './formato';

// Recorrido horizontal compacto de un pedido dentro de una fila (paso 5.3; DESIGN.md 7.2). Sale de la etapa actual del
// resumen y de las etapas de la definición del flujo, que se carga una sola vez para toda la tabla: no se pide el
// detalle de cada pedido. En un flujo lineal, las etapas anteriores a la actual ya se recorrieron; las siguientes no
// (una etapa devuelta vuelve a recorrerse). El texto acompaña a los puntos: el estado no depende solo del color.

export interface EtapaDeFlujo {
  numero: number;
  nombre: string;
}

interface Props {
  tipoFlujo: string;
  etapaActual: number;
  nombreEtapaActual: string;
  finalizado: boolean;
  /** Etapas de la versión vigente del flujo; sin ellas solo se muestra el texto de la etapa actual. */
  etapas?: EtapaDeFlujo[];
}

// Paso 5.5: memorizada; una tabla de 20–100 filas no la vuelve a dibujar si sus datos no cambian.
export const LineaCompacta = memo(function LineaCompacta({
  tipoFlujo,
  etapaActual,
  nombreEtapaActual,
  finalizado,
  etapas,
}: Props) {
  const posicion = etapas ? etapas.findIndex((etapa) => etapa.numero === etapaActual) + 1 : 0;
  const texto = finalizado
    ? 'Finalizado'
    : etapas && posicion > 0
      ? `Etapa ${posicion} de ${etapas.length}`
      : `${codigoEtapa(tipoFlujo, etapaActual)} · ${nombreEtapaActual}`;

  return (
    <div className="flex min-w-40 flex-col gap-1">
      {etapas && etapas.length > 0 && (
        <ol aria-hidden="true" className="flex items-center">
          {etapas.map((etapa, indice) => {
            const recorrida = finalizado || etapa.numero < etapaActual;
            const actual = !finalizado && etapa.numero === etapaActual;
            return (
              <li
                key={etapa.numero}
                className="flex items-center"
                title={`${codigoEtapa(tipoFlujo, etapa.numero)} · ${etapa.nombre}`}
              >
                {indice > 0 && (
                  <span className={cn('h-0.5 w-2', recorrida || actual ? 'bg-primary' : 'bg-border')} />
                )}
                <span
                  className={cn(
                    'block rounded-full border-2',
                    actual
                      ? 'size-3 border-primary bg-card ring-2 ring-marca-naranja'
                      : recorrida
                        ? 'size-2.5 border-primary bg-primary'
                        : 'size-2.5 border-border bg-card',
                  )}
                />
              </li>
            );
          })}
        </ol>
      )}
      <span className="text-xs text-muted-foreground tabular-nums">{texto}</span>
    </div>
  );
});

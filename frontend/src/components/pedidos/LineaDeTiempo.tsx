import { CheckIcon, FlagIcon, Undo2Icon } from 'lucide-react';
import { useEffect, useRef } from 'react';
import type { EtapaEnLinea } from '@/api/pedidos';
import { cn } from '@/lib/utils';
import { codigoEtapa, formatearFechaCorta } from './formato';

// Línea de tiempo horizontal (DESIGN.md 7.2): el recorrido del pedido "como el rastreo de un envío". Una etapa por
// elemento, cada una es un botón para ver su detalle; la actual lleva aria-current="step" y la etiqueta de texto.
// El estado lo da el servidor (`estado` de cada etapa); aquí solo se dibuja. El nombre accesible sale del contenido
// visible, en el mismo orden (código, nombre, estado y fecha), sin aria-label que lo sustituya (WCAG 2.5.3, F5-B3-H01).

interface Props {
  numeroPedido: string;
  tipoFlujo: string;
  etapas: EtapaEnLinea[];
  seleccionada: number;
  automaticas: Set<number>;
  onSeleccionar: (numero: number) => void;
}

const ETIQUETA_ESTADO = {
  COMPLETADA: 'completada',
  ACTUAL: 'etapa actual',
  PENDIENTE: 'pendiente',
  DEVUELTA: 'devuelta',
} as const;

export function LineaDeTiempo({ numeroPedido, tipoFlujo, etapas, seleccionada, automaticas, onSeleccionar }: Props) {
  const actual = useRef<HTMLLIElement>(null);

  // En pantallas angostas, la etapa actual queda a la vista al abrir el pedido.
  useEffect(() => {
    actual.current?.scrollIntoView?.({ block: 'nearest', inline: 'center' });
  }, []);

  return (
    <div className="overflow-x-auto rounded-lg border bg-card px-2 py-4 shadow-sm">
      <ol aria-label={`Recorrido del pedido ${numeroPedido}`} className="flex min-w-max items-start">
        {etapas.map((etapa, indice) => {
          const esActual = etapa.estado === 'ACTUAL';
          const recorrida = etapa.estado !== 'PENDIENTE';
          const tramoRecorrido = indice + 1 < etapas.length && etapas[indice + 1].estado !== 'PENDIENTE';
          const fecha = etapa.salida ?? etapa.venceEn ?? null;
          const elegida = etapa.numero === seleccionada;
          return (
            <li
              key={etapa.numero}
              ref={esActual ? actual : undefined}
              className="relative flex w-28 flex-col items-center"
            >
              {indice + 1 < etapas.length && (
                <span
                  aria-hidden="true"
                  className={cn('absolute top-5 left-1/2 h-0.5 w-full', tramoRecorrido ? 'bg-primary' : 'bg-border')}
                />
              )}
              <button
                type="button"
                onClick={() => onSeleccionar(etapa.numero)}
                aria-current={esActual ? 'step' : undefined}
                aria-pressed={elegida}
                className={cn(
                  'relative z-10 flex min-h-11 w-full flex-col items-center gap-1.5 rounded-md px-1 pb-1.5 text-center outline-none hover:bg-morado-50 focus-visible:ring-3 focus-visible:ring-ring',
                  elegida && 'bg-morado-100 hover:bg-morado-100',
                )}
              >
                <span
                  aria-hidden="true"
                  className={cn(
                    'mt-1 flex size-8 items-center justify-center rounded-full border-2 text-xs font-semibold tabular-nums',
                    etapa.estado === 'COMPLETADA' && 'border-primary bg-primary text-primary-foreground',
                    esActual && 'border-primary bg-card text-primary ring-3 ring-marca-naranja ring-offset-2',
                    etapa.estado === 'PENDIENTE' && 'border-border bg-card text-muted-foreground',
                    etapa.estado === 'DEVUELTA' && 'border-estado-aviso bg-estado-aviso-fondo text-estado-aviso',
                  )}
                >
                  {etapa.estado === 'COMPLETADA' ? (
                    <CheckIcon className="size-4" />
                  ) : etapa.estado === 'DEVUELTA' ? (
                    <Undo2Icon className="size-4" />
                  ) : automaticas.has(etapa.numero) ? (
                    <FlagIcon className="size-4" />
                  ) : (
                    etapa.numero
                  )}
                </span>
                <span className="text-xs font-medium text-muted-foreground tabular-nums">
                  {codigoEtapa(tipoFlujo, etapa.numero)}
                </span>{' '}
                <span
                  className={cn(
                    'line-clamp-2 text-xs leading-tight',
                    esActual ? 'font-semibold text-foreground' : recorrida ? 'text-primary' : 'text-muted-foreground',
                  )}
                >
                  {etapa.nombre}
                </span>
                {/* El estado se nombra una sola vez: en texto para lectores; el rótulo visible que lo repite se oculta
                    a ellos para no leerlo dos veces. */}
                <span className="sr-only">, {ETIQUETA_ESTADO[etapa.estado]}</span>
                {esActual ? (
                  <span aria-hidden="true" className="text-xs font-semibold text-marca-naranja-texto">
                    Etapa actual
                  </span>
                ) : etapa.estado === 'DEVUELTA' ? (
                  <span aria-hidden="true" className="text-xs text-estado-aviso">
                    Devuelta
                  </span>
                ) : fecha ? (
                  <span className="text-xs text-muted-foreground tabular-nums">
                    <span className="sr-only">, {etapa.salida ? 'salió el' : 'vence el'} </span>
                    {formatearFechaCorta(fecha)}
                  </span>
                ) : null}
              </button>
            </li>
          );
        })}
      </ol>
    </div>
  );
}

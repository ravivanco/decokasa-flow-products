import { CheckIcon, FlagIcon, Undo2Icon } from 'lucide-react';
import { useEffect, useRef } from 'react';
import type { EtapaEnLinea } from '@/api/pedidos';
import { cn } from '@/lib/utils';
import { duracionReloj, useAhoraServidor } from './Cronometro';
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
  /** Fase de cada etapa, si la versión del flujo las define (V32). En versiones anteriores no cambia nada. */
  fases?: Map<number, { numero: number; nombre: string }>;
  /**
   * Fecha de fin de cada etapa según la planificación hecha al crear el pedido (cronograma, AAAA-MM-DD). Con ella, cada
   * etapa muestra su cuenta regresiva desde la creación (pedido del cliente, 2026-11-14).
   */
  planes?: Map<number, string>;
  servidorAhora?: string | null;
}

/** Fin del día planificado en Ecuador (UTC−5). */
const finDelDia = (fecha: string) => new Date(`${fecha}T23:59:59-05:00`).getTime();

/** El cronómetro solo corre en la etapa activa: las demás no muestran cuenta regresiva. */
function cuentaDeEtapa(etapa: EtapaEnLinea, plan: string | undefined, ahora: number) {
  if (etapa.estado !== 'ACTUAL') return null;
  if (!plan) return { texto: 'Sin plazo', clase: 'text-muted-foreground' };
  const fin = finDelDia(plan);
  const resto = fin - ahora;
  if (resto < 0) return { texto: `Vencido hace ${duracionReloj(resto)}`, clase: 'font-medium text-estado-vencido' };
  return { texto: `Quedan ${duracionReloj(resto)}`, clase: resto < 86_400_000 ? 'text-estado-aviso' : 'text-estado-ok' };
}

const ETIQUETA_ESTADO = {
  COMPLETADA: 'completada',
  ACTUAL: 'etapa actual',
  PENDIENTE: 'pendiente',
  DEVUELTA: 'devuelta',
} as const;

export function LineaDeTiempo({ numeroPedido, tipoFlujo, etapas, seleccionada, automaticas, onSeleccionar, fases, planes, servidorAhora }: Props) {
  // Cuenta regresiva viva, segundo a segundo, solo si hay fechas planificadas que contar.
  const ahora = useAhoraServidor(servidorAhora, planes && planes.size > 0 ? 1000 : 0);
  const actual = useRef<HTMLLIElement>(null);

  // En pantallas angostas, la etapa actual queda a la vista al abrir el pedido.
  useEffect(() => {
    actual.current?.scrollIntoView?.({ block: 'nearest', inline: 'center' });
  }, []);

  // Etapas agrupadas por fase, en el orden del flujo; sin fases, un solo grupo como siempre.
  const grupos: { fase?: { numero: number; nombre: string }; etapas: EtapaEnLinea[] }[] = [];
  for (const etapa of etapas) {
    const fase = fases?.get(etapa.numero);
    const ultimo = grupos.at(-1);
    if (ultimo && ultimo.fase?.numero === fase?.numero) ultimo.etapas.push(etapa);
    else grupos.push({ fase, etapas: [etapa] });
  }

  return (
    <div className="min-w-0 overflow-x-auto rounded-lg border bg-card px-2 py-4 shadow-sm">
    {/* Cada fase ocupa un ancho proporcional a sus etapas y las etapas se reparten por igual: en escritorio todo el
        recorrido cabe sin desplazarse; por debajo de ese ancho mínimo, el recorrido se desplaza en horizontal. */}
    <div className="flex w-full min-w-[60rem] gap-3">
      {grupos.map((grupo) => (
      <div key={grupo.fase?.numero ?? 'todas'} style={{ flexGrow: grupo.etapas.length, flexBasis: 0 }}
        className={cn('flex min-w-0 flex-col gap-2', grupo.fase && 'rounded-md border bg-muted/30 px-1 py-2')}>
      {grupo.fase && (
        <p className="flex flex-wrap items-center gap-2 px-2 font-heading text-sm font-semibold text-primary">
          Fase {grupo.fase.numero} · {grupo.fase.nombre}
          <span className="rounded-sm bg-secondary px-1.5 py-0.5 text-xs font-medium">
            {grupo.etapas.every((etapa) => etapa.estado === 'COMPLETADA') ? 'Completada'
              : grupo.etapas.some((etapa) => etapa.estado !== 'PENDIENTE') ? 'En curso' : 'Pendiente'}
          </span>
        </p>
      )}
      <ol aria-label={grupo.fase ? `Fase ${grupo.fase.numero} del pedido ${numeroPedido}` : `Recorrido del pedido ${numeroPedido}`} className="flex w-full items-start">
        {grupo.etapas.map((etapa, indice, lista) => {
          const esActual = etapa.estado === 'ACTUAL';
          const recorrida = etapa.estado !== 'PENDIENTE';
          const tramoRecorrido = indice + 1 < lista.length && lista[indice + 1].estado !== 'PENDIENTE';
          const fecha = etapa.salida ?? etapa.venceEn ?? null;
          const elegida = etapa.numero === seleccionada;
          return (
            <li
              key={etapa.numero}
              ref={esActual ? actual : undefined}
              className="relative flex min-w-0 flex-1 flex-col items-center"
            >
              {indice + 1 < lista.length && (
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
                  title={etapa.nombre}
                  lang="es"
                  className={cn(
                    'line-clamp-3 text-xs leading-tight hyphens-auto',
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
                {planes && planes.size > 0 && !automaticas.has(etapa.numero) && (() => {
                  const cuenta = cuentaDeEtapa(etapa, planes.get(etapa.numero), ahora);
                  return cuenta ? <span className={cn('text-xs tabular-nums', cuenta.clase)}><span className="sr-only">, </span>{cuenta.texto}</span> : null;
                })()}
              </button>
            </li>
          );
        })}
      </ol>
      </div>
      ))}
    </div>
    </div>
  );
}

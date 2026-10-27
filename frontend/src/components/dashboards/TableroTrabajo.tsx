import { CircleAlertIcon, InboxIcon, type LucideIcon, ZapIcon } from 'lucide-react';
import { type KeyboardEvent, useEffect, useId, useRef, useState } from 'react';
import { INTERVALO_ACTUALIZACION_MS } from '@/api/actualizacion';
import { type FiltroBandeja, useBandeja, useConteosBandeja, useDefinicionesVigentes } from '@/api/pedidos';
import { mensajeDeErrorPedido } from '@/components/pedidos/formato';
import { Paginacion, TablaPedidos } from '@/components/pedidos/TablaPedidos';
import { cn } from '@/lib/utils';

// Trabajo de la persona en el dashboard (paso 5.3): su bandeja según P-06 en tres pestañas (pendientes, plazo
// vencido y con urgencia), con responsable, etapa, vencimiento, motivo y la línea horizontal de cada pedido. Los
// totales y los filtros los da el servidor; el admin ve todo lo que está en curso. Pestañas con la semántica de ARIA
// (flechas, Inicio y Fin) y el estado siempre con texto e ícono.

type IdPestana = 'pendientes' | 'vencidos' | 'urgencias';

const PESTANAS: { id: IdPestana; titulo: string; filtro: FiltroBandeja; vacio: string; icono: LucideIcon }[] = [
  { id: 'pendientes', titulo: 'Pendientes', filtro: {}, vacio: 'No hay pedidos pendientes.', icono: InboxIcon },
  {
    id: 'vencidos',
    titulo: 'Plazo vencido',
    filtro: { vencido: true },
    vacio: 'Ningún pedido tiene vencido el plazo de su etapa actual.',
    icono: CircleAlertIcon,
  },
  {
    id: 'urgencias',
    titulo: 'Con urgencia',
    filtro: { conUrgencia: true },
    vacio: 'Ningún pedido tiene una urgencia vigente.',
    icono: ZapIcon,
  },
];

const TAMANO = 20;

export function TableroTrabajo({ admin }: { admin: boolean }) {
  const ids = useId();
  const [elegida, setElegida] = useState<IdPestana>('pendientes');
  const [pagina, setPagina] = useState(0);
  const botones = useRef<(HTMLButtonElement | null)[]>([]);
  const conteos = useConteosBandeja();
  const pestana = PESTANAS.find((candidata) => candidata.id === elegida) ?? PESTANAS[0];
  const bandeja = useBandeja(pagina, pestana.filtro, TAMANO);
  // Fase 6: A y B de Ecuador; cada línea usa la versión vigente de su flujo (una consulta por flujo para toda la tabla).
  const definiciones = useDefinicionesVigentes();
  const totales: Record<IdPestana, number | undefined> = {
    pendientes: conteos.pendientes,
    vencidos: conteos.vencidos,
    urgencias: conteos.conUrgencia,
  };

  // Aviso de lo que trae la actualización periódica (F5-B3-H04): un mensaje de estado discreto (role="status", que no
  // interrumpe) y sin mover el foco, solo cuando la lista visible o los totales cambian dentro de la misma pestaña y
  // página. La carga inicial, el cambio de pestaña o de página y un sondeo sin cambios no anuncian nada: con el
  // structural sharing de TanStack Query, los datos conservan la misma referencia si el servidor devuelve lo mismo.
  const [anuncio, setAnuncio] = useState('');
  const vistaAnterior = useRef<{ clave: string; lista: unknown; totales: string | null } | null>(null);
  const lista = bandeja.isPlaceholderData ? undefined : bandeja.data;
  const clave = `${elegida}:${pagina}`;
  const totalesConocidos =
    conteos.pendientes === undefined || conteos.vencidos === undefined || conteos.conUrgencia === undefined
      ? null
      : `${conteos.pendientes} pendientes, ${conteos.vencidos} con plazo vencido y ${conteos.conUrgencia} con urgencia`;

  useEffect(() => {
    if (lista === undefined) return;
    const anterior = vistaAnterior.current;
    vistaAnterior.current = { clave, lista, totales: totalesConocidos };
    if (!anterior || anterior.clave !== clave) return;
    // Que lleguen los totales por primera vez no es un cambio: solo cuenta si ya se conocían.
    const cambiaronTotales = anterior.totales !== null && totalesConocidos !== null && anterior.totales !== totalesConocidos;
    if (anterior.lista === lista && !cambiaronTotales) return;
    const hora = new Intl.DateTimeFormat('es-EC', {
      hour: '2-digit',
      minute: '2-digit',
      timeZone: 'America/Guayaquil',
    }).format(new Date());
    setAnuncio(
      totalesConocidos
        ? `Lista actualizada a las ${hora}: ${totalesConocidos}.`
        : `Lista actualizada a las ${hora}.`,
    );
  }, [lista, clave, totalesConocidos]);

  function elegir(id: IdPestana) {
    setElegida(id);
    setPagina(0);
  }

  function moverConTeclado(evento: KeyboardEvent<HTMLDivElement>) {
    const actual = PESTANAS.findIndex((candidata) => candidata.id === elegida);
    const ultima = PESTANAS.length - 1;
    const destino =
      evento.key === 'ArrowRight'
        ? (actual + 1) % PESTANAS.length
        : evento.key === 'ArrowLeft'
          ? (actual + ultima) % PESTANAS.length
          : evento.key === 'Home'
            ? 0
            : evento.key === 'End'
              ? ultima
              : null;
    if (destino === null) return;
    evento.preventDefault();
    elegir(PESTANAS[destino].id);
    botones.current[destino]?.focus();
  }

  return (
    <section id="trabajo" tabIndex={-1} aria-labelledby={`${ids}-titulo`} className="flex scroll-mt-4 flex-col gap-4 outline-none">
      <div className="flex flex-wrap items-end justify-between gap-3">
        <div className="flex flex-col gap-1">
          <h2 id={`${ids}-titulo`} className="font-heading text-xl font-semibold">
            {admin ? 'Pedidos en curso' : 'Mi trabajo'}
          </h2>
        </div>
      </div>

      <div
        role="tablist"
        aria-label={admin ? 'Pedidos en curso' : 'Mi trabajo'}
        onKeyDown={moverConTeclado}
        className="flex flex-wrap gap-2"
      >
        {PESTANAS.map((candidata, indice) => {
          const seleccionada = candidata.id === elegida;
          const Icono = candidata.icono;
          const total = totales[candidata.id];
          return (
            <button
              key={candidata.id}
              ref={(boton) => {
                botones.current[indice] = boton;
              }}
              type="button"
              role="tab"
              id={`${ids}-pestana-${candidata.id}`}
              aria-selected={seleccionada}
              aria-controls={`${ids}-panel`}
              tabIndex={seleccionada ? 0 : -1}
              onClick={() => elegir(candidata.id)}
              className={cn(
                'inline-flex min-h-10 items-center gap-2 rounded-md border px-3 text-sm font-medium outline-none focus-visible:ring-3 focus-visible:ring-ring',
                seleccionada ? 'border-primary bg-morado-100 text-primary' : 'bg-card hover:bg-morado-50',
              )}
            >
              <Icono aria-hidden="true" className="size-4" />
              {candidata.titulo}
              <span className="min-w-[3ch] rounded-sm bg-background px-1.5 text-center tabular-nums">
                {total === undefined ? '…' : total}
              </span>
            </button>
          );
        })}
      </div>
      {/* Siempre presente para que el lector registre la región antes del primer cambio. */}
      <p role="status" className="sr-only">
        {anuncio}
      </p>
      {conteos.error && (
        <p role="status" className="text-sm text-muted-foreground">
          No se pudieron cargar los totales; las listas siguen disponibles.
        </p>
      )}

      <div
        role="tabpanel"
        id={`${ids}-panel`}
        aria-labelledby={`${ids}-pestana-${elegida}`}
        tabIndex={0}
        className="flex flex-col gap-3 rounded-lg outline-none focus-visible:ring-3 focus-visible:ring-ring"
      >
        <TablaPedidos
          alturaEstable
          titulo={`${pestana.titulo}: pedidos`}
          pedidos={bandeja.isPlaceholderData ? undefined : bandeja.data?.elementos}
          cargando={bandeja.isPending || bandeja.isPlaceholderData}
          error={bandeja.isError ? mensajeDeErrorPedido(bandeja.error, 'No se pudo cargar la lista.') : null}
          onReintentar={() => void bandeja.refetch()}
          vacio={<p className="text-muted-foreground">{pestana.vacio}</p>}
          recorrido={{ A: definiciones.A?.etapas, B: definiciones.B?.etapas }}
        />
        <div className="min-h-10">{bandeja.data && !bandeja.isPlaceholderData && (
          <Paginacion
            pagina={bandeja.data.pagina}
            tamano={bandeja.data.tamano}
            total={bandeja.data.total}
            ocupado={bandeja.isFetching}
            onCambiar={setPagina}
          />
        )}</div>
      </div>
    </section>
  );
}

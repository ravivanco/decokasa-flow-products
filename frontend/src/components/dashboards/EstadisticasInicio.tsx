import { ActivityIcon, AlertTriangleIcon, CircleCheckIcon, FilePlus2Icon, type LucideIcon, RotateCcwIcon, TimerIcon, TruckIcon, ZapIcon } from 'lucide-react';
import { type ReactNode, useId, useState } from 'react';
import { Link } from 'react-router';
import { type Estadisticas, useEstadisticas } from '@/api/estadisticas';
import { useUsuarioActual } from '@/auth/SesionProvider';
import { GraficoBarras } from '@/components/graficos/GraficoBarras';
import { GraficoDona, type SegmentoDona } from '@/components/graficos/GraficoDona';
import { Ayuda, MarcoGrafico, Muestra } from '@/components/graficos/MarcoGrafico';
import { formatearFecha, formatearFechaHora, mensajeDeErrorPedido } from '@/components/pedidos/formato';
import { Button } from '@/components/ui/button';
import { Input } from '@/components/ui/input';
import { Label } from '@/components/ui/label';
import { enUnidad, formatearDuracion, type Unidad, unidadPara } from '@/lib/duracion';
import { cn } from '@/lib/utils';
import { NOMBRE_PAIS, esPais, usePaisActivo } from '@/pais/PaisProvider';

// Dashboard del Inicio (ADR-0029): el mismo para todos los roles; el servidor limita los datos a los países vigentes
// de cada persona y calcula sobre el conjunto completo. Cada indicador dice qué cuenta y en qué período. Los filtros
// afectan a todo salvo la dona por año, que compara todos los años registrados (y lo dice).
// Mejora visual del 2026-10-27: indicadores en una sola banda, gráficos por filas de altura parecida (mes y estado;
// tiempo por etapa con los flujos lado a lado; tiempo por pedido y años) para no dejar huecos.

const MESES = ['ene', 'feb', 'mar', 'abr', 'may', 'jun', 'jul', 'ago', 'sep', 'oct', 'nov', 'dic'];
const MESES_LARGOS = ['enero', 'febrero', 'marzo', 'abril', 'mayo', 'junio', 'julio', 'agosto', 'septiembre', 'octubre', 'noviembre', 'diciembre'];
const TODOS = 'todos';
const selectClase =
  'h-10 rounded-md border border-input bg-background px-2.5 text-sm focus-visible:outline-none focus-visible:ring-2 focus-visible:ring-ring';
const nombrePais = (pais: string) => (esPais(pais) ? NOMBRE_PAIS[pais] : pais);

export function EstadisticasInicio({ encabezado }: { encabezado: ReactNode }) {
  const ids = useId();
  const usuario = useUsuarioActual();
  const paisActivo = usePaisActivo();
  const anioActual = new Date().getFullYear();
  const [anio, setAnio] = useState(anioActual);
  const [pais, setPais] = useState<string>(paisActivo);
  const [tipoFlujo, setTipoFlujo] = useState<'' | 'A' | 'B'>('');
  const [desde, setDesde] = useState('');
  const [hasta, setHasta] = useState('');
  const fechasValidas = !desde || !hasta || desde <= hasta;
  const consulta = useEstadisticas({
    anio,
    pais: pais === TODOS ? undefined : pais,
    tipoFlujo: tipoFlujo || undefined,
    desde: fechasValidas && desde ? desde : undefined,
    hasta: fechasValidas && hasta ? hasta : undefined,
  }, fechasValidas);
  const datos = consulta.data;
  const cargando = consulta.isPending;
  const error = consulta.isError ? mensajeDeErrorPedido(consulta.error, 'No se pudieron calcular las estadísticas.') : null;
  const filtrado = anio !== anioActual || pais !== paisActivo || tipoFlujo !== '' || desde !== '' || hasta !== '';
  const periodo = datos
    ? desde || hasta
      ? `del ${formatearFecha(datos.periodo.desde)} al ${formatearFecha(datos.periodo.hasta)}`
      : `en ${datos.periodo.anio}`
    : '';
  const anios = Array.from(new Set([anioActual, ...(datos?.porAnio.map((a) => a.anio) ?? [])])).sort((a, b) => b - a);
  const alcance = [
    datos ? datos.paises.map(nombrePais).join(' y ') || 'sin países asignados' : null,
    tipoFlujo ? `flujo ${tipoFlujo}` : 'flujos A y B',
  ].filter(Boolean).join(' · ');

  function restablecer() {
    setAnio(anioActual);
    setPais(paisActivo);
    setTipoFlujo('');
    setDesde('');
    setHasta('');
  }

  return (
    <div className="flex flex-col gap-4">
      <div className="flex flex-col gap-3 rounded-lg border bg-card p-4">
        {encabezado}
        <div role="group" aria-labelledby={`${ids}-filtros`} className="flex flex-wrap items-end gap-2">
          <span id={`${ids}-filtros`} className="sr-only">Filtros del dashboard</span>
          <div className="flex flex-col gap-1">
            <Label htmlFor={`${ids}-anio`} className="text-xs">Año</Label>
            <select id={`${ids}-anio`} className={selectClase} value={anio} onChange={(e) => setAnio(Number(e.target.value))}>
              {anios.map((a) => <option key={a} value={a}>{a}</option>)}
            </select>
          </div>
          {usuario.paises.length > 1 && (
            <div className="flex flex-col gap-1">
              <Label htmlFor={`${ids}-pais`} className="text-xs">País</Label>
              <select id={`${ids}-pais`} className={selectClase} value={pais} onChange={(e) => setPais(e.target.value)}>
                {usuario.paises.map((p) => <option key={p} value={p}>{nombrePais(p)}</option>)}
                <option value={TODOS}>Todos mis países</option>
              </select>
            </div>
          )}
          <div className="flex flex-col gap-1">
            <Label htmlFor={`${ids}-flujo`} className="text-xs">Flujo</Label>
            <select id={`${ids}-flujo`} className={selectClase} value={tipoFlujo} onChange={(e) => setTipoFlujo(e.target.value as '' | 'A' | 'B')}>
              <option value="">A y B</option>
              <option value="A">A · pedido de Ecuador</option>
              <option value="B">B · propuesto por China</option>
            </select>
          </div>
          <div className="flex flex-col gap-1">
            <Label htmlFor={`${ids}-desde`} className="text-xs">Desde</Label>
            <Input id={`${ids}-desde`} type="date" className="h-10 w-38" value={desde} onChange={(e) => setDesde(e.target.value)} />
          </div>
          <div className="flex flex-col gap-1">
            <Label htmlFor={`${ids}-hasta`} className="text-xs">Hasta</Label>
            <Input id={`${ids}-hasta`} type="date" className="h-10 w-38" value={hasta} onChange={(e) => setHasta(e.target.value)} aria-invalid={!fechasValidas} aria-describedby={`${ids}-fechas`} />
          </div>
          {filtrado && (
            <Button variant="ghost" size="sm" onClick={restablecer}>
              <RotateCcwIcon data-icon="inline-start" />
              Restablecer
            </Button>
          )}
          <p id={`${ids}-fechas`} className="basis-full text-xs text-muted-foreground" aria-live="polite">
            {!fechasValidas ? (
              <span className="text-destructive">La fecha final no puede ser anterior a la inicial.</span>
            ) : (
              <>
                Período {periodo || '…'} ({desde || hasta ? 'fechas elegidas' : 'año completo'}) · {alcance}
                {consulta.isFetching && !cargando ? ' · actualizando…' : ''}
              </>
            )}
          </p>
        </div>
      </div>
      {error && datos && <p role="alert" className="text-sm text-destructive">{error} Se muestran los últimos datos obtenidos.</p>}

      <dl className="grid grid-cols-2 gap-px overflow-hidden rounded-lg border bg-border shadow-sm sm:grid-cols-3 xl:grid-cols-5">
        <Indicador icono={FilePlus2Icon} titulo="Pedidos creados" valor={datos?.tarjetas.creados} cargando={cargando} nota={`Creados ${periodo || 'en el período'}`} />
        <Indicador icono={ActivityIcon} titulo="En curso hoy" valor={datos?.tarjetas.enCurso} cargando={cargando} nota="Sin finalizar, creados en cualquier fecha" />
        <Indicador icono={CircleCheckIcon} titulo="Finalizados" valor={datos?.tarjetas.finalizados} cargando={cargando} nota={`Terminados ${periodo || 'en el período'}`} />
        <Indicador
          icono={AlertTriangleIcon}
          titulo="Con retraso vigente"
          valor={datos?.tarjetas.conRetraso}
          cargando={cargando}
          nota="Hoy, de los en curso: etapa o urgencia vencida"
          alerta={(datos?.tarjetas.conRetraso ?? 0) > 0}
        />
        <Indicador
          icono={TimerIcon}
          titulo="Duración de resolución"
          texto={datos?.tarjetas.medianaResolucionSegundos != null ? formatearDuracion(datos.tarjetas.medianaResolucionSegundos) : datos ? 'Sin finalizados' : undefined}
          cargando={cargando}
          nota={datos && datos.tarjetas.resolucionObservaciones > 0
            ? `Mediana de ${datos.tarjetas.resolucionObservaciones} ${datos.tarjetas.resolucionObservaciones === 1 ? 'pedido finalizado' : 'pedidos finalizados'} ${periodo}, de creación a finalización${datos.tarjetas.resolucionObservaciones < 5 ? ' (pocos casos)' : ''}`
            : `Ningún pedido finalizado ${periodo || 'en el período'}`}
        />
      </dl>

      <div className="grid gap-3 lg:grid-cols-12">
        <PedidosPorMes datos={datos} anio={anio} cargando={cargando} error={error} className="lg:col-span-7 xl:col-span-8" />
        <EstadoPedidos datos={datos} periodo={periodo} cargando={cargando} error={error} className="lg:col-span-5 xl:col-span-4" />
        <TiempoPorEtapa datos={datos} periodo={periodo} cargando={cargando} error={error} className="lg:col-span-12" />
        <PedidosEnCurso datos={datos} periodo={periodo} cargando={cargando} error={error} className="lg:col-span-6 xl:col-span-4" />
        <PedidosFinalizados datos={datos} periodo={periodo} cargando={cargando} error={error} className="lg:col-span-6 xl:col-span-4" />
        <PedidosPorAnio datos={datos} cargando={cargando} error={error} className="lg:col-span-12 xl:col-span-4" />
      </div>
      {datos && (
        <p className="text-xs text-muted-foreground">
        </p>
      )}
    </div>
  );
}

function Indicador({ icono: Icono, titulo, valor, texto, nota, cargando, alerta }: {
  icono: LucideIcon;
  titulo: string;
  valor?: number;
  texto?: string;
  nota: string;
  cargando: boolean;
  alerta?: boolean;
}) {
  const mostrado = texto ?? (valor === undefined ? undefined : valor.toLocaleString('es-EC'));
  return (
    <div className={cn('flex min-w-0 flex-col gap-0.5 bg-card px-4 py-3', alerta && 'bg-estado-vencido-fondo')}>
      <dt className={cn('flex items-center gap-1.5 text-xs font-medium text-muted-foreground', alerta && 'text-estado-vencido')}>
        <Icono aria-hidden="true" className="size-3.5 shrink-0" />
        {titulo}
      </dt>
      <dd className={cn('font-heading text-2xl font-semibold tabular-nums', alerta && 'text-estado-vencido')}>
        {cargando || mostrado === undefined ? '…' : mostrado}
        {alerta && !cargando && <span className="ml-2 align-middle font-sans text-xs font-medium">requiere atención</span>}
      </dd>
      <dd className="text-xs leading-snug text-muted-foreground">{nota}</dd>
    </div>
  );
}

interface PropsGrafico {
  datos: Estadisticas | undefined;
  cargando: boolean;
  error: string | null;
  className?: string;
}

function PedidosPorMes({ datos, anio, ...estado }: PropsGrafico & { anio: number }) {
  const meses = datos?.porMes ?? [];
  const total = meses.reduce((suma, m) => suma + (m.pedidos ?? 0), 0);
  return (
    <MarcoGrafico
      titulo={`Pedidos por mes - ${anio}`}
      descripcion={``}
      cargando={estado.cargando}
      error={estado.error}
      vacio={datos && meses.length === 0 ? 'Sin datos para estos filtros.' : null}
      className={estado.className}
      tabla={{
        columnas: ['Mes', 'Pedidos creados'],
        numericas: [1],
        filas: meses.map((m) => [MESES_LARGOS[m.mes - 1], m.pedidos ?? 'aún no llega']),
      }}
    >
      <GraficoBarras
        orientacion="vertical"
        alto="h-56 lg:h-72"
        sinValor="aún no llega"
        datos={meses.map((m) => ({
          clave: String(m.mes),
          etiqueta: MESES[m.mes - 1],
          nombre: `${MESES_LARGOS[m.mes - 1]} de ${anio}`,
          valor: m.pedidos,
          texto: m.pedidos === null ? undefined : String(m.pedidos),
        }))}
      />
    </MarcoGrafico>
  );
}

/** A lo sumo seis segmentos: los cinco años más recientes y el resto como «Anteriores». */
function segmentosPorAnio(porAnio: Estadisticas['porAnio']): SegmentoDona[] {
  const conPedidos = porAnio.filter((a) => a.pedidos > 0).sort((a, b) => b.anio - a.anio);
  const recientes = conPedidos.slice(0, conPedidos.length > 6 ? 5 : 6);
  const resto = conPedidos.slice(recientes.length);
  const segmentos = recientes.map((a) => ({ clave: String(a.anio), nombre: String(a.anio), valor: a.pedidos }));
  if (resto.length > 0) {
    segmentos.push({
      clave: 'anteriores',
      nombre: `${resto[resto.length - 1].anio}–${resto[0].anio}`,
      valor: resto.reduce((suma, a) => suma + a.pedidos, 0),
    });
  }
  return segmentos;
}

function PedidosPorAnio({ datos, ...estado }: PropsGrafico) {
  const segmentos = segmentosPorAnio(datos?.porAnio ?? []);
  return (
    <MarcoGrafico
      titulo="Distribución de pedidos por año"
      descripcion=""
      cargando={estado.cargando}
      error={estado.error}
      vacio={datos && segmentos.length === 0 ? 'Todavía no hay pedidos registrados.' : null}
      className={estado.className}
      tabla={{
        columnas: ['Año', 'Pedidos', 'Porcentaje'],
        numericas: [1, 2],
        filas: segmentos.map((s) => {
          const total = segmentos.reduce((suma, x) => suma + x.valor, 0);
          return [s.nombre, s.valor, `${((s.valor / total) * 100).toLocaleString('es-EC', { maximumFractionDigits: 1 })} %`];
        }),
      }}
      pie={segmentos.length === 1 ? (
        <p className="text-xs text-muted-foreground">Solo hay pedidos registrados en {segmentos[0].nombre}</p>
      ) : null}
    >
      <GraficoDona segmentos={segmentos} unidad="pedidos" rotuloTotal="pedidos en total" />
    </MarcoGrafico>
  );
}

function EstadoPedidos({ datos, periodo, ...estado }: PropsGrafico & { periodo: string }) {
  const e = datos?.estado;
  const total = e ? e.enCurso + e.finalizados : 0;
  const pct = (n: number, de: number) => (de === 0 ? '0 %' : `${Math.round((n / de) * 100)} %`);
  const retrasos = datos?.retrasosPorEtapa ?? [];
  return (
    <MarcoGrafico
      titulo="Estado de los pedidos"
      descripcion={``}
      cargando={estado.cargando}
      error={estado.error}
      vacio={datos && total === 0 ? 'No hay pedidos creados en este período.' : null}
      className={estado.className}
      tabla={e ? {
        columnas: ['Estado', 'Pedidos', 'Base'],
        numericas: [1],
        filas: [
          ['En curso', e.enCurso, `${pct(e.enCurso, total)} de ${total}`],
          ['Finalizados', e.finalizados, `${pct(e.finalizados, total)} de ${total}`],
          ['En curso con retraso vigente', e.enCursoConRetraso, `de ${e.enCurso} en curso`],
          ['En curso con urgencia activa', e.enCursoConUrgencia, `de ${e.enCurso} en curso`],
          ['En curso con demora reportada', e.enCursoConDemora, `de ${e.enCurso} en curso`],
        ],
      } : undefined}
      leyenda={e ? (
        <ul className="flex flex-wrap gap-x-4 gap-y-1 text-xs" aria-label="Leyenda">
          <li className="flex items-center gap-1.5"><Muestra clase="bg-grafico-serie" />En curso <b className="tabular-nums">{e.enCurso}</b> ({pct(e.enCurso, total)})</li>
          <li className="flex items-center gap-1.5"><Muestra clase="bg-grafico-finalizado" />Finalizados <b className="tabular-nums">{e.finalizados}</b> ({pct(e.finalizados, total)})</li>
        </ul>
      ) : null}
    >
      {e && (
        <div className="flex flex-col gap-4">
          <div className="flex h-5 w-full gap-0.5 overflow-hidden rounded-[4px]" role="img" aria-label={`En curso ${e.enCurso}, finalizados ${e.finalizados}, de ${total}`}>
            {e.enCurso > 0 && <span className="bg-grafico-serie" style={{ width: `${(e.enCurso / total) * 100}%` }} />}
            {e.finalizados > 0 && <span className="bg-grafico-finalizado" style={{ width: `${(e.finalizados / total) * 100}%` }} />}
          </div>
          <div className="flex flex-col gap-1.5">
            <p className="text-xs font-medium text-muted-foreground">Dentro de los {e.enCurso} en curso (pueden coincidir)</p>
            <GraficoBarras
              anchoEtiqueta="11rem"
              maximo={Math.max(1, e.enCurso)}
              datos={[
                { clave: 'retraso', etiqueta: <><AlertTriangleIcon aria-hidden="true" className="size-3.5 shrink-0 text-estado-vencido" />Con retraso vigente</>, nombre: 'En curso con retraso vigente', valor: e.enCursoConRetraso, texto: `${e.enCursoConRetraso} de ${e.enCurso}`, serie: 'alerta' },
                { clave: 'urgencia', etiqueta: <><ZapIcon aria-hidden="true" className="size-3.5 shrink-0" />Con urgencia activa</>, nombre: 'En curso con urgencia activa', valor: e.enCursoConUrgencia, texto: `${e.enCursoConUrgencia} de ${e.enCurso}` },
                { clave: 'demora', etiqueta: <><TruckIcon aria-hidden="true" className="size-3.5 shrink-0" />Con demora reportada</>, nombre: 'En curso con demora reportada', valor: e.enCursoConDemora, texto: `${e.enCursoConDemora} de ${e.enCurso}` },
              ]}
            />
          </div>
          <div className="flex flex-col gap-1 border-t pt-3">
            
            {retrasos.length === 0 ? (
              <p className="text-xs text-muted-foreground"></p>
            ) : (
              <ul className="flex flex-col gap-0.5 text-xs">
                {retrasos.map((r) => (
                  <li key={`${r.tipoFlujo}-${r.etapa}`} className="flex justify-between gap-2">
                    <span className="truncate">{r.tipoFlujo}{r.etapa} · {r.nombreEtapa}</span>
                    <span className="shrink-0 tabular-nums"><b>{r.pedidos}</b> <span className="text-muted-foreground">(etapa vencida {r.etapaVencida}, urgencia vencida {r.urgenciaVencida})</span></span>
                  </li>
                ))}
              </ul>
            )}
          </div>
        </div>
      )}
    </MarcoGrafico>
  );
}

function TiempoPorEtapa({ datos, periodo, ...estado }: PropsGrafico & { periodo: string }) {
  const filas = datos?.tiempoPorEtapa ?? [];
  const multipais = (datos?.paises.length ?? 0) > 1;
  const unidad = unidadPara(Math.max(0, ...filas.map((f) => f.medianaSegundos ?? 0)));
  const grupos = new Map<string, typeof filas>();
  for (const fila of filas) {
    const clave = `${fila.tipoFlujo}-${fila.pais}`;
    grupos.set(clave, [...(grupos.get(clave) ?? []), fila]);
  }
  const maximo = Math.max(0, ...filas.map((f) => f.medianaSegundos ?? 0));
  return (
    <MarcoGrafico
      titulo="¿Cuánto tarda cada etapa?"
      descripcion={`Tiempo típico que un pedido pasa en cada etapa.`}
      cargando={estado.cargando}
      error={estado.error}
      vacio={datos && filas.length === 0 ? 'Todavía no hay etapas terminadas en este período.' : null}
      className={estado.className}
      tabla={{
        columnas: ['Etapa', 'Tiempo típico (mediana)', 'Promedio', 'Veces medidas', 'Abiertas ahora'],
        numericas: [1, 2, 3, 4],
        filas: filas.map((f) => [
          `${f.tipoFlujo}${f.etapa} · ${f.nombreEtapa}${multipais ? ` · ${nombrePais(f.pais)}` : ''}`,
          f.medianaSegundos === null ? 'sin datos' : formatearDuracion(f.medianaSegundos),
          f.promedioSegundos === null ? '—' : formatearDuracion(f.promedioSegundos),
          f.terminadas,
          f.abiertas,
        ]),
      }}
    >
      <div className="grid gap-x-10 gap-y-6 xl:grid-cols-2">
        {[...grupos.entries()].map(([clave, etapas]) => (
          <RecorridoTipico key={clave} etapas={etapas} unidad={unidad} maximo={maximo} titulo={`Flujo ${etapas[0].tipoFlujo}${multipais ? ` · ${nombrePais(etapas[0].pais)}` : ''}`} />
        ))}
      </div>
    </MarcoGrafico>
  );
}

type FilaEtapa = Estadisticas['tiempoPorEtapa'][number];

/**
 * Un flujo en dos lecturas: una frase con la etapa más lenta, una barra del recorrido típico (cada tramo es una etapa,
 * proporcional a su tiempo típico) y la lista de etapas con su barra. La más lenta se marca con texto, no solo color.
 */
function RecorridoTipico({ etapas, unidad, maximo, titulo }: { etapas: FilaEtapa[]; unidad: Unidad; maximo: number; titulo: string }) {
  const medidas = etapas.filter((f) => f.medianaSegundos !== null && f.medianaSegundos > 0);
  const total = medidas.reduce((suma, f) => suma + (f.medianaSegundos ?? 0), 0);
  const lenta = medidas.reduce<FilaEtapa | null>((mayor, f) => (mayor === null || (f.medianaSegundos ?? 0) > (mayor.medianaSegundos ?? 0) ? f : mayor), null);
  const codigo = (f: FilaEtapa) => `${f.tipoFlujo}${f.etapa}`;
  return (
    <section aria-label={titulo} className="flex min-w-0 flex-col gap-3">
      <div className="flex flex-col gap-1">
        <h4 className="text-sm font-semibold">{titulo}</h4>
        {lenta ? (
          <p className="text-sm text-muted-foreground">
            La etapa que más tarda es <span className="font-medium text-foreground">{codigo(lenta)} · {lenta.nombreEtapa}</span>{' '}
            ({formatearDuracion(lenta.medianaSegundos ?? 0)}). Sumando los tiempos típicos, recorrer estas etapas lleva
            unos <span className="font-medium text-foreground tabular-nums">{formatearDuracion(total)}</span>.
          </p>
        ) : (
          <p className="text-sm text-muted-foreground">Sin etapas terminadas todavía.</p>
        )}
      </div>
      {medidas.length > 0 && (
        <div className="flex flex-col gap-1">
          <p className="text-xs text-muted-foreground">Recorrido típico, de la primera etapa a la última</p>
          <div className="flex h-9 w-full gap-0.5 overflow-hidden rounded-md" role="img" aria-label={`${titulo}: recorrido típico de ${formatearDuracion(total)}; ${medidas.map((f) => `${codigo(f)} ${formatearDuracion(f.medianaSegundos ?? 0)}`).join(', ')}`}>
            {medidas.map((f) => {
              const ancho = ((f.medianaSegundos ?? 0) / total) * 100;
              const esLenta = f === lenta;
              return (
                <span
                  key={f.etapa}
                  tabIndex={0}
                  className={cn(
                    'group relative flex min-w-1 items-center justify-center text-xs font-medium outline-none focus-visible:ring-2 focus-visible:ring-ring focus-visible:ring-inset',
                    esLenta ? 'bg-marca-naranja text-gris-900' : 'bg-grafico-serie text-white',
                  )}
                  style={{ width: `${ancho}%` }}
                  aria-label={`${codigo(f)} ${f.nombreEtapa}: ${formatearDuracion(f.medianaSegundos ?? 0)}, ${Math.round(ancho)} % del recorrido`}
                >
                  {ancho >= 7 && <span aria-hidden="true" className="truncate px-0.5">{codigo(f)}</span>}
                  <Ayuda>
                    <b>{codigo(f)} · {f.nombreEtapa}</b><br />
                    Tiempo típico: {formatearDuracion(f.medianaSegundos ?? 0)} ({Math.round(ancho)} % del recorrido)
                  </Ayuda>
                </span>
              );
            })}
          </div>
        </div>
      )}
      <GraficoBarras
        anchoEtiqueta="12rem"
        maximo={maximo}
        sinValor="sin datos"
        datos={etapas.map((f) => ({
          clave: `${f.tipoFlujo}-${f.pais}-${f.etapa}`,
          etiqueta: (
            <span className={cn('truncate', f === lenta && 'font-medium text-foreground')}>
              {codigo(f)} · {f.nombreEtapa}
            </span>
          ),
          nombre: `${codigo(f)} ${f.nombreEtapa}`,
          valor: f.medianaSegundos,
          serie: f === lenta ? 'abierto' : 'principal',
          texto: f.medianaSegundos === null ? undefined : `${enUnidad(f.medianaSegundos, unidad)}${f === lenta ? ' · la más lenta' : ''}`,
          ayuda: (
            <>
              <b>{codigo(f)} · {f.nombreEtapa}</b><br />
              Tiempo típico: {f.medianaSegundos === null ? 'sin datos' : formatearDuracion(f.medianaSegundos)}<br />
              Promedio: {f.promedioSegundos === null ? '—' : formatearDuracion(f.promedioSegundos)}<br />
              Medido {f.terminadas} {f.terminadas === 1 ? 'vez' : 'veces'} · {f.abiertas} {f.abiertas === 1 ? 'pedido está' : 'pedidos están'} ahora en esta etapa
            </>
          ),
        }))}
      />
    </section>
  );
}

// Antigüedad de los pedidos en curso, en tramos fáciles de leer (días corridos desde la creación). El tramo de más
// de 85 días coincide con la meta propuesta de pedido a llegada.
const TRAMOS: { clave: string; etiqueta: string; nombre: string; hasta: number }[] = [
  { clave: 'd1', etiqueta: '< 1 día', nombre: 'Menos de 1 día', hasta: 1 },
  { clave: 'd7', etiqueta: '1–7 d', nombre: 'De 1 a 7 días', hasta: 7 },
  { clave: 'd30', etiqueta: '8–30 d', nombre: 'De 8 a 30 días', hasta: 30 },
  { clave: 'd60', etiqueta: '31–60 d', nombre: 'De 31 a 60 días', hasta: 60 },
  { clave: 'd85', etiqueta: '61–85 d', nombre: 'De 61 a 85 días', hasta: 85 },
  { clave: 'mas', etiqueta: '> 85 d', nombre: 'Más de 85 días', hasta: Number.POSITIVE_INFINITY },
];

function PedidosEnCurso({ datos, periodo, ...estado }: PropsGrafico & { periodo: string }) {
  const abiertos = (datos?.tiempoPorPedido ?? []).filter((p) => !p.finalizado);
  const recortado = (datos?.tiempoPorPedidoTotal ?? 0) > (datos?.tiempoPorPedido.length ?? 0);
  const porTramo = TRAMOS.map((tramo, i) => {
    const desde = i === 0 ? 0 : TRAMOS[i - 1].hasta;
    const pedidos = abiertos.filter((p) => p.segundos / 86_400 >= desde && p.segundos / 86_400 < tramo.hasta);
    return { ...tramo, pedidos };
  });
  const masAntiguo = abiertos.reduce<(typeof abiertos)[number] | null>((mayor, p) => (mayor === null || p.segundos > mayor.segundos ? p : mayor), null);
  return (
    <MarcoGrafico
      titulo="¿Cuánto llevan los pedidos en curso?"
      descripcion={``}
      cargando={estado.cargando}
      error={estado.error}
      vacio={datos && abiertos.length === 0 ? 'No hay pedidos en curso creados en este período.' : null}
      className={estado.className}
      tabla={{
        columnas: ['Pedido', 'Lleva', 'Creado'],
        numericas: [1],
        filas: [...abiertos].sort((a, b) => b.segundos - a.segundos).map((p) => [
          <Link key="n" className="underline-offset-2 hover:underline" to={`/pedidos/${p.pedidoId}`}>{p.numero}</Link>,
          formatearDuracion(p.segundos),
          formatearFechaHora(p.creadoEn),
        ]),
      }}
      pie={masAntiguo ? (
        <p className="text-xs text-muted-foreground">
          El más antiguo es{' '}
          <Link className="font-medium text-foreground underline-offset-2 hover:underline" to={`/pedidos/${masAntiguo.pedidoId}`}>{masAntiguo.numero}</Link>
          , creado hace {formatearDuracion(masAntiguo.segundos)}.
        </p>
      ) : null}
    >
      <GraficoBarras
        orientacion="vertical"
        datos={porTramo.map((t) => ({
          clave: t.clave,
          etiqueta: t.etiqueta,
          nombre: `${t.nombre}: ${t.pedidos.length} ${t.pedidos.length === 1 ? 'pedido' : 'pedidos'}`,
          valor: t.pedidos.length,
          serie: t.clave === 'mas' ? 'alerta' : 'abierto',
          ayuda: (
            <>
              <b>{t.nombre}</b>: {t.pedidos.length} {t.pedidos.length === 1 ? 'pedido' : 'pedidos'}
              {t.pedidos.length > 0 && <><br />{t.pedidos.slice(0, 6).map((p) => p.numero).join(', ')}{t.pedidos.length > 6 ? '…' : ''}</>}
            </>
          ),
        }))}
      />
    </MarcoGrafico>
  );
}

/**
 * Duración de los pedidos finalizados: un punto por pedido sobre una sola escala de tiempo y una línea con el tiempo
 * típico (mediana). Así se ve de un vistazo qué es lo normal y qué pedidos se salieron de lo habitual.
 */
function PedidosFinalizados({ datos, periodo, ...estado }: PropsGrafico & { periodo: string }) {
  const finalizados = (datos?.tiempoPorPedido ?? []).filter((p) => p.finalizado).sort((a, b) => a.segundos - b.segundos);
  const maximo = Math.max(0, ...finalizados.map((p) => p.segundos));
  const unidad = unidadPara(maximo);
  const mediana = finalizados.length === 0 ? null : finalizados.length % 2 === 1
    ? finalizados[(finalizados.length - 1) / 2].segundos
    : (finalizados[finalizados.length / 2 - 1].segundos + finalizados[finalizados.length / 2].segundos) / 2;
  const pos = (segundos: number) => (maximo === 0 ? 0 : (segundos / maximo) * 100);
  // Filas para que los puntos cercanos no se tapen (como mucho 4).
  const filasDePuntos: number[] = [];
  const ultimos: number[] = [];
  for (const p of finalizados) {
    const x = pos(p.segundos);
    let fila = ultimos.findIndex((anterior) => x - anterior >= 4);
    if (fila === -1) fila = ultimos.length < 4 ? ultimos.length : ultimos.indexOf(Math.min(...ultimos));
    ultimos[fila] = x;
    filasDePuntos.push(fila);
  }
  const lentos = mediana === null ? [] : finalizados.filter((p) => p.segundos > mediana * 2);
  return (
    <MarcoGrafico
      titulo="¿Cuánto tardaron los pedidos finalizados?"
      descripcion={``}
      cargando={estado.cargando}
      error={estado.error}
      vacio={datos && finalizados.length === 0 ? 'Ningún pedido creado en este período ha finalizado.' : null}
      className={estado.className}
      tabla={{
        columnas: ['Pedido', 'Duración', 'Creado', 'Finalizado'],
        numericas: [1],
        filas: [...finalizados].reverse().map((p) => [
          <Link key="n" className="underline-offset-2 hover:underline" to={`/pedidos/${p.pedidoId}`}>{p.numero}</Link>,
          formatearDuracion(p.segundos),
          formatearFechaHora(p.creadoEn),
          formatearFechaHora(p.finalizadoEn),
        ]),
      }}
      pie={mediana !== null ? (
        <p className="text-xs text-muted-foreground">
          Lo normal es unos <span className="font-medium text-foreground">{formatearDuracion(mediana)}</span>.{' '}
          {lentos.length > 0
            ? `${lentos.length} ${lentos.length === 1 ? 'pedido tardó' : 'pedidos tardaron'} más del doble: ${lentos.slice(-3).map((p) => p.numero).join(', ')}.`
            : 'Ninguno tardó más del doble de lo normal.'}
          {finalizados.length < 5 ? ' Hay pocos casos todavía.' : ''}
        </p>
      ) : null}
    >
      <div className="flex flex-col gap-1 pt-2">
        <div className="relative h-24 border-b border-border" role="group" aria-label="Duración de cada pedido finalizado">
          {mediana !== null && (
            <span aria-hidden="true" className="absolute top-0 bottom-0 w-px border-l-2 border-dashed border-marca-naranja-texto" style={{ left: `${pos(mediana)}%` }}>
              <span className="absolute -top-0.5 left-1 text-xs whitespace-nowrap text-marca-naranja-texto">típico</span>
            </span>
          )}
          {finalizados.map((p, i) => (
            <Link
              key={p.pedidoId}
              to={`/pedidos/${p.pedidoId}`}
              aria-label={`${p.numero}: ${formatearDuracion(p.segundos)}`}
              className="group absolute size-3.5 -translate-x-1/2 rounded-full bg-grafico-serie ring-2 ring-card outline-none hover:scale-125 focus-visible:ring-ring"
              style={{ left: `${pos(p.segundos)}%`, bottom: `${10 + filasDePuntos[i] * 20}px` }}
            >
              <Ayuda>
                <b>{p.numero}</b>: {formatearDuracion(p.segundos)}<br />
                {formatearFechaHora(p.creadoEn)} → {formatearFechaHora(p.finalizadoEn)}
              </Ayuda>
            </Link>
          ))}
        </div>
        <div className="flex justify-between text-xs text-muted-foreground tabular-nums" aria-hidden="true">
          <span>0</span>
          <span>{enUnidad(maximo / 2, unidad)}</span>
          <span>{enUnidad(maximo, unidad)}</span>
        </div>
      </div>
    </MarcoGrafico>
  );
}

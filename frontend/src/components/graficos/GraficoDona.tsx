import { useState } from 'react';
import { cn } from '@/lib/utils';

// Dona de partes de un todo (ADR-0029): pocas categorías, en orden fijo de colores (nunca cíclico), separación de
// 2 px entre segmentos, total en el centro y leyenda con cantidad y porcentaje. Al pasar o enfocar un segmento, el
// centro muestra su detalle. Más de seis categorías se agrupan antes de llegar aquí.

export interface SegmentoDona {
  clave: string;
  nombre: string;
  valor: number;
}

const COLORES = ['var(--grafico-cat-1)', 'var(--grafico-cat-2)', 'var(--grafico-cat-3)', 'var(--grafico-cat-4)', 'var(--grafico-cat-5)', 'var(--grafico-cat-6)'];
const RADIO = 42;
const CIRCUNFERENCIA = 2 * Math.PI * RADIO;
const HUECO = 1.2;

const porcentaje = (valor: number, total: number) =>
  `${(total === 0 ? 0 : (valor / total) * 100).toLocaleString('es-EC', { maximumFractionDigits: 1 })} %`;

export function GraficoDona({ segmentos, unidad, rotuloTotal }: { segmentos: SegmentoDona[]; unidad: string; rotuloTotal: string }) {
  const [activo, setActivo] = useState<string | null>(null);
  const total = segmentos.reduce((suma, s) => suma + s.valor, 0);
  const elegido = segmentos.find((s) => s.clave === activo);
  let acumulado = 0;
  const unico = segmentos.filter((s) => s.valor > 0).length === 1;

  return (
    <div className="flex flex-col items-center gap-4 sm:flex-row sm:items-center lg:flex-col xl:flex-row">
      <svg viewBox="0 0 100 100" className="size-40 shrink-0 -rotate-90" role="group" aria-label={`${rotuloTotal}: ${total} ${unidad}`}>
        <circle cx="50" cy="50" r={RADIO} fill="none" stroke="var(--muted)" strokeWidth="13" />
        {segmentos.map((s, i) => {
          const largo = total === 0 ? 0 : (s.valor / total) * CIRCUNFERENCIA;
          const inicio = acumulado;
          acumulado += largo;
          if (largo === 0) return null;
          return (
            <circle
              key={s.clave}
              cx="50"
              cy="50"
              r={RADIO}
              fill="none"
              stroke={COLORES[i % COLORES.length]}
              strokeWidth={activo === s.clave ? 16 : 13}
              strokeDasharray={`${Math.max(0.5, largo - (unico ? 0 : HUECO))} ${CIRCUNFERENCIA}`}
              strokeDashoffset={-inicio}
              tabIndex={0}
              role="img"
              aria-label={`${s.nombre}: ${s.valor} ${unidad} (${porcentaje(s.valor, total)})`}
              className="cursor-default outline-none transition-[stroke-width] focus-visible:stroke-[17]"
              onMouseEnter={() => setActivo(s.clave)}
              onMouseLeave={() => setActivo(null)}
              onFocus={() => setActivo(s.clave)}
              onBlur={() => setActivo(null)}
            />
          );
        })}
        <g className="rotate-90" style={{ transformOrigin: '50px 50px' }}>
          <text x="50" y="47" textAnchor="middle" className="fill-foreground font-heading text-[15px] font-semibold tabular-nums">
            {elegido ? elegido.valor : total}
          </text>
          <text x="50" y="60" textAnchor="middle" className="fill-muted-foreground text-[7px]">
            {elegido ? `${elegido.nombre} · ${porcentaje(elegido.valor, total)}` : rotuloTotal}
          </text>
        </g>
      </svg>
      <ul className="flex w-full min-w-0 flex-col gap-1.5 text-sm" aria-label="Leyenda">
        {segmentos.map((s, i) => (
          <li
            key={s.clave}
            className={cn('flex items-center justify-between gap-3 rounded px-1', activo === s.clave && 'bg-muted')}
            onMouseEnter={() => setActivo(s.clave)}
            onMouseLeave={() => setActivo(null)}
          >
            <span className="flex items-center gap-2">
              <span aria-hidden="true" className="inline-block size-3 rounded-[3px]" style={{ background: COLORES[i % COLORES.length] }} />
              {s.nombre}
            </span>
            <span className="tabular-nums">
              <span className="font-medium">{s.valor}</span>
              <span className="ml-2 text-muted-foreground">{porcentaje(s.valor, total)}</span>
            </span>
          </li>
        ))}
      </ul>
    </div>
  );
}

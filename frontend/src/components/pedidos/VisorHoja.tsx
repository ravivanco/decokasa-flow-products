import { useState } from 'react';
import { type Hoja, MAX_COLUMNAS, MAX_FILAS } from '@/lib/hojaCalculo';
import { cn } from '@/lib/utils';

/** Letra de columna de Excel: 0 → A, 26 → AA. */
function letra(indice: number): string {
  let n = indice + 1;
  let texto = '';
  while (n > 0) { const r = (n - 1) % 26; texto = String.fromCharCode(65 + r) + texto; n = Math.floor((n - 1) / 26); }
  return texto;
}

/** Tabla de solo lectura de un Excel o CSV, con una pestaña por hoja. */
export function VisorHoja({ hojas }: { hojas: Hoja[] }) {
  const [actual, setActual] = useState(0);
  const hoja = hojas[actual];
  const columnas = Math.max(1, ...(hoja?.filas ?? []).map((f) => f.length));
  return (
    <div className="flex min-h-0 flex-1 flex-col gap-2">
      {hojas.length > 1 && (
        <div role="tablist" aria-label="Hojas" className="flex flex-wrap gap-1">
          {hojas.map((h, i) => (
            <button key={h.nombre + i} type="button" role="tab" aria-selected={i === actual} onClick={() => setActual(i)}
              className={cn('min-h-10 rounded-md border px-3 text-sm focus-visible:outline-none focus-visible:ring-3 focus-visible:ring-ring',
                i === actual ? 'border-primary bg-secondary font-semibold text-primary' : 'text-muted-foreground')}>
              {h.nombre}
            </button>
          ))}
        </div>
      )}
      <div className="min-h-0 flex-1 overflow-auto rounded-md border">
        {hoja && hoja.filas.length > 0 ? (
          <table className="w-max min-w-full border-collapse text-sm tabular-nums">
            <thead className="sticky top-0 bg-muted">
              <tr>
                <th scope="col" className="border-b border-r px-2 py-1 text-xs font-medium text-muted-foreground" />
                {Array.from({ length: columnas }, (_, c) => (
                  <th key={c} scope="col" className="border-b border-r px-2 py-1 text-xs font-medium text-muted-foreground">{letra(c)}</th>
                ))}
              </tr>
            </thead>
            <tbody>
              {hoja.filas.map((fila, f) => (
                <tr key={f} className="odd:bg-card even:bg-muted/30">
                  <th scope="row" className="border-r px-2 py-1 text-right text-xs font-medium text-muted-foreground">{f + 1}</th>
                  {Array.from({ length: columnas }, (_, c) => (
                    <td key={c} className="max-w-80 truncate border-r px-2 py-1" title={fila[c] ?? ''}>{fila[c] ?? ''}</td>
                  ))}
                </tr>
              ))}
            </tbody>
          </table>
        ) : (
          <p className="p-4 text-sm text-muted-foreground">Esta hoja está vacía.</p>
        )}
      </div>
      {hoja?.recortada && (
        <p className="text-xs text-muted-foreground">
          Se muestran las primeras {MAX_FILAS} filas y {MAX_COLUMNAS} columnas. Descarga el archivo para verlo completo.
        </p>
      )}
    </div>
  );
}

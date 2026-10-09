import React, { useState } from 'react';
import { Pedido } from '../types';
import { calculateOrderSummary, getStageDefinitions } from '../utils/orderState';
import { formatDisplayDate } from '../utils/dateUtils';
import { Search, Ship, CheckCircle2, Clock, AlertTriangle, ArrowRight, FileText, User } from 'lucide-react';

interface TrackingSearchViewProps {
  pedidos: Pedido[];
  onOpenOrderDetail: (pedido: Pedido) => void;
}

export const TrackingSearchView: React.FC<TrackingSearchViewProps> = ({
  pedidos,
  onOpenOrderDetail,
}) => {
  const [searchCode, setSearchCode] = useState('DK-EC-2026-0004');
  const [searchedOrder, setSearchedOrder] = useState<Pedido | null>(() => {
    return pedidos.find((p) => p.codigo === 'DK-EC-2026-0004') || pedidos[0];
  });
  const [hasSearched, setHasSearched] = useState(true);

  const handleSearch = (e: React.FormEvent) => {
    e.preventDefault();
    setHasSearched(true);
    const codeClean = searchCode.trim().toUpperCase();
    const found = pedidos.find(
      (p) =>
        p.codigo.toUpperCase() === codeClean ||
        p.codigo.toUpperCase().includes(codeClean) ||
        p.nombre.toLowerCase().includes(searchCode.trim().toLowerCase())
    );
    setSearchedOrder(found || null);
  };

  const summary = searchedOrder ? calculateOrderSummary(searchedOrder) : null;
  const defs = searchedOrder ? getStageDefinitions(searchedOrder.flujo) : [];

  return (
    <div className="space-y-6 sm:space-y-8 animate-in fade-in duration-200">
      {/* Big Search Box (Estilo Servientrega / DHL) */}
      <div className="bg-white rounded-3xl p-6 sm:p-10 border border-[#6C6B6D]/20 shadow-xs text-center space-y-4 max-w-3xl mx-auto">
        <div className="w-14 h-14 rounded-2xl bg-[#FFD100] text-[#2E2E2E] flex items-center justify-center mx-auto shadow-sm">
          <Ship className="w-8 h-8" />
        </div>
        <h1 className="text-2xl sm:text-3xl font-black text-[#2E2E2E]">
          Rastreo de Pedido de Importación
        </h1>
        <p className="text-xs sm:text-sm text-[#515151] max-w-lg mx-auto">
          Ingresa el número de pedido único de Decokasa para consultar el flujo completo, la etapa actual y el resumen de lo ocurrido en cada etapa.
        </p>

        <form onSubmit={handleSearch} className="flex flex-col sm:flex-row gap-2 max-w-xl mx-auto pt-2">
          <div className="relative flex-1">
            <Search className="w-5 h-5 text-[#6C6B6D] absolute left-4 top-1/2 -translate-y-1/2" />
            <input
              type="text"
              required
              placeholder="Ej: DK-EC-2026-0004 o nombre..."
              value={searchCode}
              onChange={(e) => setSearchCode(e.target.value)}
              className="w-full pl-12 pr-4 py-3.5 rounded-2xl border border-[#6C6B6D]/30 font-mono font-bold text-sm text-[#2E2E2E] focus:outline-hidden focus:ring-2 focus:ring-[#FFD100]"
            />
          </div>
          <button
            type="submit"
            className="py-3.5 px-6 rounded-2xl bg-[#FFD100] hover:bg-[#E6BC00] text-[#2E2E2E] font-black text-sm transition shadow-md flex items-center justify-center gap-2 cursor-pointer shrink-0"
          >
            <span>Rastrear</span>
            <ArrowRight className="w-4 h-4" />
          </button>
        </form>

        {/* Ejemplos rápidos para hacer clic */}
        <div className="flex flex-wrap items-center justify-center gap-1.5 pt-1 text-[11px] text-[#6C6B6D]">
          <span>Ejemplos rápidos:</span>
          {pedidos.slice(0, 4).map((p) => (
            <button
              key={p.id}
              type="button"
              onClick={() => {
                setSearchCode(p.codigo);
                setSearchedOrder(p);
                setHasSearched(true);
              }}
              className="px-2 py-0.5 rounded-md bg-slate-100 hover:bg-[#FFF6CC] text-[#2E2E2E] font-mono font-bold transition cursor-pointer"
            >
              {p.codigo}
            </button>
          ))}
        </div>
      </div>

      {/* Resultados del Rastreo */}
      {hasSearched && searchedOrder && summary && (
        <div className="space-y-6">
          {/* Card Resumen de Estado estilo Tracking Courier */}
          <div className="bg-white rounded-3xl p-6 sm:p-8 border border-[#6C6B6D]/20 shadow-xs space-y-6">
            <div className="flex flex-col md:flex-row md:items-center justify-between gap-4 border-b border-slate-100 pb-5">
              <div>
                <div className="flex items-center gap-2 mb-1">
                  <span className="px-2.5 py-0.5 rounded-full text-xs font-black uppercase bg-slate-100 text-[#515151]">
                    {searchedOrder.flujo === 'A'
                      ? 'Flujo A · Pedido Ecuador'
                      : 'Flujo B · Propuesto por China'}
                  </span>
                  <span className="font-mono text-base font-black text-[#2E2E2E]">
                    {searchedOrder.codigo}
                  </span>
                </div>
                <h2 className="text-xl sm:text-2xl font-black text-[#2E2E2E]">
                  {searchedOrder.nombre}
                </h2>
                <p className="text-xs text-[#515151] mt-0.5">
                  Creado por: <strong>{searchedOrder.creadoPor}</strong> · Fecha de pedido:{' '}
                  <strong className="font-mono">{formatDisplayDate(searchedOrder.fechaPedido)}</strong>
                </p>
              </div>

              <div className="flex flex-col sm:flex-row sm:items-center gap-3">
                <div className="p-3.5 rounded-2xl bg-slate-50 border border-slate-200 text-right">
                  <div className="text-[10px] font-black uppercase tracking-wider text-[#6C6B6D]">
                    Llegada Estimada a Bodega
                  </div>
                  <div className="text-base sm:text-lg font-black font-mono text-[#2E2E2E]">
                    {summary.etaBodega}
                  </div>
                  <div
                    className={`text-xs font-black mt-0.5 ${
                      summary.semaforo === 'rojo'
                        ? 'text-[#DC2626]'
                        : summary.semaforo === 'naranja'
                        ? 'text-[#EA580C]'
                        : 'text-[#16A34A]'
                    }`}
                  >
                    {summary.aviso}
                  </div>
                </div>

                <button
                  type="button"
                  onClick={() => onOpenOrderDetail(searchedOrder)}
                  className="px-5 py-3 rounded-2xl bg-[#FFD100] hover:bg-[#E6BC00] text-[#2E2E2E] font-black text-xs transition shadow-2xs flex items-center justify-center gap-2 cursor-pointer h-full"
                >
                  <span>Abrir Expediente Completo</span>
                  <ArrowRight className="w-4 h-4" />
                </button>
              </div>
            </div>

            {/* Barra de Progreso y Línea de Estados Horizontal */}
            <div className="space-y-4">
              <div className="flex items-center justify-between text-xs font-bold text-[#515151]">
                <span>
                  Etapa actual: <strong>Etapa {searchedOrder.etapaActualNumero} de {defs.length}</strong>
                </span>
                <span>{summary.progresoPorcentaje}% completado</span>
              </div>
              <div className="w-full bg-slate-100 rounded-full h-3 overflow-hidden p-0.5">
                <div
                  className="bg-[#FFD100] h-full rounded-full transition-all duration-500"
                  style={{ width: `${summary.progresoPorcentaje}%` }}
                />
              </div>

              {/* Pasos horizontales tipo Tracking */}
              <div className="grid grid-cols-2 sm:grid-cols-4 lg:grid-cols-6 gap-2 pt-2">
                {searchedOrder.etapas.map((etapa) => {
                  const isCompleted = etapa.completada;
                  const isCurrent = etapa.numero === searchedOrder.etapaActualNumero;
                  return (
                    <div
                      key={etapa.numero}
                      className={`p-3 rounded-2xl border text-xs transition ${
                        isCurrent
                          ? 'bg-[#FFFBEA] border-2 border-[#FFD100] ring-2 ring-[#FFD100]/20 shadow-xs'
                          : isCompleted
                          ? 'bg-slate-50 border-slate-200'
                          : 'bg-white border-slate-200 opacity-60'
                      }`}
                    >
                      <div className="flex items-center justify-between mb-1">
                        <span className="font-mono text-[10px] font-black text-[#6C6B6D]">
                          Etapa {etapa.numero}
                        </span>
                        {isCompleted ? (
                          <CheckCircle2 className="w-3.5 h-3.5 text-emerald-600" />
                        ) : isCurrent ? (
                          <span className="w-2 h-2 rounded-full bg-amber-500 animate-ping" />
                        ) : (
                          <Clock className="w-3.5 h-3.5 text-slate-300" />
                        )}
                      </div>
                      <div className="font-black text-[#2E2E2E] leading-tight truncate">
                        {etapa.nombre}
                      </div>
                      <div className="text-[10px] text-[#6C6B6D] mt-0.5 truncate">
                        {isCompleted
                          ? `Fin: ${formatDisplayDate(etapa.fechaRealFin)}`
                          : isCurrent
                          ? `Plazo: ${etapa.plazoTexto}`
                          : `Est: ${formatDisplayDate(etapa.fechaFinEstimada)}`}
                      </div>
                    </div>
                  );
                })}
              </div>
            </div>

            {/* Resumen de lo ocurrido en cada etapa anterior (Pedido del cliente) */}
            <div className="pt-4 border-t border-slate-100 space-y-3">
              <h3 className="text-xs font-black uppercase tracking-wider text-[#515151]">
                Resumen de lo ocurrido en las etapas anteriores:
              </h3>

              <div className="space-y-2">
                {searchedOrder.etapas
                  .filter((e) => e.completada || e.numero === searchedOrder.etapaActualNumero)
                  .map((etapa) => (
                    <div
                      key={etapa.numero}
                      className="p-3 bg-slate-50 rounded-xl border border-slate-100 flex flex-col sm:flex-row sm:items-center justify-between gap-2 text-xs"
                    >
                      <div className="flex items-center gap-2">
                        <span
                          className={`w-6 h-6 rounded-lg flex items-center justify-center font-mono font-black text-xs ${
                            etapa.completada ? 'bg-emerald-100 text-emerald-800' : 'bg-[#FFD100] text-[#2E2E2E]'
                          }`}
                        >
                          {etapa.numero}
                        </span>
                        <div>
                          <strong className="text-[#2E2E2E]">{etapa.nombre}</strong>
                          <span className="text-[#6C6B6D] ml-2">
                            {etapa.completada
                              ? `✓ Aprobada por ${etapa.completadoPor || 'Responsable'} el ${formatDisplayDate(etapa.fechaRealFin)}`
                              : `⏳ Etapa actual en curso · Esperando check`}
                          </span>
                        </div>
                      </div>

                      {etapa.motivoRetraso && (
                        <div className="text-[11px] text-amber-800 bg-amber-50 px-2.5 py-1 rounded-lg">
                          Nota: {etapa.motivoRetraso}
                        </div>
                      )}
                    </div>
                  ))}
              </div>
            </div>
          </div>
        </div>
      )}

      {hasSearched && !searchedOrder && (
        <div className="bg-white rounded-3xl p-12 text-center border border-[#6C6B6D]/20 space-y-2 max-w-lg mx-auto">
          <AlertTriangle className="w-10 h-10 text-amber-500 mx-auto" />
          <h3 className="text-base font-black text-[#2E2E2E]">Pedido no encontrado</h3>
          <p className="text-xs text-[#515151]">
            No encontramos ningún pedido con el código <strong>{searchCode}</strong>. Revisa los códigos de ejemplo (DK-EC-2026-0001 al 0008).
          </p>
        </div>
      )}
    </div>
  );
};

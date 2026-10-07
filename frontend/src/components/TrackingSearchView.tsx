import React, { useState } from 'react';
import { Pedido } from '../types';
import { calculateOrderSummary } from '../utils/orderState';
import { HorizontalStatusLine } from './HorizontalStatusLine';
import { formatDisplayDate } from '../utils/dateUtils';
import { Search, Ship, AlertTriangle, Clock, CheckCircle2, ArrowRight } from 'lucide-react';

interface TrackingSearchViewProps {
  pedidos: Pedido[];
  onOpenOrderDetail: (pedido: Pedido) => void;
}

export const TrackingSearchView: React.FC<TrackingSearchViewProps> = ({
  pedidos,
  onOpenOrderDetail,
}) => {
  const [searchCode, setSearchCode] = useState('IMP-2026-006-PANEL-EXT-WPC');
  const [searchedOrder, setSearchedOrder] = useState<Pedido | null>(() => {
    return pedidos.find((p) => p.codigo.includes('006')) || pedidos[0];
  });
  const [hasSearched, setHasSearched] = useState(true);

  const handleSearch = (e: React.FormEvent) => {
    e.preventDefault();
    setHasSearched(true);
    const codeClean = searchCode.trim().toUpperCase();
    const found = pedidos.find(
      (p) => p.codigo.toUpperCase() === codeClean || p.codigo.toUpperCase().includes(codeClean)
    );
    setSearchedOrder(found || null);
  };

  const summary = searchedOrder ? calculateOrderSummary(searchedOrder) : null;

  return (
    <div className="space-y-6 sm:space-y-8 animate-in fade-in duration-200">
      {/* Big Centered Search Box (Estilo Servientrega) */}
      <div className="bg-white rounded-3xl p-8 sm:p-12 border border-[#6C6B6D]/20 shadow-xs text-center space-y-4 max-w-3xl mx-auto">
        <div className="w-14 h-14 rounded-2xl bg-[#FFD100] text-[#2E2E2E] flex items-center justify-center mx-auto shadow-sm">
          <Ship className="w-8 h-8" />
        </div>
        <h1 className="text-2xl sm:text-3xl font-black text-[#2E2E2E]">
          Rastrear Pedido de Importación
        </h1>
        <p className="text-xs sm:text-sm text-[#515151] max-w-lg mx-auto">
          Ingresa el código de importación de Decokasa para consultar la etapa en tiempo real, el cronograma y el semáforo de llegada.
        </p>

        <form onSubmit={handleSearch} className="flex flex-col sm:flex-row gap-2 max-w-xl mx-auto pt-2">
          <div className="relative flex-1">
            <Search className="w-5 h-5 text-[#6C6B6D] absolute left-4 top-1/2 -translate-y-1/2" />
            <input
              type="text"
              required
              placeholder="Ej: IMP-2026-006-PANEL-EXT-WPC"
              value={searchCode}
              onChange={(e) => setSearchCode(e.target.value)}
              className="w-full pl-12 pr-4 py-3.5 rounded-2xl border border-[#6C6B6D]/30 font-mono font-bold text-sm text-[#2E2E2E] focus:outline-hidden focus:ring-2 focus:ring-[#FFD100]"
            />
          </div>
          <button
            type="submit"
            className="py-3.5 px-8 rounded-2xl font-black text-sm bg-[#FFD100] hover:bg-[#E6BC00] text-[#2E2E2E] transition shadow-md cursor-pointer shrink-0"
          >
            Rastrear
          </button>
        </form>

        {/* Quick chip examples */}
        <div className="flex flex-wrap items-center justify-center gap-2 pt-2 text-[11px] text-[#6C6B6D]">
          <span className="font-bold">Ejemplos para probar:</span>
          {['IMP-2026-006-PANEL-EXT-WPC', 'IMP-2026-001-PISO-SPC', 'IMP-2026-003-PAPEL-TAPIZ'].map((ex) => (
            <button
              key={ex}
              type="button"
              onClick={() => {
                setSearchCode(ex);
                const found = pedidos.find((p) => p.codigo === ex);
                setSearchedOrder(found || null);
              }}
              className="font-mono font-bold px-2.5 py-1 rounded-lg bg-[#FFFBEA] border border-[#FFD100] text-[#2E2E2E] hover:bg-[#FFD100] transition cursor-pointer"
            >
              {ex}
            </button>
          ))}
        </div>
      </div>

      {/* Result Section */}
      {searchedOrder && summary ? (
        <div className="space-y-6">
          {/* Summary Box */}
          <div className="bg-white rounded-3xl border border-[#6C6B6D]/20 shadow-xs p-6 sm:p-8 space-y-4">
            <div className="flex flex-col sm:flex-row sm:items-center justify-between gap-4 border-b border-slate-100 pb-4">
              <div>
                <span className="font-mono text-xs font-black px-3 py-1 rounded-lg bg-[#FFD100] text-[#2E2E2E]">
                  {searchedOrder.codigo}
                </span>
                <h2 className="text-xl sm:text-2xl font-black text-[#2E2E2E] mt-2">
                  {searchedOrder.nombre}
                </h2>
                <p className="text-xs text-[#515151] mt-0.5">
                  Fecha del pedido: {formatDisplayDate(searchedOrder.fechaPedido)} · Bodega destino: {searchedOrder.bodegaDestino}
                </p>
              </div>

              <div className="flex items-center gap-2">
                <span
                  className={`inline-flex items-center gap-1.5 px-4 py-2 rounded-xl text-xs font-black ${
                    summary.semaforo === 'rojo'
                      ? 'bg-rose-100 text-[#DC2626]'
                      : summary.semaforo === 'naranja'
                      ? 'bg-orange-100 text-[#EA580C]'
                      : 'bg-emerald-100 text-[#16A34A]'
                  }`}
                >
                  {summary.semaforo === 'rojo' && <AlertTriangle className="w-4 h-4" />}
                  {summary.semaforo === 'naranja' && <Clock className="w-4 h-4" />}
                  {summary.semaforo === 'verde' && <CheckCircle2 className="w-4 h-4" />}
                  <span>{summary.aviso}</span>
                </span>
              </div>
            </div>

            {/* Quick Metrics */}
            <div className="grid grid-cols-2 sm:grid-cols-4 gap-3 p-4 rounded-2xl bg-[#FFFBEA] text-xs">
              <div>
                <span className="text-[#6C6B6D] block font-semibold">Estado actual:</span>
                <strong className="text-[#2E2E2E] font-black">{searchedOrder.estado}</strong>
              </div>
              <div>
                <span className="text-[#6C6B6D] block font-semibold">Etapa en curso:</span>
                <strong className="text-[#2E2E2E] font-black">
                  {searchedOrder.etapaActualNumero}. {searchedOrder.etapas[searchedOrder.etapaActualNumero - 1]?.nombre}
                </strong>
              </div>
              <div>
                <span className="text-[#6C6B6D] block font-semibold">Llegada est. a bodega:</span>
                <strong className="text-[#2E2E2E] font-black">{summary.etaBodega}</strong>
              </div>
              <div>
                <span className="text-[#6C6B6D] block font-semibold">Contenedor:</span>
                <strong className="text-[#2E2E2E] font-mono font-bold truncate block">
                  {searchedOrder.contenedor || 'Por asignar'}
                </strong>
              </div>
            </div>

            {/* View Full Detail Button */}
            <div className="pt-2 flex justify-end">
              <button
                type="button"
                onClick={() => onOpenOrderDetail(searchedOrder)}
                className="px-5 py-2.5 rounded-xl font-black text-xs bg-[#2E2E2E] hover:bg-black text-white flex items-center gap-2 shadow-sm transition cursor-pointer"
              >
                <span>Ver gestión completa del pedido</span>
                <ArrowRight className="w-3.5 h-3.5 stroke-[3]" />
              </button>
            </div>
          </div>

          {/* Read-Only Horizontal Status Line */}
          <HorizontalStatusLine
            pedido={searchedOrder}
            selectedStageNum={searchedOrder.etapaActualNumero}
            onSelectStage={(num) => {
              onOpenOrderDetail(searchedOrder);
            }}
            readOnly={true}
          />
        </div>
      ) : hasSearched ? (
        <div className="bg-white rounded-3xl border border-[#6C6B6D]/20 p-12 text-center text-[#515151]">
          <p className="text-sm font-bold">
            No se encontró ningún pedido con el código ingresado.
          </p>
          <p className="text-xs text-[#6C6B6D] mt-1">
            Revisa el código o consulta la lista completa en la sección Pedidos.
          </p>
        </div>
      ) : null}
    </div>
  );
};

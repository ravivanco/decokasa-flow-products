import React, { useRef, useEffect } from 'react';
import { Pedido } from '../types';
import { formatDisplayDate } from '../utils/dateUtils';
import { getStageDefinitions } from '../utils/orderState';
import {
  FilePlus,
  Tag,
  CheckCheck,
  Search,
  Factory,
  Ship,
  Anchor,
  ShieldCheck,
  Warehouse,
  FileCheck,
  Flag,
  Sparkles,
  CheckCircle2,
  Clock,
} from 'lucide-react';

interface HorizontalStatusLineProps {
  pedido: Pedido;
  selectedStageNum: number;
  onSelectStage: (stageNum: number) => void;
}

export const HorizontalStatusLine: React.FC<HorizontalStatusLineProps> = ({
  pedido,
  selectedStageNum,
  onSelectStage,
}) => {
  const scrollRef = useRef<HTMLDivElement>(null);
  const defs = getStageDefinitions(pedido.flujo);

  useEffect(() => {
    if (scrollRef.current) {
      const activeEl = scrollRef.current.querySelector(`[data-stage="${pedido.etapaActualNumero}"]`);
      if (activeEl) {
        activeEl.scrollIntoView({ behavior: 'smooth', inline: 'center', block: 'nearest' });
      }
    }
  }, [pedido.etapaActualNumero]);

  const getStageIcon = (etapaNombre: string) => {
    if (etapaNombre.includes('Crear solicitud')) return <FilePlus className="w-4 h-4" />;
    if (etapaNombre.includes('propuesto')) return <Sparkles className="w-4 h-4 text-amber-600" />;
    if (etapaNombre.includes('Codificación')) return <Tag className="w-4 h-4" />;
    if (etapaNombre.includes('Revisión')) return <CheckCheck className="w-4 h-4" />;
    if (etapaNombre.includes('Análisis')) return <Search className="w-4 h-4" />;
    if (etapaNombre.includes('Fabricación')) return <Factory className="w-4 h-4" />;
    if (etapaNombre.includes('Embarque')) return <Ship className="w-4 h-4" />;
    if (etapaNombre.includes('Aduana')) return <Anchor className="w-4 h-4" />;
    if (etapaNombre.includes('salida')) return <ShieldCheck className="w-4 h-4" />;
    if (etapaNombre.includes('Bodega')) return <Warehouse className="w-4 h-4" />;
    if (etapaNombre.includes('Incidencias')) return <FileCheck className="w-4 h-4" />;
    return <Flag className="w-4 h-4" />;
  };

  return (
    <div className="bg-white rounded-3xl p-4 sm:p-5 border border-[#6C6B6D]/20 shadow-xs space-y-3">
      <div className="flex items-center justify-between text-xs font-bold text-[#515151] px-1">
        <div className="flex items-center gap-2">
          <span className="font-mono px-2 py-0.5 rounded-md bg-slate-100 text-[#2E2E2E] font-black">
            {pedido.flujo === 'A' ? 'Flujo A (11 Etapas)' : 'Flujo B (12 Etapas)'}
          </span>
          <span>Línea de tiempo de importación:</span>
        </div>
        <span className="text-[11px] text-[#6C6B6D] hidden sm:inline">
          Haz clic en cualquier etapa para ver su estado e historial
        </span>
      </div>

      {/* Horizontal scrollable track */}
      <div ref={scrollRef} className="overflow-x-auto pb-2 scrollbar-thin">
        <div className="flex items-center gap-2 min-w-max p-1">
          {defs.map((def, idx) => {
            const etapa = pedido.etapas[idx];
            const isCompleted = etapa ? etapa.completada : false;
            const isCurrent = def.numero === pedido.etapaActualNumero && pedido.estado !== 'Finalizado';
            const isSelected = def.numero === selectedStageNum;

            return (
              <React.Fragment key={def.id}>
                {/* Stage Button */}
                <button
                  type="button"
                  data-stage={def.numero}
                  onClick={() => onSelectStage(def.numero)}
                  className={`flex flex-col items-center text-center p-3 rounded-2xl border transition cursor-pointer min-w-32 max-w-36 ${
                    isSelected
                      ? 'border-2 border-[#2E2E2E] ring-2 ring-[#FFD100] shadow-sm'
                      : isCurrent
                      ? 'bg-[#FFFBEA] border-2 border-[#FFD100] shadow-2xs'
                      : isCompleted
                      ? 'bg-slate-50 border-slate-200 hover:bg-slate-100'
                      : 'bg-white border-slate-200 opacity-60 hover:opacity-100'
                  }`}
                >
                  {/* Status Indicator Icon */}
                  <div
                    className={`w-9 h-9 rounded-xl flex items-center justify-center font-bold mb-1.5 transition ${
                      isCompleted
                        ? 'bg-emerald-500 text-white'
                        : isCurrent
                        ? 'bg-[#FFD100] text-[#2E2E2E] animate-bounce-subtle'
                        : 'bg-slate-100 text-[#6C6B6D]'
                    }`}
                  >
                    {isCompleted ? (
                      <CheckCircle2 className="w-5 h-5" />
                    ) : (
                      getStageIcon(def.nombre)
                    )}
                  </div>

                  <span className="font-mono text-[10px] font-black text-[#6C6B6D]">
                    Etapa {def.numero}
                  </span>

                  <span className="font-black text-xs text-[#2E2E2E] leading-tight line-clamp-2 mt-0.5">
                    {def.nombre}
                  </span>

                  <span className="text-[10px] text-[#6C6B6D] mt-1 font-mono">
                    {isCompleted
                      ? `Fin: ${formatDisplayDate(etapa?.fechaRealFin)}`
                      : isCurrent
                      ? `⏳ En curso`
                      : `Est: ${formatDisplayDate(etapa?.fechaFinEstimada)}`}
                  </span>
                </button>

                {/* Arrow connector between stages */}
                {idx < defs.length - 1 && (
                  <div
                    className={`h-0.5 w-4 shrink-0 transition ${
                      isCompleted ? 'bg-emerald-500' : 'bg-slate-200'
                    }`}
                  />
                )}
              </React.Fragment>
            );
          })}
        </div>
      </div>
    </div>
  );
};

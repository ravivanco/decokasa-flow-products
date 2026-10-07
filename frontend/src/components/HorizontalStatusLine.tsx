import React, { useRef, useEffect } from 'react';
import { Pedido, EtapaInstancia } from '../types';
import { formatDisplayDate } from '../utils/dateUtils';
import { ETAPAS_BASE_CONFIG } from '../data/initialData';
import {
  FileText,
  Search,
  Users,
  Factory,
  PackageCheck,
  FileSpreadsheet,
  Box,
  Ship,
  ShieldAlert,
  Warehouse,
  Archive,
  Check,
  RotateCcw,
  AlertTriangle,
  PauseCircle,
  XCircle,
  Clock,
  CheckCircle2,
} from 'lucide-react';

interface HorizontalStatusLineProps {
  pedido: Pedido;
  selectedStageNum: number;
  onSelectStage: (stageNum: number) => void;
  readOnly?: boolean;
}

export const HorizontalStatusLine: React.FC<HorizontalStatusLineProps> = ({
  pedido,
  selectedStageNum,
  onSelectStage,
  readOnly = false,
}) => {
  const scrollContainerRef = useRef<HTMLDivElement>(null);

  // Auto-scroll on mount to center active stage
  useEffect(() => {
    if (scrollContainerRef.current) {
      const activeElement = scrollContainerRef.current.querySelector(
        `[data-stage="${pedido.etapaActualNumero}"]`
      );
      if (activeElement) {
        activeElement.scrollIntoView({
          behavior: 'smooth',
          inline: 'center',
          block: 'nearest',
        });
      }
    }
  }, [pedido.etapaActualNumero]);

  const getStageIcon = (stageNum: number) => {
    switch (stageNum) {
      case 1:
        return <FileText className="w-4 h-4" />;
      case 2:
        return <Search className="w-4 h-4" />;
      case 3:
        return <Users className="w-4 h-4" />;
      case 4:
        return <Factory className="w-4 h-4" />;
      case 5:
        return <PackageCheck className="w-4 h-4" />;
      case 6:
        return <FileSpreadsheet className="w-4 h-4" />;
      case 7:
        return <Box className="w-4 h-4" />;
      case 8:
        return <Ship className="w-4 h-4" />;
      case 9:
        return <ShieldAlert className="w-4 h-4" />;
      case 10:
        return <Warehouse className="w-4 h-4" />;
      case 11:
        return <Archive className="w-4 h-4" />;
      default:
        return <Clock className="w-4 h-4" />;
    }
  };

  const completedCount = pedido.etapas.filter((e) => e.completada).length;
  const progressPercent = Math.round((completedCount / 11) * 100);

  return (
    <div className="bg-white rounded-3xl border border-[#6C6B6D]/20 p-5 sm:p-7 shadow-xs space-y-4">
      {/* Banner if Paused or Cancelled */}
      {(pedido.estado === 'En pausa' || pedido.estado === 'Cancelado') && (
        <div
          className={`p-3.5 rounded-2xl flex items-start sm:items-center gap-3 text-xs font-bold border ${
            pedido.estado === 'Cancelado'
              ? 'bg-rose-50 border-rose-200 text-[#DC2626]'
              : 'bg-[#FFFBEA] border-[#FFD100] text-[#2E2E2E]'
          }`}
        >
          {pedido.estado === 'Cancelado' ? (
            <XCircle className="w-5 h-5 shrink-0 text-[#DC2626]" />
          ) : (
            <PauseCircle className="w-5 h-5 shrink-0 text-[#6C6B6D]" />
          )}
          <div className="flex-1">
            <span className="font-black uppercase tracking-wide mr-1.5">
              {pedido.estado === 'Cancelado' ? 'Pedido Cancelado:' : 'Pedido en Pausa:'}
            </span>
            <span>
              {pedido.motivoPausaCancelacion || 'Revisión técnica en proceso'}
            </span>
            {pedido.usuarioPausaCancelacion && (
              <span className="text-[#6C6B6D] font-medium ml-1">
                (Por: {pedido.usuarioPausaCancelacion})
              </span>
            )}
          </div>
        </div>
      )}

      {/* Group headers above timeline */}
      <div className="hidden md:grid grid-cols-11 gap-1 text-[11px] font-black uppercase text-[#6C6B6D] px-2 select-none">
        <div className="col-span-3 text-center border-b-2 border-[#FFD100] pb-1">
          1-3 · Pedido
        </div>
        <div className="col-span-2 text-center border-b-2 border-[#FFD100] pb-1">
          4-5 · Producción en China
        </div>
        <div className="col-span-2 text-center border-b-2 border-[#FFD100] pb-1">
          6-7 · Documentos
        </div>
        <div className="col-span-1 text-center border-b-2 border-[#FFD100] pb-1">
          8 · Tránsito
        </div>
        <div className="col-span-3 text-center border-b-2 border-[#FFD100] pb-1">
          9-11 · Llegada a Ecuador
        </div>
      </div>

      {/* Progress Line and Percentage */}
      <div className="flex items-center justify-between text-xs font-bold text-[#515151]">
        <span>Progreso general de importación:</span>
        <span className="font-black text-[#2E2E2E]">{progressPercent}% completado</span>
      </div>

      {/* Horizontal scrollable track */}
      <div
        ref={scrollContainerRef}
        className="overflow-x-auto pb-4 pt-2 -mx-4 px-4 sm:-mx-6 sm:px-6 scroll-smooth focus:outline-hidden"
      >
        <div className="min-w-[920px] relative px-4">
          {/* Background gray connecting line */}
          <div className="absolute top-6 left-12 right-12 h-1.5 bg-[#6C6B6D]/20 z-0 rounded-full" />

          {/* Foreground yellow connecting progress line */}
          <div
            className="absolute top-6 left-12 h-1.5 bg-[#FFD100] z-0 rounded-full transition-all duration-500"
            style={{
              width: `${Math.min(96, Math.max(0, (completedCount / 10) * 88))}%`,
            }}
          />

          {/* 11 Horizontal Stages Grid */}
          <div className="relative z-10 grid grid-cols-11 gap-2">
            {pedido.etapas.map((etapa) => {
              const def = ETAPAS_BASE_CONFIG[etapa.numero - 1];
              const isCompleted = etapa.completada;
              const isCurrent = etapa.numero === pedido.etapaActualNumero && pedido.estado !== 'Finalizado';
              const isPending = !isCompleted && !isCurrent;
              const isSelected = selectedStageNum === etapa.numero;
              const isReturned = etapa.devuelta || (etapa.numero === pedido.etapaActualNumero && pedido.estado === 'Devuelto con observaciones');
              const isCustomsHeld = etapa.numero === 9 && etapa.subestadoAduana === 'Retenido';

              const dateDisplay = isCompleted
                ? formatDisplayDate(etapa.fechaRealFin)
                : formatDisplayDate(etapa.fechaFinEstimada);

              return (
                <button
                  key={etapa.numero}
                  data-stage={etapa.numero}
                  type="button"
                  onClick={() => onSelectStage(etapa.numero)}
                  className={`flex flex-col items-center text-center group cursor-pointer focus:outline-hidden transition-transform ${
                    isSelected ? 'scale-105' : 'hover:scale-102'
                  }`}
                >
                  {/* Circle Node:
                      - Completada: amarillo #FFD100 con ✓
                      - Actual: amarillo #FFD100 con borde grueso #2E2E2E y pulso suave
                      - Pendiente: blanco con borde gris #6C6B6D
                      - Devuelta: borde naranja #EA580C con ↺
                      - Retenida: rojo #DC2626 con ⚠ */}
                  <div
                    className={`w-11 h-11 sm:w-12 sm:h-12 rounded-full flex items-center justify-center font-bold text-xs transition-all shadow-sm ${
                      isCustomsHeld
                        ? 'bg-[#DC2626] text-white ring-4 ring-rose-200 animate-pulse'
                        : isReturned
                        ? 'bg-white text-[#EA580C] border-3 border-[#EA580C] ring-4 ring-orange-100'
                        : isCompleted
                        ? 'bg-[#FFD100] text-[#2E2E2E] ring-4 ring-white'
                        : isCurrent
                        ? 'bg-[#FFD100] text-[#2E2E2E] border-3.5 border-[#2E2E2E] ring-4 ring-yellow-200 animate-pulse'
                        : 'bg-white text-[#6C6B6D] border-2 border-[#6C6B6D]/40 ring-4 ring-white'
                    }`}
                  >
                    {isCustomsHeld ? (
                      <AlertTriangle className="w-5 h-5 stroke-[2.5]" />
                    ) : isReturned ? (
                      <RotateCcw className="w-5 h-5 stroke-[2.5]" />
                    ) : isCompleted ? (
                      <Check className="w-5 h-5 stroke-[3]" />
                    ) : (
                      getStageIcon(etapa.numero)
                    )}
                  </div>

                  {/* Stage number & title */}
                  <div className="mt-2.5 space-y-0.5 max-w-[85px]">
                    <span className="text-[10px] font-mono font-bold text-[#6C6B6D] block">
                      {etapa.numero}.
                    </span>
                    <span
                      className={`text-[11px] font-bold block leading-tight truncate ${
                        isSelected
                          ? 'text-[#2E2E2E] underline underline-offset-2'
                          : isCurrent
                          ? 'text-[#2E2E2E] font-black'
                          : 'text-[#515151]'
                      }`}
                      title={etapa.nombre}
                    >
                      {etapa.nombre}
                    </span>

                    {/* Date */}
                    <span className="text-[10px] text-[#6C6B6D] block font-semibold truncate">
                      {dateDisplay}
                    </span>

                    {/* Status Pill on each node */}
                    {isCurrent && (
                      <span className="inline-block mt-1 px-1.5 py-0.5 rounded-md text-[9px] font-black bg-[#FFD100] text-[#2E2E2E] border border-black/10">
                        Actual
                      </span>
                    )}
                    {isCustomsHeld && (
                      <span className="inline-block mt-1 px-1.5 py-0.5 rounded-md text-[9px] font-black bg-[#DC2626] text-white">
                        Retenido
                      </span>
                    )}
                  </div>
                </button>
              );
            })}
          </div>
        </div>
      </div>
    </div>
  );
};

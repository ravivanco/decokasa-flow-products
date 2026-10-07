import React from 'react';
import { Pedido } from '../types';
import { calculateOrderSummary } from '../utils/orderState';
import { BarChart3, Download, FileSpreadsheet, FileText, CheckCircle2, AlertTriangle, Ship } from 'lucide-react';

interface ReportsViewProps {
  pedidos: Pedido[];
  onShowToast: (msg: string) => void;
}

export const ReportsView: React.FC<ReportsViewProps> = ({ pedidos, onShowToast }) => {
  const handleExport = (type: 'PDF' | 'Excel') => {
    onShowToast(`Exportación a ${type}: Disponible en la versión final`);
  };

  // Status counts
  const statusCounts: { [key: string]: number } = {};
  pedidos.forEach((p) => {
    statusCounts[p.estado] = (statusCounts[p.estado] || 0) + 1;
  });

  return (
    <div className="space-y-6 sm:space-y-8 animate-in fade-in duration-200">
      <div className="flex flex-col sm:flex-row sm:items-center justify-between gap-4">
        <div>
          <h1 className="text-2xl sm:text-3xl font-black text-[#2E2E2E]">
            Reportes e Indicadores Estratégicos
          </h1>
          <p className="text-xs sm:text-sm text-[#515151] mt-0.5">
            Módulo exclusivo para Junta de Socios y Dirección. Monitoreo de cumplimiento de plazos de importación.
          </p>
        </div>

        <div className="flex items-center gap-2">
          <button
            type="button"
            onClick={() => handleExport('PDF')}
            className="px-4 py-2 rounded-xl text-xs font-black bg-white border border-[#6C6B6D]/30 text-[#2E2E2E] hover:bg-slate-50 flex items-center gap-1.5 shadow-2xs cursor-pointer"
          >
            <FileText className="w-4 h-4" />
            <span>Exportar PDF</span>
          </button>
          <button
            type="button"
            onClick={() => handleExport('Excel')}
            className="px-4 py-2 rounded-xl text-xs font-black bg-[#FFD100] hover:bg-[#E6BC00] text-[#2E2E2E] flex items-center gap-1.5 shadow-2xs cursor-pointer"
          >
            <FileSpreadsheet className="w-4 h-4" />
            <span>Exportar Excel</span>
          </button>
        </div>
      </div>

      {/* Grid of Report Cards */}
      <div className="grid grid-cols-1 md:grid-cols-2 gap-6">
        {/* 1. Pedidos por Estado (Gráfico visual de barras) */}
        <div className="bg-white rounded-3xl border border-[#6C6B6D]/20 p-6 shadow-xs space-y-4">
          <h3 className="text-sm font-black uppercase tracking-wider text-[#2E2E2E] flex items-center gap-2">
            <BarChart3 className="w-4 h-4 text-[#515151]" />
            <span>Distribución de Pedidos por Estado</span>
          </h3>

          <div className="space-y-3 pt-2">
            {Object.entries(statusCounts).map(([estado, count]) => {
              const pct = Math.round((count / pedidos.length) * 100);
              return (
                <div key={estado} className="space-y-1">
                  <div className="flex justify-between text-xs font-bold">
                    <span className="text-[#2E2E2E]">{estado}</span>
                    <span className="text-[#6C6B6D]">{count} pedido(s) ({pct}%)</span>
                  </div>
                  <div className="w-full bg-slate-100 rounded-full h-2.5 overflow-hidden">
                    <div
                      className="bg-[#FFD100] h-full rounded-full transition-all duration-500"
                      style={{ width: `${Math.max(8, pct)}%` }}
                    />
                  </div>
                </div>
              );
            })}
          </div>
        </div>

        {/* 2. Plazos Promedio por Etapa vs Plazo Base */}
        <div className="bg-white rounded-3xl border border-[#6C6B6D]/20 p-6 shadow-xs space-y-4">
          <h3 className="text-sm font-black uppercase tracking-wider text-[#2E2E2E] flex items-center gap-2">
            <Ship className="w-4 h-4 text-[#515151]" />
            <span>Cumplimiento de Hitos Críticos</span>
          </h3>

          <div className="space-y-3 pt-2 text-xs">
            {[
              { etapa: 'Fabricación (Meta 35d hábiles)', promedio: '36.2 días', status: 'Leve desfase (+1.2d)', ok: false },
              { etapa: 'Viaje marítimo (Meta 40d hábiles)', promedio: '39.8 días', status: 'En rango óptimo (-0.2d)', ok: true },
              { etapa: 'Desaduanización (Meta 15d hábiles)', promedio: '14.5 días', status: 'En rango óptimo (-0.5d)', ok: true },
              { etapa: 'Revisión técnica (Meta 3d hábiles)', promedio: '3.1 días', status: 'En rango óptimo', ok: true },
            ].map((m, idx) => (
              <div key={idx} className="p-3 rounded-2xl bg-slate-50 border border-slate-200 flex items-center justify-between">
                <div>
                  <span className="font-bold text-[#2E2E2E] block">{m.etapa}</span>
                  <span className="text-[11px] text-[#6C6B6D]">{m.status}</span>
                </div>
                <strong className={`font-black ${m.ok ? 'text-[#16A34A]' : 'text-[#EA580C]'}`}>
                  {m.promedio}
                </strong>
              </div>
            ))}
          </div>
        </div>
      </div>
    </div>
  );
};

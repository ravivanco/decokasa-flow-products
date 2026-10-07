import React from 'react';
import { AlertaItem, Pedido } from '../types';
import { Bell, AlertTriangle, Clock, RotateCcw, ShieldAlert, Check, ArrowRight } from 'lucide-react';

interface AlertsViewProps {
  alertas: AlertaItem[];
  onMarkAsRead: (id: string) => void;
  onSelectPedidoById: (pedidoId: string) => void;
}

export const AlertsView: React.FC<AlertsViewProps> = ({
  alertas,
  onMarkAsRead,
  onSelectPedidoById,
}) => {
  return (
    <div className="space-y-6 sm:space-y-8 animate-in fade-in duration-200">
      <div>
        <h1 className="text-2xl sm:text-3xl font-black text-[#2E2E2E]">
          Centro de Alertas y Notificaciones
        </h1>
        <p className="text-xs sm:text-sm text-[#515151] mt-0.5">
          Avisos preventivos 3 días antes del vencimiento y notificaciones críticas de atrasos o aforos aduaneros.
        </p>
      </div>

      <div className="bg-white rounded-3xl border border-[#6C6B6D]/20 shadow-xs p-6 space-y-4">
        {alertas.length === 0 ? (
          <p className="text-xs text-[#6C6B6D] text-center py-10">
            No tienes alertas activas en este momento.
          </p>
        ) : (
          <div className="space-y-3">
            {alertas.map((a) => (
              <div
                key={a.id}
                className={`p-4 rounded-2xl border transition flex flex-col sm:flex-row sm:items-center justify-between gap-3 ${
                  a.leida ? 'bg-slate-50 border-slate-200 opacity-75' : 'bg-[#FFFBEA] border-[#FFD100]'
                }`}
              >
                <div className="flex items-start gap-3 min-w-0">
                  <div
                    className={`p-2 rounded-xl shrink-0 mt-0.5 ${
                      a.tipo === 'Retenido en aduana' || a.tipo === 'Retrasado'
                        ? 'bg-rose-100 text-[#DC2626]'
                        : a.tipo === 'Por vencer'
                        ? 'bg-orange-100 text-[#EA580C]'
                        : 'bg-slate-100 text-[#515151]'
                    }`}
                  >
                    {a.tipo === 'Retenido en aduana' ? (
                      <ShieldAlert className="w-5 h-5" />
                    ) : a.tipo === 'Retrasado' ? (
                      <AlertTriangle className="w-5 h-5" />
                    ) : a.tipo === 'Por vencer' ? (
                      <Clock className="w-5 h-5" />
                    ) : (
                      <RotateCcw className="w-5 h-5" />
                    )}
                  </div>

                  <div className="min-w-0">
                    <div className="flex items-center gap-2 flex-wrap">
                      <span className="font-mono text-xs font-black text-[#2E2E2E]">
                        {a.pedidoCodigo}
                      </span>
                      <span className="text-xs font-bold text-[#515151]">
                        · Etapa {a.etapaNumero}: {a.etapaNombre}
                      </span>
                      <span className="text-[10px] font-black px-2 py-0.5 rounded bg-black/10 text-[#2E2E2E]">
                        {a.tipo}
                      </span>
                    </div>

                    <p className="text-xs font-bold text-[#2E2E2E] mt-1">
                      {a.mensaje}
                    </p>

                    <span className="text-[10px] text-[#6C6B6D] block mt-0.5 font-mono">
                      {a.fecha}
                    </span>
                  </div>
                </div>

                <div className="flex items-center gap-2 shrink-0 self-end sm:self-center">
                  {!a.leida && (
                    <button
                      type="button"
                      onClick={() => onMarkAsRead(a.id)}
                      className="px-3 py-1.5 rounded-xl text-xs font-bold text-[#6C6B6D] hover:bg-white border border-[#6C6B6D]/20 cursor-pointer"
                    >
                      Marcar leída
                    </button>
                  )}
                  <button
                    type="button"
                    onClick={() => onSelectPedidoById(a.pedidoId)}
                    className="px-4 py-1.5 rounded-xl text-xs font-black bg-[#FFD100] hover:bg-[#E6BC00] text-[#2E2E2E] flex items-center gap-1 shadow-2xs cursor-pointer"
                  >
                    <span>Ver pedido</span>
                    <ArrowRight className="w-3.5 h-3.5" />
                  </button>
                </div>
              </div>
            ))}
          </div>
        )}
      </div>
    </div>
  );
};

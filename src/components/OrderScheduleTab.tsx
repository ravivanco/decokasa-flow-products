import React from 'react';
import { Pedido } from '../types';
import { formatDisplayDate, countBusinessDaysDiff } from '../utils/dateUtils';
import { Calendar, CheckCircle2, Clock, AlertTriangle } from 'lucide-react';

interface OrderScheduleTabProps {
  pedido: Pedido;
}

export const OrderScheduleTab: React.FC<OrderScheduleTabProps> = ({ pedido }) => {
  return (
    <div className="bg-white rounded-3xl border border-[#6C6B6D]/20 shadow-xs p-6 sm:p-8 space-y-6">
      <div className="border-b border-slate-100 pb-4">
        <h3 className="text-lg font-black text-[#2E2E2E] flex items-center gap-2">
          <Calendar className="w-5 h-5 text-[#515151]" />
          <span>Cronograma Detallado en Días Hábiles</span>
        </h3>
        <p className="text-xs text-[#6C6B6D] mt-0.5">
          Cálculo encadenado de inicio y fin de cada hito excluyendo sábados y domingos.
        </p>
      </div>

      <div className="overflow-x-auto">
        <table className="w-full text-left text-xs text-[#515151]">
          <thead className="bg-[#FFFBEA] text-[#2E2E2E] font-black uppercase text-[11px] border-b border-[#FFD100]/40">
            <tr>
              <th className="py-3 px-3">#</th>
              <th className="py-3 px-3">Etapa</th>
              <th className="py-3 px-3">Plazo Base</th>
              <th className="py-3 px-3">Días Extra</th>
              <th className="py-3 px-3">Inicio Est.</th>
              <th className="py-3 px-3">Fin Est.</th>
              <th className="py-3 px-3">Fin Real</th>
              <th className="py-3 px-3 text-center">Desfase</th>
              <th className="py-3 px-3 text-center">Estado</th>
            </tr>
          </thead>
          <tbody className="divide-y divide-slate-100 font-medium">
            {pedido.etapas.map((etapa) => {
              const diff =
                etapa.completada && etapa.fechaRealFin
                  ? countBusinessDaysDiff(etapa.fechaFinEstimada, etapa.fechaRealFin)
                  : 0;

              return (
                <tr key={etapa.numero} className="hover:bg-[#FFFBEA]/40 transition">
                  <td className="py-3 px-3 font-mono font-bold text-[#2E2E2E]">
                    {etapa.numero}
                  </td>
                  <td className="py-3 px-3 font-bold text-[#2E2E2E]">
                    {etapa.nombre}
                  </td>
                  <td className="py-3 px-3">
                    {etapa.plazoDiasHabiles} días hábiles
                  </td>
                  <td className="py-3 px-3">
                    {etapa.plazoExtraDias > 0 ? (
                      <span className="font-bold text-[#2E2E2E]">
                        +{etapa.plazoExtraDias} días
                      </span>
                    ) : (
                      '—'
                    )}
                  </td>
                  <td className="py-3 px-3 font-mono">
                    {formatDisplayDate(etapa.fechaInicioEstimada)}
                  </td>
                  <td className="py-3 px-3 font-mono font-bold text-[#2E2E2E]">
                    {formatDisplayDate(etapa.fechaFinEstimada)}
                  </td>
                  <td className="py-3 px-3 font-mono">
                    {etapa.fechaRealFin ? formatDisplayDate(etapa.fechaRealFin) : '—'}
                  </td>
                  <td className="py-3 px-3 text-center font-bold">
                    {etapa.completada ? (
                      diff === 0 ? (
                        <span className="text-[#16A34A]">0 d</span>
                      ) : diff > 0 ? (
                        <span className="text-[#DC2626]">+{diff} d</span>
                      ) : (
                        <span className="text-[#16A34A]">{diff} d</span>
                      )
                    ) : (
                      '—'
                    )}
                  </td>
                  <td className="py-3 px-3 text-center">
                    {etapa.completada ? (
                      <span className="inline-flex items-center gap-1 text-[11px] font-bold text-[#16A34A]">
                        <CheckCircle2 className="w-3.5 h-3.5" />
                        Completada
                      </span>
                    ) : etapa.numero === pedido.etapaActualNumero ? (
                      <span className="inline-flex items-center gap-1 text-[11px] font-black text-[#2E2E2E] bg-[#FFD100] px-2 py-0.5 rounded-full">
                        En curso
                      </span>
                    ) : (
                      <span className="text-[11px] text-[#6C6B6D]">
                        Pendiente
                      </span>
                    )}
                  </td>
                </tr>
              );
            })}
          </tbody>
        </table>
      </div>
    </div>
  );
};

import React from 'react';
import { Pedido } from '../types';
import { History, Check, X, AlertTriangle, Clock, ShieldAlert, FileText, Warehouse, CheckCircle2 } from 'lucide-react';

interface OrderHistoryTabProps {
  pedido: Pedido;
}

export const OrderHistoryTab: React.FC<OrderHistoryTabProps> = ({ pedido }) => {
  return (
    <div className="bg-white rounded-3xl border border-[#6C6B6D]/20 shadow-xs p-6 sm:p-8 space-y-6">
      <div className="border-b border-slate-100 pb-4">
        <h3 className="text-lg font-black text-[#2E2E2E] flex items-center gap-2">
          <History className="w-5 h-5 text-[#515151]" />
          <span>Historial de Auditoría Inmutable del Pedido</span>
        </h3>
        <p className="text-xs text-[#6C6B6D] mt-0.5">
          Trazabilidad de cada avance, rechazo, devolución urgente, reporte de demora en aduana y cierre.
        </p>
      </div>

      <div className="space-y-3">
        {pedido.historial.map((item) => (
          <div
            key={item.id}
            className="p-4 rounded-2xl border border-slate-200 bg-slate-50/60 flex items-start gap-3.5 text-xs"
          >
            <div className="w-8 h-8 rounded-xl bg-white border border-slate-200 flex items-center justify-center shrink-0 mt-0.5">
              {item.tipo === 'check' && <Check className="w-4 h-4 text-[#16A34A] stroke-[3]" />}
              {item.tipo === 'aprobacion' && <CheckCircle2 className="w-4 h-4 text-emerald-600" />}
              {item.tipo === 'rechazo' && <X className="w-4 h-4 text-[#DC2626]" />}
              {item.tipo === 'devolucion_urgente' && <AlertTriangle className="w-4 h-4 text-amber-600" />}
              {item.tipo === 'demora_aduana' && <Clock className="w-4 h-4 text-blue-600" />}
              {item.tipo === 'bodega' && <Warehouse className="w-4 h-4 text-amber-700" />}
              {item.tipo === 'documento' && <FileText className="w-4 h-4 text-[#515151]" />}
              {item.tipo === 'observacion' && <FileText className="w-4 h-4 text-[#515151]" />}
              {item.tipo === 'estado' && <History className="w-4 h-4 text-[#515151]" />}
              {item.tipo === 'reasignacion' && <ShieldAlert className="w-4 h-4 text-purple-600" />}
            </div>

            <div className="flex-1 min-w-0">
              <div className="flex flex-col sm:flex-row sm:items-center justify-between gap-1">
                <span className="font-black text-[#2E2E2E]">{item.accion}</span>
                <span className="font-mono text-[11px] text-[#6C6B6D] shrink-0">
                  {item.fecha}
                </span>
              </div>
              <p className="text-[#6C6B6D] text-[11px] mt-0.5">
                Por: <strong className="text-[#515151]">{item.usuarioNombre}</strong> ({item.usuarioRol})
              </p>
              {item.motivo && (
                <div className="p-2.5 rounded-xl bg-white border border-slate-200 text-[#515151] italic mt-2">
                  &ldquo;{item.motivo}&rdquo;
                </div>
              )}
            </div>
          </div>
        ))}
      </div>
    </div>
  );
};

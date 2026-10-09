import React, { useState } from 'react';
import { NotificacionGmail, Pedido } from '../types';
import { Mail, Search, CheckCircle2, Clock, AlertTriangle, ArrowRight, ShieldAlert, Sparkles } from 'lucide-react';

interface GmailNotificationsViewProps {
  notificaciones: NotificacionGmail[];
  onMarkAsRead: (id: string) => void;
  onSelectPedidoCodigo: (codigo: string) => void;
}

export const GmailNotificationsView: React.FC<GmailNotificationsViewProps> = ({
  notificaciones,
  onMarkAsRead,
  onSelectPedidoCodigo,
}) => {
  const [filterType, setFilterType] = useState<string>('todos');
  const [searchTerm, setSearchTerm] = useState('');

  const filtered = notificaciones.filter((n) => {
    if (filterType === 'no_leidas' && n.leida) return false;
    if (filterType === 'urgencias' && !n.urgencia) return false;
    if (filterType === 'retrasos' && !n.esAlertaRetraso) return false;

    if (searchTerm.trim()) {
      const term = searchTerm.toLowerCase();
      return (
        n.asunto.toLowerCase().includes(term) ||
        n.pedidoCodigo.toLowerCase().includes(term) ||
        n.contenido.toLowerCase().includes(term)
      );
    }
    return true;
  });

  return (
    <div className="space-y-6 sm:space-y-8 animate-in fade-in duration-200">
      {/* Header */}
      <div className="flex flex-col sm:flex-row sm:items-center justify-between gap-4">
        <div>
          <h1 className="text-2xl sm:text-3xl font-black text-[#2E2E2E]">
            Bandeja de Avisos y Alertas por Gmail
          </h1>
          <p className="text-xs sm:text-sm text-[#515151] mt-0.5">
            Notificaciones automáticas enviadas al Administrador General (Mirian Franco) y a los responsables en cada cambio de etapa y vencimiento.
          </p>
        </div>

        <div className="text-xs font-bold text-[#6C6B6D] px-3.5 py-1.5 rounded-xl bg-white border border-slate-200 shadow-2xs">
          Destinatario predeterminado: <strong className="text-[#2E2E2E]">mfranco@decokasa.ec</strong>
        </div>
      </div>

      {/* Filter and Search */}
      <div className="bg-white rounded-3xl p-4 sm:p-5 border border-[#6C6B6D]/20 shadow-xs flex flex-col sm:flex-row gap-3">
        <div className="relative flex-1">
          <Search className="w-4 h-4 text-[#6C6B6D] absolute left-3.5 top-1/2 -translate-y-1/2" />
          <input
            type="text"
            placeholder="Buscar por código de pedido, asunto o contenido del correo..."
            value={searchTerm}
            onChange={(e) => setSearchTerm(e.target.value)}
            className="w-full pl-10 pr-4 py-2.5 rounded-xl border border-[#6C6B6D]/20 text-xs font-semibold text-[#2E2E2E] focus:outline-hidden focus:ring-2 focus:ring-[#FFD100]"
          />
        </div>

        <div className="flex items-center gap-2">
          <select
            value={filterType}
            onChange={(e) => setFilterType(e.target.value)}
            className="px-3 py-2.5 rounded-xl border border-[#6C6B6D]/20 text-xs font-bold text-[#2E2E2E] focus:outline-hidden focus:ring-2 focus:ring-[#FFD100] cursor-pointer"
          >
            <option value="todos">Todos los correos ({notificaciones.length})</option>
            <option value="no_leidas">No leídos</option>
            <option value="urgencias">Urgencias (4h, 12h, 24h)</option>
            <option value="retrasos">Alertas de Demora SENAE</option>
          </select>
        </div>
      </div>

      {/* List of Email cards */}
      <div className="space-y-3">
        {filtered.length === 0 ? (
          <div className="bg-white rounded-3xl p-12 text-center text-xs text-[#6C6B6D] border border-[#6C6B6D]/20">
            No hay notificaciones con los filtros actuales.
          </div>
        ) : (
          filtered.map((item) => (
            <div
              key={item.id}
              className={`p-5 rounded-3xl border transition space-y-3 ${
                !item.leida
                  ? 'bg-[#FFFBEA] border-amber-300 shadow-xs'
                  : 'bg-white border-slate-200'
              }`}
            >
              <div className="flex flex-col sm:flex-row sm:items-center justify-between gap-2">
                <div className="flex items-center gap-2.5 min-w-0">
                  <div
                    className={`w-9 h-9 rounded-xl flex items-center justify-center shrink-0 ${
                      item.urgencia
                        ? 'bg-red-100 text-red-700'
                        : item.esAlertaRetraso
                        ? 'bg-amber-100 text-amber-800'
                        : 'bg-slate-100 text-[#515151]'
                    }`}
                  >
                    <Mail className="w-4 h-4" />
                  </div>

                  <div className="min-w-0">
                    <div className="flex items-center gap-2">
                      <span className="font-mono text-xs font-black text-[#2E2E2E]">
                        {item.pedidoCodigo}
                      </span>
                      {item.urgencia && (
                        <span className="px-2 py-0.5 rounded-md text-[10px] font-black uppercase bg-red-600 text-white animate-pulse">
                          Urgente {item.urgencia}
                        </span>
                      )}
                      {item.esAlertaRetraso && (
                        <span className="px-2 py-0.5 rounded-md text-[10px] font-black uppercase bg-amber-200 text-amber-900">
                          Aviso Demora
                        </span>
                      )}
                      {!item.leida && (
                        <span className="w-2 h-2 rounded-full bg-amber-500" />
                      )}
                    </div>

                    <h3 className="text-xs sm:text-sm font-black text-[#2E2E2E] truncate mt-0.5">
                      {item.asunto}
                    </h3>
                  </div>
                </div>

                <div className="text-right text-[11px] text-[#6C6B6D] font-mono shrink-0">
                  {item.fechaHora}
                </div>
              </div>

              {/* Message body */}
              <p className="text-xs text-[#515151] leading-relaxed bg-white/70 p-3.5 rounded-2xl border border-black/5">
                {item.contenido}
              </p>

              {/* Footer info */}
              <div className="flex flex-col sm:flex-row sm:items-center justify-between gap-2 pt-1 text-[11px] text-[#6C6B6D]">
                <div>
                  Para: <strong>{item.destinatarioNombre}</strong> ({item.destinatarioEmail}) · De: {item.remitente}
                </div>

                <div className="flex items-center gap-2">
                  {!item.leida && (
                    <button
                      type="button"
                      onClick={() => onMarkAsRead(item.id)}
                      className="text-xs font-bold text-[#515151] hover:underline cursor-pointer"
                    >
                      Marcar como leído
                    </button>
                  )}

                  <button
                    type="button"
                    onClick={() => onSelectPedidoCodigo(item.pedidoCodigo)}
                    className="px-3 py-1.5 rounded-xl bg-[#FFD100] hover:bg-[#E6BC00] text-[#2E2E2E] font-black text-xs transition cursor-pointer flex items-center gap-1 shadow-2xs"
                  >
                    <span>Ver Pedido</span>
                    <ArrowRight className="w-3.5 h-3.5" />
                  </button>
                </div>
              </div>
            </div>
          ))
        )}
      </div>
    </div>
  );
};

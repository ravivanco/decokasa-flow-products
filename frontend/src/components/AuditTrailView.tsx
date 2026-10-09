import React, { useState, useMemo } from 'react';
import { Pedido, HistorialAuditoria } from '../types';
import { ShieldCheck, Search, Filter, Calendar, FileText, CheckCircle2, RotateCcw, AlertTriangle } from 'lucide-react';

interface AuditTrailViewProps {
  pedidos: Pedido[];
}

export const AuditTrailView: React.FC<AuditTrailViewProps> = ({ pedidos }) => {
  const [searchTerm, setSearchTerm] = useState('');
  const [typeFilter, setTypeFilter] = useState<string>('todos');

  // Flatten all history items with their respective order codes
  const allEvents = useMemo(() => {
    const list: HistorialAuditoria[] = [];
    pedidos.forEach((p) => {
      p.historial.forEach((h) => {
        list.push({
          ...h,
          pedidoCodigo: h.pedidoCodigo || p.codigo,
        });
      });
    });
    // Deduplicate by ID and sort descending by date
    const unique = Array.from(new Map(list.map((item) => [item.id, item])).values());
    return unique;
  }, [pedidos]);

  const filteredEvents = useMemo(() => {
    return allEvents.filter((e) => {
      if (typeFilter !== 'todos' && e.tipo !== typeFilter) return false;
      if (searchTerm) {
        const term = searchTerm.toLowerCase();
        const matchCode = e.pedidoCodigo?.toLowerCase().includes(term);
        const matchUser = e.usuarioNombre?.toLowerCase().includes(term);
        const matchAction = e.accion?.toLowerCase().includes(term);
        const matchMotivo = e.motivo?.toLowerCase().includes(term);
        if (!matchCode && !matchUser && !matchAction && !matchMotivo) return false;
      }
      return true;
    });
  }, [allEvents, typeFilter, searchTerm]);

  return (
    <div className="space-y-6 sm:space-y-8 animate-in fade-in duration-200">
      <div className="flex flex-col sm:flex-row sm:items-center justify-between gap-4">
        <div>
          <h1 className="text-2xl sm:text-3xl font-black text-[#2E2E2E]">
            Registro Global de Auditoría
          </h1>
          <p className="text-xs sm:text-sm text-[#515151] mt-0.5">
            Trazabilidad inmutable de cada avance, reversión, aprobación y modificación en Decokasa.
          </p>
        </div>

        <div className="flex items-center gap-2">
          <span className="text-xs font-bold text-[#6C6B6D]">
            Total registros: <strong className="text-[#2E2E2E]">{allEvents.length}</strong>
          </span>
        </div>
      </div>

      {/* Filters bar */}
      <div className="bg-white rounded-3xl p-4 sm:p-5 border border-[#6C6B6D]/20 shadow-xs flex flex-col sm:flex-row gap-3">
        <div className="relative flex-1">
          <Search className="w-4 h-4 text-[#6C6B6D] absolute left-3.5 top-1/2 -translate-y-1/2" />
          <input
            type="text"
            placeholder="Buscar por código de pedido, usuario, motivo o acción..."
            value={searchTerm}
            onChange={(e) => setSearchTerm(e.target.value)}
            className="w-full pl-10 pr-4 py-2.5 rounded-xl border border-[#6C6B6D]/20 text-xs font-semibold text-[#2E2E2E] focus:outline-hidden focus:ring-2 focus:ring-[#FFD100]"
          />
        </div>

        <div className="flex items-center gap-2">
          <Filter className="w-4 h-4 text-[#6C6B6D]" />
          <select
            value={typeFilter}
            onChange={(e) => setTypeFilter(e.target.value)}
            className="px-3 py-2.5 rounded-xl border border-[#6C6B6D]/20 text-xs font-bold text-[#2E2E2E] focus:outline-hidden focus:ring-2 focus:ring-[#FFD100] cursor-pointer"
          >
            <option value="todos">Todos los tipos de eventos</option>
            <option value="check">Checks y avances</option>
            <option value="aprobacion">Aprobaciones</option>
            <option value="rechazo">Rechazos con observaciones</option>
            <option value="devolucion_urgente">Devolución urgente (4h/12h/24h)</option>
            <option value="demora_aduana">Reportes de demora SENAE</option>
            <option value="bodega">Asignación de bodega</option>
            <option value="documento">Archivos en Google Drive</option>
            <option value="observacion">Observaciones publicadas</option>
          </select>
        </div>
      </div>

      {/* Events Table */}
      <div className="bg-white rounded-3xl border border-[#6C6B6D]/20 shadow-xs overflow-hidden">
        <div className="overflow-x-auto">
          <table className="w-full text-left border-collapse">
            <thead>
              <tr className="bg-slate-50 border-b border-slate-200 text-[11px] font-black uppercase tracking-wider text-[#6C6B6D]">
                <th className="py-3 px-4">Fecha y Hora</th>
                <th className="py-3 px-4">Pedido</th>
                <th className="py-3 px-4">Usuario Responsable</th>
                <th className="py-3 px-4">Acción Registrada</th>
                <th className="py-3 px-4">Etapa</th>
                <th className="py-3 px-4">Motivo / Detalle</th>
              </tr>
            </thead>
            <tbody className="divide-y divide-slate-100 text-xs">
              {filteredEvents.length === 0 ? (
                <tr>
                  <td colSpan={6} className="py-10 text-center text-xs text-[#6C6B6D]">
                    No se encontraron registros de auditoría para este filtro.
                  </td>
                </tr>
              ) : (
                filteredEvents.map((evt) => (
                  <tr key={evt.id} className="hover:bg-slate-50/70 transition">
                    <td className="py-3 px-4 font-mono text-[11px] text-[#6C6B6D] whitespace-nowrap">
                      {evt.fecha}
                    </td>
                    <td className="py-3 px-4 font-mono font-bold text-[#2E2E2E] whitespace-nowrap">
                      {evt.pedidoCodigo || '—'}
                    </td>
                    <td className="py-3 px-4 whitespace-nowrap">
                      <div className="font-bold text-[#2E2E2E]">{evt.usuarioNombre}</div>
                      <div className="text-[10px] text-[#6C6B6D]">{evt.usuarioRol}</div>
                    </td>
                    <td className="py-3 px-4">
                      <span
                        className={`inline-flex items-center gap-1.5 px-2.5 py-1 rounded-lg text-[11px] font-bold ${
                          evt.tipo === 'rechazo'
                            ? 'bg-rose-100 text-rose-800'
                            : evt.tipo === 'devolucion_urgente'
                            ? 'bg-amber-100 text-amber-900 border border-amber-300'
                            : evt.tipo === 'demora_aduana'
                            ? 'bg-blue-100 text-blue-800'
                            : evt.tipo === 'check' || evt.tipo === 'aprobacion'
                            ? 'bg-emerald-100 text-emerald-800'
                            : 'bg-slate-100 text-[#2E2E2E]'
                        }`}
                      >
                        {evt.accion}
                      </span>
                    </td>
                    <td className="py-3 px-4 text-[#515151] whitespace-nowrap">
                      {evt.etapaNumero ? `Etapa ${evt.etapaNumero}` : '—'}
                    </td>
                    <td className="py-3 px-4 text-[#6C6B6D] max-w-xs truncate" title={evt.motivo}>
                      {evt.motivo || '—'}
                    </td>
                  </tr>
                ))
              )}
            </tbody>
          </table>
        </div>
      </div>
    </div>
  );
};

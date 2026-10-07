import React from 'react';
import { Pedido, Usuario } from '../types';
import { calculateOrderSummary } from '../utils/orderState';
import { canUserOperateStage } from '../utils/permissions';
import { formatDisplayDate } from '../utils/dateUtils';
import {
  Package,
  Ship,
  ShieldAlert,
  AlertTriangle,
  CalendarClock,
  ArrowRight,
  Clock,
  CheckCircle2,
  ListTodo,
  ExternalLink,
} from 'lucide-react';

interface DashboardViewProps {
  pedidos: Pedido[];
  currentUser: Usuario;
  onSelectPedido: (pedido: Pedido) => void;
  onNavigateToTracking: () => void;
}

export const DashboardView: React.FC<DashboardViewProps> = ({
  pedidos,
  currentUser,
  onSelectPedido,
  onNavigateToTracking,
}) => {
  // Pedidos con metadata calculada
  const ordersMeta = pedidos.map((p) => ({
    pedido: p,
    summary: calculateOrderSummary(p),
  }));

  // Indicadores
  const totalEnCurso = ordersMeta.filter(
    (o) => o.pedido.estado !== 'Finalizado' && o.pedido.estado !== 'Cancelado'
  ).length;
  const enTransito = ordersMeta.filter((o) => o.pedido.etapaActualNumero === 8).length;
  const enAduana = ordersMeta.filter((o) => o.pedido.etapaActualNumero === 9).length;
  const retrasados = ordersMeta.filter(
    (o) => o.summary.esAtrasado || o.pedido.subestadoAduana === 'Retenido'
  ).length;
  const proximasLlegadas = ordersMeta.filter(
    (o) =>
      o.pedido.estado !== 'Finalizado' &&
      o.pedido.estado !== 'Cancelado' &&
      o.summary.diasDiferencia >= 0 &&
      o.summary.diasDiferencia <= 30
  );

  // Mis tareas pendientes: pedidos donde currentUser tiene etapa asignada y está en curso
  const misTareasPendientes = ordersMeta.filter(({ pedido }) => {
    if (pedido.estado === 'Finalizado' || pedido.estado === 'Cancelado') return false;
    return canUserOperateStage(currentUser, pedido.etapaActualNumero);
  });

  return (
    <div className="space-y-6 sm:space-y-8 animate-in fade-in duration-200">
      {/* Saludo personalizado con nombre y rol */}
      <div className="bg-white rounded-3xl p-6 sm:p-8 border border-[#6C6B6D]/20 shadow-xs relative overflow-hidden">
        <div className="absolute top-0 left-0 right-0 h-1.5 bg-[#FFD100]" />
        <div className="flex flex-col md:flex-row md:items-center justify-between gap-4">
          <div>
            <span className="text-xs font-bold text-[#6C6B6D] uppercase tracking-wider block">
              Panel de Control
            </span>
            <h1 className="text-2xl sm:text-3xl font-black text-[#2E2E2E] mt-0.5">
              Hola, {currentUser.nombre} · {currentUser.cargo}
            </h1>
            <p className="text-xs sm:text-sm text-[#515151] font-medium mt-1">
              Área: <strong className="text-[#2E2E2E]">{currentUser.area}</strong> ({currentUser.pais}).
              Aquí tienes el resumen operativo al día de hoy.
            </p>
          </div>

          <button
            type="button"
            onClick={onNavigateToTracking}
            className="self-start md:self-center px-4 py-2.5 rounded-xl font-black text-xs bg-[#FFD100] hover:bg-[#E6BC00] text-[#2E2E2E] flex items-center gap-2 shadow-sm transition cursor-pointer"
          >
            <span>Buscar pedido rápido</span>
            <ArrowRight className="w-4 h-4 stroke-[3]" />
          </button>
        </div>
      </div>

      {/* Tarjeta: Mis tareas pendientes */}
      <div className="bg-white rounded-3xl border border-[#FFD100] p-6 shadow-xs relative overflow-hidden bg-gradient-to-r from-white via-white to-[#FFFBEA]">
        <div className="flex items-center justify-between border-b border-slate-100 pb-4 mb-4">
          <div className="flex items-center gap-2.5">
            <div className="w-8 h-8 rounded-xl bg-[#FFD100] text-[#2E2E2E] flex items-center justify-center font-bold">
              <ListTodo className="w-4 h-4" />
            </div>
            <div>
              <h2 className="text-base font-black text-[#2E2E2E]">Mis Tareas Pendientes</h2>
              <p className="text-xs text-[#6C6B6D]">
                Pedidos donde debes dar check o subir documentación en la etapa actual.
              </p>
            </div>
          </div>
          <span className="font-black text-xs px-2.5 py-1 rounded-full bg-[#FFD100] text-[#2E2E2E]">
            {misTareasPendientes.length} pendientes
          </span>
        </div>

        {misTareasPendientes.length === 0 ? (
          <p className="text-xs text-[#6C6B6D] italic py-2">
            No tienes tareas pendientes asignadas en este momento. ¡Todo al día!
          </p>
        ) : (
          <div className="grid grid-cols-1 md:grid-cols-2 gap-3">
            {misTareasPendientes.map(({ pedido, summary }) => {
              const currentStage = pedido.etapas[pedido.etapaActualNumero - 1];
              return (
                <div
                  key={pedido.id}
                  className="p-4 rounded-2xl border border-[#6C6B6D]/20 bg-white hover:border-[#FFD100] transition shadow-2xs flex flex-col justify-between gap-3"
                >
                  <div>
                    <div className="flex items-center justify-between gap-2">
                      <span className="font-mono text-xs font-black text-[#2E2E2E]">
                        {pedido.codigo}
                      </span>
                      <span
                        className={`text-[10px] font-black px-2 py-0.5 rounded-full ${
                          summary.semaforo === 'rojo'
                            ? 'bg-rose-100 text-[#DC2626]'
                            : summary.semaforo === 'naranja'
                            ? 'bg-orange-100 text-[#EA580C]'
                            : 'bg-emerald-100 text-[#16A34A]'
                        }`}
                      >
                        {summary.semaforo === 'rojo'
                          ? 'Retrasado'
                          : summary.semaforo === 'naranja'
                          ? 'Por vencer'
                          : 'A tiempo'}
                      </span>
                    </div>

                    <h4 className="text-xs font-black text-[#2E2E2E] mt-1 line-clamp-1">
                      {pedido.nombre}
                    </h4>

                    <p className="text-[11px] text-[#515151] mt-1 font-semibold">
                      Etapa {pedido.etapaActualNumero}: {currentStage?.nombre}
                    </p>
                  </div>

                  <div className="flex items-center justify-between pt-2 border-t border-slate-100 text-xs">
                    <span className="text-[11px] text-[#6C6B6D]">
                      Fin est: {formatDisplayDate(currentStage?.fechaFinEstimada)}
                    </span>
                    <button
                      type="button"
                      onClick={() => onSelectPedido(pedido)}
                      className="px-3 py-1.5 rounded-xl font-black text-xs bg-[#FFD100] hover:bg-[#E6BC00] text-[#2E2E2E] inline-flex items-center gap-1 shadow-2xs cursor-pointer"
                    >
                      <span>Ir a la etapa</span>
                      <ArrowRight className="w-3.5 h-3.5 stroke-[3]" />
                    </button>
                  </div>
                </div>
              );
            })}
          </div>
        )}
      </div>

      {/* Indicadores: Franja superior amarilla de 4px */}
      <div className="grid grid-cols-2 sm:grid-cols-3 lg:grid-cols-5 gap-3.5 sm:gap-4">
        {[
          { label: 'Pedidos en curso', value: totalEnCurso, icon: <Package className="w-4 h-4 text-[#2E2E2E]" />, desc: 'activos' },
          { label: 'En tránsito', value: enTransito, icon: <Ship className="w-4 h-4 text-[#2E2E2E]" />, desc: 'altamar' },
          { label: 'En aduana', value: enAduana, icon: <ShieldAlert className="w-4 h-4 text-[#2E2E2E]" />, desc: 'SENAE' },
          { label: 'Retrasados', value: retrasados, icon: <AlertTriangle className="w-4 h-4 text-[#DC2626]" />, desc: 'con alerta', alert: retrasados > 0 },
          { label: 'Próximas llegadas', value: proximasLlegadas.length, icon: <CalendarClock className="w-4 h-4 text-[#16A34A]" />, desc: '≤30 días' },
        ].map((kpi, idx) => (
          <div
            key={idx}
            className="bg-white p-4 rounded-2xl border border-[#6C6B6D]/20 shadow-xs relative overflow-hidden"
          >
            {/* Franja superior amarilla de 4 px */}
            <div className={`absolute top-0 left-0 right-0 h-1 ${kpi.alert ? 'bg-[#DC2626]' : 'bg-[#FFD100]'}`} />

            <div className="flex items-center justify-between text-[#6C6B6D]">
              <span className="text-xs font-bold truncate">{kpi.label}</span>
              <span className="p-1.5 rounded-lg bg-[#FFFBEA]">{kpi.icon}</span>
            </div>

            <div className="mt-3 flex items-baseline gap-2">
              <span className={`text-2xl sm:text-3xl font-black ${kpi.alert ? 'text-[#DC2626]' : 'text-[#2E2E2E]'}`}>
                {kpi.value}
              </span>
              <span className="text-[11px] text-[#6C6B6D] font-medium">{kpi.desc}</span>
            </div>
          </div>
        ))}
      </div>

      {/* Listas cortas: Próximas llegadas a bodega y Pedidos retrasados */}
      <div className="grid grid-cols-1 lg:grid-cols-2 gap-6">
        {/* Próximas llegadas a bodega */}
        <div className="bg-white rounded-3xl border border-[#6C6B6D]/20 p-6 shadow-xs space-y-4">
          <div className="flex items-center justify-between border-b border-slate-100 pb-3">
            <h3 className="text-sm font-black uppercase tracking-wider text-[#2E2E2E] flex items-center gap-2">
              <CalendarClock className="w-4 h-4 text-[#16A34A]" />
              <span>Próximas Llegadas a Bodega</span>
            </h3>
            <span className="text-xs font-bold text-[#6C6B6D]">Próximos 30 días</span>
          </div>

          <div className="space-y-2.5">
            {proximasLlegadas.slice(0, 4).map(({ pedido, summary }) => (
              <div
                key={pedido.id}
                onClick={() => onSelectPedido(pedido)}
                className="p-3.5 rounded-2xl border border-slate-200 hover:border-[#FFD100] bg-slate-50/50 hover:bg-[#FFFBEA]/40 transition flex items-center justify-between gap-3 cursor-pointer"
              >
                <div className="min-w-0">
                  <span className="font-mono text-xs font-bold text-[#2E2E2E] block truncate">
                    {pedido.codigo}
                  </span>
                  <span className="text-xs font-black text-[#515151] block truncate">
                    {pedido.nombre}
                  </span>
                  <span className="text-[10px] text-[#6C6B6D]">
                    Destino: Bodega {pedido.bodegaDestino}
                  </span>
                </div>

                <div className="text-right shrink-0">
                  <span className="text-[10px] text-[#6C6B6D] block font-semibold">ETA Bodega:</span>
                  <strong className="text-xs font-black text-[#2E2E2E]">
                    {summary.etaBodega}
                  </strong>
                </div>
              </div>
            ))}
          </div>
        </div>

        {/* Pedidos retrasados */}
        <div className="bg-white rounded-3xl border border-[#6C6B6D]/20 p-6 shadow-xs space-y-4">
          <div className="flex items-center justify-between border-b border-slate-100 pb-3">
            <h3 className="text-sm font-black uppercase tracking-wider text-[#DC2626] flex items-center gap-2">
              <AlertTriangle className="w-4 h-4 text-[#DC2626]" />
              <span>Pedidos con Retraso o Novedad</span>
            </h3>
            <span className="text-xs font-bold text-[#DC2626]">{retrasados} alertas</span>
          </div>

          <div className="space-y-2.5">
            {ordersMeta
              .filter((o) => o.summary.esAtrasado || o.pedido.subestadoAduana === 'Retenido')
              .slice(0, 4)
              .map(({ pedido, summary }) => (
                <div
                  key={pedido.id}
                  onClick={() => onSelectPedido(pedido)}
                  className="p-3.5 rounded-2xl border border-rose-200 bg-rose-50/60 hover:bg-rose-50 transition flex items-center justify-between gap-3 cursor-pointer"
                >
                  <div className="min-w-0">
                    <span className="font-mono text-xs font-bold text-[#2E2E2E] block truncate">
                      {pedido.codigo}
                    </span>
                    <span className="text-xs font-black text-[#2E2E2E] block truncate">
                      {pedido.nombre}
                    </span>
                    <span className="text-[10px] font-bold text-[#DC2626] block">
                      ⚠ {summary.aviso}
                    </span>
                  </div>

                  <span className="text-xs font-black text-[#DC2626] shrink-0">
                    Ver orden →
                  </span>
                </div>
              ))}
          </div>
        </div>
      </div>
    </div>
  );
};

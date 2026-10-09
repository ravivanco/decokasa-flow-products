import React from 'react';
import { Pedido, Usuario } from '../types';
import { calculateOrderSummary } from '../utils/orderState';
import { canUserOperateStage } from '../utils/permissions';
import { formatDisplayDate } from '../utils/dateUtils';
import {
  Package,
  Ship,
  AlertTriangle,
  Clock,
  ArrowRight,
  Plus,
  CheckCircle2,
  Tag,
  Warehouse,
  ShieldCheck,
  Sparkles,
  FileText,
  Anchor,
} from 'lucide-react';

interface DashboardViewProps {
  pedidos: Pedido[];
  currentUser: Usuario;
  onSelectPedido: (pedido: Pedido) => void;
  onNavigateToTracking: () => void;
  onNavigateToNewOrderA: () => void;
  onNavigateToNewOrderB: () => void;
  onNavigateToWarehouses: () => void;
}

export const DashboardView: React.FC<DashboardViewProps> = ({
  pedidos,
  currentUser,
  onSelectPedido,
  onNavigateToTracking,
  onNavigateToNewOrderA,
  onNavigateToNewOrderB,
  onNavigateToWarehouses,
}) => {
  // Resumen de cada pedido
  const ordersMeta = pedidos.map((p) => ({
    pedido: p,
    summary: calculateOrderSummary(p),
  }));

  // Métricas generales
  const totalEnCurso = ordersMeta.filter(
    (o) => o.pedido.estado !== 'Finalizado' && o.pedido.estado !== 'Cancelado'
  ).length;

  const pedidosConUrgencia = ordersMeta.filter(
    (o) => o.pedido.urgenciaDevolucion !== undefined
  );

  const pedidosEnTransito = ordersMeta.filter(
    (o) =>
      (o.pedido.flujo === 'A' && o.pedido.etapaActualNumero === 7) ||
      (o.pedido.flujo === 'B' && o.pedido.etapaActualNumero === 8)
  ).length;

  const pedidosDemorados = ordersMeta.filter(
    (o) => o.summary.esAtrasado || o.pedido.demoraAduana !== undefined
  ).length;

  // Tareas pendientes específicas para el rol del usuario
  const misTareasPendientes = ordersMeta.filter(({ pedido }) => {
    if (pedido.estado === 'Finalizado' || pedido.estado === 'Cancelado') return false;
    return canUserOperateStage(currentUser, pedido.etapaActualNumero, pedido.flujo);
  });

  return (
    <div className="space-y-6 sm:space-y-8 animate-in fade-in duration-200">
      {/* Banner de Bienvenida personalizado por Rol */}
      <div className="bg-white rounded-3xl p-6 sm:p-8 border border-[#6C6B6D]/20 shadow-xs relative overflow-hidden">
        <div className="absolute top-0 left-0 w-full h-1.5 bg-[#FFD100]" />

        <div className="flex flex-col md:flex-row md:items-center justify-between gap-4">
          <div className="space-y-1">
            <div className="flex items-center gap-2">
              <span className="text-xs font-black uppercase tracking-wider px-2.5 py-0.5 rounded-full bg-slate-100 text-[#515151]">
                {currentUser.area} · {currentUser.pais === 'China' ? '🇨🇳 China' : '🇪🇨 Ecuador'}
              </span>
              <span className="text-xs text-[#6C6B6D]">Rol: <strong>{currentUser.cargo}</strong></span>
            </div>

            <h1 className="text-2xl sm:text-3xl font-black text-[#2E2E2E]">
              Bienvenido, {currentUser.nombre}
            </h1>

            <p className="text-xs sm:text-sm text-[#515151] max-w-2xl">
              {currentUser.rol === 'admin' &&
                'Panel del Administrador General: Control total de etapas, asignaciones, usuarios y trazabilidad de pedidos de importación.'}
              {currentUser.rol === 'marketing' &&
                'Bandeja de Codificación: Revisa cantidades, modelos, colores y etiquetas de proformas. Atiende con prioridad las urgencias de 4 h y 12 h.'}
              {currentUser.rol === 'asistente_compras' &&
                'Bandeja de Revisión y Aprobación: Da el visto bueno (✓) o desaprueba (✗) la codificación, y revisa los productos propuestos por China.'}
              {currentUser.rol === 'compras_china' &&
                'Compras China: Analiza pedidos en 3 días hábiles, coordina fabricación, embarca en 10 días y reporta arribos o demoras en aduana.'}
              {currentUser.rol === 'logistica' &&
                'Logística Ecuador: Autoriza salidas de aduana, asigna bodegas de destino y sube expedientes de incidencias para el cierre final.'}
              {currentUser.rol === 'asistente_china' &&
                'Consulta China: Monitoreo y rastreo de avance de pedidos marítimos en curso.'}
              {currentUser.rol === 'consulta' &&
                'Panel de Consulta y Auditoría: Rastreo de embarques e historial inmutable.'}
            </p>
          </div>

          {/* Botones de acción rápida según rol */}
          <div className="flex flex-wrap items-center gap-2.5">
            {currentUser.rol === 'admin' && (
              <button
                type="button"
                onClick={onNavigateToNewOrderA}
                className="px-4 py-2.5 rounded-xl bg-[#FFD100] hover:bg-[#E6BC00] text-[#2E2E2E] font-black text-xs transition shadow-2xs flex items-center gap-1.5 cursor-pointer"
              >
                <Plus className="w-4 h-4 stroke-[2.5]" />
                <span>Crear Pedido (Flujo A)</span>
              </button>
            )}

            {currentUser.rol === 'compras_china' && (
              <button
                type="button"
                onClick={onNavigateToNewOrderB}
                className="px-4 py-2.5 rounded-xl bg-[#FFD100] hover:bg-[#E6BC00] text-[#2E2E2E] font-black text-xs transition shadow-2xs flex items-center gap-1.5 cursor-pointer"
              >
                <Sparkles className="w-4 h-4 text-amber-700" />
                <span>Proponer Producto Nuevo (Flujo B)</span>
              </button>
            )}

            {currentUser.rol === 'logistica' && (
              <button
                type="button"
                onClick={onNavigateToWarehouses}
                className="px-4 py-2.5 rounded-xl bg-white border border-[#6C6B6D]/30 hover:bg-slate-50 text-[#2E2E2E] font-black text-xs transition shadow-2xs flex items-center gap-1.5 cursor-pointer"
              >
                <Warehouse className="w-4 h-4 text-[#515151]" />
                <span>Catálogo de Bodegas</span>
              </button>
            )}

            <button
              type="button"
              onClick={onNavigateToTracking}
              className="px-4 py-2.5 rounded-xl bg-white border border-[#6C6B6D]/30 hover:bg-slate-50 text-[#2E2E2E] font-black text-xs transition shadow-2xs flex items-center gap-1.5 cursor-pointer"
            >
              <Package className="w-4 h-4 text-[#515151]" />
              <span>Rastrear Pedido</span>
            </button>
          </div>
        </div>
      </div>

      {/* Alerta Destacada si hay Urgencias Activas (4h, 12h, 24h) */}
      {pedidosConUrgencia.length > 0 && (
        <div className="bg-amber-50 border-2 border-amber-400 rounded-3xl p-5 shadow-xs space-y-3">
          <div className="flex items-center justify-between">
            <div className="flex items-center gap-2 text-amber-900 font-black text-sm">
              <span className="flex h-3 w-3 rounded-full bg-orange-500 animate-ping" />
              <span>Pedidos Devueltos con Urgencia Activa ({pedidosConUrgencia.length})</span>
            </div>
            <span className="text-xs text-amber-800 font-bold">
              Plazo especial para Codificación y Revisión
            </span>
          </div>

          <div className="grid grid-cols-1 md:grid-cols-2 gap-3">
            {pedidosConUrgencia.map(({ pedido }) => (
              <div
                key={pedido.id}
                onClick={() => onSelectPedido(pedido)}
                className="p-3.5 bg-white rounded-2xl border border-amber-300 shadow-2xs flex items-center justify-between gap-3 hover:border-amber-500 transition cursor-pointer"
              >
                <div className="min-w-0">
                  <div className="flex items-center gap-2">
                    <span className="px-2 py-0.5 rounded-md text-[10px] font-black uppercase bg-red-100 text-red-800">
                      Urgencia: {pedido.urgenciaDevolucion?.horasMaximas} h
                    </span>
                    <span className="font-mono text-xs font-black text-[#2E2E2E]">
                      {pedido.codigo}
                    </span>
                  </div>
                  <div className="text-xs font-bold text-[#515151] truncate mt-0.5">
                    {pedido.nombre}
                  </div>
                  <div className="text-[11px] text-[#6C6B6D] truncate">
                    Motivo: {pedido.urgenciaDevolucion?.motivo}
                  </div>
                </div>

                <div className="text-right shrink-0">
                  <span className="text-[10px] text-amber-800 font-bold block">Límite hoy:</span>
                  <span className="text-xs font-black font-mono text-[#2E2E2E]">
                    {pedido.urgenciaDevolucion?.fechaLimite.split(' ')[1] || '13:30'}
                  </span>
                </div>
              </div>
            ))}
          </div>
        </div>
      )}

      {/* Indicadores Principales (Cards con franja superior amarilla de 4px) */}
      <div className="grid grid-cols-2 sm:grid-cols-4 gap-4">
        {/* Total en curso */}
        <div className="bg-white rounded-2xl p-4 sm:p-5 border border-[#6C6B6D]/20 shadow-xs relative overflow-hidden">
          <div className="absolute top-0 left-0 w-full h-1 bg-[#FFD100]" />
          <div className="flex items-center justify-between text-[#6C6B6D] mb-2">
            <span className="text-[11px] font-black uppercase tracking-wider text-[#515151]">Total en Curso</span>
            <Package className="w-4 h-4" />
          </div>
          <div className="text-2xl sm:text-3xl font-black text-[#2E2E2E]">{totalEnCurso}</div>
          <p className="text-[10px] sm:text-[11px] text-[#6C6B6D] mt-1">Pedidos activos en importación</p>
        </div>

        {/* En tránsito marítimo */}
        <div className="bg-white rounded-2xl p-4 sm:p-5 border border-[#6C6B6D]/20 shadow-xs relative overflow-hidden">
          <div className="absolute top-0 left-0 w-full h-1 bg-[#FFD100]" />
          <div className="flex items-center justify-between text-[#6C6B6D] mb-2">
            <span className="text-[11px] font-black uppercase tracking-wider text-[#515151]">En Tránsito</span>
            <Ship className="w-4 h-4 text-blue-600" />
          </div>
          <div className="text-2xl sm:text-3xl font-black text-[#2E2E2E]">{pedidosEnTransito}</div>
          <p className="text-[10px] sm:text-[11px] text-[#6C6B6D] mt-1">Buques en altamar hacia puerto</p>
        </div>

        {/* Con retraso o demora en aduana */}
        <div className="bg-white rounded-2xl p-4 sm:p-5 border border-[#6C6B6D]/20 shadow-xs relative overflow-hidden">
          <div className="absolute top-0 left-0 w-full h-1 bg-red-500" />
          <div className="flex items-center justify-between text-[#6C6B6D] mb-2">
            <span className="text-[11px] font-black uppercase tracking-wider text-[#DC2626]">Con Retraso / Demora</span>
            <AlertTriangle className="w-4 h-4 text-[#DC2626]" />
          </div>
          <div className="text-2xl sm:text-3xl font-black text-[#DC2626]">{pedidosDemorados}</div>
          <p className="text-[10px] sm:text-[11px] text-[#6C6B6D] mt-1">Aforo aduanero o plazo vencido</p>
        </div>

        {/* Mis tareas pendientes */}
        <div className="bg-white rounded-2xl p-4 sm:p-5 border border-[#6C6B6D]/20 shadow-xs relative overflow-hidden">
          <div className="absolute top-0 left-0 w-full h-1 bg-[#FFD100]" />
          <div className="flex items-center justify-between text-[#6C6B6D] mb-2">
            <span className="text-[11px] font-black uppercase tracking-wider text-[#515151]">Mis Tareas</span>
            <CheckCircle2 className="w-4 h-4 text-emerald-600" />
          </div>
          <div className="text-2xl sm:text-3xl font-black text-[#2E2E2E]">
            {currentUser.rol === 'admin' ? totalEnCurso : misTareasPendientes.length}
          </div>
          <p className="text-[10px] sm:text-[11px] text-[#6C6B6D] mt-1">Esperando tu acción operativa</p>
        </div>
      </div>

      {/* Bloque: Tareas Pendientes para el Usuario Activo */}
      <div className="bg-white rounded-3xl p-6 border border-[#6C6B6D]/20 shadow-xs space-y-4">
        <div className="flex items-center justify-between border-b border-slate-100 pb-3">
          <div>
            <h2 className="text-base font-black text-[#2E2E2E]">
              {currentUser.rol === 'admin'
                ? 'Todos los Pedidos en Curso (Supervisión Admin)'
                : `Pedidos que requieren tu acción (${misTareasPendientes.length})`}
            </h2>
            <p className="text-xs text-[#515151]">
              Haz clic en cualquier pedido para revisar documentos, dar check, aprobar o devolver.
            </p>
          </div>
          <span className="text-xs font-bold text-[#6C6B6D]">
            {currentUser.nombre} ({currentUser.cargo})
          </span>
        </div>

        {(currentUser.rol === 'admin' ? ordersMeta : misTareasPendientes).length === 0 ? (
          <div className="text-center py-10 space-y-2">
            <CheckCircle2 className="w-8 h-8 text-emerald-500 mx-auto" />
            <p className="text-sm font-bold text-[#2E2E2E]">
              ¡Al día! No tienes pedidos pendientes en tu etapa en este momento.
            </p>
            <p className="text-xs text-[#6C6B6D]">
              Puedes explorar los demás pedidos en el menú "Pedidos".
            </p>
          </div>
        ) : (
          <div className="divide-y divide-slate-100">
            {(currentUser.rol === 'admin' ? ordersMeta.slice(0, 5) : misTareasPendientes).map(
              ({ pedido, summary }) => {
                const currentStage = pedido.etapas[pedido.etapaActualNumero - 1];
                return (
                  <div
                    key={pedido.id}
                    onClick={() => onSelectPedido(pedido)}
                    className="py-3.5 flex flex-col sm:flex-row sm:items-center justify-between gap-3 hover:bg-slate-50/80 rounded-xl px-2.5 transition cursor-pointer"
                  >
                    <div className="flex items-start gap-3 min-w-0">
                      <div className="p-2 rounded-xl bg-slate-100 text-[#2E2E2E] font-mono text-xs font-black shrink-0 mt-0.5">
                        {pedido.flujo === 'A' ? 'Flujo A' : 'Flujo B'}
                      </div>
                      <div className="min-w-0">
                        <div className="flex items-center gap-2">
                          <span className="font-mono font-black text-xs text-[#2E2E2E]">
                            {pedido.codigo}
                          </span>
                          <span
                            className={`px-2 py-0.5 rounded-full text-[10px] font-black uppercase ${
                              pedido.estado === 'Finalizado'
                                ? 'bg-emerald-100 text-emerald-800'
                                : pedido.estado === 'Devuelto a codificación'
                                ? 'bg-amber-100 text-amber-800'
                                : 'bg-slate-100 text-[#515151]'
                            }`}
                          >
                            {pedido.estado}
                          </span>
                        </div>
                        <div className="text-xs font-bold text-[#2E2E2E] truncate mt-0.5">
                          {pedido.nombre}
                        </div>
                        <div className="text-[11px] text-[#6C6B6D]">
                          Etapa {pedido.etapaActualNumero}:{' '}
                          <strong className="text-[#515151]">{currentStage?.nombre}</strong> · Plazo:{' '}
                          {currentStage?.plazoTexto}
                        </div>
                      </div>
                    </div>

                    <div className="flex items-center justify-between sm:justify-end gap-3 shrink-0">
                      <div className="text-right">
                        <div
                          className={`text-xs font-black inline-flex items-center gap-1 ${
                            summary.semaforo === 'rojo'
                              ? 'text-[#DC2626]'
                              : summary.semaforo === 'naranja'
                              ? 'text-[#EA580C]'
                              : 'text-[#16A34A]'
                          }`}
                        >
                          <span>{summary.semaforo === 'rojo' ? '⚠' : summary.semaforo === 'naranja' ? '🕒' : '✓'}</span>
                          <span>{summary.aviso}</span>
                        </div>
                        <div className="text-[10px] text-[#6C6B6D]">
                          Llegada bodega: <strong className="font-mono">{summary.etaBodega}</strong>
                        </div>
                      </div>

                      <ArrowRight className="w-4 h-4 text-[#6C6B6D]" />
                    </div>
                  </div>
                );
              }
            )}
          </div>
        )}
      </div>
    </div>
  );
};

import React, { useState, useMemo } from 'react';
import { Pedido, Usuario, TipoFlujo, Semaforo } from '../types';
import { calculateOrderSummary } from '../utils/orderState';
import { formatDisplayDate, SIMULATED_TODAY } from '../utils/dateUtils';
import { canUserCreateOrder } from '../utils/permissions';
import { buildStagesForOrder } from '../data/initialData';
import {
  Plus,
  Search,
  SlidersHorizontal,
  Clock,
  AlertTriangle,
  CheckCircle2,
  Calendar,
  Sparkles,
  ArrowRight,
  X,
  Upload,
  FileText,
} from 'lucide-react';

interface OrdersListViewProps {
  pedidos: Pedido[];
  currentUser: Usuario;
  onSelectPedido: (pedido: Pedido) => void;
  onAddNewOrder: (newOrder: Pedido) => void;
}

export const OrdersListView: React.FC<OrdersListViewProps> = ({
  pedidos,
  currentUser,
  onSelectPedido,
  onAddNewOrder,
}) => {
  const [searchTerm, setSearchTerm] = useState('');
  const [filterFlujo, setFilterFlujo] = useState<string>('todos');
  const [filterSemaforo, setFilterSemaforo] = useState<string>('todos');
  const [showNewOrderModal, setShowNewOrderModal] = useState(false);

  // Modal new order state
  const [newFlujo, setNewFlujo] = useState<TipoFlujo>(
    currentUser.rol === 'compras_china' ? 'B' : 'A'
  );
  const [newNombre, setNewNombre] = useState('');
  const [newDescripcion, setNewDescripcion] = useState('');
  const [newEsProductoNuevoEc, setNewEsProductoNuevoEc] = useState(false);
  const [newProductoPropuestoChina, setNewProductoPropuestoChina] = useState('');
  const [uploadedFile, setUploadedFile] = useState<string | null>(null);

  const canCreateA = canUserCreateOrder(currentUser, 'A');
  const canCreateB = canUserCreateOrder(currentUser, 'B');

  const ordersMeta = useMemo(() => {
    return pedidos.map((p) => ({
      pedido: p,
      summary: calculateOrderSummary(p),
    }));
  }, [pedidos]);

  const filteredOrders = useMemo(() => {
    return ordersMeta.filter(({ pedido, summary }) => {
      if (filterFlujo !== 'todos' && pedido.flujo !== filterFlujo) return false;
      if (filterSemaforo !== 'todos' && summary.semaforo !== filterSemaforo) return false;

      if (searchTerm.trim()) {
        const term = searchTerm.toLowerCase();
        const matchCode = pedido.codigo.toLowerCase().includes(term);
        const matchName = pedido.nombre.toLowerCase().includes(term);
        const matchState = pedido.estado.toLowerCase().includes(term);
        if (!matchCode && !matchName && !matchState) return false;
      }

      return true;
    });
  }, [ordersMeta, filterFlujo, filterSemaforo, searchTerm]);

  const handleCreateSubmit = (e: React.FormEvent) => {
    e.preventDefault();
    if (!newNombre.trim()) return;

    // Generar siguiente número DK-EC-2026-XXXX
    const nextSeq = pedidos.length + 1;
    const seqStr = String(nextSeq).padStart(4, '0');
    const codigo = `DK-EC-2026-${seqStr}`;

    const newOrder: Pedido = {
      id: `ped-${Date.now()}`,
      codigo,
      pais: 'EC',
      flujo: newFlujo,
      nombre: newNombre.trim(),
      descripcion: newDescripcion.trim() || undefined,
      fechaPedido: SIMULATED_TODAY,
      creadoPor: currentUser.nombre,
      creadoPorRol: currentUser.cargo,
      version: 'v1',
      esProductoNuevoEcuador: newFlujo === 'A' ? newEsProductoNuevoEc : false,
      productoPropuestoChinaNombre:
        newFlujo === 'B' ? newProductoPropuestoChina.trim() || newNombre.trim() : undefined,
      etapaActualNumero: 1,
      estado: 'Solicitud creada',
      productos: [
        {
          id: `p-${Date.now()}`,
          codigoDecokasa: `DK-${newNombre.slice(0, 3).toUpperCase()}-01`,
          descripcion: newNombre.trim(),
          medida: 'Estándar',
          color: 'Surtido',
          cantidadPedida: 1000,
          estado: 'Pendiente',
        },
      ],
      etapas: buildStagesForOrder(newFlujo, SIMULATED_TODAY, 1),
      historial: [
        {
          id: `hist-${Date.now()}`,
          fecha: `${formatDisplayDate(SIMULATED_TODAY)} 09:00`,
          usuarioId: currentUser.id,
          usuarioNombre: currentUser.nombre,
          usuarioRol: currentUser.cargo,
          pedidoCodigo: codigo,
          accion:
            newFlujo === 'A'
              ? 'Creó solicitud de pedido en Flujo A (Ecuador)'
              : 'Creó solicitud de pedido en Flujo B con producto propuesto desde China',
          etapaNumero: 1,
          etapaNombre:
            newFlujo === 'A' ? 'Crear solicitud de pedido' : 'Crear solicitud de pedido (China)',
          tipo: 'check',
        },
      ],
    };

    onAddNewOrder(newOrder);
    setShowNewOrderModal(false);
    setNewNombre('');
    setNewDescripcion('');
    setUploadedFile(null);
  };

  return (
    <div className="space-y-6 sm:space-y-8 animate-in fade-in duration-200">
      {/* Header Bar */}
      <div className="flex flex-col sm:flex-row sm:items-center justify-between gap-4">
        <div>
          <h1 className="text-2xl sm:text-3xl font-black text-[#2E2E2E]">
            Pedidos de Importación
          </h1>
          <p className="text-xs sm:text-sm text-[#515151] mt-0.5">
            Flujo A (Ecuador) y Flujo B (China) con número secuencial único (DK-EC-2026-XXXX).
          </p>
        </div>

        {(canCreateA || canCreateB) && (
          <button
            type="button"
            onClick={() => {
              setNewFlujo(currentUser.rol === 'compras_china' ? 'B' : 'A');
              setShowNewOrderModal(true);
            }}
            className="px-4 py-2.5 rounded-2xl bg-[#FFD100] hover:bg-[#E6BC00] text-[#2E2E2E] font-black text-xs transition shadow-2xs flex items-center justify-center gap-2 cursor-pointer shrink-0"
          >
            <Plus className="w-4 h-4 stroke-[2.5]" />
            <span>Crear Solicitud de Pedido</span>
          </button>
        )}
      </div>

      {/* Filter and Search Bar */}
      <div className="bg-white rounded-3xl p-4 sm:p-5 border border-[#6C6B6D]/20 shadow-xs flex flex-col md:flex-row gap-3">
        {/* Search */}
        <div className="relative flex-1">
          <Search className="w-4 h-4 text-[#6C6B6D] absolute left-3.5 top-1/2 -translate-y-1/2" />
          <input
            type="text"
            placeholder="Buscar por código (DK-EC-2026-XXXX), producto o estado..."
            value={searchTerm}
            onChange={(e) => setSearchTerm(e.target.value)}
            className="w-full pl-10 pr-4 py-2.5 rounded-xl border border-[#6C6B6D]/20 text-xs font-semibold text-[#2E2E2E] focus:outline-hidden focus:ring-2 focus:ring-[#FFD100]"
          />
        </div>

        {/* Filter Flow */}
        <div className="flex items-center gap-2">
          <select
            value={filterFlujo}
            onChange={(e) => setFilterFlujo(e.target.value)}
            className="px-3 py-2.5 rounded-xl border border-[#6C6B6D]/20 text-xs font-bold text-[#2E2E2E] focus:outline-hidden focus:ring-2 focus:ring-[#FFD100] cursor-pointer"
          >
            <option value="todos">Todos los Flujos (A y B)</option>
            <option value="A">Flujo A · Pedido Ecuador</option>
            <option value="B">Flujo B · Propuesto por China</option>
          </select>
        </div>

        {/* Filter Traffic light */}
        <div className="flex items-center gap-2">
          <select
            value={filterSemaforo}
            onChange={(e) => setFilterSemaforo(e.target.value)}
            className="px-3 py-2.5 rounded-xl border border-[#6C6B6D]/20 text-xs font-bold text-[#2E2E2E] focus:outline-hidden focus:ring-2 focus:ring-[#FFD100] cursor-pointer"
          >
            <option value="todos">Todos los Semáforos</option>
            <option value="verde">A tiempo (Verde)</option>
            <option value="naranja">Por vencer (Naranja 🕒)</option>
            <option value="rojo">Retrasado (Rojo ⚠)</option>
          </select>
        </div>
      </div>

      {/* Orders Table */}
      <div className="bg-white rounded-3xl border border-[#6C6B6D]/20 shadow-xs overflow-hidden">
        <div className="overflow-x-auto">
          <table className="w-full text-left border-collapse">
            <thead>
              <tr className="bg-slate-50 border-b border-slate-200 text-[11px] font-black uppercase tracking-wider text-[#6C6B6D]">
                <th className="py-3 px-4">Código / Flujo</th>
                <th className="py-3 px-4">Producto y Descripción</th>
                <th className="py-3 px-4">Fecha Pedido</th>
                <th className="py-3 px-4">Etapa Actual</th>
                <th className="py-3 px-4">Estado / Semáforo</th>
                <th className="py-3 px-4">Llegada Bodega</th>
                <th className="py-3 px-4 text-right">Acción</th>
              </tr>
            </thead>
            <tbody className="divide-y divide-slate-100 text-xs">
              {filteredOrders.length === 0 ? (
                <tr>
                  <td colSpan={7} className="py-12 text-center text-xs text-[#6C6B6D]">
                    No se encontraron pedidos con los filtros seleccionados.
                  </td>
                </tr>
              ) : (
                filteredOrders.map(({ pedido, summary }) => {
                  const currentStage = pedido.etapas[pedido.etapaActualNumero - 1];
                  return (
                    <tr
                      key={pedido.id}
                      onClick={() => onSelectPedido(pedido)}
                      className="hover:bg-slate-50/80 transition cursor-pointer"
                    >
                      {/* Código y Flujo */}
                      <td className="py-3.5 px-4 whitespace-nowrap">
                        <div className="font-mono font-black text-xs text-[#2E2E2E]">
                          {pedido.codigo}
                        </div>
                        <div className="flex items-center gap-1.5 mt-0.5">
                          <span className="text-[10px] font-black uppercase px-2 py-0.5 rounded-md bg-slate-100 text-[#515151]">
                            {pedido.flujo === 'A' ? 'Flujo A' : 'Flujo B'}
                          </span>
                          {pedido.urgenciaDevolucion && (
                            <span className="text-[10px] font-black uppercase px-1.5 py-0.5 rounded-md bg-red-100 text-red-800 animate-pulse">
                              ⚡ {pedido.urgenciaDevolucion.horasMaximas}h
                            </span>
                          )}
                        </div>
                      </td>

                      {/* Producto */}
                      <td className="py-3.5 px-4 min-w-48 max-w-xs">
                        <div className="font-bold text-[#2E2E2E] truncate">
                          {pedido.nombre}
                        </div>
                        <div className="text-[11px] text-[#6C6B6D] truncate">
                          {pedido.descripcion || `Creado por ${pedido.creadoPor}`}
                        </div>
                      </td>

                      {/* Fecha Pedido */}
                      <td className="py-3.5 px-4 font-mono text-xs text-[#515151] whitespace-nowrap">
                        {formatDisplayDate(pedido.fechaPedido)}
                      </td>

                      {/* Etapa Actual */}
                      <td className="py-3.5 px-4 whitespace-nowrap">
                        <div className="font-bold text-[#2E2E2E]">
                          Etapa {pedido.etapaActualNumero}: {currentStage?.nombre}
                        </div>
                        <div className="text-[10px] text-[#6C6B6D]">
                          Plazo: {currentStage?.plazoTexto}
                        </div>
                      </td>

                      {/* Estado y Semáforo */}
                      <td className="py-3.5 px-4 whitespace-nowrap">
                        <div
                          className={`inline-flex items-center gap-1 font-bold ${
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
                          {pedido.estado}
                        </div>
                      </td>

                      {/* Llegada Bodega */}
                      <td className="py-3.5 px-4 font-mono font-bold text-xs text-[#2E2E2E] whitespace-nowrap">
                        {summary.etaBodega}
                      </td>

                      {/* Acción */}
                      <td className="py-3.5 px-4 text-right whitespace-nowrap">
                        <button
                          type="button"
                          onClick={(e) => {
                            e.stopPropagation();
                            onSelectPedido(pedido);
                          }}
                          className="px-3 py-1.5 rounded-xl bg-slate-100 hover:bg-[#FFD100] text-[#2E2E2E] font-black text-xs transition cursor-pointer"
                        >
                          Ver Detalle
                        </button>
                      </td>
                    </tr>
                  );
                })
              )}
            </tbody>
          </table>
        </div>
      </div>

      {/* Modal Crear Solicitud de Pedido (Flujo A o B) */}
      {showNewOrderModal && (
        <div className="fixed inset-0 z-50 bg-black/50 backdrop-blur-xs flex items-center justify-center p-4">
          <div className="bg-white rounded-3xl max-w-xl w-full p-6 sm:p-8 space-y-5 shadow-2xl animate-in zoom-in-95 duration-150">
            <div className="flex items-center justify-between border-b border-slate-100 pb-3">
              <div>
                <h3 className="text-lg font-black text-[#2E2E2E]">
                  Crear Solicitud de Pedido de Importación
                </h3>
                <p className="text-xs text-[#6C6B6D]">
                  Genera el número DK-EC-2026-XXXX y activa el flujo documental.
                </p>
              </div>
              <button
                type="button"
                onClick={() => setShowNewOrderModal(false)}
                className="p-1 rounded-lg text-[#6C6B6D] hover:bg-slate-100 cursor-pointer"
              >
                <X className="w-5 h-5" />
              </button>
            </div>

            <form onSubmit={handleCreateSubmit} className="space-y-4">
              {/* Selector Flujo A vs Flujo B */}
              <div>
                <label className="block text-xs font-black uppercase tracking-wider text-[#515151] mb-1.5">
                  Tipo de Flujo de Importación
                </label>
                <div className="grid grid-cols-2 gap-2">
                  <div
                    onClick={() => canCreateA && setNewFlujo('A')}
                    className={`p-3 rounded-2xl border text-left cursor-pointer transition ${
                      newFlujo === 'A'
                        ? 'border-2 border-[#FFD100] bg-[#FFFBEA]'
                        : 'border-slate-200 hover:bg-slate-50'
                    } ${!canCreateA ? 'opacity-40 cursor-not-allowed' : ''}`}
                  >
                    <div className="font-black text-xs text-[#2E2E2E]">Flujo A (Ecuador)</div>
                    <div className="text-[10px] text-[#6C6B6D]">
                      Iniciado por Mirian / Admin. 11 etapas. Productos existentes o pedidos por Ecuador.
                    </div>
                  </div>

                  <div
                    onClick={() => canCreateB && setNewFlujo('B')}
                    className={`p-3 rounded-2xl border text-left cursor-pointer transition ${
                      newFlujo === 'B'
                        ? 'border-2 border-[#FFD100] bg-[#FFFBEA]'
                        : 'border-slate-200 hover:bg-slate-50'
                    } ${!canCreateB ? 'opacity-40 cursor-not-allowed' : ''}`}
                  >
                    <div className="font-black text-xs text-[#2E2E2E]">Flujo B (China)</div>
                    <div className="text-[10px] text-[#6C6B6D]">
                      Iniciado por David Túlcan. 12 etapas. Producto nuevo propuesto desde China.
                    </div>
                  </div>
                </div>
              </div>

              {/* Nombre del pedido / mercadería */}
              <div>
                <label className="block text-xs font-black uppercase tracking-wider text-[#515151] mb-1.5">
                  Nombre de la Mercadería / Producto
                </label>
                <input
                  type="text"
                  required
                  placeholder="Ej: Paneles Acústicos Ranurados WPC"
                  value={newNombre}
                  onChange={(e) => setNewNombre(e.target.value)}
                  className="w-full px-3.5 py-2.5 rounded-xl border border-[#6C6B6D]/30 text-xs font-semibold text-[#2E2E2E] focus:outline-hidden focus:ring-2 focus:ring-[#FFD100]"
                />
              </div>

              {/* Si es Flujo A, opción de producto nuevo pedido por Ecuador */}
              {newFlujo === 'A' && (
                <label className="flex items-center gap-2 p-2.5 rounded-xl bg-slate-50 border border-slate-200 cursor-pointer text-xs font-bold text-[#515151]">
                  <input
                    type="checkbox"
                    checked={newEsProductoNuevoEc}
                    onChange={(e) => setNewEsProductoNuevoEc(e.target.checked)}
                    className="w-4 h-4 rounded text-amber-500 focus:ring-[#FFD100]"
                  />
                  <span>¿Incluye un producto nuevo solicitado por Ecuador?</span>
                </label>
              )}

              {/* Si es Flujo B, nombre del producto propuesto por David */}
              {newFlujo === 'B' && (
                <div>
                  <label className="block text-xs font-black uppercase tracking-wider text-[#515151] mb-1.5">
                    Especificación del Producto Propuesto desde China
                  </label>
                  <input
                    type="text"
                    placeholder="Ej: Mármol Sintético Flexible UV 3mm"
                    value={newProductoPropuestoChina}
                    onChange={(e) => setNewProductoPropuestoChina(e.target.value)}
                    className="w-full px-3.5 py-2.5 rounded-xl border border-[#6C6B6D]/30 text-xs font-semibold text-[#2E2E2E] focus:outline-hidden focus:ring-2 focus:ring-[#FFD100]"
                  />
                </div>
              )}

              {/* Descripción */}
              <div>
                <label className="block text-xs font-black uppercase tracking-wider text-[#515151] mb-1.5">
                  Observaciones / Descripción Inicial
                </label>
                <textarea
                  rows={2}
                  placeholder="Detalles sobre contenedor, fábrica o solicitud..."
                  value={newDescripcion}
                  onChange={(e) => setNewDescripcion(e.target.value)}
                  className="w-full px-3.5 py-2.5 rounded-xl border border-[#6C6B6D]/30 text-xs font-semibold text-[#2E2E2E] focus:outline-hidden focus:ring-2 focus:ring-[#FFD100]"
                />
              </div>

              {/* Simulación de archivo en Google Drive */}
              <div>
                <label className="block text-xs font-black uppercase tracking-wider text-[#515151] mb-1.5">
                  Plantilla de Solicitud de Pedido (Google Drive · Límite 100 MB)
                </label>
                <div className="p-3 rounded-2xl border-2 border-dashed border-[#6C6B6D]/30 bg-slate-50 text-center space-y-1">
                  <Upload className="w-5 h-5 text-[#6C6B6D] mx-auto" />
                  <p className="text-xs font-bold text-[#2E2E2E]">
                    {uploadedFile ? `Archivo cargado: ${uploadedFile}` : 'Subir formato de pedido (PDF/Excel)'}
                  </p>
                  <p className="text-[10px] text-[#6C6B6D]">
                    Se almacena automáticamente en Google Drive mediante cuenta de servicio.
                  </p>
                  <button
                    type="button"
                    onClick={() => setUploadedFile('Solicitud_Pedido_Decokasa_2026.xlsx')}
                    className="mt-1 px-3 py-1 rounded-lg bg-white border border-slate-200 text-xs font-bold text-[#2E2E2E] hover:bg-slate-100 cursor-pointer"
                  >
                    Simular adjunto de Drive (14.2 MB)
                  </button>
                </div>
              </div>

              <div className="flex items-center justify-end gap-2.5 pt-2">
                <button
                  type="button"
                  onClick={() => setShowNewOrderModal(false)}
                  className="px-4 py-2.5 rounded-xl bg-slate-100 hover:bg-slate-200 text-[#2E2E2E] font-bold text-xs transition cursor-pointer"
                >
                  Cancelar
                </button>
                <button
                  type="submit"
                  className="px-5 py-2.5 rounded-xl bg-[#FFD100] hover:bg-[#E6BC00] text-[#2E2E2E] font-black text-xs transition shadow-md cursor-pointer"
                >
                  Crear y Enviar a Codificación
                </button>
              </div>
            </form>
          </div>
        </div>
      )}
    </div>
  );
};

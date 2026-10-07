import React, { useState, useMemo } from 'react';
import { Pedido, Usuario, Semaforo, BodegaDestino } from '../types';
import { calculateOrderSummary } from '../utils/orderState';
import { formatDisplayDate, SIMULATED_TODAY, addBusinessDays } from '../utils/dateUtils';
import { buildChainedStages, ETAPAS_BASE_CONFIG } from '../data/initialData';
import {
  Plus,
  Search,
  SlidersHorizontal,
  Clock,
  AlertTriangle,
  PauseCircle,
  CheckCircle2,
  Calendar,
  MapPin,
  Upload,
  ArrowRight,
  X,
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
  const [filterStatus, setFilterStatus] = useState<string>('todos');
  const [filterTraffic, setFilterTraffic] = useState<string>('todos');
  const [showNewOrderModal, setShowNewOrderModal] = useState(false);

  // New order form state
  const [newProductName, setNewProductName] = useState('');
  const [newOrderDate, setNewOrderDate] = useState(SIMULATED_TODAY);
  const [newIsNewProduct, setNewIsNewProduct] = useState(false);
  const [newWarehouse, setNewWarehouse] = useState<BodegaDestino>('Chongón');
  const [newExcelFile, setNewExcelFile] = useState<File | null>(null);
  const [newZipFile, setNewZipFile] = useState<File | null>(null);
  const [newOrderError, setNewOrderError] = useState<string | null>(null);

  const canCreateOrder = currentUser.rol === 'admin' || currentUser.rol === 'editor_marketing';

  // Calculate order items with summaries
  const ordersMeta = useMemo(() => {
    return pedidos.map((p) => ({
      pedido: p,
      summary: calculateOrderSummary(p),
    }));
  }, [pedidos]);

  // Filtering
  const filteredOrders = useMemo(() => {
    return ordersMeta.filter(({ pedido, summary }) => {
      // Search
      const term = searchTerm.toLowerCase().trim();
      if (term) {
        const matches =
          pedido.codigo.toLowerCase().includes(term) ||
          pedido.nombre.toLowerCase().includes(term) ||
          (pedido.proveedor || '').toLowerCase().includes(term);
        if (!matches) return false;
      }

      // Semáforo
      if (filterTraffic !== 'todos' && summary.semaforo !== filterTraffic) {
        return false;
      }

      // Status
      if (filterStatus !== 'todos') {
        if (filterStatus === 'pausa' && pedido.estado !== 'En pausa') return false;
        if (filterStatus === 'finalizado' && pedido.estado !== 'Finalizado') return false;
        if (filterStatus === 'retrasado' && !summary.esAtrasado) return false;
        if (filterStatus === 'aduana' && pedido.etapaActualNumero !== 9) return false;
        if (filterStatus === 'transito' && pedido.etapaActualNumero !== 8) return false;
      }

      return true;
    });
  }, [ordersMeta, searchTerm, filterStatus, filterTraffic]);

  // Preview code generator: IMP-AAAA-NNN-PRODUCTO
  const nextOrderNumber = String(pedidos.length + 1).padStart(3, '0');
  const cleanShortName = newProductName.trim()
    ? newProductName
        .trim()
        .slice(0, 15)
        .toUpperCase()
        .replace(/[^A-Z0-9]/g, '-')
    : 'PRODUCTO';
  const previewCode = `IMP-2026-${nextOrderNumber}-${cleanShortName}`;

  // Preview schedule calculation
  const previewSchedule = useMemo(() => {
    return buildChainedStages(newOrderDate, newIsNewProduct, 0, {}, 1);
  }, [newOrderDate, newIsNewProduct]);

  const handleCreateOrderSubmit = (e: React.FormEvent) => {
    e.preventDefault();
    setNewOrderError(null);

    if (!newProductName.trim()) {
      setNewOrderError('Por favor ingresa el nombre del producto.');
      return;
    }
    if (!newExcelFile) {
      setNewOrderError('Debes subir el archivo Excel del pedido (obligatorio).');
      return;
    }
    if (!newZipFile) {
      setNewOrderError('Debes subir el archivo Zip de etiquetas (obligatorio).');
      return;
    }

    const newOrder: Pedido = {
      id: `ped-${Date.now()}`,
      codigo: previewCode,
      nombre: newProductName.trim(),
      descripcion: `Importación regular programada para ${newProductName.trim()}.`,
      proveedor: 'Fábrica Asignada en China',
      puertoOrigen: 'Ningbo, China',
      puertoDestino: 'Guayaquil (Posorja), Ecuador',
      contenedor: 'Por asignar en etapa 7',
      blNumero: 'Por emitir en etapa 7',
      fechaPedido: newOrderDate,
      responsablePedido: currentUser.nombre,
      gestionChina: 'David Tulcán (Compras China)',
      elaboradoPor: currentUser.nombre,
      version: 'v1',
      esProductoNuevo: newIsNewProduct,
      ampliacionFabricacionDias: 0,
      etapaActualNumero: 1,
      estado: 'Creado',
      bodegaDestino: newWarehouse,
      productos: [
        {
          id: `p-${Date.now()}-1`,
          codigoDecokasa: 'ITEM-01',
          codigoChino: 'CN-ITEM-01',
          descripcion: newProductName.trim(),
          medida: 'Estándar',
          color: 'Según pedido',
          cantidadPedida: 1000,
          estado: 'Pendiente',
        },
      ],
      etapas: previewSchedule,
      historial: [
        {
          id: `h-${Date.now()}`,
          fecha: `${formatDisplayDate(SIMULATED_TODAY)} 09:00`,
          usuarioId: currentUser.id,
          usuarioNombre: currentUser.nombre,
          usuarioRol: currentUser.cargo,
          pedidoCodigo: previewCode,
          etapaNumero: 1,
          etapaNombre: 'Solicitud y creación',
          accion: 'Creó el pedido formal de importación',
          motivo: newIsNewProduct ? 'Lanzamiento de producto nuevo (+10 días en revisión)' : 'Creación en sistema',
          tipo: 'check',
        },
      ],
    };

    onAddNewOrder(newOrder);
    setShowNewOrderModal(false);
    // Reset fields
    setNewProductName('');
    setNewExcelFile(null);
    setNewZipFile(null);
  };

  return (
    <div className="space-y-6 sm:space-y-8 animate-in fade-in duration-200">
      {/* Header and New Order Action */}
      <div className="flex flex-col sm:flex-row sm:items-center justify-between gap-4">
        <div>
          <h1 className="text-2xl sm:text-3xl font-black text-[#2E2E2E]">
            Listado General de Pedidos
          </h1>
          <p className="text-xs sm:text-sm text-[#515151] mt-0.5">
            Consulta el estado, la etapa operativa y la fecha estimada de llegada a bodega.
          </p>
        </div>

        {canCreateOrder && (
          <button
            type="button"
            onClick={() => setShowNewOrderModal(true)}
            className="px-5 py-2.5 rounded-2xl font-black text-sm bg-[#FFD100] hover:bg-[#E6BC00] text-[#2E2E2E] flex items-center gap-2 shadow-sm transition cursor-pointer self-start sm:self-auto"
          >
            <Plus className="w-4 h-4 stroke-[3]" />
            <span>Nuevo pedido</span>
          </button>
        )}
      </div>

      {/* Filter and Search Bar */}
      <div className="bg-white p-5 rounded-3xl border border-[#6C6B6D]/20 shadow-xs space-y-4">
        <div className="flex flex-col md:flex-row items-stretch md:items-center justify-between gap-3">
          {/* Search box */}
          <div className="relative flex-1">
            <Search className="w-4 h-4 text-[#6C6B6D] absolute left-3.5 top-1/2 -translate-y-1/2" />
            <input
              type="text"
              placeholder="Buscar por código, nombre del producto o proveedor..."
              value={searchTerm}
              onChange={(e) => setSearchTerm(e.target.value)}
              className="w-full pl-10 pr-4 py-2.5 rounded-2xl border border-[#6C6B6D]/30 text-xs text-[#2E2E2E] focus:outline-hidden focus:ring-2 focus:ring-[#FFD100]"
            />
          </div>

          {/* Quick status dropdown */}
          <div className="flex items-center gap-2 flex-wrap">
            <select
              value={filterStatus}
              onChange={(e) => setFilterStatus(e.target.value)}
              className="px-3 py-2 rounded-xl border border-[#6C6B6D]/30 text-xs font-bold text-[#515151] bg-[#FFFBEA]"
            >
              <option value="todos">Todos los estados</option>
              <option value="transito">En tránsito marítimo</option>
              <option value="aduana">En aduana</option>
              <option value="retrasado">Con retraso</option>
              <option value="pausa">En pausa</option>
              <option value="finalizado">Finalizados</option>
            </select>
          </div>
        </div>

        {/* Semáforo Filters */}
        <div className="flex items-center gap-2 flex-wrap pt-2 border-t border-slate-100 text-xs">
          <span className="font-bold text-[#515151] flex items-center gap-1 mr-1">
            <SlidersHorizontal className="w-3.5 h-3.5" />
            <span>Semáforo:</span>
          </span>

          <button
            type="button"
            onClick={() => setFilterTraffic('todos')}
            className={`px-3 py-1.5 rounded-xl font-bold transition cursor-pointer ${
              filterTraffic === 'todos'
                ? 'bg-[#FFD100] text-[#2E2E2E]'
                : 'bg-slate-100 text-[#515151] hover:bg-slate-200'
            }`}
          >
            Todos ({pedidos.length})
          </button>

          <button
            type="button"
            onClick={() => setFilterTraffic('verde')}
            className={`px-3 py-1.5 rounded-xl font-bold flex items-center gap-1.5 transition cursor-pointer ${
              filterTraffic === 'verde'
                ? 'bg-[#16A34A] text-white shadow-xs'
                : 'bg-emerald-50 text-[#16A34A] hover:bg-emerald-100'
            }`}
          >
            <CheckCircle2 className="w-3.5 h-3.5" />
            <span>A tiempo</span>
          </button>

          <button
            type="button"
            onClick={() => setFilterTraffic('naranja')}
            className={`px-3 py-1.5 rounded-xl font-bold flex items-center gap-1.5 transition cursor-pointer ${
              filterTraffic === 'naranja'
                ? 'bg-[#EA580C] text-white shadow-xs'
                : 'bg-orange-50 text-[#EA580C] hover:bg-orange-100'
            }`}
          >
            <Clock className="w-3.5 h-3.5" />
            <span>Por vencer</span>
          </button>

          <button
            type="button"
            onClick={() => setFilterTraffic('rojo')}
            className={`px-3 py-1.5 rounded-xl font-bold flex items-center gap-1.5 transition cursor-pointer ${
              filterTraffic === 'rojo'
                ? 'bg-[#DC2626] text-white shadow-xs'
                : 'bg-rose-50 text-[#DC2626] hover:bg-rose-100'
            }`}
          >
            <AlertTriangle className="w-3.5 h-3.5" />
            <span>Retrasados</span>
          </button>

          <button
            type="button"
            onClick={() => setFilterTraffic('gris')}
            className={`px-3 py-1.5 rounded-xl font-bold flex items-center gap-1.5 transition cursor-pointer ${
              filterTraffic === 'gris'
                ? 'bg-[#6C6B6D] text-white shadow-xs'
                : 'bg-slate-100 text-[#6C6B6D] hover:bg-slate-200'
            }`}
          >
            <PauseCircle className="w-3.5 h-3.5" />
            <span>En pausa / Cancelado</span>
          </button>
        </div>
      </div>

      {/* Orders Table */}
      <div className="bg-white rounded-3xl border border-[#6C6B6D]/20 shadow-xs overflow-hidden">
        <div className="overflow-x-auto">
          <table className="w-full text-left text-xs text-[#515151]">
            <thead className="bg-[#FFFBEA] border-b border-[#FFD100]/40 text-[11px] font-black uppercase text-[#2E2E2E]">
              <tr>
                <th className="py-3.5 px-4">Código</th>
                <th className="py-3.5 px-4">Nombre del Producto</th>
                <th className="py-3.5 px-4">Etapa Actual</th>
                <th className="py-3.5 px-4">Estado</th>
                <th className="py-3.5 px-4">Semáforo</th>
                <th className="py-3.5 px-4">Fecha Pedido</th>
                <th className="py-3.5 px-4">Llegada Est. Bodega</th>
                <th className="py-3.5 px-4">Responsable Actual</th>
                <th className="py-3.5 px-4 text-right">Acción</th>
              </tr>
            </thead>
            <tbody className="divide-y divide-slate-100 font-medium">
              {filteredOrders.map(({ pedido, summary }) => {
                const currentStage = pedido.etapas[pedido.etapaActualNumero - 1];
                const def = ETAPAS_BASE_CONFIG[pedido.etapaActualNumero - 1];

                return (
                  <tr
                    key={pedido.id}
                    onClick={() => onSelectPedido(pedido)}
                    className="hover:bg-[#FFFBEA]/40 transition cursor-pointer"
                  >
                    <td className="py-3.5 px-4 font-mono font-black text-[#2E2E2E] whitespace-nowrap">
                      {pedido.codigo}
                    </td>

                    <td className="py-3.5 px-4 font-bold text-[#2E2E2E] max-w-xs">
                      <div className="truncate">{pedido.nombre}</div>
                      {pedido.esProductoNuevo && (
                        <span className="text-[10px] font-bold text-[#EA580C]">
                          +10d Producto Nuevo
                        </span>
                      )}
                    </td>

                    <td className="py-3.5 px-4 whitespace-nowrap">
                      <span className="font-bold text-[#2E2E2E]">
                        {pedido.etapaActualNumero}. {currentStage?.nombre}
                      </span>
                    </td>

                    <td className="py-3.5 px-4 whitespace-nowrap">
                      <span className="px-2.5 py-1 rounded-lg text-[10px] font-bold bg-slate-100 text-[#2E2E2E]">
                        {pedido.subestadoAduana === 'Retenido' ? 'Retenido en aduana' : pedido.estado}
                      </span>
                    </td>

                    <td className="py-3.5 px-4 whitespace-nowrap">
                      <span
                        className={`inline-flex items-center gap-1.5 px-2.5 py-1 rounded-full text-[11px] font-black ${
                          summary.semaforo === 'rojo'
                            ? 'bg-rose-100 text-[#DC2626]'
                            : summary.semaforo === 'naranja'
                            ? 'bg-orange-100 text-[#EA580C]'
                            : summary.semaforo === 'gris'
                            ? 'bg-slate-100 text-[#6C6B6D]'
                            : 'bg-emerald-100 text-[#16A34A]'
                        }`}
                      >
                        {summary.semaforo === 'rojo' && <AlertTriangle className="w-3.5 h-3.5" />}
                        {summary.semaforo === 'naranja' && <Clock className="w-3.5 h-3.5" />}
                        {summary.semaforo === 'gris' && <PauseCircle className="w-3.5 h-3.5" />}
                        {summary.semaforo === 'verde' && <CheckCircle2 className="w-3.5 h-3.5" />}
                        <span>
                          {summary.semaforo === 'rojo'
                            ? 'Retrasado'
                            : summary.semaforo === 'naranja'
                            ? 'Por vencer'
                            : summary.semaforo === 'gris'
                            ? 'Pausado'
                            : 'A tiempo'}
                        </span>
                      </span>
                    </td>

                    <td className="py-3.5 px-4 font-mono whitespace-nowrap">
                      {formatDisplayDate(pedido.fechaPedido)}
                    </td>

                    <td className="py-3.5 px-4 whitespace-nowrap">
                      <strong className="text-[#2E2E2E] block">{summary.etaBodega}</strong>
                      <span className="text-[10px] text-[#6C6B6D]">
                        Bodega {pedido.bodegaDestino}
                      </span>
                    </td>

                    <td className="py-3.5 px-4 text-[#6C6B6D] whitespace-nowrap">
                      {def?.responsableTexto.split('(')[0] || 'Logística'}
                    </td>

                    <td className="py-3.5 px-4 text-right whitespace-nowrap">
                      <button
                        type="button"
                        onClick={(e) => {
                          e.stopPropagation();
                          onSelectPedido(pedido);
                        }}
                        className="px-3 py-1.5 rounded-xl font-black text-xs bg-[#FFD100] hover:bg-[#E6BC00] text-[#2E2E2E] shadow-2xs cursor-pointer"
                      >
                        Ver detalle →
                      </button>
                    </td>
                  </tr>
                );
              })}
            </tbody>
          </table>
        </div>
      </div>

      {/* ==================================================
          MODAL: NUEVO PEDIDO CON VISTA PREVIA DE CRONOGRAMA
          ================================================== */}
      {showNewOrderModal && (
        <div className="fixed inset-0 z-50 flex items-center justify-center p-4 bg-black/60 backdrop-blur-xs overflow-y-auto">
          <div className="bg-white rounded-3xl max-w-2xl w-full p-6 sm:p-8 border border-[#6C6B6D]/30 shadow-2xl space-y-6 my-8">
            <div className="flex items-center justify-between border-b border-slate-100 pb-3">
              <div className="flex items-center gap-2.5">
                <div className="w-9 h-9 rounded-xl bg-[#FFD100] text-[#2E2E2E] flex items-center justify-center font-bold">
                  <Plus className="w-5 h-5 stroke-[3]" />
                </div>
                <div>
                  <h3 className="text-lg font-black text-[#2E2E2E]">Crear Nuevo Pedido de Importación</h3>
                  <p className="text-xs text-[#6C6B6D]">Formulario oficial de solicitud de compra a fábricas</p>
                </div>
              </div>
              <button
                type="button"
                onClick={() => setShowNewOrderModal(false)}
                className="p-1 rounded-lg text-[#6C6B6D] hover:text-[#2E2E2E]"
              >
                <X className="w-5 h-5" />
              </button>
            </div>

            {newOrderError && (
              <div className="p-3.5 rounded-xl bg-rose-50 border border-rose-200 text-xs font-bold text-[#DC2626]">
                ⚠ {newOrderError}
              </div>
            )}

            <form onSubmit={handleCreateOrderSubmit} className="space-y-4 text-xs">
              {/* Auto generated code */}
              <div className="p-3.5 rounded-2xl bg-[#FFFBEA] border border-[#FFD100] flex items-center justify-between">
                <div>
                  <span className="text-[10px] uppercase font-bold text-[#6C6B6D] block">
                    Código automático generado:
                  </span>
                  <span className="font-mono text-sm font-black text-[#2E2E2E]">
                    {previewCode}
                  </span>
                </div>
                <span className="text-[10px] font-bold text-[#515151]">Formato IMP-AAAA-NNN-PRODUCTO</span>
              </div>

              <div>
                <label className="block font-bold text-[#2E2E2E] mb-1">
                  Nombre del producto / carga: *
                </label>
                <input
                  type="text"
                  required
                  placeholder="Ej: Porcelanato Pulido Carrara 60x120"
                  value={newProductName}
                  onChange={(e) => setNewProductName(e.target.value)}
                  className="w-full p-2.5 rounded-xl border border-slate-300 text-xs text-[#2E2E2E] focus:ring-2 focus:ring-[#FFD100]"
                />
              </div>

              <div className="grid grid-cols-1 sm:grid-cols-2 gap-3">
                <div>
                  <label className="block font-bold text-[#2E2E2E] mb-1">
                    Fecha del pedido: *
                  </label>
                  <input
                    type="date"
                    required
                    value={newOrderDate}
                    onChange={(e) => setNewOrderDate(e.target.value)}
                    className="w-full p-2.5 rounded-xl border border-slate-300 text-xs text-[#2E2E2E] font-mono"
                  />
                </div>

                <div>
                  <label className="block font-bold text-[#2E2E2E] mb-1">
                    Bodega de destino: *
                  </label>
                  <select
                    value={newWarehouse}
                    onChange={(e) => setNewWarehouse(e.target.value as BodegaDestino)}
                    className="w-full p-2.5 rounded-xl border border-slate-300 text-xs font-bold text-[#2E2E2E]"
                  >
                    <option value="Chongón">Chongón (Matriz Guayaquil)</option>
                    <option value="Quito">Quito</option>
                    <option value="Ibarra">Ibarra</option>
                    <option value="Cuenca">Cuenca</option>
                    <option value="Manta">Manta</option>
                  </select>
                </div>
              </div>

              {/* Checkbox: Producto nuevo */}
              <label className="flex items-start gap-3 p-3.5 rounded-2xl border border-slate-200 bg-slate-50 cursor-pointer">
                <input
                  type="checkbox"
                  checked={newIsNewProduct}
                  onChange={(e) => setNewIsNewProduct(e.target.checked)}
                  className="mt-0.5 w-4 h-4 rounded text-[#FFD100] focus:ring-[#FFD100]"
                />
                <div>
                  <span className="font-black text-[#2E2E2E] block">
                    ¿Incluye producto nuevo en catálogo?
                  </span>
                  <span className="text-[11px] text-[#6C6B6D]">
                    Suma automáticamente +10 días hábiles a la etapa de Revisión y check list para validación técnica.
                  </span>
                </div>
              </label>

              {/* Required Uploads */}
              <div className="grid grid-cols-1 sm:grid-cols-2 gap-3 pt-2">
                <div className="p-3.5 rounded-2xl border border-dashed border-[#6C6B6D]/40 bg-slate-50 text-center space-y-1">
                  <span className="font-bold text-[#2E2E2E] block">Excel del pedido / proforma *</span>
                  <input
                    type="file"
                    accept=".xlsx,.xls"
                    required
                    onChange={(e) => setNewExcelFile(e.target.files?.[0] || null)}
                    className="text-[11px] text-[#6C6B6D]"
                  />
                  {newExcelFile && (
                    <span className="block text-[11px] text-[#16A34A] font-bold">
                      ✓ {newExcelFile.name}
                    </span>
                  )}
                </div>

                <div className="p-3.5 rounded-2xl border border-dashed border-[#6C6B6D]/40 bg-slate-50 text-center space-y-1">
                  <span className="font-bold text-[#2E2E2E] block">Zip de etiquetas de códigos *</span>
                  <input
                    type="file"
                    accept=".zip,.rar"
                    required
                    onChange={(e) => setNewZipFile(e.target.files?.[0] || null)}
                    className="text-[11px] text-[#6C6B6D]"
                  />
                  {newZipFile && (
                    <span className="block text-[11px] text-[#16A34A] font-bold">
                      ✓ {newZipFile.name}
                    </span>
                  )}
                </div>
              </div>

              {/* Vista previa del cronograma calculado */}
              <div className="p-4 rounded-2xl bg-[#FFFBEA] border border-[#FFD100]/40 space-y-2">
                <div className="flex items-center justify-between font-black text-[#2E2E2E]">
                  <span>Vista previa de llegada estimada a bodega:</span>
                  <span className="text-sm">
                    {formatDisplayDate(previewSchedule[9]?.fechaFinEstimada)}
                  </span>
                </div>
                <p className="text-[11px] text-[#6C6B6D]">
                  Cronograma calculado automáticamente con 11 etapas encadenadas en días hábiles.
                </p>
              </div>

              {/* Action buttons */}
              <div className="flex justify-end gap-2 pt-3 border-t border-slate-100">
                <button
                  type="button"
                  onClick={() => setShowNewOrderModal(false)}
                  className="px-4 py-2 rounded-xl text-xs font-bold text-[#6C6B6D] hover:bg-slate-100 cursor-pointer"
                >
                  Cancelar
                </button>
                <button
                  type="submit"
                  className="px-6 py-2.5 rounded-xl font-black text-xs bg-[#FFD100] hover:bg-[#E6BC00] text-[#2E2E2E] shadow-sm cursor-pointer"
                >
                  Guardar y Crear Pedido
                </button>
              </div>
            </form>
          </div>
        </div>
      )}
    </div>
  );
};

import React, { useState } from 'react';
import {
  Pedido,
  Usuario,
  SubestadoAduana,
  BodegaDestino,
} from '../types';
import { formatDisplayDate, SIMULATED_TODAY } from '../utils/dateUtils';
import { calculateOrderSummary } from '../utils/orderState';
import { HorizontalStatusLine } from './HorizontalStatusLine';
import { StagePanel } from './StagePanel';
import { OrderProductsTab } from './OrderProductsTab';
import { OrderDocumentsTab } from './OrderDocumentsTab';
import { OrderScheduleTab } from './OrderScheduleTab';
import { OrderHistoryTab } from './OrderHistoryTab';
import {
  ArrowLeft,
  Calendar,
  MapPin,
  Clock,
  Layers,
  PauseCircle,
  PlayCircle,
  XCircle,
  Edit3,
  AlertTriangle,
  Package,
  FileText,
  History,
  CheckCircle2,
  Copy,
} from 'lucide-react';

interface OrderDetailProps {
  pedido: Pedido;
  currentUser: Usuario;
  onBackToOrders: () => void;
  onAdvanceStage: (observaciones?: string) => void;
  onReturnStage: (motivo: string) => void;
  onRevertStage: (motivo: string) => void;
  onExtendFabrication: (dias: number, motivo: string) => void;
  onUpdateCustomsSubstate: (subestado: SubestadoAduana, motivo?: string) => void;
  onOrderStatusChange: (action: 'pausar' | 'reanudar' | 'cancelar' | 'reabrir' | 'aprobar_fabricacion', motivo?: string) => void;
  onToggleChecklist: (stageNum: number, checkId: string) => void;
  onAddObservation: (stageNum: number, texto: string) => void;
  onUploadFile: (stageNum: number, docId: string, fileData: { nombre: string; tamano: string; driveUrl?: string; esImagen?: boolean }) => void;
  onUpdateProductQuantity: (lineId: string, recibida: number, incidencia?: string) => void;
  onEditDatesManually: (nuevaFecha: string, motivo: string) => void;
}

export const OrderDetail: React.FC<OrderDetailProps> = ({
  pedido,
  currentUser,
  onBackToOrders,
  onAdvanceStage,
  onReturnStage,
  onRevertStage,
  onExtendFabrication,
  onUpdateCustomsSubstate,
  onOrderStatusChange,
  onToggleChecklist,
  onAddObservation,
  onUploadFile,
  onUpdateProductQuantity,
  onEditDatesManually,
}) => {
  const summary = calculateOrderSummary(pedido);
  const [selectedStageNum, setSelectedStageNum] = useState<number>(pedido.etapaActualNumero);
  const [activeTab, setActiveTab] = useState<'etapa' | 'productos' | 'documentos' | 'cronograma' | 'historial'>('etapa');

  // Modals
  const [showEditDatesModal, setShowEditDatesModal] = useState(false);
  const [showPauseModal, setShowPauseModal] = useState(false);
  const [showCancelModal, setShowCancelModal] = useState(false);
  const [showReopenModal, setShowReopenModal] = useState(false);
  const [motiveInput, setMotiveInput] = useState('');
  const [newDateInput, setNewDateInput] = useState(pedido.fechaPedido);

  const isJuntaOrAdmin = currentUser.rol === 'junta' || currentUser.rol === 'admin';
  const isAdmin = currentUser.rol === 'admin';

  return (
    <div className="space-y-6 animate-in fade-in duration-200">
      {/* Breadcrumbs */}
      <nav className="flex items-center gap-2 text-xs font-bold text-[#6C6B6D]">
        <button
          type="button"
          onClick={onBackToOrders}
          className="hover:text-[#2E2E2E] flex items-center gap-1 cursor-pointer"
        >
          <ArrowLeft className="w-3.5 h-3.5" />
          <span>Inicio</span>
        </button>
        <span>›</span>
        <button
          type="button"
          onClick={onBackToOrders}
          className="hover:text-[#2E2E2E] cursor-pointer"
        >
          Pedidos
        </button>
        <span>›</span>
        <span className="text-[#2E2E2E] font-black">{pedido.codigo}</span>
      </nav>

      {/* Main Order Header Card */}
      <div className="bg-white rounded-3xl border border-[#6C6B6D]/20 shadow-xs p-6 sm:p-8 space-y-6">
        <div className="flex flex-col lg:flex-row lg:items-center justify-between gap-4">
          <div>
            <div className="flex items-center gap-2 flex-wrap">
              <span className="font-mono text-sm sm:text-base font-black px-3 py-1 rounded-xl bg-[#FFD100] text-[#2E2E2E]">
                {pedido.codigo}
              </span>
              <span className="text-xs font-black px-2.5 py-0.5 rounded-lg bg-black/5 text-[#515151]">
                Versión {pedido.version}
              </span>
              {pedido.esProductoNuevo && (
                <span className="text-xs font-black px-2.5 py-0.5 rounded-lg bg-[#FFFBEA] border border-[#FFD100] text-[#2E2E2E]">
                  +10d Producto Nuevo
                </span>
              )}
            </div>

            <h1 className="text-2xl sm:text-3xl font-black text-[#2E2E2E] mt-2 tracking-tight">
              {pedido.nombre}
            </h1>

            {pedido.descripcion && (
              <p className="text-xs sm:text-sm text-[#515151] font-medium mt-1 max-w-3xl">
                {pedido.descripcion}
              </p>
            )}
          </div>

          {/* Status badge & top action buttons */}
          <div className="flex flex-col items-start lg:items-end gap-3 shrink-0">
            {/* Semáforo badge: ALWAYS icon + text */}
            <div className="flex items-center gap-2">
              <span
                className={`inline-flex items-center gap-1.5 px-3.5 py-1.5 rounded-xl text-xs font-black ${
                  summary.semaforo === 'rojo'
                    ? 'bg-rose-100 text-[#DC2626]'
                    : summary.semaforo === 'naranja'
                    ? 'bg-orange-100 text-[#EA580C]'
                    : summary.semaforo === 'gris'
                    ? 'bg-slate-100 text-[#6C6B6D]'
                    : 'bg-emerald-100 text-[#16A34A]'
                }`}
              >
                {summary.semaforo === 'rojo' && <AlertTriangle className="w-4 h-4 stroke-[2.5]" />}
                {summary.semaforo === 'naranja' && <Clock className="w-4 h-4 stroke-[2.5]" />}
                {summary.semaforo === 'gris' && <PauseCircle className="w-4 h-4 stroke-[2.5]" />}
                {summary.semaforo === 'verde' && <CheckCircle2 className="w-4 h-4 stroke-[2.5]" />}
                <span>
                  {summary.semaforo === 'rojo'
                    ? 'Retrasado'
                    : summary.semaforo === 'naranja'
                    ? 'Por vencer'
                    : summary.semaforo === 'gris'
                    ? pedido.estado === 'Cancelado' ? 'Cancelado' : 'En pausa'
                    : 'A tiempo'}
                </span>
              </span>

              {/* Order State Pill */}
              <span className="px-3.5 py-1.5 rounded-xl text-xs font-bold bg-slate-100 text-[#2E2E2E]">
                {pedido.subestadoAduana === 'Retenido' ? 'Retenido en aduana' : pedido.estado}
              </span>
            </div>

            {/* Role Action Buttons (Pausar, Cancelar, Reabrir, Editar fechas) */}
            <div className="flex items-center gap-2 flex-wrap">
              {isAdmin && (
                <button
                  type="button"
                  onClick={() => {
                    setNewDateInput(pedido.fechaPedido);
                    setMotiveInput('');
                    setShowEditDatesModal(true);
                  }}
                  className="px-3 py-1.5 rounded-xl text-xs font-bold bg-white border border-[#6C6B6D]/30 text-[#515151] hover:bg-slate-50 flex items-center gap-1.5 cursor-pointer shadow-xs"
                >
                  <Edit3 className="w-3.5 h-3.5" />
                  <span>Editar fechas</span>
                </button>
              )}

              {isJuntaOrAdmin && pedido.estado !== 'Cancelado' && pedido.estado !== 'Finalizado' && (
                <>
                  {pedido.estado === 'En pausa' ? (
                    <button
                      type="button"
                      onClick={() => {
                        setMotiveInput('');
                        setShowReopenModal(true);
                      }}
                      className="px-3 py-1.5 rounded-xl text-xs font-black bg-[#FFD100] hover:bg-[#E6BC00] text-[#2E2E2E] flex items-center gap-1.5 cursor-pointer shadow-xs"
                    >
                      <PlayCircle className="w-3.5 h-3.5" />
                      <span>Reanudar pedido</span>
                    </button>
                  ) : (
                    <button
                      type="button"
                      onClick={() => {
                        setMotiveInput('');
                        setShowPauseModal(true);
                      }}
                      className="px-3 py-1.5 rounded-xl text-xs font-bold bg-slate-100 hover:bg-slate-200 text-[#515151] flex items-center gap-1.5 cursor-pointer"
                    >
                      <PauseCircle className="w-3.5 h-3.5" />
                      <span>Pausar</span>
                    </button>
                  )}

                  <button
                    type="button"
                    onClick={() => {
                      setMotiveInput('');
                      setShowCancelModal(true);
                    }}
                    className="px-3 py-1.5 rounded-xl text-xs font-bold bg-rose-50 hover:bg-rose-100 text-[#DC2626] border border-rose-200 flex items-center gap-1.5 cursor-pointer"
                  >
                    <XCircle className="w-3.5 h-3.5" />
                    <span>Cancelar</span>
                  </button>
                </>
              )}
            </div>
          </div>
        </div>

        {/* Quick info row */}
        <div className="grid grid-cols-2 sm:grid-cols-4 gap-3 p-4 rounded-2xl bg-[#FFFBEA] border border-[#FFD100]/40 text-xs">
          <div>
            <span className="text-[#6C6B6D] block font-semibold">Fecha del pedido:</span>
            <strong className="text-[#2E2E2E] font-black text-sm">
              {formatDisplayDate(pedido.fechaPedido)}
            </strong>
          </div>
          <div>
            <span className="text-[#6C6B6D] block font-semibold">Llegada est. a bodega:</span>
            <strong className="text-[#2E2E2E] font-black text-sm">
              {summary.etaBodega}
            </strong>
          </div>
          <div>
            <span className="text-[#6C6B6D] block font-semibold">Bodega de destino:</span>
            <strong className="text-[#2E2E2E] font-black text-sm flex items-center gap-1">
              <MapPin className="w-3.5 h-3.5 text-[#515151]" />
              {pedido.bodegaDestino}
            </strong>
          </div>
          <div>
            <span className="text-[#6C6B6D] block font-semibold">Proveedor en China:</span>
            <strong className="text-[#2E2E2E] font-bold text-xs truncate block" title={pedido.proveedor}>
              {pedido.proveedor || 'No especificado'}
            </strong>
          </div>
        </div>
      </div>

      {/* ==================================================
          LÍNEA DE ESTADOS HORIZONTAL
          ================================================== */}
      <HorizontalStatusLine
        pedido={pedido}
        selectedStageNum={selectedStageNum}
        onSelectStage={(num) => {
          setSelectedStageNum(num);
          setActiveTab('etapa');
        }}
      />

      {/* ==================================================
          PESTAÑAS DE NAVEGACIÓN
          ================================================== */}
      <div className="flex items-center gap-2 border-b border-slate-200 overflow-x-auto pb-1 text-xs">
        {[
          { id: 'etapa', label: `Etapa ${selectedStageNum}: ${pedido.etapas[selectedStageNum - 1]?.nombre || ''}`, icon: <Layers className="w-4 h-4" /> },
          { id: 'productos', label: 'Productos del pedido', icon: <Package className="w-4 h-4" /> },
          { id: 'documentos', label: 'Documentos', icon: <FileText className="w-4 h-4" /> },
          { id: 'cronograma', label: 'Cronograma', icon: <Calendar className="w-4 h-4" /> },
          { id: 'historial', label: 'Historial', icon: <History className="w-4 h-4" /> },
        ].map((tab) => (
          <button
            key={tab.id}
            type="button"
            onClick={() => setActiveTab(tab.id as any)}
            className={`px-4 py-2.5 rounded-t-2xl font-black flex items-center gap-2 transition cursor-pointer border-t border-x ${
              activeTab === tab.id
                ? 'bg-white border-[#6C6B6D]/20 text-[#2E2E2E] border-b-2 border-b-white -mb-px shadow-2xs'
                : 'bg-[#FFFBEA]/40 border-transparent text-[#6C6B6D] hover:bg-[#FFFBEA] hover:text-[#2E2E2E]'
            }`}
          >
            {tab.icon}
            <span>{tab.label}</span>
          </button>
        ))}
      </div>

      {/* ==================================================
          CONTENIDO DE LA PESTAÑA ACTIVA
          ================================================== */}
      {activeTab === 'etapa' && (
        <StagePanel
          pedido={pedido}
          stageNumber={selectedStageNum}
          currentUser={currentUser}
          onAdvanceStage={onAdvanceStage}
          onReturnStage={onReturnStage}
          onRevertStage={onRevertStage}
          onExtendFabrication={onExtendFabrication}
          onUpdateCustomsSubstate={onUpdateCustomsSubstate}
          onApproveBoardFabrication={() => onOrderStatusChange('aprobar_fabricacion')}
          onRejectBoardFabrication={(motivo) => onOrderStatusChange('cancelar', motivo)}
          onToggleChecklist={(chkId) => onToggleChecklist(selectedStageNum, chkId)}
          onAddObservation={(txt) => onAddObservation(selectedStageNum, txt)}
          onUploadFile={(docId, data) => onUploadFile(selectedStageNum, docId, data)}
        />
      )}

      {activeTab === 'productos' && (
        <OrderProductsTab
          pedido={pedido}
          currentUser={currentUser}
          onUpdateProductQuantity={onUpdateProductQuantity}
        />
      )}

      {activeTab === 'documentos' && (
        <OrderDocumentsTab pedido={pedido} currentUser={currentUser} />
      )}

      {activeTab === 'cronograma' && (
        <OrderScheduleTab pedido={pedido} />
      )}

      {activeTab === 'historial' && (
        <OrderHistoryTab pedido={pedido} />
      )}

      {/* ==================================================
          MODALES DE ADMINISTRACIÓN
          ================================================== */}

      {/* Modal: Editar Fechas Manualmente (Con Advertencia) */}
      {showEditDatesModal && (
        <div className="fixed inset-0 z-50 flex items-center justify-center p-4 bg-black/50 backdrop-blur-xs">
          <div className="bg-white rounded-3xl p-6 sm:p-7 max-w-md w-full shadow-2xl border border-amber-300 space-y-4 animate-in fade-in">
            <div className="flex items-center gap-3 text-[#2E2E2E]">
              <div className="w-10 h-10 rounded-2xl bg-amber-100 text-[#EA580C] flex items-center justify-center font-bold">
                <AlertTriangle className="w-5 h-5" />
              </div>
              <h4 className="text-lg font-black">Modificar Fecha Base del Pedido</h4>
            </div>

            {/* ADVERTENCIA EXIGIDA EN REGLA 3 */}
            <div className="p-3.5 rounded-2xl bg-amber-50 border border-amber-200 text-xs font-bold text-amber-900 leading-snug">
              ⚠ Advertencia: Modificar fechas puede generar retrasos en ventas, pérdidas económicas y quiebre de stock.
            </div>

            <div>
              <label className="block text-xs font-bold text-[#2E2E2E] mb-1">
                Nueva fecha del pedido (YYYY-MM-DD):
              </label>
              <input
                type="date"
                required
                value={newDateInput}
                onChange={(e) => setNewDateInput(e.target.value)}
                className="w-full p-2.5 rounded-xl border border-slate-300 text-xs text-[#2E2E2E] font-mono"
              />
            </div>

            <div>
              <label className="block text-xs font-bold text-[#2E2E2E] mb-1">
                Motivo justificado del cambio:
              </label>
              <textarea
                rows={2}
                required
                placeholder="Indica la razón de fuerza mayor o autorización..."
                value={motiveInput}
                onChange={(e) => setMotiveInput(e.target.value)}
                className="w-full p-2.5 rounded-xl border border-slate-300 text-xs text-[#2E2E2E]"
              />
            </div>

            <div className="flex justify-end gap-2 pt-2">
              <button
                type="button"
                onClick={() => setShowEditDatesModal(false)}
                className="px-4 py-2 rounded-xl text-xs font-bold text-[#6C6B6D] hover:bg-slate-100 cursor-pointer"
              >
                Cancelar
              </button>
              <button
                type="button"
                disabled={!motiveInput.trim()}
                onClick={() => {
                  onEditDatesManually(newDateInput, motiveInput.trim());
                  setShowEditDatesModal(false);
                }}
                className="px-5 py-2.5 rounded-xl text-xs font-black bg-[#FFD100] hover:bg-[#E6BC00] text-[#2E2E2E] cursor-pointer"
              >
                Guardar y Recalcular
              </button>
            </div>
          </div>
        </div>
      )}

      {/* Modal: Pausar Pedido */}
      {showPauseModal && (
        <div className="fixed inset-0 z-50 flex items-center justify-center p-4 bg-black/50 backdrop-blur-xs">
          <div className="bg-white rounded-3xl p-6 sm:p-7 max-w-md w-full shadow-2xl border border-slate-200 space-y-4 animate-in fade-in">
            <h4 className="text-lg font-black text-[#2E2E2E]">Pausar Pedido</h4>
            <p className="text-xs text-[#515151]">
              Ingresa el motivo obligatorio para registrar la pausa en el historial:
            </p>
            <textarea
              rows={3}
              required
              placeholder="Ej: Junta solicita revisar cantidades..."
              value={motiveInput}
              onChange={(e) => setMotiveInput(e.target.value)}
              className="w-full p-2.5 rounded-xl border border-slate-300 text-xs text-[#2E2E2E]"
            />
            <div className="flex justify-end gap-2 pt-2">
              <button
                type="button"
                onClick={() => setShowPauseModal(false)}
                className="px-4 py-2 rounded-xl text-xs font-bold text-[#6C6B6D] hover:bg-slate-100 cursor-pointer"
              >
                Cancelar
              </button>
              <button
                type="button"
                disabled={!motiveInput.trim()}
                onClick={() => {
                  onOrderStatusChange('pausar', motiveInput.trim());
                  setShowPauseModal(false);
                }}
                className="px-5 py-2.5 rounded-xl text-xs font-black bg-[#FFD100] hover:bg-[#E6BC00] text-[#2E2E2E] cursor-pointer"
              >
                Confirmar Pausa
              </button>
            </div>
          </div>
        </div>
      )}

      {/* Modal: Cancelar Pedido */}
      {showCancelModal && (
        <div className="fixed inset-0 z-50 flex items-center justify-center p-4 bg-black/50 backdrop-blur-xs">
          <div className="bg-white rounded-3xl p-6 sm:p-7 max-w-md w-full shadow-2xl border border-rose-200 space-y-4 animate-in fade-in">
            <h4 className="text-lg font-black text-[#DC2626]">Cancelar Pedido</h4>
            <p className="text-xs text-[#515151]">
              Esta acción cancelará definitivamente el pedido. Ingresa el motivo:
            </p>
            <textarea
              rows={3}
              required
              placeholder="Motivo formal de cancelación..."
              value={motiveInput}
              onChange={(e) => setMotiveInput(e.target.value)}
              className="w-full p-2.5 rounded-xl border border-rose-300 text-xs text-[#2E2E2E]"
            />
            <div className="flex justify-end gap-2 pt-2">
              <button
                type="button"
                onClick={() => setShowCancelModal(false)}
                className="px-4 py-2 rounded-xl text-xs font-bold text-[#6C6B6D] hover:bg-slate-100 cursor-pointer"
              >
                Regresar
              </button>
              <button
                type="button"
                disabled={!motiveInput.trim()}
                onClick={() => {
                  onOrderStatusChange('cancelar', motiveInput.trim());
                  setShowCancelModal(false);
                }}
                className="px-5 py-2.5 rounded-xl text-xs font-black bg-[#DC2626] text-white cursor-pointer"
              >
                Confirmar Cancelación
              </button>
            </div>
          </div>
        </div>
      )}

      {/* Modal: Reabrir / Reanudar */}
      {showReopenModal && (
        <div className="fixed inset-0 z-50 flex items-center justify-center p-4 bg-black/50 backdrop-blur-xs">
          <div className="bg-white rounded-3xl p-6 sm:p-7 max-w-md w-full shadow-2xl border border-[#FFD100] space-y-4 animate-in fade-in">
            <h4 className="text-lg font-black text-[#2E2E2E]">Reanudar Pedido</h4>
            <p className="text-xs text-[#515151]">
              El pedido continuará su curso en la etapa actual. ¿Confirmar reanudación?
            </p>
            <textarea
              rows={2}
              placeholder="Observación opcional al reanudar..."
              value={motiveInput}
              onChange={(e) => setMotiveInput(e.target.value)}
              className="w-full p-2.5 rounded-xl border border-slate-300 text-xs text-[#2E2E2E]"
            />
            <div className="flex justify-end gap-2 pt-2">
              <button
                type="button"
                onClick={() => setShowReopenModal(false)}
                className="px-4 py-2 rounded-xl text-xs font-bold text-[#6C6B6D] hover:bg-slate-100 cursor-pointer"
              >
                Cancelar
              </button>
              <button
                type="button"
                onClick={() => {
                  onOrderStatusChange('reanudar', motiveInput.trim() || undefined);
                  setShowReopenModal(false);
                }}
                className="px-5 py-2.5 rounded-xl text-xs font-black bg-[#FFD100] hover:bg-[#E6BC00] text-[#2E2E2E] cursor-pointer"
              >
                Reanudar Pedido
              </button>
            </div>
          </div>
        </div>
      )}
    </div>
  );
};

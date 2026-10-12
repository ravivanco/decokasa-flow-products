import React, { useState } from 'react';
import {
  Pedido,
  Usuario,
  BodegaCatalogo,
  UrgenciaNivel,
} from '../types';
import { formatDisplayDate } from '../utils/dateUtils';
import { calculateOrderSummary, getStageDefinitions } from '../utils/orderState';
import { HorizontalStatusLine } from './HorizontalStatusLine';
import { StagePanel } from './StagePanel';
import { OrderProductsTab } from './OrderProductsTab';
import { OrderDocumentsTab } from './OrderDocumentsTab';
import { OrderScheduleTab } from './OrderScheduleTab';
import { OrderHistoryTab } from './OrderHistoryTab';
import {
  ArrowLeft,
  Calendar,
  Clock,
  Layers,
  PauseCircle,
  PlayCircle,
  XCircle,
  Package,
  FileText,
  History,
  CheckCircle2,
  AlertTriangle,
  Warehouse,
  Sparkles,
} from 'lucide-react';

interface OrderDetailProps {
  pedido: Pedido;
  currentUser: Usuario;
  bodegasCatalogo: BodegaCatalogo[];
  onBackToOrders: () => void;
  onAdvanceStage: (observaciones?: string) => void;
  onRejectStage: (motivo: string) => void;
  onReturnWithUrgency: (motivo: string, urgencia: UrgenciaNivel) => void;
  onReportCustomsDelay: (motivo: string, nuevaFecha: string, dias: number) => void;
  onSelectWarehouse: (bodegaNombre: string) => void;
  onOrderStatusChange: (action: 'pausar' | 'reanudar' | 'cancelar', motivo?: string) => void;
  onToggleChecklist: (stageNum: number, checkId: string) => void;
  onAddObservation: (stageNum: number, texto: string) => void;
  onUploadFile: (stageNum: number, docId: string, fileData: { nombre: string; tamano: string }) => void;
  onUpdateProductQuantity: (lineId: string, recibida: number, incidencia?: string) => void;
  onShowToast: (msg: string) => void;
}

export const OrderDetail: React.FC<OrderDetailProps> = ({
  pedido,
  currentUser,
  bodegasCatalogo,
  onBackToOrders,
  onAdvanceStage,
  onRejectStage,
  onReturnWithUrgency,
  onReportCustomsDelay,
  onSelectWarehouse,
  onOrderStatusChange,
  onToggleChecklist,
  onAddObservation,
  onUploadFile,
  onUpdateProductQuantity,
  onShowToast,
}) => {
  const summary = calculateOrderSummary(pedido);
  const defs = getStageDefinitions(pedido.flujo);
  const [selectedStageNum, setSelectedStageNum] = useState<number>(pedido.etapaActualNumero);
  const [activeTab, setActiveTab] = useState<'etapa' | 'productos' | 'documentos' | 'cronograma' | 'historial'>('etapa');

  // Modales de control administrativo
  const [showPauseModal, setShowPauseModal] = useState(false);
  const [showCancelModal, setShowCancelModal] = useState(false);
  const [showReopenModal, setShowReopenModal] = useState(false);
  const [motiveInput, setMotiveInput] = useState('');

  const isAdmin = currentUser.rol === 'admin';

  return (
    <div className="space-y-6 sm:space-y-8 animate-in fade-in duration-200">
      {/* Botón Volver y Header del Pedido */}
      <div className="flex flex-col sm:flex-row sm:items-center justify-between gap-4">
        <div className="flex items-center gap-3">
          <button
            type="button"
            onClick={onBackToOrders}
            className="p-2.5 rounded-2xl bg-white border border-[#6C6B6D]/30 hover:bg-slate-50 text-[#2E2E2E] shadow-2xs transition cursor-pointer shrink-0"
            title="Volver al listado de pedidos"
          >
            <ArrowLeft className="w-4 h-4 stroke-[2.5]" />
          </button>

          <div>
            <div className="flex items-center gap-2 flex-wrap">
              <span className="font-mono text-sm font-black text-[#2E2E2E]">
                {pedido.codigo}
              </span>
              <span className="px-2 py-0.5 rounded-md text-[10px] font-black uppercase bg-slate-100 text-[#515151]">
                {pedido.flujo === 'A' ? 'Flujo A · Pedido Ecuador' : 'Flujo B · Propuesto por China'}
              </span>
              <span
                className={`px-2 py-0.5 rounded-full text-[10px] font-black uppercase ${
                  pedido.estado === 'Finalizado'
                    ? 'bg-emerald-100 text-emerald-800'
                    : pedido.estado === 'Devuelto a codificación'
                    ? 'bg-amber-100 text-amber-800'
                    : pedido.estado === 'En pausa'
                    ? 'bg-slate-200 text-[#515151]'
                    : 'bg-[#FFFBEA] text-[#2E2E2E] border border-[#FFD100]'
                }`}
              >
                {pedido.estado}
              </span>
              {pedido.urgenciaDevolucion && (
                <span className="px-2 py-0.5 rounded-md text-[10px] font-black uppercase bg-red-600 text-white animate-pulse">
                  ⚡ Urgencia {pedido.urgenciaDevolucion.horasMaximas} h
                </span>
              )}
            </div>

            <h1 className="text-xl sm:text-2xl font-black text-[#2E2E2E] mt-0.5">
              {pedido.nombre}
            </h1>
          </div>
        </div>

        {/* Acciones de Admin (Pausar / Reanudar / Cancelar) */}
        {isAdmin && (
          <div className="flex items-center gap-2 self-start sm:self-auto">
            {pedido.estado === 'En pausa' ? (
              <button
                type="button"
                onClick={() => setShowReopenModal(true)}
                className="px-3 py-1.5 rounded-xl bg-emerald-600 hover:bg-emerald-700 text-white font-bold text-xs transition flex items-center gap-1.5 shadow-2xs cursor-pointer"
              >
                <PlayCircle className="w-3.5 h-3.5" />
                <span>Reanudar Pedido</span>
              </button>
            ) : pedido.estado !== 'Finalizado' && pedido.estado !== 'Cancelado' ? (
              <>
                <button
                  type="button"
                  onClick={() => setShowPauseModal(true)}
                  className="px-3 py-1.5 rounded-xl bg-white border border-[#6C6B6D]/30 hover:bg-slate-50 text-[#515151] font-bold text-xs transition flex items-center gap-1.5 shadow-2xs cursor-pointer"
                >
                  <PauseCircle className="w-3.5 h-3.5" />
                  <span>Pausar</span>
                </button>
                <button
                  type="button"
                  onClick={() => setShowCancelModal(true)}
                  className="px-3 py-1.5 rounded-xl bg-white border border-rose-300 hover:bg-rose-50 text-rose-700 font-bold text-xs transition flex items-center gap-1.5 shadow-2xs cursor-pointer"
                >
                  <XCircle className="w-3.5 h-3.5" />
                  <span>Cancelar</span>
                </button>
              </>
            ) : null}
          </div>
        )}
      </div>

      {/* Franja de Datos Clave estilo Courier */}
      <div className="bg-white rounded-3xl p-5 border border-[#6C6B6D]/20 shadow-xs grid grid-cols-2 md:grid-cols-4 gap-4">
        <div>
          <span className="text-[10px] font-black uppercase text-[#6C6B6D] block">
            Fecha de Pedido
          </span>
          <span className="font-mono text-xs sm:text-sm font-black text-[#2E2E2E]">
            {formatDisplayDate(pedido.fechaPedido)}
          </span>
          <span className="text-[10px] text-[#6C6B6D] block mt-0.5">
            Por {pedido.creadoPor}
          </span>
        </div>

        <div>
          <span className="text-[10px] font-black uppercase text-[#6C6B6D] block">
            Llegada a Bodega
          </span>
          <span className="font-mono text-xs sm:text-sm font-black text-[#2E2E2E]">
            {summary.etaBodega}
          </span>
          <span
            className={`text-[10px] font-black block mt-0.5 ${
              summary.semaforo === 'rojo'
                ? 'text-[#DC2626]'
                : summary.semaforo === 'naranja'
                ? 'text-[#EA580C]'
                : 'text-[#16A34A]'
            }`}
          >
            {summary.aviso}
          </span>
        </div>

        <div>
          <span className="text-[10px] font-black uppercase text-[#6C6B6D] block">
            Bodega de Destino
          </span>
          <span className="text-xs sm:text-sm font-black text-[#2E2E2E] truncate block">
            {pedido.bodegaDestino || 'Por asignar en etapa Bodega'}
          </span>
          <span className="text-[10px] text-[#6C6B6D] block mt-0.5">
            Responsable: Anderson Enriquez
          </span>
        </div>

        <div>
          <span className="text-[10px] font-black uppercase text-[#6C6B6D] block">
            Avance del Flujo
          </span>
          <span className="font-mono text-xs sm:text-sm font-black text-[#2E2E2E]">
            {summary.progresoPorcentaje}% completado
          </span>
          <span className="text-[10px] text-[#6C6B6D] block mt-0.5">
            Etapa {pedido.etapaActualNumero} de {defs.length}
          </span>
        </div>
      </div>

      {/* Línea Horizontal Interactiva de Estados */}
      <HorizontalStatusLine
        pedido={pedido}
        selectedStageNum={selectedStageNum}
        onSelectStage={(num) => {
          setSelectedStageNum(num);
          setActiveTab('etapa');
        }}
      />

      {/* Tabs de Navegación del Expediente */}
      <div className="flex border-b border-slate-200 gap-2 overflow-x-auto pb-1">
        <button
          type="button"
          onClick={() => setActiveTab('etapa')}
          className={`px-4 py-2.5 rounded-2xl text-xs font-black transition cursor-pointer flex items-center gap-2 shrink-0 ${
            activeTab === 'etapa'
              ? 'bg-[#FFD100] text-[#2E2E2E] shadow-2xs'
              : 'text-[#515151] hover:bg-slate-100'
          }`}
        >
          <Layers className="w-4 h-4" />
          <span>Etapa {selectedStageNum} ({defs[selectedStageNum - 1]?.nombre})</span>
        </button>

        <button
          type="button"
          onClick={() => setActiveTab('productos')}
          className={`px-4 py-2.5 rounded-2xl text-xs font-black transition cursor-pointer flex items-center gap-2 shrink-0 ${
            activeTab === 'productos'
              ? 'bg-[#FFD100] text-[#2E2E2E] shadow-2xs'
              : 'text-[#515151] hover:bg-slate-100'
          }`}
        >
          <Package className="w-4 h-4" />
          <span>Productos ({pedido.productos.length})</span>
        </button>

        <button
          type="button"
          onClick={() => setActiveTab('documentos')}
          className={`px-4 py-2.5 rounded-2xl text-xs font-black transition cursor-pointer flex items-center gap-2 shrink-0 ${
            activeTab === 'documentos'
              ? 'bg-[#FFD100] text-[#2E2E2E] shadow-2xs'
              : 'text-[#515151] hover:bg-slate-100'
          }`}
        >
          <FileText className="w-4 h-4" />
          <span>Google Drive</span>
        </button>

        <button
          type="button"
          onClick={() => setActiveTab('cronograma')}
          className={`px-4 py-2.5 rounded-2xl text-xs font-black transition cursor-pointer flex items-center gap-2 shrink-0 ${
            activeTab === 'cronograma'
              ? 'bg-[#FFD100] text-[#2E2E2E] shadow-2xs'
              : 'text-[#515151] hover:bg-slate-100'
          }`}
        >
          <Calendar className="w-4 h-4" />
          <span>Cronograma & Meta 90d</span>
        </button>

        <button
          type="button"
          onClick={() => setActiveTab('historial')}
          className={`px-4 py-2.5 rounded-2xl text-xs font-black transition cursor-pointer flex items-center gap-2 shrink-0 ${
            activeTab === 'historial'
              ? 'bg-[#FFD100] text-[#2E2E2E] shadow-2xs'
              : 'text-[#515151] hover:bg-slate-100'
          }`}
        >
          <History className="w-4 h-4" />
          <span>Historial de Auditoría</span>
        </button>
      </div>

      {/* Contenido de la Pestaña Activa */}
      {activeTab === 'etapa' && (
        <StagePanel
          pedido={pedido}
          stageNumber={selectedStageNum}
          currentUser={currentUser}
          bodegasCatalogo={bodegasCatalogo}
          onAdvanceStage={onAdvanceStage}
          onRejectStage={onRejectStage}
          onReturnWithUrgency={onReturnWithUrgency}
          onReportCustomsDelay={onReportCustomsDelay}
          onSelectWarehouse={onSelectWarehouse}
          onToggleChecklist={(chkId) => onToggleChecklist(selectedStageNum, chkId)}
          onAddObservation={(txt) => onAddObservation(selectedStageNum, txt)}
          onUploadFile={(docId, fData) => onUploadFile(selectedStageNum, docId, fData)}
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
        <OrderDocumentsTab
          pedido={pedido}
          currentUser={currentUser}
          onShowToast={onShowToast}
        />
      )}

      {activeTab === 'cronograma' && (
        <OrderScheduleTab pedido={pedido} />
      )}

      {activeTab === 'historial' && (
        <OrderHistoryTab pedido={pedido} />
      )}

      {/* Modales de Pausa y Cancelación */}
      {showPauseModal && (
        <div className="fixed inset-0 z-50 bg-black/50 backdrop-blur-xs flex items-center justify-center p-4">
          <div className="bg-white rounded-3xl max-w-md w-full p-6 space-y-4 shadow-2xl animate-in zoom-in-95">
            <h3 className="text-base font-black text-[#2E2E2E] flex items-center gap-2">
              <PauseCircle className="w-5 h-5" />
              <span>Pausar Temporalmente el Pedido</span>
            </h3>
            <p className="text-xs text-[#515151]">
              El pedido detendrá el cómputo de plazos y mostrará estado &quot;En pausa&quot;.
            </p>
            <textarea
              rows={2}
              required
              placeholder="Ingresa el motivo de la pausa..."
              value={motiveInput}
              onChange={(e) => setMotiveInput(e.target.value)}
              className="w-full px-3 py-2 rounded-xl border border-slate-300 text-xs"
            />
            <div className="flex items-center justify-end gap-2 pt-2">
              <button
                type="button"
                onClick={() => setShowPauseModal(false)}
                className="px-4 py-2 rounded-xl bg-slate-100 text-xs font-bold"
              >
                Cancelar
              </button>
              <button
                type="button"
                disabled={!motiveInput.trim()}
                onClick={() => {
                  onOrderStatusChange('pausar', motiveInput);
                  setShowPauseModal(false);
                  setMotiveInput('');
                }}
                className="px-4 py-2 rounded-xl bg-slate-800 text-white font-bold text-xs"
              >
                Confirmar Pausa
              </button>
            </div>
          </div>
        </div>
      )}

      {showCancelModal && (
        <div className="fixed inset-0 z-50 bg-black/50 backdrop-blur-xs flex items-center justify-center p-4">
          <div className="bg-white rounded-3xl max-w-md w-full p-6 space-y-4 shadow-2xl animate-in zoom-in-95">
            <h3 className="text-base font-black text-rose-700 flex items-center gap-2">
              <XCircle className="w-5 h-5" />
              <span>Cancelar Definitivamente el Pedido</span>
            </h3>
            <p className="text-xs text-[#515151]">
              Esta acción no se puede deshacer. Se archivará el pedido en el historial con motivo.
            </p>
            <textarea
              rows={2}
              required
              placeholder="Ingresa el motivo de la cancelación..."
              value={motiveInput}
              onChange={(e) => setMotiveInput(e.target.value)}
              className="w-full px-3 py-2 rounded-xl border border-rose-300 text-xs"
            />
            <div className="flex items-center justify-end gap-2 pt-2">
              <button
                type="button"
                onClick={() => setShowCancelModal(false)}
                className="px-4 py-2 rounded-xl bg-slate-100 text-xs font-bold"
              >
                Volver
              </button>
              <button
                type="button"
                disabled={!motiveInput.trim()}
                onClick={() => {
                  onOrderStatusChange('cancelar', motiveInput);
                  setShowCancelModal(false);
                  setMotiveInput('');
                }}
                className="px-4 py-2 rounded-xl bg-rose-600 text-white font-bold text-xs"
              >
                Confirmar Cancelación
              </button>
            </div>
          </div>
        </div>
      )}

      {showReopenModal && (
        <div className="fixed inset-0 z-50 bg-black/50 backdrop-blur-xs flex items-center justify-center p-4">
          <div className="bg-white rounded-3xl max-w-md w-full p-6 space-y-4 shadow-2xl animate-in zoom-in-95">
            <h3 className="text-base font-black text-emerald-800 flex items-center gap-2">
              <PlayCircle className="w-5 h-5 text-emerald-600" />
              <span>Reanudar Curso del Pedido</span>
            </h3>
            <p className="text-xs text-[#515151]">
              El pedido retomará su etapa actual y se reactivarán los plazos en días hábiles.
            </p>
            <div className="flex items-center justify-end gap-2 pt-2">
              <button
                type="button"
                onClick={() => setShowReopenModal(false)}
                className="px-4 py-2 rounded-xl bg-slate-100 text-xs font-bold"
              >
                Cancelar
              </button>
              <button
                type="button"
                onClick={() => {
                  onOrderStatusChange('reanudar');
                  setShowReopenModal(false);
                }}
                className="px-4 py-2 rounded-xl bg-emerald-600 text-white font-bold text-xs"
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

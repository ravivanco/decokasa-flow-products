import React, { useState } from 'react';
import {
  Pedido,
  EtapaInstancia,
  Usuario,
  UrgenciaNivel,
  BodegaCatalogo,
} from '../types';
import { getStageDefinitions } from '../utils/orderState';
import { canUserOperateStage } from '../utils/permissions';
import { formatDisplayDate } from '../utils/dateUtils';
import {
  CheckSquare,
  Square,
  Upload,
  Clock,
  AlertTriangle,
  Send,
  CheckCircle2,
  X,
  Sparkles,
  Warehouse,
  ShieldCheck,
  FileText,
  RotateCcw,
  CornerUpLeft,
  ChevronDown,
} from 'lucide-react';

interface StagePanelProps {
  pedido: Pedido;
  stageNumber: number;
  currentUser: Usuario;
  bodegasCatalogo: BodegaCatalogo[];
  onAdvanceStage: (observaciones?: string) => void;
  onRejectStage: (motivo: string) => void;
  onReturnWithUrgency: (motivo: string, urgencia: UrgenciaNivel) => void;
  onReportCustomsDelay: (motivo: string, nuevaFecha: string, dias: number) => void;
  onSelectWarehouse: (bodegaNombre: string) => void;
  onToggleChecklist: (checkId: string) => void;
  onAddObservation: (texto: string) => void;
  onUploadFile: (docId: string, fileData: { nombre: string; tamano: string }) => void;
}

export const StagePanel: React.FC<StagePanelProps> = ({
  pedido,
  stageNumber,
  currentUser,
  bodegasCatalogo,
  onAdvanceStage,
  onRejectStage,
  onReturnWithUrgency,
  onReportCustomsDelay,
  onSelectWarehouse,
  onToggleChecklist,
  onAddObservation,
  onUploadFile,
}) => {
  const defs = getStageDefinitions(pedido.flujo);
  const def = defs[stageNumber - 1];
  const stage = pedido.etapas[stageNumber - 1];

  const isCurrentStage = pedido.etapaActualNumero === stageNumber && pedido.estado !== 'Finalizado';
  const isAuthorized = canUserOperateStage(currentUser, stageNumber, pedido.flujo);
  const isAdmin = currentUser.rol === 'admin';

  // Observation state
  const [newObsText, setNewObsText] = useState('');

  // Modals state
  const [showAdvanceModal, setShowAdvanceModal] = useState(false);
  const [advanceNotes, setAdvanceNotes] = useState('');

  const [showRejectModal, setShowRejectModal] = useState(false);
  const [rejectReason, setRejectReason] = useState('');

  const [showUrgencyModal, setShowUrgencyModal] = useState(false);
  const [selectedUrgency, setSelectedUrgency] = useState<UrgenciaNivel>('Urgente');
  const [urgencyReason, setUrgencyReason] = useState('');

  const [showDelayModal, setShowDelayModal] = useState(false);
  const [delayDays, setDelayDays] = useState(5);
  const [delayReason, setDelayReason] = useState('Aforo físico intrusivo de SENAE en Guayaquil.');

  const [selectedBodegaInput, setSelectedBodegaInput] = useState(
    pedido.bodegaDestino || bodegasCatalogo[0]?.nombre || 'Bodega Principal Chongón'
  );

  if (!def || !stage) {
    return (
      <div className="bg-white rounded-3xl p-8 text-center text-xs text-[#6C6B6D]">
        Etapa no encontrada.
      </div>
    );
  }

  // Verificar si faltan checks obligatorios
  const pendingChecklist = stage.checklist.filter((item) => !item.completado);
  const missingDocs = stage.documentos.filter((doc) => doc.esObligatorio && !doc.archivoActual);

  const handlePostObservation = (e: React.FormEvent) => {
    e.preventDefault();
    if (!newObsText.trim()) return;
    onAddObservation(newObsText.trim());
    setNewObsText('');
  };

  return (
    <div className="bg-white rounded-3xl p-6 sm:p-8 border border-[#6C6B6D]/20 shadow-xs space-y-6">
      {/* 1. Header de la etapa */}
      <div className="flex flex-col md:flex-row md:items-center justify-between gap-4 border-b border-slate-100 pb-5">
        <div>
          <div className="flex items-center gap-2 mb-1">
            <span className="font-mono text-xs font-black uppercase px-2 py-0.5 rounded-md bg-slate-100 text-[#515151]">
              Etapa {def.numero} de {defs.length}
            </span>
            <span className="text-xs text-[#6C6B6D]">
              Fase: <strong>{def.fase}</strong>
            </span>
          </div>

          <h2 className="text-xl sm:text-2xl font-black text-[#2E2E2E]">
            {def.nombre}
          </h2>

          <p className="text-xs text-[#515151] mt-1 max-w-2xl leading-relaxed">
            {def.descripcion}
          </p>
        </div>

        {/* SLA & Responsable Badge */}
        <div className="flex flex-wrap sm:flex-col items-start sm:items-end gap-2 shrink-0">
          <div className="px-3.5 py-1.5 rounded-xl bg-slate-50 border border-slate-200 text-xs font-bold text-[#2E2E2E]">
            <span className="text-[#6C6B6D]">Responsable:</span>{' '}
            <strong className="text-[#2E2E2E]">{def.responsableTexto}</strong>
          </div>

          <div className="px-3.5 py-1.5 rounded-xl bg-amber-50 border border-amber-200 text-xs font-bold text-amber-900 flex items-center gap-1.5">
            <Clock className="w-3.5 h-3.5 text-amber-700" />
            <span>Plazo: <strong>{stage.plazoTexto}</strong></span>
          </div>

          <div className="text-[11px] text-[#6C6B6D] font-mono">
            {stage.completada
              ? `✓ Finalizada: ${formatDisplayDate(stage.fechaRealFin)}`
              : `Límite estimado: ${formatDisplayDate(stage.fechaFinEstimada)}`}
          </div>
        </div>
      </div>

      {/* Alerta de Urgencia Especial si está activa */}
      {pedido.urgenciaDevolucion && isCurrentStage && (
        <div className="p-4 rounded-2xl bg-red-50 border-2 border-red-300 text-xs text-red-900 space-y-1">
          <div className="flex items-center gap-2 font-black">
            <AlertTriangle className="w-4 h-4 text-red-600" />
            <span>URGENCIA ASIGNADA DESDE CHINA: {pedido.urgenciaDevolucion.nivel.toUpperCase()} ({pedido.urgenciaDevolucion.horasMaximas} h)</span>
          </div>
          <p className="text-xs font-medium">
            <strong>Motivo:</strong> {pedido.urgenciaDevolucion.motivo}
          </p>
          <p className="text-[11px] text-red-700 font-mono">
            Límite de entrega: {pedido.urgenciaDevolucion.fechaLimite} · Asignado a: {pedido.urgenciaDevolucion.asignadoA}
          </p>
        </div>
      )}

      {/* Notificación si es etapa de Fin automático */}
      {def.esFinAutomatico && (
        <div className="p-4 rounded-2xl bg-emerald-50 border border-emerald-200 text-xs text-emerald-900 flex items-center gap-2.5">
          <CheckCircle2 className="w-5 h-5 text-emerald-600 shrink-0" />
          <span>
            Esta etapa es automática: se marca sola cuando la etapa anterior de Incidencias recibe su check de Anderson Enriquez.
          </span>
        </div>
      )}

      {/* 2. Checklists Requeridos */}
      {def.checklistsBase.length > 0 && (
        <div className="space-y-3">
          <h3 className="text-xs font-black uppercase tracking-wider text-[#515151] flex items-center justify-between">
            <span>Puntos de Control y Check List de la Etapa:</span>
            <span className="text-[11px] font-mono text-[#6C6B6D]">
              {stage.checklist.filter((c) => c.completado).length} de {stage.checklist.length} listos
            </span>
          </h3>

          <div className="space-y-2">
            {stage.checklist.map((item) => (
              <div
                key={item.id}
                onClick={() => (isAuthorized || isAdmin) && onToggleChecklist(item.id)}
                className={`p-3 rounded-2xl border transition flex items-center justify-between gap-3 ${
                  item.completado
                    ? 'bg-slate-50/70 border-slate-200 text-[#2E2E2E]'
                    : 'bg-white border-slate-200 hover:border-[#FFD100]'
                } ${isAuthorized || isAdmin ? 'cursor-pointer' : 'cursor-default'}`}
              >
                <div className="flex items-center gap-3">
                  {item.completado ? (
                    <CheckSquare className="w-5 h-5 text-emerald-600 shrink-0" />
                  ) : (
                    <Square className="w-5 h-5 text-[#6C6B6D] shrink-0" />
                  )}
                  <span className={`text-xs font-bold ${item.completado ? 'line-through text-[#6C6B6D]' : 'text-[#2E2E2E]'}`}>
                    {item.texto}
                  </span>
                </div>

                {item.completado && item.completadoPor && (
                  <span className="text-[10px] text-[#6C6B6D] shrink-0">
                    {item.completadoPor} · {item.fecha}
                  </span>
                )}
              </div>
            ))}
          </div>
        </div>
      )}

      {/* 3. Documentos Google Drive */}
      {stage.documentos.length > 0 && (
        <div className="space-y-3 pt-2 border-t border-slate-100">
          <h3 className="text-xs font-black uppercase tracking-wider text-[#515151] flex items-center justify-between">
            <span>Archivos y Evidencias en Google Drive (Límite 100 MB):</span>
            <span className="text-[11px] text-[#6C6B6D]">Copia en segundo plano vía cuenta de servicio</span>
          </h3>

          <div className="grid grid-cols-1 sm:grid-cols-2 gap-3">
            {stage.documentos.map((doc) => {
              const hasFile = !!doc.archivoActual;
              return (
                <div
                  key={doc.id}
                  className="p-3.5 rounded-2xl border border-slate-200 bg-slate-50/50 space-y-2"
                >
                  <div className="flex items-start justify-between gap-2">
                    <div>
                      <span className="font-bold text-xs text-[#2E2E2E] block">
                        {doc.tipoDoc}
                      </span>
                      {doc.esObligatorio ? (
                        <span className="text-[10px] font-black uppercase text-rose-600">
                          * Obligatorio para dar check
                        </span>
                      ) : (
                        <span className="text-[10px] text-[#6C6B6D]">Opcional</span>
                      )}
                    </div>

                    {hasFile ? (
                      <span className="px-2 py-0.5 rounded-md bg-emerald-100 text-emerald-800 text-[10px] font-black shrink-0">
                        Drive ✓
                      </span>
                    ) : (
                      <span className="px-2 py-0.5 rounded-md bg-amber-100 text-amber-800 text-[10px] font-bold shrink-0">
                        Pendiente
                      </span>
                    )}
                  </div>

                  {hasFile && doc.archivoActual ? (
                    <div className="text-[11px] text-[#515151] space-y-0.5 bg-white p-2 rounded-xl border border-slate-200">
                      <div className="font-mono font-bold truncate">{doc.archivoActual.nombre}</div>
                      <div className="text-[10px] text-[#6C6B6D]">
                        {doc.archivoActual.tamano} · Subido por {doc.archivoActual.subidoPor} ({doc.archivoActual.fecha})
                      </div>
                    </div>
                  ) : (
                    (isAuthorized || isAdmin) && (
                      <button
                        type="button"
                        onClick={() =>
                          onUploadFile(doc.id, {
                            nombre: `${doc.tipoDoc.replace(/[\/\s]/g, '_')}_v1.pdf`,
                            tamano: '8.4 MB',
                          })
                        }
                        className="w-full py-2 px-3 rounded-xl bg-white hover:bg-slate-100 border border-slate-200 text-xs font-bold text-[#2E2E2E] transition flex items-center justify-center gap-1.5 cursor-pointer shadow-2xs"
                      >
                        <Upload className="w-3.5 h-3.5 text-[#6C6B6D]" />
                        <span>Subir a Google Drive (8.4 MB)</span>
                      </button>
                    )
                  )}
                </div>
              );
            })}
          </div>
        </div>
      )}

      {/* 4. Selector de Bodega especial para etapa 9 (A) o 10 (B) */}
      {isCurrentStage &&
        ((pedido.flujo === 'A' && stageNumber === 9) || (pedido.flujo === 'B' && stageNumber === 10)) && (
          <div className="p-4 rounded-2xl bg-amber-50 border border-amber-300 space-y-3">
            <div className="flex items-center gap-2">
              <Warehouse className="w-5 h-5 text-amber-800" />
              <h4 className="text-xs font-black uppercase tracking-wider text-amber-900">
                Selección de Bodega de Destino (Logística Ecuador):
              </h4>
            </div>
            <p className="text-xs text-amber-800">
              Anderson Enriquez debe elegir la bodega de destino autorizada del catálogo:
            </p>
            <div className="flex flex-col sm:flex-row gap-2">
              <select
                value={selectedBodegaInput}
                onChange={(e) => {
                  setSelectedBodegaInput(e.target.value);
                  onSelectWarehouse(e.target.value);
                }}
                className="flex-1 px-3 py-2 rounded-xl border border-amber-300 bg-white text-xs font-bold text-[#2E2E2E] focus:outline-hidden focus:ring-2 focus:ring-[#FFD100] cursor-pointer"
              >
                {bodegasCatalogo.map((b) => (
                  <option key={b.id} value={b.nombre}>
                    {b.nombre} ({b.ciudad})
                  </option>
                ))}
              </select>
              <button
                type="button"
                onClick={() => onSelectWarehouse(selectedBodegaInput)}
                className="px-4 py-2 rounded-xl bg-[#FFD100] hover:bg-[#E6BC00] text-[#2E2E2E] font-black text-xs transition cursor-pointer shrink-0"
              >
                Confirmar Bodega
              </button>
            </div>
          </div>
        )}

      {/* 5. Acciones Operativas Principales */}
      {isCurrentStage && !def.esFinAutomatico && (
        <div className="pt-4 border-t border-slate-100 space-y-3">
          <div className="flex flex-col sm:flex-row sm:items-center justify-between gap-2">
            <h3 className="text-xs font-black uppercase tracking-wider text-[#515151]">
              Acciones de la Etapa:
            </h3>

            {!isAuthorized && !isAdmin && (
              <span className="text-xs text-rose-600 font-bold">
                Solo {def.responsableTexto} o un Administrador puede dar check en esta etapa.
              </span>
            )}
            {isAdmin && !isAuthorized && (
              <span className="text-xs text-purple-700 font-bold">
                Intervención de Administrador: Tienes permiso para actuar o reasignar.
              </span>
            )}
          </div>

          <div className="flex flex-wrap items-center gap-2.5">
            {/* Botón Principal: Dar Check / Aprobar (✓) */}
            <button
              type="button"
              disabled={!isAuthorized && !isAdmin}
              onClick={() => setShowAdvanceModal(true)}
              className="py-3 px-5 rounded-2xl bg-[#FFD100] hover:bg-[#E6BC00] text-[#2E2E2E] font-black text-xs transition shadow-md flex items-center gap-2 cursor-pointer disabled:opacity-50 disabled:cursor-not-allowed"
            >
              <CheckCircle2 className="w-4 h-4 stroke-[2.5]" />
              <span>
                {stageNumber === 3 || (pedido.flujo === 'B' && stageNumber === 4)
                  ? 'Aprobar Revisión (✓)'
                  : stageNumber === 2 && pedido.flujo === 'B'
                  ? 'Aprobar Producto Propuesto (✓)'
                  : stageNumber === 10 || (pedido.flujo === 'B' && stageNumber === 11)
                  ? 'Dar Check Final de Incidencias'
                  : 'Dar Check y Aprobar Etapa (✓)'}
              </span>
            </button>

            {/* Acciones contextuales de Marcela: Rechazar (✗) en Revisión */}
            {(stageNumber === 3 || (pedido.flujo === 'B' && (stageNumber === 2 || stageNumber === 4))) && (
              <button
                type="button"
                disabled={!isAuthorized && !isAdmin}
                onClick={() => setShowRejectModal(true)}
                className="py-3 px-4 rounded-2xl bg-white border border-rose-300 hover:bg-rose-50 text-rose-700 font-black text-xs transition flex items-center gap-2 cursor-pointer disabled:opacity-50"
              >
                <X className="w-4 h-4" />
                <span>
                  {pedido.flujo === 'B' && stageNumber === 2
                    ? 'Devolver Propuesta a China (✗)'
                    : 'Rechazar con Observaciones (✗)'}
                </span>
              </button>
            )}

            {/* Acciones contextuales de David: Devolver con Urgencia (4h, 12h, 24h) en Análisis */}
            {(stageNumber === 4 || (pedido.flujo === 'B' && stageNumber === 5)) && (
              <button
                type="button"
                disabled={!isAuthorized && !isAdmin}
                onClick={() => setShowUrgencyModal(true)}
                className="py-3 px-4 rounded-2xl bg-white border border-amber-300 hover:bg-amber-50 text-amber-900 font-black text-xs transition flex items-center gap-2 cursor-pointer disabled:opacity-50"
              >
                <AlertTriangle className="w-4 h-4 text-amber-600" />
                <span>Devolver a Codificación con Urgencia (4 h / 12 h / 24 h)</span>
              </button>
            )}

            {/* Acciones contextuales de David: Reportar Demora en Aduana */}
            {((pedido.flujo === 'A' && stageNumber === 7) ||
              (pedido.flujo === 'B' && stageNumber === 8)) && (
              <button
                type="button"
                disabled={!isAuthorized && !isAdmin}
                onClick={() => setShowDelayModal(true)}
                className="py-3 px-4 rounded-2xl bg-white border border-blue-300 hover:bg-blue-50 text-blue-900 font-black text-xs transition flex items-center gap-2 cursor-pointer disabled:opacity-50"
              >
                <Clock className="w-4 h-4 text-blue-600" />
                <span>Reportar Demora en Aduana & Recalcular</span>
              </button>
            )}
          </div>
        </div>
      )}

      {/* 6. Hilo de Observaciones */}
      <div className="pt-4 border-t border-slate-100 space-y-3">
        <h3 className="text-xs font-black uppercase tracking-wider text-[#515151]">
          Observaciones y Comentarios de la Etapa:
        </h3>

        <div className="space-y-2">
          {stage.observaciones.length === 0 ? (
            <p className="text-xs text-[#6C6B6D] italic py-2">
              No hay observaciones registradas en esta etapa aún.
            </p>
          ) : (
            stage.observaciones.map((obs) => (
              <div
                key={obs.id}
                className="p-3.5 rounded-2xl bg-slate-50 border border-slate-150 space-y-1 text-xs"
              >
                <div className="flex items-center justify-between">
                  <div className="flex items-center gap-2">
                    <div className="w-6 h-6 rounded-md bg-[#2E2E2E] text-white flex items-center justify-center font-bold text-[10px]">
                      {obs.avatar}
                    </div>
                    <strong className="text-[#2E2E2E]">{obs.usuarioNombre}</strong>
                    <span className="text-[10px] text-[#6C6B6D]">({obs.usuarioRol})</span>
                  </div>
                  <span className="text-[10px] text-[#6C6B6D] font-mono">{obs.fecha}</span>
                </div>
                <p className="text-[#515151] pl-8 leading-relaxed whitespace-pre-wrap">
                  {obs.texto}
                </p>
              </div>
            ))
          )}
        </div>

        {/* Input para agregar observación */}
        <form onSubmit={handlePostObservation} className="flex gap-2 pt-2">
          <input
            type="text"
            placeholder="Escribe una observación para el equipo en Ecuador y China..."
            value={newObsText}
            onChange={(e) => setNewObsText(e.target.value)}
            className="flex-1 px-3.5 py-2.5 rounded-xl border border-[#6C6B6D]/30 text-xs font-medium text-[#2E2E2E] focus:outline-hidden focus:ring-2 focus:ring-[#FFD100]"
          />
          <button
            type="submit"
            className="px-4 py-2.5 rounded-xl bg-[#FFD100] hover:bg-[#E6BC00] text-[#2E2E2E] font-black text-xs transition cursor-pointer flex items-center gap-1.5"
          >
            <Send className="w-3.5 h-3.5" />
            <span>Publicar</span>
          </button>
        </form>
      </div>

      {/* ======================================================= */}
      {/* MODAL: Dar Check y Aprobar */}
      {/* ======================================================= */}
      {showAdvanceModal && (
        <div className="fixed inset-0 z-50 bg-black/50 backdrop-blur-xs flex items-center justify-center p-4">
          <div className="bg-white rounded-3xl max-w-md w-full p-6 space-y-4 shadow-2xl animate-in zoom-in-95">
            <h3 className="text-base font-black text-[#2E2E2E]">
              Confirmar Check de Etapa {stageNumber}: {def.nombre}
            </h3>
            <p className="text-xs text-[#515151]">
              Al confirmar, el sistema registrará tu firma en la auditoría inmutable, notificará por Gmail al Administrador y avanzará el pedido.
            </p>

            {pendingChecklist.length > 0 && (
              <div className="p-3 bg-amber-50 border border-amber-300 rounded-xl text-xs text-amber-900 font-medium">
                Aviso: Aún tienes {pendingChecklist.length} punto(s) sin marcar en el check list. Se completarán al dar check.
              </div>
            )}

            <div>
              <label className="block text-xs font-black uppercase text-[#515151] mb-1">
                Observaciones opcionales:
              </label>
              <textarea
                rows={2}
                placeholder="Comentarios adicionales sobre la aprobación..."
                value={advanceNotes}
                onChange={(e) => setAdvanceNotes(e.target.value)}
                className="w-full px-3 py-2 rounded-xl border border-[#6C6B6D]/30 text-xs font-medium"
              />
            </div>

            <div className="flex items-center justify-end gap-2 pt-2">
              <button
                type="button"
                onClick={() => setShowAdvanceModal(false)}
                className="px-4 py-2 rounded-xl bg-slate-100 hover:bg-slate-200 text-xs font-bold"
              >
                Cancelar
              </button>
              <button
                type="button"
                onClick={() => {
                  onAdvanceStage(advanceNotes);
                  setShowAdvanceModal(false);
                  setAdvanceNotes('');
                }}
                className="px-4 py-2 rounded-xl bg-[#FFD100] hover:bg-[#E6BC00] text-[#2E2E2E] font-black text-xs shadow-xs"
              >
                Confirmar Check (✓)
              </button>
            </div>
          </div>
        </div>
      )}

      {/* ======================================================= */}
      {/* MODAL: Rechazar (✗) con observaciones — Marcela */}
      {/* ======================================================= */}
      {showRejectModal && (
        <div className="fixed inset-0 z-50 bg-black/50 backdrop-blur-xs flex items-center justify-center p-4">
          <div className="bg-white rounded-3xl max-w-md w-full p-6 space-y-4 shadow-2xl animate-in zoom-in-95">
            <h3 className="text-base font-black text-rose-700 flex items-center gap-2">
              <X className="w-5 h-5" />
              <span>Desaprobar con Observaciones (✗)</span>
            </h3>
            <p className="text-xs text-[#515151]">
              El pedido volverá inmediatamente a la etapa de Codificación con las observaciones que ingreses a continuación:
            </p>

            <div>
              <label className="block text-xs font-black uppercase text-[#515151] mb-1">
                Motivo del Rechazo / Inconsistencia detectada: *
              </label>
              <textarea
                rows={3}
                required
                placeholder="Describe el error en cantidades, etiquetas, colores o formatos..."
                value={rejectReason}
                onChange={(e) => setRejectReason(e.target.value)}
                className="w-full px-3 py-2 rounded-xl border border-rose-300 text-xs font-medium focus:ring-2 focus:ring-rose-500"
              />
            </div>

            <div className="flex items-center justify-end gap-2 pt-2">
              <button
                type="button"
                onClick={() => setShowRejectModal(false)}
                className="px-4 py-2 rounded-xl bg-slate-100 text-xs font-bold"
              >
                Cancelar
              </button>
              <button
                type="button"
                disabled={!rejectReason.trim()}
                onClick={() => {
                  onRejectStage(rejectReason);
                  setShowRejectModal(false);
                  setRejectReason('');
                }}
                className="px-4 py-2 rounded-xl bg-rose-600 hover:bg-rose-700 text-white font-black text-xs shadow-xs disabled:opacity-50"
              >
                Confirmar Rechazo y Devolver
              </button>
            </div>
          </div>
        </div>
      )}

      {/* ======================================================= */}
      {/* MODAL: Devolver con Urgencia (4h, 12h, 24h) — David */}
      {/* ======================================================= */}
      {showUrgencyModal && (
        <div className="fixed inset-0 z-50 bg-black/50 backdrop-blur-xs flex items-center justify-center p-4">
          <div className="bg-white rounded-3xl max-w-md w-full p-6 space-y-4 shadow-2xl animate-in zoom-in-95">
            <h3 className="text-base font-black text-amber-900 flex items-center gap-2">
              <AlertTriangle className="w-5 h-5 text-amber-600" />
              <span>Devolver a Codificación con Urgencia</span>
            </h3>
            <p className="text-xs text-[#515151]">
              David Túlcan en China devuelve el pedido a Codificación (Andrea Quishpe). El plazo seleccionado cubre Codificación y Revisión hasta volver a Análisis.
            </p>

            <div>
              <label className="block text-xs font-black uppercase text-[#515151] mb-1.5">
                Nivel de Urgencia Requerido:
              </label>
              <div className="grid grid-cols-3 gap-2">
                {(['Urgente', 'Prioritario', 'Normal'] as UrgenciaNivel[]).map((nivel) => {
                  const horas = nivel === 'Urgente' ? 4 : nivel === 'Prioritario' ? 12 : 24;
                  return (
                    <button
                      key={nivel}
                      type="button"
                      onClick={() => setSelectedUrgency(nivel)}
                      className={`p-2.5 rounded-xl border text-center transition cursor-pointer ${
                        selectedUrgency === nivel
                          ? 'border-2 border-[#FFD100] bg-[#FFFBEA] text-[#2E2E2E] font-black'
                          : 'border-slate-200 text-[#6C6B6D] font-bold'
                      }`}
                    >
                      <div className="text-xs">{nivel}</div>
                      <div className="text-[10px] font-mono font-bold text-amber-800">{horas} h</div>
                    </button>
                  );
                })}
              </div>
            </div>

            <div>
              <label className="block text-xs font-black uppercase text-[#515151] mb-1">
                Motivo de Devolución / Requerimiento de Fábrica: *
              </label>
              <textarea
                rows={3}
                required
                placeholder="Especifica el código Pantone, muestra o producto nuevo a codificar..."
                value={urgencyReason}
                onChange={(e) => setUrgencyReason(e.target.value)}
                className="w-full px-3 py-2 rounded-xl border border-amber-300 text-xs font-medium"
              />
            </div>

            <div className="flex items-center justify-end gap-2 pt-2">
              <button
                type="button"
                onClick={() => setShowUrgencyModal(false)}
                className="px-4 py-2 rounded-xl bg-slate-100 text-xs font-bold"
              >
                Cancelar
              </button>
              <button
                type="button"
                disabled={!urgencyReason.trim()}
                onClick={() => {
                  onReturnWithUrgency(urgencyReason, selectedUrgency);
                  setShowUrgencyModal(false);
                  setUrgencyReason('');
                }}
                className="px-4 py-2 rounded-xl bg-amber-500 hover:bg-amber-600 text-[#2E2E2E] font-black text-xs shadow-xs disabled:opacity-50"
              >
                Enviar con Urgencia {selectedUrgency}
              </button>
            </div>
          </div>
        </div>
      )}

      {/* ======================================================= */}
      {/* MODAL: Reportar Demora en Aduana — David */}
      {/* ======================================================= */}
      {showDelayModal && (
        <div className="fixed inset-0 z-50 bg-black/50 backdrop-blur-xs flex items-center justify-center p-4">
          <div className="bg-white rounded-3xl max-w-md w-full p-6 space-y-4 shadow-2xl animate-in zoom-in-95">
            <h3 className="text-base font-black text-blue-900 flex items-center gap-2">
              <Clock className="w-5 h-5 text-blue-600" />
              <span>Reportar Demora en Aduana & Recalcular</span>
            </h3>
            <p className="text-xs text-[#515151]">
              David Túlcan reporta una demora aduanera (aforo físico SENAE, inspección antinarcóticos, etc.). El sistema recalculará en cascada las fechas posteriores.
            </p>

            <div>
              <label className="block text-xs font-black uppercase text-[#515151] mb-1">
                Días hábiles adicionales de demora:
              </label>
              <input
                type="number"
                min="1"
                max="30"
                value={delayDays}
                onChange={(e) => setDelayDays(parseInt(e.target.value) || 1)}
                className="w-full px-3 py-2 rounded-xl border border-blue-300 font-mono font-black text-sm"
              />
            </div>

            <div>
              <label className="block text-xs font-black uppercase text-[#515151] mb-1">
                Motivo de la demora en Aduana: *
              </label>
              <textarea
                rows={2}
                value={delayReason}
                onChange={(e) => setDelayReason(e.target.value)}
                className="w-full px-3 py-2 rounded-xl border border-blue-300 text-xs font-medium"
              />
            </div>

            <div className="flex items-center justify-end gap-2 pt-2">
              <button
                type="button"
                onClick={() => setShowDelayModal(false)}
                className="px-4 py-2 rounded-xl bg-slate-100 text-xs font-bold"
              >
                Cancelar
              </button>
              <button
                type="button"
                onClick={() => {
                  // Calcular nueva fecha
                  const nueva = '2026-10-16';
                  onReportCustomsDelay(delayReason, nueva, delayDays);
                  setShowDelayModal(false);
                }}
                className="px-4 py-2 rounded-xl bg-blue-600 hover:bg-blue-700 text-white font-black text-xs shadow-xs"
              >
                Aplicar Recálculo de Llegada
              </button>
            </div>
          </div>
        </div>
      )}
    </div>
  );
};

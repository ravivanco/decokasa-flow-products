import React, { useState } from 'react';
import {
  Pedido,
  EtapaInstancia,
  Usuario,
  SubestadoAduana,
} from '../types';
import { ETAPAS_BASE_CONFIG } from '../data/initialData';
import { formatDisplayDate, countBusinessDaysDiff, SIMULATED_TODAY } from '../utils/dateUtils';
import { checkStageReadiness } from '../utils/orderState';
import { canUserOperateStage } from '../utils/permissions';
import {
  Info,
  CheckSquare,
  Square,
  Upload,
  Link,
  File,
  FileText,
  FileSpreadsheet,
  Image,
  Archive,
  Clock,
  RotateCcw,
  CornerUpLeft,
  Check,
  AlertTriangle,
  Send,
  Eye,
  History,
  ShieldCheck,
  CheckCircle2,
  X,
} from 'lucide-react';

interface StagePanelProps {
  pedido: Pedido;
  stageNumber: number;
  currentUser: Usuario;
  onAdvanceStage: (observaciones?: string) => void;
  onReturnStage: (motivo: string) => void;
  onRevertStage: (motivo: string) => void;
  onExtendFabrication: (dias: number, motivo: string) => void;
  onUpdateCustomsSubstate: (subestado: SubestadoAduana, motivo?: string) => void;
  onApproveBoardFabrication: () => void;
  onRejectBoardFabrication: (motivo: string) => void;
  onToggleChecklist: (checkId: string) => void;
  onAddObservation: (texto: string) => void;
  onUploadFile: (docId: string, fileData: { nombre: string; tamano: string; driveUrl?: string; esImagen?: boolean }) => void;
}

export const StagePanel: React.FC<StagePanelProps> = ({
  pedido,
  stageNumber,
  currentUser,
  onAdvanceStage,
  onReturnStage,
  onRevertStage,
  onExtendFabrication,
  onUpdateCustomsSubstate,
  onApproveBoardFabrication,
  onRejectBoardFabrication,
  onToggleChecklist,
  onAddObservation,
  onUploadFile,
}) => {
  const stage = pedido.etapas[stageNumber - 1];
  const def = ETAPAS_BASE_CONFIG[stageNumber - 1];
  const isCurrentStage = pedido.etapaActualNumero === stageNumber && pedido.estado !== 'Finalizado';

  // Permission check
  const hasStagePermission = canUserOperateStage(currentUser, stageNumber);
  const readiness = checkStageReadiness(stage);

  // States for modals & inputs
  const [obsText, setObsText] = useState('');
  const [driveUrlInput, setDriveUrlInput] = useState<{ [docId: string]: string }>({});
  const [showDriveInput, setShowDriveInput] = useState<{ [docId: string]: boolean }>({});
  const [fileError, setFileError] = useState<string | null>(null);
  const [previewImage, setPreviewImage] = useState<string | null>(null);

  // Action modals states
  const [activeModal, setActiveModal] = useState<
    null | 'check' | 'return' | 'revert' | 'extend' | 'customs' | 'rejectBoard'
  >(null);
  const [modalMotive, setModalMotive] = useState('');
  const [extendDaysInput, setExtendDaysInput] = useState(3);
  const [customsStateInput, setCustomsStateInput] = useState<SubestadoAduana>(
    stage.subestadoAduana || 'En trámite'
  );

  // Handle real file upload
  const handleFileInputChange = (docId: string, event: React.ChangeEvent<HTMLInputElement>) => {
    setFileError(null);
    const file = event.target.files?.[0];
    if (!file) return;

    // Validate size (100 MB max)
    const maxSizeBytes = 100 * 1024 * 1024;
    if (file.size > maxSizeBytes) {
      setFileError('El archivo supera los 100 MB permitidos.');
      return;
    }

    // Validate type (PDF, Excel, Word, JPG, PNG, Zip)
    const allowedExtensions = ['.pdf', '.xlsx', '.xls', '.docx', '.doc', '.jpg', '.jpeg', '.png', '.zip'];
    const fileName = file.name.toLowerCase();
    const isAllowed = allowedExtensions.some((ext) => fileName.endsWith(ext));
    if (!isAllowed) {
      setFileError('Tipo de archivo no permitido. Solo se aceptan PDF, Excel, Word, JPG/PNG y Zip.');
      return;
    }

    const isImg = fileName.endsWith('.jpg') || fileName.endsWith('.jpeg') || fileName.endsWith('.png');
    const sizeFormatted = (file.size / (1024 * 1024)).toFixed(1) + ' MB';

    onUploadFile(docId, {
      nombre: file.name,
      tamano: sizeFormatted,
      esImagen: isImg,
    });
  };

  const handleSaveDriveUrl = (docId: string) => {
    const url = driveUrlInput[docId]?.trim();
    if (!url) return;
    onUploadFile(docId, {
      nombre: `Enlace_Google_Drive.link`,
      tamano: 'Enlace web',
      driveUrl: url,
    });
    setDriveUrlInput((prev) => ({ ...prev, [docId]: '' }));
    setShowDriveInput((prev) => ({ ...prev, [docId]: false }));
  };

  const handlePublishObservation = (e: React.FormEvent) => {
    e.preventDefault();
    if (!obsText.trim()) return;
    onAddObservation(obsText.trim());
    setObsText('');
  };

  // Calculations for days remaining / delay
  const diffFromToday = countBusinessDaysDiff(SIMULATED_TODAY, stage.fechaFinEstimada);
  const isDelayed = diffFromToday < 0 && !stage.completada;

  return (
    <div className="bg-white rounded-3xl border border-[#6C6B6D]/20 shadow-xs p-6 sm:p-8 space-y-8 animate-in fade-in duration-200">
      {/* ==================================================
          BLOQUE A: ¿Qué se hace en esta etapa?
          ================================================== */}
      <div className="border-b border-slate-100 pb-6 space-y-4">
        <div className="flex flex-col sm:flex-row sm:items-center justify-between gap-3">
          <div>
            <div className="flex items-center gap-2">
              <span className="font-mono text-xs font-black px-2.5 py-0.5 rounded-lg bg-[#FFD100] text-[#2E2E2E]">
                Etapa {stage.numero} de 11
              </span>
              <span className="text-xs font-bold text-[#6C6B6D]">
                Grupo: {def.grupo}
              </span>
            </div>
            <h2 className="text-xl sm:text-2xl font-black text-[#2E2E2E] mt-1">
              {stage.nombre}
            </h2>
          </div>

          {/* Status badge */}
          <div className="self-start sm:self-center">
            {stage.completada ? (
              <span className="inline-flex items-center gap-1.5 px-3 py-1.5 rounded-xl text-xs font-black bg-emerald-100 text-[#16A34A]">
                <CheckCircle2 className="w-4 h-4" />
                Completada el {formatDisplayDate(stage.fechaRealFin)}
              </span>
            ) : isDelayed ? (
              <span className="inline-flex items-center gap-1.5 px-3 py-1.5 rounded-xl text-xs font-black bg-rose-100 text-[#DC2626]">
                <AlertTriangle className="w-4 h-4" />
                Retrasado: {Math.abs(diffFromToday)} días hábiles de atraso
              </span>
            ) : diffFromToday <= 3 ? (
              <span className="inline-flex items-center gap-1.5 px-3 py-1.5 rounded-xl text-xs font-black bg-orange-100 text-[#EA580C]">
                <Clock className="w-4 h-4" />
                Por vencer: faltan {diffFromToday} días hábiles
              </span>
            ) : (
              <span className="inline-flex items-center gap-1.5 px-3 py-1.5 rounded-xl text-xs font-black bg-emerald-50 text-[#16A34A]">
                <Check className="w-4 h-4" />
                A tiempo: faltan {diffFromToday} días hábiles
              </span>
            )}
          </div>
        </div>

        {/* Explanation text */}
        <p className="text-sm font-semibold text-[#515151] leading-relaxed">
          {def.explicacion}
        </p>

        {/* Responsables & Dates Grid */}
        <div className="grid grid-cols-2 sm:grid-cols-4 gap-3 p-4 rounded-2xl bg-[#FFFBEA] border border-[#FFD100]/40 text-xs">
          <div>
            <span className="text-[#6C6B6D] block font-semibold">Responsable principal:</span>
            <strong className="text-[#2E2E2E] font-black">{def.responsableTexto}</strong>
          </div>
          <div>
            <span className="text-[#6C6B6D] block font-semibold">Suplente autorizado:</span>
            <strong className="text-[#2E2E2E] font-bold">{def.suplenteTexto}</strong>
          </div>
          <div>
            <span className="text-[#6C6B6D] block font-semibold">Plazo asignado:</span>
            <strong className="text-[#2E2E2E] font-black">
              {stage.plazoDiasHabiles} días hábiles
              {stage.plazoExtraDias > 0 && ` (+${stage.plazoExtraDias} extra)`}
            </strong>
          </div>
          <div>
            <span className="text-[#6C6B6D] block font-semibold">
              {stage.completada ? 'Fecha final real:' : 'Fecha final estimada:'}
            </span>
            <strong className="text-[#2E2E2E] font-black">
              {stage.completada ? formatDisplayDate(stage.fechaRealFin) : formatDisplayDate(stage.fechaFinEstimada)}
            </strong>
          </div>
        </div>
      </div>

      {/* ==================================================
          BLOQUE B: Check list de la etapa
          ================================================== */}
      <div className="space-y-3">
        <div className="flex items-center justify-between">
          <h3 className="text-sm font-black uppercase tracking-wider text-[#2E2E2E] flex items-center gap-2">
            <CheckSquare className="w-4 h-4 text-[#515151]" />
            <span>Check List Requerido</span>
          </h3>
          <span className="text-xs font-bold text-[#6C6B6D]">
            {stage.checklist.filter((c) => c.completado).length} de {stage.checklist.length} completos
          </span>
        </div>

        <div className="space-y-2">
          {stage.checklist.map((item) => (
            <div
              key={item.id}
              onClick={() => {
                if (hasStagePermission && isCurrentStage) {
                  onToggleChecklist(item.id);
                }
              }}
              className={`p-3.5 rounded-2xl border transition-all flex items-start gap-3 select-none ${
                hasStagePermission && isCurrentStage ? 'cursor-pointer hover:bg-[#FFFBEA]' : 'opacity-85'
              } ${
                item.completado
                  ? 'bg-emerald-50/60 border-emerald-200 text-[#2E2E2E]'
                  : 'bg-white border-[#6C6B6D]/20 text-[#515151]'
              }`}
            >
              <div className="mt-0.5 shrink-0">
                {item.completado ? (
                  <CheckSquare className="w-5 h-5 text-[#16A34A] stroke-[2.5]" />
                ) : (
                  <Square className="w-5 h-5 text-[#6C6B6D]" />
                )}
              </div>
              <div className="flex-1 text-xs font-semibold leading-snug">
                <span>{item.texto}</span>
                {item.completado && item.completadoPor && (
                  <span className="block text-[11px] text-[#16A34A] font-bold mt-0.5">
                    ✓ Verificado por {item.completadoPor} ({item.fecha})
                  </span>
                )}
              </div>
            </div>
          ))}
        </div>
      </div>

      {/* ==================================================
          BLOQUE C: Documentos de la etapa (Versionado)
          ================================================== */}
      <div className="space-y-4">
        <div className="flex items-center justify-between">
          <h3 className="text-sm font-black uppercase tracking-wider text-[#2E2E2E] flex items-center gap-2">
            <FileText className="w-4 h-4 text-[#515151]" />
            <span>Documentos de la Etapa</span>
          </h3>
          <span className="text-xs text-[#6C6B6D]">
            Formatos aceptados: PDF, Excel, Word, JPG/PNG, Zip (máx. 100 MB)
          </span>
        </div>

        {fileError && (
          <div className="p-3 rounded-xl bg-rose-50 border border-rose-200 text-xs font-bold text-[#DC2626] flex items-center gap-2">
            <AlertTriangle className="w-4 h-4 shrink-0" />
            <span>{fileError}</span>
          </div>
        )}

        <div className="space-y-3">
          {stage.documentos.map((doc) => {
            const isUploaded = !!doc.archivoActual;
            return (
              <div
                key={doc.id}
                className="p-4 rounded-2xl border border-[#6C6B6D]/20 bg-white space-y-3"
              >
                <div className="flex flex-col sm:flex-row sm:items-center justify-between gap-2">
                  <div className="flex items-center gap-2">
                    <span className="text-xs font-bold text-[#2E2E2E]">
                      {doc.tipoDoc}
                    </span>
                    {doc.esObligatorio ? (
                      <span
                        className={`text-[10px] font-black px-2 py-0.5 rounded-full ${
                          isUploaded
                            ? 'bg-emerald-100 text-[#16A34A]'
                            : 'bg-rose-100 text-[#DC2626]'
                        }`}
                      >
                        {isUploaded ? '✓ Subido' : 'Obligatorio · Falta'}
                      </span>
                    ) : (
                      <span className="text-[10px] font-bold px-2 py-0.5 rounded-full bg-slate-100 text-[#6C6B6D]">
                        Opcional
                      </span>
                    )}
                  </div>

                  {/* Upload button or link */}
                  {hasStagePermission && isCurrentStage && (
                    <div className="flex items-center gap-2">
                      <label className="inline-flex items-center gap-1.5 px-3 py-1.5 rounded-xl text-xs font-black bg-[#FFD100] hover:bg-[#E6BC00] text-[#2E2E2E] cursor-pointer shadow-xs transition">
                        <Upload className="w-3.5 h-3.5" />
                        <span>{isUploaded ? 'Reemplazar (v+1)' : 'Subir archivo'}</span>
                        <input
                          type="file"
                          className="hidden"
                          onChange={(e) => handleFileInputChange(doc.id, e)}
                        />
                      </label>

                      <button
                        type="button"
                        onClick={() =>
                          setShowDriveInput((prev) => ({
                            ...prev,
                            [doc.id]: !prev[doc.id],
                          }))
                        }
                        className="inline-flex items-center gap-1 px-2.5 py-1.5 rounded-xl text-xs font-bold bg-[#FFFBEA] border border-[#FFD100] text-[#2E2E2E] hover:bg-[#FFD100]/30 transition cursor-pointer"
                        title="Pegar enlace de Google Drive"
                      >
                        <Link className="w-3 h-3" />
                        <span>Drive</span>
                      </button>
                    </div>
                  )}
                </div>

                {/* Drive input dropdown */}
                {showDriveInput[doc.id] && (
                  <div className="flex items-center gap-2 p-2.5 rounded-xl bg-slate-50 border border-slate-200 text-xs">
                    <input
                      type="url"
                      placeholder="Pega el enlace de Google Drive aquí..."
                      value={driveUrlInput[doc.id] || ''}
                      onChange={(e) =>
                        setDriveUrlInput((prev) => ({
                          ...prev,
                          [doc.id]: e.target.value,
                        }))
                      }
                      className="flex-1 p-2 rounded-lg border border-slate-300 text-xs text-[#2E2E2E] focus:outline-hidden focus:ring-1 focus:ring-[#FFD100]"
                    />
                    <button
                      type="button"
                      onClick={() => handleSaveDriveUrl(doc.id)}
                      className="px-3 py-2 rounded-lg font-black text-xs bg-[#FFD100] text-[#2E2E2E]"
                    >
                      Guardar
                    </button>
                  </div>
                )}

                {/* Current uploaded file display */}
                {doc.archivoActual ? (
                  <div className="p-3 rounded-xl bg-[#FFFBEA]/70 border border-[#FFD100]/30 flex items-center justify-between gap-3 text-xs">
                    <div className="flex items-center gap-2.5 min-w-0">
                      <span className="p-2 rounded-lg bg-[#FFD100] text-[#2E2E2E] shrink-0 font-bold">
                        {doc.archivoActual.esImagen ? <Image className="w-4 h-4" /> : <File className="w-4 h-4" />}
                      </span>
                      <div className="min-w-0">
                        <div className="flex items-center gap-2">
                          <span className="font-bold text-[#2E2E2E] truncate">
                            {doc.archivoActual.nombre}
                          </span>
                          <span className="text-[10px] font-black px-1.5 py-0.2 rounded bg-black/10 text-[#2E2E2E]">
                            v{doc.archivoActual.version}
                          </span>
                        </div>
                        <span className="text-[11px] text-[#6C6B6D] block">
                          {doc.archivoActual.tamano} · Subido por {doc.archivoActual.subidoPor} ({doc.archivoActual.subidoPorRol}) el {doc.archivoActual.fecha}
                        </span>
                      </div>
                    </div>

                    <div className="flex items-center gap-1.5 shrink-0">
                      {doc.archivoActual.esImagen && (
                        <button
                          type="button"
                          onClick={() => setPreviewImage(doc.archivoActual?.nombre || 'Vista previa')}
                          className="p-1.5 text-[#515151] hover:text-black rounded-lg hover:bg-white"
                          title="Ver imagen"
                        >
                          <Eye className="w-4 h-4" />
                        </button>
                      )}
                    </div>
                  </div>
                ) : (
                  <div className="p-3 rounded-xl bg-slate-50 border border-dashed border-[#6C6B6D]/30 text-xs text-[#6C6B6D] text-center">
                    No se ha subido este documento aún
                  </div>
                )}

                {/* Version history if available */}
                {doc.historialVersiones.length > 0 && (
                  <div className="pl-4 pt-1 border-l-2 border-[#FFD100] text-[11px] text-[#6C6B6D] space-y-1">
                    <span className="font-bold text-[#515151] block">Historial de versiones previas:</span>
                    {doc.historialVersiones.map((v, vIdx) => (
                      <div key={vIdx} className="flex items-center justify-between">
                        <span>v{v.version}: {v.nombre} ({v.tamano})</span>
                        <span>{v.subidoPor} · {v.fecha}</span>
                      </div>
                    ))}
                  </div>
                )}
              </div>
            );
          })}
        </div>

        <p className="text-[11px] font-semibold text-[#6C6B6D] italic">
          💡 En la versión final, los archivos se guardan automáticamente en la carpeta del pedido en Google Drive.
        </p>
      </div>

      {/* ==================================================
          BLOQUE D: Observaciones de la etapa
          ================================================== */}
      <div className="space-y-4">
        <h3 className="text-sm font-black uppercase tracking-wider text-[#2E2E2E]">
          Observaciones y Comunicaciones
        </h3>

        {/* Message feed */}
        <div className="space-y-3 max-h-72 overflow-y-auto pr-1">
          {stage.observaciones.length === 0 ? (
            <p className="text-xs text-[#6C6B6D] italic p-4 bg-slate-50 rounded-2xl text-center">
              No hay observaciones registradas en esta etapa.
            </p>
          ) : (
            stage.observaciones.map((obs) => (
              <div
                key={obs.id}
                className="p-3.5 rounded-2xl bg-slate-50 border border-slate-200 text-xs space-y-1.5"
              >
                <div className="flex items-center justify-between text-[11px]">
                  <div className="flex items-center gap-2">
                    <div className="w-6 h-6 rounded-md bg-[#2E2E2E] text-white flex items-center justify-center font-bold text-[10px]">
                      {obs.avatar}
                    </div>
                    <span className="font-black text-[#2E2E2E]">{obs.usuarioNombre}</span>
                    <span className="text-[#6C6B6D]">({obs.usuarioRol})</span>
                  </div>
                  <span className="font-mono text-[#6C6B6D]">{obs.fecha}</span>
                </div>
                <p className="text-[#515151] font-medium leading-relaxed pl-8">
                  {obs.texto}
                </p>
              </div>
            ))
          )}
        </div>

        {/* Post box */}
        <form onSubmit={handlePublishObservation} className="flex gap-2">
          <input
            type="text"
            required
            placeholder="Escribe una observación de la etapa..."
            value={obsText}
            onChange={(e) => setObsText(e.target.value)}
            className="flex-1 px-4 py-2.5 rounded-xl border border-[#6C6B6D]/30 text-xs text-[#2E2E2E] focus:outline-hidden focus:ring-2 focus:ring-[#FFD100]"
          />
          <button
            type="submit"
            className="px-4 py-2.5 rounded-xl font-black text-xs bg-[#FFD100] hover:bg-[#E6BC00] text-[#2E2E2E] flex items-center gap-1.5 shadow-xs cursor-pointer"
          >
            <Send className="w-3.5 h-3.5" />
            <span>Publicar</span>
          </button>
        </form>
        <span className="text-[10px] text-[#6C6B6D] block">
          * Las observaciones no se traducen automáticamente y se guardan en el historial del pedido.
        </span>
      </div>

      {/* ==================================================
          BLOQUE E: Acciones según el rol
          ================================================== */}
      <div className="pt-6 border-t border-slate-100 space-y-3">
        <h3 className="text-sm font-black uppercase tracking-wider text-[#2E2E2E]">
          Acciones Disponibles
        </h3>

        {/* If user does not have permission */}
        {!hasStagePermission ? (
          <div className="p-4 rounded-2xl bg-slate-50 border border-slate-200 text-xs font-semibold text-[#6C6B6D] flex items-center gap-2.5">
            <ShieldCheck className="w-5 h-5 text-[#6C6B6D] shrink-0" />
            <span>
              Esta etapa la gestiona {def.responsableTexto}. Puedes ver la información pero no modificarla.
            </span>
          </div>
        ) : !isCurrentStage ? (
          <div className="p-4 rounded-2xl bg-slate-50 border border-slate-200 text-xs font-semibold text-[#6C6B6D] flex items-center gap-2">
            <Info className="w-4 h-4 text-[#6C6B6D]" />
            <span>
              {stage.completada
                ? 'Esta etapa ya ha sido completada.'
                : 'Esta etapa aún no es la activa en el cronograma.'}
            </span>
            {/* Revert allowed for admin or assigned editor if completed */}
            {stage.completada && (currentUser.rol === 'admin' || hasStagePermission) && (
              <button
                type="button"
                onClick={() => {
                  setModalMotive('');
                  setActiveModal('revert');
                }}
                className="ml-auto px-3 py-1 rounded-xl text-xs font-black bg-white border border-[#6C6B6D]/40 text-[#515151] hover:bg-slate-100 cursor-pointer"
              >
                Revertir check
              </button>
            )}
          </div>
        ) : (
          <div className="space-y-3">
            {/* Action buttons bar */}
            <div className="flex flex-wrap items-center gap-3">
              {/* SPECIAL ACTION: Aprobación de la Junta (Stage 3) */}
              {stageNumber === 3 && (currentUser.rol === 'junta' || currentUser.rol === 'admin') ? (
                <>
                  <button
                    type="button"
                    onClick={onApproveBoardFabrication}
                    className="px-5 py-2.5 rounded-xl font-black text-sm bg-[#16A34A] hover:bg-emerald-700 text-white flex items-center gap-2 shadow-sm cursor-pointer"
                  >
                    <CheckCircle2 className="w-4 h-4" />
                    <span>Aprobar fabricación</span>
                  </button>
                  <button
                    type="button"
                    onClick={() => {
                      setModalMotive('');
                      setActiveModal('rejectBoard');
                    }}
                    className="px-4 py-2.5 rounded-xl font-bold text-sm bg-rose-50 hover:bg-rose-100 text-[#DC2626] border border-rose-200 cursor-pointer"
                  >
                    Rechazar
                  </button>
                </>
              ) : (
                /* STANDARD ACTION: DAR CHECK Y AVANZAR (Amarillo dominante) */
                <div className="flex items-center gap-2">
                  <button
                    type="button"
                    disabled={!readiness.isReady}
                    onClick={() => {
                      setModalMotive('');
                      setActiveModal('check');
                    }}
                    className="px-6 py-2.5 rounded-xl font-black text-sm bg-[#FFD100] hover:bg-[#E6BC00] disabled:bg-slate-100 disabled:text-[#6C6B6D] disabled:cursor-not-allowed text-[#2E2E2E] flex items-center gap-2 shadow-sm transition cursor-pointer"
                  >
                    <Check className="w-4 h-4 stroke-[3]" />
                    <span>Dar check y avanzar</span>
                  </button>

                  {!readiness.isReady && (
                    <span className="text-xs text-[#DC2626] font-bold">
                      Falta: {readiness.missingItems.join(', ')}
                    </span>
                  )}
                </div>
              )}

              {/* ACTION: Devolver con observaciones */}
              <button
                type="button"
                onClick={() => {
                  setModalMotive('');
                  setActiveModal('return');
                }}
                className="px-4 py-2.5 rounded-xl font-bold text-xs bg-rose-50 hover:bg-rose-100 text-[#DC2626] border border-rose-200 flex items-center gap-1.5 cursor-pointer"
              >
                <RotateCcw className="w-3.5 h-3.5" />
                <span>Devolver con observaciones</span>
              </button>

              {/* ACTION: Revertir check */}
              <button
                type="button"
                onClick={() => {
                  setModalMotive('');
                  setActiveModal('revert');
                }}
                className="px-4 py-2.5 rounded-xl font-bold text-xs bg-white hover:bg-slate-50 text-[#515151] border border-[#6C6B6D]/30 flex items-center gap-1.5 cursor-pointer"
              >
                <CornerUpLeft className="w-3.5 h-3.5" />
                <span>Revertir check</span>
              </button>

              {/* SPECIAL ACTION: Solicitar ampliación de fabricación (Stage 4) */}
              {stageNumber === 4 && (currentUser.rol === 'editor_china' || currentUser.rol === 'admin') && (
                <button
                  type="button"
                  onClick={() => {
                    setExtendDaysInput(3);
                    setModalMotive('');
                    setActiveModal('extend');
                  }}
                  className="px-4 py-2.5 rounded-xl font-bold text-xs bg-[#FFFBEA] border border-[#FFD100] text-[#2E2E2E] hover:bg-[#FFD100]/40 flex items-center gap-1.5 cursor-pointer"
                >
                  <Clock className="w-3.5 h-3.5" />
                  <span>Solicitar ampliación (máx +10d)</span>
                </button>
              )}

              {/* SPECIAL ACTION: Desaduanización subestados (Stage 9) */}
              {stageNumber === 9 && (currentUser.rol === 'editor_logistica' || currentUser.rol === 'admin') && (
                <button
                  type="button"
                  onClick={() => {
                    setCustomsStateInput(stage.subestadoAduana || 'En trámite');
                    setModalMotive('');
                    setActiveModal('customs');
                  }}
                  className="px-4 py-2.5 rounded-xl font-bold text-xs bg-orange-50 border border-[#EA580C]/40 text-[#EA580C] hover:bg-orange-100 flex items-center gap-1.5 cursor-pointer"
                >
                  <ShieldCheck className="w-3.5 h-3.5" />
                  <span>Subestado SENAE ({stage.subestadoAduana || 'En trámite'})</span>
                </button>
              )}
            </div>
          </div>
        )}
      </div>

      {/* ==================================================
          BLOQUE F: Historial de la etapa
          ================================================== */}
      <div className="pt-6 border-t border-slate-100 space-y-3">
        <h3 className="text-xs font-black uppercase tracking-wider text-[#6C6B6D] flex items-center gap-1.5">
          <History className="w-3.5 h-3.5" />
          <span>Historial de acciones en esta etapa</span>
        </h3>

        <div className="space-y-2">
          {pedido.historial
            .filter((h) => h.etapaNumero === stageNumber)
            .map((h) => (
              <div
                key={h.id}
                className="text-xs p-3 rounded-xl bg-slate-50 border border-slate-200 flex flex-col sm:flex-row sm:items-center justify-between gap-1 text-[#515151]"
              >
                <div>
                  <span className="font-bold text-[#2E2E2E]">{h.accion}</span>
                  <span className="text-[#6C6B6D] ml-1.5">
                    — {h.usuarioNombre} ({h.usuarioRol})
                  </span>
                  {h.motivo && (
                    <span className="block text-[11px] text-[#515151] italic mt-0.5">
                      &ldquo;{h.motivo}&rdquo;
                    </span>
                  )}
                </div>
                <span className="font-mono text-[10px] text-[#6C6B6D] shrink-0">
                  {h.fecha}
                </span>
              </div>
            ))}
        </div>
      </div>

      {/* ==================================================
          MODALES DE CONFIRMACIÓN PROPIOS
          ================================================== */}

      {/* 1. Modal: DAR CHECK */}
      {activeModal === 'check' && (
        <div className="fixed inset-0 z-50 flex items-center justify-center p-4 bg-black/50 backdrop-blur-xs">
          <div className="bg-white rounded-3xl p-6 sm:p-7 max-w-md w-full shadow-2xl border border-[#6C6B6D]/30 space-y-4 animate-in fade-in">
            <div className="flex items-center gap-3 text-[#2E2E2E]">
              <div className="w-10 h-10 rounded-2xl bg-[#FFD100] flex items-center justify-center font-bold">
                <Check className="w-5 h-5 stroke-[3]" />
              </div>
              <h4 className="text-lg font-black">Confirmar Check de Etapa</h4>
            </div>

            <p className="text-xs text-[#515151]">
              ¿Deseas marcar como completada la etapa{' '}
              <strong>{stage.numero}. {stage.nombre}</strong> y avanzar el pedido a la siguiente fase?
            </p>

            <div>
              <label className="block text-xs font-bold text-[#2E2E2E] mb-1">
                Observación opcional:
              </label>
              <textarea
                rows={2}
                placeholder="Escribe una observación opcional para el historial..."
                value={modalMotive}
                onChange={(e) => setModalMotive(e.target.value)}
                className="w-full p-2.5 rounded-xl border border-slate-300 text-xs text-[#2E2E2E] focus:outline-hidden focus:ring-2 focus:ring-[#FFD100]"
              />
            </div>

            <div className="flex justify-end gap-2 pt-2">
              <button
                type="button"
                onClick={() => setActiveModal(null)}
                className="px-4 py-2 rounded-xl text-xs font-bold text-[#6C6B6D] hover:bg-slate-100 cursor-pointer"
              >
                Cancelar
              </button>
              <button
                type="button"
                onClick={() => {
                  onAdvanceStage(modalMotive.trim() || undefined);
                  setActiveModal(null);
                }}
                className="px-5 py-2.5 rounded-xl text-xs font-black bg-[#FFD100] hover:bg-[#E6BC00] text-[#2E2E2E] cursor-pointer"
              >
                Confirmar Check
              </button>
            </div>
          </div>
        </div>
      )}

      {/* 2. Modal: DEVOLVER CON OBSERVACIONES */}
      {activeModal === 'return' && (
        <div className="fixed inset-0 z-50 flex items-center justify-center p-4 bg-black/50 backdrop-blur-xs">
          <div className="bg-white rounded-3xl p-6 sm:p-7 max-w-md w-full shadow-2xl border border-rose-200 space-y-4 animate-in fade-in">
            <div className="flex items-center gap-3 text-[#DC2626]">
              <div className="w-10 h-10 rounded-2xl bg-rose-100 flex items-center justify-center font-bold">
                <RotateCcw className="w-5 h-5 text-[#DC2626]" />
              </div>
              <h4 className="text-lg font-black text-[#2E2E2E]">Devolver con Observaciones</h4>
            </div>

            <p className="text-xs text-[#515151]">
              El pedido regresará al responsable anterior para corrección. El motivo es <strong>obligatorio</strong>.
            </p>

            <div>
              <label className="block text-xs font-bold text-[#2E2E2E] mb-1">
                Motivo / Observaciones:
              </label>
              <textarea
                rows={3}
                required
                placeholder="Indica qué debe corregirse..."
                value={modalMotive}
                onChange={(e) => setModalMotive(e.target.value)}
                className="w-full p-2.5 rounded-xl border border-rose-300 text-xs text-[#2E2E2E] focus:outline-hidden focus:ring-2 focus:ring-rose-400"
              />
            </div>

            <div className="flex justify-end gap-2 pt-2">
              <button
                type="button"
                onClick={() => setActiveModal(null)}
                className="px-4 py-2 rounded-xl text-xs font-bold text-[#6C6B6D] hover:bg-slate-100 cursor-pointer"
              >
                Cancelar
              </button>
              <button
                type="button"
                disabled={!modalMotive.trim()}
                onClick={() => {
                  onReturnStage(modalMotive.trim());
                  setActiveModal(null);
                }}
                className="px-5 py-2.5 rounded-xl text-xs font-black bg-[#DC2626] hover:bg-rose-700 disabled:opacity-50 text-white cursor-pointer"
              >
                Devolver Pedido
              </button>
            </div>
          </div>
        </div>
      )}

      {/* 3. Modal: REVERTIR CHECK */}
      {activeModal === 'revert' && (
        <div className="fixed inset-0 z-50 flex items-center justify-center p-4 bg-black/50 backdrop-blur-xs">
          <div className="bg-white rounded-3xl p-6 sm:p-7 max-w-md w-full shadow-2xl border border-slate-300 space-y-4 animate-in fade-in">
            <div className="flex items-center gap-3 text-[#2E2E2E]">
              <div className="w-10 h-10 rounded-2xl bg-slate-100 flex items-center justify-center font-bold">
                <CornerUpLeft className="w-5 h-5 text-[#515151]" />
              </div>
              <h4 className="text-lg font-black">Revertir Check de Etapa</h4>
            </div>

            <p className="text-xs text-[#515151]">
              Esta acción reabrirá la etapa para edición. El motivo es <strong>obligatorio</strong> para la auditoría.
            </p>

            <div>
              <label className="block text-xs font-bold text-[#2E2E2E] mb-1">
                Motivo de la reversión:
              </label>
              <textarea
                rows={3}
                required
                placeholder="Especifica por qué se revierte el check..."
                value={modalMotive}
                onChange={(e) => setModalMotive(e.target.value)}
                className="w-full p-2.5 rounded-xl border border-slate-300 text-xs text-[#2E2E2E] focus:outline-hidden focus:ring-2 focus:ring-[#FFD100]"
              />
            </div>

            <div className="flex justify-end gap-2 pt-2">
              <button
                type="button"
                onClick={() => setActiveModal(null)}
                className="px-4 py-2 rounded-xl text-xs font-bold text-[#6C6B6D] hover:bg-slate-100 cursor-pointer"
              >
                Cancelar
              </button>
              <button
                type="button"
                disabled={!modalMotive.trim()}
                onClick={() => {
                  onRevertStage(modalMotive.trim());
                  setActiveModal(null);
                }}
                className="px-5 py-2.5 rounded-xl text-xs font-black bg-[#2E2E2E] hover:bg-black disabled:opacity-50 text-white cursor-pointer"
              >
                Revertir Check
              </button>
            </div>
          </div>
        </div>
      )}

      {/* 4. Modal: AMPLIACIÓN DE FABRICACIÓN */}
      {activeModal === 'extend' && (
        <div className="fixed inset-0 z-50 flex items-center justify-center p-4 bg-black/50 backdrop-blur-xs">
          <div className="bg-white rounded-3xl p-6 sm:p-7 max-w-md w-full shadow-2xl border border-[#FFD100] space-y-4 animate-in fade-in">
            <div className="flex items-center gap-3 text-[#2E2E2E]">
              <div className="w-10 h-10 rounded-2xl bg-[#FFFBEA] border border-[#FFD100] flex items-center justify-center font-bold">
                <Clock className="w-5 h-5 text-[#2E2E2E]" />
              </div>
              <h4 className="text-lg font-black">Ampliación de Fabricación</h4>
            </div>

            <p className="text-xs text-[#515151]">
              Puedes ampliar de 1 a 10 días hábiles adicionales como máximo. Motivo <strong>obligatorio</strong>.
            </p>

            <div>
              <label className="block text-xs font-bold text-[#2E2E2E] mb-1">
                Días hábiles a sumar:
              </label>
              <input
                type="number"
                min="1"
                max="10"
                value={extendDaysInput}
                onChange={(e) => setExtendDaysInput(Number(e.target.value))}
                className="w-full p-2.5 rounded-xl border border-slate-300 text-sm font-black text-[#2E2E2E]"
              />
            </div>

            <div>
              <label className="block text-xs font-bold text-[#2E2E2E] mb-1">
                Motivo de la ampliación:
              </label>
              <textarea
                rows={2}
                required
                placeholder="Ej: Fábrica reporta retraso en materia prima..."
                value={modalMotive}
                onChange={(e) => setModalMotive(e.target.value)}
                className="w-full p-2.5 rounded-xl border border-slate-300 text-xs text-[#2E2E2E]"
              />
            </div>

            <div className="flex justify-end gap-2 pt-2">
              <button
                type="button"
                onClick={() => setActiveModal(null)}
                className="px-4 py-2 rounded-xl text-xs font-bold text-[#6C6B6D] hover:bg-slate-100 cursor-pointer"
              >
                Cancelar
              </button>
              <button
                type="button"
                disabled={!modalMotive.trim() || extendDaysInput <= 0}
                onClick={() => {
                  onExtendFabrication(extendDaysInput, modalMotive.trim());
                  setActiveModal(null);
                }}
                className="px-5 py-2.5 rounded-xl text-xs font-black bg-[#FFD100] hover:bg-[#E6BC00] text-[#2E2E2E] cursor-pointer"
              >
                Aplicar Ampliación
              </button>
            </div>
          </div>
        </div>
      )}

      {/* 5. Modal: SUBESTADO DE ADUANA */}
      {activeModal === 'customs' && (
        <div className="fixed inset-0 z-50 flex items-center justify-center p-4 bg-black/50 backdrop-blur-xs">
          <div className="bg-white rounded-3xl p-6 sm:p-7 max-w-md w-full shadow-2xl border border-orange-200 space-y-4 animate-in fade-in">
            <h4 className="text-lg font-black text-[#2E2E2E]">Subestado de Desaduanización SENAE</h4>
            <p className="text-xs text-[#515151]">
              Selecciona el estado actual del trámite aduanero en Ecuador:
            </p>

            <div className="grid grid-cols-3 gap-2">
              {(['En trámite', 'Retenido', 'Liberado'] as SubestadoAduana[]).map((st) => (
                <button
                  key={st}
                  type="button"
                  onClick={() => setCustomsStateInput(st)}
                  className={`p-3 rounded-xl border text-xs font-black transition cursor-pointer ${
                    customsStateInput === st
                      ? st === 'Retenido'
                        ? 'bg-rose-50 border-[#DC2626] text-[#DC2626]'
                        : st === 'Liberado'
                        ? 'bg-emerald-50 border-[#16A34A] text-[#16A34A]'
                        : 'bg-[#FFFBEA] border-[#FFD100] text-[#2E2E2E]'
                      : 'border-slate-200 bg-white text-[#6C6B6D]'
                  }`}
                >
                  {st}
                </button>
              ))}
            </div>

            <div>
              <label className="block text-xs font-bold text-[#2E2E2E] mb-1">
                Observación de aduana:
              </label>
              <textarea
                rows={2}
                placeholder="Ej: Aduana solicitó inspección física..."
                value={modalMotive}
                onChange={(e) => setModalMotive(e.target.value)}
                className="w-full p-2.5 rounded-xl border border-slate-300 text-xs text-[#2E2E2E]"
              />
            </div>

            <div className="flex justify-end gap-2 pt-2">
              <button
                type="button"
                onClick={() => setActiveModal(null)}
                className="px-4 py-2 rounded-xl text-xs font-bold text-[#6C6B6D] hover:bg-slate-100 cursor-pointer"
              >
                Cancelar
              </button>
              <button
                type="button"
                onClick={() => {
                  onUpdateCustomsSubstate(customsStateInput, modalMotive.trim() || undefined);
                  setActiveModal(null);
                }}
                className="px-5 py-2.5 rounded-xl text-xs font-black bg-[#FFD100] hover:bg-[#E6BC00] text-[#2E2E2E] cursor-pointer"
              >
                Actualizar Aduana
              </button>
            </div>
          </div>
        </div>
      )}

      {/* 6. Modal: RECHAZAR JUNTA */}
      {activeModal === 'rejectBoard' && (
        <div className="fixed inset-0 z-50 flex items-center justify-center p-4 bg-black/50 backdrop-blur-xs">
          <div className="bg-white rounded-3xl p-6 max-w-md w-full shadow-2xl border border-rose-200 space-y-4 animate-in fade-in">
            <h4 className="text-lg font-black text-[#2E2E2E]">Rechazar Aprobación de la Junta</h4>
            <p className="text-xs text-[#515151]">
              Ingresa el motivo formal del rechazo para notificar a Compras:
            </p>
            <textarea
              rows={3}
              required
              placeholder="Motivo formal de rechazo..."
              value={modalMotive}
              onChange={(e) => setModalMotive(e.target.value)}
              className="w-full p-2.5 rounded-xl border border-rose-300 text-xs text-[#2E2E2E]"
            />
            <div className="flex justify-end gap-2 pt-2">
              <button
                type="button"
                onClick={() => setActiveModal(null)}
                className="px-4 py-2 rounded-xl text-xs font-bold text-[#6C6B6D] hover:bg-slate-100"
              >
                Cancelar
              </button>
              <button
                type="button"
                disabled={!modalMotive.trim()}
                onClick={() => {
                  onRejectBoardFabrication(modalMotive.trim());
                  setActiveModal(null);
                }}
                className="px-5 py-2 rounded-xl text-xs font-bold bg-[#DC2626] text-white"
              >
                Confirmar Rechazo
              </button>
            </div>
          </div>
        </div>
      )}

      {/* Image preview modal */}
      {previewImage && (
        <div
          onClick={() => setPreviewImage(null)}
          className="fixed inset-0 z-50 flex items-center justify-center p-4 bg-black/70 backdrop-blur-xs cursor-pointer"
        >
          <div className="bg-white rounded-3xl p-6 max-w-lg w-full text-center space-y-4">
            <div className="flex items-center justify-between pb-2 border-b border-slate-100">
              <span className="font-bold text-xs text-[#2E2E2E]">{previewImage}</span>
              <button onClick={() => setPreviewImage(null)} className="p-1 text-[#6C6B6D]">
                <X className="w-5 h-5" />
              </button>
            </div>
            <div className="h-64 bg-slate-100 rounded-2xl flex items-center justify-center text-[#6C6B6D] font-bold text-xs">
              [Visualización de imagen de inspección / comprobante]
            </div>
            <p className="text-[11px] text-[#6C6B6D]">
              Hacer clic en cualquier parte para cerrar
            </p>
          </div>
        </div>
      )}
    </div>
  );
};

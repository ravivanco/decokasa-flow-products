import React, { useState } from 'react';
import { SolicitudProductoNuevo, Usuario } from '../types';
import {
  Sparkles,
  Plus,
  Check,
  X,
  FileText,
  Clock,
  ArrowRight,
  Send,
  Upload,
  AlertTriangle,
} from 'lucide-react';

interface NewProductsViewProps {
  productosNuevos: SolicitudProductoNuevo[];
  currentUser: Usuario;
  onUpdateProductoNuevo: (updated: SolicitudProductoNuevo) => void;
  onCrearSolicitudNuevo: (solicitud: SolicitudProductoNuevo) => void;
}

export const NewProductsView: React.FC<NewProductsViewProps> = ({
  productosNuevos,
  currentUser,
  onUpdateProductoNuevo,
  onCrearSolicitudNuevo,
}) => {
  const [selectedItem, setSelectedItem] = useState<SolicitudProductoNuevo>(productosNuevos[0]);
  const [showNewModal, setShowNewModal] = useState(false);
  const [newNombre, setNewNombre] = useState('');
  const [newOrigen, setNewOrigen] = useState<'Ecuador solicita' | 'China propone'>('Ecuador solicita');
  const [newVisita, setNewVisita] = useState(false);
  const [rejectReason, setRejectReason] = useState('');
  const [showRejectModal, setShowRejectModal] = useState(false);
  const [registerCodeInput, setRegisterCodeInput] = useState('');

  const pasos = [
    { num: 1, label: 'Origen' },
    { num: 2, label: 'Investigación' },
    { num: 3, label: 'Ficha + Catálogo' },
    { num: 4, label: 'Aprobación' },
    { num: 5, label: 'Codificación' },
    { num: 6, label: 'Listo para Pedido' },
  ];

  const handleAdvanceStep = () => {
    if (!selectedItem || selectedItem.pasoActual >= 6) return;
    const nextStep = selectedItem.pasoActual + 1;
    let nextState = selectedItem.estado;
    if (nextStep === 3) nextState = 'Ficha técnica';
    if (nextStep === 4) nextState = 'En aprobación';
    if (nextStep === 5) nextState = 'En codificación';
    if (nextStep === 6) nextState = 'Listo para pedido';

    const updated: SolicitudProductoNuevo = {
      ...selectedItem,
      pasoActual: nextStep,
      estado: nextState,
    };
    onUpdateProductoNuevo(updated);
    setSelectedItem(updated);
  };

  const handleRejectStep = () => {
    if (!selectedItem || !rejectReason.trim()) return;
    const updated: SolicitudProductoNuevo = {
      ...selectedItem,
      estado: 'Rechazado',
      motivoRechazo: rejectReason.trim(),
    };
    onUpdateProductoNuevo(updated);
    setSelectedItem(updated);
    setShowRejectModal(false);
  };

  const handleRegisterCode = () => {
    if (!selectedItem || !registerCodeInput.trim()) return;
    const updated: SolicitudProductoNuevo = {
      ...selectedItem,
      codigoRegistrado: registerCodeInput.trim().toUpperCase(),
      pasoActual: 6,
      estado: 'Listo para pedido',
    };
    onUpdateProductoNuevo(updated);
    setSelectedItem(updated);
  };

  const handleCreateNewSubmit = (e: React.FormEvent) => {
    e.preventDefault();
    if (!newNombre.trim()) return;

    const newProd: SolicitudProductoNuevo = {
      id: `pn-${Date.now()}`,
      codigo: `NPROD-2026-${String(productosNuevos.length + 1).padStart(3, '0')}`,
      nombre: newNombre.trim(),
      origen: newOrigen,
      pasoActual: 1,
      estado: 'En investigación',
      propuestoPor: currentUser.nombre,
      fechaSolicitud: '06/10/2026',
      fechaEstimadaFin: '15/11/2026',
      visitaFabricas: newVisita,
      documentos: [],
      observaciones: [
        {
          id: `obs-pn-${Date.now()}`,
          usuarioId: currentUser.id,
          usuarioNombre: currentUser.nombre,
          usuarioRol: currentUser.cargo,
          avatar: currentUser.avatar,
          fecha: '06/10/2026 10:00',
          texto: `Creación formal de solicitud de producto nuevo. Origen: ${newOrigen}.`,
        },
      ],
    };

    onCrearSolicitudNuevo(newProd);
    setSelectedItem(newProd);
    setShowNewModal(false);
    setNewNombre('');
  };

  return (
    <div className="space-y-6 sm:space-y-8 animate-in fade-in duration-200">
      {/* Top Banner */}
      <div className="flex flex-col sm:flex-row sm:items-center justify-between gap-4">
        <div>
          <h1 className="text-2xl sm:text-3xl font-black text-[#2E2E2E]">
            Gestión de Productos Nuevos
          </h1>
          <p className="text-xs sm:text-sm text-[#515151] mt-0.5">
            Flujo de 6 pasos para investigación, homologación de muestras y codificación de catálogo.
          </p>
        </div>

        <button
          type="button"
          onClick={() => setShowNewModal(true)}
          className="px-5 py-2.5 rounded-2xl font-black text-sm bg-[#FFD100] hover:bg-[#E6BC00] text-[#2E2E2E] flex items-center gap-2 shadow-sm cursor-pointer self-start sm:self-auto"
        >
          <Plus className="w-4 h-4 stroke-[3]" />
          <span>Nueva solicitud</span>
        </button>
      </div>

      {/* Main Grid: Sidebar list + Detail Panel */}
      <div className="grid grid-cols-1 lg:grid-cols-3 gap-6">
        {/* List of items */}
        <div className="bg-white rounded-3xl border border-[#6C6B6D]/20 p-5 shadow-xs space-y-3">
          <h3 className="text-xs font-black uppercase tracking-wider text-[#6C6B6D] px-2">
            Solicitudes en Proceso ({productosNuevos.length})
          </h3>
          <div className="space-y-2">
            {productosNuevos.map((p) => {
              const isSelected = selectedItem?.id === p.id;
              return (
                <div
                  key={p.id}
                  onClick={() => setSelectedItem(p)}
                  className={`p-4 rounded-2xl border transition cursor-pointer ${
                    isSelected
                      ? 'border-[#FFD100] bg-[#FFFBEA]'
                      : 'border-slate-200 hover:border-[#FFD100] bg-white'
                  }`}
                >
                  <div className="flex items-center justify-between text-xs">
                    <span className="font-mono font-bold text-[#2E2E2E]">{p.codigo}</span>
                    <span
                      className={`px-2 py-0.5 rounded-full text-[10px] font-black ${
                        p.estado === 'Rechazado'
                          ? 'bg-rose-100 text-[#DC2626]'
                          : p.estado === 'Listo para pedido'
                          ? 'bg-emerald-100 text-[#16A34A]'
                          : 'bg-[#FFD100] text-[#2E2E2E]'
                      }`}
                    >
                      {p.estado}
                    </span>
                  </div>
                  <h4 className="font-black text-xs text-[#2E2E2E] mt-1 line-clamp-2">
                    {p.nombre}
                  </h4>
                  <span className="text-[10px] text-[#6C6B6D] block mt-1">
                    Origen: {p.origen} · {p.propuestoPor}
                  </span>
                </div>
              );
            })}
          </div>
        </div>

        {/* Selected Item Detail */}
        {selectedItem && (
          <div className="lg:col-span-2 space-y-6">
            <div className="bg-white rounded-3xl border border-[#6C6B6D]/20 p-6 sm:p-8 shadow-xs space-y-6">
              <div className="flex flex-col sm:flex-row sm:items-center justify-between gap-3 border-b border-slate-100 pb-4">
                <div>
                  <span className="font-mono text-xs font-black px-2.5 py-0.5 rounded-md bg-[#FFD100] text-[#2E2E2E]">
                    {selectedItem.codigo}
                  </span>
                  <h2 className="text-xl font-black text-[#2E2E2E] mt-1.5">
                    {selectedItem.nombre}
                  </h2>
                  <p className="text-xs text-[#515151] mt-0.5">
                    Propuesto por: {selectedItem.propuestoPor} · Fecha: {selectedItem.fechaSolicitud}
                  </p>
                </div>

                <div className="text-right">
                  <span className="text-xs font-bold text-[#6C6B6D] block">Origen:</span>
                  <strong className="text-xs font-black text-[#2E2E2E]">{selectedItem.origen}</strong>
                </div>
              </div>

              {/* 6-step horizontal workflow */}
              <div className="space-y-2">
                <span className="text-xs font-black uppercase text-[#2E2E2E] block">
                  Línea de Proceso de Producto Nuevo (6 Pasos)
                </span>
                <div className="grid grid-cols-6 gap-2 text-center text-xs">
                  {pasos.map((p) => {
                    const isDone = p.num < selectedItem.pasoActual;
                    const isCurrent = p.num === selectedItem.pasoActual;
                    return (
                      <div
                        key={p.num}
                        className={`p-2.5 rounded-2xl border text-[11px] font-black transition ${
                          selectedItem.estado === 'Rechazado' && isCurrent
                            ? 'bg-rose-50 border-[#DC2626] text-[#DC2626]'
                            : isDone
                            ? 'bg-[#FFD100] border-[#FFD100] text-[#2E2E2E]'
                            : isCurrent
                            ? 'bg-[#FFFBEA] border-2 border-[#2E2E2E] text-[#2E2E2E]'
                            : 'bg-slate-50 border-slate-200 text-[#6C6B6D]'
                        }`}
                      >
                        <span className="block text-sm mb-0.5">{p.num}</span>
                        <span className="truncate block">{p.label}</span>
                      </div>
                    );
                  })}
                </div>
              </div>

              {/* Rejection notice if any */}
              {selectedItem.estado === 'Rechazado' && (
                <div className="p-4 rounded-2xl bg-rose-50 border border-rose-200 text-xs text-[#DC2626] font-bold space-y-1">
                  <span>❌ Solicitud Rechazada en etapa de Aprobación por Compras Ecuador</span>
                  <p className="font-medium text-[#515151]">
                    Motivo registrado: &ldquo;{selectedItem.motivoRechazo}&rdquo;
                  </p>
                </div>
              )}

              {/* Step context explanation */}
              <div className="p-4 rounded-2xl bg-[#FFFBEA] border border-[#FFD100]/40 text-xs text-[#515151] space-y-2">
                <strong className="text-[#2E2E2E] block font-black">
                  Paso activo actual: {selectedItem.pasoActual} de 6 — {pasos[selectedItem.pasoActual - 1]?.label}
                </strong>
                {selectedItem.pasoActual === 2 && (
                  <p>
                    Compras Ecuador investiga la viabilidad comercial (plazo 7 días hábiles; +10 días si se realiza visita a fábricas en China).
                  </p>
                )}
                {selectedItem.pasoActual === 3 && (
                  <p>
                    Compras China (David Tulcán) sube en conjunto la Ficha Técnica y el Catálogo de colores y diseños.
                  </p>
                )}
                {selectedItem.pasoActual === 4 && (
                  <p>
                    Aprobación formal por Compras Ecuador (Marcela Catucuamba, plazo 10 días hábiles). Si se rechaza, es obligatorio registrar el motivo.
                  </p>
                )}
                {selectedItem.pasoActual === 5 && (
                  <p>
                    Marketing / Sistemas (Andrea Quishpe) registra el código de producto definitivo de Decokasa (plazo 4 días hábiles). Nota: El código se REGISTRA, no se genera.
                  </p>
                )}
                {selectedItem.pasoActual === 6 && (
                  <p className="text-[#16A34A] font-bold">
                    ✓ Producto homologado, codificado con éxito ({selectedItem.codigoRegistrado || 'Asignado'}) y listo para incluirse en pedidos regulares.
                  </p>
                )}
              </div>

              {/* Actions according to step */}
              {selectedItem.estado !== 'Rechazado' && selectedItem.pasoActual < 6 && (
                <div className="flex items-center gap-3 pt-2 border-t border-slate-100">
                  {selectedItem.pasoActual === 4 ? (
                    <>
                      <button
                        type="button"
                        onClick={handleAdvanceStep}
                        className="px-5 py-2.5 rounded-xl font-black text-xs bg-[#16A34A] hover:bg-emerald-700 text-white shadow-xs cursor-pointer"
                      >
                        Aprobar producto (Paso a Codificación)
                      </button>
                      <button
                        type="button"
                        onClick={() => {
                          setRejectReason('');
                          setShowRejectModal(true);
                        }}
                        className="px-4 py-2.5 rounded-xl font-bold text-xs bg-rose-50 hover:bg-rose-100 text-[#DC2626] border border-rose-200 cursor-pointer"
                      >
                        Rechazar solicitud
                      </button>
                    </>
                  ) : selectedItem.pasoActual === 5 ? (
                    <div className="flex items-center gap-2 w-full max-w-md">
                      <input
                        type="text"
                        placeholder="Registrar código manual (ej: PAN-ACUST-PET-26)..."
                        value={registerCodeInput}
                        onChange={(e) => setRegisterCodeInput(e.target.value)}
                        className="flex-1 p-2 rounded-xl border border-slate-300 font-mono text-xs"
                      />
                      <button
                        type="button"
                        disabled={!registerCodeInput.trim()}
                        onClick={handleRegisterCode}
                        className="px-4 py-2 rounded-xl font-black text-xs bg-[#FFD100] text-[#2E2E2E] disabled:opacity-50 cursor-pointer"
                      >
                        Registrar Código
                      </button>
                    </div>
                  ) : (
                    <button
                      type="button"
                      onClick={handleAdvanceStep}
                      className="px-5 py-2.5 rounded-xl font-black text-xs bg-[#FFD100] hover:bg-[#E6BC00] text-[#2E2E2E] flex items-center gap-2 shadow-xs cursor-pointer"
                    >
                      <span>Avanzar al siguiente paso ({selectedItem.pasoActual + 1})</span>
                      <ArrowRight className="w-3.5 h-3.5 stroke-[3]" />
                    </button>
                  )}
                </div>
              )}

              {/* Documents attached */}
              <div className="space-y-2 pt-2 border-t border-slate-100">
                <span className="text-xs font-black uppercase text-[#2E2E2E] block">
                  Documentos del Producto ({selectedItem.documentos.length})
                </span>
                <div className="space-y-1.5">
                  {selectedItem.documentos.map((d, dIdx) => (
                    <div
                      key={dIdx}
                      className="p-3 rounded-xl bg-slate-50 border border-slate-200 text-xs flex items-center justify-between"
                    >
                      <div className="flex items-center gap-2 font-bold text-[#2E2E2E]">
                        <FileText className="w-4 h-4 text-[#515151]" />
                        <span>{d.nombre}</span>
                      </div>
                      <span className="text-[11px] text-[#6C6B6D]">
                        {d.subidoPor} · {d.fecha}
                      </span>
                    </div>
                  ))}
                </div>
              </div>
            </div>
          </div>
        )}
      </div>

      {/* Modal: Nueva solicitud */}
      {showNewModal && (
        <div className="fixed inset-0 z-50 flex items-center justify-center p-4 bg-black/50 backdrop-blur-xs">
          <div className="bg-white rounded-3xl max-w-md w-full p-6 space-y-4 animate-in fade-in border border-[#FFD100]">
            <h3 className="text-lg font-black text-[#2E2E2E]">Nueva Solicitud de Producto</h3>
            <form onSubmit={handleCreateNewSubmit} className="space-y-3 text-xs">
              <div>
                <label className="block font-bold text-[#2E2E2E] mb-1">Nombre del producto:</label>
                <input
                  type="text"
                  required
                  placeholder="Ej: Malla de Acero Decorativa Revestida..."
                  value={newNombre}
                  onChange={(e) => setNewNombre(e.target.value)}
                  className="w-full p-2.5 rounded-xl border border-slate-300"
                />
              </div>

              <div>
                <label className="block font-bold text-[#2E2E2E] mb-1">Origen de la propuesta:</label>
                <select
                  value={newOrigen}
                  onChange={(e) => setNewOrigen(e.target.value as any)}
                  className="w-full p-2.5 rounded-xl border border-slate-300 font-bold"
                >
                  <option value="Ecuador solicita">Ecuador solicita (Compras Ecuador)</option>
                  <option value="China propone">China propone (Compras China)</option>
                </select>
              </div>

              <label className="flex items-center gap-2 p-3 rounded-xl border border-slate-200 bg-slate-50 cursor-pointer">
                <input
                  type="checkbox"
                  checked={newVisita}
                  onChange={(e) => setNewVisita(e.target.checked)}
                  className="w-4 h-4 rounded text-[#FFD100]"
                />
                <span className="font-bold text-[#2E2E2E]">
                  ¿Requiere visita presencial a fábricas en China? (+10 días hábiles)
                </span>
              </label>

              <div className="flex justify-end gap-2 pt-2">
                <button
                  type="button"
                  onClick={() => setShowNewModal(false)}
                  className="px-4 py-2 rounded-xl font-bold text-[#6C6B6D] hover:bg-slate-100"
                >
                  Cancelar
                </button>
                <button
                  type="submit"
                  className="px-5 py-2.5 rounded-xl font-black bg-[#FFD100] text-[#2E2E2E]"
                >
                  Crear Solicitud
                </button>
              </div>
            </form>
          </div>
        </div>
      )}

      {/* Modal: Rechazo con motivo obligatorio */}
      {showRejectModal && (
        <div className="fixed inset-0 z-50 flex items-center justify-center p-4 bg-black/50 backdrop-blur-xs">
          <div className="bg-white rounded-3xl max-w-md w-full p-6 space-y-4 animate-in fade-in border border-rose-300">
            <h3 className="text-lg font-black text-[#DC2626]">Rechazar Solicitud de Producto</h3>
            <p className="text-xs text-[#515151]">
              Indica el motivo obligatorio del rechazo para archivar la solicitud:
            </p>
            <textarea
              rows={3}
              required
              placeholder="Ej: Costo de flete no hace rentable la importación..."
              value={rejectReason}
              onChange={(e) => setRejectReason(e.target.value)}
              className="w-full p-2.5 rounded-xl border border-rose-300 text-xs"
            />
            <div className="flex justify-end gap-2 pt-2">
              <button
                type="button"
                onClick={() => setShowRejectModal(false)}
                className="px-4 py-2 rounded-xl text-xs font-bold text-[#6C6B6D] hover:bg-slate-100"
              >
                Cancelar
              </button>
              <button
                type="button"
                disabled={!rejectReason.trim()}
                onClick={handleRejectStep}
                className="px-5 py-2 rounded-xl text-xs font-black bg-[#DC2626] text-white"
              >
                Confirmar Rechazo
              </button>
            </div>
          </div>
        </div>
      )}
    </div>
  );
};

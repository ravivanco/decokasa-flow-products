import React, { useState } from 'react';
import { Usuario } from '../types';
import { ETAPAS_BASE_CONFIG } from '../data/initialData';
import { Settings, Clock, Save, ShieldAlert, CheckCircle2, RotateCcw } from 'lucide-react';

interface ConfigurationViewProps {
  currentUser: Usuario;
  onShowToast: (msg: string) => void;
}

export const ConfigurationView: React.FC<ConfigurationViewProps> = ({
  currentUser,
  onShowToast,
}) => {
  const [stages, setStages] = useState(
    ETAPAS_BASE_CONFIG.map((e) => ({
      numero: e.numero,
      nombre: e.nombre,
      plazoOriginal: e.plazoDiasHabiles,
      plazoDias: e.plazoDiasHabiles,
      grupo: e.grupo,
      responsable: e.responsableTexto,
    }))
  );

  const isAdmin = currentUser.rol === 'admin';

  const handleDayChange = (numero: number, days: number) => {
    if (!isAdmin) return;
    setStages((prev) =>
      prev.map((s) => (s.numero === numero ? { ...s, plazoDias: Math.max(0, days) } : s))
    );
  };

  const handleSave = () => {
    onShowToast('Configuración de plazos guardada con éxito (Simulación en memoria)');
  };

  const handleReset = () => {
    setStages((prev) =>
      prev.map((s) => ({ ...s, plazoDias: s.plazoOriginal }))
    );
    onShowToast('Plazos restaurados a los valores estándar de Decokasa');
  };

  return (
    <div className="space-y-6 sm:space-y-8 animate-in fade-in duration-200">
      <div className="flex flex-col sm:flex-row sm:items-center justify-between gap-4">
        <div>
          <h1 className="text-2xl sm:text-3xl font-black text-[#2E2E2E]">
            Configuración de Plazos y Reglas
          </h1>
          <p className="text-xs sm:text-sm text-[#515151] mt-0.5">
            Plazos estándar en días hábiles (lunes a viernes). Los cambios recalculan los cronogramas automáticos.
          </p>
        </div>

        {isAdmin && (
          <div className="flex items-center gap-2">
            <button
              type="button"
              onClick={handleReset}
              className="px-4 py-2 rounded-xl text-xs font-black bg-white border border-[#6C6B6D]/30 text-[#2E2E2E] hover:bg-slate-50 flex items-center gap-1.5 shadow-2xs cursor-pointer"
            >
              <RotateCcw className="w-3.5 h-3.5 text-[#6C6B6D]" />
              <span>Restablecer</span>
            </button>
            <button
              type="button"
              onClick={handleSave}
              className="px-4 py-2 rounded-xl text-xs font-black bg-[#FFD100] hover:bg-[#E6BC00] text-[#2E2E2E] flex items-center gap-1.5 shadow-2xs cursor-pointer"
            >
              <Save className="w-3.5 h-3.5" />
              <span>Guardar Plazos</span>
            </button>
          </div>
        )}
      </div>

      {!isAdmin && (
        <div className="p-4 rounded-2xl bg-amber-50 border border-amber-200 text-xs text-amber-800 flex items-center gap-2">
          <ShieldAlert className="w-4 h-4 shrink-0" />
          <span>
            Solo el Administrador de Sistemas (<strong className="font-bold">Mirian Franco</strong>) puede modificar los plazos base de las etapas. Estás en modo de solo lectura.
          </span>
        </div>
      )}

      {/* Table of Stages */}
      <div className="bg-white rounded-3xl border border-[#6C6B6D]/20 shadow-xs overflow-hidden">
        <div className="overflow-x-auto">
          <table className="w-full text-left border-collapse">
            <thead>
              <tr className="bg-slate-50 border-b border-slate-200 text-[11px] font-black uppercase tracking-wider text-[#6C6B6D]">
                <th className="py-3 px-4">#</th>
                <th className="py-3 px-4">Nombre de la Etapa</th>
                <th className="py-3 px-4">Fase / Grupo</th>
                <th className="py-3 px-4">Responsable Operativo</th>
                <th className="py-3 px-4 text-center">Plazo Días Hábiles</th>
                <th className="py-3 px-4 text-right">Reglas Adicionales</th>
              </tr>
            </thead>
            <tbody className="divide-y divide-slate-100 text-xs">
              {stages.map((stage) => (
                <tr key={stage.numero} className="hover:bg-slate-50/70 transition">
                  <td className="py-3 px-4 font-black text-[#2E2E2E]">
                    {stage.numero}
                  </td>
                  <td className="py-3 px-4 font-bold text-[#2E2E2E]">
                    {stage.nombre}
                  </td>
                  <td className="py-3 px-4 text-[#515151]">
                    <span className="px-2 py-0.5 rounded-md bg-slate-100 text-[10px] font-black uppercase text-[#6C6B6D]">
                      {stage.grupo}
                    </span>
                  </td>
                  <td className="py-3 px-4 text-[#515151]">
                    {stage.responsable}
                  </td>
                  <td className="py-3 px-4 text-center">
                    {isAdmin && stage.numero !== 1 ? (
                      <input
                        type="number"
                        min="1"
                        max="90"
                        value={stage.plazoDias}
                        onChange={(e) => handleDayChange(stage.numero, parseInt(e.target.value) || 0)}
                        className="w-16 py-1 px-2 border border-[#6C6B6D]/30 rounded-lg text-center font-black text-sm text-[#2E2E2E] focus:outline-hidden focus:ring-2 focus:ring-[#FFD100]"
                      />
                    ) : (
                      <span className="font-mono font-black text-sm text-[#2E2E2E]">
                        {stage.plazoDias} {stage.plazoDias === 1 ? 'día' : 'días'}
                      </span>
                    )}
                  </td>
                  <td className="py-3 px-4 text-right text-[11px] text-[#6C6B6D]">
                    {stage.numero === 2 && '+10 días si tiene producto nuevo'}
                    {stage.numero === 4 && 'Ampliable hasta +10 días con motivo'}
                    {stage.numero === 9 && 'Subestados: En trámite / Retenido / Liberado'}
                    {stage.numero === 10 && 'Bodega por defecto: Chongón (Quito/Ibarra)'}
                    {stage.numero !== 2 && stage.numero !== 4 && stage.numero !== 9 && stage.numero !== 10 && 'Estándar'}
                  </td>
                </tr>
              ))}
            </tbody>
          </table>
        </div>
      </div>
    </div>
  );
};

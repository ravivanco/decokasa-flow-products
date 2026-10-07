import React from 'react';
import { HelpCircle, BookOpen, Clock, AlertTriangle, ShieldCheck, Ship, CheckCircle2 } from 'lucide-react';

export const HelpView: React.FC = () => {
  return (
    <div className="space-y-6 sm:space-y-8 animate-in fade-in duration-200">
      <div>
        <h1 className="text-2xl sm:text-3xl font-black text-[#2E2E2E]">
          Manual y Guía de Operación
        </h1>
        <p className="text-xs sm:text-sm text-[#515151] mt-0.5">
          Reglas del negocio, cálculo de plazos en días hábiles y responsabilidades por etapa en Decokasa Tracking.
        </p>
      </div>

      <div className="grid grid-cols-1 md:grid-cols-2 gap-6">
        {/* Regla 1: Días hábiles */}
        <div className="bg-white rounded-3xl p-6 border border-[#6C6B6D]/20 shadow-xs space-y-3">
          <div className="w-10 h-10 rounded-xl bg-[#FFF6CC] text-[#2E2E2E] flex items-center justify-center">
            <Clock className="w-5 h-5 text-[#2E2E2E]" />
          </div>
          <h3 className="text-base font-black text-[#2E2E2E]">
            1. Cálculo de Plazos en Días Hábiles
          </h3>
          <p className="text-xs text-[#515151] leading-relaxed">
            Todos los plazos del sistema se computan exclusivamente de <strong>lunes a viernes</strong>, excluyendo sábados y domingos. La fecha de referencia actual es el <strong>06/10/2026</strong>. Si una etapa termina con atraso, la fecha estimada de las siguientes se recalcula en cascada de forma automática.
          </p>
        </div>

        {/* Regla 2: Semáforo y Alertas */}
        <div className="bg-white rounded-3xl p-6 border border-[#6C6B6D]/20 shadow-xs space-y-3">
          <div className="w-10 h-10 rounded-xl bg-orange-100 text-orange-600 flex items-center justify-center">
            <AlertTriangle className="w-5 h-5" />
          </div>
          <h3 className="text-base font-black text-[#2E2E2E]">
            2. Semáforo Dinámico de 4 Estados
          </h3>
          <ul className="text-xs text-[#515151] space-y-1.5 leading-relaxed">
            <li className="flex items-center gap-2">
              <span className="w-2.5 h-2.5 rounded-full bg-[#16A34A] shrink-0" />
              <span><strong>A tiempo (Verde):</strong> Más de 3 días hábiles restantes.</span>
            </li>
            <li className="flex items-center gap-2">
              <span className="w-2.5 h-2.5 rounded-full bg-[#EA580C] shrink-0" />
              <span><strong>Por vencer (Naranja 🕒):</strong> Faltan 3 días hábiles o menos.</span>
            </li>
            <li className="flex items-center gap-2">
              <span className="w-2.5 h-2.5 rounded-full bg-[#DC2626] shrink-0" />
              <span><strong>Retrasado (Rojo ⚠):</strong> Plazo vencido o retenido en aduana.</span>
            </li>
            <li className="flex items-center gap-2">
              <span className="w-2.5 h-2.5 rounded-full bg-[#6C6B6D] shrink-0" />
              <span><strong>Pausado / Cancelado (Gris ⏸):</strong> Detenido por Junta o Administrador.</span>
            </li>
          </ul>
        </div>

        {/* Regla 3: Aprobación y Reversiones */}
        <div className="bg-white rounded-3xl p-6 border border-[#6C6B6D]/20 shadow-xs space-y-3">
          <div className="w-10 h-10 rounded-xl bg-blue-100 text-blue-600 flex items-center justify-center">
            <ShieldCheck className="w-5 h-5" />
          </div>
          <h3 className="text-base font-black text-[#2E2E2E]">
            3. Checks, Observaciones y Reversión
          </h3>
          <p className="text-xs text-[#515151] leading-relaxed">
            Solo el rol autorizado puede marcar el check de su etapa cuando se cumplan los checklists y se suban los documentos requeridos. Si un pedido tiene inconsistencias, se puede <strong>Devolver con observaciones</strong> o <strong>Revertir el check</strong> ingresando obligatoriamente un motivo en la auditoría.
          </p>
        </div>

        {/* Regla 4: Fabricación y Aduana */}
        <div className="bg-white rounded-3xl p-6 border border-[#6C6B6D]/20 shadow-xs space-y-3">
          <div className="w-10 h-10 rounded-xl bg-[#FFD100] text-[#2E2E2E] flex items-center justify-center">
            <Ship className="w-5 h-5" />
          </div>
          <h3 className="text-base font-black text-[#2E2E2E]">
            4. Fabricación, Aduana y Bodega
          </h3>
          <p className="text-xs text-[#515151] leading-relaxed">
            - <strong>Fabricación:</strong> 35 días hábiles. Compras China puede ampliar hasta +10 días adicionales con justificación.<br />
            - <strong>Aduana:</strong> 15 días hábiles con subestados SENAE (En trámite, Retenido, Liberado).<br />
            - <strong>Bodega:</strong> 5 días hábiles con recepción física e ingreso de cantidades recibidas contra pedidas.
          </p>
        </div>
      </div>
    </div>
  );
};

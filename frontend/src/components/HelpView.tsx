import React from 'react';
import { HelpCircle, Clock, AlertTriangle, ShieldCheck, Ship, CheckCircle2, FileText, Sparkles, Warehouse, Mail } from 'lucide-react';

export const HelpView: React.FC = () => {
  return (
    <div className="space-y-6 sm:space-y-8 animate-in fade-in duration-200">
      <div>
        <h1 className="text-2xl sm:text-3xl font-black text-[#2E2E2E]">
          Documento de Requerimientos y Reglas del Sistema
        </h1>
        <p className="text-xs sm:text-sm text-[#515151] mt-0.5">
          Resumen ejecutivo del levantamiento de requerimientos de DECOKASA S.A.S. (Octubre 2026).
        </p>
      </div>

      <div className="grid grid-cols-1 md:grid-cols-2 gap-6">
        {/* Flujo A vs Flujo B */}
        <div className="bg-white rounded-3xl p-6 border border-[#6C6B6D]/20 shadow-xs space-y-3">
          <div className="w-10 h-10 rounded-xl bg-[#FFF6CC] text-[#2E2E2E] flex items-center justify-center font-black">
            <Ship className="w-5 h-5" />
          </div>
          <h3 className="text-base font-black text-[#2E2E2E]">
            1. Dos Flujos de Importación (A y B)
          </h3>
          <p className="text-xs text-[#515151] leading-relaxed">
            - <strong>Flujo A (11 etapas)</strong>: Inicia en Ecuador. Mirian Franco (Admin) crea la solicitud para productos existentes o pedidos por Ecuador.<br />
            - <strong>Flujo B (12 etapas)</strong>: Inicia en China. David Túlcan propone un producto nuevo desde China (Etapa 1), que Marcela Catucuamba revisa en la Etapa 2 antes de entrar a Codificación.<br />
            - En ambos flujos, la última etapa (<strong>Fin</strong>) es automática: se marca sola al dar check en Incidencias.
          </p>
        </div>

        {/* Responsables */}
        <div className="bg-white rounded-3xl p-6 border border-[#6C6B6D]/20 shadow-xs space-y-3">
          <div className="w-10 h-10 rounded-xl bg-purple-100 text-purple-800 flex items-center justify-center font-black">
            <ShieldCheck className="w-5 h-5" />
          </div>
          <h3 className="text-base font-black text-[#2E2E2E]">
            2. Responsables y Ubicación Geográfica
          </h3>
          <ul className="text-xs text-[#515151] space-y-1 leading-relaxed">
            <li><strong>Mirian Franco (Ecuador)</strong>: Admin, control de todo el sistema. Puede actuar o reasignar en cualquier etapa.</li>
            <li><strong>Andrea Quishpe (Ecuador)</strong>: Marketing. Codificación (A2 / B3). Plazo: 24 h.</li>
            <li><strong>Marcela Catucuamba (Ecuador)</strong>: Asistente de compras. Revisión y aprobación (A3 / B4) y Revisión de productos propuestos (B2). Plazo: 24 h.</li>
            <li><strong>David Túlcan (China)</strong>: Compras China. Análisis (3 días), Fabricación, Embarque (10 días) y Aduana. Crea Flujo B.</li>
            <li><strong>Anderson Enriquez (Ecuador)</strong>: Logística. Salida aduana (24 h), Bodega (24 h), Incidencias (24 h). Mantiene catálogo de bodegas.</li>
            <li><strong>Andrea Collaguazo (China)</strong>: Asistente en China. Consulta y seguimiento.</li>
          </ul>
        </div>

        {/* Urgencias y Devoluciones */}
        <div className="bg-white rounded-3xl p-6 border border-[#6C6B6D]/20 shadow-xs space-y-3">
          <div className="w-10 h-10 rounded-xl bg-red-100 text-red-600 flex items-center justify-center font-black">
            <AlertTriangle className="w-5 h-5" />
          </div>
          <h3 className="text-base font-black text-[#2E2E2E]">
            3. Devoluciones a Codificación y Urgencias
          </h3>
          <p className="text-xs text-[#515151] leading-relaxed">
            - Marcela puede desaprobar (✗) en Revisión hacia Codificación con observaciones.<br />
            - David en Análisis puede devolver por error a Codificación, o devolver si China propone productos nuevos no codificados con urgencia:<br />
            &nbsp;&nbsp;• <strong>Urgente</strong>: 4 horas hábiles.<br />
            &nbsp;&nbsp;• <strong>Prioritario</strong>: 12 horas hábiles.<br />
            &nbsp;&nbsp;• <strong>Normal</strong>: 24 horas hábiles.<br />
            Ese plazo cubre Codificación y Revisión hasta llegar de nuevo a Análisis en China.
          </p>
        </div>

        {/* Cronograma y Reglas */}
        <div className="bg-white rounded-3xl p-6 border border-[#6C6B6D]/20 shadow-xs space-y-3">
          <div className="w-10 h-10 rounded-xl bg-amber-100 text-amber-800 flex items-center justify-center font-black">
            <Clock className="w-5 h-5" />
          </div>
          <h3 className="text-base font-black text-[#2E2E2E]">
            4. Reglas del Cronograma y Días Hábiles
          </h3>
          <p className="text-xs text-[#515151] leading-relaxed">
            - <strong>Días hábiles y laborables</strong>: Excluyen sábados y domingos (hora de Ecuador).<br />
            - <strong>Fin de semana pasa a lunes</strong>: Cualquier fecha que caiga en sábado o domingo se mueve al lunes, incluida la llegada del barco.<br />
            - <strong>Recálculo en cascada</strong>: Un atraso o demora en aduana arrastra todas las fechas posteriores.<br />
            - <strong>Meta de 90 días</strong>: Configurable frente a los 99 días estándar planificados.
          </p>
        </div>

        {/* Google Drive y Alertas Gmail */}
        <div className="bg-white rounded-3xl p-6 border border-[#6C6B6D]/20 shadow-xs space-y-3">
          <div className="w-10 h-10 rounded-xl bg-blue-100 text-blue-800 flex items-center justify-center font-black">
            <Mail className="w-5 h-5" />
          </div>
          <h3 className="text-base font-black text-[#2E2E2E]">
            5. Google Drive y Alertas por Gmail
          </h3>
          <p className="text-xs text-[#515151] leading-relaxed">
            - <strong>Google Drive (límite 100 MB)</strong>: Copia en segundo plano desde el servidor con cuenta de servicio de Google Workspace. David en China nunca conecta directamente con dominios de Google.<br />
            - <strong>Avisos por Gmail</strong>: En cada etapa y en cada vencimiento se envía un correo al Administrador y al responsable con el motivo visible.
          </p>
        </div>

        {/* Número de Pedido y Bodegas */}
        <div className="bg-white rounded-3xl p-6 border border-[#6C6B6D]/20 shadow-xs space-y-3">
          <div className="w-10 h-10 rounded-xl bg-emerald-100 text-emerald-800 flex items-center justify-center font-black">
            <Warehouse className="w-5 h-5" />
          </div>
          <h3 className="text-base font-black text-[#2E2E2E]">
            6. Número de Pedido y Bodegas
          </h3>
          <p className="text-xs text-[#515151] leading-relaxed">
            - <strong>Número de Pedido Secuencial</strong>: Formato profesional <code>DK-EC-2026-0001</code> (Empresa, País, Año, Secuencia).<br />
            - <strong>Catálogo de Bodegas</strong>: Mantenido por Anderson Enriquez. En la etapa de Bodega se selecciona la bodega de destino (Chongón, Quito, Ibarra, Manta, Cuenca...).<br />
            - <strong>Incidencias</strong>: Anderson sube el expediente completo y da el check que finaliza el pedido.
          </p>
        </div>
      </div>
    </div>
  );
};

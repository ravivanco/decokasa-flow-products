import React, { useState } from 'react';
import {
  parseDate,
  formatDisplayDateWithDay,
  formatDisplayDate,
  addCalendarDaysWithWeekendRoll,
  formatDateISO,
  SIMULATED_TODAY,
} from '../utils/dateUtils';
import { Clock, Copy, Check, AlertTriangle, Sparkles, CheckCircle2, ShieldCheck, ArrowRight } from 'lucide-react';

interface TimelineCalculatorViewProps {
  onShowToast: (msg: string) => void;
}

export const TimelineCalculatorView: React.FC<TimelineCalculatorViewProps> = ({ onShowToast }) => {
  const [fechaPedido, setFechaPedido] = useState<string>(SIMULATED_TODAY);
  const [metaDias, setMetaDias] = useState<number>(90);
  const [copiedSlack, setCopiedSlack] = useState(false);
  const [copiedDates, setCopiedDates] = useState(false);

  // Etapas estándar de la calculadora existente de Decokasa
  const duraciones = [
    { etapa: '1. Pedido', dias: 0, desc: 'Fecha base del requerimiento' },
    { etapa: '2. Revisión', dias: 3, desc: 'Revisión y checklist de compras' },
    { etapa: '3. Inicio de fabricación', dias: 8, desc: 'Coordinación con fábrica en China' },
    { etapa: '4. Fin de fabricación', dias: 31, desc: 'Producción y control de calidad' },
    { etapa: '5. Embarque', dias: 12, desc: 'Búsqueda de naviera, Packing List y BL' },
    { etapa: '6. Llegada a Ecuador', dias: 45, desc: 'Tránsito marítimo altamar a puerto' },
  ];

  // Cálculo encadenado de la calculadora
  let currentStart = fechaPedido;
  let totalDuracionBase = 0;
  let totalAjustesFinDeSemana = 0;

  const resultadoCalculado = duraciones.map((item, idx) => {
    if (idx === 0) {
      return {
        etapa: item.etapa,
        diasSumados: 0,
        fechaCalculada: currentStart,
        ajusteTexto: 'ninguno',
      };
    }

    totalDuracionBase += item.dias;
    const calc = addCalendarDaysWithWeekendRoll(currentStart, item.dias);
    totalAjustesFinDeSemana += calc.ajusteDias;
    currentStart = calc.fechaFinal;

    return {
      etapa: item.etapa,
      diasSumados: item.dias,
      fechaCalculada: calc.fechaFinal,
      ajusteTexto: calc.ajusteDias > 0 ? `+${calc.ajusteDias} días (fin de semana pasa a lunes)` : 'ninguno',
    };
  });

  const totalDiasAcumulados = totalDuracionBase + totalAjustesFinDeSemana;
  const diferenciaContraMeta = totalDiasAcumulados - metaDias;

  // Generador de comandos Slack /remind que usaba la calculadora
  const slackRemindersText = resultadoCalculado
    .slice(1)
    .map((r, i) => {
      return `/remind #imp2026-general "Revisar hito: ${r.etapa} de importación Decokasa" ${formatDisplayDate(r.fechaCalculada)} at 9:00AM`;
    })
    .join('\n');

  const datesClipboardText = resultadoCalculado
    .map((r) => `${r.etapa}: ${formatDisplayDate(r.fechaCalculada)}`)
    .join('\n');

  const handleCopySlack = () => {
    navigator.clipboard?.writeText(slackRemindersText);
    setCopiedSlack(true);
    onShowToast('Comandos de Slack copiados al portapapeles');
    setTimeout(() => setCopiedSlack(false), 3000);
  };

  const handleCopyDates = () => {
    navigator.clipboard?.writeText(datesClipboardText);
    setCopiedDates(true);
    onShowToast('Fechas planificadas copiadas');
    setTimeout(() => setCopiedDates(false), 3000);
  };

  return (
    <div className="space-y-6 sm:space-y-8 animate-in fade-in duration-200">
      {/* Header */}
      <div className="flex flex-col sm:flex-row sm:items-center justify-between gap-4">
        <div>
          <h1 className="text-2xl sm:text-3xl font-black text-[#2E2E2E]">
            Calculadora de Cronograma & Meta de 90 Días
          </h1>
          <p className="text-xs sm:text-sm text-[#515151] mt-0.5">
            Módulo que reemplaza la calculadora externa y los recordatorios manuales de Slack con trazabilidad en vivo.
          </p>
        </div>

        <div className="flex items-center gap-2">
          <button
            type="button"
            onClick={handleCopyDates}
            className="px-3.5 py-2 rounded-xl bg-white border border-[#6C6B6D]/30 hover:bg-slate-50 text-xs font-black text-[#2E2E2E] shadow-2xs flex items-center gap-1.5 cursor-pointer"
          >
            {copiedDates ? <Check className="w-3.5 h-3.5 text-emerald-600" /> : <Copy className="w-3.5 h-3.5" />}
            <span>Copiar Fechas Planificadas</span>
          </button>
          <button
            type="button"
            onClick={handleCopySlack}
            className="px-3.5 py-2 rounded-xl bg-[#FFD100] hover:bg-[#E6BC00] text-xs font-black text-[#2E2E2E] shadow-2xs flex items-center gap-1.5 cursor-pointer"
          >
            {copiedSlack ? <Check className="w-3.5 h-3.5 text-emerald-600" /> : <Copy className="w-3.5 h-3.5" />}
            <span>Copiar Comandos Slack</span>
          </button>
        </div>
      </div>

      {/* Controles interactivos: Fecha de Pedido y Meta */}
      <div className="bg-white rounded-3xl p-6 border border-[#6C6B6D]/20 shadow-xs grid grid-cols-1 md:grid-cols-3 gap-4">
        <div>
          <label className="block text-xs font-black uppercase text-[#515151] mb-1.5">
            Fecha del Pedido (Fecha Base):
          </label>
          <input
            type="date"
            value={fechaPedido}
            onChange={(e) => setFechaPedido(e.target.value)}
            className="w-full px-3.5 py-2.5 rounded-xl border border-[#6C6B6D]/30 font-mono font-bold text-xs text-[#2E2E2E]"
          />
          <p className="text-[11px] text-[#6C6B6D] mt-1">
            Fecha de inicio del ciclo de importación marítima.
          </p>
        </div>

        <div>
          <label className="block text-xs font-black uppercase text-[#515151] mb-1.5">
            Meta Corporativa de Días:
          </label>
          <input
            type="number"
            min="60"
            max="150"
            value={metaDias}
            onChange={(e) => setMetaDias(parseInt(e.target.value) || 90)}
            className="w-full px-3.5 py-2.5 rounded-xl border border-[#6C6B6D]/30 font-mono font-black text-sm text-[#2E2E2E]"
          />
          <p className="text-[11px] text-[#6C6B6D] mt-1">
            Meta fijada por la Junta de Socios (90 días estándar).
          </p>
        </div>

        {/* Comparador de Meta vs Calculado */}
        <div className="p-4 rounded-2xl bg-[#FFFBEA] border border-[#FFD100] flex flex-col justify-between">
          <div className="flex items-center justify-between text-xs font-black text-[#515151]">
            <span>Total Planificado:</span>
            <span className="font-mono text-base text-[#2E2E2E]">{totalDiasAcumulados} días</span>
          </div>

          <div className="text-xs text-[#515151] mt-1">
            Meta: <strong>{metaDias} días</strong> · Excedente:{' '}
            <strong className="text-rose-700">+{diferenciaContraMeta} días</strong>
          </div>

          <p className="text-[10px] text-amber-900 font-medium mt-1">
            {diferenciaContraMeta > 0
              ? `⚠ La meta de ${metaDias} días se supera por ${diferenciaContraMeta} días debido a la suma estándar (99d) y el ajuste de fin de semana.`
              : '✓ El cronograma cumple con la meta corporativa.'}
          </p>
        </div>
      </div>

      {/* Tabla de 6 Etapas Encadenadas (Sección 1.8 del documento) */}
      <div className="bg-white rounded-3xl border border-[#6C6B6D]/20 shadow-xs overflow-hidden">
        <div className="p-4 border-b border-slate-100 flex items-center justify-between">
          <h2 className="text-sm font-black text-[#2E2E2E] uppercase tracking-wider">
            Cronograma Encadenado de 6 Etapas (+ Días Calendario con Ajuste al Lunes)
          </h2>
          <span className="text-xs text-[#6C6B6D]">Regla: Fin de semana pasa a lunes</span>
        </div>

        <div className="overflow-x-auto">
          <table className="w-full text-left border-collapse">
            <thead>
              <tr className="bg-slate-50 border-b border-slate-200 text-[11px] font-black uppercase text-[#6C6B6D]">
                <th className="py-3 px-4">Etapa</th>
                <th className="py-3 px-4 text-center">+ Días Sumados</th>
                <th className="py-3 px-4">Fecha Planificada</th>
                <th className="py-3 px-4">Ajuste de Fin de Semana</th>
                <th className="py-3 px-4 text-right">Comando Slack Simulado</th>
              </tr>
            </thead>
            <tbody className="divide-y divide-slate-100 text-xs">
              {resultadoCalculado.map((row, idx) => (
                <tr key={row.etapa} className="hover:bg-slate-50/70">
                  <td className="py-3 px-4 font-black text-[#2E2E2E]">
                    {row.etapa}
                  </td>
                  <td className="py-3 px-4 text-center font-mono font-bold text-[#515151]">
                    {row.diasSumados === 0 ? '—' : `+${row.diasSumados} d`}
                  </td>
                  <td className="py-3 px-4 font-mono font-bold text-[#2E2E2E]">
                    {formatDisplayDateWithDay(row.fechaCalculada)}
                  </td>
                  <td className="py-3 px-4 text-[11px]">
                    <span
                      className={`px-2 py-0.5 rounded-md font-bold ${
                        row.ajusteTexto.includes('+')
                          ? 'bg-amber-100 text-amber-800'
                          : 'bg-slate-100 text-[#6C6B6D]'
                      }`}
                    >
                      {row.ajusteTexto}
                    </span>
                  </td>
                  <td className="py-3 px-4 text-right font-mono text-[10px] text-[#6C6B6D] max-w-xs truncate">
                    {idx === 0
                      ? '—'
                      : `/remind #imp2026-general "Revisar ${row.etapa}" ${formatDisplayDate(row.fechaCalculada)}`}
                  </td>
                </tr>
              ))}
            </tbody>
          </table>
        </div>
      </div>

      {/* Análisis de Hallazgos del Cliente (Sección 1.8 del documento) */}
      <div className="bg-white rounded-3xl p-6 border border-[#6C6B6D]/20 shadow-xs space-y-4">
        <h3 className="text-base font-black text-[#2E2E2E] flex items-center gap-2">
          <ShieldCheck className="w-5 h-5 text-emerald-600" />
          <span>Hallazgos del Cliente sobre la Calculadora Anterior vs Esta Web:</span>
        </h3>

        <div className="grid grid-cols-1 md:grid-cols-2 gap-4 text-xs">
          <div className="p-4 rounded-2xl bg-rose-50 border border-rose-200 space-y-2 text-rose-950">
            <h4 className="font-black text-rose-900 flex items-center gap-1.5">
              <AlertTriangle className="w-4 h-4 text-rose-600" />
              <span>Problemas de la calculadora anterior en Slack:</span>
            </h4>
            <ul className="list-disc pl-5 space-y-1 text-[11px] leading-relaxed">
              <li><strong>Meta de 90 días inalcanzable:</strong> Las duraciones estándar suman 99 días (con ajuste sube a 101).</li>
              <li><strong>Canales de Slack inexistentes:</strong> El bot enviaba a <code>#imp2026-00X-producto</code> que no existe.</li>
              <li><strong>Recordatorios rechazados:</strong> Si el pedido se creaba después de las 9:00 AM, Slack rechazaba el primer comando.</li>
              <li><strong>Copia engorrosa:</strong> "Copiar" copiaba los 5 comandos juntos, pero Slack exige enviarlos uno por uno.</li>
            </ul>
          </div>

          <div className="p-4 rounded-2xl bg-emerald-50 border border-emerald-200 space-y-2 text-emerald-950">
            <h4 className="font-black text-emerald-900 flex items-center gap-1.5">
              <CheckCircle2 className="w-4 h-4 text-emerald-600" />
              <span>Cómo lo soluciona esta aplicación web:</span>
            </h4>
            <ul className="list-disc pl-5 space-y-1 text-[11px] leading-relaxed">
              <li><strong>Reemplaza Slack por completo:</strong> Las alertas se generan automáticamente en la plataforma y por Gmail al Admin.</li>
              <li><strong>Recálculo dinámico en cascada:</strong> Si una etapa se atrasa (ej. aduana), todas las etapas posteriores se mueven solas.</li>
              <li><strong>Días hábiles y laborables:</strong> El cómputo no se infla artificialmente y respeta los plazos de cada responsable.</li>
              <li><strong>Línea horizontal visible:</strong> Cualquier usuario ve en segundos en qué etapa va el barco sin preguntar en chats.</li>
            </ul>
          </div>
        </div>
      </div>
    </div>
  );
};

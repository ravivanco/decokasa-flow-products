import React from 'react';
import { Pedido, Usuario } from '../types';
import { FileText, Download, Eye, File, Image } from 'lucide-react';

interface OrderDocumentsTabProps {
  pedido: Pedido;
  currentUser: Usuario;
  onShowToast: (msg: string) => void;
}

export const OrderDocumentsTab: React.FC<OrderDocumentsTabProps> = ({ pedido, currentUser, onShowToast }) => {
  return (
    <div className="bg-white rounded-3xl border border-[#6C6B6D]/20 shadow-xs p-6 sm:p-8 space-y-6">
      <div className="border-b border-slate-100 pb-4">
        <h3 className="text-lg font-black text-[#2E2E2E] flex items-center gap-2">
          <FileText className="w-5 h-5 text-[#515151]" />
          <span>Repositorio de Google Drive del Pedido (Límite 100 MB)</span>
        </h3>
        <p className="text-xs text-[#6C6B6D] mt-0.5">
          Archivos oficiales respaldados mediante cuenta de servicio en Google Drive. El navegador de David en China nunca conecta directamente con Google.
        </p>
      </div>

      <div className="space-y-6">
        {pedido.etapas.map((etapa) => {
          const filesWithDoc = etapa.documentos.filter((d) => d.archivoActual);
          if (filesWithDoc.length === 0) return null;

          return (
            <div key={etapa.numero} className="space-y-3">
              <div className="flex items-center gap-2">
                <span className="font-mono text-xs font-black px-2 py-0.5 rounded-md bg-[#FFD100] text-[#2E2E2E]">
                  Etapa {etapa.numero}
                </span>
                <span className="text-xs font-black text-[#2E2E2E]">
                  {etapa.nombre}
                </span>
              </div>

              <div className="grid grid-cols-1 md:grid-cols-2 gap-3">
                {filesWithDoc.map((doc) => {
                  const arch = doc.archivoActual!;
                  return (
                    <div
                      key={doc.id}
                      className="p-3.5 rounded-2xl border border-[#6C6B6D]/20 bg-[#FFFBEA]/40 hover:bg-[#FFFBEA] transition flex items-start justify-between gap-3 text-xs"
                    >
                      <div className="flex items-start gap-3 min-w-0">
                        <span className="p-2 rounded-xl bg-[#FFD100] text-[#2E2E2E] shrink-0 mt-0.5">
                          {arch.esImagen ? <Image className="w-4 h-4" /> : <File className="w-4 h-4" />}
                        </span>
                        <div className="min-w-0">
                          <div className="flex items-center gap-1.5 flex-wrap">
                            <span className="font-bold text-[#2E2E2E] truncate">
                              {arch.nombre}
                            </span>
                            <span className="text-[10px] font-black px-1.5 rounded bg-black/10 text-[#2E2E2E]">
                              v{arch.version}
                            </span>
                          </div>
                          <div className="text-[11px] text-[#6C6B6D] mt-0.5">
                            Tipo: <strong className="text-[#515151]">{doc.tipoDoc}</strong> · Tamaño: {arch.tamano}
                          </div>
                          <div className="text-[10px] text-[#6C6B6D] mt-0.5">
                            Subido por {arch.subidoPor} ({arch.subidoPorRol}) el {arch.fecha}
                          </div>
                        </div>
                      </div>

                      <div className="flex items-center gap-1.5 shrink-0">
                        <button
                          type="button"
                          onClick={() => onShowToast(`Abriendo ${arch.nombre} en Google Drive...`)}
                          className="p-2 rounded-xl bg-white border border-[#6C6B6D]/20 hover:bg-slate-100 text-[#2E2E2E] shadow-2xs transition cursor-pointer"
                          title="Ver en Google Drive"
                        >
                          <Eye className="w-3.5 h-3.5" />
                        </button>
                        <button
                          type="button"
                          onClick={() => onShowToast(`Descargando copia segura de ${arch.nombre}...`)}
                          className="p-2 rounded-xl bg-[#FFD100] hover:bg-[#E6BC00] text-[#2E2E2E] shadow-2xs transition cursor-pointer"
                          title="Descargar archivo"
                        >
                          <Download className="w-3.5 h-3.5" />
                        </button>
                      </div>
                    </div>
                  );
                })}
              </div>
            </div>
          );
        })}
      </div>
    </div>
  );
};

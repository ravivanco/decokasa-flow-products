import React from 'react';
import { Pedido, Usuario } from '../types';
import { FileText, Download, Link, Eye, File, Image } from 'lucide-react';

interface OrderDocumentsTabProps {
  pedido: Pedido;
  currentUser: Usuario;
}

export const OrderDocumentsTab: React.FC<OrderDocumentsTabProps> = ({ pedido, currentUser }) => {
  const canDownload = currentUser.rol !== 'consulta';

  return (
    <div className="bg-white rounded-3xl border border-[#6C6B6D]/20 shadow-xs p-6 sm:p-8 space-y-6">
      <div className="border-b border-slate-100 pb-4">
        <h3 className="text-lg font-black text-[#2E2E2E] flex items-center gap-2">
          <FileText className="w-5 h-5 text-[#515151]" />
          <span>Repositorio de Documentos del Pedido</span>
        </h3>
        <p className="text-xs text-[#6C6B6D] mt-0.5">
          Archivos oficiales organizados por etapa con trazabilidad de versiones.
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
                          <span className="text-[11px] text-[#6C6B6D] block mt-0.5">
                            {doc.tipoDoc} · {arch.tamano}
                          </span>
                          <span className="text-[10px] text-[#6C6B6D] block">
                            Subido por {arch.subidoPor} ({arch.subidoPorRol}) el {arch.fecha}
                          </span>
                          {arch.driveUrl && (
                            <a
                              href={arch.driveUrl}
                              target="_blank"
                              rel="noreferrer"
                              className="text-[11px] text-[#2E2E2E] underline font-bold mt-1 inline-flex items-center gap-1"
                            >
                              <Link className="w-3 h-3" />
                              <span>Abrir en Google Drive</span>
                            </a>
                          )}
                        </div>
                      </div>

                      {canDownload && (
                        <button
                          type="button"
                          onClick={() => alert(`Descargando muestra de archivo: ${arch.nombre}`)}
                          className="p-2 text-[#515151] hover:text-black rounded-lg hover:bg-white shrink-0 cursor-pointer"
                          title="Descargar archivo"
                        >
                          <Download className="w-4 h-4" />
                        </button>
                      )}
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

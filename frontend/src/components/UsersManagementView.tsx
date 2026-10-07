import React from 'react';
import { Usuario } from '../types';
import { USUARIOS_DEMO } from '../data/initialData';
import { ShieldCheck, UserCheck, Eye, Sparkles, MapPin, CheckCircle2, User } from 'lucide-react';

interface UsersManagementViewProps {
  currentUser: Usuario;
  onSwitchUser: (user: Usuario) => void;
  onShowToast: (msg: string) => void;
}

export const UsersManagementView: React.FC<UsersManagementViewProps> = ({
  currentUser,
  onSwitchUser,
  onShowToast,
}) => {
  return (
    <div className="space-y-6 sm:space-y-8 animate-in fade-in duration-200">
      <div className="flex flex-col sm:flex-row sm:items-center justify-between gap-4">
        <div>
          <h1 className="text-2xl sm:text-3xl font-black text-[#2E2E2E]">
            Usuarios, Roles y Permisos
          </h1>
          <p className="text-xs sm:text-sm text-[#515151] mt-0.5">
            Equipo multidisciplinario en Ecuador y China con roles estrictos y etapas autorizadas.
          </p>
        </div>

        <div className="px-3.5 py-1.5 rounded-xl bg-[#FFFBEA] border border-[#FFD100] text-xs font-bold text-[#2E2E2E]">
          Sesión actual: <span className="font-black">{currentUser.nombre}</span> ({currentUser.cargo})
        </div>
      </div>

      {/* Grid of Users */}
      <div className="grid grid-cols-1 md:grid-cols-2 lg:grid-cols-3 gap-5">
        {USUARIOS_DEMO.map((u) => {
          const isCurrent = currentUser.id === u.id;
          return (
            <div
              key={u.id}
              className={`bg-white rounded-3xl border p-5 shadow-xs transition hover:shadow-md flex flex-col justify-between ${
                isCurrent ? 'border-2 border-[#FFD100] ring-2 ring-[#FFD100]/20' : 'border-[#6C6B6D]/20'
              }`}
            >
              <div className="space-y-3">
                <div className="flex items-start justify-between gap-3">
                  <div className="flex items-center gap-3">
                    <div className="w-12 h-12 rounded-2xl bg-[#FFD100] text-[#2E2E2E] flex items-center justify-center font-black text-sm shadow-xs shrink-0">
                      {u.avatar}
                    </div>
                    <div>
                      <h3 className="text-sm font-black text-[#2E2E2E] leading-tight">
                        {u.nombre}
                      </h3>
                      <p className="text-xs font-bold text-[#515151] leading-tight mt-0.5">
                        {u.cargo}
                      </p>
                      <p className="text-[11px] font-mono text-[#6C6B6D] mt-0.5">
                        {u.email}
                      </p>
                    </div>
                  </div>

                  <span
                    className={`text-[10px] font-black uppercase px-2 py-0.5 rounded-full shrink-0 ${
                      u.rol === 'admin'
                        ? 'bg-purple-100 text-purple-800'
                        : u.rol === 'junta'
                        ? 'bg-blue-100 text-blue-800'
                        : u.rol === 'editor_china'
                        ? 'bg-rose-100 text-rose-800'
                        : u.rol === 'editor_logistica'
                        ? 'bg-amber-100 text-amber-800'
                        : u.rol === 'editor_compras_ec'
                        ? 'bg-emerald-100 text-emerald-800'
                        : u.rol === 'editor_marketing'
                        ? 'bg-cyan-100 text-cyan-800'
                        : 'bg-slate-100 text-slate-700'
                    }`}
                  >
                    {u.rol}
                  </span>
                </div>

                <div className="flex items-center gap-4 text-xs text-[#515151] pt-1 border-t border-slate-100">
                  <div className="flex items-center gap-1">
                    <MapPin className="w-3.5 h-3.5 text-[#6C6B6D]" />
                    <span>{u.pais}</span>
                  </div>
                  <div>
                    <span className="text-[#6C6B6D]">Área: </span>
                    <span className="font-semibold">{u.area}</span>
                  </div>
                </div>

                {/* Etapas asignadas */}
                <div className="pt-2">
                  <span className="text-[10px] font-black uppercase tracking-wider text-[#6C6B6D] block mb-1.5">
                    Etapas operativas autorizadas:
                  </span>
                  <div className="flex flex-wrap gap-1">
                    {u.etapasAsignadas.length === 0 ? (
                      <span className="text-[11px] text-[#6C6B6D] italic">Solo visualización y consulta</span>
                    ) : (
                      u.etapasAsignadas.map((etapaNum) => (
                        <span
                          key={etapaNum}
                          className="px-2 py-0.5 rounded-lg bg-slate-100 text-[#2E2E2E] text-[10px] font-black border border-slate-200"
                        >
                          Etapa {etapaNum}
                        </span>
                      ))
                    )}
                  </div>
                </div>
              </div>

              {/* Botón simular sesión */}
              <div className="pt-4 mt-4 border-t border-slate-100">
                {isCurrent ? (
                  <div className="w-full py-2 rounded-xl bg-[#FFF6CC] text-[#2E2E2E] text-xs font-black flex items-center justify-center gap-1.5 border border-[#FFD100]">
                    <CheckCircle2 className="w-4 h-4 text-emerald-600" />
                    <span>Usuario actualmente activo</span>
                  </div>
                ) : (
                  <button
                    type="button"
                    onClick={() => {
                      onSwitchUser(u);
                      onShowToast(`Sesión cambiada a ${u.nombre} (${u.cargo})`);
                    }}
                    className="w-full py-2 rounded-xl bg-[#FFD100] hover:bg-[#E6BC00] text-[#2E2E2E] text-xs font-black transition cursor-pointer shadow-2xs"
                  >
                    Simular sesión como {u.nombre.split(' ')[0]}
                  </button>
                )}
              </div>
            </div>
          );
        })}
      </div>
    </div>
  );
};

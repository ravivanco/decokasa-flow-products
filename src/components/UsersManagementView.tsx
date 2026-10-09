import React from 'react';
import { Usuario } from '../types';
import { USUARIOS_DEMO } from '../data/initialData';
import { ShieldCheck, MapPin, CheckCircle2, User, Phone, Mail, Sparkles } from 'lucide-react';
import { getRoleBadgeColor, getRoleNameDisplay } from '../utils/permissions';

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
            Gestión de Usuarios, Roles y Etapas
          </h1>
          <p className="text-xs sm:text-sm text-[#515151] mt-0.5">
            Administrado por Mirian Franco. Asignación de responsabilidades operativas en Ecuador y China para Flujos A y B.
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
                      <div className="flex items-center gap-1.5 text-[11px] font-mono text-[#6C6B6D] mt-1">
                        <span>@{u.usuarioLogin}</span>
                        <span>·</span>
                        <span>{u.pais === 'China' ? '🇨🇳 China' : '🇪🇨 Ecuador'}</span>
                      </div>
                    </div>
                  </div>

                  <span
                    className={`text-[10px] font-black uppercase px-2 py-0.5 rounded-full shrink-0 border ${getRoleBadgeColor(
                      u.rol
                    )}`}
                  >
                    {u.rol}
                  </span>
                </div>

                <div className="text-xs text-[#515151] space-y-1 pt-2 border-t border-slate-100">
                  <div className="flex items-center gap-1.5 text-[11px]">
                    <Mail className="w-3.5 h-3.5 text-[#6C6B6D]" />
                    <span className="font-mono truncate">{u.email}</span>
                  </div>
                  <div className="flex items-center gap-1.5 text-[11px]">
                    <Phone className="w-3.5 h-3.5 text-[#6C6B6D]" />
                    <span className="font-mono">{u.telefono}</span>
                  </div>
                </div>

                {/* Etapas asignadas en Flujo A y B */}
                <div className="pt-2 border-t border-slate-100 space-y-1.5">
                  <div>
                    <span className="text-[10px] font-black uppercase tracking-wider text-[#6C6B6D] block">
                      Flujo A (Ecuador · 11 etapas):
                    </span>
                    <div className="flex flex-wrap gap-1 mt-1">
                      {u.etapasAsignadasA.length === 0 ? (
                        <span className="text-[11px] text-[#6C6B6D] italic">Sin etapas operativas (Consulta)</span>
                      ) : (
                        u.etapasAsignadasA.map((num) => (
                          <span
                            key={num}
                            className="px-2 py-0.5 rounded-md bg-slate-100 text-[#2E2E2E] text-[10px] font-black font-mono"
                          >
                            Etapa {num}
                          </span>
                        ))
                      )}
                    </div>
                  </div>

                  <div>
                    <span className="text-[10px] font-black uppercase tracking-wider text-[#6C6B6D] block">
                      Flujo B (China · 12 etapas):
                    </span>
                    <div className="flex flex-wrap gap-1 mt-1">
                      {u.etapasAsignadasB.length === 0 ? (
                        <span className="text-[11px] text-[#6C6B6D] italic">Sin etapas operativas (Consulta)</span>
                      ) : (
                        u.etapasAsignadasB.map((num) => (
                          <span
                            key={num}
                            className="px-2 py-0.5 rounded-md bg-amber-50 text-amber-900 border border-amber-200 text-[10px] font-black font-mono"
                          >
                            Etapa {num}
                          </span>
                        ))
                      )}
                    </div>
                  </div>
                </div>
              </div>

              {/* Botón simular sesión */}
              <div className="pt-4 mt-4 border-t border-slate-100">
                {isCurrent ? (
                  <div className="w-full py-2 rounded-xl bg-[#FFF6CC] text-[#2E2E2E] text-xs font-black flex items-center justify-center gap-1.5 border border-[#FFD100]">
                    <CheckCircle2 className="w-4 h-4 text-emerald-600" />
                    <span>Sesión actual activa</span>
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

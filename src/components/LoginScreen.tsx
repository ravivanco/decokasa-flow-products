import React, { useState } from 'react';
import { Usuario, Pais } from '../types';
import { USUARIOS_DEMO, DEMO_PASSWORD } from '../data/initialData';
import { Ship, Lock, User, AlertCircle, ArrowRight, Phone, Mail, Globe, Sparkles, CheckCircle2 } from 'lucide-react';
import { getRoleBadgeColor } from '../utils/permissions';

interface LoginScreenProps {
  onLoginSuccess: (user: Usuario, pais: Pais) => void;
  selectedPais: Pais;
  onSelectPais: (pais: Pais) => void;
}

export const LoginScreen: React.FC<LoginScreenProps> = ({
  onLoginSuccess,
  selectedPais,
  onSelectPais,
}) => {
  const [identifier, setIdentifier] = useState(''); // Correo, usuario o teléfono
  const [password, setPassword] = useState('');
  const [errorMessage, setErrorMessage] = useState('');
  const [pais, setPais] = useState<Pais>(selectedPais);

  const handleSubmit = (e: React.FormEvent) => {
    e.preventDefault();
    setErrorMessage('');

    const cleanId = identifier.trim().toLowerCase();
    const user = USUARIOS_DEMO.find((u) => {
      const matchEmail = u.email.toLowerCase() === cleanId;
      const matchLogin = u.usuarioLogin.toLowerCase() === cleanId;
      const matchTel = u.telefono.replace(/\s+/g, '') === cleanId.replace(/\s+/g, '');
      return matchEmail || matchLogin || matchTel;
    });

    if (user && password === DEMO_PASSWORD && user.activo) {
      onSelectPais(pais);
      onLoginSuccess(user, pais);
    } else {
      setErrorMessage(
        'Credenciales incorrectas. Ingresa tu correo, usuario o teléfono y la contraseña Demo2026.'
      );
    }
  };

  const handleQuickLogin = (user: Usuario) => {
    setIdentifier(user.email);
    setPassword(DEMO_PASSWORD);
    setErrorMessage('');
    onSelectPais(pais);
    onLoginSuccess(user, pais);
  };

  return (
    <div className="min-h-screen bg-[#FFD100] flex flex-col justify-center items-center p-4 sm:p-6 selection:bg-[#2E2E2E] selection:text-white">
      <div className="w-full max-w-xl">
        {/* Main white card */}
        <div className="bg-white rounded-3xl shadow-2xl border border-black/10 overflow-hidden">
          {/* Header banner inside card */}
          <div className="p-6 sm:p-8 text-center border-b border-slate-100 bg-linear-to-b from-amber-50/50 to-white">
            <div className="inline-flex items-center justify-center w-16 h-16 rounded-2xl bg-[#FFD100] text-[#2E2E2E] shadow-md mb-3">
              <Ship className="w-9 h-9 stroke-[2.2]" />
            </div>

            <div className="flex items-baseline justify-center gap-1.5">
              <span className="text-3xl sm:text-4xl font-black tracking-tight text-[#2E2E2E]">
                Decokasa
              </span>
              <span className="text-xl sm:text-2xl font-bold tracking-tight text-[#515151]">
                S.A.S
              </span>
            </div>

            <p className="text-xs sm:text-sm font-bold text-[#515151] mt-1">
              Sistema Web de Seguimiento y Control de Importaciones
            </p>
            <p className="text-[11px] text-[#6C6B6D] mt-0.5">
              Flujos A (Ecuador) y B (China) · Plazos en días hábiles · Trazabilidad completa
            </p>

            {/* Selector de País: Ecuador / Colombia */}
            <div className="mt-4 pt-3 border-t border-slate-100 flex items-center justify-center gap-2">
              <span className="text-xs font-bold text-[#515151] flex items-center gap-1">
                <Globe className="w-3.5 h-3.5 text-[#6C6B6D]" />
                País de operación:
              </span>
              <div className="inline-flex p-1 bg-slate-100 rounded-xl border border-slate-200">
                <button
                  type="button"
                  onClick={() => setPais('EC')}
                  className={`px-3 py-1 rounded-lg text-xs font-black transition cursor-pointer flex items-center gap-1.5 ${
                    pais === 'EC'
                      ? 'bg-[#FFD100] text-[#2E2E2E] shadow-2xs'
                      : 'text-[#6C6B6D] hover:text-[#2E2E2E]'
                  }`}
                >
                  <span>🇪🇨</span>
                  <span>Ecuador (Activo)</span>
                </button>
                <button
                  type="button"
                  onClick={() => setPais('CO')}
                  className={`px-3 py-1 rounded-lg text-xs font-bold transition cursor-pointer flex items-center gap-1.5 ${
                    pais === 'CO'
                      ? 'bg-[#FFD100] text-[#2E2E2E] shadow-2xs'
                      : 'text-[#6C6B6D] hover:text-[#2E2E2E]'
                  }`}
                  title="Proceso de Colombia planificado para Fase 2"
                >
                  <span>🇨🇴</span>
                  <span>Colombia (Fase 2)</span>
                </button>
              </div>
            </div>
            {pais === 'CO' && (
              <p className="text-[10px] text-amber-800 bg-amber-50 py-1 px-3 rounded-lg mt-2 font-medium">
                Nota del cliente: El proceso de Colombia se implementará más adelante. La demo visual utiliza las etapas estándar de Ecuador.
              </p>
            )}
          </div>

          {/* Form */}
          <div className="p-6 sm:p-8 space-y-5">
            {errorMessage && (
              <div className="p-3.5 rounded-2xl bg-rose-50 border border-rose-200 text-[#DC2626] text-xs flex items-start gap-2 animate-in fade-in">
                <AlertCircle className="w-4 h-4 shrink-0 mt-0.5" />
                <span className="font-medium">{errorMessage}</span>
              </div>
            )}

            <form onSubmit={handleSubmit} className="space-y-4">
              <div>
                <label className="block text-xs font-black uppercase tracking-wider text-[#515151] mb-1.5">
                  Correo, Usuario o Teléfono
                </label>
                <div className="relative">
                  <div className="absolute left-3.5 top-1/2 -translate-y-1/2 flex items-center gap-1 text-[#6C6B6D]">
                    <User className="w-4 h-4" />
                  </div>
                  <input
                    type="text"
                    required
                    placeholder="mfranco@decokasa.ec / mfranco / +593..."
                    value={identifier}
                    onChange={(e) => setIdentifier(e.target.value)}
                    className="w-full pl-10 pr-4 py-3 rounded-2xl border border-[#6C6B6D]/30 text-xs sm:text-sm font-semibold text-[#2E2E2E] focus:outline-hidden focus:ring-2 focus:ring-[#FFD100]"
                  />
                </div>
                <p className="text-[11px] text-[#6C6B6D] mt-1">
                  Permite iniciar sesión con correo corporativo, nombre de usuario o teléfono.
                </p>
              </div>

              <div>
                <div className="flex items-center justify-between mb-1.5">
                  <label className="text-xs font-black uppercase tracking-wider text-[#515151]">
                    Contraseña
                  </label>
                  <span className="text-[11px] font-bold text-[#6C6B6D]">
                    Clave demo: <strong className="font-mono text-[#2E2E2E]">Demo2026</strong>
                  </span>
                </div>
                <div className="relative">
                  <Lock className="w-4 h-4 text-[#6C6B6D] absolute left-3.5 top-1/2 -translate-y-1/2" />
                  <input
                    type="password"
                    required
                    placeholder="••••••••"
                    value={password}
                    onChange={(e) => setPassword(e.target.value)}
                    className="w-full pl-10 pr-4 py-3 rounded-2xl border border-[#6C6B6D]/30 text-xs sm:text-sm font-semibold text-[#2E2E2E] focus:outline-hidden focus:ring-2 focus:ring-[#FFD100]"
                  />
                </div>
              </div>

              <button
                type="submit"
                className="w-full py-3.5 px-4 rounded-2xl bg-[#FFD100] hover:bg-[#E6BC00] text-[#2E2E2E] font-black text-sm tracking-wide shadow-md transition flex items-center justify-center gap-2 cursor-pointer"
              >
                <span>Ingresar al Sistema</span>
                <ArrowRight className="w-4 h-4 stroke-[2.5]" />
              </button>
            </form>

            {/* Quick Login Chips for the 6 roles from requirements */}
            <div className="pt-4 border-t border-slate-100">
              <div className="flex items-center justify-between mb-2.5">
                <span className="text-[11px] font-black uppercase tracking-wider text-[#6C6B6D] flex items-center gap-1">
                  <Sparkles className="w-3.5 h-3.5 text-amber-500" />
                  Acceso rápido de prueba (1 clic por rol)
                </span>
              </div>

              <div className="grid grid-cols-1 sm:grid-cols-2 gap-2">
                {USUARIOS_DEMO.map((u) => (
                  <button
                    key={u.id}
                    type="button"
                    onClick={() => handleQuickLogin(u)}
                    className="p-2.5 rounded-xl border border-slate-200 hover:border-[#FFD100] hover:bg-[#FFFBEA] transition text-left flex items-center gap-2.5 cursor-pointer group"
                  >
                    <div className="w-8 h-8 rounded-lg bg-[#2E2E2E] text-white group-hover:bg-[#FFD100] group-hover:text-[#2E2E2E] flex items-center justify-center font-black text-xs shrink-0 transition">
                      {u.avatar}
                    </div>
                    <div className="min-w-0 flex-1">
                      <div className="text-xs font-black text-[#2E2E2E] truncate">
                        {u.nombre}
                      </div>
                      <div className="text-[10px] text-[#6C6B6D] truncate">
                        {u.cargo}
                      </div>
                    </div>
                    <span className="text-[10px] text-[#6C6B6D] shrink-0 font-mono">
                      {u.pais === 'China' ? '🇨🇳' : '🇪🇨'}
                    </span>
                  </button>
                ))}
              </div>
            </div>
          </div>
        </div>

        {/* Footer */}
        <p className="text-center text-xs text-[#2E2E2E] font-bold mt-4">
          © 2026 Decokasa S.A.S — Procedimiento de Solicitud, Revisión, Fabricación y Bodega
        </p>
      </div>
    </div>
  );
};

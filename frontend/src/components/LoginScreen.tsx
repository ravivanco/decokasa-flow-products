import React, { useState } from 'react';
import { Usuario } from '../types';
import { USUARIOS_DEMO, DEMO_PASSWORD } from '../data/initialData';
import { Ship, Lock, Mail, ChevronDown, ChevronUp, AlertCircle, ArrowRight } from 'lucide-react';

interface LoginScreenProps {
  onLoginSuccess: (user: Usuario) => void;
}

export const LoginScreen: React.FC<LoginScreenProps> = ({ onLoginSuccess }) => {
  const [email, setEmail] = useState('');
  const [password, setPassword] = useState('');
  const [errorMessage, setErrorMessage] = useState('');
  const [showDemoUsers, setShowDemoUsers] = useState(true);

  const handleSubmit = (e: React.FormEvent) => {
    e.preventDefault();
    setErrorMessage('');

    const trimmedEmail = email.trim().toLowerCase();
    const user = USUARIOS_DEMO.find((u) => u.email.toLowerCase() === trimmedEmail);

    if (user && password === DEMO_PASSWORD && user.activo) {
      onLoginSuccess(user);
    } else {
      setErrorMessage('Usuario o contraseña incorrectos. Revisa los datos e inténtalo de nuevo.');
    }
  };

  const handleUseDemoUser = (user: Usuario) => {
    setEmail(user.email);
    setPassword(DEMO_PASSWORD);
    setErrorMessage('');
  };

  return (
    <div className="min-h-screen bg-[#FFD100] flex flex-col justify-center items-center p-4 sm:p-6">
      <div className="w-full max-w-xl">
        {/* Main white card */}
        <div className="bg-white rounded-3xl shadow-2xl border border-black/10 overflow-hidden">
          {/* Top banner inside card */}
          <div className="p-8 sm:p-10 text-center border-b border-slate-100">
            <div className="inline-flex items-center justify-center w-16 h-16 rounded-2xl bg-[#FFD100] text-[#2E2E2E] shadow-md mb-4">
              <Ship className="w-9 h-9 stroke-[2.2]" />
            </div>

            <div className="flex items-baseline justify-center gap-1.5">
              <span className="text-3xl sm:text-4xl font-extrabold tracking-tight text-[#2E2E2E]">
                Decokasa
              </span>
              <span className="text-xl sm:text-2xl font-bold tracking-tight text-[#515151]">
                S.A.S
              </span>
            </div>

            <p className="text-sm font-semibold text-[#6C6B6D] mt-1">
              Sistema de Seguimiento de Importaciones Marítimas
            </p>
          </div>

          {/* Form */}
          <form onSubmit={handleSubmit} className="p-8 sm:p-10 space-y-5">
            {errorMessage && (
              <div className="p-4 rounded-xl bg-rose-50 border border-rose-200 text-xs font-bold text-[#DC2626] flex items-start gap-2.5 animate-in fade-in">
                <AlertCircle className="w-4 h-4 shrink-0 mt-0.5" />
                <span>{errorMessage}</span>
              </div>
            )}

            <div>
              <label className="block text-xs font-bold uppercase tracking-wider text-[#515151] mb-1.5">
                Usuario (correo)
              </label>
              <div className="relative">
                <Mail className="w-4 h-4 text-[#6C6B6D] absolute left-3.5 top-1/2 -translate-y-1/2" />
                <input
                  type="email"
                  required
                  placeholder="ejemplo@decokasa.ec"
                  value={email}
                  onChange={(e) => setEmail(e.target.value)}
                  className="w-full pl-10 pr-4 py-3 rounded-xl border border-[#6C6B6D]/30 text-sm font-medium text-[#2E2E2E] focus:outline-hidden focus:ring-2 focus:ring-[#FFD100] focus:border-[#2E2E2E] transition"
                />
              </div>
            </div>

            <div>
              <label className="block text-xs font-bold uppercase tracking-wider text-[#515151] mb-1.5">
                Contraseña
              </label>
              <div className="relative">
                <Lock className="w-4 h-4 text-[#6C6B6D] absolute left-3.5 top-1/2 -translate-y-1/2" />
                <input
                  type="password"
                  required
                  placeholder="••••••••"
                  value={password}
                  onChange={(e) => setPassword(e.target.value)}
                  className="w-full pl-10 pr-4 py-3 rounded-xl border border-[#6C6B6D]/30 text-sm font-medium text-[#2E2E2E] focus:outline-hidden focus:ring-2 focus:ring-[#FFD100] focus:border-[#2E2E2E] transition"
                />
              </div>
            </div>

            <button
              type="submit"
              className="w-full py-3.5 px-6 rounded-xl font-black text-sm bg-[#FFD100] hover:bg-[#E6BC00] text-[#2E2E2E] shadow-md hover:shadow-lg transition-all flex items-center justify-center gap-2 cursor-pointer active:scale-98"
            >
              <span>Ingresar</span>
              <ArrowRight className="w-4 h-4 stroke-[3]" />
            </button>
          </form>

          {/* Demo Users Section */}
          <div className="border-t border-slate-100 bg-[#FFFBEA] p-6 sm:p-8">
            <button
              type="button"
              onClick={() => setShowDemoUsers(!showDemoUsers)}
              className="w-full flex items-center justify-between text-xs font-black uppercase tracking-wider text-[#2E2E2E] cursor-pointer"
            >
              <span>Usuarios de demostración (Contraseña: {DEMO_PASSWORD})</span>
              {showDemoUsers ? (
                <ChevronUp className="w-4 h-4" />
              ) : (
                <ChevronDown className="w-4 h-4" />
              )}
            </button>

            {showDemoUsers && (
              <div className="mt-4 overflow-x-auto">
                <table className="w-full text-left text-xs text-[#515151]">
                  <thead>
                    <tr className="border-b border-[#6C6B6D]/20 text-[11px] font-bold text-[#6C6B6D] uppercase">
                      <th className="pb-2">Nombre</th>
                      <th className="pb-2">Rol / Área</th>
                      <th className="pb-2 text-right">Acción</th>
                    </tr>
                  </thead>
                  <tbody className="divide-y divide-[#6C6B6D]/10">
                    {USUARIOS_DEMO.map((u) => (
                      <tr key={u.id} className="hover:bg-white/60 transition">
                        <td className="py-2.5 font-bold text-[#2E2E2E]">
                          <div>{u.nombre}</div>
                          <div className="text-[10px] font-normal text-[#6C6B6D] font-mono">
                            {u.email}
                          </div>
                        </td>
                        <td className="py-2.5">
                          <span className="font-semibold text-[#2E2E2E] block">
                            {u.cargo}
                          </span>
                          <span className="text-[10px] text-[#6C6B6D]">
                            {u.area}
                          </span>
                        </td>
                        <td className="py-2.5 text-right">
                          <button
                            type="button"
                            onClick={() => handleUseDemoUser(u)}
                            className="px-3 py-1 rounded-lg text-xs font-black bg-[#FFD100] hover:bg-[#E6BC00] text-[#2E2E2E] border border-black/10 transition shadow-xs cursor-pointer"
                          >
                            Usar
                          </button>
                        </td>
                      </tr>
                    ))}
                  </tbody>
                </table>
              </div>
            )}
          </div>
        </div>

        {/* Footer info */}
        <p className="text-center text-xs font-bold text-[#2E2E2E] mt-4 opacity-90">
          © 2026 Decokasa S.A.S — Sistema de seguimiento de importaciones
        </p>
      </div>
    </div>
  );
};

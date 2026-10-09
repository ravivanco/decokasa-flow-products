import React from 'react';
import { Usuario, Pais, NotificacionGmail } from '../types';
import { formatDisplayDate, SIMULATED_TODAY } from '../utils/dateUtils';
import { Ship, Mail, Calendar, Globe, LogOut, Menu, User, Sparkles } from 'lucide-react';
import { getRoleBadgeColor } from '../utils/permissions';

interface HeaderProps {
  currentUser: Usuario;
  pais: Pais;
  onTogglePais: () => void;
  onLogout: () => void;
  onToggleMobileMenu: () => void;
  notificaciones: NotificacionGmail[];
  onOpenGmailDrawer: () => void;
  onSwitchUserPrompt: () => void;
}

export const Header: React.FC<HeaderProps> = ({
  currentUser,
  pais,
  onTogglePais,
  onLogout,
  onToggleMobileMenu,
  notificaciones,
  onOpenGmailDrawer,
  onSwitchUserPrompt,
}) => {
  const unreadCount = notificaciones.filter((n) => !n.leida).length;

  return (
    <header className="sticky top-0 z-40 bg-[#FFD100] text-[#2E2E2E] shadow-sm border-b border-[#E6BC00]">
      <div className="w-full px-4 sm:px-6 lg:px-8">
        <div className="flex items-center justify-between h-16 sm:h-18 gap-3">
          {/* Left: Mobile hamburger & Logo */}
          <div className="flex items-center gap-3">
            <button
              type="button"
              onClick={onToggleMobileMenu}
              className="lg:hidden p-2 rounded-xl text-[#2E2E2E] hover:bg-[#E6BC00] transition cursor-pointer"
              aria-label="Abrir menú"
            >
              <Menu className="w-6 h-6 stroke-[2.5]" />
            </button>

            {/* Logo: Decokasa en #2E2E2E negrita y S.A.S más pequeño */}
            <div className="flex items-center gap-2.5">
              <div className="w-9 h-9 sm:w-10 sm:h-10 rounded-xl bg-[#2E2E2E] text-[#FFD100] flex items-center justify-center font-black shadow-xs shrink-0">
                <Ship className="w-5 h-5 sm:w-6 sm:h-6" />
              </div>
              <div>
                <div className="flex items-baseline gap-1.5">
                  <span className="text-xl sm:text-2xl font-black tracking-tight text-[#2E2E2E]">
                    Decokasa
                  </span>
                  <span className="text-sm sm:text-base font-bold tracking-tight text-[#515151]">
                    S.A.S
                  </span>
                </div>
                <p className="text-[10px] sm:text-[11px] font-bold text-[#515151] leading-none hidden sm:block">
                  Control de Pedidos de Importación · China — Ecuador
                </p>
              </div>
            </div>
          </div>

          {/* Right: Country, Simulated Date, Gmail Notifications, Current User, Logout */}
          <div className="flex items-center gap-2 sm:gap-3">
            {/* Country Selector: Ecuador / Colombia */}
            <button
              type="button"
              onClick={onTogglePais}
              className="flex items-center gap-1.5 px-3 py-1.5 rounded-xl bg-white/80 hover:bg-white border border-black/10 text-xs font-black text-[#2E2E2E] shadow-2xs transition cursor-pointer"
              title="Cambiar país de operación (Ecuador / Colombia)"
            >
              <span>{pais === 'EC' ? '🇪🇨' : '🇨🇴'}</span>
              <span>{pais === 'EC' ? 'Ecuador' : 'Colombia (Fase 2)'}</span>
            </button>

            {/* Simulated Date Badge: 09/10/2026 */}
            <div className="hidden md:flex items-center gap-1.5 px-3 py-1.5 rounded-xl bg-white/80 border border-black/10 text-xs font-bold text-[#2E2E2E] shadow-2xs">
              <Calendar className="w-3.5 h-3.5 text-[#515151]" />
              <span className="text-[#6C6B6D]">Fecha:</span>
              <span className="font-mono">{formatDisplayDate(SIMULATED_TODAY)}</span>
            </div>

            {/* Gmail Notifications Bell */}
            <button
              type="button"
              onClick={onOpenGmailDrawer}
              className="relative p-2 rounded-xl bg-white/80 hover:bg-white text-[#2E2E2E] border border-black/10 transition shadow-2xs cursor-pointer"
              title="Notificaciones Gmail de alertas y vencimientos"
            >
              <Mail className="w-5 h-5" />
              {unreadCount > 0 && (
                <span className="absolute -top-1 -right-1 flex h-5 w-5 items-center justify-center rounded-full bg-[#DC2626] text-[10px] font-black text-white shadow-xs animate-pulse">
                  {unreadCount}
                </span>
              )}
            </button>

            {/* User Chip */}
            <div
              onClick={onSwitchUserPrompt}
              className="flex items-center gap-2 px-3 py-1.5 rounded-xl bg-white/80 border border-black/10 shadow-2xs cursor-pointer hover:bg-white transition"
              title="Clic para cambiar de rol rápidamente"
            >
              <div className="w-7 h-7 rounded-lg bg-[#2E2E2E] text-white flex items-center justify-center font-bold text-xs shrink-0">
                {currentUser.avatar}
              </div>
              <div className="text-left hidden sm:block">
                <div className="text-xs font-black text-[#2E2E2E] leading-tight flex items-center gap-1">
                  <span>{currentUser.nombre}</span>
                  <span className="text-[10px]">{currentUser.pais === 'China' ? '🇨🇳' : '🇪🇨'}</span>
                </div>
                <div className="text-[10px] text-[#515151] leading-tight truncate max-w-36">
                  {currentUser.cargo}
                </div>
              </div>
            </div>

            {/* Logout */}
            <button
              type="button"
              onClick={onLogout}
              className="p-2 rounded-xl bg-white/80 hover:bg-white text-[#DC2626] border border-black/10 transition shadow-2xs cursor-pointer"
              title="Cerrar sesión"
            >
              <LogOut className="w-4 h-4" />
            </button>
          </div>
        </div>
      </div>
    </header>
  );
};

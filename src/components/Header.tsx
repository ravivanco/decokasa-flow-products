import React from 'react';
import { Usuario, Language, AlertaItem } from '../types';
import { formatDisplayDate, SIMULATED_TODAY } from '../utils/dateUtils';
import { Ship, Bell, Calendar, Globe, LogOut, Menu, User } from 'lucide-react';

interface HeaderProps {
  currentUser: Usuario;
  lang: Language;
  onToggleLang: () => void;
  onLogout: () => void;
  onToggleMobileMenu: () => void;
  alertas: AlertaItem[];
  onOpenAlerts: () => void;
}

export const Header: React.FC<HeaderProps> = ({
  currentUser,
  lang,
  onToggleLang,
  onLogout,
  onToggleMobileMenu,
  alertas,
  onOpenAlerts,
}) => {
  const unreadAlertsCount = alertas.filter((a) => !a.leida).length;

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
                  <span className="text-xl sm:text-2xl font-extrabold tracking-tight text-[#2E2E2E]">
                    Decokasa
                  </span>
                  <span className="text-sm sm:text-base font-bold tracking-tight text-[#515151]">
                    S.A.S
                  </span>
                </div>
                <p className="text-[10px] sm:text-[11px] font-bold text-[#515151] leading-none hidden sm:block">
                  Seguimiento de Importaciones Marítimas
                </p>
              </div>
            </div>
          </div>

          {/* Right: Simulated Date, Lang, Alerts, User info, Logout */}
          <div className="flex items-center gap-2 sm:gap-3">
            {/* Simulated Date Badge */}
            <div className="hidden md:flex items-center gap-1.5 px-3 py-1.5 rounded-xl bg-white/70 border border-black/10 text-xs font-bold text-[#2E2E2E] shadow-2xs">
              <Calendar className="w-3.5 h-3.5 text-[#515151]" />
              <span className="text-[#6C6B6D]">Fecha:</span>
              <span>{formatDisplayDate(SIMULATED_TODAY)}</span>
            </div>

            {/* Language toggle */}
            <button
              type="button"
              onClick={onToggleLang}
              className="flex items-center gap-1 px-2.5 py-1.5 rounded-xl bg-white/70 hover:bg-white text-xs font-black text-[#2E2E2E] border border-black/10 transition shadow-2xs cursor-pointer"
              title="Cambiar idioma (ES / EN)"
            >
              <Globe className="w-3.5 h-3.5 text-[#515151]" />
              <span>{lang.toUpperCase()}</span>
            </button>

            {/* Alerts Bell */}
            <button
              type="button"
              onClick={onOpenAlerts}
              className="relative p-2 rounded-xl bg-white/70 hover:bg-white text-[#2E2E2E] border border-black/10 transition shadow-2xs cursor-pointer"
              title="Alertas del sistema"
            >
              <Bell className="w-5 h-5" />
              {unreadAlertsCount > 0 && (
                <span className="absolute -top-1 -right-1 flex h-5 w-5 items-center justify-center rounded-full bg-[#DC2626] text-[10px] font-black text-white shadow-xs animate-pulse">
                  {unreadAlertsCount}
                </span>
              )}
            </button>

            {/* User Info */}
            <div className="hidden sm:flex items-center gap-2 px-3 py-1.5 rounded-xl bg-white/70 border border-black/10 shadow-2xs">
              <div className="w-7 h-7 rounded-lg bg-[#2E2E2E] text-white flex items-center justify-center font-bold text-xs shrink-0">
                {currentUser.avatar}
              </div>
              <div className="text-left text-xs leading-tight">
                <span className="font-bold text-[#2E2E2E] block truncate max-w-[130px]">
                  {currentUser.nombre}
                </span>
                <span className="text-[10px] font-semibold text-[#6C6B6D] block truncate max-w-[130px]">
                  {currentUser.cargo}
                </span>
              </div>
            </div>

            {/* Logout button */}
            <button
              type="button"
              onClick={onLogout}
              className="inline-flex items-center gap-1.5 px-3 py-1.5 rounded-xl text-xs font-black bg-[#2E2E2E] hover:bg-black text-white transition shadow-sm cursor-pointer"
              title="Cerrar sesión"
            >
              <LogOut className="w-3.5 h-3.5" />
              <span className="hidden sm:inline">Cerrar sesión</span>
            </button>
          </div>
        </div>
      </div>
    </header>
  );
};

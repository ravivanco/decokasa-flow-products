import React from 'react';
import { AppModule, Usuario, NotificacionGmail } from '../types';
import { canUserAccessModule } from '../utils/permissions';
import {
  Home,
  Search,
  Package,
  Warehouse,
  Mail,
  Clock,
  Users,
  ShieldCheck,
  HelpCircle,
  X,
  Sparkles,
} from 'lucide-react';

interface SidebarProps {
  currentModule: AppModule;
  onSelectModule: (module: AppModule) => void;
  currentUser: Usuario;
  notificaciones: NotificacionGmail[];
  isOpenMobile: boolean;
  onCloseMobile: () => void;
}

export const Sidebar: React.FC<SidebarProps> = ({
  currentModule,
  onSelectModule,
  currentUser,
  notificaciones,
  isOpenMobile,
  onCloseMobile,
}) => {
  const unreadAlerts = notificaciones.filter((n) => !n.leida).length;

  const navItems: { id: AppModule; label: string; icon: React.ReactNode; badge?: number }[] = [
    { id: 'inicio', label: 'Dashboard por Rol', icon: <Home className="w-4 h-4" /> },
    { id: 'rastreo', label: 'Rastrear pedido', icon: <Search className="w-4 h-4" /> },
    { id: 'pedidos', label: 'Pedidos (Flujo A y B)', icon: <Package className="w-4 h-4" /> },
    { id: 'bodegas', label: 'Catálogo de bodegas', icon: <Warehouse className="w-4 h-4" /> },
    { id: 'gmail_alertas', label: 'Avisos por Gmail', icon: <Mail className="w-4 h-4" />, badge: unreadAlerts },
    { id: 'cronograma_meta', label: 'Cronograma & Meta 90d', icon: <Clock className="w-4 h-4" /> },
    { id: 'usuarios', label: 'Usuarios y roles', icon: <Users className="w-4 h-4" /> },
    { id: 'auditoria', label: 'Auditoría inmutable', icon: <ShieldCheck className="w-4 h-4" /> },
    { id: 'ayuda', label: 'Requerimientos y Reglas', icon: <HelpCircle className="w-4 h-4" /> },
  ];

  const visibleItems = navItems.filter((item) =>
    canUserAccessModule(currentUser, item.id)
  );

  return (
    <>
      {/* Mobile backdrop */}
      {isOpenMobile && (
        <div
          onClick={onCloseMobile}
          className="fixed inset-0 z-40 bg-black/40 backdrop-blur-xs lg:hidden"
        />
      )}

      {/* Sidebar container */}
      <aside
        className={`fixed lg:sticky top-0 lg:top-18 left-0 z-50 lg:z-30 h-screen lg:h-[calc(100vh-4.5rem)] w-64 bg-white border-r border-[#6C6B6D]/20 shadow-sm flex flex-col justify-between transition-transform duration-300 ease-in-out ${
          isOpenMobile ? 'translate-x-0' : '-translate-x-full lg:translate-x-0'
        }`}
      >
        <div className="p-4 space-y-4 overflow-y-auto">
          {/* Mobile close button */}
          <div className="flex items-center justify-between lg:hidden pb-3 border-b border-slate-100">
            <span className="text-xs font-black uppercase text-[#2E2E2E]">Menú de Navegación</span>
            <button
              onClick={onCloseMobile}
              className="p-1 rounded-lg text-[#6C6B6D] hover:text-[#2E2E2E] hover:bg-slate-100 cursor-pointer"
            >
              <X className="w-5 h-5" />
            </button>
          </div>

          {/* User role context card in sidebar */}
          <div className="p-3 rounded-2xl bg-[#FFFBEA] border border-[#FFD100]/60 space-y-1">
            <div className="flex items-center justify-between">
              <span className="text-[10px] font-black uppercase tracking-wider text-[#515151]">
                Panel Activo:
              </span>
              <span className="text-[10px] font-mono font-bold text-amber-800">
                {currentUser.pais === 'China' ? '🇨🇳 China' : '🇪🇨 Ecuador'}
              </span>
            </div>
            <div className="text-xs font-black text-[#2E2E2E] truncate">
              {currentUser.nombre}
            </div>
            <div className="text-[10px] text-[#515151] truncate">
              {currentUser.cargo}
            </div>
          </div>

          {/* Navigation Links */}
          <nav className="space-y-1.5">
            {visibleItems.map((item) => {
              const isActive = currentModule === item.id;
              return (
                <button
                  key={item.id}
                  onClick={() => {
                    onSelectModule(item.id);
                    onCloseMobile();
                  }}
                  className={`w-full flex items-center justify-between px-3.5 py-2.5 rounded-xl text-xs font-bold transition-colors cursor-pointer text-left ${
                    isActive
                      ? 'bg-[#FFD100] text-[#2E2E2E] shadow-xs'
                      : 'text-[#515151] hover:bg-[#FFF6CC] hover:text-[#2E2E2E]'
                  }`}
                >
                  <div className="flex items-center gap-3">
                    <span className={isActive ? 'text-[#2E2E2E]' : 'text-[#6C6B6D]'}>
                      {item.icon}
                    </span>
                    <span>{item.label}</span>
                  </div>

                  {item.badge !== undefined && item.badge > 0 && (
                    <span className="px-1.5 py-0.5 rounded-full text-[10px] font-black bg-[#DC2626] text-white">
                      {item.badge}
                    </span>
                  )}
                </button>
              );
            })}
          </nav>
        </div>

        {/* Sidebar Footer info */}
        <div className="p-4 border-t border-slate-100 text-[11px] text-[#6C6B6D] space-y-1 bg-slate-50/50">
          <div className="flex items-center justify-between">
            <span className="font-bold text-[#515151]">Versión:</span>
            <span className="font-mono">Demo v2.0</span>
          </div>
          <div className="text-[10px] text-[#6C6B6D] leading-tight">
            DECOKASA S.A.S · Octubre 2026
          </div>
        </div>
      </aside>
    </>
  );
};

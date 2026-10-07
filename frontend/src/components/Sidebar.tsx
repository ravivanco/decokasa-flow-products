import React from 'react';
import { AppModule, Usuario, AlertaItem } from '../types';
import { canUserAccessModule } from '../utils/permissions';
import {
  Home,
  Search,
  Package,
  Sparkles,
  Bell,
  BarChart3,
  Users,
  ShieldCheck,
  Settings,
  HelpCircle,
  X,
  ExternalLink,
} from 'lucide-react';

interface SidebarProps {
  currentModule: AppModule;
  onSelectModule: (module: AppModule) => void;
  currentUser: Usuario;
  alertas: AlertaItem[];
  isOpenMobile: boolean;
  onCloseMobile: () => void;
}

export const Sidebar: React.FC<SidebarProps> = ({
  currentModule,
  onSelectModule,
  currentUser,
  alertas,
  isOpenMobile,
  onCloseMobile,
}) => {
  const unreadAlerts = alertas.filter((a) => !a.leida).length;

  const navItems: { id: AppModule; label: string; icon: React.ReactNode; badge?: number }[] = [
    { id: 'inicio', label: 'Inicio', icon: <Home className="w-4 h-4" /> },
    { id: 'rastreo', label: 'Rastrear pedido', icon: <Search className="w-4 h-4" /> },
    { id: 'pedidos', label: 'Pedidos', icon: <Package className="w-4 h-4" /> },
    { id: 'productos_nuevos', label: 'Productos nuevos', icon: <Sparkles className="w-4 h-4" /> },
    { id: 'alertas', label: 'Alertas', icon: <Bell className="w-4 h-4" />, badge: unreadAlerts },
    { id: 'reportes', label: 'Reportes', icon: <BarChart3 className="w-4 h-4" /> },
    { id: 'usuarios', label: 'Usuarios y roles', icon: <Users className="w-4 h-4" /> },
    { id: 'auditoria', label: 'Auditoría', icon: <ShieldCheck className="w-4 h-4" /> },
    { id: 'configuracion', label: 'Configuración', icon: <Settings className="w-4 h-4" /> },
    { id: 'ayuda', label: 'Ayuda', icon: <HelpCircle className="w-4 h-4" /> },
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

      {/* Sidebar container: White background, active #FFD100 with #2E2E2E, hover #FFF6CC */}
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
              className="p-1 rounded-lg text-[#6C6B6D] hover:text-[#2E2E2E] hover:bg-slate-100"
            >
              <X className="w-5 h-5" />
            </button>
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
                    <span
                      className={`px-1.5 py-0.5 rounded-full text-[10px] font-black ${
                        isActive
                          ? 'bg-[#2E2E2E] text-white'
                          : 'bg-[#DC2626] text-white'
                      }`}
                    >
                      {item.badge}
                    </span>
                  )}
                </button>
              );
            })}
          </nav>
        </div>

        {/* Sidebar footer with current user role summary */}
        <div className="p-4 border-t border-[#6C6B6D]/15 bg-[#FFFBEA]">
          <div className="text-[11px] leading-tight">
            <span className="text-[#6C6B6D] block font-semibold">Sesión activa:</span>
            <strong className="text-[#2E2E2E] block font-black mt-0.5 truncate">
              {currentUser.nombre}
            </strong>
            <span className="text-[10px] text-[#515151] font-semibold block truncate">
              {currentUser.cargo}
            </span>
          </div>
        </div>
      </aside>
    </>
  );
};

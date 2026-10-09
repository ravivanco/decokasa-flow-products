import React, { useState } from 'react';
import {
  Usuario,
  Pedido,
  Pais,
  AppModule,
  BodegaCatalogo,
  NotificacionGmail,
  UrgenciaNivel,
} from './types';
import {
  USUARIOS_DEMO,
  PEDIDOS_INICIALES,
  BODEGAS_CATALOGO_INICIAL,
  NOTIFICACIONES_GMAIL_INICIALES,
} from './data/initialData';
import { LoginScreen } from './components/LoginScreen';
import { Header } from './components/Header';
import { Sidebar } from './components/Sidebar';
import { DashboardView } from './components/DashboardView';
import { OrdersListView } from './components/OrdersListView';
import { OrderDetail } from './components/OrderDetail';
import { TrackingSearchView } from './components/TrackingSearchView';
import { WarehouseCatalogView } from './components/WarehouseCatalogView';
import { GmailNotificationsView } from './components/GmailNotificationsView';
import { TimelineCalculatorView } from './components/TimelineCalculatorView';
import { UsersManagementView } from './components/UsersManagementView';
import { AuditTrailView } from './components/AuditTrailView';
import { HelpView } from './components/HelpView';
import { ToastContainer, ToastMessage } from './components/Toast';
import {
  advanceOrderStage,
  rejectStageWithObservations,
  returnToCodingWithUrgency,
  reportCustomsDelay,
  setOrderDestinationWarehouse,
  setOrderStatusChange,
  toggleStageChecklist,
  addStageObservation,
  uploadStageFile,
  updateProductQuantityReceived,
} from './utils/orderState';
import { canUserAccessModule } from './utils/permissions';
import { formatDisplayDate, SIMULATED_TODAY } from './utils/dateUtils';
import { X, Sparkles } from 'lucide-react';

export default function App() {
  // Estado de sesión: default a Mirian Franco (Admin) para pruebas inmediatas, o null para ver Login
  const [currentUser, setCurrentUser] = useState<Usuario | null>(USUARIOS_DEMO[0]);
  const [pais, setPais] = useState<Pais>('EC');
  const [currentModule, setCurrentModule] = useState<AppModule>('inicio');
  const [isMobileMenuOpen, setIsMobileMenuOpen] = useState(false);
  const [showUserSwitchModal, setShowUserSwitchModal] = useState(false);

  // Datos en memoria
  const [pedidos, setPedidos] = useState<Pedido[]>(PEDIDOS_INICIALES);
  const [selectedPedidoId, setSelectedPedidoId] = useState<string | null>(null);
  const [bodegas, setBodegas] = useState<BodegaCatalogo[]>(BODEGAS_CATALOGO_INICIAL);
  const [notificaciones, setNotificaciones] = useState<NotificacionGmail[]>(
    NOTIFICACIONES_GMAIL_INICIALES
  );
  const [toasts, setToasts] = useState<ToastMessage[]>([]);

  // Toast feedback
  const showToast = (text: string, type: 'success' | 'error' | 'info' = 'success') => {
    const id = `toast-${Date.now()}-${Math.random()}`;
    setToasts((prev) => [...prev, { id, text, type }]);
    setTimeout(() => {
      setToasts((prev) => prev.filter((t) => t.id !== id));
    }, 4000);
  };

  const removeToast = (id: string) => {
    setToasts((prev) => prev.filter((t) => t.id !== id));
  };

  // Login & Logout
  const handleLoginSuccess = (user: Usuario, selectedPais: Pais) => {
    setCurrentUser(user);
    setPais(selectedPais);
    setCurrentModule('inicio');
    showToast(`Bienvenido a Decokasa Tracking, ${user.nombre}`, 'success');
  };

  const handleLogout = () => {
    setCurrentUser(null);
    setSelectedPedidoId(null);
    setCurrentModule('inicio');
    showToast('Sesión cerrada. Puedes ingresar con otro rol o credenciales.', 'info');
  };

  // Pedido activo
  const selectedPedido = selectedPedidoId
    ? pedidos.find((p) => p.id === selectedPedidoId) || null
    : null;

  const updatePedidoInState = (updated: Pedido) => {
    setPedidos((prev) => prev.map((p) => (p.id === updated.id ? updated : p)));
  };

  // Operaciones de pedido
  const handleAdvanceStage = (observaciones?: string) => {
    if (!selectedPedido || !currentUser) return;
    const { updatedPedido, message } = advanceOrderStage(selectedPedido, currentUser, observaciones);
    updatePedidoInState(updatedPedido);
    showToast(message, 'success');

    // Registrar notificación Gmail simulada
    const newGmail: NotificacionGmail = {
      id: `gml-${Date.now()}`,
      pedidoCodigo: selectedPedido.codigo,
      asunto: `✓ Avance de Etapa: ${selectedPedido.codigo} completó etapa ${selectedPedido.etapaActualNumero}`,
      destinatarioEmail: 'mfranco@decokasa.ec',
      destinatarioNombre: 'Mirian Franco (Admin)',
      destinatarioRol: 'Admin',
      remitente: `${currentUser.email} (${currentUser.nombre})`,
      fechaHora: `${formatDisplayDate(SIMULATED_TODAY)} 12:30`,
      contenido: `El usuario ${currentUser.nombre} (${currentUser.cargo}) dio check en la etapa ${selectedPedido.etapaActualNumero}. Observaciones: ${observaciones || 'Ninguna'}.`,
      etapaNombre: selectedPedido.etapas[selectedPedido.etapaActualNumero - 1]?.nombre || '',
      leida: false,
    };
    setNotificaciones((prev) => [newGmail, ...prev]);
  };

  const handleRejectStage = (motivo: string) => {
    if (!selectedPedido || !currentUser) return;
    const { updatedPedido, message } = rejectStageWithObservations(selectedPedido, currentUser, motivo);
    updatePedidoInState(updatedPedido);
    showToast(message, 'info');

    const newGmail: NotificacionGmail = {
      id: `gml-${Date.now()}`,
      pedidoCodigo: selectedPedido.codigo,
      asunto: `✗ DESAPROBADO: ${selectedPedido.codigo} devuelto con observaciones`,
      destinatarioEmail: 'mfranco@decokasa.ec',
      destinatarioNombre: 'Mirian Franco (Admin)',
      destinatarioRol: 'Admin',
      remitente: `${currentUser.email} (${currentUser.nombre})`,
      fechaHora: `${formatDisplayDate(SIMULATED_TODAY)} 14:15`,
      contenido: `${currentUser.nombre} desaprobó el pedido en la etapa de Revisión. Motivo: "${motivo}". El pedido volvió a Codificación.`,
      etapaNombre: 'Revisión y aprobación',
      leida: false,
    };
    setNotificaciones((prev) => [newGmail, ...prev]);
  };

  const handleReturnWithUrgency = (motivo: string, urgencia: UrgenciaNivel) => {
    if (!selectedPedido || !currentUser) return;
    const { updatedPedido, message } = returnToCodingWithUrgency(selectedPedido, currentUser, motivo, urgencia);
    updatePedidoInState(updatedPedido);
    showToast(message, 'success');

    const newGmail: NotificacionGmail = {
      id: `gml-${Date.now()}`,
      pedidoCodigo: selectedPedido.codigo,
      asunto: `🚨 DEVOLUCIÓN CON URGENCIA (${urgencia}): ${selectedPedido.codigo} devuelto desde China`,
      destinatarioEmail: 'mfranco@decokasa.ec',
      destinatarioNombre: 'Mirian Franco (Admin)',
      destinatarioRol: 'Admin',
      remitente: `${currentUser.email} (David Túlcan · China)`,
      fechaHora: `${formatDisplayDate(SIMULATED_TODAY)} 10:00`,
      contenido: `David Túlcan devolvió el pedido a Codificación con urgencia ${urgencia} (${urgencia === 'Urgente' ? '4 h' : urgencia === 'Prioritario' ? '12 h' : '24 h'}). Motivo: "${motivo}".`,
      etapaNombre: 'Codificación',
      urgencia,
      leida: false,
    };
    setNotificaciones((prev) => [newGmail, ...prev]);
  };

  const handleReportCustomsDelay = (motivo: string, nuevaFecha: string, dias: number) => {
    if (!selectedPedido || !currentUser) return;
    const { updatedPedido, message } = reportCustomsDelay(selectedPedido, currentUser, motivo, nuevaFecha, dias);
    updatePedidoInState(updatedPedido);
    showToast(message, 'info');

    const newGmail: NotificacionGmail = {
      id: `gml-${Date.now()}`,
      pedidoCodigo: selectedPedido.codigo,
      asunto: `⏱ AVISO SENAE: Demora reportada en Aduana para ${selectedPedido.codigo} (+${dias} días)`,
      destinatarioEmail: 'mfranco@decokasa.ec',
      destinatarioNombre: 'Mirian Franco (Admin)',
      destinatarioRol: 'Admin',
      remitente: `${currentUser.email} (${currentUser.nombre})`,
      fechaHora: `${formatDisplayDate(SIMULATED_TODAY)} 16:30`,
      contenido: `Se reportó una demora aduanera de +${dias} días hábiles. Motivo: "${motivo}". El cronograma se recalculó automáticamente.`,
      etapaNombre: 'Aduana Ecuador o Colombia',
      esAlertaRetraso: true,
      leida: false,
    };
    setNotificaciones((prev) => [newGmail, ...prev]);
  };

  const handleSelectWarehouse = (bodegaNombre: string) => {
    if (!selectedPedido || !currentUser) return;
    const updated = setOrderDestinationWarehouse(selectedPedido, currentUser, bodegaNombre);
    updatePedidoInState(updated);
    showToast(`Bodega de destino asignada: ${bodegaNombre}`, 'success');
  };

  const handleOrderStatusChange = (action: 'pausar' | 'reanudar' | 'cancelar', motivo?: string) => {
    if (!selectedPedido || !currentUser) return;
    const { updatedPedido, message } = setOrderStatusChange(selectedPedido, currentUser, action, motivo);
    updatePedidoInState(updatedPedido);
    showToast(message, 'success');
  };

  const handleToggleChecklist = (stageNum: number, checkId: string) => {
    if (!selectedPedido || !currentUser) return;
    const updated = toggleStageChecklist(selectedPedido, stageNum, checkId, currentUser);
    updatePedidoInState(updated);
  };

  const handleAddObservation = (stageNum: number, texto: string) => {
    if (!selectedPedido || !currentUser) return;
    const updated = addStageObservation(selectedPedido, stageNum, currentUser, texto);
    updatePedidoInState(updated);
    showToast('Observación publicada en el pedido', 'success');
  };

  const handleUploadFile = (
    stageNum: number,
    docId: string,
    fileData: { nombre: string; tamano: string }
  ) => {
    if (!selectedPedido || !currentUser) return;
    const updated = uploadStageFile(selectedPedido, stageNum, docId, fileData, currentUser);
    updatePedidoInState(updated);
    showToast(`Archivo "${fileData.nombre}" subido a Google Drive exitosamente`, 'success');
  };

  const handleUpdateProductQuantity = (lineId: string, recibida: number, incidencia?: string) => {
    if (!selectedPedido) return;
    const updated = updateProductQuantityReceived(selectedPedido, lineId, recibida, incidencia);
    updatePedidoInState(updated);
    showToast('Recepción en bodega actualizada', 'success');
  };

  const handleAddNewOrder = (newOrder: Pedido) => {
    setPedidos((prev) => [newOrder, ...prev]);
    setSelectedPedidoId(newOrder.id);
    setCurrentModule('detalle_pedido');
    showToast(`Solicitud ${newOrder.codigo} creada exitosamente`, 'success');
  };

  const handleAddWarehouse = (nueva: BodegaCatalogo) => {
    setBodegas((prev) => [...prev, nueva]);
    showToast(`Bodega "${nueva.nombre}" agregada al catálogo`, 'success');
  };

  const handleToggleWarehouseStatus = (id: string) => {
    setBodegas((prev) =>
      prev.map((b) => (b.id === id ? { ...b, activa: !b.activa } : b))
    );
    showToast('Estado de bodega actualizado', 'info');
  };

  const handleMarkGmailAsRead = (id: string) => {
    setNotificaciones((prev) =>
      prev.map((n) => (n.id === id ? { ...n, leida: true } : n))
    );
  };

  // Si no hay usuario logueado, mostrar pantalla de Login
  if (!currentUser) {
    return (
      <>
        <LoginScreen
          onLoginSuccess={handleLoginSuccess}
          selectedPais={pais}
          onSelectPais={setPais}
        />
        <ToastContainer toasts={toasts} onDismiss={removeToast} />
      </>
    );
  }

  // Comprobar permiso de módulo
  const safeModule: AppModule = canUserAccessModule(currentUser, currentModule)
    ? currentModule
    : 'inicio';

  return (
    <div className="min-h-screen bg-[#F4F4F4] text-[#515151] flex flex-col font-sans selection:bg-[#FFD100] selection:text-[#2E2E2E]">
      {/* 1. Header con identidad Decokasa, país, fecha simulada y alertas */}
      <Header
        currentUser={currentUser}
        pais={pais}
        onTogglePais={() => {
          const next = pais === 'EC' ? 'CO' : 'EC';
          setPais(next);
          showToast(
            next === 'CO'
              ? 'País cambiado a Colombia (Proceso en diseño para Fase 2)'
              : 'País cambiado a Ecuador (Proceso oficial activo)',
            'info'
          );
        }}
        onLogout={handleLogout}
        onToggleMobileMenu={() => setIsMobileMenuOpen((prev) => !prev)}
        notificaciones={notificaciones}
        onOpenGmailDrawer={() => setCurrentModule('gmail_alertas')}
        onSwitchUserPrompt={() => setShowUserSwitchModal(true)}
      />

      {/* 2. Layout principal con Menú lateral y Contenido */}
      <div className="flex-1 flex max-w-7xl w-full mx-auto">
        <Sidebar
          currentModule={safeModule}
          onSelectModule={(mod) => {
            if (mod === 'detalle_pedido' && !selectedPedidoId) {
              setSelectedPedidoId(pedidos[0].id);
            }
            setCurrentModule(mod);
          }}
          currentUser={currentUser}
          notificaciones={notificaciones}
          isOpenMobile={isMobileMenuOpen}
          onCloseMobile={() => setIsMobileMenuOpen(false)}
        />

        {/* Área de Contenido Principal */}
        <main className="flex-1 p-4 sm:p-6 lg:p-8 min-w-0">
          {safeModule === 'inicio' && (
            <DashboardView
              pedidos={pedidos}
              currentUser={currentUser}
              onSelectPedido={(p) => {
                setSelectedPedidoId(p.id);
                setCurrentModule('detalle_pedido');
              }}
              onNavigateToTracking={() => setCurrentModule('rastreo')}
              onNavigateToNewOrderA={() => setCurrentModule('pedidos')}
              onNavigateToNewOrderB={() => setCurrentModule('pedidos')}
              onNavigateToWarehouses={() => setCurrentModule('bodegas')}
            />
          )}

          {safeModule === 'rastreo' && (
            <TrackingSearchView
              pedidos={pedidos}
              onOpenOrderDetail={(p) => {
                setSelectedPedidoId(p.id);
                setCurrentModule('detalle_pedido');
              }}
            />
          )}

          {safeModule === 'pedidos' && (
            <OrdersListView
              pedidos={pedidos}
              currentUser={currentUser}
              onSelectPedido={(p) => {
                setSelectedPedidoId(p.id);
                setCurrentModule('detalle_pedido');
              }}
              onAddNewOrder={handleAddNewOrder}
            />
          )}

          {safeModule === 'detalle_pedido' && selectedPedido && (
            <OrderDetail
              pedido={selectedPedido}
              currentUser={currentUser}
              bodegasCatalogo={bodegas.filter((b) => b.activa)}
              onBackToOrders={() => setCurrentModule('pedidos')}
              onAdvanceStage={handleAdvanceStage}
              onRejectStage={handleRejectStage}
              onReturnWithUrgency={handleReturnWithUrgency}
              onReportCustomsDelay={handleReportCustomsDelay}
              onSelectWarehouse={handleSelectWarehouse}
              onOrderStatusChange={handleOrderStatusChange}
              onToggleChecklist={handleToggleChecklist}
              onAddObservation={handleAddObservation}
              onUploadFile={handleUploadFile}
              onUpdateProductQuantity={handleUpdateProductQuantity}
              onShowToast={(msg) => showToast(msg, 'info')}
            />
          )}

          {safeModule === 'bodegas' && (
            <WarehouseCatalogView
              bodegas={bodegas}
              currentUser={currentUser}
              onAddWarehouse={handleAddWarehouse}
              onToggleWarehouseStatus={handleToggleWarehouseStatus}
            />
          )}

          {safeModule === 'gmail_alertas' && (
            <GmailNotificationsView
              notificaciones={notificaciones}
              onMarkAsRead={handleMarkGmailAsRead}
              onSelectPedidoCodigo={(codigo) => {
                const found = pedidos.find((p) => p.codigo === codigo);
                if (found) {
                  setSelectedPedidoId(found.id);
                  setCurrentModule('detalle_pedido');
                } else {
                  setCurrentModule('pedidos');
                }
              }}
            />
          )}

          {safeModule === 'cronograma_meta' && (
            <TimelineCalculatorView onShowToast={(msg) => showToast(msg, 'info')} />
          )}

          {safeModule === 'usuarios' && (
            <UsersManagementView
              currentUser={currentUser}
              onSwitchUser={(u) => {
                setCurrentUser(u);
                showToast(`Sesión cambiada a: ${u.nombre} (${u.cargo})`, 'info');
              }}
              onShowToast={(msg) => showToast(msg, 'info')}
            />
          )}

          {safeModule === 'auditoria' && <AuditTrailView pedidos={pedidos} />}

          {safeModule === 'ayuda' && <HelpView />}
        </main>
      </div>

      {/* 3. Footer institucional Decokasa S.A.S */}
      <footer className="bg-white border-t border-[#6C6B6D]/20 py-4 text-center text-xs text-[#515151]">
        <div className="max-w-7xl mx-auto px-4 flex flex-col sm:flex-row items-center justify-between gap-2">
          <div className="font-bold text-[#2E2E2E]">
            © 2026 Decokasa S.A.S — Sistema de Seguimiento y Control de Importaciones Marítimas
          </div>
          <div className="text-[11px] text-[#6C6B6D] font-mono">
            Fecha de cálculo simulada: 09/10/2026 · Hora de Ecuador (UTC−5)
          </div>
        </div>
      </footer>

      {/* Modal Rápido de Cambio de Usuario */}
      {showUserSwitchModal && (
        <div className="fixed inset-0 z-50 bg-black/50 backdrop-blur-xs flex items-center justify-center p-4">
          <div className="bg-white rounded-3xl max-w-lg w-full p-6 space-y-4 shadow-2xl animate-in zoom-in-95">
            <div className="flex items-center justify-between border-b border-slate-100 pb-3">
              <div>
                <h3 className="text-base font-black text-[#2E2E2E] flex items-center gap-1.5">
                  <Sparkles className="w-4 h-4 text-amber-500" />
                  <span>Simulador de Roles de Decokasa S.A.S</span>
                </h3>
                <p className="text-xs text-[#6C6B6D]">
                  Cambia de rol con un solo clic para comprobar la experiencia de cada responsable.
                </p>
              </div>
              <button
                type="button"
                onClick={() => setShowUserSwitchModal(false)}
                className="p-1 rounded-lg text-[#6C6B6D] hover:bg-slate-100 cursor-pointer"
              >
                <X className="w-5 h-5" />
              </button>
            </div>

            <div className="space-y-2">
              {USUARIOS_DEMO.map((u) => {
                const isCurrent = currentUser.id === u.id;
                return (
                  <button
                    key={u.id}
                    type="button"
                    onClick={() => {
                      setCurrentUser(u);
                      setShowUserSwitchModal(false);
                      showToast(`Sesión cambiada a: ${u.nombre} (${u.cargo})`, 'info');
                    }}
                    className={`w-full p-3 rounded-2xl border text-left flex items-center justify-between gap-3 transition cursor-pointer ${
                      isCurrent
                        ? 'border-2 border-[#FFD100] bg-[#FFFBEA]'
                        : 'border-slate-200 hover:bg-slate-50'
                    }`}
                  >
                    <div className="flex items-center gap-3 min-w-0">
                      <div className="w-8 h-8 rounded-xl bg-[#2E2E2E] text-white flex items-center justify-center font-black text-xs shrink-0">
                        {u.avatar}
                      </div>
                      <div className="min-w-0">
                        <div className="text-xs font-black text-[#2E2E2E] truncate">
                          {u.nombre}
                        </div>
                        <div className="text-[11px] text-[#6C6B6D] truncate">
                          {u.cargo}
                        </div>
                      </div>
                    </div>

                    <div className="text-right shrink-0">
                      <span className="text-[10px] font-mono text-[#515151]">
                        {u.pais === 'China' ? '🇨🇳 China' : '🇪🇨 Ecuador'}
                      </span>
                      {isCurrent && (
                        <span className="block text-[10px] font-black text-emerald-600">
                          (Activo)
                        </span>
                      )}
                    </div>
                  </button>
                );
              })}
            </div>
          </div>
        </div>
      )}

      {/* Global Toast notifications */}
      <ToastContainer toasts={toasts} onDismiss={removeToast} />
    </div>
  );
}

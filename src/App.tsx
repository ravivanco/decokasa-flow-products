import React, { useState } from 'react';
import {
  Usuario,
  Pedido,
  Language,
  SubestadoAduana,
  BodegaDestino,
  AppModule,
  SolicitudProductoNuevo,
  AlertaItem,
} from './types';
import {
  USUARIOS_DEMO,
  PEDIDOS_DEMO,
  SOLICITUDES_PRODUCTOS_NUEVOS_DEMO,
  ALERTAS_INICIALES,
} from './data/initialData';
import { LoginScreen } from './components/LoginScreen';
import { Header } from './components/Header';
import { Sidebar } from './components/Sidebar';
import { DashboardView } from './components/DashboardView';
import { OrdersListView } from './components/OrdersListView';
import { OrderDetail } from './components/OrderDetail';
import { TrackingSearchView } from './components/TrackingSearchView';
import { NewProductsView } from './components/NewProductsView';
import { AlertsView } from './components/AlertsView';
import { ReportsView } from './components/ReportsView';
import { UsersManagementView } from './components/UsersManagementView';
import { AuditTrailView } from './components/AuditTrailView';
import { ConfigurationView } from './components/ConfigurationView';
import { HelpView } from './components/HelpView';
import { ToastContainer, ToastMessage } from './components/Toast';
import {
  advanceOrderStage,
  returnOrderWithObservations,
  revertOrderCheck,
  extendFabricationDays,
  updateCustomsSubstate,
  setOrderStatusChange,
  toggleStageChecklist,
  addStageObservation,
  uploadStageFile,
  updateProductQuantityReceived,
  updateOrderDatesManually,
} from './utils/orderState';
import { canUserAccessModule } from './utils/permissions';

export default function App() {
  // Authentication: default to Mirian Franco (Admin) for immediate live testing, but can log out to test LoginScreen
  const [currentUser, setCurrentUser] = useState<Usuario | null>(USUARIOS_DEMO[0]);
  const [currentModule, setCurrentModule] = useState<AppModule>('inicio');
  const [lang, setLang] = useState<Language>('es');
  const [isMobileMenuOpen, setIsMobileMenuOpen] = useState(false);

  // In-Memory Data State
  const [pedidos, setPedidos] = useState<Pedido[]>(PEDIDOS_DEMO);
  const [selectedPedidoId, setSelectedPedidoId] = useState<string | null>(null);
  const [productosNuevos, setProductosNuevos] = useState<SolicitudProductoNuevo[]>(
    SOLICITUDES_PRODUCTOS_NUEVOS_DEMO
  );
  const [alertas, setAlertas] = useState<AlertaItem[]>(ALERTAS_INICIALES);
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

  // Login handler
  const handleLoginSuccess = (user: Usuario) => {
    setCurrentUser(user);
    setCurrentModule('inicio');
    showToast(`Bienvenido a Decokasa Tracking, ${user.nombre}`, 'success');
  };

  const handleLogout = () => {
    setCurrentUser(null);
    setSelectedPedidoId(null);
    setCurrentModule('inicio');
    showToast('Sesión cerrada correctamente', 'info');
  };

  // Active selected order
  const selectedPedido = selectedPedidoId
    ? pedidos.find((p) => p.id === selectedPedidoId) || null
    : null;

  // Generic updater helper
  const updatePedidoInState = (updated: Pedido) => {
    setPedidos((prev) => prev.map((p) => (p.id === updated.id ? updated : p)));
  };

  // Order Operations
  const handleAdvanceStage = (observaciones?: string) => {
    if (!selectedPedido || !currentUser) return;
    const { updatedPedido, message } = advanceOrderStage(selectedPedido, currentUser, observaciones);
    updatePedidoInState(updatedPedido);
    showToast(message, 'success');
  };

  const handleReturnStage = (motivo: string) => {
    if (!selectedPedido || !currentUser) return;
    const { updatedPedido, message } = returnOrderWithObservations(selectedPedido, currentUser, motivo);
    updatePedidoInState(updatedPedido);
    showToast(message, 'info');
  };

  const handleRevertStage = (motivo: string) => {
    if (!selectedPedido || !currentUser) return;
    const { updatedPedido, message } = revertOrderCheck(selectedPedido, currentUser, motivo);
    updatePedidoInState(updatedPedido);
    showToast(message, 'info');
  };

  const handleExtendFabrication = (dias: number, motivo: string) => {
    if (!selectedPedido || !currentUser) return;
    const { updatedPedido, message, error } = extendFabricationDays(selectedPedido, currentUser, dias, motivo);
    if (error) {
      showToast(error, 'error');
      return;
    }
    updatePedidoInState(updatedPedido);
    showToast(message, 'success');
  };

  const handleUpdateCustomsSubstate = (subestado: SubestadoAduana, motivo?: string) => {
    if (!selectedPedido || !currentUser) return;
    const { updatedPedido, message } = updateCustomsSubstate(selectedPedido, currentUser, subestado, motivo);
    updatePedidoInState(updatedPedido);
    showToast(message, 'success');
  };

  const handleOrderStatusChange = (
    action: 'pausar' | 'reanudar' | 'cancelar' | 'reabrir' | 'aprobar_fabricacion',
    motivo?: string
  ) => {
    if (!selectedPedido || !currentUser) return;
    const { updatedPedido, message } = setOrderStatusChange(selectedPedido, currentUser, action, motivo);
    updatePedidoInState(updatedPedido);
    showToast(message, 'success');
  };

  const handleToggleChecklist = (stageNum: number, checkId: string) => {
    if (!selectedPedido || !currentUser) return;
    const updated = toggleStageChecklist(selectedPedido, stageNum, checkId, currentUser);
    updatePedidoInState(updated);
    showToast('Checklist actualizado', 'success');
  };

  const handleAddObservation = (stageNum: number, texto: string) => {
    if (!selectedPedido || !currentUser) return;
    const updated = addStageObservation(selectedPedido, stageNum, currentUser, texto);
    updatePedidoInState(updated);
    showToast('Observación publicada', 'success');
  };

  const handleUploadFile = (
    stageNum: number,
    docId: string,
    fileData: { nombre: string; tamano: string; driveUrl?: string; esImagen?: boolean }
  ) => {
    if (!selectedPedido || !currentUser) return;
    const updated = uploadStageFile(selectedPedido, stageNum, docId, fileData, currentUser);
    updatePedidoInState(updated);
    showToast(`Archivo "${fileData.nombre}" subido exitosamente`, 'success');
  };

  const handleUpdateProductQuantity = (lineId: string, recibida: number, incidencia?: string) => {
    if (!selectedPedido) return;
    const updated = updateProductQuantityReceived(selectedPedido, lineId, recibida, incidencia);
    updatePedidoInState(updated);
    showToast('Recepción de producto actualizada', 'success');
  };

  const handleEditDatesManually = (nuevaFecha: string, motivo: string) => {
    if (!selectedPedido || !currentUser) return;
    const updated = updateOrderDatesManually(selectedPedido, nuevaFecha, currentUser, motivo);
    updatePedidoInState(updated);
    showToast('Cronograma de pedido recalculado con éxito', 'success');
  };

  const handleCreateNewOrder = (newOrder: Pedido) => {
    setPedidos((prev) => [newOrder, ...prev]);
    setSelectedPedidoId(newOrder.id);
    setCurrentModule('detalle_pedido');
    showToast(`Pedido ${newOrder.codigo} creado en Etapa 1`, 'success');
  };

  // Alerts
  const handleMarkAlertAsRead = (alertId: string) => {
    setAlertas((prev) =>
      prev.map((a) => (a.id === alertId ? { ...a, leida: true } : a))
    );
  };

  // New Products
  const handleUpdateProductoNuevo = (updated: SolicitudProductoNuevo) => {
    setProductosNuevos((prev) => prev.map((p) => (p.id === updated.id ? updated : p)));
    showToast(`Solicitud de ${updated.nombre} actualizada`, 'success');
  };

  const handleCrearSolicitudNuevo = (solicitud: SolicitudProductoNuevo) => {
    setProductosNuevos((prev) => [solicitud, ...prev]);
    showToast(`Solicitud creada para ${solicitud.nombre}`, 'success');
  };

  // If user is not logged in, show Login Screen
  if (!currentUser) {
    return (
      <>
        <LoginScreen onLoginSuccess={handleLoginSuccess} />
        <ToastContainer toasts={toasts} onDismiss={removeToast} />
      </>
    );
  }

  // Navigation module safety
  const safeModule: AppModule = canUserAccessModule(currentUser, currentModule)
    ? currentModule
    : 'inicio';

  return (
    <div className="min-h-screen bg-[#F4F4F4] text-[#515151] flex flex-col font-sans selection:bg-[#FFD100] selection:text-[#2E2E2E]">
      {/* 1. Header institucional amarillo dominante #FFD100 */}
      <Header
        currentUser={currentUser}
        lang={lang}
        onToggleLang={() => setLang((prev) => (prev === 'es' ? 'en' : 'es'))}
        onLogout={handleLogout}
        onToggleMobileMenu={() => setIsMobileMenuOpen((prev) => !prev)}
        alertas={alertas}
        onOpenAlerts={() => setCurrentModule('alertas')}
      />

      {/* 2. Main Layout with Sidebar + View Container */}
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
          alertas={alertas}
          isOpenMobile={isMobileMenuOpen}
          onCloseMobile={() => setIsMobileMenuOpen(false)}
        />

        {/* Content Area */}
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
              onAddNewOrder={handleCreateNewOrder}
            />
          )}

          {safeModule === 'detalle_pedido' && selectedPedido && (
            <OrderDetail
              pedido={selectedPedido}
              currentUser={currentUser}
              onBackToOrders={() => setCurrentModule('pedidos')}
              onAdvanceStage={handleAdvanceStage}
              onReturnStage={handleReturnStage}
              onRevertStage={handleRevertStage}
              onExtendFabrication={handleExtendFabrication}
              onUpdateCustomsSubstate={handleUpdateCustomsSubstate}
              onOrderStatusChange={handleOrderStatusChange}
              onToggleChecklist={handleToggleChecklist}
              onAddObservation={handleAddObservation}
              onUploadFile={handleUploadFile}
              onUpdateProductQuantity={handleUpdateProductQuantity}
              onEditDatesManually={handleEditDatesManually}
            />
          )}

          {safeModule === 'productos_nuevos' && (
            <NewProductsView
              productosNuevos={productosNuevos}
              currentUser={currentUser}
              onUpdateProductoNuevo={handleUpdateProductoNuevo}
              onCrearSolicitudNuevo={handleCrearSolicitudNuevo}
            />
          )}

          {safeModule === 'alertas' && (
            <AlertsView
              alertas={alertas}
              onMarkAsRead={handleMarkAlertAsRead}
              onSelectPedidoById={(pId) => {
                setSelectedPedidoId(pId);
                setCurrentModule('detalle_pedido');
              }}
            />
          )}

          {safeModule === 'reportes' && (
            <ReportsView pedidos={pedidos} onShowToast={(msg) => showToast(msg, 'info')} />
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

          {safeModule === 'configuracion' && (
            <ConfigurationView
              currentUser={currentUser}
              onShowToast={(msg) => showToast(msg, 'success')}
            />
          )}

          {safeModule === 'ayuda' && <HelpView />}
        </main>
      </div>

      {/* 3. Footer corporativo Decokasa S.A.S */}
      <footer className="bg-white border-t border-[#6C6B6D]/20 py-4 text-center text-xs text-[#515151]">
        <div className="max-w-7xl mx-auto px-4 flex flex-col sm:flex-row items-center justify-between gap-2">
          <div className="font-bold text-[#2E2E2E]">
            © 2026 Decokasa S.A.S — Sistema de seguimiento de importaciones
          </div>
          <div className="text-[11px] text-[#6C6B6D] font-mono">
            {lang === 'es'
              ? 'Fecha de cálculo simulada: 06/10/2026'
              : 'Simulated reference date: 10/06/2026'}
          </div>
        </div>
      </footer>

      {/* 4. Global Toast notifications */}
      <ToastContainer toasts={toasts} onDismiss={removeToast} />
    </div>
  );
}

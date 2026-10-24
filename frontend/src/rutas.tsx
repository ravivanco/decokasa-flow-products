import { Navigate, type RouteObject } from 'react-router';
import App from './App';
import { CatalogoBodegas } from './components/CatalogoBodegas';
import { ConfiguracionPlazos } from './components/ConfiguracionPlazos';
import { GmailNotificationsView } from './components/GmailNotificationsView';
import { RutaProtegida, SoloAdmin } from './auth/RutaProtegida';
import { DashboardRol } from './components/dashboards/DashboardRol';
import { ErrorRuta } from './components/ErrorRuta';
import { LoginScreen } from './components/LoginScreen';
import { PaginaNoEncontrada } from './components/PaginasSimples';
import { BandejaPedidos } from './components/pedidos/BandejaPedidos';
import { AsistentePedido } from './components/asistente/AsistentePedido';
import { DetallePedido } from './components/pedidos/DetallePedido';
import { ListaPedidos } from './components/pedidos/ListaPedidos';
import { NuevoPedido } from './components/pedidos/NuevoPedido';
import { RolesManagementView } from './components/RolesManagementView';
import { UsersManagementView } from './components/UsersManagementView';

// Rutas de la aplicación (plan, tarea 19): /login pública; el resto exige sesión, y /usuarios, /roles y la meta el rol
// admin. Crear pedidos (fase 6): A el admin y B Compras China; la pantalla lo deduce de las definiciones y el servidor
// lo decide. Pedidos y bandeja (fase 2, paso 2.5) leen la API real; el servidor valida cada permiso (R6).
// Bodegas usa la API y limita el mantenimiento a admin/Logística; Correos sigue limitado al admin.
// Un error que ninguna pantalla atrapó muestra ErrorRuta (sin detalles técnicos) en lugar de la página de React Router.
export const rutas: RouteObject[] = [
  { path: '/login', element: <LoginScreen />, errorElement: <ErrorRuta /> },
  {
    element: <RutaProtegida />,
    errorElement: <ErrorRuta />,
    children: [
      {
        element: <App />,
        children: [
          { index: true, element: <Navigate to="/inicio" replace /> },
          { path: '/inicio', element: <DashboardRol /> },
          { path: '/bandeja', element: <BandejaPedidos /> },
          { path: '/bodegas', element: <CatalogoBodegas /> },
          { path: '/pedidos', element: <ListaPedidos /> },
          { path: '/pedidos/nuevo', element: <NuevoPedido /> },
          { path: '/pedidos/:pedidoId', element: <DetallePedido /> },
          // Paso 7.5: chat del asistente por pedido. El servidor decide el acceso (F7-D4, país, pedido, P-17).
          { path: '/pedidos/:pedidoId/asistente', element: <AsistentePedido /> },
          {
            element: <SoloAdmin />,
            children: [
              { path: '/notificaciones', element: <GmailNotificationsView /> },
              { path: '/usuarios', element: <UsersManagementView /> },
              { path: '/roles', element: <RolesManagementView /> },
              { path: '/configuracion/plazos', element: <ConfiguracionPlazos /> },
            ],
          },
          { path: '*', element: <PaginaNoEncontrada /> },
        ],
      },
    ],
  },
];

import { Navigate, type RouteObject } from 'react-router';
import App from './App';
import { CatalogoBodegas } from './components/CatalogoBodegas';
import { ConfiguracionPlazos } from './components/ConfiguracionPlazos';
import { RutaProtegida, SoloAdmin } from './auth/RutaProtegida';
import { DashboardRol } from './components/dashboards/DashboardRol';
import { ErrorRuta } from './components/ErrorRuta';
import { LoginScreen } from './components/LoginScreen';
import { CambiarContrasena } from './components/acceso/CambiarContrasena';
import { RecuperarContrasena } from './components/acceso/RecuperarContrasena';
import { RestablecerContrasena } from './components/acceso/RestablecerContrasena';
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
// Bodegas usa la API y limita el mantenimiento a admin/Logística. La pantalla «Correos» se retiró de la interfaz el
// 2026-10-27 (las notificaciones y Gmail siguen en el servidor); su dirección antigua lleva al Inicio.
// Un error que ninguna pantalla atrapó muestra ErrorRuta (sin detalles técnicos) en lugar de la página de React Router.
export const rutas: RouteObject[] = [
  { path: '/login', element: <LoginScreen />, errorElement: <ErrorRuta /> },
  // ADR-0033: recuperación pública (sin sesión). El enlace del correo trae el token en el fragmento.
  { path: '/recuperar-contrasena', element: <RecuperarContrasena />, errorElement: <ErrorRuta /> },
  { path: '/restablecer-contrasena', element: <RestablecerContrasena />, errorElement: <ErrorRuta /> },
  {
    element: <RutaProtegida />,
    errorElement: <ErrorRuta />,
    children: [
      {
        element: <App />,
        children: [
          { index: true, element: <Navigate to="/inicio" replace /> },
          { path: '/inicio', element: <DashboardRol /> },
          // ADR-0029: las alertas viven en Mi bandeja; el enlace antiguo lleva allí.
          { path: '/alertas', element: <Navigate to="/bandeja#alertas" replace /> },
          { path: '/notificaciones', element: <Navigate to="/inicio" replace /> },
          { path: '/bandeja', element: <BandejaPedidos /> },
          { path: '/bodegas', element: <CatalogoBodegas /> },
          // ADR-0033: cambiar la propia contraseña (cualquier rol con sesión).
          { path: '/perfil/contrasena', element: <CambiarContrasena /> },
          { path: '/pedidos', element: <ListaPedidos /> },
          { path: '/pedidos/nuevo', element: <NuevoPedido /> },
          { path: '/pedidos/:pedidoId', element: <DetallePedido /> },
          // Paso 7.5: chat del asistente por pedido. El servidor decide el acceso (F7-D4, país, pedido, P-17).
          { path: '/pedidos/:pedidoId/asistente', element: <AsistentePedido /> },
          {
            element: <SoloAdmin />,
            children: [
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

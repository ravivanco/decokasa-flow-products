import { Navigate, type RouteObject } from 'react-router';
import App from './App';
import { RutaProtegida, SoloAdmin } from './auth/RutaProtegida';
import { DashboardRol } from './components/dashboards/DashboardRol';
import { ErrorRuta } from './components/ErrorRuta';
import { LoginScreen } from './components/LoginScreen';
import { PaginaNoEncontrada } from './components/PaginasSimples';
import { BandejaPedidos } from './components/pedidos/BandejaPedidos';
import { DetallePedido } from './components/pedidos/DetallePedido';
import { ListaPedidos } from './components/pedidos/ListaPedidos';
import { NuevoPedido } from './components/pedidos/NuevoPedido';
import { RolesManagementView } from './components/RolesManagementView';
import { UsersManagementView } from './components/UsersManagementView';

// Rutas de la aplicación (plan, tarea 19): /login pública; el resto exige sesión, y /usuarios, /roles y la creación de
// pedidos el rol admin. Pedidos y bandeja (fase 2, paso 2.5) leen la API real; el servidor valida cada permiso (R6).
// Las pantallas del demo de fases posteriores (bodegas, avisos, auditoría) no tienen ruta hasta su fase.
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
          { path: '/pedidos', element: <ListaPedidos /> },
          { path: '/pedidos/:pedidoId', element: <DetallePedido /> },
          {
            element: <SoloAdmin />,
            children: [
              { path: '/usuarios', element: <UsersManagementView /> },
              { path: '/roles', element: <RolesManagementView /> },
              { path: '/pedidos/nuevo', element: <NuevoPedido /> },
            ],
          },
          { path: '*', element: <PaginaNoEncontrada /> },
        ],
      },
    ],
  },
];

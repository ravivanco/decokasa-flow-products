import { Navigate, type RouteObject } from 'react-router';
import App from './App';
import { RutaProtegida, SoloAdmin } from './auth/RutaProtegida';
import { DashboardRol } from './components/dashboards/DashboardRol';
import { ErrorRuta } from './components/ErrorRuta';
import { LoginScreen } from './components/LoginScreen';
import { PaginaNoEncontrada } from './components/PaginasSimples';
import { RolesManagementView } from './components/RolesManagementView';
import { UsersManagementView } from './components/UsersManagementView';

// Rutas de la aplicación (plan, tarea 19): /login pública; el resto exige sesión, y /usuarios y /roles el rol admin.
// Las pantallas del demo de fases posteriores (pedidos, bodegas, avisos, auditoría) no tienen ruta hasta su fase.
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
          {
            element: <SoloAdmin />,
            children: [
              { path: '/usuarios', element: <UsersManagementView /> },
              { path: '/roles', element: <RolesManagementView /> },
            ],
          },
          { path: '*', element: <PaginaNoEncontrada /> },
        ],
      },
    ],
  },
];

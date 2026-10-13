import type { RouteObject } from 'react-router';

// Rutas de la aplicación (plan, tarea 19): /login pública; /inicio, /usuarios y /roles protegidas por sesión y rol.
// Montaje mínimo del paso 1.6: las rutas existen para que el router arranque en las pruebas, pero todavía no tienen
// pantallas ni protección. Las pantallas, la redirección al login y la página "Sin permiso" llegan en el paso 1.8.
export const rutas: RouteObject[] = [
  { path: '/login' },
  { path: '/inicio' },
  { path: '/usuarios' },
  { path: '/roles' },
  { path: '*' },
];

import type { ReactNode } from 'react';

// Sesión del usuario en memoria (plan, tarea 18). Interfaz mínima del paso 1.6 para escribir las pruebas;
// se implementa en el paso 1.8.
export function SesionProvider({ children }: { children: ReactNode }) {
  return <>{children}</>;
}

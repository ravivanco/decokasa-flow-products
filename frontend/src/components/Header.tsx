import { LogOutIcon } from 'lucide-react';
import { useState } from 'react';
import { useNavigate } from 'react-router';
import { useSesion, useUsuarioActual } from '@/auth/SesionProvider';
import { Button } from '@/components/ui/button';

// Encabezado de la aplicación con sesión (plan, tarea 21): marca, usuario y cierre de sesión. Sin cambio de usuario,
// de rol ni de país sin autenticación (auditoría S2): esas opciones del demo se retiraron.
export function Header() {
  const usuario = useUsuarioActual();
  const { cerrarSesion } = useSesion();
  const navegar = useNavigate();
  const [cerrando, setCerrando] = useState(false);

  async function salir() {
    setCerrando(true);
    try {
      await cerrarSesion();
    } catch {
      // La sesión se olvida igual en el navegador; el servidor la revoca al vencer la cookie.
    } finally {
      navegar('/login', { replace: true });
    }
  }

  return (
    <header className="flex items-center justify-between gap-4 border-b bg-primary px-4 py-3 text-primary-foreground sm:px-6">
      <div className="flex flex-col">
        <span className="font-heading text-lg font-bold tracking-wide">LATINOAMÉRICA </span>
        <span className="text-xs text-primary-foreground/80">Importanto y Exportando el Mundo</span>
      </div>
      <div className="flex items-center gap-3">
        <span className="truncate text-sm font-medium">{usuario.nombre}</span>
        <Button variant="secondary" size="sm" onClick={salir} disabled={cerrando}>
          <LogOutIcon data-icon="inline-start" />
          Cerrar sesión
        </Button>
      </div>
    </header>
  );
}

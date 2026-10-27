import { ChevronDownIcon, KeyRoundIcon, LogOutIcon } from 'lucide-react';
import { useState } from 'react';
import { Link, useNavigate } from 'react-router';
import logo from '@/assets/marca/logo1.jpeg';
import { useSesion, useUsuarioActual } from '@/auth/SesionProvider';
import { Button } from '@/components/ui/button';
import {
  DropdownMenu,
  DropdownMenuContent,
  DropdownMenuGroup,
  DropdownMenuItem,
  DropdownMenuLabel,
  DropdownMenuSeparator,
  DropdownMenuTrigger,
} from '@/components/ui/dropdown-menu';
import { CampanaAlertas } from './CampanaAlertas';

// Encabezado de la aplicación con sesión (plan, tarea 21): marca, usuario y cierre de sesión. Sin cambio de usuario,
// de rol ni de país sin autenticación (auditoría S2): esas opciones del demo se retiraron.
// ADR-0033: logo original del cliente (logo1.jpeg sobre su mismo morado; se recorta solo el margen vacío al mostrarlo)
// y menú de la cuenta con «Cambiar contraseña».
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
    <header className="flex items-center justify-between gap-3 border-b bg-marca-morado px-3 py-1.5 text-primary-foreground sm:px-6">
      <Link
        to="/inicio"
        className="flex min-w-0 items-center gap-3 rounded-md outline-none focus-visible:ring-3 focus-visible:ring-white"
      >
        <span className="block h-14 w-16 shrink-0 overflow-hidden">
          <img src={logo} alt="" width={1416} height={648} className="size-full scale-[1.15] object-cover" />
        </span>
        <span className="sr-only">LATINOAMÉRICA World · </span>
        <span className="hidden truncate text-sm font-medium text-white/90 sm:inline">LATINOAMÉRICA</span>
      </Link>
      <div className="flex min-w-0 items-center gap-2 sm:gap-3">
        <DropdownMenu>
          <DropdownMenuTrigger asChild>
            <Button variant="ghost" size="sm" className="min-w-0 text-white hover:bg-white/10 hover:text-white focus-visible:ring-white">
              <span className="truncate">{usuario.nombre}</span>
              <ChevronDownIcon data-icon="inline-end" aria-hidden="true" />
            </Button>
          </DropdownMenuTrigger>
          <DropdownMenuContent align="end" className="min-w-56">
            <DropdownMenuLabel className="flex flex-col">
              <span className="truncate font-medium">{usuario.nombre}</span>
              <span className="truncate text-xs font-normal text-muted-foreground">{usuario.correo}</span>
            </DropdownMenuLabel>
            <DropdownMenuSeparator />
            <DropdownMenuGroup>
              <DropdownMenuItem asChild>
                <Link to="/perfil/contrasena"><KeyRoundIcon aria-hidden="true" />Cambiar contraseña</Link>
              </DropdownMenuItem>
            </DropdownMenuGroup>
          </DropdownMenuContent>
        </DropdownMenu>
        <CampanaAlertas />
        <Button variant="secondary" size="sm" onClick={salir} disabled={cerrando}>
          <LogOutIcon data-icon="inline-start" />
          <span className="hidden sm:inline">Cerrar sesión</span>
          <span className="sr-only sm:hidden">Cerrar sesión</span>
        </Button>
      </div>
    </header>
  );
}

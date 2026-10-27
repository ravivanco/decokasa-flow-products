import { zodResolver } from '@hookform/resolvers/zod';
import { ArrowLeftIcon, CircleCheckIcon } from 'lucide-react';
import { useEffect, useState } from 'react';
import { useForm } from 'react-hook-form';
import { Link, useLocation } from 'react-router';
import type { z } from 'zod';
import { useRestablecerContrasena } from '@/api/acceso';
import { CODIGO_SIN_CONEXION, esErrorApi } from '@/api/cliente';
import { MENSAJE_SIN_CONEXION } from '@/api/mensajes';
import { useSesion } from '@/auth/SesionProvider';
import { Alert, AlertDescription, AlertTitle } from '@/components/ui/alert';
import { Button } from '@/components/ui/button';
import { FieldGroup } from '@/components/ui/field';
import { CampoContrasena } from './CampoContrasena';
import { AYUDA_CONTRASENA, esquemaContrasenaNueva } from './esquemas';
import { MarcoAcceso } from './MarcoAcceso';

// Restablecer con el enlace del correo (ADR-0033). El token llega en el fragmento (#token=…), se lee una vez y se
// borra de la barra de direcciones y del historial. Al terminar no se inicia sesión: se vuelve al login.

type Datos = z.infer<typeof esquemaContrasenaNueva>;

function tokenDelFragmento(fragmento: string): string | null {
  const encontrado = /(?:^#|&)token=([^&]+)/.exec(fragmento);
  if (!encontrado) return null;
  try {
    return decodeURIComponent(encontrado[1]);
  } catch {
    return null;
  }
}

const enlaceClase = 'inline-flex items-center gap-1.5 self-start rounded-sm text-sm font-medium text-primary underline-offset-4 outline-none hover:underline focus-visible:ring-3 focus-visible:ring-ring';

export function RestablecerContrasena() {
  const ubicacion = useLocation();
  const { estado, cerrarSesion } = useSesion();
  const [token] = useState(() => tokenDelFragmento(ubicacion.hash || window.location.hash));
  const restablecer = useRestablecerContrasena();
  const [enlaceInvalido, setEnlaceInvalido] = useState(false);
  const { register, handleSubmit, setError, formState: { errors } } = useForm<Datos>({
    resolver: zodResolver(esquemaContrasenaNueva),
    defaultValues: { contrasenaNueva: '', confirmacion: '' },
  });

  // Quita el token de la barra y del historial sin avisar al enrutador (una navegación volvería a montar la pantalla
  // y perdería el token antes de usarlo).
  useEffect(() => {
    if (window.location.hash) {
      window.history.replaceState(window.history.state, '', window.location.pathname + window.location.search);
    }
  }, []);

  async function enviar(datos: Datos) {
    if (!token) return;
    try {
      await restablecer.mutateAsync({ token, ...datos });
      // Las sesiones de la cuenta ya se revocaron en el servidor; si este navegador tenía una, se olvida también.
      if (estado.fase === 'abierta') await cerrarSesion().catch(() => undefined);
    } catch (error) {
      if (esErrorApi(error, 'ENLACE_INVALIDO')) setEnlaceInvalido(true);
      if (esErrorApi(error, 'VALIDACION')) {
        for (const { campo, mensaje } of error.errores) {
          if (campo === 'contrasenaNueva' || campo === 'confirmacion') setError(campo, { message: mensaje });
        }
      }
    }
  }

  const otroError = restablecer.error && !esErrorApi(restablecer.error, 'ENLACE_INVALIDO') && !esErrorApi(restablecer.error, 'VALIDACION')
    ? esErrorApi(restablecer.error, CODIGO_SIN_CONEXION)
      ? MENSAJE_SIN_CONEXION
      : esErrorApi(restablecer.error) && restablecer.error.status === 429
        ? 'Demasiados intentos. Espera unos minutos e intenta de nuevo.'
        : 'No se pudo cambiar la contraseña. Intenta de nuevo.'
    : null;

  if (restablecer.isSuccess) {
    return (
      <MarcoAcceso titulo="Contraseña actualizada">
        <Alert role="status">
          <CircleCheckIcon />
          <AlertTitle>Ya puedes entrar con tu contraseña nueva</AlertTitle>
          <AlertDescription>Cerramos todas las sesiones abiertas de tu cuenta. Inicia sesión de nuevo.</AlertDescription>
        </Alert>
        <Button size="lg" asChild><Link to="/login" replace>Ir a iniciar sesión</Link></Button>
      </MarcoAcceso>
    );
  }

  if (!token || enlaceInvalido) {
    return (
      <MarcoAcceso titulo="El enlace no sirve">
        <Alert variant="destructive">
          <AlertDescription>
            El enlace no es válido, ya se usó o caducó. Pide uno nuevo; solo vale el último que recibas.
          </AlertDescription>
        </Alert>
        <Button size="lg" asChild><Link to="/recuperar-contrasena">Pedir un enlace nuevo</Link></Button>
        <Link to="/login" className={enlaceClase}><ArrowLeftIcon aria-hidden="true" className="size-4" />Volver a iniciar sesión</Link>
      </MarcoAcceso>
    );
  }

  return (
    <MarcoAcceso titulo="Elige una contraseña nueva" descripcion="Al guardarla se cerrarán todas las sesiones abiertas de tu cuenta.">
      <form noValidate onSubmit={handleSubmit(enviar)} className="flex flex-col gap-5">
        {otroError && (
          <Alert variant="destructive">
            <AlertDescription>{otroError}</AlertDescription>
          </Alert>
        )}
        <FieldGroup>
          <CampoContrasena id="contrasenaNueva" etiqueta="Contraseña nueva" autoComplete="new-password" registro={register('contrasenaNueva')} error={errors.contrasenaNueva} ayuda={AYUDA_CONTRASENA} />
          <CampoContrasena id="confirmacion" etiqueta="Confirmar contraseña" autoComplete="new-password" registro={register('confirmacion')} error={errors.confirmacion} />
        </FieldGroup>
        <Button type="submit" size="lg" disabled={restablecer.isPending}>
          {restablecer.isPending ? 'Guardando…' : 'Guardar contraseña'}
        </Button>
      </form>
    </MarcoAcceso>
  );
}

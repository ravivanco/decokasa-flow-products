import { zodResolver } from '@hookform/resolvers/zod';
import { InfoIcon } from 'lucide-react';
import { useForm } from 'react-hook-form';
import { useNavigate } from 'react-router';
import { z } from 'zod';
import { useCambiarContrasena } from '@/api/acceso';
import { CODIGO_SIN_CONEXION, esErrorApi } from '@/api/cliente';
import { MENSAJE_SIN_CONEXION } from '@/api/mensajes';
import { useSesion } from '@/auth/SesionProvider';
import { Alert, AlertDescription } from '@/components/ui/alert';
import { Button } from '@/components/ui/button';
import { FieldGroup } from '@/components/ui/field';
import { bytesUtf8 } from '@/lib/validacion';
import { CampoContrasena } from './CampoContrasena';
import { AYUDA_CONTRASENA, esquemaContrasenaNueva } from './esquemas';

// Cambiar la propia contraseña (ADR-0033). El servidor revoca todas las sesiones, incluida esta: al terminar, la
// pantalla cierra la sesión del navegador y lleva al login con un aviso.

const esquema = esquemaContrasenaNueva.and(z.object({
  contrasenaActual: z.string().min(1, 'Escribe tu contraseña actual.').refine((v) => bytesUtf8(v) <= 72, 'Máximo 72 bytes.'),
}));
type Datos = z.infer<typeof esquema>;

export const AVISO_CONTRASENA_CAMBIADA = 'Tu contraseña se cambió y se cerraron todas tus sesiones. Inicia sesión con la contraseña nueva.';

export function CambiarContrasena() {
  const cambiar = useCambiarContrasena();
  const { cerrarSesion } = useSesion();
  const navegar = useNavigate();
  const { register, handleSubmit, setError, formState: { errors } } = useForm<Datos>({
    resolver: zodResolver(esquema),
    defaultValues: { contrasenaActual: '', contrasenaNueva: '', confirmacion: '' },
  });

  async function enviar(datos: Datos) {
    try {
      await cambiar.mutateAsync(datos);
    } catch (error) {
      if (esErrorApi(error, 'VALIDACION')) {
        for (const { campo, mensaje } of error.errores) {
          if (campo === 'contrasenaActual' || campo === 'contrasenaNueva' || campo === 'confirmacion') {
            setError(campo, { message: mensaje });
          }
        }
      }
      return;
    }
    await cerrarSesion().catch(() => undefined);
    navegar('/login', { replace: true, state: { aviso: AVISO_CONTRASENA_CAMBIADA } });
  }

  const error = cambiar.error;
  const mensajeError = !error || esErrorApi(error, 'VALIDACION')
    ? null
    : esErrorApi(error, CODIGO_SIN_CONEXION)
      ? MENSAJE_SIN_CONEXION
      : esErrorApi(error) && error.status === 423
        ? 'La cuenta quedó bloqueada 15 minutos tras 5 intentos con una contraseña actual incorrecta.'
        : 'No se pudo cambiar la contraseña. Intenta de nuevo.';

  return (
    <section className="flex max-w-lg flex-col gap-6">
      <div className="flex flex-col gap-1">
        <h1 className="font-heading text-2xl font-bold text-primary">Cambiar contraseña</h1>
        <p className="text-muted-foreground">Confirma tu contraseña actual y elige una nueva.</p>
      </div>
      <form noValidate onSubmit={handleSubmit(enviar)} className="flex flex-col gap-5 rounded-lg border bg-card p-5 shadow-sm">
        {mensajeError && (
          <Alert variant="destructive">
            <AlertDescription>{mensajeError}</AlertDescription>
          </Alert>
        )}
        <FieldGroup>
          <CampoContrasena id="contrasenaActual" etiqueta="Contraseña actual" autoComplete="current-password" registro={register('contrasenaActual')} error={errors.contrasenaActual} />
          <CampoContrasena id="contrasenaNueva" etiqueta="Contraseña nueva" autoComplete="new-password" registro={register('contrasenaNueva')} error={errors.contrasenaNueva} ayuda={AYUDA_CONTRASENA} />
          <CampoContrasena id="confirmacion" etiqueta="Confirmar contraseña nueva" autoComplete="new-password" registro={register('confirmacion')} error={errors.confirmacion} />
        </FieldGroup>
        <Alert>
          <InfoIcon />
          <AlertDescription>
            Al guardar se cerrarán todas tus sesiones, también esta, y tendrás que iniciar sesión de nuevo.
          </AlertDescription>
        </Alert>
        <Button type="submit" size="lg" className="self-start" disabled={cambiar.isPending}>
          {cambiar.isPending ? 'Guardando…' : 'Cambiar contraseña'}
        </Button>
      </form>
    </section>
  );
}

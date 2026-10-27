import { zodResolver } from '@hookform/resolvers/zod';
import { ArrowLeftIcon, MailCheckIcon } from 'lucide-react';
import { useForm } from 'react-hook-form';
import { Link } from 'react-router';
import { z } from 'zod';
import { useSolicitarRecuperacion } from '@/api/acceso';
import { CODIGO_SIN_CONEXION, esErrorApi } from '@/api/cliente';
import { MENSAJE_SIN_CONEXION } from '@/api/mensajes';
import { Alert, AlertDescription, AlertTitle } from '@/components/ui/alert';
import { Button } from '@/components/ui/button';
import { Field, FieldError, FieldGroup, FieldLabel } from '@/components/ui/field';
import { Input } from '@/components/ui/input';
import { MarcoAcceso } from './MarcoAcceso';

// «Olvidé mi contraseña» (ADR-0033). La respuesta es la misma para cualquier correo y no confirma un envío: el correo
// sale solo si el servicio está activo. Nunca se muestra el enlace aquí.

const esquema = z.object({
  correo: z.string().trim().min(1, 'Escribe tu correo.').max(254, 'Máximo 254 caracteres.').email('Escribe un correo válido.'),
});

const enlaceClase = 'inline-flex items-center gap-1.5 self-start rounded-sm text-sm font-medium text-primary underline-offset-4 outline-none hover:underline focus-visible:ring-3 focus-visible:ring-ring';

export function RecuperarContrasena() {
  const solicitar = useSolicitarRecuperacion();
  const { register, handleSubmit, formState: { errors } } = useForm<z.infer<typeof esquema>>({
    resolver: zodResolver(esquema),
    defaultValues: { correo: '' },
  });

  const error = solicitar.error;
  const mensajeError = !error
    ? null
    : esErrorApi(error, CODIGO_SIN_CONEXION)
      ? MENSAJE_SIN_CONEXION
      : esErrorApi(error) && error.status === 429
        ? 'Hiciste demasiadas solicitudes. Espera unos minutos e intenta de nuevo.'
        : 'No se pudo registrar la solicitud. Intenta de nuevo.';

  return (
    <MarcoAcceso
      titulo="Recuperar la contraseña"
      descripcion="Escribe el correo registrado en tu cuenta. Si corresponde a una cuenta activa, te enviaremos un enlace para elegir una contraseña nueva."
    >
      {solicitar.isSuccess ? (
        <div className="flex flex-col gap-5">
          <Alert role="status">
            <MailCheckIcon />
            <AlertTitle>Solicitud recibida</AlertTitle>
            <AlertDescription>
              {solicitar.data.mensaje} Si no te llega en unos minutos, revisa el correo que escribiste o vuelve a
              pedirlo más tarde.
            </AlertDescription>
          </Alert>
          <Link to="/login" className={enlaceClase}><ArrowLeftIcon aria-hidden="true" className="size-4" />Volver a iniciar sesión</Link>
        </div>
      ) : (
        <form noValidate onSubmit={handleSubmit((datos) => solicitar.mutate(datos.correo))} className="flex flex-col gap-5">
          {mensajeError && (
            <Alert variant="destructive">
              <AlertDescription>{mensajeError}</AlertDescription>
            </Alert>
          )}
          <FieldGroup>
            <Field data-invalid={errors.correo ? true : undefined}>
              <FieldLabel htmlFor="correo">Correo registrado</FieldLabel>
              <Input id="correo" type="email" autoComplete="email" inputMode="email" aria-invalid={errors.correo ? true : undefined} {...register('correo')} />
              <FieldError errors={[errors.correo]} />
            </Field>
          </FieldGroup>
          <Button type="submit" size="lg" disabled={solicitar.isPending}>
            {solicitar.isPending ? 'Enviando solicitud…' : 'Pedir enlace'}
          </Button>
          <Link to="/login" className={enlaceClase}><ArrowLeftIcon aria-hidden="true" className="size-4" />Volver a iniciar sesión</Link>
        </form>
      )}
    </MarcoAcceso>
  );
}

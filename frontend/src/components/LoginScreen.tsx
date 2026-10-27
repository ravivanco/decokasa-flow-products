import { zodResolver } from '@hookform/resolvers/zod';
import { CircleCheckIcon } from 'lucide-react';
import { useState } from 'react';
import { useForm } from 'react-hook-form';
import { Link, Navigate, useLocation, useNavigate } from 'react-router';
import { z } from 'zod';
import { CODIGO_SIN_CONEXION, esErrorApi } from '@/api/cliente';
import { MENSAJE_SIN_CONEXION } from '@/api/mensajes';
import { Cargando } from '@/auth/RutaProtegida';
import { useSesion } from '@/auth/SesionProvider';
import { CampoContrasena } from '@/components/acceso/CampoContrasena';
import { MarcoAcceso } from '@/components/acceso/MarcoAcceso';
import { Alert, AlertDescription } from '@/components/ui/alert';
import { Button } from '@/components/ui/button';
import { Field, FieldError, FieldGroup, FieldLabel } from '@/components/ui/field';
import { Input } from '@/components/ui/input';
import { bytesUtf8 } from '@/lib/validacion';

// Inicio de sesión real (plan, tarea 20; requisito 4.2 #1; ADR-0002): correo, usuario o teléfono y contraseña contra
// POST /auth/login. Sin accesos rápidos, cambio de rol ni contraseña de demostración (auditoría S1 y S2).
// ADR-0033: logo original del cliente, «Mostrar contraseña» y enlace a «¿Olvidaste tu contraseña?».

const esquemaLogin = z.object({
  identificador: z.string().trim().min(1, 'Escribe tu correo, usuario o teléfono.').max(254, 'Máximo 254 caracteres.'),
  contrasena: z
    .string()
    .min(1, 'Escribe tu contraseña.')
    .refine((valor) => bytesUtf8(valor) <= 72, 'La contraseña tiene como máximo 72 bytes.'),
});
type DatosLogin = z.infer<typeof esquemaLogin>;

function mensajeDeError(error: unknown): string {
  if (esErrorApi(error) && error.status === 401) {
    return 'Credenciales incorrectas. Revisa tu correo, usuario o teléfono y la contraseña.';
  }
  if (esErrorApi(error) && error.status === 423) {
    return 'La cuenta está bloqueada por 15 minutos tras 5 intentos fallidos. Espera e intenta de nuevo.';
  }
  if (esErrorApi(error, CODIGO_SIN_CONEXION)) return MENSAJE_SIN_CONEXION;
  return 'No se pudo iniciar sesión. Intenta de nuevo.';
}

export function LoginScreen() {
  const { estado, iniciarSesion } = useSesion();
  const navegar = useNavigate();
  const ubicacion = useLocation();
  const [error, setError] = useState<string | null>(null);
  const {
    register,
    handleSubmit,
    formState: { errors, isSubmitting },
  } = useForm<DatosLogin>({
    resolver: zodResolver(esquemaLogin),
    defaultValues: { identificador: '', contrasena: '' },
  });

  const estadoRuta = ubicacion.state as { desde?: unknown; aviso?: unknown } | null;
  const desde = estadoRuta?.desde;
  const aviso = typeof estadoRuta?.aviso === 'string' ? estadoRuta.aviso : null;
  const destino = typeof desde === 'string' && desde.startsWith('/') && !desde.startsWith('//') ? desde : '/inicio';

  if (estado.fase === 'cargando') return <Cargando />;
  if (estado.fase === 'abierta' && !isSubmitting) return <Navigate to={destino} replace />;

  async function enviar(datos: DatosLogin) {
    setError(null);
    try {
      await iniciarSesion(datos.identificador, datos.contrasena);
      navegar(destino, { replace: true });
    } catch (e) {
      setError(mensajeDeError(e));
    }
  }

  return (
    <MarcoAcceso titulo="Inicia sesión" descripcion="">
      <form noValidate onSubmit={handleSubmit(enviar)} className="flex flex-col gap-5">
        {aviso && !error && (
          <Alert role="status">
            <CircleCheckIcon />
            <AlertDescription>{aviso}</AlertDescription>
          </Alert>
        )}
        {error && (
          <Alert variant="destructive">
            <AlertDescription>{error}</AlertDescription>
          </Alert>
        )}
        <FieldGroup>
          <Field data-invalid={errors.identificador ? true : undefined}>
            <FieldLabel htmlFor="identificador">Correo</FieldLabel>
            <Input
              id="identificador"
              autoComplete="username"
              autoCapitalize="none"
              spellCheck={false}
              aria-invalid={errors.identificador ? true : undefined}
              {...register('identificador')}
            />
            <FieldError errors={[errors.identificador]} />
          </Field>
          <CampoContrasena
            id="contrasena"
            etiqueta="Contraseña"
            autoComplete="current-password"
            registro={register('contrasena')}
            error={errors.contrasena}
          />
        </FieldGroup>
        <div className="flex flex-col gap-4">
          <Button type="submit" size="lg" disabled={isSubmitting}>
            {isSubmitting ? 'Ingresando…' : 'Ingresar'}
          </Button>
          <Link
            to="/recuperar-contrasena"
            className="self-center rounded-sm text-sm font-medium text-primary underline-offset-4 outline-none hover:underline focus-visible:ring-3 focus-visible:ring-ring"
          >
            ¿Olvidaste tu contraseña?
          </Link>
        </div>
      </form>
    </MarcoAcceso>
  );
}

import { zodResolver } from '@hookform/resolvers/zod';
import { useState } from 'react';
import { useForm } from 'react-hook-form';
import { Navigate, useLocation, useNavigate } from 'react-router';
import { z } from 'zod';
import { CODIGO_SIN_CONEXION, esErrorApi } from '@/api/cliente';
import { MENSAJE_SIN_CONEXION } from '@/api/mensajes';
import { Cargando } from '@/auth/RutaProtegida';
import { useSesion } from '@/auth/SesionProvider';
import { Alert, AlertDescription } from '@/components/ui/alert';
import { Button } from '@/components/ui/button';
import { Card, CardContent, CardDescription, CardHeader, CardTitle } from '@/components/ui/card';
import { Field, FieldError, FieldGroup, FieldLabel } from '@/components/ui/field';
import { Input } from '@/components/ui/input';
import { bytesUtf8 } from '@/lib/validacion';

// Inicio de sesión real (plan, tarea 20; requisito 4.2 #1; ADR-0002): correo, usuario o teléfono y contraseña contra
// POST /auth/login. Sin accesos rápidos, cambio de rol ni contraseña de demostración (auditoría S1 y S2).

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

  const desde = (ubicacion.state as { desde?: unknown } | null)?.desde;
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
    <main className="flex min-h-svh items-center justify-center bg-secondary p-4">
      <Card className="w-full max-w-md">
        <CardHeader>
          <CardTitle>
            <h1 className="font-heading text-2xl font-bold text-primary">DECOKASA</h1>
          </CardTitle>
          <CardDescription>Seguimiento de pedidos. Ingresa con tu cuenta.</CardDescription>
        </CardHeader>
        <CardContent>
          <form noValidate onSubmit={handleSubmit(enviar)} className="flex flex-col gap-5">
            {error && (
              <Alert variant="destructive">
                <AlertDescription>{error}</AlertDescription>
              </Alert>
            )}
            <FieldGroup>
              <Field data-invalid={errors.identificador ? true : undefined}>
                <FieldLabel htmlFor="identificador">Correo, usuario o teléfono</FieldLabel>
                <Input
                  id="identificador"
                  autoComplete="username"
                  aria-invalid={errors.identificador ? true : undefined}
                  {...register('identificador')}
                />
                <FieldError errors={[errors.identificador]} />
              </Field>
              <Field data-invalid={errors.contrasena ? true : undefined}>
                <FieldLabel htmlFor="contrasena">Contraseña</FieldLabel>
                <Input
                  id="contrasena"
                  type="password"
                  autoComplete="current-password"
                  aria-invalid={errors.contrasena ? true : undefined}
                  {...register('contrasena')}
                />
                <FieldError errors={[errors.contrasena]} />
              </Field>
            </FieldGroup>
            <Button type="submit" size="lg" disabled={isSubmitting}>
              {isSubmitting ? 'Ingresando…' : 'Ingresar'}
            </Button>
          </form>
        </CardContent>
      </Card>
    </main>
  );
}

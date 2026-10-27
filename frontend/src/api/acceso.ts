import { useMutation } from '@tanstack/react-query';
import { z } from 'zod';
import { llamar } from '@/api/cliente';
import { useSesion } from '@/auth/SesionProvider';

// Recuperación y cambio de contraseña (contrato 0.28.0; ADR-0033). Solicitar y restablecer son públicos: no llevan
// token. Cambiar usa la sesión. Ninguna respuesta trae el enlace ni el token; la contraseña solo viaja en el cuerpo.

const esquemaRespuestaRecuperacion = z.object({ mensaje: z.string() });

export interface NuevaContrasena {
  contrasenaNueva: string;
  confirmacion: string;
}

export function useSolicitarRecuperacion() {
  return useMutation({
    mutationFn: (correo: string) =>
      llamar('/auth/recuperacion', { metodo: 'POST', cuerpo: { correo }, esquema: esquemaRespuestaRecuperacion }),
  });
}

export function useRestablecerContrasena() {
  return useMutation({
    // Con credenciales: la respuesta borra la cookie de renovación de este navegador.
    mutationFn: (datos: NuevaContrasena & { token: string }) =>
      llamar<void>('/auth/recuperacion/restablecer', { metodo: 'POST', cuerpo: datos, credenciales: true }),
  });
}

export function useCambiarContrasena() {
  const { pedir } = useSesion();
  return useMutation({
    mutationFn: (datos: NuevaContrasena & { contrasenaActual: string }) =>
      pedir<void>('/auth/contrasena', { metodo: 'POST', cuerpo: datos }),
  });
}

import { z } from 'zod';
import { bytesUtf8 } from '@/lib/validacion';

// Política de contraseña nueva, la misma del servidor (ContrasenaNueva: 8 caracteres y 72 bytes; ADR-0033). El
// servidor vuelve a validarla.
export const AYUDA_CONTRASENA = 'Al menos 8 caracteres. Las letras con tilde y los emoji cuentan más (máximo 72 bytes).';

export const esquemaContrasenaNueva = z
  .object({
    contrasenaNueva: z
      .string()
      .refine((valor) => [...valor].length >= 8, 'Usa al menos 8 caracteres.')
      .refine((valor) => bytesUtf8(valor) <= 72, 'Es demasiado larga: máximo 72 bytes.'),
    confirmacion: z.string().min(1, 'Repite la contraseña nueva.'),
  })
  .refine((datos) => datos.contrasenaNueva === datos.confirmacion, {
    path: ['confirmacion'],
    message: 'No coincide con la contraseña nueva.',
  });

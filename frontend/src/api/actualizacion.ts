import { useQueryClient } from '@tanstack/react-query';
import { useEffect } from 'react';
import { esErrorApi } from './cliente';

// Actualización entre pantallas (paso 5.4; ADR-0014, decisión técnica del equipo aprobada provisionalmente por
// Richard el 2026-10-20): consultas periódicas con TanStack Query, sin WebSocket. Un cambio hecho por otra persona
// puede tardar en verse hasta el intervalo más lo que tarde la petición; no es "al instante". Los cambios propios se
// ven enseguida porque cada operación invalida sus consultas (useRefrescarPedido).
//
// - Intervalo configurable con VITE_ACTUALIZACION_MS: 30 s por defecto, entre 10 s y 5 min.
// - Solo con la pestaña visible (refetchIntervalInBackground: false) y solo las consultas montadas en pantalla.
//   TanStack no duplica la petición de una consulta que ya está en curso.
// - Al recuperar el foco o la conexión se piden datos actuales aunque la caché se considere reciente ('always').
// - Un error 4xx detiene el intervalo de esa consulta (un permiso denegado se muestra como tal y no se insiste); se
//   vuelve a intentar al recuperar el foco. La sesión solo se cierra con la política existente (renovación rechazada).
// - Mientras haya un formulario con cambios sin enviar o un diálogo de acción abierto, el refresco automático se
//   pausa para no reemplazar lo que la persona está viendo; al terminar, se pide una lectura actual. Las
//   comprobaciones de versión, etapa y entrada al confirmar siguen en el servidor.
// - Al cambiar de sesión, el coordinador cancela las consultas y vacía la caché (SesionProvider); las claves llevan
//   el número de sesión y cada lectura vuelve a pasar por los permisos del servidor.

const PREDETERMINADO_MS = 30_000;
const MINIMO_MS = 10_000;
const MAXIMO_MS = 300_000;

/** Intervalo en milisegundos: el configurado, limitado a 10 s–5 min; 30 s si falta o no es un número. */
export function intervaloActualizacion(valor: string | undefined = import.meta.env.VITE_ACTUALIZACION_MS): number {
  const numero = Number(valor);
  if (valor === undefined || valor.trim() === '' || !Number.isFinite(numero)) return PREDETERMINADO_MS;
  return Math.min(MAXIMO_MS, Math.max(MINIMO_MS, Math.round(numero)));
}

export const INTERVALO_ACTUALIZACION_MS = intervaloActualizacion();

let ediciones = 0;

/** Si hay un formulario con cambios sin enviar o un diálogo de acción abierto en esta pestaña. */
export function hayEdicionEnCurso(): boolean {
  return ediciones > 0;
}

/**
 * Pausa el refresco automático mientras {@code activa} sea verdadero. Al terminar la última edición, las consultas de
 * seguimiento activas se vuelven a pedir (así el intervalo detenido se reanuda con datos actuales).
 */
export function usePausarActualizacion(activa: boolean): void {
  const consultas = useQueryClient();
  useEffect(() => {
    if (!activa) return;
    ediciones += 1;
    return () => {
      ediciones -= 1;
      if (ediciones === 0) {
        void consultas.invalidateQueries({
          predicate: (consulta) => consulta.meta?.seguimiento === true,
          refetchType: 'active',
        });
      }
    };
  }, [activa, consultas]);
}

/** Intervalo de una consulta de seguimiento: se detiene con una edición en curso o tras un error 4xx. */
export function intervaloSeguimiento<C extends { state: { error: unknown } }>(consulta: C): number | false {
  if (hayEdicionEnCurso()) return false;
  const error = consulta.state.error;
  if (esErrorApi(error) && error.status >= 400 && error.status < 500) return false;
  return INTERVALO_ACTUALIZACION_MS;
}

function alRecuperar(): boolean | 'always' {
  return hayEdicionEnCurso() ? false : 'always';
}

/** Opciones comunes de las consultas de seguimiento (paso 5.4). */
export const opcionesSeguimiento = {
  refetchInterval: intervaloSeguimiento,
  refetchIntervalInBackground: false,
  refetchOnWindowFocus: alRecuperar,
  refetchOnReconnect: alRecuperar,
  meta: { seguimiento: true },
};

import { useMutation } from '@tanstack/react-query';
import { z } from 'zod';
import { useSesion, useUsuarioActual } from '@/auth/SesionProvider';
import { ErrorApi } from './cliente';
import { type DatosSubida, esquemaAdjunto, useRefrescarPedido } from './pedidos';

const BLOQUE = 4 * 1024 * 1024;
const esquemaFragmento = z.object({
  inicio: z.number().int().nonnegative(),
  tamano: z.number().int().min(1).max(BLOQUE),
  sha256: z.string().regex(/^[0-9a-f]{64}$/),
});
const esquemaCarga = z.object({
  id: z.string().uuid(), pedidoId: z.string().uuid(), etapa: z.number().int(), entradaId: z.string().uuid(),
  nombre: z.string(), tipoDocumento: z.string(), tamanoBytes: z.number().int().min(1).max(100 * 1024 * 1024),
  offsetConfirmado: z.number().int().nonnegative(), fragmentoBytes: z.literal(BLOQUE),
  estado: z.enum(['RECIBIENDO', 'COMPLETADA']), adjuntoId: z.string().uuid().nullable(),
  eventoId: z.string().uuid().nullable(), creadaEn: z.string(), actualizadaEn: z.string(),
  fragmentos: z.array(esquemaFragmento).max(25),
}).superRefine((carga, contexto) => {
  let offset = 0;
  for (const fragmento of carga.fragmentos) {
    if (fragmento.inicio !== offset || fragmento.tamano !== Math.min(BLOQUE, carga.tamanoBytes - offset)) {
      contexto.addIssue({ code: 'custom', message: 'Progreso incoherente.' });
    }
    offset += fragmento.tamano;
  }
  if (offset !== carga.offsetConfirmado || offset > carga.tamanoBytes
    || (carga.estado === 'COMPLETADA' && (offset !== carga.tamanoBytes || !carga.adjuntoId || !carga.eventoId))
    || (carga.estado === 'RECIBIENDO' && (carga.adjuntoId !== null || carga.eventoId !== null))) {
    contexto.addIssue({ code: 'custom', message: 'Estado de carga incoherente.' });
  }
});
const esquemaRecuerdo = z.object({ claveIdempotencia: z.string().uuid(), cargaId: z.string().uuid().optional() });
export interface ProgresoCarga {
  confirmados: number;
  total: number;
  fase: 'comprobando' | 'subiendo' | 'finalizando';
}
interface DatosCarga extends DatosSubida {
  signal: AbortSignal;
  onProgreso: (progreso: ProgresoCarga) => void;
}

async function sha256(bytes: ArrayBuffer): Promise<string> {
  const resumen = await crypto.subtle.digest('SHA-256', bytes);
  return Array.from(new Uint8Array(resumen), (valor) => valor.toString(16).padStart(2, '0')).join('');
}
function leerRecuerdo(clave: string): z.infer<typeof esquemaRecuerdo> {
  try {
    const texto = sessionStorage.getItem(clave);
    if (texto) return esquemaRecuerdo.parse(JSON.parse(texto));
    const nuevo = { claveIdempotencia: crypto.randomUUID() };
    sessionStorage.setItem(clave, JSON.stringify(nuevo));
    return nuevo;
  } catch {
    throw new ErrorApi(0, 'CARGA_MEMORIA', 'No se pudo recuperar la referencia de carga de esta pestaña.');
  }
}
function guardarRecuerdo(clave: string, recuerdo: z.infer<typeof esquemaRecuerdo>) {
  try { sessionStorage.setItem(clave, JSON.stringify(recuerdo)); }
  catch { throw new ErrorApi(0, 'CARGA_MEMORIA', 'No se pudo conservar la referencia de carga en esta pestaña.'); }
}

/** Bytes solo en File/Blob de un bloque y servidor. sessionStorage conserva UUID, nunca archivo o token. */
export function useCargaArchivo() {
  const { pedir } = useSesion();
  const usuario = useUsuarioActual();
  const refrescar = useRefrescarPedido();
  return useMutation({
    retry: false,
    mutationFn: async ({ archivo, pedidoId, etapa, entradaEsperadaId, tipoDocumento, signal, onProgreso }: DatosCarga) => {
      signal.throwIfAborted();
      if (archivo.size < 1) throw new ErrorApi(400, 'VALIDACION', 'El archivo está vacío.');
      if (archivo.size > 100 * 1024 * 1024) throw new ErrorApi(413, 'ARCHIVO_DEMASIADO_GRANDE', 'El archivo supera 100 MiB.');
      const descriptor = JSON.stringify([pedidoId, etapa, entradaEsperadaId, tipoDocumento.trim(), archivo.name, archivo.size, archivo.lastModified]);
      const clave = `decokasa:carga:${usuario.id}:${await sha256(new TextEncoder().encode(descriptor).buffer)}`;
      signal.throwIfAborted();
      const recuerdo = leerRecuerdo(clave);
      let carga = recuerdo.cargaId
        ? await pedir(`/archivos/cargas/${recuerdo.cargaId}`, { esquema: esquemaCarga, signal })
        : await pedir(`/pedidos/${encodeURIComponent(pedidoId)}/etapas/${etapa}/archivos/cargas`, {
          metodo: 'POST', esquema: esquemaCarga, signal,
          cuerpo: { entradaEsperadaId, claveIdempotencia: recuerdo.claveIdempotencia, nombre: archivo.name, tipoDocumento, tamanoBytes: archivo.size },
        });
      if (carga.pedidoId !== pedidoId || carga.etapa !== etapa || carga.entradaId !== entradaEsperadaId
        || carga.tamanoBytes !== archivo.size || carga.tipoDocumento !== tipoDocumento.trim()) {
        throw new ErrorApi(409, 'CARGA_CONFLICTO', 'La sesión corresponde a otro archivo o entrada.');
      }
      guardarRecuerdo(clave, { ...recuerdo, cargaId: carga.id });
      onProgreso({ confirmados: carga.offsetConfirmado, total: archivo.size, fase: 'comprobando' });
      // Tras recargar, volver a elegir el archivo y comprobar TODOS los bloques del prefijo confirmado.
      for (const fragmento of carga.fragmentos) {
        signal.throwIfAborted();
        const real = await sha256(await archivo.slice(fragmento.inicio, fragmento.inicio + fragmento.tamano).arrayBuffer());
        signal.throwIfAborted();
        if (real !== fragmento.sha256) throw new ErrorApi(409, 'CARGA_CONFLICTO', 'El archivo elegido no coincide con los bytes ya confirmados.');
      }
      while (carga.offsetConfirmado < archivo.size) {
        signal.throwIfAborted();
        const inicio = carga.offsetConfirmado;
        const contenido = archivo.slice(inicio, Math.min(inicio + BLOQUE, archivo.size));
        const huella = await sha256(await contenido.arrayBuffer());
        signal.throwIfAborted();
        const siguiente = await pedir(`/archivos/cargas/${carga.id}/fragmentos`, {
          metodo: 'PUT', esquema: esquemaCarga, signal,
          fragmento: { contenido, rango: `bytes ${inicio}-${inicio + contenido.size - 1}/${archivo.size}`, sha256: huella },
        });
        if (siguiente.id !== carga.id || siguiente.offsetConfirmado !== inicio + contenido.size) {
          throw new ErrorApi(409, 'CARGA_CONFLICTO', 'El progreso cambió. Consulta y reanuda de nuevo.');
        }
        carga = siguiente;
        onProgreso({ confirmados: carga.offsetConfirmado, total: archivo.size, fase: 'subiendo' });
      }
      signal.throwIfAborted();
      onProgreso({ confirmados: carga.offsetConfirmado, total: archivo.size, fase: 'finalizando' });
      const adjunto = await pedir(`/archivos/cargas/${carga.id}/finalizacion`, {
        metodo: 'POST', esquema: esquemaAdjunto, signal,
      });
      try { sessionStorage.removeItem(clave); }
      catch { console.warn('La carga terminó; no se pudo retirar su referencia local. Un reintento recuperará el mismo resultado.'); }
      return adjunto;
    },
    onSettled: (_adjunto, _error, { pedidoId }) => refrescar(pedidoId),
  });
}

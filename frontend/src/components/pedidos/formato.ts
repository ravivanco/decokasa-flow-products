import { CODIGO_RESPUESTA_INVALIDA, esErrorApi } from '@/api/cliente';
import type { Usuario } from '@/api/esquemas';
import { mensajeDeError } from '@/api/mensajes';
import type { AccionEtapa, DefinicionFlujo, EtapaEnLinea, NivelUrgencia, TipoEvento } from '@/api/pedidos';

// Textos y formatos de las pantallas de pedidos. Las fechas llegan con la zona de Ecuador (-05:00, ADR-0006) y se
// muestran en esa misma zona, sin depender de la zona del navegador (David trabaja desde China).

const ZONA = 'America/Guayaquil';

/** Fecha civil que devuelve el servidor: se presenta sin convertirla a UTC ni recalcularla. */
export function formatearFecha(valor: string | null | undefined): string {
  const partes = valor?.match(/^(\d{4})-(\d{2})-(\d{2})$/);
  return partes ? `${partes[3]}/${partes[2]}/${partes[1]}` : '—';
}

const fechaHora = new Intl.DateTimeFormat('es-EC', {
  timeZone: ZONA,
  day: '2-digit',
  month: 'short',
  year: 'numeric',
  hour: '2-digit',
  minute: '2-digit',
  hour12: false,
});

const fechaCorta = new Intl.DateTimeFormat('es-EC', { timeZone: ZONA, day: '2-digit', month: 'short' });

/** "14 oct 2026, 16:00" en hora de Ecuador. */
export function formatearFechaHora(valor: string | null | undefined): string {
  if (!valor) return '—';
  const fecha = new Date(valor);
  return Number.isNaN(fecha.getTime()) ? '—' : fechaHora.format(fecha);
}

export function formatearFechaCorta(valor: string | null | undefined): string {
  if (!valor) return '—';
  const fecha = new Date(valor);
  return Number.isNaN(fecha.getTime()) ? '—' : fechaCorta.format(fecha);
}

export function formatearTamano(bytes: number): string {
  if (bytes < 1024) return `${bytes} B`;
  if (bytes < 1024 * 1024) return `${(bytes / 1024).toFixed(1)} KB`;
  return `${(bytes / (1024 * 1024)).toFixed(1)} MB`;
}

/** "A2", "B5": el código de etapa que usan los requisitos. */
export function codigoEtapa(tipoFlujo: string, numero: number): string {
  return `${tipoFlujo}${numero}`;
}

const NOMBRE_FLUJO: Record<string, string> = { A: 'A · Ecuador', B: 'B · China' };

/** "A · Ecuador", "B · China": de dónde nace el pedido (requerimientos 1.4 y 1.5). */
export function nombreFlujo(tipoFlujo: string): string {
  return NOMBRE_FLUJO[tipoFlujo] ?? tipoFlujo;
}

/**
 * Pista para la pantalla, no un permiso: los flujos cuya primera etapa tiene un rol vigente de la persona, que es la
 * regla del servidor para crear (paso 6.2). El admin crea A porque A1 es suyo; no crea B sin Compras China (P-14 ⏸).
 * Con varios roles se suman. Sale de las definiciones, no de nombres de rol fijos.
 */
export function flujosQuePuedeCrear(
  definiciones: { A?: DefinicionFlujo; B?: DefinicionFlujo },
  roles: string[],
): ('A' | 'B')[] {
  return (['A', 'B'] as const).filter((tipo) => {
    const inicial = definiciones[tipo]?.etapas[0];
    return inicial?.rolResponsable != null && roles.includes(inicial.rolResponsable);
  });
}

export const NOMBRE_ACCION: Record<AccionEtapa, string> = {
  ENVIAR: 'Enviar',
  APROBAR: 'Aprobar',
  RECHAZAR: 'Rechazar',
  DEVOLVER_POR_ERROR: 'Devolver por error',
  DEVOLVER_POR_PRODUCTO_NUEVO: 'Devolver por producto nuevo',
  REPORTAR_DEMORA: 'Reportar demora',
  ELEGIR_BODEGA: 'Elegir bodega',
};

export const NOMBRE_URGENCIA: Record<NivelUrgencia, string> = {
  URGENTE: 'Urgente',
  PRIORITARIO: 'Prioritario',
  NORMAL: 'Normal',
};

export const HORAS_URGENCIA: Record<NivelUrgencia, number> = { URGENTE: 4, PRIORITARIO: 12, NORMAL: 24 };

export const NOMBRE_EVENTO: Record<TipoEvento, string> = {
  PEDIDO_CREADO: 'Pedido creado',
  ETAPA_ENVIADA: 'Etapa enviada',
  ETAPA_APROBADA: 'Etapa aprobada',
  ETAPA_RECHAZADA: 'Etapa rechazada',
  PEDIDO_DEVUELTO: 'Pedido devuelto',
  DEMORA_REPORTADA: 'Demora reportada',
  BODEGA_ASIGNADA: 'Bodega asignada',
  OBSERVACION_AGREGADA: 'Observación',
  ARCHIVO_SUBIDO: 'Archivo subido',
  ETAPA_REASIGNADA: 'Etapa reasignada',
  PLAZO_VENCIDO: 'Plazo vencido',
  MOTIVO_RETRASO_REGISTRADO: 'Motivo del retraso',
  PEDIDO_FINALIZADO: 'Pedido finalizado',
};

/** Nombres de los 7 roles iniciales; un rol creado por el admin se muestra con su código. */
const NOMBRE_ROL: Record<string, string> = {
  admin: 'Administrador',
  asistente_compras: 'Asistente de compras',
  marketing: 'Marketing y codificación',
  compras_china: 'Compras China',
  logistica: 'Logística',
  asistente_china: 'Asistente de compras en China',
  consulta: 'Consulta',
};

export function nombreRol(codigo: string | null | undefined): string {
  if (!codigo) return 'Automática';
  return NOMBRE_ROL[codigo] ?? codigo;
}

/**
 * Pista para la pantalla, no un permiso: si la persona podría actuar en una etapa según P-06 (asignada vigente con el
 * rol, o con el rol si no hay asignación) o por ser admin. Sirve para decidir qué ofrecer; el servidor vuelve a decidir
 * con el estado vigente y su respuesta manda (R6). `consulta` y `asistente_china` no conceden nada, pero no quitan lo
 * que dan otros roles.
 */
export function podriaActuarEn(
  etapa: Pick<EtapaEnLinea, 'rolResponsable' | 'asignadoA' | 'asignacionVigente'>,
  usuario: Usuario,
): boolean {
  if (usuario.roles.includes('admin')) return true;
  const rol = etapa.rolResponsable;
  if (!rol || !usuario.roles.includes(rol)) return false;
  if (!etapa.asignadoA) return true;
  return etapa.asignadoA.id === usuario.id && etapa.asignacionVigente !== false;
}

/**
 * Mensaje de un error de pedidos para mostrar a la persona. Los casos del contrato tienen su texto; el `detail` del
 * servidor no se muestra salvo en VALIDACION, donde los errores por campo dicen qué corregir.
 */
export function mensajeDeErrorPedido(error: unknown, general: string): string {
  if (esErrorApi(error, 'ETAPA_CAMBIO')) {
    return 'Otra persona actualizó este pedido mientras lo veías. Ya se muestra el estado actual: revísalo antes de volver a intentarlo.';
  }
  if (esErrorApi(error, 'TRANSICION_NO_PERMITIDA')) {
    return 'Esta acción ya no está permitida en la etapa actual del pedido.';
  }
  if (esErrorApi(error, 'REQUISITO_FALTANTE')) {
    return error.faltantes.length > 0
      ? `Falta: ${error.faltantes.join(', ')}.`
      : 'Faltan requisitos de la etapa para completar la acción.';
  }
  if (esErrorApi(error, 'ARCHIVO_DEMASIADO_GRANDE')) {
    return 'El archivo supera el tamaño máximo permitido (100 MiB, valor provisional).';
  }
  if (esErrorApi(error, 'TIPO_ARCHIVO_NO_ACEPTADO')) {
    return 'El servidor no acepta este tipo de archivo.';
  }
  if (esErrorApi(error, 'ARCHIVO_NO_DISPONIBLE')) {
    return 'El archivo no está disponible o está dañado, y el servidor no lo entrega. Avisa al administrador.';
  }
  if (esErrorApi(error, 'VALIDACION')) {
    const detalle = error.errores.map((e) => e.mensaje).join(' ');
    return detalle || 'Revisa los datos e intenta de nuevo.';
  }
  if (esErrorApi(error, 'NO_ENCONTRADO')) {
    return 'El pedido o el archivo ya no existe.';
  }
  if (esErrorApi(error, CODIGO_RESPUESTA_INVALIDA)) {
    return 'La respuesta del servidor no tiene la forma esperada. Avisa al equipo técnico.';
  }
  return mensajeDeError(error, general);
}

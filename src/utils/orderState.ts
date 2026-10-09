import {
  Pedido,
  Usuario,
  HistorialAuditoria,
  EstadoPedido,
  ObservacionMensaje,
  ArchivoVersion,
  Semaforo,
  EtapaInstancia,
  UrgenciaNivel,
} from '../types';
import {
  SIMULATED_TODAY,
  getStageTrafficLight,
  addBusinessDays,
  formatDisplayDate,
  countBusinessDaysDiff,
} from './dateUtils';
import { ETAPAS_FLUJO_A, ETAPAS_FLUJO_B } from '../data/initialData';

export function getStageDefinitions(flujo: 'A' | 'B') {
  return flujo === 'A' ? ETAPAS_FLUJO_A : ETAPAS_FLUJO_B;
}

export function getStatusForOrderStage(flujo: 'A' | 'B', stageNum: number): EstadoPedido {
  if (flujo === 'A') {
    switch (stageNum) {
      case 1: return 'Solicitud creada';
      case 2: return 'En codificación';
      case 3: return 'En revisión y aprobación';
      case 4: return 'En análisis y gestión';
      case 5: return 'En fabricación';
      case 6: return 'En embarque';
      case 7: return 'En aduana';
      case 8: return 'En autorización de salida';
      case 9: return 'En bodega';
      case 10: return 'En incidencias';
      default: return 'Finalizado';
    }
  } else {
    switch (stageNum) {
      case 1: return 'Solicitud creada';
      case 2: return 'En revisión de producto propuesto';
      case 3: return 'En codificación';
      case 4: return 'En revisión y aprobación';
      case 5: return 'En análisis y gestión';
      case 6: return 'En fabricación';
      case 7: return 'En embarque';
      case 8: return 'En aduana';
      case 9: return 'En autorización de salida';
      case 10: return 'En bodega';
      case 11: return 'En incidencias';
      default: return 'Finalizado';
    }
  }
}

/**
 * Recalcula en cascada las fechas estimadas a partir de una etapa dada
 */
export function recalculateOrderStages(pedido: Pedido): Pedido {
  const etapas = [...pedido.etapas];
  const defs = getStageDefinitions(pedido.flujo);
  let currentStart = pedido.fechaPedido;

  for (let i = 0; i < etapas.length; i++) {
    const stage = { ...etapas[i] };
    const def = defs[i];
    const daysToAdd = def.plazoDiasHabiles !== undefined ? def.plazoDiasHabiles : 1;

    stage.fechaInicioEstimada = currentStart;
    stage.fechaFinEstimada = addBusinessDays(currentStart, daysToAdd);

    // Si hubo reporte de demora en aduana en esta etapa
    if (
      ((pedido.flujo === 'A' && stage.numero === 7) || (pedido.flujo === 'B' && stage.numero === 8)) &&
      pedido.demoraAduana
    ) {
      stage.fechaFinEstimada = pedido.demoraAduana.nuevaFechaEstimadaLlegada;
    }

    etapas[i] = stage;

    // Próxima etapa inicia en la fecha real si terminó, o en la estimada si está en curso
    if (stage.completada && stage.fechaRealFin) {
      currentStart = stage.fechaRealFin;
    } else {
      currentStart = stage.fechaFinEstimada;
    }
  }

  return {
    ...pedido,
    etapas,
  };
}

/**
 * Calcula el resumen, semáforo dinámico y estado de entrega de un pedido
 */
export function calculateOrderSummary(pedido: Pedido) {
  const defs = getStageDefinitions(pedido.flujo);
  const maxEtapa = defs.length;
  const isFinished = pedido.estado === 'Finalizado' || pedido.etapaActualNumero >= maxEtapa;

  if (isFinished) {
    const lastStage = pedido.etapas[pedido.etapas.length - 1];
    return {
      semaforo: 'verde' as Semaforo,
      diasDiferencia: 0,
      esAtrasado: false,
      aviso: 'Pedido finalizado y recibido con éxito',
      etaBodega: formatDisplayDate(lastStage?.fechaRealFin || lastStage?.fechaFinEstimada),
      progresoPorcentaje: 100,
    };
  }

  if (pedido.estado === 'En pausa' || pedido.estado === 'Cancelado') {
    return {
      semaforo: 'gris' as Semaforo,
      diasDiferencia: 0,
      esAtrasado: false,
      aviso: pedido.estado === 'En pausa' ? 'Pedido en pausa' : 'Pedido cancelado',
      etaBodega: 'Suspendido',
      progresoPorcentaje: Math.round(((pedido.etapaActualNumero - 1) / maxEtapa) * 100),
    };
  }

  const currentStageIndex = pedido.etapaActualNumero - 1;
  const currentStage = pedido.etapas[currentStageIndex];
  const lastStage = pedido.etapas[pedido.etapas.length - 1];
  const etaBodega = formatDisplayDate(lastStage?.fechaFinEstimada);

  if (!currentStage) {
    return {
      semaforo: 'verde' as Semaforo,
      diasDiferencia: 0,
      esAtrasado: false,
      aviso: 'En curso',
      etaBodega,
      progresoPorcentaje: 10,
    };
  }

  // Verificar urgencia activa (4 h / 12 h / 24 h)
  if (pedido.urgenciaDevolucion) {
    return {
      semaforo: 'naranja' as Semaforo,
      diasDiferencia: 0,
      esAtrasado: false,
      aviso: `⚡ URGENCIA ${pedido.urgenciaDevolucion.nivel.toUpperCase()} (${pedido.urgenciaDevolucion.horasMaximas} h) activa`,
      etaBodega,
      progresoPorcentaje: Math.round((currentStageIndex / maxEtapa) * 100),
    };
  }

  const traffic = getStageTrafficLight(
    currentStage.fechaFinEstimada,
    currentStage.completada,
    currentStage.fechaRealFin,
    SIMULATED_TODAY
  );

  return {
    semaforo: traffic.semaforo,
    diasDiferencia: traffic.diasDiferencia,
    esAtrasado: traffic.esAtrasado,
    aviso: traffic.textoEstado,
    etaBodega,
    progresoPorcentaje: Math.max(8, Math.round((currentStageIndex / maxEtapa) * 100)),
  };
}

/**
 * Da Check (✓) y avanza a la siguiente etapa
 */
export function advanceOrderStage(
  pedido: Pedido,
  usuario: Usuario,
  observaciones?: string
): { updatedPedido: Pedido; message: string } {
  const defs = getStageDefinitions(pedido.flujo);
  const currentStageNum = pedido.etapaActualNumero;
  const nextStageNum = currentStageNum + 1;
  const updatedEtapas = [...pedido.etapas];
  const currentIdx = currentStageNum - 1;

  if (!updatedEtapas[currentIdx]) {
    return { updatedPedido: pedido, message: 'Etapa no válida' };
  }

  // Marcar etapa actual como completada
  const stageCompleted = {
    ...updatedEtapas[currentIdx],
    completada: true,
    fechaRealFin: SIMULATED_TODAY,
    completadoPor: usuario.nombre,
    completadoPorRol: usuario.cargo,
  };

  // Si hay observaciones, agregarlas
  if (observaciones && observaciones.trim()) {
    const newObs: ObservacionMensaje = {
      id: `obs-${Date.now()}`,
      usuarioId: usuario.id,
      usuarioNombre: usuario.nombre,
      usuarioRol: usuario.cargo,
      avatar: usuario.avatar,
      fecha: `${formatDisplayDate(SIMULATED_TODAY)} 12:00`,
      texto: observaciones.trim(),
    };
    stageCompleted.observaciones = [...stageCompleted.observaciones, newObs];
  }

  updatedEtapas[currentIdx] = stageCompleted;

  // Comprobar si pasa a la etapa Fin (automática)
  const isAutoFinishing =
    (pedido.flujo === 'A' && currentStageNum === 10) ||
    (pedido.flujo === 'B' && currentStageNum === 11);

  let finalStageNum = nextStageNum;
  let finalEstado: EstadoPedido = getStatusForOrderStage(pedido.flujo, nextStageNum);

  if (isAutoFinishing) {
    const finIdx = currentIdx + 1;
    if (updatedEtapas[finIdx]) {
      updatedEtapas[finIdx] = {
        ...updatedEtapas[finIdx],
        completada: true,
        fechaRealFin: SIMULATED_TODAY,
        completadoPor: 'Sistema (Automático)',
        completadoPorRol: 'Fin automático',
      };
    }
    finalStageNum = nextStageNum;
    finalEstado = 'Finalizado';
  }

  const newHistorialItem: HistorialAuditoria = {
    id: `hist-${Date.now()}`,
    fecha: `${formatDisplayDate(SIMULATED_TODAY)} 12:30`,
    usuarioId: usuario.id,
    usuarioNombre: usuario.nombre,
    usuarioRol: usuario.cargo,
    pedidoCodigo: pedido.codigo,
    accion: `Aprobó check en etapa ${currentStageNum}: ${defs[currentIdx].nombre}`,
    etapaNumero: currentStageNum,
    etapaNombre: defs[currentIdx].nombre,
    motivo: observaciones,
    tipo: 'check',
  };

  let updatedPedido: Pedido = {
    ...pedido,
    etapaActualNumero: finalStageNum,
    estado: finalEstado,
    urgenciaDevolucion: undefined, // Limpiar urgencia si existía
    etapas: updatedEtapas,
    historial: [newHistorialItem, ...pedido.historial],
  };

  updatedPedido = recalculateOrderStages(updatedPedido);

  const msg = isAutoFinishing
    ? `Check completado. Etapa ${defs[defs.length - 1].nombre} marcada automáticamente. ¡Pedido finalizado!`
    : `Check aprobado en ${defs[currentIdx].nombre}. Avanzó a etapa ${finalStageNum}: ${defs[finalStageNum - 1]?.nombre || ''}`;

  return {
    updatedPedido,
    message: msg,
  };
}

/**
 * Rechazar (✗) con observaciones — Marcela en Revisión
 * Flujo A: vuelve a Codificación (etapa 2)
 * Flujo B: en etapa 2 vuelve a etapa 1; en etapa 4 vuelve a Codificación (etapa 3)
 */
export function rejectStageWithObservations(
  pedido: Pedido,
  usuario: Usuario,
  observaciones: string
): { updatedPedido: Pedido; message: string } {
  const currentStageNum = pedido.etapaActualNumero;
  const defs = getStageDefinitions(pedido.flujo);
  const currentIdx = currentStageNum - 1;

  let returnTargetStageNum = 2; // Por defecto etapa de codificación
  if (pedido.flujo === 'B') {
    returnTargetStageNum = currentStageNum === 2 ? 1 : 3;
  }

  const targetIdx = returnTargetStageNum - 1;
  const updatedEtapas = [...pedido.etapas];

  // Registrar observación
  const newObs: ObservacionMensaje = {
    id: `obs-${Date.now()}`,
    usuarioId: usuario.id,
    usuarioNombre: usuario.nombre,
    usuarioRol: usuario.cargo,
    avatar: usuario.avatar,
    fecha: `${formatDisplayDate(SIMULATED_TODAY)} 14:15`,
    texto: `[RECHAZADO]: ${observaciones}`,
  };

  if (updatedEtapas[currentIdx]) {
    updatedEtapas[currentIdx] = {
      ...updatedEtapas[currentIdx],
      observaciones: [...updatedEtapas[currentIdx].observaciones, newObs],
    };
  }

  // Reabrir etapa de destino
  if (updatedEtapas[targetIdx]) {
    updatedEtapas[targetIdx] = {
      ...updatedEtapas[targetIdx],
      completada: false,
      devuelta: true,
      fechaRealFin: undefined,
      observaciones: [...updatedEtapas[targetIdx].observaciones, newObs],
    };
  }

  const newHistorialItem: HistorialAuditoria = {
    id: `hist-${Date.now()}`,
    fecha: `${formatDisplayDate(SIMULATED_TODAY)} 14:15`,
    usuarioId: usuario.id,
    usuarioNombre: usuario.nombre,
    usuarioRol: usuario.cargo,
    pedidoCodigo: pedido.codigo,
    accion: `Rechazó (✗) en etapa ${currentStageNum}. Devolvió pedido a etapa ${returnTargetStageNum}: ${defs[targetIdx].nombre}`,
    etapaNumero: currentStageNum,
    etapaNombre: defs[currentIdx].nombre,
    motivo: observaciones,
    tipo: 'rechazo',
  };

  let updatedPedido: Pedido = {
    ...pedido,
    etapaActualNumero: returnTargetStageNum,
    estado: 'Devuelto a codificación',
    etapas: updatedEtapas,
    historial: [newHistorialItem, ...pedido.historial],
  };

  updatedPedido = recalculateOrderStages(updatedPedido);

  return {
    updatedPedido,
    message: `Pedido devuelto a etapa ${returnTargetStageNum} (${defs[targetIdx].nombre}) con observaciones.`,
  };
}

/**
 * Devolver por error o producto nuevo con Urgencia (4 h / 12 h / 24 h) — David en Análisis
 * Vuelve a Codificación (etapa 2 en A / etapa 3 en B)
 */
export function returnToCodingWithUrgency(
  pedido: Pedido,
  usuario: Usuario,
  motivo: string,
  urgencia: UrgenciaNivel,
  archivosOpcionales?: string[]
): { updatedPedido: Pedido; message: string } {
  const currentStageNum = pedido.etapaActualNumero;
  const defs = getStageDefinitions(pedido.flujo);
  const currentIdx = currentStageNum - 1;
  const targetStageNum = pedido.flujo === 'A' ? 2 : 3;
  const targetIdx = targetStageNum - 1;

  const horasMaximas = urgencia === 'Urgente' ? 4 : urgencia === 'Prioritario' ? 12 : 24;

  const urgenciaInfo = {
    nivel: urgencia,
    horasMaximas,
    motivo,
    activadaEn: `${formatDisplayDate(SIMULATED_TODAY)} 10:00`,
    fechaLimite: `${formatDisplayDate(SIMULATED_TODAY)} ${urgencia === 'Urgente' ? '14:00' : '18:00'}`,
    asignadoA: 'Andrea Quishpe (Marketing)',
  };

  const updatedEtapas = [...pedido.etapas];

  const newObs: ObservacionMensaje = {
    id: `obs-${Date.now()}`,
    usuarioId: usuario.id,
    usuarioNombre: usuario.nombre,
    usuarioRol: usuario.cargo,
    avatar: usuario.avatar,
    fecha: `${formatDisplayDate(SIMULATED_TODAY)} 10:00`,
    texto: `[DEVOLUCIÓN DESDE CHINA - URGENCIA ${urgencia.toUpperCase()} (${horasMaximas} h)]: ${motivo}`,
  };

  if (updatedEtapas[currentIdx]) {
    updatedEtapas[currentIdx] = {
      ...updatedEtapas[currentIdx],
      observaciones: [...updatedEtapas[currentIdx].observaciones, newObs],
    };
  }

  if (updatedEtapas[targetIdx]) {
    updatedEtapas[targetIdx] = {
      ...updatedEtapas[targetIdx],
      completada: false,
      devuelta: true,
      fechaRealFin: undefined,
      motivoRetraso: `Urgencia ${urgencia} (${horasMaximas} h) asignada desde China.`,
      observaciones: [...updatedEtapas[targetIdx].observaciones, newObs],
    };
  }

  const newHistorialItem: HistorialAuditoria = {
    id: `hist-${Date.now()}`,
    fecha: `${formatDisplayDate(SIMULATED_TODAY)} 10:00`,
    usuarioId: usuario.id,
    usuarioNombre: usuario.nombre,
    usuarioRol: usuario.cargo,
    pedidoCodigo: pedido.codigo,
    accion: `David Túlcan devolvió desde China con urgencia ${urgencia.toUpperCase()} (${horasMaximas} h) a etapa ${targetStageNum} (${defs[targetIdx].nombre})`,
    etapaNumero: currentStageNum,
    etapaNombre: defs[currentIdx].nombre,
    motivo,
    tipo: 'devolucion_urgente',
    driveArchivos: archivosOpcionales,
  };

  let updatedPedido: Pedido = {
    ...pedido,
    etapaActualNumero: targetStageNum,
    estado: 'Devuelto a codificación',
    urgenciaDevolucion: urgenciaInfo,
    etapas: updatedEtapas,
    historial: [newHistorialItem, ...pedido.historial],
  };

  updatedPedido = recalculateOrderStages(updatedPedido);

  return {
    updatedPedido,
    message: `Devolución enviada a Codificación con urgencia ${urgencia} (${horasMaximas} h). Alerta enviada por Gmail.`,
  };
}

/**
 * Reportar demora en Aduana y recalcular fecha de llegada
 */
export function reportCustomsDelay(
  pedido: Pedido,
  usuario: Usuario,
  motivo: string,
  nuevaFechaEstimadaLlegada: string,
  diasAjuste: number
): { updatedPedido: Pedido; message: string } {
  const currentStageNum = pedido.etapaActualNumero;
  const defs = getStageDefinitions(pedido.flujo);
  const currentIdx = currentStageNum - 1;

  const updatedEtapas = [...pedido.etapas];
  if (updatedEtapas[currentIdx]) {
    updatedEtapas[currentIdx] = {
      ...updatedEtapas[currentIdx],
      fechaFinEstimada: nuevaFechaEstimadaLlegada,
      motivoRetraso: motivo,
    };
  }

  const newHistorialItem: HistorialAuditoria = {
    id: `hist-${Date.now()}`,
    fecha: `${formatDisplayDate(SIMULATED_TODAY)} 16:30`,
    usuarioId: usuario.id,
    usuarioNombre: usuario.nombre,
    usuarioRol: usuario.cargo,
    pedidoCodigo: pedido.codigo,
    accion: `Reportó demora en Aduana (+${diasAjuste} días). Nueva llegada estimada: ${formatDisplayDate(nuevaFechaEstimadaLlegada)}`,
    etapaNumero: currentStageNum,
    etapaNombre: defs[currentIdx].nombre,
    motivo,
    tipo: 'demora_aduana',
  };

  let updatedPedido: Pedido = {
    ...pedido,
    demoraAduana: {
      reportadaPor: usuario.nombre,
      fechaReporte: formatDisplayDate(SIMULATED_TODAY),
      motivo,
      nuevaFechaEstimadaLlegada,
      diasAjuste,
    },
    etapas: updatedEtapas,
    historial: [newHistorialItem, ...pedido.historial],
  };

  updatedPedido = recalculateOrderStages(updatedPedido);

  return {
    updatedPedido,
    message: `Demora de +${diasAjuste} días reportada. Cronograma de llegada recalculado en cascada.`,
  };
}

/**
 * Seleccionar bodega de destino (Anderson en etapa Bodega)
 */
export function setOrderDestinationWarehouse(
  pedido: Pedido,
  usuario: Usuario,
  bodegaNombre: string
): Pedido {
  const currentStageNum = pedido.etapaActualNumero;
  const defs = getStageDefinitions(pedido.flujo);
  const currentIdx = currentStageNum - 1;

  const newHistorialItem: HistorialAuditoria = {
    id: `hist-${Date.now()}`,
    fecha: `${formatDisplayDate(SIMULATED_TODAY)} 11:20`,
    usuarioId: usuario.id,
    usuarioNombre: usuario.nombre,
    usuarioRol: usuario.cargo,
    pedidoCodigo: pedido.codigo,
    accion: `Asignó bodega de destino: ${bodegaNombre}`,
    etapaNumero: currentStageNum,
    etapaNombre: defs[currentIdx]?.nombre || 'Bodega',
    tipo: 'bodega',
  };

  return {
    ...pedido,
    bodegaDestino: bodegaNombre,
    historial: [newHistorialItem, ...pedido.historial],
  };
}

/**
 * Toggle de checkbox individual
 */
export function toggleStageChecklistItem(
  pedido: Pedido,
  stageNumber: number,
  checkId: string,
  usuario: Usuario
): Pedido {
  const updatedEtapas = [...pedido.etapas];
  const stage = { ...updatedEtapas[stageNumber - 1] };
  if (!stage) return pedido;

  stage.checklist = stage.checklist.map((item) => {
    if (item.id === checkId) {
      const willBeDone = !item.completado;
      return {
        ...item,
        completado: willBeDone,
        completadoPor: willBeDone ? usuario.nombre : undefined,
        fecha: willBeDone ? `${formatDisplayDate(SIMULATED_TODAY)} 11:00` : undefined,
      };
    }
    return item;
  });

  updatedEtapas[stageNumber - 1] = stage;
  return {
    ...pedido,
    etapas: updatedEtapas,
  };
}

/**
 * Agregar mensaje / observación a una etapa
 */
export function addStageObservation(
  pedido: Pedido,
  stageNumber: number,
  usuario: Usuario,
  texto: string,
  archivosAdjuntos?: { nombre: string; tamano: string; driveUrl?: string }[]
): Pedido {
  const updatedEtapas = [...pedido.etapas];
  const stage = { ...updatedEtapas[stageNumber - 1] };
  if (!stage) return pedido;

  const newObs: ObservacionMensaje = {
    id: `obs-${Date.now()}`,
    usuarioId: usuario.id,
    usuarioNombre: usuario.nombre,
    usuarioRol: usuario.cargo,
    avatar: usuario.avatar,
    fecha: `${formatDisplayDate(SIMULATED_TODAY)} 12:45`,
    texto,
    archivosAdjuntos,
  };

  stage.observaciones = [...stage.observaciones, newObs];
  updatedEtapas[stageNumber - 1] = stage;

  const newHistorialItem: HistorialAuditoria = {
    id: `hist-${Date.now()}`,
    fecha: `${formatDisplayDate(SIMULATED_TODAY)} 12:45`,
    usuarioId: usuario.id,
    usuarioNombre: usuario.nombre,
    usuarioRol: usuario.cargo,
    pedidoCodigo: pedido.codigo,
    accion: `Agregó observación en etapa ${stageNumber}: ${stage.nombre}`,
    etapaNumero: stageNumber,
    etapaNombre: stage.nombre,
    motivo: texto.slice(0, 100),
    tipo: 'observacion',
  };

  return {
    ...pedido,
    etapas: updatedEtapas,
    historial: [newHistorialItem, ...pedido.historial],
  };
}

/**
 * Simular subida de archivo a Google Drive (máximo 100 MB)
 */
export function uploadStageFile(
  pedido: Pedido,
  stageNumber: number,
  docId: string,
  fileInfo: {
    nombre: string;
    tamano: string;
    driveUrl?: string;
    esImagen?: boolean;
  },
  usuario: Usuario
): Pedido {
  const updatedEtapas = [...pedido.etapas];
  const stage = { ...updatedEtapas[stageNumber - 1] };
  if (!stage) return pedido;

  stage.documentos = stage.documentos.map((doc) => {
    if (doc.id === docId) {
      const currentVersion = doc.archivoActual ? doc.archivoActual.version : 0;
      const nextVersion = currentVersion + 1;

      const newVersion: ArchivoVersion = {
        version: nextVersion,
        nombre: fileInfo.nombre,
        driveId: `gdrive-${Date.now()}`,
        driveUrl: fileInfo.driveUrl || `https://drive.google.com/decokasa/${fileInfo.nombre}`,
        tamano: fileInfo.tamano,
        subidoPor: usuario.nombre,
        subidoPorRol: usuario.cargo,
        fecha: `${formatDisplayDate(SIMULATED_TODAY)} 10:00`,
        esImagen: fileInfo.esImagen,
      };

      const newHistorialVersiones = doc.archivoActual
        ? [doc.archivoActual, ...doc.historialVersiones]
        : doc.historialVersiones;

      return {
        ...doc,
        archivoActual: newVersion,
        historialVersiones: newHistorialVersiones,
      };
    }
    return doc;
  });

  updatedEtapas[stageNumber - 1] = stage;

  const newHistorialItem: HistorialAuditoria = {
    id: `hist-${Date.now()}`,
    fecha: `${formatDisplayDate(SIMULATED_TODAY)} 10:00`,
    usuarioId: usuario.id,
    usuarioNombre: usuario.nombre,
    usuarioRol: usuario.cargo,
    pedidoCodigo: pedido.codigo,
    accion: `Subió a Google Drive: ${fileInfo.nombre} en etapa ${stageNumber}`,
    etapaNumero: stageNumber,
    etapaNombre: stage.nombre,
    tipo: 'documento',
  };

  return {
    ...pedido,
    etapas: updatedEtapas,
    historial: [newHistorialItem, ...pedido.historial],
  };
}

/**
 * Pausar, Reanudar o Cancelar pedido (Admin o Junta)
 */
export function setOrderStatusChange(
  pedido: Pedido,
  usuario: Usuario,
  accion: 'pausar' | 'reanudar' | 'cancelar',
  motivo?: string
): { updatedPedido: Pedido; message: string } {
  let nuevoEstado: EstadoPedido = pedido.estado;
  let accionTexto = '';

  switch (accion) {
    case 'pausar':
      nuevoEstado = 'En pausa';
      accionTexto = 'Pausó temporalmente el pedido';
      break;
    case 'reanudar':
      nuevoEstado = getStatusForOrderStage(pedido.flujo, pedido.etapaActualNumero);
      accionTexto = 'Reanudó el curso del pedido';
      break;
    case 'cancelar':
      nuevoEstado = 'Cancelado';
      accionTexto = 'Canceló definitivamente el pedido';
      break;
  }

  const newHistorialItem: HistorialAuditoria = {
    id: `hist-${Date.now()}`,
    fecha: `${formatDisplayDate(SIMULATED_TODAY)} 16:30`,
    usuarioId: usuario.id,
    usuarioNombre: usuario.nombre,
    usuarioRol: usuario.cargo,
    pedidoCodigo: pedido.codigo,
    accion: accionTexto,
    etapaNumero: pedido.etapaActualNumero,
    etapaNombre: pedido.etapas[pedido.etapaActualNumero - 1]?.nombre,
    motivo,
    tipo: 'estado',
  };

  const updatedPedido: Pedido = {
    ...pedido,
    estado: nuevoEstado,
    motivoPausaCancelacion: motivo,
    historial: [newHistorialItem, ...pedido.historial],
  };

  return {
    updatedPedido,
    message: `${accionTexto} exitosamente.`,
  };
}

/**
 * Modificar manualmente fecha base de pedido y recalcular
 */
export function updateOrderDatesManually(
  pedido: Pedido,
  nuevaFechaPedido: string,
  usuario: Usuario,
  motivo: string
): Pedido {
  let updatedPedido: Pedido = {
    ...pedido,
    fechaPedido: nuevaFechaPedido,
    version: pedido.version === 'v1' ? 'v2' : 'v3',
    historial: [
      {
        id: `hist-${Date.now()}`,
        fecha: `${formatDisplayDate(SIMULATED_TODAY)} 17:00`,
        usuarioId: usuario.id,
        usuarioNombre: usuario.nombre,
        usuarioRol: usuario.cargo,
        pedidoCodigo: pedido.codigo,
        accion: `Modificó fecha base de pedido a ${formatDisplayDate(nuevaFechaPedido)}`,
        motivo,
        tipo: 'estado',
      },
      ...pedido.historial,
    ],
  };

  return recalculateOrderStages(updatedPedido);
}

// Aliases para compatibilidad
export const returnOrderWithObservations = rejectStageWithObservations;
export const revertOrderCheck = (pedido: Pedido, usuario: Usuario, motivo: string) => {
  return rejectStageWithObservations(pedido, usuario, `[REVERSIÓN DE CHECK]: ${motivo}`);
};
export const extendFabricationDays = (pedido: Pedido, _u: Usuario, _d: number, _m: string) => ({
  updatedPedido: pedido,
  message: 'Ampliación registrada',
});
export const updateCustomsSubstate = (pedido: Pedido, _u: Usuario, _s: any) => ({
  updatedPedido: pedido,
  message: 'Subestado aduanero actualizado',
});
export const updateDestinationWarehouse = setOrderDestinationWarehouse;
export const toggleStageChecklist = toggleStageChecklistItem;
export const updateProductQuantityReceived = (
  pedido: Pedido,
  lineId: string,
  recibida: number,
  incidencia?: string
): Pedido => {
  const updatedProductos = pedido.productos.map((prod) => {
    if (prod.id === lineId) {
      const diferencia = recibida - prod.cantidadPedida;
      let estado: 'Completo' | 'Faltante' | 'Sobrante' | 'Dañado' | 'Pendiente' = 'Completo';
      if (diferencia < 0) estado = 'Faltante';
      else if (diferencia > 0) estado = 'Sobrante';
      if (incidencia && incidencia.toLowerCase().includes('dañ')) estado = 'Dañado';
      return {
        ...prod,
        cantidadRecibida: recibida,
        diferencia,
        estado,
        incidencias: incidencia,
      };
    }
    return prod;
  });
  return { ...pedido, productos: updatedProductos };
};

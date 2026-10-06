import {
  Pedido,
  Usuario,
  SubestadoAduana,
  BodegaDestino,
  HistorialAuditoria,
  EstadoPedido,
  ObservacionMensaje,
  ArchivoVersion,
  Semaforo,
  EtapaInstancia,
  ItemChecklist,
  DocumentoEtapa,
} from '../types';
import { SIMULATED_TODAY, getStageTrafficLight, addBusinessDays, formatDisplayDate } from './dateUtils';
import { ETAPAS_BASE_CONFIG } from '../data/initialData';

export function getStatusForStageNumber(stageNum: number, subestado?: SubestadoAduana): EstadoPedido {
  switch (stageNum) {
    case 1:
      return 'Creado';
    case 2:
      return 'En revisión';
    case 3:
      return 'En revisión';
    case 4:
      return 'En fabricación';
    case 5:
    case 6:
    case 7:
      return 'Pendiente de embarque';
    case 8:
      return 'En tránsito';
    case 9:
      return 'En aduana';
    case 10:
      return 'En bodega';
    case 11:
      return 'En cierre documental';
    default:
      return 'Finalizado';
  }
}

export function recalculateOrderStages(pedido: Pedido): Pedido {
  const etapas = [...pedido.etapas];
  let currentStart = pedido.fechaPedido;

  for (let i = 0; i < etapas.length; i++) {
    const stage = { ...etapas[i] };
    const def = ETAPAS_BASE_CONFIG[i];

    let extraDays = 0;
    if (stage.numero === 2 && pedido.esProductoNuevo) {
      extraDays = 10;
    } else if (stage.numero === 4) {
      extraDays = pedido.ampliacionFabricacionDias;
    }
    stage.plazoExtraDias = extraDays;

    const totalDays = def.plazoDiasHabiles + extraDays;
    stage.fechaInicioEstimada = currentStart;
    stage.fechaFinEstimada = addBusinessDays(currentStart, totalDays);

    if (stage.completada && stage.fechaRealFin) {
      currentStart = stage.fechaRealFin;
    } else {
      currentStart = stage.fechaFinEstimada;
    }

    etapas[i] = stage;
  }

  return {
    ...pedido,
    etapas,
  };
}

export function calculateOrderSummary(pedido: Pedido): {
  semaforo: Semaforo;
  diasDiferencia: number;
  esAtrasado: boolean;
  aviso: string;
  etaBodega: string;
  etaBodegaRaw: string;
} {
  const rawEta = pedido.etapas[9]?.fechaRealFin || pedido.etapas[9]?.fechaFinEstimada;
  const formattedEta = formatDisplayDate(rawEta);

  if (pedido.estado === 'Finalizado') {
    return {
      semaforo: 'verde',
      diasDiferencia: 0,
      esAtrasado: false,
      aviso: 'Pedido finalizado con éxito',
      etaBodega: formattedEta,
      etaBodegaRaw: rawEta,
    };
  }

  if (pedido.estado === 'Cancelado') {
    return {
      semaforo: 'gris',
      diasDiferencia: 0,
      esAtrasado: false,
      aviso: 'Pedido cancelado por la Junta',
      etaBodega: formattedEta,
      etaBodegaRaw: rawEta,
    };
  }

  if (pedido.estado === 'En pausa') {
    return {
      semaforo: 'gris',
      diasDiferencia: 0,
      esAtrasado: false,
      aviso: pedido.motivoPausaCancelacion || 'Pedido en pausa por la Junta',
      etaBodega: formattedEta,
      etaBodegaRaw: rawEta,
    };
  }

  // Si está retenido en aduana, alertar en rojo
  if (pedido.subestadoAduana === 'Retenido') {
    return {
      semaforo: 'rojo',
      diasDiferencia: 0,
      esAtrasado: true,
      aviso: 'Retenido en aduana: inspección física requerida',
      etaBodega: formattedEta,
      etaBodegaRaw: rawEta,
    };
  }

  const currentStage = pedido.etapas[pedido.etapaActualNumero - 1];
  if (!currentStage) {
    return {
      semaforo: 'verde',
      diasDiferencia: 0,
      esAtrasado: false,
      aviso: 'A tiempo',
      etaBodega: formattedEta,
      etaBodegaRaw: rawEta,
    };
  }

  const stageResult = getStageTrafficLight(
    currentStage.fechaFinEstimada,
    currentStage.completada,
    currentStage.fechaRealFin,
    SIMULATED_TODAY
  );

  let aviso = 'A tiempo';
  if (stageResult.esAtrasado) {
    aviso = `Este pedido llegará con retraso: ${stageResult.diasDiferencia} días hábiles de atraso`;
  } else if (stageResult.semaforo === 'naranja') {
    aviso = `Plazo próximo a vencer: faltan ${stageResult.diasDiferencia} días hábiles`;
  }

  return {
    semaforo: stageResult.semaforo,
    diasDiferencia: stageResult.diasDiferencia,
    esAtrasado: stageResult.esAtrasado,
    aviso,
    etaBodega: formattedEta,
    etaBodegaRaw: rawEta,
  };
}

/**
 * Verifica si los requisitos de la etapa actual están completos para dar check
 */
export function checkStageReadiness(stage: EtapaInstancia): {
  isReady: boolean;
  missingItems: string[];
} {
  const missingItems: string[] = [];

  // 1. Checklist
  const pendingChecklist = stage.checklist.filter((item) => !item.completado);
  if (pendingChecklist.length > 0) {
    missingItems.push(`Completar ${pendingChecklist.length} punto(s) del check list`);
  }

  // 2. Documentos obligatorios
  const missingDocs = stage.documentos.filter(
    (doc) => doc.esObligatorio && !doc.archivoActual
  );
  if (missingDocs.length > 0) {
    missingDocs.forEach((d) => {
      missingItems.push(`Subir: ${d.tipoDoc}`);
    });
  }

  return {
    isReady: missingItems.length === 0,
    missingItems,
  };
}

export function advanceOrderStage(
  pedido: Pedido,
  usuario: Usuario,
  observaciones?: string
): { updatedPedido: Pedido; message: string } {
  const currentIdx = pedido.etapaActualNumero - 1;
  const currentStage = pedido.etapas[currentIdx];

  const updatedEtapas = [...pedido.etapas];
  updatedEtapas[currentIdx] = {
    ...currentStage,
    completada: true,
    devuelta: false,
    fechaRealFin: SIMULATED_TODAY,
    completadoPor: usuario.nombre,
    completadoPorRol: usuario.area,
    checklist: currentStage.checklist.map((c) => ({
      ...c,
      completado: true,
      completadoPor: c.completadoPor || usuario.nombre,
    })),
  };

  const isLastStage = pedido.etapaActualNumero >= 11;
  const nextStageNum = isLastStage ? 11 : pedido.etapaActualNumero + 1;
  const newEstado = isLastStage ? 'Finalizado' : getStatusForStageNumber(nextStageNum);

  const newHistorialItem: HistorialAuditoria = {
    id: `hist-${Date.now()}`,
    fecha: `${formatDisplayDate(SIMULATED_TODAY)} 10:15`,
    usuarioId: usuario.id,
    usuarioNombre: usuario.nombre,
    usuarioRol: usuario.cargo,
    pedidoCodigo: pedido.codigo,
    accion: isLastStage
      ? 'Completó validación final de cierre documental (Estado: Finalizado)'
      : `Dio check y avanzó la etapa ${currentStage.numero}. ${currentStage.nombre}`,
    etapaNumero: currentStage.numero,
    etapaNombre: currentStage.nombre,
    motivo: observaciones || 'Check list y documentos completados satisfactoriamente',
    tipo: 'check',
  };

  let updatedPedido: Pedido = {
    ...pedido,
    etapaActualNumero: nextStageNum,
    estado: newEstado,
    etapas: updatedEtapas,
    historial: [newHistorialItem, ...pedido.historial],
  };

  updatedPedido = recalculateOrderStages(updatedPedido);

  return {
    updatedPedido,
    message: isLastStage
      ? `¡Pedido ${pedido.codigo} finalizado exitosamente!`
      : `Check registrado en etapa ${currentStage.numero}. Avanzado a etapa ${nextStageNum}.`,
  };
}

export function returnOrderWithObservations(
  pedido: Pedido,
  usuario: Usuario,
  observaciones: string
): { updatedPedido: Pedido; message: string } {
  const currentStage = pedido.etapas[pedido.etapaActualNumero - 1];

  const updatedEtapas = [...pedido.etapas];
  updatedEtapas[pedido.etapaActualNumero - 1] = {
    ...currentStage,
    devuelta: true,
  };

  const newHistorialItem: HistorialAuditoria = {
    id: `hist-${Date.now()}`,
    fecha: `${formatDisplayDate(SIMULATED_TODAY)} 11:20`,
    usuarioId: usuario.id,
    usuarioNombre: usuario.nombre,
    usuarioRol: usuario.cargo,
    pedidoCodigo: pedido.codigo,
    accion: `Devolvió la etapa ${currentStage.numero}. ${currentStage.nombre} con observaciones`,
    etapaNumero: currentStage.numero,
    etapaNombre: currentStage.nombre,
    motivo: observaciones,
    tipo: 'devolucion',
  };

  const updatedPedido: Pedido = {
    ...pedido,
    estado: 'Devuelto con observaciones',
    etapas: updatedEtapas,
    historial: [newHistorialItem, ...pedido.historial],
  };

  return {
    updatedPedido,
    message: `Pedido devuelto con observaciones registrado en historial.`,
  };
}

export function revertOrderCheck(
  pedido: Pedido,
  usuario: Usuario,
  motivo: string
): { updatedPedido: Pedido; message: string } {
  let targetStageIdx = pedido.etapaActualNumero - 1;
  if (!pedido.etapas[targetStageIdx].completada && targetStageIdx > 0) {
    targetStageIdx = targetStageIdx - 1;
  }

  const updatedEtapas = [...pedido.etapas];
  const targetStage = updatedEtapas[targetStageIdx];
  updatedEtapas[targetStageIdx] = {
    ...targetStage,
    completada: false,
    fechaRealFin: undefined,
    completadoPor: undefined,
    completadoPorRol: undefined,
  };

  const newStageNum = targetStage.numero;
  const newEstado = getStatusForStageNumber(newStageNum);

  const newHistorialItem: HistorialAuditoria = {
    id: `hist-${Date.now()}`,
    fecha: `${formatDisplayDate(SIMULATED_TODAY)} 12:30`,
    usuarioId: usuario.id,
    usuarioNombre: usuario.nombre,
    usuarioRol: usuario.cargo,
    pedidoCodigo: pedido.codigo,
    accion: `Revirtió el check de la etapa ${targetStage.numero}. ${targetStage.nombre}`,
    etapaNumero: targetStage.numero,
    etapaNombre: targetStage.nombre,
    motivo,
    tipo: 'reversion',
  };

  let updatedPedido: Pedido = {
    ...pedido,
    etapaActualNumero: newStageNum,
    estado: newEstado,
    etapas: updatedEtapas,
    historial: [newHistorialItem, ...pedido.historial],
  };

  updatedPedido = recalculateOrderStages(updatedPedido);

  return {
    updatedPedido,
    message: `Check revertido. El pedido regresó a la etapa ${newStageNum} (${targetStage.nombre}).`,
  };
}

export function extendFabricationDays(
  pedido: Pedido,
  usuario: Usuario,
  diasExtra: number,
  motivo: string
): { updatedPedido: Pedido; message: string; error?: string } {
  const currentExtension = pedido.ampliacionFabricacionDias || 0;
  const newTotal = currentExtension + diasExtra;

  if (newTotal > 10) {
    return {
      updatedPedido: pedido,
      message: '',
      error: `El máximo permitido de ampliación es de +10 días hábiles en total. Actualmente tiene +${currentExtension} días.`,
    };
  }

  const newHistorialItem: HistorialAuditoria = {
    id: `hist-${Date.now()}`,
    fecha: `${formatDisplayDate(SIMULATED_TODAY)} 14:15`,
    usuarioId: usuario.id,
    usuarioNombre: usuario.nombre,
    usuarioRol: usuario.cargo,
    pedidoCodigo: pedido.codigo,
    accion: `Ampliación de plazo de fabricación en +${diasExtra} días hábiles (Acumulado: +${newTotal}d)`,
    etapaNumero: 4,
    etapaNombre: 'Fabricación',
    motivo,
    tipo: 'ampliacion',
  };

  let updatedPedido: Pedido = {
    ...pedido,
    ampliacionFabricacionDias: newTotal,
    historial: [newHistorialItem, ...pedido.historial],
  };

  updatedPedido = recalculateOrderStages(updatedPedido);

  return {
    updatedPedido,
    message: `Plazo de fabricación ampliado en +${diasExtra} días hábiles exitosamente.`,
  };
}

export function updateCustomsSubstate(
  pedido: Pedido,
  usuario: Usuario,
  subestado: SubestadoAduana,
  motivo?: string
): { updatedPedido: Pedido; message: string } {
  const updatedEtapas = [...pedido.etapas];
  if (updatedEtapas[8]) {
    updatedEtapas[8] = {
      ...updatedEtapas[8],
      subestadoAduana: subestado,
    };
  }

  const newHistorialItem: HistorialAuditoria = {
    id: `hist-${Date.now()}`,
    fecha: `${formatDisplayDate(SIMULATED_TODAY)} 15:45`,
    usuarioId: usuario.id,
    usuarioNombre: usuario.nombre,
    usuarioRol: usuario.cargo,
    pedidoCodigo: pedido.codigo,
    accion: `Actualizó subestado aduanero a: ${subestado.toUpperCase()}`,
    etapaNumero: 9,
    etapaNombre: 'Desaduanización',
    motivo: motivo || 'Gestión de trámite SENAE',
    tipo: 'subestado',
  };

  const updatedPedido: Pedido = {
    ...pedido,
    subestadoAduana: subestado,
    etapas: updatedEtapas,
    historial: [newHistorialItem, ...pedido.historial],
  };

  return {
    updatedPedido,
    message: `Subestado aduanero actualizado a "${subestado}".`,
  };
}

export function setOrderStatusChange(
  pedido: Pedido,
  usuario: Usuario,
  accion: 'pausar' | 'reanudar' | 'cancelar' | 'reabrir' | 'aprobar_fabricacion',
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
    case 'reabrir':
      nuevoEstado = getStatusForStageNumber(pedido.etapaActualNumero, pedido.subestadoAduana);
      accionTexto = accion === 'reabrir' ? 'Reabrió el pedido' : 'Reanudó el curso del pedido';
      break;
    case 'cancelar':
      nuevoEstado = 'Cancelado';
      accionTexto = 'Canceló definitivamente el pedido';
      break;
    case 'aprobar_fabricacion':
      nuevoEstado = 'En fabricación';
      accionTexto = 'Aprobó fabricación (Junta de Socios)';
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
    motivoPausaCancelacion: accion === 'pausar' || accion === 'cancelar' ? motivo : undefined,
    usuarioPausaCancelacion: accion === 'pausar' || accion === 'cancelar' ? usuario.nombre : undefined,
    historial: [newHistorialItem, ...pedido.historial],
  };

  return {
    updatedPedido,
    message: `${accionTexto} correctamente.`,
  };
}

export function toggleStageChecklist(
  pedido: Pedido,
  stageNumber: number,
  checkId: string,
  usuario: Usuario
): Pedido {
  const updatedEtapas = [...pedido.etapas];
  const stage = { ...updatedEtapas[stageNumber - 1] };
  const updatedChecklist = stage.checklist.map((item) => {
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

  stage.checklist = updatedChecklist;
  updatedEtapas[stageNumber - 1] = stage;

  return {
    ...pedido,
    etapas: updatedEtapas,
  };
}

export function addStageObservation(
  pedido: Pedido,
  stageNumber: number,
  usuario: Usuario,
  texto: string,
  archivosAdjuntos?: { nombre: string; tamano: string }[]
): Pedido {
  const updatedEtapas = [...pedido.etapas];
  const stage = { ...updatedEtapas[stageNumber - 1] };

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
    accion: `Publicó observación en etapa ${stageNumber}. ${stage.nombre}`,
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

  const updatedDocs = stage.documentos.map((doc) => {
    if (doc.id === docId) {
      const currentVersion = doc.archivoActual ? doc.archivoActual.version : 0;
      const nextVersion = currentVersion + 1;

      const newVersion: ArchivoVersion = {
        version: nextVersion,
        nombre: fileInfo.nombre,
        driveUrl: fileInfo.driveUrl,
        tamano: fileInfo.tamano,
        subidoPor: usuario.nombre,
        subidoPorRol: usuario.area,
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

  stage.documentos = updatedDocs;
  updatedEtapas[stageNumber - 1] = stage;

  const newHistorialItem: HistorialAuditoria = {
    id: `hist-${Date.now()}`,
    fecha: `${formatDisplayDate(SIMULATED_TODAY)} 10:00`,
    usuarioId: usuario.id,
    usuarioNombre: usuario.nombre,
    usuarioRol: usuario.cargo,
    pedidoCodigo: pedido.codigo,
    accion: `Subió archivo: ${fileInfo.nombre} en etapa ${stageNumber}`,
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
        accion: `Modificó fecha base de pedido a ${formatDisplayDate(nuevaFechaPedido)} (Generó nueva versión ${pedido.version === 'v1' ? 'v2' : 'v3'})`,
        motivo,
        tipo: 'config',
      },
      ...pedido.historial,
    ],
  };

  updatedPedido = recalculateOrderStages(updatedPedido);
  return updatedPedido;
}

export function updateOrderDestinationWarehouse(
  pedido: Pedido,
  nuevaBodega: BodegaDestino,
  usuario: Usuario
): Pedido {
  return {
    ...pedido,
    bodegaDestino: nuevaBodega,
    historial: [
      {
        id: `hist-${Date.now()}`,
        fecha: `${formatDisplayDate(SIMULATED_TODAY)} 14:00`,
        usuarioId: usuario.id,
        usuarioNombre: usuario.nombre,
        usuarioRol: usuario.cargo,
        pedidoCodigo: pedido.codigo,
        accion: `Cambió bodega de destino a: ${nuevaBodega}`,
        motivo: 'Ajuste logístico de distribución',
        tipo: 'config',
      },
      ...pedido.historial,
    ],
  };
}

export const updateDestinationWarehouse = updateOrderDestinationWarehouse;

export function updateProductQuantityReceived(
  pedido: Pedido,
  lineId: string,
  recibida: number,
  incidencia?: string
): Pedido {
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

  return {
    ...pedido,
    productos: updatedProductos,
  };
}

export function canUserActOnCurrentStage(usuario: Usuario, stageNum: number): boolean {
  if (usuario.rol === 'admin') return true;
  return usuario.etapasAsignadas.includes(stageNum);
}


import {
  Usuario,
  EtapaDef,
  Pedido,
  EtapaInstancia,
  ProductoLineaPedido,
  BodegaCatalogo,
  NotificacionGmail,
  DocumentoEtapa,
} from '../types';
import { addBusinessDays, formatDisplayDate, SIMULATED_TODAY } from '../utils/dateUtils';

// Definiciones de etapas del demo (referencia para el motor de flujos de la fase 2). En el paso 1.8 se retiraron los
// usuarios, la contraseña de demostración, las bodegas, los pedidos y los avisos simulados (auditoría S1 y S2): el
// acceso real usa la API y los datos de pedidos llegan desde el backend en la fase 2.

// ==========================================
// FLUJO A — PRODUCTO EXISTENTE / ECUADOR (11 Etapas)
// ==========================================
export const ETAPAS_FLUJO_A: EtapaDef[] = [
  {
    numero: 1,
    id: 'a-etapa-1',
    nombre: 'Crear solicitud de pedido',
    fase: 'Solicitud',
    descripcion: 'Mirian Franco (o un admin) usa el formato establecido y sube el documento. Se genera el número de pedido secuencial (DK-EC-2026-XXXX).',
    plazoDiasHabiles: 0,
    responsableTexto: 'Mirian Franco (Admin)',
    responsableRol: 'admin',
    icono: 'FilePlus',
    checklistsBase: [
      'Formato y plantilla de solicitud de pedido completada',
      'Detalle de productos existentes y nuevos requeridos por Ecuador',
      'Archivo de solicitud cargado en Google Drive',
    ],
    documentosRequeridos: [
      { tipoDoc: 'Plantilla de Solicitud de Pedido', obligatorio: true },
    ],
  },
  {
    numero: 2,
    id: 'a-etapa-2',
    nombre: 'Codificación',
    fase: 'Codificación y Revisión',
    descripcion: 'Andrea Quishpe revisa cantidades, etiquetas, modelos, medidas, colores y especificaciones sin campos vacíos. Plazo 24 h (o urgencia 4/12/24 h si viene devuelto de David).',
    plazoHorasHabiles: 24,
    plazoDiasHabiles: 1,
    responsableTexto: 'Andrea Quishpe (Marketing)',
    responsableRol: 'marketing',
    icono: 'Tag',
    checklistsBase: [
      'Cantidades y etiquetas correctas',
      'Modelos, medidas, colores y especificaciones detallados',
      'Sin campos vacíos dentro del documento',
      'Subir documentos codificados a Google Drive',
    ],
    documentosRequeridos: [
      { tipoDoc: 'Documento Codificado y Etiquetas', obligatorio: true },
    ],
  },
  {
    numero: 3,
    id: 'a-etapa-3',
    nombre: 'Revisión y aprobación',
    fase: 'Codificación y Revisión',
    descripcion: 'Marcela Catucuamba ve los documentos de Andrea y aprueba (✓) o desaprueba (✗) con observaciones. Si desaprueba, vuelve a la etapa 2.',
    plazoHorasHabiles: 24,
    plazoDiasHabiles: 1,
    responsableTexto: 'Marcela Catucuamba (Asistente de compras)',
    responsableRol: 'asistente_compras',
    icono: 'CheckCheck',
    checklistsBase: [
      'Verificación documental de la codificación y etiquetas',
      'Precios de proforma y compatibilidad comercial aprobados',
      'Aprobación formal para envío a Compras China',
    ],
    documentosRequeridos: [
      { tipoDoc: 'Acta de Revisión y Aprobación', obligatorio: false },
    ],
  },
  {
    numero: 4,
    id: 'a-etapa-4',
    nombre: 'Análisis y gestión',
    fase: 'China (Gestión y Fabricación)',
    descripcion: 'David Túlcan tiene 3 días para analizar. Si hay errores, devuelve a etapa 2. Si China propone productos nuevos no codificados, vuelve a etapa 2 con Urgencia (4 h, 12 h o 24 h).',
    plazoDiasHabiles: 3,
    responsableTexto: 'David Túlcan (Compras China)',
    responsableRol: 'compras_china',
    icono: 'Search',
    checklistsBase: [
      'Análisis técnico de factibilidad con las fábricas en China',
      'Confirmación de costos de producción y empaque',
      'Verificación de requerimientos de producto nuevo o devolución si aplica',
    ],
    documentosRequeridos: [
      { tipoDoc: 'Informe de Factibilidad y Cotización Fábrica', obligatorio: false },
    ],
  },
  {
    numero: 5,
    id: 'a-etapa-5',
    nombre: 'Fabricación',
    fase: 'China (Gestión y Fabricación)',
    descripcion: 'David coordina la producción con las fábricas. Cuando la fabricación termina, da check y observaciones para pasar a la siguiente etapa.',
    plazoDiasHabiles: 39, // Calculadora: 8 días inicio + 31 días fabricación
    responsableTexto: 'David Túlcan (Compras China)',
    responsableRol: 'compras_china',
    icono: 'Factory',
    checklistsBase: [
      'Orden de fabricación iniciada con el proveedor',
      'Control de calidad intermedio de productos y acabados',
      'Fabricación terminada y lista para consolidación',
    ],
    documentosRequeridos: [
      { tipoDoc: 'Reporte de Inspección de Calidad QC', obligatorio: false },
    ],
  },
  {
    numero: 6,
    id: 'a-etapa-6',
    nombre: 'Embarque',
    fase: 'China (Gestión y Fabricación)',
    descripcion: 'David tiene 10 días para buscar navieras y embarcar. Gestiona Packing List y Bill of Lading (BL), sube archivos con observaciones y da check.',
    plazoDiasHabiles: 10,
    responsableTexto: 'David Túlcan (Compras China)',
    responsableRol: 'compras_china',
    icono: 'Ship',
    checklistsBase: [
      'Cotización y reserva de flete con naviera marítima',
      'Contenedor consolidado e inspeccionado',
      'Subir Packing List formal',
      'Subir Bill of Lading (BL) marítimo',
    ],
    documentosRequeridos: [
      { tipoDoc: 'Packing List de Embarque', obligatorio: true },
      { tipoDoc: 'Bill of Lading (BL) Marítimo', obligatorio: true },
    ],
  },
  {
    numero: 7,
    id: 'a-etapa-7',
    nombre: 'Aduana Ecuador o Colombia',
    fase: 'Tránsito y Llegada',
    descripcion: 'David da check cuando llega la mercadería (tránsito marítimo estándar 45 días). Si se demora, sube una observación para recalcular la llegada.',
    plazoDiasHabiles: 45, // Tránsito marítimo
    responsableTexto: 'David Túlcan (Compras China)',
    responsableRol: 'compras_china',
    icono: 'Anchor',
    checklistsBase: [
      'Seguimiento satelital de la nave en altamar',
      'Arribo a puerto de Guayaquil / Buenaventura',
      'Aforo y trámites aduaneros (SENAE / DIAN)',
    ],
    documentosRequeridos: [
      { tipoDoc: 'Documento de Arribo / Manifiesto de Carga', obligatorio: false },
    ],
  },
  {
    numero: 8,
    id: 'a-etapa-8',
    nombre: 'Autorización de salida',
    fase: 'Tránsito y Llegada',
    descripcion: 'Anderson Enriquez autoriza la salida de aduana con check una vez liberada la mercadería por la autoridad aduanera. Plazo: 24 h.',
    plazoHorasHabiles: 24,
    plazoDiasHabiles: 1,
    responsableTexto: 'Anderson Enriquez (Logística)',
    responsableRol: 'logistica',
    icono: 'ShieldCheck',
    checklistsBase: [
      'Liquidación aduanera y pago de aranceles confirmado',
      'Levante y autorización formal de salida del puerto',
    ],
    documentosRequeridos: [
      { tipoDoc: 'Levante Aduanero / Autorización de Salida', obligatorio: true },
    ],
  },
  {
    numero: 9,
    id: 'a-etapa-9',
    nombre: 'Bodega',
    fase: 'Cierre y Bodega',
    descripcion: 'Anderson Enriquez elige la bodega de destino (Quito, Guayaquil, Ibarra...), da check y sube observaciones y documentos de recepción. Plazo: 24 h.',
    plazoHorasHabiles: 24,
    plazoDiasHabiles: 1,
    responsableTexto: 'Anderson Enriquez (Logística)',
    responsableRol: 'logistica',
    icono: 'Warehouse',
    checklistsBase: [
      'Selección de bodega de destino en el sistema',
      'Recepción física y descarga de contenedor',
      'Conteo inicial de bultos y cajas',
    ],
    documentosRequeridos: [
      { tipoDoc: 'Acta de Ingreso y Recepción en Bodega', obligatorio: true },
    ],
  },
  {
    numero: 10,
    id: 'a-etapa-10',
    nombre: 'Incidencias',
    fase: 'Cierre y Bodega',
    descripcion: 'Anderson Enriquez sube todos los documentos finales y comprobantes de novedades/faltantes/sobrantes, y da check.',
    plazoHorasHabiles: 24,
    plazoDiasHabiles: 1,
    responsableTexto: 'Anderson Enriquez (Logística)',
    responsableRol: 'logistica',
    icono: 'FileCheck',
    checklistsBase: [
      'Verificación de diferencias físicas vs documentos',
      'Registro fotográfico de incidencias o mercadería dañada',
      'Subir todos los documentos de cierre de importación',
    ],
    documentosRequeridos: [
      { tipoDoc: 'Expediente Completo de Importación', obligatorio: true },
      { tipoDoc: 'Reporte de Incidencias y Faltantes', obligatorio: false },
    ],
  },
  {
    numero: 11,
    id: 'a-etapa-11',
    nombre: 'Fin',
    fase: 'Cierre y Bodega',
    descripcion: 'Fin automático: se marca sola cuando la etapa 10 (Incidencias) recibe su check.',
    plazoDiasHabiles: 0,
    esFinAutomatico: true,
    responsableTexto: 'Sistema (Automático)',
    responsableRol: 'admin',
    icono: 'Flag',
    checklistsBase: [
      'Pedido completado y cerrado en su totalidad',
    ],
    documentosRequeridos: [],
  },
];

// ==========================================
// FLUJO B — PRODUCTO NUEVO DE CHINA (12 Etapas)
// ==========================================
export const ETAPAS_FLUJO_B: EtapaDef[] = [
  {
    numero: 1,
    id: 'b-etapa-1',
    nombre: 'Crear solicitud de pedido (China)',
    fase: 'Solicitud',
    descripcion: 'David Túlcan crea la solicitud con el formato establecido y sube el producto nuevo que propone desde China.',
    plazoDiasHabiles: 0,
    responsableTexto: 'David Túlcan (Compras China)',
    responsableRol: 'compras_china',
    icono: 'FilePlus',
    checklistsBase: [
      'Propuesta de producto nuevo desde China redactada',
      'Ficha técnica y cotización preliminar de fábrica',
      'Subir solicitud de pedido a Google Drive',
    ],
    documentosRequeridos: [
      { tipoDoc: 'Propuesta de Producto Nuevo China', obligatorio: true },
    ],
  },
  {
    numero: 2,
    id: 'b-etapa-2',
    nombre: 'Revisión de productos propuestos',
    fase: 'Solicitud',
    descripcion: 'Marcela Catucuamba da check de aprobación hacia Codificación o devuelve a la etapa 1 con observación a David en China.',
    plazoHorasHabiles: 24,
    plazoDiasHabiles: 1,
    responsableTexto: 'Marcela Catucuamba (Asistente de compras)',
    responsableRol: 'asistente_compras',
    icono: 'Sparkles',
    checklistsBase: [
      'Evaluación de viabilidad comercial del producto propuesto',
      'Aprobación de muestra / fotos enviadas desde China',
    ],
    documentosRequeridos: [
      { tipoDoc: 'Visto Bueno Comercial de Producto Nuevo', obligatorio: false },
    ],
  },
  {
    numero: 3,
    id: 'b-etapa-3',
    nombre: 'Codificación',
    fase: 'Codificación y Revisión',
    descripcion: 'Andrea Quishpe revisa cantidades y etiquetas; modelos, medidas, colores y especificaciones detallados sin campos vacíos. Plazo: 24 h (o urgencia 4/12/24 h).',
    plazoHorasHabiles: 24,
    plazoDiasHabiles: 1,
    responsableTexto: 'Andrea Quishpe (Marketing)',
    responsableRol: 'marketing',
    icono: 'Tag',
    checklistsBase: [
      'Cantidades y etiquetas correctas',
      'Modelos, medidas, colores y especificaciones detallados',
      'Sin campos vacíos dentro del documento',
      'Subir documentos codificados a Google Drive',
    ],
    documentosRequeridos: [
      { tipoDoc: 'Documento Codificado y Etiquetas', obligatorio: true },
    ],
  },
  {
    numero: 4,
    id: 'b-etapa-4',
    nombre: 'Revisión y aprobación',
    fase: 'Codificación y Revisión',
    descripcion: 'Marcela Catucuamba ve los documentos de Andrea y aprueba (✓) o desaprueba (✗). Si desaprueba, vuelve a la etapa 3 (Codificación).',
    plazoHorasHabiles: 24,
    plazoDiasHabiles: 1,
    responsableTexto: 'Marcela Catucuamba (Asistente de compras)',
    responsableRol: 'asistente_compras',
    icono: 'CheckCheck',
    checklistsBase: [
      'Verificación documental de la codificación aprobada',
      'Aprobación formal para compras y fabricación en China',
    ],
    documentosRequeridos: [
      { tipoDoc: 'Acta de Revisión y Aprobación', obligatorio: false },
    ],
  },
  {
    numero: 5,
    id: 'b-etapa-5',
    nombre: 'Análisis y gestión',
    fase: 'China (Gestión y Fabricación)',
    descripcion: 'David Túlcan analiza en 3 días. Los errores vuelven a la etapa 3 (Codificación). Los productos nuevos no codificados vuelven a etapa 3 con urgencia (4 h, 12 h o 24 h).',
    plazoDiasHabiles: 3,
    responsableTexto: 'David Túlcan (Compras China)',
    responsableRol: 'compras_china',
    icono: 'Search',
    checklistsBase: [
      'Análisis final de especificaciones con fábrica',
      'Confirmación de insumos y tiempos de fabricación',
    ],
    documentosRequeridos: [
      { tipoDoc: 'Informe de Factibilidad y Cotización Fábrica', obligatorio: false },
    ],
  },
  {
    numero: 6,
    id: 'b-etapa-6',
    nombre: 'Fabricación',
    fase: 'China (Gestión y Fabricación)',
    descripcion: 'David coordina la fabricación. Check al terminar con observaciones.',
    plazoDiasHabiles: 39,
    responsableTexto: 'David Túlcan (Compras China)',
    responsableRol: 'compras_china',
    icono: 'Factory',
    checklistsBase: [
      'Orden de fabricación iniciada con el proveedor',
      'Control de calidad intermedio',
      'Fabricación terminada',
    ],
    documentosRequeridos: [
      { tipoDoc: 'Reporte de Inspección de Calidad QC', obligatorio: false },
    ],
  },
  {
    numero: 7,
    id: 'b-etapa-7',
    nombre: 'Embarque',
    fase: 'China (Gestión y Fabricación)',
    descripcion: 'David tiene 10 días para buscar navieras y embarcar. Packing List y BL.',
    plazoDiasHabiles: 10,
    responsableTexto: 'David Túlcan (Compras China)',
    responsableRol: 'compras_china',
    icono: 'Ship',
    checklistsBase: [
      'Reserva de naviera y consolidación',
      'Subir Packing List',
      'Subir Bill of Lading (BL)',
    ],
    documentosRequeridos: [
      { tipoDoc: 'Packing List de Embarque', obligatorio: true },
      { tipoDoc: 'Bill of Lading (BL) Marítimo', obligatorio: true },
    ],
  },
  {
    numero: 8,
    id: 'b-etapa-8',
    nombre: 'Aduana Ecuador o Colombia',
    fase: 'Tránsito y Llegada',
    descripcion: 'David da check al llegar. Si se demora, sube observación y recálculo.',
    plazoDiasHabiles: 45,
    responsableTexto: 'David Túlcan (Compras China)',
    responsableRol: 'compras_china',
    icono: 'Anchor',
    checklistsBase: [
      'Tránsito altamar y arribo a puerto',
      'Inspección aduanera',
    ],
    documentosRequeridos: [
      { tipoDoc: 'Documento de Arribo / Manifiesto', obligatorio: false },
    ],
  },
  {
    numero: 9,
    id: 'b-etapa-9',
    nombre: 'Autorización de salida',
    fase: 'Tránsito y Llegada',
    descripcion: 'Anderson Enriquez autoriza la salida de aduana con check. Plazo: 24 h.',
    plazoHorasHabiles: 24,
    plazoDiasHabiles: 1,
    responsableTexto: 'Anderson Enriquez (Logística)',
    responsableRol: 'logistica',
    icono: 'ShieldCheck',
    checklistsBase: [
      'Pago de aranceles y liquidación SENAE',
      'Levante aduanero concedido',
    ],
    documentosRequeridos: [
      { tipoDoc: 'Levante Aduanero', obligatorio: true },
    ],
  },
  {
    numero: 10,
    id: 'b-etapa-10',
    nombre: 'Bodega',
    fase: 'Cierre y Bodega',
    descripcion: 'Anderson Enriquez elige la bodega de destino, da check y sube documentos. Plazo: 24 h.',
    plazoHorasHabiles: 24,
    plazoDiasHabiles: 1,
    responsableTexto: 'Anderson Enriquez (Logística)',
    responsableRol: 'logistica',
    icono: 'Warehouse',
    checklistsBase: [
      'Selección de bodega',
      'Recepción física y conteo',
    ],
    documentosRequeridos: [
      { tipoDoc: 'Acta de Ingreso en Bodega', obligatorio: true },
    ],
  },
  {
    numero: 11,
    id: 'b-etapa-11',
    nombre: 'Incidencias',
    fase: 'Cierre y Bodega',
    descripcion: 'Anderson Enriquez sube todos los documentos de cierre y da check.',
    plazoHorasHabiles: 24,
    plazoDiasHabiles: 1,
    responsableTexto: 'Anderson Enriquez (Logística)',
    responsableRol: 'logistica',
    icono: 'FileCheck',
    checklistsBase: [
      'Subir todos los documentos de cierre',
      'Reportar incidencias si existen',
    ],
    documentosRequeridos: [
      { tipoDoc: 'Expediente Completo de Importación', obligatorio: true },
    ],
  },
  {
    numero: 12,
    id: 'b-etapa-12',
    nombre: 'Fin',
    fase: 'Cierre y Bodega',
    descripcion: 'Fin automático: se marca sola al dar check en la etapa 11.',
    plazoDiasHabiles: 0,
    esFinAutomatico: true,
    responsableTexto: 'Sistema (Automático)',
    responsableRol: 'admin',
    icono: 'Flag',
    checklistsBase: [
      'Pedido completado y cerrado',
    ],
    documentosRequeridos: [],
  },
];

// Helper para construir etapas encadenadas
export function buildStagesForOrder(
  flujo: 'A' | 'B',
  fechaPedido: string,
  currentStageNum: number,
  overrides?: {
    [stageNum: number]: {
      fechaRealFin?: string;
      completadoPor?: string;
      completadoPorRol?: string;
      checklistCheckIds?: string[];
      documentos?: DocumentoEtapa[];
      motivoRetraso?: string;
    };
  }
): EtapaInstancia[] {
  const defs = flujo === 'A' ? ETAPAS_FLUJO_A : ETAPAS_FLUJO_B;
  const result: EtapaInstancia[] = [];
  let currentStart = fechaPedido;

  for (let i = 0; i < defs.length; i++) {
    const def = defs[i];
    const stageNum = def.numero;
    const ov = overrides ? overrides[stageNum] : undefined;
    const isCompleted = stageNum < currentStageNum || (ov !== undefined && ov.fechaRealFin !== undefined);
    const daysToAdd = def.plazoDiasHabiles !== undefined ? def.plazoDiasHabiles : 1;
    const estEnd = addBusinessDays(currentStart, daysToAdd);
    const realEnd = ov?.fechaRealFin;

    const checkedIds = ov?.checklistCheckIds || [];
    const checklist = def.checklistsBase.map((txt, idx) => {
      const chkId = `chk-${stageNum}-${idx + 1}`;
      const isDone = isCompleted || checkedIds.includes(chkId);
      return {
        id: chkId,
        texto: txt,
        completado: isDone,
        completadoPor: isDone ? (ov?.completadoPor || def.responsableTexto) : undefined,
        fecha: isDone ? (realEnd || estEnd) : undefined,
      };
    });

    const docs: DocumentoEtapa[] = def.documentosRequeridos.map((req, docIdx) => {
      const hasUploaded = isCompleted || stageNum === 1;
      return {
        id: `doc-${stageNum}-${docIdx + 1}`,
        tipoDoc: req.tipoDoc,
        esObligatorio: req.obligatorio,
        archivoActual: hasUploaded
          ? {
              version: 1,
              nombre: `${req.tipoDoc.replace(/[\/\s]/g, '_')}_v1.pdf`,
              tamano: '12.4 MB',
              subidoPor: ov?.completadoPor || def.responsableTexto,
              subidoPorRol: def.responsableRol,
              fecha: realEnd || currentStart,
            }
          : undefined,
        historialVersiones: [],
      };
    });

    let plazoTexto = '';
    if (def.esFinAutomatico) {
      plazoTexto = 'Automático';
    } else if (def.plazoHorasHabiles) {
      plazoTexto = `${def.plazoHorasHabiles} h hábiles`;
    } else if (def.plazoDiasHabiles !== undefined) {
      plazoTexto = `${def.plazoDiasHabiles} ${def.plazoDiasHabiles === 1 ? 'día hábil' : 'días hábiles'}`;
    }

    result.push({
      numero: stageNum,
      nombre: def.nombre,
      plazoTexto,
      plazoHorasHabiles: def.plazoHorasHabiles,
      plazoDiasHabiles: def.plazoDiasHabiles,
      fechaInicioEstimada: currentStart,
      fechaFinEstimada: estEnd,
      fechaRealInicio: stageNum <= currentStageNum ? currentStart : undefined,
      fechaRealFin: realEnd,
      completada: isCompleted,
      completadoPor: isCompleted ? (ov?.completadoPor || def.responsableTexto) : undefined,
      completadoPorRol: isCompleted ? (ov?.completadoPorRol || def.responsableRol) : undefined,
      checklist,
      documentos: docs,
      observaciones: [],
      motivoRetraso: ov?.motivoRetraso,
    });

    // Encadenar fecha de inicio para la siguiente etapa
    currentStart = isCompleted && realEnd ? realEnd : estEnd;
  }

  return result;
}

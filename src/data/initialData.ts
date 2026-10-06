import {
  Usuario,
  EtapaDef,
  Pedido,
  EtapaInstancia,
  ProductoLineaPedido,
  SolicitudProductoNuevo,
  AlertaItem,
  StageConfig,
  DocumentoEtapa,
} from '../types';
import { addBusinessDays } from '../utils/dateUtils';

export const DEMO_PASSWORD = 'Demo2026';

export const USUARIOS_DEMO: Usuario[] = [
  {
    id: 'usr-1',
    nombre: 'Mirian Franco',
    email: 'mfranco@decokasa.ec',
    cargo: 'Líder de Sistemas & Procesos',
    area: 'Sistemas',
    pais: 'Ecuador',
    rol: 'admin',
    avatar: 'MF',
    activo: true,
    etapasAsignadas: [1, 2, 3, 4, 5, 6, 7, 8, 9, 10, 11],
  },
  {
    id: 'usr-2',
    nombre: 'Juan Carlos Tulcán',
    email: 'jctulcan@decokasa.ec',
    cargo: 'Socio · Responsable de Compras',
    area: 'Junta de Socios',
    pais: 'Ecuador',
    rol: 'junta',
    avatar: 'JC',
    activo: true,
    etapasAsignadas: [3],
  },
  {
    id: 'usr-3',
    nombre: 'Daniel Tulcán',
    email: 'dftulcan@decokasa.ec',
    cargo: 'Socio Directivo',
    area: 'Junta de Socios',
    pais: 'Ecuador',
    rol: 'junta',
    avatar: 'DT',
    activo: true,
    etapasAsignadas: [3],
  },
  {
    id: 'usr-4',
    nombre: 'David Tulcán',
    email: 'cdtulcan@decokasa.ec',
    cargo: 'Gestión China · Latinoamérica World',
    area: 'Compras Internacionales (China)',
    pais: 'China',
    rol: 'editor_china',
    avatar: 'DT',
    activo: true,
    etapasAsignadas: [2, 4, 5, 7, 8],
  },
  {
    id: 'usr-5',
    nombre: 'Andrea Collaguazo',
    email: 'acollaguazo@decokasa.ec',
    cargo: 'Asistente de Compras China',
    area: 'Compras Internacionales (China)',
    pais: 'China',
    rol: 'editor_china',
    avatar: 'AC',
    activo: true,
    etapasAsignadas: [2, 4, 5, 7, 8],
  },
  {
    id: 'usr-6',
    nombre: 'Andrea Quishpe',
    email: 'aquishpe@decokasa.ec',
    cargo: 'Coordinadora de Marketing & Catálogo',
    area: 'Marketing / Sistemas',
    pais: 'Ecuador',
    rol: 'editor_marketing',
    avatar: 'AQ',
    activo: true,
    etapasAsignadas: [1],
  },
  {
    id: 'usr-7',
    nombre: 'Anderson Enriquez',
    email: 'aenriquez@decokasa.ec',
    cargo: 'Jefe de Logística Ecuador',
    area: 'Logística Ecuador importaciones',
    pais: 'Ecuador',
    rol: 'editor_logistica',
    avatar: 'AE',
    activo: true,
    etapasAsignadas: [6, 9, 10, 11],
  },
  {
    id: 'usr-8',
    nombre: 'Marcela Catucuamba',
    email: 'mcatucuamba@decokasa.ec',
    cargo: 'Coordinadora de Compras Ecuador',
    area: 'Departamento de Compras',
    pais: 'Ecuador',
    rol: 'editor_compras_ec',
    avatar: 'MC',
    activo: true,
    etapasAsignadas: [11],
  },
  {
    id: 'usr-9',
    nombre: 'Invitado Auditor',
    email: 'consulta@decokasa.ec',
    cargo: 'Auditor Externo',
    area: 'Consulta',
    pais: 'Ecuador',
    rol: 'consulta',
    avatar: 'IN',
    activo: true,
    etapasAsignadas: [],
  },
];

export const ETAPAS_BASE_CONFIG: EtapaDef[] = [
  {
    numero: 1,
    id: 'etapa-1',
    nombre: 'Solicitud y creación',
    grupo: 'Pedido',
    explicacion: 'Marketing / Sistemas crea el requerimiento con la proforma formal y el paquete de etiquetas de empaque.',
    plazoDiasHabiles: 0,
    responsableTexto: 'Andrea Quishpe (Marketing / Sistemas)',
    suplenteTexto: 'Mirian Franco (Sistemas)',
    rolesAutorizados: ['admin', 'editor_marketing'],
    icono: 'FileText',
    checklistsBase: [
      'Proforma o Excel con detalle de cantidades y colores cargado',
      'Archivo comprimido con etiquetas de código de barras verificado',
      'Bodega de destino seleccionada y confirmada',
    ],
    documentosRequeridos: [
      { tipoDoc: 'Excel del pedido / proforma', obligatorio: true },
      { tipoDoc: 'Zip de etiquetas', obligatorio: true },
    ],
  },
  {
    numero: 2,
    id: 'etapa-2',
    nombre: 'Revisión y check list',
    grupo: 'Pedido',
    explicacion: 'David Tulcán en China verifica fichas técnicas, precios de fábrica y compatibilidad de productos.',
    plazoDiasHabiles: 3,
    responsableTexto: 'David Tulcán (Compras China)',
    suplenteTexto: 'Andrea Collaguazo (Asistente China)',
    rolesAutorizados: ['admin', 'editor_china'],
    icono: 'ClipboardCheck',
    checklistsBase: [
      'El pedido cumple con el formato establecido por Decokasa',
      'Las fechas del cronograma son correctas y viables con la fábrica',
      'Revisadas las observaciones de Compras China y especificaciones',
      'La información cargada es correcta y completa',
      'Etiquetas, productos y fechas de vencimiento completos',
    ],
    documentosRequeridos: [
      { tipoDoc: 'Checklist técnico de fábrica', obligatorio: false },
      { tipoDoc: 'Ficha técnica de producto', obligatorio: false },
    ],
  },
  {
    numero: 3,
    id: 'etapa-3',
    nombre: 'Aprobación de la Junta',
    grupo: 'Pedido',
    explicacion: 'Los socios aprueban la inversión financiera y autorizan formalmente el desembolso a la fábrica.',
    plazoDiasHabiles: 2,
    responsableTexto: 'Junta de Socios (Juan Carlos & Daniel Tulcán)',
    suplenteTexto: 'Mirian Franco (Administrador)',
    rolesAutorizados: ['admin', 'junta'],
    icono: 'Users',
    checklistsBase: [
      'Presupuesto y condiciones de pago aprobados',
      'Margen de comercialización ratificado',
      'Autorización formal de orden de producción a fábrica',
    ],
    documentosRequeridos: [
      { tipoDoc: 'Acta de Aprobación de Socios', obligatorio: false },
    ],
  },
  {
    numero: 4,
    id: 'etapa-4',
    nombre: 'Fabricación',
    grupo: 'Producción en China',
    explicacion: 'Producción en planta industrial en China (35 días hábiles, ampliable máximo +10 días con motivo).',
    plazoDiasHabiles: 35,
    responsableTexto: 'David Tulcán (Compras China)',
    suplenteTexto: 'Andrea Collaguazo (Asistente China)',
    rolesAutorizados: ['admin', 'editor_china'],
    icono: 'Factory',
    checklistsBase: [
      'Anticipo recibido por el fabricante en China',
      'Inicio de línea de producción confirmado',
      'Inspección de calidad intermedia en planta',
      'Lote terminado conforme a especificaciones',
    ],
    documentosRequeridos: [
      { tipoDoc: 'Fotos de inspección en fábrica', obligatorio: false },
    ],
  },
  {
    numero: 5,
    id: 'etapa-5',
    nombre: 'Aprobación de embarque y Packing List',
    grupo: 'Producción en China',
    explicacion: 'Validación de cubicaje, peso, embalaje final y emisión del Packing List definitivo con la Commercial Invoice.',
    plazoDiasHabiles: 10,
    responsableTexto: 'David Tulcán (Compras China)',
    suplenteTexto: 'Andrea Collaguazo (Asistente China)',
    rolesAutorizados: ['admin', 'editor_china'],
    icono: 'PackageCheck',
    checklistsBase: [
      'Packing List final subido y cotejado',
      'Commercial Invoice final subida con montos exactos',
      'Fecha y booking de embarque confirmados con la naviera',
    ],
    documentosRequeridos: [
      { tipoDoc: 'Packing List final', obligatorio: true },
      { tipoDoc: 'Commercial Invoice final', obligatorio: true },
    ],
  },
  {
    numero: 6,
    id: 'etapa-6',
    nombre: 'Reporte de mercadería',
    grupo: 'Documentos',
    explicacion: 'Anderson Enriquez en Ecuador verifica el desglose de ítems, bultos y códigos enviado desde China.',
    plazoDiasHabiles: 5,
    responsableTexto: 'Anderson Enriquez (Logística Ecuador)',
    suplenteTexto: 'Mirian Franco (Sistemas)',
    rolesAutorizados: ['admin', 'editor_logistica'],
    icono: 'FileSpreadsheet',
    checklistsBase: [
      'Reporte de mercadería en Excel subido por China',
      'Anderson Enriquez verificó congruencia de bultos y pesos',
      'Códigos arancelarios y descripciones comerciales validados',
    ],
    documentosRequeridos: [
      { tipoDoc: 'Reporte de mercadería en Excel', obligatorio: true },
    ],
  },
  {
    numero: 7,
    id: 'etapa-7',
    nombre: 'BL y contenedores',
    grupo: 'Documentos',
    explicacion: 'Emisión del Bill of Lading marítimo y asignación de números de contenedor y precinto.',
    plazoDiasHabiles: 5,
    responsableTexto: 'David Tulcán (Compras China)',
    suplenteTexto: 'Andrea Collaguazo (Asistente China)',
    rolesAutorizados: ['admin', 'editor_china'],
    icono: 'FileSignature',
    checklistsBase: [
      'Número de contenedor y sello/precinto registrados',
      'BL original o Express Release emitido por la naviera',
      'Documentos transmitidos para trámites aduaneros',
    ],
    documentosRequeridos: [
      { tipoDoc: 'Bill of Lading (BL en PDF)', obligatorio: true },
      { tipoDoc: 'Certificado de origen', obligatorio: false },
    ],
  },
  {
    numero: 8,
    id: 'etapa-8',
    nombre: 'Viaje marítimo',
    grupo: 'Tránsito',
    explicacion: 'Travesía transpacífica desde puertos de China (Ningbo, Shanghai, Shenzhen) hasta Guayaquil (Posorja/Contecon).',
    plazoDiasHabiles: 40,
    responsableTexto: 'David Tulcán (Compras China)',
    suplenteTexto: 'Anderson Enriquez (Logística Ecuador)',
    rolesAutorizados: ['admin', 'editor_china'],
    icono: 'Ship',
    checklistsBase: [
      'Zarpe efectivo confirmado en puerto de origen',
      'Seguimiento satelital de motonave en tránsito',
      'Aviso de llegada (Arrival Notice) recibido del agente de carga',
    ],
    documentosRequeridos: [
      { tipoDoc: 'Itinerario naviera y aviso de arribo', obligatorio: false },
    ],
  },
  {
    numero: 9,
    id: 'etapa-9',
    nombre: 'Desaduanización',
    grupo: 'Llegada a Ecuador',
    explicacion: 'Nacionalización ante el SENAE en Ecuador. Subestados: En trámite / Retenido / Liberado.',
    plazoDiasHabiles: 15,
    responsableTexto: 'Anderson Enriquez (Logística Ecuador)',
    suplenteTexto: 'Mirian Franco (Sistemas)',
    rolesAutorizados: ['admin', 'editor_logistica'],
    icono: 'ShieldAlert',
    checklistsBase: [
      'Transmisión de DAU (Declaración Aduanera de Importación)',
      'Pago de tributos arancelarios efectuado',
      'Canal de aforo asignado (Automático / Documental / Físico)',
      'Mercancía con salida autorizada de patio portuario',
    ],
    documentosRequeridos: [
      { tipoDoc: 'DAU / Liquidación Aduanera SENAE', obligatorio: false },
    ],
  },
  {
    numero: 10,
    id: 'etapa-10',
    nombre: 'Bodega',
    grupo: 'Llegada a Ecuador',
    explicacion: 'Recepción física y descarga en Bodega (Chongón u otra), conteo por ítem y reporte de incidencias.',
    plazoDiasHabiles: 5,
    responsableTexto: 'Anderson Enriquez (Logística Ecuador)',
    suplenteTexto: 'Mirian Franco (Sistemas)',
    rolesAutorizados: ['admin', 'editor_logistica'],
    icono: 'Warehouse',
    checklistsBase: [
      'Mercadería verificada físicamente contra el Packing List',
      'Cantidades recibidas registradas por línea de producto',
      'Incidencias registradas con fotografías (si existiesen)',
      'Acta de entrega-recepción de bodega firmada',
    ],
    documentosRequeridos: [
      { tipoDoc: 'Acta de recepción de bodega', obligatorio: false },
      { tipoDoc: 'Informe de incidencias con fotos', obligatorio: false },
    ],
  },
  {
    numero: 11,
    id: 'etapa-11',
    nombre: 'Cierre documental',
    grupo: 'Llegada a Ecuador',
    explicacion: 'Auditoría de carpeta completa (Proforma, Packing List, Invoice, BL, Reporte). Compras Ecuador da el check final.',
    plazoDiasHabiles: 5,
    responsableTexto: 'Marcela Catucuamba & Anderson Enriquez',
    suplenteTexto: 'Mirian Franco (Administrador)',
    rolesAutorizados: ['admin', 'editor_compras_ec', 'editor_logistica'],
    icono: 'Archive',
    checklistsBase: [
      'Carpeta física y digital con Proforma, Invoice, Packing List y BL',
      'Liquidación de costos y fletes archivada',
      'Validación final de Compras Ecuador para marcar como Finalizado',
    ],
    documentosRequeridos: [
      { tipoDoc: 'Carpeta de liquidación contable final', obligatorio: false },
    ],
  },
];

export interface StageOverride {
  fechaRealFin: string;
  completadoPor: string;
  completadoPorRol?: string;
  observaciones?: string;
  checklistCheckIds?: string[];
  documentos?: DocumentoEtapa[];
}

/**
 * Función que encadena las 11 etapas calculando plazos en días hábiles
 */
export function buildChainedStages(
  fechaPedido: string,
  esProductoNuevo: boolean,
  ampliacionFabricacion: number,
  completedOverrides: { [key: number]: StageOverride },
  currentStageNum: number,
  subestadoAduana?: 'En trámite' | 'Retenido' | 'Liberado'
): EtapaInstancia[] {
  const result: EtapaInstancia[] = [];
  let currentStart = fechaPedido;

  for (let i = 1; i <= 11; i++) {
    const def = ETAPAS_BASE_CONFIG[i - 1];
    let extraDays = 0;
    if (i === 2 && esProductoNuevo) {
      extraDays = 10;
    } else if (i === 4) {
      extraDays = ampliacionFabricacion;
    }

    const totalDays = def.plazoDiasHabiles + extraDays;
    const estEnd = addBusinessDays(currentStart, totalDays);
    const override: StageOverride | undefined = completedOverrides[i];
    const isCompleted = i < currentStageNum || (override !== undefined && override.fechaRealFin !== undefined);
    const realEnd = override?.fechaRealFin;

    const checkedIds = override?.checklistCheckIds || [];
    const overrideDocs = override?.documentos || [];

    // Checklists
    const checklist = def.checklistsBase.map((txt, idx) => {
      const chkId = `chk-${i}-${idx + 1}`;
      const isChecked = isCompleted || checkedIds.includes(chkId);
      return {
        id: chkId,
        texto: txt,
        completado: isChecked,
        completadoPor: isChecked ? (override?.completadoPor || 'Equipo Decokasa') : undefined,
        fecha: isChecked ? (realEnd || estEnd) : undefined,
      };
    });

    // Documentos requeridos base
    const documentos: DocumentoEtapa[] = def.documentosRequeridos.map((req, docIdx) => {
      const hasUploaded =
        isCompleted ||
        i === 1 ||
        overrideDocs.some((d: DocumentoEtapa) => d.tipoDoc === req.tipoDoc);
      return {
        id: `doc-${i}-${docIdx + 1}`,
        tipoDoc: req.tipoDoc,
        esObligatorio: req.obligatorio,
        archivoActual: hasUploaded
          ? {
              version: 1,
              nombre: `${req.tipoDoc.replace(/[\/\s]/g, '_')}_v1.${req.tipoDoc.includes('Zip') ? 'zip' : req.tipoDoc.includes('Excel') || req.tipoDoc.includes('Packing') ? 'xlsx' : 'pdf'}`,
              tamano: '1.4 MB',
              subidoPor: override?.completadoPor || 'Mirian Franco',
              subidoPorRol: 'Sistemas',
              fecha: realEnd || currentStart,
            }
          : undefined,
        historialVersiones: [],
      };
    });


    const stageInstance: EtapaInstancia = {
      numero: i,
      nombre: def.nombre,
      plazoDiasHabiles: def.plazoDiasHabiles,
      plazoExtraDias: extraDays,
      fechaInicioEstimada: currentStart,
      fechaFinEstimada: estEnd,
      fechaRealInicio: currentStart,
      fechaRealFin: realEnd,
      completada: isCompleted,
      completadoPor: override?.completadoPor,
      completadoPorRol: override?.completadoPorRol,
      checklist,
      documentos: override?.documentos || documentos,
      observaciones: override?.observaciones ? [
        {
          id: `obs-${i}-1`,
          usuarioId: 'usr-1',
          usuarioNombre: override.completadoPor || 'Equipo Decokasa',
          usuarioRol: override.completadoPorRol || 'Sistemas',
          avatar: 'DK',
          fecha: `${realEnd || currentStart} 11:30`,
          texto: override.observaciones,
        }
      ] : [],
      subestadoAduana: i === 9 ? subestadoAduana : undefined,
    };

    result.push(stageInstance);
    currentStart = realEnd || estEnd;
  }

  return result;
}

// ==========================================
// LÍNEAS DE PRODUCTO DEL PEDIDO IMP-2026-006 (Total: 6.903 unidades)
// ==========================================
export const PRODUCTOS_IMP_2026_006: ProductoLineaPedido[] = [
  {
    id: 'prod-006-1',
    codigoDecokasa: 'EXT10',
    codigoChino: 'EXT 01 /10',
    descripcion: 'Panel ranurado exterior WPC',
    medida: '219 x 26 x 2900 mm',
    color: 'Teak',
    cantidadPedida: 640,
    cantidadRecibida: 0,
    diferencia: 0,
    estado: 'Pendiente',
  },
  {
    id: 'prod-006-2',
    codigoDecokasa: 'ANG-EXT10',
    codigoChino: 'EXT 02 /10',
    descripcion: 'Ángulo remate panel exterior WPC (30%)',
    medida: '40 x 50 x 2900 mm',
    color: 'Único Teak',
    cantidadPedida: 192,
    cantidadRecibida: 0,
    diferencia: 0,
    estado: 'Pendiente',
  },
  {
    id: 'prod-006-3',
    codigoDecokasa: 'EXT20',
    codigoChino: 'EXT 01 /20',
    descripcion: 'Panel ranurado exterior WPC',
    medida: '219 x 26 x 2900 mm',
    color: 'Según catálogo (Nogal)',
    cantidadPedida: 880,
    cantidadRecibida: 0,
    diferencia: 0,
    estado: 'Pendiente',
  },
  {
    id: 'prod-006-4',
    codigoDecokasa: 'ANG-EXT20',
    codigoChino: 'EXT 02 /20',
    descripcion: 'Ángulo remate panel exterior WPC (30%)',
    medida: '40 x 50 x 2900 mm',
    color: 'Único Nogal',
    cantidadPedida: 264,
    cantidadRecibida: 0,
    diferencia: 0,
    estado: 'Pendiente',
  },
  {
    id: 'prod-006-5',
    codigoDecokasa: 'EXT30',
    codigoChino: 'EXT 01 /30',
    descripcion: 'Panel ranurado exterior WPC',
    medida: '219 x 26 x 2900 mm',
    color: 'Según catálogo (Gris humo)',
    cantidadPedida: 640,
    cantidadRecibida: 0,
    diferencia: 0,
    estado: 'Pendiente',
  },
  {
    id: 'prod-006-6',
    codigoDecokasa: 'ANG-EXT30',
    codigoChino: 'EXT 02 /30',
    descripcion: 'Ángulo remate panel exterior WPC (30%)',
    medida: '40 x 50 x 2900 mm',
    color: 'Único Gris humo',
    cantidadPedida: 192,
    cantidadRecibida: 0,
    diferencia: 0,
    estado: 'Pendiente',
  },
  {
    id: 'prod-006-7',
    codigoDecokasa: 'EXT40',
    codigoChino: 'EXT 01 /40',
    descripcion: 'Panel ranurado exterior WPC',
    medida: '219 x 26 x 2900 mm',
    color: 'Según catálogo (Roble rústico)',
    cantidadPedida: 350,
    cantidadRecibida: 0,
    diferencia: 0,
    estado: 'Pendiente',
  },
  {
    id: 'prod-006-8',
    codigoDecokasa: 'ANG-EXT40',
    codigoChino: 'EXT 02 /40',
    descripcion: 'Ángulo remate panel exterior WPC (30%)',
    medida: '40 x 50 x 2900 mm',
    color: 'Único Roble',
    cantidadPedida: 105,
    cantidadRecibida: 0,
    diferencia: 0,
    estado: 'Pendiente',
  },
  {
    id: 'prod-006-9',
    codigoDecokasa: 'EXT50',
    codigoChino: 'EXT 01 /50',
    descripcion: 'Panel ranurado exterior WPC',
    medida: '219 x 26 x 2900 mm',
    color: 'Según catálogo (Chocolate)',
    cantidadPedida: 880,
    cantidadRecibida: 0,
    diferencia: 0,
    estado: 'Pendiente',
  },
  {
    id: 'prod-006-10',
    codigoDecokasa: 'ANG-EXT50',
    codigoChino: 'EXT 02 /50',
    descripcion: 'Ángulo remate panel exterior WPC (30%)',
    medida: '40 x 50 x 2900 mm',
    color: 'Único Chocolate',
    cantidadPedida: 264,
    cantidadRecibida: 0,
    diferencia: 0,
    estado: 'Pendiente',
  },
  {
    id: 'prod-006-11',
    codigoDecokasa: 'EXT60',
    codigoChino: 'EXT60',
    descripcion: 'Panel ranurado exterior WPC',
    medida: '219 x 26 x 2900 mm',
    color: 'Según catálogo (Antracita)',
    cantidadPedida: 640,
    cantidadRecibida: 0,
    diferencia: 0,
    estado: 'Pendiente',
  },
  {
    id: 'prod-006-12',
    codigoDecokasa: 'ANG-EXT60',
    codigoChino: 'ANG-EXT60',
    descripcion: 'Ángulo remate panel exterior WPC (30%)',
    medida: '40 x 50 x 2900 mm',
    color: 'Único Antracita',
    cantidadPedida: 192,
    cantidadRecibida: 0,
    diferencia: 0,
    estado: 'Pendiente',
  },
  {
    id: 'prod-006-13',
    codigoDecokasa: 'EXT70',
    codigoChino: 'EXT70',
    descripcion: 'Panel ranurado exterior WPC',
    medida: '219 x 26 x 2900 mm',
    color: 'Según catálogo (Caoba)',
    cantidadPedida: 640,
    cantidadRecibida: 0,
    diferencia: 0,
    estado: 'Pendiente',
  },
  {
    id: 'prod-006-14',
    codigoDecokasa: 'ANG-EXT70',
    codigoChino: 'ANG-EXT70',
    descripcion: 'Ángulo remate panel exterior WPC (30%)',
    medida: '40 x 50 x 2900 mm',
    color: 'Único Caoba',
    cantidadPedida: 192,
    cantidadRecibida: 0,
    diferencia: 0,
    estado: 'Pendiente',
  },
  {
    id: 'prod-006-15',
    codigoDecokasa: 'EXT80',
    codigoChino: 'EXT80',
    descripcion: 'Panel ranurado exterior WPC',
    medida: '219 x 26 x 2900 mm',
    color: 'Según catálogo (Fresno silver)',
    cantidadPedida: 640,
    cantidadRecibida: 0,
    diferencia: 0,
    estado: 'Pendiente',
  },
  {
    id: 'prod-006-16',
    codigoDecokasa: 'ANG-EXT80',
    codigoChino: 'ANG-EXT80',
    descripcion: 'Ángulo remate panel exterior WPC (30%)',
    medida: '40 x 50 x 2900 mm',
    color: 'Único Fresno',
    cantidadPedida: 192,
    cantidadRecibida: 0,
    diferencia: 0,
    estado: 'Pendiente',
  },
];

// ==========================================
// LISTADO EXACTO DE PEDIDOS DE EJEMPLO
// ==========================================
export const PEDIDOS_INICIALES: Pedido[] = [
  // 1. PEDIDO PRINCIPAL: IMP-2026-006-PANEL-EXT-WPC
  {
    id: 'ped-main-006',
    codigo: 'IMP-2026-006-PANEL-EXT-WPC',
    nombre: 'Panel ranurado exterior WPC — Productos de Alta Rotación',
    descripcion: 'Cargamento industrial de 6.903 piezas: 5.310 paneles exteriores y 1.593 ángulos complementarios en 2 contenedores 40HQ.',
    proveedor: 'Zhejiang Forest Eco Building Tech Co., Ltd. (Haining)',
    puertoOrigen: 'Ningbo-Zhoushan, China',
    puertoDestino: 'Guayaquil (DP World Posorja), Ecuador',
    contenedor: 'CMAU-982104-5 (40HQ) & MSKU-481902-1 (40HQ)',
    blNumero: 'Por emitir en etapa 7',
    fechaPedido: '2026-08-20',
    responsablePedido: 'Juan Carlos Tulcán (Compras)',
    gestionChina: 'David Tulcán (Latinoamérica World)',
    elaboradoPor: 'Mirian Franco (Compras & Marketing)',
    version: 'v1',
    esProductoNuevo: false,
    ampliacionFabricacionDias: 0,
    etapaActualNumero: 4, // En fabricación, a tiempo
    estado: 'En fabricación',
    bodegaDestino: 'Chongón',
    productos: PRODUCTOS_IMP_2026_006,
    etapas: buildChainedStages(
      '2026-08-20',
      false,
      0,
      {
        1: {
          fechaRealFin: '2026-08-20',
          completadoPor: 'Mirian Franco',
          completadoPorRol: 'Sistemas / Marketing',
          observaciones: 'Se recomienda venir empacado con plástico para evitar rayaduras.',
          documentos: [
            {
              id: 'doc-1-1',
              tipoDoc: 'Excel del pedido / proforma',
              esObligatorio: true,
              archivoActual: {
                version: 1,
                nombre: 'Orden_Pedido_Panel_Exterior_WPC.xlsx',
                tamano: '2.4 MB',
                subidoPor: 'Mirian Franco',
                subidoPorRol: 'Marketing',
                fecha: '2026-08-20',
              },
              historialVersiones: [],
            },
            {
              id: 'doc-1-2',
              tipoDoc: 'Zip de etiquetas',
              esObligatorio: true,
              archivoActual: {
                version: 1,
                nombre: 'etiquetas-codigos-barras-EXTERIOR.zip',
                tamano: '14.8 MB',
                subidoPor: 'Mirian Franco',
                subidoPorRol: 'Marketing',
                fecha: '2026-08-20',
              },
              historialVersiones: [],
            },
          ],
        },
        2: {
          fechaRealFin: '2026-08-25',
          completadoPor: 'David Tulcán',
          completadoPorRol: 'Editor Compras China',
          observaciones: 'Fichas técnicas y colores de paneles y ángulos validados con fábrica. Se aprueba formato.',
        },
        3: {
          fechaRealFin: '2026-08-27',
          completadoPor: 'Juan Carlos Tulcán',
          completadoPorRol: 'Junta de Socios',
          observaciones: 'Aprobación de orden de compra y autorización de anticipo otorgada por la Junta.',
        },
      },
      4
    ),
    historial: [
      {
        id: 'hist-006-1',
        fecha: '20/08/2026 09:15',
        usuarioId: 'usr-1',
        usuarioNombre: 'Mirian Franco',
        usuarioRol: 'Sistemas',
        pedidoCodigo: 'IMP-2026-006-PANEL-EXT-WPC',
        etapaNumero: 1,
        etapaNombre: 'Solicitud y creación',
        accion: 'Creó el pedido formal de importación',
        motivo: 'Reposición alta rotación y nuevo formato de ángulos',
        tipo: 'check',
      },
      {
        id: 'hist-006-2',
        fecha: '25/08/2026 15:40',
        usuarioId: 'usr-4',
        usuarioNombre: 'David Tulcán',
        usuarioRol: 'Editor Compras China',
        pedidoCodigo: 'IMP-2026-006-PANEL-EXT-WPC',
        etapaNumero: 2,
        etapaNombre: 'Revisión y check list',
        accion: 'Aprobó revisión técnica con fabricante',
        motivo: 'Verificación de catálogo y códigos de barras aprobados',
        tipo: 'check',
      },
      {
        id: 'hist-006-3',
        fecha: '27/08/2026 11:10',
        usuarioId: 'usr-2',
        usuarioNombre: 'Juan Carlos Tulcán',
        usuarioRol: 'Junta de Socios',
        pedidoCodigo: 'IMP-2026-006-PANEL-EXT-WPC',
        etapaNumero: 3,
        etapaNombre: 'Aprobación de la Junta',
        accion: 'Aprobó paso a fabricación',
        motivo: 'Autorización unánime de socios para producción en planta',
        tipo: 'check',
      },
    ],
  },

  // 2. IMP-2026-001-PISO-SPC — en Bodega Chongón, a tiempo, con 1 incidencia ("2 cajas con esquinas golpeadas", con foto).
  {
    id: 'ped-001',
    codigo: 'IMP-2026-001-PISO-SPC',
    nombre: 'Piso SPC 5mm con Manta IXPE Acústica',
    descripcion: 'Contenedor 40HQ con 1.800 cajas de pisos SPC roble nórdico.',
    proveedor: 'Changzhou Elegant Floors Co., Ltd.',
    puertoOrigen: 'Ningbo, China',
    puertoDestino: 'Guayaquil (Posorja), Ecuador',
    contenedor: 'MSKU-782910-4 (40HQ)',
    blNumero: 'MAEU982104521',
    fechaPedido: '2026-03-02',
    responsablePedido: 'Marcela Catucuamba',
    gestionChina: 'David Tulcán',
    elaboradoPor: 'Mirian Franco',
    version: 'v1',
    esProductoNuevo: false,
    ampliacionFabricacionDias: 0,
    etapaActualNumero: 10,
    estado: 'En bodega',
    bodegaDestino: 'Chongón',
    productos: [
      {
        id: 'prod-001-1',
        codigoDecokasa: 'SPC-ROBLE-01',
        codigoChino: 'SPC-NORDIC-5MM',
        descripcion: 'Piso SPC 5mm Roble Nórdico',
        medida: '180 x 1220 x 5 mm',
        color: 'Roble Claro',
        cantidadPedida: 1800,
        cantidadRecibida: 1798,
        diferencia: -2,
        estado: 'Dañado',
        incidencias: '2 cajas con esquinas golpeadas durante la estiba en puerto',
        fotoIncidencia: 'cajas_esquinas_golpeadas_spc.jpg',
      },
    ],
    etapas: buildChainedStages(
      '2026-03-02',
      false,
      0,
      {
        1: { fechaRealFin: '2026-03-02', completadoPor: 'Marcela Catucuamba', observaciones: 'Creación del pedido' },
        2: { fechaRealFin: '2026-03-05', completadoPor: 'David Tulcán', observaciones: 'Checklist aprobado' },
        3: { fechaRealFin: '2026-03-09', completadoPor: 'Juan Carlos Tulcán', observaciones: 'Aprobación de la Junta' },
        4: { fechaRealFin: '2026-04-27', completadoPor: 'David Tulcán', observaciones: 'Fabricación finalizada a tiempo' },
        5: { fechaRealFin: '2026-05-11', completadoPor: 'David Tulcán', observaciones: 'Packing list auditado' },
        6: { fechaRealFin: '2026-05-18', completadoPor: 'Anderson Enriquez', observaciones: 'Reporte conforme' },
        7: { fechaRealFin: '2026-05-25', completadoPor: 'David Tulcán', observaciones: 'BL transmitido' },
        8: { fechaRealFin: '2026-07-20', completadoPor: 'David Tulcán', observaciones: 'Arribo a Guayaquil' },
        9: { fechaRealFin: '2026-08-10', completadoPor: 'Anderson Enriquez', observaciones: 'Canal verde liberado' },
      },
      10
    ),
    historial: [
      {
        id: 'h-001-1',
        fecha: '10/08/2026 14:00',
        usuarioId: 'usr-7',
        usuarioNombre: 'Anderson Enriquez',
        usuarioRol: 'Editor Logística',
        accion: 'Desaduanizó carga y autorizó ingreso a Bodega Chongón',
        motivo: 'Canal verde sin aforo físico',
        tipo: 'check',
      },
      {
        id: 'h-001-2',
        fecha: '18/08/2026 10:30',
        usuarioId: 'usr-7',
        usuarioNombre: 'Anderson Enriquez',
        usuarioRol: 'Editor Logística',
        accion: 'Registró incidencia en recepción de bodega: 2 cajas con esquinas golpeadas',
        motivo: 'Evidencia fotográfica adjunta para reclamo de seguro',
        tipo: 'observacion',
      },
    ],
  },

  // 3. IMP-2026-002-CORTINAS-ROLLER — Desaduanización, RETENIDO ("Aduana solicitó inspección física").
  {
    id: 'ped-002',
    codigo: 'IMP-2026-002-CORTINAS-ROLLER',
    nombre: 'Cortinas Roller Motorizadas Blackout & Sunscreen',
    descripcion: 'Contenedor 20GP con 950 sets completos de cortinas y accesorios inteligentes.',
    proveedor: 'Hangzhou Smart Sunshade Industry Co.',
    puertoOrigen: 'Shanghai, China',
    puertoDestino: 'Guayaquil (Contecon), Ecuador',
    contenedor: 'CMAU-419082-1 (20GP)',
    blNumero: 'CMACGM0829103',
    fechaPedido: '2026-04-06',
    responsablePedido: 'Marcela Catucuamba',
    gestionChina: 'David Tulcán',
    elaboradoPor: 'Mirian Franco',
    version: 'v1',
    esProductoNuevo: false,
    ampliacionFabricacionDias: 0,
    etapaActualNumero: 9,
    estado: 'En aduana',
    subestadoAduana: 'Retenido',
    bodegaDestino: 'Chongón',
    productos: [
      {
        id: 'p-002-1',
        codigoDecokasa: 'ROL-BO-120',
        codigoChino: 'BO-SMART-120',
        descripcion: 'Cortina Roller Blackout motorizada',
        medida: '1.20 x 2.20 m',
        color: 'Blanco perla',
        cantidadPedida: 500,
        estado: 'Pendiente',
      },
      {
        id: 'p-002-2',
        codigoDecokasa: 'ROL-SUN-150',
        codigoChino: 'SUN-SMART-150',
        descripcion: 'Cortina Roller Sunscreen 5%',
        medida: '1.50 x 2.20 m',
        color: 'Gris plata',
        cantidadPedida: 450,
        estado: 'Pendiente',
      },
    ],
    etapas: buildChainedStages(
      '2026-04-06',
      false,
      0,
      {
        1: { fechaRealFin: '2026-04-06', completadoPor: 'Marcela Catucuamba' },
        2: { fechaRealFin: '2026-04-09', completadoPor: 'David Tulcán' },
        3: { fechaRealFin: '2026-04-13', completadoPor: 'Juan Carlos Tulcán' },
        4: { fechaRealFin: '2026-06-01', completadoPor: 'David Tulcán' },
        5: { fechaRealFin: '2026-06-15', completadoPor: 'David Tulcán' },
        6: { fechaRealFin: '2026-06-22', completadoPor: 'Anderson Enriquez' },
        7: { fechaRealFin: '2026-06-29', completadoPor: 'David Tulcán' },
        8: { fechaRealFin: '2026-08-24', completadoPor: 'David Tulcán' },
      },
      9,
      'Retenido'
    ),
    historial: [
      {
        id: 'h-002-1',
        fecha: '02/09/2026 11:30',
        usuarioId: 'usr-7',
        usuarioNombre: 'Anderson Enriquez',
        usuarioRol: 'Editor Logística',
        accion: 'Marcó subestado aduanero como RETENIDO',
        motivo: 'Aduana solicitó inspección física',
        tipo: 'subestado',
      },
    ],
  },

  // 4. IMP-2026-003-PAPEL-TAPIZ — Viaje marítimo, RETRASADO 8 días hábiles.
  {
    id: 'ped-003',
    codigo: 'IMP-2026-003-PAPEL-TAPIZ',
    nombre: 'Papel Tapiz Vinílico Lavable Texturizado',
    descripcion: 'Contenedor 40HQ con 3.400 rollos de papel tapiz decorativo.',
    proveedor: 'Haining Elegant Wallpaper Ind.',
    puertoOrigen: 'Ningbo, China',
    puertoDestino: 'Guayaquil (Posorja), Ecuador',
    contenedor: 'COSU-928172-8 (40HQ)',
    blNumero: 'COSU63901928',
    fechaPedido: '2026-05-04',
    responsablePedido: 'Marcela Catucuamba',
    gestionChina: 'David Tulcán',
    elaboradoPor: 'Mirian Franco',
    version: 'v1',
    esProductoNuevo: false,
    ampliacionFabricacionDias: 0,
    etapaActualNumero: 8,
    estado: 'En tránsito',
    bodegaDestino: 'Chongón',
    productos: [
      {
        id: 'p-003-1',
        codigoDecokasa: 'TAP-VIN-GOLD',
        codigoChino: 'HN-GOLD-26',
        descripcion: 'Papel tapiz vinílico relieve dorado',
        medida: '0.53 x 10 m',
        color: 'Champagne texturizado',
        cantidadPedida: 3400,
        estado: 'Pendiente',
      },
    ],
    etapas: buildChainedStages(
      '2026-05-04',
      false,
      0,
      {
        1: { fechaRealFin: '2026-05-04', completadoPor: 'Marcela Catucuamba' },
        2: { fechaRealFin: '2026-05-07', completadoPor: 'David Tulcán' },
        3: { fechaRealFin: '2026-05-11', completadoPor: 'Juan Carlos Tulcán' },
        // La fabricación terminó 8 días hábiles tarde:
        4: { fechaRealFin: '2026-07-10', completadoPor: 'David Tulcán', observaciones: 'La fabricación terminó 8 días tarde por escasez de pigmentos en fábrica.' },
        5: { fechaRealFin: '2026-07-24', completadoPor: 'David Tulcán' },
        6: { fechaRealFin: '2026-07-31', completadoPor: 'Anderson Enriquez' },
        7: { fechaRealFin: '2026-08-07', completadoPor: 'David Tulcán' },
      },
      8
    ),
    historial: [
      {
        id: 'h-003-1',
        fecha: '10/07/2026 17:00',
        usuarioId: 'usr-4',
        usuarioNombre: 'David Tulcán',
        usuarioRol: 'Editor Compras China',
        accion: 'Completó fabricación con 8 días hábiles de retraso',
        motivo: 'Demora en calibración de tintas y materias primas',
        tipo: 'check',
      },
    ],
  },

  // 5. IMP-2026-004-PERSIANAS — BL y contenedores, POR VENCER (faltan 2 días hábiles), falta subir el BL.
  {
    id: 'ped-004',
    codigo: 'IMP-2026-004-PERSIANAS',
    nombre: 'Persianas Horizontales de Aluminio y Faux Wood',
    descripcion: 'Contenedor 40GP con 1.200 persianas a medida y perfiles de montaje.',
    proveedor: 'Guangdong Sunshade Industrial Ltd.',
    puertoOrigen: 'Shenzhen, China',
    puertoDestino: 'Guayaquil (Posorja), Ecuador',
    contenedor: 'ONEU-582910-3 (40GP)',
    blNumero: 'Pendiente de emisión por la naviera',
    fechaPedido: '2026-07-15',
    responsablePedido: 'Marcela Catucuamba',
    gestionChina: 'David Tulcán',
    elaboradoPor: 'Mirian Franco',
    version: 'v1',
    esProductoNuevo: false,
    ampliacionFabricacionDias: 0,
    etapaActualNumero: 7, // BL y contenedores, POR VENCER, falta subir BL
    estado: 'Pendiente de embarque',
    bodegaDestino: 'Quito',
    productos: [
      {
        id: 'p-004-1',
        codigoDecokasa: 'PER-ALUM-50',
        codigoChino: 'GD-AL-50',
        descripcion: 'Persiana de aluminio 50mm blanco mate',
        medida: '1.20 x 1.80 m',
        color: 'Blanco',
        cantidadPedida: 1200,
        estado: 'Pendiente',
      },
    ],
    etapas: buildChainedStages(
      '2026-07-15',
      false,
      0,
      {
        1: { fechaRealFin: '2026-07-15', completadoPor: 'Marcela Catucuamba' },
        2: { fechaRealFin: '2026-07-20', completadoPor: 'David Tulcán' },
        3: { fechaRealFin: '2026-07-22', completadoPor: 'Juan Carlos Tulcán' },
        4: { fechaRealFin: '2026-09-09', completadoPor: 'David Tulcán' },
        5: { fechaRealFin: '2026-09-23', completadoPor: 'David Tulcán' },
        6: { fechaRealFin: '2026-09-30', completadoPor: 'Anderson Enriquez' },
      },
      7
    ),
    historial: [
      {
        id: 'h-004-1',
        fecha: '30/09/2026 16:00',
        usuarioId: 'usr-7',
        usuarioNombre: 'Anderson Enriquez',
        usuarioRol: 'Editor Logística',
        accion: 'Aprobó reporte de mercadería',
        motivo: 'Verificación de pesos y dimensiones aprobada',
        tipo: 'check',
      },
    ],
  },

  // 6. IMP-2026-005-ALFOMBRAS — Fabricación con ampliación de +5 días ("Fábrica reporta retraso en materia prima").
  {
    id: 'ped-005',
    codigo: 'IMP-2026-005-ALFOMBRAS',
    nombre: 'Alfombras Modulares de Polipropileno Alto Tráfico',
    descripcion: 'Contenedor 40HQ con 2.500 m2 de alfombras modulares en palets flejados.',
    proveedor: 'Tianjin Shanhua Carpet Co., Ltd.',
    puertoOrigen: 'Tianjin, China',
    puertoDestino: 'Guayaquil (Posorja), Ecuador',
    contenedor: 'Por asignar',
    blNumero: 'Por emitir',
    fechaPedido: '2026-08-03',
    responsablePedido: 'Marcela Catucuamba',
    gestionChina: 'David Tulcán',
    elaboradoPor: 'Mirian Franco',
    version: 'v1',
    esProductoNuevo: false,
    ampliacionFabricacionDias: 5,
    etapaActualNumero: 4,
    estado: 'En fabricación',
    bodegaDestino: 'Chongón',
    productos: [
      {
        id: 'p-005-1',
        codigoDecokasa: 'ALF-MOD-50X50',
        codigoChino: 'SH-TIAN-50',
        descripcion: 'Alfombra modular 50x50cm base PVC',
        medida: '50 x 50 cm',
        color: 'Gris grafito / azul naval',
        cantidadPedida: 10000,
        estado: 'Pendiente',
      },
    ],
    etapas: buildChainedStages(
      '2026-08-03',
      false,
      5,
      {
        1: { fechaRealFin: '2026-08-03', completadoPor: 'Marcela Catucuamba' },
        2: { fechaRealFin: '2026-08-06', completadoPor: 'David Tulcán' },
        3: { fechaRealFin: '2026-08-10', completadoPor: 'Juan Carlos Tulcán' },
      },
      4
    ),
    historial: [
      {
        id: 'h-005-1',
        fecha: '15/09/2026 10:20',
        usuarioId: 'usr-4',
        usuarioNombre: 'David Tulcán',
        usuarioRol: 'Editor Compras China',
        accion: 'Solicitó ampliación de fabricación de +5 días hábiles',
        motivo: 'Fábrica reporta retraso en materia prima',
        tipo: 'ampliacion',
      },
    ],
  },

  // 7. IMP-2026-007-ZOCALOS — EN PAUSA por la Junta ("Revisar cantidades").
  {
    id: 'ped-007',
    codigo: 'IMP-2026-007-ZOCALOS',
    nombre: 'Zócalos de Poliestireno Blanco 8cm Anti-Humedad',
    descripcion: 'Lote de 15.000 tiras de zócalo decorativo impermeable.',
    proveedor: 'Foshan Hero Metal Industrial Co., Ltd.',
    puertoOrigen: 'Shenzhen, China',
    puertoDestino: 'Guayaquil, Ecuador',
    contenedor: 'SUDU-839201-9 (40HQ)',
    blNumero: 'HLCU0912830',
    fechaPedido: '2026-08-10',
    responsablePedido: 'Marcela Catucuamba',
    gestionChina: 'David Tulcán',
    elaboradoPor: 'Mirian Franco',
    version: 'v1',
    esProductoNuevo: false,
    ampliacionFabricacionDias: 0,
    etapaActualNumero: 3,
    estado: 'En pausa',
    motivoPausaCancelacion: 'Revisar cantidades',
    usuarioPausaCancelacion: 'Juan Carlos Tulcán (Junta de Socios)',
    bodegaDestino: 'Ibarra',
    productos: [
      {
        id: 'p-007-1',
        codigoDecokasa: 'ZOC-POL-8CM',
        codigoChino: 'FS-HERO-Z8',
        descripcion: 'Zócalo poliestireno blanco 8cm x 2.40m',
        medida: '80 x 15 x 2400 mm',
        color: 'Blanco liso',
        cantidadPedida: 15000,
        estado: 'Pendiente',
      },
    ],
    etapas: buildChainedStages(
      '2026-08-10',
      false,
      0,
      {
        1: { fechaRealFin: '2026-08-10', completadoPor: 'Marcela Catucuamba' },
        2: { fechaRealFin: '2026-08-13', completadoPor: 'David Tulcán' },
      },
      3
    ),
    historial: [
      {
        id: 'h-007-1',
        fecha: '14/08/2026 15:00',
        usuarioId: 'usr-2',
        usuarioNombre: 'Juan Carlos Tulcán',
        usuarioRol: 'Junta de Socios',
        accion: 'Pausó el pedido temporalmente',
        motivo: 'Revisar cantidades',
        tipo: 'estado',
      },
    ],
  },

  // 8. IMP-2026-008-LAMINAS-PVC — Revisión, DEVUELTO con observaciones ("El formato del Excel no coincide con la plantilla"), incluye producto nuevo (+10 días).
  {
    id: 'ped-008',
    codigo: 'IMP-2026-008-LAMINAS-PVC',
    nombre: 'Láminas de Mármol PVC UV Translúcidas — Línea Nueva',
    descripcion: 'Contenedor 20GP con 900 láminas de PVC efecto ónix translúcido con retroiluminación.',
    proveedor: 'Haining UV Decor Material Co., Ltd.',
    puertoOrigen: 'Ningbo, China',
    puertoDestino: 'Guayaquil, Ecuador',
    contenedor: 'Por asignar',
    blNumero: 'Por emitir',
    fechaPedido: '2026-09-28',
    responsablePedido: 'Andrea Quishpe (Marketing)',
    gestionChina: 'David Tulcán',
    elaboradoPor: 'Andrea Quishpe',
    version: 'v1',
    esProductoNuevo: true,
    ampliacionFabricacionDias: 0,
    etapaActualNumero: 2,
    estado: 'Devuelto con observaciones',
    bodegaDestino: 'Chongón',
    productos: [
      {
        id: 'p-008-1',
        codigoDecokasa: 'LAM-PVC-ONIX',
        codigoChino: 'HN-ONIX-UV',
        descripcion: 'Lámina PVC translúcida ónix',
        medida: '1220 x 2440 x 3 mm',
        color: 'Ámbar translúcido',
        cantidadPedida: 900,
        estado: 'Pendiente',
      },
    ],
    etapas: buildChainedStages(
      '2026-09-28',
      true, // +10 días en etapa 2
      0,
      {
        1: { fechaRealFin: '2026-09-28', completadoPor: 'Andrea Quishpe' },
      },
      2
    ),
    historial: [
      {
        id: 'h-008-1',
        fecha: '28/09/2026 10:00',
        usuarioId: 'usr-6',
        usuarioNombre: 'Andrea Quishpe',
        usuarioRol: 'Editor Marketing',
        accion: 'Creó el pedido (marcado como Producto Nuevo: +10 días adicionales)',
        motivo: 'Lanzamiento de producto decorativo nuevo',
        tipo: 'check',
      },
      {
        id: 'h-008-2',
        fecha: '30/09/2026 16:30',
        usuarioId: 'usr-4',
        usuarioNombre: 'David Tulcán',
        usuarioRol: 'Editor Compras China',
        accion: 'Devolvió la etapa con observaciones',
        motivo: 'El formato del Excel no coincide con la plantilla',
        tipo: 'devolucion',
      },
    ],
  },

  // 9. IMP-2026-000-LAMINAS-PVC — FINALIZADO, con todos los documentos.
  {
    id: 'ped-000',
    codigo: 'IMP-2026-000-LAMINAS-PVC',
    nombre: 'Láminas de Mármol PVC UV Brillante Carrara',
    descripcion: 'Contenedor 20GP con 800 planchas de PVC tipo mármol carrara.',
    proveedor: 'Haining UV Decor Material Co., Ltd.',
    puertoOrigen: 'Ningbo, China',
    puertoDestino: 'Guayaquil (Contecon), Ecuador',
    contenedor: 'MSKU-102938-7 (20GP)',
    blNumero: 'MEDU82910398',
    fechaPedido: '2026-01-05',
    responsablePedido: 'Marcela Catucuamba',
    gestionChina: 'David Tulcán',
    elaboradoPor: 'Mirian Franco',
    version: 'v1',
    esProductoNuevo: false,
    ampliacionFabricacionDias: 0,
    etapaActualNumero: 11,
    estado: 'Finalizado',
    bodegaDestino: 'Chongón',
    productos: [
      {
        id: 'p-000-1',
        codigoDecokasa: 'LAM-MARMOL-CARRARA',
        codigoChino: 'HN-CARRARA-UV',
        descripcion: 'Lámina PVC brillo mármol carrara',
        medida: '1220 x 2440 x 3 mm',
        color: 'Carrara Blanco',
        cantidadPedida: 800,
        cantidadRecibida: 800,
        diferencia: 0,
        estado: 'Completo',
      },
    ],
    etapas: buildChainedStages(
      '2026-01-05',
      false,
      0,
      {
        1: { fechaRealFin: '2026-01-05', completadoPor: 'Marcela Catucuamba' },
        2: { fechaRealFin: '2026-01-08', completadoPor: 'David Tulcán' },
        3: { fechaRealFin: '2026-01-12', completadoPor: 'Juan Carlos Tulcán' },
        4: { fechaRealFin: '2026-03-02', completadoPor: 'David Tulcán' },
        5: { fechaRealFin: '2026-03-16', completadoPor: 'David Tulcán' },
        6: { fechaRealFin: '2026-03-23', completadoPor: 'Anderson Enriquez' },
        7: { fechaRealFin: '2026-03-30', completadoPor: 'David Tulcán' },
        8: { fechaRealFin: '2026-05-25', completadoPor: 'David Tulcán' },
        9: { fechaRealFin: '2026-06-15', completadoPor: 'Anderson Enriquez' },
        10: { fechaRealFin: '2026-06-22', completadoPor: 'Anderson Enriquez' },
        11: { fechaRealFin: '2026-06-29', completadoPor: 'Marcela Catucuamba', observaciones: 'Cierre documental y liquidación final archivada con contabilidad.' },
      },
      12 // Todas completadas
    ),
    historial: [
      {
        id: 'h-000-1',
        fecha: '29/06/2026 17:00',
        usuarioId: 'usr-8',
        usuarioNombre: 'Marcela Catucuamba',
        usuarioRol: 'Editor Compras Ecuador',
        accion: 'Validó cierre documental y marcó pedido como Finalizado',
        motivo: 'Documentación al 100% y costos conciliados',
        tipo: 'check',
      },
    ],
  },
];

// ==========================================
// SOLICITUD DE PRODUCTO NUEVO DE EJEMPLO
// ==========================================
export const PRODUCTOS_NUEVOS_INICIALES: SolicitudProductoNuevo[] = [
  {
    id: 'pn-001',
    codigo: 'NPROD-2026-001',
    nombre: 'Panel acústico de madera ranurada con fieltro reciclado PET',
    origen: 'China propone',
    pasoActual: 4, // En paso 4 Aprobación (Compras Ecuador)
    estado: 'En aprobación',
    propuestoPor: 'David Tulcán (Compras China)',
    fechaSolicitud: '10/09/2026',
    fechaEstimadaFin: '15/10/2026',
    visitaFabricas: false,
    codigoRegistrado: 'PAN-ACUST-PET-26',
    documentos: [
      { nombre: 'Ficha_Tecnica_Panel_Acustico_PET.pdf', fecha: '22/09/2026', subidoPor: 'David Tulcán' },
      { nombre: 'Catalogo_Colores_Disenos_2027.pdf', fecha: '22/09/2026', subidoPor: 'David Tulcán' },
      { nombre: 'Certificado_Acustico_ISO354.pdf', fecha: '25/09/2026', subidoPor: 'David Tulcán' },
    ],
    observaciones: [
      {
        id: 'obs-pn-1',
        usuarioId: 'usr-4',
        usuarioNombre: 'David Tulcán',
        usuarioRol: 'Editor Compras China',
        avatar: 'DT',
        fecha: '22/09/2026 14:10',
        texto: 'Se sube en conjunto la ficha técnica y el catálogo con 8 muestras de colores de madera natural y fieltro negro.',
      },
      {
        id: 'obs-pn-2',
        usuarioId: 'usr-8',
        usuarioNombre: 'Marcela Catucuamba',
        usuarioRol: 'Editor Compras Ecuador',
        avatar: 'MC',
        fecha: '26/09/2026 10:30',
        texto: 'Recibido en Compras Ecuador. Evaluando precios de flete y demanda proyectada con ventas.',
      },
    ],
  },
];

// ==========================================
// ALERTAS INICIALES DEL SISTEMA
// ==========================================
export const ALERTAS_INICIALES: AlertaItem[] = [
  {
    id: 'alt-1',
    pedidoId: 'ped-002',
    pedidoCodigo: 'IMP-2026-002-CORTINAS-ROLLER',
    productoNombre: 'Cortinas Roller Motorizadas',
    etapaNumero: 9,
    etapaNombre: 'Desaduanización',
    tipo: 'Retenido en aduana',
    mensaje: 'Aduana solicitó inspección física. Coordinar aforo con agente aduanero.',
    fecha: '02/10/2026 09:15',
    leida: false,
    semaforo: 'rojo',
  },
  {
    id: 'alt-2',
    pedidoId: 'ped-003',
    pedidoCodigo: 'IMP-2026-003-PAPEL-TAPIZ',
    productoNombre: 'Papel Tapiz Vinílico',
    etapaNumero: 8,
    etapaNombre: 'Viaje marítimo',
    tipo: 'Retrasado',
    mensaje: 'Este pedido llegará con retraso: 8 días hábiles de atraso acumulados.',
    fecha: '05/10/2026 08:30',
    leida: false,
    semaforo: 'rojo',
  },
  {
    id: 'alt-3',
    pedidoId: 'ped-004',
    pedidoCodigo: 'IMP-2026-004-PERSIANAS',
    productoNombre: 'Persianas Horizontales',
    etapaNumero: 7,
    etapaNombre: 'BL y contenedores',
    tipo: 'Por vencer',
    mensaje: 'Plazo próximo a vencer (faltan 2 días hábiles). Falta subir: Bill of Lading (BL).',
    fecha: '06/10/2026 07:45',
    leida: false,
    semaforo: 'naranja',
  },
  {
    id: 'alt-4',
    pedidoId: 'ped-008',
    pedidoCodigo: 'IMP-2026-008-LAMINAS-PVC',
    productoNombre: 'Láminas de Mármol PVC UV Translúcidas',
    etapaNumero: 2,
    etapaNombre: 'Revisión y check list',
    tipo: 'Devuelto',
    mensaje: 'Pedido devuelto con observaciones: El formato del Excel no coincide con la plantilla.',
    fecha: '30/09/2026 16:30',
    leida: true,
    semaforo: 'naranja',
  },
];

// Aliases
export const USUARIOS = USUARIOS_DEMO;
export const INITIAL_PEDIDOS = PEDIDOS_INICIALES;
export const PEDIDOS_DEMO = PEDIDOS_INICIALES;
export const SOLICITUDES_PRODUCTOS_NUEVOS_DEMO = PRODUCTOS_NUEVOS_INICIALES;
export const ETAPAS_DEFINICION = ETAPAS_BASE_CONFIG;


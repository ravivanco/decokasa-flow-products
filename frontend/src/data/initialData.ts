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

export const DEMO_PASSWORD = 'Demo2026';

// ==========================================
// USUARIOS OFICIALES DE DECOKASA S.A.S
// ==========================================
export const USUARIOS_DEMO: Usuario[] = [
  {
    id: 'usr-mirian',
    nombre: 'Mirian Franco',
    usuarioLogin: 'mfranco',
    telefono: '+593 99 123 4567',
    email: 'mfranco@decokasa.ec',
    cargo: 'Admin · Control de todo el sistema',
    area: 'Sistemas & Dirección',
    pais: 'Ecuador',
    rol: 'admin',
    avatar: 'MF',
    activo: true,
    etapasAsignadasA: [1, 2, 3, 4, 5, 6, 7, 8, 9, 10, 11],
    etapasAsignadasB: [1, 2, 3, 4, 5, 6, 7, 8, 9, 10, 11, 12],
  },
  {
    id: 'usr-marcela',
    nombre: 'Marcela Catucuamba',
    usuarioLogin: 'mcatucuamba',
    telefono: '+593 98 234 5678',
    email: 'mcatucuamba@decokasa.ec',
    cargo: 'Asistente de compras',
    area: 'Departamento de Compras',
    pais: 'Ecuador',
    rol: 'asistente_compras',
    avatar: 'MC',
    activo: true,
    etapasAsignadasA: [3],       // A3: Revisión y aprobación
    etapasAsignadasB: [2, 4],    // B2: Revisión prod propuestos, B4: Revisión y aprobación
  },
  {
    id: 'usr-andrea-q',
    nombre: 'Andrea Quishpe',
    usuarioLogin: 'aquishpe',
    telefono: '+593 97 345 6789',
    email: 'aquishpe@decokasa.ec',
    cargo: 'Marketing · Codificación',
    area: 'Marketing & Catálogo',
    pais: 'Ecuador',
    rol: 'marketing',
    avatar: 'AQ',
    activo: true,
    etapasAsignadasA: [2],       // A2: Codificación
    etapasAsignadasB: [3],       // B3: Codificación
  },
  {
    id: 'usr-david',
    nombre: 'David Túlcan',
    usuarioLogin: 'dtulcan',
    telefono: '+86 138 0013 8000',
    email: 'cdtulcan@decokasa.ec',
    cargo: 'Compras China · Gestión y Fabricación',
    area: 'Compras Internacionales (China)',
    pais: 'China',
    rol: 'compras_china',
    avatar: 'DT',
    activo: true,
    etapasAsignadasA: [4, 5, 6, 7],    // A4: Análisis, A5: Fabricación, A6: Embarque, A7: Aduana
    etapasAsignadasB: [1, 5, 6, 7, 8], // B1: Crea solicitud, B5: Análisis, B6: Fab, B7: Emb, B8: Aduana
  },
  {
    id: 'usr-anderson',
    nombre: 'Anderson Enriquez',
    usuarioLogin: 'aenriquez',
    telefono: '+593 96 456 7890',
    email: 'aenriquez@decokasa.ec',
    cargo: 'Jefe de Logística',
    area: 'Logística & Bodega',
    pais: 'Ecuador',
    rol: 'logistica',
    avatar: 'AE',
    activo: true,
    etapasAsignadasA: [8, 9, 10],      // A8: Autorización salida, A9: Bodega, A10: Incidencias
    etapasAsignadasB: [9, 10, 11],     // B9: Autorización salida, B10: Bodega, B11: Incidencias
  },
  {
    id: 'usr-andrea-c',
    nombre: 'Andrea Collaguazo',
    usuarioLogin: 'acollaguazo',
    telefono: '+86 139 1234 5678',
    email: 'acollaguazo@decokasa.ec',
    cargo: 'Asistente de compras en China',
    area: 'Compras Internacionales (China)',
    pais: 'China',
    rol: 'asistente_china',
    avatar: 'AC',
    activo: true,
    etapasAsignadasA: [],              // Solo consulta y seguimiento
    etapasAsignadasB: [],
  },
  {
    id: 'usr-auditor',
    nombre: 'Invitado Auditor',
    usuarioLogin: 'auditor',
    telefono: '+593 95 567 8901',
    email: 'consulta@decokasa.ec',
    cargo: 'Auditor Externo / Consulta',
    area: 'Auditoría',
    pais: 'Ecuador',
    rol: 'consulta',
    avatar: 'IA',
    activo: true,
    etapasAsignadasA: [],
    etapasAsignadasB: [],
  },
];

// ==========================================
// CATÁLOGO DE BODEGAS (Mantenido por Anderson)
// ==========================================
export const BODEGAS_CATALOGO_INICIAL: BodegaCatalogo[] = [
  {
    id: 'bod-chongon',
    nombre: 'Bodega Principal Chongón',
    ciudad: 'Guayaquil',
    direccion: 'Km 24 Vía a la Costa, Parque Logístico Chongón',
    activa: true,
    creadaPor: 'Anderson Enriquez',
    fechaCreacion: '15/01/2026',
  },
  {
    id: 'bod-quito-norte',
    nombre: 'Bodega Quito Norte (Calderón)',
    ciudad: 'Quito',
    direccion: 'Panamericana Norte Km 11, Complejo Industrial Decokasa',
    activa: true,
    creadaPor: 'Anderson Enriquez',
    fechaCreacion: '18/01/2026',
  },
  {
    id: 'bod-ibarra',
    nombre: 'Bodega Regional Ibarra',
    ciudad: 'Ibarra',
    direccion: 'Av. Cristóbal de Troya y Mariano Acosta',
    activa: true,
    creadaPor: 'Anderson Enriquez',
    fechaCreacion: '22/02/2026',
  },
  {
    id: 'bod-manta',
    nombre: 'Bodega Manta Puerto',
    ciudad: 'Manta',
    direccion: 'Vía Puerto-Aeropuerto, Zona Franca Manta',
    activa: true,
    creadaPor: 'Anderson Enriquez',
    fechaCreacion: '05/03/2026',
  },
  {
    id: 'bod-cuenca',
    nombre: 'Bodega Cuenca Industrial',
    ciudad: 'Cuenca',
    direccion: 'Parque Industrial Cuenca, Sector Challuabamba',
    activa: true,
    creadaPor: 'Anderson Enriquez',
    fechaCreacion: '12/04/2026',
  },
];

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
              driveId: `gdrive-${stageNum}-${docIdx}`,
              driveUrl: `https://drive.google.com/decokasa/${req.tipoDoc}`,
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

// ==========================================
// NOTIFICACIONES GMAIL SIMULADAS
// ==========================================
export const NOTIFICACIONES_GMAIL_INICIALES: NotificacionGmail[] = [
  {
    id: 'gml-1',
    pedidoCodigo: 'DK-EC-2026-0004',
    asunto: '🚨 DEVOLUCIÓN URGENTE (4 h): Pedido DK-EC-2026-0004 requiere recodificación',
    destinatarioEmail: 'mfranco@decokasa.ec',
    destinatarioNombre: 'Mirian Franco (Admin)',
    destinatarioRol: 'Admin',
    remitente: 'cdtulcan@decokasa.ec (David Túlcan · China)',
    fechaHora: '09/10/2026 09:30',
    contenido: 'David Túlcan ha devuelto el pedido DK-EC-2026-0004 a la etapa 2 (Codificación). Motivo: La fábrica en Ningbo requiere especificar el código Pantone y calibre exacto del perfil de aluminio. Se asignó urgencia de 4 horas hábiles.',
    etapaNombre: 'Codificación',
    esAlertaRetraso: false,
    urgencia: 'Urgente',
    leida: false,
  },
  {
    id: 'gml-2',
    pedidoCodigo: 'DK-EC-2026-0003',
    asunto: '⏱ ALERTA SENAE: Demora reportada en Aduana para DK-EC-2026-0003',
    destinatarioEmail: 'mfranco@decokasa.ec',
    destinatarioNombre: 'Mirian Franco (Admin)',
    destinatarioRol: 'Admin',
    remitente: 'cdtulcan@decokasa.ec (David Túlcan · China)',
    fechaHora: '08/10/2026 15:45',
    contenido: 'Se ha reportado una demora en Aduana Guayaquil por aforo físico intrusivo de SENAE (+5 días hábiles). La fecha estimada de llegada y las etapas posteriores han sido recalculadas automáticamente.',
    etapaNombre: 'Aduana Ecuador o Colombia',
    esAlertaRetraso: true,
    leida: false,
  },
  {
    id: 'gml-3',
    pedidoCodigo: 'DK-EC-2026-0002',
    asunto: '📌 AVISO DE ETAPA: Pedido DK-EC-2026-0002 arribó a Bodega (plazo 24 h)',
    destinatarioEmail: 'aenriquez@decokasa.ec',
    destinatarioNombre: 'Anderson Enriquez (Logística)',
    destinatarioRol: 'Logística',
    remitente: 'sistema@decokasa.ec',
    fechaHora: '09/10/2026 08:00',
    contenido: 'El pedido DK-EC-2026-0002 (Cortinas Roller) fue liberado de aduana y ha ingresado a la etapa 9 (Bodega). Por favor selecciona la bodega de destino y sube el acta de ingreso.',
    etapaNombre: 'Bodega',
    esAlertaRetraso: false,
    leida: true,
  },
  {
    id: 'gml-4',
    pedidoCodigo: 'DK-EC-2026-0007',
    asunto: '✨ NUEVO PEDIDO FLUJO B: David Túlcan propuso producto nuevo desde China',
    destinatarioEmail: 'mcatucuamba@decokasa.ec',
    destinatarioNombre: 'Marcela Catucuamba (Asistente de compras)',
    destinatarioRol: 'Asistente de compras',
    remitente: 'cdtulcan@decokasa.ec (David Túlcan · China)',
    fechaHora: '07/10/2026 11:20',
    contenido: 'David Túlcan ha creado el pedido DK-EC-2026-0007 con la propuesta de Revestimiento de Mármol Sintético Flexible. Tienes 24 horas hábiles para dar visto bueno en la etapa 2.',
    etapaNombre: 'Revisión de productos propuestos',
    esAlertaRetraso: false,
    leida: true,
  },
];

// ==========================================
// 8 PEDIDOS INICIALES (FORMATO PROFESIONAL DK-EC-2026-XXXX)
// ==========================================
export const PEDIDOS_INICIALES: Pedido[] = [
  // 1. Finalizado con éxito (Flujo A)
  {
    id: 'ped-001',
    codigo: 'DK-EC-2026-0001',
    pais: 'EC',
    flujo: 'A',
    nombre: 'Paneles WPC Ranurados para Interiores',
    descripcion: 'Contenedor 40HQ con paneles acústicos y perfiles de terminación.',
    fechaPedido: '2026-06-01',
    creadoPor: 'Mirian Franco',
    creadoPorRol: 'Admin',
    version: 'v1',
    esProductoNuevoEcuador: false,
    etapaActualNumero: 11, // Fin automático
    estado: 'Finalizado',
    bodegaDestino: 'Bodega Principal Chongón',
    productos: [
      {
        id: 'p-1',
        codigoDecokasa: 'DK-WPC-TEKA-290',
        codigoChino: 'CN-WPC-08',
        descripcion: 'Panel WPC color Teka 2900x160x22mm',
        medida: '2.90 m',
        color: 'Teka',
        cantidadPedida: 1800,
        cantidadRecibida: 1800,
        diferencia: 0,
        estado: 'Completo',
      },
      {
        id: 'p-2',
        codigoDecokasa: 'DK-WPC-NOGAL-290',
        codigoChino: 'CN-WPC-09',
        descripcion: 'Panel WPC color Nogal 2900x160x22mm',
        medida: '2.90 m',
        color: 'Nogal',
        cantidadPedida: 1200,
        cantidadRecibida: 1200,
        diferencia: 0,
        estado: 'Completo',
      },
    ],
    etapas: buildStagesForOrder('A', '2026-06-01', 11, {
      1: { fechaRealFin: '2026-06-01', completadoPor: 'Mirian Franco' },
      2: { fechaRealFin: '2026-06-02', completadoPor: 'Andrea Quishpe' },
      3: { fechaRealFin: '2026-06-03', completadoPor: 'Marcela Catucuamba' },
      4: { fechaRealFin: '2026-06-06', completadoPor: 'David Túlcan' },
      5: { fechaRealFin: '2026-07-31', completadoPor: 'David Túlcan' },
      6: { fechaRealFin: '2026-08-14', completadoPor: 'David Túlcan' },
      7: { fechaRealFin: '2026-09-28', completadoPor: 'David Túlcan' },
      8: { fechaRealFin: '2026-09-29', completadoPor: 'Anderson Enriquez' },
      9: { fechaRealFin: '2026-09-30', completadoPor: 'Anderson Enriquez' },
      10: { fechaRealFin: '2026-10-01', completadoPor: 'Anderson Enriquez' },
      11: { fechaRealFin: '2026-10-01', completadoPor: 'Sistema (Automático)' },
    }),
    historial: [
      {
        id: 'h-1',
        fecha: '01/10/2026 17:00',
        usuarioId: 'usr-anderson',
        usuarioNombre: 'Anderson Enriquez',
        usuarioRol: 'Logística',
        pedidoCodigo: 'DK-EC-2026-0001',
        accion: 'Completó Incidencias y cerró importación. Fin automático marcado.',
        etapaNumero: 10,
        etapaNombre: 'Incidencias',
        tipo: 'check',
      },
    ],
  },

  // 2. En Bodega - Anderson eligiendo bodega (Flujo A)
  {
    id: 'ped-002',
    codigo: 'DK-EC-2026-0002',
    pais: 'EC',
    flujo: 'A',
    nombre: 'Cortinas Roller Motorizadas y Telas Blackout',
    descripcion: 'Contenedor 40HC con mecanismos motorizados Tuya y rollos de tela.',
    fechaPedido: '2026-06-15',
    creadoPor: 'Mirian Franco',
    creadoPorRol: 'Admin',
    version: 'v1',
    esProductoNuevoEcuador: false,
    etapaActualNumero: 9, // Bodega
    estado: 'En bodega',
    bodegaDestino: 'Bodega Principal Chongón',
    productos: [
      {
        id: 'p-3',
        codigoDecokasa: 'DK-ROL-MOTOR-35',
        codigoChino: 'CN-MOT-TUYA',
        descripcion: 'Motor tubular 35mm radio frecuencia + Wifi',
        medida: '35 mm',
        color: 'Blanco',
        cantidadPedida: 450,
        cantidadRecibida: 448,
        diferencia: -2,
        estado: 'Faltante',
        incidencias: 'Faltan 2 motores en la caja #14.',
      },
    ],
    etapas: buildStagesForOrder('A', '2026-06-15', 9, {
      1: { fechaRealFin: '2026-06-15', completadoPor: 'Mirian Franco' },
      2: { fechaRealFin: '2026-06-16', completadoPor: 'Andrea Quishpe' },
      3: { fechaRealFin: '2026-06-17', completadoPor: 'Marcela Catucuamba' },
      4: { fechaRealFin: '2026-06-20', completadoPor: 'David Túlcan' },
      5: { fechaRealFin: '2026-08-14', completadoPor: 'David Túlcan' },
      6: { fechaRealFin: '2026-08-28', completadoPor: 'David Túlcan' },
      7: { fechaRealFin: '2026-10-06', completadoPor: 'David Túlcan' },
      8: { fechaRealFin: '2026-10-08', completadoPor: 'Anderson Enriquez' },
    }),
    historial: [
      {
        id: 'h-2',
        fecha: '08/10/2026 16:30',
        usuarioId: 'usr-anderson',
        usuarioNombre: 'Anderson Enriquez',
        usuarioRol: 'Logística',
        pedidoCodigo: 'DK-EC-2026-0002',
        accion: 'Autorizó salida de aduana con levante SENAE',
        etapaNumero: 8,
        etapaNombre: 'Autorización de salida',
        tipo: 'check',
      },
    ],
  },

  // 3. En Aduana con reporte de Demora y recálculo (Flujo A)
  {
    id: 'ped-003',
    codigo: 'DK-EC-2026-0003',
    pais: 'EC',
    flujo: 'A',
    nombre: 'Papel Tapiz Vinílico de Alto Tráfico',
    descripcion: 'Rollos de papel tapiz texturizado para hoteles y oficinas.',
    fechaPedido: '2026-06-25',
    creadoPor: 'Mirian Franco',
    creadoPorRol: 'Admin',
    version: 'v1',
    esProductoNuevoEcuador: false,
    etapaActualNumero: 7, // Aduana
    estado: 'En aduana',
    demoraAduana: {
      reportadaPor: 'David Túlcan',
      fechaReporte: '08/10/2026',
      motivo: 'Aforo físico intrusivo de SENAE en Contecon Guayaquil.',
      nuevaFechaEstimadaLlegada: '2026-10-16',
      diasAjuste: 5,
    },
    productos: [
      {
        id: 'p-4',
        codigoDecokasa: 'DK-TAP-GEOM-01',
        descripcion: 'Papel tapiz vinílico diseño geométrico dorado',
        medida: '10 x 0.53 m',
        color: 'Oro/Gris',
        cantidadPedida: 3500,
        estado: 'Pendiente',
      },
    ],
    etapas: buildStagesForOrder('A', '2026-06-25', 7, {
      1: { fechaRealFin: '2026-06-25', completadoPor: 'Mirian Franco' },
      2: { fechaRealFin: '2026-06-26', completadoPor: 'Andrea Quishpe' },
      3: { fechaRealFin: '2026-06-29', completadoPor: 'Marcela Catucuamba' },
      4: { fechaRealFin: '2026-07-02', completadoPor: 'David Túlcan' },
      5: { fechaRealFin: '2026-08-26', completadoPor: 'David Túlcan' },
      6: { fechaRealFin: '2026-09-09', completadoPor: 'David Túlcan' },
      7: { motivoRetraso: 'Demora en aforo aduanero SENAE reportada por David (+5 días).' },
    }),
    historial: [
      {
        id: 'h-3',
        fecha: '08/10/2026 15:45',
        usuarioId: 'usr-david',
        usuarioNombre: 'David Túlcan',
        usuarioRol: 'Compras China',
        pedidoCodigo: 'DK-EC-2026-0003',
        accion: 'Reportó demora en Aduana (+5 días hábiles). Se recalculó cronograma.',
        etapaNumero: 7,
        etapaNombre: 'Aduana Ecuador o Colombia',
        motivo: 'Aforo físico intrusivo de SENAE en Contecon Guayaquil.',
        tipo: 'demora_aduana',
      },
    ],
  },

  // 4. En Codificación - Devuelto por David con URGENCIA 4 H (Flujo A)
  {
    id: 'ped-004',
    codigo: 'DK-EC-2026-0004',
    pais: 'EC',
    flujo: 'A',
    nombre: 'Persianas Horizontales de Aluminio 50mm',
    descripcion: 'Láminas microperforadas y cabezales reforzados.',
    fechaPedido: '2026-10-05',
    creadoPor: 'Mirian Franco',
    creadoPorRol: 'Admin',
    version: 'v2',
    esProductoNuevoEcuador: true,
    etapaActualNumero: 2, // Codificación
    estado: 'Devuelto a codificación',
    urgenciaDevolucion: {
      nivel: 'Urgente',
      horasMaximas: 4,
      motivo: 'La fábrica en Ningbo exige codificar los colores Pantone y el grosor exacto de las láminas.',
      activadaEn: '09/10/2026 09:30',
      fechaLimite: '09/10/2026 13:30',
      asignadoA: 'Andrea Quishpe (Marketing)',
    },
    productos: [
      {
        id: 'p-5',
        codigoDecokasa: 'DK-PER-ALU-50-BL',
        descripcion: 'Persiana de aluminio 50mm esmaltado blanco',
        medida: '50 mm',
        color: 'Blanco Mate',
        cantidadPedida: 800,
        estado: 'Pendiente',
      },
    ],
    etapas: buildStagesForOrder('A', '2026-10-05', 2, {
      1: { fechaRealFin: '2026-10-05', completadoPor: 'Mirian Franco' },
      2: { motivoRetraso: 'Urgencia 4 h activada por Compras China. Pendiente de recodificación.' },
    }),
    historial: [
      {
        id: 'h-4',
        fecha: '09/10/2026 09:30',
        usuarioId: 'usr-david',
        usuarioNombre: 'David Túlcan',
        usuarioRol: 'Compras China',
        pedidoCodigo: 'DK-EC-2026-0004',
        accion: 'Devolvió pedido a Codificación con URGENCIA de 4 h',
        etapaNumero: 4,
        etapaNombre: 'Análisis y gestión',
        motivo: 'Fábrica en Ningbo exige código Pantone y grosor exacto de lámina.',
        tipo: 'devolucion_urgente',
      },
    ],
  },

  // 5. En Fabricación en China (Flujo A)
  {
    id: 'ped-005',
    codigo: 'DK-EC-2026-0005',
    pais: 'EC',
    flujo: 'A',
    nombre: 'Pisos SPC Vinílicos Click 5.5mm con Manta IXPE',
    descripcion: 'Pisos hidrófugos imitación madera roble escandinavo.',
    fechaPedido: '2026-08-10',
    creadoPor: 'Mirian Franco',
    creadoPorRol: 'Admin',
    version: 'v1',
    esProductoNuevoEcuador: false,
    etapaActualNumero: 5, // Fabricación
    estado: 'En fabricación',
    productos: [
      {
        id: 'p-6',
        codigoDecokasa: 'DK-SPC-ROBLE-18',
        descripcion: 'Piso vinílico rígido SPC 182x1220mm capa 0.5mm',
        medida: '1.22 m',
        color: 'Roble Escandinavo',
        cantidadPedida: 2400,
        estado: 'Pendiente',
      },
    ],
    etapas: buildStagesForOrder('A', '2026-08-10', 5, {
      1: { fechaRealFin: '2026-08-10', completadoPor: 'Mirian Franco' },
      2: { fechaRealFin: '2026-08-11', completadoPor: 'Andrea Quishpe' },
      3: { fechaRealFin: '2026-08-12', completadoPor: 'Marcela Catucuamba' },
      4: { fechaRealFin: '2026-08-15', completadoPor: 'David Túlcan' },
    }),
    historial: [
      {
        id: 'h-5',
        fecha: '15/08/2026 10:00',
        usuarioId: 'usr-david',
        usuarioNombre: 'David Túlcan',
        usuarioRol: 'Compras China',
        pedidoCodigo: 'DK-EC-2026-0005',
        accion: 'Aprobó análisis y gestión. Orden enviada a fabricación.',
        etapaNumero: 4,
        etapaNombre: 'Análisis y gestión',
        tipo: 'check',
      },
    ],
  },

  // 6. En Revisión y aprobación por Marcela (Flujo A)
  {
    id: 'ped-006',
    codigo: 'DK-EC-2026-0006',
    pais: 'EC',
    flujo: 'A',
    nombre: 'Molduras Decorativas de Poliuretano para Techo',
    descripcion: 'Cornisas de 2.40m tratadas contra humedad.',
    fechaPedido: '2026-10-07',
    creadoPor: 'Mirian Franco',
    creadoPorRol: 'Admin',
    version: 'v1',
    esProductoNuevoEcuador: true,
    etapaActualNumero: 3, // Revisión y aprobación
    estado: 'En revisión y aprobación',
    productos: [
      {
        id: 'p-7',
        codigoDecokasa: 'DK-MOL-CR-90',
        descripcion: 'Cornisa clásica de poliuretano 90x90mm',
        medida: '2.40 m',
        color: 'Blanco primer',
        cantidadPedida: 1500,
        estado: 'Pendiente',
      },
    ],
    etapas: buildStagesForOrder('A', '2026-10-07', 3, {
      1: { fechaRealFin: '2026-10-07', completadoPor: 'Mirian Franco' },
      2: { fechaRealFin: '2026-10-08', completadoPor: 'Andrea Quishpe' },
    }),
    historial: [
      {
        id: 'h-6',
        fecha: '08/10/2026 17:15',
        usuarioId: 'usr-andrea-q',
        usuarioNombre: 'Andrea Quishpe',
        usuarioRol: 'Marketing',
        pedidoCodigo: 'DK-EC-2026-0006',
        accion: 'Completó codificación y subió etiquetas. Envió a Marcela.',
        etapaNumero: 2,
        etapaNombre: 'Codificación',
        tipo: 'check',
      },
    ],
  },

  // 7. Flujo B (Producto nuevo propuesto por China) — Etapa 2: Marcela revisando
  {
    id: 'ped-007',
    codigo: 'DK-EC-2026-0007',
    pais: 'EC',
    flujo: 'B',
    nombre: 'Revestimiento de Mármol Sintético Flexible UV',
    descripcion: 'Propuesta desde China: láminas flexibles traslúcidas retroiluminables.',
    fechaPedido: '2026-10-07',
    creadoPor: 'David Túlcan',
    creadoPorRol: 'Compras China',
    version: 'v1',
    productoPropuestoChinaNombre: 'Láminas Flexible UV Marble 2800x1220x3mm',
    etapaActualNumero: 2, // Revisión de productos propuestos
    estado: 'En revisión de producto propuesto',
    productos: [
      {
        id: 'p-8',
        codigoDecokasa: 'DK-MAR-FLEX-01',
        codigoChino: 'CN-FLEX-MARBLE',
        descripcion: 'Lámina de mármol flexible traslúcida',
        medida: '2.80 x 1.22 m',
        color: 'Calacatta Gold',
        cantidadPedida: 600,
        estado: 'Pendiente',
      },
    ],
    etapas: buildStagesForOrder('B', '2026-10-07', 2, {
      1: { fechaRealFin: '2026-10-07', completadoPor: 'David Túlcan' },
    }),
    historial: [
      {
        id: 'h-7',
        fecha: '07/10/2026 11:20',
        usuarioId: 'usr-david',
        usuarioNombre: 'David Túlcan',
        usuarioRol: 'Compras China',
        pedidoCodigo: 'DK-EC-2026-0007',
        accion: 'Creó solicitud de pedido con propuesta de producto nuevo desde China (Flujo B)',
        etapaNumero: 1,
        etapaNombre: 'Crear solicitud de pedido (China)',
        tipo: 'check',
      },
    ],
  },

  // 8. Flujo B (Producto nuevo propuesto por China) — Etapa 5: David en Análisis
  {
    id: 'ped-008',
    codigo: 'DK-EC-2026-0008',
    pais: 'EC',
    flujo: 'B',
    nombre: 'Sistema de Iluminación LED Magnética Lineal',
    descripcion: 'Rieles magnéticos empotrables y módulos LED difusos y direccionales.',
    fechaPedido: '2026-09-28',
    creadoPor: 'David Túlcan',
    creadoPorRol: 'Compras China',
    version: 'v1',
    productoPropuestoChinaNombre: 'Track Light Magnetic Slim 48V',
    etapaActualNumero: 5, // Análisis y gestión
    estado: 'En análisis y gestión',
    productos: [
      {
        id: 'p-9',
        codigoDecokasa: 'DK-LED-TRK-48V',
        codigoChino: 'CN-MAG-48V',
        descripcion: 'Riel magnético empotrable 2m 48V negro mate',
        medida: '2.00 m',
        color: 'Negro',
        cantidadPedida: 400,
        estado: 'Pendiente',
      },
    ],
    etapas: buildStagesForOrder('B', '2026-09-28', 5, {
      1: { fechaRealFin: '2026-09-28', completadoPor: 'David Túlcan' },
      2: { fechaRealFin: '2026-09-29', completadoPor: 'Marcela Catucuamba' },
      3: { fechaRealFin: '2026-09-30', completadoPor: 'Andrea Quishpe' },
      4: { fechaRealFin: '2026-10-01', completadoPor: 'Marcela Catucuamba' },
    }),
    historial: [
      {
        id: 'h-8',
        fecha: '01/10/2026 16:00',
        usuarioId: 'usr-marcela',
        usuarioNombre: 'Marcela Catucuamba',
        usuarioRol: 'Asistente de compras',
        pedidoCodigo: 'DK-EC-2026-0008',
        accion: 'Aprobó codificación y envió a Análisis de David en China',
        etapaNumero: 4,
        etapaNombre: 'Revisión y aprobación',
        tipo: 'aprobacion',
      },
    ],
  },
];

// Compatibilidad
export const USUARIOS = USUARIOS_DEMO;
export const INITIAL_PEDIDOS = PEDIDOS_INICIALES;
export const PEDIDOS_DEMO = PEDIDOS_INICIALES;
export const ETAPAS_BASE_CONFIG = ETAPAS_FLUJO_A;
export const ETAPAS_DEFINICION = ETAPAS_FLUJO_A;

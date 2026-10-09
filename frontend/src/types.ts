export type UserRole =
  | 'admin'             // Mirian Franco: control de todo el sistema, crea solicitudes flujo A, agrega usuarios/roles, actúa en cualquier etapa
  | 'asistente_compras' // Marcela Catucuamba: Revisión y aprobación (A3/B4), Revisión de productos propuestos (B2)
  | 'marketing'         // Andrea Quishpe: Codificación (A2/B3)
  | 'compras_china'     // David Túlcan: Análisis y gestión (A4/B5), Fabricación, Embarque, Aduana; crea solicitudes flujo B
  | 'logistica'         // Anderson Enriquez: Autorización de salida, Bodega, Incidencias; catálogo de bodegas
  | 'asistente_china'   // Andrea Collaguazo: Asistente de compras en China (consulta/seguimiento)
  | 'consulta';         // Invitado / Auditor

export interface Usuario {
  id: string;
  nombre: string;
  usuarioLogin: string;  // Login por nombre de usuario
  telefono: string;      // Login por teléfono
  email: string;         // Login por correo
  cargo: string;
  area: string;
  pais: 'Ecuador' | 'China';
  rol: UserRole;
  avatar: string;
  activo: boolean;
  etapasAsignadasA: number[]; // Etapas asignadas en Flujo A
  etapasAsignadasB: number[]; // Etapas asignadas en Flujo B
}

export type Pais = 'EC' | 'CO'; // Ecuador (activo) / Colombia (fase 2)

export type TipoFlujo = 'A' | 'B'; // A: Pedido Ecuador (existente/nuevo) | B: Propuesto por China

export type Semaforo = 'verde' | 'naranja' | 'rojo' | 'gris';

export type UrgenciaNivel = 'Normal' | 'Prioritario' | 'Urgente';

export interface UrgenciaDevolucion {
  nivel: UrgenciaNivel;
  horasMaximas: number; // 4, 12 o 24
  motivo: string;
  activadaEn: string;
  fechaLimite: string;
  asignadoA: string;
}

export type EstadoPedido =
  | 'Solicitud creada'
  | 'En revisión de producto propuesto'
  | 'En codificación'
  | 'En revisión y aprobación'
  | 'Devuelto a codificación'
  | 'En análisis y gestión'
  | 'En fabricación'
  | 'En embarque'
  | 'En aduana'
  | 'En autorización de salida'
  | 'En bodega'
  | 'En incidencias'
  | 'Finalizado'
  | 'En pausa'
  | 'Cancelado';

export interface BodegaCatalogo {
  id: string;
  nombre: string;
  ciudad: string;
  direccion?: string;
  activa: boolean;
  creadaPor: string;
  fechaCreacion: string;
}

export interface ArchivoVersion {
  version: number;
  nombre: string;
  url?: string;
  driveId?: string;
  driveUrl?: string;
  tamano: string; // ej: 14.2 MB (límite 100 MB)
  subidoPor: string;
  subidoPorRol: string;
  fecha: string;
  esImagen?: boolean;
}

export interface DocumentoEtapa {
  id: string;
  tipoDoc: string;
  esObligatorio: boolean;
  archivoActual?: ArchivoVersion;
  historialVersiones: ArchivoVersion[];
}

export interface ItemChecklist {
  id: string;
  texto: string;
  completado: boolean;
  completadoPor?: string;
  fecha?: string;
}

export interface ObservacionMensaje {
  id: string;
  usuarioId: string;
  usuarioNombre: string;
  usuarioRol: string;
  avatar: string;
  fecha: string;
  texto: string;
  archivosAdjuntos?: { nombre: string; tamano: string; driveUrl?: string }[];
}

export interface HistorialAuditoria {
  id: string;
  fecha: string; // dd/mm/aaaa hh:mm
  usuarioId: string;
  usuarioNombre: string;
  usuarioRol: string;
  pedidoCodigo?: string;
  etapaNumero?: number;
  etapaNombre?: string;
  accion: string;
  motivo?: string;
  tipo:
    | 'check'
    | 'aprobacion'
    | 'rechazo'
    | 'devolucion_urgente'
    | 'demora_aduana'
    | 'estado'
    | 'observacion'
    | 'documento'
    | 'reasignacion'
    | 'bodega';
  driveArchivos?: string[];
}

export interface NotificacionGmail {
  id: string;
  pedidoCodigo: string;
  asunto: string;
  destinatarioEmail: string;
  destinatarioNombre: string;
  destinatarioRol: string;
  remitente: string;
  fechaHora: string;
  contenido: string;
  etapaNombre: string;
  esAlertaRetraso?: boolean;
  urgencia?: UrgenciaNivel;
  leida: boolean;
}

export interface EtapaDef {
  numero: number;
  id: string;
  nombre: string;
  fase: 'Solicitud' | 'Codificación y Revisión' | 'China (Gestión y Fabricación)' | 'Tránsito y Llegada' | 'Cierre y Bodega';
  descripcion: string;
  plazoHorasHabiles?: number; // Para 24h, 4h, 12h
  plazoDiasHabiles?: number;  // Para 3 días, 10 días, etc.
  responsableTexto: string;
  responsableRol: UserRole;
  icono: string;
  esFinAutomatico?: boolean;
  checklistsBase: string[];
  documentosRequeridos: { tipoDoc: string; obligatorio: boolean }[];
}

export interface EtapaInstancia {
  numero: number;
  nombre: string;
  plazoTexto: string;
  plazoHorasHabiles?: number;
  plazoDiasHabiles?: number;
  fechaInicioEstimada: string; // YYYY-MM-DD
  fechaFinEstimada: string;    // YYYY-MM-DD
  fechaRealInicio?: string;
  fechaRealFin?: string;
  completada: boolean;
  devuelta?: boolean;
  completadoPor?: string;
  completadoPorRol?: string;
  checklist: ItemChecklist[];
  documentos: DocumentoEtapa[];
  observaciones: ObservacionMensaje[];
  motivoRetraso?: string;
}

export interface DemoraAduanaInfo {
  reportadaPor: string;
  fechaReporte: string;
  motivo: string;
  nuevaFechaEstimadaLlegada: string;
  diasAjuste: number;
}

export interface ProductoLineaPedido {
  id: string;
  codigoDecokasa: string;
  codigoChino?: string;
  descripcion: string;
  medida: string;
  color: string;
  cantidadPedida: number;
  cantidadRecibida?: number;
  diferencia?: number;
  estado: 'Completo' | 'Faltante' | 'Sobrante' | 'Dañado' | 'Pendiente';
  incidencias?: string;
  fotoIncidencia?: string;
}

export interface Pedido {
  id: string;
  codigo: string;          // Formato DK-EC-2026-0001
  pais: Pais;              // EC | CO
  flujo: TipoFlujo;        // A | B
  nombre: string;
  descripcion?: string;
  fechaPedido: string;     // YYYY-MM-DD
  creadoPor: string;
  creadoPorRol: string;
  version: string;         // v1, v2
  esProductoNuevoEcuador?: boolean; // En flujo A, si incluye producto nuevo pedido por Ecuador
  productoPropuestoChinaNombre?: string; // En flujo B
  etapaActualNumero: number; // 1 a 11 en Flujo A; 1 a 12 en Flujo B
  estado: EstadoPedido;
  urgenciaDevolucion?: UrgenciaDevolucion; // Urgencia activa (4h, 12h, 24h)
  demoraAduana?: DemoraAduanaInfo;
  bodegaDestino?: string;  // Elegida por Anderson en etapa Bodega
  motivoPausaCancelacion?: string;
  etapas: EtapaInstancia[];
  productos: ProductoLineaPedido[];
  historial: HistorialAuditoria[];
}

export type AppModule =
  | 'inicio'              // Dashboard por rol
  | 'rastreo'             // Búsqueda tipo Servientrega/DHL
  | 'pedidos'             // Lista y filtro de pedidos
  | 'detalle_pedido'      // Detalle del pedido con línea de tiempo y checks
  | 'bodegas'             // Catálogo de bodegas (Anderson)
  | 'gmail_alertas'       // Bandeja de notificaciones Gmail al Admin
  | 'cronograma_meta'     // Calculadora de cronograma vs meta de 90 días
  | 'usuarios'            // Gestión de usuarios y roles (Mirian)
  | 'auditoria'           // Historial global inmutable
  | 'ayuda';              // Documento de requerimientos y manual

export type Language = 'es' | 'en';

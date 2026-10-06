export type UserRole =
  | 'admin'
  | 'junta'
  | 'editor_china'
  | 'editor_marketing'
  | 'editor_logistica'
  | 'editor_compras_ec'
  | 'consulta';

export interface Usuario {
  id: string;
  nombre: string;
  email: string;
  cargo: string;
  area: string;
  pais: 'Ecuador' | 'China';
  rol: UserRole;
  avatar: string;
  activo: boolean;
  etapasAsignadas: number[];
}

export type Semaforo = 'verde' | 'naranja' | 'rojo' | 'gris';

export type EstadoPedido =
  | 'Creado'
  | 'En revisión'
  | 'Devuelto con observaciones'
  | 'En fabricación'
  | 'Pendiente de embarque'
  | 'En tránsito'
  | 'En aduana'
  | 'En bodega'
  | 'En cierre documental'
  | 'Finalizado'
  | 'Reabierto'
  | 'En pausa'
  | 'Cancelado';

export type SubestadoAduana = 'En trámite' | 'Retenido' | 'Liberado';

export type BodegaDestino = 'Chongón' | 'Quito' | 'Ibarra' | 'Manta' | 'Cuenca';

export interface ArchivoVersion {
  version: number;
  nombre: string;
  url?: string;
  driveUrl?: string;
  tamano: string;
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
  archivosAdjuntos?: { nombre: string; tamano: string }[];
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
  tipo: 'check' | 'reversion' | 'devolucion' | 'ampliacion' | 'estado' | 'observacion' | 'subestado' | 'documento' | 'config';
}

export interface EtapaDef {
  numero: number;
  id: string;
  nombre: string;
  grupo: 'Pedido' | 'Producción en China' | 'Documentos' | 'Tránsito' | 'Llegada a Ecuador';
  explicacion: string;
  plazoDiasHabiles: number;
  responsableTexto: string;
  suplenteTexto: string;
  rolesAutorizados: UserRole[];
  icono: string;
  checklistsBase: string[];
  documentosRequeridos: { tipoDoc: string; obligatorio: boolean }[];
}

export interface EtapaInstancia {
  numero: number;
  nombre: string;
  plazoDiasHabiles: number;
  plazoExtraDias: number; // Por producto nuevo (+10) o ampliación de fabricación (max +10)
  fechaInicioEstimada: string; // YYYY-MM-DD
  fechaFinEstimada: string; // YYYY-MM-DD
  fechaRealInicio?: string;
  fechaRealFin?: string;
  completada: boolean;
  devuelta?: boolean;
  completadoPor?: string;
  completadoPorRol?: string;
  checklist: ItemChecklist[];
  documentos: DocumentoEtapa[];
  observaciones: ObservacionMensaje[];
  subestadoAduana?: SubestadoAduana;
}

export interface ProductoLineaPedido {
  id: string;
  codigoDecokasa: string;
  codigoChino: string;
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
  codigo: string;
  nombre: string;
  descripcion?: string;
  proveedor?: string;
  puertoOrigen?: string;
  puertoDestino?: string;
  contenedor?: string;
  blNumero?: string;
  fechaPedido: string; // YYYY-MM-DD
  responsablePedido: string;
  gestionChina: string;
  elaboradoPor: string;
  version: string; // v1, v2
  esProductoNuevo: boolean;
  ampliacionFabricacionDias: number;
  etapaActualNumero: number; // 1 a 11
  estado: EstadoPedido;
  subestadoAduana?: SubestadoAduana;
  bodegaDestino: BodegaDestino;
  motivoPausaCancelacion?: string;
  usuarioPausaCancelacion?: string;
  etapas: EtapaInstancia[];
  productos: ProductoLineaPedido[];
  historial: HistorialAuditoria[];
}

export interface SolicitudProductoNuevo {
  id: string;
  codigo: string;
  nombre: string;
  origen: 'Ecuador solicita' | 'China propone';
  pasoActual: number; // 1 a 6
  estado: 'En investigación' | 'Ficha técnica' | 'En aprobación' | 'En codificación' | 'Listo para pedido' | 'Rechazado';
  propuestoPor: string;
  fechaSolicitud: string;
  fechaEstimadaFin: string;
  visitaFabricas: boolean; // +10 días si es true
  motivoRechazo?: string;
  codigoRegistrado?: string;
  documentos: { nombre: string; fecha: string; subidoPor: string }[];
  observaciones: ObservacionMensaje[];
}

export interface AlertaItem {
  id: string;
  pedidoId: string;
  pedidoCodigo: string;
  productoNombre: string;
  etapaNumero: number;
  etapaNombre: string;
  tipo: 'Por vencer' | 'Retrasado' | 'Retenido en aduana' | 'Devuelto' | 'Cambio de estado';
  mensaje: string;
  fecha: string;
  leida: boolean;
  semaforo: Semaforo;
}

export interface StageConfig {
  numero: number;
  nombre: string;
  plazoDiasHabiles: number;
}

export type AppModule =
  | 'inicio'
  | 'rastreo'
  | 'pedidos'
  | 'detalle_pedido'
  | 'productos_nuevos'
  | 'alertas'
  | 'reportes'
  | 'usuarios'
  | 'auditoria'
  | 'configuracion'
  | 'ayuda';

export type Language = 'es' | 'en';

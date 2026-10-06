import { UserRole, AppModule, Usuario } from '../types';

export interface PermisoRolMatriz {
  rol: UserRole;
  nombreRol: string;
  descripcion: string;
  modulosPermitidos: AppModule[];
  etapasOperativas: number[]; // 1 a 11
  puedeCrearPedido: boolean;
  puedeAprobarJunta: boolean;
  puedePausarCancelarReabrir: boolean;
  puedeAmpliarFabricacion: boolean;
  puedeGestionarAduana: boolean;
  puedeRegistrarIncidenciasBodega: boolean;
  puedeCierreFinal: boolean;
  puedeEditarFechas: boolean;
  puedeGestionarUsuarios: boolean;
  puedeConfigurarPlazos: boolean;
  puedeVerAuditoria: boolean;
  puedeVerReportes: boolean;
  puedeDescargarArchivos: boolean;
  puedeSubirArchivos: boolean;
}

export const MATRIZ_PERMISOS: Record<UserRole, PermisoRolMatriz> = {
  admin: {
    rol: 'admin',
    nombreRol: 'Administrador',
    descripcion: 'Control total de la plataforma, usuarios, auditoría, plazos y cualquier etapa del pedido.',
    modulosPermitidos: [
      'inicio',
      'rastreo',
      'pedidos',
      'detalle_pedido',
      'productos_nuevos',
      'alertas',
      'reportes',
      'usuarios',
      'auditoria',
      'configuracion',
      'ayuda',
    ],
    etapasOperativas: [1, 2, 3, 4, 5, 6, 7, 8, 9, 10, 11],
    puedeCrearPedido: true,
    puedeAprobarJunta: true,
    puedePausarCancelarReabrir: true,
    puedeAmpliarFabricacion: true,
    puedeGestionarAduana: true,
    puedeRegistrarIncidenciasBodega: true,
    puedeCierreFinal: true,
    puedeEditarFechas: true,
    puedeGestionarUsuarios: true,
    puedeConfigurarPlazos: true,
    puedeVerAuditoria: true,
    puedeVerReportes: true,
    puedeDescargarArchivos: true,
    puedeSubirArchivos: true,
  },
  junta: {
    rol: 'junta',
    nombreRol: 'Junta de Socios (Aprobador)',
    descripcion: 'Aprueba paso a fabricación en etapa 3, pausa, cancela, reabre pedidos y revisa reportes.',
    modulosPermitidos: [
      'inicio',
      'rastreo',
      'pedidos',
      'detalle_pedido',
      'alertas',
      'reportes',
      'ayuda',
    ],
    etapasOperativas: [3],
    puedeCrearPedido: false,
    puedeAprobarJunta: true,
    puedePausarCancelarReabrir: true,
    puedeAmpliarFabricacion: false,
    puedeGestionarAduana: false,
    puedeRegistrarIncidenciasBodega: false,
    puedeCierreFinal: false,
    puedeEditarFechas: false,
    puedeGestionarUsuarios: false,
    puedeConfigurarPlazos: false,
    puedeVerAuditoria: false,
    puedeVerReportes: true,
    puedeDescargarArchivos: true,
    puedeSubirArchivos: false,
  },
  editor_china: {
    rol: 'editor_china',
    nombreRol: 'Editor Compras China',
    descripcion: 'Gestión con fábricas en China, revisión técnica, packing list, embarque, BL y navegación.',
    modulosPermitidos: [
      'inicio',
      'rastreo',
      'pedidos',
      'detalle_pedido',
      'productos_nuevos',
      'alertas',
      'ayuda',
    ],
    etapasOperativas: [2, 4, 5, 7, 8],
    puedeCrearPedido: false,
    puedeAprobarJunta: false,
    puedePausarCancelarReabrir: false,
    puedeAmpliarFabricacion: true,
    puedeGestionarAduana: false,
    puedeRegistrarIncidenciasBodega: false,
    puedeCierreFinal: false,
    puedeEditarFechas: false,
    puedeGestionarUsuarios: false,
    puedeConfigurarPlazos: false,
    puedeVerAuditoria: false,
    puedeVerReportes: false,
    puedeDescargarArchivos: true,
    puedeSubirArchivos: true,
  },
  editor_marketing: {
    rol: 'editor_marketing',
    nombreRol: 'Editor Marketing / Sistemas',
    descripcion: 'Crea solicitudes iniciales de pedido con Excel y Zip de etiquetas, codifica productos nuevos.',
    modulosPermitidos: [
      'inicio',
      'rastreo',
      'pedidos',
      'detalle_pedido',
      'productos_nuevos',
      'alertas',
      'ayuda',
    ],
    etapasOperativas: [1],
    puedeCrearPedido: true,
    puedeAprobarJunta: false,
    puedePausarCancelarReabrir: false,
    puedeAmpliarFabricacion: false,
    puedeGestionarAduana: false,
    puedeRegistrarIncidenciasBodega: false,
    puedeCierreFinal: false,
    puedeEditarFechas: false,
    puedeGestionarUsuarios: false,
    puedeConfigurarPlazos: false,
    puedeVerAuditoria: false,
    puedeVerReportes: false,
    puedeDescargarArchivos: true,
    puedeSubirArchivos: true,
  },
  editor_logistica: {
    rol: 'editor_logistica',
    nombreRol: 'Editor Logística Ecuador',
    descripcion: 'Valida reporte de mercadería, gestiona aduana SENAE, recepción e incidencias en bodega.',
    modulosPermitidos: [
      'inicio',
      'rastreo',
      'pedidos',
      'detalle_pedido',
      'alertas',
      'ayuda',
    ],
    etapasOperativas: [6, 9, 10, 11],
    puedeCrearPedido: false,
    puedeAprobarJunta: false,
    puedePausarCancelarReabrir: false,
    puedeAmpliarFabricacion: false,
    puedeGestionarAduana: true,
    puedeRegistrarIncidenciasBodega: true,
    puedeCierreFinal: false,
    puedeEditarFechas: false,
    puedeGestionarUsuarios: false,
    puedeConfigurarPlazos: false,
    puedeVerAuditoria: false,
    puedeVerReportes: false,
    puedeDescargarArchivos: true,
    puedeSubirArchivos: true,
  },
  editor_compras_ec: {
    rol: 'editor_compras_ec',
    nombreRol: 'Editor Compras Ecuador',
    descripcion: 'Investiga productos nuevos, valida liquidación documental y da check final de cierre.',
    modulosPermitidos: [
      'inicio',
      'rastreo',
      'pedidos',
      'detalle_pedido',
      'productos_nuevos',
      'alertas',
      'ayuda',
    ],
    etapasOperativas: [11],
    puedeCrearPedido: false,
    puedeAprobarJunta: false,
    puedePausarCancelarReabrir: false,
    puedeAmpliarFabricacion: false,
    puedeGestionarAduana: false,
    puedeRegistrarIncidenciasBodega: false,
    puedeCierreFinal: true,
    puedeEditarFechas: false,
    puedeGestionarUsuarios: false,
    puedeConfigurarPlazos: false,
    puedeVerAuditoria: false,
    puedeVerReportes: false,
    puedeDescargarArchivos: true,
    puedeSubirArchivos: true,
  },
  consulta: {
    rol: 'consulta',
    nombreRol: 'Consulta Externa',
    descripcion: 'Solo lectura del tablero general y seguimiento. Sin acciones ni descargas de archivos.',
    modulosPermitidos: [
      'inicio',
      'rastreo',
      'pedidos',
      'detalle_pedido',
      'ayuda',
    ],
    etapasOperativas: [],
    puedeCrearPedido: false,
    puedeAprobarJunta: false,
    puedePausarCancelarReabrir: false,
    puedeAmpliarFabricacion: false,
    puedeGestionarAduana: false,
    puedeRegistrarIncidenciasBodega: false,
    puedeCierreFinal: false,
    puedeEditarFechas: false,
    puedeGestionarUsuarios: false,
    puedeConfigurarPlazos: false,
    puedeVerAuditoria: false,
    puedeVerReportes: false,
    puedeDescargarArchivos: false,
    puedeSubirArchivos: false,
  },
};

/**
 * Valida si un usuario tiene acceso a un módulo específico
 */
export function canUserAccessModule(user: Usuario, module: AppModule): boolean {
  const perm = MATRIZ_PERMISOS[user.rol];
  return perm ? perm.modulosPermitidos.includes(module) : false;
}

/**
 * Valida si el usuario puede operar la etapa dada
 */
export function canUserOperateStage(user: Usuario, stageNumber: number): boolean {
  if (user.rol === 'admin') return true;
  if (user.rol === 'consulta') return false;
  const perm = MATRIZ_PERMISOS[user.rol];
  return perm ? perm.etapasOperativas.includes(stageNumber) : false;
}

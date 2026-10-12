import { UserRole, AppModule, Usuario, TipoFlujo } from '../types';

export function canUserOperateStage(usuario: Usuario, stageNum: number, flujo: TipoFlujo = 'A'): boolean {
  if (usuario.rol === 'admin') return true; // Mirian Franco (Admin) puede actuar en cualquier etapa
  if (flujo === 'A') {
    return usuario.etapasAsignadasA.includes(stageNum);
  } else {
    return usuario.etapasAsignadasB.includes(stageNum);
  }
}

export function canUserAccessModule(usuario: Usuario, module: AppModule): boolean {
  if (usuario.rol === 'admin') return true;

  switch (module) {
    case 'inicio':
    case 'rastreo':
    case 'pedidos':
    case 'detalle_pedido':
    case 'cronograma_meta':
    case 'gmail_alertas':
    case 'ayuda':
      return true;

    case 'bodegas':
      return usuario.rol === 'logistica';

    case 'usuarios':
    case 'auditoria':
      return false; // Solo admin accede a auditoría y usuarios

    default:
      return true;
  }
}

export function canUserCreateOrder(usuario: Usuario, flujo: TipoFlujo): boolean {
  if (usuario.rol === 'admin') return true;
  if (flujo === 'A') {
    return false; // Solo admin crea flujo A
  } else {
    // Flujo B es propuesto desde China por David
    return usuario.rol === 'compras_china';
  }
}

export function getRoleBadgeColor(rol: UserRole): string {
  switch (rol) {
    case 'admin':
      return 'bg-purple-100 text-purple-800 border-purple-200';
    case 'asistente_compras':
      return 'bg-emerald-100 text-emerald-800 border-emerald-200';
    case 'marketing':
      return 'bg-cyan-100 text-cyan-800 border-cyan-200';
    case 'compras_china':
      return 'bg-rose-100 text-rose-800 border-rose-200';
    case 'logistica':
      return 'bg-amber-100 text-amber-800 border-amber-200';
    case 'asistente_china':
      return 'bg-blue-100 text-blue-800 border-blue-200';
    case 'consulta':
    default:
      return 'bg-slate-100 text-slate-700 border-slate-200';
  }
}

export function getRoleNameDisplay(rol: UserRole): string {
  switch (rol) {
    case 'admin':
      return 'Administrador General';
    case 'asistente_compras':
      return 'Asistente de Compras (Ecuador)';
    case 'marketing':
      return 'Marketing & Codificación';
    case 'compras_china':
      return 'Compras China (Gestión y Embarque)';
    case 'logistica':
      return 'Jefe de Logística & Bodega';
    case 'asistente_china':
      return 'Asistente de Compras en China';
    case 'consulta':
    default:
      return 'Consulta / Auditor';
  }
}

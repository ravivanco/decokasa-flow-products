import { describe, expect, it } from 'vitest';
import { ErrorApi, CODIGO_RESPUESTA_INVALIDA, CODIGO_SIN_CONEXION } from '@/api/cliente';
import { ana } from '@/test/apiFicticia';
import { formatearFecha, formatearFechaHora, formatearFechaCorta, formatearTamano, nombreRol, podriaActuarEn, mensajeDeErrorPedido } from './formato';

describe('Presentación de fechas y datos ausentes del servidor', () => {
  it('conserva la fecha civil y muestra la hora en Ecuador aunque el instante sea del día siguiente en UTC', () => {
    expect(formatearFecha('2026-10-19')).toBe('19/10/2026');
    expect(formatearFechaHora('2026-10-20T02:30:00Z')).toMatch(/19.*oct.*2026.*21:30/);
    expect(formatearFechaCorta('2026-10-20T02:30:00Z')).toMatch(/19.*oct/);
  });
  it.each([null, undefined, '', 'sin-fecha'])('una fecha ausente o ilegible %s no inventa un plazo', valor => {
    expect(formatearFecha(valor)).toBe('—');
    expect(formatearFechaHora(valor)).toBe('—');
    expect(formatearFechaCorta(valor)).toBe('—');
  });
  it('no trata como fecha civil una cadena con hora', () => expect(formatearFecha('2026-10-19T21:30:00Z')).toBe('—'));
  it.each([[0,'0 B'],[1023,'1023 B'],[1024,'1.0 KB'],[1048576,'1.0 MB'],[104857600,'100.0 MB']] as const)
  ('el tamaño recibido de %i bytes se presenta como %s', (bytes, esperado) => expect(formatearTamano(bytes)).toBe(esperado));
  it('identifica el evento automático y conserva el código de un rol aún desconocido', () => {
    expect(nombreRol(null)).toBe('Automática');expect(nombreRol('rol_futuro')).toBe('rol_futuro');
  });
});

describe('Pistas de permisos: roles múltiples y asignación vigente, sin sustituir la autorización del servidor', () => {
  const asignado = { id: ana.id, nombre: ana.nombre, roles: ['logistica'] };
  it.each([
    {nombre:'consulta sola',roles:['consulta'],responsable:'logistica',asignadoA:null,vigente:true,esperado:false},
    {nombre:'consulta no resta logística',roles:['consulta','logistica'],responsable:'logistica',asignadoA:null,vigente:true,esperado:true},
    {nombre:'asistente China no resta Compras China',roles:['asistente_china','compras_china'],responsable:'compras_china',asignadoA:null,vigente:true,esperado:true},
    {nombre:'asignación propia vigente',roles:['logistica'],responsable:'logistica',asignadoA:asignado,vigente:true,esperado:true},
    {nombre:'asignación propia vencida',roles:['logistica'],responsable:'logistica',asignadoA:asignado,vigente:false,esperado:false},
    {nombre:'asignación a otra persona',roles:['logistica'],responsable:'logistica',asignadoA:{id:'otra',nombre:'Otra ficticia',roles:['logistica']},vigente:true,esperado:false},
    {nombre:'tener asignación no concede rol',roles:['consulta'],responsable:'logistica',asignadoA:asignado,vigente:true,esperado:false},
    {nombre:'etapa automática',roles:['logistica'],responsable:null,asignadoA:null,vigente:true,esperado:false},
    {nombre:'admin con asignación ajena',roles:['admin'],responsable:'logistica',asignadoA:{id:'otra',nombre:'Otra ficticia',roles:['logistica']},vigente:false,esperado:true},
  ])('$nombre', ({roles,responsable,asignadoA,vigente,esperado}) => {
    expect(podriaActuarEn({rolResponsable:responsable,asignadoA,asignacionVigente:vigente},{...ana,roles})).toBe(esperado);
  });
});

describe('Mensajes accionables sin exponer el detalle técnico', () => {
  const general='No se pudo completar la operación.';
  it.each([
    ['TRANSICION_NO_PERMITIDA','Esta acción ya no está permitida en la etapa actual del pedido.'],
    ['REQUISITO_FALTANTE','Faltan requisitos de la etapa para completar la acción.'],
    ['ARCHIVO_NO_DISPONIBLE','El archivo no está disponible o está dañado, y el servidor no lo entrega. Avisa al administrador.'],
    ['NO_ENCONTRADO','El pedido o el archivo ya no existe.'],
    [CODIGO_RESPUESTA_INVALIDA,'La respuesta del servidor no tiene la forma esperada. Avisa al equipo técnico.'],
    ['VALIDACION','Revisa los datos e intenta de nuevo.'],
    ['ERROR_INTERNO',general],
  ])('%s ofrece una indicación segura', (codigo, texto) => {
    expect(mensajeDeErrorPedido(new ErrorApi(400,codigo,'Detalle técnico ficticio'),general)).toBe(texto);
  });
  it('identifica los requisitos faltantes y los errores por campo aprobados por el contrato', () => {
    expect(mensajeDeErrorPedido(new ErrorApi(400,'REQUISITO_FALTANTE','Privado',[],['Observación','Bodega']),general)).toBe('Falta: Observación, Bodega.');
    expect(mensajeDeErrorPedido(new ErrorApi(400,'VALIDACION','Privado',[{campo:'fecha',mensaje:'Fecha inválida.'},{campo:'observacion',mensaje:'Escribe una observación.'}]),general)).toBe('Fecha inválida. Escribe una observación.');
  });
  it('un fallo de red o una excepción desconocida no revela su mensaje interno', () => {
    expect(mensajeDeErrorPedido(new ErrorApi(0,CODIGO_SIN_CONEXION,'Privado'),general)).toMatch(/No hay conexión/);
    expect(mensajeDeErrorPedido(new Error('Privado'),general)).toBe(general);
  });
});

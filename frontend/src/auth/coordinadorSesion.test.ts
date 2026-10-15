import { describe, expect, it, vi } from 'vitest';
import { ErrorApi } from '@/api/cliente';
import type { Usuario } from '@/api/esquemas';
import {
  bloqueoEntrePestanas,
  bloqueoLocal,
  crearCoordinadorSesion,
  type Canal,
  type MensajeSesion,
  type SesionAbierta,
} from './coordinadorSesion';

// A3 (revisión de seguridad del paso 1.7): la cookie de renovación se rota en cada uso y reutilizar una ya rotada
// revoca toda la sesión (A2, sin margen). Varias pestañas comparten la misma cookie, así que las renovaciones se
// coordinan: una sola a la vez dentro de la pestaña y entre pestañas. Ningún token sale de la memoria de su pestaña.

const usuario: Usuario = {
  id: '00000000-0000-4000-8000-000000000001',
  nombre: 'Ana Prueba',
  correo: 'ana@ejemplo.test',
  usuario: 'anaprueba',
  telefono: '+593990000001',
  roles: ['compras_china'],
  paises: ['EC'],
  activo: true,
  version: 0,
};

/**
 * Servidor ficticio con la regla del backend: cada renovación rota la cookie; si llega una cookie que ya no es la
 * vigente, revoca toda la familia (401 en adelante). El navegador comparte la cookie entre pestañas: cada petición
 * envía la que haya en ese momento, y la respuesta la reemplaza.
 */
function servidorFicticio() {
  const navegador = { cookie: 'c0' };
  const servidor = { vigente: 'c0', rotaciones: 0, revocada: false, recibidas: [] as string[] };
  async function renovarEnServidor(): Promise<SesionAbierta> {
    const enviada = navegador.cookie;
    servidor.recibidas.push(enviada);
    await new Promise((resolver) => setTimeout(resolver, 5));
    if (servidor.revocada || enviada !== servidor.vigente) {
      servidor.revocada = true;
      throw new ErrorApi(401, 'NO_AUTENTICADO', 'La sesión terminó.');
    }
    servidor.rotaciones++;
    servidor.vigente = `c${servidor.rotaciones}`;
    navegador.cookie = servidor.vigente;
    return { tokenAcceso: `t${servidor.rotaciones}`, usuario };
  }
  return { renovarEnServidor, servidor };
}

/** Canal entre pestañas en memoria: cada pestaña recibe lo que publican las demás, no lo propio. */
function busFicticio() {
  const oyentes = new Set<(mensaje: MensajeSesion) => void>();
  return (): Canal => {
    let propio: ((mensaje: MensajeSesion) => void) | null = null;
    return {
      publicar: (mensaje) => oyentes.forEach((oyente) => oyente !== propio && oyente(mensaje)),
      alRecibir: (oyente) => {
        propio = oyente;
        oyentes.add(oyente);
        return () => oyentes.delete(oyente);
      },
      cerrar: () => {},
    };
  };
}

describe('Coordinación de la sesión entre renovaciones y pestañas (A3)', () => {
  it('un login remoto retira inmediatamente token y señal anteriores y avisa antes de recuperar otra cuenta', async () => {
    const canalDe = busFicticio();
    let responder!: (sesion: SesionAbierta) => void;
    const renovar = vi.fn(() => new Promise<SesionAbierta>(resolve => { responder = resolve; }));
    const invalidar = vi.fn();
    const a = crearCoordinadorSesion({ renovarEnServidor: renovar, bloqueo: bloqueoLocal(), canal: canalDe(), alInvalidar: invalidar });
    const b = crearCoordinadorSesion({ renovarEnServidor: renovar, bloqueo: bloqueoLocal(), canal: canalDe() });
    a.abrir({ tokenAcceso: 'anterior', usuario });
    const anterior = a.senal(); const generacion = a.generacion();
    const desconectar = a.conectar();
    const otra = { ...usuario, id: 'otra-cuenta', nombre: 'Otra Prueba', roles: ['marketing'] };
    b.abrir({ tokenAcceso: 'no-viaja-por-el-canal', usuario: otra });
    expect(a.token()).toBeNull();
    expect(a.estado()).toEqual({ fase: 'cargando' });
    expect(anterior.aborted).toBe(true);
    expect(a.generacion()).toBeGreaterThan(generacion);
    expect(a.aviso()).toContain('La sesión cambió en otra pestaña');
    expect(invalidar).toHaveBeenCalledTimes(2);
    await vi.waitFor(() => expect(renovar).toHaveBeenCalledTimes(1));
    responder({ tokenAcceso: 'vigente', usuario: otra });
    await vi.waitFor(() => expect(a.estado()).toEqual({ fase: 'abierta', usuario: otra }));
    expect(a.token()).toBe('vigente');
    desconectar();
  });

  it('una renovación tardía no vuelve a abrir la sesión después del cierre', async () => {
    let responder!: (sesion: SesionAbierta) => void;
    const renovar = vi.fn(() => new Promise<SesionAbierta>(resolve => { responder = resolve; }));
    const a = crearCoordinadorSesion({ renovarEnServidor: renovar, bloqueo: bloqueoLocal() });
    a.abrir({ tokenAcceso: 'anterior', usuario });
    const pendiente = a.renovar('anterior');
    await vi.waitFor(() => expect(renovar).toHaveBeenCalledTimes(1));
    a.cerrar();
    responder({ tokenAcceso: 'respuesta-vieja', usuario });
    await expect(pendiente).resolves.toBeNull();
    expect(a.estado()).toEqual({ fase: 'anonima' });
    expect(a.token()).toBeNull();
  });

  it('si la renovación descubre otra identidad sin aviso también invalida y comunica el cambio', async () => {
    const otra = { ...usuario, id: 'otra-cuenta', roles: ['marketing'] };
    const a = crearCoordinadorSesion({ renovarEnServidor: async () => ({ tokenAcceso: 'vigente', usuario: otra }), bloqueo: bloqueoLocal() });
    a.abrir({ tokenAcceso: 'anterior', usuario });
    const senalAnterior = a.senal(); const generacion = a.generacion();
    await a.renovar('anterior');
    expect(a.generacion()).toBeGreaterThan(generacion);
    expect(senalAnterior.aborted).toBe(true);
    expect(a.estado()).toEqual({ fase: 'abierta', usuario: otra });
    expect(a.aviso()).toContain('La sesión cambió');
  });

  it.each([0, 503])('un fallo temporal %s al renovar conserva la sesión y permite intentar de nuevo', async (status) => {
    const renovar = vi.fn().mockRejectedValueOnce(new ErrorApi(status, status ? 'DESCONOCIDO' : 'SIN_CONEXION', 'fallo'))
      .mockResolvedValue({ tokenAcceso: 'nuevo', usuario });
    const pestana = crearCoordinadorSesion({ renovarEnServidor: renovar, bloqueo: bloqueoLocal() });
    pestana.abrir({ tokenAcceso: 'anterior', usuario });
    await expect(pestana.renovar('anterior')).rejects.toMatchObject({ status });
    expect(pestana.token()).toBe('anterior');
    expect(pestana.estado()).toEqual({ fase: 'abierta', usuario });
    await expect(pestana.renovar('anterior')).resolves.toBe('nuevo');
    expect(renovar).toHaveBeenCalledTimes(2);
  });

  it('al recuperar sin conexión queda anónima y no inventa una sesión', async () => {
    const pestana = crearCoordinadorSesion({ bloqueo: bloqueoLocal(),
      renovarEnServidor: () => Promise.reject(new ErrorApi(0, 'SIN_CONEXION', 'fallo')) });
    await expect(pestana.renovar(null)).resolves.toBeNull();
    expect(pestana.estado()).toEqual({ fase: 'anonima' });
  });

  it('en una pestaña, varias renovaciones simultáneas hacen una sola llamada y comparten el token', async () => {
    const { renovarEnServidor, servidor } = servidorFicticio();
    const pestana = crearCoordinadorSesion({ renovarEnServidor, bloqueo: bloqueoLocal() });

    const tokens = await Promise.all([pestana.renovar(null), pestana.renovar(null), pestana.renovar(null)]);

    expect(tokens).toEqual(['t1', 't1', 't1']);
    expect(servidor.recibidas).toEqual(['c0']);
    expect(pestana.estado()).toMatchObject({ fase: 'abierta' });
  });

  it('si la pestaña ya renovó, una llamada que falló con el token anterior usa el nuevo sin volver a renovar', async () => {
    const { renovarEnServidor, servidor } = servidorFicticio();
    const pestana = crearCoordinadorSesion({ renovarEnServidor, bloqueo: bloqueoLocal() });
    await pestana.renovar(null);

    expect(await pestana.renovar('token-que-ya-no-esta')).toBe('t1');
    expect(servidor.recibidas).toHaveLength(1);
  });

  it('dos pestañas que renuevan a la vez esperan su turno y nunca envían una cookie ya rotada', async () => {
    const { renovarEnServidor, servidor } = servidorFicticio();
    const bloqueoCompartido = bloqueoLocal(); // como navigator.locks: uno para todas las pestañas
    const pestanaA = crearCoordinadorSesion({ renovarEnServidor, bloqueo: bloqueoCompartido });
    const pestanaB = crearCoordinadorSesion({ renovarEnServidor, bloqueo: bloqueoCompartido });

    const [tokenA, tokenB] = await Promise.all([pestanaA.renovar(null), pestanaB.renovar(null)]);

    expect(tokenA).not.toBeNull();
    expect(tokenB).not.toBeNull();
    expect(servidor.recibidas).toEqual(['c0', 'c1']);
    expect(servidor.revocada).toBe(false);
  });

  it('control: sin bloqueo entre pestañas, la segunda reutiliza la cookie y el servidor revoca la sesión', async () => {
    const { renovarEnServidor, servidor } = servidorFicticio();
    const pestanaA = crearCoordinadorSesion({ renovarEnServidor, bloqueo: bloqueoLocal() });
    const pestanaB = crearCoordinadorSesion({ renovarEnServidor, bloqueo: bloqueoLocal() });

    const tokens = await Promise.all([pestanaA.renovar(null), pestanaB.renovar(null)]);

    expect(tokens).toContain(null);
    expect(servidor.revocada).toBe(true);
  });

  it.each([401, 403])('una renovación rechazada con %s deja la sesión anónima sin token', async (status) => {
    const pestana = crearCoordinadorSesion({
      renovarEnServidor: () => Promise.reject(new ErrorApi(status, 'NO_AUTENTICADO', 'La sesión terminó.')),
      bloqueo: bloqueoLocal(),
    });

    expect(await pestana.renovar(null)).toBeNull();
    expect(pestana.estado()).toEqual({ fase: 'anonima' });
    expect(pestana.token()).toBeNull();
  });

  it('cerrar la sesión en una pestaña la cierra en las demás, sin enviar ningún token', async () => {
    const { renovarEnServidor } = servidorFicticio();
    const canalDe = busFicticio();
    const bloqueo = bloqueoLocal();
    const pestanaA = crearCoordinadorSesion({ renovarEnServidor, bloqueo, canal: canalDe() });
    const pestanaB = crearCoordinadorSesion({ renovarEnServidor, bloqueo, canal: canalDe() });
    const desconectarA = pestanaA.conectar();
    const desconectarB = pestanaB.conectar();
    const publicados: MensajeSesion[] = [];
    const canalEspia = canalDe();
    canalEspia.alRecibir((mensaje) => publicados.push(mensaje));
    await pestanaA.renovar(null);
    await pestanaB.renovar(null);

    pestanaA.cerrar();

    expect(pestanaB.estado()).toEqual({ fase: 'anonima' });
    expect(pestanaB.token()).toBeNull();
    expect(JSON.stringify(publicados)).not.toMatch(/t\d/);
    desconectarA();
    desconectarB();
  });

  it('entre pestañas usa navigator.locks con un nombre propio cuando el navegador lo ofrece', async () => {
    const pedir = vi.fn((_nombre: string, _opciones: object, tarea: () => Promise<unknown>) => tarea());
    const bloqueo = bloqueoEntrePestanas({ locks: { request: pedir } as unknown as LockManager });

    await expect(bloqueo.exclusivo(async () => 'hecho')).resolves.toBe('hecho');
    expect(pedir).toHaveBeenCalledWith('decokasa-renovacion', { mode: 'exclusive' }, expect.any(Function));
  });
});

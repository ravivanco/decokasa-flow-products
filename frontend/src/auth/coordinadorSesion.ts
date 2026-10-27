import { esErrorApi } from '@/api/cliente';
import type { Usuario } from '@/api/esquemas';

// Coordinador de la sesión de una pestaña (D3b y A3). Guarda el token de acceso solo en memoria y serializa las
// operaciones de sesión: la cookie se comparte y reutilizar una ya rotada revoca toda la sesión (A2, sin
// margen). Por eso:
//  - dentro de la pestaña, las renovaciones simultáneas comparten una sola llamada;
//  - entre pestañas, cada renovación espera su turno con un bloqueo del navegador (navigator.locks), así la siguiente
//    envía la cookie que dejó la anterior y nunca una ya rotada;
//  - si mientras esperaba ya llegó un token nuevo, no vuelve a renovar.
// Entre pestañas solo viajan avisos sin datos (cerrada, iniciada): ningún token sale de la memoria de su pestaña.

export interface SesionAbierta {
  tokenAcceso: string;
  usuario: Usuario;
}

export type EstadoSesion = { fase: 'cargando' } | { fase: 'anonima' } | { fase: 'abierta'; usuario: Usuario };

/** Exclusión mutua para las renovaciones. */
export interface Bloqueo {
  exclusivo<T>(tarea: () => Promise<T>): Promise<T>;
}

export type MensajeSesion = { tipo: 'cerrada' } | { tipo: 'iniciada' };

/** Canal de avisos entre pestañas (BroadcastChannel en el navegador). */
export interface Canal {
  publicar(mensaje: MensajeSesion): void;
  alRecibir(oyente: (mensaje: MensajeSesion) => void): () => void;
  cerrar(): void;
}

/** El servidor no renovó porque la sesión ya no es válida (y no por un fallo de red o del servidor). */
function esRechazoDeSesion(error: unknown): boolean {
  return esErrorApi(error) && (error.status === 401 || error.status === 403);
}

const NOMBRE_BLOQUEO = 'decokasa-renovacion';
const NOMBRE_CANAL = 'decokasa-sesion';

/** Bloqueo en memoria: una tarea detrás de otra. Sirve dentro de una pestaña y en las pruebas. */
export function bloqueoLocal(): Bloqueo {
  let cola: Promise<unknown> = Promise.resolve();
  return {
    exclusivo<T>(tarea: () => Promise<T>): Promise<T> {
      const resultado = cola.then(tarea, tarea);
      cola = resultado.catch(() => undefined);
      return resultado;
    },
  };
}

/** Bloqueo compartido por todas las pestañas del mismo origen; sin Web Locks, solo cubre la pestaña. */
export function bloqueoEntrePestanas(navegador: { locks?: LockManager } | undefined): Bloqueo {
  const locks = navegador?.locks;
  if (!locks) return bloqueoLocal();
  return {
    exclusivo: <T>(tarea: () => Promise<T>) =>
      locks.request(NOMBRE_BLOQUEO, { mode: 'exclusive' }, tarea) as Promise<T>,
  };
}

/** Canal entre pestañas del navegador; sin BroadcastChannel no hay avisos (cada pestaña sigue funcionando sola). */
export function canalDelNavegador(): Canal | undefined {
  if (typeof BroadcastChannel === 'undefined') return undefined;
  const canal = new BroadcastChannel(NOMBRE_CANAL);
  return {
    publicar: (mensaje) => canal.postMessage(mensaje),
    alRecibir: (oyente) => {
      const manejar = (evento: MessageEvent) => {
        const tipo = (evento.data as { tipo?: unknown } | null)?.tipo;
        if (tipo === 'cerrada' || tipo === 'iniciada') oyente({ tipo });
      };
      canal.addEventListener('message', manejar);
      return () => canal.removeEventListener('message', manejar);
    },
    cerrar: () => canal.close(),
  };
}

export interface OpcionesCoordinador {
  /** POST /auth/renovar con la cookie; lanza si el servidor no renueva. */
  renovarEnServidor: () => Promise<SesionAbierta>;
  bloqueo: Bloqueo;
  canal?: Canal;
  /** Retira sin esperar las consultas y mutaciones de la identidad anterior. */
  alInvalidar?: () => void;
}

export interface CoordinadorSesion {
  estado(): EstadoSesion;
  token(): string | null;
  generacion(): number;
  senal(): AbortSignal;
  aviso(): string | null;
  descartarAviso(): void;
  suscribir(oyente: () => void): () => void;
  /**
   * Pide un token nuevo. `tokenFallido` es el token con el que una llamada recibió 401 (o null al recuperar la
   * sesión): si ya hay otro distinto, se devuelve ese sin llamar al servidor. Devuelve null si la sesión terminó.
   * Con la sesión abierta, si el servidor no respondió (sin red, 5xx), lanza ese error y la sesión sigue abierta.
   */
  renovar(tokenFallido: string | null): Promise<string | null>;
  /** Guarda la sesión recién abierta con el login y avisa a las demás pestañas. */
  abrir(sesion: SesionAbierta): void;
  /** Login y cierre usan el mismo bloqueo entre pestañas que la renovación. */
  iniciar(tarea: () => Promise<SesionAbierta>): Promise<Usuario>;
  salir(tarea: () => Promise<unknown>): Promise<void>;
  /** Olvida la sesión de esta pestaña y avisa a las demás. */
  cerrar(): void;
  /** Escucha los avisos de las demás pestañas; devuelve cómo dejar de escuchar. */
  conectar(): () => void;
}

export function crearCoordinadorSesion({ renovarEnServidor, bloqueo, canal, alInvalidar }: OpcionesCoordinador): CoordinadorSesion {
  let estado: EstadoSesion = { fase: 'cargando' };
  let token: string | null = null;
  let generacion = 0;
  let controlador = new AbortController();
  let aviso: string | null = null;
  let enCurso: { generacion: number; promesa: Promise<string | null> } | null = null;
  const oyentes = new Set<() => void>();

  function cambiar(nuevo: EstadoSesion, nuevoToken: string | null) {
    estado = nuevo;
    token = nuevoToken;
    oyentes.forEach((oyente) => oyente());
  }

  function invalidar(nuevo: EstadoSesion = { fase: 'cargando' }) {
    generacion++;
    token = null;
    controlador.abort();
    controlador = new AbortController();
    enCurso = null;
    alInvalidar?.();
    cambiar(nuevo, null);
  }

  function olvidar() {
    invalidar({ fase: 'anonima' });
  }

  function guardar(sesion: SesionAbierta) {
    // También protege una renovación que descubre otra identidad aunque se haya perdido el aviso del canal.
    if (estado.fase === 'abierta' && estado.usuario.id !== sesion.usuario.id) {
      aviso = 'La sesión cambió. Se descartaron los datos y las acciones pendientes de la cuenta anterior.';
      invalidar();
    }
    cambiar({ fase: 'abierta', usuario: sesion.usuario }, sesion.tokenAcceso);
  }

  function recuperarCambio() {
    aviso = 'La sesión cambió en otra pestaña. Se descartaron los datos y las acciones pendientes de la cuenta anterior.';
    invalidar();
    // Se recupera la cookie vigente en el turno de esta pestaña; nunca se reutiliza el token anterior.
    void renovar(null);
  }

  function abrir(sesion: SesionAbierta) {
    invalidar();
    guardar(sesion);
    canal?.publicar({ tipo: 'iniciada' });
  }

  function renovar(tokenFallido: string | null): Promise<string | null> {
    if (enCurso?.generacion === generacion) return enCurso.promesa;
    if (tokenFallido !== null && token !== null && token !== tokenFallido) return Promise.resolve(token);

    const iniciadaEn = generacion;
    const promesa = bloqueo
      .exclusivo(async () => {
        if (iniciadaEn !== generacion) return null;
        // Mientras esperaba el turno pudo llegar un token nuevo (por ejemplo, un login): entonces no se renueva.
        if (tokenFallido !== null && token !== null && token !== tokenFallido) return token;
        try {
          // No se aborta una respuesta que rota cookies: se deja terminar y se descarta si ya cambió la sesión.
          const sesion = await renovarEnServidor();
          if (iniciadaEn !== generacion) return null;
          guardar(sesion);
          return token;
        } catch (error) {
          if (iniciadaEn !== generacion) return null;
          // 401 o 403: la cookie ya no sirve (vencida, revocada, reutilizada o sin Origin válido): la sesión terminó.
          // Con la sesión abierta, otro fallo (sin red, 5xx) no la cierra: se informa a quien pidió el token y la
          // siguiente llamada vuelve a intentarlo. Al recuperar la sesión no hay nada que conservar: se pide el login.
          if (esRechazoDeSesion(error) || estado.fase !== 'abierta') {
            olvidar();
            return null;
          }
          throw error;
        }
      })
      .finally(() => {
        if (enCurso?.promesa === promesa) enCurso = null;
      });
    enCurso = { generacion: iniciadaEn, promesa };
    return promesa;
  }

  return {
    estado: () => estado,
    token: () => token,
    generacion: () => generacion,
    senal: () => controlador.signal,
    aviso: () => aviso,
    descartarAviso() {
      aviso = null;
      // useSyncExternalStore necesita una nueva referencia para volver a pintar el aviso.
      cambiar({ ...estado }, token);
    },
    suscribir(oyente) {
      oyentes.add(oyente);
      return () => oyentes.delete(oyente);
    },
    renovar,
    abrir,
    iniciar(tarea) {
      return bloqueo.exclusivo(async () => {
        const iniciadaEn = generacion;
        const sesion = await tarea();
        if (iniciadaEn !== generacion) {
          // El login pudo cambiar la cookie, pero un cierre o aviso invalidó su intención mientras esperaba.
          // No reabre una identidad con esa respuesta atrasada: todas recuperan lo que haya quedado vigente.
          canal?.publicar({ tipo: 'iniciada' });
          recuperarCambio();
          throw new DOMException('La sesión cambió durante el inicio de sesión.', 'AbortError');
        }
        abrir(sesion);
        return sesion.usuario;
      });
    },
    async salir(tarea) {
      olvidar();
      canal?.publicar({ tipo: 'cerrada' });
      await bloqueo.exclusivo(async () => {
        try {
          await tarea();
        } finally {
          canal?.publicar({ tipo: 'cerrada' });
        }
      });
    },
    cerrar() {
      olvidar();
      canal?.publicar({ tipo: 'cerrada' });
    },
    conectar() {
      if (!canal) return () => {};
      return canal.alRecibir((mensaje) => {
        if (mensaje.tipo === 'cerrada') {
          olvidar();
        } else {
          // Incluso si ya estaba abierta: retirar primero identidad, token, formularios y cachés anteriores.
          recuperarCambio();
        }
      });
    },
  };
}

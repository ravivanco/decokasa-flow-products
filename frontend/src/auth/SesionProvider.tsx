import { useQueryClient, type QueryClient } from '@tanstack/react-query';
import { createContext, Fragment, useCallback, useContext, useEffect, useMemo, useState, useSyncExternalStore } from 'react';
import type { ReactNode } from 'react';
import { ErrorApi, llamar, type OpcionesLlamada } from '@/api/cliente';
import { esquemaRespuestaSesion, type Usuario } from '@/api/esquemas';
import { Alert, AlertDescription, AlertTitle } from '@/components/ui/alert';
import { Button } from '@/components/ui/button';
import {
  bloqueoEntrePestanas,
  canalDelNavegador,
  crearCoordinadorSesion,
  type CoordinadorSesion,
  type EstadoSesion,
} from './coordinadorSesion';

// Sesión del usuario (D3b, plan, tarea 18). El token de acceso vive solo en la memoria del coordinador; la cookie de
// renovación es HttpOnly y JavaScript no la lee. Al cargar se recupera con /auth/renovar; ante un 401 de la API se
// renueva una vez (coordinado, A3) y se repite la llamada.

interface ValorSesion {
  estado: EstadoSesion;
  /** Separa las cachés y las acciones de cada sesión, también si vuelve a entrar el mismo usuario. */
  sesionId: number;
  iniciarSesion(identificador: string, contrasena: string): Promise<Usuario>;
  cerrarSesion(): Promise<void>;
  /** Llama a la API con el token de acceso; ante un 401 renueva una vez y repite. */
  pedir<T>(ruta: string, opciones?: Omit<OpcionesLlamada<T>, 'token' | 'credenciales'>): Promise<T>;
}

const ContextoSesion = createContext<ValorSesion | null>(null);

function nuevoCoordinador(consultas: QueryClient): CoordinadorSesion {
  return crearCoordinadorSesion({
    renovarEnServidor: () =>
      llamar('/auth/renovar', { metodo: 'POST', credenciales: true, esquema: esquemaRespuestaSesion }),
    bloqueo: bloqueoEntrePestanas(typeof navigator === 'undefined' ? undefined : navigator),
    canal: canalDelNavegador(),
    alInvalidar: () => {
      void consultas.cancelQueries();
      consultas.clear();
    },
  });
}

const accesoLocalHabilitado = import.meta.env.DEV && import.meta.env.VITE_ACCESO_LOCAL === 'true';

const usuarioLocal: Usuario = {
  id: 'acceso-local',
  nombre: 'Acceso local',
  correo: 'local@decokasa.test',
  usuario: 'local',
  telefono: '0000000000',
  roles: ['admin'],
  paises: ['EC'],
  activo: true,
  version: 0,
};

export function SesionProvider({ children }: { children: ReactNode }) {
  const consultas = useQueryClient();
  const [coordinador] = useState(() => nuevoCoordinador(consultas));
  const estado = useSyncExternalStore(coordinador.suscribir, coordinador.estado);
  const sesionId = coordinador.generacion();
  const aviso = coordinador.aviso();

  useEffect(() => {
    const desconectar = coordinador.conectar();
    if (coordinador.estado().fase === 'cargando') {
      if (accesoLocalHabilitado) {
        coordinador.abrir({ tokenAcceso: 'solo-desarrollo-local', usuario: usuarioLocal });
      } else {
        void coordinador.renovar(null);
      }
    }
    return desconectar;
  }, [coordinador]);

  const iniciarSesion = useCallback(
    (identificador: string, contrasena: string) =>
      coordinador.iniciar(() => llamar('/auth/login', {
        metodo: 'POST',
        credenciales: true,
        cuerpo: { identificador, contrasena },
        esquema: esquemaRespuestaSesion,
      })),
    [coordinador],
  );

  const cerrarSesion = useCallback(async () => {
    if (accesoLocalHabilitado) {
      coordinador.cerrar();
      return;
    }
    // Retira el acceso inmediatamente; la operación sobre la cookie espera el turno de las demás pestañas.
    await coordinador.salir(() => llamar('/auth/cerrar-sesion', { metodo: 'POST', credenciales: true }));
  }, [coordinador]);

  const pedir = useCallback(
    async <T,>(ruta: string, opciones: Omit<OpcionesLlamada<T>, 'token' | 'credenciales'> = {}) => {
      function exigirSesionVigente() {
        if (sesionId !== coordinador.generacion() || coordinador.estado().fase !== 'abierta') {
          throw new DOMException('Se descartó una operación de la sesión anterior.', 'AbortError');
        }
      }
      exigirSesionVigente();
      const signal = opciones.signal
        ? AbortSignal.any([opciones.signal, coordinador.senal()]) : coordinador.senal();
      async function enviar(token: string | null) {
        exigirSesionVigente();
        signal.throwIfAborted();
        const resultado = await llamar<T>(ruta, { ...opciones, token, signal });
        exigirSesionVigente();
        signal.throwIfAborted();
        return resultado;
      }
      const token = coordinador.token();
      try {
        return await enviar(token);
      } catch (error) {
        exigirSesionVigente();
        signal.throwIfAborted();
        if (!(error instanceof ErrorApi) || error.status !== 401) throw error;
        const nuevo = await coordinador.renovar(token);
        exigirSesionVigente();
        if (!nuevo) throw error;
        // Nunca se repite una lectura o escritura anterior con la identidad que acaba de entrar en otra pestaña.
        return enviar(nuevo);
      }
    },
    [coordinador, sesionId],
  );

  const valor = useMemo(
    () => ({ estado, sesionId, iniciarSesion, cerrarSesion, pedir }),
    [estado, sesionId, iniciarSesion, cerrarSesion, pedir],
  );
  return (
    <ContextoSesion.Provider value={valor}>
      {aviso && (
        <div className="p-4">
          <Alert>
            <AlertTitle>Cambio de sesión</AlertTitle>
            <AlertDescription className="flex flex-col items-start gap-3">
              <p>{aviso}</p>
              <p>{estado.fase === 'abierta'
                ? `Ahora estás usando la cuenta de ${estado.usuario.nombre}. Revisa la cuenta antes de continuar.`
                : estado.fase === 'cargando'
                  ? 'Estamos recuperando la sesión vigente.'
                  : 'No se pudo recuperar una sesión vigente. Vuelve a iniciar sesión.'}</p>
              {estado.fase !== 'cargando' && (
                <Button variant="outline" size="sm" onClick={() => coordinador.descartarAviso()}>Entendido</Button>
              )}
            </AlertDescription>
          </Alert>
        </div>
      )}
      {/* Un cambio de sesión descarta también formularios, diálogos y estado local de las rutas anteriores. */}
      <Fragment key={sesionId}>{children}</Fragment>
    </ContextoSesion.Provider>
  );
}

export function useSesion(): ValorSesion {
  const valor = useContext(ContextoSesion);
  if (!valor) throw new Error('useSesion debe usarse dentro de SesionProvider.');
  return valor;
}

/** Usuario con sesión abierta; solo para pantallas dentro de RutaProtegida. */
export function useUsuarioActual(): Usuario {
  const { estado } = useSesion();
  if (estado.fase !== 'abierta') throw new Error('No hay sesión abierta.');
  return estado.usuario;
}

export function esAdmin(usuario: Usuario): boolean {
  return usuario.roles.includes('admin');
}

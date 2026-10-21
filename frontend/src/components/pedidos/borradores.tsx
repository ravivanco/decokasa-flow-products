import { CopyIcon, FileWarningIcon, Trash2Icon } from 'lucide-react';
import { createContext, type ReactNode, useCallback, useContext, useMemo, useState } from 'react';
import { toast } from 'sonner';
import type { PedidoDetalle } from '@/api/pedidos';
import { Alert, AlertDescription, AlertTitle } from '@/components/ui/alert';
import { Button } from '@/components/ui/button';
import { Textarea } from '@/components/ui/textarea';
import { codigoEtapa } from './formato';

// Borradores del detalle del pedido (paso 5.5, corrección del pendiente de 5.4). El texto sin enviar de una
// observación o de un motivo del retraso vive aquí, fuera del panel de la etapa, ligado a la etapa y a la entrada en la
// que se escribió. Si una respuesta tardía o un cambio de otra persona cambia la etapa o su entrada, el panel se
// vuelve a montar pero el borrador no se pierde: se muestra como conflicto. Nunca se envía solo ni se pasa a la
// entrada nueva (el servidor lo rechazaría, P-18). Vive en memoria del detalle: no se guarda en el navegador y se
// descarta al salir del pedido o al cambiar de sesión.

export type TipoBorrador = 'observacion' | 'motivo';

export interface Borrador {
  tipo: TipoBorrador;
  etapa: number;
  entradaId: string;
  texto: string;
}

interface ValorBorradores {
  borradores: Borrador[];
  guardar: (borrador: Borrador) => void;
  descartar: (tipo: TipoBorrador, etapa: number, entradaId: string) => void;
}

const ContextoBorradores = createContext<ValorBorradores | null>(null);

const clave = (tipo: TipoBorrador, etapa: number, entradaId: string) => `${tipo}:${etapa}:${entradaId}`;

export function ProveedorBorradores({ children }: { children: ReactNode }) {
  const [mapa, setMapa] = useState<Record<string, Borrador>>({});
  const guardar = useCallback((borrador: Borrador) => {
    setMapa((actual) => ({ ...actual, [clave(borrador.tipo, borrador.etapa, borrador.entradaId)]: borrador }));
  }, []);
  const descartar = useCallback((tipo: TipoBorrador, etapa: number, entradaId: string) => {
    setMapa((actual) => {
      const llave = clave(tipo, etapa, entradaId);
      if (!(llave in actual)) return actual;
      const siguiente = { ...actual };
      delete siguiente[llave];
      return siguiente;
    });
  }, []);
  const valor = useMemo(() => ({ borradores: Object.values(mapa), guardar, descartar }), [mapa, guardar, descartar]);
  return <ContextoBorradores.Provider value={valor}>{children}</ContextoBorradores.Provider>;
}

/**
 * Texto de un formulario de la entrada actual. Con proveedor, sobrevive a que el panel se vuelva a montar; sin él (por
 * ejemplo, en una prueba aislada), es estado local como antes.
 *
 * @param inicial valor mostrado mientras la persona no escriba (el motivo vigente, por ejemplo)
 */
export function useBorrador(tipo: TipoBorrador, etapa: number, entradaId: string, inicial = '') {
  const contexto = useContext(ContextoBorradores);
  const [local, setLocal] = useState<string | null>(null);
  const guardado = contexto?.borradores.find(
    (borrador) => borrador.tipo === tipo && borrador.etapa === etapa && borrador.entradaId === entradaId,
  );
  const texto = contexto ? (guardado?.texto ?? inicial) : (local ?? inicial);
  const cambiar = (nuevoTexto: string) => {
    if (contexto) contexto.guardar({ tipo, etapa, entradaId, texto: nuevoTexto });
    else setLocal(nuevoTexto);
  };
  const descartar = () => {
    if (contexto) contexto.descartar(tipo, etapa, entradaId);
    else setLocal(null);
  };
  return { texto, cambiar, descartar };
}

const NOMBRE: Record<TipoBorrador, string> = { observacion: 'observación', motivo: 'motivo del retraso' };

/**
 * Borradores con texto cuya etapa o entrada ya no es la actual, o cuya observación ya no se puede publicar porque la
 * persona dejó de tener acciones en la etapa. Se informan con su texto para copiarlo; no se envían.
 */
export function BorradoresEnConflicto({ pedido }: { pedido: PedidoDetalle }) {
  const contexto = useContext(ContextoBorradores);
  if (!contexto) return null;
  const actual = pedido.lineaDeTiempo.find((etapa) => etapa.numero === pedido.etapaActual);
  const enConflicto = contexto.borradores.filter((borrador) => {
    if (borrador.texto.trim() === '') return false;
    const vigente =
      !pedido.finalizado && borrador.etapa === pedido.etapaActual && borrador.entradaId === actual?.entradaId;
    if (!vigente) return true;
    return borrador.tipo === 'observacion' && pedido.accionesDisponibles.length === 0;
  });

  return (
    <div aria-live="polite" className="flex flex-col gap-3 empty:hidden">
      {enConflicto.map((borrador) => (
        <Alert
          key={`${borrador.tipo}:${borrador.etapa}:${borrador.entradaId}`}
          role="note"
          className="border-estado-aviso bg-estado-aviso-fondo"
        >
          <FileWarningIcon />
          <AlertTitle>
            Borrador sin enviar: {NOMBRE[borrador.tipo]} de {codigoEtapa(pedido.tipoFlujo, borrador.etapa)}
          </AlertTitle>
          <AlertDescription className="flex w-full flex-col gap-3">
            <p>
              La etapa cambió mientras escribías (otra persona actuó, la etapa volvió a entrar o ya no puedes intervenir
              en ella). Tu texto no se envió y no se pasó a la entrada nueva. Cópialo si lo necesitas y descártalo.
            </p>
            <Textarea
              readOnly
              value={borrador.texto}
              aria-label={`Texto sin enviar: ${NOMBRE[borrador.tipo]}`}
              className="bg-card"
            />
            <div className="flex flex-wrap gap-2">
              <Button
                type="button"
                variant="outline"
                size="sm"
                onClick={() => {
                  if (!navigator.clipboard) {
                    toast.error('No se pudo copiar: selecciona el texto y cópialo a mano.');
                    return;
                  }
                  navigator.clipboard.writeText(borrador.texto).then(
                    () => toast.success('Texto copiado.'),
                    () => toast.error('No se pudo copiar: selecciona el texto y cópialo a mano.'),
                  );
                }}
              >
                <CopyIcon data-icon="inline-start" />
                Copiar texto
              </Button>
              <Button
                type="button"
                variant="ghost"
                size="sm"
                onClick={() => contexto.descartar(borrador.tipo, borrador.etapa, borrador.entradaId)}
              >
                <Trash2Icon data-icon="inline-start" />
                Descartar borrador
              </Button>
            </div>
          </AlertDescription>
        </Alert>
      ))}
    </div>
  );
}

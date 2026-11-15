import { LockIcon, UserMinusIcon, UserPlusIcon } from 'lucide-react';
import { useState } from 'react';
import { useMutation, useQuery, useQueryClient } from '@tanstack/react-query';
import { z } from 'zod';
import { useUsuarios } from '@/api/consultas';
import { mensajeDeError } from '@/api/mensajes';
import { useSesion } from '@/auth/SesionProvider';
import { Alert, AlertDescription } from '@/components/ui/alert';
import { Badge } from '@/components/ui/badge';
import { Button } from '@/components/ui/button';
import { Dialog, DialogContent, DialogDescription, DialogHeader, DialogTitle } from '@/components/ui/dialog';
import { Select, SelectContent, SelectGroup, SelectItem, SelectTrigger, SelectValue } from '@/components/ui/select';
import { formatearFechaHora } from './formato';

// V37: documentos confidenciales de Aduana. Admin ve quién los puede abrir en este pedido y da o quita acceso. El
// servidor decide (solo Admin vigente) y aplica la misma regla en la lista, la descarga, el checklist y el asistente.

const esquemaConAcceso = z.array(z.object({
  usuarioId: z.string(),
  nombre: z.string(),
  porRol: z.boolean(),
  otorgadoPor: z.string().nullish(),
  otorgadoEn: z.string().nullish(),
}));

const esquemaResumen = z.object({
  puedeVer: z.boolean(),
  etapas: z.array(z.object({ etapa: z.number().int(), cantidad: z.number().int() })),
});

/** Cuántos confidenciales hay por etapa y si la persona los puede ver (cualquiera que vea el pedido). */
export function useResumenConfidencial(pedidoId: string) {
  const { pedir, sesionId } = useSesion();
  return useQuery({
    queryKey: ['sesion', sesionId, 'pedidos', pedidoId, 'acceso-confidencial', 'resumen'] as const,
    queryFn: ({ signal }) => pedir(`/pedidos/${encodeURIComponent(pedidoId)}/acceso-confidencial/resumen`, { esquema: esquemaResumen, signal }),
    staleTime: 15_000,
  });
}

function useAccesoConfidencial(pedidoId: string, habilitada: boolean) {
  const { pedir, sesionId } = useSesion();
  const consultas = useQueryClient();
  const clave = ['sesion', sesionId, 'pedidos', pedidoId, 'acceso-confidencial'] as const;
  const lista = useQuery({
    queryKey: clave,
    queryFn: ({ signal }) => pedir(`/pedidos/${encodeURIComponent(pedidoId)}/acceso-confidencial`, { esquema: esquemaConAcceso, signal }),
    enabled: habilitada,
  });
  const actualizar = () => consultas.invalidateQueries({ queryKey: clave });
  const otorgar = useMutation({
    mutationFn: (usuarioId: string) =>
      pedir(`/pedidos/${encodeURIComponent(pedidoId)}/acceso-confidencial`, { metodo: 'POST', cuerpo: { usuarioId } }),
    onSettled: actualizar,
  });
  const revocar = useMutation({
    mutationFn: (usuarioId: string) =>
      pedir(`/pedidos/${encodeURIComponent(pedidoId)}/acceso-confidencial/${encodeURIComponent(usuarioId)}`, { metodo: 'DELETE' }),
    onSettled: actualizar,
  });
  return { lista, otorgar, revocar };
}

export function AccesoConfidencial({ pedidoId }: { pedidoId: string }) {
  const [abierto, setAbierto] = useState(false);
  const [elegido, setElegido] = useState('');
  const [error, setError] = useState<string | null>(null);
  const { lista, otorgar, revocar } = useAccesoConfidencial(pedidoId, abierto);
  const usuarios = useUsuarios();
  const conAcceso = new Set((lista.data ?? []).map((p) => p.usuarioId));
  const candidatos = (usuarios.data ?? []).filter((u) => u.activo && !conAcceso.has(u.id));

  async function dar() {
    if (!elegido) return;
    setError(null);
    try {
      await otorgar.mutateAsync(elegido);
      setElegido('');
    } catch (fallo) {
      setError(mensajeDeError(fallo, 'No se pudo dar el acceso.'));
    }
  }

  return (
    <>
      <Button type="button" variant="outline" size="sm" onClick={() => setAbierto(true)}>
        <LockIcon data-icon="inline-start" />
        Acceso a confidenciales
      </Button>
      <Dialog open={abierto} onOpenChange={setAbierto}>
        <DialogContent className="sm:max-w-lg">
          <DialogHeader>
            <DialogTitle>Documentos confidenciales</DialogTitle>
            <DialogDescription>Los documentos del checklist de Aduana. Solo los ven estas personas.</DialogDescription>
          </DialogHeader>
          {error && <Alert variant="destructive"><AlertDescription>{error}</AlertDescription></Alert>}
          <ul className="flex max-h-72 flex-col divide-y overflow-y-auto rounded-md border">
            {(lista.data ?? []).map((persona) => (
              <li key={persona.usuarioId} className="flex items-center justify-between gap-3 px-3 py-2">
                <div className="flex min-w-0 flex-col">
                  <span className="truncate text-sm font-medium">{persona.nombre}</span>
                  {!persona.porRol && (
                    <span className="text-xs text-muted-foreground">
                      Acceso dado por {persona.otorgadoPor ?? 'Admin'}{persona.otorgadoEn ? ` · ${formatearFechaHora(persona.otorgadoEn)}` : ''}
                    </span>
                  )}
                </div>
                {persona.porRol ? (
                  <Badge variant="outline">Por rol</Badge>
                ) : (
                  <Button type="button" variant="ghost" size="sm" disabled={revocar.isPending}
                    onClick={() => revocar.mutate(persona.usuarioId, { onError: (fallo) => setError(mensajeDeError(fallo, 'No se pudo quitar el acceso.')) })}>
                    <UserMinusIcon data-icon="inline-start" />
                    Quitar
                  </Button>
                )}
              </li>
            ))}
          </ul>
          <div className="flex gap-2">
            <Select value={elegido} onValueChange={setElegido}>
              <SelectTrigger className="flex-1" aria-label="Persona a la que dar acceso">
                <SelectValue placeholder="Elige una persona" />
              </SelectTrigger>
              <SelectContent>
                <SelectGroup>
                  {candidatos.map((u) => <SelectItem key={u.id} value={u.id}>{u.nombre}</SelectItem>)}
                </SelectGroup>
              </SelectContent>
            </Select>
            <Button type="button" disabled={!elegido || otorgar.isPending} onClick={() => void dar()}>
              <UserPlusIcon data-icon="inline-start" />
              Dar acceso
            </Button>
          </div>
        </DialogContent>
      </Dialog>
    </>
  );
}

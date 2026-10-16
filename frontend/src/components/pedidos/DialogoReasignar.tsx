import { CircleAlertIcon, LoaderIcon } from 'lucide-react';
import { type FormEvent, useId, useState } from 'react';
import { toast } from 'sonner';
import { esErrorApi } from '@/api/cliente';
import { useUsuarios } from '@/api/consultas';
import { type EtapaEnLinea, type PedidoDetalle, useReasignarEtapa } from '@/api/pedidos';
import { Alert, AlertDescription } from '@/components/ui/alert';
import { Button } from '@/components/ui/button';
import {
  Dialog,
  DialogContent,
  DialogDescription,
  DialogFooter,
  DialogHeader,
  DialogTitle,
} from '@/components/ui/dialog';
import { Field, FieldDescription, FieldError, FieldLabel } from '@/components/ui/field';
import { Select, SelectContent, SelectGroup, SelectItem, SelectTrigger, SelectValue } from '@/components/ui/select';
import { Textarea } from '@/components/ui/textarea';
import { codigoEtapa, mensajeDeErrorPedido, nombreRol } from './formato';

// Reasignar la etapa actual (contrato: PUT …/asignacion; solo el admin). P-07, valor provisional del equipo: la
// persona que la recibe debe estar activa y tener el rol responsable de la etapa; reasignar a otro rol sigue pendiente
// del cliente, así que la lista solo ofrece personas con ese rol. El servidor lo vuelve a comprobar.

interface Props {
  pedido: PedidoDetalle;
  etapa: EtapaEnLinea;
  onCerrar: () => void;
}

export function DialogoReasignar({ pedido, etapa, onCerrar }: Props) {
  const ids = useId();
  const usuarios = useUsuarios();
  const reasignar = useReasignarEtapa();
  const [usuarioId, setUsuarioId] = useState('');
  const [motivo, setMotivo] = useState('');
  const [errorPersona, setErrorPersona] = useState<string | null>(null);
  const [fallo, setFallo] = useState<unknown>(null);

  const rol = etapa.rolResponsable;
  const candidatos = usuarios.data?.filter((usuario) => usuario.activo && rol !== null && usuario.roles.includes(rol)) ?? [];

  function confirmar(evento: FormEvent) {
    evento.preventDefault();
    if (!usuarioId) {
      setErrorPersona('Elige a la persona que recibe la etapa.');
      return;
    }
    setErrorPersona(null);
    setFallo(null);
    reasignar.mutate(
      { pedidoId: pedido.id, etapa: etapa.numero, usuarioId, motivo: motivo.trim() || undefined },
      {
        onSuccess: () => {
          const nombre = candidatos.find((candidato) => candidato.id === usuarioId)?.nombre ?? 'la persona elegida';
          toast.success(`${codigoEtapa(pedido.tipoFlujo, etapa.numero)} quedó asignada a ${nombre}.`);
          onCerrar();
        },
        onError: (error) => {
          if (esErrorApi(error, 'VALIDACION')) {
            const campo = error.errores.find((e) => e.campo === 'usuarioId');
            if (campo) setErrorPersona(campo.mensaje);
          }
          setFallo(error);
        },
      },
    );
  }

  return (
    <Dialog open onOpenChange={(abierto) => !abierto && !reasignar.isPending && onCerrar()}>
      <DialogContent className="sm:max-w-md">
        <DialogHeader>
          <DialogTitle>Reasignar etapa</DialogTitle>
          <DialogDescription>
            {codigoEtapa(pedido.tipoFlujo, etapa.numero)} · {etapa.nombre}. Solo personas activas con el rol{' '}
            {nombreRol(rol)}.
          </DialogDescription>
        </DialogHeader>
        <form noValidate onSubmit={confirmar} className="flex flex-col gap-5">
          <Field data-invalid={errorPersona ? true : undefined}>
            <FieldLabel htmlFor={`${ids}-persona`}>Persona</FieldLabel>
            <Select value={usuarioId} onValueChange={setUsuarioId} disabled={!usuarios.data}>
              <SelectTrigger id={`${ids}-persona`} className="w-full" aria-invalid={errorPersona ? true : undefined}>
                <SelectValue placeholder={usuarios.isPending ? 'Cargando personas…' : 'Elige una persona'} />
              </SelectTrigger>
              <SelectContent>
                <SelectGroup>
                  {candidatos.map((candidato) => {
                    const yaAsignada = candidato.id === etapa.asignadoA?.id;
                    return (
                      <SelectItem key={candidato.id} value={candidato.id} disabled={yaAsignada}>
                        {candidato.nombre}
                        {yaAsignada ? ' (asignada ahora)' : ''}
                      </SelectItem>
                    );
                  })}
                </SelectGroup>
              </SelectContent>
            </Select>
            {usuarios.data && candidatos.length === 0 && (
              <FieldDescription>Nadie activo tiene ese rol. Asígnalo primero en Usuarios.</FieldDescription>
            )}
            {usuarios.isError && (
              <FieldDescription>
                {mensajeDeErrorPedido(usuarios.error, 'No se pudo cargar la lista de personas.')}
              </FieldDescription>
            )}
            <FieldError>{errorPersona}</FieldError>
          </Field>
          <Field>
            <FieldLabel htmlFor={`${ids}-motivo`}>Motivo (opcional)</FieldLabel>
            <Textarea
              id={`${ids}-motivo`}
              value={motivo}
              maxLength={1000}
              onChange={(evento) => setMotivo(evento.target.value)}
              aria-describedby={`${ids}-motivo-contador`}
            />
            <FieldDescription id={`${ids}-motivo-contador`} className="tabular-nums">
              {motivo.length} de 1000 caracteres · queda en el historial
            </FieldDescription>
          </Field>
          {fallo !== null && (
            <Alert variant="destructive" aria-live="polite">
              <CircleAlertIcon />
              <AlertDescription>
                {mensajeDeErrorPedido(fallo, 'No se pudo reasignar la etapa. Intenta de nuevo.')}
              </AlertDescription>
            </Alert>
          )}
          <DialogFooter>
            <Button type="button" variant="outline" onClick={onCerrar} disabled={reasignar.isPending}>
              Cancelar
            </Button>
            <Button type="submit" disabled={reasignar.isPending || candidatos.length === 0}>
              {reasignar.isPending && <LoaderIcon data-icon="inline-start" className="animate-spin" />}
              Reasignar
            </Button>
          </DialogFooter>
        </form>
      </DialogContent>
    </Dialog>
  );
}

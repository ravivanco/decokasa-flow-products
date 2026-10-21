import { SearchIcon } from 'lucide-react';
import { type FormEvent, useId, useState } from 'react';
import { useNavigate } from 'react-router';
import { esErrorApi } from '@/api/cliente';
import { useBuscarPedido } from '@/api/pedidos';
import { Button } from '@/components/ui/button';
import { Input } from '@/components/ui/input';
import { Label } from '@/components/ui/label';
import { mensajeDeErrorPedido } from '@/components/pedidos/formato';

// Búsqueda por número (requisito 4.2 #17; paso 5.3): lleva al pedido, con su línea de tiempo, su cronograma y el
// resumen de cada etapa. Es una búsqueda exacta en el servidor; para buscar por nombre está la lista de pedidos.

export function BuscarPorNumero() {
  const ids = useId();
  const navegar = useNavigate();
  const buscar = useBuscarPedido();
  const [numero, setNumero] = useState('');
  const [aviso, setAviso] = useState<string | null>(null);

  function enviar(evento: FormEvent) {
    evento.preventDefault();
    const limpio = numero.trim();
    if (!limpio) {
      setAviso('Escribe el número del pedido.');
      return;
    }
    setAviso(null);
    buscar.mutate(limpio, {
      onSuccess: (pedido) => void navegar(`/pedidos/${pedido.id}`),
      onError: (error) =>
        setAviso(
          esErrorApi(error, 'NO_ENCONTRADO')
            ? 'No hay un pedido con ese número.'
            : mensajeDeErrorPedido(error, 'No se pudo buscar el pedido. Intenta de nuevo.'),
        ),
    });
  }

  return (
    <form
      role="search"
      aria-label="Buscar pedido por número"
      noValidate
      onSubmit={enviar}
      className="flex flex-col gap-2 rounded-lg border bg-card p-4 shadow-sm"
    >
      <Label htmlFor={`${ids}-numero`}>Buscar pedido por número</Label>
      <div className="flex gap-2">
        <Input
          id={`${ids}-numero`}
          type="search"
          value={numero}
          onChange={(evento) => setNumero(evento.target.value)}
          placeholder="DK-EC-2026-0001"
          aria-invalid={aviso ? true : undefined}
          aria-describedby={`${ids}-aviso`}
        />
        <Button type="submit" variant="outline" disabled={buscar.isPending}>
          <SearchIcon data-icon="inline-start" />
          Buscar
        </Button>
      </div>
      <p id={`${ids}-aviso`} aria-live="polite" className="min-h-5 text-sm text-estado-vencido">
        {aviso}
      </p>
    </form>
  );
}

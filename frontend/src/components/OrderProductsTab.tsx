import React, { useState } from 'react';
import { Pedido, Usuario, ProductoLineaPedido } from '../types';
import { Package, Camera, AlertTriangle, Check, Plus } from 'lucide-react';

interface OrderProductsTabProps {
  pedido: Pedido;
  currentUser: Usuario;
  onUpdateProductQuantity?: (lineId: string, recibida: number, incidencia?: string) => void;
}

export const OrderProductsTab: React.FC<OrderProductsTabProps> = ({
  pedido,
  currentUser,
  onUpdateProductQuantity,
}) => {
  const [editingLineId, setEditingLineId] = useState<string | null>(null);
  const [inputRecibida, setInputRecibida] = useState<number>(0);
  const [inputIncidencia, setInputIncidencia] = useState('');

  const canEditWarehouse =
    (currentUser.rol === 'logistica' || currentUser.rol === 'admin') &&
    ((pedido.flujo === 'A' && pedido.etapaActualNumero === 9) ||
      (pedido.flujo === 'B' && pedido.etapaActualNumero === 10));

  const totalPedidos = pedido.productos.reduce((sum, p) => sum + p.cantidadPedida, 0);
  const totalRecibidos = pedido.productos.reduce((sum, p) => sum + (p.cantidadRecibida ?? 0), 0);
  const totalDiferencia = totalRecibidos - totalPedidos;

  const handleStartEdit = (line: ProductoLineaPedido) => {
    setEditingLineId(line.id);
    setInputRecibida(line.cantidadRecibida ?? line.cantidadPedida);
    setInputIncidencia(line.incidencias || '');
  };

  const handleSaveEdit = (lineId: string) => {
    if (onUpdateProductQuantity) {
      onUpdateProductQuantity(lineId, inputRecibida, inputIncidencia.trim() || undefined);
    }
    setEditingLineId(null);
  };

  return (
    <div className="bg-white rounded-3xl border border-[#6C6B6D]/20 shadow-xs p-6 sm:p-8 space-y-6">
      <div className="flex flex-col sm:flex-row sm:items-center justify-between gap-3 border-b border-slate-100 pb-4">
        <div>
          <h3 className="text-lg font-black text-[#2E2E2E] flex items-center gap-2">
            <Package className="w-5 h-5 text-[#515151]" />
            <span>Desglose de Productos del Pedido</span>
          </h3>
          <p className="text-xs text-[#6C6B6D] mt-0.5">
            Líneas arancelarias e inventario de bultos por contenedor.
          </p>
        </div>

        {canEditWarehouse && (
          <span className="text-xs font-black px-3 py-1.5 rounded-xl bg-[#FFFBEA] border border-[#FFD100] text-[#2E2E2E]">
            Modo Recepción de Bodega Activo (Anderson Enriquez)
          </span>
        )}
      </div>

      {/* Table */}
      <div className="overflow-x-auto">
        <table className="w-full text-left text-xs text-[#515151]">
          <thead className="bg-[#FFFBEA] text-[#2E2E2E] font-black uppercase text-[11px] border-b border-[#FFD100]/40">
            <tr>
              <th className="py-3 px-3">Cód. Decokasa</th>
              <th className="py-3 px-3">Cód. Chino</th>
              <th className="py-3 px-3">Descripción</th>
              <th className="py-3 px-3">Medida</th>
              <th className="py-3 px-3">Color</th>
              <th className="py-3 px-3 text-right">Cant. Pedida</th>
              <th className="py-3 px-3 text-right">Cant. Recibida</th>
              <th className="py-3 px-3 text-right">Diferencia</th>
              <th className="py-3 px-3 text-center">Estado</th>
              {canEditWarehouse && <th className="py-3 px-3 text-right">Acción</th>}
            </tr>
          </thead>
          <tbody className="divide-y divide-slate-100 font-medium">
            {pedido.productos.map((prod) => {
              const isEditing = editingLineId === prod.id;
              const diff = (prod.cantidadRecibida ?? prod.cantidadPedida) - prod.cantidadPedida;

              return (
                <tr key={prod.id} className="hover:bg-[#FFFBEA]/40 transition">
                  <td className="py-3 px-3 font-mono font-bold text-[#2E2E2E]">
                    {prod.codigoDecokasa}
                  </td>
                  <td className="py-3 px-3 font-mono text-[#6C6B6D]">
                    {prod.codigoChino}
                  </td>
                  <td className="py-3 px-3 font-bold text-[#2E2E2E] max-w-xs">
                    {prod.descripcion}
                    {prod.incidencias && (
                      <span className="block text-[10px] text-[#DC2626] font-bold mt-0.5">
                        ⚠ Incidencia: {prod.incidencias}
                      </span>
                    )}
                  </td>
                  <td className="py-3 px-3 text-[#6C6B6D]">{prod.medida}</td>
                  <td className="py-3 px-3">{prod.color}</td>
                  <td className="py-3 px-3 text-right font-black text-[#2E2E2E]">
                    {prod.cantidadPedida.toLocaleString()}
                  </td>

                  <td className="py-3 px-3 text-right">
                    {isEditing ? (
                      <input
                        type="number"
                        value={inputRecibida}
                        onChange={(e) => setInputRecibida(Number(e.target.value))}
                        className="w-20 p-1 border rounded font-black text-right text-xs"
                      />
                    ) : (
                      <span className="font-black text-[#2E2E2E]">
                        {prod.cantidadRecibida !== undefined
                          ? prod.cantidadRecibida.toLocaleString()
                          : '—'}
                      </span>
                    )}
                  </td>

                  <td className="py-3 px-3 text-right font-bold">
                    {prod.cantidadRecibida !== undefined ? (
                      <span
                        className={
                          diff === 0
                            ? 'text-[#16A34A]'
                            : diff < 0
                            ? 'text-[#DC2626]'
                            : 'text-[#EA580C]'
                        }
                      >
                        {diff > 0 ? `+${diff}` : diff}
                      </span>
                    ) : (
                      '—'
                    )}
                  </td>

                  <td className="py-3 px-3 text-center">
                    <span
                      className={`inline-block px-2 py-0.5 rounded-full text-[10px] font-bold ${
                        prod.estado === 'Completo'
                          ? 'bg-emerald-100 text-[#16A34A]'
                          : prod.estado === 'Dañado'
                          ? 'bg-rose-100 text-[#DC2626]'
                          : prod.estado === 'Faltante'
                          ? 'bg-orange-100 text-[#EA580C]'
                          : 'bg-slate-100 text-[#6C6B6D]'
                      }`}
                    >
                      {prod.estado}
                    </span>
                  </td>

                  {canEditWarehouse && (
                    <td className="py-3 px-3 text-right">
                      {isEditing ? (
                        <div className="flex items-center justify-end gap-1">
                          <button
                            type="button"
                            onClick={() => handleSaveEdit(prod.id)}
                            className="p-1 rounded bg-[#FFD100] text-[#2E2E2E] font-bold"
                            title="Guardar recepción"
                          >
                            <Check className="w-3.5 h-3.5" />
                          </button>
                        </div>
                      ) : (
                        <button
                          type="button"
                          onClick={() => handleStartEdit(prod)}
                          className="px-2 py-1 rounded-lg text-[10px] font-bold bg-[#FFD100] text-[#2E2E2E]"
                        >
                          Recibir
                        </button>
                      )}
                    </td>
                  )}
                </tr>
              );
            })}
          </tbody>

          {/* Totales al pie */}
          <tfoot className="border-t-2 border-[#FFD100] bg-[#FFFBEA] font-black text-xs text-[#2E2E2E]">
            <tr>
              <td colSpan={5} className="py-3.5 px-3 uppercase tracking-wider">
                Totales Consolidados del Pedido
              </td>
              <td className="py-3.5 px-3 text-right text-sm">
                {totalPedidos.toLocaleString()} unidades
              </td>
              <td className="py-3.5 px-3 text-right text-sm">
                {totalRecibidos > 0 ? `${totalRecibidos.toLocaleString()} unidades` : '—'}
              </td>
              <td className="py-3.5 px-3 text-right">
                {totalRecibidos > 0 ? (
                  <span
                    className={
                      totalDiferencia === 0
                        ? 'text-[#16A34A]'
                        : totalDiferencia < 0
                        ? 'text-[#DC2626]'
                        : 'text-[#EA580C]'
                    }
                  >
                    {totalDiferencia > 0 ? `+${totalDiferencia}` : totalDiferencia}
                  </span>
                ) : (
                  '—'
                )}
              </td>
              <td colSpan={canEditWarehouse ? 2 : 1}></td>
            </tr>
          </tfoot>
        </table>
      </div>

      {/* Incidences summary block */}
      {pedido.productos.some((p) => p.incidencias) && (
        <div className="p-4 rounded-2xl bg-rose-50 border border-rose-200 text-xs space-y-2">
          <div className="flex items-center gap-2 text-[#DC2626] font-black">
            <AlertTriangle className="w-4 h-4" />
            <span>Informe Automático de Incidencias en Bodega</span>
          </div>
          <ul className="list-disc pl-5 space-y-1 text-[#515151]">
            {pedido.productos
              .filter((p) => p.incidencias)
              .map((p) => (
                <li key={p.id}>
                  <strong>{p.codigoDecokasa} ({p.descripcion}):</strong> {p.incidencias}
                  {p.fotoIncidencia && (
                    <span className="text-[#6C6B6D] ml-1.5 font-mono text-[10px]">
                      [Foto adjunta: {p.fotoIncidencia}]
                    </span>
                  )}
                </li>
              ))}
          </ul>
        </div>
      )}
    </div>
  );
};

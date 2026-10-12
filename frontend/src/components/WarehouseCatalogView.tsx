import React, { useState } from 'react';
import { BodegaCatalogo, Usuario } from '../types';
import { Warehouse, Plus, MapPin, CheckCircle2, XCircle, Search, ShieldCheck } from 'lucide-react';
import { formatDisplayDate, SIMULATED_TODAY } from '../utils/dateUtils';

interface WarehouseCatalogViewProps {
  bodegas: BodegaCatalogo[];
  currentUser: Usuario;
  onAddWarehouse: (nueva: BodegaCatalogo) => void;
  onToggleWarehouseStatus: (id: string) => void;
}

export const WarehouseCatalogView: React.FC<WarehouseCatalogViewProps> = ({
  bodegas,
  currentUser,
  onAddWarehouse,
  onToggleWarehouseStatus,
}) => {
  const [searchTerm, setSearchTerm] = useState('');
  const [showAddModal, setShowAddModal] = useState(false);
  const [nombre, setNombre] = useState('');
  const [ciudad, setCiudad] = useState('');
  const [direccion, setDireccion] = useState('');

  const canManage = currentUser.rol === 'admin' || currentUser.rol === 'logistica';

  const filtered = bodegas.filter(
    (b) =>
      b.nombre.toLowerCase().includes(searchTerm.toLowerCase()) ||
      b.ciudad.toLowerCase().includes(searchTerm.toLowerCase())
  );

  const handleAddSubmit = (e: React.FormEvent) => {
    e.preventDefault();
    if (!nombre.trim() || !ciudad.trim()) return;

    const nueva: BodegaCatalogo = {
      id: `bod-${Date.now()}`,
      nombre: nombre.trim(),
      ciudad: ciudad.trim(),
      direccion: direccion.trim() || undefined,
      activa: true,
      creadaPor: currentUser.nombre,
      fechaCreacion: formatDisplayDate(SIMULATED_TODAY),
    };

    onAddWarehouse(nueva);
    setShowAddModal(false);
    setNombre('');
    setCiudad('');
    setDireccion('');
  };

  return (
    <div className="space-y-6 sm:space-y-8 animate-in fade-in duration-200">
      {/* Header */}
      <div className="flex flex-col sm:flex-row sm:items-center justify-between gap-4">
        <div>
          <h1 className="text-2xl sm:text-3xl font-black text-[#2E2E2E]">
            Catálogo de Bodegas de Destino
          </h1>
          <p className="text-xs sm:text-sm text-[#515151] mt-0.5">
            Mantenido por Anderson Enriquez (Logística). Bodegas autorizadas para la recepción en la etapa 9 (Flujo A) o 10 (Flujo B).
          </p>
        </div>

        {canManage && (
          <button
            type="button"
            onClick={() => setShowAddModal(true)}
            className="px-4 py-2.5 rounded-2xl bg-[#FFD100] hover:bg-[#E6BC00] text-[#2E2E2E] font-black text-xs transition shadow-2xs flex items-center justify-center gap-2 cursor-pointer shrink-0"
          >
            <Plus className="w-4 h-4 stroke-[2.5]" />
            <span>Agregar Nueva Bodega</span>
          </button>
        )}
      </div>

      {/* Search */}
      <div className="bg-white rounded-3xl p-4 sm:p-5 border border-[#6C6B6D]/20 shadow-xs flex items-center gap-3">
        <div className="relative flex-1">
          <Search className="w-4 h-4 text-[#6C6B6D] absolute left-3.5 top-1/2 -translate-y-1/2" />
          <input
            type="text"
            placeholder="Buscar por nombre de bodega o ciudad (Quito, Guayaquil, Ibarra, Manta, Cuenca...)..."
            value={searchTerm}
            onChange={(e) => setSearchTerm(e.target.value)}
            className="w-full pl-10 pr-4 py-2.5 rounded-xl border border-[#6C6B6D]/20 text-xs font-semibold text-[#2E2E2E] focus:outline-hidden focus:ring-2 focus:ring-[#FFD100]"
          />
        </div>

        <span className="text-xs font-bold text-[#6C6B6D] shrink-0">
          Total: <strong className="text-[#2E2E2E]">{bodegas.length} bodegas</strong>
        </span>
      </div>

      {/* Grid of Warehouses */}
      <div className="grid grid-cols-1 md:grid-cols-2 lg:grid-cols-3 gap-4">
        {filtered.map((b) => (
          <div
            key={b.id}
            className="bg-white rounded-3xl p-5 border border-[#6C6B6D]/20 shadow-xs space-y-3 flex flex-col justify-between"
          >
            <div className="space-y-2">
              <div className="flex items-start justify-between gap-2">
                <div className="flex items-center gap-2.5">
                  <div className="w-10 h-10 rounded-xl bg-[#FFFBEA] border border-[#FFD100] text-[#2E2E2E] flex items-center justify-center">
                    <Warehouse className="w-5 h-5 text-amber-700" />
                  </div>
                  <div>
                    <h3 className="text-xs sm:text-sm font-black text-[#2E2E2E] leading-tight">
                      {b.nombre}
                    </h3>
                    <div className="flex items-center gap-1 text-[11px] text-[#6C6B6D] mt-0.5">
                      <MapPin className="w-3 h-3 text-[#6C6B6D]" />
                      <span>{b.ciudad}</span>
                    </div>
                  </div>
                </div>

                <span
                  className={`px-2 py-0.5 rounded-md text-[10px] font-black uppercase shrink-0 ${
                    b.activa ? 'bg-emerald-100 text-emerald-800' : 'bg-slate-100 text-slate-600'
                  }`}
                >
                  {b.activa ? 'Activa' : 'Inactiva'}
                </span>
              </div>

              {b.direccion && (
                <p className="text-xs text-[#515151] bg-slate-50 p-2.5 rounded-xl border border-slate-100">
                  {b.direccion}
                </p>
              )}
            </div>

            <div className="pt-3 border-t border-slate-100 flex items-center justify-between text-[11px] text-[#6C6B6D]">
              <span>Registrada por: <strong>{b.creadaPor}</strong></span>
              {canManage && (
                <button
                  type="button"
                  onClick={() => onToggleWarehouseStatus(b.id)}
                  className="text-xs font-bold text-[#2E2E2E] hover:underline cursor-pointer"
                >
                  {b.activa ? 'Desactivar' : 'Activar'}
                </button>
              )}
            </div>
          </div>
        ))}
      </div>

      {/* Modal Agregar Bodega */}
      {showAddModal && (
        <div className="fixed inset-0 z-50 bg-black/50 backdrop-blur-xs flex items-center justify-center p-4">
          <div className="bg-white rounded-3xl max-w-md w-full p-6 space-y-4 shadow-2xl animate-in zoom-in-95">
            <h3 className="text-base font-black text-[#2E2E2E]">
              Registrar Nueva Bodega de Destino
            </h3>
            <p className="text-xs text-[#515151]">
              Anderson Enriquez puede añadir nuevas bodegas regionales para el ruteo logístico de contenedores.
            </p>

            <form onSubmit={handleAddSubmit} className="space-y-3">
              <div>
                <label className="block text-xs font-black uppercase text-[#515151] mb-1">
                  Nombre de la Bodega: *
                </label>
                <input
                  type="text"
                  required
                  placeholder="Ej: Bodega Santo Domingo"
                  value={nombre}
                  onChange={(e) => setNombre(e.target.value)}
                  className="w-full px-3 py-2 rounded-xl border border-[#6C6B6D]/30 text-xs font-medium"
                />
              </div>

              <div>
                <label className="block text-xs font-black uppercase text-[#515151] mb-1">
                  Ciudad: *
                </label>
                <input
                  type="text"
                  required
                  placeholder="Ej: Santo Domingo de los Tsáchilas"
                  value={ciudad}
                  onChange={(e) => setCiudad(e.target.value)}
                  className="w-full px-3 py-2 rounded-xl border border-[#6C6B6D]/30 text-xs font-medium"
                />
              </div>

              <div>
                <label className="block text-xs font-black uppercase text-[#515151] mb-1">
                  Dirección / Sector:
                </label>
                <input
                  type="text"
                  placeholder="Ej: Km 4 Vía a Quevedo"
                  value={direccion}
                  onChange={(e) => setDireccion(e.target.value)}
                  className="w-full px-3 py-2 rounded-xl border border-[#6C6B6D]/30 text-xs font-medium"
                />
              </div>

              <div className="flex items-center justify-end gap-2 pt-2">
                <button
                  type="button"
                  onClick={() => setShowAddModal(false)}
                  className="px-4 py-2 rounded-xl bg-slate-100 text-xs font-bold"
                >
                  Cancelar
                </button>
                <button
                  type="submit"
                  className="px-4 py-2 rounded-xl bg-[#FFD100] hover:bg-[#E6BC00] text-[#2E2E2E] font-black text-xs shadow-xs"
                >
                  Guardar Bodega
                </button>
              </div>
            </form>
          </div>
        </div>
      )}
    </div>
  );
};

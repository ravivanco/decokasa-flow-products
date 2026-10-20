package com.decokasa.seguimiento.bodegas.application.port.out;

import com.decokasa.seguimiento.bodegas.domain.Bodega;
import java.util.List;
import java.util.Optional;
import java.util.UUID;

/** Persistencia de las bodegas. Se usa dentro de la transacción del caso de uso. */
public interface BodegaRepositorio {

    /** @param activa nulo para todas. Ordenadas por nombre. */
    List<Bodega> listar(Boolean activa);

    Optional<Bodega> buscarPorId(UUID id);

    /** Bloquea la fila hasta el final de la transacción y la relee: dos ediciones simultáneas se serializan. */
    Optional<Bodega> buscarParaActualizar(UUID id);

    /** Búsqueda sin distinguir mayúsculas, como el índice único de V9. */
    Optional<Bodega> buscarPorNombre(String nombre);

    /**
     * Inserta o actualiza y devuelve lo guardado, con la versión de la base.
     *
     * @throws com.decokasa.seguimiento.compartido.domain.DatoDuplicado si otra alta o edición simultánea dejó el nombre
     *         repetido
     * @throws com.decokasa.seguimiento.compartido.domain.VersionDesactualizada si la versión cambió sin el bloqueo
     */
    Bodega guardar(Bodega bodega);
}

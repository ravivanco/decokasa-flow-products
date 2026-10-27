package com.decokasa.seguimiento.bodegas.application.port.in;

import com.decokasa.seguimiento.bodegas.domain.Bodega;
import com.decokasa.seguimiento.compartido.domain.Solicitante;
import java.util.UUID;

/**
 * Edición, activación o desactivación (contrato: PATCH /bodegas/{id}). F4-D1, provisional del equipo: solo Logística o
 * admin vigentes. Una bodega no se borra; desactivarla impide asignaciones nuevas y no cambia los pedidos que ya la
 * tienen ni su historial.
 *
 * @throws com.decokasa.seguimiento.compartido.domain.SinPermiso sin Logística ni admin vigentes en la base
 * @throws com.decokasa.seguimiento.compartido.domain.NoEncontrado si la bodega no existe
 * @throws com.decokasa.seguimiento.compartido.domain.VersionDesactualizada si {@code version} ya no es la vigente
 * @throws com.decokasa.seguimiento.compartido.domain.DatoDuplicado si el nombre nuevo ya es de otra bodega
 */
public interface ActualizarBodega {

    Bodega actualizar(Solicitante solicitante, Comando comando);

    /** Los campos nulos no cambian. {@code version} es la que leyó el cliente. */
    record Comando(UUID bodegaId, String nombre, String ciudad, Boolean activa, long version) {
    }
}

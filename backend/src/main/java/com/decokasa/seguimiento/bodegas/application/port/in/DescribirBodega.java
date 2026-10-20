package com.decokasa.seguimiento.bodegas.application.port.in;

import java.util.Optional;
import java.util.UUID;

/**
 * Para otros módulos (pedidos, en el detalle): una bodega por su identificador, activa o no. Una bodega desactivada
 * sigue apareciendo en los pedidos que ya la tienen (F4-D1). Sin control de acceso: quien la usa ya autorizó su
 * consulta.
 */
public interface DescribirBodega {

    /** Vacío si la bodega no existe. */
    Optional<Vista> describir(UUID bodegaId);

    /** Esquema Bodega del contrato. */
    record Vista(UUID id, String nombre, String ciudad, boolean activa, long version) {
    }
}

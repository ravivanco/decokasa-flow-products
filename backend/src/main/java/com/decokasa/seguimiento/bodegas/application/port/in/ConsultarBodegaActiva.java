package com.decokasa.seguimiento.bodegas.application.port.in;

import com.decokasa.seguimiento.compartido.domain.CodigoPais;
import java.util.Optional;
import java.util.UUID;

/**
 * Para otros módulos (pedidos, al elegir la bodega en A9; todavía sin uso): una bodega existente y activa. Sin control de
 * acceso: quien la usa ya autorizó su operación.
 */
public interface ConsultarBodegaActiva {

    /** Vacío si la bodega no existe o está inactiva. */
    Optional<BodegaActiva> activa(UUID bodegaId);

    /** @param ciudad nula si está pendiente; @param pais país de la bodega (fase 7: debe ser el del pedido) */
    record BodegaActiva(UUID id, String nombre, String ciudad, CodigoPais pais) {

        /** Solo compatibilidad documentada (anterior a la fase 7): una bodega de Ecuador. No decide permisos. */
        public BodegaActiva(UUID id, String nombre, String ciudad) {
            this(id, nombre, ciudad, CodigoPais.EC);
        }
    }
}

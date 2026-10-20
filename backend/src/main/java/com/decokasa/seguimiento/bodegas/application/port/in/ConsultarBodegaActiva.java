package com.decokasa.seguimiento.bodegas.application.port.in;

import java.util.Optional;
import java.util.UUID;

/**
 * Para otros módulos (pedidos, al elegir la bodega en A9; todavía sin uso): una bodega existente y activa. Sin control de
 * acceso: quien la usa ya autorizó su operación.
 */
public interface ConsultarBodegaActiva {

    /** Vacío si la bodega no existe o está inactiva. */
    Optional<BodegaActiva> activa(UUID bodegaId);

    record BodegaActiva(UUID id, String nombre, String ciudad) {
    }
}

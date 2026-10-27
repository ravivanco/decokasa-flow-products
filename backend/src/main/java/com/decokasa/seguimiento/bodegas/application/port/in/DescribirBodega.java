package com.decokasa.seguimiento.bodegas.application.port.in;

import com.decokasa.seguimiento.compartido.domain.CodigoPais;
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
    record Vista(UUID id, String nombre, String ciudad, boolean activa, long version, CodigoPais pais) {

        /** Solo compatibilidad documentada (anterior a la fase 7): una bodega de Ecuador. No decide permisos. */
        public Vista(UUID id, String nombre, String ciudad, boolean activa, long version) {
            this(id, nombre, ciudad, activa, version, CodigoPais.EC);
        }
    }
}

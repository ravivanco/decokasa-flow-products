package com.decokasa.seguimiento.bodegas.adapter.in.web;

import com.decokasa.seguimiento.bodegas.domain.Bodega;
import com.decokasa.seguimiento.compartido.domain.CodigoPais;
import java.util.UUID;

/**
 * Esquema Bodega del contrato, con la versión para editar (409 VERSION_DESACTUALIZADA si ya no es la vigente). Desde
 * 0.17.0: {@code pais}, y {@code ciudad} nula mientras está pendiente.
 */
public record BodegaRespuesta(UUID id, String nombre, String ciudad, boolean activa, long version, CodigoPais pais) {

    static BodegaRespuesta de(Bodega bodega) {
        return new BodegaRespuesta(bodega.id(), bodega.nombre(), bodega.ciudad(), bodega.activa(), bodega.version(),
                bodega.pais());
    }
}

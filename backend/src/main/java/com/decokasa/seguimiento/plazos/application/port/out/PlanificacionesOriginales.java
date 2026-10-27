package com.decokasa.seguimiento.plazos.application.port.out;

import com.decokasa.seguimiento.plazos.domain.PlanificacionOriginal;
import java.util.Optional;
import java.util.UUID;

/** Planificaciones originales guardadas (V25, solo inserción). */
public interface PlanificacionesOriginales {

    /** La guardada al crear el pedido; vacío si el pedido es anterior a V25. */
    Optional<PlanificacionOriginal> de(UUID pedidoId);

    void guardar(UUID flujoId, PlanificacionOriginal planificacion);
}

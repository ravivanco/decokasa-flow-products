package com.decokasa.seguimiento.notificaciones.domain;

import java.time.Instant;
import java.util.Objects;
import java.util.UUID;

/**
 * Aviso de etapa para una persona (ADR-0032). Copia lo que se muestra para no depender de cambios posteriores del
 * pedido. Java puro (R1).
 *
 * @param etapaOrigen nula en la creación
 * @param motivo      observación o motivo de la acción; nulo si no tiene
 */
public record AvisoEtapa(UUID id, UUID eventoId, UUID pedidoId, UUID usuarioId, Tipo tipo, String pais,
        String tipoFlujo, String numeroPedido, Integer etapaOrigen, String nombreOrigen, int etapaDestino,
        String nombreDestino, UUID actorId, String actorNombre, String motivo, boolean comoAdmin,
        boolean comoResponsable, Instant ocurridoEn) {

    public enum Tipo {
        CREACION, LLEGADA, DEVOLUCION, REASIGNACION, CIERRE
    }

    public AvisoEtapa {
        Objects.requireNonNull(id, "id");
        Objects.requireNonNull(eventoId, "eventoId");
        Objects.requireNonNull(pedidoId, "pedidoId");
        Objects.requireNonNull(usuarioId, "usuarioId");
        Objects.requireNonNull(tipo, "tipo");
        Objects.requireNonNull(ocurridoEn, "ocurridoEn");
        if (!comoAdmin && !comoResponsable) {
            throw new IllegalArgumentException("Un aviso es para la admin, para un responsable o para ambos.");
        }
    }
}

package com.decokasa.seguimiento.compartido.domain.eventos;

import com.decokasa.seguimiento.compartido.domain.CodigoPais;
import com.decokasa.seguimiento.compartido.domain.TipoFlujo;
import java.time.Instant;
import java.util.List;
import java.util.Objects;
import java.util.UUID;

/**
 * Se agregó una observación a la entrada actual de la etapa actual (tipo OBSERVACION_AGREGADA). P-19, provisional del
 * equipo: la agrega quien puede actuar en la etapa (P-06) o el admin. No mueve el pedido ni cambia plazos.
 *
 * @param eventoId identificador del evento en el historial; lo fija el dominio para que la API devuelva ese evento
 * @param adjuntoIds archivos de la entrada actual que acompañan la observación (relación PRESENTADO)
 */
public record ObservacionAgregada(DatosDelPedido pedido, int etapa, Interviniente actor, Instant ocurridoEn,
        String observacion, UUID eventoId, List<UUID> adjuntoIds)
        implements EventoDePedido {

    public ObservacionAgregada {
        Objects.requireNonNull(pedido, "pedido");
        Objects.requireNonNull(actor, "actor");
        Objects.requireNonNull(ocurridoEn, "ocurridoEn");
        Objects.requireNonNull(observacion, "observacion");
        adjuntoIds = adjuntoIds == null ? List.of() : List.copyOf(adjuntoIds);
    }

    /** Constructor anterior al paso 2.4: sin id fijo (el historial genera uno) y sin archivos. */
    public ObservacionAgregada(DatosDelPedido pedido, int etapa, Interviniente actor, Instant ocurridoEn,
            String observacion) {
        this(pedido, etapa, actor, ocurridoEn, observacion, null, List.of());
    }

    @Override
    public UUID pedidoId() {
        return pedido.pedidoId();
    }

    @Override
    public String numeroPedido() {
        return pedido.numeroPedido();
    }

    @Override
    public CodigoPais pais() {
        return pedido.pais();
    }

    @Override
    public TipoFlujo tipoFlujo() {
        return pedido.tipoFlujo();
    }
}

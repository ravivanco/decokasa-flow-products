package com.decokasa.seguimiento.compartido.domain.eventos;

import com.decokasa.seguimiento.compartido.domain.CodigoPais;
import com.decokasa.seguimiento.compartido.domain.TipoFlujo;
import java.time.Instant;
import java.util.Objects;
import java.util.UUID;

/**
 * Se asignó la bodega de destino al aprobar la etapa de Bodega (tipo BODEGA_ASIGNADA; F4-D2, provisional del equipo).
 * Se registra una sola vez por pedido, en la misma transacción que la aprobación.
 */
public record BodegaAsignada(DatosDelPedido pedido, int etapa, Interviniente actor, Instant ocurridoEn,
        UUID bodegaId) implements EventoDePedido {

    public BodegaAsignada {
        Objects.requireNonNull(pedido, "pedido");
        Objects.requireNonNull(actor, "actor");
        Objects.requireNonNull(ocurridoEn, "ocurridoEn");
        Objects.requireNonNull(bodegaId, "bodegaId");
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

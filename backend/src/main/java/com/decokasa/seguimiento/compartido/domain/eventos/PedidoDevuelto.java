package com.decokasa.seguimiento.compartido.domain.eventos;

import com.decokasa.seguimiento.compartido.domain.CodigoPais;
import com.decokasa.seguimiento.compartido.domain.NivelUrgencia;
import com.decokasa.seguimiento.compartido.domain.TipoFlujo;
import java.time.Instant;
import java.util.List;
import java.util.Objects;
import java.util.UUID;

/**
 * El pedido se devolvió por error o por producto nuevo (tipo PEDIDO_DEVUELTO). {@code nivelUrgencia} solo existe en la
 * devolución por producto nuevo.
 */
public record PedidoDevuelto(DatosDelPedido pedido, int etapa, Interviniente actor, Instant ocurridoEn,
        int etapaDestino, MotivoDevolucion motivo, String observacion, NivelUrgencia nivelUrgencia,
        List<UUID> adjuntoIds)
        implements EventoDePedido {

    public PedidoDevuelto {
        adjuntoIds = adjuntoIds == null ? List.of() : List.copyOf(adjuntoIds);
        Objects.requireNonNull(pedido, "pedido");
        Objects.requireNonNull(actor, "actor");
        Objects.requireNonNull(ocurridoEn, "ocurridoEn");
        Objects.requireNonNull(motivo, "motivo");
        if ((motivo == MotivoDevolucion.PRODUCTO_NUEVO) != (nivelUrgencia != null)) {
            throw new IllegalArgumentException("Solo la devolución por producto nuevo lleva urgencia.");
        }
    }

    /** Sin archivos presentados. */
    public PedidoDevuelto(DatosDelPedido pedido, int etapa, Interviniente actor, Instant ocurridoEn,
            int etapaDestino, MotivoDevolucion motivo, String observacion, NivelUrgencia nivelUrgencia) {
        this(pedido, etapa, actor, ocurridoEn, etapaDestino, motivo, observacion, nivelUrgencia, List.of());
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

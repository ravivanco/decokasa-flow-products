package com.decokasa.seguimiento.compartido.domain.eventos;

import com.decokasa.seguimiento.compartido.domain.CodigoPais;
import com.decokasa.seguimiento.compartido.domain.TipoFlujo;
import java.time.Instant;
import java.util.List;
import java.util.Objects;
import java.util.UUID;

/**
 * La etapa se envió a la siguiente (tipo ETAPA_ENVIADA). {@code urgenciaTerminada}: el pedido volvió a la etapa de
 * retorno de una urgencia.
 */
public record EtapaEnviada(DatosDelPedido pedido, int etapa, Interviniente actor, Instant ocurridoEn, int etapaDestino,
        String observacion, boolean urgenciaTerminada,
        List<UUID> adjuntoIds)
        implements EventoDePedido {

    public EtapaEnviada {
        adjuntoIds = adjuntoIds == null ? List.of() : List.copyOf(adjuntoIds);
        Objects.requireNonNull(pedido, "pedido");
        Objects.requireNonNull(actor, "actor");
        Objects.requireNonNull(ocurridoEn, "ocurridoEn");
    }

    /** Sin archivos presentados. */
    public EtapaEnviada(DatosDelPedido pedido, int etapa, Interviniente actor, Instant ocurridoEn, int etapaDestino,
            String observacion, boolean urgenciaTerminada) {
        this(pedido, etapa, actor, ocurridoEn, etapaDestino, observacion, urgenciaTerminada, List.of());
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

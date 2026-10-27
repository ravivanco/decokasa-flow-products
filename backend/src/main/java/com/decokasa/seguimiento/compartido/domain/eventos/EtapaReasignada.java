package com.decokasa.seguimiento.compartido.domain.eventos;

import com.decokasa.seguimiento.compartido.domain.CodigoPais;
import com.decokasa.seguimiento.compartido.domain.TipoFlujo;
import java.time.Instant;
import java.util.Objects;
import java.util.UUID;

/**
 * El admin reasignó la etapa actual (tipo ETAPA_REASIGNADA). {@code asignadoAnterior} es nulo si nadie la tenía;
 * {@code motivo} es opcional.
 */
public record EtapaReasignada(DatosDelPedido pedido, int etapa, Interviniente actor, Instant ocurridoEn,
        UUID asignadoAnterior, UUID asignadoA, String motivo)
        implements EventoDePedido {

    public EtapaReasignada {
        Objects.requireNonNull(pedido, "pedido");
        Objects.requireNonNull(actor, "actor");
        Objects.requireNonNull(ocurridoEn, "ocurridoEn");
        Objects.requireNonNull(asignadoA, "asignadoA");
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

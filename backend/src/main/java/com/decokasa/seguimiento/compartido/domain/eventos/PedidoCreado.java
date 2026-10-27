package com.decokasa.seguimiento.compartido.domain.eventos;

import com.decokasa.seguimiento.compartido.domain.CodigoPais;
import com.decokasa.seguimiento.compartido.domain.TipoFlujo;
import java.time.Instant;
import java.util.Objects;
import java.util.UUID;

/** Se creó el pedido en la primera etapa de su flujo (tipo PEDIDO_CREADO). */
public record PedidoCreado(DatosDelPedido pedido, int etapa, Interviniente actor, Instant ocurridoEn)
        implements EventoDePedido {

    public PedidoCreado {
        Objects.requireNonNull(pedido, "pedido");
        Objects.requireNonNull(actor, "actor");
        Objects.requireNonNull(ocurridoEn, "ocurridoEn");
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

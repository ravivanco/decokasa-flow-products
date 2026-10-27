package com.decokasa.seguimiento.compartido.domain.eventos;

import com.decokasa.seguimiento.compartido.domain.CodigoPais;
import com.decokasa.seguimiento.compartido.domain.TipoFlujo;
import java.time.Instant;
import java.util.Objects;
import java.util.UUID;

/** Fin automático: evento del sistema, sin atribuir al usuario el check de la etapa visual. */
public record PedidoFinalizado(DatosDelPedido pedido, int etapa, Instant ocurridoEn) implements EventoDePedido {

    public PedidoFinalizado {
        Objects.requireNonNull(pedido, "pedido");
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

    @Override
    public Interviniente actor() {
        return null;
    }
}

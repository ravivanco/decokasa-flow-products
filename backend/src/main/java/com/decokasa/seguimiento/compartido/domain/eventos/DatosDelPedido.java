package com.decokasa.seguimiento.compartido.domain.eventos;

import com.decokasa.seguimiento.compartido.domain.CodigoPais;
import com.decokasa.seguimiento.compartido.domain.TipoFlujo;
import java.util.Objects;
import java.util.UUID;

/** Identidad del pedido en un evento: no cambia durante la vida del pedido. */
public record DatosDelPedido(UUID pedidoId, String numeroPedido, CodigoPais pais, TipoFlujo tipoFlujo) {

    public DatosDelPedido {
        Objects.requireNonNull(pedidoId, "pedidoId");
        Objects.requireNonNull(numeroPedido, "numeroPedido");
        Objects.requireNonNull(pais, "pais");
        Objects.requireNonNull(tipoFlujo, "tipoFlujo");
    }
}

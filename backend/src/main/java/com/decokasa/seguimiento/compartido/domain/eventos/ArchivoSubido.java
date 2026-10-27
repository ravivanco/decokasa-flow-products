package com.decokasa.seguimiento.compartido.domain.eventos;

import com.decokasa.seguimiento.compartido.domain.CodigoPais;
import com.decokasa.seguimiento.compartido.domain.TipoFlujo;
import java.time.Instant;
import java.util.List;
import java.util.Objects;
import java.util.UUID;

/**
 * Se subió un archivo real a una entrada de una etapa (tipo ARCHIVO_SUBIDO; P-05 opción A, P-18). Solo lleva
 * identificadores y el tipo de documento: nunca el contenido ni la ruta de almacenamiento.
 */
public record ArchivoSubido(DatosDelPedido pedido, int etapa, Interviniente actor, Instant ocurridoEn, UUID adjuntoId,
        UUID entradaId, String tipoDocumento)
        implements EventoDePedido {

    public ArchivoSubido {
        Objects.requireNonNull(pedido, "pedido");
        Objects.requireNonNull(actor, "actor");
        Objects.requireNonNull(ocurridoEn, "ocurridoEn");
        Objects.requireNonNull(adjuntoId, "adjuntoId");
        Objects.requireNonNull(entradaId, "entradaId");
        Objects.requireNonNull(tipoDocumento, "tipoDocumento");
    }

    @Override
    public List<UUID> adjuntoIds() {
        return List.of(adjuntoId);
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

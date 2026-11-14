package com.decokasa.seguimiento.compartido.domain.eventos;

import com.decokasa.seguimiento.compartido.domain.CodigoPais;
import com.decokasa.seguimiento.compartido.domain.TipoFlujo;
import java.time.Instant;
import java.util.Objects;

/**
 * Algo pasó con los BL del pedido (tipo EMBARQUE_REGISTRADO): guardar, vincular, corregir un Packing List, enviar o
 * decidir. No mueve el pedido; los movimientos siguen siendo ETAPA_ENVIADA, ETAPA_APROBADA y ETAPA_RECHAZADA. El detalle
 * estructurado vive en las tablas de envíos; {@code texto} es la línea que lee la persona en el historial.
 *
 * @param avisaResponsables avisar a los responsables de la etapa actual: hay una versión nueva que revisar
 */
public record EventoEmbarque(DatosDelPedido pedido, int etapa, Interviniente actor, Instant ocurridoEn, String texto,
        boolean avisaResponsables) implements EventoDePedido {

    public EventoEmbarque {
        Objects.requireNonNull(pedido, "pedido");
        Objects.requireNonNull(actor, "actor");
        Objects.requireNonNull(ocurridoEn, "ocurridoEn");
        Objects.requireNonNull(texto, "texto");
    }

    @Override
    public java.util.UUID pedidoId() {
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

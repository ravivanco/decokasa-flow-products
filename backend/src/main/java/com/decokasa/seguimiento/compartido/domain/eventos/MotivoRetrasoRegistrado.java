package com.decokasa.seguimiento.compartido.domain.eventos;

import com.decokasa.seguimiento.compartido.domain.CodigoPais;
import com.decokasa.seguimiento.compartido.domain.TipoFlujo;
import java.time.Instant;
import java.util.Objects;
import java.util.UUID;

/**
 * Se registró o corrigió el motivo del retraso de la entrada actual de la etapa actual (tipo
 * MOTIVO_RETRASO_REGISTRADO; F5-D3, aprobada provisionalmente por Richard el 2026-10-20). Lo hace quien puede actuar en
 * la etapa (P-06) o el admin, mientras hay un vencimiento activo. Cada registro es un evento nuevo: los motivos
 * anteriores quedan en el historial. No mueve el pedido, no cambia plazos y no genera correo.
 *
 * @param entradaId entrada de la etapa a la que pertenece el motivo (P-18)
 * @param eventoId  identificador del evento en el historial; lo fija el dominio para que la API devuelva ese evento
 */
public record MotivoRetrasoRegistrado(DatosDelPedido pedido, int etapa, Interviniente actor, Instant ocurridoEn,
        String motivo, UUID entradaId, UUID eventoId) implements EventoDePedido {

    public MotivoRetrasoRegistrado {
        Objects.requireNonNull(pedido, "pedido");
        Objects.requireNonNull(actor, "actor");
        Objects.requireNonNull(ocurridoEn, "ocurridoEn");
        Objects.requireNonNull(motivo, "motivo");
        Objects.requireNonNull(entradaId, "entradaId");
        Objects.requireNonNull(eventoId, "eventoId");
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

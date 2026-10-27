package com.decokasa.seguimiento.compartido.domain.eventos;

import java.util.Objects;
import java.util.UUID;

/** Aviso síncrono interno tras insertar historial, aún antes del commit de la acción. No es otro evento histórico. */
public record EventoPedidoRegistrado(UUID eventoId, EventoDePedido evento) {
    public EventoPedidoRegistrado {
        Objects.requireNonNull(eventoId, "eventoId");
        Objects.requireNonNull(evento, "evento");
    }
}

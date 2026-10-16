package com.decokasa.seguimiento.historial.application.usecase;

import com.decokasa.seguimiento.compartido.domain.eventos.EventoDePedido;
import com.decokasa.seguimiento.historial.application.port.in.RegistrarEventoPedido;
import com.decokasa.seguimiento.historial.application.port.out.HistorialRepositorio;
import com.decokasa.seguimiento.historial.domain.EventoPedido;
import java.util.Objects;
import java.util.UUID;

/** Traduce el evento y lo inserta. No abre transacción: usa la de la acción (ADR-0012). Sin Spring (R2). */
public final class RegistrarEventoPedidoCasoDeUso implements RegistrarEventoPedido {

    private final HistorialRepositorio historial;

    public RegistrarEventoPedidoCasoDeUso(HistorialRepositorio historial) {
        this.historial = historial;
    }

    @Override
    public void registrar(EventoDePedido evento) {
        Objects.requireNonNull(evento, "evento");
        UUID id = evento.eventoId() == null ? UUID.randomUUID() : evento.eventoId();
        historial.insertar(EventoPedido.desde(id, evento));
    }
}

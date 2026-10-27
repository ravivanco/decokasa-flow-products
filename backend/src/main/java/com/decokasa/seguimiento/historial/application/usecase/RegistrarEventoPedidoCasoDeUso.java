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
    private final com.decokasa.seguimiento.compartido.application.port.out.PublicadorEventosRegistrados publicador;

    public RegistrarEventoPedidoCasoDeUso(HistorialRepositorio historial) {
        this(historial, evento -> { });
    }

    public RegistrarEventoPedidoCasoDeUso(HistorialRepositorio historial,
            com.decokasa.seguimiento.compartido.application.port.out.PublicadorEventosRegistrados publicador) {
        this.historial = Objects.requireNonNull(historial);
        this.publicador = Objects.requireNonNull(publicador);
    }

    @Override
    public void registrar(EventoDePedido evento) {
        Objects.requireNonNull(evento, "evento");
        UUID id = evento.eventoId() == null ? UUID.randomUUID() : evento.eventoId();
        historial.insertar(EventoPedido.desde(id, evento));
        publicador.publicar(new com.decokasa.seguimiento.compartido.domain.eventos.EventoPedidoRegistrado(id, evento));
    }
}

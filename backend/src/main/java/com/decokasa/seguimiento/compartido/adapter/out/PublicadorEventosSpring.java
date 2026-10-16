package com.decokasa.seguimiento.compartido.adapter.out;

import com.decokasa.seguimiento.compartido.application.port.out.PublicadorEventos;
import com.decokasa.seguimiento.compartido.domain.eventos.EventoDePedido;
import java.util.Objects;
import org.springframework.context.ApplicationEventPublisher;

/**
 * Publica los eventos con el mecanismo de Spring. La entrega a los {@code @EventListener} es síncrona, en el mismo hilo
 * y en la misma transacción que la acción: un error del oyente llega al caso de uso y revierte todo.
 */
public final class PublicadorEventosSpring implements PublicadorEventos {

    private final ApplicationEventPublisher publicador;

    public PublicadorEventosSpring(ApplicationEventPublisher publicador) {
        this.publicador = Objects.requireNonNull(publicador, "publicador");
    }

    @Override
    public void publicar(EventoDePedido evento) {
        publicador.publishEvent(Objects.requireNonNull(evento, "evento"));
    }
}

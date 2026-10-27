package com.decokasa.seguimiento.usuarios.adapter.out.eventos;

import com.decokasa.seguimiento.compartido.application.port.out.Reloj;
import com.decokasa.seguimiento.compartido.domain.eventos.UsuarioCreado;
import com.decokasa.seguimiento.usuarios.application.port.out.AvisoAltaUsuario;
import java.util.Objects;
import java.util.UUID;
import org.springframework.context.ApplicationEventPublisher;

/**
 * Publica {@link UsuarioCreado} con el mecanismo de Spring: la entrega a los {@code @EventListener} es síncrona y en la
 * misma transacción del alta, como {@code PublicadorEventosSpring}. Un error del oyente revierte el alta.
 */
public final class AvisoAltaUsuarioSpring implements AvisoAltaUsuario {

    private final ApplicationEventPublisher publicador;
    private final Reloj reloj;

    public AvisoAltaUsuarioSpring(ApplicationEventPublisher publicador, Reloj reloj) {
        this.publicador = Objects.requireNonNull(publicador, "publicador");
        this.reloj = Objects.requireNonNull(reloj, "reloj");
    }

    @Override
    public void usuarioCreado(UUID usuarioId) {
        publicador.publishEvent(new UsuarioCreado(usuarioId, reloj.ahora()));
    }
}
